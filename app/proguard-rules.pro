# PDFBox Android — optional native/JPEG2000 not on Android
-dontwarn com.gemalto.jp2.**
-dontwarn com.gemalto.**
-dontwarn org.apache.commons.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.fontbox.**

# Keep only our app package (small)
-keep class com.pdfcraft.studio.** { *; }

# DO NOT keep all of androidx.compose — that blocked R8 shrinking
# and was a main cause of the \~12MB release APK.

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
