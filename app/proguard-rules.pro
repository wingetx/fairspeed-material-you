-keep class com.fairspeed.** { *; }
-keepclassmembers class ** {
    *** **(android.content.Context, ...);
}
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
