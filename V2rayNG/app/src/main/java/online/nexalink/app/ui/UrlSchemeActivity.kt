package online.nexalink.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import online.nexalink.app.AppConfig
import online.nexalink.app.R
import online.nexalink.app.extension.toast
import online.nexalink.app.extension.toastError
import online.nexalink.app.handler.AngConfigManager
import online.nexalink.app.ui.base.BaseComponentActivity
import online.nexalink.app.ui.main.MainActivity
import online.nexalink.app.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UrlSchemeActivity : BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            var handledAsync = false
            intent.apply {
                if (action == Intent.ACTION_SEND) {
                    if ("text/plain" == type) {
                        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                            handledAsync = parseUri(it, null)
                        }
                    }
                } else if (action == Intent.ACTION_VIEW) {
                    when (data?.host) {
                        "install-config" -> {
                            val uri: Uri? = intent.data
                            val shareUrl = uri?.getQueryParameter("url").orEmpty()
                            handledAsync = parseUri(shareUrl, uri?.fragment)
                        }

                        "install-sub" -> {
                            val uri: Uri? = intent.data
                            val shareUrl = uri?.getQueryParameter("url").orEmpty()
                            handledAsync = parseUri(shareUrl, uri?.fragment)
                        }

                        else -> {
                            toastError(R.string.toast_failure)
                        }
                    }
                }
            }

            // NEXALINK: раньше finish() вызывался сразу же, не дожидаясь фоновой
            // корутины из parseUri() — на тёплом старте (приложение уже открывали)
            // импорт обычно успевал проскочить, а на чистой установке (deep-link
            // "Открыть в приложении" сразу после установки APK) activity убивалась
            // раньше, чем подписка успевала скачаться, и lifecycleScope корутину
            // отменял на середине — пользователь видел "Сервер не выбран", хотя
            // ссылка была абсолютно рабочая. Если запустили асинхронный импорт —
            // finishToMain() вызовет сам parseUri() после его реального завершения.
            if (!handledAsync) {
                finishToMain()
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Error processing URL scheme", e)
            finishToMain()
        }
    }

    @Composable
    override fun ScreenContent() {
    }

    private fun finishToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /** @return true, если импорт запущен асинхронно (и сам закроет activity по завершении). */
    private fun parseUri(uriString: String?, fragment: String?): Boolean {
        if (uriString.isNullOrEmpty()) {
            return false
        }
        LogUtil.i(AppConfig.TAG, uriString)

        // NEXALINK: было URLDecoder.decode() — а он декодирует по правилам
        // application/x-www-form-urlencoded и превращает "+" в пробел. Для
        // install-sub/install-config uriString приходит из
        // uri.getQueryParameter("url"), который сам уже полностью раскодировал
        // строку — повторный URLDecoder.decode() ломал ссылку, если токен
        // подписки содержал "+" (валидный символ в base64). Uri.decode() делает
        // только % → символ, "+" не трогает — безопасно даже если строку уже
        // раскодировали один раз.
        var decodedUrl = Uri.decode(uriString)
        val uri = Uri.parse(decodedUrl)
        if (uri != null) {
            if (uri.fragment.isNullOrEmpty() && !fragment.isNullOrEmpty()) {
                decodedUrl += "#${fragment}"
            }
            LogUtil.i(AppConfig.TAG, decodedUrl)
            lifecycleScope.launch(Dispatchers.IO) {
                val (count, countSub) = AngConfigManager.importBatchConfig(decodedUrl, "", false)
                withContext(Dispatchers.Main) {
                    if (count + countSub > 0) {
                        toast(R.string.import_subscription_success)
                    } else {
                        toast(R.string.import_subscription_failure)
                    }
                    finishToMain()
                }
            }
            return true
        }
        return false
    }
}
