package ai.sach.libraryup.domain

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-protected backups.
 *
 * The backup is encrypted with a random 256-bit data key (AES-GCM). That key is stored
 * in the file twice ("slots"), each copy wrapped with a key derived from a secret: the
 * owner's password and a one-time recovery code. Either secret opens the file, so a
 * forgotten password is not the end of the library's records.
 *
 * File layout (all integers big-endian):
 * ```
 * "LUENC1"                      6 bytes magic
 * kdf                           1 byte   1 = PBKDF2-HMAC-SHA256, 2 = PBKDF2-HMAC-SHA1
 * iterations                    4 bytes
 * slotCount                     1 byte
 * slot × slotCount:  salt 16 | iv 12 | wrappedKey 48 (32 key + 16 tag)
 * dataIv                        12 bytes
 * ciphertext + tag              rest
 * ```
 * SHA1 exists only because Android 7.x (API 24–25) lacks PBKDF2WithHmacSHA256; the
 * algorithm used is recorded so any device can open any file.
 */
object BackupCrypto {

    class WrongSecretException : GeneralSecurityException("Wrong password or recovery code")
    class NotEncryptedException : GeneralSecurityException("Not an encrypted LibraryUp backup")

    private val MAGIC = "LUENC1".toByteArray(Charsets.US_ASCII)
    const val DEFAULT_ITERATIONS = 310_000
    private const val KDF_SHA256: Byte = 1
    private const val KDF_SHA1: Byte = 2
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BYTES = 32
    private const val TAG_BITS = 128
    private const val WRAPPED_BYTES = KEY_BYTES + TAG_BITS / 8

    private val random = SecureRandom()

    fun isEncrypted(header: ByteArray): Boolean =
        header.size >= MAGIC.size && header.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    /** A recovery code like `K7QM-2XRP-9HDW-4TNB-VC3E`: 25 unambiguous characters, ~124 bits. */
    fun newRecoveryCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..25).map { alphabet[random.nextInt(alphabet.length)] }
            .chunked(5).joinToString("-") { it.joinToString("") }
    }

    /** Recovery codes are compared without dashes, spaces or letter case. */
    fun normalizeRecoveryCode(code: String): String = code.filter { it.isLetterOrDigit() }.uppercase()

    fun encrypt(plain: ByteArray, secrets: List<String>, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        require(secrets.isNotEmpty() && secrets.size <= 8) { "1–8 secrets" }
        require(secrets.all { it.isNotEmpty() }) { "empty secret" }
        val kdf = if (supports(KDF_SHA256)) KDF_SHA256 else KDF_SHA1
        val dataKey = ByteArray(KEY_BYTES).also { random.nextBytes(it) }

        val out = ByteArrayOutputStream(plain.size + 256)
        DataOutputStream(out).use { o ->
            o.write(MAGIC)
            o.writeByte(kdf.toInt())
            o.writeInt(iterations)
            o.writeByte(secrets.size)
            secrets.forEach { secret ->
                val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
                val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
                val wrapKey = derive(secret, salt, iterations, kdf)
                o.write(salt)
                o.write(iv)
                o.write(gcm(Cipher.ENCRYPT_MODE, wrapKey, iv).doFinal(dataKey))
            }
            val dataIv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
            o.write(dataIv)
            o.write(gcm(Cipher.ENCRYPT_MODE, dataKey, dataIv).doFinal(plain))
        }
        return out.toByteArray()
    }

    fun decrypt(data: ByteArray, secret: String): ByteArray {
        if (!isEncrypted(data)) throw NotEncryptedException()
        val input = DataInputStream(data.inputStream())
        input.skipBytes(MAGIC.size)
        val kdf = input.readByte()
        val iterations = input.readInt()
        val slots = input.readUnsignedByte()
        if (kdf != KDF_SHA256 && kdf != KDF_SHA1 || iterations <= 0 || slots == 0) throw NotEncryptedException()

        var dataKey: ByteArray? = null
        repeat(slots) {
            val salt = ByteArray(SALT_BYTES).also { input.readFully(it) }
            val iv = ByteArray(IV_BYTES).also { input.readFully(it) }
            val wrapped = ByteArray(WRAPPED_BYTES).also { input.readFully(it) }
            if (dataKey == null) {
                dataKey = runCatching {
                    gcm(Cipher.DECRYPT_MODE, derive(secret, salt, iterations, kdf), iv).doFinal(wrapped)
                }.getOrNull()
            }
        }
        val key = dataKey ?: throw WrongSecretException()
        val dataIv = ByteArray(IV_BYTES).also { input.readFully(it) }
        val cipherText = input.readBytes()
        return try {
            gcm(Cipher.DECRYPT_MODE, key, dataIv).doFinal(cipherText)
        } catch (e: GeneralSecurityException) {
            // Right key but the payload fails authentication: the file was altered.
            throw GeneralSecurityException("Backup file is damaged", e)
        }
    }

    private fun gcm(mode: Int, key: ByteArray, iv: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        }

    private fun derive(secret: String, salt: ByteArray, iterations: Int, kdf: Byte): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, KEY_BYTES * 8)
        return try {
            SecretKeyFactory.getInstance(algorithm(kdf)).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun algorithm(kdf: Byte) = if (kdf == KDF_SHA256) "PBKDF2WithHmacSHA256" else "PBKDF2WithHmacSHA1"

    private fun supports(kdf: Byte): Boolean =
        runCatching { SecretKeyFactory.getInstance(algorithm(kdf)) }.isSuccess
}
