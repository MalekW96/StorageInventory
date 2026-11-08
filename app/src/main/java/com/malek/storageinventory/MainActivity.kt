package com.malek.storageinventory

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        ActivityLog.init(applicationContext)
        val store = InventoryStore(applicationContext)
        setContent {
            InventoryTheme {
                InventoryApp(store)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Theme
// ---------------------------------------------------------------------------

private val Teal = Color(0xFF1F4E5F)

/**
 * Notepad-style monospace font. Windows Notepad uses Consolas, which can't be bundled
 * in an app, so this uses Inconsolata (SIL Open Font License), designed after Consolas.
 */
private val NotepadFont = FontFamily(
    Font(R.font.inconsolata_regular, FontWeight.Normal),
    Font(R.font.inconsolata_medium, FontWeight.Medium),
    Font(R.font.inconsolata_semibold, FontWeight.SemiBold),
    Font(R.font.inconsolata_bold, FontWeight.Bold),
)

private val AppTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = NotepadFont),
        displayMedium = t.displayMedium.copy(fontFamily = NotepadFont),
        displaySmall = t.displaySmall.copy(fontFamily = NotepadFont),
        headlineLarge = t.headlineLarge.copy(fontFamily = NotepadFont),
        headlineMedium = t.headlineMedium.copy(fontFamily = NotepadFont),
        headlineSmall = t.headlineSmall.copy(fontFamily = NotepadFont),
        titleLarge = t.titleLarge.copy(fontFamily = NotepadFont),
        titleMedium = t.titleMedium.copy(fontFamily = NotepadFont),
        titleSmall = t.titleSmall.copy(fontFamily = NotepadFont),
        bodyLarge = t.bodyLarge.copy(fontFamily = NotepadFont),
        bodyMedium = t.bodyMedium.copy(fontFamily = NotepadFont),
        bodySmall = t.bodySmall.copy(fontFamily = NotepadFont),
        labelLarge = t.labelLarge.copy(fontFamily = NotepadFont),
        labelMedium = t.labelMedium.copy(fontFamily = NotepadFont),
        labelSmall = t.labelSmall.copy(fontFamily = NotepadFont),
    )
}
private val Amber = Color(0xFFE09A3E)

@Composable
fun InventoryTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFF8CCFE3),
            onPrimary = Color(0xFF00363F),
            secondary = Color(0xFFF2B35B),
            primaryContainer = Teal,
            onPrimaryContainer = Color.White,
        )
    } else {
        lightColorScheme(
            primary = Teal,
            onPrimary = Color.White,
            secondary = Amber,
            primaryContainer = Teal,
            onPrimaryContainer = Color.White,
            background = Color(0xFFF6F7F8),
            surface = Color(0xFFF6F7F8),
        )
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}

// ---------------------------------------------------------------------------
// App flow
// ---------------------------------------------------------------------------

/** What a scan is for. */
private sealed interface ScanMode {
    data object NewStorage : ScanMode
    data class AddTo(val parentId: String) : ScanMode
    data object Find : ScanMode
    /** Pick the storage/container a photo-only item goes into. */
    data object PickTarget : ScanMode
    /** Attach a QR code to an entry created without one. */
    data class AssignQr(val nodeId: String) : ScanMode
}

/** A scanned code waiting to be named (and maybe photographed) before it's saved. */
private data class PendingNew(val qr: String, val parentId: String?, val photos: List<String>)

// Simple string encoding so pending state survives Android closing the app while
// the camera app is open.
private const val SEP = "\u001F"
private const val PSEP = "\u001E"
private fun splitPhotos(raw: String?): List<String> = raw?.split(PSEP)?.filter { it.isNotBlank() } ?: emptyList()
private fun joinPhotos(list: List<String>): String? = list.takeIf { it.isNotEmpty() }?.joinToString(PSEP)
private fun PendingNew.encode() = listOf(qr, parentId ?: "", photos.joinToString(PSEP)).joinToString(SEP)
private fun decodePending(s: String?): PendingNew? = s?.split(SEP)?.takeIf { it.size == 3 }?.let {
    PendingNew(it[0], it[1].ifEmpty { null }, splitPhotos(it[2]))
}

/** Full-screen photo viewer state: the photos, where to start, and the entry they belong to (for deleting). */
private data class ViewerState(val paths: List<String>, val index: Int, val nodeId: String?)

private val stackSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

@Composable
fun InventoryApp(store: InventoryStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Navigation: empty = home, otherwise the ids of opened storages/containers.
    val stack = rememberSaveable(saver = stackSaver) { mutableListOf<String>().toMutableStateList() }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    // Photo-only item being added ("Take photo to add to existing storage")
    var photoItemRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var photoItemTarget by rememberSaveable { mutableStateOf<String?>(null) }
    // Temporary storage being created
    var creatingTemp by rememberSaveable { mutableStateOf(false) }
    var tempPhotosRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var scanMode by remember { mutableStateOf<ScanMode?>(null) }

    // Dialog state
    var pendingStorageRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingChildRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var askAnother by remember { mutableStateOf<Pair<String, String>?>(null) }  // parentId to added name
    var duplicate by remember { mutableStateOf<Node?>(null) }
    var notFoundQr by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<Node?>(null) }
    var deleting by remember { mutableStateOf<Node?>(null) }
    var viewingItem by remember { mutableStateOf<String?>(null) }
    var viewer by remember { mutableStateOf<ViewerState?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var infoFor by remember { mutableStateOf<String?>(null) }

    // --- Photos -------------------------------------------------------------
    // captureTarget says what the photo being taken is for:
    //   "S<sep>qr"           new storage, "C<sep>parent<sep>qr"  new item/container,
    //   "N<sep>nodeId"       add a photo to an existing entry
    var captureTarget by rememberSaveable { mutableStateOf<String?>(null) }

    fun onPhotoResult(target: String, path: String?) {
        val parts = target.split(SEP)
        when (parts[0]) {
            "S", "C" -> {
                val isStorage = parts[0] == "S"
                val parentId = if (isStorage) null else parts[1]
                val qr = if (isStorage) parts[1] else parts[2]
                val old = decodePending(if (isStorage) pendingStorageRaw else pendingChildRaw)
                    ?.takeIf { it.qr == qr }
                val photos = (old?.photos ?: emptyList()) + listOfNotNull(path)
                val encoded = PendingNew(qr, parentId, photos).encode()
                if (isStorage) pendingStorageRaw = encoded else pendingChildRaw = encoded
            }
            "N" -> if (path != null) store.addPhoto(parts[1], path)
            "P" -> if (path != null) {
                photoItemRaw = joinPhotos(splitPhotos(photoItemRaw) + path)
            } else if (photoItemRaw == null) {
                photoItemTarget = null
            }
            "T" -> if (path != null) {
                tempPhotosRaw = joinPhotos(splitPhotos(tempPhotosRaw) + path)
            }
            "Q" -> if (path != null) {
                searchPhoto?.let { File(it).delete() }
                searchPhoto = path
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val target = captureTarget ?: return@rememberLauncherForActivityResult
        captureTarget = null
        ActivityLog.log(if (ok) "Photo taken" else "Photo cancelled", photoPurpose(target))
        scope.launch {
            val path = if (ok) withContext(Dispatchers.IO) { PhotoStore.finishCapture(context) } else null
            onPhotoResult(target, path)
        }
    }

    fun launchCamera(target: String) {
        captureTarget = target
        try {
            cameraLauncher.launch(PhotoStore.captureUri(context))
        } catch (e: Exception) {
            captureTarget = null
            onPhotoResult(target, null)
            message = "Couldn't open the camera app. You can add a photo later from the item's menu."
        }
    }

    var permissionTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val target = permissionTarget ?: return@rememberLauncherForActivityResult
        permissionTarget = null
        if (granted) launchCamera(target) else {
            onPhotoResult(target, null)
            message = "Camera access is needed to take photos."
        }
    }

    fun takePhoto(target: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera(target)
        } else {
            permissionTarget = target
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun closeSearch() {
        searchPhoto?.let { File(it).delete() }
        searchPhoto = null
        showSearch = false
    }

    fun clearPhotoItem() {
        splitPhotos(photoItemRaw).forEach { File(it).delete() }
        photoItemRaw = null
        photoItemTarget = null
    }

    fun discardPending(raw: String?) {
        decodePending(raw)?.photos?.forEach { File(it).delete() }
    }

    /** Removes one not-yet-saved photo from a pending entry. */
    fun removePendingPhoto(raw: String?, path: String): String? {
        File(path).delete()
        return decodePending(raw)?.let { it.copy(photos = it.photos - path).encode() }
    }

    fun openNode(node: Node) {
        // Items open their containing storage/container.
        val target = if (node.canHoldChildren) node else node.parentId?.let { store.get(it) }
        stack.clear()
        if (target != null) stack.addAll(store.path(target.id).map { it.id })
        if (!node.canHoldChildren) viewingItem = node.id
    }

    // If the open storage/container was deleted, step back out of it.
    val currentId = stack.lastOrNull()
    if (currentId != null && store.get(currentId) == null) {
        LaunchedEffect(currentId, store.nodes.size) {
            while (stack.isNotEmpty() && store.get(stack.last()) == null) stack.removeAt(stack.lastIndex)
        }
    }

    fun exportStorage(node: Node) {
        ActivityLog.log("Export to Excel started", "“${node.name}”")
        scope.launch {
            try {
                val version = Updater.installedVersion(context).second
                val sheets = Exporter.storageSheets(store, node, version)
                val file = withContext(Dispatchers.IO) {
                    Exporter.write(context, Exporter.fileName(node.name), sheets)
                }
                ActivityLog.log("Exported to Excel", "“${node.name}” · ${sheets.first().rows.size} rows · ${file.name}")
                Exporter.share(context, file, "Storage Inventory – ${node.name}")
            } catch (e: Exception) {
                ActivityLog.log("Export failed", "“${node.name}” · ${e.message}")
                message = "Couldn't create the Excel file: ${e.message}"
            }
        }
    }

    val screenName = when {
        showSettings -> "Settings"
        showSearch -> "Search"
        else -> currentId?.let { store.get(it) }?.let { "${typeLabel(it.type)}: ${it.name}" } ?: "Home"
    }
    LaunchedEffect(screenName) {
        ActivityLog.screen = screenName
        ActivityLog.log("Opened screen", screenName)
    }

    BackHandler(enabled = stack.isNotEmpty() && scanMode == null && !showSettings && !showSearch) {
        stack.removeAt(stack.lastIndex)
    }

    Box(Modifier.fillMaxSize()) {
        val current = currentId?.let { store.get(it) }
        when {
            showSettings -> SettingsScreen(store = store, onBack = { showSettings = false })
            showSearch -> SearchScreen(
                store = store,
                photoPath = searchPhoto,
                onTakePhoto = { takePhoto("Q") },
                onClearPhoto = {
                    searchPhoto?.let { File(it).delete() }
                    searchPhoto = null
                },
                onScanQr = { scanMode = ScanMode.Find },
                onOpen = { closeSearch(); openNode(it) },
                onBack = { closeSearch() },
            )
            current == null -> HomeScreen(
                store = store,
                onAddByPhoto = { photoItemTarget = null; takePhoto("P") },
                onAdd = { scanMode = ScanMode.NewStorage },
                onAddTemporary = { creatingTemp = true },
                onFind = { showSearch = true },
                onAssignQr = { scanMode = ScanMode.AssignQr(it.id) },
                onExport = { exportStorage(it) },
                onSettings = { showSettings = true },
                onOpen = { stack.add(it.id) },
                onRename = { renaming = it },
                onDelete = { deleting = it },
                onPhoto = { takePhoto("N$SEP${it.id}") },
                onInfo = { infoFor = it.id },
            )
            else -> DetailScreen(
                store = store,
                node = current,
                onBack = { stack.removeAt(stack.lastIndex) },
                onScanAdd = { scanMode = ScanMode.AddTo(current.id) },
                onPhotoAdd = { photoItemTarget = current.id; takePhoto("P") },
                onAssignQr = { scanMode = ScanMode.AssignQr(it.id) },
                onExport = { exportStorage(it) },
                onOpen = { child ->
                    if (child.canHoldChildren) stack.add(child.id) else viewingItem = child.id
                },
                onRename = { renaming = it },
                onDelete = { deleting = it },
                onPhoto = { takePhoto("N$SEP${it.id}") },
                onViewPhoto = { node, index -> viewer = ViewerState(node.photoPaths, index, node.id) },
                onInfo = { infoFor = it.id },
            )
        }

        val mode = scanMode
        if (mode != null) {
            val (title, subtitle) = when (mode) {
                ScanMode.NewStorage -> "Scan the Storage QR code" to "Point the camera at the QR code on the storage unit."
                is ScanMode.AddTo -> {
                    val parentName = store.get(mode.parentId)?.name ?: ""
                    "Scan the next QR code" to "Adding to “$parentName”. Scan the code on the item or smaller container."
                }
                ScanMode.Find -> "Find by QR code" to "Scan any code to jump to where it is stored."
                ScanMode.PickTarget -> "Scan the storage's QR code" to
                    "Scan the QR code of the storage or container to put this item in."
                is ScanMode.AssignQr -> {
                    val name = store.get(mode.nodeId)?.name ?: ""
                    "Scan a QR code" to "The code will be attached to “$name”."
                }
            }
            LaunchedEffect(mode) { ActivityLog.log("Scanner opened", title) }
            QrScannerScreen(
                title = title,
                subtitle = subtitle,
                onCancel = {
                    ActivityLog.log("Scan cancelled", title)
                    scanMode = null
                },
                onScanned = { qr ->
                    ActivityLog.log("QR scanned", "$qr · $title")
                    scanMode = null
                    val existing = store.findByQr(qr)
                    when (mode) {
                        ScanMode.Find -> if (existing != null) {
                            closeSearch()
                            openNode(existing)
                        } else notFoundQr = qr
                        ScanMode.PickTarget -> when {
                            existing == null -> message = "That QR code isn't in your inventory yet. Pick a storage from the list instead."
                            !existing.canHoldChildren -> message = "That code belongs to the item “${existing.name}”. Scan a storage or container instead."
                            else -> photoItemTarget = existing.id
                        }
                        is ScanMode.AssignQr ->
                            if (existing != null) duplicate = existing
                            else store.get(mode.nodeId)?.let { node ->
                                store.setQr(node.id, qr)
                                message = "QR code attached to “${node.name}”."
                            }
                        ScanMode.NewStorage ->
                            if (existing != null) duplicate = existing
                            else takePhoto("S$SEP$qr")
                        is ScanMode.AddTo ->
                            if (existing != null) duplicate = existing
                            else takePhoto("C$SEP${mode.parentId}$SEP$qr")
                    }
                    Unit
                },
            )
        }
    }

    // 1) Name a newly scanned storage (with its photo)
    val pendingStorage = decodePending(pendingStorageRaw)
    if (pendingStorage != null) {
        NameDialog(
            title = "Name this storage",
            message = "QR code: ${pendingStorage.qr}",
            label = "Storage name",
            confirmText = "Done",
            photoPaths = pendingStorage.photos,
            onTakePhoto = { takePhoto("S$SEP${pendingStorage.qr}") },
            onRemovePhoto = { pendingStorageRaw = removePendingPhoto(pendingStorageRaw, it) },
            onViewPhoto = { i -> viewer = ViewerState(pendingStorage.photos, i, null) },
            onConfirm = { name ->
                val storage = store.add(pendingStorage.qr, name, NodeType.STORAGE, null, pendingStorage.photos)
                pendingStorageRaw = null
                stack.clear()
                stack.add(storage.id)
            },
            onDismiss = {
                discardPending(pendingStorageRaw)
                pendingStorageRaw = null
            },
        )
    }

    // Photo-only item: choose where it goes, then name it
    val photoItems = splitPhotos(photoItemRaw)
    if (photoItems.isNotEmpty() && scanMode == null) {
        val target = photoItemTarget?.let { store.get(it) }
        if (target == null) {
            ChooseTargetDialog(
                store = store,
                photoPath = photoItems.first(),
                onPick = { photoItemTarget = it.id },
                onScan = { scanMode = ScanMode.PickTarget },
                onDismiss = { clearPhotoItem() },
            )
        } else {
            AddChildDialog(
                parentName = target.name,
                qr = "",
                photoPaths = photoItems,
                onTakePhoto = { takePhoto("P") },
                onRemovePhoto = { path ->
                    File(path).delete()
                    val left = photoItems - path
                    photoItemRaw = joinPhotos(left)
                    if (left.isEmpty()) photoItemTarget = null
                },
                onViewPhoto = { i -> viewer = ViewerState(photoItems, i, null) },
                onConfirm = { type, name ->
                    store.add("", name, type, target.id, photoItems)
                    photoItemRaw = null
                    photoItemTarget = null
                    openNode(target)
                },
                onDismiss = { clearPhotoItem() },
            )
        }
    }

    // Temporary storage (no QR code yet)
    if (creatingTemp) {
        NameDialog(
            title = "Create temporary storage",
            message = "No QR code needed. You can attach one later with “Assign QR code” in its ⋮ menu.",
            label = "Storage name",
            confirmText = "Create",
            photoPaths = splitPhotos(tempPhotosRaw),
            onTakePhoto = { takePhoto("T") },
            onRemovePhoto = { path ->
                File(path).delete()
                tempPhotosRaw = joinPhotos(splitPhotos(tempPhotosRaw) - path)
            },
            onViewPhoto = { i -> viewer = ViewerState(splitPhotos(tempPhotosRaw), i, null) },
            onConfirm = { name ->
                val storage = store.add("", name, NodeType.STORAGE, null, splitPhotos(tempPhotosRaw))
                tempPhotosRaw = null
                creatingTemp = false
                stack.clear()
                stack.add(storage.id)
            },
            onDismiss = {
                splitPhotos(tempPhotosRaw).forEach { File(it).delete() }
                tempPhotosRaw = null
                creatingTemp = false
            },
        )
    }

    // 3) Scanned something to add -> item or smaller container? (with its photo)
    val pendingChild = decodePending(pendingChildRaw)
    if (pendingChild != null) {
        val parent = pendingChild.parentId?.let { store.get(it) }
        if (parent == null) {
            discardPending(pendingChildRaw)
            pendingChildRaw = null
        } else {
            AddChildDialog(
                parentName = parent.name,
                qr = pendingChild.qr,
                photoPaths = pendingChild.photos,
                onTakePhoto = { takePhoto("C$SEP${parent.id}$SEP${pendingChild.qr}") },
                onRemovePhoto = { pendingChildRaw = removePendingPhoto(pendingChildRaw, it) },
                onViewPhoto = { i -> viewer = ViewerState(pendingChild.photos, i, null) },
                onConfirm = { type, name ->
                    store.add(pendingChild.qr, name, type, parent.id, pendingChild.photos)
                    pendingChildRaw = null
                    askAnother = parent.id to name.trim()
                },
                onDismiss = {
                    discardPending(pendingChildRaw)
                    pendingChildRaw = null
                },
            )
        }
    }

    // 4) Added -> scan another?
    askAnother?.let { (parentId, addedName) ->
        val parent = store.get(parentId)
        if (parent == null) {
            askAnother = null
        } else {
            AlertDialog(
                onDismissRequest = { askAnother = null },
                title = { Text("Added “$addedName”") },
                text = { Text("Scan the next QR code to keep adding to “${parent.name}”?") },
                confirmButton = {
                    TextButton(onClick = {
                        askAnother = null
                        scanMode = ScanMode.AddTo(parentId)
                    }) { Text("Scan next") }
                },
                dismissButton = {
                    TextButton(onClick = { askAnother = null }) { Text("Done") }
                },
            )
        }
    }

    // Item details
    viewingItem?.let { id ->
        val item = store.get(id)
        LaunchedEffect(id) { item?.let { ActivityLog.log("Opened item", "“${it.name}”") } }
        if (item == null) {
            viewingItem = null
        } else {
            ItemDialog(
                item = item,
                location = store.path(item.id).dropLast(1).joinToString(" › ") { it.name },
                onPhoto = { takePhoto("N$SEP${item.id}") },
                onViewPhoto = { i -> viewer = ViewerState(item.photoPaths, i, item.id) },
                onRename = { renaming = item },
                onDelete = { viewingItem = null; deleting = item },
                onInfo = { infoFor = item.id },
                onDismiss = { viewingItem = null },
            )
        }
    }

    viewer?.let { v ->
        // For saved entries, show the live list so deletions are reflected.
        val paths = v.nodeId?.let { store.get(it)?.photoPaths } ?: v.paths
        if (paths.isEmpty()) {
            viewer = null
        } else {
            PhotoViewer(
                paths = paths,
                startIndex = v.index.coerceIn(0, paths.lastIndex),
                onDelete = v.nodeId?.let { id -> { path: String -> store.removePhoto(id, path) } },
                onDismiss = { viewer = null },
            )
        }
    }

    infoFor?.let { id ->
        val node = store.get(id)
        LaunchedEffect(id) { node?.let { ActivityLog.log("Viewed info", "“${it.name}”") } }
        if (node == null) {
            infoFor = null
        } else {
            InfoDialog(store = store, node = node, onDismiss = { infoFor = null })
        }
    }

    // QR already used
    duplicate?.let { node ->
        LaunchedEffect(node.id) { ActivityLog.log("Duplicate QR code", "Belongs to “${node.name}” · QR ${node.qr}") }
        val where = store.path(node.id).dropLast(1).joinToString(" › ") { it.name }
        AlertDialog(
            onDismissRequest = { duplicate = null },
            title = { Text("QR code already registered") },
            text = {
                Text(
                    "This code belongs to the ${typeLabel(node.type).lowercase()} “${node.name}”" +
                        (if (where.isNotEmpty()) " in $where." else ".") +
                        " Each QR code can only be used once."
                )
            },
            confirmButton = {
                TextButton(onClick = { duplicate = null; openNode(node) }) { Text("Open it") }
            },
            dismissButton = { TextButton(onClick = { duplicate = null }) { Text("OK") } },
        )
    }

    notFoundQr?.let { qr ->
        LaunchedEffect(qr) { ActivityLog.log("QR not in inventory", qr) }
        AlertDialog(
            onDismissRequest = { notFoundQr = null },
            title = { Text("Not in your inventory") },
            text = { Text("No storage, container or item uses this QR code:\n$qr") },
            confirmButton = { TextButton(onClick = { notFoundQr = null }) { Text("OK") } },
        )
    }

    message?.let { text ->
        LaunchedEffect(text) { ActivityLog.log("Notice shown", text) }
        AlertDialog(
            onDismissRequest = { message = null },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
        )
    }

    renaming?.let { node ->
        NameDialog(
            title = "Rename ${typeLabel(node.type).lowercase()}",
            message = null,
            label = "Name",
            initial = node.name,
            confirmText = "Save",
            onConfirm = { store.rename(node.id, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }

    deleting?.let { node ->
        val (items, containers) = store.countInside(node.id)
        val inside = if (items + containers > 0) {
            " Everything inside it will be removed too (${countText(items, containers)})."
        } else ""
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete “${node.name}”?") },
            text = { Text("This can't be undone.$inside") },
            confirmButton = {
                TextButton(onClick = { store.delete(node.id); deleting = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

// ---------------------------------------------------------------------------
// Screens
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    store: InventoryStore,
    onAddByPhoto: () -> Unit,
    onAdd: () -> Unit,
    onAddTemporary: () -> Unit,
    onFind: () -> Unit,
    onAssignQr: (Node) -> Unit,
    onExport: (Node) -> Unit,
    onSettings: () -> Unit,
    onOpen: (Node) -> Unit,
    onRename: (Node) -> Unit,
    onDelete: (Node) -> Unit,
    onPhoto: (Node) -> Unit,
    onInfo: (Node) -> Unit,
) {
    val storages = store.storages()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Inventory", fontWeight = FontWeight.SemiBold) },
                colors = barColors(),
                actions = {
                    if (storages.isNotEmpty()) {
                        IconButton(onClick = onFind) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                    var addMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { addMenu = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add")
                        }
                        DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Take photo to add to existing storage") },
                                enabled = storages.isNotEmpty(),
                                onClick = { addMenu = false; onAddByPhoto() },
                            )
                            DropdownMenuItem(
                                text = { Text("Add new storage (scan QR code)") },
                                onClick = { addMenu = false; onAdd() },
                            )
                            DropdownMenuItem(
                                text = { Text("Create temporary storage") },
                                onClick = { addMenu = false; onAddTemporary() },
                            )
                        }
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings and updates")
                    }
                },
            )
        },
    ) { padding ->
        if (storages.isEmpty()) {
            EmptyState(
                title = "No Storage Data Available",
                hint = "Tap + in the top right corner to add a storage.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(storages, key = { it.id }) { storage ->
                    val (items, containers) = store.countInside(storage.id)
                    NodeRow(
                        node = storage,
                        detail = (if (storage.isTemporary) "Temporary · " else "") + countText(items, containers),
                        onClick = { onOpen(storage) },
                        onRename = { onRename(storage) },
                        onDelete = { onDelete(storage) },
                        onPhoto = { onPhoto(storage) },
                        onInfo = { onInfo(storage) },
                        onAssignQr = { onAssignQr(storage) },
                        onExport = { onExport(storage) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(
    store: InventoryStore,
    node: Node,
    onBack: () -> Unit,
    onScanAdd: () -> Unit,
    onPhotoAdd: () -> Unit,
    onAssignQr: (Node) -> Unit,
    onExport: (Node) -> Unit,
    onOpen: (Node) -> Unit,
    onRename: (Node) -> Unit,
    onDelete: (Node) -> Unit,
    onPhoto: (Node) -> Unit,
    onViewPhoto: (Node, Int) -> Unit,
    onInfo: (Node) -> Unit,
) {
    val children = store.children(node.id)
    val path = store.path(node.id)
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(node.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        val crumbs = if (path.size > 1) path.dropLast(1).joinToString(" › ") { it.name }
                        else if (node.isTemporary) "Temporary storage" else typeLabel(node.type)
                        Text(
                            crumbs,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    }
                },
                actions = {
                    OverflowMenu(
                        hasPhoto = node.photoPath != null,
                        onRename = { onRename(node) },
                        onDelete = { onDelete(node) },
                        onPhoto = { onPhoto(node) },
                        onInfo = { onInfo(node) },
                        onAssignQr = if (node.hasQr) null else ({ onAssignQr(node) }),
                        onExport = { onExport(node) },
                    )
                },
                colors = barColors(),
            )
        },
        floatingActionButton = {
            var addOpen by remember { mutableStateOf(false) }
            Box {
                ExtendedFloatingActionButton(
                    onClick = { addOpen = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color(0xFF2B1A00),
                )
                DropdownMenu(expanded = addOpen, onDismissRequest = { addOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Add by photo") },
                        onClick = { addOpen = false; onPhotoAdd() },
                    )
                    DropdownMenuItem(
                        text = { Text("Add with QR code + photo") },
                        onClick = { addOpen = false; onScanAdd() },
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                PhotoHeader(
                    photoPaths = node.photoPaths,
                    emptyText = "No photo of this ${typeLabel(node.type).lowercase()} yet",
                    onView = { index -> onViewPhoto(node, index) },
                    onTakePhoto = { onPhoto(node) },
                )
            }
            if (children.isEmpty()) {
                item {
                    Text(
                        "No items yet. Press + to start adding.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp),
                    )
                }
            } else {
                item {
                    val (items, containers) = store.countInside(node.id)
                    Text(
                        "Total inside: ${countText(items, containers)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
                    )
                }
                items(children, key = { it.id }) { child ->
                    val detail = if (child.canHoldChildren) {
                        val (i, c) = store.countInside(child.id)
                        "Container · ${countText(i, c)}"
                    } else "Item"
                    val photos = child.photoPaths.size
                    NodeRow(
                        node = child,
                        detail = if (photos > 1) "$detail · $photos photos" else detail,
                        onClick = { onOpen(child) },
                        onRename = { onRename(child) },
                        onDelete = { onDelete(child) },
                        onPhoto = { onPhoto(child) },
                        onInfo = { onInfo(child) },
                        onAssignQr = { onAssignQr(child) },
                        onExport = { onExport(child) },
                    )
                }
            }
        }
    }
}

private sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val latest: String) : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState
    data class Ready(val info: UpdateInfo, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(store: InventoryStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (installedCode, installedName) = remember { Updater.installedVersion(context) }
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var urlText by remember { mutableStateOf(Updater.updateUrl(context)) }
    var editingUrl by remember { mutableStateOf(false) }
    var permissionNote by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    fun check() {
        state = UpdateState.Checking
        ActivityLog.log("Checking for updates", "Installed $installedName ($installedCode)")
        scope.launch {
            state = try {
                val info = Updater.fetchLatest(context)
                if (info.versionCode > installedCode) {
                    ActivityLog.log("Update available", "${info.versionName} (${info.versionCode})")
                    UpdateState.Available(info)
                } else {
                    ActivityLog.log("Up to date", "Latest published ${info.versionName}")
                    UpdateState.UpToDate(info.versionName)
                }
            } catch (e: Exception) {
                ActivityLog.log("Update check failed", e.message ?: "")
                UpdateState.Failed(e.message ?: "Couldn't check for updates.")
            }
        }
    }

    var logCount by remember { mutableStateOf(ActivityLog.count()) }
    var logBusy by remember { mutableStateOf(false) }
    var logError by remember { mutableStateOf<String?>(null) }

    fun exportLog() {
        logBusy = true
        logError = null
        ActivityLog.log("Activity log export started")
        scope.launch {
            try {
                val sheets = Exporter.logSheets(installedName)
                val file = withContext(Dispatchers.IO) {
                    Exporter.write(context, Exporter.fileName("StorageInventory_log"), sheets)
                }
                ActivityLog.log("Activity log exported", "${sheets.first().rows.size} entries · ${file.name}")
                Exporter.share(context, file, "Storage Inventory activity log")
            } catch (e: Exception) {
                ActivityLog.log("Activity log export failed", e.message ?: "")
                logError = "Couldn't create the log file: ${e.message}"
            }
            logCount = ActivityLog.count()
            logBusy = false
        }
    }

    fun downloadAndInstall(info: UpdateInfo) {
        if (!Updater.ensureInstallPermission(context)) {
            ActivityLog.log("Install permission requested")
            permissionNote = true
            return
        }
        ActivityLog.log("Downloading update", info.versionName)
        permissionNote = false
        state = UpdateState.Downloading(info, 0f)
        scope.launch {
            state = try {
                val file = Updater.download(context, info) { p ->
                    scope.launch { state = UpdateState.Downloading(info, p) }
                }
                Updater.install(context, file)
                ActivityLog.log("Update downloaded, installer opened", info.versionName)
                UpdateState.Ready(info, file)
            } catch (e: Exception) {
                ActivityLog.log("Update download failed", e.message ?: "")
                UpdateState.Failed("Download failed: ${e.message ?: "unknown error"}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                colors = barColors(),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsCard(title = "App updates") {
                Text(
                    "Installed version: $installedName (build $installedCode)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                when (val s = state) {
                    UpdateState.Idle -> {}
                    UpdateState.Checking -> {
                        Text("Checking for updates…", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    is UpdateState.UpToDate -> Text(
                        "You're up to date. Latest published version is ${s.latest}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    is UpdateState.Available -> {
                        Text(
                            "Version ${s.info.versionName} is available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (s.info.notes.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(s.info.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { downloadAndInstall(s.info) }) { Text("Download and install") }
                    }
                    is UpdateState.Downloading -> {
                        Text(
                            "Downloading version ${s.info.versionName}… ${(s.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    }
                    is UpdateState.Ready -> {
                        Text(
                            "Version ${s.info.versionName} downloaded. Android's installer should be open; tap Update there.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { Updater.install(context, s.file) }) { Text("Open installer again") }
                    }
                    is UpdateState.Failed -> Text(
                        s.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (permissionNote) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Android needs your OK first: turn on “Allow from this source” for Storage Inventory " +
                            "in the screen that just opened, then come back and tap Download and install again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val busy = state is UpdateState.Checking || state is UpdateState.Downloading
                if (state !is UpdateState.Available) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { check() }, enabled = !busy) { Text("Check for updates") }
                }
            }

            SettingsCard(title = "Update address") {
                Text(
                    "Where the app looks for new versions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (editingUrl) {
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            Updater.setUpdateUrl(context, urlText)
                            urlText = Updater.updateUrl(context)
                            editingUrl = false
                        }) { Text("Save") }
                        TextButton(onClick = {
                            Updater.setUpdateUrl(context, null)
                            urlText = Updater.updateUrl(context)
                            editingUrl = false
                        }) { Text("Reset to default") }
                    }
                } else {
                    Text(urlText, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { editingUrl = true }) { Text("Change") }
                }
            }

            SettingsCard(title = "Troubleshooting") {
                Text(
                    "Every action in the app is recorded with its date and time ($logCount entries kept on this phone).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { exportLog() }, enabled = !logBusy) {
                    Text(if (logBusy) "Preparing…" else "Export activity log (Excel)")
                }
                logError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            SettingsCard(title = "Inventory") {
                val storages = store.nodes.count { it.type == NodeType.STORAGE }
                val containers = store.nodes.count { it.type == NodeType.CONTAINER }
                val items = store.nodes.count { it.type == NodeType.ITEM }
                Text(
                    "$storages storage${if (storages == 1) "" else "s"} · " +
                        "$containers container${if (containers == 1) "" else "s"} · " +
                        "$items item${if (items == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Saved on this phone only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// Building blocks
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun barColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = Teal,
    titleContentColor = Color.White,
    navigationIconContentColor = Color.White,
    actionIconContentColor = Color.White,
)

@Composable
private fun EmptyState(title: String, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PhotoHeader(
    photoPaths: List<String>,
    emptyText: String,
    onView: (Int) -> Unit,
    onTakePhoto: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    if (photoPaths.isNotEmpty()) {
        if (photoPaths.size == 1) {
            PhotoImage(
                path = photoPaths[0],
                maxPx = 1200,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(shape)
                    .clickable { onView(0) },
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(photoPaths) { i, path ->
                    PhotoImage(
                        path = path,
                        maxPx = 900,
                        modifier = Modifier
                            .width(260.dp)
                            .height(200.dp)
                            .clip(shape)
                            .clickable { onView(i) },
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (photoPaths.size == 1) "1 photo" else "${photoPaths.size} photos · swipe to see all",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            TextButton(onClick = onTakePhoto) { Text("Add photo") }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onTakePhoto) { Text("Take photo") }
            }
        }
    }
}

@Composable
private fun NodeRow(
    node: Node,
    detail: String,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onPhoto: () -> Unit,
    onInfo: () -> Unit,
    onAssignQr: () -> Unit,
    onExport: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                val mainPhoto = node.photoPath
                if (mainPhoto != null) {
                    PhotoImage(
                        path = mainPhoto,
                        maxPx = 200,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    Box(Modifier.align(Alignment.BottomEnd).padding(2.dp)) {
                        TypeBadge(node.type, small = true)
                    }
                } else {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { TypeBadge(node.type) }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    node.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OverflowMenu(
                hasPhoto = node.photoPath != null,
                onRename = onRename,
                onDelete = onDelete,
                onPhoto = onPhoto,
                onInfo = onInfo,
                onAssignQr = if (node.hasQr) null else onAssignQr,
                onExport = if (node.canHoldChildren) onExport else null,
            )
        }
    }
}

@Composable
private fun TypeBadge(type: NodeType, small: Boolean = false) {
    val (letter, bg, fg) = when (type) {
        NodeType.STORAGE -> Triple("S", Teal, Color.White)
        NodeType.CONTAINER -> Triple("C", Color(0xFFF2B35B), Color(0xFF3A2500))
        NodeType.ITEM -> Triple("I", Color(0xFFDDE5E8), Teal)
    }
    val size = if (small) 18.dp else 40.dp
    Surface(shape = RoundedCornerShape(if (small) 5.dp else 10.dp), color = bg, modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                letter,
                color = fg,
                fontWeight = FontWeight.Bold,
                style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun OverflowMenu(
    hasPhoto: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onPhoto: () -> Unit,
    onInfo: () -> Unit,
    onAssignQr: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Info") }, onClick = { open = false; onInfo() })
            if (onAssignQr != null) {
                DropdownMenuItem(text = { Text("Assign QR code") }, onClick = { open = false; onAssignQr() })
            }
            if (onExport != null) {
                DropdownMenuItem(text = { Text("Export to Excel") }, onClick = { open = false; onExport() })
            }
            DropdownMenuItem(
                text = { Text("Add photo") },
                onClick = { open = false; onPhoto() },
            )
            DropdownMenuItem(text = { Text("Rename") }, onClick = { open = false; onRename() })
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { open = false; onDelete() },
            )
        }
    }
}

/**
 * Photos of an entry that is being added: a row of thumbnails (tap to view, ✕ to remove)
 * and a button to take another photo of the same thing.
 */
@Composable
private fun DialogPhotos(
    photoPaths: List<String>,
    onTakePhoto: () -> Unit,
    onRemove: (String) -> Unit,
    onView: (Int) -> Unit,
) {
    if (photoPaths.isEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "No photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onTakePhoto) { Text("Take photo") }
        }
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(photoPaths, key = { _, p -> p }) { i, path ->
            Box {
                PhotoImage(
                    path = path,
                    maxPx = 400,
                    modifier = Modifier
                        .size(if (photoPaths.size == 1) 150.dp else 104.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onView(i) },
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clickable { onRemove(path) },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (photoPaths.size == 1) "1 photo" else "${photoPaths.size} photos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onTakePhoto) { Text("Add another photo") }
    }
}

@Composable
private fun NameDialog(
    title: String,
    message: String?,
    label: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initial: String = "",
    photoPaths: List<String> = emptyList(),
    onTakePhoto: (() -> Unit)? = null,
    onRemovePhoto: (String) -> Unit = {},
    onViewPhoto: (Int) -> Unit = {},
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (onTakePhoto != null) {
                    DialogPhotos(photoPaths, onTakePhoto, onRemovePhoto, onViewPhoto)
                    Spacer(Modifier.height(4.dp))
                }
                if (message != null) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddChildDialog(
    parentName: String,
    qr: String,
    photoPaths: List<String>,
    onTakePhoto: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    onViewPhoto: (Int) -> Unit,
    onConfirm: (NodeType, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var typeName by rememberSaveable { mutableStateOf<String?>(null) }
    val type = typeName?.let { NodeType.valueOf(it) }
    var name by rememberSaveable { mutableStateOf("") }
    val options = listOf(
        NodeType.ITEM to "Item",
        NodeType.CONTAINER to "Smaller container",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to “$parentName”") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DialogPhotos(photoPaths, onTakePhoto, onRemovePhoto, onViewPhoto)
                Text(
                    if (qr.isBlank()) "No QR code (added by photo)" else "QR code: $qr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Text("What is this?", style = MaterialTheme.typography.titleSmall)
                Column(Modifier.selectableGroup()) {
                    options.forEach { (value, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = type == value,
                                    onClick = { typeName = value.name },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = type == value, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    if (value == NodeType.ITEM) "A single thing you're storing"
                                    else "A box or bin that holds more items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (type == NodeType.CONTAINER) "Container name" else "Item name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { type?.let { onConfirm(it, name) } },
                enabled = type != null && name.isNotBlank(),
            ) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ItemDialog(
    item: Node,
    location: String,
    onPhoto: () -> Unit,
    onViewPhoto: (Int) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val photos = item.photoPaths
                if (photos.isNotEmpty()) {
                    if (photos.size == 1) {
                        PhotoImage(
                            path = photos[0],
                            maxPx = 1000,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onViewPhoto(0) },
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(photos) { i, path ->
                                PhotoImage(
                                    path = path,
                                    maxPx = 800,
                                    modifier = Modifier
                                        .width(200.dp)
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onViewPhoto(i) },
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (photos.size == 1) "1 photo" else "${photos.size} photos · swipe to see all",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onPhoto) { Text("Add photo") }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "No photo yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onPhoto) { Text("Take photo") }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Stored in: $location", style = MaterialTheme.typography.bodyMedium)
                Row {
                    TextButton(onClick = onInfo) { Text("Info") }
                    TextButton(onClick = onRename) { Text("Rename") }
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private val addedFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)

/** Traceability details: when an entry was added, where it is and its QR code. */
@Composable
private fun InfoDialog(store: InventoryStore, node: Node, onDismiss: () -> Unit) {
    val location = store.path(node.id).dropLast(1).joinToString(" › ") { it.name }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Info", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow("Name", node.name)
                InfoRow("Type", if (node.isTemporary) "Temporary storage" else typeLabel(node.type))
                InfoRow(
                    "Added",
                    if (node.createdAt > 0) addedFormat.format(Date(node.createdAt)) else "Unknown",
                )
                InfoRow("Stored in", location.ifEmpty { "Top level" })
                InfoRow("Photos", node.photoPaths.size.toString())
                if (node.canHoldChildren) {
                    val (items, containers) = store.countInside(node.id)
                    InfoRow("Contains", countText(items, containers))
                }
                InfoRow(
                    "QR code",
                    if (node.hasQr) node.qr else "None yet (use “Assign QR code” in the ⋮ menu)",
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Full-screen photos: swipe left/right between them; optional delete for saved entries. */
@Composable
private fun PhotoViewer(
    paths: List<String>,
    startIndex: Int,
    onDelete: ((String) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val pager = rememberPagerState(initialPage = startIndex) { paths.size }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { paths.getOrElse(it) { "" } }) { page ->
                PhotoImage(
                    path = paths[page],
                    maxPx = 2000,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onDismiss),
                    contentScale = ContentScale.Fit,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = paths.getOrNull(pager.currentPage) }) {
                        Text("Delete", color = Color(0xFFFFB4AB))
                    }
                } else Spacer(Modifier.width(72.dp))
                Text(
                    if (paths.size > 1) "${pager.currentPage + 1} / ${paths.size} · swipe" else "Tap to close",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) { Text("Close", color = Color.White) }
            }
        }
    }
    confirmDelete?.let { path ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this photo?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete?.invoke(path); confirmDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

private fun typeLabel(type: NodeType) = when (type) {
    NodeType.STORAGE -> "Storage"
    NodeType.CONTAINER -> "Container"
    NodeType.ITEM -> "Item"
}

private fun countText(items: Int, containers: Int): String {
    val i = if (items == 1) "1 item" else "$items items"
    return if (containers == 0) i else {
        val c = if (containers == 1) "1 container" else "$containers containers"
        "$i · $c"
    }
}

// ---------------------------------------------------------------------------
// Search (text, photo similarity, QR) and target picker
// ---------------------------------------------------------------------------

/** "Add to which storage?" list for items added by photo. */
@Composable
private fun ChooseTargetDialog(
    store: InventoryStore,
    photoPath: String,
    onPick: (Node) -> Unit,
    onScan: () -> Unit,
    onDismiss: () -> Unit,
) {
    val targets = store.nodes
        .filter { it.canHoldChildren }
        .map { node -> node to store.path(node.id).joinToString(" › ") { it.name } }
        .sortedBy { it.second.lowercase() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to which storage?") },
        text = {
            Column {
                PhotoImage(
                    path = photoPath,
                    maxPx = 600,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    targets.forEach { (node, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(node) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TypeBadge(node.type, small = true)
                            Spacer(Modifier.width(10.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                TextButton(onClick = onScan) { Text("Scan its QR code instead") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    store: InventoryStore,
    photoPath: String?,
    onTakePhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onScanQr: () -> Unit,
    onOpen: (Node) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember(photoPath) { mutableStateOf<List<Pair<Node, Float>>?>(null) }
    var progress by remember(photoPath) { mutableStateOf(0 to 0) }
    var failure by remember(photoPath) { mutableStateOf<String?>(null) }

    BackHandler { onBack() }

    LaunchedEffect(photoPath) {
        if (photoPath != null) {
            ActivityLog.log("Searched by photo", "Started")
            try {
                val found = PhotoSimilarity.findSimilar(context, photoPath, store.nodes.toList()) { done, total ->
                    progress = done to total
                }
                results = found
                ActivityLog.log(
                    "Photo search results",
                    "${found.size} match(es)" + (found.firstOrNull()?.let { " · best: “${it.first.name}” ${(it.second * 100).toInt()}%" } ?: "")
                )
            } catch (e: Exception) {
                failure = e.message ?: "Photo search failed."
                ActivityLog.log("Photo search failed", e.message ?: "")
            }
        }
    }

    val q = query.trim()
    LaunchedEffect(q) {
        if (q.isNotEmpty()) {
            delay(1500)
            val n = store.nodes.count { it.name.contains(q, ignoreCase = true) || (it.hasQr && it.qr.contains(q, ignoreCase = true)) }
            ActivityLog.log("Searched by name", "“$q” · $n result(s)")
        }
    }
    val textMatches = if (q.isEmpty()) emptyList() else store.nodes
        .filter { it.name.contains(q, ignoreCase = true) || (it.hasQr && it.qr.contains(q, ignoreCase = true)) }
        .sortedBy { it.name.lowercase() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Search", fontWeight = FontWeight.SemiBold) },
                colors = barColors(),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search by name") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onTakePhoto, modifier = Modifier.weight(1f)) { Text("Search by photo") }
                    OutlinedButton(onClick = onScanQr, modifier = Modifier.weight(1f)) { Text("Scan QR code") }
                }
            }
            if (q.isNotEmpty()) {
                item {
                    Text(
                        "${textMatches.size} result${if (textMatches.size == 1) "" else "s"} for “$q”",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(textMatches, key = { "t" + it.id }) { node ->
                    SearchResultRow(store, node, null) {
                        ActivityLog.log("Opened search result", "“${node.name}” (name search)")
                        onOpen(node)
                    }
                }
            } else if (photoPath != null) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PhotoImage(
                            path = photoPath,
                            maxPx = 300,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Your photo", style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = onClearPhoto) { Text("Clear") }
                        }
                    }
                }
                val found = results
                val problem = failure
                when {
                    problem != null -> item {
                        Text(problem, color = MaterialTheme.colorScheme.error)
                    }
                    found == null -> item {
                        Column {
                            Text(
                                if (progress.second > 0)
                                    "Comparing with your stored photos… ${progress.first}/${progress.second}"
                                else "Analysing your photo…",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                    found.isEmpty() -> item {
                        Text(
                            "No similar items found. Try a closer photo with the object filling most of the frame.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        item {
                            Text(
                                "Most similar",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(found, key = { "p" + it.first.id }) { (node, score) ->
                            SearchResultRow(store, node, score) {
                                ActivityLog.log("Opened search result", "“${node.name}” (photo search, ${(score * 100).toInt()}%)")
                                onOpen(node)
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "Type a name, take a photo to find similar-looking things you've stored, or scan a QR code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(store: InventoryStore, node: Node, score: Float?, onClick: () -> Unit) {
    val location = store.path(node.id).dropLast(1).joinToString(" › ") { it.name }.ifEmpty { "Top level" }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val photo = node.photoPath
            if (photo != null) {
                PhotoImage(
                    path = photo,
                    maxPx = 200,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { TypeBadge(node.type) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(node.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${if (node.isTemporary) "Temporary storage" else typeLabel(node.type)} · $location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (score != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "${(score * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun photoPurpose(target: String): String = when (target.substringBefore(SEP)) {
    "S" -> "New storage"
    "C" -> "New item/container"
    "N" -> "Add photo to entry"
    "P" -> "Item added by photo"
    "T" -> "Temporary storage"
    "Q" -> "Search by photo"
    else -> target
}
