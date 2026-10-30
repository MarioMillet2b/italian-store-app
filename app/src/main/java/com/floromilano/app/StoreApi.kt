package com.floromilano.app

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ApiFailure(val status: Int, message: String) : Exception(message)

class StoreApi(
    private val base: String,
    private val token: String,
    private val language: String = "it",
    private val errorLabel: (Int) -> String = { "HTTP $it" },
) : StoreGateway {
    override suspend fun request(path: String, method: String, payload: JSONObject?): JSONObject =
        withContext(Dispatchers.IO) {
            require(path.startsWith("/") && !path.startsWith("//"))
            val connection =
                URL("$base/api/operations/v1$path").openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects =
                    false // Never forward a token to another origin.
                connection.requestMethod = method
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Accept-Language", language)
                connection.setRequestProperty("Authorization", "Bearer $token")
                if (payload != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use {
                        it.write(payload.toString().toByteArray(Charsets.UTF_8))
                    }
                }
                val status = connection.responseCode
                val stream =
                    if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                val json =
                    try {
                        JSONObject(body)
                    } catch (_: Exception) {
                        JSONObject()
                    }
                if (status !in 200..299) {
                    val errors = json.optJSONObject("errors")
                    val first =
                        errors?.keys()?.asSequence()?.firstOrNull()?.let {
                            errors.optJSONArray(it)?.optString(0)
                        }
                    throw ApiFailure(
                        status,
                        first ?: json.optString("message").ifBlank { errorLabel(status) },
                    )
                }
                json
            } finally {
                connection.disconnect()
            }
        }
}
