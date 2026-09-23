package com.example.crossnavi.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StockPresetDataTest {

    @Test
    fun testGetStocksByMonth() {
        // 9月の銘柄 (ANA, JAL, コロワイド, ゼンショーなど)
        val septStocks = StockPresetData.getStocksByMonth(9)
        assertTrue(septStocks.isNotEmpty())
        assertTrue(septStocks.any { it.tickerCode == "9202" }) // ANA
        assertTrue(septStocks.any { it.stockName.contains("ゼンショー") })

        // 3月の銘柄
        val marchStocks = StockPresetData.getStocksByMonth(3)
        assertTrue(marchStocks.isNotEmpty())
        assertTrue(marchStocks.any { it.tickerCode == "9202" }) // 3月と9月両方
    }

    @Test
    fun testSearchStocks() {
        // 全体から "マクドナルド" 検索
        val macResults = StockPresetData.searchStocks(null, "マクドナルド")
        assertEquals(1, macResults.size)
        assertEquals("2702", macResults.first().tickerCode)

        // コード "9202" 検索
        val anaResults = StockPresetData.searchStocks(null, "9202")
        assertEquals(1, anaResults.size)
        assertEquals("ANAホールディングス", anaResults.first().stockName)

        // 優待内容で検索 ("食事券")
        val mealResults = StockPresetData.searchStocks(null, "食事券")
        assertTrue(mealResults.size >= 3)
    }
}
