package com.example.crossnavi.data

import com.example.crossnavi.model.StockPreset

object StockPresetData {

    val PRESET_STOCKS = listOf(
        // --- 1月 ---
        StockPreset("1928", "積水ハウス", listOf(1), 3500.0, 1000, 4000, "魚沼産コシヒカリ 新米5kg", 125.0),
        StockPreset("3193", "エターナルホスピタリティ(鳥貴族)", listOf(1, 7), 3800.0, 100, 1000, "自社店舗お食事券 1,000円分", 12.0),
        StockPreset("2590", "ダイドーグループHD", listOf(1), 2800.0, 100, 6000, "自社グループ飲料・ゼリー詰合せ 6,000円相当", 60.0, requiresLongTerm = true, longTermDescription = "6ヶ月以上継続保有が必須"),

        // --- 2月 ---
        StockPreset("8267", "イオン", listOf(2, 8), 3800.0, 100, 4000, "オーナーズカード (お買物3%キャッシュバック)", 40.0),
        StockPreset("9861", "吉野家ホールディングス", listOf(2, 8), 3100.0, 100, 2000, "飲食ご優待券 2,000円分 (年間4,000円)", 20.0),
        StockPreset("3387", "クリエイト・レストランツHD", listOf(2, 8), 1100.0, 100, 2000, "ご優待食事券 2,000円分 (年間4,000円)", 8.0),
        StockPreset("3048", "ビックカメラ", listOf(2, 8), 1600.0, 100, 2000, "株主様お買物優待券 2,000円分", 24.0, requiresLongTerm = true, longTermDescription = "1年以上・2年以上で追加優待"),
        StockPreset("3543", "コメダホールディングス", listOf(2, 8), 2700.0, 100, 1000, "自社店舗用電子マネー 1,000円分", 54.0, requiresLongTerm = true, longTermDescription = "3年以上で追加電子マネー"),
        StockPreset("7545", "西松屋チェーン", listOf(2, 8), 2200.0, 100, 1000, "株主ご優待カード 1,000円分", 27.0),

        // --- 3月 ---
        StockPreset("9202", "ANAホールディングス", listOf(3, 9), 3000.0, 100, 3500, "国内線50%割引優待番号 1枚", 50.0),
        StockPreset("9201", "日本航空 (JAL)", listOf(3, 9), 2600.0, 100, 3000, "国内線50%割引優待券 1枚", 75.0),
        StockPreset("9433", "KDDI", listOf(3), 4800.0, 100, 3000, "Pontaポイント等から選べるギフト 3,000円相当", 145.0, requiresLongTerm = true, longTermDescription = "1年以上継続保有が必須"),
        StockPreset("9831", "ヤマダホールディングス", listOf(3, 9), 450.0, 100, 1500, "株主様お買物優待券 1,500円分", 13.0, requiresLongTerm = true, longTermDescription = "1年以上で追加優待"),
        StockPreset("7616", "コロワイド", listOf(3, 9), 2000.0, 500, 20000, "株主様ご優待ポイント 20,000円相当", 5.0, requiresLongTerm = true, longTermDescription = "3年以上継続保有で優待ポイント追加"),
        StockPreset("8252", "丸井グループ", listOf(3, 9), 2400.0, 100, 2000, "お買物券・Webクーポン 2,000円相当", 101.0, requiresLongTerm = true, longTermDescription = "長期保有優遇制度あり"),
        StockPreset("4661", "オリエンタルランド", listOf(3, 9), 3800.0, 100, 8400, "東京ディズニーリゾート 1デーパスポート 1枚", 13.0),

        // --- 4月 ---
        StockPreset("2695", "くら寿司", listOf(4), 4200.0, 100, 2500, "お食事ご優待電子チケット 2,500円分", 20.0),
        StockPreset("2751", "テンポスホールディングス", listOf(4), 3200.0, 100, 8000, "「あさくま」等で使える食事券 8,000円分", 11.0),

        // --- 5月 ---
        StockPreset("2792", "ハニーズホールディングス", listOf(5), 1700.0, 100, 3000, "株主ご優待券 3,000円分", 55.0),
        StockPreset("1419", "タマホーム", listOf(5, 11), 3700.0, 100, 500, "特製クオカード 500円分 (3年以上で1,000円)", 185.0, requiresLongTerm = true, longTermDescription = "3年以上でQUOカード倍増"),

        // --- 6月 ---
        StockPreset("2702", "日本マクドナルドHD", listOf(6, 12), 6500.0, 100, 5000, "株主ご優待食事券 1冊 (バーガー・サイド・ドリンク引換券×6)", 42.0, requiresLongTerm = true, longTermDescription = "1年以上継続保有が必須"),
        StockPreset("3197", "すかいらーくHD", listOf(6, 12), 2200.0, 100, 2000, "株主様ご優待カード 2,000円分 (年間4,000円)", 17.0),
        StockPreset("2914", "JT (日本たばこ産業)", listOf(6, 12), 4200.0, 100, 2500, "自社グループ関連商品 2,500円相当", 194.0, requiresLongTerm = true, longTermDescription = "1年以上継続保有が必須"),
        StockPreset("3097", "物語コーポレーション", listOf(6, 12), 3600.0, 100, 3500, "焼肉きんぐ等で使えるお食事券 3,500円分", 35.0),

        // --- 7月 ---
        StockPreset("3539", "JMホールディングス", listOf(7), 2800.0, 100, 2500, "精肉関連商品（鶏肉・豚肉等）2,500円相当", 40.0, requiresLongTerm = true, longTermDescription = "1年以上継続保有が必須"),

        // --- 8月 ---
        StockPreset("7513", "コジマ", listOf(8), 950.0, 100, 1000, "株主様お買物優待券 1,000円分", 14.0, requiresLongTerm = true, longTermDescription = "1年以上・2年以上で追加優待"),

        // --- 9月 ---
        StockPreset("7550", "ゼンショーホールディングス", listOf(3, 9), 7800.0, 100, 1000, "すき家・はま寿司等お食事ご優待券 1,000円分", 50.0),
        StockPreset("3397", "トリドールホールディングス", listOf(3, 9), 3900.0, 100, 3000, "丸亀製麺等で使えるお食事割引券 3,000円分", 15.0, requiresLongTerm = true, longTermDescription = "1年以上継続保有で追加進呈"),
        StockPreset("6458", "新晃工業", listOf(9), 4200.0, 100, 3000, "選べるカタログギフト 3,000円相当", 120.0, requiresLongTerm = true, longTermDescription = "1年以上継続保有が必須"),

        // --- 10月 ---
        StockPreset("3038", "神戸物産", listOf(10), 3800.0, 100, 1000, "業務スーパー商品券 1,000円分", 22.0),
        StockPreset("9603", "エイチ・アイ・エス (HIS)", listOf(10), 1800.0, 100, 2000, "HIS旅行商品割引券 2,000円分", 10.0),

        // --- 11月 ---
        StockPreset("2769", "ヴィレッジヴァンガード", listOf(11), 1050.0, 100, 10000, "株主ご優待お買物券 10,000円分 (2,000円毎に1,000円利用可)", 0.0),
        StockPreset("2678", "アスクル", listOf(5, 11), 2100.0, 100, 2000, "LOHACOで使えるクーポン 2,000円分", 38.0),

        // --- 12月 ---
        StockPreset("2503", "キリンホールディングス", listOf(12), 2100.0, 100, 1000, "キリンビール詰め合わせ等 1,000円相当", 71.0, requiresLongTerm = true, longTermDescription = "1年以上継続保有が必須"),
        StockPreset("4912", "ライオン", listOf(12), 1400.0, 100, 2500, "自社製品セット（ハミガキ・洗剤等）2,500円相当", 27.0, requiresLongTerm = true, longTermDescription = "1年以上継続保有が必須")
    )

    fun getStocksByMonth(month: Int): List<StockPreset> {
        return PRESET_STOCKS.filter { it.months.contains(month) }
    }

    fun searchStocks(month: Int?, query: String): List<StockPreset> {
        val baseList = if (month != null && month in 1..12) {
            getStocksByMonth(month)
        } else {
            PRESET_STOCKS
        }

        if (query.isBlank()) {
            return baseList
        }

        val q = query.trim().lowercase()
        return baseList.filter {
            it.tickerCode.contains(q, ignoreCase = true) ||
                    it.stockName.contains(q, ignoreCase = true) ||
                    it.giftDescription.contains(q, ignoreCase = true)
        }
    }
}
