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
    private int page = 0;
    private String language = "en";
    private int theme = 0;

    private int accent = Color.rgb(0, 220, 180);
    private int bg = Color.rgb(10, 18, 38);
    private int card = Color.rgb(27, 42, 68);

    private final String[] langCodes = {
        "en","mr","hi","bn","te","ta","kn","ml","gu","pa",
        "or","as","ur","sa","kok","mai","ne","doi","ks","mni","sat","br"
    };

    private final String[] langNames = {
        "English","मराठी","हिन्दी","বাংলা","తెలుగు","தமிழ்",
        "ಕನ್ನಡ","മലയാളം","ગુજરાતી","ਪੰਜਾਬੀ","ଓଡ଼ିଆ","অসমীয়া",
        "اردو","संस्कृतम्","कोंकणी","मैथिली","नेपाली","डोगरी",
        "کٲشُر","মৈতৈলোন্","ᱥᱟᱱᱛᱟᱲᱤ","बड़ो"
    };

    private final String[][] labels = {
        {
            "Home","Progress","History","Charts","Settings","About",
            "Today's steps","Distance","Calories","Daily goal","Set goal",
            "Language","Theme","Save","Cancel","Last 7 days",
            "No step limit — keep walking!","Step sensor available",
            "Step sensor not available","Reset today's count","Weight (kg)",
            "Stride (cm)","Dark navy","Ocean blue","Forest green","Purple",
            "Goal progress","Total steps","Average steps","Highest day",
            "About StepFit PRO","Offline step counter","Choose language",
            "Choose theme","Settings saved","Enter goal (0 = no goal)",
            "Sensor permission needed","Permission required"
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
            "StepFit PRO बद्दल","ऑफलाइन स्टेप काउंटर","भाषा निवडा",
            "थीम निवडा","सेटिंग्ज जतन झाल्या",
            "लक्ष्य भरा (० = लक्ष्य नाही)","सेन्सर परवानगी आवश्यक",
            "परवानगी आवश्यक"
        },
        {
            "Inicio","Progreso","Historial","Gráficos","Ajustes","Acerca de",
            "Pasos de hoy","Distancia","Calorías","Meta diaria","Definir meta",
            "Idioma","Tema","Guardar","Cancelar","Últimos 7 días",
            "Sin límite de pasos","Sensor disponible","Sensor no disponible",
            "Reiniciar pasos de hoy","Peso (kg)","Longitud de paso (cm)",
            "Azul oscuro","Azul océano","Verde bosque","Morado",
            "Progreso de meta","Pasos totales","Promedio","Día máximo",
            "Acerca de StepFit PRO","Contador sin conexión","Elegir idioma",
            "Elegir tema","Ajustes guardados","Meta (0 = sin meta)",
            "Permiso del sensor necesario","Permiso necesario"
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
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
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

        try {
            Intent serviceIntent = new Intent(this, StepCounterService.class);

            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception ignored) {
            // सेवा सुरू न झाल्यास अॅप बंद होऊ नये.
        }
    }

    private void applyTheme() {
        int[] bgs = {
            Color.rgb(10,18,38),
            Color.rgb(8,27,48),
            Color.rgb(12,32,25),
            Color.rgb(28,18,48)
        };

        int[] cards = {
            Color.rgb(27,42,68),
            Color.rgb(20,55,78),
            Color.rgb(26,61,48),
            Color.rgb(52,35,73)
        };

        int[] accents = {
            Color.rgb(0,220,180),
            Color.rgb(55,190,255),
            Color.rgb(98,230,145),
            Color.rgb(200,130,255)
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

    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void buildShell() {
        ScrollView scroll = new ScrollView(this);

        root = column();
        root.setBackgroundColor(bg);

        TextView brand = text("👟 StepFit PRO", 30, accent, true);
        root.addView(brand, lp(-1, 65));

        root.addView(
            text("A fresh start for your health every day!",
                16, Color.LTGRAY, false),
            lp(-1, 40)
        );

        content = column();
        root.addView(content, lp(-1, -2));

        nav = row();
        nav.setBackgroundColor(card);

        String[] names = {t(0), t(1), t(2), t(4)};
        int[] pages = {0, 1, 2, 4};

        for (int i = 0; i < names.length; i++) {
            final int p = pages[i];
            Button b = button(names[i], () -> showPage(p));

            nav.addView(
                b,
                new LinearLayout.LayoutParams(0, dp(54), 1)
            );
        }

        root.addView(nav, lp(-1, 58));
        scroll.addView(root);
        setContentView(scroll);
    }

    private void cardAdd(LinearLayout parent, View v) {
        LinearLayout wrap = column();
        wrap.setBackground(shape(card, 20));
        wrap.addView(v);

        LinearLayout.LayoutParams p = lp(-1, -2);
        p.setMargins(0, dp(6), 0, dp(8));

        parent.addView(wrap, p);
    }

    private long steps() {
        return prefs.getLong("today_steps", 0);
    }

    private double stride() {
        return prefs.getFloat("stride_cm", 75f) / 100.0;
    }

    private int goal() {
        return prefs.getInt("goal", 9000);
    }

    private double weight() {
        return prefs.getFloat("weight_kg", 70f);
    }

    private void showPage(int p) {
        page = p;
        content.removeAllViews();

        applyTheme();
        root.setBackgroundColor(bg);

        if (p == 0) {
            long st = steps();
            double km = st * stride() / 1000.0;
            long kcal = Math.round(st * weight() * 0.0005);

            LinearLayout hero = column();
            hero.setBackground(shape(card, 24));

            hero.addView(text(t(6), 21, Color.LTGRAY, false));

            TextView stepsView = text(
                String.format(Locale.US, "%,d", st),
                54, accent, true
            );
            hero.addView(stepsView);

            hero.addView(text(t(16), 15, Color.WHITE, false));

            LinearLayout stats = row();
            LinearLayout dist = column();
            LinearLayout cal = column();

            dist.addView(text("📏 " + t(7), 15, Color.LTGRAY, false));
            dist.addView(
                text(String.format(Locale.US, "%.2f km", km),
                    25, Color.WHITE, true)
            );

            cal.addView(text("🔥 " + t(8), 15, Color.LTGRAY, false));
            cal.addView(
                text(kcal + " kcal", 25, Color.WHITE, true)
            );

            stats.addView(dist, new LinearLayout.LayoutParams(0, -2, 1));
            stats.addView(cal, new LinearLayout.LayoutParams(0, -2, 1));
            hero.addView(stats);

            hero.addView(
                text(t(9) + ": " +
                    (goal() == 0 ? "No limit" :
                    String.format(Locale.US, "%,d", goal())),
                    17, accent, true)
            );

            int percent = goal() == 0 ? 0 :
                (int)Math.min(100, st * 100 / Math.max(1, goal()));

            hero.addView(
                text(goal() == 0 ? "" : "Goal progress: " + percent + "%",
                    15, Color.WHITE, false)
            );

            cardAdd(content, hero);

            content.addView(
                button("🎯 " + t(10), this::editGoal),
                lp(-1, 52)
            );

            content.addView(
                button("🌐 " + t(11) + ": " + languageName(),
                    this::chooseLanguage),
                lp(-1, 52)
            );

            content.addView(
                button("🎨 " + t(12), this::chooseTheme),
                lp(-1, 52)
            );

            content.addView(
                button("📊 " + t(15), () -> showPage(2)),
                lp(-1, 52)
            );

            content.addView(
                text(sensorStatus(), 13, Color.LTGRAY, false),
                lp(-1, 50)
            );

        } else if (p == 1) {

            content.addView(text("📈 " + t(1), 26, accent, true));

            long st = steps();
            int g = goal();

            int percent = g == 0 ? 0 :
                (int)Math.min(100, st * 100 / Math.max(1, g));

            cardAdd(content,
                text(t(26) + ": " + percent + "%", 24, Color.WHITE, true));

            cardAdd(content,
                text(t(27) + ": " + String.format(Locale.US, "%,d", st),
                    20, Color.WHITE, true));

            cardAdd(content,
                text(t(28) + ": " +
                    String.format(Locale.US, "%,d", weeklyAverage()),
                    18, Color.WHITE, false));

            cardAdd(content,
                text(t(29) + ": " +
                    String.format(Locale.US, "%,d", highestDay()),
                    18, Color.WHITE, false));

            content.addView(
                button("🎯 " + t(10), this::editGoal),
                lp(-1, 52)
            );

        } else if (p == 2 || p == 3) {

            content.addView(
                text((p == 2 ? "📅 " : "📊 ") + t(p == 2 ? 2 : 3),
                    26, accent, true)
            );

            if (p == 3) {
                drawBars(content);
            }

            for (int i = 6; i >= 0; i--) {
                String day = dayKey(i);

                long count = day.equals(today())
                    ? steps()
                    : prefs.getLong("history_" + day, 0);

                LinearLayout r = row();

                r.addView(
                    text(day, 14, Color.LTGRAY, false),
                    new LinearLayout.LayoutParams(0, -2, 1)
                );

                r.addView(
                    text(String.format(Locale.US, "%,d steps", count),
                        15, Color.WHITE, true),
                    new LinearLayout.LayoutParams(0, -2, 1)
                );

                cardAdd(content, r);
            }

        } else if (p == 4) {

            content.addView(text("⚙️ " + t(4), 26, accent, true));

            content.addView(
                button("🌐 " + t(11) + ": " + languageName(),
                    this::chooseLanguage),
                lp(-1, 52)
            );

            content.addView(
                button("🎨 " + t(12), this::chooseTheme),
                lp(-1, 52)
            );

            content.addView(
                button("🎯 " + t(10), this::editGoal),
                lp(-1, 52)
            );

            content.addView(
                button("⚖️ " + t(20), this::editWeight),
                lp(-1, 52)
            );

            content.addView(
                button("👟 " + t(21), this::editStride),
                lp(-1, 52)
            );

            content.addView(
                button("↻ " + t(19), this::resetToday),
                lp(-1, 52)
            );

            content.addView(
                button("ℹ️ " + t(5), () -> showPage(5)),
                lp(-1, 52)
            );

        } else {

            content.addView(text("ℹ️ " + t(30), 26, accent, true));

            cardAdd(content,
                text("StepFit PRO — " + t(31), 18, Color.WHITE, false));

            cardAdd(content,
                text("Free offline step counter. Uses your phone's built-in " +
                    "step sensor. Step totals, distance and calories are estimates.",
                    15, Color.LTGRAY, false));

            cardAdd(content,
                text("Background counting may be restricted by Android/Oppo " +
                    "battery settings.", 14, Color.LTGRAY, false));
        }
    }

    private void drawBars(LinearLayout parent) {
        long max = Math.max(1, highestDay());

        for (int i = 6; i >= 0; i--) {
            String d = dayKey(i);

            long n = d.equals(today())
                ? steps()
                : prefs.getLong("history_" + d, 0);

            parent.addView(text(d + "  " + n + " steps",
                13, Color.WHITE, false));

            ProgressBar bar = new ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal
            );

            bar.setMax(100);
            bar.setProgress((int)Math.min(100, n * 100 / max));
            parent.addView(bar, lp(-1, 12));
        }
    }

    private String dayKey(int daysAgo) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -daysAgo);

        return new SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(c.getTime());
    }

    private long weeklyAverage() {
        long total = 0;

        for (int i = 0; i < 7; i++) {
            String d = dayKey(i);

            total += d.equals(today()
                ? steps()
                : prefs.getLong("history_" + d, 0));
        }

        return total / 7;
    }

    private long highestDay() {
        long max = 0;

        for (int i = 0; i < 7; i++) {
            String d = dayKey(i);

            max = Math.max(max, d.equals(today()
                ? steps()
                : prefs.getLong("history_" + d, 0)));
        }

        return max;
    }

    private String sensorStatus() {
        SensorManager sm =
            (SensorManager)getSystemService(SENSOR_SERVICE);

        return sm != null &&
            sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
            ? "● " + t(17)
            : "● " + t(18);
    }

    private String languageName() {
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(language)) {
                return langNames[i];
            }
        }

        return "English";
    }

    private void chooseLanguage() {
        new AlertDialog.Builder(this)
            .setTitle(t(32))
            .setItems(langNames, (d, which) -> {
                language = langCodes[which];

                prefs.edit()
                    .putString("language", language)
                    .apply();

                buildShell();
                showPage(page);
            })
            .setNegativeButton(t(14), null)
            .show();
    }

    private void chooseTheme() {
        String[] themes = {t(22), t(23), t(24), t(25)};

        new AlertDialog.Builder(this)
            .setTitle(t(33))
            .setItems(themes, (d, w) -> {
                theme = w;

                prefs.edit()
                    .putInt("theme", theme)
                    .apply();

                applyTheme();
                buildShell();
                showPage(page);
            })
            .setNegativeButton(t(14), null)
            .show();
    }

    private void editGoal() {
        EditText input = new EditText(this);
        input.setInputType(2);
        input.setHint(t(35));
        input.setText(String.valueOf(goal()));

        new AlertDialog.Builder(this)
            .setTitle(t(10))
            .setView(input)
            .setPositiveButton(t(13), (d, w) -> {
                try {
                    int value = Math.max(
                        0,
                        Integer.parseInt(input.getText().toString().trim())
                    );

                    prefs.edit().putInt("goal", value).apply();
                    showPage(page);

                } catch (Exception e) {
                    Toast.makeText(this, "Enter a valid number",
                        Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(t(14), null)
            .show();
    }

    private void editWeight() {
        EditText input = new EditText(this);
        input.setInputType(8194);
        input.setText(String.valueOf(weight()));

        new AlertDialog.Builder(this)
            .setTitle(t(20))
            .setView(input)
            .setPositiveButton(t(13), (d, w) -> {
                try {
                    float val = Float.parseFloat(
                        input.getText().toString()
                    );

                    if (val > 0 && val < 400) {
                        prefs.edit()
                            .putFloat("weight_kg", val)
                            .apply();
                    }

                    showPage(page);

                } catch (Exception ignored) {}
            })
            .setNegativeButton(t(14), null)
            .show();
    }

    private void editStride() {
        EditText input = new EditText(this);
        input.setInputType(8194);
        input.setText(
            String.valueOf(prefs.getFloat("stride_cm", 75f))
        );

        new AlertDialog.Builder(this)
            .setTitle(t(21))
            .setView(input)
            .setPositiveButton(t(13), (d, w) -> {
                try {
                    float val = Float.parseFloat(
                        input.getText().toString()
                    );

                    if (val > 20 && val < 200) {
                        prefs.edit()
                            .putFloat("stride_cm", val)
                            .apply();
                    }

                    showPage(page);

                } catch (Exception ignored) {}
            })
            .setNegativeButton(t(14), null)
            .show();
    }

    private void resetToday() {
        new AlertDialog.Builder(this)
            .setTitle(t(19))
            .setMessage("Reset today's displayed step count?")
            .setPositiveButton("Reset", (d, w) -> {
                prefs.edit()
                    .putLong("today_steps", 0)
                    .putLong("base_steps", -1)
                    .putLong("last_raw", -1)
                    .apply();

                showPage(0);
            })
            .setNegativeButton(t(14), null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (content != null) {
            showPage(page);
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int req, String[] permissions, int[] results
    ) {
        super.onRequestPermissionsResult(req, permissions, results);

        if (req == 1001 &&
            (results.length == 0 ||
            results[0] != PackageManager.PERMISSION_GRANTED)) {

            Toast.makeText(this, t(36), Toast.LENGTH_LONG).show();
        }
    }
}
