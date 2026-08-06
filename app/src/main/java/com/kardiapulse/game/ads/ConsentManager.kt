package com.kardiapulse.game.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.kardiapulse.game.BuildConfig

/**
 * Consentimento de privacidade (UMP / Google Funding Choices).
 *
 * Na Europa e em alguns outros territórios é obrigatório perguntar antes de pedir anúncios
 * personalizados. Fora dessas regiões o formulário simplesmente não aparece e o fluxo segue.
 * Se qualquer coisa falhar aqui, o jogo continua — sem anúncios se for o caso.
 */
class ConsentManager(private val activity: Activity) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    /** Verdadeiro quando já é permitido pedir anúncios. */
    val canRequestAds: Boolean
        get() = runCatching { consentInformation.canRequestAds() }.getOrDefault(false)

    /** Verdadeiro quando faz sentido mostrar a opção "Configurações de privacidade". */
    val privacyOptionsRequired: Boolean
        get() = runCatching {
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        }.getOrDefault(false)

    fun gather(onComplete: (Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .apply {
                if (BuildConfig.DEBUG) {
                    // Em debug dá para forçar a região do formulário adicionando o seu
                    // hashed device id em addTestDeviceHashedId(...).
                    setConsentDebugSettings(
                        ConsentDebugSettings.Builder(activity).build()
                    )
                }
            }
            .build()

        runCatching {
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                        if (error != null) Log.d(TAG, "Formulário de consentimento: ${error.message}")
                        onComplete(canRequestAds)
                    }
                },
                { error ->
                    Log.d(TAG, "Consentimento indisponível: ${error.message}")
                    onComplete(canRequestAds)
                }
            )
        }.onFailure {
            Log.w(TAG, "Falha no fluxo de consentimento", it)
            onComplete(false)
        }
    }

    /** Reabre o formulário a partir das configurações do jogo. */
    fun showPrivacyOptions(onComplete: () -> Unit) {
        runCatching {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { onComplete() }
        }.onFailure { onComplete() }
    }

    private companion object {
        const val TAG = "ConsentManager"
    }
}
