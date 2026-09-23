package com.example.crossnavi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CostCalculatorTest {

    @Test
    fun testSettlementDate() {
        // 月曜約定 -> 水曜受渡 (2営業日後)
        val monday = LocalDate.of(2026, 9, 21) // 月
        val wednesday = CostCalculator.getSettlementDate(monday)
        assertEquals(LocalDate.of(2026, 9, 23), wednesday)

        // 木曜約定 -> 翌週月曜受渡 (金・土・日 -> 月)
        val thursday = LocalDate.of(2026, 9, 24)
        val nextMonday = CostCalculator.getSettlementDate(thursday)
        assertEquals(LocalDate.of(2026, 9, 28), nextMonday)

        // 金曜約定 -> 翌週火曜受渡
        val friday = LocalDate.of(2026, 9, 25)
        val nextTuesday = CostCalculator.getSettlementDate(friday)
        assertEquals(LocalDate.of(2026, 9, 29), nextTuesday)
    }

    @Test
    fun testHoldingDays() {
        // 金曜取得、翌週月曜現渡 -> 受渡日は火曜と水曜 -> 1日
        val friday = LocalDate.of(2026, 9, 25)
        val monday = LocalDate.of(2026, 9, 28)
        val days = CostCalculator.calculateHoldingDays(friday, monday)
        assertEquals(1, days)
    }

    @Test
    fun testCalculationResult() {
        // 株価 3,000円, 100株 (30万円), 年利 3.9%, 10日間保有, 優待 3,000円
        val openDate = LocalDate.of(2026, 9, 10)
        val deliveryDate = LocalDate.of(2026, 9, 20)
        val result = CostCalculator.calculate(
            stockPrice = 3000.0,
            quantity = 100,
            annualRate = 3.9,
            openDate = openDate,
            deliveryDate = deliveryDate,
            giftValue = 3000,
            dividendPerShare = 0.0,
            feeBuy = 0,
            feeSell = 0
        )

        // 300,000 * 0.039 / 365 = 約32.05円/日
        // 貸株料 = 32.05 * 日数
        assertTrue(result.borrowFee > 0)
        assertTrue(result.netProfit > 0)
        assertTrue(result.breakEvenDays > 0)
    }
}
