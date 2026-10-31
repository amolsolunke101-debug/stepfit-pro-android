package com.amolsolunke.stepfitpro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

public class StepCounterService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SharedPreferences prefs;

    private static final String CHANNEL_ID = "stepfit_step_channel";
    private static final int NOTIFICATION_ID = 101;

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = getSharedPreferences("StepFitPRO", MODE_PRIVATE);

        createNotificationChannel();

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("StepFit PRO")
                .setContentText("Your steps are being tracked")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor != null) {
            sensorManager.registerListener(
                    this,
                    stepSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "StepFit PRO Step Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription("Keeps step tracking active");
            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) {
            return;
        }

        int rawSteps = (int) event.values[0];

        String today = new java.text.SimpleDateFormat(
                "yyyy-MM-dd",
                java.util.Locale.getDefault()
        ).format(new java.util.Date());

        String savedDate = prefs.getString("step_date", "");

        int baseline = prefs.getInt("step_baseline", -1);
        int previousRaw = prefs.getInt("step_last_raw", -1);
        int savedSteps = prefs.getInt("today_steps", 0);

        SharedPreferences.Editor editor = prefs.edit();

        if (!today.equals(savedDate)) {
            editor.putString("step_date", today);
            editor.putInt("step_baseline", rawSteps);
            editor.putInt("step_last_raw", rawSteps);
            editor.putInt("today_steps", 0);
            editor.apply();
            return;
        }

        if (baseline < 0 || previousRaw < 0 || rawSteps < previousRaw) {
            editor.putInt("step_baseline", rawSteps);
            editor.putInt("step_last_raw", rawSteps);
            editor.apply();
            return;
        }

        int todaySteps = Math.max(0, rawSteps - baseline);

        editor.putInt("step_last_raw", rawSteps);
        editor.putInt("today_steps", todaySteps);
        editor.apply();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action required.
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
