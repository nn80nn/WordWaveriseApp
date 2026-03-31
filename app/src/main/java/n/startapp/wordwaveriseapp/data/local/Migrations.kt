package n.startapp.wordwaveriseapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrations for the WordWaverise local database.
 *
 * Versions 1 and 2 were development-only (never shipped to users),
 * so these migrations simply recreate the tables to the current schema.
 */

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `flashcards`")
        db.execSQL("DROP TABLE IF EXISTS `saved_words`")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `saved_words` (
                `word` TEXT NOT NULL,
                `savedAt` INTEGER NOT NULL,
                `serverId` INTEGER,
                `isSynced` INTEGER NOT NULL,
                PRIMARY KEY(`word`))"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `flashcards` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `serverId` INTEGER,
                `word` TEXT NOT NULL,
                `definition` TEXT NOT NULL,
                `example` TEXT,
                `translation` TEXT,
                `phonetic` TEXT,
                `partOfSpeech` TEXT,
                `repetitionLevel` INTEGER NOT NULL,
                `lastReviewed` INTEGER,
                `nextReviewDate` INTEGER NOT NULL,
                `correctCount` INTEGER NOT NULL,
                `incorrectCount` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL)"""
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `flashcards`")
        db.execSQL("DROP TABLE IF EXISTS `saved_words`")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `saved_words` (
                `word` TEXT NOT NULL,
                `savedAt` INTEGER NOT NULL,
                `serverId` INTEGER,
                `isSynced` INTEGER NOT NULL,
                PRIMARY KEY(`word`))"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `flashcards` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `serverId` INTEGER,
                `word` TEXT NOT NULL,
                `definition` TEXT NOT NULL,
                `example` TEXT,
                `translation` TEXT,
                `phonetic` TEXT,
                `partOfSpeech` TEXT,
                `repetitionLevel` INTEGER NOT NULL,
                `lastReviewed` INTEGER,
                `nextReviewDate` INTEGER NOT NULL,
                `correctCount` INTEGER NOT NULL,
                `incorrectCount` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL)"""
        )
    }
}
