package demo.app.analysisapp.data

// 解析线路：线路一云汐（免密钥），线路二小虫（需 token）
internal enum class ApiChannel(val label: String) {
    Line1("线路一"),
    Line2("线路二")
}

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

/** 按当前选择的线路解析抖音作品 */
internal fun requestAnalysis(douyinUrl: String, channel: ApiChannel): ParseResult {
    return when (channel) {
        ApiChannel.Line1 -> requestAnalysisYunxi(douyinUrl)
        ApiChannel.Line2 -> requestAnalysisXiaochong(douyinUrl)
    }
}
