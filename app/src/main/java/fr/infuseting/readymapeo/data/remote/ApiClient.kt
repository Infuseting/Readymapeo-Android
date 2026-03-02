package fr.infuseting.readymapeo.data.remote

import fr.infuseting.readymapeo.BuildConfig
import fr.infuseting.readymapeo.data.local.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client HTTP basé uniquement sur HttpURLConnection (API Java standard).
 * Aucune bibliothèque tierce n'est utilisée.
 *
 * Gère automatiquement :
 * - L'injection du header Authorization: Bearer <token>
 * - Le header Accept: application/json
 * - L'exécution sur Dispatchers.IO
 */
class ApiClient(private val tokenManager: TokenManager) {

    private val baseUrl: String = BuildConfig.BASE_URL

    /**
     * Effectue une requête GET.
     * @return le body de la réponse sous forme de String JSON.
     */
    suspend fun get(endpoint: String): String = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl$endpoint")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setHeaders(this)
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            handleResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Effectue une requête POST avec un body JSON.
     */
    suspend fun post(endpoint: String, body: String? = null): String = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl$endpoint")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setHeaders(this)
            connectTimeout = 15_000
            readTimeout = 15_000
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { it.write(body) }
            }
        }
        try {
            handleResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Effectue une requête PUT avec un body JSON.
     */
    suspend fun put(endpoint: String, body: String): String = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl$endpoint")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            setHeaders(this)
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { it.write(body) }
        }
        try {
            handleResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Effectue une requête DELETE.
     */
    suspend fun delete(endpoint: String): String = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl$endpoint")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            setHeaders(this)
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            handleResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private fun setHeaders(connection: HttpURLConnection) {
        connection.setRequestProperty("Accept", "application/json")
        tokenManager.fetchToken()?.let { token ->
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
    }

    private fun handleResponse(connection: HttpURLConnection): String {
        val code = connection.responseCode
        val stream = if (code in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }

        if (code !in 200..299) {
            throw ApiException(code, body)
        }

        return body
    }
}

/**
 * Exception personnalisée pour les erreurs HTTP.
 */
class ApiException(val statusCode: Int, val responseBody: String) :
    Exception("HTTP $statusCode: $responseBody")
