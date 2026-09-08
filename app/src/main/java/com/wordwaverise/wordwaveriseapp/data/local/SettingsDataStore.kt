package com.wordwaverise.wordwaveriseapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wordwaverise.wordwaveriseapp.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

/**
 * Appearance preferences. Kept apart from [TokenDataStore] on purpose: logging
 * out clears that store wholesale, and the chosen theme should survive it.
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val LAST_SAVE_FOLDERS_KEY = stringPreferencesKey("last_save_folders")
        private val READER_PAGED_KEY = booleanPreferencesKey("reader_paged")
    }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    /**
     * Папки прошлого сохранения — то, что диалог предлагает отмеченными заранее.
     *
     * Урок собирают в несколько заходов, и «куда я клал предыдущие двадцать слов» — вопрос,
     * на который человеку отвечать не должен никто, кроме приложения. Хранятся **локальные**
     * id папок: диалог показывает строки Room, а не ответ сервера.
     */
    val lastSaveFolders: Flow<List<Long>> = context.settingsDataStore.data.map { prefs ->
        prefs[LAST_SAVE_FOLDERS_KEY]
            ?.split(',')
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?: emptyList()
    }

    suspend fun setLastSaveFolders(ids: List<Long>) {
        context.settingsDataStore.edit { it[LAST_SAVE_FOLDERS_KEY] = ids.joinToString(",") }
    }

    /**
     * Листать страницами или скроллом.
     *
     * Настройка на всё приложение, а не на книгу: человек читает одним способом, каким привык,
     * и переспрашивать про это на каждой новой книге — значит спрашивать про его привычку.
     * По умолчанию скролл: он ничего не обещает про то, где «страница», и потому не может
     * соврать на длинном абзаце.
     */
    val readerPaged: Flow<Boolean> = context.settingsDataStore.data.map { it[READER_PAGED_KEY] ?: false }

    suspend fun setReaderPaged(paged: Boolean) {
        context.settingsDataStore.edit { it[READER_PAGED_KEY] = paged }
    }

    /**
     * Точное место внутри абзаца — то, чего сервер не хранит и хранить не должен.
     *
     * Позиция на сервере это `ordinal` блока: она обязана значить одно и то же на телефоне и в
     * браузере, где строки ложатся иначе, и пиксель там был бы ложью. Но на **этом** устройстве
     * абзац разложен ровно так же, как минуту назад, поэтому вернуться можно в ту самую строку,
     * а не к началу абзаца.
     *
     * ⚠️ Смещение применяется, только если абзац совпал с тем, что помнит сервер. Читали на
     * планшете — сервер знает другое место, и локальный пиксель к нему не относится.
     */
    fun readerOffset(bookId: Int): Flow<Pair<Int, Int>?> =
        context.settingsDataStore.data.map { prefs ->
            prefs[readerOffsetKey(bookId)]
                ?.split(':')
                ?.takeIf { it.size == 2 }
                ?.let { parts ->
                    val ordinal = parts[0].toIntOrNull() ?: return@let null
                    val offset = parts[1].toIntOrNull() ?: return@let null
                    ordinal to offset
                }
        }

    suspend fun setReaderOffset(bookId: Int, ordinal: Int, offset: Int) {
        context.settingsDataStore.edit { it[readerOffsetKey(bookId)] = "$ordinal:$offset" }
    }

    private fun readerOffsetKey(bookId: Int) = stringPreferencesKey("reader_offset_$bookId")
}
