package com.example.crossnavi.ui.calculator

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crossnavi.model.Broker
import com.example.crossnavi.model.CreditType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostCalculatorSheet(
    viewModel: CalculatorViewModel,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.JAPAN) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd (E)", Locale.JAPANESE) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // シートヘッダー
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "コスト試算",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.tickerCode.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${uiState.tickerCode} ${uiState.stockName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.tickerCode.isNotBlank()) {
                        val isFav = uiState.favoriteTickers.contains(uiState.tickerCode)
                        IconButton(
                            onClick = { viewModel.toggleFavorite(uiState.tickerCode) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (isFav) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "お気に入り",
                                tint = if (isFav) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "閉じる")
                    }
                }
            }

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
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("CrossNavi試算結果", shareText)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "試算結果をクリップボードにコピーしました", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("試算結果テキストをコピー", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 2. 証券会社・信用区分の選択
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("証券会社 ＆ 信用区分", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
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
                }
            }

            // 3. 銘柄情報・優待内容
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("銘柄情報 ＆ 優待内容", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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

                                uiState.selectedStockPreset?.let { preset ->
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
                                                    text = "【長期保有条件あり】${if (preset.longTermDescription.isNotBlank()) preset.longTermDescription else "1年以上等の継続保有が必要な場合があります。"}",
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
                            label = { Text("コード") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.stockName,
                            onValueChange = viewModel::onStockNameChanged,
                            label = { Text("銘柄名") },
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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.giftValue,
                            onValueChange = viewModel::onGiftValueChanged,
                            label = { Text("優待価値 (円)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.dividendPerShare,
                            onValueChange = viewModel::onDividendChanged,
                            label = { Text("配当 (円, 任意)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }
            }

            // 4. クロス日程 (約定日)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            modifier = Modifier.padding(12.dp),
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
                            modifier = Modifier.padding(12.dp),
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

            // 5. 保有管理への保存ボタン
            Button(
                onClick = { viewModel.saveToHoldings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSavedSuccess
            ) {
                if (uiState.isSavedSuccess) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保有管理に保存しました！", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保有管理に保存する", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
