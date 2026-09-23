package com.example.crossnavi.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crossnavi.data.StockMasterRepository

/**
 * 一般信用在庫のクラウド同期URL設定ダイアログ
 * PC不要・スマホ単体での更新設定を管理
 */
@Composable
fun SyncSettingsDialog(
    currentUrl: String,
    lastUpdatedTime: String,
    onDismissRequest: () -> Unit,
    onSaveAndSync: (String) -> Unit,
    onResetToDefault: () -> Unit
) {
    var editingUrl by remember(currentUrl) { mutableStateOf(currentUrl) }
    val presetScrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                Icons.Default.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "一般信用 在庫同期設定",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "💡 スマホ単体で完結 (PC起動不要)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "クラウド上の最新在庫JSON（SBI証券・楽天証券）を直接取得します。PCを起動したりAPKを入れ直す必要はありません。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // プリセット切替チップ
                Text(
                    text = "クイック切替プリセット:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(presetScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isInternal = editingUrl == StockMasterRepository.INTERNAL_PRESET_URL || editingUrl.isBlank()
                    FilterChip(
                        selected = isInternal,
                        onClick = { editingUrl = StockMasterRepository.INTERNAL_PRESET_URL },
                        label = { Text("📱 アプリ内蔵データ (推奨)", fontSize = 11.sp) }
                    )

                    val isCloud = editingUrl == StockMasterRepository.DEFAULT_CLOUD_URL
                    FilterChip(
                        selected = isCloud,
                        onClick = { editingUrl = StockMasterRepository.DEFAULT_CLOUD_URL },
                        label = { Text("🌐 クラウド (GitHub)", fontSize = 11.sp) }
                    )

                    val isLocal = editingUrl == StockMasterRepository.LOCAL_TEST_URL
                    FilterChip(
                        selected = isLocal,
                        onClick = { editingUrl = StockMasterRepository.LOCAL_TEST_URL },
                        label = { Text("💻 PCローカル (10.0.2.2)", fontSize = 11.sp) }
                    )
                }

                // URL入力欄
                OutlinedTextField(
                    value = editingUrl,
                    onValueChange = { editingUrl = it },
                    label = { Text("在庫JSON 取得先URL (HTTPS/HTTP)") },
                    placeholder = { Text("https://raw.githubusercontent.com/...") },
                    trailingIcon = {
                        if (editingUrl.isNotEmpty()) {
                            IconButton(onClick = { editingUrl = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "クリア")
                            }
                        }
                    },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (editingUrl.contains("mayon-dev")) {
                    Text(
                        text = "💡 「mayon-dev」は初期サンプルの仮URLです。未連携時は自動的に「アプリ内蔵データ」で更新されます。独自リポジトリをお持ちの場合はURLを書き換えてください。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        lineHeight = 15.sp
                    )
                }

                Text(
                    text = "現在の更新情報: $lastUpdatedTime",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveAndSync(editingUrl) }
            ) {
                Text("保存して今すぐ同期")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        editingUrl = StockMasterRepository.DEFAULT_CLOUD_URL
                        onResetToDefault()
                    }
                ) {
                    Text("初期値に戻す")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismissRequest) {
                    Text("閉じる")
                }
            }
        }
    )
}
