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

    private static final int NOTIFICATION_ID = 4817;
    private static final String CHANNEL_ID = "stepfit_counter";

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = getSharedPreferences(
                "StepFitData",
                MODE_PRIVATE
        );

        createNotificationChannel();

        Notification notification = buildNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, notification);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        sensorManager = (SensorManager)
                getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(
                    Sensor.TYPE_STEP_COUNTER
            );

            if (stepSensor != null) {
                sensorManager.registerListener(
                        this,
                        stepSensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                );
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "StepFit PRO Step Counter",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Keeps step counting active in the background"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {

        Intent intent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | (Build.VERSION.SDK_INT >= 23
                                ? PendingIntent.FLAG_IMMUTABLE : 0)
                );

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("StepFit PRO")
                .setContentText("Step counting is active")
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private String getTodayDate() {
        return new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
        ).format(new Date());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) {
            return;
        }

        long rawSteps = (long) event.values[0];

        String today = getTodayDate();

        String savedDate = prefs.getString("date", "");

        long todaySteps = prefs.getLong("today_steps", 0);

        long baseSteps = prefs.getLong("base_steps", -1);

        long lastRawSteps = prefs.getLong("last_raw", -1);

        SharedPreferences.Editor editor = prefs.edit();

        if (!today.equals(savedDate)) {

            if (!savedDate.isEmpty()) {
                editor.putLong(
                        "history_" + savedDate,
                        todaySteps
                );
            }

            todaySteps = 0;
            baseSteps = -1;
            lastRawSteps = -1;

            editor.putString("date", today);
        }

        if (baseSteps < 0 || lastRawSteps < 0
                || rawSteps < lastRawSteps) {

            baseSteps = rawSteps - todaySteps;
        }

        if (lastRawSteps < 0 || rawSteps >= lastRawSteps) {

            todaySteps = Math.max(
                    todaySteps,
                    rawSteps - baseSteps
            );
        }

        editor.putLong("today_steps", todaySteps);
        editor.putLong("base_steps", baseSteps);
        editor.putLong("last_raw", rawSteps);

        editor.apply();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action required.
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
