package com.example.crossnavi.data

import com.example.crossnavi.model.BrokerInventory
import com.example.crossnavi.model.InventoryStatus
import com.example.crossnavi.model.StockPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesLogicTest {

    private val sampleStocks = listOf(
        StockPreset(
            tickerCode = "2702",
            stockName = "日本マクドナルドHD",
            months = listOf(6, 12),
            defaultPrice = 6800.0,
            defaultQuantity = 100,
            giftValue = 5000,
            giftDescription = "優待食事券1冊",
            sbiInventory = BrokerInventory(InventoryStatus.LOW_STOCK, 1400, "19:00"),
            rakutenInventory = BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
        ),
        StockPreset(
            tickerCode = "9202",
            stockName = "ANAホールディングス",
            months = listOf(3, 9),
            defaultPrice = 3000.0,
            defaultQuantity = 100,
            giftValue = 4000,
            giftDescription = "国内線50%割引券",
            sbiInventory = BrokerInventory(InventoryStatus.IN_STOCK, 145000, "19:00"),
            rakutenInventory = BrokerInventory(InventoryStatus.LOW_STOCK, 8400, "19:00")
        ),
        StockPreset(
            tickerCode = "7616",
            stockName = "コロワイド",
            months = listOf(3, 9),
            defaultPrice = 2200.0,
            defaultQuantity = 500,
            giftValue = 10000,
            giftDescription = "株主優待ポイント10,000円分",
            sbiInventory = BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00"),
            rakutenInventory = BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
        )
    )

    @Test
    fun testFavoriteTickerFiltering() {
        val favoriteSet = setOf("2702", "9202")

        // お気に入りのみで絞り込み
        val favoriteStocks = sampleStocks.filter { favoriteSet.contains(it.tickerCode) }
        assertEquals(2, favoriteStocks.size)
        assertTrue(favoriteStocks.any { it.tickerCode == "2702" })
        assertTrue(favoriteStocks.any { it.tickerCode == "9202" })
        assertFalse(favoriteStocks.any { it.tickerCode == "7616" })
    }

    @Test
    fun testFavoriteToggleLogic() {
        val set = mutableSetOf("2702")

        // 9202 を追加
        val added = set.add("9202")
        assertTrue(added)
        assertEquals(2, set.size)

        // 2702 をトグル (解除)
        val removed = set.remove("2702")
        assertTrue(removed)
        assertEquals(1, set.size)
        assertTrue(set.contains("9202"))
        assertFalse(set.contains("2702"))
    }
}
