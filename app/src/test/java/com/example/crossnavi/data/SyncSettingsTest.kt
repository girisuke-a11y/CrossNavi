package com.example.crossnavi.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSettingsTest {

    @Test
    fun testDefaultUrls() {
        // デフォルトクラウドURLがセキュアなHTTPS形式であること
        assertTrue(StockMasterRepository.DEFAULT_CLOUD_URL.startsWith("https://"))
        assertTrue(StockMasterRepository.DEFAULT_CLOUD_URL.endsWith(".json"))

        // DEFAULT_SYNC_URLがクラウドURLに設定されていること
        assertEquals(StockMasterRepository.DEFAULT_CLOUD_URL, StockMasterRepository.DEFAULT_SYNC_URL)

        // エミュレータ用ローカルURLの検証
        assertEquals("http://10.0.2.2:8080/inventory_latest.json", StockMasterRepository.LOCAL_TEST_URL)
    }
}
