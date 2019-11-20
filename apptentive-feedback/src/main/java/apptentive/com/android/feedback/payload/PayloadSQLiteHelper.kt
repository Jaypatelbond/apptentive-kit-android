package apptentive.com.android.feedback.payload

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.VisibleForTesting

// FIXME: provide a name for the helper (based on local conversation id)
class PayloadSQLiteHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_QUERY_CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_QUERY_DROP_TABLE)
        onCreate(db)
    }

    fun addPayload(payload: Payload) {
        val values = ContentValues().apply {
            put(COL_NONCE, payload.nonce)
            put(COL_TYPE, payload.type.toString())
            put(COL_MEDIA_TYPE, payload.mediaType.toString())
            put(COL_PAYLOAD_DATA, payload.data)
        }

        writableDatabase.use { db ->
            val result = db.insert(TABLE_NAME, null, values)
            if (result == -1L) {
                throw RuntimeException("Unable to add payload: $payload")
            }
        }
    }

    fun deletePayload(nonce: String): Boolean {
        writableDatabase.use { db ->
            deletePayload(db, nonce)
        }
        return false

    }

    fun nextUnsentPayload(): Payload? {
        writableDatabase.use { db ->
            while (true) {
                db.select(tableName = TABLE_NAME, orderBy = COL_PRIMARY_KEY, limit = 1)
                    .use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nonce = cursor.getString(COL_NONCE)

                            try {
                                return Payload(
                                    nonce = nonce,
                                    type = PayloadType.parse(cursor.getString(COL_TYPE)),
                                    mediaType = MediaType.parse(cursor.getString(COL_MEDIA_TYPE)),
                                    data = cursor.getBlob(COL_PAYLOAD_DATA)
                                )
                            } catch (e: Exception) {
                                deletePayload(db, nonce)
                            }
                        } else {
                            return null
                        }
                    }
            }
        }

        return null
    }

    private fun deletePayload(db: SQLiteDatabase, nonce: String): Boolean {
        val deletedRows = db.delete(TABLE_NAME, column = COL_NONCE, value = nonce)
        return deletedRows > 0
    }

    @VisibleForTesting
    internal fun deleteDatabase(context: Context): Boolean {
        val file = context.getDatabasePath(DATABASE_NAME)
        return file.delete()
    }

    companion object {
        private const val DATABASE_NAME = "payloads.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "payloads"
        private val COL_PRIMARY_KEY = Column(index = 0, name = "_ID")
        private val COL_NONCE = Column(index = 1, name = "nonce")
        private val COL_TYPE = Column(index = 2, name = "payload_type")
        private val COL_MEDIA_TYPE = Column(index = 3, name = "media_type")
        private val COL_PAYLOAD_DATA = Column(index = 4, name = "data")

        private val SQL_QUERY_CREATE_TABLE = "CREATE TABLE $TABLE_NAME (" +
                "$COL_PRIMARY_KEY INTEGER PRIMARY KEY, " +
                "$COL_NONCE TEXT, " +
                "$COL_TYPE TEXT, " +
                "$COL_MEDIA_TYPE TEXT, " +
                "$COL_PAYLOAD_DATA BLOB" +
                ")"
        private const val SQL_QUERY_DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}

private data class Column(val index: Int, val name: String) {
    override fun toString() = name
}

private fun SQLiteDatabase.select(tableName: String, orderBy: Column, limit: Int? = null): Cursor {
    return query(
        tableName,
        null,
        null,
        null,
        null,
        null,
        "${orderBy.name} ASC",
        limit?.toString()
    )
}

private fun SQLiteDatabase.delete(tableName: String, column: Column, value: String) =
    delete(tableName, "${column.name} = ?", arrayOf(value))

private fun ContentValues.put(column: Column, value: String) = put(column.name, value)
private fun ContentValues.put(column: Column, value: ByteArray) = put(column.name, value)

private fun Cursor.getString(column: Column) = getString(column.index)
private fun Cursor.getBlob(column: Column) = getBlob(column.index)