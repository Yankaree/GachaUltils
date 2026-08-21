package com.example.gachacloudloader.core

/**
 * Unique identifier for a BlackBox instance, mapping to a virtualized Android user environment.
 *
 * Each InstanceId represents an independent virtual Android instance, where each instance
 * runs in its own virtualized data directory. This allows multiple instances of Gacha games
 * to run simultaneously with isolated storage environments.
 *
 * Mapping:
 * Instance 0 → BlackBox userId 0
 * Instance 1 → BlackBox userId 1
 * Instance 2 → BlackBox userId 2
 *
 * The InstanceId encapsulates the identity of a virtual instance, including its package
 * association, display name, and creation metadata.
 */
data class InstanceId(
    /** Unique numeric ID for this instance */
    val id: Int,
    /** The BlackBox userId that this instance maps to */
    val userId: Int,
    /** The package name associated with this instance */
    val packageName: String,
    /** The GameId for the game running in this instance */
    val gameId: GameId,
    /** Human-readable display name for the instance */
    val displayName: String,
    /** Timestamp when this instance was created */
    val createdAt: Long,
    /** Timestamp when this instance was last updated */
    val updatedAt: Long
) {
    /**
     * Creates a new InstanceId with default timestamps.
     *
     * @param id Unique numeric ID
     * @param userId BlackBox userId mapping
     * @param packageName Associated game package name
     * @param gameId The GameId for the game
     * @param displayName Human-readable display name
     * @return A new InstanceId with current timestamps
     */
    companion object {
        fun create(
            id: Int,
            userId: Int,
            packageName: String,
            gameId: GameId,
            displayName: String
        ): InstanceId = InstanceId(
            id = id,
            userId = userId,
            packageName = packageName,
            gameId = gameId,
            displayName = displayName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        /** Creates an InstanceId from a NewBlackbox userId */
        fun fromUserId(userId: Int, packageName: String, gameId: GameId, displayName: String): InstanceId = create(
            id = userId,
            userId = userId,
            packageName = packageName,
            gameId = gameId,
            displayName = displayName
        )
    }
}