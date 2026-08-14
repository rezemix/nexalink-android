package online.nexalink.app.dto

import com.google.gson.annotations.SerializedName

/**
 * NEXALINK: активное объявление с сайта (nexalink.online/api/announcements/active) —
 * например, предупреждение о тестах на боевых нодах. Показывается один раз на id,
 * дальше не повторяется (id сохраняется в настройках как "уже видели").
 */
data class AnnouncementInfo(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("body")
    val body: String,
    @SerializedName("type")
    val type: String = "info"
)
