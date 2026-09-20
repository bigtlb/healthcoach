package com.lbthomas.healthcoach.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox as MaterialTooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Utility composable that wraps [content] with a [MaterialTooltipBox] displaying the given [tooltip] text.
 *
 * @param tooltip The text to display in the tooltip.
 * @param modifier Modifier applied to the TooltipBox.
 * @param positionProvider Position provider for the tooltip. Defaults to below anchor.
 * @param state Tooltip state.
 * @param content The composable content anchored to the tooltip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tooltip(
    tooltip: String,
    modifier: Modifier = Modifier,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
    state: TooltipState = rememberTooltipState(),
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        MaterialTooltipBox(
            positionProvider = positionProvider,
            tooltip = {
                PlainTooltip {
                    Text(tooltip)
                }
            },
            state = state,
            modifier = Modifier.wrapContentSize(),
            content = content
        )
    }
}

