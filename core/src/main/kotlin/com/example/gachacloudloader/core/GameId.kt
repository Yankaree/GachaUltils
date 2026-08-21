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
)
