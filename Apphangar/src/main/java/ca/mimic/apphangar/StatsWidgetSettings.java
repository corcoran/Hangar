package ca.mimic.apphangar;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import ca.mimic.apphangar.Settings.PrefsGet;

public class StatsWidgetSettings extends PreferenceActivity {

    final static int WIDGET_3X3_DEFAULT_APPSNO = 6;
    final static int WIDGET_3X3_DEFAULT_APPSNO_LS = 3;

    static PrefsGet prefs;
    static Context mContext;
    static Bundle extras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PrefsGet(getSharedPreferences("StatsWidget", Context.MODE_PRIVATE));

        mContext = getApplicationContext();
        extras = getIntent().getExtras();

        setTitle(R.string.stats_widget_name);
        setResult(RESULT_CANCELED);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new StatsWidgetFragment().newInstance()).commit();

        ListView v = getListView();
        v.setFooterDividersEnabled(false);

    }
    public static class StatsWidgetFragment extends PreferenceFragment {

        CheckBoxPreference divider_preference;
        ColorPickerPreference background_color_preference;
        SwitchPreference apps_by_widget_size_preference;
        UpdatingListPreference appnos_preference;
        UpdatingListPreference appnos_ls_preference;

        public static StatsWidgetFragment newInstance() {
            return new StatsWidgetFragment();
        }

        public StatsWidgetFragment() {
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            LinearLayout v = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            v.setLayoutParams(params);

            LinearLayout footer = (LinearLayout) inflater.inflate(R.layout.widget_footer, v, false);
            footer.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
            Button createButton = (Button) footer.findViewById(R.id.placementButton);
            if (extras == null) {
                createButton.setText(R.string.reconfigure_button_name);
            } else {
                createButton.setText(R.string.button_name);
            }
            createButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int mAppWidgetId;
                    savePrefs();
                    if (extras != null) {
                        mAppWidgetId = extras.getInt(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                AppWidgetManager.INVALID_APPWIDGET_ID);
                        Intent resultValue = new Intent();
                        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                        getActivity().setResult(RESULT_OK, resultValue);
                        Toast.makeText(mContext, R.string.reconfigure_reminder, Toast.LENGTH_LONG).show();
                        getActivity().finish();
                    } else {
                        Tools.updateWidget(mContext);
                        getActivity().finish();
                    }
                }
            });

            v.addView(footer);
            return v;
        }
        public void savePrefs() {
            final SharedPreferences.Editor mEditor = prefs.editorGet();
            mEditor.putBoolean(Settings.DIVIDER_PREFERENCE, divider_preference.isChecked());
            mEditor.putBoolean(Settings.APPS_BY_WIDGET_SIZE_PREFERENCE, apps_by_widget_size_preference.isChecked());
            mEditor.putString(Settings.STATS_WIDGET_APPSNO_PREFERENCE, appnos_preference.getValue());
            mEditor.putString(Settings.STATS_WIDGET_APPSNO_LS_PREFERENCE, appnos_ls_preference.getValue());
            int intHex = ColorPickerPreference.convertToColorInt(String.valueOf(background_color_preference.getSummary()));
            mEditor.putInt(Settings.BACKGROUND_COLOR_PREFERENCE, intHex);
            mEditor.apply();
        }
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.layout.stats_widget_settings);
            int defaultAppsNo = WIDGET_3X3_DEFAULT_APPSNO;
            int defaultAppsNoLs = WIDGET_3X3_DEFAULT_APPSNO_LS;

            SharedPreferences mPrefs = prefs.prefsGet();

            divider_preference = (CheckBoxPreference)findPreference(Settings.DIVIDER_PREFERENCE);
            divider_preference.setChecked(mPrefs.getBoolean(Settings.DIVIDER_PREFERENCE, Settings.DIVIDER_DEFAULT));
            divider_preference.setOnPreferenceChangeListener(changeListener);

            apps_by_widget_size_preference = (SwitchPreference)findPreference(Settings.APPS_BY_WIDGET_SIZE_PREFERENCE);
            apps_by_widget_size_preference.setChecked(mPrefs.getBoolean(Settings.APPS_BY_WIDGET_SIZE_PREFERENCE, Settings.APPS_BY_WIDGET_SIZE_DEFAULT));
            apps_by_widget_size_preference.setOnPreferenceChangeListener(changeListener);

            background_color_preference = (ColorPickerPreference) findPreference(Settings.BACKGROUND_COLOR_PREFERENCE);
            int intColor = mPrefs.getInt(Settings.BACKGROUND_COLOR_PREFERENCE, Settings.BACKGROUND_COLOR_DEFAULT);
            String hexColor = String.format("#%08x", (intColor));
            background_color_preference.setSummary(hexColor);
            background_color_preference.setOnPreferenceChangeListener(changeListener);

            appnos_preference = (UpdatingListPreference)findPreference(Settings.STATS_WIDGET_APPSNO_PREFERENCE);
            appnos_preference.setValue(mPrefs.getString(extras == null ? Settings.STATS_WIDGET_APPSNO_PREFERENCE : Integer.toString(defaultAppsNo),
                    Integer.toString(defaultAppsNo)));
            appnos_preference.setOnPreferenceChangeListener(changeListener);
            if (apps_by_widget_size_preference.isChecked()) {
                appnos_preference.setSummary(R.string.automatic_appsno);
            }

            appnos_ls_preference = (UpdatingListPreference)findPreference(Settings.STATS_WIDGET_APPSNO_LS_PREFERENCE);
            appnos_ls_preference.setValue(mPrefs.getString(extras == null ? Settings.STATS_WIDGET_APPSNO_LS_PREFERENCE : Integer.toString(defaultAppsNoLs),
                    Integer.toString(defaultAppsNoLs)));
            appnos_ls_preference.setOnPreferenceChangeListener(changeListener);
            if (apps_by_widget_size_preference.isChecked()) {
                appnos_ls_preference.setSummary(R.string.automatic_appsno);
            }
        }
        Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                if (preference.getKey().equals(Settings.BACKGROUND_COLOR_PREFERENCE)) {
                    String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(newValue)));
                    preference.setSummary(hex);
                } else if (preference.getKey().equals(Settings.APPS_BY_WIDGET_SIZE_PREFERENCE)) {
                    if (apps_by_widget_size_preference.isChecked()) {
                        appnos_preference.setSummary(R.string.automatic_appsno);
                        appnos_ls_preference.setSummary(R.string.automatic_appsno);
                    } else {
                        appnos_preference.goDefault(String.format(getResources().getString(R.string.summary_stats_widget_appsno_preference), appnos_preference.getValue()));
                        appnos_ls_preference.goDefault(String.format(getResources().getString(R.string.summary_stats_widget_appsno_ls_preference), appnos_ls_preference.getValue()));
                    }
                } else if (preference.getKey().equals(Settings.STATS_WIDGET_APPSNO_PREFERENCE)) {
                    appnos_preference.goDefault(String.format(getResources().getString(R.string.summary_stats_widget_appsno_preference), (String) newValue));
                } else if (preference.getKey().equals(Settings.STATS_WIDGET_APPSNO_LS_PREFERENCE)) {
                    appnos_ls_preference.goDefault(String.format(getResources().getString(R.string.summary_stats_widget_appsno_ls_preference), (String) newValue));
                }
                return true;
            }
        };
    }
}
