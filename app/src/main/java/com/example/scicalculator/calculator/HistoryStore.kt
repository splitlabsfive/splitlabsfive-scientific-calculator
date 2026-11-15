package com.example.scicalculator.calculator

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(val expression: String, val result: String)

class HistoryStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sci_calc_history", Context.MODE_PRIVATE)

    fun load(): List<HistoryEntry> {
        val json = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                HistoryEntry(
                    expression = obj.getString(KEY_EXPRESSION),
                    result = obj.getString(KEY_RESULT)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(entries: List<HistoryEntry>) {
        val arr = JSONArray()
        entries.take(MAX_ENTRIES).forEach { entry ->
            arr.put(
                JSONObject()
                    .put(KEY_EXPRESSION, entry.expression)
                    .put(KEY_RESULT, entry.result)
            )
        }
        prefs.edit().putString(KEY_ENTRIES, arr.toString()).apply()
    }

    companion object {
        private const val KEY_ENTRIES = "entries"
        private const val KEY_EXPRESSION = "expression"
        private const val KEY_RESULT = "result"
        const val MAX_ENTRIES = 50
    }
}
