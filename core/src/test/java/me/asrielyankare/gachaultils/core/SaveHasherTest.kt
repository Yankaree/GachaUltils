package me.asrielyankare.gachaultils.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SaveHasherTest {

    @Test
    fun `hashFile produces consistent SHA-256`() {
        val tempFile = File.createTempFile("test", ".sol")
        try {
            tempFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))

            val hash1 = SaveHasher.hashFile(tempFile)
            val hash2 = SaveHasher.hashFile(tempFile)

            assertEquals(hash1, hash2)
            assertEquals(64, hash1.length) // SHA-256 hex = 64 chars
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `hashBytes produces correct SHA-256`() {
        val data = "Hello, World!".toByteArray()
        val hash = SaveHasher.hashBytes(data)

        // Known SHA-256 of "Hello, World!"
        assertEquals("dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f", hash)
    }

    @Test
    fun `different files produce different hashes`() {
        val file1 = File.createTempFile("test1", ".sol")
        val file2 = File.createTempFile("test2", ".sol")
        try {
            file1.writeBytes(byteArrayOf(1, 2, 3))
            file2.writeBytes(byteArrayOf(4, 5, 6))

            assertNotEquals(SaveHasher.hashFile(file1), SaveHasher.hashFile(file2))
        } finally {
            file1.delete()
            file2.delete()
        }
    }
}
