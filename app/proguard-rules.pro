# Format Frute
-keepattributes *Annotation*, InnerClasses, Signature

# Modelos serializados em preferencias
-keep class com.formatfrute.game.data.** { *; }

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
