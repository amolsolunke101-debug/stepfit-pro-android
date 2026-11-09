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
        android.content.SharedPreferences routePrefs = getSharedPreferences("StepFitData", MODE_PRIVATE);
        if (ACTION_STOP.equals(action)) {
            // Ignore a Stop tap when no route is active. Without this guard,
            // an idle stop could create a misleading empty route-history entry.
            if (startedAt <= 0 && !routePrefs.getBoolean("route_tracking", false)) {
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }
            // ACTION_STOP can create a fresh service instance after Android
            // has killed the recording service. Restore the persisted route
            // before saving so a stop tap cannot overwrite it with an empty route.
            if (startedAt <= 0 && routePrefs.getBoolean("route_tracking", false)) {
                meters = Math.max(0, routePrefs.getFloat("route_live_km", 0f) * 1000.0);
                startedAt = routePrefs.getLong("route_started_at", System.currentTimeMillis());
                String saved = routePrefs.getString("route_live_points", "");
                points.clear();
                if (saved != null && !saved.isEmpty()) {
                    for (String point : saved.split(";")) {
                        if (!point.isEmpty() && points.size() < 12000) points.add(point);
                    }
                }
            }
            saveAndStop();
            return START_NOT_STICKY;
        }
        // A sticky restart has a null Intent. Resume only a route that was
        // already active; never create a new route from an unsolicited restart.
        if (intent == null && !routePrefs.getBoolean("route_tracking", false)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (action != null && !ACTION_START.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If permission was revoked while a route was active, restore its
            // locally persisted data before saving so the partial route is not lost.
            if (routePrefs.getBoolean("route_tracking", false)) {
                meters = Math.max(0, routePrefs.getFloat("route_live_km", 0f) * 1000.0);
                startedAt = routePrefs.getLong("route_started_at", System.currentTimeMillis());
                String saved = routePrefs.getString("route_live_points", "");
                points.clear();
                if (saved != null && !saved.isEmpty()) {
                    for (String point : saved.split(";")) {
                        if (!point.isEmpty() && points.size() < 12000) points.add(point);
                    }
                }
                saveAndStop();
            } else {
                stopSelf();
            }
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, notification("GPS route is being recorded"));
        if (callback == null) {
            // If a route is already active, a repeated Start tap or service
            // recreation must continue it instead of wiping its recorded path.
            if (routePrefs.getBoolean("route_tracking", false)) resumeUpdates();
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
        // Use high accuracy only when the user grants precise location.
        // Approximate-only permission still supports a lower-accuracy route
        // instead of silently producing few or no points.
        boolean preciseLocation = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        int priority = preciseLocation ? Priority.PRIORITY_HIGH_ACCURACY
                : Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        LocationRequest request = new LocationRequest.Builder(priority, 3000)
                .setMinUpdateDistanceMeters(2).build();
        callback = new LocationCallback() {
            @Override public void onLocationResult(LocationResult result) {
                if (result == null) return;
                for (Location loc : result.getLocations()) {
                    // Ignore low-quality fixes so GPS drift while standing still
                    // does not inflate the recorded distance.
                    if (loc.hasAccuracy() && loc.getAccuracy() > 50f) continue;
                    if (previous != null) {
                        float delta = previous.distanceTo(loc);
                        // Reject large jumps; they are usually GPS drift or a lost fix.
                        if (delta >= 0 && delta < 100f) meters += delta;
                    }
                    previous = loc;
                    if (points.size() < 12000) {
                        long pointTime = loc.getTime() > 0 ? loc.getTime() : System.currentTimeMillis();
                        points.add(loc.getLatitude() + "," + loc.getLongitude() + "," + pointTime);
                    }                }
                getSharedPreferences("StepFitData", MODE_PRIVATE).edit()
                        .putFloat("route_live_km", (float)(meters / 1000.0))
                        .putLong("route_live_seconds", (System.currentTimeMillis() - startedAt) / 1000)
                        .putString("route_live_points", android.text.TextUtils.join(";", points)).apply();
            }
        };
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                    .addOnFailureListener(error -> {
                        // The Play Services request is asynchronous. Handle failures
                        // here so the UI cannot remain stuck in a false "recording" state.
                        if (callback != null) {
                            client.removeLocationUpdates(callback);
                            callback = null;
                        }
                        // Preserve any points collected before the provider failed.
                        // Saving also clears the active flag and keeps the partial route
                        // available in local history instead of silently discarding it.
                        saveAndStop();
                    });
        } catch (SecurityException e) {
            // Keep a recoverable partial route if permission changes while recording.
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

    // SharedPreferences is not designed for an unlimited collection of large GPS tracks.
    // Keep the full live track while recording, but compact completed tracks so route history
    // remains lightweight and does not create oversized preference files.
    private String compactPointsForHistory() {
        final int maxSavedPoints = 3000;
        if (points.size() <= maxSavedPoints) return android.text.TextUtils.join(";", points);
        ArrayList<String> compact = new ArrayList<>(maxSavedPoints);
        double step = (points.size() - 1.0) / (maxSavedPoints - 1.0);
        for (int i = 0; i < maxSavedPoints; i++) {
            int index = (int)Math.round(i * step);
            compact.add(points.get(Math.min(points.size() - 1, index)));
        }
        return android.text.TextUtils.join(";", compact);
    }

    private void saveAndStop() {
        if (callback != null) {
            client.removeLocationUpdates(callback);
            callback = null;
        }
        long duration = startedAt > 0 ? Math.max(0, (System.currentTimeMillis() - startedAt) / 1000) : 0;
        String joinedPoints = compactPointsForHistory();
        android.content.SharedPreferences prefs = getSharedPreferences("StepFitData", MODE_PRIVATE);
        prefs.edit()
                .putFloat("last_route_km", (float)(meters / 1000.0))
                .putLong("last_route_duration", duration)
                .putString("last_route_points", joinedPoints)
                .putBoolean("route_tracking", false).apply();

        // Keep the latest 10 completed routes locally on the device (new route + 9 older routes).
        try {
            org.json.JSONArray history = new org.json.JSONArray(prefs.getString("route_history", "[]"));
            org.json.JSONObject route = new org.json.JSONObject();
            route.put("savedAt", System.currentTimeMillis());
            route.put("distanceKm", meters / 1000.0);
            route.put("durationSeconds", duration);
            route.put("points", joinedPoints);
            org.json.JSONArray updated = new org.json.JSONArray();
            updated.put(route);
            for (int i = 0; i < history.length() && i < 9; i++) updated.put(history.getJSONObject(i));
            prefs.edit().putString("route_history", updated.toString()).apply();
        } catch (org.json.JSONException ignored) {
            // Keep the most recent route even if an older history record is malformed.
        }
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() {
        if (callback != null) client.removeLocationUpdates(callback);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}