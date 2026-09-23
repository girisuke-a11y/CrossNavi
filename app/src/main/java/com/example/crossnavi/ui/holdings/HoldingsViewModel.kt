package com.example.crossnavi.ui.holdings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crossnavi.data.HoldingsRepository
import com.example.crossnavi.domain.CostCalculator
import com.example.crossnavi.model.CrossHolding
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HoldingsSummary(
    val totalHoldingsCount: Int = 0,
    val pendingDeliveryCount: Int = 0,
    val totalTradeAmount: Double = 0.0,
    val totalGiftValue: Int = 0,
    val totalCost: Int = 0,
    val totalNetProfit: Int = 0
)

data class HoldingItemUi(
    val holding: CrossHolding,
    val totalCost: Int,
    val netProfit: Int,
    val holdingDays: Int
)

class HoldingsViewModel(
    private val repository: HoldingsRepository
) : ViewModel() {

    val holdingItems: StateFlow<List<HoldingItemUi>> = repository.holdings
        .map { list ->
            list.map { h ->
                val res = CostCalculator.calculate(
                    stockPrice = h.stockPrice,
                    quantity = h.quantity,
                    annualRate = h.annualRate,
                    openDate = h.openDate,
                    deliveryDate = h.deliveryDate,
                    giftValue = h.giftValue,
                    dividendPerShare = h.dividendPerShare,
                    feeBuy = h.feeBuy,
                    feeSell = h.feeSell
                )
                HoldingItemUi(
                    holding = h,
                    totalCost = res.totalCost,
                    netProfit = res.netProfit,
                    holdingDays = res.holdingDays
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<HoldingsSummary> = holdingItems
        .map { items ->
            var totalTrade = 0.0
            var totalGift = 0
            var totalCost = 0
            var totalProfit = 0
            var pendingCount = 0

            for (item in items) {
                totalTrade += item.holding.stockPrice * item.holding.quantity
                totalGift += item.holding.giftValue
                totalCost += item.totalCost
                totalProfit += item.netProfit
                if (!item.holding.isDelivered) {
                    pendingCount++
                }
            }

            HoldingsSummary(
                totalHoldingsCount = items.size,
                pendingDeliveryCount = pendingCount,
                totalTradeAmount = totalTrade,
                totalGiftValue = totalGift,
                totalCost = totalCost,
                totalNetProfit = totalProfit
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HoldingsSummary())

    fun toggleDelivery(id: String) {
        viewModelScope.launch {
            repository.toggleDeliveryStatus(id)
        }
    }

    fun deleteHolding(id: String) {
        viewModelScope.launch {
            repository.deleteHolding(id)
        }
    }
}
