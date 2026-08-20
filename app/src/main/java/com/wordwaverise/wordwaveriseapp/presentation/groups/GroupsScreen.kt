package com.wordwaverise.wordwaveriseapp.presentation.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.GroupDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.StudentAssignmentDto
import com.wordwaverise.wordwaveriseapp.ui.theme.*

/**
 * Классы и то, что в них задали.
 *
 * Открывается из Профиля, а не пятой вкладкой внизу: четыре вкладки — общий каркас с сайтом, и
 * пятая сжала бы подписи ради экрана, который открывают не каждый день.
 */
@Composable
fun GroupsScreen(
    onBack: () -> Unit,
    onPractise: (assignmentId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = WaveTheme.colors
    var code by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null) {
            snackbar.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .waveSurface()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = TextSecondary)
                }
                Text(
                    text = "Группы",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // ── Задания идут первыми: только у них есть срок ──────────────
            val pending = state.assignments.filter { !it.completed }
            if (pending.isNotEmpty()) {
                Eyebrow("Задания")
                pending.forEach { item ->
                    AssignmentCard(item = item, onPractise = { onPractise(item.assignment.id) })
                }
            }

            if (state.owned.isNotEmpty()) {
                Eyebrow("Я преподаю")
                state.owned.forEach { GroupCard(group = it, onLeave = null) }
                Text(
                    text = "Управление группой — на сайте wordwaverise.com",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }

            if (state.joined.isNotEmpty()) {
                Eyebrow("Я учусь")
                state.joined.forEach { group ->
                    GroupCard(group = group, onLeave = { viewModel.leave(group.id) })
                }
            }

            if (!state.hasAny && !state.isLoading) {
                Text(
                    text = "Пока никаких групп. Введите код, который дал преподаватель.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Eyebrow("Вступить по коду")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.trim().lowercase().take(8) },
                    placeholder = { Text("abcd2345", fontSize = 14.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.joinByCode(code); code = "" },
                    enabled = code.isNotBlank() && !state.isJoining,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Вступить", fontSize = 14.sp)
                }
            }
            Text(
                text = "Папки преподавателя появятся в ваших словах только для чтения",
                fontSize = 12.sp,
                color = TextTertiary
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GroupCard(group: GroupDto, onLeave: (() -> Unit)?) {
    val colors = WaveTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Group,
                    contentDescription = null,
                    tint = colors.brass,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = group.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (onLeave != null) {
                    TextButton(onClick = onLeave) {
                        Text("Выйти", fontSize = 12.sp, color = TextTertiary)
                    }
                }
            }
            val details = buildList {
                group.teacherName?.let { add(it) }
                add("учеников ${group.memberCount}")
                add("папок ${group.folderCount}")
            }
            Text(
                text = details.joinToString(" · "),
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun AssignmentCard(item: StudentAssignmentDto, onPractise: () -> Unit) {
    val colors = WaveTheme.colors
    val a = item.assignment
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onPractise() },
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = a.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            val meta = buildList {
                add(a.groupName)
                a.categoryName?.let { add(it) }
                // Просроченное подписано словом, а не только цветом: красная полоска без
                // подписи одинаково читается как «сломалось».
                if (item.overdue) add("просрочено")
            }
            Text(
                text = meta.joinToString(" · "),
                fontSize = 12.sp,
                color = if (item.overdue) Error else TextTertiary,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.percent / 100f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (item.overdue) Error else PrimaryCyan)
                )
            }

            // Числами, а не только процентом: «7 из 10» можно проверить, «70%» приходится
            // принимать на веру.
            val goals = buildList {
                a.exerciseTarget?.let { add("упражнений ${item.exercisesDone} из $it") }
                a.reviewTarget?.let { add("карточек ${item.reviewsDone} из $it") }
            }
            Text(
                text = goals.joinToString(" · "),
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
