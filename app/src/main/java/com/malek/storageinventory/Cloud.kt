package com.malek.storageinventory

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

/** Waits for a Google Play Services task without extra libraries. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { t ->
        val e = t.exception
        if (e != null) cont.resumeWithException(e)
        else if (t.isCanceled) cont.cancel()
        else cont.resume(t.result)
    }
}

/**
 * Account sign-in (email/password or Google) and cloud sync through Firebase.
 * Same data layout as the web app:
 *   users/{uid}/nodes/{id}   – one document per storage / container / item
 *   users/{uid}/photos/{id}  – the photo as base64 JPEG in field "data"
 * The newest change (updatedAt) wins; deletions are written as { deleted: true }.
 */
object Cloud {
    private const val TAG = "Cloud"
    private const val WEB_CLIENT_ID =
        "355071035442-31nu48807keq8n8n92f5uab208ek1k7h.apps.googleusercontent.com"

    enum class Status { OFF, CONNECTING, SYNCING, SYNCED, OFFLINE, ERROR }

    // ----- State shown in the UI -----
    var userEmail by mutableStateOf<String?>(null)
        private set
    var provider by mutableStateOf("")
        private set
    var status by mutableStateOf(Status.OFF)
        private set
    var message by mutableStateOf("")
        private set
    var lastSync by mutableLongStateOf(0L)
        private set
    /** Bumped whenever a photo arrives from another device, so images reload. */
    var photoVersion by mutableIntStateOf(0)
        private set
    /** Set when this device holds data from a different account; the UI asks what to do. */
    var accountConflict by mutableStateOf<Int?>(null)
        private set

    private lateinit var appContext: Context
    private lateinit var store: InventoryStore
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    private var listener: ListenerRegistration? = null
    private var uid: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val fetching = HashSet<String>()

    private val prefs by lazy { appContext.getSharedPreferences("cloud", Context.MODE_PRIVATE) }

    fun init(context: Context, inventory: InventoryStore) {
        appContext = context.applicationContext
        store = inventory
        try {
            val app = FirebaseApp.getApps(appContext).firstOrNull() ?: FirebaseApp.initializeApp(
                appContext,
                FirebaseOptions.Builder()
                    .setApiKey("AIzaSyDMKtV9uIkopONzbdwYxdlKBFnbnVIqTFs")
                    .setApplicationId("1:355071035442:android:437cfe8990957cca4ad42b")
                    .setProjectId("storageinventory-fd8ca")
                    .setGcmSenderId("355071035442")
                    .setStorageBucket("storageinventory-fd8ca.firebasestorage.app")
                    .build(),
            )
            auth = FirebaseAuth.getInstance(app)
            db = FirebaseFirestore.getInstance(app)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase init failed", e)
            status = Status.ERROR
            message = "Sync unavailable: ${e.message}"
            return
        }
        store.onLocalChange = { n -> pushNode(n) }
        store.onLocalDelete = { id, t, photos -> writeDelete(id, t, photos.map { store.photoId(it) }) }
        store.onPhotoRemoved = { p -> deletePhoto(store.photoId(p)) }
        auth?.addAuthStateListener { a -> onUser(a.currentUser) }
    }

    // ----- Sign-in -----

    private fun onUser(u: FirebaseUser?) {
        if (u?.uid == uid && listener != null) return
        listener?.remove(); listener = null
        uid = u?.uid
        if (u == null) {
            userEmail = null; provider = ""; status = Status.OFF; message = ""
            return
        }
        userEmail = u.email ?: u.displayName ?: "your account"
        provider = if (u.providerData.any { it.providerId == "google.com" }) "Google" else "email"
        val last = prefs.getString("lastUid", null)
        prefs.edit().putString("lastUid", u.uid).apply()
        ActivityLog.log("Signed in", "$userEmail ($provider)")
        if (last != null && last != u.uid && store.nodes.isNotEmpty()) {
            accountConflict = store.nodes.size // UI asks: replace or add
            return
        }
        startListening()
    }

    /** Answer to the “data from another account” question. */
    fun resolveConflict(replace: Boolean) {
        accountConflict = null
        if (replace) store.clearAll()
        startListening()
    }

    suspend fun signInEmail(email: String, password: String) {
        auth!!.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun createAccount(email: String, password: String) {
        auth!!.createUserWithEmailAndPassword(email, password).await()
    }

    suspend fun resetPassword(email: String) {
        auth!!.sendPasswordResetEmail(email).await()
    }

    /** Google sign-in through Android's Credential Manager. Returns false if the person cancelled. */
    suspend fun signInGoogle(activityContext: Context): Boolean {
        val option = GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request)
        } catch (e: GetCredentialCancellationException) {
            return false
        }
        val cred = result.credential
        if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("Google didn't return an account.")
        }
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        auth!!.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        return true
    }

    fun signOut(removeFromDevice: Boolean) {
        ActivityLog.log("Signed out", if (removeFromDevice) "removed data from device" else "kept a copy on device")
        auth?.signOut()
        if (removeFromDevice) {
            store.clearAll()
            prefs.edit().remove("lastUid").apply()
        }
    }

    /** Friendly text for sign-in errors. */
    fun errorText(e: Throwable): String {
        val code = (e as? FirebaseAuthException)?.errorCode ?: ""
        return when (code) {
            "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD" -> "Wrong email or password."
            "ERROR_USER_NOT_FOUND" -> "No account with that email. Tap “Create account” instead."
            "ERROR_INVALID_EMAIL" -> "That email address doesn’t look right."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists. Use “Sign in” instead."
            "ERROR_WEAK_PASSWORD" -> "Choose a password with at least 6 characters."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a few minutes and try again."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "This email is already used with a different sign-in method."
            else -> when (e) {
                is NoCredentialException -> "No Google account found on this phone. Add one in Android Settings › Accounts, or use email."
                is GetCredentialException -> "Google sign-in didn’t work: ${e.message ?: e.type}"
                else -> if ((e.message ?: "").contains("network", true)) "No internet connection. Try again when you’re online."
                else e.message ?: "Something went wrong."
            }
        }
    }

    // ----- Sync -----

    private fun nodes() = db!!.collection("users").document(uid!!).collection("nodes")
    private fun photos() = db!!.collection("users").document(uid!!).collection("photos")

    private fun startListening() {
        val myUid = uid ?: return
        status = Status.SYNCING
        var merged = false
        listener = nodes().addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (uid != myUid) return@addSnapshotListener
            if (err != null) {
                Log.w(TAG, "listen failed", err)
                ActivityLog.log("Sync error", err.message ?: "")
                status = Status.ERROR; message = err.message ?: ""
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            snap.documentChanges.forEach { c ->
                if (c.type != DocumentChange.Type.REMOVED) applyRemote(c.document)
            }
            if (!merged && !snap.metadata.isFromCache) {
                merged = true
                pushLocal(snap.documents)
            }
            if (!snap.metadata.hasPendingWrites() && !snap.metadata.isFromCache) lastSync = System.currentTimeMillis()
            status = when {
                snap.metadata.hasPendingWrites() -> Status.SYNCING
                snap.metadata.isFromCache && !merged -> Status.OFFLINE
                else -> Status.SYNCED
            }
        }
    }

    private fun num(d: DocumentSnapshot, key: String): Long = (d.get(key) as? Number)?.toLong() ?: 0L

    /** Brings a change made on another device into this one. */
    private fun applyRemote(d: DocumentSnapshot) {
        val id = d.getString("id") ?: d.id
        val rt = num(d, "updatedAt")
        val local = store.get(id)
        if (d.getBoolean("deleted") == true) {
            if (local != null && local.updatedAt <= rt) store.removeRemote(id)
            return
        }
        val tomb = store.tombstones[id]
        if (tomb != null && tomb >= rt) return // deleted here more recently; pushLocal sends it
        if (local != null && local.updatedAt >= rt) return
        @Suppress("UNCHECKED_CAST")
        val photoIds = (d.get("photoIds") as? List<Any?>)?.mapNotNull { it as? String } ?: emptyList()
        val type = try { NodeType.valueOf(d.getString("type") ?: "ITEM") } catch (e: Exception) { NodeType.ITEM }
        val node = Node(
            id = id,
            qr = d.getString("qr") ?: "",
            name = d.getString("name") ?: "",
            type = type,
            parentId = d.getString("parentId"),
            createdAt = num(d, "createdAt").takeIf { it > 0 } ?: rt,
            photoPaths = photoIds.map { store.photoPath(it) },
            archived = d.getBoolean("archived") == true,
            updatedAt = rt,
        )
        store.applyRemote(node)
        photoIds.forEach { ensurePhoto(it) }
    }

    /** After sign-in: sends what only this device has, or has newer. */
    private fun pushLocal(remote: List<DocumentSnapshot>) {
        val byId = remote.associateBy { it.getString("id") ?: it.id }
        var sent = 0
        store.nodes.toList().forEach { n ->
            val r = byId[n.id]
            if (r == null || n.updatedAt > num(r, "updatedAt")) { pushNode(n); sent++ }
        }
        store.tombstones.forEach { (id, t) ->
            val r = byId[id]
            if (r != null && r.getBoolean("deleted") != true && num(r, "updatedAt") <= t) { writeDelete(id, t, emptyList()); sent++ }
        }
        if (sent > 0) ActivityLog.log("Uploaded to account", "$sent change(s) from this device")
    }

    fun pushNode(n: Node) {
        if (uid == null || db == null || accountConflict != null) return
        val photoIds = n.photoPaths.map { store.photoId(it) }
        val data = hashMapOf<String, Any?>(
            "id" to n.id, "qr" to n.qr, "name" to n.name, "type" to n.type.name, "parentId" to n.parentId,
            "createdAt" to n.createdAt, "updatedAt" to n.updatedAt, "photoIds" to photoIds,
            "archived" to n.archived, "deleted" to false,
        )
        nodes().document(n.id).set(data).addOnFailureListener { fail(it) }
        photoIds.forEach { uploadPhoto(it) }
    }

    private fun writeDelete(id: String, t: Long, photoIds: List<String>) {
        if (uid == null || db == null) return
        nodes().document(id).set(hashMapOf("id" to id, "deleted" to true, "updatedAt" to t)).addOnFailureListener { fail(it) }
        photoIds.forEach { deletePhoto(it) }
    }

    private fun uploadedKey() = "uploaded_$uid"
    private fun uploaded(): MutableSet<String> = HashSet(prefs.getStringSet(uploadedKey(), emptySet()) ?: emptySet())
    private fun markUploaded(id: String, yes: Boolean = true) {
        val s = uploaded()
        if (yes) s.add(id) else s.remove(id)
        prefs.edit().putStringSet(uploadedKey(), s).apply()
    }

    private fun uploadPhoto(id: String) {
        val myUid = uid ?: return
        if (id in uploaded()) return
        scope.launch {
            val data = withContext(Dispatchers.IO) { encodePhoto(File(store.photoPath(id))) } ?: return@launch
            if (uid != myUid) return@launch
            photos().document(id).set(hashMapOf("id" to id, "data" to data, "updatedAt" to System.currentTimeMillis()))
                .addOnSuccessListener { markUploaded(id) }
                .addOnFailureListener { fail(it) }
        }
    }

    /** Shrinks a photo to fit in one cloud document (≤ ~700 KB). */
    private fun encodePhoto(file: File): String? {
        if (!file.exists()) return null
        val src = PhotoStore.load(file.path, 1280) ?: return null
        fun jpeg(bmp: Bitmap, side: Int, quality: Int): ByteArray {
            val scale = min(1f, side.toFloat() / max(bmp.width, bmp.height))
            val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
            return ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
        }
        var bytes = jpeg(src, 1280, 72)
        if (bytes.size > 700_000) bytes = jpeg(src, 900, 60)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun deletePhoto(id: String) {
        if (uid == null || db == null) return
        markUploaded(id, false)
        photos().document(id).delete()
    }

    /** Downloads a photo another device added. */
    private fun ensurePhoto(id: String) {
        val file = File(store.photoPath(id))
        if (file.exists() || id in fetching || uid == null) return
        fetching.add(id)
        photos().document(id).get()
            .addOnSuccessListener { d ->
                val b64 = d.getString("data")
                if (b64 != null) scope.launch {
                    withContext(Dispatchers.IO) { file.writeBytes(Base64.decode(b64, Base64.DEFAULT)) }
                    markUploaded(id)
                    photoVersion++
                }
                fetching.remove(id)
            }
            .addOnFailureListener { fetching.remove(id); Log.w(TAG, "photo download failed $id", it) }
    }

    fun syncNow() {
        if (uid == null) return
        status = Status.SYNCING
        store.nodes.toList().forEach { n ->
            pushNode(n)
            n.photoPaths.forEach { ensurePhoto(store.photoId(it)) }
        }
    }

    private fun fail(e: Exception) {
        Log.w(TAG, "sync write failed", e)
        ActivityLog.log("Sync error", e.message ?: e.toString())
        if ((e.message ?: "").contains("PERMISSION_DENIED")) {
            status = Status.ERROR; message = "The account rejected the change (permission denied)."
        }
    }
}
