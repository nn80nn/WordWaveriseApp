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
 * The 6 → 7 migration teaches a saved word and a card which sense of the article they are about.
 *
 * Worth a test of its own because it runs on every existing install at once: a migration that
 * throws does not degrade a feature, it stops the app from opening at all. And because rows
 * that predate the column have to survive it — everyone's existing vocabulary was saved
 * without a chosen sense, and must keep working as "whatever the article puts first".
 */
@RunWith(AndroidJUnit4::class)
class Migration6To7Test {

    private companion object {
        const val DB_NAME = "migration-6-7-test-db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun wordsAndCardsSavedBeforeTheColumnExistedSurviveWithNoSense() {
        helper.createDatabase(DB_NAME, 6).use { db ->
            db.execSQL(
                "INSERT INTO saved_words (word, savedAt, serverId, isSynced, categoryId) " +
                    "VALUES ('resolve', 100, 42, 1, NULL)"
            )
            db.execSQL(
                "INSERT INTO flashcards (word, definition, example, translation, phonetic, " +
                    "partOfSpeech, serverCategoryId, customized, repetitionLevel, lastReviewed, " +
                    "nextReviewDate, correctCount, incorrectCount, createdAt, updatedAt) " +
                    "VALUES ('resolve', 'to find a solution', NULL, 'решать', NULL, 'verb', " +
                    "NULL, 0, 3, NULL, 200, 5, 1, 100, 100)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 7, true, MIGRATION_6_7)

        db.query("SELECT senseId, serverId, categoryId FROM saved_words WHERE word = 'resolve'").use { c ->
            assertEquals("the saved word must still be there", 1, c.count)
            c.moveToFirst()
            assertNull("a word saved before the column has no sense", c.getString(0))
            assertEquals("its server link survives", 42, c.getInt(1))
        }

        db.query("SELECT senseId, repetitionLevel, correctCount FROM flashcards WHERE word = 'resolve'").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertNull("an existing card starts unpinned", c.getString(0))
            // Расписание — самое дорогое, что есть у карточки: миграция схемы не имеет права
            // сбросить то, что уже известно про человека.
            assertEquals("its schedule is untouched", 3, c.getInt(1))
            assertEquals(5, c.getInt(2))
        }
    }

    @Test
    fun aSenseCanBeStoredOnBothTablesAfterTheMigration() {
        helper.createDatabase(DB_NAME, 6).close()
        val db = helper.runMigrationsAndValidate(DB_NAME, 7, true, MIGRATION_6_7)

        db.execSQL(
            "INSERT INTO saved_words (word, savedAt, isSynced, senseId) VALUES ('settle', 1, 1, 'v2')"
        )
        db.execSQL(
            "INSERT INTO flashcards (word, definition, nextReviewDate, createdAt, updatedAt, " +
                "customized, repetitionLevel, correctCount, incorrectCount, senseId) " +
                "VALUES ('settle', 'to resolve a dispute', 1, 1, 1, 0, 0, 0, 0, 'v2')"
        )

        db.query("SELECT senseId FROM saved_words WHERE word = 'settle'").use { c ->
            c.moveToFirst()
            assertEquals("v2", c.getString(0))
        }
        db.query("SELECT senseId FROM flashcards WHERE word = 'settle'").use { c ->
            c.moveToFirst()
            assertEquals("v2", c.getString(0))
        }
    }
}
