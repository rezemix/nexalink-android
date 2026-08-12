package online.nexalink.app.dto

import com.google.gson.annotations.SerializedName

/**
 * NEXALINK: своя маленькая точка проверки обновлений (nexalink.online/dl/version.json)
 * вместо GitHub Releases — мы не публикуем формальные релизы, просто
 * пересобираем APK и выкладываем на свой сайт.
 */
data class NexalinkVersionInfo(
    @SerializedName("version")
    val version: String,
    @SerializedName("download_url")
    val downloadUrl: String,
    @SerializedName("notes")
    val notes: String = ""
)
