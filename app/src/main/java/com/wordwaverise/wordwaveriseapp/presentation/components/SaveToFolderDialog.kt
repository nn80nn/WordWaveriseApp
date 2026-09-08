package com.wordwaverise.wordwaveriseapp.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 *
 * ⚠️ «Без папки» — **строка списка, а не пустой список**. Отсутствие галочек это и есть
 * «никуда», но прочитать это можно было только по подписи внизу: человек, которому папка не
 * нужна, стоял перед списком, где нечего выбрать, и искал, чего от него хотят. Строка
 * снимает все отметки и сама показывает себя выбранной — то есть отвечает на вопрос, а не
 * заставляет обойти его.
 */
@Composable
fun SaveToFolderDialog(
    word: String,
    summary: String?,
    folders: List<CategoryEntity>,
    chosen: List<Long>,
    saving: Boolean,
    onToggle: (Long) -> Unit,
    onSelectNone: () -> Unit,
    onCreate: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var newFolder by remember { mutableStateOf("") }
    val nowhere = chosen.isEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(
                    text = stringResource(R.string.sokhranit_slovo, word),
                    fontFamily = Comfortaa,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                // Сохраняется значение, а не написание, — диалог называет именно его.
                summary?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FolderRow(
                    label = stringResource(R.string.bez_kategorii),
                    hint = stringResource(R.string.bez_papki_poyasnenie),
                    selected = nowhere,
                    nested = false,
                    group = false,
                    onClick = onSelectNone
                )

                if (folders.isNotEmpty()) {
                    HorizontalDivider(
                        color = BorderLight,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                folders.forEach { folder ->
                    FolderRow(
                        label = folder.name,
                        hint = null,
                        selected = folder.id in chosen,
                        // Вложенная папка отступом и своим значком: без родителя «Урок 5»
                        // одного модуля неотличим от «Урока 5» соседнего.
                        nested = folder.parentServerId != null,
                    book = folder.bookServerId != null,
                        group = folders.any { it.parentServerId != null && it.parentServerId == folder.serverId },
                        onClick = { onToggle(folder.id) }
                    )
                }

                // Новая папка — строкой, а не отдельным экраном: «положить в папку, которой
                // ещё нет» это одно намерение, и разрывать его на два диалога значит терять
                // слово по дороге.
                HorizontalDivider(
                    color = BorderLight,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newFolder,
                        onValueChange = { newFolder = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.novaya_kategoriya),
                                color = TextPlaceholder,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCyan,
                            unfocusedBorderColor = BorderLight
                        )
                    )
                    TextButton(
                        onClick = {
                            onCreate(newFolder)
                            newFolder = ""
                        },
                        enabled = newFolder.isNotBlank()
                    ) {
                        Text(
                            stringResource(R.string.sozdat),
                            color = if (newFolder.isNotBlank()) PrimaryCyan else TextTertiary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !saving) {
                Text(
                    stringResource(R.string.sohranit),
                    color = PrimaryCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.otmena), color = TextSecondary)
            }
        }
    )
}

/**
 * Строка выбора: вся она и есть кнопка.
 *
 * Системный `Checkbox` здесь был чужим — квадрат материаловского синего посреди палитры, в
 * которой ни одного такого цвета нет, и нажимать приходилось точно в него. Отметка теперь
 * рисуется своя, а нажатие принимает вся строка: попасть в неё легче, а выбранная папка
 * читается фоном и рамкой, а не одним значком в двадцать точек.
 */
@Composable
private fun FolderRow(
    label: String,
    hint: String?,
    selected: Boolean,
    nested: Boolean,
    group: Boolean,
    onClick: () -> Unit,
    /** Папка книги: узнаётся значком, а не названием — переименовать можно и книгу, и папку. */
    book: Boolean = false
) {
    val background by animateColorAsState(
        targetValue = if (selected) WaveTheme.colors.secondarySoft.copy(alpha = 0.14f)
                      else Color.Transparent,
        label = "folder-row-bg"
    )
    val outline by animateColorAsState(
        targetValue = if (selected) WaveTheme.colors.secondary else WaveTheme.colors.border,
        label = "folder-row-border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (nested) 18.dp else 0.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        SelectionMark(selected)

        Icon(
            imageVector = when {
                book -> Icons.Outlined.AutoStories
                group -> Icons.Default.FolderOpen
                else -> Icons.Default.Folder
            },
            contentDescription = null,
            tint = if (selected) PrimaryCyan else TextTertiary,
            modifier = Modifier.size(17.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            hint?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = TextTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Отметка выбора: скруглённый квадрат нашего бирюзового, а не системный флажок. */
@Composable
private fun SelectionMark(selected: Boolean) {
    val fill by animateColorAsState(
        targetValue = if (selected) WaveTheme.colors.secondary else Color.Transparent,
        label = "mark-fill"
    )
    val edge by animateColorAsState(
        targetValue = if (selected) WaveTheme.colors.secondary else WaveTheme.colors.borderStrong,
        label = "mark-edge"
    )

    Box(
        modifier = Modifier
            .size(21.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(fill)
            .border(1.5.dp, edge, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = WaveTheme.colors.onAccent,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
