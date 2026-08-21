package me.asrielyankare.gachaultils.core

import kotlinx.coroutines.runBlocking

/**
 * Manages the complete lifecycle of virtual instances.
 *
 * Instance lifecycle:
 * CREATED → INSTALLING → READY ⇄ RUNNING ⇄ STOPPING
 *
 * Each operation is protected by per-instance locks to prevent concurrent conflicts.
 */
class InstanceManager {

    /**
     * Creates a new virtual instance with proper directory setup.
     *
     * Flow:
     * 1. Create BlackBox user
     * 2. Initialize virtual directories
     * 3. Store instance metadata
     */
    fun createInstance(
        packageName: String,
        gameId: GameId,
        displayName: String,
        apkPath: String = ""
    ): GachaResult<InstanceId> {
        val nextId = InstanceStorage.getNextId()

        return runBlocking { InstanceOperationLock.withInstanceLock(nextId, "create") {
            // 1. Create user in BlackBox
            val userResult = BlackBoxCore.getBUserManager().createUser(nextId)
            if (userResult is GachaResult.Failure) {
                throw GachaException(GachaError.UserCreationError(nextId))
            }

            // 2. Initialize directories
            val envResult = BlackBoxCore.getBEnvironment().initializeDirectories(nextId)
            if (envResult is GachaResult.Failure) {
                throw GachaException(GachaError.StorageError(
                    path = "user/$nextId",
                    message = "Failed to initialize directories"
                ))
            }

            // 3. Create and store instance
            val instance = InstanceId.create(
                id = nextId,
                userId = nextId,
                packageName = packageName,
                gameId = gameId,
                displayName = displayName,
                apkPath = apkPath
            )

            InstanceStorage.addInstance(instance)
            instance
        } }
    }

    /**
     * Installs an APK into an instance.
     *
     * Flow:
     * 1. Validate instance state
     * 2. Install APK via BlackBox
     * 3. Update instance state to READY
     */
    fun installApk(instanceId: Int, apkPath: String): GachaResult<InstanceId> {
        val instance = InstanceStorage.getInstance(instanceId)
            ?: return GachaResult.failure(GachaError.InstanceNotFound(instanceId))

        if (!instance.state.isTransitionAllowed(InstanceState.INSTALLING)) {
            return GachaResult.failure(GachaError.InvalidState(
                currentState = instance.state,
                attemptedOperation = "installApk"
            ))
        }

        return runBlocking { InstanceOperationLock.withInstanceLock(instanceId, "install") {
            // Update state to INSTALLING
            InstanceStorage.updateInstance(instance.copy(
                state = InstanceState.INSTALLING,
                updatedAt = System.currentTimeMillis()
            ))

            // Install via BlackBox
            val installResult = BlackBoxCore.getBPackageManager().installPackageAsUser(apkPath, instance.userId)
            when (installResult) {
                is GachaResult.Success -> {
                    val updated = instance.copy(
                        state = InstanceState.READY,
                        apkPath = apkPath,
                        updatedAt = System.currentTimeMillis()
                    )
                    InstanceStorage.updateInstance(updated)
                    updated
                }
                is GachaResult.Failure -> {
                    InstanceStorage.updateInstance(instance.copy(
                        state = InstanceState.ERROR,
                        updatedAt = System.currentTimeMillis()
                    ))
                    throw GachaException(installResult.error)
                }
            }
        } }
    }

    /**
     * Launches a game in the specified instance.
     *
     * Flow:
     * 1. Validate instance is READY
     * 2. Get launch intent
     * 3. Start activity
     * 4. Update state to RUNNING
     */
    fun launchInstance(instanceId: Int): GachaResult<InstanceId> {
        val instance = InstanceStorage.getInstance(instanceId)
            ?: return GachaResult.failure(GachaError.InstanceNotFound(instanceId))

        if (instance.state != InstanceState.READY && instance.state != InstanceState.STOPPING) {
            return GachaResult.failure(GachaError.InvalidState(
                currentState = instance.state,
                attemptedOperation = "launch"
            ))
        }

        return runBlocking { InstanceOperationLock.withInstanceLock(instanceId, "launch") {
            // Get launch intent
            val intent = BlackBoxCore.getBPackageManager().getLaunchIntentForPackage(
                instance.packageName,
                instance.userId
            ) ?: throw GachaException(GachaError.LaunchError(
                packageName = instance.packageName,
                userId = instance.userId,
                message = "No launch intent found for ${instance.packageName}"
            ))

            // Start activity
            val startResult = BlackBoxCore.getBActivityManager().startActivity(intent, instance.userId)
            when (startResult) {
                is GachaResult.Success -> {
                    val updated = instance.copy(
                        state = InstanceState.RUNNING,
                        updatedAt = System.currentTimeMillis()
                    )
                    InstanceStorage.updateInstance(updated)
                    updated
                }
                is GachaResult.Failure -> throw GachaException(startResult.error)
            }
        } }
    }

    /**
     * Stops a running instance.
     *
     * Flow:
     * 1. Validate instance is RUNNING
     * 2. Stop package
     * 3. Update state to READY
     */
    fun stopInstance(instanceId: Int): GachaResult<InstanceId> {
        val instance = InstanceStorage.getInstance(instanceId)
            ?: return GachaResult.failure(GachaError.InstanceNotFound(instanceId))

        if (instance.state != InstanceState.RUNNING) {
            return GachaResult.failure(GachaError.InvalidState(
                currentState = instance.state,
                attemptedOperation = "stop"
            ))
        }

        return runBlocking { InstanceOperationLock.withInstanceLock(instanceId, "stop") {
            // Update state to STOPPING
            InstanceStorage.updateInstance(instance.copy(
                state = InstanceState.STOPPING,
                updatedAt = System.currentTimeMillis()
            ))

            // Stop package
            val stopResult = BlackBoxCore.getBPackageManager().stopPackage(
                instance.packageName,
                instance.userId
            )

            val updated = instance.copy(
                state = InstanceState.READY,
                updatedAt = System.currentTimeMillis()
            )
            InstanceStorage.updateInstance(updated)
            updated
        } }
    }

    /**
     * Deletes an instance and cleans up resources.
     */
    fun deleteInstance(instanceId: Int): GachaResult<Unit> {
        val instance = InstanceStorage.getInstance(instanceId)
            ?: return GachaResult.failure(GachaError.InstanceNotFound(instanceId))

        if (InstanceOperationLock.isLocked(instanceId)) {
            return GachaResult.failure(GachaError.OperationInProgress(
                instanceId = instanceId,
                operation = "delete"
            ))
        }

        // Stop if running
        if (instance.state == InstanceState.RUNNING) {
            val stopResult = stopInstance(instanceId)
            if (stopResult is GachaResult.Failure) {
                return GachaResult.failure(stopResult.error)
            }
        }

        // Uninstall from BlackBox
        try {
            BlackBoxCore.getBPackageManager().uninstallPackageAsUser(
                instance.packageName,
                instance.userId
            )
        } catch (e: Exception) {
            // Continue cleanup even if uninstall fails
        }

        // Delete user from BlackBox
        try {
            BlackBoxCore.getBUserManager().deleteUser(instance.userId)
        } catch (e: Exception) {
            // Continue cleanup
        }

        // Clean up directories
        try {
            BlackBoxCore.getBEnvironment().cleanupDirectories(instance.userId)
        } catch (e: Exception) {
            // Continue cleanup
        }

        // Remove from storage
        InstanceStorage.removeInstance(instanceId)
        InstanceOperationLock.removeLock(instanceId)

        return GachaResult.success(Unit)
    }

    /**
     * Get all instances.
     */
    fun listInstances(): List<InstanceId> {
        return InstanceStorage.getAllInstances()
    }

    /**
     * Get instance by ID.
     */
    fun getInstance(instanceId: Int): InstanceId? {
        return InstanceStorage.getInstance(instanceId)
    }

    /**
     * Select an instance (update UI state reference).
     */
    fun selectInstance(instanceId: Int): InstanceId? {
        return InstanceStorage.getInstance(instanceId)
    }
}
