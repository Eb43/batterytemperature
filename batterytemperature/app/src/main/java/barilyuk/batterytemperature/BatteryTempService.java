package barilyuk.batterytemperature;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.graphics.drawable.Icon;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;
import android.app.UiModeManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.os.Environment;




public class BatteryTempService extends Service {

    private static final String CHANNEL_ID = "BateryTempChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "BatteryTempService";

    private BroadcastReceiver batteryReceiver;
    private Handler handler;
    private Runnable runnable;
    private Notification.Builder notificationBuilder;
    private NotificationManager notificationManager;

    private long loggingEndTime;
    private boolean isLogging = false;
    private String lastLoggedTemperature = null;

    private static final String PREFS_NAME = "MyPrefs";
    private static final String RADIO_CHOSEN_BLACK_KEY = "RadioChosenBlack";
    private int TEXT_COLOR;


    private MediaPlayer alarmPlayer;
    private android.media.Ringtone ringtone;
    private boolean isAlarmPlaying = false;
    private static final String TEMP_THRESHOLD_KEY = "TempThreshold";
    private static final String ENABLE_TEMP_ALARM_KEY = "EnableTempAlarm";
    private Handler alarmHandler = new Handler();
    private Runnable alarmStopRunnable;
    private static final long ALARM_AUTO_STOP_DURATION = 20000; // 20 seconds

    private float lastTriggeredThreshold = -999f;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Service created");
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateNotification(); // reuse existing method
            }
        };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Load the saved text color preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean radioChosenBlack = prefs.getBoolean(RADIO_CHOSEN_BLACK_KEY, true);
        TEXT_COLOR = radioChosenBlack ? Color.BLACK : Color.WHITE;

// Initialize logging state
        isLogging = prefs.getBoolean("IS_LOGGING", false);
        if (isLogging) {
            // Check if logging should still be active (in case service was restarted)
            long endTime = prefs.getLong("LOGGING_END_TIME", 0);
            if (System.currentTimeMillis() < endTime) {
                loggingEndTime = endTime;
                lastLoggedTemperature = getCurrentBatteryTemperature();
            } else {
                // Logging period has expired
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("IS_LOGGING", false);
                editor.apply();
                isLogging = false;
            }
        }
        //handler = new Handler(Looper.getMainLooper());

        notificationBuilder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.battery_temperature_notification_title))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        notificationBuilder.setContentIntent(pendingIntent);

        /*
        //Updating battery value every .. seconds
        runnable = new Runnable() {
            @Override
            public void run() {
                updateNotification();
                handler.postDelayed(this, 60000); // Update every X seconds
            }
        };
        handler.post(runnable);
         */
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Service started");


        // Handle stop alarm command
        if (intent != null && "STOP_ALARM".equals(intent.getAction())) {
            stopAlarm();
            return START_STICKY;
        }


        //Update icon color immediately on radiobutton switch
        if (intent != null && "UPDATE_NOTIFICATION".equals(intent.getAction())) {
            updateNotification(); // rebuild notification with new color
            return START_STICKY;
        }

        // Handle logging commands
        if (intent != null && "START_LOGGING".equals(intent.getAction())) {
            int duration = intent.getIntExtra("DURATION", 1);
            String timeUnit = intent.getStringExtra("TIME_UNIT");
            startLogging(duration, timeUnit);
            return START_STICKY;
        }

        if (intent != null && "STOP_LOGGING".equals(intent.getAction())) {
            stopLogging();
            return START_STICKY;
        }

        startForeground(NOTIFICATION_ID, createNotification(getCurrentBatteryTemperature()));
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Service destroyed");
        stopAlarm();
        alarmHandler.removeCallbacksAndMessages(null);
        //handler.removeCallbacks(runnable);
        notificationManager.cancel(NOTIFICATION_ID);

        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);
        }


    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "createNotificationChannel: Creating notification channel");
            CharSequence name = "Battery temperature Notification Channel";
            String description = "Channel for displaying battery temperature notifications";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "createNotificationChannel: Notification channel created");
        }
    }



    private Notification createNotification(String temperatureText) {
        Log.d(TAG, "createNotification: Creating notification with temperature: " + temperatureText);


        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isRadioChosenBlack = prefs.getBoolean(RADIO_CHOSEN_BLACK_KEY, true); // Default to true if not set
        int TEXT_COLOR = isRadioChosenBlack ? Color.BLACK : Color.WHITE;


        UiModeManager uiModeManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        boolean isDarkMode = (uiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES
                || uiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_AUTO);

        RemoteViews notificationExpandedLayout = new RemoteViews(getPackageName(), R.layout.notification_expanded);
        notificationExpandedLayout.setTextViewText(R.id.notification_text_expanded, getString(R.string.battery_temp_with_value, temperatureText));
        if (isDarkMode) notificationExpandedLayout.setTextColor(R.id.notification_text_expanded, Color.WHITE);

        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification);
        notificationLayout.setTextViewText(R.id.notification_text, getCurrentBatteryTemperature() + " ℃ ");
        if (isDarkMode) notificationLayout.setTextColor(R.id.notification_text, Color.WHITE);


        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

// Remove anything that is not part of the number (digits, minus sign, dot)
        String numericPart = temperatureText.replaceAll("[^\\d.-]", "");
// Parse float safely
        float temp = Float.parseFloat(numericPart);
// Take only the integer part
        int tempInt = (int) temp;
// Convert to string
        String shorttemperatureText = String.valueOf(tempInt);

        Bitmap temperatureBitmap = BitmapUtils.textToBitmap(shorttemperatureText, TEXT_COLOR);
        Icon icon = Icon.createWithBitmap(temperatureBitmap);


        return new Notification.Builder(this, CHANNEL_ID)
                .setCustomContentView(notificationLayout)
                .setSmallIcon(icon)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_LOW)
                .setCustomBigContentView(notificationExpandedLayout)
                .setStyle(new Notification.DecoratedCustomViewStyle())
                .build();

    }

    private void updateNotification() {
        String temperatureText = getCurrentBatteryTemperature();  // + " ℃"
        Log.d(TAG, "updateNotification: Updating notification with temperature: " + temperatureText);

        // Check temperature alarm
        try {
            float currentTemp = Float.parseFloat(temperatureText);
            checkTemperatureAlarm(currentTemp);
        } catch (NumberFormatException e) {
            // Ignore parsing errors
        }

        // Check if logging is active and temperature has changed
        if (isLogging && System.currentTimeMillis() < loggingEndTime) {
            if (!temperatureText.equals(lastLoggedTemperature)) {
                logTemperature(temperatureText, "Temperature Change");
                lastLoggedTemperature = temperatureText;
            }
        } else if (isLogging && System.currentTimeMillis() >= loggingEndTime) {
            // Auto-stop logging when duration expires with final log entry
            String currentTemp = getCurrentBatteryTemperature();
            logTemperature(currentTemp, "Logging Expired");
            stopLogging();
        }

        notificationManager.notify(NOTIFICATION_ID, createNotification(temperatureText));
    }

    private String getCurrentBatteryTemperature() {
        return BatteryUtils.getBatteryTemperatureString(this);
    }


    private void startLogging(int duration, String timeUnit) {
        long durationMillis = convertToMillis(duration, timeUnit);
        loggingEndTime = System.currentTimeMillis() + durationMillis;
        isLogging = true;

        // Save logging state and end time
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putLong("LOGGING_END_TIME", loggingEndTime);
        editor.apply();

        // Log current temperature when starting
        String currentTemp = getCurrentBatteryTemperature();
        logTemperature(currentTemp, "Logging Started");
        lastLoggedTemperature = currentTemp;

        Log.d(TAG, "Started logging for " + duration + " " + timeUnit);
    }

    private void stopLogging() {
        // Log current temperature when stopping (before setting isLogging to false)
        if (isLogging) {
            String currentTemp = getCurrentBatteryTemperature();
            logTemperature(currentTemp, "Logging Stopped");
        }

        isLogging = false;

        // Update shared preferences
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("IS_LOGGING", false);
        editor.apply();

        Log.d(TAG, "Stopped logging");

        Intent broadcastIntent = new Intent("LOGGING_FINISHED");
        sendBroadcast(broadcastIntent);
    }

    private long convertToMillis(int duration, String timeUnit) {
        switch (timeUnit.toLowerCase()) {
            case "minutes": return duration * 60L * 1000L;
            case "hours": return duration * 60L * 60L * 1000L;
            case "days": return duration * 24L * 60L * 60L * 1000L;
            case "months": return duration * 30L * 24L * 60L * 60L * 1000L; // Approximate
            default: return duration * 60L * 1000L; // Default to minutes
        }
    }

    private void logTemperature(String temperature, String event) {
        try {
            File logFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "battery_temperature_log.csv");

            // Create header if file doesn't exist
            if (!logFile.exists()) {
                FileWriter writer = new FileWriter(logFile, false);
                writer.append("Date,Time,Temperature,Event\n");
                writer.close();
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Date now = new Date();
            String date = dateFormat.format(now);
            String time = timeFormat.format(now);

            String translatedEvent = event;
            // Translate common events
            if ("Logging Started".equals(event)) {
                translatedEvent = getString(R.string.logging_started);
            } else if ("Logging Stopped".equals(event)) {
                translatedEvent = getString(R.string.logging_stopped);
            } else if ("Logging Expired".equals(event)) {
                translatedEvent = getString(R.string.logging_expired);
            } else if ("Temperature Change".equals(event)) {
                translatedEvent = getString(R.string.temperature_change);
            }

            String logEntry = date + "," + time + "," + temperature + "," + event + "\n";

            FileWriter writer = new FileWriter(logFile, true);
            writer.append(logEntry);
            writer.close();

            Log.d(TAG, "Temperature logged: " + logEntry.trim());
        } catch (IOException e) {
            Log.e(TAG, "Error writing temperature log", e);
        }
    }


    private void checkTemperatureAlarm(float currentTemp) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isAlarmEnabled = prefs.getBoolean(ENABLE_TEMP_ALARM_KEY, false);
        float threshold = prefs.getFloat(TEMP_THRESHOLD_KEY, 50.0f);

        if (isAlarmEnabled && currentTemp >= threshold) {
            // Allow alarm if no alarm is playing OR threshold has changed significantly
            if (!isAlarmPlaying || Math.abs(threshold - lastTriggeredThreshold) > 1.0f) {
                // Stop existing alarm if playing with different threshold
                if (isAlarmPlaying && Math.abs(threshold - lastTriggeredThreshold) > 1.0f) {
                    stopAlarm();
                }

                lastTriggeredThreshold = threshold;
                triggerTemperatureAlarm(currentTemp, threshold);
            }
        }
    }

    private void triggerTemperatureAlarm(float currentTemp, float threshold) {
        // Show system notification instead of toast for background operation
        showAlarmNotification(currentTemp, threshold);

        // Play alarm sound
        // Try Ringtone instead of MediaPlayer
        try {
            android.media.RingtoneManager ringtoneManager = new android.media.RingtoneManager(this);
            android.net.Uri alarmUri = ringtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);

            if (alarmUri == null) {
                alarmUri = ringtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            }

            if (alarmUri != null) {
                ringtone = android.media.RingtoneManager.getRingtone(this, alarmUri);
                if (ringtone != null) {
                    ringtone.play();
                    isAlarmPlaying = true;
                    Log.d(TAG, "Ringtone started playing");

                    // Auto-stop after 30 seconds
                    alarmStopRunnable = new Runnable() {
                        @Override
                        public void run() {
                            stopAlarm();
                            Log.d(TAG, "Ringtone auto-stopped");
                        }
                    };
                    alarmHandler.postDelayed(alarmStopRunnable, ALARM_AUTO_STOP_DURATION);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing ringtone", e);
        }
    }

    private void showAlarmNotification(float currentTemp, float threshold) {
        NotificationChannel alarmChannel = new NotificationChannel(
                "ALARM_CHANNEL",
                getString(R.string.temperature_alarm_title),
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager.createNotificationChannel(alarmChannel);

        Intent stopAlarmIntent = new Intent(this, BatteryTempService.class);
        stopAlarmIntent.setAction("STOP_ALARM");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification alarmNotification = new NotificationCompat.Builder(this, "ALARM_CHANNEL")
                .setContentTitle(getString(R.string.temperature_alarm_title))
                .setContentText(getString(R.string.temperature_alarm_text, currentTemp, threshold))
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_media_pause, getString(R.string.stop_alarm), stopPendingIntent)
                .build();

        notificationManager.notify(999, alarmNotification);
    }

    private void stopAlarm() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
            ringtone = null;
            isAlarmPlaying = false;
            Log.d(TAG, "Ringtone stopped");
        }

        // Cancel auto-stop timer
        if (alarmStopRunnable != null) {
            alarmHandler.removeCallbacks(alarmStopRunnable);
            alarmStopRunnable = null;
        }

        // Cancel alarm notification
        notificationManager.cancel(999);

        // Reset threshold tracking when alarm stops
        lastTriggeredThreshold = -999f;
    }
}