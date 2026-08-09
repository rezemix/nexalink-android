package online.nexalink.app.handler

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import online.nexalink.app.util.JsonUtil
import online.nexalink.app.util.LogUtil
import online.nexalink.app.AppConfig
import java.util.concurrent.TimeUnit

/**
 * Вход/регистрация по аккаунту NEXALINK (email+пароль — тот же аккаунт, что
 * на сайте nexalink.online и в Telegram-боте) вместо ручного копирования
 * ссылки подписки. После успешного входа сразу забирает актуальный sub_url
 * из личного кабинета.
 */
object NexalinkAccountManager {

    private const val API_BASE = "https://nexalink.online/api"
    private const val KEY_TOKEN = "nexalink_account_token"
    private const val KEY_EMAIL = "nexalink_account_email"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    data class AuthResult(val token: String? = null, val error: String? = null) {
        val isSuccess get() = token != null
    }

    data class CabinetResult(
        val subUrl: String? = null,
        val hasActiveSubscription: Boolean = false,
        val trialAvailable: Boolean = false,
        val error: String? = null,
    ) {
        val isSuccess get() = error == null
    }

    fun getSavedToken(): String? = MmkvManager.decodeSettingsString(KEY_TOKEN)
    fun getSavedEmail(): String? = MmkvManager.decodeSettingsString(KEY_EMAIL)

    fun logout() {
        MmkvManager.encodeSettings(KEY_TOKEN, "")
        MmkvManager.encodeSettings(KEY_EMAIL, "")
    }

    private fun saveSession(token: String, email: String) {
        MmkvManager.encodeSettings(KEY_TOKEN, token)
        MmkvManager.encodeSettings(KEY_EMAIL, email)
    }

    /** POST /auth/login */
    fun login(email: String, password: String): AuthResult =
        auth("$API_BASE/auth/login", email, password)

    /** POST /auth/register — тот же аккаунт потом виден и на сайте, и в боте. */
    fun register(email: String, password: String): AuthResult =
        auth("$API_BASE/auth/register", email, password)

    private fun auth(url: String, email: String, password: String): AuthResult {
        return try {
            val body = JsonUtil.toJson(mapOf("email" to email, "password" to password))
                .toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = JsonUtil.parseString(text)?.get("detail")?.asString
                    return AuthResult(error = detail ?: "Ошибка сервера (${response.code})")
                }
                val json = JsonUtil.parseString(text)
                val token = json?.get("token")?.asString
                if (token.isNullOrEmpty()) {
                    return AuthResult(error = "Не удалось получить токен")
                }
                saveSession(token, email)
                AuthResult(token = token)
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "NexalinkAccountManager auth failed", e)
            AuthResult(error = "Ошибка сети — проверьте подключение")
        }
    }

    /** GET /cabinet/ — берём ссылку подписки текущего аккаунта. */
    fun fetchCabinet(token: String): CabinetResult {
        return try {
            val request = Request.Builder()
                .url("$API_BASE/cabinet/")
                .get()
                .header("Authorization", "Bearer $token")
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return CabinetResult(error = "Не удалось загрузить кабинет (${response.code})")
                }
                val json = JsonUtil.parseString(text)
                val sub = json?.getAsJsonObject("subscription")
                val trialEnded = json?.get("trial_ended")
                val subUrl = sub?.get("sub_url")?.takeIf { !it.isJsonNull }?.asString
                CabinetResult(
                    subUrl = subUrl,
                    hasActiveSubscription = sub?.get("is_active")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
                    trialAvailable = sub == null && (trialEnded == null || trialEnded.isJsonNull),
                )
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "NexalinkAccountManager fetchCabinet failed", e)
            CabinetResult(error = "Ошибка сети — проверьте подключение")
        }
    }

    /** POST /payments/trial — активирует бесплатный период, если он ещё доступен. */
    fun activateTrial(token: String): Boolean {
        return try {
            val body = "{}".toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$API_BASE/payments/trial")
                .post(body)
                .header("Authorization", "Bearer $token")
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "NexalinkAccountManager activateTrial failed", e)
            false
        }
    }
}
