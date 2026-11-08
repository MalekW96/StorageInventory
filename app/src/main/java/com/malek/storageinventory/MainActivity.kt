package com.malek.storageinventory

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
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
    MaterialTheme(colorScheme = scheme, content = content)
}

// ---------------------------------------------------------------------------
// App flow
// ---------------------------------------------------------------------------

/** What a scan is for. */
private sealed interface ScanMode {
    data object NewStorage : ScanMode
    data class AddTo(val parentId: String) : ScanMode
    data object Find : ScanMode
}

/** A scanned code waiting to be named (and maybe photographed) before it's saved. */
private data class PendingNew(val qr: String, val parentId: String?, val photo: String?)

// Simple string encoding so pending state survives Android closing the app while
// the camera app is open.
private const val SEP = "\u001F"
private fun PendingNew.encode() = listOf(qr, parentId ?: "", photo ?: "").joinToString(SEP)
private fun decodePending(s: String?): PendingNew? = s?.split(SEP)?.takeIf { it.size == 3 }?.let {
    PendingNew(it[0], it[1].ifEmpty { null }, it[2].ifEmpty { null })
}

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
    var scanMode by remember { mutableStateOf<ScanMode?>(null) }

    // Dialog state
    var pendingStorageRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingChildRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var askStartAddingFor by rememberSaveable { mutableStateOf<String?>(null) }
    var askAnother by remember { mutableStateOf<Pair<String, String>?>(null) }  // parentId to added name
    var duplicate by remember { mutableStateOf<Node?>(null) }
    var notFoundQr by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<Node?>(null) }
    var deleting by remember { mutableStateOf<Node?>(null) }
    var viewingItem by remember { mutableStateOf<String?>(null) }
    var viewingPhoto by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    // --- Photos -------------------------------------------------------------
    // captureTarget says what the photo being taken is for:
    //   "S<sep>qr"           new storage, "C<sep>parent<sep>qr"  new item/container,
    //   "N<sep>nodeId"       replace the photo of an existing entry
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
                val photo = if (path != null) {
                    old?.photo?.let { File(it).delete() }
                    path
                } else old?.photo
                val encoded = PendingNew(qr, parentId, photo).encode()
                if (isStorage) pendingStorageRaw = encoded else pendingChildRaw = encoded
            }
            "N" -> if (path != null) store.setPhoto(parts[1], path)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val target = captureTarget ?: return@rememberLauncherForActivityResult
        captureTarget = null
        scope.launch {
            val path = if (ok) withContext(Dispatchers.IO) { PhotoStore.finishCapture(context) } else null
            onPhotoResult(target, path)
        }
    }

    fun takePhoto(target: String) {
        captureTarget = target
        try {
            cameraLauncher.launch(PhotoStore.captureUri(context))
        } catch (e: Exception) {
            captureTarget = null
            onPhotoResult(target, null)
            message = "Couldn't open the camera app. You can add a photo later from the item's menu."
        }
    }

    fun discardPending(raw: String?) {
        decodePending(raw)?.photo?.let { File(it).delete() }
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

    BackHandler(enabled = stack.isNotEmpty() && scanMode == null && !showSettings) {
        stack.removeAt(stack.lastIndex)
    }

    Box(Modifier.fillMaxSize()) {
        val current = currentId?.let { store.get(it) }
        when {
            showSettings -> SettingsScreen(store = store, onBack = { showSettings = false })
            current == null -> HomeScreen(
                store = store,
                onAdd = { scanMode = ScanMode.NewStorage },
                onFind = { scanMode = ScanMode.Find },
                onSettings = { showSettings = true },
                onOpen = { stack.add(it.id) },
                onRename = { renaming = it },
                onDelete = { deleting = it },
                onPhoto = { takePhoto("N$SEP${it.id}") },
            )
            else -> DetailScreen(
                store = store,
                node = current,
                onBack = { stack.removeAt(stack.lastIndex) },
                onScanAdd = { scanMode = ScanMode.AddTo(current.id) },
                onOpen = { child ->
                    if (child.canHoldChildren) stack.add(child.id) else viewingItem = child.id
                },
                onRename = { renaming = it },
                onDelete = { deleting = it },
                onPhoto = { takePhoto("N$SEP${it.id}") },
                onViewPhoto = { viewingPhoto = it },
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
            }
            QrScannerScreen(
                title = title,
                subtitle = subtitle,
                onCancel = { scanMode = null },
                onScanned = { qr ->
                    scanMode = null
                    val existing = store.findByQr(qr)
                    when (mode) {
                        ScanMode.Find -> if (existing != null) openNode(existing) else notFoundQr = qr
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
            photoPath = pendingStorage.photo,
            onTakePhoto = { takePhoto("S$SEP${pendingStorage.qr}") },
            onConfirm = { name ->
                val storage = store.add(pendingStorage.qr, name, NodeType.STORAGE, null, pendingStorage.photo)
                pendingStorageRaw = null
                stack.clear()
                stack.add(storage.id)
                askStartAddingFor = storage.id
            },
            onDismiss = {
                discardPending(pendingStorageRaw)
                pendingStorageRaw = null
            },
        )
    }

    // 2) Storage created -> start adding items?
    askStartAddingFor?.let { id ->
        val storage = store.get(id)
        if (storage == null) {
            askStartAddingFor = null
        } else {
            AlertDialog(
                onDismissRequest = { askStartAddingFor = null },
                title = { Text("“${storage.name}” is ready") },
                text = { Text("Start adding items by scanning the next QR code.") },
                confirmButton = {
                    TextButton(onClick = {
                        askStartAddingFor = null
                        scanMode = ScanMode.AddTo(id)
                    }) { Text("Scan QR code") }
                },
                dismissButton = {
                    TextButton(onClick = { askStartAddingFor = null }) { Text("Later") }
                },
            )
        }
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
                photoPath = pendingChild.photo,
                onTakePhoto = { takePhoto("C$SEP${parent.id}$SEP${pendingChild.qr}") },
                onConfirm = { type, name ->
                    store.add(pendingChild.qr, name, type, parent.id, pendingChild.photo)
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
        if (item == null) {
            viewingItem = null
        } else {
            ItemDialog(
                item = item,
                location = store.path(item.id).dropLast(1).joinToString(" › ") { it.name },
                onPhoto = { takePhoto("N$SEP${item.id}") },
                onViewPhoto = { viewingPhoto = it },
                onRename = { renaming = item },
                onDelete = { viewingItem = null; deleting = item },
                onDismiss = { viewingItem = null },
            )
        }
    }

    viewingPhoto?.let { path ->
        PhotoViewer(path = path, onDismiss = { viewingPhoto = null })
    }

    // QR already used
    duplicate?.let { node ->
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
        AlertDialog(
            onDismissRequest = { notFoundQr = null },
            title = { Text("Not in your inventory") },
            text = { Text("No storage, container or item uses this QR code:\n$qr") },
            confirmButton = { TextButton(onClick = { notFoundQr = null }) { Text("OK") } },
        )
    }

    message?.let { text ->
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
    onAdd: () -> Unit,
    onFind: () -> Unit,
    onSettings: () -> Unit,
    onOpen: (Node) -> Unit,
    onRename: (Node) -> Unit,
    onDelete: (Node) -> Unit,
    onPhoto: (Node) -> Unit,
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
                            Icon(Icons.Filled.Search, contentDescription = "Find by QR code")
                        }
                    }
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Filled.Add, contentDescription = "Add storage")
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
                hint = "Tap + in the top right corner to scan a storage QR code.",
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
                        detail = countText(items, containers),
                        onClick = { onOpen(storage) },
                        onRename = { onRename(storage) },
                        onDelete = { onDelete(storage) },
                        onPhoto = { onPhoto(storage) },
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
    onOpen: (Node) -> Unit,
    onRename: (Node) -> Unit,
    onDelete: (Node) -> Unit,
    onPhoto: (Node) -> Unit,
    onViewPhoto: (String) -> Unit,
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
                        else typeLabel(node.type)
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
                    )
                },
                colors = barColors(),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScanAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Scan to add") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color(0xFF2B1A00),
            )
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
                    photoPath = node.photoPath,
                    emptyText = "No photo of this ${typeLabel(node.type).lowercase()} yet",
                    onView = onViewPhoto,
                    onTakePhoto = { onPhoto(node) },
                )
            }
            if (children.isEmpty()) {
                item {
                    Text(
                        "Nothing in here yet. Tap “Scan to add” and scan the QR code on an item or a smaller container.",
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
                    NodeRow(
                        node = child,
                        detail = detail,
                        onClick = { onOpen(child) },
                        onRename = { onRename(child) },
                        onDelete = { onDelete(child) },
                        onPhoto = { onPhoto(child) },
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
        scope.launch {
            state = try {
                val info = Updater.fetchLatest(context)
                if (info.versionCode > installedCode) UpdateState.Available(info)
                else UpdateState.UpToDate(info.versionName)
            } catch (e: Exception) {
                UpdateState.Failed(e.message ?: "Couldn't check for updates.")
            }
        }
    }

    fun downloadAndInstall(info: UpdateInfo) {
        if (!Updater.ensureInstallPermission(context)) {
            permissionNote = true
            return
        }
        permissionNote = false
        state = UpdateState.Downloading(info, 0f)
        scope.launch {
            state = try {
                val file = Updater.download(context, info) { p ->
                    scope.launch { state = UpdateState.Downloading(info, p) }
                }
                Updater.install(context, file)
                UpdateState.Ready(info, file)
            } catch (e: Exception) {
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
    photoPath: String?,
    emptyText: String,
    onView: (String) -> Unit,
    onTakePhoto: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    if (photoPath != null) {
        PhotoImage(
            path = photoPath,
            maxPx = 1200,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(shape)
                .clickable { onView(photoPath) },
        )
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
                if (node.photoPath != null) {
                    PhotoImage(
                        path = node.photoPath,
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
                Text(
                    "QR: ${node.qr}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OverflowMenu(
                hasPhoto = node.photoPath != null,
                onRename = onRename,
                onDelete = onDelete,
                onPhoto = onPhoto,
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
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(if (hasPhoto) "Retake photo" else "Take photo") },
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

/** Photo preview used inside the naming dialogs, with a take/retake button. */
@Composable
private fun DialogPhoto(photoPath: String?, onTakePhoto: () -> Unit) {
    if (photoPath != null) {
        PhotoImage(
            path = photoPath,
            maxPx = 800,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        TextButton(onClick = onTakePhoto) { Text("Retake photo") }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "No photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onTakePhoto) { Text("Take photo") }
        }
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
    photoPath: String? = null,
    onTakePhoto: (() -> Unit)? = null,
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
                    DialogPhoto(photoPath, onTakePhoto)
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
    photoPath: String?,
    onTakePhoto: () -> Unit,
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
                DialogPhoto(photoPath, onTakePhoto)
                Text(
                    "QR code: $qr",
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
    onViewPhoto: (String) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val photo = item.photoPath
                if (photo != null) {
                    PhotoImage(
                        path = photo,
                        maxPx = 1000,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onViewPhoto(photo) },
                    )
                    TextButton(onClick = onPhoto) { Text("Retake photo") }
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
                Text(
                    "QR: ${item.qr}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    TextButton(onClick = onRename) { Text("Rename") }
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun PhotoViewer(path: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            PhotoImage(
                path = path,
                maxPx = 2000,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            Text(
                "Tap to close",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            )
        }
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
