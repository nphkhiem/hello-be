package com.nphkhiem.englishforyourchildren.data.pack

import java.io.IOException
import java.net.URI
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Where the fetcher will and will not go. Nothing here touches the network: every refusal happens
 * before a connection is opened, which is the point.
 */
class HttpContentFetcherTest {

    private val fetcher = HttpContentFetcher(URI("https://lessons.example/hello-be/"))

    @Test
    fun givenAPlainHttpAddress_whenTheFetcherIsBuilt_thenItIsRefused() {
        assertThrows<IllegalArgumentException> {
            HttpContentFetcher(URI("http://lessons.example/"))
        }
    }

    @Test
    fun givenAPathToAnotherHost_whenOpened_thenItIsRefusedWithoutConnecting() {
        assertThrows<IOException> { fetcher.open("https://elsewhere.example/packs/u01.zip") }
    }

    @Test
    fun givenAPathDowngradingToHttp_whenOpened_thenItIsRefusedWithoutConnecting() {
        assertThrows<IOException> { fetcher.open("http://lessons.example/hello-be/manifest.json") }
    }
}
