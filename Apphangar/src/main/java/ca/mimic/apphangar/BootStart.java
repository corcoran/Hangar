package ca.mimic.apphangar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootStart extends BroadcastReceiver {
    SharedPreferences prefs;

    public void onReceive(Context context, Intent arg1) {
        prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);

        if (prefs.getBoolean(Settings.BOOT_PREFERENCE, Settings.BOOT_DEFAULT)) {
            Tools.HangarLog("Starting WatchfulService on boot");

            Intent intent = new Intent(context, WatchfulService.class);
            context.startService(intent);
        } else {
            Tools.HangarLog("Start on boot [" + prefs.getBoolean(Settings.BOOT_PREFERENCE, Settings.BOOT_DEFAULT)  + "] or Notification disabled [" + prefs.getBoolean(Settings.TOGGLE_PREFERENCE, Settings.TOGGLE_DEFAULT) + "]");
        }
    }
}