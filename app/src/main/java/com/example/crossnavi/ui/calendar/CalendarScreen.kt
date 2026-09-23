package com.example.crossnavi.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RightsSchedule(
    val monthNumber: Int,
    val monthName: String,
    val lastTradingDay: String,      // 権利付き最終日
    val exDividendDate: String,      // 権利落ち日 (現渡発注可能)
    val settlementDate: String,      // 権利確定日 / 受渡日
    val note: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToMonth: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 主な権利確定日スケジュール (2026年参考データ)
    val schedules = listOf(
        RightsSchedule(9, "2026年 9月 (秋の大型権利月)", "2026/09/28 (月)", "2026/09/29 (火)", "2026/09/30 (水)", "ANA、JAL、コロワイド等人気銘柄多数"),
        RightsSchedule(10, "2026年 10月", "2026/10/28 (水)", "2026/10/29 (木)", "2026/10/30 (金)", "神戸物産、エイチ・アイ・エスなど"),
        RightsSchedule(11, "2026年 11月", "2026/11/26 (木)", "2026/11/27 (金)", "2026/11/30 (月)", "ヴィレッジヴァンガード、タマホームなど"),
        RightsSchedule(12, "2026年 12月 (年末の大型権利月)", "2026/12/28 (月)", "2026/12/29 (火)", "2026/12/30 (水)", "すかいらーく、マクドナルド、JTなど"),
        RightsSchedule(1, "2027年 1月", "2027/01/27 (水)", "2027/01/28 (木)", "2027/01/29 (金)", "積水ハウス、鳥貴族など"),
        RightsSchedule(2, "2027年 2月 (小売り・飲食権利月)", "2027/02/24 (水)", "2027/02/25 (木)", "2027/02/26 (金)", "イオン、吉野家、クリレスなど"),
        RightsSchedule(3, "2027年 3月 (年間最大の権利月)", "2027/03/29 (月)", "2027/03/30 (火)", "2027/03/31 (水)", "全上場企業の半数以上が集中")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("権利確定日 ＆ 争奪戦ガイド", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 争奪戦・クロス取引の重要タイムテーブルカード
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SBI・楽天 争奪戦 ＆ 発注の鉄則", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider()

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⏰ 毎日 19:00（夜間争奪戦）", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("SBI証券・楽天証券ともに一般信用短期の在庫補充・抽選受付が開始されます。", style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("⏰ 権利付き最終日の 15:30（大引け後）", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("夕方（17:00〜19:00頃）各証券会社の日付更新が完了したら、すぐに【現渡（品渡）】注文を発注しましょう！", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // スケジュール一覧
            item {
                Text("主要権利確定カレンダー", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(schedules.size) { index ->
                val s = schedules[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(s.monthName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("権利付き最終日 (クロス約定):", style = MaterialTheme.typography.bodyMedium)
                            Text(s.lastTradingDay, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("権利落ち日 (現渡日):", style = MaterialTheme.typography.bodyMedium)
                            Text(s.exDividendDate, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("権利確定日 (受渡日):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(s.settlementDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Text("主な銘柄: ${s.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                        Spacer(modifier = Modifier.height(4.dp))
                        FilledTonalButton(
                            onClick = { onNavigateToMonth(s.monthNumber) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(
                                text = "${s.monthNumber}月の優待銘柄を見る",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
