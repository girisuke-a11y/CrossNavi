package com.example.crossnavi.ui.favorites

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crossnavi.data.InventoryFilterType
import com.example.crossnavi.model.InventoryStatus
import com.example.crossnavi.model.StockPreset
import com.example.crossnavi.ui.components.SyncSettingsDialog
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onNavigateToCalculate: (StockPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = NumberFormat.getNumberInstance(Locale.JAPAN)
    val invScrollState = rememberScrollState()

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
                            text = "お気に入り銘柄",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${uiState.favoriteStocks.size}件",
                                fontSize = 12.sp,
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
                        enabled = !uiState.isSyncing
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "最新在庫を同期")
                        }
                    }
                    IconButton(
                        onClick = viewModel::onOpenSyncSettings
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "在庫同期設定")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 同期状態の表示
            val isError = uiState.lastSyncStatus == "error"
            Surface(
                color = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val msg = buildString {
                    if (isError) {
                        append("❌ データ取得失敗: ${uiState.lastUpdatedTime} (全サイトでエラー)")
                    } else {
                        val source = if (uiState.lastSyncSource.isBlank()) "内蔵データ" else uiState.lastSyncSource
                        append("✅ 最新データ取得元: $source (${uiState.lastUpdatedTime})")
                    }
                }
                Text(
                    text = msg,
                    fontSize = 12.sp,
                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // 在庫絞り込みチップ
            if (uiState.favoriteStocks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(invScrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("在庫:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    InventoryFilterType.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.inventoryFilter == filter,
                            onClick = { viewModel.onInventoryFilterSelected(filter) },
                            label = { Text(filter.label, fontSize = 12.sp) }
                        )
                    }
                }

                // 長期条件絞り込みチップ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("条件:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    com.example.crossnavi.data.LongTermFilterType.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.longTermFilter == filter,
                            onClick = { viewModel.onLongTermFilterSelected(filter) },
                            label = {
                                val icon = when (filter) {
                                    com.example.crossnavi.data.LongTermFilterType.ALL -> ""
                                    com.example.crossnavi.data.LongTermFilterType.SHORT_TERM_OK -> "⚡ "
                                    com.example.crossnavi.data.LongTermFilterType.REQUIRES_LONG_TERM -> "⏱ "
                                }
                                Text("$icon${filter.label}", fontSize = 12.sp)
                            }
                        )
                    }
                }

                // 並び順絞り込みチップ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("並び順:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    com.example.crossnavi.data.StockSortOrder.entries.forEach { order ->
                        FilterChip(
                            selected = uiState.sortOrder == order,
                            onClick = { viewModel.onSortOrderSelected(order) },
                            label = {
                                val icon = when (order) {
                                    com.example.crossnavi.data.StockSortOrder.CODE_ASC -> "# "
                                    com.example.crossnavi.data.StockSortOrder.YIELD_DESC -> "📈 "
                                    com.example.crossnavi.data.StockSortOrder.GIFT_VALUE_DESC -> "🎁 "
                                    com.example.crossnavi.data.StockSortOrder.INVENTORY_DESC -> "📦 "
                                }
                                Text("$icon${order.label}", fontSize = 12.sp)
                            }
                        )
                    }
                }
            }

            // メインコンテンツ
            if (uiState.favoriteStocks.isEmpty()) {
                // 0件時の空画面ガイド
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Outlined.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "お気に入り銘柄がありません",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "銘柄選択シートや試算画面の「★」アイコンをタップすると、気になる銘柄をここに追加して在庫状況をいつでもウォッチできます。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else if (uiState.filteredStocks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "選択した在庫条件に一致するお気に入り銘柄はありません",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                // 銘柄リスト
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.filteredStocks, key = { it.tickerCode }) { stock ->
                        FavoriteStockCard(
                            stock = stock,
                            currencyFormat = currencyFormat,
                            onRemoveFavorite = { viewModel.onRemoveFavorite(stock.tickerCode) },
                            onCalculate = { onNavigateToCalculate(stock) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteStockCard(
    stock: StockPreset,
    currencyFormat: NumberFormat,
    onRemoveFavorite: () -> Unit,
    onCalculate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1行目: コード・銘柄名・お気に入り解除ボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = stock.tickerCode,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stock.stockName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // お気に入り解除★ボタン
                IconButton(onClick = onRemoveFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "お気に入り解除",
                        tint = Color(0xFFFFB300), // ゴールドイエロー
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 2行目: 信用在庫バッジ (SBI / 楽天)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // 2.5行目: 長期保有条件バッジ (短期OK / 長期条件あり)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stock.requiresLongTerm) {
                    Surface(
                        color = Color(0xFFE65100).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱ 長期条件あり",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            if (stock.longTermDescription.isNotBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${stock.longTermDescription})",
                                    fontSize = 10.sp,
                                    color = Color(0xFFE65100),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = Color(0xFF00796B).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "⚡ 短期OK (長期不要)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00796B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 3行目: 優待内容 & 換算額
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stock.giftDescription,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "約${currencyFormat.format(stock.giftValue)}円相当 (${String.format(Locale.US, "%.2f", stock.estimatedYield)}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // 4行目: 株価目安・権利月 ＆ 「試算する」アクションボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "株価目安: ${currencyFormat.format(stock.defaultPrice.toInt())}円 (${stock.months.joinToString(",")}月権利)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Button(
                    onClick = onCalculate,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("試算する", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
