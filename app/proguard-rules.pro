# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.routefit.app.data.api.dto.** { *; }

# Moshi
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
