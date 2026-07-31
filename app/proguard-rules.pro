# Add project specific ProGuard rules here.
# By default, the flags in this file are marked with an asterisk (*).

# Keep Room entities
-keep class com.birthapp.data.Birthday { *; }

# Keep BroadcastReceiver subclasses
-keep class com.birthapp.alarm.AlarmReceiver { *; }
-keep class com.birthapp.alarm.BootReceiver { *; }
