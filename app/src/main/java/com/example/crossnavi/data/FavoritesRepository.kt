package com.example.crossnavi.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * お気に入り（ウォッチリスト）銘柄コードの永続化リポジトリ
 */
class FavoritesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("crossnavi_favorites_pref", Context.MODE_PRIVATE)

    private val _favoriteTickers = MutableStateFlow<Set<String>>(loadFavorites())
    val favoriteTickers: StateFlow<Set<String>> = _favoriteTickers.asStateFlow()

    private fun loadFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    /**
     * お気に入りの登録／解除をトグル
     * @return 変更後の登録状態 (true: 登録中, false: 解除済)
     */
    fun toggleFavorite(tickerCode: String): Boolean {
        val current = _favoriteTickers.value.toMutableSet()
        val newState = if (current.contains(tickerCode)) {
            current.remove(tickerCode)
            false
        } else {
            current.add(tickerCode)
            true
        }
        saveFavorites(current)
        return newState
    }

    fun addFavorite(tickerCode: String) {
        val current = _favoriteTickers.value.toMutableSet()
        if (current.add(tickerCode)) {
            saveFavorites(current)
        }
    }

    fun removeFavorite(tickerCode: String) {
        val current = _favoriteTickers.value.toMutableSet()
        if (current.remove(tickerCode)) {
            saveFavorites(current)
        }
    }

    fun isFavorite(tickerCode: String): Boolean {
        return _favoriteTickers.value.contains(tickerCode)
    }

    private fun saveFavorites(set: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITES, set).apply()
        _favoriteTickers.value = set
    }

    companion object {
        private const val KEY_FAVORITES = "favorite_ticker_codes"
    }
}
