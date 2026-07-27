# Modelos serializados com kotlinx.serialization precisam sobreviver ao R8.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.chuvadeletras.game.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.chuvadeletras.game.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.chuvadeletras.game.data.**$$serializer { *; }

# Enums usados como chave/valor nos dados salvos
-keepclassmembers enum com.chuvadeletras.game.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
