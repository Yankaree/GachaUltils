package me.asrielyankare.gachaultils.core

/**
 * Represents the lifecycle state of a virtual instance.
 *
 * Each instance transitions through these states during its lifetime:
 * CREATED → INSTALLING → READY → RUNNING → STOPPING → READY (loop)
 *
 * ERROR can occur at any point and requires recovery.
 */
enum class InstanceState {
    /** Instance has been created but not yet set up */
    CREATED,
    /** APK is being installed into NewBlackbox */
    INSTALLING,
    /** Instance is ready to launch */
    READY,
    /** Game is currently running */
    RUNNING,
    /** Game is being stopped */
    STOPPING,
    /** An error occurred */
    ERROR;

    fun isTransitionAllowed(next: InstanceState): Boolean {
        return when (this) {
            CREATED -> next in setOf(INSTALLING, ERROR)
            INSTALLING -> next in setOf(READY, ERROR)
            READY -> next in setOf(RUNNING, ERROR)
            RUNNING -> next in setOf(STOPPING, ERROR)
            STOPPING -> next in setOf(READY, ERROR)
            ERROR -> next in setOf(CREATED, READY)
        }
    }
}
