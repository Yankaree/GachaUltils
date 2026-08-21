package me.asrielyankare.gachaultils.core

/**
 * Manager for cloud synchronization operations.
 * Currently local-only; cloud providers will be added later.
 */
object SyncManager {
    fun syncToCloud(instanceId: Int, snapshot: SaveSnapshot, provider: CloudProvider): GachaResult<Boolean> {
        // Cloud sync not yet implemented
        return GachaResult.failure(GachaError.Unknown("Cloud sync not yet implemented"))
    }

    fun syncFromCloud(instanceId: Int, snapshot: SaveSnapshot, provider: CloudProvider): GachaResult<SaveSnapshot> {
        // Cloud sync not yet implemented
        return GachaResult.failure(GachaError.Unknown("Cloud sync not yet implemented"))
    }
}
