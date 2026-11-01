package com.amolsolunke.stepfitpro;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepCounterService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "stepfit_counter";
    private static final int NOTIFICATION_ID = 4817;
    private static final String ACTION_STEPS_UPDATED = "com.amolsolunke.stepfitpro.STEPS_UPDATED";

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SharedPreferences prefs;
    private long lastNotificationSteps = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("StepFitData", MODE_PRIVATE);
        createNotificationChannel();

        // Android requires a foreground notification for a foreground service.
        startForeground(NOTIFICATION_ID, buildNotification(
                Math.max(0L, prefs.getLong("today_steps", prefs.getInt("today_steps", 0)))
        ));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        boolean permissionGranted = Build.VERSION.SDK_INT < 29 ||
                checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        boolean available = stepSensor != null && permissionGranted;
        prefs.edit().putBoolean("sensor_available", available).apply();

        if (available) {
            boolean registered = sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            prefs.edit().putBoolean("sensor_registered", registered).apply();
        } else {
            prefs.edit().putBoolean("sensor_registered", false).apply();
        }
        broadcastUpdate();
    }

    private String getToday() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "StepFit PRO Step Counter", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps StepFit PRO step counting active");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(long steps) {
        Intent intent = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder.setContentTitle("StepFit PRO")
                .setContentText(String.format(Locale.getDefault(), "Today's steps: %,d", Math.max(0L, steps)))
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void broadcastUpdate() {
        Intent updateIntent = new Intent(ACTION_STEPS_UPDATED);
        updateIntent.setPackage(getPackageName());
        sendBroadcast(updateIntent);
    }

    private void saveDayIfNeeded(String today, String savedDate, long steps) {
        if (!savedDate.isEmpty() && !savedDate.equals(today)) {
            prefs.edit().putLong("history_" + savedDate, Math.max(0L, steps)).apply();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null ||
                event.sensor.getType() != Sensor.TYPE_STEP_COUNTER ||
                event.values == null || event.values.length == 0) return;

        long rawSteps = Math.max(0L, (long) event.values[0]);
        String today = getToday();
        String savedDate = prefs.getString("date", "");
        long todaySteps = Math.max(0L, prefs.getLong("today_steps", prefs.getInt("today_steps", 0)));
        long baseSteps = prefs.getLong("base_steps", -1L);
        long lastRaw = prefs.getLong("last_raw", -1L);

        if (!today.equals(savedDate)) {
            saveDayIfNeeded(today, savedDate, todaySteps);
            todaySteps = 0L;
            baseSteps = rawSteps;
            lastRaw = rawSteps;
        } else if (baseSteps < 0L || lastRaw < 0L) {
            // First event after installation or reset: establish baseline.
            baseSteps = rawSteps;
            lastRaw = rawSteps;
        } else if (rawSteps < lastRaw) {
            // Sensor counter restarted; preserve today's accumulated total.
            baseSteps = rawSteps - todaySteps;
        }

        long calculated = Math.max(0L, rawSteps - baseSteps);
        todaySteps = Math.max(todaySteps, calculated);

        prefs.edit()
                .putLong("today_steps", todaySteps)
                .putLong("base_steps", baseSteps)
                .putLong("last_raw", rawSteps)
                .putString("date", today)
                .putBoolean("sensor_available", true)
                .putBoolean("sensor_registered", true)
                .apply();

        broadcastUpdate();

        if (lastNotificationSteps < 0 || Math.abs(todaySteps - lastNotificationSteps) >= 5) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification(todaySteps));
            lastNotificationSteps = todaySteps;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Re-register after a sticky service restart if needed.
        if (sensorManager == null) sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (stepSensor == null && sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
        boolean permissionGranted = Build.VERSION.SDK_INT < 29 ||
                checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        if (sensorManager != null && stepSensor != null && permissionGranted) {
            sensorManager.unregisterListener(this);
            boolean registered = sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            prefs.edit().putBoolean("sensor_available", true).putBoolean("sensor_registered", registered).apply();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
