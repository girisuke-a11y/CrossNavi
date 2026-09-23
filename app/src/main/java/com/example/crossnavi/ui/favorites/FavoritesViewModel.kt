package com.example.crossnavi.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crossnavi.data.FavoritesRepository
import com.example.crossnavi.data.InventoryFilterType
import com.example.crossnavi.data.LongTermFilterType
import com.example.crossnavi.data.StockSortOrder
import com.example.crossnavi.data.StockMasterRepository
import com.example.crossnavi.model.InventoryStatus
import com.example.crossnavi.model.StockPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FavoritesUiState(
    val favoriteStocks: List<StockPreset> = emptyList(),
    val filteredStocks: List<StockPreset> = emptyList(),
    val inventoryFilter: InventoryFilterType = InventoryFilterType.ALL,
    val longTermFilter: LongTermFilterType = LongTermFilterType.ALL,
    val sortOrder: StockSortOrder = StockSortOrder.CODE_ASC,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val lastUpdatedTime: String = "19:00",
    val lastSyncSource: String = "",
    val lastSyncStatus: String = "success",
    val isSyncSettingsOpen: Boolean = false,
    val currentSyncUrl: String = ""
)

class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val stockMasterRepository: StockMasterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            lastUpdatedTime = stockMasterRepository.lastUpdatedTime,
            lastSyncSource = stockMasterRepository.lastSyncSource,
            lastSyncStatus = stockMasterRepository.lastSyncStatus,
            currentSyncUrl = stockMasterRepository.getSyncUrl()
        )
        // お気に入りコードの変更を監視して自動更新
        viewModelScope.launch {
            favoritesRepository.favoriteTickers.collect { favoriteCodes ->
                updateFavoritesList(favoriteCodes, _uiState.value.inventoryFilter)
            }
        }
        
        // アプリ起動時にバックグラウンドで最新在庫を自動取得
        onSyncInventory()
    }

    fun onInventoryFilterSelected(filter: InventoryFilterType) {
        _uiState.value = _uiState.value.copy(inventoryFilter = filter)
        applyFilter()
    }

    fun onLongTermFilterSelected(filter: LongTermFilterType) {
        _uiState.value = _uiState.value.copy(longTermFilter = filter)
        applyFilter()
    }

    fun onSortOrderSelected(order: StockSortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order)
        applyFilter()
    }

    fun onRemoveFavorite(tickerCode: String) {
        favoritesRepository.removeFavorite(tickerCode)
    }

    fun onSyncInventory(customUrl: String? = null, force: Boolean = true) {
        _uiState.value = _uiState.value.copy(isSyncing = true)
        viewModelScope.launch(Dispatchers.IO) {
            stockMasterRepository.syncOnlineInventory(customUrl, force)
            val favorites = favoritesRepository.favoriteTickers.value
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastUpdatedTime = stockMasterRepository.lastUpdatedTime,
                    lastSyncSource = stockMasterRepository.lastSyncSource,
                    lastSyncStatus = stockMasterRepository.lastSyncStatus
                )
                updateFavoritesList(favorites, _uiState.value.inventoryFilter)
            }
        }
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

    private fun updateFavoritesList(favoriteCodes: Set<String>, filter: InventoryFilterType) {
        viewModelScope.launch(Dispatchers.IO) {
            val allStocks = stockMasterRepository.getAllStocks()
            val stockMap = allStocks.associateBy { it.tickerCode }
            val favorites = favoriteCodes.mapNotNull { stockMap[it] }

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    favoriteStocks = favorites,
                    isLoading = false
                )
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val current = _uiState.value
        val afterInventory = when (current.inventoryFilter) {
            InventoryFilterType.ALL -> current.favoriteStocks
            InventoryFilterType.ANY_IN_STOCK -> {
                current.favoriteStocks.filter {
                    it.sbiInventory.status != InventoryStatus.OUT_OF_STOCK ||
                            it.rakutenInventory.status != InventoryStatus.OUT_OF_STOCK
                }
            }
            InventoryFilterType.SBI_IN_STOCK -> {
                current.favoriteStocks.filter {
                    it.sbiInventory.status != InventoryStatus.OUT_OF_STOCK
                }
            }
            InventoryFilterType.RAKUTEN_IN_STOCK -> {
                current.favoriteStocks.filter {
                    it.rakutenInventory.status != InventoryStatus.OUT_OF_STOCK
                }
            }
        }
        val filtered = when (current.longTermFilter) {
            LongTermFilterType.ALL -> afterInventory
            LongTermFilterType.SHORT_TERM_OK -> afterInventory.filter { !it.requiresLongTerm }
            LongTermFilterType.REQUIRES_LONG_TERM -> afterInventory.filter { it.requiresLongTerm }
        }
        val sorted = when (current.sortOrder) {
            StockSortOrder.CODE_ASC -> filtered.sortedBy { it.tickerCode }
            StockSortOrder.YIELD_DESC -> filtered.sortedWith(
                compareByDescending<StockPreset> { it.estimatedYield }
                    .thenBy { it.tickerCode }
            )
            StockSortOrder.GIFT_VALUE_DESC -> filtered.sortedWith(
                compareByDescending<StockPreset> { it.giftValue }
                    .thenBy { it.tickerCode }
            )
            StockSortOrder.INVENTORY_DESC -> filtered.sortedWith(
                compareByDescending<StockPreset> { it.totalInventoryShares }
                    .thenBy { it.tickerCode }
            )
        }
        _uiState.value = _uiState.value.copy(filteredStocks = sorted)
    }
}
