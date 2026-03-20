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
data class SonySubtitle(
    @JsonProperty("subtitleId") val subtitleId: String? = null,
    @JsonProperty("subtitleType") val subtitleType: String? = null,
    @JsonProperty("subtitleDisplayName") val subtitleDisplayName: String? = null,
    @JsonProperty("subtitleLanguageName") val subtitleLanguageName: String? = null,
    @JsonProperty("subtitleUrl") val subtitleUrl: String? = null,
)
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
    @JsonProperty("poster") val poster: String? = null,
    @JsonProperty("emfAttributes") val emfAttributes: SonyEmfAttributes? = null,
    @JsonProperty("contentId") val contentId: Long? = null,
    @JsonProperty("objectType") val objectType: String? = null,
    @JsonProperty("contentType") val contentType: String? = null,
    @JsonProperty("season") val season: Int? = null,
    @JsonProperty("card_name") val cardName: String? = null,  // ADD
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
    @JsonProperty("id") val id: Any? = null,  // API returns both String and Int IDs
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("layout") val layout: String? = null,
    @JsonProperty("metadata") val metadata: SonyMetadata? = null,
    @JsonProperty("assetMetadata") val assetMetadata: SonyMetadata? = null,
    @JsonProperty("editorialMetadata") val editorialMetadata: SonyMetadata? = null,
    @JsonProperty("actions") val actions: List<SonyAction>? = null,
    @JsonProperty("assets") val assets: SonyAssets? = null,
    @JsonDeserialize(using = FlexibleContainerListDeserializer::class)
    @JsonProperty("containers") val containers: List<SonyContainer>? = null,
    @JsonProperty("retrieveItems") val retrieveItems: SonyRetrieveItems? = null,
    @JsonProperty("episodeCount") val episodeCount: Int? = null,
    @JsonProperty("seasonCount") val seasonCount: Int? = null,
)
// The top-level search response wrapper
data class SonySearchResponse(
    @JsonProperty("resultCode") val resultCode: String? = null,
    @JsonProperty("resultObj")  val resultObj: SonySearchResultObj? = null,
)

data class SonySearchResultObj(
    @JsonProperty("total")      val total: Int? = null,
    @JsonProperty("containers") val containers: List<SonySearchOuterContainer>? = null,
)

// containers[0] — the outer shell
data class SonySearchOuterContainer(
    @JsonProperty("containers") val containers: List<SonySearchTab>? = null,
)

// containers[0].containers[0] — the "All" tab (or "Videos" etc.)
data class SonySearchTab(
    @JsonProperty("tab")    val tab: String? = null,
    @JsonProperty("tabKey") val tabKey: String? = null,
    @JsonProperty("title")  val title: String? = null,
    @JsonProperty("total")  val total: Int? = null,
    @JsonProperty("assets") val assets: List<SonySearchAsset>? = null,
)

// Each item in assets[]
data class SonySearchAsset(
    // ID fields — use contentId as the video ID for VOD calls
    @JsonProperty("id")              val id: Any? = null,
    @JsonProperty("contentId")       val contentId: Long? = null,
    @JsonProperty("GGID")            val ggId: Any? = null,          // bundleId for movies

    // Type discriminators — TOP LEVEL, not in metadata
    @JsonProperty("objectSubtype")   val objectSubtype: String? = null,   // "MOVIE", "SHOW", "EPISODIC_SHOW"
    @JsonProperty("contentCategory") val contentCategory: String? = null, // "MOVIES", "TV_SHOW", "ORIGINALS", "SPORTS"

    // Display
    @JsonProperty("title")           val title: String? = null,
    @JsonProperty("isEncrypted")     val isEncrypted: Boolean? = null,
    @JsonProperty("isLive")          val isLive: Boolean? = null,

    // Bundle parent — for movies, parents[0].parentId is the MOVIE_BUNDLE id
    @JsonProperty("parents")         val parents: List<SonySearchParent>? = null,

    // The nested metadata (for thumbnails)
    @JsonProperty("metadata")        val metadata: SonySearchMetadata? = null,
)

data class SonySearchParent(
    @JsonProperty("parentId")      val parentId: Long? = null,
    @JsonProperty("parentType")    val parentType: String? = null,    // "BUNDLE"
    @JsonProperty("parentSubType") val parentSubType: String? = null, // "MOVIE_BUNDLE"
)

data class SonySearchMetadata(
    @JsonProperty("emfAttributes") val emfAttributes: SonyEmfAttributes? = null,
    @JsonProperty("objectType")    val objectType: String? = null,
    @JsonProperty("objectSubtype") val objectSubtype: String? = null,
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
    @JsonProperty("subtitle") val subtitle: List<SonySubtitle>? = null,
    @JsonProperty("LA_Details") val laDetails: SonyLaDetails? = null,
    @JsonProperty("LA_ID")      val laId: String? = null,
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
data class SonyLaDetails(
    @JsonProperty("laURL")   val laURL: String? = null,
    @JsonProperty("drm")     val drm: String? = null,
    @JsonProperty("isDummy") val isDummy: Boolean? = null,
)


// ─── Provider ─────────────────────────────────────────────────────────────────

class SonyLivProvider : MainAPI() {

    override var mainUrl        = "https://www.sonyliv.com"
    override var name           = "SonyLIV"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang           = "hi"
    override val hasMainPage    = true

    private val apiBase      = "https://apiv2.sonyliv.com"  // token/ULD/stream endpoints
    private val apiBase3     = "https://apiv3.sonyliv.com"  // detail/bundle/episode endpoints
    private val appVersion   = "3.5.8"
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
        "x-playback-session-id" to sessionId,
        "X-Via-Device"    to "true",
        "Session_id"      to sessionId,
        "Security_token"  to securityToken,
        "Device_id"       to sessionId,
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
    private suspend fun getdeviceid(): String {
            return sessionId
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

    /** Safely get container ID as String regardless of whether API returned Int or String. */
    private fun SonyContainer.idStr() = id?.toString()

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
        "35211_7935" to "New on Liv",
        "38048_7123"  to "SET Shows",
        "38048_8417"  to "Sony SAB Shows",
        "38048_7131"  to "SET Classics",
        // Movies sub-trays  (page id 7745)
        "47570_22200"  to "Movies",
        "47570_22204" to "Liv Movies",
        // Sports
        "39379_24064"  to "Sports",
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
    // trayId format: "collectionId_itemId" e.g. "1111_9013870"
    // Shows use PAGE-V2/{trayId}, movies use EXTCOLLECTION/{itemId}
    val parts = trayId.split("_")
    
    val pageUrl = "$apiBase/AGL/4.8/R/ENG/WEB/IN/MH/PAGE-V2/$trayId?kids_safe=false&from=0&to=30"

    val pageResp = app.get(pageUrl, headers = buildHeaders())
    val pageData = parseJson<SonyResponse>(pageResp.text)

    val results = mutableListOf<SearchResponse>()

    // EXTCOLLECTION: items are directly in resultObj.containers[0].assets.containers
    // PAGE-V2: same path — resultObj.containers[0].assets.containers
    val tray  = pageData.resultObj?.containers?.firstOrNull() ?: return emptyList()
    val items = tray.assets?.containers ?: return emptyList()

    items.forEach { item ->
        val meta  = item.metadata ?: return@forEach
        val stype = meta.objectSubtype ?: meta.contentSubtype ?: ""

        if (stype == "LAUNCHER") {
            // Movies tray: real data is in assetMetadata + editorialMetadata
            val ameta = item.assetMetadata ?: return@forEach
            val emeta = item.editorialMetadata ?: return@forEach
            val title = ameta.title?.takeIf { it.isNotBlank() }
                ?: emeta.poster  // fallback unlikely needed
                ?: return@forEach
            val mid   = ameta.contentId?.toString() ?: return@forEach
            val thumb = emeta.poster
            results.add(
                newMovieSearchResponse(title, "MOVIE::$mid", TvType.Movie) {
                    this.posterUrl = thumb
                }
            )
            return@forEach
        }

        // Shows tray: normal path
        val title = meta.title?.takeIf { it.isNotBlank() } ?: return@forEach
        val sid   = item.idStr() ?: return@forEach
        val thumb = item.bestThumb()

        when (stype) {
            "SHOW", "EPISODIC_SHOW", "GROUP_OF_BUNDLES" -> results.add(
                newTvSeriesSearchResponse(title, "SHOW::$sid", TvType.TvSeries) {
                    this.posterUrl = thumb
                }
            )
            "MOVIE", "MOVIE_BUNDLE" -> results.add(
                newMovieSearchResponse(title, "MOVIE::$sid", TvType.Movie) {
                    this.posterUrl = thumb
                }
            )
            else -> results.add(
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
    val url = "$apiBase3/AGL/4.8/A/ENG/WEB/IN/MH/TRAY/SEARCH" +
        "?query=query.encoded&from=0&to=20&tabs=1&kids_safe=false"
    val resp = app.get(url, headers = buildHeaders())
    val data = parseJson<SonySearchResponse>(resp.text)

    val results = mutableListOf<SearchResponse>()

    val allTab = data.resultObj?.containers
        ?.firstOrNull()
        ?.containers
        ?.firstOrNull { it.tab == "All" || it.tabKey == "All" }
        ?: data.resultObj?.containers?.firstOrNull()?.containers?.firstOrNull()

    val items = allTab?.assets ?: return results

    items.forEach { asset ->
        val title = asset.title ?: return@forEach
        val emf   = asset.metadata?.emfAttributes
        val thumb = emf?.portraitThumb ?: emf?.thumbnail ?: emf?.landscapeThumb

        // objectSubtype and contentCategory are TOP-LEVEL on the asset
        when (asset.objectSubtype) {
            "MOVIE" -> {
                // Use contentId as the video ID, GGID as the bundle ID
                val vid = asset.contentId?.toString() ?: return@forEach
                results.add(newMovieSearchResponse(title, "MOVIE::$vid", TvType.Movie) {
                    this.posterUrl = thumb
                })
            }
            "SHOW", "GROUP_OF_BUNDLES", "EPISODIC_SHOW" -> {
                val sid = asset.contentId?.toString() ?: return@forEach
                results.add(newTvSeriesSearchResponse(title, "SHOW::$sid", TvType.TvSeries) {
                    this.posterUrl = thumb
                })
            }
            "MOVIE_BUNDLE" -> {
                // Standalone bundle acting as a movie
                val sid = asset.contentId?.toString() ?: return@forEach
                results.add(newMovieSearchResponse(title, "MOVIE::$sid", TvType.Movie) {
                    this.posterUrl = thumb
                })
            }
            // Skip TRAILER, live channels, etc.
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
            "SHOW"   -> loadShow(id)
            "MOVIE"  -> loadMovie(id)
            "BUNDLE" -> loadBundle(id)
            else     -> loadShow(id)
        }
    }
    private suspend fun urlExists(url: String): Boolean {
    return try {
        val resp = app.head(url, headers = buildHeaders())
        resp.isSuccessful
    } catch (e: Exception) {
        false
    }
    }
     /**
     * Load a show. Handles three show types:
     *
     * 1. GROUP_OF_BUNDLES (e.g. TMKOC) — no direct children in DETAIL response.
     *    Must call CONTENT/DETAIL/BUNDLE/{showId} to get EPISODE_RANGE bundles,
     *    then show the latest bundle's episodes via TRAY/SEARCH/VOD?filter_parentId={bundleId}
     *
     * 2. SHOW with direct EPISODE children (e.g. single-season shows)
     *    DETAIL returns episodes directly in containers[0].containers
     *
     * 3. SHOW with SEASON children (multi-season shows like Shark Tank)
     *    Each season is drillable via SHOW::seasonId
     */
    private suspend fun loadShow(sid: String): TvSeriesLoadResponse? {
        val detailUrl  = "$apiBase3/AGL/4.8/A/ENG/WEB/IN/MH/DETAIL/$sid"
        val detailResp = app.get(detailUrl, headers = buildHeaders())
        val detailData = parseJson<SonyResponse>(detailResp.text)

        if (detailData.resultCode != "OK") return null

        val showContainer = detailData.resultObj?.containers?.firstOrNull() ?: return null
        val showMeta      = showContainer.metadata
        val showEmf       = showMeta?.emfAttributes
        val showTitle     = showMeta?.title ?: "SonyLIV"
        val showPlot      = showMeta?.longDescription ?: ""
        val showPoster    = showEmf?.let { it.portraitThumb ?: it.poster ?: it.thumbnail } ?: ""
        val showYear      = showMeta?.year
        val objectType    = showMeta?.objectType ?: showMeta?.contentType ?: ""

        val episodes = mutableListOf<Episode>()

        when {
            // ── Type 1: GROUP_OF_BUNDLES (TMKOC-style, 100s of episodes in range buckets)
            // The DETAIL response already contains the bundle list in
            // containers[0].containers[] — each has layout="BUNDLE_ITEM",
            // contentSubtype="EPISODE_RANGE", and its own bundle ID (NOT the show ID).
            objectType == "GROUP_OF_BUNDLES" -> {
                val bundles = showContainer.containers ?: emptyList()

                bundles.forEach { bundle ->
                    // Bundle id is e.g. 1590014677 — NOT the show id
                    val bundleId    = bundle.idStr() ?: return@forEach
                    val tkmocjsonurl="https://raw.githubusercontent.com/PurveshPatelYu/TestPlugins/refs/heads/master/SonyLivProvider/$bundleId.json"
                    if (sid=="1700000084" && urlExists(tkmocjsonurl)){
                        val rawJson = app.get(tkmocjsonurl).text
                        val data = parseJson<List<Map<String, Any>>>(rawJson)
                        data.forEach { item ->
                            episodes.add(newEpisode("PLAY::${item["id"]}") {
                                this.name        = item["name"] as? String
                                this.episode     = (item["ep"] as? Double)?.toInt()
                                this.season      = (item["sn"] as? Double)?.toInt()
                                this.posterUrl   = item["url"] as? String
                                this.description = item["desc"] as? String
                                this.runTime     = (item["time"] as? Double)?.toInt()
                            })
                        }
                        return@forEach
                    }
                    else{
                    val bundleTitle = bundle.metadata?.title ?: bundleId  // e.g. "4601-4700"
                    val seasonNum   = bundle.metadata?.season
                    val bundleThumb = bundle.metadata?.emfAttributes?.let {
                        it.portraitThumb ?: it.thumbnail ?: it.landscapeThumb
                    } ?: showPoster
                    val bundleMetaUrl  = "$apiBase3/AGL/2.6/A/ENG/WEB/IN/MH/CONTENT/DETAIL/BUNDLE/$bundleId?from=0&to=100&orderBy=episodeNumber&sortOrder=desc&kids_safe=false"
                    val bundleMetaResp = app.get(bundleMetaUrl, headers = buildHeaders())
                    val bundleMetaData = parseJson<SonyResponse>(bundleMetaResp.text)
                    val bundleContainer = bundleMetaData.resultObj?.containers?.firstOrNull()
                    val items = bundleContainer?.containers ?: emptyList()
                    items.forEach { item ->
                        val meta    = item.metadata ?: return@forEach
                        val itemId  = item.idStr() ?: return@forEach
                        val emf     = meta.emfAttributes
                        val thumb   = emf?.let { it.landscapeThumb ?: it.thumbnail } ?: bundleThumb
                        val epNum   = meta.episodeNumber
                        val epTitle = meta.episodeTitle?.takeIf { it.isNotBlank() }
                            ?: meta.title?.takeIf { it.isNotBlank() }
                            ?: "Ep $epNum"

                        episodes.add(newEpisode("PLAY::$itemId") {
                            this.name        = epTitle
                            this.episode     = epNum
                            this.season      = seasonNum
                            this.posterUrl   = thumb
                            this.description = meta.longDescription
                            this.runTime     = meta.duration?.div(60)
                        })
                    }
                    }
                }
            }


            else -> {
                val subtype = showMeta?.contentSubtype
                val itemId  = showContainer.idStr()
                val emf     = showMeta?.emfAttributes
                val thumb   = emf?.let { it.portraitThumb ?: it.poster ?: it.landscapeThumb ?: it.thumbnail } ?: showPoster

                when (subtype) {
                    "EPISODE" -> {
                        val epTitle = showMeta?.episodeTitle?.takeIf { it.isNotBlank() }
                            ?: "Ep ${showMeta?.episodeNumber}"
                        episodes.add(newEpisode("PLAY::$itemId") {
                            this.name        = epTitle
                            this.episode     = showMeta?.episodeNumber
                            this.season      = showMeta?.season
                            this.posterUrl   = thumb
                            this.description = showMeta?.longDescription
                            this.runTime     = showMeta?.duration?.div(60)
                        })
                    }
                    else -> {
                        val epTitle = showMeta?.episodeTitle?.takeIf { it.isNotBlank() }
                            ?: showMeta?.title ?: showTitle
                        episodes.add(newEpisode("PLAY::$itemId") {
                            this.name        = epTitle
                            this.posterUrl   = thumb
                            this.description = showMeta?.longDescription
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
     * Load individual episodes from an EPISODE_RANGE bundle.
     * Uses TRAY/SEARCH/VOD?filter_parentId={bundleId} — the same endpoint the web app uses.
     * Dispatched when user taps a "4601-4700" bundle row (data = "BUNDLE::bundleId").
     */
    /**
     * Load episodes inside a bundle (e.g. "1-100", "4601-4700").
     *
     * The CONTENT/DETAIL/BUNDLE endpoint only returns bundle metadata — NOT episodes.
     * Episodes are fetched from the TRAY/SEARCH/VOD endpoint using filter_parentId.
     * Response structure: resultObj.containers[] — each is a VOD episode directly.
     */
    private suspend fun loadBundle(bundleId: String): TvSeriesLoadResponse? {
        // Get bundle metadata (title like "4601-4700", season number, poster)
        val bundleMetaUrl  = "$apiBase3/AGL/2.6/A/ENG/WEB/IN/MH/CONTENT/DETAIL/BUNDLE/$bundleId"
        val bundleMetaResp = app.get(bundleMetaUrl, headers = buildHeaders())
        val bundleMetaData = parseJson<SonyResponse>(bundleMetaResp.text)
        val bundleContainer = bundleMetaData.resultObj?.containers?.firstOrNull()
        val bundleMeta      = bundleContainer?.metadata
        val bundleTitle     = bundleMeta?.title ?: bundleId
        val bundleSeason    = bundleMeta?.season
        val bundlePoster    = bundleMeta?.emfAttributes?.let {
            it.portraitThumb ?: it.thumbnail ?: it.landscapeThumb
        } ?: ""

        // Fetch episodes using the retrieveItems URI pattern from the bundle
        // URI: /TRAY/SEARCH/VOD?filter_parentId={bundleId}&filter_contentType=VOD
        val epUrl = "$apiBase3/AGL/2.6/A/ENG/WEB/IN/MH/TRAY/SEARCH/VOD" +
                "?filter_parentId=$bundleId&filter_contentType=VOD" +
                "&from=0&to=100&orderBy=episodeNumber&sortOrder=asc&kids_safe=false"
        val epResp = app.get(epUrl, headers = buildHeaders())
        val epData = parseJson<SonyResponse>(epResp.text)

        val episodes = mutableListOf<Episode>()

        // TRAY/SEARCH/VOD response: episodes are directly in resultObj.containers[]
        // Each container is a VOD item with metadata.contentSubtype = "EPISODE"
        val items = epData.resultObj?.containers ?: emptyList()

        items.forEach { item ->
            val meta    = item.metadata ?: return@forEach
            val itemId  = item.idStr() ?: return@forEach
            val emf     = meta.emfAttributes
            val thumb   = emf?.let { it.portraitThumb ?: it.landscapeThumb ?: it.thumbnail } ?: bundlePoster
            val epNum   = meta.episodeNumber
            val epTitle = meta.episodeTitle?.takeIf { it.isNotBlank() }
                ?: meta.title?.takeIf { it.isNotBlank() }
                ?: "Ep $epNum"

            episodes.add(newEpisode("PLAY::$itemId") {
                this.name        = epTitle
                this.episode     = epNum
                this.season      = bundleSeason
                this.posterUrl   = thumb
                this.description = meta.longDescription
                this.runTime     = meta.duration?.div(60)
            })
        }

        return newTvSeriesLoadResponse(bundleTitle, bundleId, TvType.TvSeries, episodes) {
            this.posterUrl = bundlePoster
        }
    }

private suspend fun loadMovie(sid: String): LoadResponse? {
    ensureInit()
    val url = "$apiBase3/AGL/2.6/A/ENG/WEB/IN/MH/CONTENT/DETAIL/BUNDLE/$sid"
    val resp = app.get(url, headers = buildHeaders())
    val data = parseJson<SonyResponse>(resp.text)

    val container = data.resultObj?.containers?.firstOrNull() ?: return null
    val meta = container.metadata ?: return null
    val emf  = meta.emfAttributes

    // The actual playable video ID — from containers inside or from the bundle's own ID
    // In BUNDLE detail, the first container IS the video item with the real contentId
    val videoId = container.idStr() ?: sid  // fallback to bundle id itself

    val thumb = emf?.let { it.portraitThumb ?: it.poster ?: it.landscapeThumb ?: it.thumbnail }

    return newMovieLoadResponse(
        name    = meta.title ?: return null,
        url     = "PLAY::$videoId",
        type    = TvType.Movie,
        dataUrl = "PLAY::$videoId"
    ) {
        this.posterUrl   = thumb
        this.plot        = meta.longDescription
        this.year        = meta.year
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

    val result    = if (isLive) getLiveResult(vid) else getVodResult(vid)
    val streamUrl = result?.videoURL
    if (streamUrl.isNullOrEmpty()) return false

    // Subtitles
    result.subtitle?.forEach { sub ->
        val subUrl  = sub.subtitleUrl ?: return@forEach
        val subName = sub.subtitleDisplayName ?: sub.subtitleLanguageName ?: "Unknown"
        subtitleCallback.invoke(SubtitleFile(subName, subUrl))
    }

    val type = when {
        streamUrl.contains(".mpd") -> ExtractorLinkType.DASH
        else                       -> ExtractorLinkType.M3U8
    }

    val licenseUrl = result.laDetails?.laURL
    val isEncrypted = result.isEncrypted == true && licenseUrl != null

    if (isEncrypted) {
        callback.invoke(
            newDrmExtractorLink(
                source = name,
                name   = name,
                url    = streamUrl,
                type   = type,
                uuid   = WIDEVINE_UUID,
            ) {
                this.referer    = "$mainUrl/"
                this.headers    = buildHeaders()
                this.quality    = Qualities.Unknown.value
                this.licenseUrl = licenseUrl
            }
        )
    } else {
        callback.invoke(
            newExtractorLink(
                source = name,
                name   = name,
                url    = streamUrl,
                type   = type,
            ) {
                this.referer  = "$mainUrl/"
                this.headers  = buildHeaders()
                this.quality  = Qualities.Unknown.value
            }
        )
    }

    return true
}

private suspend fun getVodResult(vid: String): SonyResultObj? {
    val body = mapOf(
        "actionType"       to "play",
        "browser"          to "Chrome",
        "deviceId"         to getdeviceid(),
        "os"               to "Windows",
        "platform"         to "web",
        "hasLAURLEnabled"  to true,
        "adsParams"        to mapOf(
            "Idtype" to "uuid",
            "Is_lat" to "0",
            "ppid"   to null
        )
    )

    var url  = "$apiBase/AGL/4.8/R/ENG/WEB/IN/$stateCode/CONTENT/VIDEOURL/VOD/$vid/freepreview"
    var resp = app.post(url, headers = buildHeaders(), json = body)
    
    if (!resp.isSuccessful) {
        url  = "$apiBase/AGL/4.8/SR/ENG/WEB/IN/$stateCode/CONTENT/VIDEOURL/VOD/$vid"
        resp = app.post(url, headers = buildHeaders(), json = body)
    }
    if (!resp.isSuccessful) return null
    return parseJson<SonyResponse>(resp.text).resultObj
}

private suspend fun getLiveResult(vid: String): SonyResultObj? {
    val resp = app.get(
        "$apiBase/AGL/3.2/R/ENG/WEB/IN/ALL/CONTENT/VIDEOURL/VOD/$vid/freepreview?contactId=MSMIND",
        headers = buildHeaders()
    )
    return parseJson<SonyResponse>(resp.text).resultObj
}

    private suspend fun getVodUrl(vid: String): String? {
        var url  = "$apiBase/AGL/4.8/R/ENG/WEB/IN/$stateCode/CONTENT/VIDEOURL/VOD/$vid/freepreview"
        var resp = app.get(url, headers = buildHeaders())
        if (!resp.isSuccessful) {
            url  = "$apiBase/AGL/4.8/SR/ENG/WEB/IN/$stateCode/CONTENT/VIDEOURL/VOD/$vid"
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
