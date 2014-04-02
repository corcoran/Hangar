package ca.mimic.apphangar;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import ca.mimic.apphangar.Settings.PrefsGet;

public class StatsWidgetSettings extends PreferenceActivity {

    static PrefsGet prefs;

    CheckBoxPreference divider_preference;
    ColorPickerPreference background_color_preference;
    UpdatingListPreference appnos_preference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PrefsGet(getSharedPreferences("StatsWidget", Context.MODE_PRIVATE));
        addPreferencesFromResource(R.layout.stats_widget_settings);
        setTitle(R.string.stats_widget_name);
        setResult(RESULT_CANCELED);

        ListView v = getListView();
        v.setFooterDividersEnabled(true);

        LayoutInflater inflater =
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout footer = (LinearLayout) inflater.inflate(R.layout.stats_widget_footer, v, false);
        footer.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
        Button createButton = (Button) footer.findViewById(R.id.placementButton);
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            createButton.setText(R.string.reconfigure_button_name);
        } else {
            createButton.setText(R.string.button_name);
        }
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle extras = getIntent().getExtras();
                int mAppWidgetId;
                if (extras != null) {
                    mAppWidgetId = extras.getInt(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID);
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    setResult(RESULT_OK, resultValue);
                    Toast.makeText(getApplicationContext(), R.string.reconfigure_reminder, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    finish();
                }
            }
        });

        v.addFooterView(footer, null, false);
        initializePrefs();
    }
    void initializePrefs() {
        SharedPreferences mPrefs = prefs.prefsGet();

        divider_preference = (CheckBoxPreference)findPreference(Settings.DIVIDER_PREFERENCE);
        divider_preference.setChecked(mPrefs.getBoolean(Settings.DIVIDER_PREFERENCE, Settings.DIVIDER_DEFAULT));
        divider_preference.setOnPreferenceChangeListener(changeListener);

        background_color_preference = (ColorPickerPreference) findPreference(Settings.BACKGROUND_COLOR_PREFERENCE);
        int intColor = mPrefs.getInt(Settings.BACKGROUND_COLOR_PREFERENCE, Settings.BACKGROUND_COLOR_DEFAULT);
        String hexColor = String.format("#%08x", (intColor));
        background_color_preference.setSummary(hexColor);
        background_color_preference.setOnPreferenceChangeListener(changeListener);

        appnos_preference = (UpdatingListPreference)findPreference(Settings.APPSNO_PREFERENCE);
        appnos_preference.setValue(mPrefs.getString(Settings.APPSNO_PREFERENCE, Integer.toString(Settings.APPSNO_DEFAULT)));
        appnos_preference.setOnPreferenceChangeListener(changeListener);
    }
    Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object newValue) {
            final SharedPreferences.Editor mEditor = prefs.editorGet();

            if (preference.getKey().equals(Settings.DIVIDER_PREFERENCE)) {
                mEditor.putBoolean(Settings.DIVIDER_PREFERENCE, (Boolean) newValue);
            } else if (preference.getKey().equals(Settings.APPSNO_PREFERENCE)) {
                mEditor.putString(Settings.APPSNO_PREFERENCE, (String) newValue);
            } else if (preference.getKey().equals(Settings.BACKGROUND_COLOR_PREFERENCE)) {
                String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                mEditor.putInt(Settings.BACKGROUND_COLOR_PREFERENCE, intHex);
            }
            mEditor.apply();
            return true;
        }
    };
}
