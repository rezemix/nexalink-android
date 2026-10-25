package online.nexalink.app.handler

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import online.nexalink.app.dto.SubscriptionUpdateResult
import online.nexalink.app.dto.entities.SubscriptionCache
import online.nexalink.app.dto.entities.SubscriptionItem
import online.nexalink.app.util.DeviceUtil
import online.nexalink.app.util.JsonUtil
import online.nexalink.app.util.LogUtil
import online.nexalink.app.AppConfig
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Вход/регистрация по аккаунту NEXALINK (email+пароль — тот же аккаунт, что
 * на сайте nexalink.online и в Telegram-боте) вместо ручного копирования
 * ссылки подписки. После успешного входа сразу забирает актуальный sub_url
 * из личного кабинета.
 */
object NexalinkAccountManager {

    private const val API_BASE = "https://nexalink.online/api"
    private const val SUB_HOST = "sub.nexalink.online"
    const val SUBSCRIPTION_REMARK = "NEXALINK"
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

    data class FreeAccessResult(val configCount: Int = 0, val error: String? = null)

    /**
     * NEXALINK бесплатный: POST https://sub.nexalink.online/api/public/register —
     * панель выдаёт личную подписку на эту установку (тот же hwid → та же ссылка,
     * повторный вызов ничего не плодит) и сразу импортируем её как "NEXALINK".
     */
    fun getFreeAccess(): FreeAccessResult {
        return try {
            val body = JsonUtil.toJson(mapOf("hwid" to DeviceUtil.getOrCreateHwid(), "source" to "app"))
                .toRequestBody(jsonMediaType)
            val request = Request.Builder().url("https://$SUB_HOST/api/public/register").post(body).build()
            val subUrl = client.newCall(request).execute().use { response ->
                val json = JsonUtil.parseString(response.body?.string().orEmpty())
                if (!response.isSuccessful) {
                    return FreeAccessResult(error = json?.get("detail")?.asString ?: "Ошибка сервера (${response.code})")
                }
                json?.get("subscriptionUrl")?.asString
            }
            if (subUrl.isNullOrEmpty()) return FreeAccessResult(error = "Сервер не выдал ссылку")
            val result = importSubscription(subUrl)
            if (result.configCount > 0) FreeAccessResult(configCount = result.configCount)
            else FreeAccessResult(error = "Не удалось загрузить серверы — попробуйте ещё раз")
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "NexalinkAccountManager getFreeAccess failed", e)
            FreeAccessResult(error = "Нет связи с сервером — проверьте интернет")
        }
    }

    private const val KEY_EXTRA_LINK = "nexalink_extra_device_link"

    /**
     * Отдельная бесплатная ссылка для другого устройства (iPhone, компьютер) — чтобы не делиться
     * своей. Запоминается: повторное открытие показывает ту же, новая — только по forceNew.
     */
    fun getExtraDeviceLink(forceNew: Boolean = false): Pair<String?, String?> {
        val cached = MmkvManager.decodeSettingsString(KEY_EXTRA_LINK)
        if (!forceNew && !cached.isNullOrEmpty()) return cached to null
        return try {
            val body = JsonUtil.toJson(mapOf("hwid" to "app-extra-" + UUID.randomUUID(), "source" to "app-extra"))
                .toRequestBody(jsonMediaType)
            val request = Request.Builder().url("https://$SUB_HOST/api/public/register").post(body).build()
            client.newCall(request).execute().use { response ->
                val json = JsonUtil.parseString(response.body?.string().orEmpty())
                if (!response.isSuccessful) {
                    return null to (json?.get("detail")?.asString ?: "Ошибка сервера (${response.code})")
                }
                val url = json?.get("subscriptionUrl")?.asString ?: return null to "Сервер не выдал ссылку"
                MmkvManager.encodeSettings(KEY_EXTRA_LINK, url)
                url to null
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "NexalinkAccountManager getExtraDeviceLink failed", e)
            null to "Нет связи с сервером — проверьте интернет"
        }
    }

    /** Ссылка основной подписки NEXALINK на этом устройстве (для экрана «Мой доступ»). */
    fun getSubscriptionUrl(): String? = MmkvManager.decodeSubscriptions()
        .firstOrNull { it.subscription.remarks == SUBSCRIPTION_REMARK }?.subscription?.url
        ?: MmkvManager.decodeSubscriptions().firstOrNull { isNexalinkSubUrl(it.subscription.url) }?.subscription?.url

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

    /** Ссылка вида https://sub.nexalink.online/<id> — наша подписка (по ней можно войти без пароля). */
    fun isNexalinkSubUrl(url: String?): Boolean {
        val uri = try { java.net.URI(url?.trim().orEmpty()) } catch (e: Exception) { return false }
        return uri.scheme == "https" && uri.host == SUB_HOST && (uri.path?.trim('/')?.isNotEmpty() == true)
    }

    /**
     * POST /auth/sub-login — вход по ссылке подписки (переход по ссылке из сайта/сообщения).
     * Для подписок без аккаунта на сайте (выданы вручную в панели) вернёт ошибку —
     * это нормально, подписка всё равно импортируется.
     */
    fun loginBySubscription(subUrl: String): AuthResult {
        return try {
            val body = JsonUtil.toJson(mapOf("sub" to subUrl)).toRequestBody(jsonMediaType)
            val request = Request.Builder().url("$API_BASE/auth/sub-login").post(body).build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val json = JsonUtil.parseString(text)
                if (!response.isSuccessful) {
                    return AuthResult(error = json?.get("detail")?.asString ?: "Ошибка сервера (${response.code})")
                }
                val token = json?.get("token")?.asString
                val email = json?.get("email")?.asString
                if (token.isNullOrEmpty() || email.isNullOrEmpty()) {
                    return AuthResult(error = "Не удалось получить токен")
                }
                saveSession(token, email)
                AuthResult(token = token)
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "NexalinkAccountManager loginBySubscription failed", e)
            AuthResult(error = "Ошибка сети — проверьте подключение")
        }
    }

    /**
     * Добавляет/обновляет подписку NEXALINK и сразу скачивает серверы. Переиспользует
     * существующую запись (с той же ссылкой или с именем NEXALINK), чтобы повторный
     * вход или переход по ссылке не плодил дубликаты.
     */
    fun importSubscription(subUrl: String): SubscriptionUpdateResult {
        val subs = MmkvManager.decodeSubscriptions()
        val ours = subs.filter {
            it.subscription.url.trim() == subUrl.trim() || it.subscription.remarks == SUBSCRIPTION_REMARK
        }
        val guid = (ours.firstOrNull { it.subscription.url.trim() == subUrl.trim() } ?: ours.firstOrNull())
            ?.guid ?: UUID.randomUUID().toString()
        // Дубли от старых импортов ("import sub" с той же ссылкой, прежний аккаунт) —
        // удаляем вместе с их серверами, иначе в списке по два одинаковых сервера.
        ours.filter { it.guid != guid }.forEach { MmkvManager.removeSubscription(it.guid) }

        val item = MmkvManager.decodeSubscription(guid) ?: SubscriptionItem()
        item.remarks = SUBSCRIPTION_REMARK
        item.url = subUrl.trim()
        item.enabled = true
        item.autoUpdate = true

        MmkvManager.encodeSubscription(guid, item)
        SettingsChangeManager.makeSetupGroupTab()

        return AngConfigManager.updateConfigViaSub(SubscriptionCache(guid, item))
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
