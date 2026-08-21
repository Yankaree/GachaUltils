package me.asrielyankare.gachaultils.core

/**
 * Manages virtual instance lifecycle in the Gacha Cloud Loader
 * Handles mapping between NewBlackbox user IDs and virtual instances
 * Coordinates with NewBlackbox's BPackageManager and BEnvironment
 */
class InstanceManager {
    /**
     * Creates a new virtual instance with proper directory mapping
     *
     * @param instanceId     Unique numeric instance ID
     * @param packageName    Game package name to virtualize
     * @param gameId         GameId of the game to run
     * @param displayName    Human-readable instance name
     * @return InstanceId    Created instance identifier
     */
    fun createInstance(
        instanceId: Int,
        packageName: String,
        gameId: GameId,
        displayName: String
    ): InstanceId {
        // 1. Verify NewBlackbox supports this user ID
        if (!BlackBoxCore.getBPackageManager().isUserValid(instanceId)) {
            throw IllegalStateException("User ID $instanceId is not valid in NewBlackbox")
        }

        // 2. Initialize virtual directories via BEnvironment
        BlackBoxCore.getBEnvironment().initializeInstanceDirectories(instanceId)

        // 3. Map user ID to package via BPackageManager
        BlackBoxCore.getBPackageManager().setCurrentUser(instanceId)

        // 4. Create InstanceId record
        val instance = InstanceId.create(
            id = instanceId,
            userId = instanceId,
            packageName = packageName,
            gameId = gameId,
            displayName = displayName
        )

        // 5. Notify system of new instance
        BlackBoxCore.getBUserManager().addUser(instanceId)
        InstanceStorage.addInstance(instance)

        return instance
    }

    /**
     * Launch a game instance in NewBlackbox
     *
     * @param instanceId Instance to launch
     * @param apkPath    Path to game APK
     * @return true if successful
     */
    fun launchInstance(instanceId: Int, apkPath: String): Boolean {
        // Validate instance exists
        val instance = InstanceStorage.getInstance(instanceId)
        if (instance == null) {
            throw IllegalStateException("Instance $instanceId not found")
        }


        // 1. Install APK via BlackBoxPackageManager
        val installResult = BlackBoxCore.getBPackageManager().installPackageAsUser(
            apkPath,
            InstallOptions(),
            instance.userId
        )

        if (!installResult.success) {
            return false
        }


        // 2. Map instance to virtual user ID
        BlackBoxCore.getBEnvironment().setCurrentUser(instance.userId)


        // 3. Launch through NewBlackbox's activity system
        val launchIntent = BlackBoxCore.getBPackageManager().getLaunchIntentForPackage(
            instance.packageName,
            instance.userId
        )

        if (launchIntent == null) {
            return false
        }


        // 4. Start activity (simplified - actual implementation would handle UI)
        // This would typically involve starting an IntentService or similar
        BlackBoxCore.getBActivityManager().startActivity(
            launchIntent,
            instance.userId
        )

        return true
    }

    /**
     * Stop an instance and clean up resources
     *
     * @param instanceId Instance to stop
     * @return true if successful
     */
    fun stopInstance(instanceId: Int): Boolean {
        val instance = InstanceStorage.getInstance(instanceId)
        if (instance == null) {
            return false
        }


        // 1. Stop the package in NewBlackbox
        BlackBoxCore.getBPackageManager().stopPackage(
            instance.packageName,
            instance.userId
        )


        // 2. Clean up virtual directories
        BlackBoxCore.getBEnvironment().cleanupInstanceDirectories(instance.userId)


        // 3. Remove from storage
        InstanceStorage.removeInstance(instanceId)


        // 4. Notify user manager
        BlackBoxCore.getBUserManager().removeUser(instance.userId)

        return true
    }

    /**
     * Get all active instances
     *
     * @return List of InstanceId objects
     */
    fun listInstances(): List<InstanceId> {
        return InstanceStorage.getAllInstances()
    }

    /**
     * Get instance by ID
     *
     * @param instanceId Instance identifier
     * @return InstanceId or null if not found
     */
    fun getInstance(instanceId: Int): InstanceId? {
        return InstanceStorage.getInstance(instanceId)
    }
}

// Storage implementation would need to be fleshed out
object InstanceStorage {
    private val instanceMap = mutableMapOf<Int, InstanceId>()

    fun addInstance(instance: InstanceId) {
        instanceMap[instance.id] = instance
    }

    fun removeInstance(instanceId: Int) {
        instanceMap.remove(instanceId)
    }

    fun getInstance(instanceId: Int): InstanceId? {
        return instanceMap[instanceId]
    }

    fun getAllInstances(): List<InstanceId> {
        return instanceMap.values.toList()
    }
}