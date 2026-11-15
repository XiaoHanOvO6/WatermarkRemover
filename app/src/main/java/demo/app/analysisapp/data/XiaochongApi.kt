package demo.app.analysisapp.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

// 小虫 API 聚合解析接口配置
//主token
//internal const val XIAOCHONG_API_TOKEN = "自己去申请"
internal const val XIAOCHONG_API_TOKEN = "389ccd2bf6ad0aa138cbc651c090cd16"
//子密钥（不能用的）
//internal const val XIAOCHONG_API_TOKEN = "api-c5ba36cfab09ec5f49ee085c8d6189b651dac54cbac1f37c"
internal const val XIAOCHONG_API_ENDPOINT = "https://api.ovo1.cc/api/analysis"

/** 请求小虫 API 解析抖音作品 */
internal fun requestAnalysisXiaochong(douyinUrl: String): ParseResult {
    val encodedUrl = URLEncoder.encode(douyinUrl, Charsets.UTF_8.name())
    val requestUrl = "$XIAOCHONG_API_ENDPOINT?token=$XIAOCHONG_API_TOKEN&url=$encodedUrl"
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
        parseXiaochongResponse(body)
    } finally {
        connection.disconnect()
    }
}

/** 解析小虫 API 返回的 JSON */
internal fun parseXiaochongResponse(body: String): ParseResult {
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
        videoUrls = listOfNotNull(videoUrl.takeIf { it.isNotBlank() }),
        imageUrls = imageUrls,
        publishTime = extraInfo?.optString("发布时间").orEmpty(),
        musicUrl = musicUrl
    )
}
