package com.lagradost

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID

// ─── Flexible list deserializer ──────────────────────────────────────────────
// SonyLIV sometimes returns {} instead of [] for "containers" fields.

class FlexibleContainerListDeserializer :
    StdDeserializer<List<SonyContainer>>(List::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): List<SonyContainer> {
        return when (p.currentToken) {
            JsonToken.START_ARRAY -> {
                val mapper = p.codec as ObjectMapper
                val type = mapper.typeFactory.constructCollectionType(
                    List::class.java, SonyContainer::class.java
                )
                mapper.readValue(p, type)
            }
            JsonToken.START_OBJECT -> {
                val mapper = p.codec as ObjectMapper
                listOf(mapper.readValue(p, SonyContainer::class.java))
            }
            JsonToken.VALUE_NULL -> emptyList()
            else -> emptyList()
        }
    }
}

// ─── Data classes ─────────────────────────────────────────────────────────────

data class SonyMetadata(
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("episodeTitle") val episodeTitle: String? = null,
    @JsonProperty("longDescription") val longDescription: String? = null,
    @JsonProperty("genres") val genres: List<String>? = null,
    @JsonProperty("duration") val duration: Int? = null,
    @JsonProperty("year") val year: Int? = null,
    @JsonProperty("episodeNumber") val episodeNumber: Int? = null,
    @JsonProperty("objectSubtype") val objectSubtype: String? = null,
    @JsonProperty("contentSubtype") val contentSubtype: String? = null,
    @JsonProperty("originalAirDate") val originalAirDate: Long? = null,
    @JsonProperty("contractStartDate") val contractStartDate: Long? = null,
    @JsonProperty("externalId") val externalId: String? = null,
    @JsonProperty("label") val label: String? = null,
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("emfAttributes") val emfAttributes: SonyEmfAttributes? = null,
    @JsonProperty("contentId") val contentId: Long? = null,
)

data class SonyEmfAttributes(
    @JsonProperty("masthead_background") val mastheadBackground: String? = null,
    @JsonProperty("masthead_background_mobile") val mastheadBackgroundMobile: String? = null,
    @JsonProperty("portrait_thumb") val portraitThumb: String? = null,
    @JsonProperty("landscape_thumb") val landscapeThumb: String? = null,
    @JsonProperty("thumbnail") val thumbnail: String? = null,
    @JsonProperty("tv_background_image") val tvBackgroundImage: String? = null,
    @JsonProperty("poster") val poster: String? = null,
    @JsonProperty("cast_and_crew") val castAndCrew: String? = null,
    @JsonProperty("is_preview_enabled") val isPreviewEnabled: Boolean? = null,
)

data class SonyAction(
    @JsonProperty("uri") val uri: String? = null,
)

data class SonyAssets(
    @JsonProperty("total") val total: Int? = null,
    @JsonDeserialize(using = FlexibleContainerListDeserializer::class)
    @JsonProperty("containers") val containers: List<SonyContainer>? = null,
)

data class SonyContainer(
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("metadata") val metadata: SonyMetadata? = null,
    @JsonProperty("actions") val actions: List<SonyAction>? = null,
    @JsonProperty("assets") val assets: SonyAssets? = null,
    @JsonDeserialize(using = FlexibleContainerListDeserializer::class)
    @JsonProperty("containers") val containers: List<SonyContainer>? = null,
    @JsonProperty("retrieveItems") val retrieveItems: SonyRetrieveItems? = null,
    @JsonProperty("episodeCount") val episodeCount: Int? = null,
    @JsonProperty("seasonCount") val seasonCount: Int? = null,
)

data class SonyRetrieveItems(
    @JsonProperty("uri") val uri: String? = null,
)

data class SonyResultObj(
    @JsonProperty("total") val total: Int? = null,
    @JsonDeserialize(using = FlexibleContainerListDeserializer::class)
    @JsonProperty("containers") val containers: List<SonyContainer>? = null,
    @JsonProperty("videoURL") val videoURL: String? = null,
    @JsonProperty("isEncrypted") val isEncrypted: Boolean? = null,
    @JsonProperty("laURL") val laURL: String? = null,
    @JsonProperty("state_code") val stateCode: String? = null,
    @JsonProperty("channelPartnerID") val channelPartnerID: String? = null,
    @JsonProperty("dai_asset_key") val daiAssetKey: String? = null,
)

data class SonyResponse(
    @JsonProperty("resultCode") val resultCode: String? = null,
    @JsonProperty("resultObj") val resultObj: SonyResultObj? = null,
    @JsonProperty("message") val message: String? = null,
)

data class SonyTokenResponse(
    @JsonProperty("resultObj") val resultObj: String? = null,
)

data class SonyULDResponse(
    @JsonProperty("resultObj") val resultObj: SonyULDObj? = null,
)

data class SonyULDObj(
    @JsonProperty("state_code") val stateCode: String? = null,
    @JsonProperty("channelPartnerID") val channelPartnerID: String? = null,
)

// ─── Provider ─────────────────────────────────────────────────────────────────

class SonyLivProvider : MainAPI() {

    override var mainUrl        = "https://www.sonyliv.com"
    override var name           = "SonyLIV"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang           = "hi"
    override val hasMainPage    = true

    private val apiBase      = "https://apiv2.sonyliv.com"
    private val appVersion   = "3.5.8"
    private val deviceId     = "9c7631d21edd4a65a92b2b641c8a13a2-1634808345996"
    private val xForwardedFor = "103.250.158.149"

    private var securityToken  = ""
    private var stateCode      = "IN"
    private var channelPartnerID = "MSMIND"

    private val sessionId = "${UUID.randomUUID().toString().replace("-", "")}-${System.currentTimeMillis()}"

    // ── Headers ───────────────────────────────────────────────────────────────

    private fun buildHeaders(): Map<String, String> = mapOf(
        "Content-Type"    to "application/json",
        "App_version"     to appVersion,
        "Accept"          to "application/json, text/plain, */*",
        "Accept-Language" to "en-US,en;q=0.9",
        "Origin"          to mainUrl,
        "Referer"         to "$mainUrl/",
        "X-Via-Device"    to "true",
        "Session_id"      to sessionId,
        "Security_token"  to securityToken,
        "Device_id"       to deviceId,
        "X-Forwarded-For" to xForwardedFor,
        "User-Agent"      to "Mozilla/5.0 (Linux; Android 7.1.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36",
    )

    // ── Init ──────────────────────────────────────────────────────────────────

    private suspend fun ensureInit() {
        if (securityToken.isNotEmpty()) return
        securityToken = getToken()
        val uld = getULD()
        stateCode        = uld.first
        channelPartnerID = uld.second
    }

    private suspend fun getToken(): String {
        val resp = app.get("$apiBase/AGL/1.4/A/ENG/WEB/ALL/GETTOKEN", headers = buildHeaders())
        return parseJson<SonyTokenResponse>(resp.text).resultObj ?: ""
    }

    private suspend fun getULD(): Pair<String, String> {
        val resp = app.get("$apiBase/AGL/1.4/A/ENG/WEB/ALL/USER/ULD", headers = buildHeaders())
        val obj  = parseJson<SonyULDResponse>(resp.text).resultObj
        return Pair(obj?.stateCode ?: "IN", obj?.channelPartnerID ?: "MSMIND")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Strip mainUrl prefix that CloudStream prepends to all data strings. */
    private fun String.clean() = removePrefix(mainUrl).removePrefix("/")

    private fun SonyContainer.bestThumb() =
        metadata?.emfAttributes?.let {
            it.portraitThumb ?: it.thumbnail ?: it.landscapeThumb
        } ?: ""

    // ── Main page: one row per tray ───────────────────────────────────────────
    // Each row in mainPageOf maps a tray-collection ID to a display name.
    // The tray IDs come from the PAGE-V2 response for the top-level category pages.

    override val mainPage = mainPageOf(
        // TV Shows sub-trays  (page id 7738)
        "31155_24783" to "Latest Episodes",
        "38048_7123"  to "SET Shows",
        "38048_8417"  to "Sony SAB Shows",
        "38048_8416"  to "Sony Marathi Shows",
        "38048_7131"  to "SET Classics",
        // Movies sub-trays  (page id 7745)
        "38048_7124"  to "Movies",
        "38048_7130"  to "SonyLIV Originals",
        // Sports
        "38048_7125"  to "Sports",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        ensureInit()
        // request.data is the tray collection ID e.g. "31155_24783"
        // Fetch the shows inside that tray directly
        val shows = fetchTrayShows(request.data)
        return newHomePageResponse(request.name, shows, hasNext = false)
    }

    /**
     * Fetch the list of shows/movies inside a tray collection.
     * Uses the EXTCOLLECTION tray URI derived from the tray id.
     * Returns SearchResponse items where each item is a show (not an episode).
     */
    private suspend fun fetchTrayShows(trayId: String): List<SearchResponse> {
        // First resolve the tray's retrieveItems URI via PAGE-V2
        val pageUrl  = "$apiBase/AGL/4.8/R/ENG/WEB/IN/MH/PAGE-V2/$trayId?kids_safe=false&from=0&to=1"
        val pageResp = app.get(pageUrl, headers = buildHeaders())
        val pageData = parseJson<SonyResponse>(pageResp.text)

        val tray    = pageData.resultObj?.containers?.firstOrNull() ?: return emptyList()
        val trayUri = tray.retrieveItems?.uri ?: return emptyList()

        // Fetch shows from the tray
        return fetchShowsFromTrayUri(trayUri)
    }

    /**
     * Given a tray URI like /TRAY/EXTCOLLECTION/380487123?...
     * fetch the show list and return as SearchResponse.
     */
    private suspend fun fetchShowsFromTrayUri(trayUri: String, from: Int = 0, to: Int = 20): List<SearchResponse> {
        val url  = "$apiBase/AGL/4.8/R/ENG/WEB/IN/MH$trayUri&kids_safe=false&from=$from&to=$to"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)

        val results = mutableListOf<SearchResponse>()

        // v2 tray: items are in resultObj.containers[0].assets.containers
        val items = data.resultObj?.containers?.firstOrNull()?.assets?.containers
            ?: data.resultObj?.containers
            ?: return emptyList()

        items.forEach { item ->
            val meta   = item.metadata ?: return@forEach
            val title  = meta.title?.takeIf { it.isNotBlank() } ?: return@forEach
            val sid    = item.id?.toString() ?: return@forEach
            val thumb  = item.bestThumb()
            val stype  = meta.objectSubtype ?: meta.contentSubtype ?: ""

            when (stype) {
                "SHOW" -> results.add(
                    newTvSeriesSearchResponse(title, "SHOW::$sid", TvType.TvSeries) {
                        this.posterUrl = thumb
                    }
                )
                "MOVIE" -> results.add(
                    newMovieSearchResponse(title, "MOVIE::$sid", TvType.Movie) {
                        this.posterUrl = thumb
                    }
                )
                else -> results.add(
                    // Default to TvSeries for unknown subtypes (clips, highlights, etc.)
                    newTvSeriesSearchResponse(title, "SHOW::$sid", TvType.TvSeries) {
                        this.posterUrl = thumb
                    }
                )
            }
        }
        return results
    }

    // ── Search ────────────────────────────────────────────────────────────────

    override suspend fun search(query: String): List<SearchResponse> {
        ensureInit()
        val encoded = query.replace(" ", "+")
        val url  = "$apiBase/AGL/2.4/A/ENG/WEB/IN/$stateCode/TRAY/SEARCH?query=$encoded&from=0&to=20&kids_safe=false"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)

        val results = mutableListOf<SearchResponse>()
        val containers = data.resultObj?.containers ?: return results

        containers.forEachIndexed { idx, section ->
            section.containers?.forEach { item ->
                val meta  = item.metadata ?: return@forEach
                val title = meta.title?.takeIf { it.isNotBlank() } ?: return@forEach
                val sid   = item.id ?: return@forEach
                val thumb = item.bestThumb()
                val stype = meta.objectSubtype ?: ""

                if (idx == 0 || stype == "SHOW") {
                    results.add(newTvSeriesSearchResponse(title, "SHOW::$sid", TvType.TvSeries) {
                        this.posterUrl = thumb
                    })
                } else {
                    results.add(newMovieSearchResponse(title, "MOVIE::$sid", TvType.Movie) {
                        this.posterUrl = thumb
                    })
                }
            }
        }
        return results
    }

    // ── Load ──────────────────────────────────────────────────────────────────
    // Flow:
    //   SHOW::sid   → loadShow(sid)  → TvSeriesLoadResponse with episodes list
    //   MOVIE::sid  → loadMovie(sid) → MovieLoadResponse
    //   PLAY::sid   → should not reach load(), only loadLinks()

    override suspend fun load(url: String): LoadResponse? {
        ensureInit()
        val data  = url.clean()
        val parts = data.split("::")
        val kind  = parts.getOrElse(0) { "" }
        val id    = parts.getOrElse(1) { "" }

        if (id.isBlank()) return null

        return when (kind) {
            "SHOW"  -> loadShow(id)
            "MOVIE" -> loadMovie(id)
            else    -> loadShow(id)
        }
    }

    /**
     * Load a show's season/episode list.
     * DETAIL endpoint returns seasons (or directly episodes for single-season shows).
     */
    private suspend fun loadShow(sid: String): TvSeriesLoadResponse? {
        val url  = "$apiBase/AGL/4.8/R/ENG/WEB/IN/MH/DETAIL/$sid?kids_safe=false&from=0&to=20"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)

        if (data.resultCode != "OK") return null

        val showContainer = data.resultObj?.containers?.firstOrNull() ?: return null
        val showMeta      = showContainer.metadata
        val showEmf       = showMeta?.emfAttributes

        val showTitle  = showMeta?.title ?: "SonyLIV"
        val showPlot   = showMeta?.longDescription ?: ""
        val showPoster = showEmf?.let { it.portraitThumb ?: it.poster ?: it.thumbnail } ?: ""
        val showYear   = showMeta?.year

        val children = showContainer.containers ?: emptyList()
        val episodes = mutableListOf<Episode>()

        // Detect if children are season-bundle nodes (e.g. "4601-4700") vs real episodes.
        // Season bundles have contentSubtype = "SEASON" and their episodeTitle looks like "N-M"
        // or their title is a range. We fetch all individual episodes from each bundle.
        val firstSubtype = children.firstOrNull()?.metadata?.contentSubtype ?: ""
        val isSeasonBundles = firstSubtype == "SEASON"

        if (isSeasonBundles) {
            // Each child is a bundle — fetch actual episodes from each bundle in parallel
            // and flatten into a single episode list sorted by episode number
            children.forEach { bundle ->
                val bundleId = bundle.id ?: return@forEach
                val bundleEpisodes = fetchBundleEpisodes(bundleId, showTitle, showPoster)
                episodes.addAll(bundleEpisodes)
            }
            // Sort by episode number ascending so Ep 1 is first
            episodes.sortBy { it.episode ?: Int.MAX_VALUE }
        } else {
            // Direct children: either individual episodes or named seasons (multi-season show)
            children.forEach { item ->
                val meta    = item.metadata ?: return@forEach
                val subtype = meta.contentSubtype ?: meta.objectSubtype ?: ""
                val itemId  = item.id ?: return@forEach
                val emf     = meta.emfAttributes
                val thumb   = emf?.let { it.portraitThumb ?: it.poster ?: it.landscapeThumb ?: it.thumbnail } ?: ""

                when (subtype) {
                    "EPISODE" -> {
                        val epTitle = meta.episodeTitle?.takeIf { it.isNotBlank() }
                            ?: "Ep ${meta.episodeNumber}"
                        episodes.add(newEpisode("PLAY::$itemId") {
                            this.name        = epTitle
                            this.episode     = meta.episodeNumber
                            this.posterUrl   = thumb
                            this.description = meta.longDescription
                            this.runTime     = meta.duration?.div(60)
                        })
                    }
                    "SEASON", "SHOW" -> {
                        // Named season (e.g. "Season 1", "Season 2") — drill into it
                        val seasonTitle = meta.title?.takeIf { it.isNotBlank() } ?: "Season"
                        episodes.add(newEpisode("SHOW::$itemId") {
                            this.name      = seasonTitle
                            this.season    = meta.episodeNumber
                            this.posterUrl = thumb
                        })
                    }
                    else -> {
                        val epTitle = meta.episodeTitle?.takeIf { it.isNotBlank() }
                            ?: meta.title ?: showTitle
                        episodes.add(newEpisode("PLAY::$itemId") {
                            this.name        = epTitle
                            this.posterUrl   = thumb
                            this.description = meta.longDescription
                        })
                    }
                }
            }
        }

        return newTvSeriesLoadResponse(showTitle, sid, TvType.TvSeries, episodes) {
            this.plot      = showPlot
            this.posterUrl = showPoster
            this.year      = showYear
            this.tags      = showMeta?.genres
        }
    }

    /**
     * Fetch individual episodes from a season bundle node.
     * A bundle is a DETAIL container whose children are actual EPISODEs.
     */
    private suspend fun fetchBundleEpisodes(
        bundleId: String,
        showTitle: String,
        showPoster: String
    ): List<Episode> {
        val url  = "$apiBase/AGL/4.8/R/ENG/WEB/IN/MH/DETAIL/$bundleId?kids_safe=false&from=0&to=100"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)

        if (data.resultCode != "OK") return emptyList()

        val bundleContainer = data.resultObj?.containers?.firstOrNull() ?: return emptyList()
        val children        = bundleContainer.containers ?: return emptyList()
        val episodes        = mutableListOf<Episode>()

        children.forEach { item ->
            val meta   = item.metadata ?: return@forEach
            val itemId = item.id ?: return@forEach
            val emf    = meta.emfAttributes
            val thumb  = emf?.let { it.portraitThumb ?: it.landscapeThumb ?: it.thumbnail } ?: showPoster

            // Individual episode inside a bundle
            val epNum   = meta.episodeNumber
            val epTitle = meta.episodeTitle?.takeIf { it.isNotBlank() }
                ?: "Ep $epNum"

            episodes.add(newEpisode("PLAY::$itemId") {
                this.name        = epTitle
                this.episode     = epNum
                this.posterUrl   = thumb
                this.description = meta.longDescription
                this.runTime     = meta.duration?.div(60)
            })
        }
        return episodes
    }

    private suspend fun loadMovie(mid: String): MovieLoadResponse? {
        val url  = "$apiBase/AGL/4.8/R/ENG/WEB/IN/MH/DETAIL/$mid?kids_safe=false&from=0&to=1"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)

        val container = data.resultObj?.containers?.firstOrNull() ?: return null
        val meta      = container.metadata ?: return null
        val emf       = meta.emfAttributes
        val title     = meta.title ?: "SonyLIV"
        val plot      = buildString {
            append(meta.longDescription ?: "")
            emf?.castAndCrew?.let { append("\n\n$it") }
        }

        return newMovieLoadResponse(title, mid, TvType.Movie, "PLAY::$mid") {
            this.plot      = plot
            this.posterUrl = emf?.let { it.portraitThumb ?: it.poster ?: it.thumbnail } ?: ""
            this.year      = meta.year
            this.tags      = meta.genres
        }
    }

    // ── Load links ────────────────────────────────────────────────────────────

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        ensureInit()
        val clean  = data.clean()
        val parts  = clean.split("::")
        val vid    = parts.getOrElse(1) { clean }
        val isLive = clean.contains("LIVE", ignoreCase = true)

        val streamUrl = if (isLive) getLiveUrl(vid) else getVodUrl(vid)
        if (streamUrl.isNullOrEmpty()) return false

        val quality = when {
            streamUrl.contains("4k",   ignoreCase = true) -> Qualities.P2160.value
            streamUrl.contains("1080", ignoreCase = true) -> Qualities.P1080.value
            streamUrl.contains("720",  ignoreCase = true) -> Qualities.P720.value
            else                                           -> Qualities.Unknown.value
        }

        val type = when {
            streamUrl.contains(".mpd")  -> ExtractorLinkType.DASH
            else                        -> ExtractorLinkType.M3U8
        }

        callback.invoke(
            newExtractorLink(source = name, name = name, url = streamUrl) {
                this.referer = "$mainUrl/"
                this.quality = quality
                this.type    = type
            }
        )
        return true
    }

    private suspend fun getVodUrl(vid: String): String? {
        var url  = "$apiBase/AGL/3.0/R/ENG/WEB/IN/$stateCode/CONTENT/VIDEOURL/VOD/$vid/freepreview"
        var resp = app.get(url, headers = buildHeaders())
        if (!resp.isSuccessful) {
            url  = "$apiBase/AGL/3.0/SR/ENG/WEB/IN/$stateCode/CONTENT/VIDEOURL/VOD/$vid"
            resp = app.get(url, headers = buildHeaders())
        }
        if (!resp.isSuccessful) return null
        return parseJson<SonyResponse>(resp.text).resultObj?.videoURL
    }

    private suspend fun getLiveUrl(vid: String): String? {
        val resp = app.get(
            "$apiBase/AGL/3.2/R/ENG/WEB/IN/ALL/CONTENT/VIDEOURL/VOD/$vid/freepreview?contactId=MSMIND",
            headers = buildHeaders()
        )
        return parseJson<SonyResponse>(resp.text).resultObj?.videoURL
    }
}
