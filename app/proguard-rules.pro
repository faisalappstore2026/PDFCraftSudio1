# PDFBox Android — optional native/JPEG2000 not on Android
-dontwarn com.gemalto.jp2.**
-dontwarn com.gemalto.**
-dontwarn org.apache.commons.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.fontbox.**
-dontwarn com.tom_roush.**

# PDFBox-Android: AAR ships consumer rules for SecurityHandler reflection;
# keep core packages too so R8 minify does not strip load/link paths.
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.pdfbox.android.** { *; }
-keep,allowobfuscation class * extends com.tom_roush.pdfbox.pdmodel.encryption.SecurityHandler {
    public <init>(...);
}
-keep class com.tom_roush.pdfbox.pdmodel.documentinterchange.** { *; }

# Keep only our app package (small)
-keep class com.pdfcraft.studio.** { *; }

# DO NOT keep all of androidx.compose — that blocked R8 shrinking
# and was a main cause of the \~12MB release APK.

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
