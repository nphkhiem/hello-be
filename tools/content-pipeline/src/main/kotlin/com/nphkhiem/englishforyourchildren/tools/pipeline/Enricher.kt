package com.nphkhiem.englishforyourchildren.tools.pipeline

import com.nphkhiem.englishforyourchildren.content.validation.AttributionDto
import com.nphkhiem.englishforyourchildren.content.validation.AttributionsDto
import java.io.File
import java.net.URLEncoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** What [Enricher] added, and what it could not, for a person to finish by hand. */
data class EnrichResult(val added: List<String>, val notes: List<String>)

/**
 * Fills in the recordings an authored course names and nobody has made yet, from Wiktionary's
 * pronunciations on Wikimedia Commons.
 *
 * Only the words a child chooses between, because those are what a dictionary has: a sentence
 * prompt is not in one, and stays a person's to record. A US recording is preferred, being the
 * accent the rest of the course is voiced in.
 *
 * The dictionary is used to find a recording and nothing else. Its own licence covers its text, not
 * the recording, so every recording's author and licence are read from its own Commons file page,
 * and the original file is downloaded unchanged. A byte-identical copy is the licensed work itself,
 * with no derivative to declare. Only free licences are taken: public domain, CC0, CC BY and
 * CC BY-SA. Anything else is left for a person to decide.
 *
 * Pictures are not fetched. Commons supplies photographs, and the approved brief calls for
 * commissioned illustration.
 */
class Enricher(private val web: WebSource) {
    private val json = Json { ignoreUnknownKeys = true }
    private val writer = Json { prettyPrint = true }

    fun enrich(source: File): EnrichResult {
        val bundle = CourseBundle(
            source.walkTopDown()
                .filter { it.isFile && !it.name.startsWith('.') }
                .associate { it.relativeTo(source).invariantSeparatorsPath to it.readBytes() }
        )
        val wanted = bundle.units.asSequence()
            .flatMap { it.lessons }
            .flatMap { it.activities }
            .flatMap { it.choices }
            .filter { it.audio !in bundle.mediaIds }
            .associate { it.audio to it.label }

        val added = mutableListOf<String>()
        val notes = mutableListOf<String>()
        val credits = bundle.attributions.entries.toMutableList()
        wanted.forEach { (assetId, word) ->
            when (val found = recordingFor(word)) {
                is Lookup.Found -> {
                    val file =
                        File(source, "${CourseBundle.MEDIA}/audio/$assetId.${found.extension}")
                    file.parentFile.mkdirs()
                    file.writeBytes(found.bytes)
                    credits += found.credit(assetId)
                    added += "$assetId from ${found.title}"
                }

                is Lookup.Missing -> notes += "$assetId (\"$word\"): ${found.why}"
            }
        }

        if (added.isNotEmpty()) {
            File(source, CourseBundle.ATTRIBUTIONS).writeText(
                writer.encodeToString(
                    AttributionsDto.serializer(),
                    bundle.attributions.copy(entries = credits)
                )
            )
        }
        return EnrichResult(added, notes)
    }

    private fun recordingFor(word: String): Lookup {
        val entries = web.get(DICTIONARY + URLEncoder.encode(word, Charsets.UTF_8))
            ?: return Lookup.Missing("no dictionary entry")
        val phonetics = json.parseToJsonElement(entries.decodeToString()).jsonArray
            .flatMap { it.jsonObject["phonetics"]?.jsonArray.orEmpty() }
            .map { it.jsonObject }
            .filter { it.text("audio").isNotBlank() && "curid=" in it.text("sourceUrl") }
        val chosen = phonetics.firstOrNull { "-us." in it.text("audio") }
            ?: phonetics.firstOrNull()
            ?: return Lookup.Missing("the dictionary has no recording")

        val pageId = chosen.text("sourceUrl").substringAfter("curid=")
        val page = web.get(COMMONS + pageId)
            ?.let { pageOf(json.parseToJsonElement(it.decodeToString())) }
            ?: return Lookup.Missing("its Commons file could not be read")
        val free = isFree(page.licence)
        if (!free) return Lookup.Missing("licensed ${page.licence}, which is not free")
        val playable = page.extension in PLAYABLE
        if (!playable) return Lookup.Missing("a .${page.extension} file the app cannot play")

        val bytes = web.get(page.fileUrl) ?: return Lookup.Missing("its file is gone from Commons")
        return Lookup.Found(bytes, page.extension, page.title, page.author, page.licence)
    }

    private fun pageOf(response: JsonElement): CommonsFile? {
        val page = response.jsonObject["query"]?.jsonObject?.get("pages")?.jsonObject?.values
            ?.firstOrNull()?.jsonObject ?: return null
        val info = (page["imageinfo"] as? JsonArray)?.firstOrNull()?.jsonObject ?: return null
        val meta = info["extmetadata"]?.jsonObject ?: return null
        fun field(name: String) = meta[name]?.jsonObject?.text("value").orEmpty()
        val title = page.text("title")
        return CommonsFile(
            title = title,
            fileUrl = info.text("url").substringBefore('?'),
            extension = title.substringAfterLast('.').lowercase(),
            author = authorOf(field("Artist")),
            licence = field("LicenseShortName")
        )
    }

    private data class CommonsFile(
        val title: String,
        val fileUrl: String,
        val extension: String,
        val author: String,
        val licence: String
    )

    private sealed interface Lookup {
        class Found(
            val bytes: ByteArray,
            val extension: String,
            val title: String,
            val author: String,
            val licence: String
        ) : Lookup {
            fun credit(assetId: String): AttributionDto {
                val name = title.removePrefix("File:")
                return AttributionDto(
                    assetId = assetId,
                    kind = "audio",
                    provenance = "licensed",
                    source = "$author, via Wikimedia Commons",
                    licence = licence,
                    licenceEvidence = COMMONS_PAGE + title.replace(' ', '_'),
                    attributionText = "\"$name\" by $author, $licence, via Wikimedia Commons"
                )
            }
        }

        class Missing(val why: String) : Lookup
    }

    private companion object {
        const val DICTIONARY = "https://api.dictionaryapi.dev/api/v2/entries/en/"
        const val COMMONS = "https://commons.wikimedia.org/w/api.php?action=query&format=json" +
            "&prop=imageinfo&iiprop=url%7Cextmetadata&pageids="
        const val COMMONS_PAGE = "https://commons.wikimedia.org/wiki/"

        /** What the app's audio locator can find and Media3 can play. */
        val PLAYABLE = setOf("m4a", "ogg", "mp3")

        fun JsonObject.text(name: String): String = this[name]?.jsonPrimitive?.content.orEmpty()

        /**
         * The author's name out of Commons' HTML: the linked name when there is one, as in the
         * common "Dvortygirl assumed" credit, otherwise the text with its markup removed.
         */
        fun authorOf(html: String): String =
            Regex("<a [^>]*>([^<]+)</a>").find(html)?.groupValues?.get(1)?.trim()
                ?: html.replace(Regex("<[^>]+>"), "").trim()

        /** Free to store, redistribute and credit. Non-commercial and no-derivatives are not. */
        fun isFree(licence: String): Boolean {
            val name = licence.uppercase()
            if ("NC" in name.split(' ', '-') || "ND" in name.split(' ', '-')) return false
            return name.startsWith("CC BY") || name.startsWith("CC0") ||
                name.startsWith("PUBLIC DOMAIN")
        }
    }
}
