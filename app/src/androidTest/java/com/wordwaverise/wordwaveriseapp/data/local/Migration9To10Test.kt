package com.wordwaverise.wordwaveriseapp.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Миграция 9 → 10: слово перестаёт быть строкой и становится записью.
 *
 * Первичным ключом было само написание, поэтому таблицу приходится пересоздавать — ALTER'ом
 * SQLite ключ не меняет. Пересоздание и есть риск: у человека уже разложен словарь, и
 * обновление приложения не тот момент, когда папка слова может «просто пропасть».
 *
 * Проверяется ровно это: слова доезжают, папка переезжает в список из одного элемента, пометки
 * группы сохраняются, а два значения одного слова — то, ради чего всё и делалось — теперь
 * ложатся рядом, а не затирают друг друга.
 */
@RunWith(AndroidJUnit4::class)
class Migration9To10Test {

    private companion object {
        const val DB_NAME = "migration-9-10-test-db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun wordsKeepTheirFolderAndTheirGroupMarks() {
        helper.createDatabase(DB_NAME, 9).use { db ->
            db.execSQL(
                "INSERT INTO categories (id, serverId, name, color, createdAt, groupServerId, groupName, readOnly) " +
                    "VALUES (3, 42, 'Unit 5', '#00A6A6', 100, 8, '9Б', 1)"
            )
            db.execSQL(
                "INSERT INTO saved_words " +
                    "(word, savedAt, serverId, isSynced, categoryId, senseId, groupServerId, readOnly) " +
                    "VALUES ('resolve', 500, 7, 1, 3, 'v1', 8, 1)"
            )
            db.execSQL(
                "INSERT INTO saved_words " +
                    "(word, savedAt, serverId, isSynced, categoryId, senseId, groupServerId, readOnly) " +
                    "VALUES ('waver', 600, 9, 1, NULL, NULL, NULL, 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 10, true, MIGRATION_9_10)

        db.query("SELECT * FROM saved_words WHERE word = 'resolve'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            // Папка переезжает списком из одного элемента, а не теряется.
            assertEquals("3", cursor.getString(cursor.getColumnIndexOrThrow("categoryIds")))
            assertEquals("v1", cursor.getString(cursor.getColumnIndexOrThrow("senseId")))
            assertEquals(7, cursor.getInt(cursor.getColumnIndexOrThrow("serverId")))
            assertEquals(8, cursor.getInt(cursor.getColumnIndexOrThrow("groupServerId")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("readOnly")))
            // Ключ теперь суррогатный, и он обязан быть присвоен: иначе строки неразличимы.
            assertTrue(cursor.getLong(cursor.getColumnIndexOrThrow("id")) > 0)
        }

        db.query("SELECT categoryIds FROM saved_words WHERE word = 'waver'").use { cursor ->
            cursor.moveToFirst()
            // «Без папки» — это пустой список, а не строка "null".
            assertEquals("", cursor.getString(0))
        }
    }

    @Test
    fun twoSensesOfOneWordNoLongerOverwriteEachOther() {
        helper.createDatabase(DB_NAME, 9).use { db ->
            // ⚠️ readOnly перечисляется явно: в схеме 9 это NOT NULL без SQL-дефолта —
            // `= false` у поля сущности живёт в Kotlin, а не в DDL, которое строит helper.
            db.execSQL(
                "INSERT INTO saved_words (word, savedAt, serverId, isSynced, categoryId, senseId, readOnly) " +
                    "VALUES ('resolve', 500, 7, 1, NULL, 'v1', 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 10, true, MIGRATION_9_10)

        // До миграции это была та же строка, и вторая запись затирала первую вместе с папкой.
        db.execSQL(
            "INSERT INTO saved_words (word, savedAt, serverId, isSynced, categoryIds, senseId, readOnly) " +
                "VALUES ('resolve', 600, 11, 1, '', 'v2', 0)"
        )

        db.query("SELECT id, senseId FROM saved_words WHERE word = 'resolve' ORDER BY senseId").use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToFirst()
            val first = cursor.getLong(0)
            cursor.moveToNext()
            assertNotEquals(first, cursor.getLong(0))
        }
    }
}
