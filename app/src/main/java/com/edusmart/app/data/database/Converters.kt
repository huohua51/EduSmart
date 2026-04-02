package com.edusmart.app.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null || value.isEmpty() || value == "null") {
            return null
        }
        try {
            val listType = object : TypeToken<List<String>>() {}.type
            return gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            return null
        }
    }
    
    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        if (list == null || list.isEmpty()) {
            return null
        }
        return gson.toJson(list)
    }
}

