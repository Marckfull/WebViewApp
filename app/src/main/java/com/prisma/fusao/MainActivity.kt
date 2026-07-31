package com.prisma.fusao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.prisma.fusao.ui.PrismaNavHost
import com.prisma.fusao.ui.theme.PrismaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as PrismaApplication

        // O fluxo de consentimento roda antes de qualquer requisição de anúncio.
        // Se falhar, o jogo continua normalmente — só não exibe anúncios.
        app.adsManager.gatherConsent(this)

        setContent {
            PrismaTheme {
                Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
                    PrismaNavHost(activity = this)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        (application as PrismaApplication).soundEngine.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        (application as PrismaApplication).soundEngine.resumeMusic()
    }
}
