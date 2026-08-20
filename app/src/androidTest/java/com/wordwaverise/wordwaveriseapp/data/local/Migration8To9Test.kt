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
 * Миграция 8 → 9: папка может прийти на телефон от класса, а не быть своей.
 *
 * Ставки те же, что у любой миграции: она выполняется у всех разом при обновлении, и всё, что
 * человек собрал до неё — слова, папки, привязка слов к папкам — обязано пройти через неё
 * нетронутым. Отдельно проверяется, что старые строки получают `readOnly = 0`, а не NULL:
 * своя папка не должна вдруг стать нередактируемой.
 */
@RunWith(AndroidJUnit4::class)
class Migration8To9Test {

    private companion object {
        const val DB_NAME = "migration-8-9-test-db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun ownFoldersAndTheirWordsSurviveAndAreNotReadOnly() {
        helper.createDatabase(DB_NAME, 8).use { db ->
            db.execSQL(
                "INSERT INTO categories (id, serverId, name, color, createdAt) " +
                    "VALUES (1, 42, 'Unit 5', '#00A6A6', 100)"
            )
            db.execSQL(
                "INSERT INTO saved_words (word, savedAt, serverId, isSynced, categoryId, senseId) " +
                    "VALUES ('resolve', 500, 7, 1, 1, 'v1')"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 9, true, MIGRATION_8_9)

        db.query("SELECT * FROM categories WHERE id = 1").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(42, cursor.getInt(cursor.getColumnIndexOrThrow("serverId")))
            assertEquals("Unit 5", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            // Своя папка остаётся своей: NULL здесь означал бы «неизвестно», и приложение
            // перестало бы предлагать переименование там, где оно всегда работало.
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("readOnly")))
            assertNull(
                cursor.getString(cursor.getColumnIndexOrThrow("groupName"))
            )
        }

        db.query("SELECT * FROM saved_words WHERE word = 'resolve'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            // Слово не потеряло ни папку, ни выбранное значение.
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("categoryId")))
            assertEquals("v1", cursor.getString(cursor.getColumnIndexOrThrow("senseId")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("readOnly")))
        }
    }

    @Test
    fun aFolderLentByAClassCanBeWrittenAfterTheMigration() {
        helper.createDatabase(DB_NAME, 8).close()
        val db = helper.runMigrationsAndValidate(DB_NAME, 9, true, MIGRATION_8_9)

        db.execSQL(
            "INSERT INTO categories (id, serverId, name, color, createdAt, groupServerId, groupName, readOnly) " +
                "VALUES (2, 99, 'Unit 6', NULL, 200, 3, '9Б', 1)"
        )
        db.execSQL(
            "INSERT INTO saved_words (word, savedAt, serverId, isSynced, categoryId, senseId, groupServerId, readOnly) " +
                "VALUES ('settle', 600, 8, 1, 2, NULL, 3, 1)"
        )

        db.query("SELECT * FROM categories WHERE id = 2").use { cursor ->
            cursor.moveToFirst()
            assertEquals(3, cursor.getInt(cursor.getColumnIndexOrThrow("groupServerId")))
            assertEquals("9Б", cursor.getString(cursor.getColumnIndexOrThrow("groupName")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("readOnly")))
        }

        db.query("SELECT * FROM saved_words WHERE word = 'settle'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(3, cursor.getInt(cursor.getColumnIndexOrThrow("groupServerId")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("readOnly")))
        }
    }
}
