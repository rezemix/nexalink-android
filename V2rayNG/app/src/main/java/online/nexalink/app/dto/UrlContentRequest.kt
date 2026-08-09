package online.nexalink.app.dto

data class UrlContentRequest(
    val url: String?,
    val timeout: Int = 15000,
    val httpPort: Int = 0,
    val proxyUsername: String? = null,
    val proxyPassword: String? = null,
    val userAgent: String? = null,
    val requestHeaders: String? = null,
    /** Заголовки, которые всегда перекрывают одноимённые из [requestHeaders] (например HWID). */
    val forcedHeaders: Map<String, String>? = null
)