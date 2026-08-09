package online.nexalink.app.dto

data class OutboundTrafficStat(
    val tag: String,
    val direction: String,
    val value: Long,
)