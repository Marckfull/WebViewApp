package com.neuroflip.game.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.neuroflip.game.BuildConfig

/**
 * Consentimento de privacidade (Google UMP).
 *
 * Cobre LGPD (Brasil), GDPR (Europa) e as regras de estados americanos:
 * quando o usuário está numa região que exige, o formulário aparece antes
 * de qualquer anúncio personalizado. O SDK é gratuito.
 */
class ConsentManager(activity: Activity) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    val canRequestAds: Boolean
        get() = runCatching { consentInformation.canRequestAds() }.getOrDefault(false)

    val isPrivacyOptionsRequired: Boolean
        get() = runCatching {
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        }.getOrDefault(false)

    /** @param onReady chamado quando já é possível (ou não) pedir anúncios. */
    fun gather(activity: Activity, onReady: (canRequestAds: Boolean) -> Unit) {
        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        if (BuildConfig.DEBUG) {
            paramsBuilder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .build()
            )
        }

        runCatching {
            consentInformation.requestConsentInfoUpdate(
                activity,
                paramsBuilder.build(),
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                        if (error != null) {
                            Log.w(TAG, "Formulário de consentimento: ${error.message}")
                        }
                        onReady(canRequestAds)
                    }
                },
                { error ->
                    Log.w(TAG, "Consentimento indisponível: ${error.message}")
                    onReady(canRequestAds)
                }
            )
        }.onFailure { onReady(false) }
    }

    /** Reabre o painel de privacidade (link obrigatório nas Configurações). */
    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit = {}) {
        runCatching {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (error != null) Log.w(TAG, "Painel de privacidade: ${error.message}")
                onDone()
            }
        }.onFailure { onDone() }
    }

    fun reset() {
        runCatching { consentInformation.reset() }
    }

    companion object {
        private const val TAG = "NeuroFlipConsent"
    }
}
