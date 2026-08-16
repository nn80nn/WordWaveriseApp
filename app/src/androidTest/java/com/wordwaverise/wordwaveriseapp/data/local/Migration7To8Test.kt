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
 * The 7 → 8 migration gives a card the recording of its own word.
 *
 * Same stakes as every migration: it runs on every install at once, and a card that predates the
 * column has to come through it with its schedule intact — the deck is the one thing in the app
 * that took the user weeks to build.
 */
@RunWith(AndroidJUnit4::class)
class Migration7To8Test {

    private companion object {
        const val DB_NAME = "migration-7-8-test-db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun aCardFromBeforeTheColumnKeepsItsScheduleAndGainsAnEmptyRecording() {
        helper.createDatabase(DB_NAME, 7).use { db ->
            db.execSQL(
                "INSERT INTO flashcards (word, definition, example, translation, phonetic, " +
                    "partOfSpeech, serverCategoryId, customized, senseId, repetitionLevel, " +
                    "lastReviewed, nextReviewDate, correctCount, incorrectCount, createdAt, updatedAt) " +
                    "VALUES ('resolve', 'to find a solution', NULL, 'решать', '/rɪˈzɒlv/', " +
                    "'verb', NULL, 0, 'v1', 4, 500, 900, 7, 2, 100, 100)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 8, true, MIGRATION_7_8)

        db.query(
            "SELECT audioUrl, phonetic, senseId, repetitionLevel, correctCount FROM flashcards " +
                "WHERE word = 'resolve'"
        ).use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertNull("no recording until the corpus refresh supplies one", c.getString(0))
            assertEquals("the transcription it already had survives", "/rɪˈzɒlv/", c.getString(1))
            assertEquals("v1", c.getString(2))
            assertEquals("its place in the rotation is untouched", 4, c.getInt(3))
            assertEquals(7, c.getInt(4))
        }
    }

    @Test
    fun aRecordingCanBeStoredAfterTheMigration() {
        helper.createDatabase(DB_NAME, 7).close()
        val db = helper.runMigrationsAndValidate(DB_NAME, 8, true, MIGRATION_7_8)

        db.execSQL(
            "INSERT INTO flashcards (word, definition, nextReviewDate, createdAt, updatedAt, " +
                "customized, repetitionLevel, correctCount, incorrectCount, audioUrl) " +
                "VALUES ('lead', 'a soft heavy metal', 1, 1, 1, 0, 0, 0, 0, 'https://audio/noun.mp3')"
        )

        db.query("SELECT audioUrl FROM flashcards WHERE word = 'lead'").use { c ->
            c.moveToFirst()
            assertEquals("https://audio/noun.mp3", c.getString(0))
        }
    }
}
