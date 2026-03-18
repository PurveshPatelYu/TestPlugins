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
//
// SonyLIV sometimes returns a JSON object {} instead of an array [] for
// "containers" fields. This deserializer handles both transparently.

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
                // Single object — wrap in a list
                val mapper = p.codec as ObjectMapper
                listOf(mapper.readValue(p, SonyContainer::class.java))
            }
            JsonToken.VALUE_NULL -> emptyList()
            else -> emptyList()
        }
    }
}

// ─── Data classes mirroring the SonyLiv API JSON ────────────────────────────

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
    @JsonProperty("items_count") val itemsCount: Int? = null,
    @JsonProperty("label") val label: String? = null,
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("emfAttributes") val emfAttributes: SonyEmfAttributes? = null,
)

data class SonyEmfAttributes(
    @JsonProperty("masthead_background") val mastheadBackground: String? = null,
    @JsonProperty("masthead_background_mobile") val mastheadBackgroundMobile: String? = null,
    @JsonProperty("masthead_foreground") val mastheadForeground: String? = null,
    @JsonProperty("portrait_thumb") val portraitThumb: String? = null,
    @JsonProperty("masthead_logo") val mastheadLogo: String? = null,
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
)

data class SonyRetrieveItems(
    @JsonProperty("uri") val uri: String? = null,
)

data class SonyResultObj(
    @JsonDeserialize(using = FlexibleContainerListDeserializer::class)
    @JsonProperty("containers") val containers: List<SonyContainer>? = null,
    @JsonProperty("videoURL") val videoURL: String? = null,
    @JsonProperty("isEncrypted") val isEncrypted: Boolean? = null,
    @JsonProperty("laURL") val laURL: String? = null,
    @JsonProperty("signature") val signature: String? = null,
    @JsonProperty("token") val token: String? = null,
    @JsonProperty("drm") val drm: String? = null,
    @JsonProperty("state_code") val stateCode: String? = null,
    @JsonProperty("channelPartnerID") val channelPartnerID: String? = null,
    @JsonProperty("dai_asset_key") val daiAssetKey: String? = null,
    @JsonProperty("metadata") val metadata: SonyPageMetadata? = null,
)

data class SonyPageMetadata(
    @JsonProperty("page_id") val pageId: String? = null,
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

// ─── Plugin entry point ──────────────────────────────────────────────────────

class SonyLivProvider : MainAPI() {

    override var mainUrl = "https://www.sonyliv.com"
    override var name = "SonyLIV"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "hi"          // Primary language is Hindi/Indian content
    override val hasMainPage = true

    private val apiBase = "https://apiv2.sonyliv.com"
    private val appVersion = "3.5.8"
    private val deviceId = "9c7631d21edd4a65a92b2b641c8a13a2-1634808345996"
    private val xForwardedFor = "103.250.158.149"

    // Lazily initialised after first network calls
    private var securityToken: String = ""
    private var stateCode: String = "IN"
    private var channelPartnerID: String = "MSMIND"

    // Session id stays constant for the lifetime of the provider instance
    private val sessionId: String = "${UUID.randomUUID().toString().replace("-", "")}-${System.currentTimeMillis()}"

    // ── Headers ──────────────────────────────────────────────────────────────

    private fun buildHeaders(): Map<String, String> = mapOf(
        "Content-Type"    to "application/json",
        "App_version"     to appVersion,
        "Accept"          to "application/json, text/plain, */*",
        "Accept-Language" to "en-US,en;q=0.9",
        "Origin"          to "https://www.sonyliv.com",
        "Referer"         to "https://www.sonyliv.com/",
        "X-Via-Device"    to "true",
        "Session_id"      to sessionId,
        "Security_token"  to securityToken,
        "Device_id"       to deviceId,
        "X-Forwarded-For" to xForwardedFor,
        "User-Agent"      to "Mozilla/5.0 (Linux; Android 7.1.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36",
    )

    // ── Initialisation helpers ────────────────────────────────────────────────

    private suspend fun ensureInit() {
        if (securityToken.isNotEmpty()) return
        securityToken = getToken()
        val uld = getULD()
        stateCode = uld.first
        channelPartnerID = uld.second
    }

    private suspend fun getToken(): String {
        val url = "$apiBase/AGL/1.4/A/ENG/WEB/ALL/GETTOKEN"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyTokenResponse>(resp.text)
        return data.resultObj ?: ""
    }

    private suspend fun getULD(): Pair<String, String> {
        val url = "$apiBase/AGL/1.4/A/ENG/WEB/ALL/USER/ULD"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyULDResponse>(resp.text)
        val stCode = data.resultObj?.stateCode ?: "IN"
        val cpID   = data.resultObj?.channelPartnerID ?: "MSMIND"
        return Pair(stCode, cpID)
    }

    // ── Main page ─────────────────────────────────────────────────────────────

    override val mainPage = mainPageOf(
        "7738|48"  to "TV Shows",
        "7745|30"  to "Movies",
        "7750|40"  to "Sports",
        "35223|19" to "Premium",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        ensureInit()
        val (topId, pages) = request.data.split("|")
        val items = getChannelItems(topId, pages.toInt())
        return newHomePageResponse(request.name, items)
    }

    private suspend fun getChannelItems(topId: String, pages: Int): List<SearchResponse> {
        val url = "$apiBase/AGL/4.8/R/ENG/WEB/IN/MH/PAGE-V2/$topId" +
                "?kids_safe=false&from=0&to=14"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)
        val results = mutableListOf<SearchResponse>()

        // v2 PAGE response: resultObj.containers = [{ id, metadata, retrieveItems, assets:{total, containers:[...shows...]} }]
        data.resultObj?.containers?.forEach { tray ->
            val label = tray.metadata?.label ?: tray.title ?: return@forEach
            val total = tray.assets?.total ?: return@forEach
            if (total == 0) return@forEach
            val cid   = tray.id ?: tray.metadata?.id ?: return@forEach
            val thumb = tray.assets?.containers?.firstOrNull()?.metadata?.emfAttributes
                ?.let { it.portraitThumb ?: it.thumbnail } ?: ""

            results.add(
                newMovieSearchResponse(label, "CHANNEL::$cid:::$total", TvType.Movie) {
                    this.posterUrl = thumb
                }
            )
        }
        return results
    }

    // ── Search ────────────────────────────────────────────────────────────────

    override suspend fun search(query: String): List<SearchResponse> {
        ensureInit()
        val encoded = query.replace(" ", "+")
        val url = "$apiBase/AGL/2.4/A/ENG/WEB/IN/$stateCode/TRAY/SEARCH" +
                "?query=$encoded&from=0&to=9&kids_safe=true"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)

        val results = mutableListOf<SearchResponse>()
        val containers = data.resultObj?.containers ?: return results

        // Index 0 = TV Shows, Index 1 = Movies
        containers.forEachIndexed { idx, section ->
            section.containers?.forEach { item ->
                val meta  = item.metadata ?: return@forEach
                val title = meta.episodeTitle?.takeIf { it.isNotBlank() } ?: meta.title ?: return@forEach
                val sid   = item.id ?: return@forEach
                val stype = meta.objectSubtype ?: ""
                val total = item.assets?.total ?: 100
                val thumb = meta.emfAttributes?.portraitThumb
                    ?: meta.emfAttributes?.mastheadBackground ?: ""

                if (idx == 0) {
                    // TV Shows → TvSeries
                    results.add(
                        newTvSeriesSearchResponse(title, "SHOW::$sid::$stype::$total", TvType.TvSeries) {
                            this.posterUrl = thumb
                        }
                    )
                } else {
                    // Movies
                    results.add(
                        newMovieSearchResponse(title, "MOVIE::$sid:::", TvType.Movie) {
                            this.posterUrl = thumb
                        }
                    )
                }
            }
        }
        return results
    }

    // ── Load detail page ──────────────────────────────────────────────────────

    override suspend fun load(url: String): LoadResponse? {
        ensureInit()
        // Data format: "KIND::id::stype::extra"
        val parts = url.split("::")
        if (parts.isEmpty()) return null

        val kind  = parts[0]
        val id    = parts.getOrElse(1) { "" }
        val stype = parts.getOrElse(2) { "" }
        val extra = parts.getOrElse(3) { "" }

        return when (kind) {
            "CHANNEL" -> loadShowContainer(id)
            "SHOW"    -> loadShow(id)
            "MOVIE"   -> loadMovie(id)
            "PLAY"    -> null  // handled by loadLinks, not load()
            else      -> loadShow(id)
        }
    }

    private suspend fun loadShowContainer(cid: String): LoadResponse? {
        // Use the v2 PAGE endpoint that the web app actually uses
        val url = "$apiBase/AGL/4.8/R/ENG/WEB/IN/MH/PAGE-V2/$cid?kids_safe=false&from=0&to=14"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)
        val first = data.resultObj?.containers?.firstOrNull() ?: return null

        val showUri   = first.retrieveItems?.uri ?: return null
        val showTotal = first.assets?.total ?: 14

        return loadShowList(showUri, 0, showTotal, first)
    }

    private suspend fun loadShowList(
        mainUri: String,
        page: Int,
        total: Int,
        parentContainer: SonyContainer? = null
    ): TvSeriesLoadResponse? {
        val from = page * 14
        val to   = minOf(from + 13, total)
        // The retrieveItems URI looks like: /TRAY/EXTCOLLECTION/380487123?...
        // We prepend the base API path
        val url = "$apiBase/AGL/4.8/R/ENG/WEB/IN/MH$mainUri&kids_safe=false&from=$from&to=$to"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)

        val episodes = mutableListOf<Episode>()
        var title    = parentContainer?.metadata?.label ?: "SonyLIV"
        var plot     = ""
        var poster   = parentContainer?.metadata?.emfAttributes?.portraitThumb ?: ""

        // Items are inside resultObj.containers[0].assets.containers (v2 tray response)
        // OR directly in resultObj.containers (v1 tray response)
        val items = data.resultObj?.containers?.firstOrNull()?.assets?.containers
            ?: data.resultObj?.containers
            ?: return newTvSeriesLoadResponse(title, mainUri, TvType.TvSeries, episodes) {
                this.posterUrl = poster
            }

        items.forEachIndexed { idx, item ->
            val meta    = item.metadata ?: return@forEachIndexed
            val stype   = meta.objectSubtype ?: meta.contentSubtype ?: ""
            val epTitle = meta.episodeTitle?.takeIf { it.isNotBlank() } ?: meta.title ?: ""
            val sid     = item.id?.toString() ?: return@forEachIndexed
            val emf     = meta.emfAttributes

            if (idx == 0 && plot.isEmpty()) {
                plot   = meta.longDescription ?: ""
                if (poster.isEmpty()) poster = emf?.portraitThumb ?: emf?.landscapeThumb ?: ""
            }

            val epPoster = emf?.portraitThumb ?: emf?.landscapeThumb ?: emf?.thumbnail ?: ""

            when (stype) {
                "MOVIE", "SPORTS_CLIPS", "HIGHLIGHTS" -> {
                    episodes.add(newEpisode("PLAY::$sid") {
                        this.name        = epTitle
                        this.posterUrl   = epPoster
                        this.description = meta.longDescription
                    })
                }
                "SHOW", "EPISODE" -> {
                    // SHOW = TV series, drill into it
                    episodes.add(newEpisode("SHOW::$sid::") {
                        this.name      = epTitle
                        this.posterUrl = epPoster
                    })
                }
                else -> {
                    episodes.add(newEpisode("SHOW::$sid::") {
                        this.name      = epTitle
                        this.posterUrl = epPoster
                    })
                }
            }
        }

        return newTvSeriesLoadResponse(title, mainUri, TvType.TvSeries, episodes) {
            this.plot      = plot
            this.posterUrl = poster
        }
    }

    private suspend fun loadShow(sid: String): LoadResponse? {
        // Fetch season/episode breakdown
        val url = "$apiBase/AGL/4.8/R/ENG/WEB/IN/MH/DETAIL/$sid?kids_safe=false&from=0&to=14"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)

        if (data.resultCode != "OK") return null

        val containers = data.resultObj?.containers?.firstOrNull()?.containers ?: return null
        val episodes   = mutableListOf<Episode>()
        var title      = ""
        var poster     = ""
        var plot       = ""

        containers.forEachIndexed { idx, item ->
            val meta    = item.metadata ?: return@forEachIndexed
            val subtype = meta.contentSubtype ?: ""
            val stitle  = meta.episodeTitle?.takeIf { it.isNotBlank() } ?: meta.title ?: ""
            val itemSid = item.id ?: return@forEachIndexed
            val emf     = meta.emfAttributes
            val epCount = item.episodeCount ?: 100

            if (idx == 0) {
                title  = stitle
                poster = emf?.poster ?: emf?.landscapeThumb ?: ""
                plot   = meta.longDescription ?: ""
            }

            val epPoster = emf?.poster ?: emf?.landscapeThumb ?: ""

            if (subtype == "EPISODE") {
                val epName = "$stitle Episode ${meta.episodeNumber}"
                episodes.add(newEpisode("PLAY::$itemSid") {
                    this.name        = epName
                    this.episode     = meta.episodeNumber
                    this.posterUrl   = epPoster
                    this.description = meta.longDescription
                    this.runTime     = meta.duration?.div(60)
                })
            } else {
                // Season node — link through to episode list
                episodes.add(newEpisode("SHOW::$itemSid::") {
                    this.name      = stitle
                    this.posterUrl = epPoster
                })
            }
        }

        return newTvSeriesLoadResponse(title, sid, TvType.TvSeries, episodes) {
            this.plot      = plot
            this.posterUrl = poster
        }
    }

    private suspend fun loadMovie(mid: String): MovieLoadResponse? {
        val url = "$apiBase/AGL/1.4/A/ENG/WEB/IN/PAGE/$mid?from=0&to=9"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)

        val item  = data.resultObj?.containers?.firstOrNull()?.assets?.containers?.firstOrNull()
            ?: return null
        val meta  = item.metadata ?: return null
        val emf   = meta.emfAttributes
        val title = meta.episodeTitle?.takeIf { it.isNotBlank() } ?: meta.title ?: "SonyLIV"
        val plot  = buildString {
            append(meta.longDescription ?: "")
            emf?.castAndCrew?.let { append("\n\n$it") }
        }

        return newMovieLoadResponse(title, mid, TvType.Movie, "PLAY::$mid") {
            this.plot      = plot
            this.posterUrl = emf?.portraitThumb ?: emf?.thumbnail ?: ""
            this.year      = meta.year
        }
    }

    private suspend fun loadVideoList(gid: String, uri: String, total: Int): TvSeriesLoadResponse? {
        val url  = "$apiBase/AGL/1.4/A/ENG/WEB/IN/PAGE/$gid?from=0&to=9"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)

        val items    = data.resultObj?.containers?.firstOrNull()?.assets?.containers ?: return null
        val episodes = items.mapNotNull { item ->
            val meta  = item.metadata ?: return@mapNotNull null
            val title = meta.episodeTitle?.takeIf { it.isNotBlank() } ?: meta.title ?: return@mapNotNull null
            val mid   = item.id ?: return@mapNotNull null
            val emf   = meta.emfAttributes
            newEpisode("PLAY::$mid") {
                this.name        = title
                this.posterUrl   = emf?.portraitThumb ?: emf?.thumbnail ?: ""
                this.description = meta.longDescription
            }
        }

        val first = items.firstOrNull()?.metadata
        return newTvSeriesLoadResponse(
            first?.title ?: "SonyLIV", gid, TvType.TvSeries, episodes
        ) {
            this.posterUrl = first?.emfAttributes?.portraitThumb ?: ""
        }
    }

    // ── Load links ─────────────────────────────────────────────────────────────

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        ensureInit()
        val parts  = data.split("::")
        // For PLAY::vid entries go straight to stream
        // For SHOW::sid entries we need to drill into loadShow first — but
        // loadLinks is only called on playable episodes so this should always be PLAY
        val vid    = parts.getOrElse(1) { parts[0] }
        val isLive = data.contains("LIVE", ignoreCase = true)

        val streamUrl = if (isLive) getLiveUrl(vid) else getVodUrl(vid)
        if (streamUrl.isNullOrEmpty()) return false

        val url = streamUrl  // smart-cast to non-null
        val quality = when {
            url.contains("4k", ignoreCase = true)   -> Qualities.P2160.value
            url.contains("1080", ignoreCase = true) -> Qualities.P1080.value
            url.contains("720", ignoreCase = true)  -> Qualities.P720.value
            else                                     -> Qualities.Unknown.value
        }

        val type = when {
            url.contains(".mpd")                          -> ExtractorLinkType.DASH
            url.contains(".m3u8") || url.contains("hls") -> ExtractorLinkType.M3U8
            else                                          -> ExtractorLinkType.M3U8
        }

        callback.invoke(
            newExtractorLink(
                source = name,
                name   = name,
                url    = url,
            ) {
                this.referer = "https://www.sonyliv.com/"
                this.quality = quality
                this.type    = type
            }
        )
        return true
    }

    private suspend fun getVodUrl(vid: String): String? {
        // Try free preview first
        var url  = "$apiBase/AGL/3.0/R/ENG/WEB/IN/$stateCode/CONTENT/VIDEOURL/VOD/$vid/freepreview"
        var resp = app.get(url, headers = buildHeaders())

        if (!resp.isSuccessful) {
            // Fallback to subscriber endpoint
            url  = "$apiBase/AGL/3.0/SR/ENG/WEB/IN/$stateCode/CONTENT/VIDEOURL/VOD/$vid"
            resp = app.get(url, headers = buildHeaders())
        }
        if (!resp.isSuccessful) return null

        val data = parseJson<SonyResponse>(resp.text)
        return data.resultObj?.videoURL
    }

    private suspend fun getLiveUrl(vid: String): String? {
        val url  = "$apiBase/AGL/3.2/R/ENG/WEB/IN/ALL/CONTENT/VIDEOURL/VOD/$vid/freepreview?contactId=MSMIND"
        val resp = app.get(url, headers = buildHeaders())
        val data = parseJson<SonyResponse>(resp.text)
        return data.resultObj?.videoURL
    }
}
