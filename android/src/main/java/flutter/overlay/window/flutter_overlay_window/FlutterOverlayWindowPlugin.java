package flutter.overlay.window.flutter_overlay_window;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class FlutterOverlayWindowPlugin implements
        FlutterPlugin, MethodCallHandler {

    private MethodChannel channel;
    private Context context;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        this.context = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), OverlayConstants.CHANNEL_TAG);
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("showOverlay")) {
            if (!checkOverlayPermission()) {
                result.error("PERMISSION", "overlay permission is not enabled", null);
                return;
            }
            String data = call.argument("data");
            Integer width = call.argument("width");
            Integer height = call.argument("height");
            Integer minimumVisibleWidth = call.argument("minimumVisibleWidth");
            Integer minimumVisibleHeight = call.argument("minimumVisibleHeight");
            String overlayTitle = call.argument("overlayTitle");
            String overlayContent = call.argument("overlayContent");

            if (overlayTitle != null) {
                WindowSetup.overlayTitle = overlayTitle;
            }
            if (overlayContent != null) {
                WindowSetup.overlayContent = overlayContent;
            }

            final Intent intent = new Intent(context, OverlayService.class);

            intent.putExtra(WindowSetup.OVERLAY_DATA, data);
            intent.putExtra(WindowSetup.OVERLAY_WIDTH, width != null ? width : -1);
            intent.putExtra(WindowSetup.OVERLAY_HEIGHT, height != null ? height : -1);
            intent.putExtra(WindowSetup.OVERLAY_MINIMUM_VISIBLE_WIDTH, minimumVisibleWidth == null ? 0 : minimumVisibleWidth);
            intent.putExtra(WindowSetup.OVERLAY_MINIMUM_VISIBLE_HEIGHT, minimumVisibleHeight == null ? 0 : minimumVisibleHeight);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);


            context.startService(intent);
            result.success(null);
        } else if (call.method.equals("isOverlayActive")) {
            result.success(OverlayService.isRunning);
        } else if (call.method.equals("closeOverlay")) {
            if (OverlayService.isRunning) {
                final Intent i = new Intent(context, OverlayService.class);
                context.stopService(i);
                result.success(true);
            }
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }


    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }
}
