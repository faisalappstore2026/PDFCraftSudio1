package com.pdfcraft.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation

import androidx.compose.foundation.border

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.layout.offset

import androidx.compose.foundation.layout.BoxWithConstraints

import androidx.compose.foundation.Image

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.os.Environment
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import android.provider.MediaStore
import android.media.MediaScannerConnection
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import com.pdfcraft.studio.core.pdf.PdfGenerator
import com.pdfcraft.studio.core.project.ProjectStore
import com.pdfcraft.studio.ui.common.colorpicker.PageBackgroundEditorDialog
import com.pdfcraft.studio.ui.common.colorpicker.TemplatePreviewPage
import com.pdfcraft.studio.ui.common.colorpicker.TemplatePreviewImage
import com.pdfcraft.studio.ui.common.colorpicker.TemplatePreviewText
import com.pdfcraft.studio.ui.common.AppIcons
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdfcraft.studio.R
import com.pdfcraft.studio.core.session.SessionStore
import com.pdfcraft.studio.ui.editor.canvas.PdfPagesPreview
import com.pdfcraft.studio.ui.editor.canvas.TextBoxTouch
import com.pdfcraft.studio.ui.theme.PDFCraftStudioTheme
import com.pdfcraft.studio.ui.theme.BrandPageSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(projectPath: String? = null, onBackClick: () -> Unit, onViewPdfClick: (String) -> Unit = {}) {
    val viewModel: EditorViewModel = viewModel(key = projectPath ?: "new")
    val sessionContext = androidx.compose.ui.platform.LocalContext.current
    val sessionSnap = remember { SessionStore.load(sessionContext) }
    val restoreThisEditor = sessionSnap.screen == SessionStore.EDITOR
    val restoredTool = sessionSnap.openTool

    // Image edit dialogs / actions state (must be before use)
    var cropImageId by remember { mutableStateOf<String?>(null) }
    var rotateImageId by remember { mutableStateOf<String?>(null) }
    var rotateDegrees by remember { mutableStateOf(0f) }
    var linkImageId by remember { mutableStateOf<String?>(null) }
    var linkUrlText by remember { mutableStateOf("") }
    var showImagePositionDialog by remember { mutableStateOf(false) }
    var imagePositionTargetId by remember { mutableStateOf<String?>(null) }

    cropImageId?.let { cid ->
        val img = viewModel.getImage(cid)
        val bmp = img?.bitmap
        if (bmp != null && !bmp.isRecycled) {
            CropImageScreen(
                bitmap = bmp,
                onCancel = { cropImageId = null },
                onApply = { l, t, r, b ->
                    viewModel.cropImageBitmap(cid, l, t, r, b)
                    cropImageId = null
                }
            )
        } else {
            cropImageId = null
        }
    }

    rotateImageId?.let { rid ->
        val img = viewModel.getImage(rid)
        val bmp = img?.bitmap
        if (bmp != null && !bmp.isRecycled) {
            RotateImageDialog(
                bitmap = bmp,
                degrees = rotateDegrees,
                onDegreesChange = { rotateDegrees = it },
                onCancel = {
                    rotateImageId = null
                    rotateDegrees = 0f
                },
                onOk = {
                    if (rotateDegrees % 360f != 0f) {
                        viewModel.rotateImageBitmap(rid, rotateDegrees)
                    }
                    rotateImageId = null
                    rotateDegrees = 0f
                }
            )
        } else {
            rotateImageId = null
        }
    }

    linkImageId?.let { lid ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { linkImageId = null },
            title = { androidx.compose.material3.Text(stringResource(R.string.image_link_title)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text(stringResource(R.string.image_link_instruction))
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    val imgLinkFocus = remember { FocusRequester() }
                    val imgLinkKb = LocalSoftwareKeyboardController.current
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(180)
                        imgLinkFocus.requestFocus()
                        imgLinkKb?.show()
                    }
                    androidx.compose.material3.OutlinedTextField(
                        value = linkUrlText,
                        onValueChange = { linkUrlText = it },
                        placeholder = { androidx.compose.material3.Text(stringResource(R.string.image_link_url_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(imgLinkFocus)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.setImageLinkUrl(lid, linkUrlText)
                    linkImageId = null
                }) { androidx.compose.material3.Text(stringResource(R.string.image_link_done)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { linkImageId = null }) {
                    androidx.compose.material3.Text(stringResource(R.string.image_link_cancel))
                }
            }
        )
    }

    if (showImagePositionDialog && imagePositionTargetId != null) {
        val srcId = imagePositionTargetId ?: ""
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showImagePositionDialog = false
                imagePositionTargetId = null
            },
            title = { androidx.compose.material3.Text(stringResource(R.string.image_position_title)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text(stringResource(R.string.image_position_move_title))
                    androidx.compose.material3.Text(stringResource(R.string.image_position_move_hint))
                    androidx.compose.material3.TextButton(onClick = {
                        viewModel.startImageMove(srcId)
                        showImagePositionDialog = false
                        imagePositionTargetId = null
                    }) { androidx.compose.material3.Text(stringResource(R.string.image_position_move_title)) }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.Text(stringResource(R.string.image_position_swap_title))
                    androidx.compose.material3.Text(stringResource(R.string.image_position_swap_hint))
                    androidx.compose.material3.TextButton(onClick = {
                        viewModel.startImageSwap(srcId)
                        showImagePositionDialog = false
                        imagePositionTargetId = null
                    }) { androidx.compose.material3.Text(stringResource(R.string.image_position_swap_title)) }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showImagePositionDialog = false
                    imagePositionTargetId = null
                }) { androidx.compose.material3.Text(stringResource(R.string.image_position_cancel)) }
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showImportSettings by remember { mutableStateOf(false) }
    var showDragPagePicker by remember { mutableStateOf(false) }
    var imagePagePickerKind by remember { mutableStateOf("drag") }
    var showDeleteImagesPagePicker by remember { mutableStateOf(false) }
    var closeMenusSignal by remember { mutableStateOf(0) }
    var toolMenuOpen by remember { mutableStateOf(false) }
    var showDeleteImagesImagePicker by remember { mutableStateOf(false) }
    var showDeleteImagesModeDialog by remember { mutableStateOf(false) }
    var deletePagesSelected by remember { mutableStateOf(SessionStore.parseInts(sessionSnap.deletePages).toSet()) }
    var deleteImagePickSelected by remember { mutableStateOf(SessionStore.parseIds(sessionSnap.deleteImgs)) }
    var showDragImagePicker by remember { mutableStateOf(false) }
    var dragPageForPicker by remember { mutableStateOf(0) }
    var dragPagesSelected by remember { mutableStateOf(SessionStore.parseInts(sessionSnap.dragPages).toSet()) }
    var dragImagePickSelected by remember { mutableStateOf(SessionStore.parseIds(sessionSnap.dragImgs)) }
    var importStartPageIndex by remember { mutableStateOf<Int?>(null) }
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showTextBgColorPicker by remember { mutableStateOf(false) }
    var showTextShadowPanel by remember { mutableStateOf(false) }
    var showTextBorderPicker by remember { mutableStateOf(false) }
    var textBorderSliderOpen by remember { mutableStateOf(false) }
    var textBgSliderOpen by remember { mutableStateOf(false) }
    var showTextSpreadPicker by remember { mutableStateOf(false) }
    var textSpreadSliderOpen by remember { mutableStateOf(false) }
    var revealTextSignal by remember { mutableStateOf(0) }
    var showFontTools by remember { mutableStateOf(false) }
    var showTextLinkDialog by remember { mutableStateOf(false) }
    var showAdvancedBgEditor by remember { mutableStateOf(false) }
    var textLinkUrl by remember { mutableStateOf("") }
    var textLinkHint by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var editorBackReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(450)
        editorBackReady = true
    }
    BackHandler(enabled = editorBackReady) {
        if (toolMenuOpen) {
            closeMenusSignal++
            toolMenuOpen = false
        } else {
            val appCtx = context.applicationContext
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    viewModel.leaveEditorAndKeepWork(appCtx)
                }
                onBackClick()
            }
        }
    }
    var openedProjectPath by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(projectPath) {
        val path = projectPath?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        openedProjectPath = path
        if (path.startsWith("pdf|")) {
            viewModel.importPdfFromUri(context, path.substring(4))
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.IO) {
            ProjectStore.load(context, java.io.File(path))
        }
        val data = result.data
        if (result.success && data != null) {
            viewModel.applyLoadedProject(data, java.io.File(path))
            val warn = result.warning
            if (!warn.isNullOrBlank()) snackbarHostState.showSnackbar(warn)
        } else {
            snackbarHostState.showSnackbar(
                result.error?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.open_project_failed)
            )
            onBackClick()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.saveDraftAsync(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            val appCtx = context.applicationContext
            val vm = viewModel
            Thread { vm.saveDraftBlocking(appCtx) }.start()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            viewModel.saveDraftAsync(context)
        }
    }
    LaunchedEffect(
        viewModel.currentDraftFile,
        viewModel.currentProjectFile,
        projectPath
    ) {
        val f = viewModel.currentProjectFile ?: viewModel.currentDraftFile
        val path = f?.absolutePath ?: projectPath
        if (!path.isNullOrBlank()) SessionStore.setEditor(sessionContext, path)
    }
    LaunchedEffect(
        viewModel.importedImages.size,
        viewModel.textElements.size,
        viewModel.minPageCount
    ) {
        viewModel.saveDraftAsync(sessionContext)
    }
    LaunchedEffect(
        showFontTools, showImportSettings, showTextColorPicker,
        showTextBgColorPicker, showTextShadowPanel, showTextLinkDialog
    ) {
        val tool = when {
            showFontTools -> "font"
            showImportSettings -> "import"
            showTextColorPicker -> "textColor"
            showTextBgColorPicker -> "textBg"
            showTextShadowPanel -> "textShadow"
            showTextLinkDialog -> "textLink"
            showAdvancedBgEditor -> "advancedBg"
            showDragPagePicker -> "dragPages"
            showDeleteImagesPagePicker -> "deleteImages"
            else -> ""
        }
        SessionStore.setOpenTool(sessionContext, tool)
    }
    LaunchedEffect(viewModel.focusedTextId) {
        SessionStore.setFocusedTextId(sessionContext, viewModel.focusedTextId.orEmpty())
    }
    LaunchedEffect(viewModel.undoCount, viewModel.redoCount) {
        SessionStore.setHistory(sessionContext, viewModel.undoCount, viewModel.redoCount)
    }
    LaunchedEffect(
        viewModel.pageSizeSelection.toList(),
        viewModel.pageBgColorSelection.toList(),
        viewModel.pageDeleteSelection.toList(),
        viewModel.pageDuplicateSelection.toList(),
        viewModel.pageNumberingSelection.toList()
    ) {
        SessionStore.setPageSelections(
            sessionContext,
            SessionStore.csv(viewModel.pageSizeSelection),
            SessionStore.csv(viewModel.pageBgColorSelection),
            SessionStore.csv(viewModel.pageDeleteSelection),
            SessionStore.csv(viewModel.pageDuplicateSelection),
            SessionStore.csv(viewModel.pageNumberingSelection)
        )
    }
    LaunchedEffect(dragPagesSelected, deletePagesSelected, dragImagePickSelected, deleteImagePickSelected) {
        SessionStore.setPickerSets(
            sessionContext,
            SessionStore.csv(dragPagesSelected),
            SessionStore.csv(deletePagesSelected),
            SessionStore.csvIds(dragImagePickSelected),
            SessionStore.csvIds(deleteImagePickSelected)
        )
    }
    LaunchedEffect(showDragImagePicker, showDeleteImagesImagePicker, showDeleteImagesModeDialog) {
        SessionStore.setExtraPickers(
            sessionContext, showDragImagePicker, showDeleteImagesImagePicker, showDeleteImagesModeDialog
        )
    }
    var historySeeded by remember { mutableStateOf(false) }

    val noImagesSelectedMessage =
        stringResource(R.string.no_images_selected)

    val imageSavedMessage =
        stringResource(R.string.image_saved_message)

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia()
        ) { uris ->
            if (uris.isEmpty()) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        noImagesSelectedMessage
                    )
                }
            } else {
                val replaceId = viewModel.pendingReplaceImageId
                viewModel.importImages(uris, replaceId = replaceId, startPageIndex = importStartPageIndex ?: 0)
            }
        }

    val fontPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                viewModel.importFontFromUri(uri)
            }
        }

    var pendingBgBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val backgroundImagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                if (showAdvancedBgEditor) {
                    viewModel.decodeBackgroundPreview(uri) { pendingBgBitmap = it }
                } else {
                    viewModel.setPageBackgroundFromUri(uri)
                }
            }
        }

    LaunchedEffect(viewModel.lastFontImportMessage) {
        val msg = viewModel.lastFontImportMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeFontImportMessage()
        }
    }


    if (showAdvancedBgEditor) {
        val first = viewModel.pageBgColorSelection.firstOrNull() ?: 0
        PageBackgroundEditorDialog(
            initial = viewModel.inspectBackground(first),
            initialBitmap = viewModel.pageBackgroundPhoto,
            pendingBitmap = pendingBgBitmap,
            previewPages = (0 until viewModel.documentPageCount().coerceAtLeast(1)).map { pi ->
                TemplatePreviewPage(
                    index = pi,
                    bitmap = viewModel.originalPdfRasters()[pi],
                    images = viewModel.imagesOnPage(pi).mapNotNull { im ->
                        val b = im.bitmap ?: return@mapNotNull null
                        if (im.absWFrac <= 0.001f) return@mapNotNull null
                        TemplatePreviewImage(b, im.absXFrac, im.absYFrac, im.absWFrac, im.absHFrac)
                    },
                    texts = viewModel.textElements.filter { it.pageIndex == pi }.map { te ->
                        TemplatePreviewText(
                            te.text, te.xFraction, te.yFraction, te.textColorArgb, te.fontSizeSp
                        )
                    },
                    aspectRatio = viewModel.pageAspectOverrides[pi] ?: viewModel.pageAspectRatio
                )
            },
            initialPreviewPageIndex = if (
                viewModel.originalPdfRasters().isNotEmpty() ||
                viewModel.pageBackgroundBitmapOverrides.isNotEmpty() ||
                    (projectPath?.startsWith("pdf|") == true)
            ) 0 else -1,
            onPickImage = {
                backgroundImagePickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onCancel = {
                showAdvancedBgEditor = false
                pendingBgBitmap = null
            },
            onOk = { spec, bmp ->
                viewModel.applyPageBackgroundToSelection(spec, bmp)
                viewModel.saveDraftAsync(context)
                showAdvancedBgEditor = false
                pendingBgBitmap = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        viewModel.currentProjectName
                            ?: stringResource(R.string.editor_title),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (toolMenuOpen) {
                            closeMenusSignal++
                            toolMenuOpen = false
                        } else {
                            val appCtx = context.applicationContext
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    viewModel.leaveEditorAndKeepWork(appCtx)
                                }
                                onBackClick()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(
                                R.string.content_desc_back
                            )
                        )
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    var showNameDialog by remember { mutableStateOf(false) }
                    var showSuccessDialog by remember { mutableStateOf(false) }
                    var showErrorDialog by remember { mutableStateOf(false) }
                    var errorMessage by remember { mutableStateOf("") }
                    var savedFileName by remember { mutableStateOf("") }
                    var linkWarning by remember { mutableStateOf<String?>(null) }
                    var savedUri by remember { mutableStateOf<android.net.Uri?>(null) }
                    var nameField by remember {
                        val n = sessionSnap.nameField.ifBlank { "Project.pdf" }
                        val end = n.substringBefore(".").length.coerceAtLeast(0)
                        mutableStateOf(TextFieldValue(n, TextRange(0, end)))
                    }
                    LaunchedEffect(menuExpanded, showNameDialog, showSuccessDialog, nameField.text) {
                        SessionStore.setOverflow(sessionContext, menuExpanded)
                        SessionStore.setNameDialog(sessionContext, showNameDialog, nameField.text, showSuccessDialog)
                    }

                    HistoryActionButton(
                        icon = AppIcons.Undo,
                        count = viewModel.undoCount,
                        enabled = viewModel.undoCount > 0,
                        contentDesc = "Undo",
                        onTap = { viewModel.undoOne() },
                        onLong = { viewModel.undoAll() }
                    )
                    HistoryActionButton(
                        icon = AppIcons.Redo,
                        count = viewModel.redoCount,
                        enabled = viewModel.redoCount > 0,
                        contentDesc = "Redo",
                        onTap = { viewModel.redoOne() },
                        onLong = { viewModel.redoAll() }
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.editor_more_options)
                            )
                        }
                        var projectSaving by remember { mutableStateOf(false) }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.save_project)) },
                                enabled = !projectSaving,
                                onClick = {
                                    menuExpanded = false
                                    if (projectSaving) return@DropdownMenuItem
                                    projectSaving = true
                                    coroutineScope.launch {
                                        val defaultName = context.getString(R.string.save_project_default_name)
                                        val saveName =
                                            if (viewModel.isDraftSession() ||
                                                (viewModel.currentProjectFile == null &&
                                                    (viewModel.currentProjectName ?: "")
                                                        .matches(Regex("^Draft Project \\d+$")))
                                            ) defaultName
                                            else viewModel.currentProjectName ?: defaultName
                                        val result = withContext(Dispatchers.IO) {
                                            ProjectStore.save(context, viewModel, saveName)
                                        }
                                        projectSaving = false
                                        if (result.success && result.path.isNotBlank()) {
                                            viewModel.onProjectSaved(java.io.File(result.path))
                                        }
                                        snackbarHostState.showSnackbar(
                                            if (result.success)
                                                context.getString(R.string.save_project_success)
                                            else
                                                result.error?.takeIf { it.isNotBlank() }
                                                    ?: context.getString(R.string.save_project_failed)
                                        )
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_pdf)) },
                                onClick = {
                                    menuExpanded = false
                                    if (!viewModel.hasExportableContent()) {
                                        errorMessage = context.getString(R.string.export_pdf_empty)
                                        showErrorDialog = true
                                    } else {
                                        nameField = TextFieldValue("Project.pdf", TextRange(0, 7))
                                        showNameDialog = true
                                    }
                                }
                            )
                        }
                    }

                    // --- Name dialog ---
                    if (showNameDialog) {
                        val nameFocusRequester = remember { FocusRequester() }
                        val keyboardController = LocalSoftwareKeyboardController.current
                        LaunchedEffect(Unit) {
                            // Wait until dialog + TextField are composed, then show keyboard
                            kotlinx.coroutines.delay(200)
                            nameFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        AlertDialog(
                            onDismissRequest = { showNameDialog = false },
                            title = { Text(stringResource(R.string.export_pdf_dialog_title)) },
                            text = {
                                OutlinedTextField(
                                    value = nameField,
                                    onValueChange = { nameField = it },
                                    label = { Text(stringResource(R.string.export_pdf_name_hint)) },
                                    singleLine = true,
                                    modifier = Modifier.focusRequester(nameFocusRequester)
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showNameDialog = false
                                    val raw = nameField.text.trim().ifEmpty { "Project.pdf" }
                                    val result = PdfGenerator.export(
                                        context = context,
                                        fileName = raw,
                                        images = viewModel.importedImages.toList(),
                                        textElements = viewModel.textElements.toList(),
                                        imagesPerRow = viewModel.imagesPerRow,
                                        pageAspectRatio = viewModel.pageAspectRatio,
                                        pageBackgroundColor = viewModel.pageBackgroundColor,
                                        imageSpacingDp = viewModel.imageSpacingDp,
                                        imageCellAspectRatio = viewModel.layoutCellAspectRatio,
                                        pageMarginDp = viewModel.pageMarginDp,
                                        minPageCount = viewModel.minPageCount,
                                        pageAspectRatioForPage = viewModel::aspectRatioForPage,
                                        pageBackgroundColorForPage = viewModel::backgroundColorForPage,
                                        pageBackgroundBitmapForPage = viewModel::backgroundBitmapForPage,
                                        pageBackgroundKindForPage = viewModel::resolvedKind,
                                        pageBackgroundColor2ForPage = viewModel::backgroundColor2ForPage,
                                        pageBackgroundDirForPage = viewModel::backgroundDirForPage
                                    )
                                    if (result.success) {
                                        savedFileName = result.fileName
                                        linkWarning = result.linkWarning
                                        savedUri = result.savedUri
                                        showSuccessDialog = true
                                    } else {
                                        errorMessage = if (result.message == "empty")
                                            context.getString(R.string.export_pdf_empty)
                                        else
                                            context.getString(R.string.export_pdf_failed)
                                        showErrorDialog = true
                                    }
                                }) {
                                    Text(stringResource(R.string.ok))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showNameDialog = false }) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }

                    // --- Success dialog ---
                    if (showSuccessDialog) {
                        AlertDialog(
                            onDismissRequest = { showSuccessDialog = false },
                            title = { Text(stringResource(R.string.export_pdf_success_title)) },
                            text = {
                                Column {
                                    Text(stringResource(R.string.export_pdf_success_message, savedFileName))
                                    val warning = linkWarning
                                    if (warning != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.export_link_warning, warning),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showSuccessDialog = false }) {
                                    Text(stringResource(R.string.export_pdf_done))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showSuccessDialog = false
                                    savedUri?.let { onViewPdfClick(it.toString()) }
                                }) {
                                    Text(stringResource(R.string.view_pdf_button))
                                }
                            }
                        )
                    }

                    // --- Error dialog ---
                    if (showErrorDialog) {
                        AlertDialog(
                            onDismissRequest = { showErrorDialog = false },
                            title = { Text(stringResource(R.string.export_pdf_failed)) },
                            text = { Text(errorMessage) },
                            confirmButton = {
                                TextButton(onClick = { showErrorDialog = false }) {
                                    Text(stringResource(R.string.export_pdf_done))
                                }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->

        if (viewModel.isImporting) {
            val progressMessage = when {
                viewModel.importingPdf -> stringResource(R.string.import_progress_message_pdf)
                viewModel.selectedImageSizeOption.targetBytes != null -> stringResource(
                    R.string.import_progress_message_compressing,
                    viewModel.selectedImageSizeOption.label
                )
                else -> stringResource(R.string.import_progress_message_default)
            }
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(progressMessage, color = Color.Black)
                    }
                }
            }
        }

        
        
        // ---- Delete Images: Step 1 pages ----
        if (showDeleteImagesPagePicker) {
            val pageCount = viewModel.documentPageCount().coerceAtLeast(1)
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteImagesPagePicker = false },
                title = { Text(stringResource(R.string.image_delete_title)) },
                text = {
                    Column {
                        Text(
                            stringResource(R.string.image_delete_select_page_instruction),
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = {
                            val allSelected =
                                pageCount > 0 &&
                                deletePagesSelected.containsAll((0 until pageCount).toList())
                            deletePagesSelected =
                                if (allSelected) emptySet()
                                else (0 until pageCount).toSet()
                        }) {
                            Text(stringResource(R.string.image_drag_select_all))
                        }
                        Column(
                            Modifier
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            for (i in 0 until pageCount) {
                                val checked = i in deletePagesSelected
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            deletePagesSelected =
                                                if (checked) deletePagesSelected - i
                                                else deletePagesSelected + i
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = checked,
                                        onCheckedChange = { on ->
                                            deletePagesSelected =
                                                if (on) deletePagesSelected + i
                                                else deletePagesSelected - i
                                        }
                                    )
                                    Text("Page ${i + 1}")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (deletePagesSelected.isNotEmpty()) {
                            deleteImagePickSelected = emptyList()
                            showDeleteImagesPagePicker = false
                            showDeleteImagesImagePicker = true
                        }
                    }) { Text(stringResource(R.string.image_drag_next)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteImagesPagePicker = false }) {
                        Text(stringResource(R.string.image_link_cancel))
                    }
                }
            )
        }

        // ---- Delete Images: Step 2 image grid ----
        if (showDeleteImagesImagePicker) {
            Dialog(
                onDismissRequest = { showDeleteImagesImagePicker = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.image_delete_title),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                            },
                            navigationIcon = {
                                Text(
                                    text = "\u2715",
                                    color = Color.Black,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .clickable { showDeleteImagesImagePicker = false }
                                )
                            },
                            actions = {
                                TextButton(onClick = {
                                    val allIds = deletePagesSelected.sorted().flatMap { p ->
                                        viewModel.imagesOnPage(p).map { it.id }
                                    }
                                    deleteImagePickSelected =
                                        if (allIds.isNotEmpty() && deleteImagePickSelected == allIds)
                                            emptyList()
                                        else
                                            allIds
                                }) {
                                    Text(stringResource(R.string.image_drag_select_all))
                                }
                                TextButton(onClick = {
                                    if (deleteImagePickSelected.isNotEmpty()) {
                                        showDeleteImagesImagePicker = false
                                        showDeleteImagesModeDialog = true
                                    }
                                }) {
                                    Text(stringResource(R.string.image_drag_next))
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                        )
                    },
                    containerColor = Color.White
                ) { pad ->
                    Column(
                        Modifier
                            .padding(pad)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.image_delete_select_images_instruction),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            stringResource(R.string.image_pick_selected_count, deleteImagePickSelected.size),
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        PageGroupedImageGrid(
                            pages = deletePagesSelected.sorted(),
                            imagesForPage = { viewModel.imagesOnPage(it) },
                            selectedIds = deleteImagePickSelected,
                            onToggle = { id ->
                                deleteImagePickSelected =
                                    if (id in deleteImagePickSelected)
                                        deleteImagePickSelected.filter { it != id }
                                    else
                                        deleteImagePickSelected + id
                            }
                        )
                    }
                }
            }
        }

        if (showDeleteImagesModeDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteImagesModeDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.image_delete_mode_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.image_delete_mode_instruction),
                            color = Color(0xFF616161),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.deleteImages(deleteImagePickSelected, keepSpace = true)
                                showDeleteImagesModeDialog = false
                                deleteImagePickSelected = emptyList()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.image_delete_keep_space),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteImages(deleteImagePickSelected, keepSpace = false)
                                showDeleteImagesModeDialog = false
                                deleteImagePickSelected = emptyList()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.image_delete_fill_space),
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { showDeleteImagesModeDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(android.R.string.cancel), color = Color.Black)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }


        // ---- Drag Images: Step 1 multi-page picker ----
        if (showDragPagePicker) {
            val pageCount = if (imagePagePickerKind == "perrow" || imagePagePickerKind == "number") viewModel.layoutPageCount() else viewModel.documentPageCount().coerceAtLeast(1)
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDragPagePicker = false },
                title = {
                    Text(stringResource(
                        when (imagePagePickerKind) {
                            "corners" -> R.string.round_corners_title
                            "resize" -> R.string.image_shape_title
                            "perrow" -> R.string.resize_images_title
                            "number" -> R.string.image_numbering_title
                            else -> R.string.image_drag_title
                        }
                    ))
                },
                text = {
                    Column {
                        Text(
                            stringResource(
                                when (imagePagePickerKind) {
                                    "corners" -> R.string.round_corners_select_page_instruction
                                    "resize" -> R.string.resize_select_page_instruction
                                    "perrow" -> R.string.per_row_select_page_instruction
                                    "number" -> R.string.image_numbering_select_page_instruction
                                    else -> R.string.image_drag_select_page_instruction
                                }
                            ),
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = {
                            // Toggle: all selected -> unselect all; otherwise select all
                            val allSelected =
                                pageCount > 0 &&
                                dragPagesSelected.containsAll((0 until pageCount).toList())
                            dragPagesSelected =
                                if (allSelected) emptySet()
                                else (0 until pageCount).toSet()
                        }) {
                            Text(stringResource(R.string.image_drag_select_all))
                        }
                        // heightIn MUST come before verticalScroll or list won't scroll in AlertDialog
                        Column(
                            Modifier
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            for (i in 0 until pageCount) {
                                val checked = i in dragPagesSelected
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            dragPagesSelected =
                                                if (checked) dragPagesSelected - i
                                                else dragPagesSelected + i
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { on ->
                                            dragPagesSelected =
                                                if (on) dragPagesSelected + i
                                                else dragPagesSelected - i
                                        }
                                    )
                                    Text(stringResource(R.string.import_page_item, i + 1))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (dragPagesSelected.isNotEmpty()) {
                            if (imagePagePickerKind == "perrow" || imagePagePickerKind == "number") {
                                dragImagePickSelected = dragPagesSelected.sorted().flatMap { p ->
                                    viewModel.layoutImagesOnPage(p).map { it.id }
                                }
                                showDragPagePicker = false
                                showDragImagePicker = true
                            } else {
                                dragPageForPicker = dragPagesSelected.minOrNull() ?: 0
                                viewModel.startDragImagesOnPage(dragPageForPicker)
                                dragImagePickSelected = emptyList()
                                showDragPagePicker = false
                                showDragImagePicker = true
                            }
                        }
                    }) { Text(stringResource(R.string.image_drag_next)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDragPagePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        
        // ---- Drag Images: Step 2 full-screen image grid ----
        if (showDragImagePicker) {
            Dialog(
                onDismissRequest = { showDragImagePicker = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(when (imagePagePickerKind) { "corners" -> R.string.round_corners_title; "resize" -> R.string.image_shape_title; "perrow" -> R.string.resize_images_title; "number" -> R.string.image_numbering_title; else -> R.string.image_drag_title }),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                            },
                            navigationIcon = {
                                Text(
                                    text = "\u2715",
                                    color = Color.Black,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .clickable { showDragImagePicker = false }
                                )
                            },
                            actions = {
                                TextButton(onClick = {
                                    val allIds = dragPagesSelected.sorted().flatMap { p ->
                                        if (imagePagePickerKind == "perrow" || imagePagePickerKind == "number") viewModel.layoutImagesOnPage(p).map { it.id }
                                        else viewModel.imagesOnPage(p).map { it.id }
                                    }
                                    dragImagePickSelected =
                                        if (allIds.isNotEmpty() && dragImagePickSelected == allIds)
                                            emptyList()
                                        else
                                            allIds
                                }) {
                                    Text(stringResource(R.string.image_drag_select_all))
                                }
                                TextButton(onClick = {
                                    if (dragImagePickSelected.isNotEmpty()) {
                                        when (imagePagePickerKind) {
                                            "corners" -> {
                                                viewModel.beginCornerEdit(dragImagePickSelected)
                                                showDragImagePicker = false
                                            }
                                            "resize" -> {
                                                viewModel.beginResizeEdit(dragImagePickSelected)
                                                showDragImagePicker = false
                                            }
                                            "perrow" -> {
                                                viewModel.beginPerRowEdit(dragImagePickSelected)
                                                showDragImagePicker = false
                                            }
                                            "number" -> {
                                                viewModel.beginNumberingSelection(dragPagesSelected, dragImagePickSelected)
                                                showDragImagePicker = false
                                            }
                                            else -> {
                                                viewModel.setDragImageSelection(dragImagePickSelected)
                                                viewModel.enterDragMoveMode()
                                                showDragImagePicker = false
                                            }
                                        }
                                    }
                                }) {
                                    Text(stringResource(R.string.image_drag_next))
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                        )
                    },
                    containerColor = Color.White
                ) { pad ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(pad)
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            stringResource(
                                when (imagePagePickerKind) {
                                    "corners" -> R.string.round_corners_select_images_instruction
                                    "resize" -> R.string.resize_select_images_instruction
                                    "perrow" -> R.string.per_row_select_images_instruction
                                    "number" -> R.string.image_numbering_select_images_instruction
                                    else -> R.string.image_drag_select_images_instruction
                                }
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.image_pick_selected_count, dragImagePickSelected.size),
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))
                        PageGroupedImageGrid(
                            pages = dragPagesSelected.sorted(),
                            imagesForPage = { p ->
                                if (imagePagePickerKind == "perrow" || imagePagePickerKind == "number") viewModel.layoutImagesOnPage(p)
                                else viewModel.imagesOnPage(p)
                            },
                            selectedIds = dragImagePickSelected,
                            onToggle = { id ->
                                dragImagePickSelected =
                                    if (id in dragImagePickSelected)
                                        dragImagePickSelected.filter { it != id }
                                    else
                                        dragImagePickSelected + id
                            }
                        )
                    }
                }
            }
        }

        if (showImportSettings) {
            ImportImagesDialog(
                selectedOption = viewModel.selectedImageSizeOption,
                onOptionSelected = viewModel::selectImageSizeOption,
                onImportClick = {
                    showImportSettings = false
                    imagePickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onDismiss = {
                    showImportSettings = false
                },
                pageCount = viewModel.documentPageCount(),
                selectedStartPageIndex = importStartPageIndex,
                onStartPageSelected = { importStartPageIndex = it },
                onRatioSelected = { key -> viewModel.applyImportRatioPreset(key) }
            )
        }

            if (textLinkHint) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { textLinkHint = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { textLinkHint = false }) {
                    androidx.compose.material3.Text("OK")
                }
            },
            text = { androidx.compose.material3.Text(stringResource(R.string.text_select_first)) }
        )
    }
    if (showTextLinkDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTextLinkDialog = false },
            title = { androidx.compose.material3.Text(stringResource(R.string.text_link_title)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text(stringResource(R.string.text_link_instruction))
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    val linkFocus = remember { FocusRequester() }
                    val linkKb = LocalSoftwareKeyboardController.current
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(180)
                        linkFocus.requestFocus()
                        linkKb?.show()
                    }
                    androidx.compose.material3.OutlinedTextField(
                        value = textLinkUrl,
                        onValueChange = { textLinkUrl = it },
                        placeholder = { androidx.compose.material3.Text(stringResource(R.string.text_link_url_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(linkFocus)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    if (viewModel.applyLinkToSelection(textLinkUrl)) {
                        showTextLinkDialog = false
                    }
                }) { androidx.compose.material3.Text(stringResource(R.string.text_link_ok)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTextLinkDialog = false }) {
                    androidx.compose.material3.Text(stringResource(R.string.text_link_cancel))
                }
            }
        )
    }

if (showFontTools) {
            val hasSelection =
                viewModel.hasTextRangeForEdit()
            FontToolsDialog(
                fonts = viewModel.availableFonts.toList(),
                selectedFontId = viewModel.currentTextFontId(),
                isBoldActive = viewModel.isSelectionBold(),
                isItalicActive = viewModel.isSelectionItalic(),
                isUnderlineActive = viewModel.isSelectionUnderline(),
                isStrikeActive = viewModel.isSelectionStrike(),
                hasTextSelection = hasSelection,
                onFontSelected = { font ->
                    viewModel.applyFontToSelectedText(font)
                    showFontTools = false
                },
                onBoldClick = {
                    viewModel.toggleBoldForSelection()
                    showFontTools = false
                },
                onItalicClick = {
                    viewModel.toggleItalicForSelection()
                    showFontTools = false
                },
                onUnderlineClick = {
                    viewModel.toggleUnderlineForSelection()
                    showFontTools = false
                },
                onStrikeClick = {
                    viewModel.toggleStrikeForSelection()
                    showFontTools = false
                },
                onImportFontClick = {
                    fontPickerLauncher.launch(
                        arrayOf(
                            "font/ttf",
                            "font/otf",
                            "application/x-font-ttf",
                            "application/x-font-otf",
                            "application/font-sfnt",
                            "*/*"
                        )
                    )
                },
                onDismiss = { showFontTools = false }
            )
        }

        
    if (showTextColorPicker) {
        ColorPickerDialog(
            initialColor = viewModel.selectedTextColorArgb(),
            onConfirm = {
                viewModel.updateSelectedTextColor(it)
                viewModel.endCoalescedEdit()
                showTextColorPicker = false
            },
            onDismiss = {
                viewModel.endCoalescedEdit()
                showTextColorPicker = false
            }
        )
    }
    if (showTextBgColorPicker) {
        ColorPickerDialog(
            initialColor = viewModel.selectedTextBgColorArgb() ?: 0xFFFFFF00,
            onConfirm = {
                viewModel.updateSelectedTextBgColor(it)
                viewModel.endCoalescedEdit()
                showTextBgColorPicker = false
                textBorderSliderOpen = false
                textSpreadSliderOpen = false
                textBgSliderOpen = true
                revealTextSignal++
            },
            onDismiss = {
                viewModel.endCoalescedEdit()
                showTextBgColorPicker = false
            },
            previewAsBackground = true,
            onNone = {
                // Own undo step: selection -> only that part, no selection -> whole box.
                viewModel.endCoalescedEdit()
                viewModel.updateSelectedTextBgColor(null)
                viewModel.endCoalescedEdit()
                showTextBgColorPicker = false
                textBgSliderOpen = false
            }
        )
    }
    if (showTextSpreadPicker) {
        ColorPickerDialog(
            initialColor = viewModel.selectedTextSpreadColorArgb() ?: 0xFFFFD54FL,
            onConfirm = {
                viewModel.updateSelectedTextSpreadColor(it)
                viewModel.endCoalescedEdit()
                showTextSpreadPicker = false
                textBorderSliderOpen = false
                textBgSliderOpen = false
                textSpreadSliderOpen = true
                revealTextSignal++
            },
            onDismiss = {
                viewModel.endCoalescedEdit()
                showTextSpreadPicker = false
            }
        )
    }
    if (showTextBorderPicker) {
        ColorPickerDialog(
            initialColor = viewModel.selectedTextBorderColorArgb(),
            onConfirm = {
                viewModel.updateSelectedTextBorder(it)
                viewModel.endCoalescedEdit()
                showTextBorderPicker = false
                textBgSliderOpen = false
                textSpreadSliderOpen = false
                textBorderSliderOpen = true
            },
            onDismiss = {
                viewModel.endCoalescedEdit()
                showTextBorderPicker = false
            }
        )
    }
    if (showTextShadowPanel) {
        val te = viewModel.activeTextElement()
        ShadowSettingsDialog(
            initialColorArgb = te?.shadowColorArgb ?: 0x80000000L,
            initialOffsetX = te?.shadowOffsetXPx ?: 0f,
            initialOffsetY = te?.shadowOffsetYPx ?: 0f,
            initialBlur = te?.shadowBlurPx ?: 0f,
            onApply = { color, x, y, b ->
                viewModel.updateSelectedTextShadow(
                    colorArgb = color, offsetXPx = x, offsetYPx = y, blurPx = b
                )
                viewModel.endCoalescedEdit()
                showTextShadowPanel = false
            },
            onDismiss = {
                viewModel.endCoalescedEdit()
                showTextShadowPanel = false
            }
        )
    }

Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            EditorToolbar(
                onImportImagesClick = {
                    importStartPageIndex = null
                    showImportSettings = true
                },
                imagesPerRow =
                    viewModel.imagesPerRow,
                pageCountForNumbering = viewModel.currentPageCountEstimate(
                    viewModel.imagesPerPageCapacity()
                ),
                pageNumberingSelected = viewModel.pageNumberingSelection.toSet(),
                onTogglePageNumberingSelection = viewModel::togglePageNumberingSelection,
                onToggleSelectAllPagesForNumbering = {
                    viewModel.toggleSelectAllPagesForNumbering(
                        viewModel.currentPageCountEstimate(viewModel.imagesPerPageCapacity())
                    )
                },
                onClearPageNumberingSelection = viewModel::clearPageNumberingSelection,
                onStartImageNumbering = {
                    viewModel.openNumberingStyleScreen()
                },
                numberingEditMode = viewModel.numberingEditMode,
                numberingEditStep = viewModel.numberingEditStep,
                onNumberingNextStep = viewModel::advanceNumberingEditStep,
                onNumberingBackStep = viewModel::backNumberingEditStep,
                onNumberingBackToStyle = viewModel::backNumberingEditToStyle,
                numberingStyleScreen = viewModel.numberingStyleScreen,
                numberingWeight = viewModel.numberingWeight,
                onNumberingWeightChange = { w ->
                    viewModel.numberingWeight = w
                    viewModel.updateNumberingLiveStyle()
                },
                onConfirmNumberingStyle = {
                    viewModel.confirmNumberingStyleAndEdit(viewModel.imagesPerPageCapacity())
                },
                onCancelNumberingStyle = { viewModel.cancelNumberingStyleScreen() },
                numberingAlpha = viewModel.numberingAlpha,
                onNumberingAlphaChange = { a ->
                    viewModel.numberingAlpha = a
                    viewModel.updateNumberingLiveStyle()
                },
                numberingSizeFrac = viewModel.numberingSizeFrac,
                onNumberingSizeChange = { s ->
                    viewModel.numberingSizeFrac = s
                    viewModel.updateNumberingLiveStyle()
                },
                onNumberingNudge = { dx, dy -> viewModel.nudgeNumbering(dx, dy) },
                onNumberingCenter = { viewModel.centerNumbering() },
                numberingBgArgb = viewModel.numberingBgArgb,
                onNumberingBgChange = { col ->
                    viewModel.numberingBgArgb = col
                    viewModel.updateNumberingLiveStyle()
                },
                numberingFgArgb = viewModel.numberingFgArgb,
                onNumberingFgChange = { col ->
                    viewModel.numberingFgArgb = col
                    viewModel.updateNumberingLiveStyle()
                },
                dragModeActive = viewModel.dragModeActive,
                dragXPercent = viewModel.dragGroupXPercent(),
                dragYPercent = viewModel.dragGroupYPercent(),
                onDragXPercentChange = { viewModel.setDragGroupPositionPercent(it, null) },
                onDragYPercentChange = { viewModel.setDragGroupPositionPercent(null, it) },
                onDragNudge = viewModel::nudgeDragImages,
                onDragCenter = viewModel::centerDragImages,
                onDragDone = viewModel::exitDragImages,
                onDragImagesMenuClick = {
                    imagePagePickerKind = "drag"
                    showDragPagePicker = true
                },
                onRoundCornersMenuClick = {
                    imagePagePickerKind = "corners"
                    showDragPagePicker = true
                },
                onResizeImagesMenuClick = {
                    imagePagePickerKind = "resize"
                    val n = viewModel.documentPageCount().coerceAtLeast(1)
                    dragPagesSelected = (0 until n).toSet()
                    showDragPagePicker = true
                },
                onImagesPerRowMenuClick = {
                    imagePagePickerKind = "perrow"
                    val n = viewModel.layoutPageCount()
                    dragPagesSelected = (0 until n).toSet()
                    showDragPagePicker = true
                },
                onImageNumberingMenuClick = {
                    imagePagePickerKind = "number"
                    val n = viewModel.layoutPageCount().coerceAtLeast(1)
                    dragPagesSelected = (0 until n).toSet()
                    showDragPagePicker = true
                },
                onNumberingStyleBack = {
                    imagePagePickerKind = "number"
                    showDragImagePicker = true
                },
                perRowEditActive = viewModel.perRowEditActive,
                onPerRowDone = { viewModel.exitPerRowEdit() },
                resizeEditActive = viewModel.resizeEditActive,
                onResizeDone = { viewModel.exitResizeEdit() },
                cornersEditActive = viewModel.cornersEditActive,
                onCornersDone = { viewModel.exitCornerEdit() },
                closeMenusSignal = closeMenusSignal,
                onToolMenuOpenChange = { toolMenuOpen = it },
                onDeleteImagesMenuClick = {
                    deletePagesSelected = emptySet()
                    deleteImagePickSelected = emptyList()
                    showDeleteImagesPagePicker = true
                },
                onNumberingDone = { viewModel.finishNumberingEdit() },

                onImagesPerRowSelected =
                    viewModel::updateImagesPerRow,
                imageSpacingDp =
                    viewModel.imageSpacingDp,
                onImageSpacingSelected =
                    viewModel::updateImageSpacing,
                imageCellAspectRatio =
                    viewModel.imageCellAspectRatio,
                onImageCellAspectRatioSelected =
                    viewModel::updateImageCellAspectRatio,
                imageCornerRadiusPercent =
                    viewModel.imageCornerRadiusPercent,
                onImageCornerRadiusSelected =
                    viewModel::updateImageCornerRadiusPercent,
                onAddTextClick =
                    viewModel::enterAddTextMode,
                onFontClick = {
                    if ( // whole box when no range is selected
                        (viewModel.selectedTextId != null || viewModel.focusedTextId != null)
                    ) showFontTools = true
                    else textLinkHint = true
                },
                onInsertLinkClick = {
                    val sel = viewModel.selectionForEdit()
                    if (sel.end < 0 || (viewModel.selectedTextId == null && viewModel.focusedTextId == null && viewModel.pinnedTextId == null)) {
                        textLinkHint = true
                    } else {
                        textLinkUrl = ""
                        showTextLinkDialog = true
                    }
                },
                onBulletListClick = { viewModel.applyBulletListToSelection() },
                onNumberedListClick = { viewModel.applyNumberedListToSelection() },
                onQuoteClick = { viewModel.applyQuoteToSelection() },
                onDividerClick = { viewModel.applyDividerToSelection() },
                hasTextRangeSelection = if (
                    toolMenuOpen || showFontTools || showTextColorPicker ||
                    showTextBgColorPicker || showTextShadowPanel || showTextLinkDialog ||
                    showTextBorderPicker || showTextSpreadPicker
                ) viewModel.hasTextRangeForEdit() else viewModel.hasLiveTextRange(),
                onDeleteTextClick =
                    viewModel::deleteSelectedText,
                hasSelectedText =
                    viewModel.selectedTextId != null || viewModel.focusedTextId != null,
                hasFocusedText = viewModel.focusedTextId != null || viewModel.selectedTextId != null,
                onNeedHint = { msg ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                },
                currentTextAlign = viewModel.activeTextAlign(),
                onTextAlignSelected = viewModel::updateSelectedTextAlign,
                onTextColorClick = {
                    if ( // whole box when no range is selected
                        (viewModel.selectedTextId != null || viewModel.focusedTextId != null)
                    ) showTextColorPicker = true else textLinkHint = true
                },
                onTextBgColorClick = {
                    if ( // whole box when no range is selected
                        (viewModel.selectedTextId != null || viewModel.focusedTextId != null)
                    ) showTextBgColorPicker = true else textLinkHint = true
                },
                onPinTextSelection = { viewModel.pinTextSelectionIfAny() },
                onTextSpreadColorClick = {
                    if (viewModel.selectedTextId != null || viewModel.focusedTextId != null) {
                        showTextSpreadPicker = true
                    } else {
                        textLinkHint = true
                    }
                },
                onTextBorderColorClick = {
                    if (viewModel.selectedTextId != null || viewModel.focusedTextId != null) {
                        showTextBorderPicker = true
                    } else {
                        textLinkHint = true
                    }
                },
                onTextShadowClick = {
                    if ( // whole box when no range is selected
                        (viewModel.selectedTextId != null || viewModel.focusedTextId != null)
                    ) showTextShadowPanel = true else textLinkHint = true
                },
                textSizeSp =
                    viewModel.selectedTextSizeSp(),
                onTextSizeClick = { textLinkHint = true },
                onTextSizeChange =
                    viewModel::updateSelectedTextSize,
                pageAspectRatio =
                    viewModel.pageAspectRatio,
                onPageAspectRatioChange =
                    viewModel::applyPageSizeToSelection,
                pageCountForSize =
                    viewModel.layoutPageCount(),
                pageSizeSelected =
                    viewModel.pageSizeSelection.toSet(),
                onTogglePageSizeSelection =
                    viewModel::togglePageSizeSelection,
                onToggleSelectAllPagesForSize = {
                    viewModel.toggleSelectAllPagesForSize(
                        viewModel.layoutPageCount()
                    )
                },
                onClearPageSizeSelection =
                    viewModel::clearPageSizeSelection,
                onBeginPageSizeEdit = { viewModel.beginPageSizeEdit() },
                sliderAspectForSelection =
                    viewModel.pageSizeSelection.firstOrNull()?.let {
                        viewModel.aspectRatioForPage(it)
                    } ?: viewModel.pageAspectRatio,
                isPageLandscape =
                    viewModel.isPageLandscape,
                onPageOrientationChange =
                    { viewModel.updatePageOrientation(it) },
                pageMarginDp =
                    viewModel.pageMarginDp,
                onPageMarginChange =
                    viewModel::updatePageMarginDp,
                pageBackgroundColor =
                    viewModel.pageBackgroundColor,
                onPageBackgroundColorChange =
                    viewModel::applyBackgroundColorToSelection,
                pageCountForBgColor =
                    viewModel.currentPageCountEstimate(
                        viewModel.imagesPerPageCapacity()
                    ),
                pageBgColorSelected =
                    viewModel.pageBgColorSelection.toSet(),
                onTogglePageBgColorSelection =
                    viewModel::togglePageBgColorSelection,
                onToggleSelectAllPagesForBgColor = {
                    viewModel.toggleSelectAllPagesForBgColor(
                        viewModel.currentPageCountEstimate(
                            viewModel.imagesPerPageCapacity()
                        )
                    )
                },
                onClearPageBgColorSelection =
                    viewModel::clearPageBgColorSelection,
                onOpenAdvancedBackgroundEditor = {
                    pendingBgBitmap = null
                    showAdvancedBgEditor = true
                },
                onPickBackgroundImage = {
                    backgroundImagePickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onClearBackgroundImage =
                    viewModel::clearPageBackgroundImage,
                hasBackgroundImage =
                    viewModel.pageBackgroundImageUri != null,
                pageNumberPosition =
                    viewModel.pageNumberPosition,
                onPageNumberPositionChange =
                    viewModel::updatePageNumberPosition,
                pageNumberStyle =
                    viewModel.pageNumberStyle,
                onPageNumberStyleChange =
                    viewModel::updatePageNumberStyle,
                onAddNewPage =
                    viewModel::addNewPage,
                onDeletePage =
                    viewModel::deleteLastPage,
                pageCountForDelete =
                    viewModel.layoutPageCount(),
                pageDeleteSelected =
                    viewModel.pageDeleteSelection.toSet(),
                onTogglePageDeleteSelection =
                    viewModel::togglePageDeleteSelection,
                onToggleSelectAllPagesForDelete = {
                    viewModel.toggleSelectAllPagesForDelete(
                        viewModel.layoutPageCount()
                    )
                },
                onClearPageDeleteSelection =
                    viewModel::clearPageDeleteSelection,
                onDeleteSelectedPages = {
                    viewModel.deleteSelectedPages(
                        viewModel.imagesPerPageCapacity()
                    )
                },
                pageCountForDuplicate =
                    viewModel.currentPageCountEstimate(
                        viewModel.imagesPerPageCapacity()
                    ),
                pageDuplicateSelected =
                    viewModel.pageDuplicateSelection.toSet(),
                onTogglePageDuplicateSelection =
                    viewModel::togglePageDuplicateSelection,
                onToggleSelectAllPagesForDuplicate = {
                    viewModel.toggleSelectAllPagesForDuplicate(
                        viewModel.currentPageCountEstimate(
                            viewModel.imagesPerPageCapacity()
                        )
                    )
                },
                onClearPageDuplicateSelection =
                    viewModel::clearPageDuplicateSelection,
                onDuplicateSelectedPages = {
                    viewModel.duplicateSelectedPages(
                        viewModel.imagesPerPageCapacity()
                    )
                },
                pageCountForArrange =
                    viewModel.currentPageCountEstimate(
                        viewModel.imagesPerPageCapacity()
                    ),
                onReorderPages = { order ->
                    viewModel.reorderPages(
                        order,
                        viewModel.imagesPerPageCapacity()
                    )
                },
                onMovePage = { from, to ->
                    viewModel.movePageTo(
                        from,
                        to,
                        viewModel.imagesPerPageCapacity()
                    )
                }
            )

            val systemTextToolbar = LocalTextToolbar.current
            val hideSystemTextMenu = toolMenuOpen || showFontTools || showTextColorPicker ||
                showTextBgColorPicker || showTextShadowPanel || showTextLinkDialog ||
                showTextBorderPicker || showTextSpreadPicker
            LaunchedEffect(hideSystemTextMenu) {
                if (hideSystemTextMenu) systemTextToolbar.hide()
            }
            LaunchedEffect(hideSystemTextMenu) {
                if (!hideSystemTextMenu) viewModel.releaseTextSelectionHold()
            }
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current
            // Dropdown menus are popups above the app window and can cover the keyboard.
            // Never show both: keyboard opening closes menus; a menu opening hides the keyboard.
            val imeShowing = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            LaunchedEffect(imeShowing) {
                if (imeShowing && toolMenuOpen) {
                    closeMenusSignal++
                    toolMenuOpen = false
                }
            }
            LaunchedEffect(toolMenuOpen) {
                if (toolMenuOpen) keyboardController?.hide()
            }
            val textBoxActive = viewModel.focusedTextId != null || viewModel.selectedTextId != null
            LaunchedEffect(textBoxActive) {
                if (!textBoxActive) {
                    textBorderSliderOpen = false
                    textBgSliderOpen = false
                    textSpreadSliderOpen = false
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (textBoxActive && !toolMenuOpen) {
                            Modifier.pointerInput(textBoxActive) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    val up = waitForUpOrCancellation()
                                    // A tap inside a text box never deselects it.
                                    if (up != null && TextBoxTouch.downUptime != down.uptimeMillis) {
                                        keyboardController?.hide()
                                        focusManager.clearFocus(force = true)
                                        viewModel.deselectText()
                                    }
                                }
                            }
                        } else Modifier
                    )
            ) {
            CompositionLocalProvider(
                LocalTextToolbar provides if (hideSystemTextMenu) NoOpTextToolbar else systemTextToolbar
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BrandPageSurface)
                        .padding(20.dp)
                ) {
                // Slider panels sit above the pages (pages shift down, not hidden).
                val panelText = viewModel.activeTextElement()
                if (textBorderSliderOpen && panelText != null && !toolMenuOpen) {
                    TextBorderSliderPanel(
                        widthDp = panelText.borderWidthDp,
                        onWidthChange = { viewModel.updateTextBorderWidth(it) },
                        onDone = {
                            viewModel.endCoalescedEdit()
                            textBorderSliderOpen = false
                        }
                    )
                }
                if (textBgSliderOpen && panelText != null && !toolMenuOpen) {
                    TextBgSliderPanel(
                        cornerDp = panelText.bgCornerDp,
                        alpha = panelText.bgAlpha,
                        onCornerChange = { viewModel.updateTextBgStyle(cornerDp = it) },
                        onAlphaChange = { viewModel.updateTextBgStyle(alpha = it) },
                        padLeft = panelText.bgPadLeftDp,
                        padRight = panelText.bgPadRightDp,
                        padTop = panelText.bgPadTopDp,
                        padBottom = panelText.bgPadBottomDp,
                        onPadLeftChange = { viewModel.updateTextBgPadding(left = it) },
                        onPadRightChange = { viewModel.updateTextBgPadding(right = it) },
                        onPadTopChange = { viewModel.updateTextBgPadding(top = it) },
                        onPadBottomChange = { viewModel.updateTextBgPadding(bottom = it) },
                        onDone = {
                            viewModel.endCoalescedEdit()
                            textBgSliderOpen = false
                        }
                    )
                }
                if (textSpreadSliderOpen && panelText != null && !toolMenuOpen) {
                    TextSpreadSliderPanel(
                        spreadDp = panelText.spreadDp,
                        onSpreadChange = { viewModel.updateTextSpread(it) },
                        onDone = {
                            viewModel.endCoalescedEdit()
                            textSpreadSliderOpen = false
                        }
                    )
                }
                PdfPagesPreview(
                    images = viewModel.importedImages,
                    imagesPerRow = viewModel.imagesPerRow,
                    imageSpacingDp = viewModel.imageSpacingDp,
                    imageCellAspectRatio = viewModel.layoutCellAspectRatio,
                    imageCornerRadiusPercent = viewModel.imageCornerRadiusPercent,

                    pageAspectRatio = viewModel.pageAspectRatio,
                    pageAspectRatioForPage = viewModel::aspectRatioForPage,
                    pageMarginDp = viewModel.pageMarginDp,
                    pageBackgroundColor = viewModel.pageBackgroundColor,
                    pageBackgroundColorForPage = viewModel::backgroundColorForPage,
                    pageBackgroundBitmap = viewModel.pageBackgroundBitmap,
                    pageBackgroundBitmapForPage = viewModel::backgroundBitmapForPage,
                    pageBackgroundKindForPage = viewModel::resolvedKind,
                    pageBackgroundColor2ForPage = viewModel::backgroundColor2ForPage,
                    pageBackgroundDirForPage = viewModel::backgroundDirForPage,
                    pageNumberPosition = viewModel.pageNumberPosition,
                    formatPageNumber = viewModel::formatPageNumber,
                    minPageCount = viewModel.minPageCount,

                    textElements =
                        viewModel.textElements,

                    addTextMode =
                        viewModel.addTextMode,

                    selectedTextId =
                        viewModel.selectedTextId,

                    pendingFocusTextId =
                        viewModel.pendingFocusTextId,

                    onAddTextAt = { pageIndex, xFraction, yFraction ->
                        viewModel.addTextAt(pageIndex, xFraction, yFraction)
                    },

                    onSelectText = { id ->
                        viewModel.selectText(id)
                    },

                    onMoveText = { id, xFraction, yFraction ->
                        viewModel.moveText(id, xFraction, yFraction)
                    },
                    onTextRotate = viewModel::setTextRotation,
                    onTextScale = viewModel::setTextHandleSize,
                    onTextBoxResize = viewModel::setTextBoxSize,
                    onTextAutoFit = viewModel::applyTextAutoFit,

                    onTextValueChange = { id, newText, newSelection ->
                        viewModel.updateTextValue(id, newText, newSelection)
                    },

                    onTextFocused = { id ->
                        viewModel.onTextFocused(id)
                    },

                    onTextUnfocused = { id ->
                        viewModel.onTextUnfocused(id)
                    },

                    onConsumePendingFocus = {
                        viewModel.consumePendingFocus()
                    },

                    onDeselectText = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        viewModel.deselectText()
                    },

                    selectedImageIds =
                        viewModel.selectedImageIds,

                    selectionMode =
                        viewModel.selectionMode,

                    singleMenuImageId =
                        viewModel.singleMenuImageId,

                    multipleActionsVisible =
                        viewModel.multipleActionsVisible,

                    reorderMode =
                        viewModel.reorderMode,

                    hasClipboardImages =
                        viewModel.hasClipboardImages,

                    onImageClick = { id ->
                        viewModel.openImageMenu(id)
                    },

                    onImageLongPress = { _ ->
                        // Long-press multi-select removed
                    },

                    onChangePosition = { id ->
                        viewModel.enterSingleReorder(id)
                    },

                    onCut = { id ->
                        viewModel.cutSingle(id)
                    },

                    onCopy = { id ->
                        viewModel.copySingle(id)
                    },

                    onPaste = {
                        viewModel.pasteImages()
                    },

                    onSaveSingle = { id ->
                        val image = viewModel.getImage(id)
                        val bmp = image?.bitmap
                        coroutineScope.launch {
                            if (bmp == null || bmp.isRecycled) {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.image_save_failed)
                                )
                                return@launch
                            }
                            val ok = withContext(Dispatchers.IO) {
                                saveBitmapToGallery(
                                    context = context,
                                    bitmap = bmp
                                )
                            }
                            snackbarHostState.showSnackbar(
                                if (ok) imageSavedMessage
                                else context.getString(R.string.image_save_failed)
                            )
                        }
                    },

                    onCropImage = { id ->
                        viewModel.dismissImageMenu()
                        cropImageId = id
                    },
                    onRotateImage = { id ->
                        viewModel.dismissImageMenu()
                        rotateImageId = id
                        rotateDegrees = 0f
                    },
                    onShareSingle = { id ->
                        viewModel.getImage(id)?.let { image ->
                            shareImages(
                                context = context,
                                images = listOf(image)
                            )
                        }
                    },

                    onReplaceImage = { id ->
                        viewModel.startReplaceImage(id)
                        showImportSettings = true
                    },
                    onImagePosition = { id ->
                        imagePositionTargetId = id
                        showImagePositionDialog = true
                    },
                    onAddImageLink = { id ->
                        linkImageId = id
                        linkUrlText = viewModel.getImage(id)?.linkUrl.orEmpty()
                    },
                    onDeleteSingle = { id ->
                        viewModel.deleteSingle(id)
                    },
                    onDeleteSingleKeepSpace = { id ->
                        viewModel.deleteSingleKeepSpace(id)
                    },
                    onDeleteSingleFillSpace = { id ->
                        viewModel.deleteSingleFillSpace(id)
                    },

                    onFinishMultipleSelection = {
                        viewModel.finishMultipleSelection()
                    },

                    onMultipleChangePosition = {
                        viewModel.closeMultipleActions()
                        viewModel.reorderMode = true
                    },

                    onMultipleCut = {
                        viewModel.cutSelected()
                    },

                    onMultipleCopy = {
                        viewModel.copySelected()
                    },

                    onMultipleSave = {
                        val bitmaps = viewModel.getSelectedImages().mapNotNull { it.bitmap }
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                bitmaps.forEach { bitmap ->
                                    saveBitmapToGallery(context, bitmap)
                                }
                            }
                        }

                        viewModel.cancelSelection()
                    },

                    onMultipleShare = {
                        shareImages(
                            context = context,
                            images = viewModel.getSelectedImages()
                        )
                        viewModel.cancelSelection()
                    },

                    onMultipleDelete = {
                        viewModel.deleteSelected()
                    },

                    onCloseMultipleActions = {
                        viewModel.closeMultipleActions()
                    },

                    onMoveSingle = { sourceId, targetId ->
                        viewModel.moveSingleImageTo(
                            sourceId,
                            targetId
                        )
                    },

                    onMoveMultiple = { targetId ->
                        viewModel.moveSelectedImagesTo(
                            targetId
                        )
                    },

                    onFinishReorder = {
                        viewModel.finishReorder()
                    },

                    dragModeActive = viewModel.dragModeActive,
                    onDragImagesByFrac = viewModel::dragImagesByFrac,
                    onSyncDragOffset = viewModel::syncDragOffset,
                    onEnsureDragSelection = viewModel::ensureDragSelection,
                    revealTextSignal = revealTextSignal,
                    customFonts = viewModel.availableFonts.filter { it.isCustom },
                    initialScrollIndex = if (restoreThisEditor) sessionSnap.editorIndex else 0,
                    initialScrollOffset = if (restoreThisEditor) sessionSnap.editorOffset else 0,
                    onScrollChanged = { i, o -> SessionStore.setEditorScroll(sessionContext, i, o) },

                    modifier = Modifier.fillMaxWidth()
                )
                }
                // (old overlay slider panels moved into the Column above)
                if (toolMenuOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                closeMenusSignal++
                                toolMenuOpen = false
                            }
                    )
                }
            }
            }
        }
    }
}

private object NoOpTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden
    override fun hide() {}
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {}
}

private const val SAVE_LOG = "PDFCraftSave"

private fun saveBitmapToGallery(
    context: Context,
    bitmap: Bitmap
): Boolean {
    if (bitmap.isRecycled) {
        Log.e(SAVE_LOG, "bitmap recycled sdk=" + Build.VERSION.SDK_INT)
        return false
    }
    val software = try {
        if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        } else bitmap
    } catch (e: Exception) {
        Log.e(SAVE_LOG, "hardware copy failed", e)
        bitmap
    }
    val filename = "PDFCraft_" + System.currentTimeMillis() + ".jpg"
    Log.i(SAVE_LOG, "start sdk=" + Build.VERSION.SDK_INT + " " + software.width + "x" + software.height + " file=" + filename)
    return try {
        val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStoreQ(context, software, filename, System.currentTimeMillis())
        } else {
            saveViaLegacyScan(context, software, filename)
        }
        Log.i(SAVE_LOG, "result=" + ok + " file=" + filename)
        ok
    } catch (e: Exception) {
        Log.e(SAVE_LOG, "save failed file=" + filename, e)
        false
    } finally {
        if (software !== bitmap && !software.isRecycled) {
            try { software.recycle() } catch (_: Exception) {}
        }
    }
}

private fun saveViaMediaStoreQ(
    context: Context,
    bitmap: Bitmap,
    filename: String,
    nowMs: Long
): Boolean {
    val resolver = context.contentResolver
    val relativeDir = Environment.DIRECTORY_DCIM + "/Camera/"
    val nowSec = nowMs / 1000L
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.TITLE, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.WIDTH, bitmap.width)
        put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        put(MediaStore.Images.Media.DATE_ADDED, nowSec)
        put(MediaStore.Images.Media.DATE_MODIFIED, nowSec)
        put(MediaStore.Images.Media.DATE_TAKEN, nowMs)
        put(MediaStore.Images.Media.RELATIVE_PATH, relativeDir)
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    Log.i(SAVE_LOG, "collection=" + collection + " path=" + relativeDir)
    val uri = resolver.insert(collection, values)
    if (uri == null) {
        Log.e(SAVE_LOG, "insert returned null")
        return false
    }
    Log.i(SAVE_LOG, "inserted " + uri)
    return try {
        var bytes = 0
        resolver.openOutputStream(uri, "w")?.use { output ->
            val buffer = java.io.ByteArrayOutputStream()
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, buffer)) {
                throw Exception("compress returned false")
            }
            val data = buffer.toByteArray()
            bytes = data.size
            output.write(data)
            output.flush()
            Log.i(SAVE_LOG, "wrote bytes=" + bytes)
        } ?: throw Exception("openOutputStream null")

        val published = resolver.update(
            uri,
            ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
                put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
            },
            null,
            null
        )
        Log.i(SAVE_LOG, "publish rows=" + published + " uri=" + uri)
        if (published <= 0) throw Exception("IS_PENDING update returned " + published)
        if (!verifyPublished(resolver, uri)) throw Exception("MediaStore row invalid")

        val disk = File(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera"),
            filename
        )
        val scanPath = if (disk.exists()) disk.absolutePath else null
        Log.i(SAVE_LOG, "disk=" + disk.absolutePath + " exists=" + disk.exists() + " len=" + disk.length())
        if (scanPath != null) {
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(scanPath),
                arrayOf("image/jpeg")
            ) { path, scanned ->
                Log.i(SAVE_LOG, "Q scanned path=" + path + " uri=" + scanned)
            }
        }
        resolver.notifyChange(uri, null)
        true
    } catch (e: Exception) {
        Log.e(SAVE_LOG, "Q save failed uri=" + uri, e)
        try { resolver.delete(uri, null, null) } catch (del: Exception) {
            Log.e(SAVE_LOG, "cleanup delete failed", del)
        }
        false
    }
}

private fun verifyPublished(
    resolver: android.content.ContentResolver,
    uri: android.net.Uri
): Boolean {
    val cols = arrayOf(
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.RELATIVE_PATH,
        MediaStore.Images.Media.IS_PENDING,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT
    )
    resolver.query(uri, cols, null, null, null)?.use { c ->
        if (!c.moveToFirst()) {
            Log.e(SAVE_LOG, "query empty")
            return false
        }
        fun col(name: String): String {
            val i = c.getColumnIndex(name)
            return if (i < 0) "?" else (c.getString(i) ?: "null")
        }
        val pIdx = c.getColumnIndex(MediaStore.Images.Media.IS_PENDING)
        val sIdx = c.getColumnIndex(MediaStore.Images.Media.SIZE)
        val pending = if (pIdx < 0) -1 else c.getInt(pIdx)
        val size = if (sIdx < 0) -1L else c.getLong(sIdx)
        Log.i(SAVE_LOG, "verify name=" + col(MediaStore.Images.Media.DISPLAY_NAME)
            + " mime=" + col(MediaStore.Images.Media.MIME_TYPE)
            + " path=" + col(MediaStore.Images.Media.RELATIVE_PATH)
            + " pending=" + pending + " size=" + size
            + " w=" + col(MediaStore.Images.Media.WIDTH)
            + " h=" + col(MediaStore.Images.Media.HEIGHT))
        if (pending != 0) {
            Log.e(SAVE_LOG, "still pending")
            return false
        }
        if (size == 0L) {
            Log.e(SAVE_LOG, "size 0")
            return false
        }
        return true
    }
    Log.e(SAVE_LOG, "query null")
    return false
}

private fun saveViaLegacyScan(
    context: Context,
    bitmap: Bitmap,
    filename: String
): Boolean {
    val folder = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
        "Camera"
    )
    if (!folder.exists() && !folder.mkdirs()) {
        Log.e(SAVE_LOG, "mkdirs failed " + folder.absolutePath)
        return false
    }
    val file = File(folder, filename)
    return try {
        FileOutputStream(file).use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
                throw Exception("compress returned false")
            }
            output.flush()
        }
        Log.i(SAVE_LOG, "legacy bytes=" + file.length() + " path=" + file.absolutePath)
        MediaScannerConnection.scanFile(
            context.applicationContext,
            arrayOf(file.absolutePath),
            arrayOf("image/jpeg")
        ) { path, uri -> Log.i(SAVE_LOG, "scanned path=" + path + " uri=" + uri) }
        file.exists() && file.length() > 0L
    } catch (e: Exception) {
        Log.e(SAVE_LOG, "legacy save failed", e)
        if (file.exists()) file.delete()
        false
    }
}

private fun shareImages(
    context: Context,
    images: List<ImportedImage>
) {
    val uris = images.mapNotNull { it.imageUri }

    if (uris.isEmpty()) {
        return
    }

    if (uris.size == 1) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.share_image)
            )
        )
    } else {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList(uris)
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.share_image)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorScreenPreview() {
    PDFCraftStudioTheme {
        EditorScreen(onBackClick = {})
    }
}



@Composable
private fun CropImageScreen(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onApply: (leftFrac: Float, topFrac: Float, rightFrac: Float, bottomFrac: Float) -> Unit
) {
    // Normalized crop rect inside the image (0..1)
    var left by remember { mutableStateOf(0.1f) }
    var top by remember { mutableStateOf(0.1f) }
    var right by remember { mutableStateOf(0.9f) }
    var bottom by remember { mutableStateOf(0.9f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.crop_cancel), color = Color.White)
                }
                Text(
                    text = stringResource(R.string.crop_title),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { onApply(left, top, right, bottom) }) {
                    Text(stringResource(R.string.crop_apply), color = Color.White)
                }
            }
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val maxW = maxWidth
                val maxH = maxHeight
                val imgAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                val boxAspect = maxW / maxH
                val drawW: androidx.compose.ui.unit.Dp
                val drawH: androidx.compose.ui.unit.Dp
                if (imgAspect > boxAspect) {
                    drawW = maxW
                    drawH = maxW / imgAspect
                } else {
                    drawH = maxH
                    drawW = maxH * imgAspect
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(drawW)
                        .height(drawH)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    // Darken outside crop — simple border rectangle
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = drawW * left,
                                top = drawH * top,
                                end = drawW * (1f - right),
                                bottom = drawH * (1f - bottom)
                            )
                            .border(2.dp, Color.White)
                            .pointerInput(Unit) {
                                val areaW = size.width.toFloat().coerceAtLeast(1f)
                                val areaH = size.height.toFloat().coerceAtLeast(1f)
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    val dx = drag.x / areaW
                                    val dy = drag.y / areaH
                                    val w = right - left
                                    val h = bottom - top
                                    val nl = (left + dx).coerceIn(0f, 1f - w)
                                    val nt = (top + dy).coerceIn(0f, 1f - h)
                                    left = nl
                                    top = nt
                                    right = nl + w
                                    bottom = nt + h
                                }
                            }
                    )
                    // Corner handles: expand/shrink
                    val handle = 28.dp
                    // Top-left
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = drawW * left - 12.dp, y = drawH * top - 12.dp)
                            .size(handle)
                            .background(Color.White, CircleShape)
                            .pointerInput(Unit) {
                                val areaW = size.width.toFloat().coerceAtLeast(1f)
                                val areaH = size.height.toFloat().coerceAtLeast(1f)
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    left = (left + drag.x / areaW).coerceIn(0f, right - 0.1f)
                                    top = (top + drag.y / areaH).coerceIn(0f, bottom - 0.1f)
                                }
                            }
                    )
                    // Bottom-right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = drawW * right - 12.dp, y = drawH * bottom - 12.dp)
                            .size(handle)
                            .background(Color.White, CircleShape)
                            .pointerInput(Unit) {
                                val areaW = size.width.toFloat().coerceAtLeast(1f)
                                val areaH = size.height.toFloat().coerceAtLeast(1f)
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    right = (right + drag.x / areaW).coerceIn(left + 0.1f, 1f)
                                    bottom = (bottom + drag.y / areaH).coerceIn(top + 0.1f, 1f)
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun RotateImageDialog(
    bitmap: Bitmap,
    degrees: Float,
    onDegreesChange: (Float) -> Unit,
    onCancel: () -> Unit,
    onOk: () -> Unit
) {
    val preview = remember(bitmap, degrees) {
        if (degrees % 360f == 0f) bitmap
        else {
            val m = android.graphics.Matrix().apply { postRotate(degrees) }
            android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        }
    }
    val norm = ((degrees % 360f) + 360f) % 360f
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = stringResource(R.string.rotate_title),
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RotateActionCard(
                        icon = "\u21B6",
                        label = stringResource(R.string.rotate_left),
                        selected = norm == 270f,
                        onClick = { onDegreesChange((degrees + 270f) % 360f) },
                        modifier = Modifier.weight(1f)
                    )
                    RotateActionCard(
                        icon = "\u21B7",
                        label = stringResource(R.string.rotate_right),
                        selected = norm == 90f,
                        onClick = { onDegreesChange((degrees + 90f) % 360f) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                RotateActionCard(
                    icon = "\u21BB",
                    label = stringResource(R.string.rotate_180),
                    selected = norm == 180f,
                    onClick = { onDegreesChange((degrees + 180f) % 360f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                RotateActionCard(
                    icon = "\u27F3",
                    label = stringResource(R.string.rotate_reset),
                    selected = norm == 0f,
                    onClick = { onDegreesChange(0f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOk) {
                Text(stringResource(R.string.rotate_ok), color = Color(0xFF1976D2))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.rotate_cancel), color = Color.Black)
            }
        }
    )
}

@Composable
private fun RotateActionCard(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Color(0xFFE3F2FD) else Color.White
    val border = if (selected) Color(0xFF1976D2) else Color(0xFFE0E0E0)
    val fg = if (selected) Color(0xFF1976D2) else Color.Black
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 22.sp, color = fg)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun PageGroupedImageGrid(
    pages: List<Int>,
    imagesForPage: (Int) -> List<ImportedImage>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit
) {
    pages.forEach { pageIndex ->
        val imgs = imagesForPage(pageIndex)
        if (imgs.isEmpty()) return@forEach
        Text(
            text = stringResource(R.string.page_label, pageIndex + 1),
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            style = MaterialTheme.typography.titleMedium
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 10.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE0E0E0))
        )
        imgs.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { img ->
                    val selIndex = selectedIds.indexOf(img.id)
                    val selected = selIndex >= 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) Color(0xFF1976D2) else Color.LightGray,
                                shape = androidx.compose.ui.graphics.RectangleShape
                            )
                            .clickable { onToggle(img.id) }
                    ) {
                        val bmp = img.bitmap
                        if (bmp != null && !bmp.isRecycled) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                Modifier.fillMaxSize().background(Color(0xFFE0E0E0)),
                                contentAlignment = Alignment.Center
                            ) { Text("…", color = Color.Gray) }
                        }
                        if (selected) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1976D2).copy(alpha = 0.28f))
                            )
                            Text(
                                text = (selIndex + 1).toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color(0xFF1976D2), CircleShape)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HistoryActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    enabled: Boolean,
    contentDesc: String,
    onTap: () -> Unit,
    onLong: () -> Unit
) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = if (enabled) Color.Black else Color(0xFFBDBDBD),
            modifier = Modifier
                .size(22.dp)
                .pointerInput(enabled, count) {
                    detectTapGestures(
                        onTap = { if (enabled) onTap() },
                        onLongPress = { if (enabled) onLong() }
                    )
                }
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 1.dp)
                    .size(18.dp)
                    .background(Color(0xFF1976D2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (count > 99) "99+" else count.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 9.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}
