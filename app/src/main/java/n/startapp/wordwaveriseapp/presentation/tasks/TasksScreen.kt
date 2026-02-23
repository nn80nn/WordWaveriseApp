package n.startapp.wordwaveriseapp.presentation.tasks

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import n.startapp.wordwaveriseapp.data.local.entity.FlashcardEntity
import n.startapp.wordwaveriseapp.ui.theme.*

@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(16.dp)
    ) {
        when {
            state.isSessionActive -> {
                FlashcardSession(
                    flashcards = state.sessionFlashcards,
                    currentIndex = state.currentCardIndex,
                    onCorrect = { viewModel.markCorrect() },
                    onIncorrect = { viewModel.markIncorrect() },
                    onExit = { viewModel.exitSession() }
                )
            }
            else -> {
                TasksOverview(
                    dueCount = state.dueCount,
                    totalCount = state.totalCount,
                    onStartSession = { viewModel.startSession() }
                )
            }
        }
    }
}

@Composable
private fun TasksOverview(
    dueCount: Int,
    totalCount: Int,
    onStartSession: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Card
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = BackgroundSecondary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    icon = "🔥",
                    label = "К повторению",
                    value = "$dueCount"
                )
                StatItem(
                    icon = "📚",
                    label = "Всего карточек",
                    value = "$totalCount"
                )
            }
        }

        // Start Session Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (dueCount > 0) listOf(PrimaryBright, PrimaryCyan)
                                 else listOf(PrimaryBright.copy(alpha = 0.5f), PrimaryCyan.copy(alpha = 0.5f))
                    )
                )
                .clickable(enabled = dueCount > 0) { onStartSession() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = if (dueCount > 0) "Начать сессию" else "Нет карточек к повторению",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Info Card
        if (dueCount == 0 && totalCount == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BackgroundSecondary)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💡",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Как создать карточки?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Сохраняйте новые слова из поиска, и они автоматически станут карточками для изучения!",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: String,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 40.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryCyan
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FlashcardSession(
    flashcards: List<FlashcardEntity>,
    currentIndex: Int,
    onCorrect: () -> Unit,
    onIncorrect: () -> Unit,
    onExit: () -> Unit
) {
    if (flashcards.isEmpty() || currentIndex >= flashcards.size) {
        SessionComplete(onExit = onExit)
        return
    }

    val currentCard = flashcards[currentIndex]
    var isFlipped by remember(currentIndex) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Выход",
                    tint = TextPrimary
                )
            }
            Text(
                text = "${currentIndex + 1} / ${flashcards.size}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            // Placeholder for symmetry
            Box(modifier = Modifier.size(48.dp))
        }

        // Progress Bar
        LinearProgressIndicator(
            progress = (currentIndex + 1).toFloat() / flashcards.size.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = PrimaryCyan,
            trackColor = BackgroundLight
        )

        Spacer(modifier = Modifier.weight(0.5f))

        // Flashcard
        FlippableCard(
            word = currentCard.word,
            phonetic = currentCard.phonetic,
            definition = currentCard.definition,
            example = currentCard.example,
            translation = currentCard.translation,
            isFlipped = isFlipped,
            onFlip = { isFlipped = !isFlipped }
        )

        Spacer(modifier = Modifier.weight(0.5f))

        // Action Buttons (только если карточка перевернута)
        AnimatedVisibility(
            visible = isFlipped,
            enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(200)) + shrinkVertically()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onIncorrect()
                        isFlipped = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("❌", fontSize = 20.sp)
                        Text(
                            "Не знаю",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Button(
                    onClick = {
                        onCorrect()
                        isFlipped = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Success
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✅", fontSize = 20.sp)
                        Text(
                            "Знаю",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Hint to flip
        if (!isFlipped) {
            Text(
                text = "👆 Нажмите на карточку чтобы увидеть ответ",
                fontSize = 13.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FlippableCard(
    word: String,
    phonetic: String?,
    definition: String,
    example: String?,
    translation: String?,
    isFlipped: Boolean,
    onFlip: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "card_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onFlip() },
        colors = CardDefaults.cardColors(
            containerColor = BackgroundSecondary
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = if (rotation > 90f) 180f else 0f
                }
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front - Word
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = word.uppercase(),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )
                    phonetic?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = it,
                            fontSize = 20.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    translation?.let {
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = PrimaryCyan.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = it,
                                fontSize = 18.sp,
                                color = PrimaryCyan,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // Back - Definition
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = definition,
                        fontSize = 18.sp,
                        color = TextPrimary,
                        lineHeight = 26.sp
                    )
                    example?.let {
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = BackgroundLight
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Пример:",
                                    fontSize = 12.sp,
                                    color = TextTertiary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"$it\"",
                                    fontSize = 15.sp,
                                    color = TextSecondary,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionComplete(onExit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            fontSize = 80.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Сессия завершена!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Отличная работа! Продолжайте учиться каждый день",
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryCyan
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Завершить",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
