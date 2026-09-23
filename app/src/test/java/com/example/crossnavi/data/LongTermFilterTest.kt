package com.example.crossnavi.data

import com.example.crossnavi.model.BrokerInventory
import com.example.crossnavi.model.InventoryStatus
import com.example.crossnavi.model.StockPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LongTermFilterTest {

    private val sampleStocks = listOf(
        StockPreset(
            tickerCode = "2702",
            stockName = "日本マクドナルドHD",
            months = listOf(6, 12),
            defaultPrice = 6800.0,
            defaultQuantity = 100,
            giftValue = 5000,
            giftDescription = "優待食事券1冊",
            requiresLongTerm = true,
            longTermDescription = "1年以上継続保有が必須"
        ),
        StockPreset(
            tickerCode = "9433",
            stockName = "KDDI",
            months = listOf(3),
            defaultPrice = 4800.0,
            defaultQuantity = 100,
            giftValue = 3000,
            giftDescription = "Pontaポイント選べるギフト",
            requiresLongTerm = true,
            longTermDescription = "1年以上継続保有が必須"
        ),
        StockPreset(
            tickerCode = "9202",
            stockName = "ANAホールディングス",
            months = listOf(3, 9),
            defaultPrice = 3000.0,
            defaultQuantity = 100,
            giftValue = 3500,
            giftDescription = "国内線50%割引券",
            requiresLongTerm = false,
            longTermDescription = ""
        ),
        StockPreset(
            tickerCode = "8267",
            stockName = "イオン",
            months = listOf(2, 8),
            defaultPrice = 3800.0,
            defaultQuantity = 100,
            giftValue = 4000,
            giftDescription = "オーナーズカード (3%キャッシュバック)",
            requiresLongTerm = false,
            longTermDescription = ""
        )
    )

    @Test
    fun testLongTermFilterTypeLabels() {
        assertEquals("長期問わず", LongTermFilterType.ALL.label)
        assertEquals("短期OK (長期不要)", LongTermFilterType.SHORT_TERM_OK.label)
        assertEquals("長期条件あり", LongTermFilterType.REQUIRES_LONG_TERM.label)
    }

    @Test
    fun testFilterAll() {
        val filtered = sampleStocks.filter { stock ->
            when (LongTermFilterType.ALL) {
                LongTermFilterType.ALL -> true
                LongTermFilterType.SHORT_TERM_OK -> !stock.requiresLongTerm
                LongTermFilterType.REQUIRES_LONG_TERM -> stock.requiresLongTerm
            }
        }
        assertEquals(4, filtered.size)
    }

    @Test
    fun testFilterShortTermOk() {
        val filtered = sampleStocks.filter { stock ->
            when (LongTermFilterType.SHORT_TERM_OK) {
                LongTermFilterType.ALL -> true
                LongTermFilterType.SHORT_TERM_OK -> !stock.requiresLongTerm
                LongTermFilterType.REQUIRES_LONG_TERM -> stock.requiresLongTerm
            }
        }
        // ANA (9202) と イオン (8267) のみ
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.tickerCode == "9202" })
        assertTrue(filtered.any { it.tickerCode == "8267" })
        assertFalse(filtered.any { it.tickerCode == "2702" }) // マクドナルドは除外
        assertFalse(filtered.any { it.tickerCode == "9433" }) // KDDIは除外
    }

    @Test
    fun testFilterRequiresLongTerm() {
        val filtered = sampleStocks.filter { stock ->
            when (LongTermFilterType.REQUIRES_LONG_TERM) {
                LongTermFilterType.ALL -> true
                LongTermFilterType.SHORT_TERM_OK -> !stock.requiresLongTerm
                LongTermFilterType.REQUIRES_LONG_TERM -> stock.requiresLongTerm
            }
        }
        // マクドナルド (2702) と KDDI (9433) のみ
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.tickerCode == "2702" })
        assertTrue(filtered.any { it.tickerCode == "9433" })
        assertFalse(filtered.any { it.tickerCode == "9202" })
        assertFalse(filtered.any { it.tickerCode == "8267" })
    }

    @Test
    fun testPresetStocksLongTermCoverage() {
        val allPresets = StockPresetData.PRESET_STOCKS
        assertTrue("プリセット銘柄が存在すること", allPresets.isNotEmpty())

        val mac = allPresets.find { it.tickerCode == "2702" }
        assertTrue("マクドナルドは長期条件ありであること", mac?.requiresLongTerm == true)

        val ana = allPresets.find { it.tickerCode == "9202" }
        assertFalse("ANAは短期OKであること", ana?.requiresLongTerm == true)

        val kddi = allPresets.find { it.tickerCode == "9433" }
        assertTrue("KDDIは長期条件ありであること", kddi?.requiresLongTerm == true)

        val ion = allPresets.find { it.tickerCode == "8267" }
        assertFalse("イオンは短期OKであること", ion?.requiresLongTerm == true)
    }
}
