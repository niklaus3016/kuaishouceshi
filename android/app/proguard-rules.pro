# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===== 快手广告 SDK 混淆规则（官方要求，请勿修改）=====
-keep class com.kwad.sdk.** { *; }
-dontwarn com.kwad.sdk.**
-keep class com.kwad.open.** { *; }
-dontwarn com.kwad.open.**
# 防止 Gson / OkHttp 相关类被混淆
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
