# NeuroFlip — regras de ofuscação

# Mantém os nomes dos enums do domínio para deixar os crash reports legíveis.
-keepclassmembers enum com.neuroflip.game.** { *; }

# AdMob / Play Services / UMP já trazem as próprias regras (consumer rules);
# estas linhas apenas silenciam avisos de classes opcionais.
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.ump.**
-dontwarn androidx.compose.**

# Stack traces com número de linha
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
