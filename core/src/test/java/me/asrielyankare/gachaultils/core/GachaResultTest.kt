package me.asrielyankare.gachaultils.core

import org.junit.Assert.*
import org.junit.Test

class GachaResultTest {

    @Test
    fun `Success result contains data`() {
        val result = GachaResult.success("hello")
        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals("hello", result.getOrNull())
    }

    @Test
    fun `Failure result contains error`() {
        val result = GachaResult.failure<String>(GachaError.Unknown("test error"))
        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getOrThrow returns data on success`() {
        val result = GachaResult.success(42)
        assertEquals(42, result.getOrThrow())
    }

    @Test(expected = GachaException::class)
    fun `getOrThrow throws on failure`() {
        val result = GachaResult.failure<String>(GachaError.Unknown("error"))
        result.getOrThrow()
    }

    @Test
    fun `map transforms success data`() {
        val result = GachaResult.success(5)
        val mapped = result.map { it * 2 }
        assertEquals(10, mapped.getOrNull())
    }

    @Test
    fun `map passes through failure`() {
        val result = GachaResult.failure<Int>(GachaError.Unknown("error"))
        val mapped = result.map { it * 2 }
        assertTrue(mapped.isFailure)
    }

    @Test
    fun `flatMap chains operations`() {
        val result = GachaResult.success(5)
        val chained = result.flatMap { GachaResult.success(it * 2) }
        assertEquals(10, chained.getOrNull())
    }

    @Test
    fun `runCatching wraps exceptions`() {
        val result = GachaResult.runCatching {
            throw RuntimeException("boom")
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun `runCatching wraps GachaException`() {
        val result = GachaResult.runCatching {
            throw GachaException(GachaError.InstanceNotFound(42))
        }
        assertTrue(result.isFailure)
        val error = (result as GachaResult.Failure).error
        assertTrue(error is GachaError.InstanceNotFound)
    }
}
