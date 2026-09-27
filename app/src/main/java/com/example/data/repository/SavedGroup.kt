package com.example.data.repository

import org.json.JSONArray
import org.json.JSONObject

/** A user-saved schedule source shown on the profile's group carousel. */
data class SavedGroup(
    val id: String,
    val title: String,
    val url: String,
    val confirmedToday: Int = 0,
    val updatedAtMillis: Long = 0L
)

internal fun SavedGroup.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("url", url)
    put("confirmedToday", confirmedToday)
    put("updatedAtMillis", updatedAtMillis)
}

internal fun JSONObject.toSavedGroup(): SavedGroup = SavedGroup(
    id = optString("id"),
    title = optString("title"),
    url = optString("url"),
    confirmedToday = optInt("confirmedToday", 0),
    updatedAtMillis = optLong("updatedAtMillis", 0L)
)

internal fun List<SavedGroup>.toJsonArray(): String = JSONArray().also { array ->
    forEach { array.put(it.toJson()) }
}.toString()
