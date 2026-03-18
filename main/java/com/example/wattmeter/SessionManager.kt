package com.example.wattmeter

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object SessionManager {

    private const val PREFS_NAME = "wattmeter_prefs"
    private const val KEY_SESSIONS = "charging_sessions"
    private const val MAX_SESSIONS = 50

    fun saveSessions(context: Context, sessions: List<ChargingSession>) {
        val jsonArray = JSONArray()
        sessions.forEach { session ->
            val obj = JSONObject()
            obj.put("date", session.date)
            obj.put("fromPercent", session.fromPercent)
            obj.put("toPercent", session.toPercent)
            obj.put("durationMinutes", session.durationMinutes)
            obj.put("avgWatts", session.avgWatts)
            obj.put("peakWatts", session.peakWatts)
            jsonArray.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSIONS, jsonArray.toString())
            .apply()
    }

    fun getSessions(context: Context): List<ChargingSession> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SESSIONS, null) ?: return emptyList()

        val sessions = mutableListOf<ChargingSession>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            sessions.add(
                ChargingSession(
                    date = obj.getLong("date"),
                    fromPercent = obj.getInt("fromPercent"),
                    toPercent = obj.getInt("toPercent"),
                    durationMinutes = obj.getInt("durationMinutes"),
                    avgWatts = obj.getDouble("avgWatts"),
                    peakWatts = obj.getDouble("peakWatts")
                )
            )
        }
        return sessions
    }

    fun addSession(context: Context, session: ChargingSession) {
        val sessions = getSessions(context).toMutableList()
        sessions.add(session)
        if (sessions.size > MAX_SESSIONS) sessions.removeAt(0)
        saveSessions(context, sessions)
    }

    fun clearSessions(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SESSIONS)
            .apply()
    }
}