package com.wordwaverise.wordwaveriseapp.data.local

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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `serverId` INTEGER,
                `name` TEXT NOT NULL,
                `color` TEXT,
                `createdAt` INTEGER NOT NULL)"""
        )
        db.execSQL("ALTER TABLE `saved_words` ADD COLUMN `categoryId` INTEGER")
    }
}

/**
 * Two things, both about making a fixed bug unrepeatable.
 *
 * The unique index on `categories.serverId` cannot be created while duplicates
 * are still in the table, so the rows left behind by the old sync are collapsed
 * first — words move onto the surviving (oldest) row before the copies go, so
 * none of them loses its folder.
 *
 * `article_cache` holds finished articles for offline reading.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """UPDATE saved_words SET categoryId = (
                   SELECT MIN(c.id) FROM categories c
                   WHERE c.serverId = (
                       SELECT o.serverId FROM categories o WHERE o.id = saved_words.categoryId
                   )
               )
               WHERE categoryId IN (SELECT id FROM categories WHERE serverId IS NOT NULL)"""
        )
        db.execSQL(
            """DELETE FROM categories WHERE serverId IS NOT NULL AND id NOT IN (
                   SELECT MIN(id) FROM categories WHERE serverId IS NOT NULL GROUP BY serverId
               )"""
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_serverId` ON `categories` (`serverId`)"
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `article_cache` (
                `key` TEXT PRIMARY KEY NOT NULL,
                `payload` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL)"""
        )
    }
}
