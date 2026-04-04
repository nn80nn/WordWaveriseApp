package n.startapp.wordwaveriseapp.presentation.tasks

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
            state.isExerciseModeActive -> {
                ExerciseMode(
                    isLoading = state.isExerciseLoading,
                    sentence = state.exerciseSentence,
                    userAnswer = state.userAnswer,
                    checked = state.exerciseChecked,
                    isCorrect = state.exerciseIsCorrect,
                    correctAnswer = state.exerciseAnswer,
                    error = state.exerciseError,
                    onAnswerChange = viewModel::onUserAnswerChange,
                    onCheck = { viewModel.checkAnswer() },
                    onNext = { viewModel.loadNextExercise() },
                    onExit = { viewModel.exitExerciseMode() }
                )
            }
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
                    hasWords = state.totalCount > 0,
                    onStartSession = { viewModel.startSession() },
                    onStartExercise = { viewModel.startExerciseMode() }
                )
            }
        }
    }
}

@Composable
private fun TasksOverview(
    dueCount: Int,
    totalCount: Int,
    hasWords: Boolean,
    onStartSession: () -> Unit,
    onStartExercise: () -> Unit
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

        // AI Exercise Button
        if (hasWords) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryBlue.copy(alpha = 0.8f), PrimaryBright.copy(alpha = 0.8f))
                        )
                    )
                    .clickable { onStartExercise() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("✨", fontSize = 18.sp)
                    Text(
                        text = "AI Упражнения",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
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
    // true = word first (default), false = definition first
    var wordFirst by remember { mutableStateOf(true) }

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
            // Toggle side order button
            IconButton(onClick = { wordFirst = !wordFirst; isFlipped = false }) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = if (wordFirst) "Начать с определения" else "Начать со слова",
                    tint = TextSecondary
                )
            }
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
            wordFirst = wordFirst,
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
    wordFirst: Boolean = true,
    onFlip: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "card_rotation"
    )
    // Whether the back side is currently showing
    val showingBack = rotation > 90f
    // translation reveal state — reset when card flips back
    var translationRevealed by remember(isFlipped) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onFlip() },
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = if (showingBack) 180f else 0f }
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!showingBack) {
                // ── Front side ───────────────────────────────────────────
                val frontIsWord = wordFirst
                if (frontIsWord) {
                    // Word side
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
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = it,
                                fontSize = 20.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "👆 Нажмите чтобы увидеть определение",
                            fontSize = 12.sp,
                            color = TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Definition side first
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = definition,
                            fontSize = 17.sp,
                            color = TextPrimary,
                            lineHeight = 25.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "👆 Нажмите чтобы увидеть слово",
                            fontSize = 12.sp,
                            color = TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // ── Back side ────────────────────────────────────────────
                val backIsDefinition = wordFirst
                if (backIsDefinition) {
                    // Definition + example + translation reveal
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = definition,
                            fontSize = 17.sp,
                            color = TextPrimary,
                            lineHeight = 25.sp
                        )
                        example?.let {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Пример:", fontSize = 11.sp, color = TextTertiary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"$it\"",
                                        fontSize = 14.sp,
                                        color = TextSecondary,
                                        fontStyle = FontStyle.Italic,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                        if (!translation.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TranslationReveal(
                                translation = translation,
                                revealed = translationRevealed,
                                onReveal = { translationRevealed = true }
                            )
                        }
                    }
                } else {
                    // Word side on back
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
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(it, fontSize = 20.sp, color = TextSecondary, textAlign = TextAlign.Center)
                        }
                        if (!translation.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TranslationReveal(
                                translation = translation,
                                revealed = translationRevealed,
                                onReveal = { translationRevealed = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationReveal(
    translation: String,
    revealed: Boolean,
    onReveal: () -> Unit
) {
    AnimatedVisibility(
        visible = !revealed,
        exit = fadeOut(tween(200)) + shrinkVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryCyan.copy(alpha = 0.08f))
                .clickable { onReveal() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                Text("Показать перевод", fontSize = 13.sp, color = PrimaryCyan)
            }
        }
    }
    AnimatedVisibility(
        visible = revealed,
        enter = fadeIn(tween(300)) + expandVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryCyan.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = translation,
                fontSize = 17.sp,
                color = PrimaryCyan,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── AI Exercise mode ──────────────────────────────────────────────────────────

@Composable
private fun ExerciseMode(
    isLoading: Boolean,
    sentence: String?,
    userAnswer: String,
    checked: Boolean,
    isCorrect: Boolean,
    correctAnswer: String?,
    error: String?,
    onAnswerChange: (String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
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
                text = "✨ AI Упражнения",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Box(modifier = Modifier.size(48.dp))
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Генерирую упражнение...", fontSize = 14.sp, color = TextTertiary)
                    }
                }
            }

            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😔", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(error, fontSize = 14.sp, color = Error, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Попробовать снова")
                        }
                    }
                }
            }

            sentence != null -> {
                // Sentence card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Вставьте пропущенное слово:",
                            fontSize = 12.sp,
                            color = TextTertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Highlight the blank in the sentence
                        val parts = sentence.split("_____")
                        Text(
                            text = buildAnnotatedString {
                                parts.forEachIndexed { i, part ->
                                    append(part)
                                    if (i < parts.size - 1) {
                                        withStyle(SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold)) {
                                            append("_____")
                                        }
                                    }
                                }
                            },
                            fontSize = 17.sp,
                            color = TextPrimary,
                            lineHeight = 25.sp
                        )
                    }
                }

                // Answer input
                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = { if (!checked) onAnswerChange(it) },
                    label = { Text("Ваш ответ") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !checked,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!checked) onCheck() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Feedback
                if (checked) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect) Success.copy(alpha = 0.12f)
                                            else Error.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(if (isCorrect) "✅" else "❌", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = if (isCorrect) "Правильно!" else "Неправильно",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCorrect) Success else Error
                                )
                                if (!isCorrect && correctAnswer != null) {
                                    Text(
                                        text = "Ответ: $correctAnswer",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action button
                if (!checked) {
                    Button(
                        onClick = onCheck,
                        enabled = userAnswer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Проверить", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Следующее слово →", fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
