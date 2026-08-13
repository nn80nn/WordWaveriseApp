# R8 rules for the release build.
#
# Retrofit, OkHttp, Room and Hilt ship their own consumer rules, so what is left
# is the part R8 cannot see: classes that exist only to be reflected over or
# generated into, and the crash reports we want to be able to read.

# ── Crash reports ──────────────────────────────────────────────────────────
# Without these a Play Console stack trace is a list of a(), b(), c().
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── kotlinx.serialization ──────────────────────────────────────────────────
# The plugin generates a $serializer for every @Serializable class and reaches
# it through the companion. R8 sees no call site for either, so both go unless
# they are kept — and the failure shows up only at runtime, as an empty article.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static <1>$Companion Companion;
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$$serializer INSTANCE;
}
-keep,includedescriptorclasses class kotlinx.serialization.**$$serializer { *; }

# Every DTO the API speaks in. Field names are the wire format: renaming one
# silently changes the JSON key it is read from.
-keep class com.wordwaverise.wordwaveriseapp.data.remote.dto.** { *; }

# ── Room ───────────────────────────────────────────────────────────────────
-keep class com.wordwaverise.wordwaveriseapp.data.local.entity.** { *; }

# ── Enums ──────────────────────────────────────────────────────────────────
# ThemeMode is persisted by name in DataStore, so obfuscating it would lose the
# user's choice on upgrade.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Coroutines ─────────────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
