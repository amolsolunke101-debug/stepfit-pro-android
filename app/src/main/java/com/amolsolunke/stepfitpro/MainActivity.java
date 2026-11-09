package com.amolsolunke.stepfitpro;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout root, content, nav;
    private TextView stepsView, distanceView, caloriesView, goalView, progressView, statusView;
    private int page = 0;
    private String language = "en";
    private int theme = 0;
    private int accent = Color.rgb(0, 220, 180);
    private int bg = Color.rgb(10, 18, 38);
    private int card = Color.rgb(27, 42, 68);
    private boolean receiverRegistered = false;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private EditText emailInput, passwordInput;
    private static final int PICK_PROFILE_PHOTO = 2001;

    private final BroadcastReceiver stepsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.amolsolunke.stepfitpro.STEPS_UPDATED".equals(intent.getAction())) {
                if (content != null) showPage(page);
            }
        }
    };

    // Only languages with complete UI translations are selectable.
    // Additional Indian languages will be enabled after their translations are complete.
    private final String[] langCodes = {"en", "mr", "hi"};

    private final String[] langNames = {"English", "मराठी", "हिन्दी"};

    private final String[][] labels = {
      {
        "Home","Progress","History","Charts","Settings","About",
        "Today's steps","Distance","Calories","Daily goal","Set goal",
        "Language","Theme","Save","Cancel","Last 7 days",
        "No step limit — keep walking!","Step sensor available",
        "Step sensor not available","Reset today's count","Weight (kg)",
        "Stride (cm)","Dark navy","Ocean blue","Forest green",
        "Purple","Goal progress","Total steps","Average steps",
        "Highest day","About StepFit PRO","Offline step counter",
        "Choose language","Choose theme","Settings saved",
        "Enter goal (0 = no goal)","Sensor permission needed",
        "Permission required"
      },
      {
        "मुख्यपृष्ठ","प्रगती","इतिहास","आलेख","सेटिंग्ज","माहिती",
        "आजची पावले","अंतर","कॅलरी","दैनिक लक्ष्य","लक्ष्य ठेवा",
        "भाषा","थीम","जतन करा","रद्द करा","मागील ७ दिवस",
        "पावलांची मर्यादा नाही — चालत राहा!","स्टेप सेन्सर उपलब्ध",
        "स्टेप सेन्सर उपलब्ध नाही","आजची मोजणी रीसेट करा",
        "वजन (किलो)","पावलांची लांबी (सेमी)","गडद निळा",
        "समुद्री निळा","हिरवा","जांभळा","लक्ष्य प्रगती",
        "एकूण पावले","सरासरी पावले","सर्वाधिक पावले",
        "StepFit PRO बद्दल","ऑफलाइन स्टेप काउंटर",
        "भाषा निवडा","थीम निवडा","सेटिंग्ज जतन झाल्या",
        "लक्ष्य भरा (० = लक्ष्य नाही)","सेन्सर परवानगी आवश्यक",
        "परवानगी आवश्यक"
      },
      {
        "होम","प्रगति","इतिहास","चार्ट","सेटिंग्स","जानकारी",
        "आज के कदम","दूरी","कैलोरी","दैनिक लक्ष्य","लक्ष्य तय करें",
        "भाषा","थीम","सहेजें","रद्द करें","पिछले 7 दिन",
        "कदमों की कोई सीमा नहीं — चलते रहें!","स्टेप सेंसर उपलब्ध है",
        "स्टेप सेंसर उपलब्ध नहीं है","आज की गिनती रीसेट करें",
        "वज़न (किलो)","कदम की लंबाई (सेमी)","गहरा नीला",
        "समुद्री नीला","वन हरा","बैंगनी","लक्ष्य की प्रगति",
        "कुल कदम","औसत कदम","सबसे अधिक कदम वाला दिन",
        "StepFit PRO के बारे में","ऑफलाइन स्टेप काउंटर",
        "भाषा चुनें","थीम चुनें","सेटिंग्स सहेजी गईं",
        "लक्ष्य दर्ज करें (0 = कोई लक्ष्य नहीं)","सेंसर की अनुमति आवश्यक",
        "अनुमति आवश्यक"
      },
      {
        "Inicio","Progreso","Historial","Gráficos","Ajustes",
        "Acerca de","Pasos de hoy","Distancia","Calorías",
        "Meta diaria","Definir meta","Idioma","Tema","Guardar",
        "Cancelar","Últimos 7 días","Sin límite de pasos",
        "Sensor disponible","Sensor no disponible",
        "Reiniciar pasos de hoy","Peso (kg)","Longitud de paso (cm)",
        "Azul oscuro","Azul océano","Verde bosque","Morado",
        "Progreso de meta","Pasos totales","Promedio","Día máximo",
        "Acerca de StepFit PRO","Contador sin conexión",
        "Elegir idioma","Elegir tema","Ajustes guardados",
        "Meta (0 = sin meta)","Permiso del sensor necesario",
        "Permiso necesario"
      }
    };

    private String t(int i) {
        if ("mr".equals(language)) return labels[1][i];
        if ("hi".equals(language)) return labels[2][i];
        return labels[0][i];
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + .5f);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("StepFitData", MODE_PRIVATE);
        language = prefs.getString("language", "en");
        theme = prefs.getInt("theme", 0);
        applyTheme();
        auth = FirebaseAuth.getInstance();

        // Request activity recognition once, before starting the health foreground service.
        // Step counting remains independent of cloud login.
        if (Build.VERSION.SDK_INT >= 29 &&
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1001);
        } else {
            requestNotificationPermissionIfNeeded();
        }

        try {
            GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
            googleSignInClient = GoogleSignIn.getClient(this, options);
        } catch (Exception ignored) {
            googleSignInClient = null;
        }

        if (auth.getCurrentUser() == null) {
            showAuthScreen();
        } else {
            openMainApp();
        }
    }

    private void openMainApp() {
        buildShell();
        showPage(0);
        startStepService();

        // Restore an existing cloud profile on this device, or create the
        // first cloud profile from locally saved details after sign-in.
        if (auth != null && auth.getCurrentUser() != null) {
            StepFitCloud.loadOrCreateProfile(
                prefs.getString("profile_name", ""),
                prefs.getString("profile_age", ""),
                prefs.getString("profile_gender", ""),
                (ok, name, age, gender) -> runOnUiThread(() -> {
                    if (!ok || isFinishing()) return;
                    String oldName = prefs.getString("profile_name", "");
                    prefs.edit().putString("profile_name", name == null ? "" : name)
                        .putString("profile_age", age == null ? "" : age)
                        .putString("profile_gender", gender == null ? "" : gender).apply();
                    if (!oldName.equals(name == null ? "" : name) && content != null) {
                        buildShell();
                        showPage(page);
                    }
                })
            );
        }
    }

    private void showAuthScreen() {
        LinearLayout screen = column();
        screen.setGravity(Gravity.CENTER_HORIZONTAL);
        screen.setBackgroundColor(bg);
        TextView heading = text("👟 StepFit PRO", 30, accent, true);
        screen.addView(heading, lp(-1, 76));
        screen.addView(text("Sign in to sync your profile", 16, Color.WHITE, false), lp(-1, 42));

        emailInput = new EditText(this);
        emailInput.setSingleLine(true);
        emailInput.setHint("Email address");
        emailInput.setInputType(33);
        emailInput.setTextColor(Color.WHITE);
        emailInput.setHintTextColor(Color.LTGRAY);
        screen.addView(emailInput, lp(-1, 54));

        passwordInput = new EditText(this);
        passwordInput.setSingleLine(true);
        passwordInput.setHint("Password (6 or more characters)");
        passwordInput.setInputType(129);
        passwordInput.setTextColor(Color.WHITE);
        passwordInput.setHintTextColor(Color.LTGRAY);
        screen.addView(passwordInput, lp(-1, 54));

        Button signIn = new Button(this);
        signIn.setText("Sign in with email");
        signIn.setOnClickListener(v -> emailSignIn(false));
        screen.addView(signIn, lp(-1, 52));

        Button create = new Button(this);
        create.setText("Create free account");
        create.setOnClickListener(v -> emailSignIn(true));
        screen.addView(create, lp(-1, 52));

        Button google = new Button(this);
        google.setText("Continue with Google");
        google.setOnClickListener(v -> {
            if (googleSignInClient == null) {
                Toast.makeText(this, "Google sign-in is not configured yet.", Toast.LENGTH_LONG).show();
            } else {
                startActivityForResult(googleSignInClient.getSignInIntent(), 1002);
            }
        });
        screen.addView(google, lp(-1, 52));

        // Cloud login is optional: users can use the offline pedometer without
        // creating an account or providing an email address.
        Button offline = new Button(this);
        offline.setText("Continue offline");
        offline.setOnClickListener(v -> {
            buildShell();
            showPage(0);
            startStepService();
        });
        screen.addView(offline, lp(-1, 52));
        screen.addView(text("Your steps stay on this device while offline. Sign in only when you want cloud sync and community features.", 13, Color.LTGRAY, false), lp(-1, 64));
        setContentView(screen);
        root = screen;
        content = null;
    }

    private void emailSignIn(boolean createAccount) {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (email.isEmpty() || password.length() < 6) {
            Toast.makeText(this, "Enter a valid email and password of at least 6 characters.", Toast.LENGTH_LONG).show();
            return;
        }
        Task<?> task = createAccount
            ? auth.createUserWithEmailAndPassword(email, password)
            : auth.signInWithEmailAndPassword(email, password);
        task.addOnCompleteListener(this, result -> {
            if (result.isSuccessful()) {
                openMainApp();
            } else {
                String message = result.getException() == null ? "Sign-in failed." : result.getException().getLocalizedMessage();
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PROFILE_PHOTO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri uri = data.getData();
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            prefs.edit().putString("profile_photo_uri", uri.toString()).apply();
            if (auth != null && auth.getCurrentUser() != null) {
                StepFitCloud.uploadProfilePhoto(uri, (ok, message) -> runOnUiThread(() ->
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()));
            } else {
                Toast.makeText(this, "Profile photo saved on this device. Sign in to sync it.", Toast.LENGTH_SHORT).show();
            }
            if (content != null) showPage(4);
            return;
        }
        if (requestCode == 1002 && googleSignInClient != null) {
            try {
                Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                com.google.android.gms.auth.api.signin.GoogleSignInAccount account = task.getResult(ApiException.class);
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                auth.signInWithCredential(credential).addOnCompleteListener(this, result -> {
                    if (result.isSuccessful()) openMainApp();
                    else Toast.makeText(this, "Google sign-in failed. Please try email sign-in.", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Toast.makeText(this, "Google sign-in cancelled or unavailable.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                !prefs.getBoolean("notification_permission_asked", false)) {
            prefs.edit().putBoolean("notification_permission_asked", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1002);
        }
    }

    private void startStepService() {
        // Wait for the runtime activity-recognition permission before
        // starting the health foreground service.
        if (Build.VERSION.SDK_INT >= 29 &&
                checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Intent serviceIntent = new Intent(this, StepCounterService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Step counter permission is required to start counting.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Step counter service could not start. Open the app again.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.amolsolunke.stepfitpro.STEPS_UPDATED");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stepsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stepsReceiver, filter);
        }
        receiverRegistered = true;
        if (content != null) showPage(page);
    }

    @Override
    protected void onPause() {
        if (receiverRegistered) {
            try { unregisterReceiver(stepsReceiver); } catch (IllegalArgumentException ignored) {}
            receiverRegistered = false;
        }
        super.onPause();
    }

    private void applyTheme() {
        int[] bgs = {Color.rgb(10,18,38), Color.rgb(8,27,48), Color.rgb(12,32,25), Color.rgb(28,18,48)};
        int[] cards = {Color.rgb(27,42,68), Color.rgb(20,55,78), Color.rgb(26,61,48), Color.rgb(52,35,73)};
        int[] accents = {Color.rgb(0,220,180), Color.rgb(55,190,255), Color.rgb(98,230,145), Color.rgb(200,130,255)};
        int index = Math.max(0, Math.min(theme, 3));
        bg = bgs[index]; card = cards[index]; accent = accents[index];
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(color);
        v.setGravity(Gravity.CENTER); v.setPadding(dp(8), dp(8), dp(8), dp(8));
        if (bold) v.setTypeface(null, Typeface.BOLD);
        return v;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(12), dp(8), dp(12), dp(8));
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER);
        return l;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        return d;
    }

    private void buildShell() {
        root = column(); root.setBackgroundColor(bg);
        String displayName = prefs.getString("profile_name", "");
        TextView title = text("👟 StepFit PRO" + (displayName.isEmpty() ? "" : "  •  " + displayName), 24, accent, true);
        root.addView(title, lp(-1, 64));
        content = column();
        ScrollView scroll = new ScrollView(this); scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        nav = row();
        String[] names = {t(0), t(1), t(2), t(4)};
        for (int i = 0; i < names.length; i++) {
            final int selected = i;
            Button b = new Button(this); b.setText(names[i]); b.setTextSize(10);
            b.setOnClickListener(v -> {
                if (selected == 3) showPage(4); else showPage(selected);
            });
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1));
        }
        root.addView(nav, lp(-1, 58));
        setContentView(root);
    }

    private void addCard(String title, String value) {
        LinearLayout box = column(); box.setBackground(shape(card, 18)); box.setElevation(dp(3));
        box.addView(text(title, 15, Color.LTGRAY, false));
        box.addView(text(value, 26, Color.WHITE, true));
        LinearLayout.LayoutParams p = lp(-1, -2); p.setMargins(0, dp(7), 0, dp(7));
        content.addView(box, p);
    }

    private void showPage(int selectedPage) {
        if (content == null) return;
        page = selectedPage; content.removeAllViews();
        switch (page) {
            case 0: showHome(); break;
            case 1: showProgress(); break;
            case 2: showHistory(); break;
            case 3: showAbout(); break;
            default: showSettings(); break;
        }
    }

    private long getSteps() {
        // Keep the full sensor count as a long; do not impose an artificial
        // Integer.MAX_VALUE ceiling on the displayed daily step total.
        Object stored = prefs.getAll().get("today_steps");
        long value = stored instanceof Number ? ((Number) stored).longValue() : 0L;
        return Math.max(0L, value);
    }

    private void showHome() {
        long steps = getSteps();
        int goal = prefs.getInt("goal", 0);
        float weight = prefs.getFloat("weight", 70f);
        float stride = prefs.getFloat("stride", 75f);
        double distanceKm = steps * stride / 100000.0;
        double calories = steps * weight * 0.0004;
        addCard(t(6), String.format(Locale.US, "%,d", steps));
        addCard(t(1), String.format(Locale.US, "%.2f km", distanceKm));
        addCard(t(2), String.format(Locale.US, "%.0f kcal", calories));
        addCard(t(9), goal <= 0 ? "No limit" : steps + " / " + goal);
        int percent = goal <= 0 ? 0 : Math.min(100, (int) (steps * 100.0 / goal));
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100); bar.setProgress(percent); content.addView(bar, lp(-1, 22));
        content.addView(text(goal <= 0 ? t(16) : percent + "% " + t(26), 17, accent, true));

        boolean sensorAvailable = prefs.getBoolean("sensor_available", false);
        String status = sensorAvailable ? t(17) : t(18);
        if (Build.VERSION.SDK_INT >= 29 &&
                checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            status = t(36);
        }
        content.addView(text(status, 14, sensorAvailable ? accent : Color.YELLOW, false));

        Button refresh = new Button(this); refresh.setText("Refresh");
        refresh.setOnClickListener(v -> showPage(0)); content.addView(refresh, lp(-1, 48));
    }

    private void showProgress() {
        long steps = getSteps(); int goal = prefs.getInt("goal", 0);
        addCard(t(27), String.format(Locale.US, "%,d", steps));
        addCard(t(26), goal <= 0 ? "No goal set" : Math.min(100, (int) (steps * 100.0 / Math.max(1, goal))) + "%");
        long total = 0L, highest = 0L;
        for (int i = 0; i < 7; i++) {
            String date = dateMinus(i);
            long value = prefs.getLong("history_" + date, 0);
            if (i == 0) value = steps;
            long count = Math.max(0L, value);
            total = total > Long.MAX_VALUE - count ? Long.MAX_VALUE : total + count;
            highest = Math.max(highest, count);
            TextView day = text(date + "    " + count + " steps", 14, Color.WHITE, false);
            day.setBackground(shape(card, 12)); content.addView(day, lp(-1, 44));
        }
        addCard(t(28), String.valueOf(total / 7)); addCard(t(29), String.valueOf(highest));

        content.addView(text(t(3) + " • " + t(15), 20, accent, true));
        StepBarChart chart = new StepBarChart(this);
        LinearLayout.LayoutParams chartParams = lp(-1, 190);
        chartParams.setMargins(0, dp(8), 0, dp(12));
        content.addView(chart, chartParams);
        TextView note = text("Daily steps • Last 7 days", 13, Color.LTGRAY, false);
        content.addView(note, lp(-1, 34));
    }

    private class StepBarChart extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final long[] values = new long[7];
        private final String[] days = new String[7];

        StepBarChart(Context context) {
            super(context);
            for (int i = 0; i < 7; i++) {
                values[6 - i] = i == 0 ? getSteps() : Math.max(0L, prefs.getLong("history_" + dateMinus(i), 0L));
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -(6 - i));
                days[i] = new SimpleDateFormat("EEE", Locale.getDefault()).format(cal.getTime());
            }
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth(), height = getHeight();
            float left = dp(12), right = width - dp(12), top = dp(12), bottom = height - dp(30);
            paint.setColor(Color.argb(55, 255, 255, 255));
            paint.setStrokeWidth(dp(1));
            canvas.drawLine(left, bottom, right, bottom, paint);
            long max = 1;
            for (long value : values) max = Math.max(max, value);
            float slot = (right - left) / 7f;
            float barWidth = slot * .55f;
            for (int i = 0; i < 7; i++) {
                float barHeight = (bottom - top) * (values[i] / (float) max);
                float x = left + i * slot + (slot - barWidth) / 2f;
                paint.setColor(accent);
                canvas.drawRoundRect(x, bottom - barHeight, x + barWidth, bottom, dp(5), dp(5), paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(dp(10));
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(days[i], x + barWidth / 2f, height - dp(8), paint);
            }
        }
    }

    private String dateMinus(int days) {
        Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -days);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private void showHistory() {
        content.addView(text(t(15), 22, accent, true));
        for (int i = 0; i < 30; i++) {
            String date = dateMinus(i);
            long count = i == 0 ? getSteps() : prefs.getLong("history_" + date, 0);
            TextView item = text(date + "     " + Math.max(0, count) + " steps", 15, Color.WHITE, false);
            item.setBackground(shape(card, 12));
            LinearLayout.LayoutParams p = lp(-1, 46); p.setMargins(0, dp(3), 0, dp(3));
            content.addView(item, p);
        }
    }

    private void showSettings() {
        content.addView(text(t(4), 24, accent, true));
        ImageView avatar = new ImageView(this);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setBackgroundColor(card);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(104), dp(104));
        avatarParams.gravity = Gravity.CENTER_HORIZONTAL;
        avatarParams.setMargins(0, dp(8), 0, dp(8));
        content.addView(avatar, avatarParams);
        loadProfilePhoto(avatar);
        Button profileButton = new Button(this); profileButton.setText("My Profile");
        profileButton.setOnClickListener(v -> editProfile()); content.addView(profileButton, lp(-1, 50));
        String profileName = prefs.getString("profile_name", "");
        String profileAge = prefs.getString("profile_age", "");
        String profileGender = prefs.getString("profile_gender", "");
        content.addView(text("Profile: " + (profileName.isEmpty() ? "Not set" : profileName) +
            (profileAge.isEmpty() ? "" : " • " + profileAge) +
            (profileGender.isEmpty() ? "" : " • " + profileGender), 14, Color.WHITE, false), lp(-1, 42));
        Button goalButton = new Button(this); goalButton.setText(t(10));
        goalButton.setOnClickListener(v -> editNumber(t(35), "goal", 0));
        content.addView(goalButton, lp(-1, 50));
        Button weightButton = new Button(this); weightButton.setText(t(20));
        weightButton.setOnClickListener(v -> editDecimal(t(20), "weight", 70f));
        content.addView(weightButton, lp(-1, 50));
        Button strideButton = new Button(this); strideButton.setText(t(21));
        strideButton.setOnClickListener(v -> editDecimal(t(21), "stride", 75f));
        content.addView(strideButton, lp(-1, 50));
        Button languageButton = new Button(this); languageButton.setText(t(11));
        languageButton.setOnClickListener(v -> chooseLanguage()); content.addView(languageButton, lp(-1, 50));
        Button themeButton = new Button(this); themeButton.setText(t(12));
        themeButton.setOnClickListener(v -> chooseTheme()); content.addView(themeButton, lp(-1, 50));
        Button gpsButton = new Button(this); gpsButton.setText("GPS Route Tracking");
        gpsButton.setOnClickListener(v -> startActivity(new Intent(this, RouteTrackingActivity.class)));
        content.addView(gpsButton, lp(-1, 50));
        Button communityButton = new Button(this); communityButton.setText("Friends • Clubs • Challenges • Admin");
        communityButton.setOnClickListener(v -> startActivity(new Intent(this, CommunityActivity.class)));
        content.addView(communityButton, lp(-1, 50));
        Button syncButton = new Button(this); syncButton.setText("Sync Profile and Activity");
        syncButton.setOnClickListener(v -> {
            StepFitCloud.syncProfile(prefs.getString("profile_name",""), prefs.getString("profile_age",""),
                prefs.getString("profile_gender",""), (ok, message) -> runOnUiThread(() -> Toast.makeText(this,message,Toast.LENGTH_LONG).show()));
            StepFitCloud.saveDailyStats(getSteps(), getSteps()*prefs.getFloat("stride",75f)/100000.0,
                getSteps()*prefs.getFloat("weight",70f)*0.0004, (ok,message) -> runOnUiThread(() -> Toast.makeText(this,message,Toast.LENGTH_LONG).show()));
        });
        content.addView(syncButton, lp(-1, 50));
        if (auth != null && auth.getCurrentUser() != null) {
            TextView account = text("Signed in: " + auth.getCurrentUser().getEmail(), 13, Color.WHITE, false);
            content.addView(account, lp(-1, 44));
            Button signOut = new Button(this); signOut.setText("Sign out");
            signOut.setOnClickListener(v -> {
                auth.signOut();
                if (googleSignInClient != null) googleSignInClient.signOut();
                showAuthScreen();
            });
            content.addView(signOut, lp(-1, 50));
        } else {
            Button signIn = new Button(this);
            signIn.setText("Sign in to sync / use community");
            signIn.setOnClickListener(v -> showAuthScreen());
            content.addView(signIn, lp(-1, 50));
        }
        Button resetButton = new Button(this); resetButton.setText(t(19));
        resetButton.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle(t(19)).setMessage("Reset today's step count?")
            .setNegativeButton(t(14), null)
            .setPositiveButton("Reset", (d, w) -> {
                prefs.edit().putLong("today_steps", 0L).putLong("base_steps", -1L)
                    .putLong("last_raw", -1L).apply();
                showPage(0);
            }).show());
        content.addView(resetButton, lp(-1, 50));
    }

    private void showLocalProfilePhoto(ImageView target) {
        String localUri = prefs.getString("profile_photo_uri", "");
        if (localUri.isEmpty()) return;
        try { target.setImageURI(android.net.Uri.parse(localUri)); } catch (Exception ignored) {}
    }

    private void loadProfilePhoto(ImageView target) {
        if (auth == null || auth.getCurrentUser() == null) {
            showLocalProfilePhoto(target);
            return;
        }
        StepFitCloud.loadProfilePhotoUrl(photoUrl -> {
            if (isFinishing()) return;
            if (photoUrl == null || photoUrl.trim().isEmpty()) {
                runOnUiThread(() -> showLocalProfilePhoto(target));
                return;
            }
            try {
                com.google.firebase.storage.FirebaseStorage.getInstance()
                    .getReferenceFromUrl(photoUrl).getBytes(2 * 1024 * 1024)
                    .addOnSuccessListener(bytes -> runOnUiThread(() -> {
                        if (isFinishing()) return;
                        android.graphics.Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bitmap != null) target.setImageBitmap(bitmap);
                        else showLocalProfilePhoto(target);
                    }))
                    .addOnFailureListener(error -> runOnUiThread(() -> {
                        if (!isFinishing()) showLocalProfilePhoto(target);
                    }));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!isFinishing()) showLocalProfilePhoto(target);
                });
            }
        });
    }

    private void editProfile() {
        LinearLayout form = column();
        EditText name = new EditText(this); name.setSingleLine(true); name.setHint("Full name");
        name.setText(prefs.getString("profile_name", "")); form.addView(name, lp(-1, 52));
        EditText age = new EditText(this); age.setSingleLine(true); age.setInputType(2); age.setHint("Age (optional)");
        age.setText(prefs.getString("profile_age", "")); form.addView(age, lp(-1, 52));
        EditText gender = new EditText(this); gender.setSingleLine(true); gender.setHint("Gender (optional)");
        gender.setText(prefs.getString("profile_gender", "")); form.addView(gender, lp(-1, 52));
        new AlertDialog.Builder(this).setTitle("My Profile").setView(form)
            .setNegativeButton(t(14), null)
            .setPositiveButton(t(13), (d, w) -> {
                prefs.edit().putString("profile_name", name.getText().toString().trim())
                    .putString("profile_age", age.getText().toString().trim())
                    .putString("profile_gender", gender.getText().toString().trim()).apply();
                if (auth != null && auth.getCurrentUser() != null) {
                    StepFitCloud.syncProfile(name.getText().toString().trim(), age.getText().toString().trim(),
                        gender.getText().toString().trim(), (ok, message) -> runOnUiThread(() ->
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()));
                }
                buildShell(); showPage(4);
            }).setNeutralButton("Choose photo", (d, w) -> {
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("image/*");
                // Request a persistable read grant so a locally selected avatar
                // remains available after the picker/activity is recreated.
                pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(pick, PICK_PROFILE_PHOTO);
            }).show();
    }

    private void editNumber(String title, String key, int value) {
        EditText input = new EditText(this); input.setInputType(2);
        input.setText(String.valueOf(prefs.getInt(key, value)));
        new AlertDialog.Builder(this).setTitle(title).setView(input)
            .setNegativeButton(t(14), null)
            .setPositiveButton(t(13), (d, w) -> {
                try { int n = Integer.parseInt(input.getText().toString().trim());
                    prefs.edit().putInt(key, Math.max(0, n)).apply(); showPage(page);
                } catch (Exception ignored) {}
            }).show();
    }

    private void editDecimal(String title, String key, float value) {
        EditText input = new EditText(this); input.setInputType(8194);
        input.setText(String.valueOf(prefs.getFloat(key, value)));
        new AlertDialog.Builder(this).setTitle(title).setView(input)
            .setNegativeButton(t(14), null)
            .setPositiveButton(t(13), (d, w) -> {
                try { float n = Float.parseFloat(input.getText().toString().trim());
                    if (n > 0) { prefs.edit().putFloat(key, n).apply(); showPage(page); }
                } catch (Exception ignored) {}
            }).show();
    }

    private void chooseLanguage() {
        new AlertDialog.Builder(this).setTitle(t(32)).setItems(langNames, (d, which) -> {
            language = langCodes[which]; prefs.edit().putString("language", language).apply();
            buildShell(); showPage(page);
        }).show();
    }

    private void chooseTheme() {
        String[] themes = {t(22), t(23), t(24), t(25)};
        new AlertDialog.Builder(this).setTitle(t(33)).setItems(themes, (d, which) -> {
            theme = which; prefs.edit().putInt("theme", theme).apply(); applyTheme();
            buildShell(); showPage(page);
        }).show();
    }

    private void showAbout() {
        content.addView(text(t(30), 25, accent, true));
        content.addView(text("StepFit PRO\n\nAn offline step counter for Android.\nTracks steps, distance and estimated calories.\n\nSteps are not limited by the daily goal.", 17, Color.WHITE, false));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startStepService();
                showPage(page);
                requestNotificationPermissionIfNeeded();
            } else {
                Toast.makeText(this, t(36), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 1002) {
            // Step counting continues even if notification permission is declined.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Step counter notifications enabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
