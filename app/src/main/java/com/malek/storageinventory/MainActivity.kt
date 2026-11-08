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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.FabPosition
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
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

// Premium dark palette
private val Bg = Color(0xFF0D0E10)
private val Surface1 = Color(0xFF17191C)
private val Surface2 = Color(0xFF202327)
private val Surface3 = Color(0xFF2A2E33)
private val TextHi = Color(0xFFEDEBE7)
private val TextMuted = Color(0xFF9A9DA3)
private val TextFaint = Color(0xFF6A6E75)
private val Brass = Color(0xFFC9A35D)
private val BrassHi = Color(0xFFE2C27F)
private val BrassInk = Color(0xFF1A1407)
private val Danger = Color(0xFFE8735E)

/** Geist (SIL Open Font License) by Vercel. */
private val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold),
)

private val AppTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = Geist),
        displayMedium = t.displayMedium.copy(fontFamily = Geist),
        displaySmall = t.displaySmall.copy(fontFamily = Geist),
        headlineLarge = t.headlineLarge.copy(fontFamily = Geist, letterSpacing = (-0.8).sp),
        headlineMedium = t.headlineMedium.copy(fontFamily = Geist, letterSpacing = (-0.7).sp),
        headlineSmall = t.headlineSmall.copy(fontFamily = Geist, letterSpacing = (-0.5).sp),
        titleLarge = t.titleLarge.copy(fontFamily = Geist, letterSpacing = (-0.4).sp),
        titleMedium = t.titleMedium.copy(fontFamily = Geist, letterSpacing = (-0.15).sp),
        titleSmall = t.titleSmall.copy(fontFamily = Geist),
        bodyLarge = t.bodyLarge.copy(fontFamily = Geist),
        bodyMedium = t.bodyMedium.copy(fontFamily = Geist),
        bodySmall = t.bodySmall.copy(fontFamily = Geist),
        labelLarge = t.labelLarge.copy(fontFamily = Geist),
        labelMedium = t.labelMedium.copy(fontFamily = Geist),
        labelSmall = t.labelSmall.copy(fontFamily = Geist),
    )
}

@Composable
fun InventoryTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Brass,
        onPrimary = BrassInk,
        primaryContainer = Surface3,
        onPrimaryContainer = TextHi,
        secondary = Brass,
        onSecondary = BrassInk,
        background = Bg,
        onBackground = TextHi,
        surface = Surface1,
        onSurface = TextHi,
        surfaceVariant = Surface2,
        onSurfaceVariant = TextMuted,
        surfaceTint = Color.Transparent,
        surfaceContainerLowest = Surface1,
        surfaceContainerLow = Surface1,
        surfaceContainer = Surface1,
        surfaceContainerHigh = Surface1,
        surfaceContainerHighest = Surface2,
        outline = TextFaint,
        outlineVariant = Color(0x14FFFFFF),
        error = Danger,
        onError = Color(0xFF1B0B07),
    )
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
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    fun endSelect() { selecting = false; selected.clear() }
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
    var optionsFor by remember { mutableStateOf<String?>(null) }
    var showNewBin by remember { mutableStateOf(false) }
    var addItemFor by remember { mutableStateOf<String?>(null) }

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
        // Archived bins can't be opened: show the entry's details instead.
        if (store.isArchived(node.id)) {
            stack.clear()
            if (node.canHoldChildren) infoFor = node.id else viewingItem = node.id
            ActivityLog.log("Viewed archived entry", "“${node.name}”")
            return
        }
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

    BackHandler(enabled = selecting && stack.isEmpty() && scanMode == null && !showSettings && !showSearch) { endSelect() }
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
                archivedView = showArchived,
                selecting = selecting,
                selected = selected,
                onNewBin = { showNewBin = true },
                onFind = { endSelect(); showSearch = true },
                onShowActive = { endSelect(); showArchived = false; ActivityLog.log("Opened screen", "Active storage") },
                onShowArchived = { endSelect(); showArchived = true; ActivityLog.log("Opened screen", "Archived storage") },
                onSettings = { endSelect(); showSettings = true },
                onToggleSelect = { node ->
                    if (!selecting) ActivityLog.log("Started selecting", "“${node.name}”")
                    selecting = true
                    if (node.id in selected) selected.remove(node.id) else selected.add(node.id)
                    if (selected.isEmpty()) endSelect()
                },
                onStartSelect = { selecting = true },
                onCancelSelect = { endSelect() },
                onSelectAll = { ids -> ids.forEach { if (it !in selected) selected.add(it) } },
                onClearSelection = { selected.clear() },
                onArchiveSelected = { ids, toArchive ->
                    ids.forEach { store.setArchived(it, toArchive) }
                    val what = if (ids.size == 1) "“${store.get(ids[0])?.name}”" else "${ids.size} bins"
                    message = if (toArchive) "$what moved to Archived storage (☰ › Archived storage)." else "$what back in Active storage."
                    endSelect()
                },
                onOpen = { stack.add(it.id) },
                onOptions = { optionsFor = it.id },
                onDelete = { deleting = it },
            )
            else -> DetailScreen(
                store = store,
                node = current,
                onBack = { stack.removeAt(stack.lastIndex) },
                onAddItem = { addItemFor = current.id },
                onOpen = { child ->
                    if (child.canHoldChildren) stack.add(child.id) else viewingItem = child.id
                },
                onOptions = { optionsFor = it.id },
                onDelete = { deleting = it },
                onPhoto = { takePhoto("N$SEP${it.id}") },
                onViewPhoto = { node, index -> viewer = ViewerState(node.photoPaths, index, node.id) },
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
                            store.isArchived(existing.id) -> message = "“${existing.name}” is in an archived bin. Unarchive it first."
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
            title = "Name this bin",
            message = "QR code: ${pendingStorage.qr}",
            label = "Bin name",
            confirmText = "Done",
            initial = remember(pendingStorage.qr) { nextBinName(store) },
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
            title = "New temporary bin",
            message = "No QR code needed. Attach one later: swipe the bin left › More › Assign QR code.",
            label = "Bin name",
            confirmText = "Create",
            initial = remember { nextBinName(store) },
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
                archivedIn = if (store.isArchived(item.id)) store.path(item.id).first().name else null,
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

    // Action sheets (replace the old three-dot menus)
    if (showNewBin) {
        ActionSheet(
            title = "New Bin",
            subtitle = null,
            actions = listOf(
                SheetAction(R.drawable.ic_qr, "Scan bin QR code", "Then photograph the bin and name it") { scanMode = ScanMode.NewStorage },
                SheetAction(R.drawable.ic_clock, "Temporary bin", "No QR code yet, attach one later") { creatingTemp = true },
                SheetAction(
                    R.drawable.ic_camera, "Add item to an existing bin", "Take a photo, then pick the bin",
                    enabled = store.nodes.any { it.canHoldChildren && !store.isArchived(it.id) },
                ) { photoItemTarget = null; takePhoto("P") },
            ),
            onDismiss = { showNewBin = false },
        )
    }
    addItemFor?.let { id ->
        val parent = store.get(id)
        if (parent == null) { addItemFor = null; return@let }
        ActionSheet(
            title = "Add item",
            subtitle = "To “${parent.name}”",
            actions = listOf(
                SheetAction(R.drawable.ic_camera, "Add by photo", "Photos only, no QR label") {
                    photoItemTarget = parent.id; takePhoto("P")
                },
                SheetAction(R.drawable.ic_qr, "Add with QR code + photo", "Scan the label, then photograph it") {
                    scanMode = ScanMode.AddTo(parent.id)
                },
            ),
            onDismiss = { addItemFor = null },
        )
    }
    optionsFor?.let { id ->
        val node = store.get(id)
        LaunchedEffect(id) { node?.let { ActivityLog.log("Opened options", "“${it.name}”") } }
        if (node == null) { optionsFor = null; return@let }
        run {
            val where = store.path(node.id).dropLast(1).joinToString(" › ") { it.name }
            ActionSheet(
                title = node.name,
                subtitle = (if (node.isTemporary) "Temporary storage" else typeLabel(node.type)) +
                    (if (node.archived) " · Archived" else "") + (if (where.isNotEmpty()) " · $where" else ""),
                actions = listOfNotNull(
                    SheetAction(R.drawable.ic_info, "Info", "Date added, location, QR code") { infoFor = node.id },
                    if (!node.archived) SheetAction(R.drawable.ic_camera, "Add photo") { takePhoto("N$SEP${node.id}") } else null,
                    if (!node.hasQr && !node.archived) SheetAction(R.drawable.ic_qr, "Assign QR code") { scanMode = ScanMode.AssignQr(node.id) } else null,
                    if (node.canHoldChildren) SheetAction(R.drawable.ic_table, "Export to Excel") { exportStorage(node) } else null,
                    SheetAction(R.drawable.ic_pencil, "Rename") { renaming = node },
                    SheetAction(R.drawable.ic_trash, "Delete", danger = true) { deleting = node },
                ),
                onDismiss = { optionsFor = null },
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
    archivedView: Boolean,
    selecting: Boolean,
    selected: List<String>,
    onNewBin: () -> Unit,
    onFind: () -> Unit,
    onShowActive: () -> Unit,
    onShowArchived: () -> Unit,
    onSettings: () -> Unit,
    onToggleSelect: (Node) -> Unit,
    onStartSelect: () -> Unit,
    onCancelSelect: () -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onClearSelection: () -> Unit,
    onArchiveSelected: (List<String>, Boolean) -> Unit,
    onOpen: (Node) -> Unit,
    onOptions: (Node) -> Unit,
    onDelete: (Node) -> Unit,
) {
    val storages = store.storages(archivedView)
    val chosen = selected.filter { id -> storages.any { it.id == id } }
    val selectMode = selecting && storages.isNotEmpty()
    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (selectMode) IconButton(onClick = onCancelSelect) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                    }
                },
                title = {
                    Text(
                        when {
                            selectMode -> if (chosen.isEmpty()) "Select bins" else "${chosen.size} selected"
                            archivedView -> "Archived storage"
                            else -> "Storage Inventory"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = barColors(),
                actions = {
                    if (store.nodes.isNotEmpty()) RoundIconButton(Icons.Filled.Search, "Search", onFind)
                    MainMenu(
                        archivedView = archivedView,
                        activeCount = store.storages(false).size,
                        archivedCount = store.storages(true).size,
                        selecting = selectMode,
                        selectedCount = chosen.size,
                        allSelected = chosen.size == storages.size,
                        canSelect = storages.isNotEmpty(),
                        onArchiveSelected = { onArchiveSelected(chosen, !archivedView) },
                        onStartSelect = onStartSelect,
                        onSelectAll = { onSelectAll(storages.map { it.id }) },
                        onClearSelection = onClearSelection,
                        onCancelSelect = onCancelSelect,
                        onShowActive = onShowActive,
                        onShowArchived = onShowArchived,
                        onSettings = onSettings,
                    )
                    Spacer(Modifier.width(8.dp))
                },
            )
        },
        floatingActionButton = { if (!archivedView && !selectMode) BrassButton("New Bin", onNewBin) },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        if (storages.isEmpty()) {
            when {
                archivedView -> EmptyState(
                    title = "No archived bins",
                    hint = "To archive bins: in Active storage tick their circles, then ☰ › Archive.",
                    modifier = Modifier.padding(padding),
                )
                store.storages().isNotEmpty() -> EmptyState(
                    title = "No active bins",
                    hint = "Tap “New Bin” to add one. Archived bins are under ☰ › Archived storage.",
                    modifier = Modifier.padding(padding),
                )
                else -> EmptyState(
                    title = "No Storage Data Available",
                    hint = "Tap “New Bin” to add your first one.",
                    modifier = Modifier.padding(padding),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + if (archivedView || selectMode) 32.dp else 110.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    val count = if (storages.size == 1) "1 bin" else "${storages.size} bins"
                    SectionLabel(if (archivedView) "Archived · $count" else count)
                }
                items(storages, key = { it.id }) { storage ->
                    val (items, containers) = store.countInside(storage.id)
                    NodeRow(
                        node = storage,
                        detail = countText(items, containers),
                        tag = if (storage.isTemporary) "Temporary" else null,
                        selectState = storage.id in chosen,
                        onSelect = { onToggleSelect(storage) },
                        swipeEnabled = !selectMode,
                        onClick = { if (selectMode || archivedView) onToggleSelect(storage) else onOpen(storage) },
                        onLongClick = { onToggleSelect(storage) },
                        onMore = { onOptions(storage) },
                        onDelete = { onDelete(storage) },
                    )
                }
                item {
                    SwipeHint(
                        when {
                            archivedView -> "Tick bins, then ☰ › Unarchive. Archived bins can’t be opened."
                            selectMode -> "Tick the bins to archive, then ☰ › Archive"
                            else -> "Tick a bin’s circle to select it · swipe left for options"
                        }
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
    onAddItem: () -> Unit,
    onOpen: (Node) -> Unit,
    onOptions: (Node) -> Unit,
    onDelete: (Node) -> Unit,
    onPhoto: (Node) -> Unit,
    onViewPhoto: (Node, Int) -> Unit,
) {
    val children = store.children(node.id)
    val path = store.path(node.id)
    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            node.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val crumbs = if (path.size > 1) path.dropLast(1).joinToString(" › ") { it.name }
                        else if (node.archived) "Archived storage"
                        else if (node.isTemporary) "Temporary storage" else typeLabel(node.type)
                        Text(
                            crumbs,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextMuted,
                        )
                    }
                },
                actions = {
                    RoundIconButton(painterResource(R.drawable.ic_dots), "Options") { onOptions(node) }
                    Spacer(Modifier.width(8.dp))
                },
                colors = barColors(),
            )
        },
        floatingActionButton = { BrassButton("Add item", onAddItem) },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 110.dp,
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
                        "No items yet. Tap “Add item” to start adding.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp),
                    )
                }
            } else {
                item {
                    val (items, containers) = store.countInside(node.id)
                    SectionLabel("Inside · ${countText(items, containers)}")
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
                        onMore = { onOptions(child) },
                        onDelete = { onDelete(child) },
                    )
                }
                item { SwipeHint() }
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
    containerColor = Bg,
    scrolledContainerColor = Bg,
    titleContentColor = TextHi,
    navigationIconContentColor = TextHi,
    actionIconContentColor = TextHi,
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
        BoxPlaceholder(72.dp)
        Spacer(Modifier.height(18.dp))
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
    tag: String? = null,
    selectState: Boolean? = null,
    onSelect: () -> Unit = {},
    swipeEnabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onMore: () -> Unit,
    onDelete: () -> Unit,
) {
    SwipeRow(
        onClick = onClick,
        onLongClick = onLongClick,
        swipeEnabled = swipeEnabled,
        selected = selectState == true,
        onMore = onMore,
        onDelete = onDelete,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (selectState == true) Surface2 else Surface1)
                .padding(start = if (selectState != null) 4.dp else 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectState != null) {
                // Tick circle: selects the whole bin (tapping the rest of the row opens it).
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSelect),
                    contentAlignment = Alignment.Center,
                ) { CheckCircle(selectState) }
                Spacer(Modifier.width(4.dp))
            }
            Thumb(node, 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    node.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tag != null) {
                        Tag(tag)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(painterResource(R.drawable.ic_chevron), contentDescription = null, tint = TextFaint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CheckCircle(on: Boolean) {
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (on) Brass else Color.Transparent)
            .border(1.6.dp, if (on) Brass else TextFaint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Icon(painterResource(R.drawable.ic_check), contentDescription = "Selected", tint = BrassInk, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun ArchivedNote(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brass.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(painterResource(R.drawable.ic_archive), contentDescription = null, tint = BrassHi, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "$text To open it: ☰ › Archived storage, select it, ☰ › Unarchive.",
            color = BrassHi,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** Row that slides left to reveal More and Delete. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeRow(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    swipeEnabled: Boolean = true,
    selected: Boolean = false,
    onMore: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val reveal = with(LocalDensity.current) { 168.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    fun close() { scope.launch { offset.animateTo(0f) } }
    LaunchedEffect(swipeEnabled) { if (!swipeEnabled) offset.snapTo(0f) }
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface3)
            .then(if (selected) Modifier.border(1.5.dp, Brass.copy(alpha = 0.55f), RoundedCornerShape(18.dp)) else Modifier)
    ) {
        Row(Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
            SwipeAction(R.drawable.ic_dots, "More", Surface3, TextHi) { close(); onMore() }
            SwipeAction(R.drawable.ic_trash, "Delete", Danger, Color(0xFF1B0B07)) { close(); onDelete() }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    enabled = swipeEnabled,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(-reveal - 60f, 0f)) }
                    },
                    onDragStopped = { velocity ->
                        offset.animateTo(if (offset.value < -reveal / 2 || velocity < -1500f) -reveal else 0f)
                    },
                )
                .combinedClickable(
                    onClick = { if (offset.value != 0f) close() else onClick() },
                    onLongClick = onLongClick?.let { lc ->
                        {
                            if (offset.value != 0f) close()
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            lc()
                        }
                    },
                )
        ) { content() }
    }
}

@Composable
private fun RowScope.SwipeAction(icon: Int, label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .width(84.dp)
            .fillMaxHeight()
            .background(bg)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Thumb(node: Node, size: Dp) {
    val photo = node.photoPath
    if (photo != null) {
        PhotoImage(path = photo, maxPx = 200, modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp)))
    } else {
        BoxPlaceholder(size)
    }
}

/** Neutral box icon for entries without a photo (replaces the old S/C/I letter badges). */
@Composable
private fun BoxPlaceholder(size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(if (size < 40.dp) 8.dp else 12.dp))
            .background(Surface2),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_box), contentDescription = null, tint = TextFaint, modifier = Modifier.size(size * 0.46f))
    }
}

/** Kept for the pickers/search lists; now shows the neutral placeholder. */
@Composable
private fun TypeBadge(@Suppress("UNUSED_PARAMETER") type: NodeType, small: Boolean = false) {
    BoxPlaceholder(if (small) 32.dp else 48.dp)
}

@Composable
private fun Tag(text: String) {
    Text(
        text.uppercase(),
        color = Brass,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Brass.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = TextMuted,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun SwipeHint(text: String = "Swipe left on a row for options") {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = TextFaint,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Surface1, contentColor = TextHi),
    ) { Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun RoundIconButton(icon: Painter, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Surface1, contentColor = TextHi),
    ) { Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp)) }
}

private val Green = Color(0xFF5BC27A)

/** ☰ menu on the home screen: switch between Active / Archived storage, or open Settings. */
@Composable
private fun MainMenu(
    archivedView: Boolean,
    activeCount: Int,
    archivedCount: Int,
    selecting: Boolean,
    selectedCount: Int,
    allSelected: Boolean,
    canSelect: Boolean,
    onArchiveSelected: () -> Unit,
    onStartSelect: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCancelSelect: () -> Unit,
    onShowActive: () -> Unit,
    onShowArchived: () -> Unit,
    onSettings: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundIconButton(painterResource(R.drawable.ic_menu), "Menu") { open = true }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = Surface2,
            shape = RoundedCornerShape(16.dp),
        ) {
            if (selecting) {
                val bins = if (selectedCount == 1) "1 bin" else "$selectedCount bins"
                MainMenuItem(
                    if (archivedView) R.drawable.ic_unarchive else R.drawable.ic_archive,
                    if (archivedView) (if (selectedCount > 0) "Unarchive $bins" else "Unarchive")
                    else (if (selectedCount > 0) "Archive $bins" else "Archive"),
                    null, current = null, enabled = selectedCount > 0,
                ) { open = false; onArchiveSelected() }
                if (allSelected) MainMenuItem(R.drawable.ic_select, "Clear selection", null, current = null) { open = false; onClearSelection() }
                else MainMenuItem(R.drawable.ic_select, "Select all", null, current = null) { open = false; onSelectAll() }
                MainMenuItem(R.drawable.ic_close, "Cancel selection", null, current = null) { open = false; onCancelSelect() }
            } else {
                MainMenuItem(R.drawable.ic_select, "Select bins", null, current = null, enabled = canSelect) { open = false; onStartSelect() }
            }
            HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(vertical = 4.dp))
            MainMenuItem(R.drawable.ic_box, "Active storage", activeCount, current = !archivedView) { open = false; onShowActive() }
            MainMenuItem(R.drawable.ic_archive, "Archived storage", archivedCount, current = archivedView) { open = false; onShowArchived() }
            HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(vertical = 4.dp))
            MainMenuItem(R.drawable.ic_settings, "Settings", null, current = false) { open = false; onSettings() }
        }
    }
}

@Composable
private fun MainMenuItem(icon: Int, label: String, count: Int?, current: Boolean?, enabled: Boolean = true, onClick: () -> Unit) {
    DropdownMenuItem(
        enabled = enabled,
        modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
        leadingIcon = { Icon(painterResource(icon), contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp)) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(min = 170.dp)) {
                Text(label, color = TextHi, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (count != null) Text("$count", color = TextFaint, style = MaterialTheme.typography.bodyMedium)
                if (current != null) {
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (current) Green else Color.Transparent)
                    )
                }
            }
        },
        onClick = onClick,
    )
}

/** Brass call-to-action pill ("New Bin", "Add item"). */
@Composable
private fun BrassButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .shadow(14.dp, shape, ambientColor = Brass, spotColor = Brass)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(BrassHi, Brass)))
            .clickable(onClick = onClick)
            .height(56.dp)
            .padding(start = 20.dp, end = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = BrassInk)
            Spacer(Modifier.width(8.dp))
            Text(text, color = BrassInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private class SheetAction(
    val icon: Int,
    val label: String,
    val note: String? = null,
    val danger: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/** Bottom sheet listing actions (replaces the three-dot menus). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionSheet(title: String, subtitle: String?, actions: List<SheetAction>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1,
        contentColor = TextHi,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .size(width = 38.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Surface3)
            )
        },
    ) {
        Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 18.dp)) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            actions.forEach { a ->
                val color = if (a.danger) Danger else TextHi
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = a.enabled) { onDismiss(); a.onClick() }
                        .padding(horizontal = 12.dp, vertical = 13.dp)
                        .alpha(if (a.enabled) 1f else 0.35f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(a.icon), contentDescription = null, tint = if (a.danger) Danger else TextMuted, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(a.label, color = color, style = MaterialTheme.typography.bodyLarge)
                        if (a.note != null) Text(a.note, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
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
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length)))
    }
    val name = field.text
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
                    value = field,
                    onValueChange = { field = it },
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
    archivedIn: String?,
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
                if (archivedIn != null) ArchivedNote("In archived bin “$archivedIn”.")
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
                        if (archivedIn == null) TextButton(onClick = onPhoto) { Text("Add photo") }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "No photo yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (archivedIn == null) TextButton(onClick = onPhoto) { Text("Take photo") }
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
                if (store.isArchived(node.id)) {
                    val root = store.path(node.id).first()
                    ArchivedNote(if (root.id == node.id) "This bin is archived." else "In archived bin “${root.name}”.")
                }
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
                    if (node.hasQr) node.qr else "None yet (swipe left › More › Assign QR code)",
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
        .filter { it.canHoldChildren && !store.isArchived(it.id) }
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
    val fullPath = store.path(node.id)
    val location = fullPath.dropLast(1).joinToString(" › ") { it.name }.ifEmpty { "Top level" } +
        (if (fullPath.firstOrNull()?.archived == true) " · Archived" else "")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
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

/** Next default name for a new bin: "Bin 001", "Bin 002", … */
private fun nextBinName(store: InventoryStore): String {
    val re = Regex("""^bin\s*0*(\d+)$""", RegexOption.IGNORE_CASE)
    val max = store.storages().mapNotNull { re.find(it.name.trim())?.groupValues?.get(1)?.toIntOrNull() }.maxOrNull() ?: 0
    return "Bin %03d".format(max + 1)
}
