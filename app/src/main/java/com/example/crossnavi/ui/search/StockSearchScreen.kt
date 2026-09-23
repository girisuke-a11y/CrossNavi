package com.example.crossnavi.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crossnavi.data.BrokerFilterType
import com.example.crossnavi.data.InventoryFilterType
import com.example.crossnavi.data.InvestmentFilterType
import com.example.crossnavi.data.LongTermFilterType
import com.example.crossnavi.data.StockSortOrder
import com.example.crossnavi.model.InventoryStatus
import com.example.crossnavi.model.StockPreset
import com.example.crossnavi.ui.calculator.CalculatorViewModel
import com.example.crossnavi.ui.components.SyncSettingsDialog
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockSearchScreen(
    viewModel: CalculatorViewModel,
    onStockSelected: (StockPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.JAPAN) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val monthScrollState = rememberScrollState()

    var isFilterExpanded by rememberSaveable { mutableStateOf(false) }

    // 適用されている詳細フィルター数のカウント
    val activeFilterCount = (if (uiState.inventoryFilter != InventoryFilterType.ALL) 1 else 0) +
            (if (uiState.longTermFilter != LongTermFilterType.ALL) 1 else 0) +
            (if (uiState.isFavoriteOnlyFilter) 1 else 0) +
            uiState.investmentFilters.size +
            (if (uiState.sortOrder != StockSortOrder.CODE_ASC) 1 else 0)

    // スクロールした時に上部へ戻るボタンを表示 (検索バーやフィルターが隠れたら表示)
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 1 }
    }

    // 月切り替えや検索文字入力時はリスト先頭へスムーズに戻る
    LaunchedEffect(uiState.selectedMonth, uiState.stockSearchQuery) {
        if (listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    if (uiState.isSyncSettingsOpen) {
        SyncSettingsDialog(
            currentUrl = uiState.currentSyncUrl,
            lastUpdatedTime = uiState.lastUpdatedTime,
            onDismissRequest = viewModel::onCloseSyncSettings,
            onSaveAndSync = { url -> viewModel.onSaveSyncUrl(url, andSyncNow = true) },
            onResetToDefault = viewModel::onResetSyncUrl
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "優待銘柄 ＆ 在庫検索",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "全${uiState.totalMasterCount}銘柄",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onSyncInventory() },
                        enabled = !uiState.isSyncingInventory
                    ) {
                        if (uiState.isSyncingInventory) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "最新在庫を同期"
                            )
                        }
                    }
                    IconButton(onClick = viewModel::onOpenSyncSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "在庫同期設定"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "上部へ戻る",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("上部へ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            // 0. 恒久的な取得元表示 (FavoritesScreen等と統一)
            item(key = "sync_status") {
                val isError = uiState.lastSyncStatus == "error"
                Surface(
                    color = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val statusMsg = buildString {
                        if (isError) {
                            append("❌ データ取得失敗: ${uiState.lastUpdatedTime} (全サイトでエラー)")
                        } else {
                            val source = if (uiState.lastSyncSource.isBlank()) "内蔵データ" else uiState.lastSyncSource
                            append("✅ 最新データ取得元: $source (${uiState.lastUpdatedTime})")
                        }
                    }
                    Text(
                        text = statusMsg,
                        fontSize = 12.sp,
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // 1. 在庫同期メッセージ (手動同期時の結果。✕ボタンで非表示可能)
            uiState.syncMessage?.let { msg ->
                item(key = "sync_message") {
                    val isErrorMsg = msg.contains("❌") || msg.contains("エラー")
                    Surface(
                        color = if (isErrorMsg) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                color = if (isErrorMsg) MaterialTheme.colorScheme.onErrorContainer
                                        else MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearSyncMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "閉じる",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isErrorMsg) MaterialTheme.colorScheme.onErrorContainer
                                           else MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // 2. 検索バー (スクロールで自然に隠れる)
            item(key = "search_field") {
                OutlinedTextField(
                    value = uiState.stockSearchQuery,
                    onValueChange = viewModel::onStockSearchQueryChanged,
                    placeholder = { Text("コード・銘柄名・優待内容・業種で検索", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.stockSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onStockSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "クリア")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 3. フィルター領域 (月別タブ + 折りたたみ可能な詳細フィルター)
            item(key = "filters_section") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 月別チップ (検索語句が空の場合に表示)
                    if (uiState.stockSearchQuery.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(monthScrollState),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..12).forEach { month ->
                                FilterChip(
                                    selected = uiState.selectedMonth == month,
                                    onClick = { viewModel.onMonthSelected(month) },
                                    label = { Text("${month}月") }
                                )
                            }
                        }
                    }

                    // フィルターヘッダー (展開トグル ＆ クイックチップ ＆ 適用中条件表示)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 詳細フィルター開閉ボタン
                        FilterChip(
                            selected = isFilterExpanded || activeFilterCount > 0,
                            onClick = { isFilterExpanded = !isFilterExpanded },
                            leadingIcon = {
                                Icon(
                                    if (isFilterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                val countText = if (activeFilterCount > 0) " ($activeFilterCount)" else ""
                                Text(
                                    text = if (isFilterExpanded) "フィルターを畳む ▲" else "詳細絞込・金額$countText ▾",
                                    fontWeight = if (activeFilterCount > 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        )

                        // クイックトグル: 在庫あり
                        val hasAnyInv = uiState.inventoryFilter == InventoryFilterType.ANY_IN_STOCK
                        FilterChip(
                            selected = hasAnyInv,
                            onClick = {
                                viewModel.onInventoryFilterChanged(
                                    if (hasAnyInv) InventoryFilterType.ALL else InventoryFilterType.ANY_IN_STOCK
                                )
                            },
                            label = { Text("◎ 在庫あり", fontSize = 12.sp) }
                        )

                        // クイックトグル: 短期OK
                        val isShortTermOk = uiState.longTermFilter == LongTermFilterType.SHORT_TERM_OK
                        FilterChip(
                            selected = isShortTermOk,
                            onClick = {
                                viewModel.onLongTermFilterChanged(
                                    if (isShortTermOk) LongTermFilterType.ALL else LongTermFilterType.SHORT_TERM_OK
                                )
                            },
                            label = { Text("⚡ 短期OK", fontSize = 12.sp) }
                        )

                        // 金額フィルターが有効な場合、各選択レンジのバッジを表示
                        uiState.investmentFilters.forEach { filter ->
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.onToggleInvestmentFilter(filter) },
                                label = { Text("金額: ${filter.label}", fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "解除", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // 並び順が変更されている場合、バッジを表示
                        if (uiState.sortOrder != StockSortOrder.CODE_ASC) {
                            FilterChip(
                                selected = true,
                                onClick = { isFilterExpanded = true },
                                label = { Text("並順: ${uiState.sortOrder.label}", fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "解除", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // お気に入りのみフィルターが有効な場合
                        if (uiState.isFavoriteOnlyFilter) {
                            FilterChip(
                                selected = true,
                                onClick = viewModel::onToggleFavoriteFilter,
                                label = { Text("★ お気に入り", fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "解除", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // フィルター一括全解除ボタン
                        if (activeFilterCount > 0) {
                            AssistChip(
                                onClick = {
                                    viewModel.onInventoryFilterChanged(InventoryFilterType.ALL)
                                    viewModel.onLongTermFilterChanged(LongTermFilterType.ALL)
                                    viewModel.onClearInvestmentFilters()
                                    viewModel.onSortOrderChanged(StockSortOrder.CODE_ASC)
                                    if (uiState.isFavoriteOnlyFilter) viewModel.onToggleFavoriteFilter()
                                },
                                label = { Text("全解除", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }

                    // 詳細フィルター展開パネル (絞込・金額・並順の全項目)
                    AnimatedVisibility(visible = isFilterExpanded) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. 絞込条件（複数選択可）- 2列均等グリッド
                                Text(
                                    text = "絞込条件（複数選択可）",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Row 1: 在庫あり / 短期OK
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val hasAnyInv = uiState.inventoryFilter == InventoryFilterType.ANY_IN_STOCK
                                        FilterChip(
                                            selected = hasAnyInv,
                                            onClick = {
                                                viewModel.onInventoryFilterChanged(
                                                    if (hasAnyInv) InventoryFilterType.ALL else InventoryFilterType.ANY_IN_STOCK
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("◎ 在庫あり", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                        val isShortTermOk = uiState.longTermFilter == LongTermFilterType.SHORT_TERM_OK
                                        FilterChip(
                                            selected = isShortTermOk,
                                            onClick = {
                                                viewModel.onLongTermFilterChanged(
                                                    if (isShortTermOk) LongTermFilterType.ALL else LongTermFilterType.SHORT_TERM_OK
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("⚡ 短期OK", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                    }

                                    // Row 2: SBI在庫あり / 楽天在庫あり
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val isSbiOnly = uiState.inventoryFilter == InventoryFilterType.SBI_IN_STOCK
                                        FilterChip(
                                            selected = isSbiOnly,
                                            onClick = {
                                                viewModel.onInventoryFilterChanged(
                                                    if (isSbiOnly) InventoryFilterType.ALL else InventoryFilterType.SBI_IN_STOCK
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("SBI在庫あり", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                        val isRakOnly = uiState.inventoryFilter == InventoryFilterType.RAKUTEN_IN_STOCK
                                        FilterChip(
                                            selected = isRakOnly,
                                            onClick = {
                                                viewModel.onInventoryFilterChanged(
                                                    if (isRakOnly) InventoryFilterType.ALL else InventoryFilterType.RAKUTEN_IN_STOCK
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("楽天在庫あり", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                    }

                                    // Row 3: お気に入り / 長期条件あり
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = uiState.isFavoriteOnlyFilter,
                                            onClick = viewModel::onToggleFavoriteFilter,
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp),
                                                        tint = if (uiState.isFavoriteOnlyFilter) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("お気に入り", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                        val isReqLong = uiState.longTermFilter == LongTermFilterType.REQUIRES_LONG_TERM
                                        FilterChip(
                                            selected = isReqLong,
                                            onClick = {
                                                viewModel.onLongTermFilterChanged(
                                                    if (isReqLong) LongTermFilterType.ALL else LongTermFilterType.REQUIRES_LONG_TERM
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("⏱ 長期条件あり", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // 2. 取得金額（複数選択可・重複なし）- 2列均等グリッド
                                Text(
                                    text = "取得金額（複数選択可・重複なし）",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Row 1: 〜10万円 / 10〜30万円
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = uiState.investmentFilters.contains(InvestmentFilterType.UNDER_100K),
                                            onClick = { viewModel.onToggleInvestmentFilter(InvestmentFilterType.UNDER_100K) },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("〜10万円", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                        FilterChip(
                                            selected = uiState.investmentFilters.contains(InvestmentFilterType.RANGE_100K_300K),
                                            onClick = { viewModel.onToggleInvestmentFilter(InvestmentFilterType.RANGE_100K_300K) },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("10〜30万円", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                    }

                                    // Row 2: 30〜50万円 / 50〜100万円
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = uiState.investmentFilters.contains(InvestmentFilterType.RANGE_300K_500K),
                                            onClick = { viewModel.onToggleInvestmentFilter(InvestmentFilterType.RANGE_300K_500K) },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("30〜50万円", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                        FilterChip(
                                            selected = uiState.investmentFilters.contains(InvestmentFilterType.RANGE_500K_1M),
                                            onClick = { viewModel.onToggleInvestmentFilter(InvestmentFilterType.RANGE_500K_1M) },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("50〜100万円", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                    }

                                    // Row 3: 100万円超 / 金額クリア (問わず)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = uiState.investmentFilters.contains(InvestmentFilterType.OVER_1M),
                                            onClick = { viewModel.onToggleInvestmentFilter(InvestmentFilterType.OVER_1M) },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("100万円超", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                        FilterChip(
                                            selected = uiState.investmentFilters.isEmpty(),
                                            onClick = { viewModel.onClearInvestmentFilters() },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("全価格帯 (問わず)", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // 3. 並び順 - 2列均等グリッド
                                Text(
                                    text = "並び順",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = uiState.sortOrder == StockSortOrder.CODE_ASC,
                                            onClick = { viewModel.onSortOrderChanged(StockSortOrder.CODE_ASC) },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("# コード順", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                        FilterChip(
                                            selected = uiState.sortOrder == StockSortOrder.YIELD_DESC,
                                            onClick = { viewModel.onSortOrderChanged(StockSortOrder.YIELD_DESC) },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("📈 利回り順", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = uiState.sortOrder == StockSortOrder.GIFT_VALUE_DESC,
                                            onClick = { viewModel.onSortOrderChanged(StockSortOrder.GIFT_VALUE_DESC) },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("🎁 優待額順", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                        FilterChip(
                                            selected = uiState.sortOrder == StockSortOrder.INVENTORY_DESC,
                                            onClick = { viewModel.onSortOrderChanged(StockSortOrder.INVENTORY_DESC) },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("📦 在庫数順", fontSize = 12.sp)
                                                }
                                            }
                                        )
                                    }
                                }

                                // 下部操作: 条件リセット ＆ 閉じる
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (activeFilterCount > 0) {
                                        TextButton(
                                            onClick = {
                                                viewModel.onInventoryFilterChanged(InventoryFilterType.ALL)
                                                viewModel.onLongTermFilterChanged(LongTermFilterType.ALL)
                                                viewModel.onClearInvestmentFilters()
                                                viewModel.onSortOrderChanged(StockSortOrder.CODE_ASC)
                                                if (uiState.isFavoriteOnlyFilter) viewModel.onToggleFavoriteFilter()
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                "全条件を初期化 (${activeFilterCount}件適用中)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    TextButton(
                                        onClick = { isFilterExpanded = false },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("▲ 閉じる", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. 該当件数表示
            item(key = "result_count") {
                Text(
                    text = "該当: ${uiState.searchResults.size} 件 (タップで試算)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 5. 銘柄一覧リスト (検索結果なし or 銘柄カード一覧)
            if (uiState.searchResults.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "条件に一致する銘柄が見つかりませんでした",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                items(uiState.searchResults, key = { it.tickerCode }) { stock ->
                    StockCard(
                        stock = stock,
                        isFavorite = uiState.favoriteTickers.contains(stock.tickerCode),
                        onToggleFavorite = { viewModel.toggleFavorite(stock.tickerCode) },
                        onStockSelected = { onStockSelected(stock) },
                        currencyFormat = currencyFormat
                    )
                }
            }
        }
    }
}

@Composable
private fun StockCard(
    stock: StockPreset,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onStockSelected: () -> Unit,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onStockSelected() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1行目: コード + 銘柄名 (横幅いっぱい) + 右端お気に入り★
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = stock.tickerCode,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stock.stockName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "お気に入り",
                        tint = if (isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 2行目: 優待内容 + 右端に優待換算額・利回りバッジ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stock.giftDescription,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "優待約${currencyFormat.format(stock.giftValue)}円 (${String.format(Locale.US, "%.2f", stock.estimatedYield)}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 3行目: 一般信用在庫バッジ (SBI / 楽天) ＆ 長期条件バッジ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SBI在庫バッジ
                val sbiStatus = stock.sbiInventory.status
                val sbiColor = when (sbiStatus) {
                    InventoryStatus.IN_STOCK -> Color(0xFF2E7D32)
                    InventoryStatus.LOW_STOCK -> Color(0xFFE65100)
                    InventoryStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.outline
                }
                Surface(
                    color = sbiColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "SBI: ${sbiStatus.shortBadge} (${stock.sbiInventory.formattedShares})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = sbiColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // 楽天在庫バッジ
                val rakStatus = stock.rakutenInventory.status
                val rakColor = when (rakStatus) {
                    InventoryStatus.IN_STOCK -> Color(0xFF2E7D32)
                    InventoryStatus.LOW_STOCK -> Color(0xFFE65100)
                    InventoryStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.outline
                }
                Surface(
                    color = rakColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "楽天: ${rakStatus.shortBadge} (${stock.rakutenInventory.formattedShares})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = rakColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // 長期条件バッジ
                if (stock.requiresLongTerm) {
                    Surface(
                        color = Color(0xFFE65100).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (stock.longTermDescription.isNotBlank()) "⏱ 長期(${stock.longTermDescription})" else "⏱ 長期要",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFF00796B).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "⚡ 短期OK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00796B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 4行目: 目安株価・業種 ＆ 試算アクションボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val totalRequired = stock.defaultPrice * stock.defaultQuantity
                    val formattedRequired = when {
                        totalRequired >= 10_000 -> "${String.format(Locale.US, "%.1f", totalRequired / 10000.0)}万円"
                        else -> "${currencyFormat.format(totalRequired.toInt())}円"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "必要額: $formattedRequired",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " (${currencyFormat.format(stock.defaultPrice.toInt())}円×${stock.defaultQuantity}株)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = "${if (stock.sector.isNotBlank()) "${stock.sector} / " else ""}${stock.months.joinToString(",")}月権利",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                FilledTonalButton(
                    onClick = onStockSelected,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("試算する ❯", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
