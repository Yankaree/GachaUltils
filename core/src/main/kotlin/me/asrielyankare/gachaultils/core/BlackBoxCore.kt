package me.asrielyankare.gachaultils.core

import java.io.File

/**
 * Abstraction for BlackBox virtual environment operations.
 *
 * This interface is defined in core. The actual implementation lives in the blackbox module
 * which wraps NewBlackbox APIs. Core never depends on NewBlackbox directly.
 */

/**
 * Package manager operations in the virtual environment.
 */
interface BPackageManager {
    fun installPackageAsUser(apkPath: String, userId: Int): GachaResult<InstallInfo>
    fun uninstallPackageAsUser(packageName: String, userId: Int): GachaResult<Unit>
    fun getLaunchIntentForPackage(packageName: String, userId: Int): Any?
    fun stopPackage(packageName: String, userId: Int): GachaResult<Unit>
    fun isInstalled(packageName: String, userId: Int): Boolean
    fun getApplicationInfo(packageName: String, userId: Int): ApplicationInfo?
}

/**
 * Activity manager operations in the virtual environment.
 */
interface BActivityManager {
    fun startActivity(intent: Any, userId: Int): GachaResult<Unit>
}

/**
 * User manager for virtual Android users.
 */
interface BUserManager {
    fun createUser(userId: Int): GachaResult<Unit>
    fun deleteUser(userId: Int): GachaResult<Unit>
    fun getUsers(): List<UserInfo>
}

/**
 * Environment for the virtual filesystem.
 */
interface BEnvironment {
    fun getDataDir(packageName: String, userId: Int): File
    fun getExternalDataDir(packageName: String, userId: Int): File
    fun getAppDir(packageName: String): File
    fun getBackupRoot(): File
    fun initializeDirectories(userId: Int): GachaResult<Unit>
    fun cleanupDirectories(userId: Int): GachaResult<Unit>
}

/**
 * Result of package installation.
 */
data class InstallInfo(
    val packageName: String,
    val success: Boolean,
    val message: String? = null
)

/**
 * Application info from the package manager.
 */
data class ApplicationInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val sourceDir: String,
    val dataDir: String
)

/**
 * User info from the user manager.
 */
data class UserInfo(
    val id: Int,
    val name: String?
)

/**
 * Registry for BlackBox service implementations.
 * The blackbox module registers its implementations at app startup.
 */
object BlackBoxRegistry {
    private var packageManager: BPackageManager? = null
    private var activityManager: BActivityManager? = null
    private var userManager: BUserManager? = null
    private var environment: BEnvironment? = null

    fun registerPackageManager(pm: BPackageManager) { packageManager = pm }
    fun registerActivityManager(am: BActivityManager) { activityManager = am }
    fun registerUserManager(um: BUserManager) { userManager = um }
    fun registerEnvironment(env: BEnvironment) { environment = env }

    fun getPackageManager(): BPackageManager = packageManager
        ?: throw IllegalStateException("BlackBox not initialized. Call BlackBoxRegistry.register*() first.")
    fun getActivityManager(): BActivityManager = activityManager
        ?: throw IllegalStateException("BlackBox not initialized.")
    fun getUserManager(): BUserManager = userManager
        ?: throw IllegalStateException("BlackBox not initialized.")
    fun getEnvironment(): BEnvironment = environment
        ?: throw IllegalStateException("BlackBox not initialized.")

    fun isInitialized(): Boolean {
        return packageManager != null && activityManager != null &&
                userManager != null && environment != null
    }
}

/**
 * Convenience accessors matching the original BlackBoxCore API surface.
 */
object BlackBoxCore {
    fun getBPackageManager(): BPackageManager = BlackBoxRegistry.getPackageManager()
    fun getBActivityManager(): BActivityManager = BlackBoxRegistry.getActivityManager()
    fun getBUserManager(): BUserManager = BlackBoxRegistry.getUserManager()
    fun getBEnvironment(): BEnvironment = BlackBoxRegistry.getEnvironment()
}
