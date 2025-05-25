package me.igrr.scribevault.data.preferences

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {
    companion object {
        private val VAULT_URI = stringPreferencesKey("vault_uri")
        private val API_KEY = stringPreferencesKey("api_key")
        private val ONBOARDING_COMPLETED = stringPreferencesKey("onboarding_completed")
    }

    suspend fun saveVaultUri(uri: Uri) {
        context.dataStore.edit { preferences ->
            preferences[VAULT_URI] = uri.toString()
        }
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = "true"
        }
    }

    suspend fun getApiKey(): String? {
        return apiKey.first()
    }

    val vaultUri: Flow<Uri?> = context.dataStore.data.map { preferences ->
        preferences[VAULT_URI]?.let { Uri.parse(it) }
    }

    val apiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[API_KEY]
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] == "true"
    }
} 