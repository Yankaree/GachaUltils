package me.asrielyankare.gachaultils.core

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

/**
 * Handles APK import, validation, and game profile creation.
 *
 * Flow:
 * 1. User selects APK file
 * 2. Validate APK
 * 3. Extract package metadata
 * 4. Create GameProfile
 * 5. Install into NewBlackbox
 * 6. Create Instance
 *
 * Does NOT modify, repack, or inject code into APKs.
 */
class ApkImporter(private val context: Context) {

    /**
     * Validates an APK file and extracts metadata.
     *
     * @param apkPath Path to the APK file
     * @return GameProfile if validation succeeds
     */
    fun validateAndExtract(apkPath: String): GachaResult<GameProfile> {
        val apkFile = File(apkPath)

        if (!apkFile.exists()) {
            return GachaResult.failure(GachaError.ApkValidationError(
                reason = "APK file not found: $apkPath"
            ))
        }

        if (!apkFile.canRead()) {
            return GachaResult.failure(GachaError.ApkValidationError(
                reason = "APK file not readable: $apkPath"
            ))
        }

        if (apkFile.length() < 1024) {
            return GachaResult.failure(GachaError.ApkValidationError(
                reason = "APK file too small (${apkFile.length()} bytes)"
            ))
        }

        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(apkPath, 0)
                ?: return GachaResult.failure(GachaError.ApkValidationError(
                    reason = "Cannot parse APK: $apkPath"
                ))

            val appInfo = packageInfo.applicationInfo
                ?: return GachaResult.failure(GachaError.ApkValidationError(
                    reason = "No application info in APK"
                ))

            val packageName = packageInfo.packageName
                ?: return GachaResult.failure(GachaError.ApkValidationError(
                    reason = "No package name in APK"
                ))

            val versionName = packageInfo.versionName ?: "unknown"
            val versionCode = packageInfo.versionCode

            // Determine game type based on package name patterns
            val gameType = detectGameType(packageName)

            val gameId = GameId(
                packageName = packageName,
                gameType = gameType
            )

            val gameName = appInfo.loadLabel(packageManager).toString()

            val profile = GameProfile(
                gameId = gameId,
                gameName = gameName,
                versionName = versionName,
                versionCode = versionCode,
                apkPath = apkPath,
                gameType = gameType
            )

            GachaResult.success(profile)
        } catch (e: PackageManager.NameNotFoundException) {
            GachaResult.failure(GachaError.ApkValidationError(
                reason = "Package not found in APK: ${e.message}"
            ))
        } catch (e: Exception) {
            GachaResult.failure(GachaError.ApkValidationError(
                reason = "Failed to validate APK: ${e.message}"
            ))
        }
    }

    /**
     * Imports an APK: validates, installs, and creates instance.
     *
     * @param apkPath Path to APK file
     * @param displayName Display name for the instance
     * @param instanceManager InstanceManager to create the instance
     * @return Created InstanceId
     */
    fun importApk(
        apkPath: String,
        displayName: String,
        instanceManager: InstanceManager
    ): GachaResult<InstanceId> {
        // 1. Validate and extract profile
        val profileResult = validateAndExtract(apkPath)
        val profile = when (profileResult) {
            is GachaResult.Success -> profileResult.data
            is GachaResult.Failure -> return GachaResult.failure(profileResult.error)
        }

        // 2. Create instance
        val createResult = instanceManager.createInstance(
            packageName = profile.gameId.packageName,
            gameId = profile.gameId,
            displayName = displayName.ifEmpty { profile.gameName },
            apkPath = apkPath
        )
        val instance = when (createResult) {
            is GachaResult.Success -> createResult.data
            is GachaResult.Failure -> return GachaResult.failure(createResult.error)
        }

        // 3. Install APK into instance
        val installResult = instanceManager.installApk(instance.id, apkPath)
        return when (installResult) {
            is GachaResult.Success -> GachaResult.success(installResult.data)
            is GachaResult.Failure -> GachaResult.failure(installResult.error)
        }
    }

    /**
     * Detects the game type from package name.
     */
    private fun detectGameType(packageName: String): GameType {
        return when {
            packageName.startsWith("air.com.lunime.") -> GameType.AIR_GACHA
            packageName.contains("gacha", ignoreCase = true) -> GameType.AIR_GACHA
            else -> GameType.UNSPECIFIED
        }
    }
}
