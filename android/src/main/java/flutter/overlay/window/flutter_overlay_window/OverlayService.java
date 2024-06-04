package flutter.overlay.window.flutter_overlay_window;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import io.flutter.FlutterInjector;
import io.flutter.embedding.android.FlutterTextureView;
import io.flutter.embedding.android.FlutterView;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.StandardMessageCodec;

public class OverlayService extends Service implements View.OnTouchListener {
    private static final int DEFAULT_NAV_BAR_HEIGHT_DP = 48;
    private static final int DEFAULT_STATUS_BAR_HEIGHT_DP = 25;

    private Point mScreenSize;
    private Integer mStatusBarHeight = -1;
    private Integer mNavigationBarHeight = -1;
    private Resources mResources;

    public static boolean isRunning = false;
    private WindowManager windowManager = null;
    private FlutterEngine flutterEngine;
    private FlutterView flutterView;
    private MethodChannel flutterChannel;
    private BasicMessageChannel<Object> overlayMessageChannel;

    private int minimumVisibleWidth;
    private int minimumVisibleHeight;
    private float lastX, lastY;
    private boolean dragging;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onDestroy() {
        Log.d("flutter_overlay_window", "Destroying overlay service");
        if (windowManager != null) {
            try {
                windowManager.removeView(flutterView);
                windowManager = null;
            } catch (Exception e) {
                Log.e("flutter_overlay_window", "Failed to dispose of window manager", e);
            }
        }
        if (flutterView != null) {
            try {
                flutterView.detachFromFlutterEngine();
                flutterView = null;
            } catch (Exception e) {
                Log.e("flutter_overlay_window", "Failed to dispose of flutter view", e);
            }
        }
        if (flutterEngine != null) {
            try {
                flutterEngine.destroy();
                flutterEngine = null;
            } catch (Exception e) {
                Log.e("flutter_overlay_window", "Failed to dispose of flutter engine", e);
            }
        }
        isRunning = false;
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(OverlayConstants.NOTIFICATION_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("flutter_overlay_window", "Starting overlay service");
        isRunning = true;

        // Extract overlay data and config from the intent. This intent will be re-passed if the
        // overlay is every killed and restarted by Android.
        final String overlayData = intent.getStringExtra(WindowSetup.OVERLAY_DATA);
        final int width = intent.getIntExtra(WindowSetup.OVERLAY_WIDTH, -1);
        final int height = intent.getIntExtra(WindowSetup.OVERLAY_HEIGHT, -1);
        minimumVisibleWidth = intent.getIntExtra(WindowSetup.OVERLAY_MINIMUM_VISIBLE_WIDTH, 0);
        minimumVisibleHeight = intent.getIntExtra(WindowSetup.OVERLAY_MINIMUM_VISIBLE_HEIGHT, 0);

        mResources = getApplicationContext().getResources();

        if (windowManager != null) {
            if (flutterView != null) {
                windowManager.removeView(flutterView);
                flutterView.detachFromFlutterEngine();
                flutterView = null;
            }
            windowManager = null;
        }

        flutterEngine.getLifecycleChannel().appIsResumed();
        flutterView = new FlutterView(getApplicationContext(), new FlutterTextureView(getApplicationContext()));
        flutterView.attachToFlutterEngine(flutterEngine);

        flutterView.setFitsSystemWindows(false);
        flutterView.setFocusable(true);
        flutterView.setFocusableInTouchMode(true);
        flutterView.setBackgroundColor(Color.TRANSPARENT);
        flutterChannel.setMethodCallHandler((call, result) -> {
            if (call.method.equals("resizeOverlay")) {
                int newWidth = call.argument("width");
                int newHeight = call.argument("height");

                Integer minimumVisibleWidth = call.argument("minimumVisibleWidth");
                Integer minimumVisibleHeight = call.argument("minimumVisibleHeight");

                if (minimumVisibleWidth != null) {
                    this.minimumVisibleWidth = minimumVisibleWidth;
                }

                if (minimumVisibleHeight != null) {
                    this.minimumVisibleHeight = minimumVisibleHeight;
                }

                resizeOverlay(newWidth, newHeight, result);
            }
        });

        // The overlay will send an message to indicate that it is ready to start receiving
        // data, at which point we will send the data since we know it won't be missed.
        overlayMessageChannel.setMessageHandler((message, reply) -> {
            overlayMessageChannel.send(overlayData);
        });
        // We will also send the data now in case the service was restarted. In that scenario
        // the overlay will already be listening for the data and will not make a request for the
        // initial data so we need to set it here.
        overlayMessageChannel.send(overlayData);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width == -1999 ? -1 : width,
                height != -1999 ? height : screenSize().y,
                screenSize().x / 2 - width / 2,
                screenSize().y / 2 - height / 2,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        flutterView.setOnTouchListener(this);
        windowManager.addView(flutterView, params);
        return START_REDELIVER_INTENT;
    }


    private Point screenSize() {
        if (mScreenSize == null) {
            mScreenSize = new Point();
            windowManager.getDefaultDisplay().getRealSize(mScreenSize);
        }

        return mScreenSize;
    }

    private int statusBarHeightPx() {
        if (mStatusBarHeight == -1) {
            int statusBarHeightId = mResources.getIdentifier("status_bar_height", "dimen", "android");

            if (statusBarHeightId > 0) {
                mStatusBarHeight = mResources.getDimensionPixelSize(statusBarHeightId);
            } else {
                mStatusBarHeight = dpToPx(DEFAULT_STATUS_BAR_HEIGHT_DP);
            }
        }

        return mStatusBarHeight;
    }

    int navigationBarHeightPx() {
        if (mNavigationBarHeight == -1) {
            int navBarHeightId = mResources.getIdentifier("navigation_bar_height", "dimen", "android");

            if (navBarHeightId > 0) {
                mNavigationBarHeight = mResources.getDimensionPixelSize(navBarHeightId);
            } else {
                mNavigationBarHeight = dpToPx(DEFAULT_NAV_BAR_HEIGHT_DP);
            }
        }

        return mNavigationBarHeight;
    }


    private void resizeOverlay(int width, int height, MethodChannel.Result result) {
        if (windowManager != null) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) flutterView.getLayoutParams();
            params.width = (width == -1999 || width == -1) ? -1 : width;
            params.height = height;
            clampForMinimumVisibility(params);
            windowManager.updateViewLayout(flutterView, params);
            result.success(true);
        } else {
            result.success(false);
        }
    }

    private void clampForMinimumVisibility(WindowManager.LayoutParams params) {

        if (minimumVisibleWidth > 0) {
            int margin = minimumVisibleWidth;
            int adjustX = 0;

            int rightEdgeX = params.x + params.width;
            if (rightEdgeX < margin) {
                adjustX = margin - rightEdgeX;
            } else {
                int screenRightToLeftEdge = screenSize().x - params.x;
                if (screenRightToLeftEdge < margin) {
                    adjustX = -(margin - screenRightToLeftEdge);
                }
            }
            params.x = params.x + adjustX;
        }

        if (minimumVisibleHeight > 0) {
            int margin = minimumVisibleHeight;
            int adjustY = 0;

            int bottomEdgeY = params.y + params.height;
            if (bottomEdgeY < margin) {
                adjustY = margin - bottomEdgeY;
            } else {
                int bottomBoundary = screenSize().y - margin - navigationBarHeightPx() - statusBarHeightPx();
                if (params.y > bottomBoundary) {
                    adjustY = -(params.y - bottomBoundary);
                }
            }
            params.y = params.y + adjustY;
        }
    }

    @Override
    public void onCreate() {
        Log.d("flutter_overlay_window", "Creating overlay service");
        flutterEngine = new FlutterEngine(this);
        DartExecutor.DartEntrypoint dEntry = new DartExecutor.DartEntrypoint(
                FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                "overlayMain");
        flutterEngine.getDartExecutor().executeDartEntrypoint(dEntry);


        final DartExecutor dartExecutor = flutterEngine.getDartExecutor();
        flutterChannel = new MethodChannel(dartExecutor, OverlayConstants.OVERLAY_TAG);
        overlayMessageChannel = new BasicMessageChannel(dartExecutor, OverlayConstants.MESSENGER_TAG, StandardMessageCodec.INSTANCE);

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, FlutterOverlayWindowPlugin.class);
        int pendingFlags;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingFlags = PendingIntent.FLAG_IMMUTABLE;
        } else {
            pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, pendingFlags);

        final int notifyIcon = getDrawableResourceId("mipmap", "launcher");
        Notification notification = new NotificationCompat.Builder(this, OverlayConstants.CHANNEL_ID)
                .setContentTitle(WindowSetup.overlayTitle)
                .setContentText(WindowSetup.overlayContent)
                .setSmallIcon(notifyIcon == 0 ? R.drawable.notification_icon : notifyIcon)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(OverlayConstants.NOTIFICATION_ID, notification);
        } else {
            startForeground(OverlayConstants.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        }

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    OverlayConstants.CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private int getDrawableResourceId(String resType, String name) {
        return getApplicationContext().getResources().getIdentifier(String.format("ic_%s", name), resType, getApplicationContext().getPackageName());
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                Float.parseFloat(dp + ""), mResources.getDisplayMetrics());
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (windowManager != null) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) flutterView.getLayoutParams();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dragging = false;
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    view.performClick();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - lastX;
                    float dy = event.getRawY() - lastY;
                    if (!dragging && dx * dx + dy * dy < 25) {
                        return false;
                    }
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    int xx = params.x + (int) dx;
                    int yy = params.y + (int) dy;
                    params.x = xx;
                    params.y = yy;
                    clampForMinimumVisibility(params);
                    if (windowManager != null) {
                        windowManager.updateViewLayout(flutterView, params);
                    }
                    dragging = true;
                    break;
                default:
                    return false;
            }
            return false;
        }
        return false;
    }
}