package com.nphkhiem.englishforyourchildren.data.pack

import com.nphkhiem.englishforyourchildren.content.validation.CurriculumJsonParser
import com.nphkhiem.englishforyourchildren.data.curriculum.CurriculumLoader
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import java.net.URI
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Builds the updater once the app knows where lessons are published and whose key signs them.
 *
 * Those two facts are the build's, not storage's, so they arrive as arguments and everything else
 * comes from the graph. The live version is read from [curriculum], the same layered course a child
 * is taught from, so "newer" always means newer than what they are actually being shown.
 */
class ContentUpdaterFactory @Inject constructor(
    private val parser: CurriculumJsonParser,
    private val loader: CurriculumLoader,
    private val packs: ContentPackStore,
    private val curriculum: CurriculumRepository
) {
    /** [publicKeyBase64] is the raw 32-byte Ed25519 key, Base64 encoded. */
    fun create(baseUrl: String, publicKeyBase64: String): ContentUpdater = ContentUpdater(
        fetcher = HttpContentFetcher(URI(baseUrl)),
        verifier = PackVerifier(Base64.getDecoder().decode(publicKeyBase64)),
        parser = parser,
        loader = loader,
        packs = packs,
        liveVersion = {
            (curriculum.observeCourse().first() as? DomainResult.Success)?.value?.version
        }
    )
}
