package com.wordwaverise.wordwaveriseapp.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Миграция 13 → 14: офлайн-чтение (только Android) — три новых таблицы, ни одна существующая
 * не трогается.
 *
 * Проверяется то единственное, что может сломаться в чистом добавлении: сама миграция
 * проходит валидацию Room (совпадает со схемой из entity-классов), и три новые таблицы готовы
 * принять строку сразу после неё — существующие данные (например, `categories` из предыдущей
 * миграции) остаются нетронутыми просто потому, что миграция их не касается.
 */
@RunWith(AndroidJUnit4::class)
class Migration13To14Test {

    private companion object {
        const val DB_NAME = "migration-13-14-test-db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun offlineTablesAcceptRowsAfterMigration() {
        helper.createDatabase(DB_NAME, 13).use { db ->
            db.execSQL(
                "INSERT INTO categories (serverId, name, color, createdAt, readOnly) " +
                    "VALUES (1, 'Мои слова', NULL, 100, 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 14, true, MIGRATION_13_14)

        db.execSQL(
            "INSERT INTO offline_books (bookId, detailPayload, blockCount, status, totalTokens, processedTokens, updatedAt) " +
                "VALUES (1, '{}', 10, 'READY', 50, 50, 1000)"
        )
        db.execSQL("INSERT INTO offline_blocks (bookId, ordinal, payload) VALUES (1, 0, '{}')")
        db.execSQL(
            "INSERT INTO offline_hints (bookId, blockOrdinal, sentenceIndex, tokenIndex, payload) " +
                "VALUES (1, 0, 0, 0, '{}')"
        )

        db.query("SELECT * FROM offline_books").use { assertEquals(1, it.count) }
        db.query("SELECT * FROM offline_blocks").use { assertEquals(1, it.count) }
        db.query("SELECT * FROM offline_hints").use { assertEquals(1, it.count) }

        // Ничего из предыдущей схемы не пострадало — миграция ничего в `categories` не трогает.
        db.query("SELECT * FROM categories").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Мои слова", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
    }
}
