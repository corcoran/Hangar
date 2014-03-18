package ca.mimic.apphangar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootStart extends BroadcastReceiver {
    SharedPreferences prefs;

    public void onReceive(Context context, Intent arg1) {
        prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);

        if (prefs.getBoolean(SettingsActivity.BOOT_PREFERENCE, SettingsActivity.BOOT_DEFAULT)) {
            Log.d(SettingsActivity.TAG, "Starting WatchfulService on boot");

            Intent intent = new Intent(context, WatchfulService.class);
            context.startService(intent);
        } else {
            Log.d(SettingsActivity.TAG, "Start on boot [" + prefs.getBoolean(SettingsActivity.BOOT_PREFERENCE, SettingsActivity.BOOT_DEFAULT)  + "] or Notification disabled [" + prefs.getBoolean(SettingsActivity.TOGGLE_PREFERENCE, SettingsActivity.TOGGLE_DEFAULT) + "]");
        }
    }
}