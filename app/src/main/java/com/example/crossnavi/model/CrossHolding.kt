package com.example.crossnavi.model

import java.time.LocalDate
import java.util.UUID

data class CrossHolding(
    val id: String = UUID.randomUUID().toString(),
    val tickerCode: String,            // 銘柄コード (例: 9202)
    val stockName: String,             // 銘柄名 (例: ANAホールディングス)
    val stockPrice: Double,            // 株価 (円)
    val quantity: Int,                 // 株数 (株)
    val broker: Broker,                // 証券会社 (SBI / 楽天)
    val creditType: CreditType,        // 信用区分
    val annualRate: Double,            // 適用年利 (%)
    val openDate: LocalDate,           // 取得日 (約定日)
    val deliveryDate: LocalDate,       // 現渡予定日 (約定日)
    val holdingDays: Int,              // 受渡日ベースの借株日数
    val giftValue: Int,                // 優待換算価値 (円)
    val dividendPerShare: Double = 0.0,// 1株当たり配当金 (円)
    val feeBuy: Int = 0,               // 買付手数料 (円, デフォルト0)
    val feeSell: Int = 0,              // 売却手数料 (円, デフォルト0)
    val isDelivered: Boolean = false,  // 現渡完了フラグ
    val memo: String = ""              // メモ
)
