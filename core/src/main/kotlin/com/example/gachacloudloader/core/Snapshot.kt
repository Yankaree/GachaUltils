package com.example.gachacloudloader.core

import java.io.File

/**
 * Represents a save snapshot that captures the state of a game's save data at a point in time.
 *
 * This model is used by both local save management and cloud synchronization systems.
 * It contains all necessary metadata to identify, verify, and manage save files.
 */
data class SaveSnapshot(
    /** The instance ID where the save belongs */
    val instanceId: Int,
    /** The package name of the game */
    val packageName: String,
    /** The relative path from the app's data directory to the save file */
    val relativePath: String,
    /** The filename of the save file (e.g., "ABC123.sol") */
    val fileName: String,
    /** Total size of the save file in bytes */
    val fileSize: Long,
    /** SHA-256 hash of the save file for integrity verification */
    val sha256: String,
    /** Timestamp when this snapshot was created (milliseconds since epoch) */
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Gets the absolute path to this snapshot in the virtual filesystem
     */
    fun getVirtualPath(): String {
        val instance = InstanceStorage.getInstance(instanceId)
        return instance?.let {
            "${BlackBoxCore.getBEnvironment().getDataDir(it.packageName, it.userId)}/$relativePath"
        } ?: throw IllegalStateException("Instance $instanceId not found")
    }

    /**
     * Gets the absolute path where this snapshot would be stored locally for backup
     */
    fun getBackupPath(): String {
        return "backups/${instanceId}/${fileName}"
    }

    /**
     * Creates a new SaveSnapshot from an actual save file
     */
    companion object {
        fun fromFile(
            instanceId: Int,
            packageName: String,
            relativePath: String,
            file: File
        ): SaveSnapshot {
            val hash = SaveHasher.hashFile(file)
            return SaveSnapshot(
                instanceId = instanceId,
                packageName = packageName,
                relativePath = relativePath,
                fileName = file.name,
                fileSize = file.length(),
                sha256 = hash
            )
        }
    }
}