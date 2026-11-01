package com.amolsolunke.stepfitpro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepCounterService extends Service
        implements SensorEventListener {

    private static final String CHANNEL_ID = "stepfit_counter";
    private static final int NOTIFICATION_ID = 4817;

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = getSharedPreferences("StepFitData", MODE_PRIVATE);

        createNotificationChannel();

        // Foreground service notification
        startForeground(NOTIFICATION_ID, buildNotification(0));

        sensorManager =
                (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(
                    Sensor.TYPE_STEP_COUNTER
            );
        }

        prefs.edit()
                .putBoolean("sensor_available", stepSensor != null)
                .apply();

        if (stepSensor != null) {
            sensorManager.registerListener(
                    this,
                    stepSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );
        }
    }

    private String getToday() {
        return new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
        ).format(new Date());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "StepFit PRO Step Counter",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription(
                    "Keeps StepFit PRO step counting active"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(long steps) {
        Intent intent = new Intent(this, MainActivity.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                flags
        );

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("StepFit PRO")
                .setContentText(
                        String.format(
                                Locale.getDefault(),
                                "Today's steps: %,d",
                                steps
                        )
                )
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER
                || event.values == null
                || event.values.length == 0) {
            return;
        }

        long rawSteps = (long) event.values[0];

        String today = getToday();
        String savedDate = prefs.getString("date", "");

        long todaySteps = prefs.getLong("today_steps", 0L);
        long baseSteps = prefs.getLong("base_steps", -1L);
        long lastRaw = prefs.getLong("last_raw", -1L);

        SharedPreferences.Editor editor = prefs.edit();

        // New day: save previous day's total and start a new baseline.
        if (!today.equals(savedDate)) {

            if (!savedDate.isEmpty()) {
                editor.putLong(
                        "history_" + savedDate,
                        todaySteps
                );
            }

            todaySteps = 0L;
            baseSteps = rawSteps;
            lastRaw = rawSteps;

            editor.putLong("today_steps", 0L);
        }

        // First reading: establish baseline.
        if (baseSteps < 0L || lastRaw < 0L) {
            baseSteps = rawSteps;
            lastRaw = rawSteps;
        }

        // Handle sensor counter reset.
        if (rawSteps < lastRaw) {
            baseSteps = rawSteps - todaySteps;
        }

        if (rawSteps >= baseSteps) {
            todaySteps = Math.max(
                    todaySteps,
                    rawSteps - baseSteps
            );
        }

        editor.putLong("today_steps", todaySteps);
        editor.putLong("base_steps", baseSteps);
        editor.putLong("last_raw", rawSteps);
        editor.putString("date", today);
        editor.putBoolean("sensor_available", true);
        editor.apply();

        // Notify the app screens that the count has changed.
        Intent updateIntent = new Intent(
                "com.amolsolunke.stepfitpro.STEPS_UPDATED"
        );

        updateIntent.setPackage(getPackageName());
        sendBroadcast(updateIntent);

        // Update persistent notification.
        NotificationManager notificationManager =
                (NotificationManager)
                        getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(todaySteps)
            );
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No additional action required.
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
