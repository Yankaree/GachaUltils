package me.asrielyankare.gachaultils.blackbox

import android.content.Context
import android.content.Intent
import me.asrielyankare.gachaultils.core.*
import java.io.File

/**
 * Fallback stub integration used when NewBlackbox fails to initialize.
 * Allows the app to function (create instances, store metadata) without
 * actually running apps in a virtual environment.
 */
class FallbackBlackBoxIntegration(private val context: Context) {

    fun initialize(): GachaResult<Unit> = GachaResult.success(Unit)

    fun isInitialized(): Boolean = true

    fun registerImplementations() {
        BlackBoxRegistry.registerPackageManager(StubPackageManager())
        BlackBoxRegistry.registerActivityManager(StubActivityManager())
        BlackBoxRegistry.registerUserManager(StubUserManager())
        BlackBoxRegistry.registerEnvironment(StubEnvironment())
    }

    private val virtualRoot: File by lazy {
        File(context.filesDir, "blackbox").apply { mkdirs() }
    }
    private val backupRoot: File by lazy {
        File(context.filesDir, "backups").apply { mkdirs() }
    }

    inner class StubPackageManager : BPackageManager {
        private val installedPackages = mutableMapOf<Pair<String, Int>, String>()

        override fun installPackageAsUser(apkPath: String, userId: Int): GachaResult<InstallInfo> {
            val file = File(apkPath)
            if (!file.exists()) {
                return GachaResult.failure(GachaError.PackageInstallError(
                    packageName = "",
                    reason = "APK file not found: $apkPath"
                ))
            }
            val packageName = file.nameWithoutExtension
            installedPackages[packageName to userId] = apkPath
            val dataDir = File(virtualRoot, "data/user/$userId/$packageName")
            dataDir.mkdirs()
            return GachaResult.success(InstallInfo(
                packageName = packageName,
                success = true,
                message = "Installed (fallback mode)"
            ))
        }

        override fun uninstallPackageAsUser(packageName: String, userId: Int): GachaResult<Unit> {
            installedPackages.remove(packageName to userId)
            return GachaResult.success(Unit)
        }

        override fun getLaunchIntentForPackage(packageName: String, userId: Int): Any? = null

        override fun stopPackage(packageName: String, userId: Int): GachaResult<Unit> =
            GachaResult.success(Unit)

        override fun isInstalled(packageName: String, userId: Int): Boolean =
            installedPackages.containsKey(packageName to userId)

        override fun getApplicationInfo(packageName: String, userId: Int): ApplicationInfo? {
            val apkPath = installedPackages[packageName to userId] ?: return null
            return ApplicationInfo(
                packageName = packageName,
                versionName = "fallback",
                versionCode = 1,
                sourceDir = apkPath,
                dataDir = "${virtualRoot.absolutePath}/data/user/$userId/$packageName"
            )
        }
    }

    inner class StubActivityManager : BActivityManager {
        override fun startActivity(intent: Any, userId: Int): GachaResult<Unit> =
            GachaResult.failure(GachaError.InvalidState(
                currentState = InstanceState.READY,
                attemptedOperation = "startActivity",
                message = "Cannot start activity in fallback mode (NewBlackbox not available)"
            ))
    }

    inner class StubUserManager : BUserManager {
        private val users = mutableMapOf<Int, UserInfo>()

        override fun createUser(userId: Int): GachaResult<Unit> {
            users[userId] = UserInfo(id = userId, name = "user$userId")
            val userDir = File(virtualRoot, "data/user/$userId")
            userDir.mkdirs()
            return GachaResult.success(Unit)
        }

        override fun deleteUser(userId: Int): GachaResult<Unit> {
            users.remove(userId)
            return GachaResult.success(Unit)
        }

        override fun getUsers(): List<UserInfo> = users.values.toList()
    }

    inner class StubEnvironment : BEnvironment {
        override fun getDataDir(packageName: String, userId: Int): File =
            File(virtualRoot, "data/user/$userId/$packageName").apply { mkdirs() }

        override fun getExternalDataDir(packageName: String, userId: Int): File =
            File(virtualRoot, "external/user/$userId/$packageName").apply { mkdirs() }

        override fun getAppDir(packageName: String): File =
            File(virtualRoot, "data/app/$packageName").apply { mkdirs() }

        override fun getBackupRoot(): File = backupRoot

        override fun initializeDirectories(userId: Int): GachaResult<Unit> {
            File(virtualRoot, "data/user/$userId").mkdirs()
            return GachaResult.success(Unit)
        }

        override fun cleanupDirectories(userId: Int): GachaResult<Unit> =
            GachaResult.success(Unit)
    }
}
