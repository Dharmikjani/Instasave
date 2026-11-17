# ==============================================================================
# InstaSaver Pro — Production ProGuard / R8 Optimization & Obfuscation Rules
# ==============================================================================

# 1. Kotlin & Coroutines
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class * {
    @kotlin.jvm.JvmField *;
}
-dontwarn kotlinx.coroutines.**

# 2. Kotlinx Serialization
-keepattributes *Annotation*, Signature
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    <fields>;
    <init>(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}

# 3. Dagger-Hilt & Assisted Injection
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class dagger.hilt.** { *; }
-keep class com.google.dagger.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclassmembers class * {
    @dagger.assisted.AssistedInject <init>(...);
}
-keepclassmembers class * {
    @dagger.assisted.AssistedFactory *;
}
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# 4. Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * implements androidx.room.RoomDatabase { *; }
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Database *;
    @androidx.room.Entity *;
    @androidx.room.Query *;
    @androidx.room.Insert *;
    @androidx.room.Update *;
    @androidx.room.Delete *;
}

# 5. OkHttp3 & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# 6. Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# 7. Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keepattributes EnclosingMethod, InnerClasses
-keepclassmembers class androidx.media3.exoplayer.** { *; }

# 8. Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# 9. App Domain & Data Models
-keep class com.instasaver.core.model.** { *; }
-keep class com.instasaver.core.database.entity.** { *; }
