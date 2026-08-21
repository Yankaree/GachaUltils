package me.asrielyankare.gachaultils.core

import org.junit.Assert.*
import org.junit.Test

class GameIdTest {

    @Test
    fun `GameId equality works correctly`() {
        val id1 = GameId("air.com.lunime.gachalife2", GameType.AIR_GACHA)
        val id2 = GameId("air.com.lunime.gachalife2", GameType.AIR_GACHA)
        assertEquals(id1, id2)
    }

    @Test
    fun `GameId with different package names are not equal`() {
        val id1 = GameId("air.com.lunime.gachalife2", GameType.AIR_GACHA)
        val id2 = GameId("air.com.lunime.gachaclub", GameType.AIR_GACHA)
        assertNotEquals(id1, id2)
    }

    @Test
    fun `GameId with different types are not equal`() {
        val id1 = GameId("air.com.lunime.gachalife2", GameType.AIR_GACHA)
        val id2 = GameId("air.com.lunime.gachalife2", GameType.UNSPECIFIED)
        assertNotEquals(id1, id2)
    }
}
