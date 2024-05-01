package flutter.overlay.window.flutter_overlay_window;


import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import io.flutter.plugin.common.BasicMessageChannel;

public abstract class WindowSetup {
    static final String OVERLAY_DATA = "OVERLAY_DATA";
    static final String OVERLAY_WIDTH = "OVERLAY_WIDTH";
    static final String OVERLAY_HEIGHT = "OVERLAY_HEIGHT";
    static final String OVERLAY_MINIMUM_VISIBLE_WIDTH = "OVERLAY_MINIMUM_VISIBLE_WIDTH";
    static final String OVERLAY_MINIMUM_VISIBLE_HEIGHT = "OVERLAY_MINIMUM_VISIBLE_HEIGHT";

    static String overlayTitle = "Overlay is activated";
    static String overlayContent = "Tap to edit settings or disable";
}
