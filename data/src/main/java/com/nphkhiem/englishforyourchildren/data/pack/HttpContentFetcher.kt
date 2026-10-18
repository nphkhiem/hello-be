package com.nphkhiem.englishforyourchildren.data.pack

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI

/**
 * Published files over HTTPS, from one host.
 *
 * HTTPS keeps the bytes private in transit and nothing more: whether they can be trusted is the
 * signature's question, per ADR 0016. Every path resolves against [base] and must stay on its host,
 * so a manifest can only ever point a television at the place the app was built to read from.
 *
 * The platform's own connection rather than an HTTP library: a handful of GET requests for static
 * files does not justify a dependency.
 */
class HttpContentFetcher(private val base: URI) : ContentFetcher {

    init {
        require(base.scheme == HTTPS) { "Lessons are only fetched over HTTPS" }
    }

    override fun open(path: String): InputStream {
        val target = base.resolve(path)
        if (target.scheme != HTTPS || target.host != base.host) {
            throw IOException("Refusing to fetch $path from outside ${base.host}")
        }
        val connection = target.toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        val status = connection.responseCode
        if (status != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            throw IOException("HTTP $status for $path")
        }
        return connection.inputStream
    }

    private companion object {
        const val HTTPS = "https"
        const val TIMEOUT_MILLIS = 15_000
    }
}
