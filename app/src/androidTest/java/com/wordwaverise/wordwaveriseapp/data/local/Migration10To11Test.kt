package com.wordwaverise.wordwaveriseapp.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Миграция 10 → 11: у сохранённого слова появляется перевод.
 *
 * Колонка добавляется пустой — заполнить её на телефоне неоткуда, перевод приходит с сервера.
 * Пустая, а не «неизвестно»: карточка просто не рисует строку, пока значения нет, и слово,
 * сохранённое до обновления, не выглядит сломанным до первой синхронизации.
 */
@RunWith(AndroidJUnit4::class)
class Migration10To11Test {

    private companion object {
        const val DB_NAME = "migration-10-11-test-db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun wordsSurviveAndArriveWithoutATranslation() {
        helper.createDatabase(DB_NAME, 10).use { db ->
            db.execSQL(
                "INSERT INTO saved_words " +
                    "(word, savedAt, serverId, isSynced, categoryIds, senseId, groupServerId, readOnly) " +
                    "VALUES ('resolve', 500, 7, 1, '3', 'v1', 8, 1)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 11, true, MIGRATION_10_11)

        db.query("SELECT * FROM saved_words WHERE word = 'resolve'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("translation")))
            // Всё остальное обязано доехать нетронутым: колонка добавляется, а не таблица
            // пересоздаётся, но проверить дешевле, чем однажды обнаружить обратное.
            assertEquals("3", cursor.getString(cursor.getColumnIndexOrThrow("categoryIds")))
            assertEquals("v1", cursor.getString(cursor.getColumnIndexOrThrow("senseId")))
            assertEquals(7, cursor.getInt(cursor.getColumnIndexOrThrow("serverId")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("readOnly")))
        }
    }

    @Test
    fun twoSensesOfOneWordCanNowBeToldApart() {
        helper.createDatabase(DB_NAME, 10).use { db ->
            db.execSQL(
                "INSERT INTO saved_words (word, savedAt, serverId, isSynced, categoryIds, senseId, readOnly) " +
                    "VALUES ('resolve', 500, 7, 1, '', 'n1', 0)"
            )
            db.execSQL(
                "INSERT INTO saved_words (word, savedAt, serverId, isSynced, categoryIds, senseId, readOnly) " +
                    "VALUES ('resolve', 600, 8, 1, '', 'n2', 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 11, true, MIGRATION_10_11)

        db.execSQL("UPDATE saved_words SET translation = 'решимость' WHERE senseId = 'n1'")
        db.execSQL("UPDATE saved_words SET translation = 'резолюция' WHERE senseId = 'n2'")

        db.query("SELECT translation FROM saved_words WHERE word = 'resolve' ORDER BY senseId").use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToFirst()
            assertEquals("решимость", cursor.getString(0))
            cursor.moveToNext()
            assertEquals("резолюция", cursor.getString(0))
        }
    }
}
