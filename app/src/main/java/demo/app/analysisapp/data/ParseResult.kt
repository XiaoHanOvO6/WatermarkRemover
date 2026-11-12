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
    val videoUrl: String?,
    val imageUrls: List<String>,
    val publishTime: String,
    /** 背景音乐直链：图文 / live 合成视频时用 */
    val musicUrl: String = ""
)

/** 界面显示的类型名称（接口返回 live 时单独标注） */
internal val ParseResult.typeLabel: String
    get() = if (rawType.equals("live", ignoreCase = true)) "live 实况" else kind.label

/** 资源数量摘要 */
internal val ParseResult.resourceSummary: String
    get() = when (kind) {
        ContentKind.Video -> if (videoUrl.isNullOrBlank()) "未返回视频直链" else "1 个视频"
        ContentKind.Images -> "${imageUrls.size} 张图片"
        ContentKind.Unsupported -> "暂不支持 $rawType"
    }

/** 是否可保存 */
internal val ParseResult.canSave: Boolean
    get() = when (kind) {
        ContentKind.Video -> !videoUrl.isNullOrBlank()
        ContentKind.Images -> imageUrls.isNotEmpty()
        ContentKind.Unsupported -> false
    }

/** 保存按钮文案 */
internal val ParseResult.saveButtonText: String
    get() = when (kind) {
        ContentKind.Video -> "保存视频"
        ContentKind.Images -> "保存静态图"
        ContentKind.Unsupported -> "暂不支持保存"
    }

/** 是否可以合成视频（图文 / live 才有图片可合成） */
internal val ParseResult.canMakeVideo: Boolean
    get() = kind == ContentKind.Images && imageUrls.isNotEmpty()
