package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Document

class MotchillProvider : MainAPI() {
    override var mainUrl = "https://motchillc.com"
    override var name = "Motchill"
    override var lang = "vi"
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)

    // 1. SEARCH FUNCTION: Finds titles matching the query
    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/tim-kiem/$query/"
        val html = app.get(url).text
        val document = org.jsoup.Jsoup.parse(html)
        
        return document.select("li.list-film-item").mapNotNull { element ->
            val title = element.selectFirst("h3")?.text() ?: return@mapNotNull null
            val filmUrl = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = element.selectFirst("img")?.attr("src") ?: ""
            val quality = element.selectFirst(".label")?.text() ?: "HD"

            MovieSearchResponse(
                name = title,
                url = fixUrl(filmUrl),
                apiName = this.name,
                type = TvType.Movie,
                posterUrl = fixUrl(poster),
                quality = getQualityFromString(quality)
            )
        }
    }

    // 2. DETAILS FUNCTION: Loads movie or episode information
    override suspend fun load(url: String): LoadResponse? {
        val html = app.get(url).text
        val document = org.jsoup.Jsoup.parse(html)

        val title = document.selectFirst("h1.title-film")?.text() ?: "Unknown Title"
        val poster = document.selectFirst(".poster-film img")?.attr("src") ?: ""
        val description = document.selectFirst(".film-content p")?.text() ?: ""
        
        val episodesList = document.select(".list-episode a")
        if (episodesList.isNotEmpty()) {
            val episodes = episodesList.map { element ->
                val epUrl = element.attr("href")
                val epName = element.text()
                Episode(
                    data = fixUrl(epUrl),
                    name = epName
                )
            }
            return TvSeriesLoadResponse(
                name = title,
                url = url,
                apiName = this.name,
                type = TvType.TvSeries,
                posterUrl = fixUrl(poster),
                plot = description,
                episodes = episodes
            )
        } else {
            val watchUrl = document.selectFirst("a.btn-watch")?.attr("href") ?: url
            return MovieLoadResponse(
                name = title,
                url = url,
                apiName = this.name,
                type = TvType.Movie,
                dataUrl = fixUrl(watchUrl),
                posterUrl = fixUrl(poster),
                plot = description
            )
        }
    }

    // 3. LINK EXTRACTION: Fetches the final streaming video link
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val html = app.get(data).text
        
        val regex = Regex("""file:\s*["'](https.*?\.m3u8.*?)["']""")
        val match = regex.find(html)
        
        val videoUrl = match?.groupValues?.get(1) ?: return false

        callback.invoke(
            ExtractorLink(
                source = this.name,
                name = "Motchill Mirror",
                url = videoUrl,
                referer = mainUrl,
                quality = Qualities.P1080.value,
                isM3u8 = true
            )
        )
        return true
    }
}
