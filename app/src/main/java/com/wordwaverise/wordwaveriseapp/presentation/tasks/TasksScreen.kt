package com.wordwaverise.wordwaveriseapp.presentation.tasks

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wordwaverise.wordwaveriseapp.data.local.entity.FlashcardEntity
import com.wordwaverise.wordwaveriseapp.ui.theme.*

@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .waveSurface()
            .padding(16.dp)
    ) {
        when {
            state.isMultipleChoiceActive -> {
                MultipleChoiceMode(
                    question = state.multipleChoiceQuestion,
                    selectedIndex = state.selectedChoiceIndex,
                    answered = state.choiceAnswered,
                    onSelect = viewModel::selectChoice,
                    onNext = viewModel::loadNextMultipleChoice,
                    onExit = viewModel::exitMultipleChoice
                )
            }
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
                    onStartExercise = { viewModel.startExerciseMode() },
                    onStartMultipleChoice = { viewModel.startMultipleChoice() }
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
    onStartExercise: () -> Unit,
    onStartMultipleChoice: () -> Unit = {}
) {
    val colors = WaveTheme.colors

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Eyebrow("Сегодня", modifier = Modifier.padding(top = 4.dp))

        // Counters. A hairline splits them instead of a second card, so the two
        // numbers read as one instrument.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
            border = BorderStroke(1.dp, WaveTheme.colors.border),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    modifier = Modifier.weight(1f),
                    label = "К повторению",
                    value = "$dueCount",
                    accent = if (dueCount > 0) colors.brass else colors.textMuted
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(colors.hairline)
                )
                StatItem(
                    modifier = Modifier.weight(1f),
                    label = "Всего карточек",
                    value = "$totalCount",
                    accent = colors.secondary
                )
            }
        }

        // The one filled control on the screen — the signature gradient is
        // spent here and nowhere else.
        val sessionEnabled = dueCount > 0
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (sessionEnabled) Modifier.background(signatureGradient())
                    else Modifier
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                )
                .then(
                    if (sessionEnabled) Modifier.contourField(
                        color = colors.onAccent, alpha = 0.10f, spacing = 20.dp
                    ) else Modifier
                )
                .clickable(enabled = sessionEnabled) { onStartSession() }
                .padding(vertical = 10.dp, horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (sessionEnabled) colors.onAccent else colors.textMuted
                )
                Text(
                    text = if (sessionEnabled) "Начать сессию"
                           else "Нет карточек к повторению",
                    color = if (sessionEnabled) colors.onAccent else colors.textMuted,
                    fontFamily = Comfortaa,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (hasWords) {
            PracticeRow(
                icon = Icons.Default.AutoAwesome,
                title = "AI Упражнения",
                subtitle = "Пропущенное слово в живом предложении",
                onClick = onStartExercise
            )
            PracticeRow(
                icon = Icons.Default.GpsFixed,
                title = "Выбор ответа",
                subtitle = "Четыре определения, одно ваше",
                onClick = onStartMultipleChoice
            )
        }

        if (hasWords) {
            Spacer(modifier = Modifier.weight(1f))
            Horizon(modifier = Modifier.padding(bottom = 4.dp))
        }

        // Info Card
        if (dueCount == 0 && totalCount == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
                border = BorderStroke(1.dp, WaveTheme.colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = colors.brass,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Как создать карточки?",
                        fontFamily = Comfortaa,
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
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}

/**
 * Secondary practice modes. Cards with a leading rule rather than more filled
 * gradients — a screen carrying three saturated slabs stops having a primary
 * action at all.
 */
@Composable
private fun PracticeRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = WaveTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        border = BorderStroke(1.dp, WaveTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(colors.secondary, colors.primary)))
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.secondary,
                modifier = Modifier
                    .padding(start = 15.dp, end = 14.dp)
                    .size(22.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = Comfortaa,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun StatItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accent: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = Comfortaa,
            fontSize = 38.sp,
            lineHeight = 42.sp,
            letterSpacing = (-1.4).sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label.uppercase(),
            style = EyebrowStyle,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FlipHint(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
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
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
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
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
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
            FlipHint(text = "Нажмите на карточку чтобы увидеть ответ", modifier = Modifier.fillMaxWidth())
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
            .heightIn(min = 320.dp, max = 420.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onFlip() },
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        border = BorderStroke(1.dp, WaveTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                        // A dictionary sets its headword the way the word is
                        // written, so no shouting caps and no added tracking.
                        Text(
                            text = word,
                            fontFamily = Comfortaa,
                            fontSize = 40.sp,
                            lineHeight = 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-1.2).sp
                        )
                        phonetic?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = it,
                                fontFamily = JetBrainsMono,
                                fontSize = 16.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        FlipHint(text = "Нажмите чтобы увидеть определение")
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
                        FlipHint(text = "Нажмите чтобы увидеть слово")
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
                        // A dictionary sets its headword the way the word is
                        // written, so no shouting caps and no added tracking.
                        Text(
                            text = word,
                            fontFamily = Comfortaa,
                            fontSize = 40.sp,
                            lineHeight = 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-1.2).sp
                        )
                        phonetic?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(it, fontFamily = JetBrainsMono, fontSize = 16.sp, color = TextSecondary, textAlign = TextAlign.Center)
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

// ── Multiple Choice mode ──────────────────────────────────────────────────────

@Composable
private fun MultipleChoiceMode(
    question: MultipleChoiceQuestion?,
    selectedIndex: Int?,
    answered: Boolean,
    onSelect: (Int) -> Unit,
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
                Icon(Icons.Default.Close, contentDescription = "Выход", tint = TextPrimary)
            }
            Text(
                text = "Выбор ответа",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Box(modifier = Modifier.size(48.dp))
        }

        if (question == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryCyan)
            }
            return@Column
        }

        // Question card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
            border = BorderStroke(1.dp, WaveTheme.colors.border),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (question.wordFirst) "Что означает слово:" else "Какое это слово:",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = question.questionText,
                    fontSize = if (question.wordFirst) 28.sp else 16.sp,
                    fontWeight = if (question.wordFirst) FontWeight.Bold else FontWeight.Normal,
                    color = TextPrimary,
                    lineHeight = 22.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Options
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.options.forEachIndexed { idx, option ->
                val isCorrect = idx == question.correctIndex
                val isSelected = idx == selectedIndex
                val bgColor = when {
                    !answered -> BackgroundSecondary
                    isCorrect -> Success.copy(alpha = 0.18f)
                    isSelected -> Error.copy(alpha = 0.18f)
                    else -> BackgroundSecondary
                }
                val borderColor = when {
                    !answered && isSelected -> PrimaryCyan
                    answered && isCorrect -> Success
                    answered && isSelected -> Error
                    else -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .then(
                            if (borderColor != Color.Transparent)
                                Modifier.background(bgColor, RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .clickable(enabled = !answered) { onSelect(idx) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${('A' + idx)}.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                answered && isCorrect -> Success
                                answered && isSelected -> Error
                                else -> TextTertiary
                            }
                        )
                        Text(
                            text = option,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 20.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (answered && isCorrect) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Success, modifier = Modifier.size(18.dp))
                        }
                        if (answered && isSelected && !isCorrect) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Next button (visible after answering)
        AnimatedVisibility(
            visible = answered,
            enter = fadeIn(tween(300)) + expandVertically()
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Следующий вопрос →", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
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
                text = "AI Упражнения",
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
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
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
                    border = BorderStroke(1.dp, WaveTheme.colors.border),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isCorrect) Success else Error,
                                modifier = Modifier.size(24.dp)
                            )
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
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = Warning,
            modifier = Modifier.size(80.dp)
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

/**
 * Layered swells closing the foot of the screen. Purely atmospheric, and
 * deliberately faint — it exists so an empty study day still looks like part
 * of the water rather than like a page that failed to load.
 */
@Composable
private fun Horizon(modifier: Modifier = Modifier) {
    val colors = WaveTheme.colors
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        val tones = listOf(
            colors.primary.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
            colors.secondary.copy(alpha = if (colors.isDark) 0.13f else 0.09f),
            colors.brass.copy(alpha = if (colors.isDark) 0.10f else 0.08f)
        )
        tones.forEachIndexed { i, tone ->
            val baseY = size.height * (0.42f + i * 0.19f)
            val amp = size.height * (0.13f - i * 0.02f)
            val path = Path().apply {
                moveTo(0f, baseY)
                var x = 0f
                var up = i % 2 == 0
                val half = size.width / 3f
                while (x < size.width) {
                    val peak = if (up) baseY - amp else baseY + amp
                    cubicTo(x + half * 0.35f, peak, x + half * 0.65f, peak, x + half, baseY)
                    x += half
                    up = !up
                }
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, tone)
        }
    }
}
