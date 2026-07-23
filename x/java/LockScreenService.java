package com.hawahi.locker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

public class LockScreenService extends Service {
    private WindowManager wm;
    private View overlay;

    @Override
    public void onCreate() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                "hch", "هواهي", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(ch);
        }
        startForeground(1, new Notification.Builder(this, "hch")
            .setContentTitle("هواهي")
            .setContentText("النظام مقفل")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build());
        showOverlay();
        return START_STICKY;
    }

    private void showOverlay() {
        if (overlay != null) return;
        overlay = LayoutInflater.from(this).inflate(R.layout.lock_screen, null);
        int type = Build.VERSION.SDK_INT >= 26 ?
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
            WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.CENTER;
        p.screenBrightness = 0.1f;
        try { wm.addView(overlay, p); } catch (Exception e) {}
    }

    @Override
    public void onDestroy() {
        if (overlay != null) { wm.removeView(overlay); overlay = null; }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}