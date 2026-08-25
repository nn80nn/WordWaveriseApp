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
 * Миграция 11 → 12: у папки появляется родительская папка.
 *
 * Колонка добавляется пустой, и это единственный правильный ответ: вложенность приезжает с
 * сервера, придумать её на телефоне не из чего. Пустая, а не «в корне по умолчанию» — разница
 * только в словах, но не в том, что произойдёт с папками, которые человек уже разложил: они
 * обязаны остаться там же, где были, вместе со своей пометкой группы.
 */
@RunWith(AndroidJUnit4::class)
class Migration11To12Test {

    private companion object {
        const val DB_NAME = "migration-11-12-test-db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun foldersSurviveAndArriveWithoutAParent() {
        helper.createDatabase(DB_NAME, 11).use { db ->
            db.execSQL(
                "INSERT INTO categories (serverId, name, color, createdAt, groupServerId, groupName, readOnly) " +
                    "VALUES (7, 'Урок 3', '#abc', 500, 4, '9Б', 1)"
            )
            db.execSQL(
                "INSERT INTO categories (serverId, name, color, createdAt, readOnly) " +
                    "VALUES (8, 'Мои слова', NULL, 600, 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 12, true, MIGRATION_11_12)

        db.query("SELECT * FROM categories ORDER BY serverId").use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToFirst()
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("parentServerId")))
            // Пометка группы обязана доехать нетронутой: папка класса, потерявшая readOnly,
            // становится редактируемой на телефоне и отклоняемой на сервере одновременно.
            assertEquals("Урок 3", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(4, cursor.getInt(cursor.getColumnIndexOrThrow("groupServerId")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("readOnly")))

            cursor.moveToNext()
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("parentServerId")))
            assertEquals("Мои слова", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
    }

    @Test
    fun aFolderCanNowNameTheGroupItIsFiledIn() {
        helper.createDatabase(DB_NAME, 11).use { db ->
            db.execSQL(
                "INSERT INTO categories (serverId, name, createdAt, readOnly) VALUES (10, 'Модуль 1', 500, 0)"
            )
            db.execSQL(
                "INSERT INTO categories (serverId, name, createdAt, readOnly) VALUES (11, 'Урок 1', 600, 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 12, true, MIGRATION_11_12)

        // Ссылка идёт по serverId, а не по локальному id: локальный автогенерируемый, и API
        // о нём ничего не знает — связь по нему жила бы только на этом телефоне.
        db.execSQL("UPDATE categories SET parentServerId = 10 WHERE serverId = 11")

        db.query("SELECT name FROM categories WHERE parentServerId = 10").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Урок 1", cursor.getString(0))
        }
    }
}
