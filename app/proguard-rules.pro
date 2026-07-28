# No minification is enabled, but keep our entry points defensively in case a
# release build later turns on shrinking.
-keep class com.universal.deviceinfo.** { *; }
-dontwarn com.universal.deviceinfo.**
