package me.asrielyankare.gachaultils.core

import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.MessageDigest

/**
 * SHA-256 hasher using standard JVM MessageDigest.
 */
object SaveHasher {
    fun hashFile(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        file.inputStream().use { inputStream ->
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun hashBytes(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}

/**
 * Abstract base for save detection providers.
 */
abstract class SaveDetector {
    abstract fun detectSaves(instanceId: Int, packageName: String): List<SaveSnapshot>
}

/**
 * Provider for Adobe AIR game save detection.
 *
 * Handles the specific save structure of AIR-based Gacha games:
 * dataDir/Local Store/#SharedObjects/game.swf/ (sol files)
 *
 * Does NOT hard-code filenames. Discovers all .sol files dynamically.
 */
object AirSaveProvider : SaveDetector() {
    fun getProvider(): SaveDetector = this

    override fun detectSaves(instanceId: Int, packageName: String): List<SaveSnapshot> {
        val instance = InstanceStorage.getInstance(instanceId) ?: return emptyList()

        // Wrap BlackBoxCore access in try-catch — may not be initialized yet
        val dataDir = try {
            BlackBoxCore.getBEnvironment().getDataDir(instance.packageName, instance.userId)
        } catch (e: Throwable) {
            // BlackBox not initialized, return empty
            return emptyList()
        }

        val saves = mutableListOf<SaveSnapshot>()

        // AIR save structure: /Local Store/#SharedObjects/<game>.swf/*.sol
        val localStoreDir = File(dataDir, "Local Store")
        if (!localStoreDir.exists()) return saves

        val sharedObjectsDir = File(localStoreDir, "#SharedObjects")
        if (!sharedObjectsDir.exists()) return saves

        // Look for .swf directories
        val swfDirs = sharedObjectsDir.listFiles { dir ->
            dir.isDirectory && dir.name.endsWith(".swf")
        } ?: return saves

        for (swfDir in swfDirs) {
            // Find .sol files in this directory
            val solFiles = swfDir.listFiles { file ->
                file.isFile && file.name.endsWith(".sol")
            } ?: continue

            for (solFile in solFiles) {
                val relativePath = "Local Store/#SharedObjects/${swfDir.name}/${solFile.name}"
                try {
                    val snapshot = SaveSnapshot.fromFile(
                        instanceId = instance.id,
                        packageName = instance.packageName,
                        relativePath = relativePath,
                        file = solFile
                    )
                    saves.add(snapshot)
                } catch (e: Exception) {
                    // Skip files that can't be read
                }
            }
        }

        return saves
    }
}

/**
 * Central manager for save detection, backup, and restoration.
 */
class SaveManager {

    /**
     * Detects all save files for an instance.
     */
    fun detectSaves(instanceId: Int, packageName: String): GachaResult<List<SaveSnapshot>> {
        val instance = InstanceStorage.getInstance(instanceId)
            ?: return GachaResult.failure(GachaError.InstanceNotFound(instanceId))

        return GachaResult.runCatching {
            val saveProvider = AirSaveProvider.getProvider()
            saveProvider.detectSaves(instanceId, packageName)
        }
    }

    /**
     * Creates a local backup of a save file.
     */
    fun backupSave(instanceId: Int, snapshot: SaveSnapshot): GachaResult<SaveSnapshot> {
        val instance = InstanceStorage.getInstance(instanceId)
            ?: return GachaResult.failure(GachaError.InstanceNotFound(instanceId))

        return runBlocking {
            InstanceOperationLock.withInstanceLock(instanceId, "backup") {
                val virtualPath = snapshot.getVirtualPath()
                val sourceFile = File(virtualPath)

                if (!sourceFile.exists()) {
                    throw GachaException(GachaError.SaveAccessError(
                        path = virtualPath,
                        message = "Save file not found at: $virtualPath"
                    ))
                }

                val fileContent = sourceFile.readBytes()
                val actualHash = SaveHasher.hashBytes(fileContent)

                if (actualHash != snapshot.sha256) {
                    throw GachaException(GachaError.SnapshotCorrupted(
                        fileName = snapshot.fileName,
                        expectedHash = snapshot.sha256,
                        actualHash = actualHash
                    ))
                }

                val backupDir = File(snapshot.getBackupDir())
                backupDir.mkdirs()

                val backupFile = File(backupDir, snapshot.fileName)
                backupFile.writeBytes(fileContent)

                val backupHash = SaveHasher.hashFile(backupFile)
                if (backupHash != actualHash) {
                    backupFile.delete()
                    throw GachaException(GachaError.SnapshotCorrupted(
                        fileName = snapshot.fileName,
                        expectedHash = actualHash,
                        actualHash = backupHash
                    ))
                }

                snapshot.copy(fileContent = fileContent)
            }
        }
    }

    /**
     * Restores a save file from backup with atomic write and integrity verification.
     */
    fun restoreSave(instanceId: Int, snapshot: SaveSnapshot): GachaResult<SaveSnapshot> {
        val instance = InstanceStorage.getInstance(instanceId)
            ?: return GachaResult.failure(GachaError.InstanceNotFound(instanceId))

        if (instance.state == InstanceState.RUNNING) {
            return GachaResult.failure(GachaError.InvalidState(
                currentState = instance.state,
                attemptedOperation = "restore",
                message = "Cannot restore while game is running. Stop the game first."
            ))
        }

        return runBlocking {
            InstanceOperationLock.withInstanceLock(instanceId, "restore") {
                val backupFile = File(snapshot.getBackupPath())
                if (!backupFile.exists()) {
                    throw GachaException(GachaError.SaveNotFound(
                        instanceId = instanceId,
                        packageName = instance.packageName,
                        message = "Backup file not found: ${snapshot.fileName}"
                    ))
                }

                val backupContent = backupFile.readBytes()
                val backupHash = SaveHasher.hashBytes(backupContent)

                if (backupHash != snapshot.sha256) {
                    throw GachaException(GachaError.SnapshotCorrupted(
                        fileName = snapshot.fileName,
                        expectedHash = snapshot.sha256,
                        actualHash = backupHash
                    ))
                }

                val targetPath = snapshot.getVirtualPath()
                val targetFile = File(targetPath)

                targetFile.parentFile?.mkdirs()

                val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                try {
                    tempFile.writeBytes(backupContent)

                    val tempHash = SaveHasher.hashFile(tempFile)
                    if (tempHash != backupHash) {
                        tempFile.delete()
                        throw GachaException(GachaError.RestoreVerificationFailed(
                            fileName = snapshot.fileName,
                            expectedHash = backupHash,
                            actualHash = tempHash
                        ))
                    }

                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    val renamed = tempFile.renameTo(targetFile)
                    if (!renamed) {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                    }

                    val finalHash = SaveHasher.hashFile(targetFile)
                    if (finalHash != backupHash) {
                        throw GachaException(GachaError.RestoreVerificationFailed(
                            fileName = snapshot.fileName,
                            expectedHash = backupHash,
                            actualHash = finalHash
                        ))
                    }

                    snapshot.copy(fileContent = backupContent)
                } catch (e: GachaException) {
                    if (tempFile.exists()) tempFile.delete()
                    throw e
                } catch (e: Exception) {
                    if (tempFile.exists()) tempFile.delete()
                    throw GachaException(GachaError.Unknown(
                        message = "Restore failed: ${e.message}",
                        cause = e
                    ))
                }
            }
        }
    }

    /**
     * Deletes a backup file.
     */
    fun deleteBackup(snapshot: SaveSnapshot): GachaResult<Unit> {
        return try {
            val backupFile = File(snapshot.getBackupPath())
            if (backupFile.exists()) {
                backupFile.delete()
            }
            GachaResult.success(Unit)
        } catch (e: Exception) {
            GachaResult.failure(GachaError.Unknown("Failed to delete backup: ${e.message}"))
        }
    }
}
