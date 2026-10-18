package com.nphkhiem.englishforyourchildren.tools.pipeline

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Reads one public URL, or null when it is not there (404). Any other failure is an
 * [IOException]: a network fault is the author's to retry, never something to record as an answer.
 */
fun interface WebSource {
    @Throws(IOException::class)
    fun get(url: String): ByteArray?
}

/**
 * The real network, through the JDK's own client. Wikimedia asks every automated client to say who
 * it is, so every request carries a User-Agent naming this tool and where it lives.
 */
class HttpWebSource : WebSource {
    private val client = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    override fun get(url: String): ByteArray? {
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
        return when (response.statusCode()) {
            OK -> response.body()
            NOT_FOUND -> null
            else -> throw IOException("HTTP ${response.statusCode()} for $url")
        }
    }

    private companion object {
        const val USER_AGENT = "HelloBe-content-pipeline/1.0 (https://github.com/nphkhiem/hello-be)"
        const val OK = 200
        const val NOT_FOUND = 404
        val TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
