package com.artchknd.tryit.weather

import com.artchknd.tryit.BuildConfig
import java.net.URL

/**
 * Builds the request URL for a CWA open data call.
 *
 * - If a CWA key is compiled in (debug builds with a key in local.properties),
 *   the call goes straight to CWA.
 * - Otherwise, including every release build, the call goes through the
 *   proxy, which adds the key on the server.
 *
 * [query] is a ready-made query string without a leading "?" or "&".
 */
internal object CwaEndpoint {

    private const val CWA_BASE = "https://opendata.cwa.gov.tw/api/v1/rest/datastore/"

    fun build(dataset: String, query: String): URL {
        val key = BuildConfig.CWA_API_KEY
        return if (key.isNotBlank()) {
            URL("$CWA_BASE$dataset?Authorization=$key&$query")
        } else {
            val base = BuildConfig.PROXY_BASE_URL.trimEnd('/')
            URL("$base/api/cwa?dataset=$dataset&$query")
        }
    }
}