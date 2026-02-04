# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Retrofit classes
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep ALL data classes and domain models (CRITICAL for Firebase and Room)
-keep class com.ciclismo.portugal.data.** { *; }
-keep class com.ciclismo.portugal.domain.** { *; }
-keepclassmembers class com.ciclismo.portugal.data.** { *; }
-keepclassmembers class com.ciclismo.portugal.domain.** { *; }

# Keep User model specifically (used by AuthService)
-keep class com.ciclismo.portugal.domain.model.User { *; }
-keepclassmembers class com.ciclismo.portugal.domain.model.User { *; }

# Keep FantasyTeam model (used by AiCoordinator)
-keep class com.ciclismo.portugal.domain.model.FantasyTeam { *; }
-keepclassmembers class com.ciclismo.portugal.domain.model.FantasyTeam { *; }

# Keep ALL Room entities
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keepclassmembers class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Google Sign-In
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.internal.firebase-auth.** { *; }
-dontwarn com.google.firebase.**
-keepclassmembers class com.google.firebase.** { *; }

# Keep Firebase Auth classes and user model
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keep class com.google.firebase.auth.FirebaseUser { *; }
-keepclassmembers class com.google.firebase.auth.FirebaseUser { *; }
-keep class com.google.firebase.auth.FirebaseAuth { *; }
-keepclassmembers class com.google.firebase.auth.FirebaseAuth { *; }

# Keep AuthService and implementations
-keep interface com.ciclismo.portugal.data.remote.firebase.AuthService { *; }
-keep class com.ciclismo.portugal.data.remote.firebase.FirebaseAuthService { *; }
-keepclassmembers class com.ciclismo.portugal.data.remote.firebase.FirebaseAuthService { *; }

# Keep extension functions (critical for toUser() conversion)
-keepclassmembers class com.ciclismo.portugal.data.remote.firebase.FirebaseAuthService {
    private *** toUser(...);
}

# Firestore
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firestore.** { *; }

# Google Play Services Auth
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.auth.api.credentials.** { *; }

# OkHttp (used by Firebase)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
