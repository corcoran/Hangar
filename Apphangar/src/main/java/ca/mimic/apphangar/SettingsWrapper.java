package ca.mimic.apphangar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import ca.mimic.apphangar.Settings.PrefsGet;

public class SettingsWrapper extends Activity {

    static PrefsGet prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PrefsGet(getSharedPreferences("StatsWidget", Context.MODE_PRIVATE));
        SharedPreferences mPrefs = prefs.prefsGet();
        SharedPreferences.Editor mEditor = prefs.editorGet();

        String hangarVersion = null;
        try {
            hangarVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            String[] versionArray = hangarVersion.split("\\."); // Only get major.minor
            hangarVersion = versionArray[0] + "." + versionArray[1];
        } catch (PackageManager.NameNotFoundException e) {}

        String whichVersion = mPrefs.getString(Settings.VERSION_CHECK, null);

        Intent i;
        if (whichVersion != null && whichVersion.equals(hangarVersion)) {
            i = new Intent(this, Settings.class);
        } else {
            i = new Intent(this, ChangeLog.class);
            mEditor.putString(Settings.VERSION_CHECK, hangarVersion);
            mEditor.apply();
        }

        i.addCategory(Intent.CATEGORY_LAUNCHER);
        startActivity(i);
        finish();
    }
}
