package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.QuestionType
import org.json.JSONArray

class DataConverters {
    @TypeConverter
    fun fromQuestionType(type: QuestionType): String {
        return type.name
    }

    @TypeConverter
    fun toQuestionType(value: String): QuestionType {
        return try {
            QuestionType.valueOf(value)
        } catch (_: Exception) {
            QuestionType.MULTIPLE_CHOICE
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        if (list == null) return "[]"
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
