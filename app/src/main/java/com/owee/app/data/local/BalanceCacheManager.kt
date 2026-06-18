package com.owee.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.owee.app.viewmodel.BalanceUiState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BalanceCacheManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("balance_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun saveBalanceState(state: BalanceUiState) {
        try {
            val data = json.encodeToString(state)
            prefs.edit().putString("balance_state", data).apply()
        } catch (e: Exception) {
            // Log or handle error
        }
    }

    fun getBalanceState(): BalanceUiState? {
        val data = prefs.getString("balance_state", null) ?: return null
        return try {
            json.decodeFromString<BalanceUiState>(data)
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
