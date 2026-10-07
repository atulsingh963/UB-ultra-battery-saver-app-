# Keep Android components referenced in Manifest
-keepclasseswithmembers class * extends android.app.Activity { <init>(...); }
-keepclasseswithmembers class * extends android.app.Service { <init>(...); }
-keepclasseswithmembers class * extends android.content.BroadcastReceiver { <init>(...); }
-keepclasseswithmembers class * extends android.service.quicksettings.TileService { <init>(...); }

# Compose rules
-keepclassmembers class * extends androidx.compose.ui.node.ModifierNodeElement {
    <init>(...);
}
