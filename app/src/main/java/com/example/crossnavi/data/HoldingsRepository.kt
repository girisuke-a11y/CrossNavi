package com.example.crossnavi.data

import android.content.Context
import com.example.crossnavi.model.Broker
import com.example.crossnavi.model.CreditType
import com.example.crossnavi.model.CrossHolding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class HoldingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("cross_holdings_prefs", Context.MODE_PRIVATE)
    private val _holdings = MutableStateFlow<List<CrossHolding>>(emptyList())
    val holdings: StateFlow<List<CrossHolding>> = _holdings.asStateFlow()

    init {
        loadHoldings()
    }

    private fun loadHoldings() {
        val jsonString = prefs.getString("holdings_json", "[]") ?: "[]"
        val list = mutableListOf<CrossHolding>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CrossHolding(
                        id = obj.getString("id"),
                        tickerCode = obj.getString("tickerCode"),
                        stockName = obj.getString("stockName"),
                        stockPrice = obj.getDouble("stockPrice"),
                        quantity = obj.getInt("quantity"),
                        broker = Broker.valueOf(obj.getString("broker")),
                        creditType = CreditType.valueOf(obj.getString("creditType")),
                        annualRate = obj.getDouble("annualRate"),
                        openDate = LocalDate.parse(obj.getString("openDate")),
                        deliveryDate = LocalDate.parse(obj.getString("deliveryDate")),
                        holdingDays = obj.getInt("holdingDays"),
                        giftValue = obj.getInt("giftValue"),
                        dividendPerShare = obj.optDouble("dividendPerShare", 0.0),
                        feeBuy = obj.optInt("feeBuy", 0),
                        feeSell = obj.optInt("feeSell", 0),
                        isDelivered = obj.optBoolean("isDelivered", false),
                        memo = obj.optString("memo", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _holdings.value = list
    }

    private fun saveHoldings(list: List<CrossHolding>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("tickerCode", item.tickerCode)
                put("stockName", item.stockName)
                put("stockPrice", item.stockPrice)
                put("quantity", item.quantity)
                put("broker", item.broker.name)
                put("creditType", item.creditType.name)
                put("annualRate", item.annualRate)
                put("openDate", item.openDate.toString())
                put("deliveryDate", item.deliveryDate.toString())
                put("holdingDays", item.holdingDays)
                put("giftValue", item.giftValue)
                put("dividendPerShare", item.dividendPerShare)
                put("feeBuy", item.feeBuy)
                put("feeSell", item.feeSell)
                put("isDelivered", item.isDelivered)
                put("memo", item.memo)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("holdings_json", jsonArray.toString()).apply()
        _holdings.value = list
    }

    fun addHolding(holding: CrossHolding) {
        val current = _holdings.value.toMutableList()
        current.add(0, holding) // 最新を先頭に
        saveHoldings(current)
    }

    fun toggleDeliveryStatus(id: String) {
        val current = _holdings.value.map {
            if (it.id == id) it.copy(isDelivered = !it.isDelivered) else it
        }
        saveHoldings(current)
    }

    fun deleteHolding(id: String) {
        val current = _holdings.value.filter { it.id != id }
        saveHoldings(current)
    }
}
