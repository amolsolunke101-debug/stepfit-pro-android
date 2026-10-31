package com.amolsolunke.stepfitpro;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

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
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date());
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

        if (Build.VERSION.SDK_INT >= 29 &&
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                1001
            );
        }

        buildShell();
        showPage(0);

        if (Build.VERSION.SDK_INT >= 26) {
            try {
                startForegroundService(
                    new Intent(this, StepCounterService.class)
                );
            } catch (Exception ignored) {}
        } else {
            try {
                startService(new Intent(this, StepCounterService.class));
            } catch (Exception ignored) {}
        }
    }

    private void applyTheme() {
        int[] bgs = {
            Color.rgb(10,18,38), Color.rgb(8,27,48),
            Color.rgb(12,32,25), Color.rgb(28,18,48)
        };

        int[] cards = {
            Color.rgb(27,42,68), Color.rgb(20,55,78),
            Color.rgb(26,61,48), Color.rgb(52,35,73)
        };

        int[] accents = {
            Color.rgb(0,220,180), Color.rgb(55,190,255),
            Color.rgb(98,230,145), Color.rgb(200,130,255)
        };

        int index = Math.max(0, Math.min(theme, 3));
        bg = bgs[index];
        card = cards[index];
        accent = accents[index];
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(8), dp(8), dp(8), dp(8));

        if (bold) {
            v.setTypeface(null, Typeface.BOLD);
        }

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
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER);
        return l;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(
            w < 0 ? w : dp(w),
            h < 0 ? h : dp(h)
        );
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private void buildShell() {
        root = column();
        root.setBackgroundColor(bg);

        TextView title = text(
            "👟 StepFit PRO",
            28,
            accent,
            true
        );
        root.addView(title, lp(-1, 64));

        content = column();
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));

        nav = row();

        String[] names = {
            t(0), t(1), t(2), t(4)
        };

        for (int i = 0; i < names.length; i++) {
            final int selected = i;
            Button b = new Button(this);
            b.setText(names[i]);
            b.setTextSize(10);
            b.setOnClickListener(v -> {
                if (selected == 3) {
                    showPage(4);
                } else {
                    showPage(selected);
                }
            });
            nav.addView(b, new LinearLayout.LayoutParams(
                0, dp(52), 1
            ));
        }

        root.addView(nav, lp(-1, 58));
        setContentView(root);
    }

    private void addCard(String title, String value) {
        LinearLayout box = column();
        box.setBackground(shape(card, 18));
        box.setElevation(dp(3));

        TextView heading = text(title, 15, Color.LTGRAY, false);
        TextView number = text(value, 26, Color.WHITE, true);

        box.addView(heading);
        box.addView(number);

        LinearLayout.LayoutParams p = lp(-1, -2);
        p.setMargins(0, dp(7), 0, dp(7));
        content.addView(box, p);
    }

    private void showPage(int selectedPage) {
        page = selectedPage;
        content.removeAllViews();

        switch (page) {
            case 0:
                showHome();
                break;
            case 1:
                showProgress();
                break;
            case 2:
                showHistory();
                break;
            case 3:
                showAbout();
                break;
            default:
                showSettings();
                break;
        }
    }

    private int getSteps() {
        return prefs.getInt("today_steps",
                (int) prefs.getLong("today_steps", 0));
    }

    private void showHome() {
        int steps = getSteps();
        int goal = prefs.getInt("goal", 9000);
        float weight = prefs.getFloat("weight", 70f);
        float stride = prefs.getFloat("stride", 75f);

        double distanceKm = steps * stride / 100000.0;
        double calories = steps * weight * 0.0004;

        addCard(t(6), String.format(Locale.US, "%,d", steps));
        addCard(t(1), String.format(Locale.US, "%.2f km", distanceKm));
        addCard(t(2), String.format(Locale.US, "%.0f kcal", calories));
        addCard(t(9), goal <= 0 ? "No limit" : steps + " / " + goal);

        int percent = goal <= 0 ? 0 :
                Math.min(100, (int) (steps * 100.0 / goal));

        ProgressBar bar = new ProgressBar(
            this, null, android.R.attr.progressBarStyleHorizontal
        );
        bar.setMax(100);
        bar.setProgress(percent);
        content.addView(bar, lp(-1, 22));

        content.addView(text(
            goal <= 0 ? t(16) : percent + "% " + t(26),
            17, accent, true
        ));

        statusView = text(
            getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_SENSOR_STEP_COUNTER
            ) ? t(17) : t(18),
            14, Color.LTGRAY, false
        );
        content.addView(statusView);

        Button refresh = new Button(this);
        refresh.setText("Refresh");
        refresh.setOnClickListener(v -> showPage(0));
        content.addView(refresh, lp(-1, 48));
    }

    private void showProgress() {
        int steps = getSteps();
        int goal = prefs.getInt("goal", 9000);

        addCard(t(27), String.format(Locale.US, "%,d", steps));
        addCard(t(26), goal <= 0 ? "No goal set" :
                Math.min(100, steps * 100 / Math.max(1, goal)) + "%");

        int total = 0;
        int highest = 0;

        for (int i = 0; i < 7; i++) {
            String date = dateMinus(i);
            int value = (int) prefs.getLong("history_" + date, 0);

            if (i == 0) value = steps;

            total += value;
            highest = Math.max(highest, value);

            TextView day = text(
                date + "    " + value + " steps",
                14, Color.WHITE, false
            );
            day.setBackground(shape(card, 12));
            content.addView(day, lp(-1, 44));
        }

        addCard(t(28), String.valueOf(total / 7));
        addCard(t(29), String.valueOf(highest));
    }

    private String dateMinus(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -days);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(c.getTime());
    }

    private void showHistory() {
        content.addView(text(
            t(15), 22, accent, true
        ));

        for (int i = 0; i < 30; i++) {
            String date = dateMinus(i);
            long count = prefs.getLong("history_" + date, 0);

            if (i == 0) {
                count = getSteps();
            }

            TextView item = text(
                date + "     " + count + " steps",
                15, Color.WHITE, false
            );

            item.setBackground(shape(card, 12));
            LinearLayout.LayoutParams p = lp(-1, 46);
            p.setMargins(0, dp(3), 0, dp(3));
            content.addView(item, p);
        }
    }

    private void showSettings() {
        content.addView(text(
            t(4), 24, accent, true
        ));

        Button goalButton = new Button(this);
        goalButton.setText(t(10));
        goalButton.setOnClickListener(v -> editNumber(
            t(35), "goal", 9000
        ));
        content.addView(goalButton, lp(-1, 50));

        Button weightButton = new Button(this);
        weightButton.setText(t(20));
        weightButton.setOnClickListener(v -> editDecimal(
            t(20), "weight", 70f
        ));
        content.addView(weightButton, lp(-1, 50));

        Button strideButton = new Button(this);
        strideButton.setText(t(21));
        strideButton.setOnClickListener(v -> editDecimal(
            t(21), "stride", 75f
        ));
        content.addView(strideButton, lp(-1, 50));

        Button languageButton = new Button(this);
        languageButton.setText(t(11));
        languageButton.setOnClickListener(v -> chooseLanguage());
        content.addView(languageButton, lp(-1, 50));

        Button themeButton = new Button(this);
        themeButton.setText(t(12));
        themeButton.setOnClickListener(v -> chooseTheme());
        content.addView(themeButton, lp(-1, 50));

        Button resetButton = new Button(this);
        resetButton.setText(t(19));
        resetButton.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle(t(19))
            .setMessage("Reset today's step count?")
            .setNegativeButton(t(14), null)
            .setPositiveButton("Reset", (d, w) -> {
                prefs.edit()
                    .putInt("today_steps", 0)
                    .putLong("today_steps", 0)
                    .putLong("base_steps", -1)
                    .putLong("last_raw", -1)
                    .apply();
                showPage(0);
            })
            .show());
        content.addView(resetButton, lp(-1, 50));
    }

    private void editNumber(String title, String key, int value) {
        EditText input = new EditText(this);
        input.setInputType(2);
        input.setText(String.valueOf(prefs.getInt(key, value)));

        new AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setNegativeButton(t(14), null)
            .setPositiveButton(t(13), (d, w) -> {
                try {
                    int n = Integer.parseInt(
                        input.getText().toString().trim()
                    );
                    prefs.edit().putInt(key, Math.max(0, n)).apply();
                    showPage(page);
                } catch (Exception ignored) {}
            })
            .show();
    }

    private void editDecimal(String title, String key, float value) {
        EditText input = new EditText(this);
        input.setInputType(8194);
        input.setText(String.valueOf(prefs.getFloat(key, value)));

        new AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setNegativeButton(t(14), null)
            .setPositiveButton(t(13), (d, w) -> {
                try {
                    float n = Float.parseFloat(
                        input.getText().toString().trim()
                    );
                    if (n > 0) {
                        prefs.edit().putFloat(key, n).apply();
                        showPage(page);
                    }
                } catch (Exception ignored) {}
            })
            .show();
    }

    private void chooseLanguage() {
        new AlertDialog.Builder(this)
            .setTitle(t(32))
            .setItems(langNames, (d, which) -> {
                language = langCodes[which];
                prefs.edit().putString("language", language).apply();
                buildShell();
                showPage(page);
            })
            .show();
    }

    private void chooseTheme() {
        String[] themes = {
            t(22), t(23), t(24), t(25)
        };

        new AlertDialog.Builder(this)
            .setTitle(t(33))
            .setItems(themes, (d, which) -> {
                theme = which;
                prefs.edit().putInt("theme", theme).apply();
                applyTheme();
                buildShell();
                showPage(page);
            })
            .show();
    }

    private void showAbout() {
        content.addView(text(
            t(30), 25, accent, true
        ));

        content.addView(text(
            "StepFit PRO\n\n" +
            "An offline step counter for Android.\n" +
            "Tracks steps, distance and estimated calories.\n\n" +
            "Steps are not limited by the daily goal.",
            17, Color.WHITE, false
        ));
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (requestCode == 1001) {
            if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPage(page);
            } else {
                Toast.makeText(
                    this, t(36), Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}
