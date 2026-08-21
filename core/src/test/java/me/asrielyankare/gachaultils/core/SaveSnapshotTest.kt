package me.asrielyankare.gachaultils.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SaveSnapshotTest {

    @Test
    fun `filename is preserved from original file`() {
        val tempFile = File.createTempFile("ABC123", ".sol")
        try {
            tempFile.writeBytes(byteArrayOf(1, 2, 3))

            val snapshot = SaveSnapshot.fromFile(
                instanceId = 0,
                packageName = "com.test.game",
                relativePath = "Local Store/#SharedObjects/game.swf/ABC123.sol",
                file = tempFile
            )

            assertEquals("ABC123.sol", snapshot.fileName)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `different filenames are preserved`() {
        val files = listOf("GL2.sol", "MySave.sol", "save_data_123.sol", "data (1).sol")

        files.forEach { name ->
            val tempFile = File.createTempFile("test", ".sol")
            val targetFile = File(tempFile.parentFile, name)
            tempFile.renameTo(targetFile)
            try {
                targetFile.writeBytes(byteArrayOf(1, 2, 3))

                val snapshot = SaveSnapshot.fromFile(
                    instanceId = 0,
                    packageName = "com.test.game",
                    relativePath = "Local Store/#SharedObjects/game.swf/$name",
                    file = targetFile
                )

                assertEquals(name, snapshot.fileName)
            } finally {
                targetFile.delete()
            }
        }
    }

    @Test
    fun `relativePath is preserved`() {
        val tempFile = File.createTempFile("test", ".sol")
        try {
            tempFile.writeBytes(byteArrayOf(1, 2, 3))

            val relativePath = "Local Store/#SharedObjects/game.swf/ABC123.sol"
            val snapshot = SaveSnapshot.fromFile(
                instanceId = 0,
                packageName = "com.test.game",
                relativePath = relativePath,
                file = tempFile
            )

            assertEquals(relativePath, snapshot.relativePath)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `fileSize matches actual file`() {
        val tempFile = File.createTempFile("test", ".sol")
        try {
            val content = ByteArray(1024) { it.toByte() }
            tempFile.writeBytes(content)

            val snapshot = SaveSnapshot.fromFile(
                instanceId = 0,
                packageName = "com.test.game",
                relativePath = "test.sol",
                file = tempFile
            )

            assertEquals(1024L, snapshot.fileSize)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `sha256 matches file content`() {
        val tempFile = File.createTempFile("test", ".sol")
        try {
            tempFile.writeBytes("test data".toByteArray())

            val snapshot = SaveSnapshot.fromFile(
                instanceId = 0,
                packageName = "com.test.game",
                relativePath = "test.sol",
                file = tempFile
            )

            val expectedHash = SaveHasher.hashBytes("test data".toByteArray())
            assertEquals(expectedHash, snapshot.sha256)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `fileContent is stored in snapshot`() {
        val tempFile = File.createTempFile("test", ".sol")
        try {
            val content = byteArrayOf(0x01, 0x02, 0x03, 0x04)
            tempFile.writeBytes(content)

            val snapshot = SaveSnapshot.fromFile(
                instanceId = 0,
                packageName = "com.test.game",
                relativePath = "test.sol",
                file = tempFile
            )

            assertNotNull(snapshot.fileContent)
            assertArrayEquals(content, snapshot.fileContent)
        } finally {
            tempFile.delete()
        }
    }
}
