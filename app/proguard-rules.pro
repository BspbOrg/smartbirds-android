# --- Obfuscation intentionally disabled (SB-C06) ---
# Shrinking + optimization stay ON so -assumenosideeffects can strip Log calls,
# but no renaming keeps reflection-heavy legacy code (EventBus/Gson) safe.
-dontobfuscate

# --- Strip all android.util.Log calls from release/beta (SB-C06) ---
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# --- EventBus 2.4.1 (reflection on onEvent* methods) ---
-keepclassmembers class ** {
    public void onEvent*(**);
}
-keep class org.bspb.smartbirds.pro.events.** { *; }

# --- Form Converters/Uploaders (instantiated reflectively via EntryType.getConverter/getUploader) ---
-keep class org.bspb.smartbirds.pro.forms.convert.** { *; }
-keep class org.bspb.smartbirds.pro.forms.upload.** { *; }

# --- Gson (reflective field access; shrinking must not drop model members) ---
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keep class org.bspb.smartbirds.pro.backend.dto.** { *; }
-keep class org.bspb.smartbirds.pro.content.** { *; }
-keep class org.bspb.smartbirds.pro.db.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Firebase Crashlytics (keep line info for readable stack traces) ---
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
