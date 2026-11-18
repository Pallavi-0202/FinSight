package com.example.finsight.auth

import android.content.Context
import android.content.SharedPreferences

/**
 * Session manager for storing and retrieving authenticated user session details in SharedPreferences.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("finsight_user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_NAME = "key_user_name"
    }

    fun login(userId: Long, name: String) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, name)
            .apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return currentUserId() != -1L
    }

    fun currentUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1L)
    }

    fun currentUserName(): String {
        return prefs.getString(KEY_USER_NAME, "").orEmpty()
    }
}
