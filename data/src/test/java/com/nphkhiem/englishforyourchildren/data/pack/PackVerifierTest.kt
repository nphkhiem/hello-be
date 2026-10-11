package com.nphkhiem.englishforyourchildren.data.pack

import com.google.common.truth.Truth.assertThat
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import org.junit.jupiter.api.Test

/**
 * Signed by the JDK's own Ed25519, verified by the app's.
 *
 * Deliberately two implementations. The content pipeline signs on a build machine with the JDK, and
 * the television verifies with Tink, so a test that used one library for both would pass while the
 * two disagreed about key or signature encoding.
 */
class PackVerifierTest {

    private val owner = KeyPairGenerator.getInstance(ED25519).generateKeyPair()
    private val verifier = PackVerifier(rawPublicKey(owner))

    @Test
    fun givenAManifestTheOwnerSigned_whenVerified_thenItIsAccepted() {
        val signed = verifier.verify(MANIFEST, sign(MANIFEST, owner))

        assertThat(signed?.bytes).isEqualTo(MANIFEST)
    }

    @Test
    fun givenAManifestChangedAfterSigning_whenVerified_thenItIsRefused() {
        // The test that protects a child: one byte moved, a lesson the owner never approved.
        val signature = sign(MANIFEST, owner)
        val tampered = MANIFEST.copyOf().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }

        assertThat(verifier.verify(tampered, signature)).isNull()
    }

    @Test
    fun givenAManifestSignedBySomeoneElse_whenVerified_thenItIsRefused() {
        val stranger = KeyPairGenerator.getInstance(ED25519).generateKeyPair()

        assertThat(verifier.verify(MANIFEST, sign(MANIFEST, stranger))).isNull()
    }

    @Test
    fun givenASignatureThatIsNotOne_whenVerified_thenItIsRefusedRatherThanThrown() {
        // A truncated download of the signature file, or an empty one.
        assertThat(verifier.verify(MANIFEST, sign(MANIFEST, owner).copyOf(10))).isNull()
        assertThat(verifier.verify(MANIFEST, ByteArray(0))).isNull()
    }

    @Test
    fun givenAVerifiedManifest_whenTheArrayItCameFromChanges_thenWhatWasVerifiedDoesNot() {
        val received = MANIFEST.copyOf()
        val signed = verifier.verify(received, sign(received, owner))

        received.fill(0)

        assertThat(signed?.bytes).isEqualTo(MANIFEST)
    }

    private fun sign(bytes: ByteArray, key: KeyPair): ByteArray =
        Signature.getInstance(ED25519).run {
            initSign(key.private)
            update(bytes)
            sign()
        }

    /** The JDK hands keys out X.509-wrapped; the key itself is the last 32 bytes. */
    private fun rawPublicKey(key: KeyPair): ByteArray =
        key.public.encoded.takeLast(RAW_KEY_BYTES).toByteArray()

    private companion object {
        const val ED25519 = "Ed25519"
        const val RAW_KEY_BYTES = 32
        val MANIFEST = """{"schemaVersion":1,"courseVersion":"2026.10","packs":[]}""".toByteArray()
    }
}
