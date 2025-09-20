package barilyuk.batterytemperature;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryUtils {
    private static final String TAG = "BatteryUtils";

    public static String getBatteryTemperatureString(Context context) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int temperature = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) : -1;
        Log.d(TAG, "getBatteryTemperatureString: Current battery temperature is " + temperature);
        return temperature != -1 ? String.valueOf((temperature / 10.0)) : "Unknown";
    }

    public static float getBatteryTemperatureFloat(Context context) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int temperature = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) : -1;
        return temperature != -1 ? temperature / 10.0f : -1f;
    }
}