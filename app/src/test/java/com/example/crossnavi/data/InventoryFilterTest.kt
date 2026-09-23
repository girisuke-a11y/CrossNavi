package com.example.crossnavi.data

import com.example.crossnavi.model.BrokerInventory
import com.example.crossnavi.model.InventoryStatus
import com.example.crossnavi.model.StockPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryFilterTest {

    private val sampleStocks = listOf(
        StockPreset(
            tickerCode = "2702",
            stockName = "日本マクドナルドHD",
            months = listOf(6, 12),
            defaultPrice = 6800.0,
            defaultQuantity = 100,
            giftValue = 5000,
            giftDescription = "優待食事券1冊",
            isSbiShort = true,
            isRakutenShort = true,
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
            isSbiShort = true,
            isRakutenShort = true,
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
            isSbiShort = true,
            isRakutenShort = true,
            sbiInventory = BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00"),
            rakutenInventory = BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
        ),
        StockPreset(
            tickerCode = "8267",
            stockName = "イオン",
            months = listOf(2, 8),
            defaultPrice = 3500.0,
            defaultQuantity = 100,
            giftValue = 3000,
            giftDescription = "オーナーズカード (3%キャッシュバック)",
            isSbiShort = true,
            isRakutenShort = false,
            sbiInventory = BrokerInventory(InventoryStatus.IN_STOCK, 82000, "19:00"),
            rakutenInventory = BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
        )
    )

    @Test
    fun testInventoryStatusBadgeAndFormatting() {
        val inStock = BrokerInventory(InventoryStatus.IN_STOCK, 145000, "19:00")
        assertEquals("在庫あり", inStock.status.displayName)
        assertEquals("◎ 在庫あり", inStock.status.shortBadge)
        assertEquals("14.5万株", inStock.formattedShares)

        val lowStock = BrokerInventory(InventoryStatus.LOW_STOCK, 3200, "19:00")
        assertEquals("残りわずか", lowStock.status.displayName)
        assertEquals("△ わずか", lowStock.status.shortBadge)
        assertEquals("3200株", lowStock.formattedShares)

        val outOfStock = BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
        assertEquals("在庫なし", outOfStock.status.displayName)
        assertEquals("✕ なし", outOfStock.status.shortBadge)
        assertEquals("0株", outOfStock.formattedShares)
    }

    @Test
    fun testFilterAll() {
        // 全件取得 (4件)
        val filtered = sampleStocks.filter { true }
        assertEquals(4, filtered.size)
    }

    @Test
    fun testFilterAnyInStock() {
        // SBIまたは楽天のどちらかに在庫がある銘柄 (マクドナルド、ANA、イオン の3件。コロワイドは両方✕なので除外)
        val filtered = sampleStocks.filter {
            it.sbiInventory.status != InventoryStatus.OUT_OF_STOCK ||
                    it.rakutenInventory.status != InventoryStatus.OUT_OF_STOCK
        }
        assertEquals(3, filtered.size)
        assertTrue(filtered.any { it.tickerCode == "2702" }) // SBIにある
        assertTrue(filtered.any { it.tickerCode == "9202" }) // 両方にある
        assertTrue(filtered.any { it.tickerCode == "8267" }) // SBIにある
        assertFalse(filtered.any { it.tickerCode == "7616" }) // コロワイドはなし
    }

    @Test
    fun testFilterSbiInStock() {
        // SBIに在庫がある銘柄 (マクドナルド、ANA、イオン)
        val filtered = sampleStocks.filter {
            it.sbiInventory.status != InventoryStatus.OUT_OF_STOCK
        }
        assertEquals(3, filtered.size)
        assertFalse(filtered.any { it.tickerCode == "7616" })
    }

    @Test
    fun testFilterRakutenInStock() {
        // 楽天に在庫がある銘柄 (ANAのみ: △残りわずか)
        val filtered = sampleStocks.filter {
            it.rakutenInventory.status != InventoryStatus.OUT_OF_STOCK
        }
        assertEquals(1, filtered.size)
        assertEquals("9202", filtered.first().tickerCode)
    }
}
