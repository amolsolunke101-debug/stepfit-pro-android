package com.amolsolunke.stepfitpro;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity
        implements SensorEventListener {

    private SharedPreferences prefs;
    private SensorManager sensorManager;
    private Sensor stepSensor;

    private TextView stepsText;
    private TextView distanceText;
    private TextView caloriesText;
    private TextView historyText;
    private TextView languageText;
    private TextView statusText;

    private long todaySteps = 0;
    private long lastRawSteps = -1;
    private long baseSteps = -1;

    private float stride = 0.75f;
    private String language = "en";

    private final int bg = Color.rgb(12, 19, 38);
    private final int card = Color.rgb(27, 38, 65);
    private final int accent = Color.rgb(0, 220, 180);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(175, 190, 210);

    // भाषा कोड, नाव, उपशीर्षक, आजची पावले,
    // पावलांचा संदेश, अंतर, कॅलरी, सेटिंग्ज, भाषा, इतिहास

    private final String[][] languages = {

        {"en","English","A fresh start for your health every day!",
         "Today's steps","No step limit — keep walking!",
         "Distance","Calories","Settings","Language","Last 7 days"},

        {"mr","मराठी","तुमच्या आरोग्याची रोजची नवी सुरुवात!",
         "आजची पावले","पावलांची मर्यादा नाही — चालत राहा!",
         "अंतर","कॅलरी","सेटिंग्ज","भाषा","मागील ७ दिवस"},

        {"hi","हिन्दी","हर दिन सेहत की नई शुरुआत!",
         "आज के कदम","कदमों की कोई सीमा नहीं!",
         "दूरी","कैलोरी","सेटिंग्स","भाषा","पिछले ७ दिन"},

        {"bn","বাংলা","প্রতিদিন সুস্থতার নতুন শুরু!",
         "আজকের পদক্ষেপ","পদক্ষেপের কোনো সীমা নেই!",
         "দূরত্ব","ক্যালোরি","সেটিংস","ভাষা","গত ৭ দিন"},

        {"te","తెలుగు","ప్రతి రోజు ఆరోగ్యానికి కొత్త ఆరంభం!",
         "నేటి అడుగులు","అడుగులకు పరిమితి లేదు!",
         "దూరం","కేలరీలు","సెట్టింగ్‌లు","భాష","గత 7 రోజులు"},

        {"ta","தமிழ்","ஆரோக்கியத்தின் புதிய தொடக்கம்!",
         "இன்றைய அடிகள்","அடிகளுக்கு வரம்பில்லை!",
         "தூரம்","கலோரிகள்","அமைப்புகள்","மொழி","கடந்த 7 நாட்கள்"},

        {"kn","ಕನ್ನಡ","ಆರೋಗ್ಯಕ್ಕೆ ಹೊಸ ಆರಂಭ!",
         "ಇಂದಿನ ಹೆಜ್ಜೆಗಳು","ಹೆಜ್ಜೆಗಳಿಗೆ ಮಿತಿಯಿಲ್ಲ!",
         "ದೂರ","ಕ್ಯಾಲೊರಿಗಳು","ಸೆಟ್ಟಿಂಗ್‌ಗಳು","ಭಾಷೆ","ಕಳೆದ 7 ದಿನಗಳು"},

        {"ml","മലയാളം","ആരോഗ്യത്തിന് പുതിയ തുടക്കം!",
         "ഇന്നത്തെ ചുവടുകൾ","ചുവടുകൾക്ക് പരിധിയില്ല!",
         "ദൂരം","കലോറി","ക്രമീകരണങ്ങൾ","ഭാഷ","കഴിഞ്ഞ 7 ദിവസം"},

        {"gu","ગુજરાતી","સ્વાસ્થ્યની નવી શરૂઆત!",
         "આજનાં પગલાં","પગલાંની કોઈ મર્યાદા નથી!",
         "અંતર","કૅલરી","સેટિંગ્સ","ભાષા","છેલ્લા 7 દિવસ"},

        {"pa","ਪੰਜਾਬੀ","ਸਿਹਤ ਦੀ ਨਵੀਂ ਸ਼ੁਰੂਆਤ!",
         "ਅੱਜ ਦੇ ਕਦਮ","ਕਦਮਾਂ ਦੀ ਕੋਈ ਹੱਦ ਨਹੀਂ!",
         "ਦੂਰੀ","ਕੈਲੋਰੀ","ਸੈਟਿੰਗਾਂ","ਭਾਸ਼ਾ","ਪਿਛਲੇ 7 ਦਿਨ"},

        {"or","ଓଡ଼ିଆ","ସ୍ୱାସ୍ଥ୍ୟର ନୂଆ ଆରମ୍ଭ!",
         "ଆଜିର ପଦକ୍ଷେପ","ପଦକ୍ଷେପର ସୀମା ନାହିଁ!",
         "ଦୂରତା","କ୍ୟାଲୋରି","ସେଟିଂସ୍","ଭାଷା","ଗତ ୭ ଦିନ"},

        {"as","অসমীয়া","স্বাস্থ্যৰ নতুন আৰম্ভণি!",
         "আজিৰ খোজ","খোজৰ কোনো সীমা নাই!",
         "দূৰত্ব","কেলৰি","ছেটিংছ","ভাষা","যোৱা ৭ দিন"},

        {"ur","اردو","صحت کی نئی شروعات!",
         "آج کے قدم","قدموں کی کوئی حد نہیں!",
         "فاصلہ","کیلوریز","ترتیبات","زبان","گزشتہ ۷ دن"},

        {"sa","संस्कृतम्","स्वास्थ्यस्य नूतनः आरम्भः!",
         "अद्यतनानि पदानि","पदानां सीमा नास्ति!",
         "दूरता","ऊर्जामात्रा","विन्यासाः","भाषा","विगताः सप्त दिनानि"},

        {"kok","कोंकणी","आरोग्याची नवी सुरवात!",
         "आयजचीं पावलां","पावलांची मर्यादा ना!",
         "अंतर","कॅलरी","सेटिंग्स","भास","फाटले ७ दीस"},

        {"mai","मैथिली","स्वास्थ्यक नव शुरुआत!",
         "आइक पयर","पयरक कोनो सीमा नहि!",
         "दूरी","कैलोरी","सेटिंग्स","भाषा","पिछला ७ दिन"},

        {"ne","नेपाली","स्वास्थ्यको नयाँ सुरुवात!",
         "आजका पाइला","पाइलाको सीमा छैन!",
         "दूरी","क्यालोरी","सेटिङहरू","भाषा","पछिल्ला ७ दिन"},

        {"doi","डोगरी","सेहत दी नमीं शुरुआत!",
         "अज्ज दे कदम","कदमें दी कोई हद्द नेईं!",
         "दूरी","कैलोरी","सेटिंग","भाशा","पिछले 7 दिन"},

        {"ks","کٲشُر","صحتُک نَو شروعات!",
         "آجُک قدم","قدمَن ہُند حد چھُ نہ!",
         "فاصلہ","کیلوریز","ترتیبات","زبان","پچھلے ۷ دۄہ"},

        {"mni","মৈতৈলোন","নুংঙাইগী অয়ুক অমা!",
         "নহাক্কী খোংথাং","খোংথাংগী লিমিট লৈতে!",
         "দূরত্ব","কেলোরি","সেটিংস","লোল","মমাংগী ৭ নুমিৎ"},

        {"sat","ᱥᱟᱱᱛᱟᱲᱤ","ᱱᱟᱶᱟ ᱮᱦᱚᱵ!",
         "ᱛᱮᱦᱮᱧ ᱥᱟᱢᱟᱝ","ᱥᱤᱢᱟ ᱵᱟᱹᱱᱩᱜ!",
         "ᱫᱩᱨᱤ","ᱠᱮᱞᱚᱨᱤ","ᱥᱮᱴᱤᱝ","ᱯᱟᱹᱨᱥᱤ","ᱦᱟᱹᱞᱤ ᱗ ᱫᱤᱱ"},

        {"brx","बड़ो","जिउनि गोजोन गोदान!",
         "दिनैनि खुरां","खुरांनि सिमा गैया!",
         "दुरि","केलरि","सेटिं","राव","आगोलनि ७ सान"}
    };

    private String text(int index) {
        for (String[] item : languages) {
            if (item[0].equals(language)) {
                return item[index];
            }
        }
        return languages[0][index];
    }

    private String dateKey(long time) {
        return new SimpleDateFormat(
                "yyyy-MM-dd", Locale.US
        ).format(new Date(time));
    }

    private String today() {
        return dateKey(System.currentTimeMillis());
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    private TextView makeText(
            String value, int size, int color) {

        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(7), dp(8), dp(7), dp(8));
        return t;
    }

    private LinearLayout makeColumn() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(10), dp(16), dp(10));
        return l;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        prefs = getSharedPreferences(
                "StepFitData", MODE_PRIVATE);

        language = prefs.getString("language", "en");
        stride = prefs.getFloat("stride", 0.75f);

        String savedDate = prefs.getString("date", "");

        if (today().equals(savedDate)) {
            todaySteps = prefs.getLong("today_steps", 0);
            lastRawSteps = prefs.getLong("last_raw", -1);
            baseSteps = prefs.getLong("base_steps", -1);
        } else {
            if (!savedDate.isEmpty()) {
                prefs.edit().putLong(
                        "history_" + savedDate,
                        prefs.getLong("today_steps", 0)
                ).apply();
            }

            todaySteps = 0;
            lastRawSteps = -1;
            baseSteps = -1;
            saveData();
        }

        sensorManager = (SensorManager)
                getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(
                    Sensor.TYPE_STEP_COUNTER);
        }

        buildUI();
        updateUI();
        askPermission();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bg);

        LinearLayout root = makeColumn();

        TextView title = makeText(
                "👟 StepFit PRO", 29, accent);
        title.setTypeface(null, 1);
        root.addView(title);

        root.addView(makeText(text(2), 15, muted));

        LinearLayout hero = makeColumn();
        hero.setBackgroundColor(card);

        hero.addView(makeText(text(3), 18, muted));

        stepsText = makeText("0", 48, accent);
        stepsText.setTypeface(null, 1);
        hero.addView(stepsText);

        hero.addView(makeText(text(4), 14, white));
        root.addView(hero);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout distanceCard = makeColumn();
        distanceCard.setBackgroundColor(card);
        distanceCard.addView(
                makeText("📏 " + text(5), 15, muted));

        distanceText = makeText("0.00 km", 21, white);
        distanceCard.addView(distanceText);

        LinearLayout calorieCard = makeColumn();
        calorieCard.setBackgroundColor(card);
        calorieCard.addView(
                makeText("🔥 " + text(6), 15, muted));

        caloriesText = makeText("0 kcal", 21, white);
        calorieCard.addView(caloriesText);

        row.addView(distanceCard,
                new LinearLayout.LayoutParams(0, -2, 1));

        row.addView(calorieCard,
                new LinearLayout.LayoutParams(0, -2, 1));

        root.addView(row);

        languageText = makeText(
                "🌐 " + text(8) + ": " + languageName(),
                15, white);
        root.addView(languageText);

        Button languageButton = new Button(this);
        languageButton.setText(text(7) + " / " + text(8));
        root.addView(languageButton);

        languageButton.setOnClickListener(
                v -> chooseLanguage());

        root.addView(makeText(
                "📊 " + text(9), 19, accent));

        historyText = makeText("", 14, white);
        historyText.setGravity(Gravity.START);
        root.addView(historyText);

        statusText = makeText("", 13, muted);
        root.addView(statusText);

        scroll.addView(root);
        setContentView(scroll);
    }

    private String languageName() {
        for (String[] item : languages) {
            if (item[0].equals(language)) {
                return item[1];
            }
        }
        return "English";
    }

    private void chooseLanguage() {
        String[] names = new String[languages.length];

        for (int i = 0; i < languages.length; i++) {
            names[i] = languages[i][1];
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose language / भाषा निवडा")
                .setItems(names, (dialog, selected) -> {
                    language = languages[selected][0];

                    prefs.edit()
                            .putString("language", language)
                            .apply();

                    buildUI();
                    updateUI();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askPermission() {
        if (Build.VERSION.SDK_INT >= 29 &&
                checkSelfPermission(
                        Manifest.permission.ACTIVITY_RECOGNITION)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACTIVITY_RECOGNITION
                    }, 1001);
        }
    }

    private void saveData() {
        prefs.edit()
                .putString("date", today())
                .putLong("today_steps", todaySteps)
                .putLong("last_raw", lastRawSteps)
                .putLong("base_steps", baseSteps)
                .apply();
    }

    private void updateUI() {
        if (stepsText == null) return;

        stepsText.setText(String.format(
                Locale.US, "%,d", todaySteps));

        distanceText.setText(String.format(
                Locale.US, "%.2f km",
                todaySteps * stride / 1000.0));

        caloriesText.setText(
                Math.round(todaySteps * 0.04) + " kcal");

        languageText.setText(
                "🌐 " + text(8) + ": " + languageName());

        StringBuilder history = new StringBuilder();

        for (int i = 6; i >= 0; i--) {
            String key = dateKey(
                    System.currentTimeMillis()
                            - i * 86400000L);

            long count = key.equals(today())
                    ? todaySteps
                    : prefs.getLong("history_" + key, 0);

            history.append(key)
                    .append(" : ")
                    .append(count)
                    .append(" steps\n");
        }

        historyText.setText(history.toString());

        statusText.setText(
                stepSensor == null
                        ? "No step counter sensor detected."
                        : "Step sensor available. Background counting needs a separate service.");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sensorManager != null && stepSensor != null &&
                (Build.VERSION.SDK_INT < 29 ||
                checkSelfPermission(
                        Manifest.permission.ACTIVITY_RECOGNITION)
                        == PackageManager.PERMISSION_GRANTED)) {

            sensorManager.registerListener(
                    this, stepSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        saveData();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()
                != Sensor.TYPE_STEP_COUNTER) return;

        long raw = (long) event.values[0];

        if (baseSteps < 0 || lastRawSteps < 0) {
            baseSteps = raw - todaySteps;
        }

        if (raw >= lastRawSteps || lastRawSteps < 0) {
            todaySteps = Math.max(0, raw - baseSteps);
            lastRawSteps = raw;

            saveData();
            updateUI();
        }
    }

    @Override
    public void onAccuracyChanged(
            Sensor sensor, int accuracy) {
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == 1001 &&
                (grantResults.length == 0 ||
                grantResults[0]
                        != PackageManager.PERMISSION_GRANTED)) {

            Toast.makeText(
                    this,
                    "Physical activity permission is required.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
