package me.asrielyankare.gachaultils.core

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class InstanceManagerTest {

    private lateinit var manager: InstanceManager
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "gacha_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        InstanceStorage.clearAll()
        InstanceOperationLock.clearAll()
        InstanceStorage.initInMemory()

        // Register stub implementations for testing
        BlackBoxRegistry.registerPackageManager(StubTestPackageManager())
        BlackBoxRegistry.registerActivityManager(StubTestActivityManager())
        BlackBoxRegistry.registerUserManager(StubTestUserManager())
        BlackBoxRegistry.registerEnvironment(StubTestEnvironment(tempDir))

        manager = InstanceManager()
    }

    @org.junit.After
    fun tearDown() {
        InstanceStorage.clearAll()
        InstanceOperationLock.clearAll()
        tempDir.deleteRecursively()
    }

    @Test
    fun `createInstance creates and stores instance`() {
        val result = manager.createInstance(
            packageName = "com.test.game",
            gameId = GameId("com.test.game", GameType.AIR_GACHA),
            displayName = "Test Game"
        )

        assertTrue(result is GachaResult.Success)
        val instance = (result as GachaResult.Success).data
        assertEquals("com.test.game", instance.packageName)
        assertEquals(InstanceState.CREATED, instance.state)
    }

    @Test
    fun `createInstance assigns incrementing IDs`() {
        val result1 = manager.createInstance("pkg1", GameId("pkg1", GameType.AIR_GACHA), "Game 1")
        val result2 = manager.createInstance("pkg2", GameId("pkg2", GameType.AIR_GACHA), "Game 2")

        assertTrue(result1 is GachaResult.Success)
        assertTrue(result2 is GachaResult.Success)
        assertEquals(0, (result1 as GachaResult.Success).data.id)
        assertEquals(1, (result2 as GachaResult.Success).data.id)
    }

    @Test
    fun `listInstances returns all instances`() {
        manager.createInstance("pkg1", GameId("pkg1", GameType.AIR_GACHA), "Game 1")
        manager.createInstance("pkg2", GameId("pkg2", GameType.AIR_GACHA), "Game 2")

        val instances = manager.listInstances()
        assertEquals(2, instances.size)
    }

    @Test
    fun `deleteInstance removes instance`() {
        val result = manager.createInstance("pkg", GameId("pkg", GameType.AIR_GACHA), "Game")
        val instance = (result as GachaResult.Success).data

        val deleteResult = manager.deleteInstance(instance.id)
        assertTrue(deleteResult is GachaResult.Success)

        assertNull(manager.getInstance(instance.id))
    }

    @Test
    fun `getInstance returns null for non-existent instance`() {
        assertNull(manager.getInstance(999))
    }

    @Test
    fun `launchInstance fails for non-existent instance`() {
        val result = manager.launchInstance(999)
        assertTrue(result is GachaResult.Failure)
        assertTrue((result as GachaResult.Failure).error is GachaError.InstanceNotFound)
    }

    @Test
    fun `stopInstance fails for non-running instance`() {
        val createResult = manager.createInstance("pkg", GameId("pkg", GameType.AIR_GACHA), "Game")
        val instance = (createResult as GachaResult.Success).data

        val stopResult = manager.stopInstance(instance.id)
        assertTrue(stopResult is GachaResult.Failure)
        assertTrue((stopResult as GachaResult.Failure).error is GachaError.InvalidState)
    }

    // Stub implementations for testing
    private class StubTestPackageManager : BPackageManager {
        override fun installPackageAsUser(apkPath: String, userId: Int): GachaResult<InstallInfo> {
            return GachaResult.success(InstallInfo("test.pkg", true))
        }
        override fun uninstallPackageAsUser(packageName: String, userId: Int) = GachaResult.success(Unit)
        override fun getLaunchIntentForPackage(packageName: String, userId: Int) = Any()
        override fun stopPackage(packageName: String, userId: Int) = GachaResult.success(Unit)
        override fun isInstalled(packageName: String, userId: Int) = true
        override fun getApplicationInfo(packageName: String, userId: Int) = null
    }

    private class StubTestActivityManager : BActivityManager {
        override fun startActivity(intent: Any, userId: Int) = GachaResult.success(Unit)
    }

    private class StubTestUserManager : BUserManager {
        private val users = mutableMapOf<Int, UserInfo>()
        override fun createUser(userId: Int): GachaResult<Unit> {
            users[userId] = UserInfo(userId, "user$userId")
            return GachaResult.success(Unit)
        }
        override fun deleteUser(userId: Int) = GachaResult.success(Unit)
        override fun getUsers() = users.values.toList()
    }

    private class StubTestEnvironment(private val root: File) : BEnvironment {
        override fun getDataDir(packageName: String, userId: Int) = File(root, "data/user/$userId/$packageName").apply { mkdirs() }
        override fun getExternalDataDir(packageName: String, userId: Int) = File(root, "external/$userId/$packageName")
        override fun getAppDir(packageName: String) = File(root, "app/$packageName")
        override fun getBackupRoot() = File(root, "backups").apply { mkdirs() }
        override fun initializeDirectories(userId: Int) = GachaResult.success(Unit)
        override fun cleanupDirectories(userId: Int) = GachaResult.success(Unit)
    }
}
