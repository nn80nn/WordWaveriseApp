package com.wordwaverise.wordwaveriseapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.R
import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.ui.theme.*

// ── Куда положить сохраняемое значение ────────────────────────────────────────

/**
 * Флажки, а не выбор одного: слово честно принадлежит и уроку, и теме к экзамену, и
 * заставлять человека решать, какая из двух папок будет неправильной, незачем.
 *
 * ⚠️ Папок класса в списке нет — писать в чужой словарь нельзя, сервер такой запрос
 * отклоняет, и предлагать заведомо нерабочее хуже, чем не предлагать вовсе. Отбор делает
 * ViewModel (`ownFolders`), здесь остаётся только показ.
 */
@Composable
fun SaveToFolderDialog(
    word: String,
    summary: String?,
    folders: List<CategoryEntity>,
    chosen: List<Long>,
    saving: Boolean,
    onToggle: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var newFolder by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.sokhranit_slovo, word),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                // Сохраняется значение, а не написание, — диалог называет именно его.
                summary?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                folders.forEach { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggle(folder.id) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = folder.id in chosen,
                            onCheckedChange = { onToggle(folder.id) }
                        )
                        Text(
                            text = folder.name,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = if (chosen.isEmpty()) stringResource(R.string.bez_papki_poyasnenie)
                           else stringResource(R.string.slovo_budet_v_papkakh, chosen.size),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 10.dp, start = 4.dp)
                )

                // Новая папка — строкой, а не отдельным экраном: «положить в папку, которой
                // ещё нет» это одно намерение, и разрывать его на два диалога значит терять
                // слово по дороге.
                HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newFolder,
                        onValueChange = { newFolder = it },
                        placeholder = { Text(stringResource(R.string.novaya_kategoriya), color = TextPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    TextButton(
                        onClick = {
                            onCreate(newFolder)
                            newFolder = ""
                        },
                        enabled = newFolder.isNotBlank()
                    ) {
                        Text(stringResource(R.string.sozdat))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !saving) {
                Text(stringResource(R.string.sohranit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.otmena)) }
        },
        containerColor = BackgroundSecondary
    )
}

