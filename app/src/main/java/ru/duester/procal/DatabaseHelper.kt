package ru.duester.procal

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            create table $TABLE_PROGRAMS (
                $COLUMN_PROGRAM_ID integer primary key autoincrement,
                $COLUMN_PROGRAM_NAME text not null,
                $COLUMN_PROGRAM_CREATED_AT integer not null
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            create table $TABLE_FUNCTIONS (
                $COLUMN_FUNCTION_ID integer primary key autoincrement,
                $COLUMN_FUNCTION_PROGRAM_ID integer not null,
                $COLUMN_FUNCTION_NAME text not null,
                $COLUMN_FUNCTION_IS_MAIN integer not null default 0,
                foreign key ($COLUMN_FUNCTION_PROGRAM_ID) references $TABLE_PROGRAMS($COLUMN_PROGRAM_ID) on delete cascade
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            create table $TABLE_COMMANDS (
                $COLUMN_COMMAND_ID integer primary key autoincrement,
                $COLUMN_COMMAND_FUNCTION_ID integer not null,
                $COLUMN_COMMAND_POSITION integer not null,
                $COLUMN_COMMAND_TYPE text not null,
                $COLUMN_COMMAND_ARG text,
                foreign key ($COLUMN_COMMAND_FUNCTION_ID) references $TABLE_FUNCTIONS($COLUMN_FUNCTION_ID) on delete cascade
            )
        """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("drop table if exists $TABLE_COMMANDS")
        db.execSQL("drop table if exists $TABLE_FUNCTIONS")
        db.execSQL("drop table if exists $TABLE_PROGRAMS")
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    companion object {
        private const val DATABASE_NAME = "calculator.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_PROGRAMS = "programs"
        const val COLUMN_PROGRAM_ID = "id"
        const val COLUMN_PROGRAM_NAME = "name"
        const val COLUMN_PROGRAM_CREATED_AT = "created_at"

        const val TABLE_FUNCTIONS = "functions"
        const val COLUMN_FUNCTION_ID = "id"
        const val COLUMN_FUNCTION_PROGRAM_ID = "program_id"
        const val COLUMN_FUNCTION_NAME = "name"
        const val COLUMN_FUNCTION_IS_MAIN = "is_main"

        const val TABLE_COMMANDS = "commands"
        const val COLUMN_COMMAND_ID = "id"
        const val COLUMN_COMMAND_FUNCTION_ID = "function_id"
        const val COLUMN_COMMAND_POSITION = "position"
        const val COLUMN_COMMAND_TYPE = "type"
        const val COLUMN_COMMAND_ARG = "arg"
    }
}