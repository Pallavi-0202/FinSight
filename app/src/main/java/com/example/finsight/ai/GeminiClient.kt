package com.example.finsight.ai

import android.util.Log
import com.example.finsight.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

const val GEMINI_MODEL = "gemini-4-hydrogen"

/**
 * Client for communicating with the Gemini REST API using HttpURLConnection,
 * featuring automatic retries for transient errors and clear error handling.
 */
class GeminiClient {

    private val tag = "GeminiClient"

    suspend fun generate(
        systemInstruction: String,
        history: List<ChatMessage>,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            Log.e(tag, "Gemini API key is blank.")
            return@withContext Result.failure(
                Exception("Gemini API key is missing. Please set GEMINI_API_KEY in local.properties.")
            )
        }

        val maxAttempts = 3
        var lastResult: Result<String>? = null

        for (attempt in 1..maxAttempts) {
            val (result, isTransient) = executeGenerateRequest(apiKey, systemInstruction, history, userMessage)
            if (result.isSuccess) {
                return@withContext result
            }

            lastResult = result

            if (isTransient && attempt < maxAttempts) {
                Log.e(tag, "Attempt $attempt failed with transient error: ${result.exceptionOrNull()?.message}. Retrying in 2 seconds...")
                delay(2000)
            } else {
                break
            }
        }

        lastResult ?: Result.failure(Exception("Failed to generate content after $maxAttempts attempts."))
    }

    private fun executeGenerateRequest(
        apiKey: String,
        systemInstruction: String,
        history: List<ChatMessage>,
        userMessage: String
    ): Pair<Result<String>, Boolean> {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 60000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("x-goog-api-key", apiKey)
            }

            val requestJson = JSONObject().apply {
                if (systemInstruction.isNotBlank()) {
                    val sysInstObj = JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                    }
                    put("system_instruction", sysInstObj)
                }

                val contentsArr = JSONArray()
                for (msg in history) {
                    val role = if (msg.isUser) "user" else "model"
                    val contentObj = JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                    }
                    contentsArr.put(contentObj)
                }

                val userMsgObj = JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
                }
                contentsArr.put(userMsgObj)

                put("contents", contentsArr)

                val generationConfig = JSONObject().apply {
                    val thinkingConfig = JSONObject().apply {
                        put("thinkingBudget", 0)
                    }
                    put("thinkingConfig", thinkingConfig)
                }
                put("generationConfig", generationConfig)
            }

            connection.outputStream.use { os ->
                os.write(requestJson.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseText)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val text = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    return Pair(Result.success(text), false)
                } else {
                    Log.e(tag, "No response candidates returned by Gemini model.")
                    return Pair(Result.failure(Exception("No response content generated by model.")), false)
                }
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Log.e(tag, "Gemini HTTP error $responseCode: $errorText")

                val isTransient = responseCode == 429 || responseCode == 503
                val errorMsg = when (responseCode) {
                    400, 401, 403 -> "Authentication error (HTTP $responseCode). Please verify your Gemini API key in local.properties."
                    404 -> "Model not found ($GEMINI_MODEL, HTTP 404). Please check the GEMINI_MODEL setting."
                    429 -> "Rate limit reached (HTTP 429), wait a minute"
                    503 -> "Gemini is busy (HTTP 503), please try again"
                    else -> "Gemini API error (HTTP $responseCode)."
                }
                return Pair(Result.failure(Exception(errorMsg)), isTransient)
            }
        } catch (e: SocketTimeoutException) {
            Log.e(tag, "SocketTimeoutException: ${e.javaClass.simpleName} - ${e.message}", e)
            return Pair(Result.failure(Exception("Request timed out. Please try again.")), true)
        } catch (e: UnknownHostException) {
            Log.e(tag, "UnknownHostException: ${e.javaClass.simpleName} - ${e.message}", e)
            return Pair(Result.failure(Exception("No internet connection. Please check your network.")), false)
        } catch (e: IOException) {
            Log.e(tag, "IOException: ${e.javaClass.simpleName} - ${e.message}", e)
            val isTimeout = e.message?.contains("timeout", ignoreCase = true) == true
            val msg = if (isTimeout) "Request timed out. Please try again." else "Network error: ${e.message}"
            return Pair(Result.failure(Exception(msg)), isTimeout)
        } catch (e: Exception) {
            Log.e(tag, "Exception: ${e.javaClass.simpleName} - ${e.message}", e)
            return Pair(Result.failure(Exception("An error occurred: ${e.message}")), false)
        }
    }
}
