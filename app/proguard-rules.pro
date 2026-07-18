# R8 keep rules for the release build (isMinifyEnabled = true).
# Most libraries (Compose, Room, ML Kit, play-services-ads) ship their own
# consumer rules; only app-specific reflection/serialization needs rules here.

# --- kotlinx.serialization (the backup export/import feature) ---
# Canonical rules from the kotlinx.serialization README so generated serializers
# for @Serializable classes survive shrinking/obfuscation.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Enums persisted by name ---
# Settings, profile and backup store enum values as their declared name() and
# read them back with entries.find { it.name == stored }. Keep the constant
# names stable so existing DataStore values and shared backup files still match.
-keepclassmembers enum com.gymgym.app.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- ViewModel ---
# Constructed by reflection via the AndroidViewModel factory (needs the
# (Application) constructor).
-keep class com.gymgym.app.ui.MainViewModel { <init>(...); }
