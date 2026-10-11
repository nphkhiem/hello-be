package com.nphkhiem.englishforyourchildren.data.pack

import com.google.crypto.tink.subtle.Ed25519Verify
import java.security.GeneralSecurityException

/**
 * Whether a manifest is the one the owner signed, per ADR 0016.
 *
 * The signature is detached and covers the manifest's exact bytes, so this checks the bytes as they
 * arrived and nothing is parsed before it has. A checksum could not stand in for it: whoever
 * controls the hosting controls the manifest, checksums included. Only a key the television already
 * holds can say a person approved what it is about to show a child.
 *
 * [publicKey] is the raw 32-byte Ed25519 key pinned in the app. Any other length is a build
 * mistake rather than something to recover from, so it fails at construction.
 */
class PackVerifier(publicKey: ByteArray) {

    private val signatureCheck = Ed25519Verify(publicKey.copyOf())

    /**
     * The manifest, vouched for, or null when the signature does not hold.
     *
     * Fails closed. A malformed signature is a refusal like any other, never an exception that a
     * caller might catch too broadly and carry on past.
     */
    fun verify(manifest: ByteArray, signature: ByteArray): SignedManifest? = try {
        signatureCheck.verify(signature, manifest)
        SignedManifest(manifest.copyOf())
    } catch (_: GeneralSecurityException) {
        null
    }
}

/**
 * Manifest bytes that passed [PackVerifier].
 *
 * Only the verifier can make one, so code that reads a manifest by taking this type cannot be
 * handed bytes that were never checked.
 */
class SignedManifest internal constructor(private val content: ByteArray) {
    /** A copy, so nothing holding one can change what was verified. */
    val bytes: ByteArray get() = content.copyOf()
}
