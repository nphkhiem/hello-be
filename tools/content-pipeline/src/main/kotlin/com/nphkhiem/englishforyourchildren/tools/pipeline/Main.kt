package com.nphkhiem.englishforyourchildren.tools.pipeline

import java.io.File
import kotlin.system.exitProcess

private const val USAGE = """
Usage:
  keygen                                     make the owner's signing key, once
  enrich <course folder>                     fetch missing word recordings from Wikimedia Commons
  build <course folder> <output> [--previous <published folder>]
                                             check the course and write packs and a manifest
  sign <output>                              sign the manifest, after a person has reviewed it
                                             (reads the private key from HELLO_BE_SIGNING_KEY)
"""

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "keygen" -> keygen()
        "enrich" -> enrich(args.drop(1))
        "build" -> build(args.drop(1))
        "sign" -> sign(args.drop(1))
        else -> fail(USAGE)
    }
}

private fun keygen() {
    val key = Signing.generate()
    println("Public key, for the app (Gradle property helloBe.contentPublicKey):")
    println(key.publicKeyBase64)
    println()
    println("Private key, for your password manager only. Never commit it or put it in a file:")
    println(key.privateKeyBase64)
}

private fun enrich(args: List<String>) {
    val course = args.singleOrNull() ?: fail(USAGE)
    val result = Enricher(HttpWebSource()).enrich(File(course))
    result.added.forEach { println("  added: $it") }
    result.notes.forEach { println("  left for a person: $it") }
    println("Added ${result.added.size} recordings. Listen to each before building.")
}

private fun build(args: List<String>) {
    val previousAt = args.indexOf("--previous")
    val previous = if (previousAt >= 0) args.getOrNull(previousAt + 1) else null
    val paths = args.filterIndexed { index, _ ->
        previousAt < 0 ||
            index !in previousAt..previousAt + 1
    }
    if (paths.size != 2 || (previousAt >= 0 && previous == null)) fail(USAGE)

    when (val result = Publisher.build(File(paths[0]), File(paths[1]), previous?.let(::File))) {
        is BuildResult.Refused -> fail(
            "Not built:\n" + result.problems.joinToString("\n") {
                "  $it"
            }
        )

        is BuildResult.Built -> {
            println("Built ${result.manifest.courseVersion}: ${result.manifest.packs.size} packs.")
            result.notes.forEach { println("  note: $it") }
            println("Review it, then sign it.")
        }
    }
}

private fun sign(args: List<String>) {
    val output = args.singleOrNull() ?: fail(USAGE)
    val key = System.getenv(Signing.KEY_VARIABLE) ?: fail("${Signing.KEY_VARIABLE} is not set.")
    Signing.sign(File(output), key)
    println("Signed. Publish ${Publisher.MANIFEST}, ${Signing.SIGNATURE} and the packs together.")
}

private fun fail(message: String): Nothing {
    System.err.println(message.trim())
    exitProcess(1)
}
