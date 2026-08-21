package com.example.gachacloudloader.core

import java.io.File
import java.security.MessageDigest

/**
 * Central abstraction for save system management in Gacha Cloud Loader.
 *
 * Manages save detection, backup, restoration, and synchronization operations.
 * Coordinates with SaveDetector to find saves, SaveHasher for integrity,
 * and cloud providers for synchronization.
 */
class SaveManager {
    /**
     * Detects save files for a given instance and game
     *
     * @param instanceId Instance ID to scan
     * @param packageName Package name of the game
     * @return List of detected SaveSnapshots
     */
    fun detectSaves(instanceId: Int, packageName: String): List<SaveSnapshot> {
        val saveProvider = AirSaveProvider.getProvider()
        return saveProvider.detectSaves(instanceId, packageName)
    }

    /**
     * Creates a backup snapshot of a save file
     *
     * @param instanceId Instance ID
     * @param snapshot Save snapshot to backup
     * @return Path to backup file if successful
     */
    fun backupSave(instanceId: Int, snapshot: SaveSnapshot): String? {
        val file = File(snapshot.getVirtualPath())
        if (!file.exists()) return null

        // Copy file to backup location
        val backupDir = File("backups/${instanceId}")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val backupFile = File(backupDir, snapshot.fileName)
        file.copyTo(backupFile, overwrite = true)

        // Verify integrity
        val backupHash = SaveHasher.hashFile(backupFile)
        if (backupHash != snapshot.sha256) {
            backupFile.delete() // Clean up failed backup
            return null
        }

        return backupFile.absolutePath
    }

    /**
     * Restores a save file from a backup snapshot
     *
     * @param instanceId Instance ID
     * @param snapshot Save snapshot to restore
     * @return true if successful
     */
    fun restoreSave(instanceId: Int, snapshot: SaveSnapshot): Boolean {
        val backupFile = File("backups/${instanceId}/${snapshot.fileName}")
        if (!backupFile.exists()) return false

        val targetFile = File(snapshot.getVirtualPath())

        // Ensure target directory exists
        targetFile.parentFile?.mkdirs()

        // Copy backup to target
        backupFile.copyTo(targetFile, overwrite = true)

        // Verify integrity
        val restoredHash = SaveHasher.hashFile(targetFile)
        return restoredHash == snapshot.sha256
    }

    /**
     * Synchronizes save with cloud provider
     *
     * @param instanceId Instance ID
     * @param snapshot Save snapshot to sync
     * @param provider Cloud provider to use
     * @return true if successful
     */
    fun syncWithCloud(
        instanceId: Int,
        snapshot: SaveSnapshot,
        provider: CloudProvider
    ): Boolean {
        // Implementation would delegate to SyncManager
        return SyncManager.syncToCloud(instanceId, snapshot, provider)
    }

    /**
     * Downloads save from cloud provider
     *
     * @param instanceId Instance ID
     * @param snapshot Save snapshot placeholder
     * @param provider Cloud provider to use
     * @return SaveSnapshot if successful
     */
    fun downloadFromCloud(
        instanceId: Int,
        snapshot: SaveSnapshot,
        provider: CloudProvider
    ): SaveSnapshot? {
        return SyncManager.syncFromCloud(instanceId, snapshot, provider)
    }
}

/**
 * Abstract base for save detection providers.
 * Allows different game types to have specialized save detection logic.
 */
abstract class SaveDetector {
    /**
     * Detects save files for a given instance
     *
     * @param instanceId Instance ID to scan
     * @param packageName Package name of the game
     * @return List of detected SaveSnapshots
     */
    abstract fun detectSaves(instanceId: Int, packageName: String): List<SaveSnapshot>
}

/**
 * Provider for Adobe AIR game save detection.
 * Handles the specific save structure of AIR-based Gacha games.
 */
object AirSaveProvider : SaveDetector() {
    /**
     * Gets the singleton instance
     */
    fun getProvider(): SaveDetector = this

    override fun detectSaves(instanceId: Int, packageName: String): List<SaveSnapshot> {
        val instance = InstanceStorage.getInstance(instanceId)
        if (instance == null) return emptyList()

        val dataDir = BlackBoxCore.getBEnvironment().getDataDir(
            instance.packageName,
            instance.userId
        )

        val saves = mutableListOf<SaveSnapshot>()

        // AIR save structure: /Local Store/#SharedObjects/<game>.swf/*.sol
        val localStoreDir = File(dataDir, "Local Store")
        if (!localStoreDir.exists()) return saves

        val sharedObjectsDir = File(localStoreDir, "#SharedObjects")
        if (!sharedObjectsDir.exists()) return saves

        // Look for .swf directories
        sharedObjectsDir.listFiles { dir ->
            dir.isDirectory && dir.name.endsWith(".swf")
        }?.forEach { swfDir ->
            // Find .sol files in this directory
            swfDir.listFiles { file ->
                file.isFile && file.name.endsWith(".sol")
            }?.forEach { solFile ->
                val relativePath = "Local Store/#SharedObjects/${swfDir.name}/${solFile.name}"
                val snapshot = SaveSnapshot.fromFile(
                    instanceId = instance.id,
                    packageName = instance.packageName,
                    relativePath = relativePath,
                    file = solFile
                )
                saves.add(snapshot)
            }
        }

        return saves
    }
}

/**
 * Interface for hashing save files to ensure integrity.
 * Used for backup verification and cloud synchronization.
 */
object SaveHasher {
    /**
     * Computes SHA-256 hash of a file
     *
     * @param file File to hash
     * @return Hex-encoded SHA-256 hash string
     */
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
}

/**
 * Lifecycle API for restore operations.
 * Handles the restore process from initiation to completion.
 */
class RestoreManager {
    fun initiateRestore(instanceId: Int, snapshot: SaveSnapshot): Boolean {
        // Would set up restore process, show progress, etc.
        return SaveManager().restoreSave(instanceId, snapshot)
    }

    fun cancelRestore() {
        // Would cancel ongoing restore operation
    }
}