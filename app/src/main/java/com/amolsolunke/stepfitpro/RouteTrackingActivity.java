package com.amolsolunke.stepfitpro;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import android.graphics.Color;
import android.view.Gravity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Locale;

public class RouteTrackingActivity extends Activity {
    private TextView stats;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresher = new Runnable() {
        @Override public void run() {
            refresh();
            if (getSharedPreferences("StepFitData", MODE_PRIVATE).getBoolean("route_tracking", false))
                handler.postDelayed(this, 1000);
        }
    };

    private void refresh() {
        android.content.SharedPreferences p = getSharedPreferences("StepFitData", MODE_PRIVATE);
        boolean active = p.getBoolean("route_tracking", false);
        double km = active ? p.getFloat("route_live_km", 0f) : p.getFloat("last_route_km", 0f);
        long seconds = active ? p.getLong("route_live_seconds", 0) : p.getLong("last_route_duration", 0);
        String raw = active ? p.getString("route_live_points", "") : p.getString("last_route_points", "");
        int count = (raw == null || raw.isEmpty()) ? 0 : raw.split(";").length;
        double speed = seconds > 0 ? km / (seconds / 3600.0) : 0;
        stats.setText(String.format(Locale.US, "Status  %s\nDistance  %.2f km\nDuration  %02d:%02d:%02d\nAverage speed  %.1f km/h\nGPS points  %d",
                active ? "Recording" : "Stopped", km, seconds/3600, (seconds%3600)/60, seconds%60, speed, count));
    }

    private void start() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 71);
            return;
        }
        Intent service = new Intent(this, RouteTrackingService.class).setAction(RouteTrackingService.ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(this, service);
            else startService(service);
            handler.removeCallbacks(refresher);
            handler.post(refresher);
        } catch (Exception e) {
            Toast.makeText(this, "GPS tracking could not start. Check location permission and try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void stop() {
        Intent service = new Intent(this, RouteTrackingService.class).setAction(RouteTrackingService.ACTION_STOP);
        startService(service);
        handler.removeCallbacks(refresher);
        handler.postDelayed(this::refresh, 500);
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(24,24,24,24);
        root.setBackgroundColor(Color.rgb(10,18,38));
        TextView title = new TextView(this);
        title.setText("GPS Route Tracking");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        stats = new TextView(this);
        stats.setTextColor(Color.WHITE);
        stats.setTextSize(20);
        stats.setPadding(8,36,8,36);
        root.addView(stats);
        Button go = new Button(this);
        go.setText("Start GPS Tracking");
        go.setOnClickListener(v -> start());
        root.addView(go);
        Button end = new Button(this);
        end.setText("Stop and Save Route");
        end.setOnClickListener(v -> stop());
        root.addView(end);
        Button back = new Button(this);
        back.setText("Back");
        back.setOnClickListener(v -> finish());
        root.addView(back);
        setContentView(root);
        refresh();
        if (getSharedPreferences("StepFitData", MODE_PRIVATE).getBoolean("route_tracking", false))
            handler.post(refresher);
    }

    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) {
        super.onRequestPermissionsResult(r,p,g);
        if (r == 71) {
            boolean locationGranted = false;
            for (int grant : g) {
                if (grant == PackageManager.PERMISSION_GRANTED) {
                    locationGranted = true;
                    break;
                }
            }
            if (locationGranted) start();
            else Toast.makeText(this, "Location permission is required to track your route.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(refresher);
        super.onDestroy();
    }
}