package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ConnectionConfig

class HermesPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hermes_control_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IP = "pref_tailscale_ip"
        private const val KEY_PORT = "pref_port"
        private const val KEY_API_KEY = "pref_api_key"
        private const val KEY_USE_HTTPS = "pref_use_https"
        private const val KEY_DEMO_MODE = "pref_demo_mode"
        private const val KEY_MODEL = "pref_selected_model"
    }

    fun getConnectionConfig(): ConnectionConfig {
        return ConnectionConfig(
            tailscaleIp = prefs.getString(KEY_IP, "100.84.12.93") ?: "100.84.12.93",
            port = prefs.getInt(KEY_PORT, 8080),
            apiKey = prefs.getString(KEY_API_KEY, "hermes_live_key_99x") ?: "hermes_live_key_99x",
            useHttps = prefs.getBoolean(KEY_USE_HTTPS, false),
            isDemoMode = prefs.getBoolean(KEY_DEMO_MODE, true) // Start in demo mode for instant interactive testing
        )
    }

    fun saveConnectionConfig(config: ConnectionConfig) {
        prefs.edit()
            .putString(KEY_IP, config.tailscaleIp)
            .putInt(KEY_PORT, config.port)
            .putString(KEY_API_KEY, config.apiKey)
            .putBoolean(KEY_USE_HTTPS, config.useHttps)
            .putBoolean(KEY_DEMO_MODE, config.isDemoMode)
            .apply()
    }

    fun setDemoMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_MODE, enabled).apply()
    }

    fun getSelectedModelId(): String {
        return prefs.getString(KEY_MODEL, "claude-3-7-sonnet") ?: "claude-3-7-sonnet"
    }

    fun saveSelectedModelId(modelId: String) {
        prefs.edit().putString(KEY_MODEL, modelId).apply()
    }
}
