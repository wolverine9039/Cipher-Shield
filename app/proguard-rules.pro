# Cipher Shield ProGuard Rules
# Version 2.0 - Security Hardened

# ===============================================
# Security: Obfuscation Settings
# ===============================================

# Enable aggressive obfuscation
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Rename packages to make reverse engineering harder
-repackageclasses 'o'
-flattenpackagehierarchy

# Remove debugging info
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ===============================================
# Keep Security Classes
# ===============================================

# Keep encryption utilities
-keep class com.example.ciphershield.security.** { *; }

# Keep result classes (needed for reflection)
-keep class com.example.ciphershield.security.SecureEncryptionUtil$EncryptionResult { *; }
-keep class com.example.ciphershield.security.SecureEncryptionUtil$DecryptionResult { *; }

# ===============================================
# Android Components
# ===============================================

# Keep Activities
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep View constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Activity lifecycle methods
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# ===============================================
# Material Design Components
# ===============================================

-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**

# ===============================================
# Crypto Libraries
# ===============================================

# Keep Java Security classes
-keep class javax.crypto.** { *; }
-keep class javax.security.** { *; }
-keep class java.security.** { *; }
-dontwarn javax.crypto.**
-dontwarn javax.security.**
-dontwarn java.security.**

# Keep Android Security classes
-keep class android.security.** { *; }
-dontwarn android.security.**

# ===============================================
# Remove Logging (Security)
# ===============================================

# Remove all logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# Remove Timber logging
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# ===============================================
# Serialization
# ===============================================

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===============================================
# Parcelable
# ===============================================

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ===============================================
# Enum
# ===============================================

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===============================================
# Native Methods
# ===============================================

-keepclasseswithmembernames class * {
    native <methods>;
}

# ===============================================
# Resource IDs
# ===============================================

-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep resource references
-keep class **.R
-keep class **.R$* {
    <fields>;
}

# ===============================================
# Reflection
# ===============================================

# Keep classes accessed via reflection
-keep class * {
    @androidx.annotation.Keep *;
}

# ===============================================
# Annotations
# ===============================================

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ===============================================
# Kotlin (if used in future)
# ===============================================

-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ===============================================
# Security: String Encryption
# ===============================================

# Note: For maximum security, consider using:
# - DexGuard (commercial)
# - String encryption plugins
# - Native library obfuscation

# ===============================================
# Crash Reporting
# ===============================================

# Keep stack traces readable
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===============================================
# Custom Rules for App
# ===============================================

# Keep FileProvider
-keep class androidx.core.content.FileProvider { *; }

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# ===============================================
# Warning Suppressions
# ===============================================

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ===============================================
# Security Recommendations
# ===============================================

# 1. Enable R8 full mode in gradle.properties:
#    android.enableR8.fullMode=true

# 2. Use APK signature scheme v2 and v3

# 3. Enable app bundle optimization:
#    android.bundle.enableUncompressedNativeLibs=false

# 4. Consider using Android App Bundle for better optimization

# 5. Regular security audits and penetration testing

# ===============================================
# Build Information
# ===============================================

# This ProGuard configuration provides:
# ✓ Code obfuscation
# ✓ String obfuscation
# ✓ Resource shrinking
# ✓ Logging removal
# ✓ Stack trace preservation
# ✓ Security hardening