package me.asrielyankare.gachaultils.core

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class InstanceStorageTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "gacha_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        InstanceStorage.clearAll()
        InstanceStorage.init(tempDir)
    }

    @org.junit.After
    fun tearDown() {
        InstanceStorage.clearAll()
        tempDir.deleteRecursively()
    }

    @Test
    fun `addInstance stores instance`() {
        val instance = InstanceId.create(
            id = 0,
            userId = 0,
            packageName = "com.test.game",
            gameId = GameId("com.test.game", GameType.AIR_GACHA),
            displayName = "Test Game"
        )
        InstanceStorage.addInstance(instance)

        val retrieved = InstanceStorage.getInstance(0)
        assertNotNull(retrieved)
        assertEquals("com.test.game", retrieved!!.packageName)
    }

    @Test
    fun `getAllInstances returns sorted list`() {
        InstanceStorage.addInstance(InstanceId.create(2, 2, "pkg2", GameId("pkg2", GameType.AIR_GACHA), "Game 2"))
        InstanceStorage.addInstance(InstanceId.create(0, 0, "pkg0", GameId("pkg0", GameType.AIR_GACHA), "Game 0"))
        InstanceStorage.addInstance(InstanceId.create(1, 1, "pkg1", GameId("pkg1", GameType.AIR_GACHA), "Game 1"))

        val instances = InstanceStorage.getAllInstances()
        assertEquals(3, instances.size)
        assertEquals(0, instances[0].id)
        assertEquals(1, instances[1].id)
        assertEquals(2, instances[2].id)
    }

    @Test
    fun `removeInstance deletes instance`() {
        InstanceStorage.addInstance(InstanceId.create(0, 0, "pkg", GameId("pkg", GameType.AIR_GACHA), "Game"))
        InstanceStorage.removeInstance(0)
        assertNull(InstanceStorage.getInstance(0))
    }

    @Test
    fun `getNextId returns next available id`() {
        assertEquals(0, InstanceStorage.getNextId())
        InstanceStorage.addInstance(InstanceId.create(0, 0, "pkg", GameId("pkg", GameType.AIR_GACHA), "Game"))
        assertEquals(1, InstanceStorage.getNextId())
    }

    @Test
    fun `instance persists to disk`() {
        InstanceStorage.addInstance(InstanceId.create(0, 0, "pkg", GameId("pkg", GameType.AIR_GACHA), "Game"))

        // Create new storage instance from same directory
        InstanceStorage.clearAll()
        InstanceStorage.init(tempDir)

        val retrieved = InstanceStorage.getInstance(0)
        assertNotNull(retrieved)
        assertEquals("pkg", retrieved!!.packageName)
    }

    @Test
    fun `updateInstance modifies stored instance`() {
        val instance = InstanceId.create(0, 0, "pkg", GameId("pkg", GameType.AIR_GACHA), "Old Name")
        InstanceStorage.addInstance(instance)

        val updated = instance.copy(displayName = "New Name", state = InstanceState.READY)
        InstanceStorage.updateInstance(updated)

        val retrieved = InstanceStorage.getInstance(0)
        assertEquals("New Name", retrieved!!.displayName)
        assertEquals(InstanceState.READY, retrieved.state)
    }
}
