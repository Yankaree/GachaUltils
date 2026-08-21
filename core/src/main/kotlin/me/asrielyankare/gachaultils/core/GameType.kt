package me.asrielyankare.gachaultils.core

/**
 * Classification of game types, determining save structure and behavior.
 *
 * This enum represents the different categories of games supported by the Gacha Cloud Loader.
 * Each game type has specific save format requirements and handling logic.
 *
 * Current supported types:
 * - AIR_GACHA: Games built on Adobe AIR runtime with .sol save files
 *
 * Future expansion points:
 * - Additional game types can be added as needed
 * - Each type should have its own save provider implementation
 */
enum class GameType {
    /**
     * Adobe AIR-based Gacha games.
     *
     * These games use Adobe AIR runtime with .sol (SharedObject) save files.
     * Examples include Gacha Club, Gacha Life 2, and various community mods.
     *
     * Save files are typically located in:
     * /data/data/<package>/Local Store/#SharedObjects/<game>.swf/<filename>.sol
     *
     * Note: The exact save path and filename may vary between games and mods.
     * The save provider must handle path detection and configuration.
     */
    AIR_GACHA,

    /**
     * Placeholder for future game types.
     * Will be expanded as additional game categories are supported.
     */
    UNSPECIFIED
}