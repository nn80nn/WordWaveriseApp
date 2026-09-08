package com.wordwaverise.wordwaveriseapp.presentation.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalysisDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextHintDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.TokenDto
import com.wordwaverise.wordwaveriseapp.R
import com.wordwaverise.wordwaveriseapp.ui.theme.*

/**
 * A pasted sentence with every word tappable.
 *
 * Answers the question the dictionary cannot: not "what can this word mean" but "what does it
 * mean here". Tokens come from the server, so the index sent back on a tap refers to exactly
 * the tokenisation the analysis was built from.
 *
 * The answer itself lives in [ContextCard], shared with the reader: a word explained in a book
 * and a word explained in a pasted line are the same question, and two renderings of it would
 * drift apart on the first change to either.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceView(
    tokens: List<TokenDto>,
    selectedIndex: Int?,
    analysis: ContextAnalysisDto?,
    isAnalyzing: Boolean,
    onTokenClick: (Int) -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.nazhmite_na_slovo_chtoby_razobrat_ego_v),
            fontSize = 13.sp,
            color = TextTertiary
        )
        Spacer(Modifier.height(12.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            tokens.forEach { token ->
                // A phrasal verb highlights as one unit, because that is how it resolves.
                val isSelected = selectedIndex != null &&
                    (token.index == selectedIndex || selectedIndex in token.groupWith)

                Box(
                    modifier = Modifier
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isSelected -> PrimaryCyan.copy(alpha = 0.20f)
                                token.tappable -> BackgroundSecondary
                                else -> androidx.compose.ui.graphics.Color.Transparent
                            }
                        )
                        .then(
                            if (token.tappable) Modifier.clickable { onTokenClick(token.index) }
                            else Modifier
                        )
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = token.text,
                        fontSize = 16.sp,
                        color = if (token.tappable) TextPrimary else TextTertiary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            isAnalyzing -> Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = PrimaryCyan,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            }

            // Здесь в карточку едет сразу полный разбор: этот экран и есть просьба разобрать
            // предложение, и ждать его — то, ради чего сюда пришли. В книге наоборот: там
            // сначала подсказка, потому что человек читает, а не разбирает.
            analysis != null -> ContextCard(
                hint = analysis.asHint(),
                isHinting = false,
                analysis = analysis,
                onOpenArticle = onOpenArticle
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** Полный разбор в форме подсказки: те же поля, и карточка рисует их одним кодом. */
private fun ContextAnalysisDto.asHint() = ContextHintDto(
    text = text,
    tokens = tokens,
    target = target,
    lemma = lemma,
    pos = pos,
    translationRu = translationRu,
    senseId = senseId,
    senseMatched = senseMatched,
    senseDefinitionEn = senseDefinitionEn,
    entryAvailable = entryAvailable
)
