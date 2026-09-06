package com.wordwaverise.wordwaveriseapp.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordwaverise.wordwaveriseapp.data.repository.ReportRepository
import com.wordwaverise.wordwaveriseapp.ui.theme.*
import com.wordwaverise.wordwaveriseapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Жалоба на сгенерированный текст ───────────────────────────────────────────

/** Что именно не так. Список закрыт и совпадает с серверным: иначе жалобы нельзя считать. */
private val REASONS = listOf(
    "wrong" to "Неверное значение, перевод или пример",
    "offensive" to "Оскорбительное или неуместное",
    "nonsense" to "Бессмыслица, обрывок текста",
    "other" to "Другое"
)

const val REPORT_KIND_ARTICLE = "article"
const val REPORT_KIND_EXERCISE = "exercise"

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: ReportRepository
) : ViewModel() {

    var sending by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun send(
        kind: String,
        word: String,
        senseId: String?,
        reason: String,
        comment: String,
        onSent: () -> Unit
    ) {
        viewModelScope.launch {
            sending = true
            error = null
            when (val result = repository.report(kind, word, senseId, reason, comment)) {
                is Resource.Success -> { sending = false; onSent() }
                is Resource.Error -> { sending = false; error = result.message }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearError() { error = null }
}

/**
 * Флажок «пожаловаться» и всё, что за ним стоит.
 *
 * Своя ViewModel внутри, а не параметр у вызывающего: жаловаться можно на любое значение любой
 * статьи и на любой вопрос упражнения, и протаскивать колбэк через каждый экран, каждую его
 * ViewModel и `MainActivity` значило бы поменять десяток сигнатур ради одной кнопки, которая
 * ничего из состояния экрана не читает.
 *
 * ⚠️ Открытый диалог — локальное `remember`, а не поле ViewModel: значений на экране много,
 * а модель одна на весь экран, и её поле открыло бы диалог сразу у всех.
 */
@Composable
fun ReportAction(
    kind: String,
    word: String,
    senseId: String? = null,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = !sent) { open = true }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Flag,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (sent) "Спасибо, жалоба отправлена" else "Пожаловаться",
            fontSize = 10.sp,
            color = TextTertiary
        )
    }

    if (open) {
        ReportDialog(
            kind = kind,
            word = word,
            senseId = senseId,
            onSent = { sent = true; open = false },
            onDismiss = { open = false }
        )
    }
}

@Composable
private fun ReportDialog(
    kind: String,
    word: String,
    senseId: String?,
    onSent: () -> Unit,
    onDismiss: () -> Unit
) {
    val viewModel: ReportViewModel = hiltViewModel()
    var reason by remember { mutableStateOf(REASONS.first().first) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Сообщить о проблеме",
                fontFamily = Comfortaa,
                fontSize = 18.sp,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Текст про «$word» написан ИИ по данным словарей. " +
                        "Расскажите, что с ним не так — мы перечитаем статью.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))

                REASONS.forEach { (key, label) ->
                    // Нажимается вся строка, а не только кружок: попадать пальцем в 20dp
                    // радиокнопку — работа, которой здесь взяться неоткуда.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { reason = key }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = reason == key,
                            onClick = { reason = key },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryCyan)
                        )
                        Text(text = label, fontSize = 14.sp, color = TextPrimary)
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Что именно не так (необязательно)", fontSize = 13.sp) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                viewModel.error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(text = message, color = Error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !viewModel.sending,
                onClick = {
                    viewModel.send(kind, word, senseId, reason, comment, onSent)
                }
            ) {
                Text(if (viewModel.sending) "Отправляем…" else "Отправить", color = PrimaryCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.clearError(); onDismiss() }) {
                Text("Отмена", color = TextTertiary)
            }
        }
    )
}
