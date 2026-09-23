package com.example.crossnavi.domain

import com.example.crossnavi.model.Broker
import com.example.crossnavi.model.CreditType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.roundToInt

data class CalculationResult(
    val totalTradeAmount: Double,    // 約定代金 (株価 × 株数)
    val holdingDays: Int,            // 借株日数 (受渡日ベース)
    val borrowFee: Int,              // 貸株料 (円)
    val dailyBorrowFee: Double,      // 1日あたりの貸株料 (円)
    val dividendGapCost: Int,        // 配当落調整金差額コスト (円)
    val totalFees: Int,              // 売買手数料 (円)
    val totalCost: Int,              // 総コスト (貸株料 + 手数料 + 配当差額)
    val netProfit: Int,              // 純利益 (優待価値 - 総コスト)
    val netYield: Double,            // 実質利回り (%)
    val breakEvenDays: Int           // 損益分岐日数 (何日以内に現渡すれば黒字か)
)

object CostCalculator {

    /**
     * 株主優待クロスのコストおよび損益を計算する
     */
    fun calculate(
        stockPrice: Double,
        quantity: Int,
        annualRate: Double,
        openDate: LocalDate,
        deliveryDate: LocalDate,
        giftValue: Int,
        dividendPerShare: Double = 0.0,
        feeBuy: Int = 0,
        feeSell: Int = 0
    ): CalculationResult {
        val totalTradeAmount = stockPrice * quantity
        val days = calculateHoldingDays(openDate, deliveryDate)

        // 1日あたりの貸株料: 約定代金 × (年利 / 100) / 365
        val dailyFee = (totalTradeAmount * (annualRate / 100.0)) / 365.0

        // 貸株料 (円未満切り捨て)
        val borrowFee = floor(dailyFee * days).toInt()

        // 配当落調整金差額 (約15.315%の税金差額コスト)
        val totalDividend = dividendPerShare * quantity
        val dividendGapCost = (totalDividend * 0.15315).roundToInt()

        // 手数料合計
        val totalFees = feeBuy + feeSell

        // 総コスト
        val totalCost = borrowFee + totalFees + dividendGapCost

        // 純利益
        val netProfit = giftValue - totalCost

        // 実質利回り (%)
        val netYield = if (totalTradeAmount > 0) {
            (netProfit.toDouble() / totalTradeAmount) * 100.0
        } else {
            0.0
        }

        // 損益分岐日数 (優待価値 - 固定コスト) / 1日あたり貸株料
        val remainingValue = giftValue - (totalFees + dividendGapCost)
        val breakEvenDays = if (dailyFee > 0 && remainingValue > 0) {
            floor(remainingValue / dailyFee).toInt()
        } else {
            0
        }

        return CalculationResult(
            totalTradeAmount = totalTradeAmount,
            holdingDays = days,
            borrowFee = borrowFee,
            dailyBorrowFee = dailyFee,
            dividendGapCost = dividendGapCost,
            totalFees = totalFees,
            totalCost = totalCost,
            netProfit = netProfit,
            netYield = netYield,
            breakEvenDays = breakEvenDays
        )
    }

    /**
     * 約定日(取得日)と現渡日から、受渡日ベースの借株日数を算出
     * 日本株は T+2 営業日受渡
     * 借株日数は (返済受渡日 - 新規建受渡日) の暦日数
     */
    fun calculateHoldingDays(openDate: LocalDate, deliveryDate: LocalDate): Int {
        if (deliveryDate.isBefore(openDate)) {
            return 1
        }
        val openSettlement = getSettlementDate(openDate)
        val deliverySettlement = getSettlementDate(deliveryDate)

        val days = ChronoUnit.DAYS.between(openSettlement, deliverySettlement).toInt()
        // 同日現渡(日計り)などの場合は最低1日
        return if (days <= 0) 1 else days
    }

    /**
     * 約定日から2営業日後の受渡日を計算 (土日をスキップ)
     */
    fun getSettlementDate(tradeDate: LocalDate): LocalDate {
        var count = 0
        var current = tradeDate
        while (count < 2) {
            current = current.plusDays(1)
            if (current.dayOfWeek != DayOfWeek.SATURDAY && current.dayOfWeek != DayOfWeek.SUNDAY) {
                count++
            }
        }
        return current
    }
}
