package io.github.clfundev.better.data.db

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.clfundev.better.data.db.converter.BooleanConverter
import io.github.clfundev.better.data.db.converter.RecurRuleConverter
import io.github.clfundev.better.data.db.dao.FocusRecordDao
import io.github.clfundev.better.data.db.dao.PlanDao
import io.github.clfundev.better.data.db.dao.PlanRecordDao
import io.github.clfundev.better.data.db.entity.FocusRecord
import io.github.clfundev.better.data.db.entity.Plan
import io.github.clfundev.better.data.db.entity.PlanRecord
import okhttp3.ResponseBody
import java.io.File

@Database(entities = [Plan::class, PlanRecord::class, FocusRecord::class], version = 1)
@TypeConverters(RecurRuleConverter::class, BooleanConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun planDao(): PlanDao

    abstract fun planRecordDao(): PlanRecordDao

    abstract fun focusRecordDao(): FocusRecordDao

    companion object {
        const val DB_NAME = "better.db"
        private const val BACKUP_DB_NAME = "prepopulate.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        /**
         * 将响应体存为备份数据库文件
         */
        fun saveResponseBodyAsBackup(
            body: ResponseBody,
            context: Context,
            onProgressUpdate: (Float) -> Unit
        ) {
            val contentLength = body.contentLength()
            var downloadedBytes = 0
            body.byteStream().use { input ->
                context.openFileOutput(BACKUP_DB_NAME, MODE_PRIVATE).use { output ->
                    val buffer = ByteArray(4096)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        onProgressUpdate(downloadedBytes.toFloat() / contentLength)
                    }
                    output.flush()
                }
            }
        }

        /**
         * 通过预填充，从备份的db文件恢复数据
         *
         * @param context Context对象
         */
        fun restoreFromBackup(context: Context) {
            // 获取数据库备份文件
            val prepopulateDbFile = File(context.filesDir, BACKUP_DB_NAME)
            if (!prepopulateDbFile.isFile) return
            // 关闭数据库以合并SQLite数据库文件
            closeDatabase()
            // 删除数据库文件
            context.getDatabasePath(DB_NAME).delete()
            // 重新创建数据库，将备份通过预填充导入新数据库
            instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            ).createFromFile(prepopulateDbFile).build()
        }

        /**
         * 关闭数据库
         */
        fun closeDatabase() {
            instance?.close()
            instance = null
        }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .allowMainThreadQueries()
                .build()
    }
}