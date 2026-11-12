package demo.app.analysisapp.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

// 小虫 API 聚合解析接口配置
//主token
internal const val API_TOKEN = "自己去申请"
//子密钥（不能用的）
//internal const val API_TOKEN = "api-c5ba36cfab09ec5f49ee085c8d6189b651dac54cbac1f37c"
internal const val API_ENDPOINT = "https://api.ovo1.cc/api/analysis"
internal val DouyinUrlRegex = Regex("""https?://[^\s\]\)）>]+""")

/** 从分享文案中提取抖音链接 */
internal fun extractDouyinUrl(text: String): String? {
    return DouyinUrlRegex.findAll(text)
        .map { match ->
            match.value.trimEnd(
                '.',
                ',',
                '，',
                '。',
                ':',
                '：',
                ';',
                '；',
                ')',
                '）',
                ']',
                '】'
            )
        }
        .firstOrNull { url ->
            url.contains("douyin.com", ignoreCase = true)
        }
}

/** 请求小虫 API 解析抖音作品 */
internal fun requestAnalysis(douyinUrl: String): ParseResult {
    val encodedUrl = URLEncoder.encode(douyinUrl, Charsets.UTF_8.name())
    val requestUrl = "$API_ENDPOINT?token=$API_TOKEN&url=$encodedUrl"
    val connection = URL(requestUrl).openConnection() as HttpURLConnection

    return try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept", "application/json")

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: throw IOException("接口请求失败：HTTP $responseCode")
        }

        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        parseApiResponse(body)
    } finally {
        connection.disconnect()
    }
}

/** 解析小虫 API 返回的 JSON */
internal fun parseApiResponse(body: String): ParseResult {
    val root = JSONObject(body)
    val code = root.optInt("code", -1)
    if (code != 200) {
        throw IOException(root.optString("message", "解析失败，接口返回 code=$code"))
    }

    val data = root.optJSONObject("data") ?: throw IOException("接口没有返回作品数据")
    val rawType = data.optString("内容类型")

    val authorInfo = data.optJSONObject("作者信息")
    val extraInfo = data.optJSONObject("扩展信息")
    val videoUrl = data.optString("视频直链").ifBlank {
        data.optJSONArray("视频备用链接")
            ?.optJSONObject(0)
            ?.optString("url")
            .orEmpty()
    }
    val imageUrls = data.optJSONArray("图片列表").toStringList()
    val musicUrl = data.optJSONObject("背景音乐")?.optString("音乐链接").orEmpty()

    val kind = when {
        videoUrl.isNotBlank() -> ContentKind.Video
        imageUrls.isNotEmpty() -> ContentKind.Images
        else -> ContentKind.Unsupported
    }

    return ParseResult(
        kind = kind,
        rawType = rawType.ifBlank { "未知" },
        title = data.optString("标题"),
        authorName = authorInfo?.optString("昵称").orEmpty(),
        coverUrl = data.optString("封面图"),
        videoUrl = videoUrl,
        imageUrls = imageUrls,
        publishTime = extraInfo?.optString("发布时间").orEmpty(),
        musicUrl = musicUrl
    )
}

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
