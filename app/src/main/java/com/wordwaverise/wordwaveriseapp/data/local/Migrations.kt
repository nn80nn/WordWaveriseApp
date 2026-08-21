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

/**
 * Folders reach flashcards, and a card can be edited by hand.
 *
 * `serverCategoryId` holds the folder id *the server* uses — the same number
 * `categories.serverId` carries — because the local `categories.id` is an
 * autogenerated key that means nothing to the API.
 *
 * `customized` is what stops the corpus refresh from undoing a correction on
 * the next study session; without it the editor and the refresh would cancel
 * each other out.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `flashcards` ADD COLUMN `serverCategoryId` INTEGER")
        db.execSQL("ALTER TABLE `flashcards` ADD COLUMN `customized` INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * A saved word remembers which sense of the article it is about.
 *
 * A word has no single translation — `resolve` is "решать" and "разлагать" — so the meaning the
 * user picked is the only thing that makes the saved word, the card and the exercises agree
 * with each other.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `saved_words` ADD COLUMN `senseId` TEXT")
        db.execSQL("ALTER TABLE `flashcards` ADD COLUMN `senseId` TEXT")
    }
}

/**
 * A card learns how its word sounds.
 *
 * The front of a card is the one place the word is met without its article, and English
 * spelling hides precisely the sound — so the recording has to sit on the card itself rather
 * than be looked up when it is needed, which offline it cannot be.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `flashcards` ADD COLUMN `audioUrl` TEXT")
    }
}

/**
 * Папка может прийти на телефон от группы, а не быть своей.
 *
 * `readOnly` — не украшение к бейджу: без него папка преподавателя неотличима от собственной
 * после первой же синхронизации, и приложение предложит переименовать и удалить то, что сервер
 * всё равно откажется менять. Отдельное поле, а не вывод из `groupServerId`, потому что читать
 * его приходится в списках, где группы под рукой нет.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `groupServerId` INTEGER")
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `groupName` TEXT")
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `readOnly` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `saved_words` ADD COLUMN `groupServerId` INTEGER")
        db.execSQL("ALTER TABLE `saved_words` ADD COLUMN `readOnly` INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Слово перестаёт быть строкой и становится записью: одно значение, сколько угодно папок.
 *
 * ⚠️ Написание было первичным ключом, поэтому вторая строка для того же слова не вставала
 * рядом с первой, а затирала её. Пока это так, «сохранить два значения как два слова» нельзя
 * было ни показать, ни принять с сервера — потолок стоял в схеме, а не в интерфейсе. Таблица
 * пересоздаётся: сменить первичный ключ ALTER'ом SQLite не умеет.
 *
 * Папка переезжает как список из одного элемента, а не теряется: у человека уже разложен
 * словарь, и обновление приложения не тот момент, когда это должно сброситься.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `saved_words_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `word` TEXT NOT NULL,
                `savedAt` INTEGER NOT NULL,
                `serverId` INTEGER,
                `isSynced` INTEGER NOT NULL,
                `categoryIds` TEXT NOT NULL,
                `senseId` TEXT,
                `groupServerId` INTEGER,
                `readOnly` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `saved_words_new`
                (`word`, `savedAt`, `serverId`, `isSynced`, `categoryIds`,
                 `senseId`, `groupServerId`, `readOnly`)
            SELECT `word`, `savedAt`, `serverId`, `isSynced`,
                   CASE WHEN `categoryId` IS NULL THEN '' ELSE CAST(`categoryId` AS TEXT) END,
                   `senseId`, `groupServerId`, `readOnly`
            FROM `saved_words`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `saved_words`")
        db.execSQL("ALTER TABLE `saved_words_new` RENAME TO `saved_words`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_saved_words_serverId` ON `saved_words` (`serverId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_words_word` ON `saved_words` (`word`)")
    }
}

/**
 * Списку слов нужен перевод: без него две записи одного слова неразличимы.
 *
 * Колонка добавляется пустой и заполняется ближайшей синхронизацией — перевод и так приходит
 * с сервера в каждой строке, а придумывать его на телефоне не из чего.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `saved_words` ADD COLUMN `translation` TEXT")
    }
}
