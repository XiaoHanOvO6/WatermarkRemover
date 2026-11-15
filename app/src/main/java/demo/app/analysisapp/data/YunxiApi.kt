package demo.app.analysisapp.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

// 云汐 API（线路一，免密钥）
// 注意：接口只提供 http，需要在 network_security_config.xml 里放行明文
internal const val YUNXI_API_ENDPOINT = "http://yfufu.com/api/douyin/"

/** 请求云汐 API 解析抖音作品 */
internal fun requestAnalysisYunxi(douyinUrl: String): ParseResult {
    val encodedUrl = URLEncoder.encode(douyinUrl, Charsets.UTF_8.name())
    val requestUrl = "$YUNXI_API_ENDPOINT?url=$encodedUrl"
    val connection = URL(requestUrl).openConnection() as HttpURLConnection

    return try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: throw IOException("接口请求失败：HTTP $responseCode")
        }

        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        parseYunxiResponse(body)
    } finally {
        connection.disconnect()
    }
}

/** 解析云汐 API 返回的 JSON */
internal fun parseYunxiResponse(body: String): ParseResult {
    val root = JSONObject(body)
    val code = root.optInt("code", -1)
    if (code != 200) {
        throw IOException(root.optString("msg", "解析失败，接口返回 code=$code"))
    }

    val data = root.optJSONObject("data") ?: throw IOException("接口没有返回作品数据")
    val type = data.optText("type")
    val authorInfo = data.optJSONObject("author")

    val primaryVideo = data.optText("url").ifBlank {
        data.optJSONArray("video_backup").optText(0)
    }
    val imageUrls = data.optJSONArray("images").toStringList()
    // live 实况：live_photo 里每项都带一段真实视频
    val liveVideos = data.optJSONArray("live_photo").toStringListOf("video")

    val videoUrls = if (type.equals("live", ignoreCase = true)) {
        liveVideos
    } else {
        listOfNotNull(primaryVideo.takeIf { it.isNotBlank() })
    }

    val kind = when {
        videoUrls.isNotEmpty() -> ContentKind.Video
        imageUrls.isNotEmpty() -> ContentKind.Images
        else -> ContentKind.Unsupported
    }

    val rawType = when (type.lowercase()) {
        "video" -> "视频"
        "image" -> "图文"
        "live" -> "live"
        else -> type.ifBlank { "未知" }
    }

    return ParseResult(
        kind = kind,
        rawType = rawType,
        title = data.optText("title").ifBlank { data.optText("desc") },
        authorName = authorInfo?.optText("name").orEmpty(),
        coverUrl = data.optText("cover"),
        videoUrls = videoUrls,
        imageUrls = imageUrls,
        musicUrl = data.optJSONObject("music")?.optText("url").orEmpty()
    )
}

/** 从对象数组里取出指定字段，空值自动跳过 */
private fun JSONArray?.toStringListOf(field: String): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val value = optJSONObject(index)?.optText(field).orEmpty()
            if (value.isNotBlank()) {
                add(value)
            }
        }
    }
}

/**
 * Android 的 optString 会把 JSON 里的 null 读成字符串 "null"，
 * 导致「图文」的 url:null 被误判成视频直链，这里只接受真正的字符串。
 */
private fun JSONObject.optText(name: String): String {
    return opt(name) as? String ?: ""
}

private fun JSONArray?.optText(index: Int): String {
    return this?.opt(index) as? String ?: ""
}
