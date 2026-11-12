package demo.app.analysisapp.data

import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL

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
