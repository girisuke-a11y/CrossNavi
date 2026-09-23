package com.example.crossnavi.ui.holdings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crossnavi.model.Broker
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingsScreen(
    viewModel: HoldingsViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.holdingItems.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.JAPAN) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d (E)", Locale.JAPANESE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("保有クロス ＆ 現渡管理", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 1. サマリーカード
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("保有クロスサマリー", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (summary.pendingDeliveryCount > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("未現渡 ${summary.pendingDeliveryCount}件", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("すべて現渡済み", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("見込み総純利益:", style = MaterialTheme.typography.bodyMedium)
                            Text("+${currencyFormat.format(summary.totalNetProfit)} 円", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("優待価値合計 / 総コスト:", style = MaterialTheme.typography.bodySmall)
                            Text("${currencyFormat.format(summary.totalGiftValue)} 円 / -${currencyFormat.format(summary.totalCost)} 円", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("拘束資金額 (約定代金):", style = MaterialTheme.typography.bodySmall)
                            Text("${currencyFormat.format(summary.totalTradeAmount.toInt())} 円", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 2. リスト表示
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "保有中のクロス銘柄はありません。\n「試算」タブから条件を計算して保存してください。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(items, key = { it.holding.id }) { itemUi ->
                    val h = itemUi.holding
                    val isDelivered = h.isDelivered

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDelivered)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDelivered) 1.dp else 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // ヘッダー行: コード + 社名 + 証券会社バッジ
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(h.tickerCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(h.stockName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }

                                // 証券会社バッジ
                                Surface(
                                    color = if (h.broker == Broker.SBI) Color(0xFF1E88E5) else Color(0xFFE53935),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        h.broker.displayName,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // 信用区分・株価・株数
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${h.creditType.displayName} (${h.annualRate}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text("${currencyFormat.format(h.stockPrice.toInt())}円 × ${h.quantity}株", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }

                            // 日程とコスト・利益
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("建 ${h.openDate.format(dateFormatter)} → 渡 ${h.deliveryDate.format(dateFormatter)} (${itemUi.holdingDays}日)", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "純利: +${currencyFormat.format(itemUi.netProfit)} 円",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDelivered) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            // アクション行: 現渡チェック ＆ 削除
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isDelivered,
                                        onCheckedChange = { viewModel.toggleDelivery(h.id) }
                                    )
                                    Text(
                                        if (isDelivered) "現渡完了済み" else "現渡待ち (権利落ち日に注文)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (!isDelivered) FontWeight.Bold else FontWeight.Normal,
                                        color = if (!isDelivered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                    )
                                }

                                IconButton(onClick = { viewModel.deleteHolding(h.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
