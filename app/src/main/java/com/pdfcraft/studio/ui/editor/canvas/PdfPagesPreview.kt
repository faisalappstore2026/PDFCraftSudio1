package com.pdfcraft.studio.ui.editor.canvas
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.material3.LocalTextStyle

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentHeight


import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.pdfcraft.studio.ui.common.AppIcons
import com.pdfcraft.studio.ui.editor.packEditorPages
import com.pdfcraft.studio.ui.editor.elementFrame
import com.pdfcraft.studio.ui.editor.packPageSource
import com.pdfcraft.studio.ui.editor.currentMenuDesign
import com.pdfcraft.studio.ui.editor.dropdownBorderAnim
import com.pdfcraft.studio.ui.editor.editorBorderLine
import com.pdfcraft.studio.ui.common.DocumentScrollBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcraft.studio.R
import com.pdfcraft.studio.core.text.AppFont
import com.pdfcraft.studio.core.text.FontCatalog
import com.pdfcraft.studio.ui.editor.ImportedImage
import com.pdfcraft.studio.ui.editor.ColorRange
import com.pdfcraft.studio.ui.editor.TextGradient
import com.pdfcraft.studio.ui.editor.FontRange
import com.pdfcraft.studio.ui.editor.TextElement
import com.pdfcraft.studio.ui.common.colorpicker.GradientDir
import com.pdfcraft.studio.ui.common.colorpicker.gradientBrush
import com.pdfcraft.studio.ui.editor.LinkRange
import com.pdfcraft.studio.ui.editor.ShadowRange
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest

private const val PAGE_ASPECT_RATIO = 9f / 16f
private const val PAGE_INNER_PADDING_DP = 10f

@OptIn(ExperimentalFoundationApi::class)

private fun packImagesForPages(
    images: List<com.pdfcraft.studio.ui.editor.ImportedImage>,
    imagesPerRow: Int,
    spacingDp: Float,
    cellWidthDp: Float,
    gridHeightDp: Float,
    defaultAspect: Float
): List<List<com.pdfcraft.studio.ui.editor.ImportedImage>> {
    return packEditorPages(images, imagesPerRow, spacingDp, cellWidthDp, gridHeightDp, defaultAspect)
}

private val editorZoomScale = mutableStateOf(1f)

@Composable
fun PdfPagesPreview(
    images: List<ImportedImage>,
    imagesPerRow: Int,
    imageSpacingDp: Int,
    imageCellAspectRatio: Float = 1f,
    imageCornerRadiusPercent: Int = 0,
    pageAspectRatio: Float = PAGE_ASPECT_RATIO,
    pageAspectRatioForPage: ((Int) -> Float)? = null,
    pageMarginDp: Int = 10,
    pageBackgroundColor: Long = 0xFFFFFFFFL,
    pageBackgroundColorForPage: ((Int) -> Long)? = null,
    pageBackgroundBitmap: android.graphics.Bitmap? = null,
    pageBackgroundBitmapForPage: ((Int) -> android.graphics.Bitmap?)? = null,
    pageBackgroundKindForPage: ((Int) -> String)? = null,
    pageBackgroundColor2ForPage: ((Int) -> Long)? = null,
    pageBackgroundDirForPage: ((Int) -> String)? = null,
    pageNumberPosition: com.pdfcraft.studio.ui.editor.EditorViewModel.PageNumberPosition =
        com.pdfcraft.studio.ui.editor.EditorViewModel.PageNumberPosition.NONE,
    formatPageNumber: (Int) -> String = { (it + 1).toString() },
    minPageCount: Int = 1,
    selectedImageIds: List<String> = emptyList(),
    selectionMode: Boolean = false,
    singleMenuImageId: String? = null,
    multipleActionsVisible: Boolean = false,
    reorderMode: Boolean = false,
    hasClipboardImages: Boolean = false,
    textElements: List<TextElement> = emptyList(),
    addTextMode: Boolean = false,
    selectedTextId: String? = null,
    pendingFocusTextId: String? = null,
    onAddTextAt: (pageIndex: Int, xFraction: Float, yFraction: Float) -> Unit = { _, _, _ -> },
    onSelectText: (String) -> Unit = {},
    onMoveText: (id: String, xFraction: Float, yFraction: Float) -> Unit = { _, _, _ -> },
    onTextValueChange: (id: String, newText: String, newSelection: TextRange) -> Unit = { _, _, _ -> },
    onTextFocused: (String) -> Unit = {},
    onTextUnfocused: (String) -> Unit = {},
    onConsumePendingFocus: () -> Unit = {},
    onDeselectText: () -> Unit = {},
    onTextRotate: (String, Float) -> Unit = { _, _ -> },
    onTextScale: (String, Float) -> Unit = { _, _ -> },
    onTextBoxResize: (String, Float, Float, Float, Float, Float) -> Unit = { _, _, _, _, _, _ -> },
    onTextAutoFit: (String, Float) -> Unit = { _, _ -> },
    hideTextSelectionChrome: Boolean = false,
    onImageClick: (String) -> Unit = {},
    onImageLongPress: (String) -> Unit = {},
    onChangePosition: (String) -> Unit = {},
    onCut: (String) -> Unit = {},
    onCopy: (String) -> Unit = {},
    onDeleteSingle: (String) -> Unit = {},
    onDeleteSingleKeepSpace: (String) -> Unit = {},
    onDeleteSingleFillSpace: (String) -> Unit = {},
    onPaste: () -> Unit = {},
    onSaveSingle: (String) -> Unit = {},
    onShareSingle: (String) -> Unit = {},
    onCropImage: (String) -> Unit = {},
    onRotateImage: (String) -> Unit = {},
    onReplaceImage: (String) -> Unit = {},
    onImagePosition: (String) -> Unit = {},
    onAddImageLink: (String) -> Unit = {},
    onFinishMultipleSelection: () -> Unit = {},
    onMultipleChangePosition: () -> Unit = {},
    onMultipleCut: () -> Unit = {},
    onMultipleCopy: () -> Unit = {},
    onMultipleSave: () -> Unit = {},
    onMultipleShare: () -> Unit = {},
    onMultipleDelete: () -> Unit = {},
    onCloseMultipleActions: () -> Unit = {},
    onMoveSingle: (String, String) -> Unit = { _, _ -> },
    onMoveMultiple: (String) -> Unit = {},
    onFinishReorder: () -> Unit = {},
    customFonts: List<AppFont> = emptyList(),
    zoom: Float = 1f,
    onZoomChange: ((Float) -> Unit)? = null,
    initialScrollIndex: Int = 0,
    initialScrollOffset: Int = 0,
    onScrollChanged: (Int, Int) -> Unit = { _, _ -> },
    dragModeActive: Boolean = false,
    onDragImagesByFrac: (Float, Float) -> Unit = { _, _ -> },
    onSyncDragOffset: (String, Float, Float) -> Unit = { _, _, _ -> },
    onEnsureDragSelection: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    revealTextSignal: Int = 0
) {
    val pageListState = rememberLazyListState(0, 0)
    val revealDensity = LocalDensity.current
    // Scroll so the selected text box sits just below the top slider panel.
    LaunchedEffect(revealTextSignal) {
        if (revealTextSignal <= 0) return@LaunchedEffect
        val te = textElements.firstOrNull { it.id == selectedTextId } ?: return@LaunchedEffect
        val total = pageListState.layoutInfo.totalItemsCount
        if (total <= 0) return@LaunchedEffect
        val src = packPageSource.copyOf()
        var item = -1
        for (q in src.indices) {
            if (src[q] == te.pageIndex) {
                item = q
                break
            }
        }
        if (item < 0) item = te.pageIndex
        item = item.coerceIn(0, total - 1)
        pageListState.animateScrollToItem(item)
        val info = pageListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == item }
        if (info != null) {
            val labelPx = with(revealDensity) { 28.dp.toPx() }
            val marginPx = with(revealDensity) { 24.dp.toPx() }
            val cardH = (info.size.toFloat() - labelPx).coerceAtLeast(0f)
            val target = labelPx + te.yFraction.coerceIn(0f, 1f) * cardH - marginPx
            if (target > 1f) {
                pageListState.animateScrollBy(target)
            }
        }
    }
    var scrollRestoreDone by remember {
        mutableStateOf(initialScrollIndex <= 0 && initialScrollOffset <= 0)
    }
    LaunchedEffect(pageListState, initialScrollIndex, initialScrollOffset) {
        if (scrollRestoreDone) return@LaunchedEffect
        snapshotFlow { pageListState.layoutInfo.totalItemsCount }
            .collect { count ->
                if (count <= 0) return@collect
                val idx = initialScrollIndex.coerceIn(0, count - 1)
                pageListState.scrollToItem(idx, initialScrollOffset.coerceAtLeast(0))
                scrollRestoreDone = true
            }
    }
    LaunchedEffect(pageListState) {
        snapshotFlow { pageListState.firstVisibleItemIndex to pageListState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collectLatest { pair ->
                // Save only when scrolling settles, not on every frame.
                kotlinx.coroutines.delay(400)
                if (scrollRestoreDone) onScrollChanged(pair.first, pair.second)
            }
    }
    androidx.compose.runtime.DisposableEffect(pageListState) {
        onDispose {
            // Leaving the editor: store the last position right away.
            if (scrollRestoreDone) {
                onScrollChanged(pageListState.firstVisibleItemIndex, pageListState.firstVisibleItemScrollOffset)
            }
        }
    }
    val scaleState = remember { mutableStateOf(1f) }
    val panState = remember { mutableStateOf(Offset.Zero) }
    val pinching = remember { mutableStateOf(false) }
    val zoomLayer = Modifier.graphicsLayer {
        val s = scaleState.value
        val p = panState.value
        scaleX = s
        scaleY = s
        translationX = p.x
        translationY = p.y
        transformOrigin = TransformOrigin(0f, 0f)
    }
    // Page zoom is off while a text box is selected (pinch then belongs to the box).
    val selTextNow = rememberUpdatedState(selectedTextId)
    val pinchMod = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            var zooming = false
            var lastCx = Float.NaN
            var lastCy = Float.NaN
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.count { it.pressed }
                if (pressed >= 2 && selTextNow.value == null) {
                    zooming = true
                    pinching.value = true
                    val z = event.calculateZoom()
                    val c = event.calculateCentroid()
                    if (z > 0f) {
                        if (lastCx.isNaN()) {
                            lastCx = c.x
                            lastCy = c.y
                        }
                        val pan = Offset(c.x - lastCx, c.y - lastCy)
                        lastCx = c.x
                        lastCy = c.y
                        val oldS = scaleState.value
                        val newS = (oldS * z).coerceIn(1f, 4f)
                        val r = newS / oldS.coerceAtLeast(0.01f)
                        val oldP = panState.value
                        val nx = c.x - (c.x - oldP.x) * r + pan.x
                        val ny = c.y - (c.y - oldP.y) * r + pan.y
                        val vw = size.width.toFloat().coerceAtLeast(1f)
                        val vh = size.height.toFloat().coerceAtLeast(1f)
                        if (newS <= 1.001f) {
                            scaleState.value = 1f
                            panState.value = Offset.Zero
                        } else {
                            scaleState.value = newS
                            panState.value = Offset(
                                nx.coerceIn(vw * (1f - newS), 0f),
                                ny.coerceIn(vh * (1f - newS), 0f)
                            )
                        }
                    }
                    event.changes.forEach { it.consume() }
                } else if (zooming) {
                    event.changes.forEach { it.consume() }
                }
                if (event.changes.none { it.pressed }) break
            }
            pinching.value = false
        }
    }
    editorZoomScale.value = scaleState.value
    // Always show at least one page so text can be added before any images.
    if (images.isEmpty() && minPageCount <= 1) {
        BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color.Transparent).clipToBounds().then(pinchMod)) { // fix36
            LazyColumn(
                state = pageListState,
                userScrollEnabled = !pinching.value,
                modifier = Modifier
                    .fillMaxSize()
                    .then(zoomLayer),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.page_label, 1),
                            style = MaterialTheme.typography.bodyMedium,
                            color = currentMenuDesign().pageLabel, // fix36
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        PageCard(
                            aspectRatio = pageAspectRatioForPage?.invoke(0) ?: pageAspectRatio,
                            backgroundColor = run {
                                val kind = pageBackgroundKindForPage?.invoke(0) ?: "SOLID"
                                if (kind == "NONE" || kind == "IMAGE") 0xFFFFFFFFL else (pageBackgroundColorForPage?.invoke(0) ?: pageBackgroundColor)
                            },
                            backgroundBitmap = if (pageBackgroundBitmapForPage != null) {
                                pageBackgroundBitmapForPage.invoke(0)
                            } else pageBackgroundBitmap,
                            backgroundBrush = run {
                                val kind = pageBackgroundKindForPage?.invoke(0) ?: "SOLID"
                                if (kind != "GRADIENT") null
                                else {
                                    gradientBrush(
                                        pageBackgroundColorForPage?.invoke(0) ?: pageBackgroundColor,
                                        pageBackgroundColor2ForPage?.invoke(0) ?: 0xFF90CAF9L,
                                        pageBackgroundDirForPage?.invoke(0) ?: "TTB"
                                    )
                                }
                            },
                            pageNumberText = if (pageNumberPosition != com.pdfcraft.studio.ui.editor.EditorViewModel.PageNumberPosition.NONE)
                                formatPageNumber(0) else null,
                            pageNumberPosition = pageNumberPosition
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                PageTextOverlay(
                                    texts = textElements.filter { it.pageIndex == 0 },
                                    addTextMode = addTextMode,
                                    selectedTextId = selectedTextId,
                                    pendingFocusTextId = pendingFocusTextId,
                                    customFonts = customFonts,
                                    onPageTap = { xFrac, yFrac -> onAddTextAt(0, xFrac, yFrac) },
                                    onTextSelect = onSelectText,
                                    onTextDrag = onMoveText,
                                    onTextValueChange = onTextValueChange,
                                    onTextFocused = onTextFocused,
                                    onTextUnfocused = onTextUnfocused,
                                    onConsumePendingFocus = onConsumePendingFocus,
                                    onDeselect = onDeselectText,
                                    onTextRotate = onTextRotate,
                                    onTextScale = onTextScale,
                                    onTextBoxResize = onTextBoxResize,
                                    onTextAutoFit = onTextAutoFit,
                                    hideSelectionChrome = hideTextSelectionChrome
                                )
                            }
                        }
                    }
                }
            }
            DocumentScrollBar(
                listState = pageListState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        return
    }

    val cellBounds = remember { mutableStateMapOf<String, Rect>() }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(Color.Transparent).clipToBounds().then(pinchMod) // fix36
    ) {
        val pageWidthDp = maxWidth.value
        val pageHeightDp = pageWidthDp / pageAspectRatio

        val innerPad = if (imagesPerRow == 1) 0f else PAGE_INNER_PADDING_DP
        val gridWidthDp = pageWidthDp - (innerPad * 2)
        val gridHeightDp = pageHeightDp - (innerPad * 2)
        val spacing = imageSpacingDp.toFloat()

        val cellWidthDp = (gridWidthDp - spacing * (imagesPerRow - 1)) / imagesPerRow
        val cellHeightDp = if (imageCellAspectRatio > 0f) {
            cellWidthDp / imageCellAspectRatio
        } else {
            cellWidthDp
        }

        val rowsPerPage = if (cellHeightDp > 0f) {
            (((gridHeightDp + spacing) / (cellHeightDp + spacing)).toInt()).coerceAtLeast(1)
        } else {
            1
        }

        // Auto-flow by real row heights (Resize Images can make some cells taller).
        val gridImages = images.filter { it.ownerPageIndex == null }
        val contentPages = packImagesForPages(
            images = gridImages,
            imagesPerRow = imagesPerRow,
            spacingDp = spacing,
            cellWidthDp = cellWidthDp,
            gridHeightDp = gridHeightDp,
            defaultAspect = imageCellAspectRatio
        )
        val pageSrc = packPageSource.copyOf()
        val pages = if (contentPages.size >= minPageCount) contentPages
            else contentPages + List(minPageCount - contentPages.size) { emptyList() }

        LazyColumn(
            state = pageListState,
            userScrollEnabled = !pinching.value,
            modifier = Modifier
                .fillMaxSize()
                .then(zoomLayer),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            itemsIndexed(
                pages,
                key = { index, _ -> index }
            ) { pageIndex, pageImages ->
                val docPage = if (pageIndex < pageSrc.size) pageSrc[pageIndex] else pageIndex

                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(
                            R.string.page_label,
                            pageIndex + 1
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = currentMenuDesign().pageLabel, // fix36
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    PageCard(
                        aspectRatio = pageAspectRatioForPage?.invoke(docPage) ?: pageAspectRatio,
                        backgroundColor = run {
                            val kind = pageBackgroundKindForPage?.invoke(docPage) ?: "SOLID"
                            if (kind == "NONE" || kind == "IMAGE") 0xFFFFFFFFL else (pageBackgroundColorForPage?.invoke(docPage) ?: pageBackgroundColor)
                        },
                        backgroundBitmap = if (pageBackgroundBitmapForPage != null) {
                            pageBackgroundBitmapForPage.invoke(docPage)
                        } else pageBackgroundBitmap,
                        backgroundBrush = run {
                            val kind = pageBackgroundKindForPage?.invoke(docPage) ?: "SOLID"
                            if (kind != "GRADIENT") null
                            else {
                                gradientBrush(
                                    pageBackgroundColorForPage?.invoke(docPage) ?: pageBackgroundColor,
                                    pageBackgroundColor2ForPage?.invoke(docPage) ?: 0xFF90CAF9L,
                                    pageBackgroundDirForPage?.invoke(docPage) ?: "TTB"
                                )
                            }
                        },
                        pageNumberText = if (pageNumberPosition != com.pdfcraft.studio.ui.editor.EditorViewModel.PageNumberPosition.NONE)
                            formatPageNumber(pageIndex) else null,
                        pageNumberPosition = pageNumberPosition
                    ) {
                        val pageOriginSt = remember(pageIndex) { mutableStateOf(Offset.Zero) }
                        val pageSizeSt = remember(pageIndex) { mutableStateOf(IntSize.Zero) }
                        Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize().clipToBounds().onGloballyPositioned {
                            pageOriginSt.value = it.positionInRoot()
                            pageSizeSt.value = it.size
                        }) {
                            if (pageImages.isNotEmpty()) ImageGrid(
                                images = pageImages,
                                imagesPerRow = imagesPerRow,
                                spacingDp = imageSpacingDp,
                                pageWidthDp = pageWidthDp,
                                pageHeightDp = pageWidthDp / (pageAspectRatioForPage?.invoke(docPage) ?: pageAspectRatio).coerceAtLeast(0.1f),
                                pageMarginDp = pageMarginDp,
                                pageOrigin = pageOriginSt.value,
                                pageSizePx = pageSizeSt.value,
                                dragModeActive = dragModeActive,
                                onDragImagesByFrac = onDragImagesByFrac,
                                onSyncDragOffset = onSyncDragOffset,
                                onEnsureDragSelection = onEnsureDragSelection,
                                cellAspectRatio = imageCellAspectRatio,
                                cellCornerRadiusPercent = imageCornerRadiusPercent,
                                selectedImageIds = selectedImageIds,
                                selectionMode = selectionMode,
                                singleMenuImageId = singleMenuImageId,
                                reorderMode = reorderMode,
                                hasClipboardImages = hasClipboardImages,
                                cellBounds = cellBounds,
                                onImageClick = onImageClick,
                                onImageLongPress = onImageLongPress,
                                onChangePosition = onChangePosition,
                                onCut = onCut,
                                onCopy = onCopy,
                                onPaste = onPaste,
                                onSaveSingle = onSaveSingle,
                                onShareSingle = onShareSingle,
                                onCropImage = onCropImage,
                                onRotateImage = onRotateImage,
                                onReplaceImage = onReplaceImage,
                                onImagePosition = onImagePosition,
                                onAddImageLink = onAddImageLink,
                                onDeleteSingle = onDeleteSingle,
                                onDeleteSingleKeepSpace = onDeleteSingleKeepSpace,
                                onDeleteSingleFillSpace = onDeleteSingleFillSpace,
                                onMoveSingle = onMoveSingle,
                                onMoveMultiple = onMoveMultiple,
                                onFinishReorder = onFinishReorder
                            )

                            AbsolutePdfImagesLayer(
                                images = images.filter { docPage >= 0 && it.ownerPageIndex == docPage },
                                cellAspectRatio = imageCellAspectRatio,
                                selectedImageIds = selectedImageIds,
                                singleMenuImageId = singleMenuImageId,
                                reorderMode = reorderMode,
                                hasClipboardImages = hasClipboardImages,
                                cellBounds = cellBounds,
                                onImageClick = onImageClick,
                                onChangePosition = onChangePosition,
                                onCut = onCut,
                                onCopy = onCopy,
                                onPaste = onPaste,
                                onSaveSingle = onSaveSingle,
                                onShareSingle = onShareSingle,
                                onCropImage = onCropImage,
                                onRotateImage = onRotateImage,
                                onReplaceImage = onReplaceImage,
                                onImagePosition = onImagePosition,
                                onAddImageLink = onAddImageLink,
                                onDeleteSingleKeepSpace = onDeleteSingleKeepSpace,
                                onDeleteSingleFillSpace = onDeleteSingleFillSpace,
                                onMoveSingle = onMoveSingle,
                                onMoveMultiple = onMoveMultiple,
                                onFinishReorder = onFinishReorder,
                                selectedCount = selectedImageIds.size
                            )
                        }

                            PageTextOverlay(
                                texts = textElements.filter { docPage >= 0 && it.pageIndex == docPage },
                                addTextMode = addTextMode,
                                selectedTextId = selectedTextId,
                                pendingFocusTextId = pendingFocusTextId,
                                customFonts = customFonts,
                                onPageTap = { xFrac, yFrac -> onAddTextAt(if (docPage < 0) pageIndex else docPage, xFrac, yFrac) },
                                onTextSelect = onSelectText,
                                onTextDrag = onMoveText,
                                onTextValueChange = onTextValueChange,
                                onTextFocused = onTextFocused,
                                onTextUnfocused = onTextUnfocused,
                                onConsumePendingFocus = onConsumePendingFocus,
                                onDeselect = onDeselectText,
                                onTextRotate = onTextRotate,
                                onTextScale = onTextScale,
                                onTextBoxResize = onTextBoxResize,
                                onTextAutoFit = onTextAutoFit,
                                hideSelectionChrome = hideTextSelectionChrome
                            )
                        }
                    }
                }
            }
        }
            DocumentScrollBar(
                listState = pageListState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )

        // Long-press selection Done button removed

    }

    // MultipleActionsDialog removed (long-press flow)

}

/** Uptime of the last finger-down that landed inside a text box (or its handles). */
internal object TextBoxTouch {
    @JvmField
    var downUptime: Long = -1L
}

/** true where a background RANGE covers the char (its spaces get background too). */
private fun bgKeepMask(len: Int, ranges: List<ColorRange>): BooleanArray {
    val m = BooleanArray(len.coerceAtLeast(0))
    ranges.forEach { cr ->
        val s = cr.range.first.coerceIn(0, m.size)
        val e = (cr.range.last + 1).coerceIn(0, m.size)
        for (q in s until e) m[q] = true
    }
    return m
}

/** Per character color: whole-box color first, then ranges on top. */
private fun charColorsFor(len: Int, whole: Long?, ranges: List<ColorRange>): Array<Long?> {
    val out = arrayOfNulls<Long>(len)
    if (whole != null) {
        for (k in 0 until len) out[k] = whole
    }
    ranges.forEach { cr ->
        val s = cr.range.first.coerceIn(0, len)
        val e = (cr.range.last + 1).coerceIn(0, len)
        for (k in s until e) out[k] = cr.colorArgb
    }
    return out
}

/**
 * Clamp a text box's top-left position (fractions) so its VISUAL edge (rotation around the box
 * center included) stays edgePx inside the page on the left / top, and leaves room for the
 * minimum box on the right / bottom. Page size unknown: position is returned unchanged.
 */
private fun clampTextBoxPos(
    xf: Float,
    yf: Float,
    pw: Float,
    ph: Float,
    wPx: Float,
    hPx: Float,
    rotDeg: Float,
    edgePx: Float,
    minWPx: Float,
    minHPx: Float
): FloatArray {
    if (pw <= 0f || ph <= 0f || xf.isNaN() || yf.isNaN()) return floatArrayOf(xf, yf)
    val w = wPx.coerceAtLeast(0f)
    val h = hPx.coerceAtLeast(0f)
    val rad = rotDeg * (Math.PI.toFloat() / 180f)
    val c = kotlin.math.abs(kotlin.math.cos(rad))
    val s = kotlin.math.abs(kotlin.math.sin(rad))
    val exX = ((w * c + h * s) / 2f - w / 2f).coerceAtLeast(0f)
    val exY = ((w * s + h * c) / 2f - h / 2f).coerceAtLeast(0f)
    val loX = ((edgePx + exX) / pw).coerceIn(0f, 1f)
    val loY = ((edgePx + exY) / ph).coerceIn(0f, 1f)
    val hiX = maxOf(loX, 1f - (edgePx + minWPx) / pw)
    val hiY = maxOf(loY, 1f - (edgePx + minHPx) / ph)
    return floatArrayOf(xf.coerceIn(loX, hiX), yf.coerceIn(loY, hiY))
}

/** Cached background runs (no padding yet): rects = left, top, right, bottom; one color each. */
private class BgRuns(val rects: List<FloatArray>, val colors: List<Long>)

/**
 * Text background runs from the layout. Computed once per layout/colors (callers remember it);
 * only the run-edge characters are measured instead of every character.
 */
private fun computeTextBgRuns(
    layout: androidx.compose.ui.text.TextLayoutResult,
    bg: Array<Long?>,
    keep: BooleanArray? = null
): BgRuns {
    val rects = ArrayList<FloatArray>()
    val colors = ArrayList<Long>()
    val txt = layout.layoutInput.text.text
    val len = minOf(bg.size, txt.length)
    if (len <= 0) return BgRuns(rects, colors)
    // Spaces inside a selected background range keep their background.
    fun keepAt(q: Int): Boolean =
        !txt[q].isWhitespace() || (keep != null && q < keep.size && keep[q])
    // Pass 1: one piece per line per color run: start, end, left, top, right, bottom.
    val pieces = ArrayList<FloatArray>()
    val pieceColors = ArrayList<Long>()
    for (line in 0 until layout.lineCount) {
        var ls = layout.getLineStart(line).coerceIn(0, len)
        var le = layout.getLineEnd(line, visibleEnd = false).coerceIn(ls, len)
        while (le > ls && txt[le - 1] == '\n') le--
        while (ls < le && !keepAt(ls)) ls++
        while (le > ls && !keepAt(le - 1)) le--
        val lineTop = layout.getLineTop(line)
        val lineBottom = layout.getLineBottom(line)
        var i = ls
        while (i < le) {
            val c = bg[i]
            if (c == null || !keepAt(i)) {
                i++
                continue
            }
            var j = i + 1
            while (j < le && bg[j] == c) j++
            var k = j
            while (k > i + 1 && !keepAt(k - 1)) k--
            // Run edges only (first + last char of the run on this line).
            val bbA = layout.getBoundingBox(i)
            val bbB = if (k - 1 > i) layout.getBoundingBox(k - 1) else bbA
            val left = minOf(bbA.left, bbB.left)
            val right = maxOf(bbA.right, bbB.right)
            if (right >= left) {
                pieces.add(floatArrayOf(i.toFloat(), k.toFloat(), left, lineTop, right, lineBottom))
                pieceColors.add(c)
            }
            i = j
        }
    }
    // Pass 2: pieces of one contiguous run (across wrapped lines) become ONE rounded rect.
    var p = 0
    while (p < pieces.size) {
        val c = pieceColors[p]
        val first = pieces[p]
        var l = first[2]
        var t = first[3]
        var r = first[4]
        var b = first[5]
        var lastEnd = first[1].toInt()
        var n = p + 1
        while (n < pieces.size && pieceColors[n] == c) {
            val nx = pieces[n]
            val ns = nx[0].toInt()
            var joined = ns >= lastEnd
            var q = lastEnd
            while (joined && q < ns) {
                if (q >= len || bg[q] != c) joined = false
                q++
            }
            if (!joined) break
            l = minOf(l, nx[2])
            t = minOf(t, nx[3])
            r = maxOf(r, nx[4])
            b = maxOf(b, nx[5])
            lastEnd = nx[1].toInt()
            n++
        }
        rects.add(floatArrayOf(l, t, r, b))
        colors.add(c)
        p = n
    }
    return BgRuns(rects, colors)
}

/** Draw cached background runs with padding, radius and box opacity (cheap per frame). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTextBackgrounds(
    runs: BgRuns,
    radiusPx: Float,
    alpha: Float,
    padLeftPx: Float = 0f,
    padTopPx: Float = 0f,
    padRightPx: Float = 0f,
    padBottomPx: Float = 0f
) {
    val n = minOf(runs.rects.size, runs.colors.size)
    for (idx in 0 until n) {
        val rc = runs.rects[idx]
        if (rc.size < 4) continue
        val left = rc[0] - padLeftPx
        val right = rc[2] + padRightPx
        val top = rc[1] - padTopPx
        val bottom = rc[3] + padBottomPx
        val h = (bottom - top).coerceAtLeast(0f)
        val rad = radiusPx.coerceIn(0f, maxOf(0f, h / 2f))
        val raw = runs.colors[idx]
        if (TextGradient.isGradient(raw)) {
            // Gradient background: one linear gradient across this rounded rect.
            val gl = TextGradient.line(raw)
            val rw = (right - left).coerceAtLeast(0f)
            if (rw <= 0.5f || h <= 0.5f) continue
            drawRoundRect(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(TextGradient.opaque1(raw)), Color(TextGradient.opaque2(raw))),
                    start = Offset(left + gl[0] * rw, top + gl[1] * h),
                    end = Offset(left + gl[2] * rw, top + gl[3] * h)
                ),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(rw, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(rad, rad),
                alpha = (TextGradient.alpha(raw) * alpha).coerceIn(0f, 1f)
            )
            continue
        }
        val col = Color(raw)
        drawRoundRect(
            color = col.copy(alpha = (col.alpha * alpha).coerceIn(0f, 1f)),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size((right - left).coerceAtLeast(0f), h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(rad, rad)
        )
    }
}

/** Brush for a gradient color value (TextGradient), spread over the whole text layout. */
private fun textGradientBrush(c: Long): androidx.compose.ui.graphics.Brush {
    val l = TextGradient.line(c)
    return com.pdfcraft.studio.ui.common.colorpicker.gradientBrush(
        TextGradient.opaque1(c), TextGradient.opaque2(c), l[0], l[1], l[2], l[3]
    )
}

/** Base text without gradient (shader) spans: the outline / glow copies set their own paint. */
private fun withoutShaderBrushes(base: AnnotatedString): AnnotatedString {
    if (base.spanStyles.none { it.item.brush is androidx.compose.ui.graphics.ShaderBrush }) return base
    return AnnotatedString(
        base.text,
        base.spanStyles.filter { it.item.brush !is androidx.compose.ui.graphics.ShaderBrush },
        base.paragraphStyles
    )
}

/** Same text and spans; spread characters drawn in the spread color with a zero-offset blur (glow). */
private fun buildSpreadText(base: AnnotatedString, spread: Array<Long?>, blurPx: Float): AnnotatedString {
    val b = AnnotatedString.Builder(withoutShaderBrushes(base))
    val len = minOf(base.text.length, spread.size)
    if (base.text.isNotEmpty()) {
        b.addStyle(
            SpanStyle(
                color = Color.Transparent,
                background = Color.Transparent,
                shadow = Shadow.None,
                textDecoration = TextDecoration.None
            ),
            0,
            base.text.length
        )
    }
    var i = 0
    while (i < len) {
        val c = spread[i]
        if (c == null) {
            i++
            continue
        }
        var j = i + 1
        while (j < len && spread[j] == c) j++
        // A glow cannot be a gradient: gradient spread uses the middle color of the two.
        val sc = Color(TextGradient.solid(c))
        b.addStyle(
            SpanStyle(
                color = sc,
                shadow = Shadow(color = sc, offset = Offset.Zero, blurRadius = blurPx)
            ),
            i,
            j
        )
        i = j
    }
    return b.toAnnotatedString()
}

/** Same text and spans, glyph fill hidden, outline (stroke) on bordered characters. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun buildBorderText(base: AnnotatedString, border: Array<Long?>, strokePx: Float): AnnotatedString {
    val b = AnnotatedString.Builder(withoutShaderBrushes(base))
    val len = minOf(base.text.length, border.size)
    if (base.text.isNotEmpty()) {
        b.addStyle(
            SpanStyle(
                color = Color.Transparent,
                background = Color.Transparent,
                shadow = Shadow.None,
                textDecoration = TextDecoration.None
            ),
            0,
            base.text.length
        )
    }
    var i = 0
    while (i < len) {
        val c = border[i]
        if (c == null) {
            i++
            continue
        }
        var j = i + 1
        while (j < len && border[j] == c) j++
        b.addStyle(
            if (TextGradient.isGradient(c)) {
                SpanStyle(
                    brush = textGradientBrush(c),
                    alpha = TextGradient.alpha(c),
                    drawStyle = Stroke(
                        width = strokePx,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            } else SpanStyle(
                color = Color(c),
                drawStyle = Stroke(
                    width = strokePx,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            ),
            i,
            j
        )
        i = j
    }
    return b.toAnnotatedString()
}

private class TextStyleRangesVisualTransformation(
    private val boldRanges: List<IntRange>,
    private val italicRanges: List<IntRange>,
    private val underlineRanges: List<IntRange> = emptyList(),
    private val strikeRanges: List<IntRange> = emptyList(),
    private val quoteRanges: List<IntRange> = emptyList(),
    private val fontRanges: List<FontRange> = emptyList(),
    private val fontFamilies: Map<String, androidx.compose.ui.text.font.FontFamily> = emptyMap(),
    private val colorRanges: List<ColorRange> = emptyList(),
    private val bgColorRanges: List<ColorRange> = emptyList(),
    private val wholeBgColorArgb: Long? = null,
    private val linkRanges: List<LinkRange> = emptyList(),
    private val shadowRanges: List<ShadowRange> = emptyList(),
    /** Whole-box text color (only used here for gradient colors). */
    private val wholeTextColorArgb: Long? = null
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        if (wholeBgColorArgb != null && text.text.isNotEmpty()) {
            builder.addStyle(
                SpanStyle(background = Color(wholeBgColorArgb)),
                0,
                text.text.length
            )
        }
        boldRanges.forEach { range ->
            val start = range.first.coerceIn(0, text.text.length)
            val end = (range.last + 1).coerceIn(0, text.text.length)
            if (start < end) {
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
            }
        }
        italicRanges.forEach { range ->
            val start = range.first.coerceIn(0, text.text.length)
            val end = (range.last + 1).coerceIn(0, text.text.length)
            if (start < end) {
                builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
            }
        }
        quoteRanges.forEach { range ->
            val start = range.first.coerceIn(0, text.text.length)
            val end = (range.last + 1).coerceIn(0, text.text.length)
            if (start < end) {
                builder.addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic, background = Color(0x141976D2)),
                    start,
                    end
                )
            }
        }
        val len = text.text.length
        val u = BooleanArray(len)
        val s = BooleanArray(len)
        underlineRanges.forEach { range ->
            val a = range.first.coerceIn(0, len)
            val b = (range.last + 1).coerceIn(0, len)
            for (i in a until b) u[i] = true
        }
        strikeRanges.forEach { range ->
            val a = range.first.coerceIn(0, len)
            val b = (range.last + 1).coerceIn(0, len)
            for (i in a until b) s[i] = true
        }
        var i = 0
        while (i < len) {
            if (!u[i] && !s[i]) { i++; continue }
            var j = i + 1
            while (j < len && u[j] == u[i] && s[j] == s[i]) j++
            val deco = when {
                u[i] && s[i] -> TextDecoration.Underline + TextDecoration.LineThrough
                u[i] -> TextDecoration.Underline
                else -> TextDecoration.LineThrough
            }
            builder.addStyle(SpanStyle(textDecoration = deco), i, j)
            i = j
        }
        fontRanges.forEach { fr ->
            val start = fr.range.first.coerceIn(0, text.text.length)
            val end = (fr.range.last + 1).coerceIn(0, text.text.length)
            val fam = fontFamilies[fr.fontId]
            if (start < end && fam != null) {
                builder.addStyle(SpanStyle(fontFamily = fam), start, end)
            }
        }
        colorRanges.forEach { cr ->
            val start = cr.range.first.coerceIn(0, text.text.length)
            val end = (cr.range.last + 1).coerceIn(0, text.text.length)
            if (start < end && !TextGradient.isGradient(cr.colorArgb)) {
                builder.addStyle(SpanStyle(color = Color(cr.colorArgb)), start, end)
            }
        }
        // Gradient text color (whole box or ranges): one brush span per run, spread over the box.
        val fgChars = charColorsFor(len, wholeTextColorArgb, colorRanges)
        var gi = 0
        while (gi < len) {
            val gc = fgChars[gi]
            if (gc == null || !TextGradient.isGradient(gc)) {
                gi++
                continue
            }
            var gj = gi + 1
            while (gj < len && fgChars[gj] == gc) gj++
            builder.addStyle(SpanStyle(brush = textGradientBrush(gc), alpha = TextGradient.alpha(gc)), gi, gj)
            gi = gj
        }
        bgColorRanges.forEach { cr ->
            val start = cr.range.first.coerceIn(0, text.text.length)
            val end = (cr.range.last + 1).coerceIn(0, text.text.length)
            if (start < end) {
                builder.addStyle(SpanStyle(background = Color(cr.colorArgb)), start, end)
            }
        }
        linkRanges.forEach { lr ->
            val start = lr.range.first.coerceIn(0, text.text.length)
            val end = (lr.range.last + 1).coerceIn(0, text.text.length)
            if (start < end) {
                builder.addStyle(
                    SpanStyle(
                        color = Color(0xFF1976D2),
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
            }
        }
        shadowRanges.forEach { sr ->
            val start = sr.range.first.coerceIn(0, text.text.length)
            val end = (sr.range.last + 1).coerceIn(0, text.text.length)
            if (start < end) {
                builder.addStyle(
                    SpanStyle(
                        shadow = Shadow(
                            color = Color(sr.colorArgb),
                            offset = Offset(sr.offsetXPx, sr.offsetYPx),
                            blurRadius = sr.blurPx
                        )
                    ),
                    start,
                    end
                )
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageTextOverlay(
    texts: List<TextElement>,
    addTextMode: Boolean,
    selectedTextId: String?,
    pendingFocusTextId: String?,
    customFonts: List<AppFont>,
    onPageTap: (xFraction: Float, yFraction: Float) -> Unit,
    onTextSelect: (String) -> Unit,
    onTextDrag: (id: String, xFraction: Float, yFraction: Float) -> Unit,
    onTextValueChange: (id: String, newText: String, newSelection: TextRange) -> Unit,
    onTextFocused: (String) -> Unit,
    onTextUnfocused: (String) -> Unit,
    onConsumePendingFocus: () -> Unit,
    onDeselect: () -> Unit,
    /** fix30: true while a text style slider is dragged (border + handles hidden). */
    hideSelectionChrome: Boolean = false,
    onTextRotate: (String, Float) -> Unit = { _, _ -> },
    onTextScale: (String, Float) -> Unit = { _, _ -> },
    onTextBoxResize: (String, Float, Float, Float, Float, Float) -> Unit = { _, _, _, _, _, _ -> },
    onTextAutoFit: (String, Float) -> Unit = { _, _ -> }
) {
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { sizePx = it }
            // IMPORTANT: only intercept taps while placing new text.
            // Otherwise this full-size layer steals clicks from images underneath
            // and the Save/Share menu never opens.
            .then(
                if (addTextMode) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (sizePx.width > 0 && sizePx.height > 0) {
                                val xFraction = (offset.x / sizePx.width).coerceIn(0f, 1f)
                                val yFraction = (offset.y / sizePx.height).coerceIn(0f, 1f)
                                onPageTap(xFraction, yFraction)
                            }
                        }
                    }
                } else if (selectedTextId != null) {
                    Modifier.pointerInput(selectedTextId) {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = true,
                                pass = PointerEventPass.Final
                            )
                            var movedAway = false
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                // Scroll / pinch / drag is not a tap: never deselect for it.
                                if (event.changes.size > 1 || change.isConsumed ||
                                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                                ) {
                                    movedAway = true
                                }
                                if (!change.pressed) {
                                    // Only a tap outside every text box border deselects.
                                    if (!movedAway && TextBoxTouch.downUptime != down.uptimeMillis) {
                                        focusManager.clearFocus(force = true)
                                        onDeselect()
                                    }
                                    break
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        // Fixed page frame (not moved / rotated): finger positions are measured here so a
        // moving box never eats part of the finger movement.
        val pageRef = remember { arrayOfNulls<LayoutCoordinates>(1) }
        Spacer(Modifier.onGloballyPositioned { pageRef[0] = it })
        texts.forEach { committedText ->
            // Live resize: local values while the handle is dragged; the ViewModel
            // gets ONE commit when the drag ends (no store update per drag event).
            val liveBox = remember(committedText.id) { mutableStateOf<FloatArray?>(null) }
            val liveVals = liveBox.value
            val sizedText0 = if (liveVals == null || liveVals.size < 3) {
                committedText
            } else {
                val k = if (committedText.fontSizeSp > 0.01f) liveVals[2] / committedText.fontSizeSp else 1f
                committedText.copy(
                    boxWidthFrac = liveVals[0],
                    boxHeightFrac = liveVals[1],
                    fontSizeSp = liveVals[2],
                    userFontSp = liveVals[2],
                    borderWidthDp = (committedText.borderWidthDp * k).coerceIn(0.5f, 8f),
                    bgCornerDp = (committedText.bgCornerDp * k).coerceIn(0f, 24f),
                    // Rotated resize also moves the box (fixed opposite corner).
                    xFraction = if (liveVals.size >= 5) liveVals[3] else committedText.xFraction,
                    yFraction = if (liveVals.size >= 5) liveVals[4] else committedText.yFraction
                )
            }
            // Position clamp (render time): old projects, undo, new boxes, resize and pinch all
            // keep the box inside the page on every side (8dp margin, rotated visual edge).
            val posRowSize = remember(committedText.id) { mutableStateOf(IntSize.Zero) }
            val posDensity = LocalDensity.current
            // fix30: free boxes (Text Box Size / Text Move) may cross the page border.
            // Size and position never depend on the rotation (rotation only turns the box).
            val posXY = if (sizedText0.freeBounds) floatArrayOf(sizedText0.xFraction, sizedText0.yFraction) else clampTextBoxPos(
                sizedText0.xFraction,
                sizedText0.yFraction,
                sizePx.width.toFloat(),
                sizePx.height.toFloat(),
                posRowSize.value.width.toFloat(),
                posRowSize.value.height.toFloat(),
                0f,
                with(posDensity) { 8.dp.toPx() },
                with(posDensity) { 24.dp.toPx() },
                with(posDensity) { 20.dp.toPx() }
            )
            // Left / top dynamic cap: the stored (virtual) position may be left of / above the
            // margin. The box is drawn at the margin and loses that overflow from its width /
            // height, so its right / bottom edge stays put (same feel as the right / bottom cap).
            // Rotated boxes: plain clamp (no cap), as in fix12.
            // Rotated boxes too: the visual (AABB) overflow is turned into a cut of the box's own
            // width / height via cos / sin (near 90 deg the axes swap). posOverL = width cut,
            // posOverT = height cut. posExX / posExY = extra AABB reach on the right / bottom.
            // fix30: rotation-independent size (was the rotated AABB, which shrank text while rotating).
            val posRad = 0f
            val posC = kotlin.math.abs(kotlin.math.cos(posRad))
            val posS = kotlin.math.abs(kotlin.math.sin(posRad))
            val posUnrot = kotlin.math.abs(sizedText0.rotationDeg) <= 0.5f
            val posOvX = if (sizePx.width > 0) ((posXY[0] - sizedText0.xFraction) * sizePx.width).coerceAtLeast(0f) else 0f
            val posOvY = if (sizePx.height > 0) ((posXY[1] - sizedText0.yFraction) * sizePx.height).coerceAtLeast(0f) else 0f
            val posSwap = !posUnrot && posS > posC
            val posK = if (posUnrot) 1f else maxOf(if (posSwap) posS else posC, 0.35f)
            val posOverL = (if (posSwap) posOvY else posOvX) / posK
            val posOverT = (if (posSwap) posOvX else posOvY) / posK
            val posRw = posRowSize.value.width.toFloat()
            val posRh = posRowSize.value.height.toFloat()
            val posExX = if (posUnrot || posRw <= 0f) 0f else ((posRw * posC + posRh * posS - posRw) / 2f).coerceAtLeast(0f)
            val posExY = if (posUnrot || posRh <= 0f) 0f else ((posRw * posS + posRh * posC - posRh) / 2f).coerceAtLeast(0f)
            val posOverLi = posOverL.toInt()
            val posOverTi = posOverT.toInt()
            val posOverState = rememberUpdatedState(floatArrayOf(posOverL, posOverT))
            val sizedText = if (posXY[0] == sizedText0.xFraction && posXY[1] == sizedText0.yFraction) {
                sizedText0
            } else {
                sizedText0.copy(xFraction = posXY[0], yFraction = posXY[1])
            }
            // Auto-fit: font = min(user font, largest size whose text fits inside the box).
            // Recomputed only when text, box size, user font or font family change.
            val fitDensity = LocalDensity.current
            val autoFitMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
            val fitUserSp = (if (sizedText.userFontSp > 0.01f) sizedText.userFontSp else sizedText.fontSizeSp)
                .coerceIn(0.5f, 96f)
            val fitPageW = sizePx.width
            val fitPageH = sizePx.height
            val fitEdge = with(fitDensity) { 8.dp.toPx() }
            val fitMinW = with(fitDensity) { 24.dp.toPx() }
            val fitMinH = with(fitDensity) { 20.dp.toPx() }
            val fitQuoteW = if (sizedText.quoteRanges.isNotEmpty()) with(fitDensity) { 9.dp.toPx() } else 0f
            val fitCapW = if (sizedText.freeBounds) fitPageW * 50f + fitMinW else (fitPageW * (1f - sizedText.xFraction) - fitEdge - posExX).coerceAtLeast(fitMinW)
            val fitCapH = if (sizedText.freeBounds) fitPageH * 50f + fitMinH else (fitPageH * (1f - sizedText.yFraction) - fitEdge - posExY).coerceAtLeast(fitMinH)
            val fitWrapPx = (
                if (sizedText.boxWidthFrac > 0.01f) {
                    ((sizedText.boxWidthFrac * fitPageW).coerceIn(fitMinW, maxOf(fitCapW, fitMinW)) - posOverL)
                        .coerceAtLeast(fitMinW)
                } else {
                    fitCapW
                } - fitQuoteW
                ).coerceAtLeast(1f)
            val fitAvailH = if (sizedText.boxHeightFrac > 0.01f) {
                ((sizedText.boxHeightFrac * fitPageH).coerceIn(fitMinH, maxOf(fitCapH, fitMinH)) - posOverT)
                    .coerceAtLeast(fitMinH)
            } else {
                fitCapH
            }
            // Real room left on the page (raw, no 24x20dp minimum) so tiny boxes near the
            // edge get a smaller font instead of overflowing.
            val fitRoomW = if (sizedText.freeBounds) fitPageW * 50f + 1f else (fitPageW * (1f - sizedText.xFraction) - fitEdge - fitQuoteW - posExX).coerceAtLeast(1f)
            val fitRoomH = if (sizedText.freeBounds) fitPageH * 50f + 1f else (fitPageH * (1f - sizedText.yFraction) - fitEdge - posExY).coerceAtLeast(1f)
            val fitClipHpx = minOf(fitAvailH, fitRoomH)
            val fitMaxWi = (minOf(fitWrapPx, fitRoomW) * 0.99f).toInt().coerceAtLeast(1)
            val fitMaxHi = (fitClipHpx * 0.99f).toInt().coerceAtLeast(1)
            // Cached: custom fonts are loaded from file on every resolve call.
            val fitFamily = remember(sizedText.fontId, customFonts) {
                FontCatalog.resolveComposeFontFamily(sizedText.fontId, customFonts)
            }
            val fitRangeFamilies = remember(sizedText.fontId, sizedText.fontRanges, customFonts) {
                (listOf(sizedText.fontId) + sizedText.fontRanges.map { it.fontId })
                    .distinct()
                    .associateWith { FontCatalog.resolveComposeFontFamily(it, customFonts) }
            }
            // One transformation instance per style change (not per recomposition).
            val visualTx = remember(
                sizedText.boldRanges, sizedText.italicRanges, sizedText.underlineRanges,
                sizedText.strikeRanges, sizedText.quoteRanges, sizedText.fontRanges,
                fitRangeFamilies, sizedText.colorRanges, sizedText.linkRanges, sizedText.shadowRanges,
                sizedText.textColorArgb
            ) {
                TextStyleRangesVisualTransformation(
                    boldRanges = sizedText.boldRanges,
                    italicRanges = sizedText.italicRanges,
                    underlineRanges = sizedText.underlineRanges,
                    strikeRanges = sizedText.strikeRanges,
                    quoteRanges = sizedText.quoteRanges,
                    fontRanges = sizedText.fontRanges,
                    fontFamilies = fitRangeFamilies,
                    colorRanges = sizedText.colorRanges,
                    // Backgrounds are drawn in drawBehind (rounded + opacity).
                    bgColorRanges = emptyList(),
                    wholeBgColorArgb = null,
                    linkRanges = sizedText.linkRanges,
                    shadowRanges = sizedText.shadowRanges,
                    wholeTextColorArgb = sizedText.textColorArgb
                )
            }
            val lastFit = remember(committedText.id) { floatArrayOf(0f) }
            val fitInfo = remember(sizedText.letterSpacingEm, sizedText.lineSpacing, sizedText.text, fitMaxWi, fitMaxHi, fitUserSp, visualTx, fitFamily, fitPageW, fitPageH, posOverLi, posOverTi) {
                if (fitPageW <= 0 || fitPageH <= 0) {
                    floatArrayOf(sizedText.fontSizeSp, -1f)
                } else {
                    // Full styled text (bold / italic / range fonts) exactly as the field draws it.
                    val styled = visualTx.filter(AnnotatedString(sizedText.text)).text
                    val fitStyle = TextStyle(
                        fontFamily = fitFamily,
                        letterSpacing = com.pdfcraft.studio.ui.editor.textLetterSpacing(sizedText), // fix43
                        lineHeight = com.pdfcraft.studio.ui.editor.textLineHeight(sizedText),
                        platformStyle = PlatformTextStyle(includeFontPadding = true)
                    )
                    // Effective limits (auto boxes lose the left / top overflow inside here).
                    val fitWi = intArrayOf(fitMaxWi)
                    val fitHi = intArrayOf(fitMaxHi)
                    fun measureAt(spv: Float) = autoFitMeasurer.measure(
                        text = styled,
                        style = fitStyle.copy(fontSize = spv.sp),
                        softWrap = true,
                        constraints = androidx.compose.ui.unit.Constraints(maxWidth = fitWi[0].coerceAtLeast(1)),
                        density = fitDensity
                    )
                    val autoWBox = sizedText.boxWidthFrac <= 0.01f
                    val autoHBox = sizedText.boxHeightFrac <= 0.01f
                    if ((posOverLi > 0 && autoWBox) || (posOverTi > 0 && autoHBox)) {
                        // Natural size (no cap) at the user font, minus the overflow.
                        val nat = measureAt(fitUserSp)
                        if (posOverLi > 0 && autoWBox) {
                            fitWi[0] = minOf(fitMaxWi, nat.size.width - posOverLi)
                                .coerceAtLeast(maxOf(1, (fitMinW * 0.5f).toInt()))
                        }
                        if (posOverTi > 0 && autoHBox) {
                            fitHi[0] = minOf(fitMaxHi, nat.size.height - posOverTi)
                                .coerceAtLeast(maxOf(1, (fitMinH * 0.5f).toInt()))
                        }
                    }
                    val atUser = measureAt(fitUserSp)
                    val res: FloatArray = if (atUser.size.height <= fitHi[0]) {
                        floatArrayOf(fitUserSp, atUser.size.width.toFloat())
                    } else {
                        // Fast path: the previous fitted size is still right (2 measures, no search).
                        val prev = lastFit[0]
                        var quick: FloatArray? = null
                        if (prev >= 0.5f && prev < fitUserSp) {
                            val atPrev = measureAt(prev)
                            if (atPrev.size.height <= fitHi[0] &&
                                measureAt(minOf(fitUserSp, prev * 1.02f)).size.height > fitHi[0]
                            ) {
                                quick = floatArrayOf(prev, atPrev.size.width.toFloat())
                            }
                        }
                        quick ?: run {
                            var lo = 0.5f
                            var hi = fitUserSp
                            repeat(12) {
                                val mid = (lo + hi) / 2f
                                if (measureAt(mid).size.height <= fitHi[0]) lo = mid else hi = mid
                            }
                            floatArrayOf(lo, measureAt(lo).size.width.toFloat())
                        }
                    }
                    lastFit[0] = res[0]
                    res
                }
            }
            val fittedSp = fitInfo[0].coerceIn(0.5f, 96f)
            val textElement = if (fitPageW > 0 && fitPageH > 0 &&
                kotlin.math.abs(fittedSp - sizedText.fontSizeSp) > 0.01f
            ) {
                val kf = if (sizedText.fontSizeSp > 0.01f) fittedSp / sizedText.fontSizeSp else 1f
                sizedText.copy(
                    fontSizeSp = fittedSp,
                    borderWidthDp = (sizedText.borderWidthDp * kf).coerceIn(0.5f, 8f),
                    bgCornerDp = (sizedText.bgCornerDp * kf).coerceIn(0f, 24f)
                )
            } else {
                sizedText
            }
            // Commit the fitted font (not during a live drag; the drag commits on release).
            val autoFitCb = rememberUpdatedState(onTextAutoFit)
            LaunchedEffect(committedText.id, fittedSp, liveVals == null, committedText.fontSizeSp) {
                if (liveBox.value == null && fitPageW > 0 && fitPageH > 0 &&
                    kotlin.math.abs(fittedSp - committedText.fontSizeSp) > 0.01f
                ) {
                    autoFitCb.value(committedText.id, fittedSp)
                }
            }
            var fieldValue by remember(textElement.id) {
                mutableStateOf(TextFieldValue(text = textElement.text))
            }
            LaunchedEffect(textElement.text) {
                if (fieldValue.text != textElement.text) {
                    val len = textElement.text.length
                    fieldValue = TextFieldValue(
                        text = textElement.text,
                        selection = TextRange(
                            fieldValue.selection.start.coerceIn(0, len),
                            fieldValue.selection.end.coerceIn(0, len)
                        )
                    )
                }
            }
            val focusRequester = remember { FocusRequester() }
            val tapKeyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val density = LocalDensity.current
            val fontPx = with(density) { textElement.fontSizeSp.sp.toPx() }
            val measurePaint = remember(fontPx) {
                android.graphics.Paint().apply {
                    textSize = fontPx
                    isAntiAlias = true
                }
            }
            val minBoxPx = measurePaint.measureText("000000000000000")
            val longest = fieldValue.text.split('\n').maxByOrNull { it.length } ?: ""
            val contentPx = if (longest.isEmpty()) {
                0f
            } else if (fitInfo[1] > 0f && fieldValue.text == textElement.text) {
                // Width of the fitted layout (real font family), so the auto box never under-wraps.
                fitInfo[1] / 0.99f
            } else {
                measurePaint.measureText(longest)
            }
            val padPx = with(density) { 10.dp.toPx() }
            // Box stays inside the page: from its position to the right / bottom page border.
            val pageEdgePx = with(density) { 8.dp.toPx() }
            val capW = if (textElement.freeBounds && sizePx.width > 0) {
                sizePx.width * 50f
            } else if (sizePx.width > 0) {
                (sizePx.width * (1f - textElement.xFraction) - pageEdgePx - posExX)
                    .coerceAtLeast(with(density) { 24.dp.toPx() })
            } else {
                (sizePx.width.toFloat() * 3f).coerceAtLeast(minBoxPx)
            }
            val capH = if (textElement.freeBounds && sizePx.height > 0) {
                sizePx.height * 50f
            } else if (sizePx.height > 0) {
                (sizePx.height * (1f - textElement.yFraction) - pageEdgePx - posExY)
                    .coerceAtLeast(with(density) { 20.dp.toPx() })
            } else {
                (sizePx.height.toFloat() * 3f).coerceAtLeast(fontPx)
            }
            val fitMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
            val fitContext = androidx.compose.ui.platform.LocalContext.current
            val autoW = (if (fieldValue.text.isEmpty()) minBoxPx else contentPx + padPx)
                .coerceAtMost(capW)
                .coerceAtLeast(with(density) { 8.dp.toPx() })
            val pageWpx = sizePx.width.toFloat().coerceAtLeast(1f)
            val pageHpx = sizePx.height.toFloat().coerceAtLeast(1f)
            val boxWpx = if (textElement.boxWidthFrac > 0.01f) {
                ((textElement.boxWidthFrac * pageWpx).coerceIn(with(density) { 24.dp.toPx() }, maxOf(capW, with(density) { 24.dp.toPx() })) - posOverL)
                    .coerceAtLeast(with(density) { 24.dp.toPx() })
            } else {
                autoW
            }
            val lineCount = 1 + fieldValue.text.count { it == '\n' }
            val autoH = (lineCount * fontPx * 1.35f * textElement.lineSpacing + padPx).coerceAtMost(capH) // fix43
            val boxHpx = if (textElement.boxHeightFrac > 0.01f) {
                ((textElement.boxHeightFrac * pageHpx).coerceIn(with(density) { 20.dp.toPx() }, maxOf(capH, with(density) { 20.dp.toPx() })) - posOverT)
                    .coerceAtLeast(with(density) { 20.dp.toPx() })
            } else {
                autoH
            }
            val boxWidthDp = with(density) { boxWpx.toDp() }
            val boxHeightDp = with(density) { boxHpx.toDp() }
            var boxMeasured by remember(textElement.id) { mutableStateOf(IntSize.Zero) }
            var rowMeasured by remember(textElement.id) { mutableStateOf(IntSize.Zero) }
            // Drag works on the stored (virtual) position; the drawn one is clamped.
            val xFracState = rememberUpdatedState(sizedText0.xFraction)
            val yFracState = rememberUpdatedState(sizedText0.yFraction)
            val freeState = rememberUpdatedState(sizedText0.freeBounds)
            val dragCoords = remember(committedText.id) { arrayOfNulls<LayoutCoordinates>(1) }
            val rotState = rememberUpdatedState(textElement.rotationDeg)
            val sizeSpState = rememberUpdatedState(textElement.fontSizeSp)
            // User's own size (handle / pinch scale this, not the auto-fitted size).
            val userSpState = rememberUpdatedState(fitUserSp)
            val sizeState = rememberUpdatedState(sizePx)
            val boxWState = rememberUpdatedState(boxWpx)
            val boxHState = rememberUpdatedState(boxHpx)
            val selected = textElement.id == selectedTextId
            var rotHud by remember(textElement.id) { mutableStateOf(false) }
            var rawRot by remember(textElement.id) { mutableStateOf(0f) }
            var sizeHud by remember(textElement.id) { mutableStateOf(false) }
            var boxHud by remember(textElement.id) { mutableStateOf(false) }
            // Edit mode = this field has focus (cursor + keyboard).
            var editFocused by remember(textElement.id) { mutableStateOf(false) }
            val selectedNow = rememberUpdatedState(selected)
            var textLayout by remember(textElement.id) {
                mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null)
            }
            // Real preview line breaks for the PDF (only when the layout matches the saved box).
            val cacheLay = textLayout
            LaunchedEffect(cacheLay, committedText, posXY[0], posXY[1], posRowSize.value, sizePx) {
                val lay = cacheLay
                val pw = sizePx.width.toFloat()
                val ph = sizePx.height.toFloat()
                val fs = lay?.layoutInput?.style?.fontSize
                if (lay != null && pw > 0f && ph > 0f && fs != null && fs.isSp &&
                    kotlin.math.abs(fs.value - committedText.fontSizeSp) < 0.01f &&
                    lay.layoutInput.text.text == committedText.text
                ) {
                    val txt = committedText.text
                    val n = lay.lineCount
                    val st = IntArray(n)
                    val en = IntArray(n)
                    for (l in 0 until n) {
                        val s = lay.getLineStart(l).coerceIn(0, txt.length)
                        var e = lay.getLineEnd(l, visibleEnd = false).coerceIn(s, txt.length)
                        while (e > s && (txt[e - 1] == '\n' || txt[e - 1] == '\r')) e--
                        st[l] = s
                        en[l] = e
                    }
                    val fontPx = with(density) { fs.toPx() }
                    com.pdfcraft.studio.core.pdf.PdfTextLineCache.put(
                        committedText.id,
                        com.pdfcraft.studio.core.pdf.PdfTextLineCache.Entry(
                            text = txt,
                            fontSp = committedText.fontSizeSp,
                            boxWFrac = committedText.boxWidthFrac,
                            boxHFrac = committedText.boxHeightFrac,
                            rawX = committedText.xFraction,
                            rawY = committedText.yFraction,
                            rotDeg = committedText.rotationDeg,
                            effX = posXY[0],
                            effY = posXY[1],
                            fontPerPageW = fontPx / pw,
                            textHFrac = lay.size.height / ph,
                            rowWFrac = posRowSize.value.width / pw,
                            rowHFrac = posRowSize.value.height / ph,
                            starts = st,
                            ends = en,
                            styleKey = com.pdfcraft.studio.core.pdf.PdfTextLineCache.styleKeyOf(committedText)
                        )
                    )
                }
            }
            val bgChars = remember(textElement.text.length, textElement.bgColorArgb, textElement.bgColorRanges) {
                charColorsFor(textElement.text.length, textElement.bgColorArgb, textElement.bgColorRanges)
            }
            val hasBg = remember(bgChars) { bgChars.any { it != null } }
            val bgKeep = remember(textElement.text.length, textElement.bgColorRanges) {
                bgKeepMask(textElement.text.length, textElement.bgColorRanges)
            }
            // Background rects computed once per layout / colors, not on every draw frame.
            val bgRuns = remember(textLayout, bgChars, bgKeep, hasBg) {
                val lay = textLayout
                if (lay == null || !hasBg) null else computeTextBgRuns(lay, bgChars, bgKeep)
            }
            val borderChars = remember(textElement.text.length, textElement.borderColorArgb, textElement.borderRanges) {
                charColorsFor(textElement.text.length, textElement.borderColorArgb, textElement.borderRanges)
            }
            val hasBorder = remember(borderChars) { borderChars.any { it != null } }
            val borderMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
            val borderLayout = remember(
                textLayout, hasBorder, textElement.text, textElement.borderColorArgb,
                textElement.borderRanges, textElement.borderWidthDp
            ) {
                val lay = textLayout
                if (!hasBorder || lay == null || lay.layoutInput.text.length != borderChars.size) {
                    null
                } else {
                    val inp = lay.layoutInput
                    val strokePx = with(density) { textElement.borderWidthDp.dp.toPx() }
                    borderMeasurer.measure(
                        text = buildBorderText(inp.text, borderChars, strokePx),
                        style = inp.style.copy(shadow = null),
                        overflow = inp.overflow,
                        softWrap = inp.softWrap,
                        maxLines = inp.maxLines,
                        placeholders = inp.placeholders,
                        constraints = inp.constraints,
                        layoutDirection = inp.layoutDirection,
                        density = inp.density,
                        fontFamilyResolver = inp.fontFamilyResolver
                    )
                }
            }
            val spreadChars = remember(textElement.text.length, textElement.spreadColorArgb, textElement.spreadRanges) {
                charColorsFor(textElement.text.length, textElement.spreadColorArgb, textElement.spreadRanges)
            }
            val hasSpread = remember(spreadChars, textElement.spreadDp) {
                textElement.spreadDp > 0.01f && spreadChars.any { it != null }
            }
            val spreadLayout = remember(
                textLayout, hasSpread, textElement.text, textElement.spreadColorArgb,
                textElement.spreadRanges, textElement.spreadDp
            ) {
                val lay = textLayout
                if (!hasSpread || lay == null || lay.layoutInput.text.length != spreadChars.size) {
                    null
                } else {
                    val inp = lay.layoutInput
                    val glowPx = with(density) { textElement.spreadDp.dp.toPx() }.coerceAtLeast(0.01f)
                    borderMeasurer.measure(
                        text = buildSpreadText(inp.text, spreadChars, glowPx),
                        style = inp.style.copy(shadow = null),
                        overflow = inp.overflow,
                        softWrap = inp.softWrap,
                        maxLines = inp.maxLines,
                        placeholders = inp.placeholders,
                        constraints = inp.constraints,
                        layoutDirection = inp.layoutDirection,
                        density = inp.density,
                        fontFamilyResolver = inp.fontFamilyResolver
                    )
                }
            }
            val ants = rememberInfiniteTransition(label = "ants")
            val dashPhase by ants.animateFloat(
                initialValue = 0f,
                targetValue = 17f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "dash"
            )

            val handlePadPx = with(density) { 16.dp.toPx() }
            // Box layout coords: its center = rotation pivot (page frame via pageRef).
            val outerRef = remember(committedText.id) { arrayOfNulls<LayoutCoordinates>(1) }
            Box(
                modifier = Modifier
                    // Selected box is on top: it gets every touch inside it, so a box
                    // underneath (or on top in list order) can never steal the selection.
                    .zIndex(if (selected) 1f else 0f)
                    // fix30: free box is measured without the page width limit (no page-edge wrap).
                    .then(if (textElement.freeBounds) Modifier.wrapContentSize(Alignment.TopStart, unbounded = true) else Modifier)
                    .offset {
                        val pw = sizePx.width.toFloat().coerceAtLeast(1f)
                        val ph = sizePx.height.toFloat().coerceAtLeast(1f)
                        IntOffset(
                            (textElement.xFraction * pw - handlePadPx).roundToInt(),
                            (textElement.yFraction * ph - handlePadPx).roundToInt()
                        )
                    }
                    .onSizeChanged { boxMeasured = it }
                    .onGloballyPositioned { outerRef[0] = it }
                    .graphicsLayer { rotationZ = textElement.rotationDeg }
            ) {
            Box(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .onSizeChanged {
                        rowMeasured = it
                        posRowSize.value = it
                    }
                    .elementFrame(textElement.id, 0.dp) // fix55
                    .then(
                        if (selected && !hideSelectionChrome) {
                            Modifier.drawWithContent {
                                // Selection outline is drawn ABOVE text background.
                                drawContent()
                                drawRoundRect(
                                    color = Color(0xFF333333),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
                                    style = Stroke(
                                        width = 1.6f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), dashPhase)
                                    )
                                )
                            }
                        } else Modifier
                    )
                    .padding(4.dp)
                    .onGloballyPositioned { dragCoords[0] = it }
                    .pointerInput(textElement.id) {
                        val slop = viewConfiguration.touchSlop
                        fun pagePos(p: Offset): Offset? {
                            val pr = pageRef[0] ?: return null
                            val dc = dragCoords[0] ?: return null
                            if (!pr.isAttached || !dc.isAttached) return null
                            return try { pr.localPositionOf(dc, p) } catch (e: Exception) { null }
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            TextBoxTouch.downUptime = down.uptimeMillis
                            var dragging = false
                            var pinching = false
                            var xf = xFracState.value
                            var yf = yFracState.value
                            var sp = userSpState.value
                            var startPage: Offset? = null
                            var startXf = xf
                            var startYf = yf
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break
                                if (pressed.size >= 2) {
                                    pinching = true
                                    dragging = false
                                    sizeHud = true
                                    val z = event.calculateZoom()
                                    if (z != 1f && z > 0f) {
                                        pressed.forEach { it.consume() }
                                        sp = (sp * z).coerceIn(6f, 96f)
                                        onTextSelect(textElement.id)
                                        onTextScale(textElement.id, sp)
                                        sizeHud = true
                                    }
                                    continue
                                }
                                if (pinching) break
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.changedToUpIgnoreConsumed() || !change.pressed) break
                                if (!dragging) {
                                    if ((change.position - down.position).getDistance() > slop) {
                                        dragging = true
                                        xf = xFracState.value
                                        yf = yFracState.value
                                        // Anchor at the touch-down point: slop movement is not lost.
                                        startPage = pagePos(down.position)
                                        startXf = xf
                                        startYf = yf
                                        onTextSelect(textElement.id)
                                    }
                                }
                                if (dragging) {
                                    change.consume()
                                    val sz = sizeState.value
                                    if (sz.width > 0 && sz.height > 0) {
                                        val bw = if (rowMeasured.width > 0) rowMeasured.width.toFloat() else boxWState.value
                                        val bh = if (rowMeasured.height > 0) rowMeasured.height.toFloat() else boxHState.value
                                        // Fixed page-frame only (fix18/fix20 style). Skip if the
                                        // frame is not ready — never fall back to rotated local
                                        // deltas (those caused drag drift).
                                        val sp0 = startPage
                                        val pNow = if (sp0 != null) pagePos(change.position) else null
                                        if (sp0 != null && pNow != null) {
                                            val nx = startXf + (pNow.x - sp0.x) / sz.width
                                            val ny = startYf + (pNow.y - sp0.y) / sz.height
                                            // fix30: free box: finger drag has no page clamp either.
                                            if (freeState.value) {
                                                onTextDrag(textElement.id, nx, ny)
                                                continue
                                            }
                                            // Live clamp on all four sides (visual edge, 8dp margin).
                                            val cl = clampTextBoxPos(
                                                nx,
                                                ny,
                                                sz.width.toFloat(),
                                                sz.height.toFloat(),
                                                bw,
                                                bh,
                                                rotState.value,
                                                8.dp.toPx(),
                                                24.dp.toPx(),
                                                20.dp.toPx()
                                            )
                                            // Virtual position for rotated boxes too (fix14).
                                            if (!rotState.value.isNaN()) {
                                                // Past the left / top margin: keep a virtual position so the
                                                // right / bottom edge stays put; box keeps at least 24x20dp.
                                                val natW = bw + ((cl[0] - xf) * sz.width).coerceAtLeast(0f)
                                                val natH = bh + ((cl[1] - yf) * sz.height).coerceAtLeast(0f)
                                                val minXv = cl[0] - ((natW - 24.dp.toPx()) / sz.width).coerceAtLeast(0f)
                                                val minYv = cl[1] - ((natH - 20.dp.toPx()) / sz.height).coerceAtLeast(0f)
                                                xf = if (nx < cl[0]) nx.coerceIn(minOf(minXv, cl[0]), cl[0]) else cl[0]
                                                yf = if (ny < cl[1]) ny.coerceIn(minOf(minYv, cl[1]), cl[1]) else cl[1]
                                            } else {
                                                xf = cl[0]
                                                yf = cl[1]
                                            }
                                            onTextDrag(textElement.id, xf, yf)
                                        }
                                    }
                                }
                            }
                            sizeHud = false
                        }
                    }
            ) {
                if (textElement.quoteRanges.isNotEmpty()) {
                    val qLineH = textElement.fontSizeSp * 1.35f
                    val qLines = textElement.text.split('\n')
                    Column(modifier = Modifier.padding(end = 6.dp)) {
                        var qPos = 0
                        qLines.forEach { qLine ->
                            val qStart = qPos
                            val qEnd = qPos + qLine.length
                            val quoted = textElement.quoteRanges.any { r ->
                                r.first < qEnd && (r.last + 1) > qStart
                            }
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(qLineH.dp)
                                    .background(
                                        if (quoted) Color(0xFF1976D2) else Color.Transparent,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            qPos = qEnd + 1
                        }
                    }
                }
                val fontFamily = fitFamily // cached (custom fonts load from file)
                Box(
                    contentAlignment = Alignment.TopStart,
                    modifier = Modifier
                        // Own layer: the animated selection border no longer re-runs the
                        // text / background / glow drawing on every frame.
                        .graphicsLayer { }
                        .width(boxWidthDp)
                        .then(
                            if (textElement.boxHeightFrac > 0.01f) {
                                // Manual size is the minimum; long text grows the box (never clipped).
                                Modifier.heightIn(min = boxHeightDp)
                            } else {
                                Modifier
                            }
                        )
                ) {
                // fix44: font-height cursor when line spacing is changed
                val spacedCursor = com.pdfcraft.studio.ui.editor.usesSpacedCursor(textElement)
                val spacedCursorOn = com.pdfcraft.studio.ui.editor.spacedCursorBlinkOn(spacedCursor && editFocused)
                BasicTextField(
                    value = fieldValue,
                    onValueChange = { newValue ->
                        // No text limit: every edit / paste is accepted; the font auto-fits
                        // (see fitInfo) so the text always stays inside the box.
                        fieldValue = newValue
                        onTextValueChange(textElement.id, newValue.text, newValue.selection)
                    },
                    visualTransformation = visualTx,
                    textStyle = TextStyle(
                        color = Color(textElement.textColorArgb),
                        fontSize = textElement.fontSizeSp.sp,
                        fontFamily = fontFamily,
                        letterSpacing = com.pdfcraft.studio.ui.editor.textLetterSpacing(textElement), // fix43
                        lineHeight = com.pdfcraft.studio.ui.editor.textLineHeight(textElement),
                        platformStyle = PlatformTextStyle(includeFontPadding = true),
                        textAlign = when (textElement.textAlign.uppercase()) {
                            "CENTER" -> TextAlign.Center
                            "RIGHT" -> TextAlign.Right
                            "JUSTIFY" -> TextAlign.Justify
                            else -> TextAlign.Start
                        },
                        shadow = if (
                            textElement.shadowRanges.isEmpty() && (
                            textElement.shadowBlurPx > 0.01f ||
                            textElement.shadowOffsetXPx != 0f ||
                            textElement.shadowOffsetYPx != 0f
                            )
                        ) {
                            Shadow(
                                color = Color(textElement.shadowColorArgb),
                                offset = Offset(textElement.shadowOffsetXPx, textElement.shadowOffsetYPx),
                                blurRadius = textElement.shadowBlurPx
                            )
                        } else null
                    ),
                    cursorBrush = SolidColor(if (spacedCursor) Color.Transparent else Color.Black), // fix44
                    onTextLayout = { res ->
                        val prev = textLayout
                        if (prev == null || prev.layoutInput != res.layoutInput || prev.size != res.size) textLayout = res
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(Alignment.Top, unbounded = true)
                        .drawBehind {
                            val lay = textLayout
                            val runs = bgRuns
                            if (lay != null && hasBg && runs != null) {
                                drawTextBackgrounds(
                                    runs,
                                    textElement.bgCornerDp.dp.toPx(),
                                    textElement.bgAlpha,
                                    textElement.bgPadLeftDp.dp.toPx(),
                                    textElement.bgPadTopDp.dp.toPx(),
                                    textElement.bgPadRightDp.dp.toPx(),
                                    textElement.bgPadBottomDp.dp.toPx()
                                )
                            }
                            val sLay = spreadLayout
                            if (sLay != null) {
                                // Spread glow: above background, below border and text.
                                drawText(textLayoutResult = sLay)
                                drawText(textLayoutResult = sLay)
                            }
                            val bLay = borderLayout
                            if (bLay != null) drawText(textLayoutResult = bLay)
                        }
                        .drawWithContent {
                            // Last safety: glyphs never leave the box / page room. Background,
                            // padding, glow and outline (drawBehind above) are not clipped.
                            if (fitPageW > 0 && fitPageH > 0) {
                                val clipH = minOf(size.height, fitClipHpx) + 2f
                                drawContext.canvas.save()
                                drawContext.canvas.clipRect(-2f, -2f, size.width + 2f, clipH)
                                drawContent()
                                drawContext.canvas.restore()
                            } else {
                                drawContent()
                            }
                        }
                        .drawWithContent {
                            drawContent()
                            // fix44: own cursor (normal font height, not the spaced line height)
                            val cLay = textLayout
                            if (spacedCursorOn && cLay != null && fieldValue.selection.collapsed) {
                                runCatching {
                                    val tOff = visualTx.filter(fieldValue.annotatedString).offsetMapping
                                        .originalToTransformed(fieldValue.selection.start)
                                    com.pdfcraft.studio.ui.editor.drawSpacedCursor(
                                        this, cLay, tOff, textElement.fontSizeSp.sp.toPx(), Color.Black
                                    )
                                }
                            }
                        }
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            editFocused = state.isFocused
                            if (state.isFocused) {
                                onTextFocused(textElement.id)
                            } else {
                                onTextUnfocused(textElement.id)
                            }
                        }
                )
                if (!editFocused) {
                    // 1st tap selects the box only. 2nd tap on the selected box
                    // focuses the field (cursor + keyboard).
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(textElement.id) {
                                detectTapGestures(
                                    onTap = { tapPos ->
                                        if (selectedNow.value) {
                                            // Cursor goes to the character under the finger.
                                            val lay = textLayout
                                            if (lay != null) {
                                                val off = lay.getOffsetForPosition(tapPos)
                                                    .coerceIn(0, fieldValue.text.length)
                                                fieldValue = fieldValue.copy(selection = TextRange(off))
                                                onTextValueChange(textElement.id, fieldValue.text, TextRange(off))
                                            }
                                            focusRequester.requestFocus()
                                            tapKeyboard?.show()
                                        } else {
                                            focusManager.clearFocus(force = true)
                                            onTextSelect(textElement.id)
                                        }
                                    }
                                )
                            }
                    )
                }
                }
            }
            }

            if (selected && !hideSelectionChrome) {
                val spinView = androidx.compose.ui.platform.LocalView.current
                val lastSnap = remember(textElement.id) { floatArrayOf(Float.NaN) }
                // Rotation drag: start angle, pivot (page frame), unwrapped angle sum, last angle.
                val rotG = remember(textElement.id) { floatArrayOf(0f, Float.NaN, Float.NaN, 0f, Float.NaN) }
                fun rotApply() {
                    val snapped = snapRotationDeg(rawRot)
                    var shown = ((snapped % 360f) + 360f) % 360f
                    if (shown > 180f) shown -= 360f
                    val step = kotlin.math.round(shown / 10f) * 10f
                    val onMagnet = kotlin.math.abs(shown - step) < 0.8f
                    val prevSnap = lastSnap[0]
                    if (onMagnet && !prevSnap.isNaN() && kotlin.math.abs(prevSnap - snapped) > 0.5f) {
                        spinView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    }
                    lastSnap[0] = snapped
                    onTextRotate(textElement.id, snapped)
                }
                fun rotCenter(): Offset? {
                    val pr = pageRef[0] ?: return null
                    val orr = outerRef[0] ?: return null
                    if (!pr.isAttached || !orr.isAttached) return null
                    return try {
                        pr.localPositionOf(orr, Offset(orr.size.width / 2f, orr.size.height / 2f))
                    } catch (e: Exception) {
                        null
                    }
                }
                TextHandle(
                    symbol = "\u21bb",
                    modifier = Modifier.align(Alignment.TopEnd),
                    onDragStart = {
                        rotHud = true
                        rawRot = rotState.value
                        lastSnap[0] = Float.NaN
                        rotG[0] = rawRot
                        val c = rotCenter()
                        rotG[1] = c?.x ?: Float.NaN
                        rotG[2] = c?.y ?: Float.NaN
                        rotG[3] = 0f
                        rotG[4] = Float.NaN
                    },
                    onDragEnd = { rotHud = false },
                    pageRef = pageRef,
                    onPagePos = { start, now ->
                        // Finger angle around the box center in the fixed page frame:
                        // works anywhere, also when the finger passes over the box.
                        onTextSelect(textElement.id)
                        if (rotG[1].isNaN()) {
                            val c = rotCenter()
                            if (c != null) {
                                rotG[1] = c.x
                                rotG[2] = c.y
                            }
                        }
                        val cx = rotG[1]
                        val cy = rotG[2]
                        if (!cx.isNaN() && !cy.isNaN()) {
                            val toDeg = 180f / Math.PI.toFloat()
                            if (rotG[4].isNaN()) {
                                rotG[4] = kotlin.math.atan2(start.y - cy, start.x - cx) * toDeg
                            }
                            val dx = now.x - cx
                            val dy = now.y - cy
                            // Too close to the pivot: angle is noise, wait for the finger.
                            if (dx * dx + dy * dy > handlePadPx * handlePadPx * 0.0625f) {
                                val a = kotlin.math.atan2(dy, dx) * toDeg
                                var d = a - rotG[4]
                                while (d > 180f) d -= 360f
                                while (d < -180f) d += 360f
                                rotG[3] += d
                                rotG[4] = a
                                rawRot = rotG[0] + rotG[3]
                                rotApply()
                            }
                        }
                    }
                ) { drag ->
                    // Fallback (no page frame): old local-delta rotation.
                    onTextSelect(textElement.id)
                    val halfW = (if (rowMeasured.width > 0) rowMeasured.width else boxMeasured.width) / 2f
                    val halfH = (if (rowMeasured.height > 0) rowMeasured.height else boxMeasured.height) / 2f
                    val vx = (halfW + handlePadPx).coerceAtLeast(12f)
                    val vy = -(halfH + handlePadPx).coerceAtLeast(12f)
                    val r2 = (vx * vx + vy * vy).coerceAtLeast(1f)
                    val dDeg = ((vx * drag.y - vy * drag.x) / r2) * (180f / 3.14159265f)
                    rawRot += dDeg
                    rotApply()
                }
                val liveW = remember(textElement.id) { floatArrayOf(0f) }
                val liveH = remember(textElement.id) { floatArrayOf(0f) }
                val startBox = remember(textElement.id) { floatArrayOf(1f, 1f, 16f) }
                // Total finger travel (box axes) since drag start; size = start + travel.
                val accWH = remember(textElement.id) { floatArrayOf(0f, 0f) }
                // Stored (virtual) top-left at drag start; rotated boxes keep the opposite corner.
                val startXY = remember(textElement.id) { floatArrayOf(0f, 0f) }
                val widthFracState = rememberUpdatedState(textElement.boxWidthFrac)
                val heightFracState = rememberUpdatedState(textElement.boxHeightFrac)
                val resizeState = rememberUpdatedState(onTextBoxResize)
                TextHandle(
                    symbol = "\u2922",
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onDragStart = {
                        boxHud = true
                        val pageW = sizeState.value.width.toFloat().coerceAtLeast(1f)
                        val pageH = sizeState.value.height.toFloat().coerceAtLeast(1f)
                        liveW[0] = if (widthFracState.value > 0.01f) {
                            widthFracState.value * pageW
                        } else {
                            boxWState.value + posOverState.value[0]
                        }
                        val pad = with(density) { 8.dp.toPx() }
                        liveH[0] = if (heightFracState.value > 0.01f) {
                            maxOf(heightFracState.value * pageH, rowMeasured.height.toFloat() - pad)
                        } else {
                            (rowMeasured.height.toFloat() - pad + posOverState.value[1]).coerceAtLeast(with(density) { 20.dp.toPx() })
                        }
                        startBox[0] = liveW[0].coerceAtLeast(1f)
                        startBox[1] = liveH[0].coerceAtLeast(1f)
                        startBox[2] = userSpState.value
                        startXY[0] = xFracState.value
                        startXY[1] = yFracState.value
                        accWH[0] = 0f
                        accWH[1] = 0f
                    },
                    onDragEnd = {
                        boxHud = false
                        val lb = liveBox.value
                        if (lb != null && lb.size >= 3) {
                            // Single commit to the ViewModel (one undo step).
                            resizeState.value(
                                textElement.id, lb[0], lb[1], lb[2],
                                if (lb.size >= 5) lb[3] else Float.NaN,
                                if (lb.size >= 5) lb[4] else Float.NaN
                            )
                        }
                        liveBox.value = null
                    },
                    pageRef = pageRef
                ) { drag ->
                    onTextSelect(textElement.id)
                    val pageW = sizeState.value.width.toFloat().coerceAtLeast(1f)
                    val pageH = sizeState.value.height.toFloat().coerceAtLeast(1f)
                    val minW = with(density) { 24.dp.toPx() }
                    val minH = with(density) { 20.dp.toPx() }
                    // Page-frame finger move -> box axes (rotated boxes grow along their own sides).
                    val rr = (if (rotState.value.isNaN()) 0f else rotState.value) * (Math.PI.toFloat() / 180f)
                    val rc = kotlin.math.cos(rr)
                    val rs = kotlin.math.sin(rr)
                    accWH[0] += drag.x * rc + drag.y * rs
                    accWH[1] += -drag.x * rs + drag.y * rc
                    liveW[0] = (startBox[0] + accWH[0]).coerceIn(minW, maxOf(minW, pageW * 1.6f))
                    liveH[0] = (startBox[1] + accWH[1]).coerceIn(minH, maxOf(minH, pageH * 1.6f))
                    // Text scales with the box (geometric mean of both axes).
                    val fw = liveW[0] / startBox[0]
                    val fh = liveH[0] / startBox[1]
                    val scaledSp = (startBox[2] * kotlin.math.sqrt((fw * fh).coerceAtLeast(0.0001f)))
                        .coerceIn(6f, 96f)
                    // Continuous float values, local only (smooth, no rounding).
                    val wFr = (liveW[0] / pageW).coerceIn(0.02f, 1.8f)
                    val hFr = (liveH[0] / pageH).coerceIn(0.02f, 1.8f)
                    if (kotlin.math.abs(rr) < 0.0087f) {
                        liveBox.value = floatArrayOf(wFr, hFr, scaledSp)
                    } else {
                        // Rotated (pivot = center): keep the opposite corner fixed on the page.
                        // center moves by R * (dW / 2, dH / 2); top-left = center - (W / 2, H / 2).
                        val dW = wFr * pageW - startBox[0]
                        val dH = hFr * pageH - startBox[1]
                        val nx = startXY[0] + ((dW / 2f) * (rc - 1f) - (dH / 2f) * rs) / pageW
                        val ny = startXY[1] + ((dW / 2f) * rs + (dH / 2f) * (rc - 1f)) / pageH
                        liveBox.value = floatArrayOf(wFr, hFr, scaledSp, nx, ny)
                    }
                }
            }
            }

            if (selected && (rotHud || sizeHud || boxHud)) {
                val hudText = if (rotHud) {
                    var n = ((textElement.rotationDeg % 360f) + 360f) % 360f
                    if (n > 180f) n -= 360f
                    n.roundToInt().toString() + "\u00b0"
                } else if (boxHud) {
                    val pageW = sizePx.width.toFloat().coerceAtLeast(1f)
                    val pageH = sizePx.height.toFloat().coerceAtLeast(1f)
                    val wPct = ((if (textElement.boxWidthFrac > 0.01f) textElement.boxWidthFrac else boxWpx / pageW) * 100f).roundToInt()
                    val hPct = ((if (textElement.boxHeightFrac > 0.01f) textElement.boxHeightFrac else boxHpx / pageH) * 100f).roundToInt()
                    wPct.toString() + " x " + hPct.toString()
                } else {
                    ((((textElement.fontSizeSp - 6f) / 90f) * 100f).roundToInt().coerceIn(0, 100)).toString()
                }
                val hudCap = if (rotHud) stringResource(R.string.tt_hud_rotate) else if (boxHud) stringResource(R.string.tt_hud_box) else stringResource(R.string.tt_hud_size)
                val boxTop = (textElement.yFraction * sizePx.height).toInt()
                Box(
                    modifier = Modifier.offset {
                        val hudY = if (boxTop > 72) 10 else (sizePx.height - 72).coerceAtLeast(10)
                        IntOffset((sizePx.width / 2) - 48, hudY)
                    }.zIndex(2f)
                ) {
                    Column(
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .background(Color(0xF2181818), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = hudText,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = hudCap,
                            color = Color(0xFFBDBDBD),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            LaunchedEffect(pendingFocusTextId, textElement.id) {
                if (pendingFocusTextId == textElement.id) {
                    focusRequester.requestFocus()
                    onConsumePendingFocus()
                }
            }
        }
    }
}

@Composable
private fun AbsolutePdfImagesLayer(
    images: List<ImportedImage>,
    cellAspectRatio: Float,
    selectedImageIds: List<String>,
    singleMenuImageId: String?,
    reorderMode: Boolean,
    hasClipboardImages: Boolean,
    cellBounds: MutableMap<String, Rect>,
    onImageClick: (String) -> Unit,
    onChangePosition: (String) -> Unit,
    onCut: (String) -> Unit,
    onCopy: (String) -> Unit,
    onPaste: () -> Unit,
    onSaveSingle: (String) -> Unit,
    onShareSingle: (String) -> Unit,
    onCropImage: (String) -> Unit,
    onRotateImage: (String) -> Unit,
    onReplaceImage: (String) -> Unit,
    onImagePosition: (String) -> Unit,
    onAddImageLink: (String) -> Unit,
    onDeleteSingleKeepSpace: (String) -> Unit,
    onDeleteSingleFillSpace: (String) -> Unit,
    onMoveSingle: (String, String) -> Unit,
    onMoveMultiple: (String) -> Unit,
    onFinishReorder: () -> Unit,
    selectedCount: Int
) {
    if (images.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val density = LocalDensity.current
        images.forEach { image ->
            val ww = (image.absWFrac * w).toInt().coerceAtLeast(8)
            val hh = (image.absHFrac * h).toInt().coerceAtLeast(8)
            Box(
                modifier = Modifier
                    .offset { IntOffset((image.absXFrac * w).toInt(), (image.absYFrac * h).toInt()) }
                    .size(
                        width = with(density) { ww.toDp() },
                        height = with(density) { hh.toDp() }
                    )
            ) {
                ImageCell(
                    image = image,
                    modifier = Modifier.fillMaxSize(),
                    aspectRatio = (image.aspectRatioOverride ?: (ww.toFloat() / hh.toFloat())).coerceIn(0.05f, 20f),
                    fillExact = true,
                    cornerRadiusPercent = image.cornerRadiusPercent.coerceIn(0, 100),
                    selected = selectedImageIds.contains(image.id),
                    reorderMode = reorderMode,
                    showMenu = singleMenuImageId == image.id,
                    hasClipboardImages = hasClipboardImages,
                    cellBounds = cellBounds,
                    onClick = { onImageClick(image.id) },
                    onLongPress = {},
                    onChangePosition = { onChangePosition(image.id) },
                    onCut = { onCut(image.id) },
                    onCopy = { onCopy(image.id) },
                    onPaste = onPaste,
                    onSave = { onSaveSingle(image.id) },
                    onShare = { onShareSingle(image.id) },
                    onCrop = { onCropImage(image.id) },
                    onRotate = { onRotateImage(image.id) },
                    onReplace = { onReplaceImage(image.id) },
                    onImagePosition = { onImagePosition(image.id) },
                    onAddLink = { onAddImageLink(image.id) },
                    onDeleteKeepSpace = { onDeleteSingleKeepSpace(image.id) },
                    onDeleteFillSpace = { onDeleteSingleFillSpace(image.id) },
                    onDropOnTarget = { targetId ->
                        if (selectedCount > 1) onMoveMultiple(targetId) else onMoveSingle(image.id, targetId)
                        onFinishReorder()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageGrid(
    images: List<ImportedImage>,
    imagesPerRow: Int,
    spacingDp: Int,
    pageWidthDp: Float = 360f,
    pageHeightDp: Float = 640f,
    pageMarginDp: Int = 10,
    pageOrigin: Offset = Offset.Zero,
    pageSizePx: IntSize = IntSize.Zero,
    dragModeActive: Boolean = false,
    onDragImagesByFrac: (Float, Float) -> Unit = { _, _ -> },
    onSyncDragOffset: (String, Float, Float) -> Unit = { _, _, _ -> },
    onEnsureDragSelection: (String) -> Unit = {},
    cellAspectRatio: Float,
    cellCornerRadiusPercent: Int,
    selectedImageIds: List<String>,
    selectionMode: Boolean,
    singleMenuImageId: String?,
    reorderMode: Boolean,
    hasClipboardImages: Boolean,
    cellBounds: MutableMap<String, Rect>,
    onImageClick: (String) -> Unit,
    onImageLongPress: (String) -> Unit,
    onChangePosition: (String) -> Unit,
    onCut: (String) -> Unit,
    onCopy: (String) -> Unit,
    onPaste: () -> Unit,
    onSaveSingle: (String) -> Unit,
    onShareSingle: (String) -> Unit,
    onCropImage: (String) -> Unit,
    onRotateImage: (String) -> Unit,
    onReplaceImage: (String) -> Unit,
    onImagePosition: (String) -> Unit,
    onAddImageLink: (String) -> Unit,
    onDeleteSingle: (String) -> Unit,
    onDeleteSingleKeepSpace: (String) -> Unit,
    onDeleteSingleFillSpace: (String) -> Unit,
    onMoveSingle: (String, String) -> Unit,
    onMoveMultiple: (String) -> Unit,
    onFinishReorder: () -> Unit,
    fillPage: Boolean = true
) {
    val exemptImgs = images.filter { it.rowExempt }
    if (fillPage && exemptImgs.isNotEmpty() && exemptImgs.size < images.size) {
        val oldRow = exemptImgs.first().pinnedPerRow.let { if (it > 0) it else imagesPerRow }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (imagesPerRow == 1) 0.dp else pageMarginDp.dp),
            verticalArrangement = Arrangement.spacedBy(spacingDp.dp)
        ) {
            ImageGrid(
                images = exemptImgs,
                imagesPerRow = oldRow,
                spacingDp = spacingDp,
                pageWidthDp = pageWidthDp,
                pageHeightDp = pageHeightDp,
                pageMarginDp = 0,
                pageOrigin = pageOrigin,
                pageSizePx = pageSizePx,
                dragModeActive = dragModeActive,
                onDragImagesByFrac = onDragImagesByFrac,
                onSyncDragOffset = onSyncDragOffset,
                onEnsureDragSelection = onEnsureDragSelection,
                cellAspectRatio = cellAspectRatio,
                cellCornerRadiusPercent = cellCornerRadiusPercent,
                selectedImageIds = selectedImageIds,
                selectionMode = selectionMode,
                singleMenuImageId = singleMenuImageId,
                reorderMode = reorderMode,
                hasClipboardImages = hasClipboardImages,
                cellBounds = cellBounds,
                onImageClick = onImageClick,
                onImageLongPress = onImageLongPress,
                onChangePosition = onChangePosition,
                onCut = onCut,
                onCopy = onCopy,
                onPaste = onPaste,
                onSaveSingle = onSaveSingle,
                onShareSingle = onShareSingle,
                onCropImage = onCropImage,
                onRotateImage = onRotateImage,
                onReplaceImage = onReplaceImage,
                onImagePosition = onImagePosition,
                onAddImageLink = onAddImageLink,
                onDeleteSingle = onDeleteSingle,
                onDeleteSingleKeepSpace = onDeleteSingleKeepSpace,
                onDeleteSingleFillSpace = onDeleteSingleFillSpace,
                onMoveSingle = onMoveSingle,
                onMoveMultiple = onMoveMultiple,
                onFinishReorder = onFinishReorder,
                fillPage = false
            )
            ImageGrid(
                images = images.filter { !it.rowExempt },
                imagesPerRow = imagesPerRow,
                spacingDp = spacingDp,
                pageWidthDp = pageWidthDp,
                pageHeightDp = pageHeightDp,
                pageMarginDp = 0,
                pageOrigin = pageOrigin,
                pageSizePx = pageSizePx,
                dragModeActive = dragModeActive,
                onDragImagesByFrac = onDragImagesByFrac,
                onSyncDragOffset = onSyncDragOffset,
                onEnsureDragSelection = onEnsureDragSelection,
                cellAspectRatio = cellAspectRatio,
                cellCornerRadiusPercent = cellCornerRadiusPercent,
                selectedImageIds = selectedImageIds,
                selectionMode = selectionMode,
                singleMenuImageId = singleMenuImageId,
                reorderMode = reorderMode,
                hasClipboardImages = hasClipboardImages,
                cellBounds = cellBounds,
                onImageClick = onImageClick,
                onImageLongPress = onImageLongPress,
                onChangePosition = onChangePosition,
                onCut = onCut,
                onCopy = onCopy,
                onPaste = onPaste,
                onSaveSingle = onSaveSingle,
                onShareSingle = onShareSingle,
                onCropImage = onCropImage,
                onRotateImage = onRotateImage,
                onReplaceImage = onReplaceImage,
                onImagePosition = onImagePosition,
                onAddImageLink = onAddImageLink,
                onDeleteSingle = onDeleteSingle,
                onDeleteSingleKeepSpace = onDeleteSingleKeepSpace,
                onDeleteSingleFillSpace = onDeleteSingleFillSpace,
                onMoveSingle = onMoveSingle,
                onMoveMultiple = onMoveMultiple,
                onFinishReorder = onFinishReorder,
                fillPage = false
            )
        }
        return
    }
    val layoutPerRow = run {
        val pinnedRow = images.firstOrNull { it.rowExempt && it.pinnedPerRow > 0 }?.pinnedPerRow ?: 0
        if (images.isNotEmpty() && images.all { it.rowExempt } && pinnedRow > 0) pinnedRow else imagesPerRow
    }
    Column(
        modifier = Modifier
            .then(if (fillPage) Modifier.fillMaxSize() else Modifier)
            .clipToBounds()
            .padding(if (layoutPerRow == 1) 0.dp else pageMarginDp.dp),
        verticalArrangement = Arrangement.spacedBy(spacingDp.dp)
    ) {
        images.chunked(layoutPerRow).forEach { rowImages ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacingDp.dp)
            ) {
                rowImages.forEach { image ->
                    ImageCell(
                        image = image,
                        modifier = Modifier.weight(1f),
                        pageWidthDp = pageWidthDp,
                        pageHeightDp = pageHeightDp,
                        pageOrigin = pageOrigin,
                        pageSizePx = pageSizePx,
                        dragModeActive = dragModeActive,
                        onDragImagesByFrac = onDragImagesByFrac,
                        onSyncDragOffset = onSyncDragOffset,
                        onEnsureDragSelection = onEnsureDragSelection,
                        aspectRatio = if (layoutPerRow == 1) {
                            val b = image.bitmap
                            if (b != null && b.height > 0)
                                (b.width.toFloat() / b.height).coerceIn(0.2f, 5f)
                            else (image.aspectRatioOverride ?: cellAspectRatio).coerceIn(0.2f, 5f)
                        } else {
                            (image.aspectRatioOverride ?: cellAspectRatio).coerceIn(0.3f, 2.0f)
                        },
                        cornerRadiusPercent = image.cornerRadiusPercent.coerceIn(0, 100),
                        selected = selectedImageIds.contains(image.id) && !image.id.startsWith("spacer_"),
                        reorderMode = reorderMode,
                        showMenu = singleMenuImageId == image.id && !image.id.startsWith("spacer_"),
                        hasClipboardImages = hasClipboardImages,
                        cellBounds = cellBounds,
                        onClick = {
                            if (!image.id.startsWith("spacer_")) onImageClick(image.id)
                        },
                        onLongPress = {
                            // long-press multi-select removed
                        },
                        onChangePosition = {
                            onChangePosition(image.id)
                        },
                        onCut = {
                            onCut(image.id)
                        },
                        onCopy = {
                            onCopy(image.id)
                        },
                        onPaste = onPaste,
                        onSave = {
                            onSaveSingle(image.id)
                        },
                        onShare = {
                            onShareSingle(image.id)
                        },
                        onCrop = {
                            onCropImage(image.id)
                        },
                        onRotate = {
                            onRotateImage(image.id)
                        },
                        onReplace = {
                            onReplaceImage(image.id)
                        },
                        onImagePosition = {
                            onImagePosition(image.id)
                        },
                        onAddLink = {
                            onAddImageLink(image.id)
                        },
                        onDeleteKeepSpace = {
                            onDeleteSingleKeepSpace(image.id)
                        },
                        onDeleteFillSpace = {
                            onDeleteSingleFillSpace(image.id)
                        },
                        onDropOnTarget = { targetId ->
                            if (selectedImageIds.size > 1) {
                                onMoveMultiple(targetId)
                            } else {
                                onMoveSingle(image.id, targetId)
                            }
                            onFinishReorder()
                        }
                    )
                }

                repeat(layoutPerRow - rowImages.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageCell(
    image: ImportedImage,
    modifier: Modifier = Modifier,
    aspectRatio: Float,
    pageWidthDp: Float = 360f,
    pageHeightDp: Float = 640f,
    pageOrigin: Offset = Offset.Zero,
    pageSizePx: IntSize = IntSize.Zero,
    dragModeActive: Boolean = false,
    onDragImagesByFrac: (Float, Float) -> Unit = { _, _ -> },
    onSyncDragOffset: (String, Float, Float) -> Unit = { _, _, _ -> },
    onEnsureDragSelection: (String) -> Unit = {},
    fillExact: Boolean = false,
    cornerRadiusPercent: Int,
    selected: Boolean,
    reorderMode: Boolean,
    showMenu: Boolean,
    hasClipboardImages: Boolean,
    cellBounds: MutableMap<String, Rect>,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onChangePosition: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onCrop: () -> Unit,
    onRotate: () -> Unit,
    onReplace: () -> Unit,
    onImagePosition: () -> Unit,
    onAddLink: () -> Unit,
    onDeleteKeepSpace: () -> Unit,
    onDeleteFillSpace: () -> Unit,
    onDropOnTarget: (String) -> Unit
) {
    var layoutCoordinates by remember {
        mutableStateOf<LayoutCoordinates?>(null)
    }
    val coordsSync = layoutCoordinates
    val pwSync = pageSizePx.width.toFloat()
    val phSync = pageSizePx.height.toFloat()
    LaunchedEffect(
        dragModeActive, image.id, image.dragOffsetXFrac, image.dragOffsetYFrac,
        coordsSync, pageOrigin, pageSizePx, image.bitmap
    ) {
        if (!dragModeActive || image.ownerPageIndex != null) return@LaunchedEffect
        if (coordsSync == null || pwSync < 1f || phSync < 1f) return@LaunchedEffect
        val zoomFix = editorZoomScale.value.coerceIn(1f, 4f)
        val origin = (coordsSync.positionInRoot() - pageOrigin) / zoomFix
        val cw = coordsSync.size.width.toFloat().coerceAtLeast(1f)
        val ch = coordsSync.size.height.toFloat().coerceAtLeast(1f)
        val bmp = image.bitmap
        val fill = fillExact || image.ownerPageIndex != null
        val visW: Float
        val visH: Float
        if (bmp != null && !fill) {
            val bmpAspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
            val cellAspect = cw / ch
            if (bmpAspect > cellAspect) {
                visW = cw; visH = cw / bmpAspect
            } else {
                visH = ch; visW = ch * bmpAspect
            }
        } else { visW = cw; visH = ch }
        val padX = (cw - visW) / 2f
        val padY = (ch - visH) / 2f
        val wantX = image.dragOffsetXFrac * pwSync
        val wantY = image.dragOffsetYFrac * phSync
        val minX = -origin.x - padX
        val minY = -origin.y - padY
        val maxX = pwSync - visW - origin.x - padX
        val maxY = phSync - visH - origin.y - padY
        val cx = wantX.coerceIn(minX, maxOf(minX, maxX))
        val cy = wantY.coerceIn(minY, maxOf(minY, maxY))
        onSyncDragOffset(image.id, cx / pwSync, cy / phSync)
    }

    Box(
        modifier = modifier
            .then(if (fillExact) Modifier.fillMaxSize() else Modifier.aspectRatio(aspectRatio))
            .onGloballyPositioned { coordinates ->
                layoutCoordinates = coordinates
                val position = coordinates.positionInRoot()

                cellBounds[image.id] = Rect(
                    left = position.x,
                    top = position.y,
                    right = position.x + coordinates.size.width,
                    bottom = position.y + coordinates.size.height
                )
            }
    ) {
        // Clip only the image content — NOT the dropdown menu
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    if (image.ownerPageIndex != null) IntOffset.Zero
                    else {
                        val coords = layoutCoordinates
                        val pw = pageSizePx.width.toFloat().coerceAtLeast(1f)
                        val ph = pageSizePx.height.toFloat().coerceAtLeast(1f)
                        val wantX = image.dragOffsetXFrac * pw
                        val wantY = image.dragOffsetYFrac * ph
                        if (coords == null || pageSizePx.width <= 0) {
                            IntOffset(wantX.roundToInt(), wantY.roundToInt())
                        } else {
                            val zoomFix = editorZoomScale.value.coerceIn(1f, 4f)
                            val origin = (coords.positionInRoot() - pageOrigin) / zoomFix
                            val cw = coords.size.width.toFloat().coerceAtLeast(1f)
                            val ch = coords.size.height.toFloat().coerceAtLeast(1f)
                            val bmp = image.bitmap
                            val fill = fillExact || image.ownerPageIndex != null
                            val visW: Float
                            val visH: Float
                            if (bmp != null && !fill && ch > 0f) {
                                val bmpAspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
                                val cellAspect = cw / ch
                                if (bmpAspect > cellAspect) {
                                    visW = cw
                                    visH = cw / bmpAspect
                                } else {
                                    visH = ch
                                    visW = ch * bmpAspect
                                }
                            } else {
                                visW = cw
                                visH = ch
                            }
                            val padX = (cw - visW) / 2f
                            val padY = (ch - visH) / 2f
                            val minX = -origin.x - padX
                            val minY = -origin.y - padY
                            val maxX = pw - visW - origin.x - padX
                            val maxY = ph - visH - origin.y - padY
                            IntOffset(
                                wantX.coerceIn(minX, maxOf(minX, maxX)).roundToInt(),
                                wantY.coerceIn(minY, maxOf(minY, maxY)).roundToInt()
                            )
                        }
                    }
                }
                .then(
                    if (reorderMode) {
                        Modifier.pointerInput(image.id) {
                            detectDragGestures(
                                onDragStart = {},
                                onDrag = { change, _ ->
                                    change.consume()
                                },
                                onDragEnd = {
                                    val coordinates = layoutCoordinates
                                        ?: return@detectDragGestures

                                    val center =
                                        coordinates.localToRoot(
                                            Offset(
                                                coordinates.size.width / 2f,
                                                coordinates.size.height / 2f
                                            )
                                        )

                                    val target = cellBounds.entries
                                        .firstOrNull { entry ->
                                            entry.key != image.id &&
                                                entry.value.contains(center)
                                        }

                                    if (target != null) {
                                        onDropOnTarget(target.key)
                                    }
                                },
                                onDragCancel = {}
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { /* long-press removed */ }
                ),
            contentAlignment = Alignment.Center
        ) {
            val cellW = maxWidth
            val cellH = maxHeight
            val bmp = image.bitmap
            val fill = fillExact || image.ownerPageIndex != null
            val drawW: androidx.compose.ui.unit.Dp
            val drawH: androidx.compose.ui.unit.Dp
            if (bmp != null && !fill) {
                val bmpAspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
                val cellAspect = cellW / cellH
                if (bmpAspect > cellAspect) {
                    drawW = cellW
                    drawH = cellW / bmpAspect
                } else {
                    drawH = cellH
                    drawW = cellH * bmpAspect
                }
            } else {
                drawW = cellW
                drawH = cellH
            }
            val cornerR = minOf(drawW, drawH) * (cornerRadiusPercent.coerceIn(0, 100) / 100f) * 0.5f
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(drawW, drawH)
                        .elementFrame(image.id, cornerR) // fix55
                        .clip(RoundedCornerShape(cornerR)),
                    contentScale = ContentScale.FillBounds
                )

            // Image numbering badge — position inside ContentScale.Fit bounds
            val label = image.numberLabel
            val badgeBmp = image.bitmap
            if (label != null && badgeBmp != null) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val cellW = maxWidth
                    val cellH = maxHeight
                    val bmpAspect = badgeBmp.width.toFloat() / badgeBmp.height.toFloat().coerceAtLeast(1f)
                    val cellAspect = (cellW / cellH)
                    val drawW: androidx.compose.ui.unit.Dp
                    val drawH: androidx.compose.ui.unit.Dp
                    if (bmpAspect > cellAspect) {
                        drawW = cellW
                        drawH = cellW / bmpAspect
                    } else {
                        drawH = cellH
                        drawW = cellH * bmpAspect
                    }
                    val originX = (cellW - drawW) / 2
                    val originY = (cellH - drawH) / 2
                    val sizeFrac = image.numberSizeFrac.coerceIn(0.08f, 0.4f)
                    val iconD = minOf(drawW, drawH) * sizeFrac
                    val half = iconD / 2
                    val xf = image.numberXFrac.coerceIn(0f, 1f)
                    val yf = image.numberYFrac.coerceIn(0f, 1f)
                    val cx = originX + drawW * xf
                    val cy = originY + drawH * yf
                    val clampedCx = cx.coerceIn(originX + half, originX + drawW - half)
                    val clampedCy = cy.coerceIn(originY + half, originY + drawH - half)
                    val bg = Color(image.numberBgArgb.toInt())
                    val fg = Color(image.numberFgArgb.toInt())
                    val a = image.numberAlpha.coerceIn(0.2f, 1f)
                    val w = image.numberWeight.coerceIn(0f, 1f)
                    val fw = when {
                        w < 0.34f -> FontWeight.Light
                        w < 0.67f -> FontWeight.Normal
                        else -> FontWeight.Bold
                    }
                    val fs = (iconD.value * 0.42f).sp
                    Box(
                        modifier = Modifier
                            .offset(x = clampedCx - half, y = clampedCy - half)
                            .size(iconD)
                            .background(bg.copy(alpha = bg.alpha * a), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label.toString(),
                            color = fg.copy(alpha = fg.alpha * a),
                            fontWeight = fw,
                            fontSize = fs,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            style = LocalTextStyle.current.copy(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeight = fs
                            ),
                            modifier = Modifier.wrapContentSize(Alignment.Center)
                        )
                    }
                }
            }
            } else {
                // Real imports (have uri) show loading while bitmap decodes.
                // Spacer slots used for "start on page N" stay empty — no endless spinner.
                if (image.imageUri != null) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                )
            }
        }

        // Compact dropdown anchored to this image cell (same style as Image/Text Tools menus).
        // Outside tap or any image click dismisses via onDismissRequest / openImageMenu logic.
        var showInfoDialog by remember(image.id) { mutableStateOf(false) }
        var showDeleteModeDialog by remember(image.id) { mutableStateOf(false) }
        if (showMenu && !reorderMode) {
            var savedToGallery by remember(image.id) { mutableStateOf(false) }
            androidx.activity.compose.BackHandler { onClick() } // fix54
            DropdownMenu(
                expanded = true,
                onDismissRequest = onClick
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (savedToGallery) {
                                "✓ " + stringResource(R.string.save_in_gallery)
                            } else {
                                stringResource(R.string.save_in_gallery)
                            },
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onSave()
                        savedToGallery = true
                        onClick() // close after use
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.share_image),
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    onClick = {
                        onShare()
                        onClick() // close after use
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.view_image_information),
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    onClick = {
                        showInfoDialog = true
                        onClick()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.image_crop),
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    onClick = {
                        onClick()
                        onCrop()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.image_rotate),
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    onClick = {
                        onClick()
                        onRotate()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.image_menu_replace),
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    onClick = {
                        onClick()
                        onReplace()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.image_menu_position),
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    onClick = {
                        onClick()
                        onImagePosition()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.image_menu_add_link),
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    onClick = {
                        onClick()
                        onAddLink()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.image_menu_delete),
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    onClick = {
                        showDeleteModeDialog = true
                    }
                )
            }

        }

        if (showInfoDialog) {
            val details = remember(image.id, image.approxSizeBytes, image.imageUri, image.bitmap) {
                imageDetailsSnapshot(image)
            }
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White,
                title = {
                    Text(
                        text = stringResource(R.string.image_info_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ImageDetailRow("\uD83D\uDCC4", stringResource(R.string.image_info_label_name), details.fileName)
                        ImageDetailRow("\uD83D\uDCBE", stringResource(R.string.image_info_label_size), details.fileSize)
                        ImageDetailRow("\uD83D\uDCD0", stringResource(R.string.image_info_label_dimensions), details.dimensions)
                        ImageDetailRow("\u25A3", stringResource(R.string.image_info_label_format), details.format)
                        ImageDetailRow("\u25AD", stringResource(R.string.image_info_label_aspect), details.aspectRatio)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text(
                            text = stringResource(R.string.image_info_ok),
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {}
            )
        }

        if (showDeleteModeDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteModeDialog = false },
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
                                showDeleteModeDialog = false
                                onClick()
                                onDeleteKeepSpace()
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
                                showDeleteModeDialog = false
                                onClick()
                                onDeleteFillSpace()
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
                            onClick = {
                                showDeleteModeDialog = false
                                onClick()
                            },
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
    }
}

/** Cached metadata only — never decode/compress on the UI thread. */
private data class ImageDetailsSnapshot(
    val fileName: String,
    val fileSize: String,
    val dimensions: String,
    val format: String,
    val aspectRatio: String
)

private fun imageDetailsSnapshot(image: ImportedImage): ImageDetailsSnapshot {
    val uriName = image.imageUri?.lastPathSegment
        ?.substringAfterLast('/')
        ?.substringBefore('?')
        ?.takeIf { it.isNotBlank() && it != "0" && '.' in it }
    val bmp = image.bitmap?.takeIf { !it.isRecycled }
    val extFromUri = uriName?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.length in 2..5 }
    val format = when (extFromUri) {
        "jpg", "jpeg" -> "JPEG"
        "png" -> "PNG"
        "webp" -> "WEBP"
        "gif" -> "GIF"
        "bmp" -> "BMP"
        "heic", "heif" -> "HEIC"
        else -> if (image.approxSizeBytes != null) "JPEG" else (extFromUri?.uppercase() ?: "JPEG")
    }
    val fileName = uriName
        ?: ("image_" + image.id.takeLast(6).replace(Regex("[^A-Za-z0-9]"), "") + "." + format.lowercase().let {
            if (it == "jpeg") "jpg" else it
        })
    val bytes = image.approxSizeBytes?.takeIf { it > 0 }
        ?: bmp?.byteCount?.takeIf { it > 0 }
    val fileSize = when {
        bytes == null -> "—"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
    val dimensions = if (bmp != null) "${bmp.width} × ${bmp.height} px" else "—"
    val aspect = if (bmp != null && bmp.width > 0 && bmp.height > 0) {
        simplifiedRatio(bmp.width, bmp.height)
    } else "—"
    return ImageDetailsSnapshot(fileName, fileSize, dimensions, format, aspect)
}

private fun simplifiedRatio(w: Int, h: Int): String {
    fun gcd(a: Int, b: Int): Int {
        var x = kotlin.math.abs(a)
        var y = kotlin.math.abs(b)
        while (y != 0) {
            val t = x % y
            x = y
            y = t
        }
        return x.coerceAtLeast(1)
    }
    val g = gcd(w, h)
    return (w / g).toString() + ":" + (h / g).toString()
}

@Composable
private fun ImageDetailRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 10.dp, top = 1.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color(0xFF757575),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


@Composable
private fun SingleImageActionsMenu(
    expanded: Boolean,
    savedToGallery: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    androidx.activity.compose.BackHandler(enabled = expanded) { onDismiss() } // fix54
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = if (savedToGallery) {
                        "✓ " + stringResource(R.string.save_in_gallery)
                    } else {
                        stringResource(R.string.save_in_gallery)
                    },
                    color = Color.Black
                )
            },
            onClick = onSave
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.share_image),
                    color = Color.Black
                )
            },
            onClick = onShare
        )
    }
}


@Composable
private fun MultipleActionsDialog(
    onChangePosition: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.selected_images_actions),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onChangePosition,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.change_position))
                }

                TextButton(
                    onClick = onCut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.cut_image))
                }

                TextButton(
                    onClick = onCopy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.copy_image))
                }

                TextButton(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save_in_gallery))
                }

                TextButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.share_image))
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.delete_all_images))
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun EmptyStatePage(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PageCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            }
        }
    }
}

@Composable
private fun PageCard(
    aspectRatio: Float = PAGE_ASPECT_RATIO,
    backgroundColor: Long = 0xFFFFFFFFL,
    backgroundBitmap: android.graphics.Bitmap? = null,
    backgroundBrush: androidx.compose.ui.graphics.Brush? = null,
    pageNumberText: String? = null,
    pageNumberPosition: com.pdfcraft.studio.ui.editor.EditorViewModel.PageNumberPosition =
        com.pdfcraft.studio.ui.editor.EditorViewModel.PageNumberPosition.NONE,
    content: @Composable () -> Unit
) {
    com.pdfcraft.studio.ui.editor.DropdownBorderAnim.ensureLoaded(androidx.compose.ui.platform.LocalContext.current) // fix45
    val frame = currentMenuDesign() // fix36: on-screen page frame only (PDF output unchanged)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(0.dp)) // fix43: no page shadow
            .border(1.dp, frame.pageBorder)
            // fix45: page border = dropdown animation 27 (or the selected one when the switch is on)
            .editorBorderLine(com.pdfcraft.studio.ui.editor.DropdownBorderAnim.pageChoice(), 0.dp, page = true) // fix48
            .then(
                if (backgroundBrush != null) Modifier.background(backgroundBrush)
                else Modifier.background(Color(backgroundColor))
            ),
        contentAlignment = Alignment.Center
    ) {
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = if (backgroundBrush == null) ColorFilter.lighting(
                    multiply = Color(backgroundColor.toInt()),
                    add = Color.Black
                ) else null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (backgroundBrush != null) Modifier.drawWithContent {
                            drawContent()
                            drawRect(brush = backgroundBrush, blendMode = BlendMode.Multiply)
                        } else Modifier
                    )
            )
        }
        content()
        if (pageNumberText != null &&
            pageNumberPosition != com.pdfcraft.studio.ui.editor.EditorViewModel.PageNumberPosition.NONE
        ) {
            val align = when (pageNumberPosition) {
                com.pdfcraft.studio.ui.editor.EditorViewModel.PageNumberPosition.LEFT -> Alignment.BottomStart
                com.pdfcraft.studio.ui.editor.EditorViewModel.PageNumberPosition.CENTER -> Alignment.BottomCenter
                com.pdfcraft.studio.ui.editor.EditorViewModel.PageNumberPosition.RIGHT -> Alignment.BottomEnd
                else -> Alignment.BottomCenter
            }
            Text(
                text = pageNumberText,
                color = Color.Black.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(align)
                    .padding(10.dp)
            )
        }
    }
}

@Composable
private fun TextHandle(
    symbol: String,
    modifier: Modifier = Modifier,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    /** Set: drag deltas are measured in this fixed page frame (handle may move). */
    pageRef: Array<LayoutCoordinates?>? = null,
    /** Set (with pageRef): finger point at down and now, both in the fixed page frame. */
    onPagePos: ((Offset, Offset) -> Unit)? = null,
    onDrag: (Offset) -> Unit
) {
    val startCb = rememberUpdatedState(onDragStart)
    val pageCb = rememberUpdatedState(onPagePos)
    val endCb = rememberUpdatedState(onDragEnd)
    val dragCb = rememberUpdatedState(onDrag)
    val selfRef = remember { arrayOfNulls<LayoutCoordinates>(1) }
    Box(
        modifier = modifier
            .size(28.dp)
            .shadow(3.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(1.dp, Color(0x33000000), CircleShape)
            .onGloballyPositioned { selfRef[0] = it }
            .pointerInput(Unit) {
                fun pagePos(p: Offset): Offset? {
                    val pr = pageRef?.get(0) ?: return null
                    val sr = selfRef[0] ?: return null
                    if (!pr.isAttached || !sr.isAttached) return null
                    return try { pr.localPositionOf(sr, p) } catch (e: Exception) { null }
                }
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    down.consume()
                    TextBoxTouch.downUptime = down.uptimeMillis
                    startCb.value()
                    val slop = viewConfiguration.touchSlop
                    var moved = false
                    var endSent = false
                    var prevPage: Offset? = pagePos(down.position)
                    val startPage = prevPage
                    try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            endSent = true
                            endCb.value()
                            break
                        }
                        val delta = change.positionChange()
                        if (!moved && (change.position - down.position).getDistance() > slop) {
                            moved = true
                        }
                        // The handle owns this finger until it lifts: nothing else sees it.
                        change.consume()
                        if (moved) {
                            val pp = prevPage
                            val pNow = if (pp != null) pagePos(change.position) else null
                            if (pp != null && pNow != null) {
                                // Page frame: real finger travel even while the handle moves
                                // (first step includes the slop distance).
                                val d = pNow - pp
                                prevPage = pNow
                                val absCb = pageCb.value
                                if (absCb != null && startPage != null) {
                                    if (d.x != 0f || d.y != 0f) absCb(startPage, pNow)
                                } else if (d.x != 0f || d.y != 0f) {
                                    dragCb.value(d)
                                }
                            } else if (delta.x != 0f || delta.y != 0f) {
                                dragCb.value(delta)
                            }
                        }
                    }
                    } finally {
                        // Gesture cancelled: still finish the drag (commit live resize).
                        if (!endSent) endCb.value()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = Color(0xFF222222),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun snapRotationDeg(deg: Float): Float {
    var s = ((deg % 360f) + 360f) % 360f
    if (s > 180f) s -= 360f
    var nearest = kotlin.math.round(s / 10f) * 10f
    if (nearest > 180f) nearest = 180f
    if (nearest < -180f) nearest = -180f
    val window = 2.4f
    val dist = kotlin.math.min(
        kotlin.math.abs(s - nearest),
        kotlin.math.min(
            kotlin.math.abs(s - nearest + 360f),
            kotlin.math.abs(s - nearest - 360f)
        )
    )
    if (dist <= window) {
        return ((nearest % 360f) + 360f) % 360f
    }
    return ((deg % 360f) + 360f) % 360f
}

private fun rotatedAabbFracLimits(
    bw: Float,
    bh: Float,
    pageW: Float,
    pageH: Float,
    rotDeg: Float
): FloatArray {
    val pw = pageW.coerceAtLeast(1f)
    val ph = pageH.coerceAtLeast(1f)
    val rad = Math.toRadians(rotDeg.toDouble())
    val c = kotlin.math.abs(kotlin.math.cos(rad)).toFloat()
    val s = kotlin.math.abs(kotlin.math.sin(rad)).toFloat()
    val aabbW = c * bw + s * bh
    val aabbH = s * bw + c * bh
    val minX = ((aabbW - bw) / 2f / pw)
    val maxX = ((pw - (bw + aabbW) / 2f) / pw)
    val minY = ((aabbH - bh) / 2f / ph)
    val maxY = ((ph - (bh + aabbH) / 2f) / ph)
    val x0 = minX.coerceAtMost(maxX)
    val x1 = maxX.coerceAtLeast(x0)
    val y0 = minY.coerceAtMost(maxY)
    val y1 = maxY.coerceAtLeast(y0)
    return floatArrayOf(x0, x1, y0, y1)
}

private fun clampedRotatedTopLeft(
    xFrac: Float,
    yFrac: Float,
    bw: Float,
    bh: Float,
    pageW: Float,
    pageH: Float,
    rotDeg: Float
): Offset {
    val lim = rotatedAabbFracLimits(bw, bh, pageW, pageH, rotDeg)
    val x = xFrac.coerceIn(lim[0], lim[1])
    val y = yFrac.coerceIn(lim[2], lim[3])
    return Offset(x * pageW, y * pageH)
}

private fun clampTextInsidePage(
    xFrac: Float,
    yFrac: Float,
    boxW: Float,
    boxH: Float,
    pageW: Float,
    pageH: Float,
    rotDeg: Float
): Pair<Float, Float> {
    if (pageW < 1f || pageH < 1f || boxW < 1f || boxH < 1f) return xFrac to yFrac
    val rad = Math.toRadians(rotDeg.toDouble())
    val c = kotlin.math.abs(kotlin.math.cos(rad)).toFloat()
    val s = kotlin.math.abs(kotlin.math.sin(rad)).toFloat()
    val extX = (c * boxW + s * boxH) / 2f
    val extY = (s * boxW + c * boxH) / 2f
    val minCx = extX.coerceAtMost(pageW / 2f)
    val maxCx = (pageW - extX).coerceAtLeast(minCx)
    val minCy = extY.coerceAtMost(pageH / 2f)
    val maxCy = (pageH - extY).coerceAtLeast(minCy)
    val cx = (xFrac * pageW + boxW / 2f).coerceIn(minCx, maxCx)
    val cy = (yFrac * pageH + boxH / 2f).coerceIn(minCy, maxCy)
    return ((cx - boxW / 2f) / pageW) to ((cy - boxH / 2f) / pageH)
}


@Composable
private fun rememberPageEnergyPhase(): androidx.compose.runtime.MutableFloatState {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var start = 0L
        withFrameNanos { frame -> start = frame }
        while (true) {
            withFrameNanos { frame ->
                phase.floatValue = ((frame - start) / 4000000000f) % 1f
            }
        }
    }
    return phase
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPageEnergyBorder(
    phase: Float,
    line: Color = Color(0xFF4A4A4A),
    glow: Color = Color(0xFF00D9FF),
    glow2: Color = Color(0xFF4285FF)
) { // fix36: colors come from the active editor design
    val inset = 1.1f.dp.toPx()
    val outline = Path().apply {
        addRect(Rect(inset, inset, size.width - inset, size.height - inset))
    }
    drawPath(outline, line, style = Stroke(width = 1.2f.dp.toPx()))
    val measure = PathMeasure().apply { setPath(outline, false) }
    val length = measure.length
    if (length <= 1f) return
    val head = ((phase % 1f) + 1f) % 1f * length
    fun piece(span: Float): Path {
        val out = Path()
        val start = head - span
        if (start >= 0f) {
            measure.getSegment(start, head, out, true)
        } else {
            measure.getSegment(length + start, length, out, true)
            measure.getSegment(0f, head, out, true)
        }
        return out
    }
    val round = StrokeCap.Round
    drawPath(piece(length * 0.18f), glow.copy(alpha = 0.2f), style = Stroke(width = 4.2f.dp.toPx(), cap = round))
    drawPath(piece(length * 0.12f), glow2.copy(alpha = 0.67f), style = Stroke(width = 1.8f.dp.toPx(), cap = round))
    drawPath(piece(length * 0.045f), glow, style = Stroke(width = 2.dp.toPx(), cap = round))
}
