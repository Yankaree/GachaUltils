package me.asrielyankare.gachaultils.blackbox

import android.content.Context
import android.content.Intent
import me.asrielyankare.gachaultils.core.ApplicationInfo
import me.asrielyankare.gachaultils.core.BActivityManager
import me.asrielyankare.gachaultils.core.BEnvironment
import me.asrielyankare.gachaultils.core.BPackageManager
import me.asrielyankare.gachaultils.core.BUserManager
import me.asrielyankare.gachaultils.core.BlackBoxRegistry
import me.asrielyankare.gachaultils.core.GachaError
import me.asrielyankare.gachaultils.core.GachaResult
import me.asrielyankare.gachaultils.core.InstallInfo
import me.asrielyankare.gachaultils.core.InstanceState
import me.asrielyankare.gachaultils.core.UserInfo
import top.niunaijun.blackbox.entity.pm.InstallOption
import top.niunaijun.blackbox.entity.pm.InstallResult
import java.io.File

/**
 * Real NewBlackbox integration layer.
 * Wraps BlackBoxCore API and provides BlackBoxCore interface used by core module.
 */
class NewBlackboxIntegration(private val context: Context) {

    private var initialized = false

    fun initialize(): GachaResult<Unit> {
        return try {
            // Initialize NewBlackbox with app context
            top.niunaijun.blackbox.BlackBoxCore.get().doAttachBaseContext(context, object : top.niunaijun.blackbox.app.configuration.ClientConfiguration() {
                override fun isHideRoot() = false
                override fun isDisableFlagSecure() = false
                override fun getHostPackageName() = context.packageName
                override fun isEnableDaemonService() = true
                override fun isEnableLauncherActivity() = false
                override fun requestInstallPackage(file: File?, userId: Int) = true
            })
            top.niunaijun.blackbox.BlackBoxCore.get().doCreate()
            initialized = true
            GachaResult.success(Unit)
        } catch (e: Throwable) {
            android.util.Log.e("NewBlackboxIntegration", "Init failed: ${e.javaClass.simpleName}: ${e.message}", e)
            GachaResult.failure(GachaError.BlackBoxInitializationError(
                message = "Failed to initialize NewBlackbox: ${e.javaClass.simpleName}: ${e.message}",
                cause = e
            ))
        }
    }

    fun isInitialized(): Boolean = initialized

    fun registerImplementations() {
        BlackBoxRegistry.registerPackageManager(NewBlackboxPackageManager())
        BlackBoxRegistry.registerActivityManager(NewBlackboxActivityManager())
        BlackBoxRegistry.registerUserManager(NewBlackboxUserManager())
        BlackBoxRegistry.registerEnvironment(NewBlackboxEnvironment())
    }

    /**
     * Real NewBlackbox PackageManager implementation.
     */
    inner class NewBlackboxPackageManager : me.asrielyankare.gachaultils.core.BPackageManager {
        override fun installPackageAsUser(apkPath: String, userId: Int): GachaResult<InstallInfo> {
            return try {
                val result: InstallResult = top.niunaijun.blackbox.BlackBoxCore.getBPackageManager()
                    .installPackageAsUser(apkPath, InstallOption.installByStorage(), userId)
                if (result.success) {
                    GachaResult.success(InstallInfo(
                        packageName = result.packageName ?: "",
                        success = true,
                        message = "Installed via NewBlackbox"
                    ))
                } else {
                    GachaResult.failure(GachaError.PackageInstallError(
                        packageName = result.packageName ?: "",
                        reason = result.msg ?: "Install failed"
                    ))
                }
            } catch (e: Exception) {
                GachaResult.failure(GachaError.PackageInstallError(
                    packageName = "",
                    reason = "NewBlackbox install failed: ${e.message}"
                ))
            }
        }

        override fun uninstallPackageAsUser(packageName: String, userId: Int): GachaResult<Unit> {
            return try {
                top.niunaijun.blackbox.BlackBoxCore.get().uninstallPackageAsUser(packageName, userId)
                GachaResult.success(Unit)
            } catch (e: Exception) {
                GachaResult.failure(GachaError.PackageInstallError(
                    packageName = packageName,
                    reason = "NewBlackbox uninstall failed: ${e.message}"
                ))
            }
        }

        override fun getLaunchIntentForPackage(packageName: String, userId: Int): Any? {
            return try {
                top.niunaijun.blackbox.BlackBoxCore.getBPackageManager().getLaunchIntentForPackage(packageName, userId)
            } catch (e: Exception) {
                null
            }
        }

        override fun stopPackage(packageName: String, userId: Int): GachaResult<Unit> {
            return try {
                top.niunaijun.blackbox.BlackBoxCore.get().stopPackage(packageName, userId)
                GachaResult.success(Unit)
            } catch (e: Exception) {
                GachaResult.failure(GachaError.InvalidState(
                    currentState = InstanceState.RUNNING,
                    attemptedOperation = "stopPackage",
                    message = "NewBlackbox stopPackage failed: ${e.message}"
                ))
            }
        }

        override fun isInstalled(packageName: String, userId: Int): Boolean {
            return try {
                top.niunaijun.blackbox.BlackBoxCore.get().isInstalled(packageName, userId)
            } catch (e: Exception) {
                false
            }
        }

        override fun getApplicationInfo(packageName: String, userId: Int): me.asrielyankare.gachaultils.core.ApplicationInfo? {
            return try {
                val appInfo = top.niunaijun.blackbox.BlackBoxCore.getBPackageManager().getApplicationInfo(packageName, 0, userId)
                me.asrielyankare.gachaultils.core.ApplicationInfo(
                    packageName = packageName,
                    versionName = "unknown",
                    versionCode = 0,
                    sourceDir = appInfo?.sourceDir ?: "",
                    dataDir = appInfo?.dataDir ?: ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Real NewBlackbox ActivityManager implementation.
     */
    inner class NewBlackboxActivityManager : me.asrielyankare.gachaultils.core.BActivityManager {
        override fun startActivity(intent: Any, userId: Int): GachaResult<Unit> {
            return try {
                val realIntent = intent as? Intent
                    ?: return GachaResult.failure(GachaError.InvalidState(
                        currentState = InstanceState.RUNNING,
                        attemptedOperation = "startActivity",
                        message = "Intent must be android.content.Intent"
                    ))
                top.niunaijun.blackbox.BlackBoxCore.get().startActivity(realIntent, userId)
                GachaResult.success(Unit)
            } catch (e: Exception) {
                GachaResult.failure(GachaError.InvalidState(
                    currentState = InstanceState.RUNNING,
                    attemptedOperation = "startActivity",
                    message = "NewBlackbox startActivity failed: ${e.message}"
                ))
            }
        }
    }

    /**
     * Real NewBlackbox UserManager implementation.
     */
    inner class NewBlackboxUserManager : me.asrielyankare.gachaultils.core.BUserManager {
        override fun createUser(userId: Int): GachaResult<Unit> {
            return try {
                top.niunaijun.blackbox.BlackBoxCore.get().createUser(userId)
                GachaResult.success(Unit)
            } catch (e: Exception) {
                GachaResult.failure(GachaError.UserCreationError(userId))
            }
        }

        override fun deleteUser(userId: Int): GachaResult<Unit> {
            return try {
                top.niunaijun.blackbox.BlackBoxCore.get().deleteUser(userId)
                GachaResult.success(Unit)
            } catch (e: Exception) {
                GachaResult.failure(GachaError.UserCreationError(userId))
            }
        }

        override fun getUsers(): List<UserInfo> {
            return try {
                top.niunaijun.blackbox.BlackBoxCore.get().users.map { bui ->
                    UserInfo(id = bui.id, name = bui.name)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Real NewBlackbox Environment implementation.
     */
    inner class NewBlackboxEnvironment : me.asrielyankare.gachaultils.core.BEnvironment {
        override fun getDataDir(packageName: String, userId: Int): File {
            return try {
                top.niunaijun.blackbox.core.env.BEnvironment.getDataDir(packageName, userId)
            } catch (e: Exception) {
                File(context.filesDir, "blackbox/data/user/$userId/$packageName").apply { mkdirs() }
            }
        }

        override fun getExternalDataDir(packageName: String, userId: Int): File {
            return try {
                top.niunaijun.blackbox.core.env.BEnvironment.getExternalDataDir(packageName, userId)
            } catch (e: Exception) {
                File(context.filesDir, "blackbox/external/user/$userId/$packageName").apply { mkdirs() }
            }
        }

        override fun getAppDir(packageName: String): File {
            return try {
                top.niunaijun.blackbox.core.env.BEnvironment.getAppDir(packageName)
            } catch (e: Exception) {
                File(context.filesDir, "blackbox/data/app/$packageName").apply { mkdirs() }
            }
        }

        override fun getBackupRoot(): File {
            return File(context.filesDir, "backups").apply { mkdirs() }
        }

        override fun initializeDirectories(userId: Int): GachaResult<Unit> {
            return try {
                top.niunaijun.blackbox.core.env.BEnvironment.load()
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
