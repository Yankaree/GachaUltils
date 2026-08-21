package com.example.gachacloudloader.core

/**
 * Represents a complete profile for a Gacha game, containing all essential metadata
 * needed for game management, installation, and save handling within the virtualized environment.
 *
 * This data class encapsulates the game's identity, version information, installation path,
 * and game type classification. It serves as the central model for game profile management
 * throughout the application architecture.
 */
data class GameProfile(
    /** Unique identifier for the game, combining package name and game type */
    val gameId: GameId,
    /** Human-readable name of the game (e.g., "Gacha Club", "Gacha Life 2") */
    val gameName: String,
    /** Version name of the game as reported by the package manager */
    val versionName: String,
    /** Version code of the game as reported by the package manager */
    val versionCode: Int,
    /** File path to the installed APK file */
    val apkPath: String,
    /** Classification of the game type, determining save structure and behavior */
    val gameType: GameType
)