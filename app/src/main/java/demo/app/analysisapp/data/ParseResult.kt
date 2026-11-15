package demo.app.analysisapp.data

/**
 * 作品类型
 */
internal enum class ContentKind(val label: String) {
    Video("视频"),
    Images("图文"),
    Unsupported("暂不支持")
}

/**
 * 解析结果数据
 */
internal data class ParseResult(
    val kind: ContentKind,
    val rawType: String,
    val title: String,
    val authorName: String,
    val coverUrl: String,
    /** 视频直链：云汐的 live 实况可能返回多个 */
    val videoUrls: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val publishTime: String = "",
    /** 背景音乐直链：图文 / live 合成视频时用 */
    val musicUrl: String = ""
)

/** 首个视频直链，单视频场景直接用 */
internal val ParseResult.videoUrl: String?
    get() = videoUrls.firstOrNull()

/** 是否是 live 实况（接口 内容类型 返回 live） */
internal val ParseResult.isLive: Boolean
    get() = rawType.equals("live", ignoreCase = true)

/** 界面显示的类型名称（接口返回 live 时单独标注） */
internal val ParseResult.typeLabel: String
    get() = if (isLive) "live 实况" else kind.label

/** 资源数量摘要 */
internal val ParseResult.resourceSummary: String
    get() = when (kind) {
        ContentKind.Video -> when (videoUrls.size) {
            0 -> "未返回视频直链"
            1 -> "1 个视频"
            else -> "${videoUrls.size} 个视频"
        }
        ContentKind.Images -> "${imageUrls.size} 张图片"
        ContentKind.Unsupported -> "暂不支持 $rawType"
    }

/** 是否可保存 */
internal val ParseResult.canSave: Boolean
    get() = when (kind) {
        ContentKind.Video -> videoUrls.isNotEmpty()
        ContentKind.Images -> imageUrls.isNotEmpty()
        ContentKind.Unsupported -> false
    }

/** 保存按钮文案：live 转成了视频来展示，所以按钮是「保存静态图」 */
internal val ParseResult.saveButtonText: String
    get() = when (kind) {
        ContentKind.Video -> if (videoUrls.size > 1) "保存全部视频" else "保存视频"
        ContentKind.Images -> if (isLive) "保存静态图" else "保存全部图片"
        ContentKind.Unsupported -> "暂不支持保存"
    }

/** 只有 live 实况且接口没给真实视频时，才用图片合成视频 */
internal val ParseResult.canMakeVideo: Boolean
    get() = isLive && videoUrls.isEmpty() && imageUrls.isNotEmpty()
