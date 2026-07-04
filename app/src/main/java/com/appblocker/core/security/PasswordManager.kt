package com.appblocker.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "appblocker_secrets",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun hasPassword(): Boolean = prefs.contains(KEY_PASSWORD_HASH)

    fun createPassword(password: String, question: String, answer: String): Boolean {
        if (password.length < 6 || answer.isBlank()) return false
        val passwordSalt = randomSalt()
        val answerSalt = randomSalt()
        prefs.edit()
            .putString(KEY_PASSWORD_HASH, hash(password, passwordSalt))
            .putString(KEY_PASSWORD_SALT, passwordSalt)
            .putString(KEY_SECURITY_QUESTION, question.trim())
            .putString(KEY_ANSWER_HASH, hash(answer.trim().lowercase(), answerSalt))
            .putString(KEY_ANSWER_SALT, answerSalt)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
        return true
    }

    fun verifyPassword(password: String): AuthResult {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (System.currentTimeMillis() < lockoutUntil) {
            return AuthResult.Locked(lockoutUntil)
        }

        val salt = prefs.getString(KEY_PASSWORD_SALT, null) ?: return AuthResult.Failed(0)
        val expected = prefs.getString(KEY_PASSWORD_HASH, null) ?: return AuthResult.Failed(0)
        if (constantTimeEquals(expected, hash(password, salt))) {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCKOUT_UNTIL, 0L).apply()
            return AuthResult.Success
        }

        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        if (attempts >= MAX_ATTEMPTS) {
            val until = System.currentTimeMillis() + LOCKOUT_MS
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCKOUT_UNTIL, until).apply()
            return AuthResult.Locked(until)
        }
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()
        return AuthResult.Failed(MAX_ATTEMPTS - attempts)
    }

    fun resetWithSecurityAnswer(newPassword: String, answer: String): Boolean {
        if (newPassword.length < 6) return false
        val salt = prefs.getString(KEY_ANSWER_SALT, null) ?: return false
        val expected = prefs.getString(KEY_ANSWER_HASH, null) ?: return false
        if (!constantTimeEquals(expected, hash(answer.trim().lowercase(), salt))) return false
        val question = prefs.getString(KEY_SECURITY_QUESTION, "") ?: ""
        return createPassword(newPassword, question, answer)
    }

    fun securityQuestion(): String = prefs.getString(KEY_SECURITY_QUESTION, "") ?: ""

    private fun randomSalt(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hash(value: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$value".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }

    sealed interface AuthResult {
        data object Success : AuthResult
        data class Failed(val attemptsRemaining: Int) : AuthResult
        data class Locked(val untilMillis: Long) : AuthResult
    }

    companion object {
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_PASSWORD_SALT = "password_salt"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_ANSWER_HASH = "security_answer_hash"
        private const val KEY_ANSWER_SALT = "security_answer_salt"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_MS = 30 * 60 * 1000L
    }
}
