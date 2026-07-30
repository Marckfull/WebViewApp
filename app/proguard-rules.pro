# Regras de ofuscacao do PRISMA - Fusao Cromatica

# kotlinx.serialization: preserva os serializadores gerados dos modelos persistidos.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.prisma.fusao.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.prisma.fusao.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.prisma.fusao.data.**$$serializer { *; }

# Google Mobile Ads faz reflexao sobre estas classes.
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
