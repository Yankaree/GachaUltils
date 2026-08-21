package me.asrielyankare.gachaultils.blackbox

import android.content.Context
import android.content.Intent
import me.asrielyankare.gachaultils.core.*
import java.io.File

/**
 * Fallback BlackBox integration that compiles without NewBlackbox dependency.
 *
 * BLOCKER: NewBlackbox library is not yet available as a Gradle dependency.
 * NewBlackbox is in the repository at NewBlackbox/ but uses a separate build system
 * (Groovy, different SDK/AGP versions) that cannot be directly included.
 *
 * TODO: When NewBlackbox is available as a dependency (AAR or module):
 * 1. Add dependency in blackbox/build.gradle.kts
 * 2. Replace this class with NewBlackboxIntegration
 * 3. Remove this file
 *
 * This implementation provides working fallback behavior for development and testing.
 * It creates real directories and manages state, but does NOT actually virtualize
 * Android app execution.
 */
class StubBlackBoxIntegration(private val context: Context) {

    private var initialized = false
    private val backupRoot: File by lazy {
        File(context.filesDir, "backups").apply { mkdirs() }
    }
    private val virtualRoot: File by lazy {
        File(context.filesDir, "blackbox").apply { mkdirs() }
    }

    fun initialize(): GachaResult<Unit> {
        return try {
            virtualRoot.mkdirs()
            backupRoot.mkdirs()
            initialized = true
            GachaResult.success(Unit)
        } catch (e: Exception) {
            GachaResult.failure(GachaError.BlackBoxInitializationError(
                message = "Failed to initialize stub BlackBox: ${e.message}",
                cause = e
            ))
        }
    }

    fun isInitialized(): Boolean = initialized

    fun registerImplementations() {
        BlackBoxRegistry.registerPackageManager(StubPackageManager())
        BlackBoxRegistry.registerActivityManager(StubActivityManager())
        BlackBoxRegistry.registerUserManager(StubUserManager())
        BlackBoxRegistry.registerEnvironment(StubEnvironment())
    }

    /**
     * Stub PackageManager - creates directories and tracks installations.
     * Does NOT actually install or run APKs.
     */
    inner class StubPackageManager : me.asrielyankare.gachaultils.core.BPackageManager {
        private val installedPackages = mutableMapOf<Pair<String, Int>, String>()

        override fun installPackageAsUser(apkPath: String, userId: Int): GachaResult<InstallInfo> {
            val file = File(apkPath)
            if (!file.exists()) {
                return GachaResult.failure(GachaError.PackageInstallError(
                    packageName = "",
                    reason = "APK file not found: $apkPath"
                ))
            }

            // Extract package name from APK path (use filename as identifier)
            val packageName = file.nameWithoutExtension
            installedPackages[packageName to userId] = apkPath

            // Create virtual data directory
            val dataDir = File(virtualRoot, "data/user/$userId/$packageName")
            dataDir.mkdirs()

            return GachaResult.success(InstallInfo(
                packageName = packageName,
                success = true,
                message = "Installed (stub mode - NewBlackbox not available)"
            ))
        }

        override fun uninstallPackageAsUser(packageName: String, userId: Int): GachaResult<Unit> {
            installedPackages.remove(packageName to userId)
            return GachaResult.success(Unit)
        }

        override fun getLaunchIntentForPackage(packageName: String, userId: Int): Any? {
            // Stub: cannot launch without NewBlackbox
            return null
        }

        override fun stopPackage(packageName: String, userId: Int): GachaResult<Unit> {
            return GachaResult.success(Unit)
        }

        override fun isInstalled(packageName: String, userId: Int): Boolean {
            return installedPackages.containsKey(packageName to userId)
        }

        override fun getApplicationInfo(packageName: String, userId: Int): ApplicationInfo? {
            val apkPath = installedPackages[packageName to userId] ?: return null
            return ApplicationInfo(
                packageName = packageName,
                versionName = "stub",
                versionCode = 1,
                sourceDir = apkPath,
                dataDir = "${virtualRoot.absolutePath}/data/user/$userId/$packageName"
            )
        }
    }

    /**
     * Stub ActivityManager.
     */
    inner class StubActivityManager : me.asrielyankare.gachaultils.core.BActivityManager {
        override fun startActivity(intent: Any, userId: Int): GachaResult<Unit> {
            // Cannot start activity without NewBlackbox
            return GachaResult.failure(GachaError.InvalidState(
                currentState = InstanceState.READY,
                attemptedOperation = "startActivity",
                message = "Cannot start activity: NewBlackbox not available (stub mode)"
            ))
        }
    }

    /**
     * Stub UserManager - tracks users in memory.
     */
    inner class StubUserManager : me.asrielyankare.gachaultils.core.BUserManager {
        private val users = mutableMapOf<Int, UserInfo>()

        override fun createUser(userId: Int): GachaResult<Unit> {
            users[userId] = UserInfo(id = userId, name = "user$userId")

            // Create virtual user directories
            val userDir = File(virtualRoot, "data/user/$userId")
            userDir.mkdirs()

            return GachaResult.success(Unit)
        }

        override fun deleteUser(userId: Int): GachaResult<Unit> {
            users.remove(userId)
            return GachaResult.success(Unit)
        }

        override fun getUsers(): List<UserInfo> {
            return users.values.toList()
        }
    }

    /**
     * Stub Environment - creates real directories in the app's private storage.
     * No actual filesystem virtualization.
     */
    inner class StubEnvironment : me.asrielyankare.gachaultils.core.BEnvironment {

        override fun getDataDir(packageName: String, userId: Int): File {
            return File(virtualRoot, "data/user/$userId/$packageName").apply { mkdirs() }
        }

        override fun getExternalDataDir(packageName: String, userId: Int): File {
            return File(virtualRoot, "external/user/$userId/Android/data/$packageName").apply { mkdirs() }
        }

        override fun getAppDir(packageName: String): File {
            return File(virtualRoot, "data/app/$packageName").apply { mkdirs() }
        }

        override fun getBackupRoot(): File = backupRoot

        override fun initializeDirectories(userId: Int): GachaResult<Unit> {
            return try {
                File(virtualRoot, "data/user/$userId").mkdirs()
                GachaResult.success(Unit)
            } catch (e: Exception) {
                GachaResult.failure(GachaError.StorageError(
                    path = "user/$userId",
                    message = "Failed to initialize: ${e.message}"
                ))
            }
        }

        override fun cleanupDirectories(userId: Int): GachaResult<Unit> {
            return GachaResult.success(Unit)
        }
    }
}
