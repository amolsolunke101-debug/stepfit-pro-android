package com.amolsolunke.stepfitpro;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.*;
import java.util.ArrayList;
import java.util.Locale;

public class RouteTrackingService extends Service {
    public static final String ACTION_START = "com.amolsolunke.stepfitpro.ROUTE_START";
    public static final String ACTION_STOP = "com.amolsolunke.stepfitpro.ROUTE_STOP";
    private static final String CHANNEL = "stepfit_route_tracking";
    private static final int NOTIFICATION_ID = 7302;
    private FusedLocationProviderClient client;
    private LocationCallback callback;
    private Location previous;
    private final ArrayList<String> points = new ArrayList<>();
    private double meters;
    private long startedAt;

    @Override public void onCreate() {
        super.onCreate();
        client = LocationServices.getFusedLocationProviderClient(this);
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            saveAndStop();
            return START_NOT_STICKY;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, notification("GPS route is being recorded"));
        if (callback == null) {
            if (action == null && getSharedPreferences("StepFitData", MODE_PRIVATE).getBoolean("route_tracking", false)) resumeUpdates();
            else beginUpdates();
        }
        return START_STICKY;
    }

    private void resumeUpdates() {
        android.content.SharedPreferences prefs = getSharedPreferences("StepFitData", MODE_PRIVATE);
        meters = Math.max(0, prefs.getFloat("route_live_km", 0f) * 1000.0);
        startedAt = prefs.getLong("route_started_at", System.currentTimeMillis());
        points.clear();
        String saved = prefs.getString("route_live_points", "");
        if (saved != null && !saved.isEmpty()) for (String point : saved.split(";")) if (!point.isEmpty() && points.size() < 12000) points.add(point);
        previous = null;
        requestUpdates();
    }

    private void beginUpdates() {
        meters = 0;
        points.clear();
        previous = null;
        startedAt = System.currentTimeMillis();
        getSharedPreferences("StepFitData", MODE_PRIVATE).edit()
                .putBoolean("route_tracking", true).putLong("route_started_at", startedAt)
                .putFloat("route_live_km", 0f).putLong("route_live_seconds", 0)
                .putString("route_live_points", "").apply();
        requestUpdates();
    }

    private void requestUpdates() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateDistanceMeters(2).build();
        callback = new LocationCallback() {
            @Override public void onLocationResult(LocationResult result) {
                if (result == null) return;
                for (Location loc : result.getLocations()) {
                    if (previous != null) {
                        float delta = previous.distanceTo(loc);
                        if (delta >= 0 && delta < 250) meters += delta;
                    }
                    previous = loc;
                    if (points.size() < 12000) points.add(loc.getLatitude() + "," + loc.getLongitude());
                }
                getSharedPreferences("StepFitData", MODE_PRIVATE).edit()
                        .putFloat("route_live_km", (float)(meters / 1000.0))
                        .putLong("route_live_seconds", (System.currentTimeMillis() - startedAt) / 1000)
                        .putString("route_live_points", android.text.TextUtils.join(";", points)).apply();
            }
        };
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper());
        } catch (SecurityException e) {
            saveAndStop();
        }
    }

    private Notification notification(String message) {
        return new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("StepFit PRO")
                .setContentText(message)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "GPS route tracking", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void saveAndStop() {
        if (callback != null) {
            client.removeLocationUpdates(callback);
            callback = null;
        }
        long duration = startedAt > 0 ? Math.max(0, (System.currentTimeMillis() - startedAt) / 1000) : 0;
        getSharedPreferences("StepFitData", MODE_PRIVATE).edit()
                .putFloat("last_route_km", (float)(meters / 1000.0))
                .putLong("last_route_duration", duration)
                .putString("last_route_points", android.text.TextUtils.join(";", points))
                .putBoolean("route_tracking", false).apply();
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() {
        if (callback != null) client.removeLocationUpdates(callback);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}