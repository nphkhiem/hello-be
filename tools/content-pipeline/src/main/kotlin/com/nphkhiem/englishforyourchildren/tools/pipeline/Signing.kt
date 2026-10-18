package com.nphkhiem.englishforyourchildren.tools.pipeline

import java.io.File
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/**
 * The owner's Ed25519 key, per ADR 0016: making one, and signing with it.
 *
 * The private key never touches a file here. It is printed once by [generate], kept in a password
 * manager, and handed back to [sign] through the environment. The public half goes into the app as
 * the `helloBe.contentPublicKey` Gradle property, as the raw 32 bytes the television expects.
 */
object Signing {
    const val KEY_VARIABLE = "HELLO_BE_SIGNING_KEY"
    const val SIGNATURE = "manifest.json.sig"

    data class KeyPair(val publicKeyBase64: String, val privateKeyBase64: String)

    fun generate(): KeyPair {
        val pair = KeyPairGenerator.getInstance(ED25519).generateKeyPair()
        return KeyPair(
            // The JDK wraps a public key in X.509; the key itself is its last 32 bytes.
            publicKeyBase64 = encode(pair.public.encoded.takeLast(RAW_KEY_BYTES).toByteArray()),
            privateKeyBase64 = encode(pair.private.encoded)
        )
    }

    /** Signs the manifest in [output] exactly as written, beside it as [SIGNATURE]. */
    fun sign(output: File, privateKeyBase64: String) {
        val key = KeyFactory.getInstance(ED25519)
            .generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64)))
        val manifest = File(output, Publisher.MANIFEST).readBytes()
        val signature = Signature.getInstance(ED25519).run {
            initSign(key)
            update(manifest)
            sign()
        }
        File(output, SIGNATURE).writeBytes(signature)
    }

    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)

    private const val ED25519 = "Ed25519"
    private const val RAW_KEY_BYTES = 32
}
