package com.example.crossnavi.model

enum class Broker(val displayName: String, val creditTypes: List<CreditType>) {
    SBI(
        displayName = "SBI証券",
        creditTypes = listOf(
            CreditType.SBI_SHORT,
            CreditType.SBI_GENERAL_15DAYS,
            CreditType.SBI_STANDARD
        )
    ),
    RAKUTEN(
        displayName = "楽天証券",
        creditTypes = listOf(
            CreditType.RAKUTEN_SHORT_14DAYS,
            CreditType.RAKUTEN_PERPETUAL,
            CreditType.RAKUTEN_STANDARD
        )
    )
}

enum class CreditType(
    val displayName: String,
    val defaultAnnualRate: Double,
    val isStandard: Boolean = false // 制度信用かどうか（逆日歩リスクの注記用）
) {
    // SBI証券
    SBI_SHORT("一般信用(短期)", 3.90),
    SBI_GENERAL_15DAYS("一般信用(15営業日)", 3.90),
    SBI_STANDARD("制度信用(6ヶ月)", 1.15, isStandard = true),

    // 楽天証券
    RAKUTEN_SHORT_14DAYS("一般信用(短期 14日)", 3.90),
    RAKUTEN_PERPETUAL("一般信用(無期限)", 2.00),
    RAKUTEN_STANDARD("制度信用(6ヶ月)", 1.10, isStandard = true)
}
