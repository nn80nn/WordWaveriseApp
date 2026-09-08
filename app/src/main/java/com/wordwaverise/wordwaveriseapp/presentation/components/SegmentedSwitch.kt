package com.wordwaverise.wordwaveriseapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.ui.theme.WaveTheme

/** One half of a merged tab. [badge] draws a dot, for a count nobody needs the exact value of. */
data class Segment(
    val key: String,
    val label: String,
    val badge: Boolean = false,
    val icon: ImageVector? = null
)

/**
 * A pill switch between the two halves of one tab.
 *
 * Two screens under one tab need something that says which one is showing and how to reach the
 * other, and a bar of chips would read as a filter — the thing that is already inside both
 * halves. The shape is the web's (`AppTopBar.vue`), so a person with both installed sees the
 * same control in the same place.
 */
@Composable
fun SegmentedSwitch(
    segments: List<Segment>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WaveTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.surfaceElevated)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        segments.forEach { segment ->
            val isSelected = segment.key == selected
            // Свой источник взаимодействий без ряби: сегмент — это не кнопка действия, а
            // указание, где ты находишься, и волна на нём читается как «что-то произошло».
            val interaction = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.surface else colors.surfaceElevated)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = !isSelected
                    ) { onSelect(segment.key) }
                    .padding(vertical = 9.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (segment.icon != null) {
                        Icon(
                            imageVector = segment.icon,
                            contentDescription = null,
                            tint = if (isSelected) colors.textPrimary else colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = segment.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) colors.textPrimary else colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    if (segment.badge) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.secondary)
                        )
                    }
                }
            }
        }
    }
}
