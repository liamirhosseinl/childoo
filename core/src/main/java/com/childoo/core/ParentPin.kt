package com.childoo.core

import java.security.MessageDigest
import java.security.SecureRandom

/** Salted SHA-256 for the parent PIN. The PIN itself is never stored. */
object ParentPin {
    const val LENGTH = 4

    fun isValid(pin: String): Boolean = pin.length == LENGTH && pin.all { it.isDigit() }

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest("$salt:$pin".toByteArray()).toHex()

    fun matches(pin: String, salt: String, hash: String): Boolean =
        MessageDigest.isEqual(hash(pin, salt).toByteArray(), hash.toByteArray())

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
