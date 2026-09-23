package com.example.crossnavi.data

import com.example.crossnavi.model.StockPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentFilterTest {

    private val sampleStocks = listOf(
        // 500円 * 100株 = 50,000円 (10万円以下)
        StockPreset(
            tickerCode = "1001",
            stockName = "少額銘柄A",
            months = listOf(3),
            defaultPrice = 500.0,
            defaultQuantity = 100,
            giftValue = 1000,
            giftDescription = "QUOカード"
        ),
        // 1,000円 * 100株 = 100,000円 (10万円以下 境界値)
        StockPreset(
            tickerCode = "1002",
            stockName = "境界銘柄10万",
            months = listOf(3),
            defaultPrice = 1000.0,
            defaultQuantity = 100,
            giftValue = 1000,
            giftDescription = "ギフト券"
        ),
        // 2,500円 * 100株 = 250,000円 (30万円以下)
        StockPreset(
            tickerCode = "1003",
            stockName = "中額銘柄B",
            months = listOf(3),
            defaultPrice = 2500.0,
            defaultQuantity = 100,
            giftValue = 2000,
            giftDescription = "優待券"
        ),
        // 4,500円 * 100株 = 450,000円 (50万円以下)
        StockPreset(
            tickerCode = "1004",
            stockName = "中高額銘柄C",
            months = listOf(3),
            defaultPrice = 4500.0,
            defaultQuantity = 100,
            giftValue = 3000,
            giftDescription = "自社商品"
        ),
        // 8,000円 * 100株 = 800,000円 (100万円以下)
        StockPreset(
            tickerCode = "1005",
            stockName = "高額銘柄D",
            months = listOf(3),
            defaultPrice = 8000.0,
            defaultQuantity = 100,
            giftValue = 5000,
            giftDescription = "カタログギフト"
        ),
        // 15,000円 * 100株 = 1,500,000円 (それ以上)
        StockPreset(
            tickerCode = "1006",
            stockName = "超高額銘柄E",
            months = listOf(3),
            defaultPrice = 15000.0,
            defaultQuantity = 100,
            giftValue = 10000,
            giftDescription = "特別優待品"
        ),
        // 2,000円 * 500株 = 1,000,000円 (100万円以下 境界値)
        StockPreset(
            tickerCode = "1007",
            stockName = "株数多め100万",
            months = listOf(3),
            defaultPrice = 2000.0,
            defaultQuantity = 500,
            giftValue = 8000,
            giftDescription = "自社製品"
        )
    )

    private fun filter(stocks: List<StockPreset>, filters: Set<InvestmentFilterType>): List<StockPreset> {
        return stocks.filter { s ->
            val required = s.defaultPrice * s.defaultQuantity
            if (filters.isEmpty()) true else filters.any { it.matches(required) }
        }
    }

    @Test
    fun testUnder100k() {
        val result = filter(sampleStocks, setOf(InvestmentFilterType.UNDER_100K))
        assertEquals(2, result.size)
        assertTrue(result.all { it.defaultPrice * it.defaultQuantity <= 100_000.0 })
        assertEquals(listOf("1001", "1002"), result.map { it.tickerCode })
    }

    @Test
    fun testRange100kTo300k() {
        // 10万〜30万 (1003: 250,000円 のみ。1001, 1002 は重複しない)
        val result = filter(sampleStocks, setOf(InvestmentFilterType.RANGE_100K_300K))
        assertEquals(1, result.size)
        assertEquals("1003", result.first().tickerCode)
        val required = result.first().defaultPrice * result.first().defaultQuantity
        assertTrue(required > 100_000.0 && required <= 300_000.0)
    }

    @Test
    fun testRange300kTo500k() {
        // 30万〜50万 (1004: 450,000円 のみ)
        val result = filter(sampleStocks, setOf(InvestmentFilterType.RANGE_300K_500K))
        assertEquals(1, result.size)
        assertEquals("1004", result.first().tickerCode)
        val required = result.first().defaultPrice * result.first().defaultQuantity
        assertTrue(required > 300_000.0 && required <= 500_000.0)
    }

    @Test
    fun testRange500kTo1m() {
        // 50万〜100万 (1005: 800,000円, 1007: 1,000,000円)
        val result = filter(sampleStocks, setOf(InvestmentFilterType.RANGE_500K_1M))
        assertEquals(2, result.size)
        assertEquals(listOf("1005", "1007"), result.map { it.tickerCode })
    }

    @Test
    fun testOver1m() {
        // 100万円超 (1006: 1,500,000円)
        val result = filter(sampleStocks, setOf(InvestmentFilterType.OVER_1M))
        assertEquals(1, result.size)
        assertEquals("1006", result.first().tickerCode)
    }

    @Test
    fun testNonOverlappingMutualExclusivity() {
        // 全5レンジの各結果を取り出し、どの2つのレンジ間でも重複がゼロであることを検証
        val r1 = filter(sampleStocks, setOf(InvestmentFilterType.UNDER_100K)).map { it.tickerCode }.toSet()
        val r2 = filter(sampleStocks, setOf(InvestmentFilterType.RANGE_100K_300K)).map { it.tickerCode }.toSet()
        val r3 = filter(sampleStocks, setOf(InvestmentFilterType.RANGE_300K_500K)).map { it.tickerCode }.toSet()
        val r4 = filter(sampleStocks, setOf(InvestmentFilterType.RANGE_500K_1M)).map { it.tickerCode }.toSet()
        val r5 = filter(sampleStocks, setOf(InvestmentFilterType.OVER_1M)).map { it.tickerCode }.toSet()

        assertTrue(r1.intersect(r2).isEmpty())
        assertTrue(r2.intersect(r3).isEmpty())
        assertTrue(r3.intersect(r4).isEmpty())
        assertTrue(r4.intersect(r5).isEmpty())
        assertTrue(r1.intersect(r5).isEmpty())

        // 全レンジの合計件数が全銘柄数と完全一致
        val totalUnion = r1 + r2 + r3 + r4 + r5
        assertEquals(sampleStocks.size, totalUnion.size)
    }

    @Test
    fun testMultiSelection() {
        // 〜10万 + 10〜30万 の複数選択 -> 0〜30万円 (1001, 1002, 1003)
        val result = filter(sampleStocks, setOf(InvestmentFilterType.UNDER_100K, InvestmentFilterType.RANGE_100K_300K))
        assertEquals(3, result.size)
        assertEquals(listOf("1001", "1002", "1003"), result.map { it.tickerCode })

        // 離れたレンジの複数選択: 〜10万 + 100万円超 -> (1001, 1002, 1006)
        val resultDisjoint = filter(sampleStocks, setOf(InvestmentFilterType.UNDER_100K, InvestmentFilterType.OVER_1M))
        assertEquals(3, resultDisjoint.size)
        assertEquals(listOf("1001", "1002", "1006"), resultDisjoint.map { it.tickerCode })
    }

    @Test
    fun testEmptySetMatchesAll() {
        val result = filter(sampleStocks, emptySet())
        assertEquals(sampleStocks.size, result.size)
    }

    @Test
    fun testFormattedInvestment() {
        val stock1 = sampleStocks[0] // 50,000
        assertEquals("5.0万円", stock1.formattedInvestment)

        val stock3 = sampleStocks[2] // 250,000
        assertEquals("25.0万円", stock3.formattedInvestment)

        val stock6 = sampleStocks[5] // 1,500,000
        assertEquals("150.0万円", stock6.formattedInvestment)
    }
}
