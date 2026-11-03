# Start.io SDK (from its integration guide; re-check against the current guide when upgrading the SDK)
-keep class com.startapp.** { *; }
-keepattributes Exceptions, InnerClasses, Signature, Deprecation, SourceFile, LineNumberTable, *Annotation*, EnclosingMethod
-dontwarn android.webkit.JavascriptInterface
-dontwarn com.startapp.**
