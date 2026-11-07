package com.amolsolunke.stepfitpro;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
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

    private final BroadcastReceiver stepsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.amolsolunke.stepfitpro.STEPS_UPDATED".equals(intent.getAction())) {
                if (content != null) showPage(page);
            }
        }
    };

    private final String[] langCodes = {
        "en","mr","hi","bn","te","ta","kn","ml","gu","pa","or",
        "as","ur","sa","kok","mai","ne","doi","ks","mni","sat","br"
    };

    private final String[] langNames = {
        "English","मराठी","हिन्दी","বাংলা","తెలుగు","தமிழ்","ಕನ್ನಡ",
        "മലയാളം","ગુજરાતી","ਪੰਜਾਬੀ","ଓଡ଼ିଆ","অসমীয়া","اردو",
        "संस्कृतम्","कोंकणी","मैथिली","नेपाली","डोगरी","کٲشُر",
        "মৈতৈলোন্","ᱥᱟᱱᱛᱟᱲᱤ","बड़ो"
    };

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
        if ("es".equals(language)) return labels[2][i];
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
        if (Build.VERSION.SDK_INT >= 29 &&
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1001);
        }
        buildShell();
        showPage(0);
        startStepService();
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
        screen.addView(text("Your step counter data stays on this device when offline.", 13, Color.LTGRAY, false), lp(-1, 56));
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

    private void startStepService() {
        Intent serviceIntent = new Intent(this, StepCounterService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
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
        TextView title = text("👟 StepFit PRO", 28, accent, true);
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

    private int getSteps() {
        // SharedPreferences may contain an Integer from an older app version
        // or a Long from the updated service. Read either type safely.
        Object stored = prefs.getAll().get("today_steps");
        long value = stored instanceof Number ? ((Number) stored).longValue() : 0L;
        if (value < 0) value = 0;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private void showHome() {
        int steps = getSteps();
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
        int steps = getSteps(); int goal = prefs.getInt("goal", 0);
        addCard(t(27), String.format(Locale.US, "%,d", steps));
        addCard(t(26), goal <= 0 ? "No goal set" : Math.min(100, steps * 100 / Math.max(1, goal)) + "%");
        int total = 0, highest = 0;
        for (int i = 0; i < 7; i++) {
            String date = dateMinus(i);
            long value = prefs.getLong("history_" + date, 0);
            if (i == 0) value = steps;
            int count = (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
            total += count; highest = Math.max(highest, count);
            TextView day = text(date + "    " + count + " steps", 14, Color.WHITE, false);
            day.setBackground(shape(card, 12)); content.addView(day, lp(-1, 44));
        }
        addCard(t(28), String.valueOf(total / 7)); addCard(t(29), String.valueOf(highest));
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
            } else {
                Toast.makeText(this, t(36), Toast.LENGTH_LONG).show();
            }
        }
    }
}
