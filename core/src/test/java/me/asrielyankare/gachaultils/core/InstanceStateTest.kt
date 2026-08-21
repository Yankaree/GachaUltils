package me.asrielyankare.gachaultils.core

import org.junit.Assert.*
import org.junit.Test

class InstanceStateTest {

    @Test
    fun `CREATED can transition to INSTALLING`() {
        assertTrue(InstanceState.CREATED.isTransitionAllowed(InstanceState.INSTALLING))
    }

    @Test
    fun `CREATED can transition to ERROR`() {
        assertTrue(InstanceState.CREATED.isTransitionAllowed(InstanceState.ERROR))
    }

    @Test
    fun `CREATED cannot transition to RUNNING`() {
        assertFalse(InstanceState.CREATED.isTransitionAllowed(InstanceState.RUNNING))
    }

    @Test
    fun `INSTALLING can transition to READY`() {
        assertTrue(InstanceState.INSTALLING.isTransitionAllowed(InstanceState.READY))
    }

    @Test
    fun `INSTALLING cannot transition to RUNNING`() {
        assertFalse(InstanceState.INSTALLING.isTransitionAllowed(InstanceState.RUNNING))
    }

    @Test
    fun `READY can transition to RUNNING`() {
        assertTrue(InstanceState.READY.isTransitionAllowed(InstanceState.RUNNING))
    }

    @Test
    fun `RUNNING can transition to STOPPING`() {
        assertTrue(InstanceState.RUNNING.isTransitionAllowed(InstanceState.STOPPING))
    }

    @Test
    fun `STOPPING can transition to READY`() {
        assertTrue(InstanceState.STOPPING.isTransitionAllowed(InstanceState.READY))
    }

    @Test
    fun `ERROR can transition to CREATED`() {
        assertTrue(InstanceState.ERROR.isTransitionAllowed(InstanceState.CREATED))
    }

    @Test
    fun `ERROR can transition to READY`() {
        assertTrue(InstanceState.ERROR.isTransitionAllowed(InstanceState.READY))
    }
}
