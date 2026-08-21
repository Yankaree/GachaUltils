package me.asrielyankare.gachaultils.core

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InstanceOperationLockTest {

    @Before
    fun setUp() {
        InstanceOperationLock.clearAll()
    }

    @Test
    fun `lock prevents concurrent operations`() = runTest {
        val instanceId = 0
        val results = mutableListOf<String>()

        // Start first operation
        val job1 = launch {
            InstanceOperationLock.withInstanceLock(instanceId, "op1") {
                delay(100)
                results.add("op1")
            }
        }

        delay(10) // Let first operation start

        // Try second operation - should fail
        val result2 = InstanceOperationLock.withInstanceLock(instanceId, "op2") {
            results.add("op2")
        }

        job1.join()

        assertTrue(result2 is GachaResult.Failure)
        assertTrue((result2 as GachaResult.Failure).error is GachaError.OperationInProgress)
    }

    @Test
    fun `different instances can run concurrently`() = runTest {
        val results = mutableListOf<String>()

        val job1 = launch {
            InstanceOperationLock.withInstanceLock(0, "op1") {
                delay(50)
                results.add("op1")
            }
        }

        val job2 = launch {
            InstanceOperationLock.withInstanceLock(1, "op2") {
                delay(50)
                results.add("op2")
            }
        }

        joinAll(job1, job2)

        assertEquals(2, results.size)
        assertTrue(results.contains("op1"))
        assertTrue(results.contains("op2"))
    }

    @Test
    fun `isLocked reflects lock state`() = runTest {
        val instanceId = 0

        assertFalse(InstanceOperationLock.isLocked(instanceId))

        val job = launch {
            InstanceOperationLock.withInstanceLock(instanceId, "op") {
                assertTrue(InstanceOperationLock.isLocked(instanceId))
                delay(100)
            }
        }

        delay(10)
        assertTrue(InstanceOperationLock.isLocked(instanceId))

        job.join()
        assertFalse(InstanceOperationLock.isLocked(instanceId))
    }

    @Test
    fun `removeLock cleans up`() {
        InstanceOperationLock.clearAll()
        assertFalse(InstanceOperationLock.isLocked(0))
    }
}
