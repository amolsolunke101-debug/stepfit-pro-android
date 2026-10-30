package com.amolsolunke.stepfitpro;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.graphics.Color;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity
        implements SensorEventListener {

    private static final int ACTIVITY_PERMISSION_REQUEST = 1001;

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SharedPreferences preferences;

    private TextView stepsText;
    private TextView goalText;
    private TextView distanceText;
    private TextView caloriesText;
    private EditText goalInput;

    private long steps = 0;
    private int dailyGoal = 6000;
    private long lastRawSteps = -1;

    private final float strideMeters = 0.75f;

    private final int backgroundColor =
            Color.rgb(16, 25, 50);

    private final int accentColor =
            Color.rgb(0, 220, 180);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(
                "StepFitData", MODE_PRIVATE);

        dailyGoal = preferences.getInt(
                "daily_goal", 6000);

        steps = preferences.getLong(
                "daily_steps", 0);

        lastRawSteps = preferences.getLong(
                "last_raw_steps", -1);

        sensorManager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(
                    Sensor.TYPE_STEP_COUNTER);
        }

        createInterface();
        updateDisplay();
    }

    private void createInterface() {

        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(28, 36, 28, 28);
        layout.setBackgroundColor(backgroundColor);

        TextView title = makeText(
                "StepFit PRO", 32, accentColor);

        title.setTypeface(null, 1);
        layout.addView(title, matchWidth());

        TextView subtitle = makeText(
                "YOUR DAILY WALKING PARTNER",
                13, Color.LTGRAY);

        subtitle.setPadding(0, 8, 0, 32);
        layout.addView(subtitle, matchWidth());

        stepsText = makeText(
                "आजची पावले\n0", 30, Color.WHITE);

        layout.addView(stepsText, matchWidth());

        goalText = makeText(
                "", 18, accentColor);

        goalText.setPadding(0, 18, 0, 10);
        layout.addView(goalText, matchWidth());

        distanceText = makeText(
                "अंतर: 0.00 किमी", 20, Color.WHITE);

        distanceText.setPadding(0, 12, 0, 12);
        layout.addView(distanceText, matchWidth());

        caloriesText = makeText(
                "अंदाजे कॅलरी: 0", 20, Color.WHITE);

        caloriesText.setPadding(0, 12, 0, 24);
        layout.addView(caloriesText, matchWidth());

        TextView goalLabel = makeText(
                "तुमचे दैनिक पावलांचे लक्ष्य",
                17, Color.WHITE);

        layout.addView(goalLabel, matchWidth());

        goalInput = new EditText(this);

        goalInput.setSingleLine(true);
        goalInput.setTextColor(Color.WHITE);
        goalInput.setHintTextColor(Color.LTGRAY);
        goalInput.setHint("उदा. 6000");
        goalInput.setInputType(2);
        goalInput.setText(String.valueOf(dailyGoal));
        goalInput.setGravity(Gravity.CENTER);

        layout.addView(goalInput, matchWidth());

        Button goalButton = new Button(this);

        goalButton.setText("लक्ष्य जतन करा");
        layout.addView(goalButton, matchWidth());

        goalButton.setOnClickListener(v -> saveGoal());

        TextView note = makeText(
                "चालायला सुरुवात करा आणि तुमचे लक्ष्य पूर्ण करा!",
                15, Color.LTGRAY);

        note.setPadding(0, 28, 0, 0);
        layout.addView(note, matchWidth());

        setContentView(layout);
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private TextView makeText(
            String text, int size, int color) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);

        return view;
    }

    private String getTodayDate() {

        return new SimpleDateFormat(
                "yyyy-MM-dd", Locale.US)
                .format(new Date());
    }

    private boolean hasActivityPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            return checkSelfPermission(
                    Manifest.permission.ACTIVITY_RECOGNITION)
                    == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    private void requestActivityPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            if (!hasActivityPermission()) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.ACTIVITY_RECOGNITION
                        },
                        ACTIVITY_PERMISSION_REQUEST);
            } else {
                startStepSensor();
            }

        } else {
            startStepSensor();
        }
    }

    private void startStepSensor() {

        if (sensorManager == null) {
            showMessage("सेन्सर उपलब्ध नाही.");
            return;
        }

        if (stepSensor == null) {
            showMessage(
                    "या फोनमध्ये पावले मोजणारा सेन्सर उपलब्ध नाही.");
            return;
        }

        boolean registered = sensorManager.registerListener(
                this,
                stepSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        if (!registered) {
            showMessage("पावले मोजणे सुरू होऊ शकले नाही.");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == ACTIVITY_PERMISSION_REQUEST) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                startStepSensor();

            } else {
                showMessage(
                        "पावले मोजण्यासाठी हालचालींची परवानगी आवश्यक आहे.");
            }
        }
    }

    private void saveGoal() {

        String value = goalInput.getText()
                .toString().trim();

        if (value.isEmpty()) {
            showMessage("कृपया लक्ष्य लिहा.");
            return;
        }

        try {

            int newGoal = Integer.parseInt(value);

            if (newGoal < 1 || newGoal > 1000000) {
                showMessage(
                        "लक्ष्य 1 ते 1000000 दरम्यान ठेवा.");
                return;
            }

            dailyGoal = newGoal;

            preferences.edit()
                    .putInt("daily_goal", dailyGoal)
                    .apply();

            InputMethodManager imm =
                    (InputMethodManager)
                    getSystemService(
                            Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.hideSoftInputFromWindow(
                        goalInput.getWindowToken(), 0);
            }

            goalInput.clearFocus();

            updateDisplay();

            showMessage("दैनिक लक्ष्य जतन झाले!");

        } catch (NumberFormatException e) {
            showMessage("योग्य संख्या लिहा.");
        }
    }

    private void updateDisplay() {

        if (stepsText == null) {
            return;
        }

        stepsText.setText("आजची पावले\n" + steps);

        if (steps >= dailyGoal) {
            goalText.setText(
                    "🎉 लक्ष्य पूर्ण! "
                            + steps + " / " + dailyGoal);
        } else {
            goalText.setText(
                    "लक्ष्य: " + dailyGoal + " पावले");
        }

        float distanceKm =
                (steps * strideMeters) / 1000f;

        long calories = Math.round(steps * 0.04f);

        distanceText.setText(String.format(
                Locale.US,
                "अंतर: %.2f किमी",
                distanceKm));

        caloriesText.setText(
                "अंदाजे कॅलरी: " + calories);
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestActivityPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType()
                != Sensor.TYPE_STEP_COUNTER) {
            return;
        }

        long rawSteps = (long) event.values[0];

        String today = getTodayDate();

        String savedDate = preferences.getString(
                "step_date", "");

        SharedPreferences.Editor editor =
                preferences.edit();

        if (!today.equals(savedDate)) {

            // नवीन दिवस: दैनिक पावले शून्य करा.
            steps = 0;
            lastRawSteps = rawSteps;

            editor.putString("step_date", today);
            editor.putLong("daily_steps", 0);
            editor.putLong("last_raw_steps", rawSteps);
            editor.apply();

        } else {

            steps = preferences.getLong(
                    "daily_steps", 0);

            lastRawSteps = preferences.getLong(
                    "last_raw_steps", -1);

            if (lastRawSteps < 0) {

                // पहिल्यांदा सेन्सरची नोंद जतन करा.
                lastRawSteps = rawSteps;

            } else if (rawSteps >= lastRawSteps) {

                // मागील नोंदीनंतर झालेली पावले जोडा.
                steps += rawSteps - lastRawSteps;

                lastRawSteps = rawSteps;

            } else {

                // फोन रीस्टार्ट झाल्यास सेन्सरचा आकडा
                // कमी होऊ शकतो. आधीची दैनिक पावले जतन करा.
                lastRawSteps = rawSteps;
            }

            editor.putString("step_date", today);
            editor.putLong("daily_steps", steps);
            editor.putLong("last_raw_steps", lastRawSteps);
            editor.apply();
        }

        updateDisplay();
    }

    private void showMessage(String message) {
        Toast.makeText(
                this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAccuracyChanged(
            Sensor sensor, int accuracy) {
        // अतिरिक्त बदल आवश्यक नाहीत.
    }
}
