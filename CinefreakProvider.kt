package com.cinefreak

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class CinefreakProvider : MainAPI() {
    override var mainUrl = "https://cinefreak.ch"
    override var name = "Cinefreak"
    override var lang = "de"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(mainUrl).document
        val items = doc.select("article, div.post-item").mapNotNull {
            val title = it.selectFirst("h2, h3")?.text() ?: return@mapNotNull null
            val href = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val poster = fixUrlNull(it.selectFirst("img")?.attr("src"))
            newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
        }
        return newHomePageResponse(HomePageList("Latest Movies", items))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=$query").document
        return doc.select("article, div.post-item").mapNotNull {
            val title = it.selectFirst("h2, h3")?.text() ?: return@mapNotNull null
            val href = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val poster = fixUrlNull(it.selectFirst("img")?.attr("src"))
            newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text()?.trim() ?: "Movie"
        val poster = fixUrlNull(doc.selectFirst("div.poster img, article img")?.attr("src"))
        val desc = doc.selectFirst("div.entry-content, div.description")?.text()?.trim()
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = desc
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCtor: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        doc.select("iframe[src]").forEach { iframe ->
            val videoUrl = fixUrl(iframe.attr("src"))
            loadExtractor(videoUrl, subtitleCallback, callback)
        }
        return true
    }
}
