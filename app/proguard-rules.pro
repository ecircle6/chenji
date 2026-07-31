# Add project specific ProGuard rules here.
# By default, the flags in this file are marked with an asterisk (*).

# Keep Room entities
-keep class com.birthapp.data.Birthday { *; }

# Keep BroadcastReceiver subclasses
-keep class com.birthapp.alarm.AlarmReceiver { *; }
-keep class com.birthapp.alarm.BootReceiver { *; }

# Keep 桌面小组件：receiver 与 Glance 界面类都靠系统按名反射调起，
# 混淆改名后会导致小组件加不出来（保险起见显式保留整个 widget 包）
-keep class com.birthapp.widget.** { *; }
