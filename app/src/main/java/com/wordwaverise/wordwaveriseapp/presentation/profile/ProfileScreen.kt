package com.wordwaverise.wordwaveriseapp.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.BuildConfig
import com.wordwaverise.wordwaveriseapp.ui.theme.*

@Composable
fun ProfileScreen(
    userEmail: String,
    userLogin: String?,
    state: ProfileState,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onLogout: () -> Unit,
    deletionScheduledFor: String? = null,
    deletionLoading: Boolean = false,
    deletionError: String? = null,
    onRequestDeletion: (String) -> Unit = {},
    onCancelDeletion: () -> Unit = {},
    onClearDeletionError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .waveSurface()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = PrimaryCyan,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
            border = BorderStroke(1.dp, WaveTheme.colors.border),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                if (userLogin != null) {
                    Text(
                        text = "Логин",
                        fontSize = 11.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "@$userLogin",
                        fontSize = 18.sp,
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = "Email",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = userEmail,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Statistics
        Eyebrow("Статистика", modifier = Modifier.padding(bottom = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Book,
                iconTint = PrimaryCyan,
                value = state.savedWordsCount.toString(),
                label = "Сохранено слов"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Style,
                iconTint = PrimaryBlue,
                value = state.totalFlashcards.toString(),
                label = "Карточек всего"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        StatCardWide(
            icon = Icons.Default.Timer,
            iconTint = if (state.dueFlashcards > 0) Warning else Success,
            value = state.dueFlashcards.toString(),
            label = "К повторению сегодня"
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Appearance
        Eyebrow("Оформление", modifier = Modifier.padding(bottom = 12.dp))

        ThemePicker(selected = themeMode, onSelect = onThemeModeChange)

        Spacer(modifier = Modifier.height(20.dp))

        // About section
        Eyebrow("О приложении", modifier = Modifier.padding(bottom = 12.dp))

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
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Версия",
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                }
                Text(
                    text = BuildConfig.VERSION_NAME,
                    fontSize = 15.sp,
                    color = TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Danger zone
        if (deletionScheduledFor != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
                border = BorderStroke(1.dp, WaveTheme.colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(
                        text = "Ваш аккаунт будет удалён: $deletionScheduledFor",
                        fontSize = 13.sp,
                        color = Error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onCancelDeletion,
                        enabled = !deletionLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (deletionLoading) "Отмена..." else "Отменить удаление")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Удалить аккаунт", color = Error, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Logout Button
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Выйти из аккаунта",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deletePassword = ""
                onClearDeletionError()
            },
            title = { Text("Удалить аккаунт") },
            text = {
                Column {
                    Text(
                        "Аккаунт и все данные будут удалены безвозвратно через 30 дней. " +
                            "Введите пароль для подтверждения.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        enabled = !deletionLoading
                    )
                    if (deletionError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(deletionError, color = Error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onRequestDeletion(deletePassword) },
                    enabled = !deletionLoading && deletePassword.isNotEmpty()
                ) {
                    Text("Удалить", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    deletePassword = ""
                    onClearDeletionError()
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    androidx.compose.runtime.LaunchedEffect(deletionScheduledFor) {
        if (deletionScheduledFor != null) {
            showDeleteDialog = false
            deletePassword = ""
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        border = BorderStroke(1.dp, WaveTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontFamily = Comfortaa,
                letterSpacing = (-1.0).sp,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StatCardWide(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String
) {
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
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = label,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            }
            Text(
                text = value,
                fontFamily = Comfortaa,
                letterSpacing = (-0.8).sp,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = iconTint
            )
        }
    }
}

// ── Appearance ────────────────────────────────────────────────────────────────

/**
 * Three tiles rather than a switch, because "Система" is a first-class choice
 * and a two-state toggle can't express it. Each tile shows the theme it selects
 * as a waterline — paper above, deep water below — so the choice is legible
 * before the label is read.
 */
@Composable
private fun ThemePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val colors = WaveTheme.colors
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
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemeOption(Modifier.weight(1f), ThemeMode.SYSTEM, "Система", selected, onSelect)
            ThemeOption(Modifier.weight(1f), ThemeMode.LIGHT, "Светлая", selected, onSelect)
            ThemeOption(Modifier.weight(1f), ThemeMode.DARK, "Тёмная", selected, onSelect)
        }
    }
}

@Composable
private fun ThemeOption(
    modifier: Modifier,
    mode: ThemeMode,
    label: String,
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val colors = WaveTheme.colors
    val isSelected = mode == selected
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.tag else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) colors.secondary else colors.border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect(mode) }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WaterlineSwatch(mode)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) colors.secondary else TextSecondary
        )
    }
}

/** Paper over water, divided by the crest of a wave. */
@Composable
private fun WaterlineSwatch(mode: ThemeMode) {
    val crest = WaveTheme.colors.secondary
    Canvas(modifier = Modifier.size(width = 46.dp, height = 30.dp)) {
        val r = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx(), 7.dp.toPx())
        val paper = LightWave.background
        val water = DarkWave.background

        fun wavePath(from: Float): Path = Path().apply {
            val y = size.height * 0.52f
            val amp = size.height * 0.10f
            moveTo(from, y)
            var x = from
            var up = true
            while (x < size.width) {
                val half = size.width / 2f
                val peak = if (up) y - amp else y + amp
                cubicTo(x + half * 0.35f, peak, x + half * 0.65f, peak, x + half, y)
                x += half
                up = !up
            }
            lineTo(size.width, size.height)
            lineTo(from, size.height)
            close()
        }

        clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, r)) }) {
            when (mode) {
                ThemeMode.LIGHT -> {
                    drawRect(paper)
                    drawPath(wavePath(0f), DarkWave.surfaceElevated.copy(alpha = 0.30f))
                }
                ThemeMode.DARK -> {
                    drawRect(water)
                    drawPath(wavePath(0f), DarkWave.surfaceElevated)
                }
                // Half and half: the system decides where the waterline falls.
                ThemeMode.SYSTEM -> {
                    drawRect(paper)
                    clipRect(left = size.width / 2f) { drawRect(water) }
                    drawPath(wavePath(0f), DarkWave.surfaceElevated.copy(alpha = 0.55f))
                }
            }
            // The crest itself is always lit — it is the logo's one bright line.
            val crestLine = Path().apply {
                val y = size.height * 0.52f
                val amp = size.height * 0.10f
                moveTo(0f, y)
                var x = 0f
                var up = true
                while (x < size.width) {
                    val half = size.width / 2f
                    val peak = if (up) y - amp else y + amp
                    cubicTo(x + half * 0.35f, peak, x + half * 0.65f, peak, x + half, y)
                    x += half
                    up = !up
                }
            }
            drawPath(crestLine, crest, style = Stroke(width = 1.6.dp.toPx()))
        }
    }
}
