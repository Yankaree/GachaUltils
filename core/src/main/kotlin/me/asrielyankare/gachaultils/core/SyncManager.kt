package me.asrielyankare.gachaultils.core

/**
 * Stub for SyncManager - will be replaced by cloud sync implementation.
 */
object SyncManager {
    fun syncToCloud(instanceId: Int, snapshot: SaveSnapshot, provider: CloudProvider): Boolean {
        // TODO: Implement cloud sync
        return false
    }

    fun syncFromCloud(instanceId: Int, snapshot: SaveSnapshot, provider: CloudProvider): SaveSnapshot? {
        // TODO: Implement cloud sync
        return null
    }
}
