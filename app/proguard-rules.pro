-keep class com.arigato.app.domain.entity.** { *; }
-keep class com.arigato.app.data.local.database.entity.** { *; }
-keepattributes *Annotation*
-keepattributes Signature
-dontwarn kotlinx.serialization.**

# Termux terminal libraries
-keep class com.termux.terminal.** { *; }
-dontwarn com.termux.terminal.**

# Markwon
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# Security Crypto
-keep class androidx.security.** { *; }
-dontwarn androidx.security.**

# Hilt
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Compose
-keep class androidx.compose.** { *; }
