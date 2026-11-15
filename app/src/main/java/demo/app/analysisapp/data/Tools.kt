package demo.app.analysisapp.data

import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/** 读取剪贴板文本 */
internal fun readClipboardText(context: Context): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = clipboard.primaryClip ?: return ""
    if (clipData.itemCount == 0) return ""
    return clipData.getItemAt(0).coerceToText(context)?.toString().orEmpty()
}

/** 复制文本到剪贴板 */
internal fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
}

/** 从网络地址加载图片 Bitmap */
internal fun loadImageBitmap(imageUrl: String): android.graphics.Bitmap? {
    val connection = URL(imageUrl).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        if (connection.responseCode !in 200..299) {
            return null
        }
        connection.inputStream.use { input ->
            BitmapFactory.decodeStream(input)
        }
    } finally {
        connection.disconnect()
    }
}

/** 把 JSON 数组转成字符串列表 */
internal fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            // 兼容两种返回：直接是字符串，或 {"url"/"image": "..."} 对象
            val value = if (opt(index) is JSONObject) {
                val item = optJSONObject(index)
                item?.optString("url").orEmpty().ifBlank { item?.optString("image").orEmpty() }
            } else {
                optString(index)
            }
            if (value.isNotBlank()) {
                add(value)
            }
        }
    }
}
