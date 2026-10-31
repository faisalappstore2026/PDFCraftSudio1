package com.pdfcraft.studio.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.PlatformTextStyle


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import com.pdfcraft.studio.R
import com.pdfcraft.studio.core.session.SessionStore
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

@Composable
fun EditorToolbar(
    onImportImagesClick: () -> Unit,
    imagesPerRow: Int,
    onImagesPerRowSelected: (Int) -> Unit,
    imageSpacingDp: Int,
    onImageSpacingSelected: (Int) -> Unit,
    imageCellAspectRatio: Float,
    onImageCellAspectRatioSelected: (Float) -> Unit,
    imageCornerRadiusPercent: Int,
    onImageCornerRadiusSelected: (Int) -> Unit,
    onAddTextClick: () -> Unit,
    onFontClick: () -> Unit,
    onDeleteTextClick: () -> Unit,
    onInsertLinkClick: () -> Unit = {},
    onBulletListClick: () -> Unit = {},
    onNumberedListClick: () -> Unit = {},
    onQuoteClick: () -> Unit = {},
    onDividerClick: () -> Unit = {},
    hasSelectedText: Boolean,
    hasTextRangeSelection: Boolean = false,
    currentTextAlign: String = "LEFT",
    onTextAlignSelected: (String) -> Unit = {},
    hasFocusedText: Boolean = false,
    onNeedHint: (String) -> Unit = {},
    onTextColorClick: () -> Unit = {},
    onTextBgColorClick: () -> Unit = {},
    onTextBorderColorClick: () -> Unit = {},
    onTextSpreadColorClick: () -> Unit = {},
    onTextShadowClick: () -> Unit = {},
    onPinTextSelection: () -> Unit = {},
    textSizeSp: Float = 16f,
    onTextSizeClick: () -> Unit = {},
    onTextSizeChange: (Float) -> Unit = {},
    onTextBoxSizeClick: () -> Unit = {},
    onTextMoveClick: () -> Unit = {},
    onTextSpacingClick: () -> Unit = {}, // fix43
    textMenuRequest: Int = 0,
    pageAspectRatio: Float,
    onPageAspectRatioChange: (Float) -> Unit,
    pageCountForSize: Int = 1,
    pageSizeSelected: Set<Int> = emptySet(),
    onTogglePageSizeSelection: (Int) -> Unit = {},
    onToggleSelectAllPagesForSize: () -> Unit = {},
    onClearPageSizeSelection: () -> Unit = {},
    onBeginPageSizeEdit: () -> Unit = {},
    sliderAspectForSelection: Float = pageAspectRatio,
    isPageLandscape: Boolean,
    onPageOrientationChange: (EditorViewModel.PageOrientation) -> Unit,
    pageMarginDp: Int,
    onPageMarginChange: (Int) -> Unit,
    pageBackgroundColor: Long,
    onPageBackgroundColorChange: (Long) -> Unit,
    onPickBackgroundImage: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    hasBackgroundImage: Boolean,
    pageCountForBgColor: Int = 1,
    pageBgColorSelected: Set<Int> = emptySet(),
    onTogglePageBgColorSelection: (Int) -> Unit = {},
    onToggleSelectAllPagesForBgColor: () -> Unit = {},
    onClearPageBgColorSelection: () -> Unit = {},
    onOpenAdvancedBackgroundEditor: () -> Unit = {},
    pageNumberPosition: EditorViewModel.PageNumberPosition,
    onPageNumberPositionChange: (EditorViewModel.PageNumberPosition) -> Unit,
    pageNumberStyle: EditorViewModel.PageNumberStyle,
    onPageNumberStyleChange: (EditorViewModel.PageNumberStyle) -> Unit,
    onAddNewPage: () -> Unit,
    onDeletePage: () -> Unit,
    pageCountForDelete: Int = 1,
    pageDeleteSelected: Set<Int> = emptySet(),
    onTogglePageDeleteSelection: (Int) -> Unit = {},
    onToggleSelectAllPagesForDelete: () -> Unit = {},
    onClearPageDeleteSelection: () -> Unit = {},
    onDeleteSelectedPages: () -> Unit = {},
    pageCountForArrange: Int = 1,
    onMovePage: (Int, Int) -> Unit = { _, _ -> },
    onReorderPages: (List<Int>) -> Unit = {},
    pageCountForDuplicate: Int = 1,
    pageDuplicateSelected: Set<Int> = emptySet(),
    onTogglePageDuplicateSelection: (Int) -> Unit = {},
    onToggleSelectAllPagesForDuplicate: () -> Unit = {},
    onClearPageDuplicateSelection: () -> Unit = {},
    onDuplicateSelectedPages: () -> Unit = {},
    pageCountForNumbering: Int = 1,
    pageNumberingSelected: Set<Int> = emptySet(),
    onTogglePageNumberingSelection: (Int) -> Unit = {},
    onToggleSelectAllPagesForNumbering: () -> Unit = {},
    onClearPageNumberingSelection: () -> Unit = {},
    onStartImageNumbering: () -> Unit = {},
    numberingEditMode: Boolean = false,
    numberingEditStep: Int = 0,
    onNumberingNextStep: () -> Unit = {},
    onNumberingBackStep: () -> Unit = {},
    onNumberingBackToStyle: () -> Unit = {},
    numberingStyleScreen: Boolean = false,
    numberingWeight: Float = 0.85f,
    onNumberingWeightChange: (Float) -> Unit = {},
    onConfirmNumberingStyle: () -> Unit = {},
    onCancelNumberingStyle: () -> Unit = {},
    numberingAlpha: Float = 0.9f,
    onNumberingAlphaChange: (Float) -> Unit = {},
    numberingSizeFrac: Float = 0.18f,
    onNumberingSizeChange: (Float) -> Unit = {},
    onNumberingNudge: (Float, Float) -> Unit = { _, _ -> },
    onNumberingCenter: () -> Unit = {},
    numberingBgArgb: Long = 0xFF7C4DFFL,
    onNumberingBgChange: (Long) -> Unit = {},
    numberingFgArgb: Long = 0xFFFFFFFFL,
    onNumberingFgChange: (Long) -> Unit = {},
    onNumberingDone: () -> Unit = {},
    dragModeActive: Boolean = false,
    dragXPercent: Float = 50f,
    dragYPercent: Float = 50f,
    onDragXPercentChange: (Float) -> Unit = {},
    onDragYPercentChange: (Float) -> Unit = {},
    onDragNudge: (Float, Float) -> Unit = { _, _ -> },
    onDragCenter: () -> Unit = {},
    onDragDone: () -> Unit = {},
    onDragImagesMenuClick: () -> Unit = {},
    onRoundCornersMenuClick: () -> Unit = {},
    onResizeImagesMenuClick: () -> Unit = {},
    onImagesPerRowMenuClick: () -> Unit = {},
    onImageNumberingMenuClick: () -> Unit = {},
    onNumberingStyleBack: (() -> Unit)? = null,
    perRowEditActive: Boolean = false,
    onPerRowDone: () -> Unit = {},
    resizeEditActive: Boolean = false,
    onResizeDone: () -> Unit = {},
    cornersEditActive: Boolean = false,
    onCornersDone: () -> Unit = {},
    onDeleteImagesMenuClick: () -> Unit = {},
    closeMenusSignal: Int = 0,
    onToolMenuOpenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sessionCtx = LocalContext.current
    val sessionUi = remember { SessionStore.load(sessionCtx) }
    var resizeModeActive by remember { mutableStateOf(false) }
    var spacingModeActive by remember { mutableStateOf(false) }
    var shapeModeActive by remember { mutableStateOf(false) }
    var cornersModeActive by remember { mutableStateOf(false) }
    var pageSizeModeActive by remember { mutableStateOf(false) }
    var showPageSizePicker by remember { mutableStateOf(false) }
    var showImageNumberingPicker by remember { mutableStateOf(false) }
    var showDragPagePicker by remember { mutableStateOf(false) }
    var showDragImagePicker by remember { mutableStateOf(false) }
    var dragPickerPage by remember { mutableStateOf(0) }
    var dragPickerSelected by remember { mutableStateOf(setOf<String>()) }
    var showPageBgColorPicker by remember { mutableStateOf(false) }
    var showPageDeletePicker by remember { mutableStateOf(false) }
    // Only one of Page / Image / Text menus open at a time
    var openToolMenu by remember { mutableStateOf(null as String?) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    androidx.compose.runtime.LaunchedEffect(openToolMenu) {
        if (openToolMenu != null) {
            keyboardController?.hide()
        }
    }
    androidx.compose.runtime.LaunchedEffect(closeMenusSignal) {
        if (closeMenusSignal > 0) openToolMenu = null
    }
    BackHandler(enabled = openToolMenu != null) {
        openToolMenu = null
    }
    androidx.compose.runtime.LaunchedEffect(openToolMenu) {
        onToolMenuOpenChange(openToolMenu != null)
    }

    var showPageDuplicatePicker by remember { mutableStateOf(false) }
    var showArrangePagesDialog by remember { mutableStateOf(false) }
    var pageBgColorModeActive by remember { mutableStateOf(false) }
    var pageMarginModeActive by remember { mutableStateOf(false) }
    var textSizeModeActive by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(
        openToolMenu, showPageSizePicker, showImageNumberingPicker, showDragPagePicker,
        showDragImagePicker, showPageBgColorPicker, showPageDeletePicker,
        showPageDuplicatePicker, showArrangePagesDialog, resizeModeActive, perRowEditActive, spacingModeActive,
        shapeModeActive, cornersModeActive, pageSizeModeActive, pageBgColorModeActive,
        pageMarginModeActive, textSizeModeActive
    ) {
        SessionStore.setOpenToolMenu(sessionCtx, openToolMenu)
        val dialog = when {
            showPageSizePicker -> "pageSize"
            showImageNumberingPicker -> "numbering"
            showDragPagePicker -> "dragPages"
            showDragImagePicker -> "dragImages"
            showPageBgColorPicker -> "pageBg"
            showPageDeletePicker -> "pageDelete"
            showPageDuplicatePicker -> "pageDup"
            showArrangePagesDialog -> "arrange"
            else -> ""
        }
        SessionStore.setToolbarDialog(sessionCtx, dialog)
        val mode = when {
            resizeModeActive || perRowEditActive -> "resize"
            spacingModeActive -> "spacing"
            shapeModeActive -> "shape"
            cornersModeActive -> "corners"
            pageSizeModeActive -> "pageSize"
            pageBgColorModeActive -> "pageBgColor"
            pageMarginModeActive -> "pageMargin"
            textSizeModeActive -> "textSize"
            else -> ""
        }
        SessionStore.setEditorMode(sessionCtx, mode)
    }
    LaunchedEffect(hasSelectedText) {
        if (!hasSelectedText) textSizeModeActive = false
    }
    // fix30: Back on Text Box Size / Text Move reopens the Text tools list.
    var handledTextMenuRequest by remember { mutableStateOf(textMenuRequest) }
    LaunchedEffect(textMenuRequest) {
        if (textMenuRequest != handledTextMenuRequest) {
            handledTextMenuRequest = textMenuRequest
            onPinTextSelection()
            openToolMenu = "text"
        }
    }
    var selectedToolCategory by remember { mutableStateOf(0) } // 0=Page 1=Images 2=Text
    var orientationMenuExpanded by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(orientationMenuExpanded) {
        SessionStore.setOrientationOpen(sessionCtx, orientationMenuExpanded)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        when {
            perRowEditActive || resizeModeActive -> ImagesPerRowSlider(
                imagesPerRow = imagesPerRow,
                onImagesPerRowChange = onImagesPerRowSelected,
                onDone = {
                    resizeModeActive = false
                    onPerRowDone()
                }
            )
            dragModeActive -> ImageDragPanel(
                xPercent = dragXPercent,
                yPercent = dragYPercent,
                onXPercentChange = onDragXPercentChange,
                onYPercentChange = onDragYPercentChange,
                onNudge = onDragNudge,
                onCenter = onDragCenter,
                onDone = onDragDone
            )
            numberingEditMode -> ImageNumberingPanel(
                step = numberingEditStep,
                alpha = numberingAlpha,
                onAlphaChange = onNumberingAlphaChange,
                sizeFrac = numberingSizeFrac,
                onSizeChange = onNumberingSizeChange,
                bgArgb = numberingBgArgb,
                fgArgb = numberingFgArgb,
                onNudge = onNumberingNudge,
                onCenter = onNumberingCenter,
                onNext = onNumberingNextStep,
                onBack = {
                    if (numberingEditStep <= 0) onNumberingBackToStyle()
                    else onNumberingBackStep()
                },
                onDone = onNumberingDone
            )
            spacingModeActive -> ImageSpacingSlider(
                spacingDp = imageSpacingDp,
                onSpacingChange = onImageSpacingSelected,
                onDone = { spacingModeActive = false }
            )
            resizeEditActive || shapeModeActive -> ImageShapeSlider(
                aspectRatio = imageCellAspectRatio,
                onAspectRatioChange = onImageCellAspectRatioSelected,
                onDone = {
                    shapeModeActive = false
                    onResizeDone()
                }
            )
            cornersEditActive || cornersModeActive -> RoundCornersSlider(
                percent = imageCornerRadiusPercent,
                onPercentChange = onImageCornerRadiusSelected,
                onDone = {
                    cornersModeActive = false
                    onCornersDone()
                }
            )
            textSizeModeActive -> TextSizeSlider(
                sizeSp = textSizeSp,
                onSizeChange = onTextSizeChange,
                onDone = { textSizeModeActive = false }
            )
            pageBgColorModeActive -> PageBackgroundColorPanel(
                pageBackgroundColor = pageBackgroundColor,
                hasBackgroundImage = hasBackgroundImage,
                onColorSelected = onPageBackgroundColorChange,
                onPickBackgroundImage = {
                    pageBgColorModeActive = false
                    onPickBackgroundImage()
                },
                onClearBackgroundImage = onClearBackgroundImage,
                onDone = { pageBgColorModeActive = false }
            )
            pageSizeModeActive -> PageSizeSlider(
                aspectRatio = sliderAspectForSelection,
                onAspectRatioChange = onPageAspectRatioChange,
                onDone = { pageSizeModeActive = false }
            )
            pageMarginModeActive -> PageMarginSlider(
                marginDp = pageMarginDp,
                onMarginChange = onPageMarginChange,
                onDone = { pageMarginModeActive = false }
            )
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .designBackground(currentMenuDesign().toolbarBg, currentMenuDesign().toolbarBrush) // fix36
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                    PageToolsMenu(
                        menuExpanded = openToolMenu == "page",
                        onMenuExpand = {
                            openToolMenu = if (openToolMenu == "page") null else "page"
                        },
                        onMenuDismiss = { openToolMenu = null },
                        onAddNewPage = onAddNewPage,
                        currentPageCount = pageCountForDelete.coerceAtLeast(1),
                        onDuplicatePages = {
                            onClearPageDuplicateSelection()
                            showPageDuplicatePicker = true
                        },
                        onArrangePages = {
                            showArrangePagesDialog = true
                        },
                        onSetPageSize = {
                            onClearPageSizeSelection()
                            onToggleSelectAllPagesForSize()
                            onBeginPageSizeEdit()
                            showPageSizePicker = true
                        },
                        onSetBackgroundColor = {
                            onClearPageBgColorSelection()
                            // After clear, toggle select-all selects every page
                            onToggleSelectAllPagesForBgColor()
                            showPageBgColorPicker = true
                        },
                        isPageLandscape = isPageLandscape,
                        pageAspectRatio = pageAspectRatio,
                        onPageOrientationChange = onPageOrientationChange,
                        onSetPageMargin = { pageMarginModeActive = true },
                        onDeletePage = onDeletePage,
                        onSetDeletePage = {
                            onClearPageDeleteSelection()
                            showPageDeletePicker = true
                        },
                        pageBackgroundColor = pageBackgroundColor,
                        onPageBackgroundColorChange = onPageBackgroundColorChange,
                        onPickBackgroundImage = onPickBackgroundImage,
                        onClearBackgroundImage = onClearBackgroundImage,
                        hasBackgroundImage = hasBackgroundImage,
                        pageNumberPosition = pageNumberPosition,
                        onPageNumberPositionChange = onPageNumberPositionChange,
                        pageNumberStyle = pageNumberStyle,
                        onPageNumberStyleChange = onPageNumberStyleChange
                    )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                    ImageToolsMenu(
                        menuExpanded = openToolMenu == "image",
                        onMenuExpand = {
                            openToolMenu = if (openToolMenu == "image") null else "image"
                        },
                        onMenuDismiss = { openToolMenu = null },
                        onImportImagesClick = onImportImagesClick,
                        onDragImagesClick = onDragImagesMenuClick,
                        onDeleteImagesClick = onDeleteImagesMenuClick,
                        onResizeImagesClick = onImagesPerRowMenuClick,
                        onAdjustSpacingClick = { spacingModeActive = true },
                        onAdjustImageShapeClick = onResizeImagesMenuClick,
                        onAdjustCornersClick = onRoundCornersMenuClick,
                        onImageNumberingClick = {
                            onImageNumberingMenuClick()
                        }
                    )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                    TextToolsMenu(
                        menuExpanded = openToolMenu == "text",
                        onMenuExpand = {
                            if (openToolMenu != "text") onPinTextSelection()
                            openToolMenu = if (openToolMenu == "text") null else "text"
                        },
                        onMenuDismiss = { openToolMenu = null },
                        onEnterTextClick = onAddTextClick,
                        onFontClick = onFontClick,
                        onTextColorClick = onTextColorClick,
                        onTextBgColorClick = onTextBgColorClick,
                        onTextBorderColorClick = onTextBorderColorClick,
                        onTextSpreadColorClick = onTextSpreadColorClick,
                        onTextShadowClick = onTextShadowClick,
                        onTextSizeClick = {
                            if (hasFocusedText) onTextBoxSizeClick()
                            else onNeedHint("Select a text box first to use this tool.")
                        },
                        onTextMoveClick = {
                            if (hasFocusedText) onTextMoveClick()
                            else onNeedHint("Select a text box first to use this tool.")
                        },
                        onTextSpacingClick = onTextSpacingClick, // fix43
                        onDeleteTextClick = onDeleteTextClick,
                        hasSelectedText = hasSelectedText,
                        hasTextRangeSelection = hasTextRangeSelection,
                        currentTextAlign = currentTextAlign,
                        onTextAlignSelected = onTextAlignSelected,
                        hasFocusedText = hasFocusedText,
                        onNeedHint = onNeedHint,
                        onInsertLinkClick = onInsertLinkClick,
                        onBulletListClick = onBulletListClick,
                        onNumberedListClick = onNumberedListClick,
                        onQuoteClick = onQuoteClick,
                        onDividerClick = onDividerClick
                    )
                    }
                }
            }
        }
        HorizontalDivider(color = currentMenuDesign().toolbarDivider) // fix36
    }

    if (showPageSizePicker) {
        PageSizePickerDialog(
            pageCount = pageCountForSize,
            selectedPages = pageSizeSelected,
            onTogglePage = onTogglePageSizeSelection,
            onToggleSelectAll = onToggleSelectAllPagesForSize,
            onOk = {
                showPageSizePicker = false
                if (pageSizeSelected.isNotEmpty()) {
                    pageSizeModeActive = true
                }
            },
            onDismiss = { showPageSizePicker = false },
            instructionText = stringResource(R.string.page_size_picker_instruction),
            confirmText = stringResource(R.string.page_size_ok)
        )
    }

    if (showPageBgColorPicker) {
        PageSizePickerDialog(
            pageCount = pageCountForBgColor,
            selectedPages = pageBgColorSelected,
            onTogglePage = onTogglePageBgColorSelection,
            onToggleSelectAll = onToggleSelectAllPagesForBgColor,
            onOk = {
                showPageBgColorPicker = false
                if (pageBgColorSelected.isNotEmpty()) {
                    onOpenAdvancedBackgroundEditor()
                }
            },
            onDismiss = { showPageBgColorPicker = false },
            instructionText = stringResource(R.string.page_bg_picker_instruction),
            confirmText = stringResource(R.string.page_size_ok)
        )
    }

    if (showPageDeletePicker) {
        PageSizePickerDialog(
            pageCount = pageCountForDelete,
            selectedPages = pageDeleteSelected,
            onTogglePage = onTogglePageDeleteSelection,
            onToggleSelectAll = onToggleSelectAllPagesForDelete,
            onOk = {
                showPageDeletePicker = false
                if (pageDeleteSelected.isNotEmpty()) {
                    onDeleteSelectedPages()
                }
            },
            onDismiss = { showPageDeletePicker = false },
            instructionText = stringResource(R.string.page_delete_picker_instruction),
            confirmText = stringResource(R.string.page_delete_button)
        )
    }

    if (showArrangePagesDialog) {
        ArrangePagesDialog(
            pageCount = pageCountForArrange,
            onArrange = { from, to ->
                showArrangePagesDialog = false
                onMovePage(from, to)
            },
            onDismiss = { showArrangePagesDialog = false }
        )
    }


    if (showPageDuplicatePicker) {
        PageSizePickerDialog(
            pageCount = pageCountForDuplicate,
            selectedPages = pageDuplicateSelected,
            onTogglePage = onTogglePageDuplicateSelection,
            onToggleSelectAll = onToggleSelectAllPagesForDuplicate,
            onOk = {
                showPageDuplicatePicker = false
                if (pageDuplicateSelected.isNotEmpty()) {
                    onDuplicateSelectedPages()
                }
            },
            onDismiss = { showPageDuplicatePicker = false },
            instructionText = stringResource(R.string.page_duplicate_picker_instruction),
            confirmText = stringResource(R.string.page_duplicate_button)
        )
    }


    if (numberingStyleScreen) {
        ImageNumberingStyleScreen(
            bgArgb = numberingBgArgb,
            onBgChange = onNumberingBgChange,
            fgArgb = numberingFgArgb,
            onFgChange = onNumberingFgChange,
            weight = numberingWeight,
            onWeightChange = onNumberingWeightChange,
            onNext = onConfirmNumberingStyle,
            onDismiss = {
                onCancelNumberingStyle()
                val back = onNumberingStyleBack
                if (back != null) {
                    back()
                } else {
                    showImageNumberingPicker = true
                }
            }
        )
    }

    if (showImageNumberingPicker) {
        PageSizePickerDialog(
            pageCount = pageCountForNumbering,
            selectedPages = pageNumberingSelected,
            onTogglePage = onTogglePageNumberingSelection,
            onToggleSelectAll = onToggleSelectAllPagesForNumbering,
            onOk = {
                if (pageNumberingSelected.isNotEmpty()) {
                    showImageNumberingPicker = false
                    onStartImageNumbering()
                }
            },
            onDismiss = { showImageNumberingPicker = false },
            instructionText = stringResource(R.string.image_numbering_instruction),
            confirmText = stringResource(R.string.image_numbering_next)
        )
    }

}

@Composable
private fun OrientationOptionRow(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    MenuItemFrame(onClick = onClick) { // fix35
        MenuItemText(icon + "  " + label)
        Spacer(modifier = Modifier.weight(1f))
        MenuItemText(if (selected) "✓" else "", modifier = Modifier.padding(start = 12.dp))
    }
}


@Composable
private fun CategoryTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(28.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(1.dp)
                )
        )
    }
}

@Composable
private fun ToolChip(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (enabled) Color.Black else Color.Gray,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .background(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(20.dp)
            )
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}




@Composable
private fun CategoryMenuLabel(
    text: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    MenuDesignHeader(text = text, expanded = expanded, onClick = onClick) // fix35
}

@Composable
private fun ToolMenuItem(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    // fix35: drawn with the chosen MenuDesign (compact, the full list fits without scroll).
    MenuItemFrame(enabled = enabled, onClick = onClick) {
        MenuItemText(label, enabled = enabled)
    }
}

@Composable
private fun BloggerToolRow(kind: String, label: String, onClick: () -> Unit) {
    val menuDesign = currentDropdownDesign() // fix40: dropdown design
    val iconColor = menuDesign.accent
    val iconArgb = menuDesign.iconArgb()
    MenuItemFrame(onClick = onClick) {
        Canvas(modifier = Modifier.size(17.dp)) {
            val w = size.width
            val h = size.height
            val stroke = (h * 0.08f).coerceAtLeast(1.6f)
            when (kind) {
                "bullet" -> {
                    val dotsX = w * 0.14f
                    val lineL = w * 0.32f
                    val lineR = w * 0.96f
                    val ys = listOf(h * 0.22f, h * 0.50f, h * 0.78f)
                    ys.forEach { y ->
                        drawCircle(color = iconColor, radius = h * 0.07f, center = Offset(dotsX, y))
                        drawLine(iconColor, Offset(lineL, y), Offset(lineR, y), strokeWidth = stroke)
                    }
                }
                "number" -> {
                    val paint = android.graphics.Paint().apply {
                        color = iconArgb
                        textSize = h * 0.34f
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    val lineL = w * 0.40f
                    val lineR = w * 0.96f
                    val labels = listOf("1", "2", "3")
                    val ys = listOf(h * 0.22f, h * 0.50f, h * 0.78f)
                    drawIntoCanvas { canvas ->
                        val nc = canvas.nativeCanvas
                        labels.zip(ys).forEach { (lab, y) ->
                            nc.drawText(lab, 0f, y + h * 0.12f, paint)
                        }
                    }
                    ys.forEach { y ->
                        drawLine(iconColor, Offset(lineL, y), Offset(lineR, y), strokeWidth = stroke)
                    }
                }
                "quote" -> {
                    val paint = android.graphics.Paint().apply {
                        color = iconArgb
                        textSize = h * 0.95f
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText("\u201D", w * 0.08f, h * 0.82f, paint)
                    }
                }
                else -> {
                    drawLine(
                        iconColor,
                        Offset(w * 0.08f, h * 0.50f),
                        Offset(w * 0.92f, h * 0.50f),
                        strokeWidth = stroke * 1.35f
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        MenuItemText(label)
    }
}


@Composable
private fun ImageToolsMenu(
    onImportImagesClick: () -> Unit,
    onDragImagesClick: () -> Unit = {},
    onDeleteImagesClick: () -> Unit = {},
    onResizeImagesClick: () -> Unit,
    onAdjustSpacingClick: () -> Unit,
    onAdjustImageShapeClick: () -> Unit,
    onAdjustCornersClick: () -> Unit,
    onImageNumberingClick: () -> Unit = {},
    menuExpanded: Boolean = false,
    onMenuExpand: () -> Unit = {},
    onMenuDismiss: () -> Unit = {}
) {
    Box {
        CategoryMenuLabel(
            text = stringResource(R.string.image_tools_menu_entry),
            expanded = menuExpanded,
            onClick = onMenuExpand
        )

        DesignedDropdownMenu(
            expanded = menuExpanded,
            menuId = DropdownPlacement.IMAGE, // fix44
            onDismissRequest = onMenuDismiss,
            properties = PopupProperties(
                focusable = false,
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            )
        ) {
            ToolMenuItem(stringResource(R.string.import_images)) {
                onMenuDismiss()
                onImportImagesClick()
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.bl_title)) { // fix55
                onMenuDismiss()
                BorderLineTool.mode = 1
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.resize_images_tool)) {
                onMenuDismiss()
                onResizeImagesClick()
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.image_spacing_tool)) {
                onMenuDismiss()
                onAdjustSpacingClick()
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.image_shape_tool)) {
                onMenuDismiss()
                onAdjustImageShapeClick()
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.round_corners_tool)) {
                onMenuDismiss()
                onAdjustCornersClick()
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.image_numbering_tool)) {
                onMenuDismiss()
                onImageNumberingClick()
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.image_drag_tool)) {
                onMenuDismiss()
                onDragImagesClick()
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.image_delete_tool)) {
                onMenuDismiss()
                onDeleteImagesClick()
            }
        }
    }
}

@Composable
private fun PageToolsMenu(
    onAddNewPage: () -> Unit,
    currentPageCount: Int = 1,
    onDuplicatePages: () -> Unit,
    onArrangePages: () -> Unit,
    onSetPageSize: () -> Unit,
    onSetBackgroundColor: () -> Unit,
    isPageLandscape: Boolean,
    pageAspectRatio: Float,
    onPageOrientationChange: (EditorViewModel.PageOrientation) -> Unit,
    onSetPageMargin: () -> Unit,
    onDeletePage: () -> Unit,
    onSetDeletePage: () -> Unit,
    pageBackgroundColor: Long,
    onPageBackgroundColorChange: (Long) -> Unit,
    onPickBackgroundImage: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    hasBackgroundImage: Boolean,
    pageCountForBgColor: Int = 1,
    pageBgColorSelected: Set<Int> = emptySet(),
    onTogglePageBgColorSelection: (Int) -> Unit = {},
    onToggleSelectAllPagesForBgColor: () -> Unit = {},
    onClearPageBgColorSelection: () -> Unit = {},
    pageNumberPosition: EditorViewModel.PageNumberPosition,
    onPageNumberPositionChange: (EditorViewModel.PageNumberPosition) -> Unit,
    pageNumberStyle: EditorViewModel.PageNumberStyle,
    onPageNumberStyleChange: (EditorViewModel.PageNumberStyle) -> Unit,
    menuExpanded: Boolean = false,
    onMenuExpand: () -> Unit = {},
    onMenuDismiss: () -> Unit = {}
) {
    var orientationSub by remember { mutableStateOf(false) }
    var backgroundSub by remember { mutableStateOf(false) }

    // Default / primary palette (3rd item = light gray 0xFFE0E0E0 is default selected in ViewModel)
    val primaryColors = listOf(
        0xFFFFFFFFL, // 1 white
        0xFFF5F5F5L, // 2 light gray
        0xFFE0E0E0L, // 3 medium light gray (DEFAULT)
        0xFFFFF8E1L, // 4 warm
        0xFFE3F2FDL, // 5 light blue
        0xFFE8F5E9L, // 6 light green
        0xFFFFEBEEL, // 7 light pink
        0xFF000000L  // 8 black
    )
    val extraColors = listOf(
        0xFFFFCDD2L, 0xFFF8BBD0L, 0xFFE1BEE7L, 0xFFD1C4E9L,
        0xFFC5CAE9L, 0xFFBBDEFBL, 0xFFB3E5FCL, 0xFFB2EBF2L,
        0xFFB2DFDBL, 0xFFC8E6C9L, 0xFFDCEDC8L, 0xFFF0F4C3L,
        0xFFFFF9C4L, 0xFFFFECB3L, 0xFFFFE0B2L, 0xFFFFCCBCL,
        0xFFD7CCC8L, 0xFFCFD8DCL, 0xFF90A4AEL, 0xFF607D8BL
    )

    Box {
        CategoryMenuLabel(
            text = stringResource(R.string.page_tools_menu_entry),
            expanded = menuExpanded,
            onClick = onMenuExpand
        )

        DesignedDropdownMenu(
            expanded = menuExpanded,
            menuId = DropdownPlacement.PAGE, // fix44
            onDismissRequest = {
                onMenuDismiss()
                orientationSub = false
                backgroundSub = false
            },
            properties = PopupProperties(
                focusable = false,
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            )
        ) {
            // 1. Add New Page + live page count badge
            MenuItemFrame(onClick = { onAddNewPage() }) { // fix35
                MenuItemText(stringResource(R.string.page_tool_add_new_page), modifier = Modifier.weight(1f))
                MenuItemText("(" + currentPageCount + ")", secondary = true)
            }
            MenuDesignDivider()

            // 2. Duplicate Pages
            ToolMenuItem(stringResource(R.string.page_tool_duplicate_pages)) {
                onMenuDismiss()
                onDuplicatePages()
            }
            MenuDesignDivider()

            // 3. Arrange Pages
            ToolMenuItem(stringResource(R.string.page_tool_arrange_pages)) {
                onMenuDismiss()
                onArrangePages()
            }
            MenuDesignDivider()

            // 4. Page Size
            ToolMenuItem(stringResource(R.string.page_tool_set_page_size)) {
                onMenuDismiss()
                onSetPageSize()
            }
            MenuDesignDivider()

            // 5. Page Layout (orientation)
            ToolMenuItem(stringResource(R.string.page_tool_orientation) + " ›") {
                orientationSub = !orientationSub
                if (orientationSub) {
                    backgroundSub = false
                }
            }
            if (orientationSub) {
                val isNormal = kotlin.math.abs(pageAspectRatio - 0.589f) < 0.03f
                val isSquare = kotlin.math.abs(pageAspectRatio - 1f) < 0.05f
                val isLand = pageAspectRatio > 1.05f
                val isPort = !isNormal && pageAspectRatio < 0.95f && !isSquare

                OrientationOptionRow(
                    icon = "▬",
                    label = stringResource(R.string.page_orientation_normal),
                    selected = isNormal,
                    onClick = {
                        onPageOrientationChange(EditorViewModel.PageOrientation.NORMAL)
                    }
                )
                OrientationOptionRow(
                    icon = "▯",
                    label = stringResource(R.string.page_orientation_portrait) + " (9:16)",
                    selected = isPort,
                    onClick = {
                        onPageOrientationChange(EditorViewModel.PageOrientation.PORTRAIT)
                    }
                )
                OrientationOptionRow(
                    icon = "▭",
                    label = stringResource(R.string.page_orientation_landscape) + " (16:9)",
                    selected = isLand,
                    onClick = {
                        onPageOrientationChange(EditorViewModel.PageOrientation.LANDSCAPE)
                    }
                )
                OrientationOptionRow(
                    icon = "□",
                    label = stringResource(R.string.page_orientation_square) + " (1:1)",
                    selected = isSquare,
                    onClick = {
                        onPageOrientationChange(EditorViewModel.PageOrientation.SQUARE)
                    }
                )
            }
            MenuDesignDivider()


            // 7. Background
            ToolMenuItem(stringResource(R.string.page_tool_background_color)) {
                onMenuDismiss()
                backgroundSub = false
                orientationSub = false
                onSetBackgroundColor()
            }
            MenuDesignDivider()

            // 8. Delete Page
            ToolMenuItem(stringResource(R.string.page_tool_delete_page)) {
                onMenuDismiss()
                onSetDeletePage()
            }

        }
    }
}

@Composable
private fun TextToolsMenu(
    onEnterTextClick: () -> Unit,
    onFontClick: () -> Unit,
    onTextSizeClick: () -> Unit,
    onTextMoveClick: () -> Unit = {},
    onTextSpacingClick: () -> Unit = {}, // fix43
    onTextColorClick: () -> Unit,
    onTextBgColorClick: () -> Unit,
    onTextShadowClick: () -> Unit,
    onTextBorderColorClick: () -> Unit = {},
    onTextSpreadColorClick: () -> Unit = {},
    onDeleteTextClick: () -> Unit,
    onInsertLinkClick: () -> Unit = {},
    onBulletListClick: () -> Unit = {},
    onNumberedListClick: () -> Unit = {},
    onQuoteClick: () -> Unit = {},
    onDividerClick: () -> Unit = {},
    hasSelectedText: Boolean,
    hasTextRangeSelection: Boolean = false,
    currentTextAlign: String = "LEFT",
    onTextAlignSelected: (String) -> Unit = {},
    hasFocusedText: Boolean = false,
    onNeedHint: (String) -> Unit = {},
    menuExpanded: Boolean = false,
    onMenuExpand: () -> Unit = {},
    onMenuDismiss: () -> Unit = {}
) {
    var alignOpen by remember { mutableStateOf(false) }
    LaunchedEffect(menuExpanded) { if (!menuExpanded) alignOpen = false }

    // fix35: no height cap and no scroll; compact MenuDesign items keep the whole
    // list right under the Text Tools button.
    Box {
        CategoryMenuLabel(
            text = stringResource(R.string.text_tools_menu_entry),
            expanded = menuExpanded,
            onClick = onMenuExpand
        )

        DesignedDropdownMenu(
            expanded = menuExpanded,
            menuId = DropdownPlacement.TEXT, // fix44
            onDismissRequest = onMenuDismiss,
            properties = PopupProperties(
                focusable = false,
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            )
        ) {
            val hintBox = stringResource(R.string.text_hint_select_box)
            val hintText = stringResource(R.string.text_hint_select_text)
            ToolMenuItem(stringResource(R.string.text_tool_enter_text)) {
                onMenuDismiss()
                onEnterTextClick()
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.bl_title)) { // fix55
                onMenuDismiss()
                BorderLineTool.mode = 2
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.text_tool_font)) {
                if (!hasSelectedText) onNeedHint(hintBox) else { // whole box or range
                    onMenuDismiss()
                    onFontClick()
                }
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.text_tool_text_size)) {
                if (!hasFocusedText) onNeedHint(hintBox) else {
                    onMenuDismiss()
                    onTextSizeClick() // fix33: Move+Rotate+Size panel
                }
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.text_spacing)) { // fix43
                if (!hasFocusedText) onNeedHint(hintBox) else {
                    onMenuDismiss()
                    onTextSpacingClick()
                }
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.text_tool_text_color)) {
                if (!hasSelectedText) onNeedHint(hintBox) else { // whole box or range
                    onMenuDismiss()
                    onTextColorClick()
                }
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.text_tool_border_color)) {
                if (!hasSelectedText) onNeedHint(hintBox) else {
                    onMenuDismiss()
                    onTextBorderColorClick()
                }
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.text_tool_spread_color)) {
                if (!hasSelectedText) onNeedHint(hintBox) else { // whole box or range
                    onMenuDismiss()
                    onTextSpreadColorClick()
                }
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.text_tool_bg_color)) {
                if (!hasSelectedText) onNeedHint(hintBox) else { // whole box or range
                    onMenuDismiss()
                    onTextBgColorClick()
                }
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.text_tool_shadow)) {
                if (!hasSelectedText) onNeedHint(hintBox) else { // whole box or range
                    onMenuDismiss()
                    onTextShadowClick()
                }
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.text_tool_align)) {
                if (!hasFocusedText) onNeedHint(hintBox) else alignOpen = !alignOpen
            }
            if (alignOpen && hasFocusedText) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .background(currentDropdownDesign().alignRowBg, RoundedCornerShape(10.dp)), // fix40
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("LEFT", "CENTER", "RIGHT", "JUSTIFY").forEach { kind ->
                        AlignGlyph(
                            kind = kind,
                            selected = currentTextAlign.equals(kind, true),
                            enabled = true,
                            onClick = { onTextAlignSelected(kind) }
                        )
                    }
                }
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.text_tool_insert_link)) {
                if (!hasSelectedText) onNeedHint(hintBox) else { // whole box or range
                    onMenuDismiss()
                    onInsertLinkClick()
                }
            }
            MenuDesignDivider()
            ToolMenuItem(stringResource(R.string.text_tool_delete)) {
                if (!hasFocusedText) onNeedHint(hintBox) else {
                    onMenuDismiss()
                    onDeleteTextClick()
                }
            }
            MenuDesignDivider()
            val hintSel = stringResource(R.string.text_hint_select_text_short)
            BloggerToolRow(kind = "bullet", label = stringResource(R.string.text_tool_bullet_list)) {
                // Range selected: only that range; box only: whole box text.
                if (!hasFocusedText && !hasTextRangeSelection) onNeedHint(hintBox) else {
                    onMenuDismiss()
                    onBulletListClick()
                }
            }
            MenuDesignDivider()
            BloggerToolRow(kind = "number", label = stringResource(R.string.text_tool_numbered_list)) {
                // Range selected: only that range; box only: whole box text.
                if (!hasFocusedText && !hasTextRangeSelection) onNeedHint(hintBox) else {
                    onMenuDismiss()
                    onNumberedListClick()
                }
            }
            MenuDesignDivider()
            BloggerToolRow(kind = "quote", label = stringResource(R.string.text_tool_quote)) {
                // Range selected: only that range; box only: whole box text.
                if (!hasFocusedText && !hasTextRangeSelection) onNeedHint(hintBox) else {
                    onMenuDismiss()
                    onQuoteClick()
                }
            }
            MenuDesignDivider()
            BloggerToolRow(kind = "divider", label = stringResource(R.string.text_tool_divider)) {
                // Range selected: only that range; box only: whole box text.
                if (!hasFocusedText && !hasTextRangeSelection) onNeedHint(hintBox) else {
                    onMenuDismiss()
                    onDividerClick()
                }
            }
        }
    }
}

@Composable
private fun SliderWithValueLabel(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    labelFormatter: (Float) -> String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val trackInset = 14.dp
        val usableWidth = (maxWidth - trackInset * 2)
        val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
            .coerceIn(0f, 1f)
        val bubbleCenterX = trackInset + usableWidth * fraction

        Text(
            text = labelFormatter(value),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier
                .offset(x = bubbleCenterX - 14.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SliderToolHeader(title: String, onDone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = Color.Black
        )
        Text(
            text = stringResource(R.string.resize_images_done),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onDone)
        )
    }
}

@Composable
private fun ImagesPerRowSlider(
    imagesPerRow: Int,
    onImagesPerRowChange: (Int) -> Unit,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SliderToolHeader(title = stringResource(R.string.resize_images_title), onDone = onDone)
        SliderWithValueLabel(
            value = imagesPerRow.toFloat(),
            onValueChange = { onImagesPerRowChange(it.roundToInt().coerceIn(1, 20)) },
            valueRange = 1f..20f,
            steps = 18,
            labelFormatter = { "${it.roundToInt()}" }
        )
    }
}





@Composable
private fun ImageDragPanel(
    xPercent: Float,
    yPercent: Float,
    onXPercentChange: (Float) -> Unit,
    onYPercentChange: (Float) -> Unit,
    onNudge: (Float, Float) -> Unit,
    onCenter: () -> Unit,
    onDone: () -> Unit
) {
    var xText by remember(xPercent) { mutableStateOf(String.format("%.2f", xPercent)) }
    var yText by remember(yPercent) { mutableStateOf(String.format("%.2f", yPercent)) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NumberingNudgeButton(label = "\u2191", onTick = { onNudge(0f, -1f) })
            Row(horizontalArrangement = Arrangement.Center) {
                NumberingNudgeButton(label = "\u2190", onTick = { onNudge(-1f, 0f) })
                NumberingNudgeButton(label = "\u25CF", onTick = onCenter)
                NumberingNudgeButton(label = "\u2192", onTick = { onNudge(1f, 0f) })
            }
            NumberingNudgeButton(label = "\u2193", onTick = { onNudge(0f, 1f) })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("X", style = MaterialTheme.typography.labelSmall, color = Color.Black)
            OutlinedTextField(
                value = xText,
                onValueChange = { input ->
                    val f = input.filter { c -> c.isDigit() || c == '.' }.let { s ->
                        val parts = s.split('.')
                        if (parts.size <= 1) s.take(3)
                        else parts[0].take(3) + "." + parts.getOrElse(1) { "" }.take(2)
                    }
                    xText = f
                    f.toFloatOrNull()?.let { if (it in 0f..100f) onXPercentChange(it) }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.labelMedium
            )
            Text("%", style = MaterialTheme.typography.labelMedium, color = Color.Black)
            Text("Y", style = MaterialTheme.typography.labelSmall, color = Color.Black)
            OutlinedTextField(
                value = yText,
                onValueChange = { input ->
                    val f = input.filter { c -> c.isDigit() || c == '.' }.let { s ->
                        val parts = s.split('.')
                        if (parts.size <= 1) s.take(3)
                        else parts[0].take(3) + "." + parts.getOrElse(1) { "" }.take(2)
                    }
                    yText = f
                    f.toFloatOrNull()?.let { if (it in 0f..100f) onYPercentChange(it) }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.labelMedium
            )
            Text("%", style = MaterialTheme.typography.labelMedium, color = Color.Black)
            TextButton(
                onClick = onDone,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    stringResource(R.string.image_drag_done),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Composable
private fun DragNudgeChip(label: String, onTick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        onTick()
        while (pressed) {
            kotlinx.coroutines.delay(55)
            onTick()
        }
    }
    Text(
        text = label,
        color = Color.Black,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .padding(2.dp)
            .background(Color(0xFFF0F0F0L), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try { tryAwaitRelease() } finally { pressed = false }
                    }
                )
            }
    )
}

@Composable
private fun ImageNumberingPanel(
    step: Int,
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    sizeFrac: Float,
    onSizeChange: (Float) -> Unit,
    bgArgb: Long,
    fgArgb: Long,
    onNudge: (Float, Float) -> Unit,
    onCenter: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        if (step <= 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.image_numbering_transparency),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(alpha * 100f).toInt()}%",
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Slider(
                value = alpha,
                onValueChange = onAlphaChange,
                valueRange = 0.2f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp, bottom = 4.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.image_numbering_size),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(sizeFrac * 100f).toInt()}%",
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Slider(
                value = sizeFrac,
                onValueChange = onSizeChange,
                valueRange = 0.08f..0.35f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Back", color = Color.Black)
                }
                TextButton(onClick = onNext, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(
                        stringResource(R.string.image_numbering_next),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NumberingNudgeButton(label = "↑", onTick = { onNudge(0f, -1f) })
                Row(horizontalArrangement = Arrangement.Center) {
                    NumberingNudgeButton(label = "←", onTick = { onNudge(-1f, 0f) })
                    NumberingNudgeButton(label = "●", onTick = onCenter)
                    NumberingNudgeButton(label = "→", onTick = { onNudge(1f, 0f) })
                }
                NumberingNudgeButton(label = "↓", onTick = { onNudge(0f, 1f) })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Back", color = Color.Black)
                }
                TextButton(onClick = onDone, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(
                        stringResource(R.string.image_numbering_done),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


@Composable
private fun NumberingColorRow(
    colors: List<Long>,
    selected: Long,
    onSelect: (Long) -> Unit,
    showMoreLabel: Boolean,
    moreExpanded: Boolean,
    onToggleMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { col ->
            val isSel = (col and 0x00FFFFFFL) == (selected and 0x00FFFFFFL) || col == selected
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(col.toInt()), CircleShape)
                    .border(
                        width = if (isSel) 2.dp else 1.dp,
                        color = if (isSel) Color(0xFF1976D2L) else Color.LightGray,
                        shape = CircleShape
                    )
                    .clickable { onSelect(col) }
            )
        }
        if (showMoreLabel) {
            Text(
                text = stringResource(R.string.image_numbering_more),
                color = Color(0xFF1976D2L),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clickable(onClick = onToggleMore)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun NumberingNudgeButton(
    label: String,
    onTick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        onTick()
        while (pressed) {
            kotlinx.coroutines.delay(55)
            onTick()
        }
    }
    Text(
        text = label,
        color = Color.Black,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .padding(8.dp)
            .background(Color(0xFFF0F0F0L), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                        }
                    }
                )
            }
    )
}


@Composable
private fun ImageSpacingSlider(
    spacingDp: Int,
    onSpacingChange: (Int) -> Unit,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SliderToolHeader(title = stringResource(R.string.image_spacing_dialog_title), onDone = onDone)
        SliderWithValueLabel(
            value = spacingDp.toFloat(),
            onValueChange = { onSpacingChange(it.roundToInt().coerceIn(0, 20)) },
            valueRange = 0f..20f,
            steps = 0,
            labelFormatter = { "${it.roundToInt()} dp" }
        )
    }
}

private const val SHAPE_MIN_RATIO = 0.3f
private const val SHAPE_MAX_RATIO = 2.0f

private fun shapeRatioToPercent(ratio: Float): Int {
    val t = ((ratio - SHAPE_MIN_RATIO) / (SHAPE_MAX_RATIO - SHAPE_MIN_RATIO)).coerceIn(0f, 1f)
    // Map 0..1 -> 1..100 so the control shows 1..100
    return (1 + t * 99f).roundToInt().coerceIn(1, 100)
}

private fun shapePercentToRatio(percent: Int): Float {
    // UI range 1..100 (not 0..100)
    val t = (percent.coerceIn(1, 100) - 1) / 99f
    return SHAPE_MIN_RATIO + t * (SHAPE_MAX_RATIO - SHAPE_MIN_RATIO)
}

@Composable
private fun ImageShapeSlider(
    aspectRatio: Float,
    onAspectRatioChange: (Float) -> Unit,
    onDone: () -> Unit
) {
    var percentText by remember(aspectRatio) {
        mutableStateOf(shapeRatioToPercent(aspectRatio).toString())
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SliderToolHeader(title = stringResource(R.string.image_shape_title), onDone = onDone)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SliderWithValueLabel(
                value = aspectRatio,
                onValueChange = onAspectRatioChange,
                valueRange = SHAPE_MIN_RATIO..SHAPE_MAX_RATIO,
                steps = 0,
                labelFormatter = { "${shapeRatioToPercent(it)}%" },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedTextField(
                value = percentText,
                onValueChange = { input ->
                    val filtered = input.filter(Char::isDigit).take(3)
                    percentText = filtered
                    val percent = filtered.toIntOrNull()
                    if (percent != null && percent in 1..100) {
                        onAspectRatioChange(shapePercentToRatio(percent))
                    }
                },
                modifier = Modifier.width(64.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RoundCornersSlider(
    percent: Int,
    onPercentChange: (Int) -> Unit,
    onDone: () -> Unit
) {
    var percentText by remember(percent) { mutableStateOf(percent.toString()) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SliderToolHeader(title = stringResource(R.string.round_corners_title), onDone = onDone)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SliderWithValueLabel(
                value = percent.toFloat(),
                onValueChange = { onPercentChange(it.roundToInt()) },
                valueRange = 0f..100f,
                steps = 0,
                labelFormatter = { "${it.roundToInt()}%" },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedTextField(
                value = percentText,
                onValueChange = { input ->
                    val filtered = input.filter(Char::isDigit).take(3)
                    percentText = filtered
                    val value = filtered.toIntOrNull()
                    if (value != null && value in 0..100) {
                        onPercentChange(value)
                    }
                },
                modifier = Modifier.width(64.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}




@Composable
private fun ArrangePagesDialog(
    pageCount: Int,
    onArrange: (from: Int, to: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val count = pageCount.coerceAtLeast(1)
    var sourcePage by remember { mutableStateOf<Int?>(null) }
    var destPage by remember { mutableStateOf<Int?>(null) }
    val step = if (sourcePage == null) 1 else 2

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.page_arrange_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (step == 1)
                    stringResource(R.string.page_arrange_step1)
                else
                    stringResource(R.string.page_arrange_step2),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
            if (sourcePage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.page_arrange_source_label, (sourcePage ?: 0) + 1),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                for (i in 0 until count) {
                    val isSource = sourcePage == i
                    val isDest = destPage == i
                    val selected = isSource || isDest
                    val bg = when {
                        isSource -> Color(0xFF1565C0)
                        isDest -> Color(0xFF2E7D32)
                        else -> Color(0xFFF5F5F5)
                    }
                    val fg = if (isSource || isDest) Color.White else Color.Black
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(bg, RoundedCornerShape(10.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) bg else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                if (step == 1) {
                                    sourcePage = i
                                    destPage = null
                                } else {
                                    if (i != sourcePage) destPage = i
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.page_arrange_page_item, i + 1),
                            style = MaterialTheme.typography.bodyLarge,
                            color = fg,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSource) {
                            Text("Source", color = fg, style = MaterialTheme.typography.labelMedium)
                        } else if (isDest) {
                            Text("Destination", color = fg, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.page_arrange_cancel))
                }
                TextButton(
                    onClick = {
                        val s = sourcePage
                        val d = destPage
                        if (s != null && d != null) onArrange(s, d)
                    },
                    enabled = sourcePage != null && destPage != null
                ) {
                    Text(stringResource(R.string.page_arrange_button))
                }
            }
        }
    }
}

@Composable
private fun PageSizePickerDialog(
    pageCount: Int,
    selectedPages: Set<Int>,
    onTogglePage: (Int) -> Unit,
    onToggleSelectAll: () -> Unit,
    onOk: () -> Unit,
    onDismiss: () -> Unit,
    instructionText: String = "",
    confirmText: String = ""
) {
    val count = pageCount.coerceAtLeast(1)
    val allSelected = selectedPages.size >= count &&
        (0 until count).all { it in selectedPages }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            // Instruction: large + bold (all 3 tools)
            Text(
                text = instructionText.ifEmpty { stringResource(R.string.page_size_picker_instruction) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Select All only (top left) — outlined
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onToggleSelectAll) {
                    Text(
                        text = stringResource(R.string.page_size_select_all),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Page list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                for (i in 0 until count) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePage(i) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = i in selectedPages,
                            onCheckedChange = { onTogglePage(i) },
                            modifier = Modifier.padding(0.dp)
                        )
                        Text(
                            text = stringResource(R.string.page_size_page_item, i + 1),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // OK / Delete: bottom-right, outlined (like Select All), blue text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOk,
                    enabled = selectedPages.isNotEmpty()
                ) {
                    Text(
                        text = confirmText.ifEmpty { stringResource(R.string.page_size_ok) },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1976D2L)
                    )
                }
            }
        }
    }
}


@Composable
private fun PageBackgroundColorPanel(
    pageBackgroundColor: Long,
    hasBackgroundImage: Boolean,
    onColorSelected: (Long) -> Unit,
    onPickBackgroundImage: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    onDone: () -> Unit
) {
    val primaryColors = listOf(
        0xFFFFFFFFL, 0xFFF5F5F5L, 0xFFE0E0E0L, 0xFFFFF8E1L,
        0xFFE3F2FDL, 0xFFE8F5E9L, 0xFFFFEBEEL, 0xFF000000L
    )
    val extraColors = listOf(
        0xFFFFCDD2L, 0xFFF8BBD0L, 0xFFE1BEE7L, 0xFFD1C4E9L,
        0xFFC5CAE9L, 0xFFBBDEFBL, 0xFFB3E5FCL, 0xFFB2EBF2L,
        0xFFB2DFDBL, 0xFFC8E6C9L, 0xFFDCEDC8L, 0xFFF0F4C3L,
        0xFFFFF9C4L, 0xFFFFECB3L, 0xFFFFE0B2L, 0xFFFFCCBCL,
        0xFFD7CCC8L, 0xFFCFD8DCL, 0xFF90A4AEL, 0xFF607D8BL
    )
    val allColors = primaryColors + extraColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SliderToolHeader(
            title = stringResource(R.string.page_tool_background_color_title),
            onDone = onDone
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onPickBackgroundImage) {
            Text(
                text = stringResource(R.string.page_bg_import_gallery),
                color = Color.Black
            )
        }
        if (hasBackgroundImage) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onClearBackgroundImage) {
                Text(stringResource(R.string.page_bg_clear_image))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            allColors.chunked(8).forEach { row ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { c ->
                        val selected = pageBackgroundColor == c && !hasBackgroundImage
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(c), CircleShape)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(c) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageSizeSlider(
    aspectRatio: Float,
    onAspectRatioChange: (Float) -> Unit,
    onDone: () -> Unit
) {
    fun ratioToPercent(ratio: Float): Int {
        val t = ((ratio - 0.4f) / (8f - 0.4f)).coerceIn(0f, 1f) // PAGE_SIZE_MAX
        return (t * 100f).roundToInt()
    }
    fun percentToRatio(percent: Int): Float {
        val t = percent.coerceIn(0, 100) / 100f
        return 0.4f + t * (8f - 0.4f)
    }

    var percentText by remember(aspectRatio) {
        mutableStateOf(ratioToPercent(aspectRatio).toString())
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SliderToolHeader(
            title = stringResource(R.string.page_tool_set_page_size_title),
            onDone = onDone
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SliderWithValueLabel(
                value = aspectRatio,
                onValueChange = { newRatio ->
                    onAspectRatioChange(newRatio)
                    percentText = ratioToPercent(newRatio).toString()
                },
                valueRange = 0.4f..8f,
                steps = 0,
                labelFormatter = { ratio ->
                    val w = 100
                    val h = (100f / ratio).roundToInt().coerceAtLeast(1)
                    if (ratio >= 1f) {
                        "Landscape " + w + ":" + h
                    } else {
                        "Portrait " + h + ":" + w
                    }
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedTextField(
                value = percentText,
                onValueChange = { input ->
                    val filtered = input.filter(Char::isDigit).take(3)
                    percentText = filtered
                    val percent = filtered.toIntOrNull()
                    if (percent != null && percent in 0..100) {
                        onAspectRatioChange(percentToRatio(percent))
                    }
                },
                modifier = Modifier.width(64.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PageMarginSlider(
    marginDp: Int,
    onMarginChange: (Int) -> Unit,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SliderToolHeader(title = stringResource(R.string.page_tool_margin), onDone = onDone)
        SliderWithValueLabel(
            value = marginDp.toFloat(),
            onValueChange = { onMarginChange(it.roundToInt().coerceIn(0, 48)) },
            valueRange = 0f..48f,
            steps = 0,
            labelFormatter = { "${it.roundToInt()} dp" }
        )
    }
}

@Composable
private fun TextSizeSlider(
    sizeSp: Float,
    onSizeChange: (Float) -> Unit,
    onDone: () -> Unit
) {
    var sizeText by remember(sizeSp) { mutableStateOf(sizeSp.roundToInt().toString()) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SliderToolHeader(title = stringResource(R.string.text_tool_text_size_title), onDone = onDone)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SliderWithValueLabel(
                value = sizeSp,
                onValueChange = {
                    onSizeChange(it)
                    sizeText = it.roundToInt().toString()
                },
                valueRange = 6f..96f,
                steps = 0,
                labelFormatter = { "${it.roundToInt()} sp" },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedTextField(
                value = sizeText,
                onValueChange = { input ->
                    val filtered = input.filter(Char::isDigit).take(3)
                    sizeText = filtered
                    val v = filtered.toIntOrNull()
                    if (v != null && v in 8..72) {
                        onSizeChange(v.toFloat())
                    }
                },
                modifier = Modifier.width(64.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ImageNumberingStyleScreen(
    bgArgb: Long,
    onBgChange: (Long) -> Unit,
    fgArgb: Long,
    onFgChange: (Long) -> Unit,
    weight: Float,
    onWeightChange: (Float) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    var expandBg by remember { mutableStateOf(false) }
    var expandFg by remember { mutableStateOf(false) }

    val primaryColors = listOf(
        0xFFFFFFFFL, 0xFF000000L, 0xFFFFEB3BL, 0xFF4CAF50L,
        0xFF2196F3L, 0xFFE91E63L, 0xFFF44336L, 0xFFFF9800L
    )
    val moreColors = listOf(
        0xFF9C27B0L, 0xFF7C4DFFL, 0xFF795548L, 0xFF9E9E9EL,
        0xFFFFD700L, 0xFFC0C0C0L, 0xFF00BCD4L, 0xFFFF00FFL,
        0xFF009688L, 0xFF001F5BL, 0xFF87CEEBL, 0xFF0D47A1L,
        0xFFCDDC39L, 0xFF808000L, 0xFF800000L, 0xFFF5F5DCL,
        0xFFFFFDD0L, 0xFFE6E6FAL, 0xFF40E0D0L, 0xFF3F51B5L,
        0xFFFF7F50L, 0xFFFFE5B4L, 0xFF98FF98L, 0xFFF0E68CL
    )
    val allColors = primaryColors + moreColors

    val previewWeight = when {
        weight < 0.34f -> FontWeight.Light
        weight < 0.67f -> FontWeight.Normal
        else -> FontWeight.Bold
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = {
            Text(stringResource(R.string.image_numbering_title), color = Color.Black)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Live preview
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(72.dp)
                        .align(Alignment.CenterHorizontally)
                        .background(Color(bgArgb.toInt()), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "12",
                        color = Color(fgArgb.toInt()),
                        fontWeight = previewWeight,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeight = 28.sp
                        )
                    )
                }

                // BG expandable
                Text(
                    text = stringResource(R.string.image_numbering_change_bg),
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandBg = !expandBg
                            if (expandBg) expandFg = false
                        }
                        .padding(vertical = 8.dp)
                )
                if (expandBg) {
                    NumberingColorGrid(
                        colors = allColors,
                        selected = bgArgb,
                        onSelect = onBgChange
                    )
                }

                // FG expandable
                Text(
                    text = stringResource(R.string.image_numbering_change_fg),
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandFg = !expandFg
                            if (expandFg) expandBg = false
                        }
                        .padding(vertical = 8.dp)
                )
                if (expandFg) {
                    NumberingColorGrid(
                        colors = allColors,
                        selected = fgArgb,
                        onSelect = onFgChange
                    )
                }

                Text(
                    text = stringResource(R.string.image_numbering_thickness),
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
                SliderWithValueLabel(
                    value = weight,
                    onValueChange = onWeightChange,
                    valueRange = 0f..1f,
                    steps = 0,
                    labelFormatter = { v ->
                        val pct = (v * 100f).toInt().coerceIn(0, 100)
                        "$pct%"
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Back", color = Color.Black)
                    }
                    TextButton(onClick = onNext) {
                        Text(
                            text = stringResource(R.string.image_numbering_style_next),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun NumberingColorGrid(
    colors: List<Long>,
    selected: Long,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.chunked(8).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { col ->
                    val isSel = (col and 0x00FFFFFFL) == (selected and 0x00FFFFFFL) || col == selected
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(col.toInt()), CircleShape)
                            .border(
                                width = if (isSel) 2.dp else 1.dp,
                                color = if (isSel) Color(0xFF1976D2L) else Color.LightGray,
                                shape = CircleShape
                            )
                            .clickable { onSelect(col) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlignGlyph(
    kind: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val dropdown = currentDropdownDesign() // fix40: glyph colors follow the dropdown design
    val accent = dropdown.accent
    val plain = dropdown.text
    val off = dropdown.subText
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                if (selected && enabled) accent.copy(alpha = 0.10f) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                if (selected && enabled) accent else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val col = when {
                !enabled -> off
                selected -> accent
                else -> plain
            }
            val stroke = 1.8.dp.toPx()
            val gap = size.height / 5f
            val full = size.width
            val widths = when (kind) {
                "CENTER" -> listOf(full, full * 0.70f, full * 0.88f, full * 0.56f)
                "JUSTIFY" -> listOf(full, full, full, full)
                else -> listOf(full, full * 0.72f, full * 0.90f, full * 0.58f)
            }
            for (i in 0 until 4) {
                val y = gap * i + stroke
                val w = widths[i]
                val x0 = when (kind) {
                    "CENTER" -> (full - w) / 2f
                    "RIGHT" -> full - w
                    else -> 0f
                }
                drawLine(col, Offset(x0, y), Offset(x0 + w, y), strokeWidth = stroke)
            }
        }
    }
}
