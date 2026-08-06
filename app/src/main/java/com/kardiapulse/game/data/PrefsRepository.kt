package com.kardiapulse.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kardiapulse.game.security.SaveGuard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kardia_pulse")

/**
 * Única fonte de verdade do progresso.
 *
 * O perfil inteiro vira um JSON e é gravado junto de uma assinatura HMAC ([SaveGuard]). Se
 * alguém editar o arquivo por fora, a assinatura não bate: o perfil é restaurado do zero e
 * marcado com `tampered`, o que a tela de perfil mostra abertamente ao jogador.
 */
class PrefsRepository(private val context: Context) {

    private val keyProfile = stringPreferencesKey("profile")
    private val keySignature = stringPreferencesKey("profile_sig")

    val profile: Flow<PlayerProfile> = context.dataStore.data.map { prefs -> read(prefs) }

    suspend fun current(): PlayerProfile = profile.first()

    private fun read(prefs: Preferences): PlayerProfile {
        val raw = prefs[keyProfile] ?: return PlayerProfile()
        val signature = prefs[keySignature] ?: ""
        if (!SaveGuard.verify(raw, signature)) {
            return PlayerProfile(tampered = true)
        }
        return PlayerProfile.fromJson(raw) ?: PlayerProfile(tampered = true)
    }

    /** Aplica uma transformação ao perfil e regrava assinado. */
    suspend fun update(transform: (PlayerProfile) -> PlayerProfile): PlayerProfile {
        var result = PlayerProfile()
        context.dataStore.edit { prefs ->
            val updated = transform(read(prefs)).copy(tampered = false)
            val json = updated.toJson()
            prefs[keyProfile] = json
            prefs[keySignature] = SaveGuard.sign(json)
            result = updated
        }
        return result
    }

    suspend fun reset() {
        context.dataStore.edit { it.clear() }
    }
}
