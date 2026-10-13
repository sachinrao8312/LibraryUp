package ai.sach.libraryup.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.GeneralSecurityException

class BackupCryptoTest {

    // Low iteration count keeps the test fast; production uses DEFAULT_ITERATIONS.
    private val iterations = 1_000
    private val plain = "छात्र सूची, payments, attendance".toByteArray() + ByteArray(5000) { it.toByte() }

    @Test
    fun `password and recovery code both open the file`() {
        val code = BackupCrypto.newRecoveryCode()
        val enc = BackupCrypto.encrypt(plain, listOf("library@123", BackupCrypto.normalizeRecoveryCode(code)), iterations)
        assertTrue(BackupCrypto.isEncrypted(enc))
        assertArrayEquals(plain, BackupCrypto.decrypt(enc, "library@123"))
        assertArrayEquals(plain, BackupCrypto.decrypt(enc, BackupCrypto.normalizeRecoveryCode(code.lowercase())))
    }

    @Test
    fun `wrong secret is reported as such`() {
        val enc = BackupCrypto.encrypt(plain, listOf("right"), iterations)
        try {
            BackupCrypto.decrypt(enc, "wrong")
            fail("expected WrongSecretException")
        } catch (e: BackupCrypto.WrongSecretException) {
            // expected
        }
    }

    @Test
    fun `tampered payload fails authentication`() {
        val enc = BackupCrypto.encrypt(plain, listOf("pw"), iterations)
        enc[enc.size - 20] = (enc[enc.size - 20].toInt() xor 1).toByte()
        try {
            BackupCrypto.decrypt(enc, "pw")
            fail("expected failure")
        } catch (e: GeneralSecurityException) {
            assertFalse(e is BackupCrypto.WrongSecretException)
        }
    }

    @Test
    fun `plain zip is not mistaken for an encrypted file`() {
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0, 0)
        assertFalse(BackupCrypto.isEncrypted(zipHeader))
        try {
            BackupCrypto.decrypt(zipHeader, "pw")
            fail("expected NotEncryptedException")
        } catch (e: BackupCrypto.NotEncryptedException) {
            // expected
        }
    }

    @Test
    fun `same input encrypts differently every time`() {
        val a = BackupCrypto.encrypt(plain, listOf("pw"), iterations)
        val b = BackupCrypto.encrypt(plain, listOf("pw"), iterations)
        assertNotEquals(a.toList(), b.toList())
    }

    @Test
    fun `recovery code format`() {
        val code = BackupCrypto.newRecoveryCode()
        assertTrue(Regex("^[A-Z2-9]{5}(-[A-Z2-9]{5}){4}$").matches(code))
        assertEquals(25, BackupCrypto.normalizeRecoveryCode(" ${code.lowercase()} ").length)
    }
}
