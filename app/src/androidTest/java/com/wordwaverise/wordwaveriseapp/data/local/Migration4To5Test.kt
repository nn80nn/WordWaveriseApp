package com.wordwaverise.wordwaveriseapp.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The 4 → 5 migration exists to make a fixed bug unrepeatable, and it has to
 * clean up after that bug before it can: a unique index cannot be created while
 * duplicates are still in the table.
 */
@RunWith(AndroidJUnit4::class)
class Migration4To5Test {

    private companion object {
        const val DB_NAME = "migration-test-db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun collapsesDuplicateCategoriesAndKeepsTheirWords() {
        helper.createDatabase(DB_NAME, 4).use { db ->
            // Exactly the shape the old sync produced: one server category
            // minted twice locally, with a word sitting on the younger copy.
            db.execSQL(
                "INSERT INTO categories (id, serverId, name, color, createdAt) VALUES (1, 3, 'Fruits', NULL, 100)"
            )
            db.execSQL(
                "INSERT INTO categories (id, serverId, name, color, createdAt) VALUES (2, 3, 'Fruits', NULL, 200)"
            )
            db.execSQL(
                "INSERT INTO categories (id, serverId, name, color, createdAt) VALUES (3, NULL, 'Офлайн', NULL, 300)"
            )
            db.execSQL(
                "INSERT INTO saved_words (word, savedAt, isSynced, categoryId) VALUES ('apple', 1, 1, 2)"
            )
            db.execSQL(
                "INSERT INTO saved_words (word, savedAt, isSynced, categoryId) VALUES ('orange', 1, 1, 1)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 5, true, MIGRATION_4_5)

        db.query("SELECT id FROM categories WHERE serverId = 3").use { c ->
            assertEquals("one row must survive per server category", 1, c.count)
            c.moveToFirst()
            assertEquals("the oldest row is the one that stays", 1L, c.getLong(0))
        }

        db.query("SELECT categoryId FROM saved_words WHERE word = 'apple'").use { c ->
            c.moveToFirst()
            assertEquals("the word moved onto the surviving row", 1L, c.getLong(0))
        }

        db.query("SELECT COUNT(*) FROM categories WHERE serverId IS NULL").use { c ->
            c.moveToFirst()
            assertEquals("a folder made offline is untouched", 1, c.getInt(0))
        }
    }

    @Test
    fun addsTheUniqueIndexAndTheArticleCache() {
        helper.createDatabase(DB_NAME, 4).close()
        val db = helper.runMigrationsAndValidate(DB_NAME, 5, true, MIGRATION_4_5)

        db.query("PRAGMA index_list(categories)").use { c ->
            var found = false
            while (c.moveToNext()) {
                if (c.getString(c.getColumnIndexOrThrow("name")) == "index_categories_serverId") {
                    found = true
                    assertEquals("the index must be unique", 1, c.getInt(c.getColumnIndexOrThrow("unique")))
                }
            }
            assertTrue("index_categories_serverId is missing", found)
        }

        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'article_cache'").use { c ->
            assertEquals(1, c.count)
        }
    }

    @Test
    fun theIndexRejectsASecondRowForTheSameServerCategory() {
        helper.createDatabase(DB_NAME, 4).close()
        val db = helper.runMigrationsAndValidate(DB_NAME, 5, true, MIGRATION_4_5)

        db.execSQL("INSERT INTO categories (serverId, name, color, createdAt) VALUES (7, 'Fruits', NULL, 1)")

        val duplicateRejected = try {
            db.execSQL("INSERT INTO categories (serverId, name, color, createdAt) VALUES (7, 'Fruits', NULL, 2)")
            false
        } catch (_: Exception) {
            true
        }
        assertTrue("a duplicate serverId must not be insertable", duplicateRejected)

        // Local-only folders are exempt: SQLite allows many NULLs in a unique index.
        db.execSQL("INSERT INTO categories (serverId, name, color, createdAt) VALUES (NULL, 'A', NULL, 3)")
        db.execSQL("INSERT INTO categories (serverId, name, color, createdAt) VALUES (NULL, 'B', NULL, 4)")
        db.query("SELECT COUNT(*) FROM categories WHERE serverId IS NULL").use { c ->
            c.moveToFirst()
            assertEquals(2, c.getInt(0))
        }
    }
}
