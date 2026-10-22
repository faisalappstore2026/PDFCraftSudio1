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
import com.pdfcraft.studio.ui.editor.packPageSource
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
import com.pdfcraft.studio.ui.editor.FontRange
import com.pdfcraft.studio.ui.editor.TextElement
import com.pdfcraft.studio.ui.common.colorpicker.GradientDir
import com.pdfcraft.studio.ui.common.colorpicker.gradientBrush
import com.pdfcraft.studio.ui.editor.LinkRange
import com.pdfcraft.studio.ui.editor.ShadowRange
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged

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
    onTextBoxResize: (String, Float, Float, Float) -> Unit = { _, _, _, _ -> },
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
    modifier: Modifier = Modifier
) {
    val pageListState = rememberLazyListState(0, 0)
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
            .collect { pair ->
                if (scrollRestoreDone) onScrollChanged(pair.first, pair.second)
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
    val pinchMod = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            var zooming = false
            var lastCx = Float.NaN
            var lastCy = Float.NaN
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.count { it.pressed }
                if (pressed >= 2) {
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
        BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color(0xFFE8E8E8)).clipToBounds().then(pinchMod)) {
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        PageCard(
                            aspectRatio = pageAspectRatioForPage?.invoke(0) ?: pageAspectRatio,
                            backgroundColor = run {
                                val kind = pageBackgroundKindForPage?.invoke(0) ?: "SOLID"
                                if (kind == "NONE" || kind == "IMAGE") 0xFFFFFFFFL else (pageBackgroundColorForPage?.invoke(0) ?: pageBackgroundColor)
                            },
                            backgroundBitmap = pageBackgroundBitmapForPage?.invoke(0) ?: pageBackgroundBitmap,
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
                                    onTextBoxResize = onTextBoxResize
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
        modifier = modifier.fillMaxSize().background(Color(0xFFE8E8E8)).clipToBounds().then(pinchMod)
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    PageCard(
                        aspectRatio = pageAspectRatioForPage?.invoke(docPage) ?: pageAspectRatio,
                        backgroundColor = run {
                            val kind = pageBackgroundKindForPage?.invoke(docPage) ?: "SOLID"
                            if (kind == "NONE" || kind == "IMAGE") 0xFFFFFFFFL else (pageBackgroundColorForPage?.invoke(docPage) ?: pageBackgroundColor)
                        },
                        backgroundBitmap = pageBackgroundBitmapForPage?.invoke(docPage) ?: pageBackgroundBitmap,
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
                                onTextBoxResize = onTextBoxResize
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

/** Text background as rounded rects per line run, with box opacity. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTextBackgrounds(
    layout: androidx.compose.ui.text.TextLayoutResult,
    bg: Array<Long?>,
    radiusPx: Float,
    alpha: Float
) {
    val len = minOf(bg.size, layout.layoutInput.text.length)
    for (line in 0 until layout.lineCount) {
        val ls = layout.getLineStart(line)
        val le = minOf(layout.getLineEnd(line, visibleEnd = true), len)
        val top = layout.getLineTop(line)
        val bottom = layout.getLineBottom(line)
        var i = ls
        while (i < le) {
            val c = bg[i]
            if (c == null) {
                i++
                continue
            }
            var j = i + 1
            while (j < le && bg[j] == c) j++
            val a = layout.getBoundingBox(i)
            val z = layout.getBoundingBox(j - 1)
            val left = minOf(a.left, z.left)
            val right = maxOf(a.right, z.right)
            val h = bottom - top
            val r = radiusPx.coerceIn(0f, h / 2f)
            val col = Color(c)
            drawRoundRect(
                color = col.copy(alpha = (col.alpha * alpha).coerceIn(0f, 1f)),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
            )
            i = j
        }
    }
}

/** Same text and spans, glyph fill hidden, outline (stroke) on bordered characters. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun buildBorderText(base: AnnotatedString, border: Array<Long?>, strokePx: Float): AnnotatedString {
    val b = AnnotatedString.Builder(base)
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
            SpanStyle(
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
    private val shadowRanges: List<ShadowRange> = emptyList()
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
            if (start < end) {
                builder.addStyle(SpanStyle(color = Color(cr.colorArgb)), start, end)
            }
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
    onTextRotate: (String, Float) -> Unit = { _, _ -> },
    onTextScale: (String, Float) -> Unit = { _, _ -> },
    onTextBoxResize: (String, Float, Float, Float) -> Unit = { _, _, _, _ -> }
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
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    focusManager.clearFocus(force = true)
                                    onDeselect()
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
        texts.forEach { textElement ->
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
            val contentPx = if (longest.isEmpty()) 0f else measurePaint.measureText(longest)
            val padPx = with(density) { 10.dp.toPx() }
            val capW = (sizePx.width.toFloat() * 3f).coerceAtLeast(minBoxPx)
            val capH = (sizePx.height.toFloat() * 3f).coerceAtLeast(fontPx)
            val autoW = (if (fieldValue.text.isEmpty()) minBoxPx else contentPx + padPx)
                .coerceAtMost(capW)
                .coerceAtLeast(with(density) { 8.dp.toPx() })
            val pageWpx = sizePx.width.toFloat().coerceAtLeast(1f)
            val pageHpx = sizePx.height.toFloat().coerceAtLeast(1f)
            val boxWpx = if (textElement.boxWidthFrac > 0.01f) {
                (textElement.boxWidthFrac * pageWpx).coerceIn(with(density) { 24.dp.toPx() }, capW)
            } else {
                autoW
            }
            val lineCount = 1 + fieldValue.text.count { it == '\n' }
            val autoH = (lineCount * fontPx * 1.35f + padPx).coerceAtMost(capH)
            val boxHpx = if (textElement.boxHeightFrac > 0.01f) {
                (textElement.boxHeightFrac * pageHpx).coerceIn(with(density) { 20.dp.toPx() }, capH)
            } else {
                autoH
            }
            val boxWidthDp = with(density) { boxWpx.toDp() }
            val boxHeightDp = with(density) { boxHpx.toDp() }
            var boxMeasured by remember(textElement.id) { mutableStateOf(IntSize.Zero) }
            var rowMeasured by remember(textElement.id) { mutableStateOf(IntSize.Zero) }
            val xFracState = rememberUpdatedState(textElement.xFraction)
            val yFracState = rememberUpdatedState(textElement.yFraction)
            val rotState = rememberUpdatedState(textElement.rotationDeg)
            val sizeSpState = rememberUpdatedState(textElement.fontSizeSp)
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
            val bgChars = charColorsFor(textElement.text.length, textElement.bgColorArgb, textElement.bgColorRanges)
            val hasBg = bgChars.any { it != null }
            val borderChars = charColorsFor(textElement.text.length, textElement.borderColorArgb, textElement.borderRanges)
            val hasBorder = borderChars.any { it != null }
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
            Box(
                modifier = Modifier
                    .offset {
                        val pw = sizePx.width.toFloat().coerceAtLeast(1f)
                        val ph = sizePx.height.toFloat().coerceAtLeast(1f)
                        IntOffset(
                            (textElement.xFraction * pw - handlePadPx).roundToInt(),
                            (textElement.yFraction * ph - handlePadPx).roundToInt()
                        )
                    }
                    .onSizeChanged { boxMeasured = it }
                    .graphicsLayer { rotationZ = textElement.rotationDeg }
            ) {
            Box(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .onSizeChanged { rowMeasured = it }
                    .then(
                        if (selected) {
                            Modifier.drawBehind {
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
                    .pointerInput(textElement.id) {
                        val slop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var dragging = false
                            var pinching = false
                            var xf = xFracState.value
                            var yf = yFracState.value
                            var sp = sizeSpState.value
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
                                val delta = change.positionChange()
                                if (!dragging) {
                                    if ((change.position - down.position).getDistance() > slop) {
                                        dragging = true
                                        xf = xFracState.value
                                        yf = yFracState.value
                                        onTextSelect(textElement.id)
                                    }
                                }
                                if (dragging) {
                                    change.consume()
                                    val sz = sizeState.value
                                    if (sz.width > 0 && sz.height > 0) {
                                        val bw = if (rowMeasured.width > 0) rowMeasured.width.toFloat() else boxWState.value
                                        val bh = if (rowMeasured.height > 0) rowMeasured.height.toFloat() else boxHState.value
                                        val rad = rotState.value * (Math.PI.toFloat() / 180f)
                                        val cos = kotlin.math.cos(rad)
                                        val sin = kotlin.math.sin(rad)
                                        val dx = delta.x * cos - delta.y * sin
                                        val dy = delta.x * sin + delta.y * cos
                                        xf = (xf + dx / sz.width).coerceIn(-1.2f, 1.2f)
                                        yf = (yf + dy / sz.height).coerceIn(-1.2f, 1.2f)
                                        onTextDrag(textElement.id, xf, yf)
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
                val fontFamily = FontCatalog.resolveComposeFontFamily(
                    textElement.fontId,
                    customFonts
                )
                Box(
                    contentAlignment = Alignment.TopStart,
                    modifier = Modifier
                        .width(boxWidthDp)
                        .then(
                            if (textElement.boxHeightFrac > 0.01f) {
                                Modifier.height(boxHeightDp).clipToBounds()
                            } else {
                                Modifier
                            }
                        )
                ) {
                BasicTextField(
                    value = fieldValue,
                    onValueChange = { newValue ->
                        fieldValue = newValue
                        onTextValueChange(textElement.id, newValue.text, newValue.selection)
                    },
                    visualTransformation = TextStyleRangesVisualTransformation(
                        boldRanges = textElement.boldRanges,
                        italicRanges = textElement.italicRanges,
                        underlineRanges = textElement.underlineRanges,
                        strikeRanges = textElement.strikeRanges,
                        quoteRanges = textElement.quoteRanges,
                        fontRanges = textElement.fontRanges,
                        fontFamilies = (listOf(textElement.fontId) + textElement.fontRanges.map { it.fontId })
                            .distinct()
                            .associateWith { FontCatalog.resolveComposeFontFamily(it, customFonts) },
                        colorRanges = textElement.colorRanges,
                        // Backgrounds are drawn in drawBehind (rounded + opacity).
                        bgColorRanges = emptyList(),
                        wholeBgColorArgb = null,
                        linkRanges = textElement.linkRanges,
                        shadowRanges = textElement.shadowRanges
                    ),
                    textStyle = TextStyle(
                        color = Color(textElement.textColorArgb),
                        fontSize = textElement.fontSizeSp.sp,
                        fontFamily = fontFamily,
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
                    cursorBrush = SolidColor(Color.Black),
                    onTextLayout = { res ->
                        val prev = textLayout
                        if (prev == null || prev.layoutInput != res.layoutInput || prev.size != res.size) textLayout = res
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(Alignment.Top, unbounded = true)
                        .drawBehind {
                            val lay = textLayout
                            if (lay != null && hasBg) {
                                drawTextBackgrounds(
                                    lay,
                                    bgChars,
                                    textElement.bgCornerDp.dp.toPx(),
                                    textElement.bgAlpha
                                )
                            }
                            val bLay = borderLayout
                            if (bLay != null) drawText(textLayoutResult = bLay)
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
                                    onTap = {
                                        if (selectedNow.value) {
                                            focusRequester.requestFocus()
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

            if (selected) {
                val spinView = androidx.compose.ui.platform.LocalView.current
                val lastSnap = remember(textElement.id) { floatArrayOf(Float.NaN) }
                TextHandle(
                    symbol = "\u21bb",
                    modifier = Modifier.align(Alignment.TopEnd),
                    onDragStart = {
                        rotHud = true
                        rawRot = rotState.value
                        lastSnap[0] = Float.NaN
                    },
                    onDragEnd = { rotHud = false }
                ) { drag ->
                    onTextSelect(textElement.id)
                    val halfW = (if (rowMeasured.width > 0) rowMeasured.width else boxMeasured.width) / 2f
                    val halfH = (if (rowMeasured.height > 0) rowMeasured.height else boxMeasured.height) / 2f
                    val vx = (halfW + handlePadPx).coerceAtLeast(12f)
                    val vy = -(halfH + handlePadPx).coerceAtLeast(12f)
                    val r2 = (vx * vx + vy * vy).coerceAtLeast(1f)
                    val dDeg = ((vx * drag.y - vy * drag.x) / r2) * (180f / 3.14159265f)
                    rawRot += dDeg
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
                val liveW = remember(textElement.id) { floatArrayOf(0f) }
                val liveH = remember(textElement.id) { floatArrayOf(0f) }
                val startBox = remember(textElement.id) { floatArrayOf(1f, 1f, 16f) }
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
                            boxWState.value
                        }
                        val pad = with(density) { 8.dp.toPx() }
                        liveH[0] = if (heightFracState.value > 0.01f) {
                            heightFracState.value * pageH
                        } else {
                            (rowMeasured.height.toFloat() - pad).coerceAtLeast(with(density) { 20.dp.toPx() })
                        }
                        startBox[0] = liveW[0].coerceAtLeast(1f)
                        startBox[1] = liveH[0].coerceAtLeast(1f)
                        startBox[2] = sizeSpState.value
                    },
                    onDragEnd = { boxHud = false }
                ) { drag ->
                    onTextSelect(textElement.id)
                    val pageW = sizeState.value.width.toFloat().coerceAtLeast(1f)
                    val pageH = sizeState.value.height.toFloat().coerceAtLeast(1f)
                    val minW = with(density) { 24.dp.toPx() }
                    val minH = with(density) { 20.dp.toPx() }
                    liveW[0] = (liveW[0] + drag.x).coerceIn(minW, pageW * 1.6f)
                    liveH[0] = (liveH[0] + drag.y).coerceIn(minH, pageH * 1.6f)
                    // Text scales with the box (geometric mean of both axes).
                    val fw = liveW[0] / startBox[0]
                    val fh = liveH[0] / startBox[1]
                    val scaledSp = (startBox[2] * kotlin.math.sqrt((fw * fh).coerceAtLeast(0.0001f)))
                        .coerceIn(6f, 96f)
                    resizeState.value(
                        textElement.id,
                        (liveW[0] / pageW).coerceIn(0.02f, 1.8f),
                        (liveH[0] / pageH).coerceIn(0.02f, 1.8f),
                        scaledSp
                    )
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
                val hudCap = if (rotHud) "Rotate" else if (boxHud) "Box" else "Size"
                val boxTop = (textElement.yFraction * sizePx.height).toInt()
                Box(
                    modifier = Modifier.offset {
                        val hudY = if (boxTop > 72) 10 else (sizePx.height - 72).coerceAtLeast(10)
                        IntOffset((sizePx.width / 2) - 48, hudY)
                    }
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
    val energyPhase = rememberPageEnergyPhase()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .shadow(2.dp, RoundedCornerShape(0.dp))
            .border(1.dp, Color(0x33000000))
            .drawWithContent {
                drawContent()
                drawPageEnergyBorder(energyPhase.floatValue)
            }
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
    onDrag: (Offset) -> Unit
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .shadow(3.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(1.dp, Color(0x33000000), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    down.consume()
                    onDragStart()
                    val slop = viewConfiguration.touchSlop
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            onDragEnd()
                            break
                        }
                        val delta = change.positionChange()
                        if (!moved && (change.position - down.position).getDistance() > slop) {
                            moved = true
                        }
                        if (moved) {
                            change.consume()
                            if (delta.x != 0f || delta.y != 0f) onDrag(delta)
                        }
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPageEnergyBorder(phase: Float) {
    val inset = 1.1f.dp.toPx()
    val outline = Path().apply {
        addRect(Rect(inset, inset, size.width - inset, size.height - inset))
    }
    drawPath(outline, Color(0xFF4A4A4A), style = Stroke(width = 1.2f.dp.toPx()))
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
    drawPath(piece(length * 0.18f), Color(0x3300D9FF), style = Stroke(width = 4.2f.dp.toPx(), cap = round))
    drawPath(piece(length * 0.12f), Color(0xAA4285FF), style = Stroke(width = 1.8f.dp.toPx(), cap = round))
    drawPath(piece(length * 0.045f), Color(0xFF00D9FF), style = Stroke(width = 2.dp.toPx(), cap = round))
}
