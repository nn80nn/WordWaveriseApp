package com.wordwaverise.wordwaveriseapp.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "👤", fontSize = 72.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // User Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
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
        Text(
            text = "Статистика",
            fontSize = 14.sp,
            color = TextTertiary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

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

        // About section
        Text(
            text = "О приложении",
            fontSize = 14.sp,
            color = TextTertiary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
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
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = iconTint
            )
        }
    }
}
