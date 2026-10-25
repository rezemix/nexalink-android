package online.nexalink.app.ui.account

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import online.nexalink.app.handler.NexalinkAccountManager
import online.nexalink.app.ui.base.BaseViewModel

/**
 * «Мой доступ»: NEXALINK бесплатный, аккаунтов нет — доступ это ссылка подписки.
 * Её можно скопировать или отправить, чтобы подключить другое устройство.
 */
class NexalinkAccountViewModel(application: Application) : BaseViewModel(application) {

    private val _subscriptionUrl = MutableStateFlow(NexalinkAccountManager.getSubscriptionUrl())
    val subscriptionUrl: StateFlow<String?> = _subscriptionUrl.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun getFreeAccess() {
        _errorMessage.value = null
        launchLoading {
            val result = withContext(Dispatchers.IO) { NexalinkAccountManager.getFreeAccess() }
            if (result.error != null) {
                _errorMessage.value = result.error
            } else {
                _subscriptionUrl.value = NexalinkAccountManager.getSubscriptionUrl()
                toastSuccess("Готово! Серверов: ${result.configCount}")
            }
        }
    }
}
