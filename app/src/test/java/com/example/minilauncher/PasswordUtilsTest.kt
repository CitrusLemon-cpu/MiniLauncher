package com.example.minilauncher

import com.example.minilauncher.util.PasswordUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordUtilsTest {
    @Test
    fun hashPassword_isDeterministic() {
        val firstHash = PasswordUtils.hashPassword("1234")
        val secondHash = PasswordUtils.hashPassword("1234")

        assertTrue(firstHash.isNotBlank())
        assertTrue(firstHash.matches(Regex("[0-9a-f]{64}")))
        assertTrue(firstHash == secondHash)
    }

    @Test
    fun verifyPassword_matchesStoredHash() {
        val storedHash = PasswordUtils.hashPassword("secret-pin")

        assertTrue(PasswordUtils.verifyPassword("secret-pin", storedHash))
        assertFalse(PasswordUtils.verifyPassword("wrong-pin", storedHash))
    }

    @Test
    fun differentPasswords_produceDifferentHashes() {
        val firstHash = PasswordUtils.hashPassword("1111")
        val secondHash = PasswordUtils.hashPassword("2222")

        assertNotEquals(firstHash, secondHash)
    }
}
