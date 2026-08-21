package me.asrielyankare.gachaultils.core

import java.io.File

/**
 * Represents a save snapshot that captures the state of a game's save data at a point in time.
 *
 * This model is used by both local save management and cloud synchronization systems.
 * It contains all necessary metadata to identify, verify, and manage save files.
 *
 * IMPORTANT: filename is always preserved from the original save file.
 * No normalization, no renaming, no generation of new names.
 */
data class SaveSnapshot(
    /** The instance ID where the save belongs */
    val instanceId: Int,
    /** The package name of the game */
    val packageName: String,
    /** The relative path from the data root to the save file */
    val relativePath: String,
    /** The original filename of the save file (e.g., "ABC123.sol") */
    val fileName: String,
    /** Total size of the save file in bytes */
    val fileSize: Long,
    /** SHA-256 hash of the save file for integrity verification */
    val sha256: String,
    /** Timestamp when this snapshot was created (milliseconds since epoch) */
    val timestamp: Long = System.currentTimeMillis(),
    /** The actual file contents for backup/restore operations */
    val fileContent: ByteArray? = null
) {
    /**
     * Gets the absolute path to this save in the virtual filesystem.
     */
    fun getVirtualPath(): String {
        val instance = InstanceStorage.getInstance(instanceId)
        return instance?.let {
            "${BlackBoxCore.getBEnvironment().getDataDir(it.packageName, it.userId)}/$relativePath"
        } ?: throw IllegalStateException("Instance $instanceId not found")
    }

    /**
     * Gets the backup storage directory for this instance.
     */
    fun getBackupDir(): String {
        return "${BlackBoxCore.getBEnvironment().getBackupRoot()}/$instanceId"
    }

    /**
     * Gets the full backup file path (preserving original filename).
     */
    fun getBackupPath(): String {
        return "${getBackupDir()}/$fileName"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SaveSnapshot) return false
        return instanceId == other.instanceId &&
                packageName == other.packageName &&
                relativePath == other.relativePath &&
                fileName == other.fileName &&
                fileSize == other.fileSize &&
                sha256 == other.sha256 &&
                timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = instanceId
        result = 31 * result + packageName.hashCode()
        result = 31 * result + relativePath.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + sha256.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }

    companion object {
        /**
         * Creates a SaveSnapshot from an actual save file.
         * Preserves original filename.
         */
        fun fromFile(
            instanceId: Int,
            packageName: String,
            relativePath: String,
            file: File
        ): SaveSnapshot {
            val hash = SaveHasher.hashFile(file)
            val content = file.readBytes()
            return SaveSnapshot(
                instanceId = instanceId,
                packageName = packageName,
                relativePath = relativePath,
                fileName = file.name,
                fileSize = file.length(),
                sha256 = hash,
                fileContent = content
            )
        }

        /**
         * Creates a SaveSnapshot from backup file content.
         * Used during restore verification.
         */
        fun fromBackupFile(
            instanceId: Int,
            packageName: String,
            relativePath: String,
            backupFile: File
        ): SaveSnapshot {
            val hash = SaveHasher.hashFile(backupFile)
            val content = backupFile.readBytes()
            return SaveSnapshot(
                instanceId = instanceId,
                packageName = packageName,
                relativePath = relativePath,
                fileName = backupFile.name,
                fileSize = backupFile.length(),
                sha256 = hash,
                fileContent = content
            )
        }
    }
}
