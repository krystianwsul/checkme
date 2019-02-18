# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Krystian\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontobfuscate

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

-dontwarn java.lang.invoke.*

-keep class com.krystianwsul.** { *; }

# guava

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe
-dontwarn javax.lang.model.element.Modifier

-keep class com.google.j2objc.annotations.** { *; }
-dontwarn   com.google.j2objc.annotations.**
-keep class java.lang.ClassValue { *; }
-dontwarn   java.lang.ClassValue
-keep class org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement { *; }
-dontwarn   org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.concurrent.LazyInit
-dontwarn com.google.errorprone.annotations.ForOverride

-keep class android.arch.lifecycle.** {*;}

-keep class com.google.firebase.** { *; }

-dontwarn com.google.auto.value.AutoValue