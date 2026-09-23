package com.example.crossnavi.ui.calculator

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crossnavi.data.StockPresetData
import com.example.crossnavi.model.Broker
import com.example.crossnavi.model.CreditType
import com.example.crossnavi.model.StockPreset
import com.example.crossnavi.ui.components.SyncSettingsDialog
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.JAPAN) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd (E)", Locale.JAPANESE) }

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
                title = { Text("優待クロス コスト試算", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = viewModel::onOpenSyncSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "在庫同期設定",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 結果サマリーカード
            uiState.calculationResult?.let { res ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (res.netProfit >= 0)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "見込み純利益",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${if (res.netProfit >= 0) "+" else ""}${currencyFormat.format(res.netProfit)} 円",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (res.netProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("借株日数 (受渡日基準):", style = MaterialTheme.typography.bodyMedium)
                            Text("${res.holdingDays} 日間", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("貸株料:", style = MaterialTheme.typography.bodyMedium)
                            Text("${currencyFormat.format(res.borrowFee)} 円 (${String.format(Locale.US, "%.1f", res.dailyBorrowFee)}円/日)", fontWeight = FontWeight.SemiBold)
                        }
                        if (res.dividendGapCost > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("配当落調整金差額 (税金):", style = MaterialTheme.typography.bodyMedium)
                                Text("${currencyFormat.format(res.dividendGapCost)} 円", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("総コスト (実質手数料):", style = MaterialTheme.typography.bodyMedium)
                            Text("${currencyFormat.format(res.totalCost)} 円", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("実質利回り / 損益分岐日数:", style = MaterialTheme.typography.bodyMedium)
                            Text("${String.format(Locale.US, "%.2f", res.netYield)}% / ${res.breakEvenDays}日", fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        OutlinedButton(
                            onClick = {
                                val stockLabel = if (uiState.tickerCode.isNotBlank() || uiState.stockName.isNotBlank())
                                    "${uiState.tickerCode} ${uiState.stockName}".trim()
                                else "指定なし"
                                val shareText = buildString {
                                    appendLine("【CrossNavi クロス取引試算】")
                                    appendLine("銘柄: $stockLabel (${uiState.quantity}株 / 株価${uiState.stockPrice}円)")
                                    appendLine("信用区分: ${uiState.selectedBroker.displayName} ${uiState.selectedCreditType.displayName} (年利${String.format(Locale.US, "%.2f", uiState.currentAnnualRate)}%)")
                                    appendLine("借株期間: ${res.holdingDays}日間 (${uiState.openDate.format(dateFormatter)} 〜 ${uiState.deliveryDate.format(dateFormatter)})")
                                    appendLine("--------------------------")
                                    appendLine("見込み純利益: ${if (res.netProfit >= 0) "+" else ""}${currencyFormat.format(res.netProfit)}円 (実質利回り ${String.format(Locale.US, "%.2f", res.netYield)}%)")
                                    appendLine("優待換算価値: ${currencyFormat.format(uiState.giftValue.toIntOrNull() ?: 0)}円${if (uiState.giftDescription.isNotBlank()) " (${uiState.giftDescription})" else ""}")
                                    appendLine("総コスト: ${currencyFormat.format(res.totalCost)}円 (貸株料 ${currencyFormat.format(res.borrowFee)}円${if (res.dividendGapCost > 0) " + 配当差額 ${currencyFormat.format(res.dividendGapCost)}円" else ""})")
                                    appendLine("損益分岐借株日数: ${res.breakEvenDays}日")
                                    appendLine("--------------------------")
                                    append("※CrossNaviで試算")
                                }
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("CrossNavi試算結果", shareText)
                                clipboard?.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "試算結果をクリップボードにコピーしました", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("試算結果テキストをコピー", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 2. 証券会社・信用区分の選択 (金利は固定・入力不要)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("証券会社 ＆ 信用区分", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // 証券会社タブ (SBI / 楽天)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        Broker.entries.forEachIndexed { index, broker ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = Broker.entries.size),
                                onClick = { viewModel.onBrokerChanged(broker) },
                                selected = uiState.selectedBroker == broker
                            ) {
                                Text(broker.displayName)
                            }
                        }
                    }

                    // 信用区分チップ
                    Text("信用区分:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.selectedBroker.creditTypes.forEach { creditType ->
                            FilterChip(
                                selected = uiState.selectedCreditType == creditType,
                                onClick = { viewModel.onCreditTypeChanged(creditType) },
                                label = { Text(creditType.displayName, fontSize = 12.sp) }
                            )
                        }
                    }

                    // 適用金利の固定表示バッジ (入力フィールドではなく情報表示)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "適用年利 (固定):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.2f", uiState.currentAnnualRate)} %",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (uiState.selectedCreditType.isStandard) {
                        Text(
                            text = "※制度信用は逆日歩（品貸料）が発生するリスクがあります。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 3. 銘柄情報 (月別プリセットから選択 ＆ 手動入力)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("銘柄情報", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.tickerCode.isNotBlank()) {
                                val isFav = uiState.favoriteTickers.contains(uiState.tickerCode)
                                IconButton(
                                    onClick = { viewModel.toggleFavorite(uiState.tickerCode) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        if (isFav) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = if (isFav) "お気に入り解除" else "お気に入り登録",
                                        tint = if (isFav) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            // 銘柄リスト選択ボタン
                            FilledTonalButton(
                                onClick = { viewModel.onOpenStockPicker() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("権利銘柄から選ぶ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // 選択された優待内容のアシスト表示
                    if (uiState.giftDescription.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("優待内容:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    uiState.giftDescription,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )

                                // 選択中銘柄の在庫ステータス
                                uiState.selectedStockPreset?.let { preset ->
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("信用在庫:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.SemiBold)

                                        // SBI
                                        val sbiColor = when (preset.sbiInventory.status) {
                                            com.example.crossnavi.model.InventoryStatus.IN_STOCK -> Color(0xFF2E7D32)
                                            com.example.crossnavi.model.InventoryStatus.LOW_STOCK -> Color(0xFFE65100)
                                            com.example.crossnavi.model.InventoryStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.outline
                                        }
                                        Surface(color = sbiColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = "SBI: ${preset.sbiInventory.status.shortBadge} (${preset.sbiInventory.formattedShares})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = sbiColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        // 楽天
                                        val rakColor = when (preset.rakutenInventory.status) {
                                            com.example.crossnavi.model.InventoryStatus.IN_STOCK -> Color(0xFF2E7D32)
                                            com.example.crossnavi.model.InventoryStatus.LOW_STOCK -> Color(0xFFE65100)
                                            com.example.crossnavi.model.InventoryStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.outline
                                        }
                                        Surface(color = rakColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = "楽天: ${preset.rakutenInventory.status.shortBadge} (${preset.rakutenInventory.formattedShares})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = rakColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // 長期保有条件の注意アラート
                                    if (preset.requiresLongTerm) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "【長期保有条件あり】${if (preset.longTermDescription.isNotBlank()) preset.longTermDescription else "1年以上等の継続保有が必要な場合があります。"}\n単発クロスでは優待対象外となる場合があるため公式IRをご確認ください。",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.tickerCode,
                            onValueChange = viewModel::onTickerChanged,
                            label = { Text("コード (例: 9202)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.stockName,
                            onValueChange = viewModel::onStockNameChanged,
                            label = { Text("銘柄名 (例: ANA)") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.stockPrice,
                            onValueChange = viewModel::onStockPriceChanged,
                            label = { Text("株価 (円)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.quantity,
                            onValueChange = viewModel::onQuantityChanged,
                            label = { Text("株数 (株)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }

            // 4. 日程設定 (取得約定日 & 現渡約定日)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("クロス日程 (約定日)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // 取得日
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val d = uiState.openDate
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        viewModel.onOpenDateChanged(LocalDate.of(year, month + 1, dayOfMonth))
                                    },
                                    d.year, d.monthValue - 1, d.dayOfMonth
                                ).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("取得日 (新規建 約定日)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(uiState.openDate.format(dateFormatter), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.DateRange, contentDescription = "取得日選択")
                        }
                    }

                    // 現渡日
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val d = uiState.deliveryDate
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        viewModel.onDeliveryDateChanged(LocalDate.of(year, month + 1, dayOfMonth))
                                    },
                                    d.year, d.monthValue - 1, d.dayOfMonth
                                ).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("現渡日 (決済 約定日)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(uiState.deliveryDate.format(dateFormatter), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.DateRange, contentDescription = "現渡日選択")
                        }
                    }
                }
            }

            // 5. 優待換算価値 & 配当予想
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("優待価値 ＆ 配当金", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = uiState.giftValue,
                        onValueChange = viewModel::onGiftValueChanged,
                        label = { Text("優待換算価値 (円)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.dividendPerShare,
                        onValueChange = viewModel::onDividendChanged,
                        label = { Text("1株当たり配当金 (円, 任意)") },
                        supportingText = { Text("※配当がある銘柄は税金差額(約15.3%)がコストになります") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }

            // 6. 保有管理への保存ボタン
            Button(
                onClick = { viewModel.saveToHoldings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSavedSuccess
            ) {
                if (uiState.isSavedSuccess) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保有管理に保存しました！", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保有管理に保存する", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- 優待銘柄選択用モーダルボトムシート ---
        if (uiState.isStockPickerVisible) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onCloseStockPicker() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                StockPickerSheetContent(
                    selectedMonth = uiState.selectedMonth,
                    brokerFilter = uiState.brokerFilter,
                    inventoryFilter = uiState.inventoryFilter,
                    longTermFilter = uiState.longTermFilter,
                    sortOrder = uiState.sortOrder,
                    isFavoriteOnly = uiState.isFavoriteOnlyFilter,
                    favoriteTickers = uiState.favoriteTickers,
                    isSyncing = uiState.isSyncingInventory,
                    syncMessage = uiState.syncMessage,
                    searchQuery = uiState.stockSearchQuery,
                    searchResults = uiState.searchResults,
                    totalCount = uiState.totalMasterCount,
                    onMonthSelected = viewModel::onMonthSelected,
                    onBrokerFilterSelected = viewModel::onBrokerFilterChanged,
                    onInventoryFilterSelected = viewModel::onInventoryFilterChanged,
                    onLongTermFilterSelected = viewModel::onLongTermFilterChanged,
                    onSortOrderSelected = viewModel::onSortOrderChanged,
                    onToggleFavoriteOnly = viewModel::onToggleFavoriteFilter,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onSyncInventory = { viewModel.onSyncInventory() },
                    onOpenSyncSettings = viewModel::onOpenSyncSettings,
                    onSearchQueryChanged = viewModel::onStockSearchQueryChanged,
                    onStockSelected = viewModel::onSelectStockPreset,
                    currencyFormat = currencyFormat
                )
            }
        }
    }
}

@Composable
private fun StockPickerSheetContent(
    selectedMonth: Int,
    brokerFilter: com.example.crossnavi.data.BrokerFilterType,
    inventoryFilter: com.example.crossnavi.data.InventoryFilterType,
    longTermFilter: com.example.crossnavi.data.LongTermFilterType,
    sortOrder: com.example.crossnavi.data.StockSortOrder,
    isFavoriteOnly: Boolean,
    favoriteTickers: Set<String>,
    isSyncing: Boolean,
    syncMessage: String?,
    searchQuery: String,
    searchResults: List<StockPreset>,
    totalCount: Int,
    onMonthSelected: (Int) -> Unit,
    onBrokerFilterSelected: (com.example.crossnavi.data.BrokerFilterType) -> Unit,
    onInventoryFilterSelected: (com.example.crossnavi.data.InventoryFilterType) -> Unit,
    onLongTermFilterSelected: (com.example.crossnavi.data.LongTermFilterType) -> Unit,
    onSortOrderSelected: (com.example.crossnavi.data.StockSortOrder) -> Unit,
    onToggleFavoriteOnly: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSyncInventory: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onStockSelected: (StockPreset) -> Unit,
    currencyFormat: NumberFormat
) {
    val monthScrollState = rememberScrollState()
    val invScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.90f)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "権利確定 優待銘柄を選択",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 在庫更新ボタン
                IconButton(
                    onClick = onSyncInventory,
                    enabled = !isSyncing,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "最新在庫を更新", modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(
                    onClick = onOpenSyncSettings,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "在庫同期設定",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "全${totalCount}銘柄網羅",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 同期メッセージ表示
        syncMessage?.let { msg ->
            val isError = msg.contains("❌") || msg.contains("エラー")
            Surface(
                color = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    fontSize = 12.sp,
                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // 検索バー
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("コード・銘柄名・優待内容・業種で検索", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "クリア")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // 1. 在庫状況フィルターチップ ＆ お気に入りフィルターチップ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(invScrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("在庫:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            com.example.crossnavi.data.InventoryFilterType.entries.forEach { filter ->
                FilterChip(
                    selected = inventoryFilter == filter,
                    onClick = { onInventoryFilterSelected(filter) },
                    label = { Text(filter.label, fontSize = 12.sp) }
                )
            }

            // ★ お気に入りのみ フィルターチップ
            FilterChip(
                selected = isFavoriteOnly,
                onClick = onToggleFavoriteOnly,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isFavoriteOnly) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("★ お気に入りのみ", fontSize = 12.sp)
                    }
                }
            )
        }

        // 2. 証券会社取扱フィルターチップ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("取扱:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            com.example.crossnavi.data.BrokerFilterType.entries.forEach { filter ->
                FilterChip(
                    selected = brokerFilter == filter,
                    onClick = { onBrokerFilterSelected(filter) },
                    label = { Text(filter.label, fontSize = 12.sp) }
                )
            }
        }

        // 3. 長期保有条件フィルターチップ (短期OK / 長期条件あり)
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
                    selected = longTermFilter == filter,
                    onClick = { onLongTermFilterSelected(filter) },
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

        // 3.5 並び順フィルターチップ (コード順 / 利回り順 / 優待額順 / 在庫数順)
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
                    selected = sortOrder == order,
                    onClick = { onSortOrderSelected(order) },
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

        // 4. 月別タブチップ (検索語句が空の場合に表示)
        if (searchQuery.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(monthScrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..12).forEach { month ->
                    FilterChip(
                        selected = selectedMonth == month,
                        onClick = { onMonthSelected(month) },
                        label = { Text("${month}月") }
                    )
                }
            }
        }

        // 該当件数表示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "該当: ${searchResults.size} 件",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 銘柄一覧リスト
        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "条件に一致する銘柄が見つかりませんでした",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(searchResults) { stock ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStockSelected(stock) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // 1行目: コード・銘柄名・優待換算額
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
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
                                        fontSize = 15.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
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

                                    Spacer(modifier = Modifier.width(4.dp))

                                    val isFav = favoriteTickers.contains(stock.tickerCode)
                                    IconButton(
                                        onClick = { onToggleFavorite(stock.tickerCode) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            if (isFav) Icons.Default.Star else Icons.Outlined.StarBorder,
                                            contentDescription = "お気に入り",
                                            tint = if (isFav) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // 2行目: 優待内容
                            Text(
                                text = stock.giftDescription,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 3行目: ★一般信用 在庫状況バッジ (SBI / 楽天)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // SBI在庫バッジ
                                val sbiStatus = stock.sbiInventory.status
                                val sbiColor = when (sbiStatus) {
                                    com.example.crossnavi.model.InventoryStatus.IN_STOCK -> Color(0xFF2E7D32)
                                    com.example.crossnavi.model.InventoryStatus.LOW_STOCK -> Color(0xFFE65100)
                                    com.example.crossnavi.model.InventoryStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.outline
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
                                    com.example.crossnavi.model.InventoryStatus.IN_STOCK -> Color(0xFF2E7D32)
                                    com.example.crossnavi.model.InventoryStatus.LOW_STOCK -> Color(0xFFE65100)
                                    com.example.crossnavi.model.InventoryStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.outline
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
                            }

                            // 3.5行目: 長期保有条件バッジ (短期OK / 長期条件あり)
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

                            // 4行目: 目安株価・業種・権利月
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (stock.sector.isNotBlank()) "業種: ${stock.sector}" else "",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "株価目安: ${currencyFormat.format(stock.defaultPrice.toInt())}円",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
