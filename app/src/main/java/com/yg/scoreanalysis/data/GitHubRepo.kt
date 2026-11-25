package com.yg.scoreanalysis.data

/** 仓库坐标与下载镜像清单 */
object GitHubRepo {
    const val OWNER = "AceovoeL"
    const val NAME = "ScoreAnalysis"

    /** releases 页面，用于「查看页面」按钮 */
    const val RELEASES_PAGE = "https://github.com/$OWNER/$NAME/releases"

    /** latest release 的 API 地址（官方） */
    const val API_LATEST =
        "https://api.github.com/repos/$OWNER/$NAME/releases/latest"

    /**
     * 国内镜像前缀，按顺序尝试；镜像同样可代理 api.github.com 与 release 资产地址。
     * 顺序按实测可用性排列，全部失败后回退官方地址直连。
     */
    val MIRRORS = listOf(
        "https://gh-proxy.com/",
        "https://ghproxy.cfd/",
        "https://gh-proxy.net/",
        "https://ghfast.top/",
    )
}
