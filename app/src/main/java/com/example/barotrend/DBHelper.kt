package com.example.barotrend

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val query =
            ("CREATE TABLE $TABLE_NAME ($ID_COL INTEGER PRIMARY KEY, $VALUE_COl REAL,$DATETIME_COL DATETIME DEFAULT CURRENT_TIMESTAMP)")
        db.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase, p1: Int, p2: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addValue(value: Float) {
        val values = ContentValues()
        values.put(VALUE_COl, value)
        val db = this.writableDatabase
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getValue(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY ID DESC LIMIT 6", null)
    }

    companion object {
        private const val DATABASE_NAME = "BAROTREND"
        private const val DATABASE_VERSION = 5
        const val TABLE_NAME = "PRESSURE"
        const val ID_COL = "id"
        const val VALUE_COl = "value"
        const val DATETIME_COL = "datetime"
    }
}
