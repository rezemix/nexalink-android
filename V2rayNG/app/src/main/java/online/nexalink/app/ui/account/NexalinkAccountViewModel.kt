package online.nexalink.app.ui.account

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import online.nexalink.app.dto.SubscriptionUpdateResult
import online.nexalink.app.dto.entities.SubscriptionCache
import online.nexalink.app.dto.entities.SubscriptionItem
import online.nexalink.app.handler.AngConfigManager
import online.nexalink.app.handler.MmkvManager
import online.nexalink.app.handler.NexalinkAccountManager
import online.nexalink.app.handler.SettingsChangeManager
import online.nexalink.app.ui.base.BaseViewModel
import java.util.UUID

class NexalinkAccountViewModel(application: Application) : BaseViewModel(application) {

    private val _isLoggedIn = MutableStateFlow(!NexalinkAccountManager.getSavedToken().isNullOrEmpty())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _savedEmail = MutableStateFlow(NexalinkAccountManager.getSavedEmail().orEmpty())
    val savedEmail: StateFlow<String> = _savedEmail.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun logout() {
        NexalinkAccountManager.logout()
        _isLoggedIn.value = false
        _savedEmail.value = ""
    }

    fun submit(email: String, password: String, isRegister: Boolean) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Заполните email и пароль"
            return
        }
        launchLoading {
            withContext(Dispatchers.IO) {
                val authResult = if (isRegister) {
                    NexalinkAccountManager.register(email.trim(), password)
                } else {
                    NexalinkAccountManager.login(email.trim(), password)
                }

                if (!authResult.isSuccess || authResult.token == null) {
                    _errorMessage.value = authResult.error ?: "Не удалось войти"
                    return@withContext
                }

                var cabinet = NexalinkAccountManager.fetchCabinet(authResult.token)

                // Триал ещё не активирован — активируем сразу, чтобы сразу было
                // что импортировать (так же, как при регистрации на сайте/в боте).
                if (cabinet.isSuccess && cabinet.subUrl == null && cabinet.trialAvailable) {
                    NexalinkAccountManager.activateTrial(authResult.token)
                    cabinet = NexalinkAccountManager.fetchCabinet(authResult.token)
                }

                if (!cabinet.isSuccess) {
                    _errorMessage.value = cabinet.error ?: "Не удалось загрузить кабинет"
                    return@withContext
                }

                if (cabinet.subUrl.isNullOrEmpty()) {
                    // Вход прошёл успешно, но подписки пока нет (например, кончилась
                    // и не продлена) — не блокируем вход, просто без импорта.
                    _isLoggedIn.value = true
                    _savedEmail.value = email.trim()
                    toast("Вход выполнен. Активной подписки пока нет — оформите её в личном кабинете на сайте.")
                    return@withContext
                }

                val importResult = importSubscription(cabinet.subUrl)
                _isLoggedIn.value = true
                _savedEmail.value = email.trim()

                if (importResult.configCount > 0) {
                    toastSuccess("Готово! Импортировано серверов: ${importResult.configCount}")
                } else {
                    toast("Вход выполнен, но не удалось загрузить серверы — потяните вниз для обновления")
                }
            }
        }
    }

    private fun importSubscription(subUrl: String): SubscriptionUpdateResult {
        // Ищем уже существующую подписку NEXALINK (повторный вход/обновление токена),
        // иначе заводим новую — чтобы не плодить дубликаты при каждом входе.
        val existingGuid = MmkvManager.decodeSubscriptions()
            .firstOrNull { it.subscription.remarks == SUBSCRIPTION_REMARK }
            ?.guid
        val guid = existingGuid ?: UUID.randomUUID().toString()

        val item = MmkvManager.decodeSubscription(guid) ?: SubscriptionItem()
        item.remarks = SUBSCRIPTION_REMARK
        item.url = subUrl
        item.enabled = true
        item.autoUpdate = true

        MmkvManager.encodeSubscription(guid, item)
        SettingsChangeManager.makeSetupGroupTab()

        return AngConfigManager.updateConfigViaSub(SubscriptionCache(guid, item))
    }

    companion object {
        private const val SUBSCRIPTION_REMARK = "NEXALINK"
    }
}
