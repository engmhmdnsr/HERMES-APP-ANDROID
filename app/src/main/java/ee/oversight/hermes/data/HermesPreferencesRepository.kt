package ee.oversight.hermes.data

import android.content.Context
import android.content.SharedPreferences
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.ConnectionConfig

class HermesPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hermes_control_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IP = "pref_tailscale_ip"
        private const val KEY_PORT = "pref_port"
        private const val KEY_API_KEY = "pref_api_key"
        private const val KEY_USE_HTTPS = "pref_use_https"
        private const val KEY_GATEWAY_URL = "pref_gateway_url"
        private const val KEY_USE_CUSTOM_URL = "pref_use_custom_url"
        private const val KEY_MODEL = "pref_selected_model"
        private const val KEY_LANGUAGE = "pref_app_language"
    }

    fun getAppLanguage(): AppLanguage {
        val code = prefs.getString(KEY_LANGUAGE, AppLanguage.EN.code)
        return if (code == AppLanguage.AR.code) {
            AppLanguage.AR
        } else {
            AppLanguage.EN
        }
    }

    fun saveAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun getConnectionConfig(): ConnectionConfig {
        return ConnectionConfig(
            tailscaleIp = prefs.getString(KEY_IP, "") ?: "",
            port = prefs.getInt(KEY_PORT, 8080),
            remoteGatewayUrl = prefs.getString(KEY_GATEWAY_URL, "") ?: "",
            useCustomGatewayUrl = prefs.getBoolean(KEY_USE_CUSTOM_URL, false),
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            useHttps = prefs.getBoolean(KEY_USE_HTTPS, false)
        )
    }

    fun saveConnectionConfig(config: ConnectionConfig) {
        prefs.edit()
            .putString(KEY_IP, config.tailscaleIp)
            .putInt(KEY_PORT, config.port)
            .putString(KEY_GATEWAY_URL, config.remoteGatewayUrl)
            .putBoolean(KEY_USE_CUSTOM_URL, config.useCustomGatewayUrl)
            .putString(KEY_API_KEY, config.apiKey)
            .putBoolean(KEY_USE_HTTPS, config.useHttps)
            .apply()
    }

    fun getSelectedModelId(): String {
        return prefs.getString(KEY_MODEL, "deepseek/deepseek-v4-flash") ?: "deepseek/deepseek-v4-flash"
    }

    fun saveSelectedModelId(modelId: String) {
        prefs.edit().putString(KEY_MODEL, modelId).apply()
    }
}
