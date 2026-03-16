package fr.infuseting.readymapeo.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Gère le stockage sécurisé du token d'authentification
 * et de l'ID utilisateur dans les SharedPreferences.
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("readymapeo_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val TAG = "TokenManager"
    }

    fun saveToken(token: String) {
        Log.i(TAG, "Saving token present=${token.isNotBlank()}")
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun fetchToken(): String? {
        val t = prefs.getString(KEY_TOKEN, null)
        Log.i(TAG, "Fetching token present=${t != null}")
        return t
    }

    fun deleteToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun saveUserId(userId: Int) {
        prefs.edit().putInt(KEY_USER_ID, userId).apply()
    }

    fun fetchUserId(): Int? {
        val v = prefs.getInt(KEY_USER_ID, -1)
        return if (v == -1) null else v
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
