# ═══════════════════════════════════════════════════════════
# SleepController — ProGuard / R8 Rules
# ═══════════════════════════════════════════════════════════

# ── Protobuf (DataStore) ──
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ── Room Database ──
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# ── Hilt / Dagger ──
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
# Keep Hilt EntryPoint interfaces
-keep @dagger.hilt.EntryPoint interface * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# ── Services (Android system components must be kept) ──
-keep class com.sleepcontroller.service.AppBlockerAccessibilityService { *; }
-keep class com.sleepcontroller.service.OverlayBlockerService { *; }
-keep class com.sleepcontroller.service.SleepScheduleService { *; }
-keep class com.sleepcontroller.service.AlarmService { *; }

# ── BroadcastReceivers (registered in manifest) ──
-keep class com.sleepcontroller.receiver.BootReceiver { *; }
-keep class com.sleepcontroller.receiver.SleepModeReceiver { *; }
-keep class com.sleepcontroller.receiver.AlarmReceiver { *; }
-keep class com.sleepcontroller.receiver.WindDownReceiver { *; }

# ── Workers (HiltWorker uses reflection) ──
-keep class com.sleepcontroller.worker.SleepScheduleWorker { *; }

# ── Domain models (used in flow/state) ──
-keep class com.sleepcontroller.domain.model.SessionState { *; }
-keep class com.sleepcontroller.domain.model.SessionState$* { *; }

# ── Data entities (Room serialization) ──
-keep class com.sleepcontroller.data.db.entity.** { *; }
-keep class com.sleepcontroller.data.db.Converters { *; }

# ── Kotlin serialization / coroutines ──
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# ── Compose (prevent stripping of Composable metadata) ──
-dontwarn androidx.compose.**

# ── Strip verbose/debug/info logs in release ──
# Keeps Log.w and Log.e for production error tracking
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
