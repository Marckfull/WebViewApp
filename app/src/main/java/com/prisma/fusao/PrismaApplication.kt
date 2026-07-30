package com.prisma.fusao

import android.app.Application
import com.prisma.fusao.ads.AdsManager
import com.prisma.fusao.audio.SoundEngine
import com.prisma.fusao.data.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * O jogo é pequeno o bastante para não precisar de injeção de dependência:
 * três objetos de processo, criados aqui e acessados pela Application.
 */
class PrismaApplication : Application() {

    lateinit var repository: GameRepository
        private set

    lateinit var soundEngine: SoundEngine
        private set

    lateinit var adsManager: AdsManager
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        repository = GameRepository(this)
        soundEngine = SoundEngine(this)
        adsManager = AdsManager(this)

        soundEngine.initialize()

        scope.launch {
            // Aplica as preferências de áudio salvas e gera o conteúdo do dia.
            repository.refreshDailyContent()
            val state = repository.state.first()
            soundEngine.musicEnabled = state.musicEnabled
            soundEngine.sfxEnabled = state.sfxEnabled
        }
    }
}
