package com.wordwaverise.wordwaveriseapp.presentation.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.RuEnCandidateDto
import com.wordwaverise.wordwaveriseapp.R
import com.wordwaverise.wordwaveriseapp.ui.theme.*

/**
 * English options for a Russian query.
 *
 * The old panel showed a row of bare words and left the user guessing which to use. Each option
 * now says which sense it covers and when to reach for it rather than its neighbours — that
 * difference is the whole point of the screen.
 */
@Composable
fun RuEnCandidatesView(
    query: String,
    candidates: List<RuEnCandidateDto>,
    note: String?,
    isAmbiguous: Boolean,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Переводы для «$query»",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        if (isAmbiguous) {
            Spacer(Modifier.height(6.dp))
            Badge(stringResource(R.string.neskolko_znacheniy), PrimaryCyan)
        }

        note?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
        }

        Spacer(Modifier.height(14.dp))

        when {
            isLoading -> Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = PrimaryCyan,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            }

            candidates.isEmpty() -> Text(
                stringResource(R.string.perevod_ne_nayden),
                fontSize = 14.sp,
                color = TextTertiary,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            else -> candidates.forEach { candidate ->
                CandidateCard(candidate = candidate, onSelect = onSelect)
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CandidateCard(
    candidate: RuEnCandidateDto,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(candidate.en) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        border = BorderStroke(1.dp, WaveTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = candidate.en,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                candidate.cefr?.let { Badge(it, PrimaryCyan) }
            }

            if (candidate.pos.isNotBlank() || candidate.ruGloss.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = listOf(candidate.pos, candidate.ruGloss)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    fontSize = 13.sp,
                    color = PrimaryCyan
                )
            }

            if (candidate.whenToUse.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(candidate.whenToUse, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
            }

            if (candidate.example.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = candidate.example,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
                if (candidate.exampleRu.isNotBlank()) {
                    Text(candidate.exampleRu, fontSize = 12.sp, color = TextTertiary, lineHeight = 17.sp)
                }
            }
        }
    }
}
