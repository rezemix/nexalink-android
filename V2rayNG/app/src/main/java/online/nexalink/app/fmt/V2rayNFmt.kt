package online.nexalink.app.fmt

import online.nexalink.app.AppConfig
import online.nexalink.app.dto.V2rayNShareItem
import online.nexalink.app.dto.entities.ProfileItem
import online.nexalink.app.util.JsonUtil
import online.nexalink.app.util.LogUtil
import online.nexalink.app.util.Utils

object V2rayNFmt : FmtBase() {
    fun parse(str: String): ProfileItem? {
        try {
            val jsonBase64Payload = str.substringAfterLast('/')
            val jsonPayload = Utils.decode(jsonBase64Payload)
            val v2rayNShareItem = JsonUtil.fromJson(jsonPayload, V2rayNShareItem::class.java)
            return v2rayNShareItem?.toProfileItem()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to parse V2rayN format", e)
        }
        return null
    }
}