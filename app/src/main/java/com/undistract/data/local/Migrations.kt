package com.undistract.data.local


import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Create new table with Date (stored as INTEGER)
        database.execSQL("""
            CREATE TABLE nfc_tags_new (
                id TEXT NOT NULL PRIMARY KEY,
                payload TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        // 2. Copy data
        database.execSQL("""
            INSERT INTO nfc_tags_new (id, payload, createdAt)
            SELECT id, payload, createdAt FROM nfc_tags
        """.trimIndent())
        // 3. Remove old table
        database.execSQL("DROP TABLE nfc_tags")
        // 4. Rename new table
        database.execSQL("ALTER TABLE nfc_tags_new RENAME TO nfc_tags")
    }
}