package fr.infuseting.readymapeo.data.local

import android.content.Context
import android.content.SharedPreferences

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
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun fetchToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
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
