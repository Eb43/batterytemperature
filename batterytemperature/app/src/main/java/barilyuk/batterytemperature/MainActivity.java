package barilyuk.batterytemperature;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.view.View;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.content.ComponentName;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.LinkedList;

import androidx.core.content.FileProvider;
import android.widget.Toast;

import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import java.io.File;
import android.os.Environment;

import android.text.TextWatcher;
import android.text.Editable;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;
    private TextView temperatureTextView;
    private Handler handler = new Handler();
    private int colorIndex = 0;
    private CheckBox autostartCheckBox;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String AUTO_START_KEY = "AutoStart";
    private int[] colors = new int[]{
            Color.parseColor("#A52A2A"),  // Brown
            Color.parseColor("#808080"),  // Grey
            Color.parseColor("#0000FF")   // Blue
    };

    private RadioGroup textColorRadioGroup;
    private RadioButton radioBlack;
    private RadioButton radioWhite;
    private static final String RADIO_CHOSEN_BLACK_KEY = "RadioChosenBlack";

    private RollingChartView tempChart;

    private EditText durationEditText;
    private Spinner timeUnitSpinner;
    private Button startStopLoggingButton;
    private TextView logFilePathTextView;
    private BroadcastReceiver loggingFinishedReceiver;

    private EditText temperatureThresholdEditText;
    private static final String TEMP_THRESHOLD_KEY = "TempThreshold";
    private CheckBox enableTempAlarmCheckBox;
    private static final String ENABLE_TEMP_ALARM_KEY = "EnableTempAlarm";
    private MediaPlayer alarmPlayer;
    private boolean isAlarmPlaying = false;

    private Spinner temperatureScaleSpinner;
    private static final String TEMP_SCALE_KEY = "TempScale";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);


        temperatureScaleSpinner = findViewById(R.id.temperatureScaleSpinner);
        String[] scales = getResources().getStringArray(R.array.temperature_scales);

        ArrayAdapter<String> scaleAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, scales) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextSize(40); // Size when spinner is closed
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextSize(32); // Size in dropdown list
                }
                return view;
            }
        };

        scaleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        temperatureScaleSpinner.setAdapter(scaleAdapter);


// Load saved scale
        String savedScale = prefs.getString(TEMP_SCALE_KEY, "°C");
        int scalePosition = 0;
        for (int i = 0; i < scales.length; i++) {
            if (scales[i].equals(savedScale)) {
                scalePosition = i;
                break;
            }
        }
        temperatureScaleSpinner.setSelection(scalePosition);

// Save scale when changed
        temperatureScaleSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedScale = parent.getItemAtPosition(position).toString();
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putString(TEMP_SCALE_KEY, selectedScale);
                editor.apply();

                tempChart.setTempScale(selectedScale);

                // Update loggerTempScale TextView
                TextView loggerTempScale = findViewById(R.id.loggerTempScale);
                loggerTempScale.setText(" " + getScaleSymbol(selectedScale));

                // Notify service to update notification
                Intent intent = new Intent(MainActivity.this, BatteryTempService.class);
                intent.setAction("UPDATE_NOTIFICATION");
                startService(intent);

                // Update threshold display when scale changes
                float savedThresholdCelsius = prefs.getFloat(TEMP_THRESHOLD_KEY, 50.0f);
                float displayThreshold = savedThresholdCelsius;
                if (selectedScale.equals("°F") || selectedScale.equals("F")) {
                    displayThreshold = savedThresholdCelsius * 9f / 5f + 32f;
                } else if (selectedScale.equals("K")) {
                    displayThreshold = savedThresholdCelsius + 273.15f;
                }
                temperatureThresholdEditText.setText(String.valueOf((int)displayThreshold));
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });



        tempChart = findViewById(R.id.tempChart);

        tempChart.setTempScale(savedScale);

        durationEditText = findViewById(R.id.durationEditText);
        timeUnitSpinner = findViewById(R.id.timeUnitSpinner);
        startStopLoggingButton = findViewById(R.id.startStopLoggingButton);

        // Set the log file path
        logFilePathTextView = findViewById(R.id.logFilePathTextView);
        File logFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "battery_temperature_log.csv");
        logFilePathTextView.setText(getString(R.string.log_file_prefix) + logFile.getAbsolutePath());

        // Setup temperature threshold
        temperatureThresholdEditText = findViewById(R.id.temperatureThresholdEditText);
        //SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        float savedThresholdCelsius = prefs.getFloat(TEMP_THRESHOLD_KEY, 50.0f);
        String scale = prefs.getString(TEMP_SCALE_KEY, "°C");
        float displayThreshold = savedThresholdCelsius;
        if (scale.equals("°F") || scale.equals("F")) {
            displayThreshold = savedThresholdCelsius * 9f / 5f + 32f;
        } else if (scale.equals("K")) {
            displayThreshold = savedThresholdCelsius + 273.15f;
        }
        temperatureThresholdEditText.setText(String.valueOf((int)displayThreshold));

// Save threshold when user changes it and validate for integers only
        temperatureThresholdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().trim();
                if (!input.isEmpty()) {
                    if (input.contains(".")) {
                        Toast.makeText(MainActivity.this, getString(R.string.integer_only_validation), Toast.LENGTH_SHORT).show();
                        // Remove the decimal part
                        String integerPart = input.split("\\.")[0];
                        temperatureThresholdEditText.removeTextChangedListener(this);
                        temperatureThresholdEditText.setText(integerPart);
                        temperatureThresholdEditText.setSelection(integerPart.length());
                        temperatureThresholdEditText.addTextChangedListener(this);
                    }
                }

                // Save after user stops typing (with small delay)
                handler.removeCallbacks(saveThresholdRunnable);
                handler.postDelayed(saveThresholdRunnable, 500);
            }
        });

        // Setup enable temperature alarm checkbox
        enableTempAlarmCheckBox = findViewById(R.id.enableTempAlarmCheckBox);
        boolean isAlarmEnabled = prefs.getBoolean(ENABLE_TEMP_ALARM_KEY, false);
        enableTempAlarmCheckBox.setChecked(isAlarmEnabled);

// Save alarm enable state when user changes it
        enableTempAlarmCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(ENABLE_TEMP_ALARM_KEY, isChecked);
                editor.apply();
            }
        });

        //clickable path field activates share menu
        logFilePathTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!logFile.exists()) {
                    Toast.makeText(MainActivity.this, getString(R.string.log_file_not_found), Toast.LENGTH_SHORT).show();
                    return;
                }

                Uri uri = FileProvider.getUriForFile(
                        MainActivity.this,
                        getPackageName() + ".fileprovider",
                        logFile
                );

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_log_file)));
            }
        });

        loggingFinishedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateLoggingButtonText();
                Toast.makeText(MainActivity.this, "Temperature logging finished", Toast.LENGTH_SHORT).show();
            }
        };
        registerReceiver(loggingFinishedReceiver, new IntentFilter(getString(R.string.temperature_logging_finished)));

// Setup time unit spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.time_units, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeUnitSpinner.setAdapter(adapter);

// Setup logging button
        startStopLoggingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                boolean isLogging = prefs.getBoolean("IS_LOGGING", false);

                if (isLogging) {
                    stopLogging();
                } else {
                    startLogging();
                }
            }
        });

// Update button text based on logging status
        updateLoggingButtonText();

        temperatureTextView = findViewById(R.id.temperatureTextView);
        autostartCheckBox = findViewById(R.id.autostartCheckBox);

        textColorRadioGroup = findViewById(R.id.textColorRadioGroup);
        radioBlack = findViewById(R.id.radioBlack);
        radioWhite = findViewById(R.id.radioWhite);
        updatetemperatureTask.run();

        handler.postDelayed(updatetemperatureTask, 15000);

        // Set initial state for the checkbox
        //SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isAutoStartEnabled = prefs.getBoolean(AUTO_START_KEY, false);
        autostartCheckBox.setChecked(isAutoStartEnabled);
        boolean radioChosenBlack = prefs.getBoolean(RADIO_CHOSEN_BLACK_KEY, true);
        radioBlack.setChecked(radioChosenBlack);
        radioWhite.setChecked(!radioChosenBlack);

        // Set checkbox change listener
        autostartCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(AUTO_START_KEY, isChecked);
                editor.apply();

                if (isChecked) {
                    showAutostartDialog();
                }
            }
        });

        // Set radio group change listener
        textColorRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                boolean radioChosenBlack = checkedId == R.id.radioBlack;
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(RADIO_CHOSEN_BLACK_KEY, radioChosenBlack);
                editor.apply();

                // Immediately notify the notification service to update the icon color
                Intent intent = new Intent(MainActivity.this, BatteryTempService.class);
                intent.setAction("UPDATE_NOTIFICATION");
                startService(intent); // onStartCommand will handle this
            }
        });

        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. Stop temperature update task
                handler.removeCallbacks(updatetemperatureTask);

                // 2. Stop logging safely
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                boolean isLogging = prefs.getBoolean("IS_LOGGING", false);
                if (isLogging) {
                    Intent stopLoggingIntent = new Intent(MainActivity.this, BatteryTempService.class);
                    stopLoggingIntent.setAction("STOP_LOGGING");
                    startService(stopLoggingIntent);

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("IS_LOGGING", false);
                    editor.apply();
                }

                // 3. Stop notification service
                stopNotificationService();

                // 4. Finish activity safely
                finish();

                // Optional: remove app from recent tasks without risk
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAndRemoveTask();
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                startNotificationService();
            }
        } else {
            startNotificationService();
        }
    }

    private Runnable saveThresholdRunnable = new Runnable() {
        @Override
        public void run() {
            saveTemperatureThreshold();
        }
    };

    private void showAutostartDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.autostart_permission_title))
                .setMessage(getString(R.string.autostart_permission_message))
                .setPositiveButton(getString(R.string.open_settings), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        String packageName = getPackageName();
                        if (Build.MANUFACTURER.equalsIgnoreCase("xiaomi")) {
                            intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                        } else if (Build.MANUFACTURER.equalsIgnoreCase("oppo")) {
                            intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
                        } else if (Build.MANUFACTURER.equalsIgnoreCase("vivo")) {
                            intent.setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
                        } else if (Build.MANUFACTURER.equalsIgnoreCase("huawei")) {
                            intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
                        } else if (Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
                            intent.setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"));
                        } else if (Build.MANUFACTURER.equalsIgnoreCase("oneplus")) {
                            intent.setComponent(new ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"));
                        } else {
                            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + packageName));
                        }
                        try {
                            startActivity(intent);
                        } catch (Exception e) {
                            intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + packageName));
                            startActivity(intent);
                        }
                    }
                })
                .setNegativeButton("OK", null)
                .setCancelable(true)
                .create();

        dialog.show();

        // Auto-dismiss after 30 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        }, 30000); // 30 seconds
    }

    private void startNotificationService() {
        Log.d(TAG, "onCreate: Starting BatteryTempService");
        Intent intent = new Intent(this, BatteryTempService.class);
        startForegroundService(intent);
    }

    private void stopNotificationService() {
        Log.d(TAG, "stopNotificationService: Stopping BatteryTempService");
        // Stop logging if it's active
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isLogging = prefs.getBoolean("IS_LOGGING", false);
        if (isLogging) {
            Intent stopLoggingIntent = new Intent(this, BatteryTempService.class);
            stopLoggingIntent.setAction("STOP_LOGGING");
            startService(stopLoggingIntent);

            // Update preferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("IS_LOGGING", false);
            editor.apply();
        }

        // Stop the service
        Intent intent = new Intent(this, BatteryTempService.class);
        stopService(intent);
    }

    private Runnable updatetemperatureTask = new Runnable() {
        @Override
        public void run() {
            float batteryTemperature = BatteryUtils.getBatteryTemperatureFloat(MainActivity.this);

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String scale = prefs.getString(TEMP_SCALE_KEY, "°C");
            String displayTemp = convertTemperature(batteryTemperature, scale);

            temperatureTextView.setText("🌡 " + displayTemp);
            temperatureTextView.setTextColor(colors[colorIndex]);
            colorIndex = (colorIndex + 1) % colors.length;

            // Update chart with Celsius value (core logic unchanged)
            tempChart.addValue(batteryTemperature);

            handler.postDelayed(this, 2000);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startNotificationService();
            } else {
                Log.w(TAG, "Notification permission denied");
            }
        }
    }


    private void startLogging() {
        String durationStr = durationEditText.getText().toString().trim();
        if (durationStr.isEmpty()) {
            durationEditText.setError(getString(R.string.please_enter_duration));
            return;
        }

        try {
            int duration = Integer.parseInt(durationStr);
            String timeUnit = timeUnitSpinner.getSelectedItem().toString();

            Intent intent = new Intent(this, BatteryTempService.class);
            intent.setAction("START_LOGGING");
            intent.putExtra("DURATION", duration);
            intent.putExtra("TIME_UNIT", timeUnit);
            startService(intent);

            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean("IS_LOGGING", true);
            editor.apply();

            updateLoggingButtonText();
        } catch (NumberFormatException e) {
            durationEditText.setError(getString(R.string.please_enter_valid_number));
        }
    }

    private void stopLogging() {
        Intent intent = new Intent(this, BatteryTempService.class);
        intent.setAction("STOP_LOGGING");
        startService(intent);

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("IS_LOGGING", false);
        editor.apply();

        updateLoggingButtonText();
    }

    private void updateLoggingButtonText() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isLogging = prefs.getBoolean("IS_LOGGING", false);
        startStopLoggingButton.setText(isLogging ? getString(R.string.stop_logging) : getString(R.string.start_logging));
    }


    private void saveTemperatureThreshold() {
        try {
            String thresholdStr = temperatureThresholdEditText.getText().toString().trim();
            if (!thresholdStr.isEmpty()) {
                int threshold = Integer.parseInt(thresholdStr);

                // Convert threshold to Celsius based on current scale
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String scale = prefs.getString(TEMP_SCALE_KEY, "°C");

                float thresholdInCelsius = threshold;
                boolean isValid = false;

                if (scale.equals("°F") || scale.equals("F")) {
                    // -30°C = -22°F, 60°C = 140°F
                    if (threshold >= -22 && threshold <= 140) {
                        thresholdInCelsius = (float) (Math.round(((threshold - 32f) * 5f / 9f) * 10f) / 10f);
                        isValid = true;
                    }
                } else if (scale.equals("K")) {
                    // -30°C = 243K, 60°C = 333K
                    if (threshold >= 243 && threshold <= 333) {
                        thresholdInCelsius = (float) (Math.round((threshold - 273.15f) * 10f) / 10f);
                        isValid = true;
                    }
                } else {
                    // Celsius: -30 to 60
                    if (threshold >= -30 && threshold <= 60) {
                        isValid = true;
                    }
                }

                if (isValid) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putFloat(TEMP_THRESHOLD_KEY, thresholdInCelsius);
                    editor.apply();
                }
            }
        } catch (NumberFormatException e) {
            // Reset to default if invalid input
            temperatureThresholdEditText.setText("50");
        }
    }


    private String convertTemperature(float celsius, String scale) {
        if (scale.equals("°F") || scale.equals("F")) {
            int fahrenheit = Math.round(celsius * 9f / 5f + 32f);
            return fahrenheit + " ";
        } else if (scale.equals("K")) {
            int kelvin = Math.round(celsius + 273.15f);
            return kelvin + " ";
        } else {
            return String.format("%.1f ", celsius);
        }
    }

    private String getScaleSymbol(String scale) {
        if (scale.equals("°F") || scale.equals("F")) return "°F";
        if (scale.equals("K")) return "K";
        return "°C";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updatetemperatureTask);
        handler.removeCallbacks(saveThresholdRunnable);
        if (loggingFinishedReceiver != null) {
            unregisterReceiver(loggingFinishedReceiver);
        }
    }


}