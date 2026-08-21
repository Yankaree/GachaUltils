package com.example.gachacloudloader.core

/**
 * Represents a unique identifier for a Gacha game, encapsulating the package name and game type.
 *
 * This immutable value object combines the package name of the game with its specific game type,
 * providing a comprehensive identifier that distinguishes between different games within the ecosystem.
 *
 * Examples include games like Gacha Club, Gacha Life 2, and various community-modded APKs.
 * The GameId serves as a fundamental concept throughout the architecture for identifying and
 * managing game profiles within the virtualized environment.
 */
data class GameId(
    /** The unique package name of the game (e.g., air.com.lunime.gachalife2) */
    val packageName: String,
    /** The specific game type identifier (e.g., AIR_GACHA) that determines save structure and behavior */
    val gameType: GameType
) {
    /**
     * Determines if this GameId is equal to another.
     * Compares based on packageName and gameType fields.
     *
     * @param other The other GameId to compare with
     * @return true if both objects have identical packageName and gameType
     */
    override fun equals(other: Any): Boolean {
        if (this === other) return true
        if (other !is GameId) return false
        return packageName == other.packageName && gameType == other.gameType
    }

    /**
     * Generates a hash code for this GameId.
     * Based on the combined values of packageName and gameType fields.
     *
     * @return A hash code for this GameId
     */
    override fun hashCode(): Int = (packageName + gameType.hashCode()).hashCode()
}
