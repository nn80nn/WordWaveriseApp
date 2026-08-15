package com.wordwaverise.wordwaveriseapp.presentation.tasks

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
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
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseFormat
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseKind
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseKindInfoDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseScope
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseSource
import com.wordwaverise.wordwaveriseapp.util.ExerciseVerdict
import com.wordwaverise.wordwaveriseapp.R
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
        when (state.mode) {
            TasksMode.CARD_SESSION -> FlashcardSession(
                flashcards = state.sessionFlashcards,
                currentIndex = state.currentCardIndex,
                onCorrect = viewModel::markCorrect,
                onIncorrect = viewModel::markIncorrect,
                onEdit = viewModel::editCurrentCard,
                onExit = viewModel::exitSession
            )

            TasksMode.EXERCISE_SETUP -> ExerciseSetup(
                state = state,
                onSelectFolder = viewModel::selectFolder,
                onSelectScope = viewModel::setScope,
                onToggleKind = viewModel::toggleKind,
                onToggleAll = viewModel::toggleAllKinds,
                onSetCount = viewModel::setCount,
                onStart = viewModel::startExercises,
                onExit = viewModel::exitExercises
            )

            TasksMode.EXERCISE_SESSION -> ExerciseSession(
                state = state,
                onChoose = viewModel::chooseOption,
                onTypedChange = viewModel::onTypedChange,
                onSubmit = viewModel::submitTyped,
                onSkip = viewModel::skipExercise,
                onNext = viewModel::nextExercise,
                onExit = viewModel::exitExercises
            )

            TasksMode.EXERCISE_RESULT -> ExerciseResultView(
                state = state,
                mistakes = viewModel.mistakes(),
                onAgain = viewModel::startExercises,
                onSetup = viewModel::backToSetup
            )

            TasksMode.OVERVIEW -> TasksOverview(
                state = state,
                onSelectFolder = viewModel::selectFolder,
                onStartSession = viewModel::startSession,
                onOpenExercises = viewModel::openExerciseSetup,
                onCreateCards = viewModel::createCardsFromFolder
            )
        }
    }

    state.editingCard?.let { card ->
        EditCardDialog(
            card = card,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::saveCard
        )
    }
}

// ── Overview ─────────────────────────────────────────────────────────────────

@Composable
private fun TasksOverview(
    state: TasksState,
    onSelectFolder: (Int?) -> Unit,
    onStartSession: () -> Unit,
    onOpenExercises: () -> Unit,
    onCreateCards: () -> Unit
) {
    val colors = WaveTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Eyebrow(stringResource(R.string.segodnya), modifier = Modifier.padding(top = 4.dp))

        // The folder decides what the whole tab is about, so it sits above the numbers it
        // changes rather than inside one of the modes.
        FolderChips(
            folders = state.folders,
            selected = state.selectedFolder,
            onSelect = onSelectFolder
        )

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
                    label = stringResource(R.string.k_povtoreniyu),
                    value = "${state.dueCount}",
                    accent = if (state.dueCount > 0) colors.brass else colors.textMuted
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(colors.hairline)
                )
                StatItem(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.vsego_kartochek),
                    value = "${state.totalCount}",
                    accent = colors.secondary
                )
            }
        }

        // The one filled control on the screen — the signature gradient is
        // spent here and nowhere else.
        val sessionEnabled = state.dueCount > 0
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
                    text = if (sessionEnabled) stringResource(R.string.nachat_sessiyu)
                           else stringResource(R.string.net_kartochek_k_povtoreniyu),
                    color = if (sessionEnabled) colors.onAccent else colors.textMuted,
                    fontFamily = Comfortaa,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        PracticeRow(
            icon = Icons.Default.AutoAwesome,
            title = stringResource(R.string.uprazhneniya),
            subtitle = stringResource(R.string.zadaniya_iz_slovarnyh_statey_vashih_slov),
            onClick = onOpenExercises
        )

        PracticeRow(
            icon = Icons.Default.LibraryAdd,
            title = stringResource(R.string.sozdat_kartochki_iz_papki),
            subtitle = stringResource(R.string.sohranyayte_novye_slova_iz_poiska_i_oni),
            enabled = !state.isBulkCreating,
            onClick = onCreateCards
        )

        state.bulkMessage?.let {
            Text(
                text = it,
                style = ApparatusStyle,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (state.totalCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Horizon(modifier = Modifier.padding(bottom = 4.dp))
        }

        // Info Card
        if (state.dueCount == 0 && state.totalCount == 0) {
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
                        text = stringResource(R.string.kak_sozdat_kartochki),
                        fontFamily = Comfortaa,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.sohranyayte_novye_slova_iz_poiska_i_oni),
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

// ── Shared pieces ────────────────────────────────────────────────────────────

/**
 * The folder filter. `null` is every folder and `-1` is the words in no folder — the same
 * three-way convention the API and the web use, so "эта папка" means one thing everywhere.
 */
@Composable
private fun FolderChips(
    folders: List<FolderOption>,
    selected: Int?,
    onSelect: (Int?) -> Unit
) {
    if (folders.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(folders, key = { it.id ?: Int.MIN_VALUE }) { folder ->
            ChipButton(
                text = folder.name,
                selected = folder.id == selected,
                onClick = { onSelect(folder.id) }
            )
        }
    }
}

@Composable
private fun ChipButton(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = WaveTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (selected) Modifier.background(colors.secondary.copy(alpha = 0.16f))
                else Modifier.background(colors.surfaceElevated)
            )
            .border(
                1.dp,
                if (selected) colors.secondary else colors.border,
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = when {
                !enabled -> colors.textMuted
                selected -> colors.secondary
                else -> TextSecondary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    enabled: Boolean = true,
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
                .clickable(enabled = enabled) { onClick() },
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
private fun ModeHeader(title: String, onExit: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onExit) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.vyhod),
                tint = TextPrimary
            )
        }
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) { trailing() }
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

// ── Exercise setup ───────────────────────────────────────────────────────────

@Composable
private fun ExerciseSetup(
    state: TasksState,
    onSelectFolder: (Int?) -> Unit,
    onSelectScope: (ExerciseScope) -> Unit,
    onToggleKind: (ExerciseKind) -> Unit,
    onToggleAll: () -> Unit,
    onSetCount: (Int) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit
) {
    val colors = WaveTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModeHeader(title = stringResource(R.string.uprazhneniya), onExit = onExit)

        Eyebrow(stringResource(R.string.papka))
        FolderChips(state.folders, state.selectedFolder, onSelectFolder)
        Text(
            text = stringResource(R.string.slov_v_podborke, state.wordsAvailable),
            style = ApparatusStyle,
            color = TextSecondary
        )

        Eyebrow(stringResource(R.string.otkuda_brat_slova))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton(
                text = stringResource(R.string.vse_sohranennye),
                selected = state.scope == ExerciseScope.SAVED
            ) { onSelectScope(ExerciseScope.SAVED) }
            ChipButton(
                text = stringResource(R.string.tolko_kartochki),
                selected = state.scope == ExerciseScope.FLASHCARDS
            ) { onSelectScope(ExerciseScope.FLASHCARDS) }
            ChipButton(
                text = stringResource(R.string.k_povtoreniyu),
                selected = state.scope == ExerciseScope.DUE
            ) { onSelectScope(ExerciseScope.DUE) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Eyebrow(stringResource(R.string.tipy_zadaniy))
            Text(
                text = if (state.selectedKinds.size == state.availableKinds.size)
                    stringResource(R.string.snyat_vse) else stringResource(R.string.vybrat_vse),
                style = ApparatusStyle,
                color = colors.secondary,
                modifier = Modifier.clickable { onToggleAll() }
            )
        }

        if (state.isKindsLoading && state.kinds.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.secondary)
            }
        }

        state.kinds.forEach { info ->
            KindRow(
                info = info,
                selected = info.kind in state.selectedKinds,
                onToggle = { onToggleKind(info.kind) }
            )
        }

        Eyebrow(stringResource(R.string.dlina_sessii))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20).forEach { n ->
                ChipButton(
                    text = stringResource(R.string.voprosov_n, n),
                    selected = state.count == n
                ) { onSetCount(n) }
            }
        }

        val canStart = state.selectedKinds.isNotEmpty() && state.wordsAvailable > 0 &&
            !state.isExerciseLoading
        Button(
            onClick = onStart,
            enabled = canStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.isExerciseLoading) {
                CircularProgressIndicator(
                    color = colors.onAccent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(stringResource(R.string.nachat), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        val message = state.error ?: state.notice ?: setupHint(state)
        message?.let {
            Text(
                text = it,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = if (state.error != null) Error else TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** What to say when the selection cannot produce a session, phrased as the next step. */
@Composable
private fun setupHint(state: TasksState): String? = when {
    state.isKindsLoading -> null
    state.wordsAvailable == 0 -> stringResource(R.string.net_slov_dlya_uprazhneniy)
    state.availableKinds.isEmpty() ->
        stringResource(R.string.statya_esche_ne_gotova_otkroyte_vkladku_istochniki)
    else -> null
}

@Composable
private fun KindRow(
    info: ExerciseKindInfoDto,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val colors = WaveTheme.colors
    val enabled = info.available > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onToggle() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected && enabled) Icons.Default.CheckCircle
                          else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = when {
                !enabled -> colors.textMuted
                selected -> colors.secondary
                else -> colors.textMuted
            },
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = info.titleRu,
                    fontFamily = Comfortaa,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) TextPrimary else colors.textMuted
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${info.available}",
                    style = ApparatusStyle,
                    color = colors.textMuted
                )
            }
            Text(
                text = info.descriptionRu,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = if (enabled) TextSecondary else colors.textMuted
            )
        }
    }
}

// ── Exercise session ─────────────────────────────────────────────────────────

/**
 * Renders any exercise the server can send.
 *
 * There is deliberately no branch on [ExerciseKind] beyond [ExerciseFormat]: a kind added on
 * the backend shows up here without an app release, and — more to the point — the phone and the
 * browser cannot drift into presenting the same question differently.
 */
@Composable
private fun ExerciseSession(
    state: TasksState,
    onChoose: (Int) -> Unit,
    onTypedChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    val colors = WaveTheme.colors
    val exercise = state.currentExercise

    if (exercise == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colors.secondary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModeHeader(
            title = stringResource(
                R.string.vopros_n_iz_m,
                state.exerciseIndex + 1,
                state.exercises.size
            ),
            onExit = onExit
        )

        LinearProgressIndicator(
            progress = (state.exerciseIndex.toFloat() / state.exercises.size.coerceAtLeast(1)),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = PrimaryCyan,
            trackColor = BackgroundLight
        )

        // Instruction + provenance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = exercise.promptRu,
                style = ApparatusStyle,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            sourceLabel(exercise.source)?.let {
                Text(text = it, style = ApparatusStyle, color = colors.textMuted)
            }
        }

        QuestionText(exercise)

        when (exercise.format) {
            ExerciseFormat.CHOICE -> OptionList(
                exercise = exercise,
                answered = state.answered,
                given = state.given,
                onChoose = onChoose
            )

            ExerciseFormat.INPUT -> {
                if (!state.answered) {
                    OutlinedTextField(
                        value = state.typed,
                        onValueChange = onTypedChange,
                        label = { Text(stringResource(R.string.vash_otvet)) },
                        placeholder = exercise.hintRu?.let {
                            { Text(stringResource(R.string.podskazka_s, it)) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            focusedLabelColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onSubmit,
                            enabled = state.typed.isNotBlank(),
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(stringResource(R.string.proverit), fontSize = 15.sp) }
                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(stringResource(R.string.propustit), fontSize = 15.sp, color = TextSecondary) }
                    }
                } else {
                    Text(
                        text = state.given.ifBlank { "—" },
                        fontFamily = JetBrainsMono,
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.answered,
            enter = fadeIn(tween(250)) + expandVertically()
        ) {
            VerdictBlock(state = state, exercise = exercise, onNext = onNext)
        }

        Text(
            text = stringResource(
                R.string.verno_pochti_mimo,
                state.correctCount, state.almostCount, state.wrongCount
            ),
            style = ApparatusStyle,
            color = TextTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun QuestionText(exercise: ExerciseDto) {
    if (exercise.questionIsSentence) {
        // The gap is the point of the question, so it is the one thing set apart.
        val parts = exercise.question.split(ExerciseDto.BLANK)
        Text(
            text = buildAnnotatedString {
                parts.forEachIndexed { i, part ->
                    append(part)
                    if (i < parts.size - 1) {
                        withStyle(SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold)) {
                            append(ExerciseDto.BLANK)
                        }
                    }
                }
            },
            fontSize = 18.sp,
            lineHeight = 27.sp,
            fontStyle = FontStyle.Italic,
            color = TextPrimary
        )
    } else {
        Text(
            text = exercise.question,
            fontFamily = Comfortaa,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            letterSpacing = (-1).sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun OptionList(
    exercise: ExerciseDto,
    answered: Boolean,
    given: String,
    onChoose: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        exercise.options.forEachIndexed { idx, option ->
            val isCorrect = idx == exercise.correctIndex
            val isChosen = answered && option == given
            val background = when {
                !answered -> BackgroundSecondary
                isCorrect -> Success.copy(alpha = 0.18f)
                isChosen -> Error.copy(alpha = 0.18f)
                else -> BackgroundSecondary
            }
            val outline = when {
                answered && isCorrect -> Success
                answered && isChosen -> Error
                else -> WaveTheme.colors.border
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(background)
                    .border(1.dp, outline, RoundedCornerShape(12.dp))
                    .clickable(enabled = !answered) { onChoose(idx) }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${('A' + idx)}.",
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        color = when {
                            answered && isCorrect -> Success
                            answered && isChosen -> Error
                            else -> TextTertiary
                        }
                    )
                    Text(
                        text = option,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        lineHeight = 21.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (answered && isCorrect) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Success, modifier = Modifier.size(18.dp))
                    } else if (answered && isChosen) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VerdictBlock(state: TasksState, exercise: ExerciseDto, onNext: () -> Unit) {
    val accent = when (state.verdict) {
        ExerciseVerdict.CORRECT -> Success
        ExerciseVerdict.ALMOST -> Warning
        else -> Error
    }
    val label = when (state.verdict) {
        ExerciseVerdict.CORRECT -> stringResource(R.string.pravilno)
        ExerciseVerdict.ALMOST -> stringResource(R.string.pochti)
        else -> stringResource(R.string.nepravilno)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (state.verdict == ExerciseVerdict.WRONG) Icons.Default.Cancel
                              else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = accent
            )
        }

        if (state.verdict != ExerciseVerdict.CORRECT) {
            Text(
                text = buildAnnotatedString {
                    append(
                        if (state.verdict == ExerciseVerdict.ALMOST)
                            stringResource(R.string.pravilno_pishetsya)
                        else stringResource(R.string.pravilnyy_otvet)
                    )
                    append(" — ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(exercise.answer) }
                },
                fontSize = 15.sp,
                color = TextSecondary
            )
        }

        // The explanation is where the learning actually happens, so it is not optional trim.
        exercise.explanationRu?.let {
            Text(text = it, fontSize = 14.sp, lineHeight = 21.sp, color = TextSecondary)
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.dalshe), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun sourceLabel(source: ExerciseSource): String? = when (source) {
    ExerciseSource.CORPUS -> stringResource(R.string.iz_slovarya)
    ExerciseSource.AI -> stringResource(R.string.ii)
    ExerciseSource.CARD -> null
}

// ── Result ───────────────────────────────────────────────────────────────────

@Composable
private fun ExerciseResultView(
    state: TasksState,
    mistakes: List<ExerciseResult>,
    onAgain: () -> Unit,
    onSetup: () -> Unit
) {
    val colors = WaveTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Eyebrow(stringResource(R.string.itog))
        Text(
            text = "${state.accuracy}%",
            fontFamily = Comfortaa,
            fontSize = 60.sp,
            lineHeight = 64.sp,
            letterSpacing = (-2).sp,
            fontWeight = FontWeight.Bold,
            color = colors.secondary
        )
        Text(
            text = stringResource(
                R.string.verno_pochti_mimo,
                state.correctCount, state.almostCount, state.wrongCount
            ),
            style = ApparatusStyle,
            color = TextSecondary
        )

        // What went wrong is the only part of a finished session worth reading.
        if (mistakes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Eyebrow(stringResource(R.string.stoit_povtorit), modifier = Modifier.fillMaxWidth())
            mistakes.forEach { result ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = result.exercise.word,
                        fontFamily = Comfortaa,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${stringResource(R.string.vy_otvetili)}: ${result.given.ifBlank { "—" }} · " +
                            "${stringResource(R.string.nuzhno)}: ${result.exercise.answer}",
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                RuleFade()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onAgain,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
            shape = RoundedCornerShape(12.dp)
        ) { Text(stringResource(R.string.esche_raz), fontSize = 16.sp) }
        OutlinedButton(
            onClick = onSetup,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text(stringResource(R.string.k_nastroykam), fontSize = 16.sp, color = TextSecondary) }
    }
}

// ── Editing a card ───────────────────────────────────────────────────────────

/**
 * The editor sits with the card rather than in a list: a wrong translation is noticed while
 * looking at it, and a correction that has to wait for another screen is one that never gets
 * made.
 */
@Composable
private fun EditCardDialog(
    card: FlashcardEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var word by remember(card.id) { mutableStateOf(card.word) }
    var translation by remember(card.id) { mutableStateOf(card.translation.orEmpty()) }
    var definition by remember(card.id) { mutableStateOf(card.definition) }
    var example by remember(card.id) { mutableStateOf(card.example.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSecondary,
        title = {
            Text(
                text = stringResource(R.string.chto_pokazyvaet_kartochka),
                fontFamily = Comfortaa,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditField(stringResource(R.string.pole_slovo), word, singleLine = true) { word = it }
                EditField(stringResource(R.string.pole_perevod), translation, singleLine = true) { translation = it }
                EditField(stringResource(R.string.pole_opredelenie), definition) { definition = it }
                EditField(stringResource(R.string.pole_primer), example) { example = it }
                Text(
                    text = stringResource(R.string.posle_pravki_kartochka_perestaet_obnovlyatsya),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = TextTertiary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(word, translation, definition, example) },
                enabled = word.isNotBlank()
            ) { Text(stringResource(R.string.sohranit), color = PrimaryCyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.otmena), color = TextSecondary)
            }
        }
    )
}

@Composable
private fun EditField(
    label: String,
    value: String,
    singleLine: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlue,
            focusedLabelColor = PrimaryBlue
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

// ── Flashcard session ────────────────────────────────────────────────────────

@Composable
private fun FlashcardSession(
    flashcards: List<FlashcardEntity>,
    currentIndex: Int,
    onCorrect: () -> Unit,
    onIncorrect: () -> Unit,
    onEdit: () -> Unit,
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
                    contentDescription = stringResource(R.string.vyhod),
                    tint = TextPrimary
                )
            }
            Text(
                text = "${currentIndex + 1} / ${flashcards.size}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.izmenit_kartochku),
                        tint = TextSecondary
                    )
                }
                // Toggle side order button
                IconButton(onClick = { wordFirst = !wordFirst; isFlipped = false }) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = if (wordFirst) stringResource(R.string.nachat_s_opredeleniya) else stringResource(R.string.nachat_so_slova),
                        tint = TextSecondary
                    )
                }
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
                            stringResource(R.string.ne_znayu),
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
                            stringResource(R.string.znayu),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Hint to flip
        if (!isFlipped) {
            FlipHint(text = stringResource(R.string.nazhmite_na_kartochku_chtoby_uvidet_otvet), modifier = Modifier.fillMaxWidth())
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
                        FlipHint(text = stringResource(R.string.nazhmite_chtoby_uvidet_opredelenie))
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
                        FlipHint(text = stringResource(R.string.nazhmite_chtoby_uvidet_slovo))
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
                                    Text(stringResource(R.string.primer), fontSize = 11.sp, color = TextTertiary)
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
                Text(stringResource(R.string.pokazat_perevod), fontSize = 13.sp, color = PrimaryCyan)
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
            text = stringResource(R.string.sessiya_zavershena),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.otlichnaya_rabota_prodolzhayte_uchitsya_kazhdyy_den),
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
                stringResource(R.string.zavershit),
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
