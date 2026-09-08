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
 * Миграция 12 → 13: у папки появляется книга, словарём которой она служит.
 *
 * Колонка добавляется пустой, и это единственный правильный ответ: признак приезжает с сервера,
 * вывести его на телефоне не из чего. Догадаться по названию нельзя — совпадение имени папки с
 * именем книги распадается на первом же переименовании любого из двух, а неверный значок хуже
 * его отсутствия: он утверждает то, чего нет.
 *
 * Проверяется главное: разложенные папки остаются на месте, со своей вложенностью и своей
 * пометкой группы. Обновление приложения — не тот момент, когда папка может «просто пропасть».
 */
@RunWith(AndroidJUnit4::class)
class Migration12To13Test {

    private companion object {
        const val DB_NAME = "migration-12-13-test-db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun foldersSurviveAndArriveWithoutABook() {
        helper.createDatabase(DB_NAME, 12).use { db ->
            db.execSQL(
                "INSERT INTO categories " +
                    "(serverId, name, color, createdAt, groupServerId, groupName, readOnly, parentServerId) " +
                    "VALUES (7, 'Урок 3', '#abc', 500, 4, '9Б', 1, 2)"
            )
            db.execSQL(
                "INSERT INTO categories (serverId, name, color, createdAt, readOnly) " +
                    "VALUES (8, 'Мои слова', NULL, 600, 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 13, true, MIGRATION_12_13)

        db.query("SELECT * FROM categories ORDER BY serverId").use { cursor ->
            assertEquals(2, cursor.count)

            cursor.moveToFirst()
            assertEquals("Урок 3", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("bookServerId")))
            // Всё, что папка уже несла, обязано доехать нетронутым: папка класса, потерявшая
            // readOnly, становится редактируемой на телефоне и отклоняемой на сервере разом.
            assertEquals(4, cursor.getInt(cursor.getColumnIndexOrThrow("groupServerId")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("readOnly")))
            assertEquals(2, cursor.getInt(cursor.getColumnIndexOrThrow("parentServerId")))

            cursor.moveToNext()
            assertEquals("Мои слова", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("bookServerId")))
        }
    }
}
