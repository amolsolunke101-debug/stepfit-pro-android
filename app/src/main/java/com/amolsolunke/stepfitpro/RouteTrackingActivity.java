package com.amolsolunke.stepfitpro;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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

    private String language = "en";
    private static final String[][] ROUTE_TEXT = {
        {"Status  %s\\nDistance  %.2f km\\nDuration  %02d:%02d:%02d\\nAverage speed  %.1f km/h\\nGPS points  %d","Recording","Stopped","No GPS routes have been saved yet.","Saved GPS routes","Date: %s\\nDistance: %.2f km\\nDuration: %02d:%02d:%02d\\nGPS points: %d","Route details","Close","Delete saved route?","This route will be removed from this device. This action cannot be undone.","Cancel","Route deleted from this device.","Could not delete route.","This route has no GPS points to export.","At least two valid GPS points are required for GPX export.","Could not create GPX file.","Not enough GPS points to display the route","Start GPS tracking","Stop and save route","View saved routes","Back","GPS Route Tracking","There is no active GPS route to stop.","GPS tracking could not start. Check location permission and try again.","Could not stop GPS tracking. Please try again.","Location permission is required for GPS route tracking.","Could not read route history.","Date: %s\nDistance: %.2f km\nDuration: %02d:%02d:%02d\nGPS points: %d","Share GPX","Delete","Share GPX route"},
        {"स्थिती  %s\nअंतर  %.2f किमी\nकालावधी  %02d:%02d:%02d\nसरासरी वेग  %.1f किमी/तास\nGPS पॉइंट्स  %d","रेकॉर्डिंग सुरू","बंद","अद्याप GPS मार्ग जतन केलेले नाहीत.","जतन केलेले GPS मार्ग","दिनांक: %s\nअंतर: %.2f किमी\nकालावधी: %02d:%02d:%02d\nGPS पॉइंट्स: %d","मार्गाचा तपशील","बंद करा","जतन केलेला मार्ग हटवायचा?","हा मार्ग या डिव्हाइसमधून हटवला जाईल. ही कृती पूर्ववत करता येणार नाही.","रद्द करा","मार्ग डिव्हाइसमधून हटवला.","मार्ग हटवता आला नाही.","या मार्गात निर्यात करण्यासाठी GPS पॉइंट्स नाहीत.","GPX निर्यातीसाठी किमान दोन वैध GPS पॉइंट्स आवश्यक आहेत.","GPX फाइल तयार करता आली नाही.","मार्ग दाखवण्यासाठी पुरेसे GPS पॉइंट्स नाहीत","GPS ट्रॅकिंग सुरू करा","थांबवा आणि मार्ग जतन करा","जतन केलेले मार्ग पहा","मागे","GPS मार्ग ट्रॅकिंग","थांबवण्यासाठी सक्रिय GPS मार्ग नाही.","GPS ट्रॅकिंग सुरू झाले नाही. लोकेशन परवानगी तपासून पुन्हा प्रयत्न करा.","GPS ट्रॅकिंग थांबवता आले नाही. पुन्हा प्रयत्न करा.","GPS मार्ग ट्रॅकिंगसाठी लोकेशन परवानगी आवश्यक आहे.","मार्ग इतिहास वाचता आला नाही.","दिनांक: %s\nअंतर: %.2f किमी\nकालावधी: %02d:%02d:%02d\nGPS पॉइंट्स: %d","GPX शेअर करा","हटवा","GPX मार्ग शेअर करा"},
        {"स्थिति  %s\nदूरी  %.2f किमी\nअवधि  %02d:%02d:%02d\nऔसत गति  %.1f किमी/घंटा\nGPS पॉइंट  %d","रिकॉर्डिंग जारी","बंद","अभी तक कोई GPS मार्ग सेव नहीं है।","सेव किए गए GPS मार्ग","दिनांक: %s\nदूरी: %.2f किमी\nअवधि: %02d:%02d:%02d\nGPS पॉइंट: %d","मार्ग का विवरण","बंद करें","सेव किया गया मार्ग हटाएँ?","यह मार्ग इस डिवाइस से हट जाएगा। इसे वापस नहीं लाया जा सकता।","रद्द करें","मार्ग इस डिवाइस से हटा दिया गया।","मार्ग हटाया नहीं जा सका।","इस मार्ग में निर्यात के लिए GPS पॉइंट नहीं हैं।","GPX निर्यात के लिए कम से कम दो वैध GPS पॉइंट आवश्यक हैं।","GPX फ़ाइल नहीं बनाई जा सकी।","मार्ग दिखाने के लिए पर्याप्त GPS पॉइंट नहीं हैं","GPS ट्रैकिंग शुरू करें","रोकें और मार्ग सेव करें","सेव किए गए मार्ग देखें","वापस","GPS मार्ग ट्रैकिंग","रोकने के लिए कोई सक्रिय GPS मार्ग नहीं है।","GPS ट्रैकिंग शुरू नहीं हो सकी। लोकेशन अनुमति जाँचें और फिर प्रयास करें।","GPS ट्रैकिंग रोक नहीं सके। कृपया फिर प्रयास करें।","GPS मार्ग ट्रैकिंग के लिए लोकेशन अनुमति आवश्यक है।","मार्ग इतिहास नहीं पढ़ सके।","दिनांक: %s\nदूरी: %.2f किमी\nअवधि: %02d:%02d:%02d\nGPS पॉइंट: %d","GPX साझा करें","हटाएँ","GPX मार्ग साझा करें"}
    };
    private String tr(int index) {
        int lang = "mr".equals(language) ? 1 : ("hi".equals(language) ? 2 : 0);
        return ROUTE_TEXT[lang][index];
    }
    private TextView stats;
    private CheckBox cloudBackup;
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
        long seconds = active
                ? Math.max(0, (System.currentTimeMillis() - p.getLong("route_started_at", System.currentTimeMillis())) / 1000)
                : p.getLong("last_route_duration", 0);
        String raw = active ? p.getString("route_live_points", "") : p.getString("last_route_points", "");
        int count = (raw == null || raw.isEmpty()) ? 0 : raw.split(";").length;
        double speed = seconds > 0 ? km / (seconds / 3600.0) : 0;
        stats.setText(String.format(Locale.US, tr(0), active ? tr(1) : tr(2), km, seconds/3600, (seconds%3600)/60, seconds%60, speed, count));
    }

    private void showRouteHistory() {
        android.content.SharedPreferences prefs = getSharedPreferences("StepFitData", MODE_PRIVATE);
        try {
            org.json.JSONArray history = new org.json.JSONArray(prefs.getString("route_history", "[]"));
            if (history.length() == 0) {
                Toast.makeText(this, tr(3), Toast.LENGTH_LONG).show();
                return;
            }
            String[] labels = new String[history.length()];
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            for (int i = 0; i < history.length(); i++) {
                org.json.JSONObject route = history.getJSONObject(i);
                String date = format.format(new java.util.Date(route.optLong("savedAt", 0)));
                labels[i] = date + " • " + String.format(Locale.getDefault(), "%.2f km", route.optDouble("distanceKm", 0));
            }
            new AlertDialog.Builder(this).setTitle(tr(4))
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
                            tr(27),
                            format.format(new java.util.Date(route.optLong("savedAt", 0))),
                            km, seconds/3600, (seconds%3600)/60, seconds%60,
                            points.isEmpty() ? 0 : points.split(";").length));
                        summary.setTextColor(Color.WHITE);
                        summary.setPadding(20, 12, 20, 12);
                        detail.addView(summary);
                        detail.addView(new RoutePreview(this, points), new LinearLayout.LayoutParams(-1, dp(220)));
                        detail.setBackgroundColor(Color.rgb(10,18,38));
                        new AlertDialog.Builder(this).setTitle(tr(6)).setView(detail)
                            .setPositiveButton(tr(7), null)
                            .setNeutralButton(tr(28), (d, w) -> shareRouteAsGpx(route))
                            .setNegativeButton(tr(29), (d, w) -> confirmDeleteSavedRoute(which))
                            .show();
                    } catch (org.json.JSONException ignored) {}
                }).setPositiveButton(tr(7), null).show();
        } catch (org.json.JSONException e) {
            Toast.makeText(this, tr(26), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeleteSavedRoute(int index) {
        new AlertDialog.Builder(this)
                .setTitle(tr(8))
                .setMessage(tr(9))
                .setNegativeButton(tr(10), null)
                .setPositiveButton(tr(29), (dialog, which) -> deleteSavedRoute(index))
                .show();
    }

    private void deleteSavedRoute(int index) {
        android.content.SharedPreferences prefs = getSharedPreferences("StepFitData", MODE_PRIVATE);
        try {
            org.json.JSONArray history = new org.json.JSONArray(prefs.getString("route_history", "[]"));
            if (index < 0 || index >= history.length()) return;
            org.json.JSONArray updated = new org.json.JSONArray();
            for (int i = 0; i < history.length(); i++) {
                if (i != index) updated.put(history.getJSONObject(i));
            }
            prefs.edit().putString("route_history", updated.toString()).apply();
            Toast.makeText(this, tr(11), Toast.LENGTH_SHORT).show();
            showRouteHistory();
        } catch (org.json.JSONException e) {
            Toast.makeText(this, tr(12), Toast.LENGTH_LONG).show();
        }
    }

    private void shareRouteAsGpx(org.json.JSONObject route) {
        String raw = route.optString("points", "");
        if (raw.isEmpty()) {
            Toast.makeText(this, tr(13), Toast.LENGTH_LONG).show();
            return;
        }
        StringBuilder gpx = new StringBuilder();
        java.text.SimpleDateFormat gpxTime = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        gpxTime.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String savedTime = gpxTime.format(new java.util.Date(route.optLong("savedAt", System.currentTimeMillis())));
        gpx.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
           .append("<gpx version=\"1.1\" creator=\"StepFit PRO\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
           .append("<metadata><name>StepFit PRO route</name><time>").append(savedTime)
           .append("</time><desc>Distance: ")
           .append(String.format(Locale.US, "%.2f km; Duration: %d seconds", route.optDouble("distanceKm", 0), route.optLong("durationSeconds", 0)))
           .append("</desc></metadata>\n")
           .append("<trk><name>StepFit PRO route</name><trkseg>\n");
        int validPoints = 0;
        for (String pair : raw.split(";")) {
            String[] parts = pair.split(",", -1);
            if (parts.length < 2) continue;
            try {
                double lat = Double.parseDouble(parts[0]);
                double lon = Double.parseDouble(parts[1]);
                if (Double.isFinite(lat) && Double.isFinite(lon) && Math.abs(lat) <= 90 && Math.abs(lon) <= 180) {
                    gpx.append(String.format(Locale.US, "<trkpt lat=\"%.7f\" lon=\"%.7f\">", lat, lon));
                    if (parts.length >= 3) {
                        try {
                            long pointMillis = Long.parseLong(parts[2]);
                            if (pointMillis > 0) gpx.append("<time>")
                                    .append(gpxTime.format(new java.util.Date(pointMillis)))
                                    .append("</time>");
                        } catch (NumberFormatException ignored) {}
                    }
                    gpx.append("</trkpt>\n");
                    validPoints++;
                }
            } catch (NumberFormatException ignored) {}
        }
        gpx.append("</trkseg></trk></gpx>");
        if (validPoints < 2) {
            Toast.makeText(this, tr(14), Toast.LENGTH_LONG).show();
            return;
        }
        try {
            java.io.File exportDir = new java.io.File(getCacheDir(), "shared_routes");
            if (!exportDir.exists() && !exportDir.mkdirs()) throw new java.io.IOException("Could not create export folder");
            java.io.File exportFile = new java.io.File(exportDir, "stepfit-route-" + System.currentTimeMillis() + ".gpx");
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(exportFile)) {
                output.write(gpx.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", exportFile);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/gpx+xml");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.setClipData(android.content.ClipData.newUri(getContentResolver(), "StepFit PRO GPX route", uri));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, tr(30)));
        } catch (Exception e) {
            Toast.makeText(this, tr(15), Toast.LENGTH_LONG).show();
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
                    String[] parts = pair.split(",", -1);
                    if (parts.length < 2) continue;
                    try {
                        float lat = Float.parseFloat(parts[0]);
                        float lon = Float.parseFloat(parts[1]);
                        if (Float.isFinite(lat) && Float.isFinite(lon)
                                && Math.abs(lat) <= 90f && Math.abs(lon) <= 180f) {
                            coordinates.add(new float[]{lat, lon});
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            setBackgroundColor(Color.rgb(18,30,50));
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (coordinates.size() < 2) {
                paint.setColor(Color.LTGRAY); paint.setTextSize(dp(14));
                canvas.drawText(tr(16), dp(12), getHeight()/2f, paint);
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

    private String batteryTipTitle() {
        if ("mr".equals(language)) return "बॅटरी आणि बॅकग्राउंड सेटिंग्ज";
        if ("hi".equals(language)) return "बैटरी और बैकग्राउंड सेटिंग्स";
        return "Battery and background settings";
    }

    private String batteryTipMessage() {
        if ("mr".equals(language)) return "स्क्रीन लॉक केल्यावर GPS ट्रॅकिंग थांबत असल्यास Settings > Battery (किंवा App battery management) मध्ये StepFit PRO साठी बॅकग्राउंड वापरास परवानगी द्या. फोननुसार मेनूची नावे बदलू शकतात.";
        if ("hi".equals(language)) return "यदि स्क्रीन लॉक होने पर GPS ट्रैकिंग रुकती है, तो Settings > Battery (या App battery management) में StepFit PRO के लिए बैकग्राउंड गतिविधि की अनुमति दें। फोन के अनुसार मेनू के नाम अलग हो सकते हैं।";
        return "If GPS tracking stops when the screen is locked, open Settings > Battery (or App battery management) and allow background activity for StepFit PRO. Menu names vary by phone and software version.";
    }

    private void start() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 71);
            return;
        }
        android.content.SharedPreferences prefs = getSharedPreferences("StepFitData", MODE_PRIVATE);
        boolean isOppoFamily = "oppo".equalsIgnoreCase(Build.MANUFACTURER)
                || "realme".equalsIgnoreCase(Build.MANUFACTURER)
                || "oneplus".equalsIgnoreCase(Build.MANUFACTURER);
        if (isOppoFamily && !prefs.getBoolean("gps_battery_tip_shown", false)) {
            String cancel = "mr".equals(language) ? "रद्द करा" : ("hi".equals(language) ? "रद्द करें" : "Cancel");
            String proceed = "mr".equals(language) ? "पुढे सुरू ठेवा" : ("hi".equals(language) ? "जारी रखें" : "Continue");
            new AlertDialog.Builder(this).setTitle(batteryTipTitle()).setMessage(batteryTipMessage())
                    .setNegativeButton(cancel, (dialog, which) -> { })
                    .setPositiveButton(proceed, (dialog, which) -> {
                        prefs.edit().putBoolean("gps_battery_tip_shown", true).apply();
                        startGpsService();
                    }).show();
            return;
        }
        startGpsService();
    }

    private void startGpsService() {
        Intent service = new Intent(this, RouteTrackingService.class).setAction(RouteTrackingService.ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(this, service);
            else startService(service);
            handler.removeCallbacks(refresher);
            handler.post(refresher);
        } catch (Exception e) {
            Toast.makeText(this, tr(23), Toast.LENGTH_LONG).show();
        }
    }


    private void backupLatestRouteIfEnabled() {
        android.content.SharedPreferences prefs = getSharedPreferences("StepFitData", MODE_PRIVATE);
        if (!prefs.getBoolean("route_cloud_backup_opt_in", false)) return;
        try {
            org.json.JSONArray history = new org.json.JSONArray(prefs.getString("route_history", "[]"));
            if (history.length() == 0) return;
            org.json.JSONObject route = history.getJSONObject(0);
            long savedAt = route.optLong("savedAt", 0);
            if (savedAt <= 0) return;
            StepFitCloud.savePrivateRoute("r" + savedAt, savedAt,
                    route.optDouble("distanceKm", 0), route.optLong("durationSeconds", 0),
                    route.optString("points", ""),
                    (ok, message) -> Toast.makeText(this,
                            ok ? message : ("Cloud backup failed: " + message),
                            Toast.LENGTH_LONG).show());
        } catch (org.json.JSONException ignored) { }
    }

    private void stop() {
        android.content.SharedPreferences prefs = getSharedPreferences("StepFitData", MODE_PRIVATE);
        if (!prefs.getBoolean("route_tracking", false)) {
            Toast.makeText(this, tr(22), Toast.LENGTH_SHORT).show();
            refresh();
            return;
        }
        Intent service = new Intent(this, RouteTrackingService.class).setAction(RouteTrackingService.ACTION_STOP);
        try {
            startService(service);
            handler.removeCallbacks(refresher);
            handler.postDelayed(() -> { refresh(); backupLatestRouteIfEnabled(); }, 1200);
        } catch (Exception e) {
            Toast.makeText(this, tr(24), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 71) {
            boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                start();
            } else {
                Toast.makeText(this, tr(25), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        language = getSharedPreferences("StepFitData", MODE_PRIVATE).getString("language", "en");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(24,24,24,24);
        root.setBackgroundColor(Color.rgb(10,18,38));
        TextView title = new TextView(this);
        title.setText(tr(21));
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
        go.setText(tr(17));
        go.setOnClickListener(v -> start());
        root.addView(go);
        Button end = new Button(this);
        end.setText(tr(18));
        end.setOnClickListener(v -> stop());
        root.addView(end);

        cloudBackup = new CheckBox(this);
        cloudBackup.setText("mr".equals(language)
                ? "माझ्या संमतीने GPS मार्गांचा खाजगी Cloud Backup सुरू करा"
                : ("hi".equals(language)
                    ? "मेरी सहमति से GPS मार्गों का निजी Cloud Backup चालू करें"
                    : "Enable private GPS route cloud backup (my consent)"));
        cloudBackup.setTextColor(Color.WHITE);
        cloudBackup.setChecked(getSharedPreferences("StepFitData", MODE_PRIVATE)
                .getBoolean("route_cloud_backup_opt_in", false));
        cloudBackup.setOnCheckedChangeListener((button, checked) ->
                getSharedPreferences("StepFitData", MODE_PRIVATE).edit()
                        .putBoolean("route_cloud_backup_opt_in", checked).apply());
        root.addView(cloudBackup);
        Button history = new Button(this);
        history.setText(tr(19));
        history.setOnClickListener(v -> showRouteHistory());
        root.addView(history);
        Button back = new Button(this);
        back.setText(tr(20));
        back.setOnClickListener(v -> finish());
        root.addView(back);
        setContentView(root);
        refresh();
        if (getSharedPreferences("StepFitData", MODE_PRIVATE).getBoolean("route_tracking", false))
            handler.post(refresher);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(refresher);
        super.onDestroy();
    }
}