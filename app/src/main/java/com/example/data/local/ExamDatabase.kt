package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.AppBrandingEntity
import com.example.data.model.ExamEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.StudentEntity
import com.example.data.model.SubmissionEntity

@Database(
    entities = [
        ExamEntity::class,
        QuestionEntity::class,
        StudentEntity::class,
        SubmissionEntity::class,
        AppBrandingEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DataConverters::class)
abstract class ExamDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao

    companion object {
        @Volatile
        private var INSTANCE: ExamDatabase? = null

        fun getDatabase(context: Context): ExamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExamDatabase::class.java,
                    "exam_forms_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
