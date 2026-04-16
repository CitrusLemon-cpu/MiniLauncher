package com.example.minilauncher.util

import java.security.MessageDigest

object PasswordUtils {
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(input: String, storedHash: String): Boolean {
        return hashPassword(input) == storedHash
    }
}
