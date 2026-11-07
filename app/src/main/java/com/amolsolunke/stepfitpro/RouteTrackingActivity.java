package com.amolsolunke.stepfitpro;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.*;
import android.graphics.Color;
import android.view.Gravity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.*;
import java.util.ArrayList;
import java.util.Locale;

public class RouteTrackingActivity extends Activity {
    private FusedLocationProviderClient client;
    private LocationCallback callback;
    private Location previous;
    private double meters = 0;
    private long startedAt = 0;
    private boolean tracking = false;
    private TextView stats;
    private final ArrayList<String> points = new ArrayList<>();
    private void refresh() {
        double km = meters / 1000.0;
        long seconds = tracking ? (System.currentTimeMillis()-startedAt)/1000 : 0;
        double speed = seconds > 0 ? km/(seconds/3600.0) : 0;
        stats.setText(String.format(Locale.US,"Distance  %.2f km\nDuration  %02d:%02d:%02d\nAverage speed  %.1f km/h\nGPS points  %d",
            km, seconds/3600, (seconds%3600)/60, seconds%60, speed, points.size()));
    }
    private void start() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},71); return;
        }
        meters=0; points.clear(); previous=null; startedAt=System.currentTimeMillis(); tracking=true;
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,3000)
            .setMinUpdateDistanceMeters(2).build();
        callback = new LocationCallback() {
            @Override public void onLocationResult(LocationResult result) {
                if (result == null) return;
                for (Location loc : result.getLocations()) {
                    if (previous != null) {
                        float delta=previous.distanceTo(loc);
                        if (delta < 250) meters += delta;
                    }
                    previous=loc;
                    points.add(loc.getLatitude()+","+loc.getLongitude());
                }
                refresh();
            }
        };
        client.requestLocationUpdates(request,callback, Looper.getMainLooper());
        refresh();
    }
    private void stop() {
        tracking=false;
        if (callback != null) client.removeLocationUpdates(callback);
        getSharedPreferences("StepFitData",MODE_PRIVATE).edit()
            .putFloat("last_route_km",(float)(meters/1000.0))
            .putLong("last_route_duration",Math.max(0,(System.currentTimeMillis()-startedAt)/1000))
            .putString("last_route_points",android.text.TextUtils.join(";",points)).apply();
        refresh();
        Toast.makeText(this,"Route saved on this device.",Toast.LENGTH_LONG).show();
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        client=LocationServices.getFusedLocationProviderClient(this);
        LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setPadding(24,24,24,24);
        root.setBackgroundColor(Color.rgb(10,18,38));
        TextView title=new TextView(this); title.setText("GPS Route Tracking"); title.setTextColor(Color.WHITE); title.setTextSize(24);
        title.setGravity(Gravity.CENTER); root.addView(title);
        stats=new TextView(this); stats.setTextColor(Color.WHITE); stats.setTextSize(20); stats.setPadding(8,36,8,36); root.addView(stats);
        Button go=new Button(this); go.setText("Start GPS Tracking"); go.setOnClickListener(v->start()); root.addView(go);
        Button end=new Button(this); end.setText("Stop and Save Route"); end.setOnClickListener(v->{if(tracking)stop();}); root.addView(end);
        Button back=new Button(this); back.setText("Back"); back.setOnClickListener(v->finish()); root.addView(back);
        setContentView(root); refresh();
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g) {
        super.onRequestPermissionsResult(r,p,g);
        if(r==71 && g.length>0 && g[0]==PackageManager.PERMISSION_GRANTED) start();
    }
    @Override protected void onDestroy() { if(tracking && callback!=null) client.removeLocationUpdates(callback); super.onDestroy(); }
}