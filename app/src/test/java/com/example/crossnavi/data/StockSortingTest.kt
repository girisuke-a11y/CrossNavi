package com.example.crossnavi.data

import com.example.crossnavi.model.BrokerInventory
import com.example.crossnavi.model.InventoryStatus
import com.example.crossnavi.model.StockPreset
import org.junit.Assert.assertEquals
import org.junit.Test

class StockSortingTest {

    private val sampleStocks = listOf(
        StockPreset(
            tickerCode = "9202",
            stockName = "ANAホールディングス",
            months = listOf(3, 9),
            defaultPrice = 3000.0,
            defaultQuantity = 100,
            giftValue = 3000, // 3000 / 300000 = 1.0%
            giftDescription = "株主優待券1枚",
            sbiInventory = BrokerInventory(InventoryStatus.IN_STOCK, 145000),
            rakutenInventory = BrokerInventory(InventoryStatus.LOW_STOCK, 8400)
        ),
        StockPreset(
            tickerCode = "7616",
            stockName = "コロワイド",
            months = listOf(3, 9),
            defaultPrice = 2000.0,
            defaultQuantity = 500,
            giftValue = 10000, // 10000 / 1000000 = 1.0%
            giftDescription = "ポイント10000円",
            sbiInventory = BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0),
            rakutenInventory = BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0)
        ),
        StockPreset(
            tickerCode = "3197",
            stockName = "すかいらーくHD",
            months = listOf(6, 12),
            defaultPrice = 2000.0,
            defaultQuantity = 100,
            giftValue = 4000, // 4000 / 200000 = 2.0%
            giftDescription = "食事カード4000円分",
            sbiInventory = BrokerInventory(InventoryStatus.LOW_STOCK, 5000),
            rakutenInventory = BrokerInventory(InventoryStatus.IN_STOCK, 15000)
        ),
        StockPreset(
            tickerCode = "9831",
            stockName = "ヤマダHD",
            months = listOf(3, 9),
            defaultPrice = 500.0,
            defaultQuantity = 100,
            giftValue = 1500, // 1500 / 50000 = 3.0% (最高利回り)
            giftDescription = "買物優待券1500円分",
            sbiInventory = BrokerInventory(InventoryStatus.LOW_STOCK, 6000),
            rakutenInventory = BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0)
        )
    )

    @Test
    fun testEstimatedYieldCalculation() {
        val yamada = sampleStocks.first { it.tickerCode == "9831" }
        assertEquals(3.0, yamada.estimatedYield, 0.001)

        val skylark = sampleStocks.first { it.tickerCode == "3197" }
        assertEquals(2.0, skylark.estimatedYield, 0.001)

        val ana = sampleStocks.first { it.tickerCode == "9202" }
        assertEquals(1.0, ana.estimatedYield, 0.001)
    }

    @Test
    fun testSortByYieldDesc() {
        val sorted = sampleStocks.sortedWith(
            compareByDescending<StockPreset> { it.estimatedYield }.thenBy { it.tickerCode }
        )
        assertEquals("9831", sorted[0].tickerCode) // 3.0%
        assertEquals("3197", sorted[1].tickerCode) // 2.0%
        assertEquals("7616", sorted[2].tickerCode) // 1.0% (code 7616 before 9202)
        assertEquals("9202", sorted[3].tickerCode) // 1.0%
    }

    @Test
    fun testSortByGiftValueDesc() {
        val sorted = sampleStocks.sortedWith(
            compareByDescending<StockPreset> { it.giftValue }.thenBy { it.tickerCode }
        )
        assertEquals("7616", sorted[0].tickerCode) // 10,000円
        assertEquals("3197", sorted[1].tickerCode) // 4,000円
        assertEquals("9202", sorted[2].tickerCode) // 3,000円
        assertEquals("9831", sorted[3].tickerCode) // 1,500円
    }

    @Test
    fun testSortByInventorySharesDesc() {
        val sorted = sampleStocks.sortedWith(
            compareByDescending<StockPreset> { it.totalInventoryShares }.thenBy { it.tickerCode }
        )
        assertEquals("9202", sorted[0].tickerCode) // 145000 + 8400 = 153400
        assertEquals("3197", sorted[1].tickerCode) // 5000 + 15000 = 20000
        assertEquals("9831", sorted[2].tickerCode) // 6000 + 0 = 6000
        assertEquals("7616", sorted[3].tickerCode) // 0 + 0 = 0
    }

    @Test
    fun testSortByCodeAsc() {
        val sorted = sampleStocks.sortedBy { it.tickerCode }
        assertEquals("3197", sorted[0].tickerCode)
        assertEquals("7616", sorted[1].tickerCode)
        assertEquals("9202", sorted[2].tickerCode)
        assertEquals("9831", sorted[3].tickerCode)
    }
}
