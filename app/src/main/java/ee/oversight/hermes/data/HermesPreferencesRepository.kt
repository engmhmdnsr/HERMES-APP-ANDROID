package ee.oversight.hermes.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ee.oversight.hermes.model.AppLanguage
import ee.oversight.hermes.model.ConnectionConfig

class HermesPreferencesRepository(context: Context) {
    // API key is stored ENCRYPTED (EncryptedSharedPreferences with a
    // hardware-backed MasterKey). Non-secret prefs (ip/port/language) use the
    // same encrypted store for simplicity.
    private val prefs: SharedPreferences = run {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "hermes_control_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Encryption unavailable (keystore broken / first-run race). We do
            // NOT silently downgrade secrets to plaintext: non-secret prefs
            // still work so the app is usable, but saveApiKey() refuses to
            // persist the key and the UI shows a warning via encryptionAvailable.
            context.getSharedPreferences("hermes_control_prefs", Context.MODE_PRIVATE)
        }
    }

    /** False when EncryptedSharedPreferences failed to initialize (keystore
     *  problem). In that state the API key is never persisted to disk. */
    val encryptionAvailable: Boolean = try {
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        true
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val KEY_IP = "pref_tailscale_ip"
        private const val KEY_PORT = "pref_port"
        private const val KEY_API_KEY = "pref_api_key"
        private const val KEY_USE_HTTPS = "pref_use_https"
        private const val KEY_GATEWAY_URL = "pref_gateway_url"
        private const val KEY_USE_CUSTOM_URL = "pref_use_custom_url"
        private const val KEY_MODEL = "pref_selected_model"
        private const val KEY_REASONING_EFFORT = "pref_reasoning_effort"
        private const val KEY_LANGUAGE = "pref_app_language"
        // Saved named profiles: "profile_<name>" -> JSON of ConnectionConfig
        private const val KEY_PROFILES = "pref_saved_profiles"
        private const val KEY_ACTIVE_PROFILE = "pref_active_profile"
        private const val KEY_PINNED_SESSIONS = "pref_pinned_sessions"
        private const val KEY_GLOBAL_AUTO_APPROVE = "pref_global_auto_approve"
        private const val KEY_BIOMETRIC_LOCK = "pref_biometric_lock"
    }

    fun isBiometricLockEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)

    fun setBiometricLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
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
        val edit = prefs.edit()
            .putString(KEY_IP, config.tailscaleIp)
            .putInt(KEY_PORT, config.port)
            .putString(KEY_GATEWAY_URL, config.remoteGatewayUrl)
            .putBoolean(KEY_USE_CUSTOM_URL, config.useCustomGatewayUrl)
            .putBoolean(KEY_USE_HTTPS, config.useHttps)
        // Never write the API key to disk unless encryption is actually on:
        // a plaintext downgrade of the key would defeat the whole point.
        if (encryptionAvailable) {
            edit.putString(KEY_API_KEY, config.apiKey)
        } else {
            edit.remove(KEY_API_KEY)
        }
        edit.apply()
    }

    // ---- Named profiles (save/load/delete/rename) ----

    /** All saved profile names (most recent first). */
    fun getSavedProfileNames(): List<String> {
        val raw = prefs.getString(KEY_PROFILES, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\u0001").filter { it.isNotBlank() }.reversed()
    }

    fun getProfileConfig(name: String): ConnectionConfig? {
        val raw = prefs.getString("profile_$name", "") ?: return null
        if (raw.isBlank()) return null
        return try {
            val json = org.json.JSONObject(raw)
            ConnectionConfig(
                tailscaleIp = json.optString("tailscaleIp", ""),
                port = json.optInt("port", 8080),
                remoteGatewayUrl = json.optString("remoteGatewayUrl", ""),
                useCustomGatewayUrl = json.optBoolean("useCustomGatewayUrl", false),
                apiKey = json.optString("apiKey", ""),
                useHttps = json.optBoolean("useHttps", false)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveProfile(name: String, config: ConnectionConfig) {
        val json = org.json.JSONObject().apply {
            put("tailscaleIp", config.tailscaleIp)
            put("port", config.port)
            put("remoteGatewayUrl", config.remoteGatewayUrl)
            put("useCustomGatewayUrl", config.useCustomGatewayUrl)
            // Never persist the key when encryption is unavailable.
            if (encryptionAvailable) put("apiKey", config.apiKey) else put("apiKey", "")
            put("useHttps", config.useHttps)
        }
        prefs.edit().putString("profile_$name", json.toString()).apply()
        // Add name to ordered list (dedupe)
        val names = getSavedProfileNames().toMutableList()
        if (name in names) names.remove(name)
        names.add(name)
        prefs.edit().putString(KEY_PROFILES, names.joinToString("\u0001")).apply()
    }

    fun deleteProfile(name: String) {
        prefs.edit().remove("profile_$name").apply()
        val names = getSavedProfileNames().toMutableList()
        names.remove(name)
        prefs.edit().putString(KEY_PROFILES, names.joinToString("\u0001")).apply()
    }

    fun getActiveProfileName(): String {
        return prefs.getString(KEY_ACTIVE_PROFILE, "") ?: ""
    }

    fun setActiveProfileName(name: String) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE, name).apply()
    }

    fun getSelectedModelId(): String {
        return prefs.getString(KEY_MODEL, "") ?: ""
    }

    fun saveSelectedModelId(modelId: String) {
        prefs.edit().putString(KEY_MODEL, modelId).apply()
    }

    fun getReasoningEffort(): String {
        return prefs.getString(KEY_REASONING_EFFORT, "medium") ?: "medium"
    }

    fun saveReasoningEffort(effort: String) {
        prefs.edit().putString(KEY_REASONING_EFFORT, effort).apply()
    }

    fun getPinnedSessionIds(): Set<String> {
        val raw = prefs.getString(KEY_PINNED_SESSIONS, "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun savePinnedSessionIds(ids: Set<String>) {
        prefs.edit().putString(KEY_PINNED_SESSIONS, ids.joinToString(",")).apply()
    }

    fun getGlobalAutoApprove(): Boolean {
        return prefs.getBoolean(KEY_GLOBAL_AUTO_APPROVE, false)
    }

    fun saveGlobalAutoApprove(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GLOBAL_AUTO_APPROVE, enabled).apply()
    }
}
