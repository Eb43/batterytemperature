package com.batterytemperature;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
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


public class BatteryTempService extends Service {

    private static final String CHANNEL_ID = "BateryTempChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "BatteryTempService";

    private Handler handler;
    private Runnable runnable;
    private Notification.Builder notificationBuilder;
    private NotificationManager notificationManager;


    private static final String PREFS_NAME = "MyPrefs";
    private static final String RADIO_CHOSEN_BLACK_KEY = "RadioChosenBlack";
    private int TEXT_COLOR;


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

        // Load the saved text color preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean radioChosenBlack = prefs.getBoolean(RADIO_CHOSEN_BLACK_KEY, true);
        TEXT_COLOR = radioChosenBlack ? Color.BLACK : Color.WHITE;

        handler = new Handler(Looper.getMainLooper());

        notificationBuilder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Battery temperature")
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        notificationBuilder.setContentIntent(pendingIntent);

        runnable = new Runnable() {
            @Override
            public void run() {
                updateNotification();
                handler.postDelayed(this, 60000); // Update every X seconds
            }
        };
        handler.post(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Service started");
        startForeground(NOTIFICATION_ID, createNotification("0.0 ℃"));
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Service destroyed");
        handler.removeCallbacks(runnable);
        notificationManager.cancel(NOTIFICATION_ID);
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



        RemoteViews notificationExpandedLayout = new RemoteViews(getPackageName(), R.layout.notification_expanded);
        notificationExpandedLayout.setTextViewText(R.id.notification_text_expanded, "\uD83C\uDF21 Battery temperature: " + temperatureText + " ℃");

        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification);
        notificationLayout.setTextViewText(R.id.notification_text, getCurrentBatteryTemperature() + " ℃ ");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String shorttemperatureText = temperatureText.length() > 3 ? temperatureText.substring(0, 2) : temperatureText;
        Bitmap temperatureBitmap = BitmapUtils.textToBitmap(shorttemperatureText+"°", TEXT_COLOR);
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
        notificationManager.notify(NOTIFICATION_ID, createNotification(temperatureText));
    }

    private String getCurrentBatteryTemperature() {
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int temperature = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) : -1;
        Log.d(TAG, "getCurrentBatteryTemperature: Current battery temperature is " + temperature);
        return temperature != -1 ? String.valueOf((temperature / 10.0)) : "Unknown";
    }
}