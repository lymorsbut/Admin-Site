# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve the line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile

# Keep all data classes and their properties
-keep class com.example.lucky.MessageDetail { *; }
-keep class com.example.lucky.UserLocation { *; }
-keep class com.example.lucky.SmsModel { *; }
-keep class com.example.lucky.UserModel { *; }

# Keep all classes with @Parcelize annotation
-keepclassmembers @kotlinx.parcelize.Parcelize class * {
    public static ** CREATOR;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Firebase rules
-keep class com.google.firebase.** { *; }
-keep class com.firebase.** { *; }
-keep class org.apache.** { *; }
-keepnames class com.fasterxml.jackson.** { *; }
-keepnames class javax.servlet.** { *; }
-keepnames class org.ietf.jgss.** { *; }
-dontwarn org.apache.**
-dontwarn org.w3c.dom.**

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static ** inflate(...);
    public static ** bind(android.view.View);
}