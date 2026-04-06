package com.project.medi_agent.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 简单的健康档案仓库：使用 SharedPreferences 存储 Key-Value 对。
 * 便于快速集成与演示，后续可替换为 Room 实现。
 */
class HealthProfileRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("health_profile", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY_MAP = "profile_map"

    fun getAll(): Map<String, String> {
        val json = prefs.getString(KEY_MAP, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun update(key: String, value: String) {
        val map = getAll().toMutableMap()
        map[key] = value
        prefs.edit().putString(KEY_MAP, gson.toJson(map)).apply()
    }

    fun remove(key: String) {
        val map = getAll().toMutableMap()
        map.remove(key)
        prefs.edit().putString(KEY_MAP, gson.toJson(map)).apply()
    }

    fun exportJson(): String {
        val map = getAll()
        return gson.toJson(map)
    }
}

