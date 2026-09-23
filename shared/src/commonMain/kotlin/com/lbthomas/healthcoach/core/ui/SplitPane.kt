package com.lbthomas.healthcoach.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalSplitPane(
    modifier: Modifier = Modifier,
    initialFraction: Float = 0.5f,
    onFractionChange: ((Float) -> Unit)? = null,
    minFirstPaneWidth: Dp = 250.dp,
    minSecondPaneWidth: Dp = 250.dp,
    first: @Composable BoxScope.() -> Unit,
    second: @Composable BoxScope.() -> Unit
) {
    var fraction by remember(initialFraction) { mutableFloatStateOf(initialFraction) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val minFraction = if (totalWidth > 0.dp) (minFirstPaneWidth / totalWidth).coerceIn(0.05f, 0.95f) else 0.1f
        val maxFraction = if (totalWidth > 0.dp) (1f - (minSecondPaneWidth / totalWidth)).coerceIn(minFraction, 0.95f) else 0.9f

        val effectiveFraction = fraction.coerceIn(minFraction, maxFraction)
        val firstWidth = totalWidth * effectiveFraction

        val totalWidthPx = this.constraints.maxWidth.toFloat()

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(firstWidth)
                    .fillMaxHeight(),
                content = first
            )

            VerticalSplitter(
                onDrag = { deltaPx ->
                    if (totalWidthPx > 0f) {
                        val deltaFraction = deltaPx / totalWidthPx
                        fraction = (fraction + deltaFraction).coerceIn(minFraction, maxFraction)
                    }
                },
                onDragStopped = {
                    onFractionChange?.invoke(fraction.coerceIn(minFraction, maxFraction))
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                content = second
            )
        }
    }
}

@Composable
fun VerticalSplitter(
    onDrag: (Float) -> Unit,
    onDragStopped: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(8.dp)
            .fillMaxHeight()
            .pointerHoverIcon(PointerIcon.Crosshair)
            .draggable(
                state = rememberDraggableState { delta ->
                    onDrag(delta)
                },
                orientation = Orientation.Horizontal,
                startDragImmediately = true,
                onDragStarted = { isDragging = true },
                onDragStopped = {
                    isDragging = false
                    onDragStopped()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isDragging) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
        )
    }
}
