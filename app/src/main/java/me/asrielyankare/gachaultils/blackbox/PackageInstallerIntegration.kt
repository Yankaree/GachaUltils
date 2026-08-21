package me.asrielyankare.gachaultils.blackbox

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import me.asrielyankare.gachaultils.core.*
import java.io.File
import java.io.FileInputStream

/**
 * PackageInstaller-based integration that installs APKs directly on the device.
 * No virtualization — apps run natively using Android's PackageInstaller API.
 */
class PackageInstallerIntegration(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun initialize(): GachaResult<Unit> = GachaResult.success(Unit)
    fun isInitialized(): Boolean = true

    fun registerImplementations() {
        BlackBoxRegistry.registerPackageManager(DirectPackageManager())
        BlackBoxRegistry.registerActivityManager(DirectActivityManager())
        BlackBoxRegistry.registerUserManager(DirectUserManager())
        BlackBoxRegistry.registerEnvironment(DirectEnvironment())
    }

    /**
     * Installs APK directly on device using PackageInstaller API.
     */
    fun installApk(apkPath: String): GachaResult<InstallInfo> {
        return try {
            val installer = packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)

            val apkFile = File(apkPath)
            FileInputStream(apkFile).use { input ->
                session.openWrite("base.apk", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            val intent = Intent(context, InstallResultReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, sessionId, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
            )

            session.commit(pendingIntent.intentSender)

            val packageName = packageManager.getPackageArchiveInfo(apkPath, 0)
                ?.packageName ?: apkFile.nameWithoutExtension

            GachaResult.success(InstallInfo(
                packageName = packageName,
                success = true,
                message = "Installed via PackageInstaller"
            ))
        } catch (e: Exception) {
            GachaResult.failure(GachaError.PackageInstallError(
                packageName = "",
                reason = "PackageInstaller failed: ${e.message}"
            ))
        }
    }

    /**
     * Direct PackageManager implementation.
     */
    inner class DirectPackageManager : BPackageManager {
        override fun installPackageAsUser(apkPath: String, userId: Int): GachaResult<InstallInfo> {
            return installApk(apkPath)
        }

        override fun uninstallPackageAsUser(packageName: String, userId: Int): GachaResult<Unit> {
            return try {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                GachaResult.success(Unit)
            } catch (e: Exception) {
                GachaResult.failure(GachaError.PackageInstallError(
                    packageName = packageName,
                    reason = "Uninstall failed: ${e.message}"
                ))
            }
        }

        override fun getLaunchIntentForPackage(packageName: String, userId: Int): Any? {
            return try {
                packageManager.getLaunchIntentForPackage(packageName)
            } catch (e: Exception) {
                null
            }
        }

        override fun stopPackage(packageName: String, userId: Int): GachaResult<Unit> {
            return try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
                    as? android.app.ActivityManager
                activityManager?.killBackgroundProcesses(packageName)
                GachaResult.success(Unit)
            } catch (e: Exception) {
                GachaResult.failure(GachaError.InvalidState(
                    currentState = InstanceState.RUNNING,
                    attemptedOperation = "stopPackage",
                    message = "Stop failed: ${e.message}"
                ))
            }
        }

        override fun isInstalled(packageName: String, userId: Int): Boolean {
            return try {
                packageManager.getPackageInfo(packageName, 0) != null
            } catch (e: Exception) {
                false
            }
        }

        override fun getApplicationInfo(packageName: String, userId: Int): ApplicationInfo? {
            return try {
                val info = packageManager.getApplicationInfo(packageName, 0)
                ApplicationInfo(
                    packageName = packageName,
                    versionName = try {
                        packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
                    } catch (_: Exception) { "unknown" },
                    versionCode = try {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(packageName, 0).versionCode
                    } catch (_: Exception) { 0 },
                    sourceDir = info.sourceDir ?: "",
                    dataDir = info.dataDir ?: ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Direct ActivityManager — starts app's main activity.
     */
    inner class DirectActivityManager : BActivityManager {
        override fun startActivity(intent: Any, userId: Int): GachaResult<Unit> {
            return try {
                val realIntent = intent as? Intent
                    ?: return GachaResult.failure(GachaError.InvalidState(
                        currentState = InstanceState.RUNNING,
                        attemptedOperation = "startActivity",
                        message = "Intent must be android.content.Intent"
                    ))
                realIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(realIntent)
                GachaResult.success(Unit)
            } catch (e: Exception) {
                GachaResult.failure(GachaError.InvalidState(
                    currentState = InstanceState.RUNNING,
                    attemptedOperation = "startActivity",
                    message = "Start activity failed: ${e.message}"
                ))
            }
        }
    }

    /**
     * Direct UserManager — single user (the device user).
     * No virtual users needed without virtualization.
     */
    inner class DirectUserManager : BUserManager {
        override fun createUser(userId: Int): GachaResult<Unit> = GachaResult.success(Unit)
        override fun deleteUser(userId: Int): GachaResult<Unit> = GachaResult.success(Unit)
        override fun getUsers(): List<UserInfo> = listOf(UserInfo(id = 0, name = "device"))
    }

    /**
     * Direct Environment — uses device filesystem directly.
     */
    inner class DirectEnvironment : BEnvironment {
        override fun getDataDir(packageName: String, userId: Int): File {
            return try {
                val info = packageManager.getApplicationInfo(packageName, 0)
                File(info.dataDir ?: context.filesDir.absolutePath)
            } catch (_: Exception) {
                File(context.filesDir, "app_data/$packageName").apply { mkdirs() }
            }
        }

        override fun getExternalDataDir(packageName: String, userId: Int): File {
            return try {
                val info = packageManager.getApplicationInfo(packageName, 0)
                File(info.dataDir ?: "").parentFile ?: File(context.filesDir, "app_external/$packageName")
            } catch (_: Exception) {
                File(context.filesDir, "app_external/$packageName").apply { mkdirs() }
            }
        }

        override fun getAppDir(packageName: String): File {
            return try {
                val info = packageManager.getApplicationInfo(packageName, 0)
                File(info.sourceDir ?: "").parentFile ?: File(context.filesDir, "app_apk/$packageName")
            } catch (_: Exception) {
                File(context.filesDir, "app_apk/$packageName").apply { mkdirs() }
            }
        }

        override fun getBackupRoot(): File = File(context.filesDir, "backups").apply { mkdirs() }

        override fun initializeDirectories(userId: Int): GachaResult<Unit> = GachaResult.success(Unit)
        override fun cleanupDirectories(userId: Int): GachaResult<Unit> = GachaResult.success(Unit)
    }
}

/**
 * Dummy receiver for PackageInstaller session result.
 */
class InstallResultReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        android.util.Log.d("InstallResult", "Install status: $status")
    }
}
