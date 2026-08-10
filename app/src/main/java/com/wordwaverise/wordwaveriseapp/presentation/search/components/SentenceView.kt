package com.wordwaverise.wordwaveriseapp.presentation.search.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalysisDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.TokenDto
import com.wordwaverise.wordwaveriseapp.ui.theme.*

/**
 * A pasted sentence with every word tappable.
 *
 * Answers the question the dictionary cannot: not "what can this word mean" but "what does it
 * mean here". Tokens come from the server, so the index sent back on a tap refers to exactly
 * the tokenisation the analysis was built from.
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
            "Нажмите на слово, чтобы разобрать его в этом предложении",
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

            analysis != null -> ContextCard(analysis = analysis, onOpenArticle = onOpenArticle)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ContextCard(
    analysis: ContextAnalysisDto,
    onOpenArticle: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary)
    ) {
        Column(Modifier.padding(16.dp)) {

            // The word as it appears here, translated in the form it appears in.
            analysis.translationRu?.let { translation ->
                Text(
                    text = translation,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                analysis.lemma?.let { lemma ->
                    Text(lemma, fontSize = 14.sp, color = PrimaryCyan, fontWeight = FontWeight.Medium)
                }
                analysis.pos?.let { pos -> Badge(pos, TextTertiary) }
                if (analysis.senseMatched) Badge("значение из статьи", PrimaryCyan)
            }

            analysis.translationLemmaRu?.takeIf { it != analysis.translationRu }?.let { lemmaRu ->
                Spacer(Modifier.height(4.dp))
                Text("словарная форма: $lemmaRu", fontSize = 12.sp, color = TextTertiary)
            }

            analysis.senseDefinitionEn?.takeIf { it.isNotBlank() }?.let { gloss ->
                Spacer(Modifier.height(10.dp))
                Text(gloss, fontSize = 14.sp, color = TextSecondary, lineHeight = 19.sp)
            }

            analysis.whyRu?.takeIf { it.isNotBlank() }?.let { why ->
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryCyan.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = why,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            analysis.sentenceRu?.takeIf { it.isNotBlank() }?.let { sentenceRu ->
                Spacer(Modifier.height(12.dp))
                Text("Перевод предложения", fontSize = 11.sp, color = TextTertiary)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sentenceRu,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
            }

            analysis.lemma?.takeIf { analysis.entryAvailable }?.let { lemma ->
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = { onOpenArticle(lemma) }) {
                    Text("Открыть статью «$lemma»", color = PrimaryCyan, fontSize = 14.sp)
                }
            }
        }
    }
}
