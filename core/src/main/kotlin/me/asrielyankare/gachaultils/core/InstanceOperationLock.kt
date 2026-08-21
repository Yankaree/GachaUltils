package me.asrielyankare.gachaultils.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-instance operation lock that prevents concurrent operations on the same instance.
 *
 * Prevents:
 * - Backup and Restore running simultaneously on same instance
 * - Restore while game is writing save
 * - Delete while operation is running
 * - Launch while restore is in progress
 */
object InstanceOperationLock {
    private val locks = ConcurrentHashMap<Int, Mutex>()

    private fun getLock(instanceId: Int): Mutex {
        return locks.getOrPut(instanceId) { Mutex() }
    }

    /**
     * Acquires the lock for the given instance and executes the block.
     * Throws OperationInProgress if another operation is already running.
     */
    suspend fun <T> withInstanceLock(
        instanceId: Int,
        operation: String,
        block: suspend () -> T
    ): GachaResult<T> {
        val mutex = getLock(instanceId)
        if (mutex.isLocked) {
            return GachaResult.failure(GachaError.OperationInProgress(instanceId, operation))
        }
        return mutex.withLock {
            GachaResult.runCatching { block() }
        }
    }

    /**
     * Check if an operation is currently in progress for the given instance.
     */
    fun isLocked(instanceId: Int): Boolean {
        return locks[instanceId]?.isLocked == true
    }

    /**
     * Remove lock for instance (called when instance is deleted).
     */
    fun removeLock(instanceId: Int) {
        locks.remove(instanceId)
    }

    /**
     * Clear all locks (for testing).
     */
    fun clearAll() {
        locks.clear()
    }
}
