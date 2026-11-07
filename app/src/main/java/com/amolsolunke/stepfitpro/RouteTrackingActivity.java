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
import android.graphics.Canvas;
import android.graphics.Paint;
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

    private void showRouteHistory() {
        android.content.SharedPreferences prefs = getSharedPreferences("StepFitData", MODE_PRIVATE);
        try {
            org.json.JSONArray history = new org.json.JSONArray(prefs.getString("route_history", "[]"));
            if (history.length() == 0) {
                Toast.makeText(this, "No saved GPS routes yet.", Toast.LENGTH_LONG).show();
                return;
            }
            String[] labels = new String[history.length()];
            for (int i = 0; i < history.length(); i++) {
                org.json.JSONObject route = history.getJSONObject(i);
                java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                String date = format.format(new java.util.Date(route.optLong("savedAt", 0)));
                labels[i] = date + " • " + String.format(Locale.getDefault(), "%.2f km", route.optDouble("distanceKm", 0));
            }
            new AlertDialog.Builder(this).setTitle("Saved GPS Routes")
                .setItems(labels, (dialog, which) -> {
                    try {
                        org.json.JSONObject route = history.getJSONObject(which);
                        String points = route.optString("points", "");
                        double km = route.optDouble("distanceKm", 0);
                        long seconds = route.optLong("durationSeconds", 0);
                        LinearLayout detail = new LinearLayout(this);
                        detail.setOrientation(LinearLayout.VERTICAL);
                        TextView summary = new TextView(this);
                        summary.setText(String.format(Locale.getDefault(),
                            "Distance: %.2f km\nDuration: %02d:%02d:%02d\nGPS points: %d",
                            km, seconds/3600, (seconds%3600)/60, seconds%60,
                            points.isEmpty() ? 0 : points.split(";").length));
                        summary.setTextColor(Color.WHITE);
                        summary.setPadding(20, 12, 20, 12);
                        detail.addView(summary);
                        detail.addView(new RoutePreview(this, points), new LinearLayout.LayoutParams(-1, dp(220)));
                        detail.setBackgroundColor(Color.rgb(10,18,38));
                        new AlertDialog.Builder(this).setTitle("Route Preview").setView(detail)
                            .setPositiveButton("Close", null).show();
                    } catch (org.json.JSONException ignored) {}
                }).setPositiveButton("Close", null).show();
        } catch (org.json.JSONException e) {
            Toast.makeText(this, "Could not read route history.", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + .5f);
    }

    private class RoutePreview extends android.view.View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final java.util.ArrayList<float[]> coordinates = new java.util.ArrayList<>();
        RoutePreview(android.content.Context context, String raw) {
            super(context);
            if (raw != null && !raw.isEmpty()) {
                for (String pair : raw.split(";")) {
                    String[] parts = pair.split(",");
                    if (parts.length != 2) continue;
                    try { coordinates.add(new float[]{Float.parseFloat(parts[0]), Float.parseFloat(parts[1])}); }
                    catch (NumberFormatException ignored) {}
                }
            }
            setBackgroundColor(Color.rgb(18,30,50));
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (coordinates.size() < 2) {
                paint.setColor(Color.LTGRAY); paint.setTextSize(dp(14));
                canvas.drawText("Not enough GPS points for route preview", dp(12), getHeight()/2f, paint);
                return;
            }
            float minLat=Float.MAX_VALUE,maxLat=-Float.MAX_VALUE,minLon=Float.MAX_VALUE,maxLon=-Float.MAX_VALUE;
            for (float[] p : coordinates) {
                minLat=Math.min(minLat,p[0]); maxLat=Math.max(maxLat,p[0]);
                minLon=Math.min(minLon,p[1]); maxLon=Math.max(maxLon,p[1]);
            }
            float pad=dp(18), w=Math.max(1,getWidth()-2*pad), h=Math.max(1,getHeight()-2*pad);
            float latSpan=Math.max(0.00001f,maxLat-minLat), lonSpan=Math.max(0.00001f,maxLon-minLon);
            paint.setColor(Color.rgb(0,220,180)); paint.setStrokeWidth(dp(3));
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
            android.graphics.Path path = new android.graphics.Path();
            for (int i=0;i<coordinates.size();i++) {
                float[] p=coordinates.get(i);
                float x=pad+(p[1]-minLon)/lonSpan*w;
                float y=getHeight()-pad-(p[0]-minLat)/latSpan*h;
                if(i==0) path.moveTo(x,y); else path.lineTo(x,y);
            }
            canvas.drawPath(path,paint);
            paint.setStyle(Paint.Style.FILL);
            float[] first=coordinates.get(0), last=coordinates.get(coordinates.size()-1);
            canvas.drawCircle(pad+(first[1]-minLon)/lonSpan*w,getHeight()-pad-(first[0]-minLat)/latSpan*h,dp(5),paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(pad+(last[1]-minLon)/lonSpan*w,getHeight()-pad-(last[0]-minLat)/latSpan*h,dp(5),paint);
        }
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
        Button history = new Button(this);
        history.setText("View Saved Route History");
        history.setOnClickListener(v -> showRouteHistory());
        root.addView(history);
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