package com.example.crossnavi.model

/**
 * 一般信用短期の在庫ステータス
 */
enum class InventoryStatus(val displayName: String, val shortBadge: String) {
    IN_STOCK("在庫あり", "◎ 在庫あり"),
    LOW_STOCK("残りわずか", "△ わずか"),
    OUT_OF_STOCK("在庫なし", "✕ なし")
}

/**
 * 証券会社ごとの一般信用在庫情報
 */
data class BrokerInventory(
    val status: InventoryStatus,
    val availableShares: Int,        // 残株数 (例: 154000)
    val updatedAt: String = "19:00"  // 更新時刻
) {
    val formattedShares: String
        get() = when {
            status == InventoryStatus.OUT_OF_STOCK || availableShares <= 0 -> "0株"
            availableShares >= 10000 -> "${String.format("%.1f", availableShares / 10000.0)}万株"
            else -> "${availableShares}株"
        }
}

/**
 * 株主優待銘柄のプリセット情報モデル
 */
data class StockPreset(
    val tickerCode: String,            // 銘柄コード (例: "9202")
    val stockName: String,             // 銘柄名 (例: "ANAホールディングス")
    val months: List<Int>,             // 権利確定月 (例: listOf(3, 9))
    val defaultPrice: Double,          // 目安株価 (円)
    val defaultQuantity: Int = 100,    // 目安優待獲得株数 (株)
    val giftValue: Int,                // 優待換算価値 (円)
    val giftDescription: String,       // 優待内容テキスト (例: "国内線50%割引優待番号 1枚")
    val dividendPerShare: Double = 0.0,// 1株当たり配当予想 (円)
    val isSbiShort: Boolean = true,    // SBI一般信用(短期)取扱フラグ
    val isRakutenShort: Boolean = true,// 楽天一般信用(短期)取扱フラグ
    val sector: String = "",           // 業種
    val sbiInventory: BrokerInventory = BrokerInventory(InventoryStatus.IN_STOCK, 120000),
    val rakutenInventory: BrokerInventory = BrokerInventory(InventoryStatus.IN_STOCK, 85000),
    val requiresLongTerm: Boolean = false, // 長期保有条件ありフラグ (1年以上保有が必須等)
    val longTermDescription: String = ""   // 長期条件補足テキスト (例: "1年以上保有で進呈")
) {
    val estimatedYield: Double
        get() {
            val totalInvestment = defaultPrice * defaultQuantity
            return if (totalInvestment > 0.0) (giftValue.toDouble() / totalInvestment) * 100.0 else 0.0
        }

    val requiredInvestment: Double
        get() = defaultPrice * defaultQuantity

    val formattedInvestment: String
        get() {
            val amount = defaultPrice * defaultQuantity
            return when {
                amount >= 10_000 -> "${String.format(java.util.Locale.US, "%.1f", amount / 10000.0)}万円"
                else -> "${amount.toInt()}円"
            }
        }

    val totalInventoryShares: Int
        get() = sbiInventory.availableShares + rakutenInventory.availableShares
}
