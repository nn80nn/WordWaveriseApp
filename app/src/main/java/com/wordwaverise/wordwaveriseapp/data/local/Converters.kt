package com.wordwaverise.wordwaveriseapp.data.local

import androidx.room.TypeConverter

/**
 * Списки id в одной колонке.
 *
 * Отдельная таблица-связка была бы честнее по форме, но здесь она ничего не даёт: слова
 * читаются целиком одним Flow и фильтруются в памяти — экран сохранённых слов и так держит
 * весь список. Таблица добавила бы `@Relation`, второй DAO и ещё одну миграцию ради запроса,
 * которого нет.
 */
class Converters {

    @TypeConverter
    fun fromLongList(value: List<Long>?): String =
        value.orEmpty().joinToString(",")

    @TypeConverter
    fun toLongList(value: String?): List<Long> =
        value.orEmpty()
            .split(',')
            .mapNotNull { it.trim().toLongOrNull() }
}
