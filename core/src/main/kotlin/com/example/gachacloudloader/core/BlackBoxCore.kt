package com.example.gachacloudloader.core

import java.io.File

/**
 * Stub for BlackBoxCore - will be replaced by NewBlackbox integration.
 */
object BlackBoxCore {
    private val bPackageManager = BPackageManagerStub()
    private val bActivityManager = BActivityManagerStub()
    private val bUserManager = BUserManagerStub()
    private val bEnvironment = BEnvironmentStub()

    fun getBPackageManager(): BPackageManagerStub = bPackageManager
    fun getBActivityManager(): BActivityManagerStub = bActivityManager
    fun getBUserManager(): BUserManagerStub = bUserManager
    fun getBEnvironment(): BEnvironmentStub = bEnvironment
}

class BPackageManagerStub {
    fun isUserValid(userId: Int): Boolean = true
    fun setCurrentUser(userId: Int) {}
    fun installPackageAsUser(apkPath: String, options: Any, userId: Int): InstallResultStub = InstallResultStub()
    fun getLaunchIntentForPackage(packageName: String, userId: Int): Any? = null
    fun stopPackage(packageName: String, userId: Int) {}
}

class BActivityManagerStub {
    fun startActivity(intent: Any, userId: Int) {}
}

class BUserManagerStub {
    fun addUser(userId: Int) {}
    fun removeUser(userId: Int) {}
}

class BEnvironmentStub {
    fun initializeInstanceDirectories(userId: Int) {}
    fun setCurrentUser(userId: Int) {}
    fun cleanupInstanceDirectories(userId: Int) {}
    fun getDataDir(packageName: String, userId: Int): String = "/data/user/$userId/$packageName"
}

class InstallResultStub {
    val success: Boolean = true
}
