package com.ascend.lifeos

import com.ascend.lifeos.data.finance.BankLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

/**
 * BankLink-Kern offline: JWT-Aufbau/Signatur (RS256) und PEM-Parsing müssen
 * stehen, BEVOR der erste echte Enable-Banking-Call passiert — ein kaputtes
 * Token äußert sich sonst nur als nichtssagendes 401.
 */
class BankLinkTest {

    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private fun b64d(s: String): ByteArray = Base64.getUrlDecoder().decode(s)

    @Test
    fun jwtHasThreePartsAndSignatureVerifies() {
        val jwt = BankLink.buildJwt("app-123", keyPair.private, nowSec = 1_700_000_000L)
        val parts = jwt.split(".")
        assertEquals(3, parts.size)

        val header = String(b64d(parts[0]))
        assertTrue(header.contains("\"RS256\""))
        assertTrue(header.contains("\"kid\":\"app-123\""))

        val payload = String(b64d(parts[1]))
        assertTrue(payload.contains("\"iss\":\"enablebanking.com\""))
        assertTrue(payload.contains("\"aud\":\"api.enablebanking.com\""))
        assertTrue(payload.contains("\"iat\":1700000000"))
        assertTrue(payload.contains("\"exp\":1700003600")) // iat + 3600

        val verifier = Signature.getInstance("SHA256withRSA").apply {
            initVerify(keyPair.public)
            update("${parts[0]}.${parts[1]}".toByteArray())
        }
        assertTrue("RS256-Signatur muss mit dem Public Key verifizieren", verifier.verify(b64d(parts[2])))
    }

    @Test
    fun jwtIsUrlSafeWithoutPadding() {
        val jwt = BankLink.buildJwt("app-123", keyPair.private)
        assertTrue(jwt.none { it == '=' || it == '+' || it == '/' })
    }

    @Test
    fun parsePemRoundtrip() {
        val pem = buildString {
            append("-----BEGIN PRIVATE KEY-----\n")
            append(Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.private.encoded))
            append("\n-----END PRIVATE KEY-----\n")
        }
        val parsed = BankLink.parsePem(pem)
        assertTrue(keyPair.private.encoded.contentEquals(parsed.encoded))
    }

    @Test
    fun categorizeMapsGermanMerchants() {
        assertEquals("Food", BankLink.categorize("REWE Markt GmbH", income = false))
        assertEquals("Food", BankLink.categorize("Lieferando.de", income = false))
        assertEquals("Transport", BankLink.categorize("DB Vertrieb GmbH", income = false))
        assertEquals("Fun", BankLink.categorize("Spotify AB", income = false))
        assertEquals("Clothes", BankLink.categorize("Zalando SE", income = false))
        assertEquals("Income", BankLink.categorize("Arbeitgeber Gehalt", income = true))
        assertEquals("Other", BankLink.categorize("Unbekannter Händler XY", income = false))
    }
}
