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
    @JsonProperty("emfAttributes") val emfAttributes: SonyEmfAttributes? = null,
    @JsonProperty("contentId") val contentId: Long? = null,
    @JsonProperty("objectType") val objectType: String? = null,
    @JsonProperty("contentType") val contentType: String? = null,
    @JsonProperty("season") val season: Int? = null,
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
    @JsonProperty("subtitle") val subtitle: List<SonySubtitle>? = null,
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

    private val apiBase      = "https://apiv2.sonyliv.com"  // token/ULD/stream endpoints
    private val apiBase3     = "https://apiv3.sonyliv.com"  // detail/bundle/episode endpoints
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
        "x-playback-session-id" to sessionId,
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
        "38048_7123"  to "SET Shows",
        "38048_8417"  to "Sony SAB Shows",
        "38048_7131"  to "SET Classics",
        // Movies sub-trays  (page id 7745)
        "1111_9013870"  to "Movies",
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
        // First resolve the tray's retrieveItems URI via PAGE-V2
        val pageUrl  = "$apiBase/AGL/4.8/R/ENG/WEB/IN/MH/PAGE-V2/$trayId?kids_safe=false&from=0&to=30"
        val pageResp = app.get(pageUrl, headers = buildHeaders())
        val pageData = parseJson<SonyResponse>(pageResp.text)

        val tray    = pageData.resultObj?.containers?.firstOrNull() ?: return emptyList()
        val results = mutableListOf<SearchResponse>()

        // v2 tray: items are in resultObj.containers[0].assets.containers
        val items = tray?.containers?: return emptyList()

        items.forEach { item ->
            val meta   = item.metadata ?: return@forEach
            val title  = meta.title?.takeIf { it.isNotBlank() } ?: return@forEach
            val sid    = item.idStr() ?: return@forEach
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
                val sid   = item.idStr() ?: return@forEach
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
            "SHOW"   -> loadShow(id)
            "MOVIE"  -> loadMovie(id)
            "BUNDLE" -> loadBundle(id)
            else     -> loadShow(id)
        }
    }

    /**
     * Load a show's season/episode list.
     * DETAIL endpoint returns seasons (or directly episodes for single-season shows).
     */
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
                    val tkmocjsonurl="https://raw.githubusercontent.com/PurveshPatelYu/TestPlugins/refs/heads/master/SonyLivProvider"
                            if (bundleId == "1500000212") {
            val jsonUrl = "$tkmocjsonurl/1500000212.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000269") {
            val jsonUrl = "$tkmocjsonurl/1500000269.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000270") {
            val jsonUrl = "$tkmocjsonurl/1500000270.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000279") {
            val jsonUrl = "$tkmocjsonurl/1500000279.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000281") {
            val jsonUrl = "$tkmocjsonurl/1500000281.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000282") {
            val jsonUrl = "$tkmocjsonurl/1500000282.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000535") {
            val jsonUrl = "$tkmocjsonurl/1500000535.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000536") {
            val jsonUrl = "$tkmocjsonurl/1500000536.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000537") {
            val jsonUrl = "$tkmocjsonurl/1500000537.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000538") {
            val jsonUrl = "$tkmocjsonurl/1500000538.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000539") {
            val jsonUrl = "$tkmocjsonurl/1500000539.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000540") {
            val jsonUrl = "$tkmocjsonurl/1500000540.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000541") {
            val jsonUrl = "$tkmocjsonurl/1500000541.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000284") {
            val jsonUrl = "$tkmocjsonurl/1500000284.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000662") {
            val jsonUrl = "$tkmocjsonurl/1500000662.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000663") {
            val jsonUrl = "$tkmocjsonurl/1500000663.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000664") {
            val jsonUrl = "$tkmocjsonurl/1500000664.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000286") {
            val jsonUrl = "$tkmocjsonurl/1500000286.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000287") {
            val jsonUrl = "$tkmocjsonurl/1500000287.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000288") {
            val jsonUrl = "$tkmocjsonurl/1500000288.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000289") {
            val jsonUrl = "$tkmocjsonurl/1500000289.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000210") {
            val jsonUrl = "$tkmocjsonurl/1500000210.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000542") {
            val jsonUrl = "$tkmocjsonurl/1500000542.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000290") {
            val jsonUrl = "$tkmocjsonurl/1500000290.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000291") {
            val jsonUrl = "$tkmocjsonurl/1500000291.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000292") {
            val jsonUrl = "$tkmocjsonurl/1500000292.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000293") {
            val jsonUrl = "$tkmocjsonurl/1500000293.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000294") {
            val jsonUrl = "$tkmocjsonurl/1500000294.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000295") {
            val jsonUrl = "$tkmocjsonurl/1500000295.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500000296") {
            val jsonUrl = "$tkmocjsonurl/1500000296.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500001057") {
            val jsonUrl = "$tkmocjsonurl/1500001057.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500002324") {
            val jsonUrl = "$tkmocjsonurl/1500002324.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500002600") {
            val jsonUrl = "$tkmocjsonurl/1500002600.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500002866") {
            val jsonUrl = "$tkmocjsonurl/1500002866.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500003057") {
            val jsonUrl = "$tkmocjsonurl/1500003057.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500003247") {
            val jsonUrl = "$tkmocjsonurl/1500003247.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500003536") {
            val jsonUrl = "$tkmocjsonurl/1500003536.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500003817") {
            val jsonUrl = "$tkmocjsonurl/1500003817.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500004046") {
            val jsonUrl = "$tkmocjsonurl/1500004046.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500005265") {
            val jsonUrl = "$tkmocjsonurl/1500005265.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500005539") {
            val jsonUrl = "$tkmocjsonurl/1500005539.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500005836") {
            val jsonUrl = "$tkmocjsonurl/1500005836.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1500006110") {
            val jsonUrl = "$tkmocjsonurl/1500006110.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1590013170") {
            val jsonUrl = "$tkmocjsonurl/1590013170.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1590014343") {
            val jsonUrl = "$tkmocjsonurl/1590014343.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }

        if (bundleId == "1590014561") {
            val jsonUrl = "$tkmocjsonurl/1590014561.json"
            val rawJson = app.get(jsonUrl).text
            val data = parseJson<List<Map<String, Any>>>(rawJson)

            data.forEach { item ->
                episodes.add(newEpisode("PLAY::${item["id"]}") {
                    this.name        = item["name"] as? String
                    this.episode     = (item["ep"] as? Double)?.toInt()
                    this.season      = (item["sn"] as? Double)?.toInt()
                    posterUrl   = item["url"] as? String
                    description = item["desc"] as? String
                    this.runTime     = (item["time"] as? Double)?.toInt()
                })
            }
            return@forEach
        }
        
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

            // ── Type 2 & 3: regular SHOW — children are direct episodes or named seasons
            else -> {
                val children = showContainer.containers ?: emptyList()
                children.forEach { item ->
                    val meta    = item.metadata ?: return@forEach
                    val subtype = meta.contentSubtype ?: meta.objectSubtype ?: ""
                    val itemId  = item.idStr() ?: return@forEach
                    val emf     = meta.emfAttributes
                    val thumb   = emf?.let { it.portraitThumb ?: it.poster ?: it.landscapeThumb ?: it.thumbnail } ?: showPoster

                    when (subtype) {
                        "EPISODE" -> {
                            val epTitle = meta.episodeTitle?.takeIf { it.isNotBlank() }
                                ?: "Ep ${meta.episodeNumber}"
                            episodes.add(newEpisode("PLAY::$itemId") {
                                this.name        = epTitle
                                this.episode     = meta.episodeNumber
                                this.season      = meta.season
                                this.posterUrl   = thumb
                                this.description = meta.longDescription
                                this.runTime     = meta.duration?.div(60)
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

    private suspend fun loadMovie(mid: String): MovieLoadResponse? {
        val url  = "$apiBase3/AGL/4.8/A/ENG/WEB/IN/MH/DETAIL/$mid?kids_safe=false&from=0&to=1"
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

    val result = if (isLive) getLiveResult(vid) else getVodResult(vid)
    val streamUrl = result?.videoURL
    if (streamUrl.isNullOrEmpty()) return false

    // ── Subtitles ──────────────────────────────────────────────────────
    result.subtitle?.forEach { sub ->
        val subUrl  = sub.subtitleUrl ?: return@forEach
        val subName = sub.subtitleDisplayName ?: sub.subtitleLanguageName ?: "Unknown"
        subtitleCallback.invoke(SubtitleFile(subName, subUrl))
    }

    // ── Stream ─────────────────────────────────────────────────────────
    val type = when {
        streamUrl.contains(".mpd") -> ExtractorLinkType.DASH
        else                       -> ExtractorLinkType.M3U8
    }

    callback.invoke(
        newExtractorLink(source = name, name = name, url = streamUrl) {
            this.referer = "$mainUrl/"
            this.headers = buildHeaders()
            this.type    = type
        }
    )
    return true
}

private suspend fun getVodResult(vid: String): SonyResultObj? {
    var url  = "$apiBase/AGL/3.0/R/ENG/WEB/IN/$stateCode/CONTENT/VIDEOURL/VOD/$vid/freepreview"
    var resp = app.get(url, headers = buildHeaders())
    if (!resp.isSuccessful) {
        url  = "$apiBase/AGL/3.0/SR/ENG/WEB/IN/$stateCode/CONTENT/VIDEOURL/VOD/$vid"
        resp = app.get(url, headers = buildHeaders())
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
