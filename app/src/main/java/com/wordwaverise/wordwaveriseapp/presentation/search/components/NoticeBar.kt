package com.wordwaverise.wordwaveriseapp.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.LookupNoticeDto
import com.wordwaverise.wordwaveriseapp.ui.theme.*

/**
 * Explains a substitution the server made — a corrected typo, a resolved word form, a fixed
 * keyboard layout.
 *
 * A typo used to produce an error screen. It now produces the right article plus this line, so
 * the correction is visible and, crucially, reversible: tapping searches for exactly what was
 * typed.
 */
@Composable
fun NoticeBar(
    notice: LookupNoticeDto,
    onSearchOriginal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PrimaryCyan.copy(alpha = 0.10f))
            .then(
                notice.originalQuery?.let { original ->
                    Modifier.clickable { onSearchOriginal(original) }
                } ?: Modifier
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = notice.textRu,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
        if (notice.originalQuery != null) {
            Text(
                text = "Искать точно",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryCyan
            )
        }
    }
}
