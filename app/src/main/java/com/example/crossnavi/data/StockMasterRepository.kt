package com.example.crossnavi.data

import android.content.Context
import com.example.crossnavi.model.BrokerInventory
import com.example.crossnavi.model.InventoryStatus
import com.example.crossnavi.model.StockPreset
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs

enum class BrokerFilterType(val label: String) {
    ALL("すべて"),
    SBI_SHORT("SBI短期あり"),
    RAKUTEN_SHORT("楽天短期あり")
}

enum class InventoryFilterType(val label: String) {
    ALL("在庫問わず"),
    ANY_IN_STOCK("◎ 在庫あり(いずれか)"),
    SBI_IN_STOCK("SBI在庫あり"),
    RAKUTEN_IN_STOCK("楽天在庫あり")
}

enum class LongTermFilterType(val label: String) {
    ALL("長期問わず"),
    SHORT_TERM_OK("短期OK (長期不要)"),
    REQUIRES_LONG_TERM("長期条件あり")
}

enum class InvestmentFilterType(val label: String) {
    UNDER_100K("〜10万円"),
    RANGE_100K_300K("10〜30万円"),
    RANGE_300K_500K("30〜50万円"),
    RANGE_500K_1M("50〜100万円"),
    OVER_1M("100万円超");

    fun matches(amount: Double): Boolean {
        return when (this) {
            UNDER_100K -> amount <= 100_000.0
            RANGE_100K_300K -> amount > 100_000.0 && amount <= 300_000.0
            RANGE_300K_500K -> amount > 300_000.0 && amount <= 500_000.0
            RANGE_500K_1M -> amount > 500_000.0 && amount <= 1_000_000.0
            OVER_1M -> amount > 1_000_000.0
        }
    }
}

enum class StockSortOrder(val label: String) {
    CODE_ASC("コード順"),
    YIELD_DESC("利回り順"),
    GIFT_VALUE_DESC("優待額順"),
    INVENTORY_DESC("在庫数順")
}

class StockMasterRepository(private val context: Context) {

    private var cachedStocks: List<StockPreset>? = null

    /**
     * assets/stocks_master.json を非同期・遅延ロードしてキャッシュ
     */
    fun getAllStocks(): List<StockPreset> {
        cachedStocks?.let { return it }

        val list = mutableListOf<StockPreset>()
        try {
            val inputStream = context.assets.open("stocks_master.json")
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val jsonString = reader.use { it.readText() }
            val cleanJson = jsonString.replace("\uFEFF", "").trim()
            val jsonArray = try {
                JSONArray(cleanJson)
            } catch (_: Exception) {
                val rootObj = JSONObject(cleanJson)
                if (rootObj.has("value")) rootObj.getJSONArray("value")
                else if (rootObj.has("stocks")) rootObj.getJSONArray("stocks")
                else JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val mArr = obj.getJSONArray("months")
                val months = mutableListOf<Int>()
                for (m in 0 until mArr.length()) {
                    months.add(mArr.getInt(m))
                }

                val code = obj.getString("code")
                val isSbi = obj.optBoolean("isSbiShort", true)
                val isRakuten = obj.optBoolean("isRakutenShort", true)
                val giftDesc = obj.getString("giftDescription")

                // 在庫情報のパース、またはリアルな在庫シミュレーション
                val (sbiInv, rakutenInv) = generateOrParseInventory(code, isSbi, isRakuten, obj)

                // 長期保有条件の判定 (JSON明記または有名銘柄辞書・文言判定)
                val (defaultRequiresLongTerm, defaultLongTermDesc) = checkLongTermCondition(code, giftDesc)
                val requiresLongTerm = if (obj.has("requiresLongTerm")) obj.getBoolean("requiresLongTerm") else defaultRequiresLongTerm
                val longTermDescription = if (obj.has("longTermDescription")) obj.getString("longTermDescription") else defaultLongTermDesc

                list.add(
                    StockPreset(
                        tickerCode = code,
                        stockName = obj.getString("name"),
                        months = months,
                        defaultPrice = obj.getDouble("price"),
                        defaultQuantity = obj.optInt("quantity", 100),
                        giftValue = obj.getInt("giftValue"),
                        giftDescription = giftDesc,
                        dividendPerShare = obj.optDouble("dividendPerShare", 0.0),
                        isSbiShort = isSbi,
                        isRakutenShort = isRakuten,
                        sector = obj.optString("sector", ""),
                        sbiInventory = sbiInv,
                        rakutenInventory = rakutenInv,
                        requiresLongTerm = requiresLongTerm,
                        longTermDescription = longTermDescription
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // フォールバック: 基本銘柄
            list.addAll(StockPresetData.PRESET_STOCKS)
        }

        cachedStocks = list
        loadCacheIfExists()
        return cachedStocks ?: list
    }

    private fun loadCacheIfExists() {
        try {
            val cacheFile = java.io.File(context.filesDir, "inventory_cache.json")
            if (cacheFile.exists()) {
                val jsonString = cacheFile.readText()
                applyInventoryJson(jsonString)
            } else {
                // 初期同梱の最新在庫JSONを読み込み
                val assetStream = context.assets.open("inventory_latest.json")
                val jsonString = assetStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                applyInventoryJson(jsonString)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val PREFS_NAME = "crossnavi_sync_prefs"
        private const val KEY_SYNC_URL = "sync_url"
        private const val KEY_LAST_UPDATED = "last_updated_time"
        private const val KEY_SYNC_SOURCE = "sync_source"
        private const val KEY_SYNC_STATUS = "sync_status"

        // デフォルトのクラウド配信URL (GitHub Raw / クラウドCDN)
        const val DEFAULT_CLOUD_URL = "https://raw.githubusercontent.com/girisuke-a11y/CrossNavi/main/app/src/main/assets/inventory_latest.json"
        // ローカルPCテスト用URL (Androidエミュレータ用)
        const val LOCAL_TEST_URL = "http://10.0.2.2:8080/inventory_latest.json"
        // アプリ内蔵データ用プリセット
        const val INTERNAL_PRESET_URL = "internal://assets/inventory_latest.json"

        const val DEFAULT_SYNC_URL = DEFAULT_CLOUD_URL
    }

    var lastSyncWasFallback: Boolean = false
        private set

    /**
     * アプリ内蔵の最新在庫JSON（assets/inventory_latest.json）を現在日時で再読み込み
     */
    fun reloadBundledInventoryWithCurrentTime(): Int {
        val assetStream = context.assets.open("inventory_latest.json")
        val jsonString = assetStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = org.json.JSONObject(jsonString)

        val now = java.time.LocalDateTime.now()
        val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm")
        val formattedTime = now.format(timeFormatter)
        root.put("updatedAt", "$formattedTime (内蔵最新)")

        val count = applyInventoryJson(root.toString())
        lastSyncWasFallback = true
        lastSyncTimeMillis = System.currentTimeMillis()
        return count
    }

    private val prefs by lazy {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
    }

    private var memoryLastUpdatedTime: String = "19:00"

    var lastUpdatedTime: String
        get() = prefs?.getString(KEY_LAST_UPDATED, memoryLastUpdatedTime) ?: memoryLastUpdatedTime
        private set(value) {
            memoryLastUpdatedTime = value
            try {
                prefs?.edit()?.putString(KEY_LAST_UPDATED, value)?.apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    var lastSyncSource: String
        get() = prefs?.getString(KEY_SYNC_SOURCE, "") ?: ""
        private set(value) {
            try {
                prefs?.edit()?.putString(KEY_SYNC_SOURCE, value)?.apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    var lastSyncStatus: String
        get() = prefs?.getString(KEY_SYNC_STATUS, "success") ?: "success"
        private set(value) {
            try {
                prefs?.edit()?.putString(KEY_SYNC_STATUS, value)?.apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    fun getSyncUrl(): String {
        return prefs?.getString(KEY_SYNC_URL, DEFAULT_SYNC_URL) ?: DEFAULT_SYNC_URL
    }

    fun setSyncUrl(url: String) {
        val trimmed = url.trim()
        val target = if (trimmed.isBlank()) DEFAULT_SYNC_URL else trimmed
        try {
            prefs?.edit()?.putString(KEY_SYNC_URL, target)?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetSyncUrl() {
        try {
            prefs?.edit()?.remove(KEY_SYNC_URL)?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 人気銘柄の品薄や各社の在庫状況をリアルに再現
     */
    private fun generateOrParseInventory(
        code: String,
        isSbi: Boolean,
        isRakuten: Boolean,
        obj: org.json.JSONObject
    ): Pair<BrokerInventory, BrokerInventory> {
        // 超人気優待・争奪戦銘柄の実態（9月権利直前の現実を忠実に反映）
        when (code) {
            "7421" -> return Pair( // カッパ・クリエイト (かっぱ寿司)
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00"),
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
            )
            "7412" -> return Pair( // アトム
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00"),
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
            )
            "7616" -> return Pair( // コロワイド
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00"),
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
            )
            "7550" -> return Pair( // ゼンショーHD
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00"),
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
            )
            "3397" -> return Pair( // トリドールHD (丸亀製麺)
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00"),
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
            )
            "7581" -> return Pair( // サイゼリヤ
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00"),
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
            )
            "3048" -> return Pair( // ビックカメラ
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00"),
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
            )
            "2702" -> return Pair( // マクドナルド
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00"),
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
            )
            "3197" -> return Pair( // すかいらーく
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00"),
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
            )
            "9861" -> return Pair( // 吉野家
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00"),
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
            )
            "9202" -> return Pair( // ANA (大型株で枠多め)
                BrokerInventory(InventoryStatus.IN_STOCK, 145000, "19:00"),
                BrokerInventory(InventoryStatus.LOW_STOCK, 8400, "19:00")
            )
            "9201" -> return Pair( // JAL
                BrokerInventory(InventoryStatus.IN_STOCK, 98000, "19:00"),
                BrokerInventory(InventoryStatus.IN_STOCK, 45000, "19:00")
            )
            "8267" -> return Pair( // イオン
                BrokerInventory(InventoryStatus.IN_STOCK, 82000, "19:00"),
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
            )
            "9831" -> return Pair( // ヤマダHD
                BrokerInventory(InventoryStatus.LOW_STOCK, 6000, "19:00"),
                BrokerInventory(InventoryStatus.OUT_OF_STOCK, 0, "19:00")
            )
        }

        // 一般銘柄: 権利落ち直前の厳しい争奪戦を模した現実的な分布
        val hash = abs(code.hashCode())
        val sbiShares = if (!isSbi) 0 else when (hash % 10) {
            in 0..6 -> 0 // 70% 在庫なし
            in 7..8 -> (hash % 6 + 1) * 1000 // 20% 残りわずか
            else -> (hash % 10 + 2) * 10000 // 10% 在庫あり (大型株等)
        }
        val sbiStatus = when {
            sbiShares <= 0 -> InventoryStatus.OUT_OF_STOCK
            sbiShares <= 9000 -> InventoryStatus.LOW_STOCK
            else -> InventoryStatus.IN_STOCK
        }

        val rakShares = if (!isRakuten) 0 else when ((hash / 3) % 10) {
            in 0..7 -> 0 // 80% 在庫なし
            8 -> ((hash / 2) % 5 + 1) * 1000 // 10% 残りわずか
            else -> ((hash / 3) % 8 + 2) * 10000 // 10% 在庫あり
        }
        val rakStatus = when {
            rakShares <= 0 -> InventoryStatus.OUT_OF_STOCK
            rakShares <= 9000 -> InventoryStatus.LOW_STOCK
            else -> InventoryStatus.IN_STOCK
        }

        return Pair(
            BrokerInventory(sbiStatus, sbiShares, "19:00"),
            BrokerInventory(rakStatus, rakShares, "19:00")
        )
    }

    var lastSyncTimeMillis: Long = 0L
        private set

    /**
     * オンラインまたはローカルサーバーから最新在庫JSONを取得して同期
     * @param syncUrl 取得先URL (nullの場合は保存済みの設定URLまたはデフォルトURL)
     * @param force 強制的に同期するかどうか。falseの場合は前回取得から5分以内の場合はスキップ
     */
    fun syncOnlineInventory(syncUrl: String? = null, force: Boolean = true): Result<Int> {
        if (!force) {
            val now = System.currentTimeMillis()
            if (now - lastSyncTimeMillis < 5 * 60 * 1000) {
                // 5分以内ならスキップ（現状のキャッシュを維持）
                return Result.success(0)
            }
        }
        val urlString = syncUrl?.takeIf { it.isNotBlank() } ?: getSyncUrl()
        val isDefaultSampleUrl = urlString.contains("mayon-dev")
        val isInternalPreset = urlString == INTERNAL_PRESET_URL || urlString.startsWith("internal://") || urlString.isBlank()

        // 1. アプリ内蔵データモードが指定された場合は通信せず即座に内蔵JSONを再読込
        if (isInternalPreset) {
            return try {
                val count = reloadBundledInventoryWithCurrentTime()
                Result.success(count)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        // 2. 外部URL通信
        return try {
            val url = java.net.URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "CrossNavi-Android/1.0")

            val responseCode = connection.responseCode
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val updatedCount = applyInventoryJson(jsonString)

                // ローカルストレージにキャッシュ保存
                try {
                    val cacheFile = java.io.File(context.filesDir, "inventory_cache.json")
                    cacheFile.writeText(jsonString)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                lastSyncWasFallback = false
                lastSyncTimeMillis = System.currentTimeMillis()
                Result.success(updatedCount)
            } else if (responseCode == java.net.HttpURLConnection.HTTP_NOT_FOUND && isDefaultSampleUrl) {
                // サンプルURLの404エラー時は、ユーザー体験を損なわないよう内蔵データで安全にフォールバック
                val count = reloadBundledInventoryWithCurrentTime()
                Result.success(count)
            } else {
                val errorMsg = if (responseCode == java.net.HttpURLConnection.HTTP_NOT_FOUND) {
                    "HTTP 404: 指定のURLに在庫ファイルが見つかりません。歯車アイコンからURLをご確認ください"
                } else {
                    "HTTP $responseCode: サーバーから応答がありませんでした"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            if (isDefaultSampleUrl) {
                try {
                    val count = reloadBundledInventoryWithCurrentTime()
                    return Result.success(count)
                } catch (fallbackEx: Exception) {
                    // ignore
                }
            }
            Result.failure(e)
        }
    }

    /**
     * 在庫JSONテキストを現在の銘柄マスターに反映
     */
    fun applyInventoryJson(jsonString: String): Int {
        val clean = jsonString.replace("\uFEFF", "").trim()
        val root = org.json.JSONObject(clean)
        val time = root.optString("updatedAt", "19:00")
        val source = root.optString("source", "")
        val status = root.optString("status", "success")
        
        lastUpdatedTime = time
        lastSyncSource = source
        lastSyncStatus = status

        val stocksObj = root.optJSONObject("stocks") ?: return 0
        val all = getAllStocks()

        val updatedList = all.map { stock ->
            val inv = stocksObj.optJSONObject(stock.tickerCode)
            if (inv != null) {
                val sbiShares = inv.optInt("sbi", if (inv.has("sbiShares")) inv.getInt("sbiShares") else 0)
                val rakShares = inv.optInt("rakuten", if (inv.has("rakutenShares")) inv.getInt("rakutenShares") else 0)

                val sbiStatus = when {
                    sbiShares <= 0 -> InventoryStatus.OUT_OF_STOCK
                    sbiShares <= 9000 -> InventoryStatus.LOW_STOCK
                    else -> InventoryStatus.IN_STOCK
                }
                val rakStatus = when {
                    rakShares <= 0 -> InventoryStatus.OUT_OF_STOCK
                    rakShares <= 9000 -> InventoryStatus.LOW_STOCK
                    else -> InventoryStatus.IN_STOCK
                }

                stock.copy(
                    sbiInventory = BrokerInventory(sbiStatus, sbiShares, time),
                    rakutenInventory = BrokerInventory(rakStatus, rakShares, time)
                )
            } else {
                stock
            }
        }

        cachedStocks = updatedList
        return stocksObj.length()
    }

    /**
     * 月・証券会社・在庫フィルター・長期保有条件・検索キーワード・並び順による多軸検索＆ソート
     */
    fun search(
        month: Int?,
        brokerFilter: BrokerFilterType = BrokerFilterType.ALL,
        inventoryFilter: InventoryFilterType = InventoryFilterType.ALL,
        longTermFilter: LongTermFilterType = LongTermFilterType.ALL,
        investmentFilters: Set<InvestmentFilterType> = emptySet(),
        query: String = "",
        sortOrder: StockSortOrder = StockSortOrder.CODE_ASC
    ): List<StockPreset> {
        val all = getAllStocks()

        val filtered = all.filter { stock ->
            // 1. 月フィルター
            val monthMatch = if (month != null && month in 1..12) {
                stock.months.contains(month)
            } else {
                true
            }

            // 2. 証券会社一般信用短期取扱フィルター
            val brokerMatch = when (brokerFilter) {
                BrokerFilterType.ALL -> true
                BrokerFilterType.SBI_SHORT -> stock.isSbiShort
                BrokerFilterType.RAKUTEN_SHORT -> stock.isRakutenShort
            }

            // 3. 在庫フィルター (重要!)
            val inventoryMatch = when (inventoryFilter) {
                InventoryFilterType.ALL -> true
                InventoryFilterType.ANY_IN_STOCK -> {
                    stock.sbiInventory.status != InventoryStatus.OUT_OF_STOCK ||
                            stock.rakutenInventory.status != InventoryStatus.OUT_OF_STOCK
                }
                InventoryFilterType.SBI_IN_STOCK -> {
                    stock.sbiInventory.status != InventoryStatus.OUT_OF_STOCK
                }
                InventoryFilterType.RAKUTEN_IN_STOCK -> {
                    stock.rakutenInventory.status != InventoryStatus.OUT_OF_STOCK
                }
            }

            // 4. 長期保有条件フィルター
            val longTermMatch = when (longTermFilter) {
                LongTermFilterType.ALL -> true
                LongTermFilterType.SHORT_TERM_OK -> !stock.requiresLongTerm
                LongTermFilterType.REQUIRES_LONG_TERM -> stock.requiresLongTerm
            }

            // 5. 取得金額フィルター (株価 × 優待必要株数・重複なし複数選択)
            val requiredAmount = stock.defaultPrice * stock.defaultQuantity
            val investmentMatch = if (investmentFilters.isEmpty()) {
                true
            } else {
                investmentFilters.any { it.matches(requiredAmount) }
            }

            // 6. キーワード検索
            val queryMatch = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase()
                stock.tickerCode.contains(q, ignoreCase = true) ||
                        stock.stockName.contains(q, ignoreCase = true) ||
                        stock.giftDescription.contains(q, ignoreCase = true) ||
                        stock.sector.contains(q, ignoreCase = true)
            }

            monthMatch && brokerMatch && inventoryMatch && longTermMatch && investmentMatch && queryMatch
        }

        return when (sortOrder) {
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
    }

    /**
     * 銘柄コードおよび優待説明から長期保有条件を判定
     */
    fun checkLongTermCondition(code: String, giftDesc: String): Pair<Boolean, String> {
        // 1. クロス取引で重要な長期保有必須・優遇の有名銘柄マスター
        val knownLongTerm = mapOf(
            "2702" to "1年以上継続保有が必須 (同一株主番号で3回以上記載)",
            "9433" to "1年以上継続保有が必須 (Ponta等カタログ)",
            "9434" to "1年以上継続保有が必須 (PayPayポイント 1,000pt)",
            "9432" to "2年以上継続保有などの条件あり (dポイント付与)",
            "8697" to "1年以上継続保有が必須 (1年未満は優待対象外)",
            "2914" to "1年以上継続保有が必須 (12月末に100株以上を1年以上)",
            "2503" to "1年以上継続保有が必須 (100株以上を1年以上保有)",
            "4912" to "1年以上継続保有が必須 (12月末に100株以上を1年以上)",
            "3539" to "1年以上継続保有が必須 (7月末に100株以上を1年以上)",
            "6458" to "1年以上継続保有が必須 (カタログギフトは1年以上保有)",
            "2590" to "6ヶ月以上継続保有が必須 (1月20日基準日)",
            "7513" to "長期保有で追加優待券 (1年以上で+1,000円、2年以上で+2,000円)",
            "3048" to "長期保有で追加優待券 (1年以上で+1,000円、2年以上で+2,000円)",
            "9831" to "長期保有で買物優待券追加進呈あり",
            "1419" to "3年以上継続保有でQUOカードが500円→1,000円に倍増",
            "3543" to "3年以上継続保有でKOMECAチャージ+1,000円追加",
            "8252" to "長期保有優遇制度あり",
            "3397" to "1年以上継続保有(200株以上)で食事割引券3,000円分追加",
            "7616" to "3年以上継続保有(500株以上)で優待ポイント追加",
            "7272" to "3年以上継続保有でポイント優遇あり",
            "2695" to "長期保有優遇制度あり (くら寿司)"
        )

        knownLongTerm[code]?.let { return Pair(true, it) }

        // 2. 優待説明文からの判定ヒューリスティック
        val desc = giftDesc.lowercase()
        if (desc.contains("以上保有") || desc.contains("継続保有") ||
            desc.contains("年以上") || desc.contains("長期") ||
            desc.contains("か月以上") || desc.contains("ヶ月以上")
        ) {
            return Pair(true, "長期継続保有による条件または優遇あり")
        }

        return Pair(false, "")
    }
}
