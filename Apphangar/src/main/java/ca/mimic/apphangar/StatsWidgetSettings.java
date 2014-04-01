package ca.mimic.apphangar;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import ca.mimic.apphangar.Settings.PrefsGet;

public class StatsWidgetSettings extends PreferenceActivity {

    static PrefsGet prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PrefsGet(getSharedPreferences(getPackageName(), Context.MODE_PRIVATE));
        // addPreferencesFromResource(R.layout.stats_widget_settings);
        setContentView(R.layout.stats_widget_settings);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
