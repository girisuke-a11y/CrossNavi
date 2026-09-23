package com.example.crossnavi.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crossnavi.data.BrokerFilterType
import com.example.crossnavi.data.HoldingsRepository
import com.example.crossnavi.data.InventoryFilterType
import com.example.crossnavi.data.InvestmentFilterType
import com.example.crossnavi.data.LongTermFilterType
import com.example.crossnavi.data.StockSortOrder
import com.example.crossnavi.data.StockMasterRepository
import com.example.crossnavi.domain.CalculationResult
import com.example.crossnavi.domain.CostCalculator
import com.example.crossnavi.model.Broker
import com.example.crossnavi.model.CreditType
import com.example.crossnavi.model.CrossHolding
import com.example.crossnavi.model.StockPreset
import com.example.crossnavi.data.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class CalculatorUiState(
    val tickerCode: String = "",
    val stockName: String = "",
    val stockPrice: String = "2000",
    val quantity: String = "100",
    val selectedBroker: Broker = Broker.SBI,
    val selectedCreditType: CreditType = CreditType.SBI_SHORT,
    val openDate: LocalDate = LocalDate.now(),
    val deliveryDate: LocalDate = LocalDate.now().plusDays(7),
    val giftValue: String = "3000",
    val giftDescription: String = "",
    val dividendPerShare: String = "0",
    val feeBuy: String = "0",
    val feeSell: String = "0",
    val selectedMonth: Int = LocalDate.now().monthValue,
    val isStockPickerVisible: Boolean = false,
    val stockSearchQuery: String = "",
    val brokerFilter: BrokerFilterType = BrokerFilterType.ALL,
    val inventoryFilter: InventoryFilterType = InventoryFilterType.ALL,
    val longTermFilter: LongTermFilterType = LongTermFilterType.ALL,
    val investmentFilters: Set<InvestmentFilterType> = emptySet(),
    val sortOrder: StockSortOrder = StockSortOrder.CODE_ASC,
    val isFavoriteOnlyFilter: Boolean = false,
    val favoriteTickers: Set<String> = emptySet(),
    val searchResults: List<StockPreset> = emptyList(),
    val selectedStockPreset: StockPreset? = null,
    val totalMasterCount: Int = 0,
    val calculationResult: CalculationResult? = null,
    val isSavedSuccess: Boolean = false,
    val isCalculationSheetOpen: Boolean = false,
    val isSyncingInventory: Boolean = false,
    val syncMessage: String? = null,
    val lastUpdatedTime: String = "19:00",
    val lastSyncSource: String = "",
    val lastSyncStatus: String = "success",
    val isSyncSettingsOpen: Boolean = false,
    val currentSyncUrl: String = ""
) {
    val currentAnnualRate: Double
        get() = selectedCreditType.defaultAnnualRate
}

class CalculatorViewModel(
    private val repository: HoldingsRepository,
    private val stockMasterRepository: StockMasterRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            lastUpdatedTime = stockMasterRepository.lastUpdatedTime,
            lastSyncSource = stockMasterRepository.lastSyncSource,
            lastSyncStatus = stockMasterRepository.lastSyncStatus,
            currentSyncUrl = stockMasterRepository.getSyncUrl()
        )
        recalculate()
        loadInitialStocks()
        observeFavorites()
        
        // アプリ起動時にバックグラウンドで最新在庫を自動取得
        onSyncInventory()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.favoriteTickers.collect { favorites ->
                _uiState.value = _uiState.value.copy(favoriteTickers = favorites)
                if (_uiState.value.isFavoriteOnlyFilter) {
                    refreshSearchResults()
                }
            }
        }
    }

    fun toggleFavorite(tickerCode: String) {
        favoritesRepository.toggleFavorite(tickerCode)
    }

    fun onToggleFavoriteFilter() {
        val next = !_uiState.value.isFavoriteOnlyFilter
        _uiState.value = _uiState.value.copy(isFavoriteOnlyFilter = next)
        refreshSearchResults()
    }

    private fun loadInitialStocks() {
        viewModelScope.launch(Dispatchers.IO) {
            val total = stockMasterRepository.getAllStocks().size
            val s = _uiState.value
            var results = stockMasterRepository.search(
                month = s.selectedMonth,
                brokerFilter = s.brokerFilter,
                inventoryFilter = s.inventoryFilter,
                longTermFilter = s.longTermFilter,
                investmentFilters = s.investmentFilters,
                query = s.stockSearchQuery,
                sortOrder = s.sortOrder
            )
            if (s.isFavoriteOnlyFilter) {
                results = results.filter { s.favoriteTickers.contains(it.tickerCode) }
            }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    totalMasterCount = total,
                    searchResults = results
                )
            }
        }
    }

    private fun refreshSearchResults() {
        val s = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            var results = stockMasterRepository.search(
                month = if (s.stockSearchQuery.isNotBlank()) null else s.selectedMonth,
                brokerFilter = s.brokerFilter,
                inventoryFilter = s.inventoryFilter,
                longTermFilter = s.longTermFilter,
                investmentFilters = s.investmentFilters,
                query = s.stockSearchQuery,
                sortOrder = s.sortOrder
            )
            if (s.isFavoriteOnlyFilter) {
                results = results.filter { s.favoriteTickers.contains(it.tickerCode) }
            }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(searchResults = results)
            }
        }
    }

    fun onSyncInventory(customUrl: String? = null, force: Boolean = true) {
        _uiState.value = _uiState.value.copy(isSyncingInventory = true, syncMessage = null)
        viewModelScope.launch(Dispatchers.IO) {
            val result = stockMasterRepository.syncOnlineInventory(customUrl, force)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    val time = stockMasterRepository.lastUpdatedTime
                    val source = stockMasterRepository.lastSyncSource
                    val status = stockMasterRepository.lastSyncStatus
                    val isFallback = stockMasterRepository.lastSyncWasFallback
                    
                    val msg = if (status == "error") {
                        "❌ データ取得失敗: $time (全サイトでエラー)"
                    } else if (isFallback) {
                        "✅ 最新在庫を更新しました (${count}銘柄, $time ※内蔵データ)"
                    } else {
                        "✅ 最新在庫を同期しました (${count}銘柄, $time / 取得元: $source)"
                    }
                    _uiState.value = _uiState.value.copy(
                        isSyncingInventory = false,
                        syncMessage = msg,
                        lastUpdatedTime = time,
                        lastSyncSource = source,
                        lastSyncStatus = status
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSyncingInventory = false,
                        syncMessage = "同期エラー: ${result.exceptionOrNull()?.message ?: "接続失敗"}"
                    )
                }
                refreshSearchResults()
            }
        }
    }

    fun clearSyncMessage() {
        _uiState.value = _uiState.value.copy(syncMessage = null)
    }

    fun onOpenSyncSettings() {
        _uiState.value = _uiState.value.copy(
            isSyncSettingsOpen = true,
            currentSyncUrl = stockMasterRepository.getSyncUrl()
        )
    }

    fun onCloseSyncSettings() {
        _uiState.value = _uiState.value.copy(isSyncSettingsOpen = false)
    }

    fun onSaveSyncUrl(url: String, andSyncNow: Boolean = true) {
        stockMasterRepository.setSyncUrl(url)
        _uiState.value = _uiState.value.copy(
            isSyncSettingsOpen = false,
            currentSyncUrl = stockMasterRepository.getSyncUrl()
        )
        if (andSyncNow) {
            onSyncInventory(url)
        }
    }

    fun onResetSyncUrl() {
        stockMasterRepository.resetSyncUrl()
        _uiState.value = _uiState.value.copy(
            currentSyncUrl = stockMasterRepository.getSyncUrl()
        )
    }

    fun onTickerChanged(value: String) {
        val currentPreset = _uiState.value.selectedStockPreset
        val matchedPreset = if (currentPreset?.tickerCode == value) currentPreset else null
        _uiState.value = _uiState.value.copy(
            tickerCode = value,
            selectedStockPreset = matchedPreset,
            isSavedSuccess = false
        )
    }

    fun onStockNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(stockName = value, isSavedSuccess = false)
    }

    fun onStockPriceChanged(value: String) {
        _uiState.value = _uiState.value.copy(stockPrice = value, isSavedSuccess = false)
        recalculate()
    }

    fun onQuantityChanged(value: String) {
        _uiState.value = _uiState.value.copy(quantity = value, isSavedSuccess = false)
        recalculate()
    }

    fun onBrokerChanged(broker: Broker) {
        val defaultType = broker.creditTypes.first()
        _uiState.value = _uiState.value.copy(
            selectedBroker = broker,
            selectedCreditType = defaultType,
            isSavedSuccess = false
        )
        recalculate()
    }

    fun onCreditTypeChanged(creditType: CreditType) {
        _uiState.value = _uiState.value.copy(
            selectedCreditType = creditType,
            isSavedSuccess = false
        )
        recalculate()
    }

    fun onOpenDateChanged(date: LocalDate) {
        val currentDelivery = _uiState.value.deliveryDate
        val newDelivery = if (currentDelivery.isBefore(date)) date else currentDelivery
        _uiState.value = _uiState.value.copy(openDate = date, deliveryDate = newDelivery, isSavedSuccess = false)
        recalculate()
    }

    fun onDeliveryDateChanged(date: LocalDate) {
        _uiState.value = _uiState.value.copy(deliveryDate = date, isSavedSuccess = false)
        recalculate()
    }

    fun onGiftValueChanged(value: String) {
        _uiState.value = _uiState.value.copy(giftValue = value, isSavedSuccess = false)
        recalculate()
    }

    fun onDividendChanged(value: String) {
        _uiState.value = _uiState.value.copy(dividendPerShare = value, isSavedSuccess = false)
        recalculate()
    }

    // --- 銘柄選択・検索・フィルター操作 ---
    fun onMonthSelected(month: Int) {
        _uiState.value = _uiState.value.copy(selectedMonth = month)
        refreshSearchResults()
    }

    fun onBrokerFilterChanged(filter: BrokerFilterType) {
        _uiState.value = _uiState.value.copy(brokerFilter = filter)
        refreshSearchResults()
    }

    fun onInventoryFilterChanged(filter: InventoryFilterType) {
        _uiState.value = _uiState.value.copy(inventoryFilter = filter)
        refreshSearchResults()
    }

    fun onLongTermFilterChanged(filter: LongTermFilterType) {
        _uiState.value = _uiState.value.copy(longTermFilter = filter)
        refreshSearchResults()
    }

    fun onToggleInvestmentFilter(filter: InvestmentFilterType) {
        val current = _uiState.value.investmentFilters
        val updated = if (current.contains(filter)) {
            current - filter
        } else {
            current + filter
        }
        _uiState.value = _uiState.value.copy(investmentFilters = updated)
        refreshSearchResults()
    }

    fun onClearInvestmentFilters() {
        _uiState.value = _uiState.value.copy(investmentFilters = emptySet())
        refreshSearchResults()
    }

    fun onSortOrderChanged(sortOrder: StockSortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = sortOrder)
        refreshSearchResults()
    }

    fun onOpenStockPicker() {
        _uiState.value = _uiState.value.copy(isStockPickerVisible = true, stockSearchQuery = "")
        refreshSearchResults()
    }

    fun onCloseStockPicker() {
        _uiState.value = _uiState.value.copy(isStockPickerVisible = false)
    }

    fun onStockSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(stockSearchQuery = query)
        refreshSearchResults()
    }

    fun onSelectStockPreset(stock: StockPreset) {
        _uiState.value = _uiState.value.copy(
            tickerCode = stock.tickerCode,
            stockName = stock.stockName,
            stockPrice = stock.defaultPrice.toInt().toString(),
            quantity = stock.defaultQuantity.toString(),
            giftValue = stock.giftValue.toString(),
            giftDescription = stock.giftDescription,
            dividendPerShare = if (stock.dividendPerShare > 0) stock.dividendPerShare.toString() else "0",
            selectedStockPreset = stock,
            isStockPickerVisible = false,
            isCalculationSheetOpen = true,
            isSavedSuccess = false
        )
        recalculate()
    }

    fun onOpenCalculationForStock(stock: StockPreset) {
        onSelectStockPreset(stock)
    }

    fun onCloseCalculationSheet() {
        _uiState.value = _uiState.value.copy(isCalculationSheetOpen = false)
    }

    private fun recalculate() {
        val state = _uiState.value
        val price = state.stockPrice.toDoubleOrNull() ?: 0.0
        val qty = state.quantity.toIntOrNull() ?: 0
        val rate = state.currentAnnualRate
        val gift = state.giftValue.toIntOrNull() ?: 0
        val dividend = state.dividendPerShare.toDoubleOrNull() ?: 0.0
        val feeB = state.feeBuy.toIntOrNull() ?: 0
        val feeS = state.feeSell.toIntOrNull() ?: 0

        val result = CostCalculator.calculate(
            stockPrice = price,
            quantity = qty,
            annualRate = rate,
            openDate = state.openDate,
            deliveryDate = state.deliveryDate,
            giftValue = gift,
            dividendPerShare = dividend,
            feeBuy = feeB,
            feeSell = feeS
        )
        _uiState.value = _uiState.value.copy(calculationResult = result)
    }

    fun saveToHoldings() {
        val state = _uiState.value
        val result = state.calculationResult ?: return
        val price = state.stockPrice.toDoubleOrNull() ?: 0.0
        val qty = state.quantity.toIntOrNull() ?: 0
        val rate = state.currentAnnualRate
        val gift = state.giftValue.toIntOrNull() ?: 0
        val dividend = state.dividendPerShare.toDoubleOrNull() ?: 0.0
        val feeB = state.feeBuy.toIntOrNull() ?: 0
        val feeS = state.feeSell.toIntOrNull() ?: 0

        val holding = CrossHolding(
            tickerCode = state.tickerCode.ifBlank { "0000" },
            stockName = state.stockName.ifBlank { "銘柄名未入力" },
            stockPrice = price,
            quantity = qty,
            broker = state.selectedBroker,
            creditType = state.selectedCreditType,
            annualRate = rate,
            openDate = state.openDate,
            deliveryDate = state.deliveryDate,
            holdingDays = result.holdingDays,
            giftValue = gift,
            dividendPerShare = dividend,
            feeBuy = feeB,
            feeSell = feeS,
            isDelivered = false,
            memo = state.giftDescription
        )
        viewModelScope.launch {
            repository.addHolding(holding)
            _uiState.value = _uiState.value.copy(isSavedSuccess = true)
        }
    }
}
