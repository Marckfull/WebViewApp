# Regras de ofuscação/encolhimento do Kardia Pulse (build de release).

# --- Modelo do jogo -----------------------------------------------------------
# O perfil é serializado manualmente para JSON usando os NOMES das constantes de enum
# (PowerType.name). Se o R8 renomear esses enums, todo save existente vira lixo.
-keepclassmembers enum com.kardiapulse.game.core.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** entries();
}
-keepnames enum com.kardiapulse.game.core.model.PowerType

# As rotas de navegação também carregam nomes de enum (GameMode, Difficulty).
-keepnames enum com.kardiapulse.game.core.model.GameMode
-keepnames enum com.kardiapulse.game.core.model.Difficulty
-keepnames enum com.kardiapulse.game.core.model.Modifier

# --- WorkManager --------------------------------------------------------------
# O worker é instanciado por reflexão a partir do nome da classe.
-keep class com.kardiapulse.game.notifications.TauntWorker { <init>(...); }

# --- Anúncios -----------------------------------------------------------------
# O SDK do Google Play Services já traz as próprias regras consumidoras; estas apenas
# evitam avisos ruidosos de dependências opcionais que não usamos.
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.ump.**

# --- Kotlin / Coroutines ------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlin.Metadata { public <methods>; }

# --- Diagnóstico --------------------------------------------------------------
# Mantém números de linha para que relatórios de erro continuem legíveis, mas esconde
# o caminho original dos arquivos.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove as chamadas de log de depuração do binário publicado.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
