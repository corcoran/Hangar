/*
 * Copyright © 2014 Jeff Corcoran
 *
 * This file is part of Hangar.
 *
 * Hangar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hangar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hangar.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package ca.mimic.apphangar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import org.json.JSONException;
import org.json.JSONObject;

public class Settings extends Activity implements ActionBar.TabListener {

    SectionsPagerAdapter mSectionsPagerAdapter;

    ViewPager mViewPager;
    private static GetFragments mGetFragments;
    private static IWatchfulService s;
    private static TasksDataSource db;

    final static String VERSION_CHECK = "version_check";

    final static int GENERAL_TAB = 1;
    final static int BEHAVIOR_TAB = 2;
    final static int APPEARANCE_TAB = 3;
    final static int APPS_TAB = 0;

    final static String DIVIDER_PREFERENCE = "divider_preference";
    final static String ROW_DIVIDER_PREFERENCE = "row_divider_preference";
    final static String APPSNO_PREFERENCE = "appsno_preference";
    final static String PRIORITY_PREFERENCE = "priority_preference";
    final static String TOGGLE_PREFERENCE = "toggle_preference";
    final static String BOOT_PREFERENCE = "boot_preference";
    final static String WEIGHTED_RECENTS_PREFERENCE = "weighted_recents_preference";
    final static String WEIGHT_PRIORITY_PREFERENCE = "weight_priority_preference";
    final static String COLORIZE_PREFERENCE = "colorize_preference";
    final static String ICON_COLOR_PREFERENCE = "icon_color_preference";
    final static String STATUSBAR_ICON_PREFERENCE = "statusbar_icon_preference";
    final static String BACKGROUND_COLOR_PREFERENCE = "background_color_preference";
    final static String STATS_WIDGET_APPSNO_PREFERENCE = "stats_widget_appsno_preference";
    final static String STATS_WIDGET_APPSNO_LS_PREFERENCE = "stats_widget_appsno_ls_preference";
    final static String APPS_BY_WIDGET_SIZE_PREFERENCE = "apps_by_widget_size_preference";
    final static String ICON_SIZE_PREFERENCE = "icon_size_preference";
    final static String ALIGNMENT_PREFERENCE = "alignment_preference";
    final static String ICON_PACK_PREFERENCE = "icon_pack_preference";
    final static String SECOND_ROW_PREFERENCE = "second_row_preference";
    final static String PINNED_SORT_PREFERENCE = "pinned_sort_preference";
    final static String PINNED_PLACEMENT_PREFERENCE = "pinned_placement_preference";
    final static String IGNORE_PINNED_PREFERENCE = "ignore_pinned_preference";
    final static String APPLIST_TOP_PREFERENCE = "applist_top_preference";
    final static String APPLIST_SORT_PREFERENCE = "applist_sort_preference";
    final static String SMART_NOTIFICATION_PREFERENCE = "smart_notification_preference";
    final static String MORE_APPS_PREFERENCE = "more_apps_preference";
    final static String MORE_APPS_PAGES_PREFERENCE = "more_apps_pages_preference";
    final static String MORE_APPS_ICON_PREFERENCE = "more_apps_icon_preference";
    final static String ROUNDED_CORNERS_PREFERENCE = "rounded_corners_preference";
    final static String FLOATING_WINDOWS_PREFERENCE = "floating_windows_preference";

    protected static Settings mInstance;
    protected static AppsRowItem mIconTask;
    protected static boolean isBound = false;
    protected static boolean mLaunchedPaypal = false;
    protected static Display display;

    static AppsRowAdapter mAppRowAdapter;
    protected static boolean completeRedraw;
    protected static ListView lv;


    static PrefsGet prefs;
    static Context mContext;
    static ServiceCall myService;
    static IconPackUpdate iconPackUpdate;

    final static String PLAY_STORE_PACKAGENAME = "com.android.vending";
    final static String PLAY_STORE_SEARCH_URI = "market://search?q=icon+pack";

    final static String MORE_APPS_PACKAGE = "ca.mimic.apphangar.MoreApps";
    final static String MORE_APPS_ACTION = "ca.mimic.apphangar.action.MORE_APPS";
    final static int MORE_APPS_DRAWABLE_RESOURCE = R.drawable.ic_apps_plus;

    final static int FLOATING_WINDOWS_INTENT_FLAG = 0x00002000;

    final static boolean DIVIDER_DEFAULT = false;
    final static boolean ROW_DIVIDER_DEFAULT = true;
    final static boolean TOGGLE_DEFAULT = true;
    final static boolean BOOT_DEFAULT = true;
    final static boolean WEIGHTED_RECENTS_DEFAULT = true;
    final static boolean COLORIZE_DEFAULT = false;
    final static boolean APPS_BY_WIDGET_SIZE_DEFAULT = true;
    final static boolean SECOND_ROW_DEFAULT = false;
    final static boolean IGNORE_PINNED_DEFAULT = false;
    final static boolean SMART_NOTIFICATION_DEFAULT = true;
    final static boolean MORE_APPS_DEFAULT = false;
    final static boolean ROUNDED_CORNERS_DEFAULT = false;
    final static boolean FLOATING_WINDOWS_DEFAULT = false;

    final static int WEIGHT_PRIORITY_DEFAULT = 0;
    final static int APPSNO_DEFAULT = 8;
    final static int PRIORITY_DEFAULT = 2;
    final static int PRIORITY_BOTTOM = -2;
    final static int ICON_COLOR_DEFAULT = 0xffffffff;
    final static int BACKGROUND_COLOR_DEFAULT = 0x5e000000;
    final static int STATS_WIDGET_APPSNO_DEFAULT = 6;
    final static int STATS_WIDGET_APPSNO_LS_DEFAULT = 3;
    final static int APPS_WIDGET_APPSNO_DEFAULT = 6;
    final static int APPS_WIDGET_APPSNO_LS_DEFAULT = 13;
    final static int ALIGNMENT_DEFAULT = 16; // 16 is middle
    final static int PINNED_SORT_DEFAULT = 0;
    final static int PINNED_PLACEMENT_DEFAULT = 0;
    final static int MORE_APPS_PAGES_DEFAULT = 3;

    final static int TASKLIST_QUEUE_LIMIT = 100;
    final static int TASKLIST_QUEUE_SIZE = 35;
    final static int APPLIST_QUEUE_SIZE = 14;

    final static String STATUSBAR_ICON_WHITE_WARM = "**white_warm**";
    final static String STATUSBAR_ICON_WHITE_COLD = "**white_cold**";
    final static String STATUSBAR_ICON_WHITE_BLUE = "**white_blue**";
    final static String STATUSBAR_ICON_WHITE = "**white**";
    final static String STATUSBAR_ICON_BLACK_WARM = "**black_warm**";
    final static String STATUSBAR_ICON_BLACK_COLD = "**black_cold**";
    final static String STATUSBAR_ICON_BLACK_BLUE = "**black_blue**";
    final static String STATUSBAR_ICON_TRANSPARENT = "**transparent**";
    final static String STATUSBAR_ICON_NONE = "**none**";
    final static String STATUSBAR_ICON_DEFAULT = STATUSBAR_ICON_WHITE;

    final static String PINNED_APPS = "pinned_apps";
    final static int PINNED_PLACEMENT_LEFT = 0;

    final static int ICON_SIZE_DEFAULT = 1;
    final static int CACHED_ICON_SIZE = 72;
    final static int CACHED_NOTIFICATION_ICON_LIMIT = 20;
    final static String ACTION_ADW_PICK_ICON = "org.adw.launcher.icons.ACTION_PICK_ICON";

    final static int SERVICE_BUILD_TASKS = 0;
    final static int SERVICE_BUILD_REORDER_LAUNCH = 1;
    final static int SERVICE_CREATE_NOTIFICATIONS = 2;
    final static int SERVICE_DESTROY_NOTIFICATIONS = 3;

    final static int THANK_YOU_GOOGLE = 0;
    final static int THANK_YOU_PAYPAL = 1;

    final static int APPLIST_TOP_DEFAULT = 2;
    final static int APPLIST_SORT_DEFAULT = 0;

    final static int START_SERVICE = 0;
    final static int STOP_SERVICE = 1;

    static boolean mAppsLoaded = false;

    static int displayWidth;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Update displayed width bars.
        updateDisplayWidth();

        updateRowItems();

        mAppRowAdapter.reDraw(false);
        updateRowItem();
    }

    protected void setUpSpinner(Spinner spinner) {
        String[] spinnerItems = getResources().getStringArray(R.array.entries_action_spinner);
        List<SparseArray<String>> items = new ArrayList<SparseArray<String>>();
        for (int i = 0;i < spinnerItems.length; i++ ) {
            SparseArray<String> spinnerMap = new SparseArray<String>();
            spinnerMap.put(i, spinnerItems[i]);
            items.add(spinnerMap);
        }
        final List<SparseArray<String>> finalItems = items;

        SpinnerAdapter mSpinnerAdapter = new CustomArrayAdapter(mContext, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(mSpinnerAdapter);

        Spinner.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    ((TextView) view).setText(finalItems.get(i).get(i));
                } catch (NullPointerException e) {
                }
                switch (i) {
                    case 0:
                        return;
                    case 1:
                        startActivity(new Intent(mContext, AppsWidgetSettings.class));
                        break;
                    case 2:
                        startActivity(new Intent(mContext, StatsWidgetSettings.class));
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };

        spinner.setAdapter(mSpinnerAdapter);
        spinner.setOnItemSelectedListener(spinnerListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;

        setContentView(R.layout.activity_settings);

        prefs = new PrefsGet(getSharedPreferences(getPackageName(), Context.MODE_MULTI_PROCESS));

        mContext = this;

        if (showChangelog(prefs)) {
            launchChangelog();
        }

        display = getWindowManager().getDefaultDisplay();
        updateDisplayWidth();

        myService = new ServiceCall(mContext);
        myService.setConnection(mConnection);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();

        actionBar.setTitle(R.string.title_activity_settings);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setCustomView(R.layout.action_spinner);
        setUpSpinner((Spinner) actionBar.getCustomView().findViewById(R.id.config_spinner));
        actionBar.setDisplayShowCustomEnabled(true);


        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);

        mGetFragments = new GetFragments();
        mGetFragments.setFm(getFragmentManager());
        mGetFragments.setVp(mViewPager);

        ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        };

        mViewPager.setOnPageChangeListener(pageChangeListener);

        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
        pageChangeListener.onPageSelected(GENERAL_TAB);

    }

    @Override
    protected void onResume() {
        super.onResume();
        prefs = new PrefsGet(getSharedPreferences(getPackageName(), Context.MODE_MULTI_PROCESS));
        Tools.HangarLog("onResume Settings!");
        try {
            ((Spinner) getActionBar().getCustomView().findViewById(R.id.config_spinner)).setSelection(0);
        } catch (Exception e) {
        }
        if (mLaunchedPaypal) {
            mLaunchedPaypal = false;
            launchThanks(THANK_YOU_PAYPAL);
        }
        myService.watchHelper(START_SERVICE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isBound) {
            try {
                unbindService(myService.mConnection);
            } catch (RuntimeException e) {
                Tools.HangarLog("Could not unbind service!");
            }
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isBound = false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Tools.HangarLog("ResultCode: " + resultCode + "RequestCode: " + requestCode + "Intent: " + data);
        if (requestCode == 1001) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

            if (responseCode == 0) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    Tools.HangarLog("It werked! productId: " + sku);
                    launchThanks(THANK_YOU_GOOGLE);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                if (responseCode > 1) {
                    Tools.HangarLog("Not user's fault, tried to purchase but bailed.");
                    Toast.makeText(mContext, getResources().getString(R.string.donate_try_paypal),
                            Toast.LENGTH_LONG).show();
                }
                launchDonate();
            }
        } else if (requestCode == 1) {
            // Icon chooser
            if (resultCode == Activity.RESULT_OK) {
                try {
                    Bitmap bitmap = data.getParcelableExtra("icon");
                    ComponentName componentTask = ComponentName.unflattenFromString(mIconTask.getPackageName() + "/" + mIconTask.getClassName());
                    IconCacheHelper.preloadComponent(mContext, componentTask, bitmap, Tools.dpToPx(mContext, CACHED_ICON_SIZE));
                    myService.execute(SERVICE_CREATE_NOTIFICATIONS);
                    completeRedraw = true;
                    Tools.updateWidget(mContext);
                } catch (Exception e) {
                    Tools.HangarLog("Icon result exception: " + e);
                }
            }
        } else if (requestCode == 2) {
            // Icon chooser
            if (resultCode == Activity.RESULT_OK) {
                try {
                    Bitmap bitmap = data.getParcelableExtra("icon");
                    IconCacheHelper.preloadIcon(mContext, Settings.MORE_APPS_PACKAGE, bitmap, Tools.dpToPx(mContext, CACHED_ICON_SIZE));
                    myService.execute(SERVICE_CREATE_NOTIFICATIONS);
                    completeRedraw = true;
                    Tools.updateWidget(mContext);
                    updateMoreAppsIcon(mContext);
                } catch (Exception e) {
                    Tools.HangarLog("Icon result exception: " + e);
                }
            }
        }
    }

    protected static void resetIconCache(String resourceName) {
        if (resourceName.equals(Settings.MORE_APPS_PACKAGE)) {
            Bitmap bitmap = Tools.drawableToBitmap(mContext.getResources().getDrawable(Settings.MORE_APPS_DRAWABLE_RESOURCE));
            IconCacheHelper.preloadIcon(mContext, resourceName, bitmap, Tools.dpToPx(mContext, CACHED_ICON_SIZE));
            updateMoreAppsIcon(mContext);
        }
        myService.execute(SERVICE_CREATE_NOTIFICATIONS);
    }

    protected static void resetIconComponent(ComponentName componentName) {
        Tools.HangarLog("resetIconCache!: " + componentName.getPackageName());

        try {
            Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(componentName.getPackageName());
            ResolveInfo rInfo = mContext.getPackageManager().resolveActivity(intent, 0);

            Drawable icon = new IconCacheHelper(mContext).getFullResIcon(rInfo.activityInfo, true);
            Bitmap bitmap = Tools.drawableToBitmap(icon);
            IconCacheHelper.preloadComponent(mContext, componentName, bitmap, Tools.dpToPx(mContext, CACHED_ICON_SIZE));
            myService.execute(SERVICE_CREATE_NOTIFICATIONS);
            mAppRowAdapter.reDraw(true);
            updateRowItem();
        } catch (Exception e) {
            Tools.HangarLog("reset Icon Cache exception: " + e);
        }
    }

    @TargetApi(17)
    protected void updateDisplayWidth() {
        Point size = new Point();
        try {
            display.getRealSize(size);
            displayWidth = size.x;
        } catch (NoSuchMethodError e) {
            displayWidth = display.getWidth();
        }
    }

    protected static void launchPriorityWarning(SharedPreferences prefs) {
        String priority = prefs.getString(PRIORITY_PREFERENCE, Integer.toString(PRIORITY_DEFAULT));
        if (Integer.parseInt(priority) != PRIORITY_DEFAULT && prefs.getBoolean(SECOND_ROW_PREFERENCE, SECOND_ROW_DEFAULT)) {
            new AlertDialog.Builder(mContext)
                    .setTitle(R.string.title_second_row_preference)
                    .setMessage(R.string.alert_second_row_summary)
                    .setPositiveButton(R.string.contribute_accept_button, null)
                    .show();
        }
    }

    protected static void launchedPaypal(boolean launched) {
        mLaunchedPaypal = launched;
        Tools.HangarLog("launchedPaypal: " + launched);
    }

    protected void launchThanks(int which) {
        String thankYouMsg = getResources().getString(R.string.donate_thanks);
        if (which == THANK_YOU_PAYPAL)
                thankYouMsg += "\n\n" + getResources().getString(R.string.donate_thanks_paypal);

        AlertDialog alert = new AlertDialog.Builder(Settings.this)
                .setTitle(R.string.donate_thanks_title)
                .setIcon(R.drawable.ic_logo)
                .setMessage(thankYouMsg)
                .setPositiveButton(R.string.donate_thanks_continue, null)
                .show();

        TextView msgTxt = (TextView) alert.findViewById(android.R.id.message);
        msgTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    }

    protected void launchChangelog() {
        ChangeLog changelog = new ChangeLog(this);
        View mChg = changelog.getView();
        mChg.refreshDrawableState();
        new AlertDialog.Builder(Settings.this)
                .setTitle(R.string.changelog_title)
                .setIcon(R.drawable.ic_logo)
                .setView(mChg)
                .setNegativeButton(R.string.changelog_donate_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                launchDonate();

                                dialog.dismiss();
                            }

                        }
                )
                .setNeutralButton(R.string.changelog_contribute_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                launchContribute(Settings.this);

                                dialog.dismiss();
                            }

                        }
                )
                .setPositiveButton(R.string.changelog_accept_button, null)
                .show();
    }

    protected void launchLicense() {
        License license = new License(this);
        View mLicense = license.getView();
        mLicense.refreshDrawableState();
        new AlertDialog.Builder(Settings.this)
                .setTitle(R.string.license_title)
                .setIcon(R.drawable.ic_logo)
                .setView(mLicense)
                .setPositiveButton(R.string.license_accept_button, null)
                .show();
    }

    static protected void launchContribute(Context context) {
        Contribute contribute = new Contribute(context);
        View mContribute = contribute.getView();
        mContribute.refreshDrawableState();
        new AlertDialog.Builder(context)
                .setTitle(R.string.contribute_title)
                .setIcon(R.drawable.ic_logo)
                .setView(mContribute)
                .setPositiveButton(R.string.contribute_accept_button, null)
                .show();
    }

    protected void launchDonate() {
        final Donate donate = new Donate(this);
        donate.bindServiceConn();
        View mDonate = donate.getView(mContext);
        mDonate.refreshDrawableState();
        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this)
                .setTitle(R.string.donate_title)
                .setIcon(R.drawable.ic_logo)
                .setView(mDonate)
                .setPositiveButton(R.string.donate_accept_button, null);
        AlertDialog alert = builder.show();
        alert.setOnDismissListener(new AlertDialog.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                donate.unbindServiceConn();
            }
        });
        donate.setAlert(alert);
    }

    protected void launchInstructions() {
        startActivity(new Intent(mContext, Instructions.class));
    }

    ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            s = IWatchfulService.Stub.asInterface(binder);
            isBound = true;
            myService.execute(SERVICE_BUILD_TASKS);
        }

        public void onServiceDisconnected(ComponentName className) {
            s = null;
            isBound = false;
        }
    };

    protected boolean showChangelog(PrefsGet prefs) {
        SharedPreferences mPrefs = prefs.prefsGet();
        SharedPreferences.Editor mEditor = prefs.editorGet();

        String hangarVersion = null;
        try {
            hangarVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            String[] versionArray = hangarVersion.split("\\."); // Only get major.minor
            hangarVersion = versionArray[0] + "." + versionArray[1];
        } catch (PackageManager.NameNotFoundException e) {}

        String whichVersion = mPrefs.getString(VERSION_CHECK, null);

        Tools.HangarLog("savedVer: " + whichVersion + " hangarVersion: " + hangarVersion);
        if (whichVersion != null) {
            if (whichVersion.equals(hangarVersion)) {
                return false;
            } else {
                mEditor.putString(VERSION_CHECK, hangarVersion);
                mEditor.commit();
                return true;
            }
        } else {
            mEditor.putString(VERSION_CHECK, hangarVersion);
            mEditor.commit();
            launchInstructions();
            return false;
        }
    }
    protected static class ServiceCall  {
        Context mContext;
        ServiceConnection mConnection;
        ServiceCall(Context context) {
            mContext = context;
        }
        protected void setConnection(ServiceConnection connection) {
            mConnection = connection;
        }
        protected void watchHelper(int which) {
            Intent intent = new Intent(mContext, WatchfulService.class);
            switch (which) {
                case 0:
                    mContext.startService(intent);
                    if (!isBound) {
                        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
                    }
                    break;
                case 1:
                    mContext.stopService(intent);
                    if (isBound) {
                        mContext.unbindService(mConnection);
                        isBound = false;
                    }
                    break;
            }
        }
        protected void execute(int which) {
            try {
                switch(which) {
                    case SERVICE_BUILD_TASKS:
                        s.buildTasks();
                        return;
                    case SERVICE_BUILD_REORDER_LAUNCH:
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    s.buildReorderAndLaunch();
                                } catch (Exception e) {
                                    Tools.HangarLog("buildReorderAndLaunch exception: " + e);
                                    e.printStackTrace();
                                }
                            }
                        };
                        new Thread(runnable).start();
                        return;
                    case SERVICE_CREATE_NOTIFICATIONS:
                        Runnable runnable2 = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    s.createNotification();
                                } catch (Exception e) {
                                    Tools.HangarLog("buildReorderAndLaunch exception: " + e);
                                }
                            }
                        };
                        new Thread(runnable2).start();
                        return;
                    case SERVICE_DESTROY_NOTIFICATIONS:
                        s.destroyNotification();
                        break;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_instructions) {
            launchInstructions();
            return true;
        } else if (id == R.id.action_changelog) {
            launchChangelog();
            return true;
        } else if (id == R.id.action_donate) {
            launchDonate();
            return true;
        } else if (id == R.id.action_license) {
            launchLicense();
            return true;
        } else if (id == R.id.action_contribute) {
            launchContribute(Settings.this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public static int[] splitToComponentTimes(int longVal) {
        int hours = longVal / 3600;
        int remainder = longVal - hours * 3600;
        int mins = remainder / 60;
        remainder = remainder - mins * 60;
        int secs = remainder;

        int[] ints = {hours , mins , secs};
        return ints;
    }

    public static class IconPackUpdate {
        Preference icon_pack_preference;
        SharedPreferences prefs2;

        IconPackUpdate(SharedPreferences prefs, Preference pref) {
            icon_pack_preference = pref;
            prefs2 = prefs;
        }

        public void iconPackUpdated() {
            Runnable runnable = new Runnable() {
                public void run(){
                    final List<AppsRowItem> appTasks = createAppTasks();
                    rebuildIconCache(appTasks);
                    mAppRowAdapter = new AppsRowAdapter(mContext, appTasks);
                }
            };
            new Thread(runnable).start();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    updateListView(true);
                }
            }, 1000); // Add delay to reduce risk of conflicting image lookups during cache
                      // reconstruction

            // Notifications need to be refreshed after cache rebuild.
            Toast.makeText(mContext, mContext.getResources().getString(R.string.switched_icon_packs_alert),
                    Toast.LENGTH_LONG).show();
            icon_pack_preference.setSummary(Tools.getApplicationName(mContext, prefs2.getString(ICON_PACK_PREFERENCE, null)));
            updateIconPackIcon(mContext);
        }
    }

    public static void pickIcon(Settings activity, AppsRowItem task) {
        IconPackHelper.setActivity(activity);
        IconPackHelper.setTask(task);
        IconPackHelper.pickIconPicker(mContext);
    }

    public static synchronized void rebuildIconCache(final List<AppsRowItem> appTasks) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                IconHelper ih = new IconHelper(mContext);

                for (AppsRowItem task : appTasks) {
                    ComponentName componentName = ComponentName.unflattenFromString(task.getPackageName() + "/" + task.getClassName());
                    ih.cachedIconHelper(componentName);
                }
                Tools.updateWidget(mContext);
                myService.execute(SERVICE_CREATE_NOTIFICATIONS);
            }
        };
        new Thread(runnable).start();
    }

    public static class PrefsGet {
        SharedPreferences realPrefs;
        PrefsGet(SharedPreferences prefs) {
            realPrefs = prefs;
        }
        SharedPreferences prefsGet() {
            return realPrefs;
        }
        SharedPreferences.Editor editorGet() {
            return realPrefs.edit();
        }
    }

    public static class GetFragments {
        static ViewPager vp;
        static FragmentManager fm;

        public Fragment getFragmentByPosition(int pos) {
            String tag = "android:switcher:" + vp.getId() + ":" + pos;
            return fm.findFragmentByTag(tag);
        }
        public void setVp(ViewPager mViewPager) {
            vp = mViewPager;
        }
        public void setFm(FragmentManager mFm) {
            fm = mFm;
        }
    }

    public static class PrefsFragment extends PreferenceFragment {
        CheckBoxPreference boot_preference;
        CheckBoxPreference divider_preference;
        CheckBoxPreference row_divider_preference;
        CheckBoxPreference weighted_recents_preference;
        CheckBoxPreference colorize_preference;
        CheckBoxPreference second_row_preference;
        CheckBoxPreference smart_notification_preference;
        CheckBoxPreference more_apps_preference;
        CheckBoxPreference floating_windows_preference;
        ColorPickerPreference icon_color_preference;
        SwitchPreference toggle_preference;
        UpdatingListPreference appnos_preference;
        UpdatingListPreference priority_preference;
        UpdatingListPreference weight_priority_preference;
        UpdatingListPreference statusbar_icon_preference;
        UpdatingListPreference icon_size_preference;
        UpdatingListPreference pinned_sort_preference;
        UpdatingListPreference pinned_placement_preference;
        UpdatingListPreference more_apps_pages_preference;
        Preference icon_pack_preference;
        Preference more_apps_icon_preference;

        public static PrefsFragment newInstance(int prefLayout) {
            PrefsFragment fragment = new PrefsFragment();
            Bundle args = new Bundle();
            args.putInt("layout", prefLayout);
            fragment.setArguments(args);
            return fragment;
        }

        public PrefsFragment() {
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            final int prefLayout = getArguments().getInt("layout");
            setHasOptionsMenu(true);
            addPreferencesFromResource(prefLayout);
            final SharedPreferences prefs2 = prefs.prefsGet();

            try {
                // *** Appearance ***
                divider_preference = (CheckBoxPreference)findPreference(DIVIDER_PREFERENCE);
                divider_preference.setChecked(prefs2.getBoolean(DIVIDER_PREFERENCE, DIVIDER_DEFAULT));
                divider_preference.setOnPreferenceChangeListener(changeListener);

                row_divider_preference = (CheckBoxPreference)findPreference(ROW_DIVIDER_PREFERENCE);
                row_divider_preference.setChecked(prefs2.getBoolean(ROW_DIVIDER_PREFERENCE, ROW_DIVIDER_DEFAULT));
                row_divider_preference.setOnPreferenceChangeListener(changeListener);

                colorize_preference = (CheckBoxPreference)findPreference(COLORIZE_PREFERENCE);
                colorize_preference.setChecked(prefs2.getBoolean(COLORIZE_PREFERENCE, COLORIZE_DEFAULT));
                colorize_preference.setOnPreferenceChangeListener(changeListener);

                icon_color_preference = (ColorPickerPreference) findPreference(ICON_COLOR_PREFERENCE);
                int intColor = prefs2.getInt(ICON_COLOR_PREFERENCE, ICON_COLOR_DEFAULT);
                String hexColor = String.format("#%08x", (intColor));
                icon_color_preference.setSummary(hexColor);
                // icon_color_preference.setNewPreviewColor(intColor);
                icon_color_preference.setOnPreferenceChangeListener(changeListener);

                appnos_preference = (UpdatingListPreference)findPreference(APPSNO_PREFERENCE);
                appnos_preference.setValue(prefs2.getString(APPSNO_PREFERENCE, Integer.toString(APPSNO_DEFAULT)));
                appnos_preference.setOnPreferenceChangeListener(changeListener);

                statusbar_icon_preference = (UpdatingListPreference)findPreference(STATUSBAR_ICON_PREFERENCE);
                statusbar_icon_preference.setValue(prefs2.getString(STATUSBAR_ICON_PREFERENCE, STATUSBAR_ICON_DEFAULT));
                statusbar_icon_preference.setOnPreferenceChangeListener(changeListener);

                icon_size_preference = (UpdatingListPreference)findPreference(ICON_SIZE_PREFERENCE);
                icon_size_preference.setValue(prefs2.getString(ICON_SIZE_PREFERENCE, Integer.toString(ICON_SIZE_DEFAULT)));
                icon_size_preference.setOnPreferenceChangeListener(changeListener);

                second_row_preference = (CheckBoxPreference)findPreference(SECOND_ROW_PREFERENCE);
                Boolean secondRow = prefs2.getBoolean(SECOND_ROW_PREFERENCE, SECOND_ROW_DEFAULT);
                second_row_preference.setChecked(secondRow);
                setAppsnoSummary(secondRow, appnos_preference);
                second_row_preference.setOnPreferenceChangeListener(changeListener);

                boolean toggleBool = prefs2.getBoolean(TOGGLE_PREFERENCE, TOGGLE_DEFAULT);
                toggleDependencies(toggleBool);

            } catch (NullPointerException e) {
            }
            try {
                // *** Behavior ***
                weighted_recents_preference = (CheckBoxPreference)findPreference(WEIGHTED_RECENTS_PREFERENCE);
                weighted_recents_preference.setChecked(prefs2.getBoolean(WEIGHTED_RECENTS_PREFERENCE, WEIGHTED_RECENTS_DEFAULT));
                weighted_recents_preference.setOnPreferenceChangeListener(changeListener);

                weight_priority_preference = (UpdatingListPreference)findPreference(WEIGHT_PRIORITY_PREFERENCE);
                weight_priority_preference.setValue(prefs2.getString(WEIGHT_PRIORITY_PREFERENCE, Integer.toString(WEIGHT_PRIORITY_DEFAULT)));
                weight_priority_preference.setOnPreferenceChangeListener(changeListener);

                smart_notification_preference = (CheckBoxPreference)findPreference(SMART_NOTIFICATION_PREFERENCE);
                smart_notification_preference.setChecked(prefs2.getBoolean(SMART_NOTIFICATION_PREFERENCE, SMART_NOTIFICATION_DEFAULT));
                smart_notification_preference.setOnPreferenceChangeListener(changeListener);

                priority_preference = (UpdatingListPreference)findPreference(PRIORITY_PREFERENCE);
                priority_preference.setValue(prefs2.getString(PRIORITY_PREFERENCE, Integer.toString(PRIORITY_DEFAULT)));
                priority_preference.setOnPreferenceChangeListener(changeListener);

                floating_windows_preference = (CheckBoxPreference)findPreference(FLOATING_WINDOWS_PREFERENCE);
                floating_windows_preference.setChecked(prefs2.getBoolean(FLOATING_WINDOWS_PREFERENCE, FLOATING_WINDOWS_DEFAULT));
                floating_windows_preference.setOnPreferenceChangeListener(changeListener);

            } catch (NullPointerException e) {
            }
            try {
                // *** General ***
                toggle_preference = (SwitchPreference)findPreference(TOGGLE_PREFERENCE);
                toggle_preference.setChecked(prefs2.getBoolean(TOGGLE_PREFERENCE, TOGGLE_DEFAULT));
                toggle_preference.setOnPreferenceChangeListener(changeListener);

                boot_preference = (CheckBoxPreference)findPreference(BOOT_PREFERENCE);
                boot_preference.setChecked(prefs2.getBoolean(BOOT_PREFERENCE, BOOT_DEFAULT));
                boot_preference.setOnPreferenceChangeListener(changeListener);

                String iconPackName = Tools.getApplicationName(mContext, prefs2.getString(ICON_PACK_PREFERENCE, null));
                icon_pack_preference = findPreference(ICON_PACK_PREFERENCE);
                if (iconPackName.isEmpty() || iconPackName.equals("")) {
                    iconPackName = getResources().getString(R.string.title_icon_pack_picker);
                }

                icon_pack_preference.setSummary(iconPackName);
                updateIconPackIcon(mContext);
                iconPackUpdate = new IconPackUpdate(prefs2, icon_pack_preference);
                icon_pack_preference.setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                IconPackHelper.pickIconPack(mContext);

                                return false;
                            }
                        }
                );

                pinned_sort_preference = (UpdatingListPreference)findPreference(PINNED_SORT_PREFERENCE);
                pinned_sort_preference.setValue(prefs2.getString(PINNED_SORT_PREFERENCE, Integer.toString(PINNED_SORT_DEFAULT)));
                pinned_sort_preference.setOnPreferenceChangeListener(changeListener);

                pinned_placement_preference = (UpdatingListPreference)findPreference(PINNED_PLACEMENT_PREFERENCE);
                pinned_placement_preference.setValue(prefs2.getString(PINNED_PLACEMENT_PREFERENCE, Integer.toString(PINNED_PLACEMENT_DEFAULT)));
                pinned_placement_preference.setOnPreferenceChangeListener(changeListener);

                more_apps_preference = (CheckBoxPreference)findPreference(MORE_APPS_PREFERENCE);
                more_apps_preference.setChecked(prefs2.getBoolean(MORE_APPS_PREFERENCE, MORE_APPS_DEFAULT));
                more_apps_preference.setOnPreferenceChangeListener(changeListener);

                more_apps_pages_preference = (UpdatingListPreference)findPreference(MORE_APPS_PAGES_PREFERENCE);
                more_apps_pages_preference.setValue(prefs2.getString(MORE_APPS_PAGES_PREFERENCE, Integer.toString(MORE_APPS_PAGES_DEFAULT)));
                more_apps_pages_preference.setOnPreferenceChangeListener(changeListener);

                more_apps_icon_preference = findPreference(MORE_APPS_ICON_PREFERENCE);
                updateMoreAppsIcon(mContext);
                more_apps_icon_preference.setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                IconPackHelper.setActivity(mInstance);
                                IconPackHelper.pickIconPack(mContext, true, true);

                                return false;
                            }
                        }
                );

            } catch (NullPointerException e) {
            }
        }
        Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                Tools.HangarLog("onPreferenceChange pref.getKey=[" + preference.getKey() + "] newValue=[" + newValue + "]");

                final SharedPreferences prefs2 = prefs.prefsGet();
                final SharedPreferences.Editor editor = prefs.editorGet();

                if (preference.getKey().equals(DIVIDER_PREFERENCE)) {
                    editor.putBoolean(DIVIDER_PREFERENCE, (Boolean) newValue);
                    editor.commit();
                } else if (preference.getKey().equals(ROW_DIVIDER_PREFERENCE)) {
                    editor.putBoolean(ROW_DIVIDER_PREFERENCE, (Boolean) newValue);
                    editor.commit();
                } else if (preference.getKey().equals(COLORIZE_PREFERENCE)) {
                    editor.putBoolean(COLORIZE_PREFERENCE, (Boolean) newValue);
                    editor.commit();
                } else if (preference.getKey().equals(STATUSBAR_ICON_PREFERENCE)) {
                    final String mStatusBarIcon = (String) newValue;
                    if (mStatusBarIcon.equals(STATUSBAR_ICON_NONE)) {
                        new AlertDialog.Builder(myService.mContext)
                            .setTitle(R.string.alert_title_statusbar_icon_preference)
                            .setMessage(R.string.alert_message_statusbar_icon_preference)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    editor.putString(STATUSBAR_ICON_PREFERENCE, mStatusBarIcon);
                                    editor.putString(PRIORITY_PREFERENCE, Integer.toString(PRIORITY_BOTTOM));
                                    editor.commit();
                                    PrefsFragment mBehaviorSettings = (PrefsFragment) mGetFragments.getFragmentByPosition(BEHAVIOR_TAB);
                                    mBehaviorSettings.priority_preference.setValue(Integer.toString(PRIORITY_BOTTOM));
                                    launchPriorityWarning(prefs2);
                                    myService.execute(SERVICE_DESTROY_NOTIFICATIONS);
                                    myService.execute(SERVICE_BUILD_REORDER_LAUNCH);
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    statusbar_icon_preference.setValue(prefs2.getString(STATUSBAR_ICON_PREFERENCE, STATUSBAR_ICON_DEFAULT));
                                }
                            }).show();
                    } else {
                        PrefsFragment mBehaviorSettings = (PrefsFragment) mGetFragments.getFragmentByPosition(BEHAVIOR_TAB);
                        if (mBehaviorSettings.priority_preference.getValue().equals(Integer.toString(PRIORITY_BOTTOM))) {
                            editor.putString(PRIORITY_PREFERENCE, Integer.toString(PRIORITY_DEFAULT));
                            mBehaviorSettings.priority_preference.setValue(Integer.toString(PRIORITY_DEFAULT));
                        }
                        editor.putString(STATUSBAR_ICON_PREFERENCE, (String) newValue);
                        editor.commit();
                        myService.execute(SERVICE_DESTROY_NOTIFICATIONS);
                    }
                    return true;
                } else if (preference.getKey().equals(ICON_COLOR_PREFERENCE)) {
                    String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(newValue)));
                    preference.setSummary(hex);
                    int intHex = ColorPickerPreference.convertToColorInt(hex);
                    editor.putInt(ICON_COLOR_PREFERENCE, intHex);
                    editor.commit();
                } else if (preference.getKey().equals(ICON_SIZE_PREFERENCE)) {
                    editor.putString(ICON_SIZE_PREFERENCE, (String) newValue);
                    editor.commit();
                } else if (preference.getKey().equals(TOGGLE_PREFERENCE)) {
                    editor.putBoolean(TOGGLE_PREFERENCE, (Boolean) newValue);
                    editor.commit();
                    boolean toggleBool = (Boolean) newValue;
                    toggleDependencies(toggleBool);
                    myService.execute(toggleBool ? SERVICE_CREATE_NOTIFICATIONS : SERVICE_DESTROY_NOTIFICATIONS);
                    return true;
                } else if (preference.getKey().equals(BOOT_PREFERENCE)) {
                    editor.putBoolean(BOOT_PREFERENCE, (Boolean) newValue);
                    editor.commit();
                    return true;
                } else if (preference.getKey().equals(WEIGHT_PRIORITY_PREFERENCE)) {
                    editor.putString(WEIGHT_PRIORITY_PREFERENCE, (String) newValue);
                    editor.commit();
                } else if (preference.getKey().equals(WEIGHTED_RECENTS_PREFERENCE)) {
                    editor.putBoolean(WEIGHTED_RECENTS_PREFERENCE, (Boolean) newValue);
                    editor.commit();
                } else if (preference.getKey().equals(SMART_NOTIFICATION_PREFERENCE)) {
                    editor.putBoolean(SMART_NOTIFICATION_PREFERENCE, (Boolean) newValue);
                    editor.commit();
                } else if (preference.getKey().equals(FLOATING_WINDOWS_PREFERENCE)) {
                    Boolean isFloating = (Boolean) newValue;
                    if (isFloating)
                        Toast.makeText(mContext, mContext.getResources().getString(R.string.alert_floating_windows),
                                Toast.LENGTH_LONG).show();
                    editor.putBoolean(FLOATING_WINDOWS_PREFERENCE, isFloating);
                    editor.commit();
                } else if (preference.getKey().equals(MORE_APPS_PREFERENCE)) {
                    editor.putBoolean(MORE_APPS_PREFERENCE, (Boolean) newValue);
                    editor.commit();
                    myService.execute(SERVICE_BUILD_REORDER_LAUNCH);
                } else if (preference.getKey().equals(MORE_APPS_PAGES_PREFERENCE)) {
                    editor.putString(MORE_APPS_PAGES_PREFERENCE, (String) newValue);
                    editor.commit();
                } else if (preference.getKey().equals(APPSNO_PREFERENCE)) {
                    editor.putString(APPSNO_PREFERENCE, (String) newValue);
                    editor.commit();
                } else if (preference.getKey().equals(PRIORITY_PREFERENCE)) {
                    String mPriorityPreference = (String) newValue;
                    editor.putString(PRIORITY_PREFERENCE, mPriorityPreference);
                    PrefsFragment mAppearanceSettings = (PrefsFragment) mGetFragments.getFragmentByPosition(APPEARANCE_TAB);
                    if (!mPriorityPreference.equals(Integer.toString(PRIORITY_BOTTOM)) &&
                            mAppearanceSettings.statusbar_icon_preference.getValue().equals(STATUSBAR_ICON_NONE)) {
                        editor.putString(STATUSBAR_ICON_PREFERENCE, STATUSBAR_ICON_DEFAULT);
                        mAppearanceSettings.statusbar_icon_preference.setValue(STATUSBAR_ICON_DEFAULT);
                    }
                    editor.commit();
                    launchPriorityWarning(prefs2);
                    myService.execute(SERVICE_DESTROY_NOTIFICATIONS);
                } else if (preference.getKey().equals(SECOND_ROW_PREFERENCE)) {
                    setAppsnoSummary((Boolean) newValue, appnos_preference);
                    editor.putBoolean(SECOND_ROW_PREFERENCE, (Boolean) newValue);
                    editor.commit();
                    launchPriorityWarning(prefs2);
                } else if (preference.getKey().equals(PINNED_SORT_PREFERENCE)) {
                    editor.putString(PINNED_SORT_PREFERENCE, (String) newValue);
                    editor.commit();
                    String pinnedApps = prefs2.getString(PINNED_APPS, null);
                    if (pinnedApps != null && !pinnedApps.isEmpty()) {
                        myService.execute(SERVICE_BUILD_REORDER_LAUNCH);
                    }
                    return true;
                } else if (preference.getKey().equals(PINNED_PLACEMENT_PREFERENCE)) {
                    editor.putString(PINNED_PLACEMENT_PREFERENCE, (String) newValue);
                    editor.commit();
                    String pinnedApps = prefs2.getString(PINNED_APPS, null);
                    if (pinnedApps != null && !pinnedApps.isEmpty()) {
                        myService.execute(SERVICE_BUILD_REORDER_LAUNCH);
                    }
                    return true;
                }
                myService.execute(SERVICE_BUILD_REORDER_LAUNCH);
                return true;
            }
        };

        void toggleDependencies(boolean isToggled) {
            try {
                PrefsFragment mBehaviorFrag = (PrefsFragment) mGetFragments.getFragmentByPosition(BEHAVIOR_TAB);
                mBehaviorFrag.priority_preference.setEnabled(isToggled);
                mBehaviorFrag.weighted_recents_preference.setEnabled(isToggled);
                mBehaviorFrag.smart_notification_preference.setEnabled(isToggled);

                PrefsFragment mAppearanceFrag = (PrefsFragment) mGetFragments.getFragmentByPosition(APPEARANCE_TAB);
                mAppearanceFrag.appnos_preference.setEnabled(isToggled);
                mAppearanceFrag.second_row_preference.setEnabled(isToggled);
                mAppearanceFrag.statusbar_icon_preference.setEnabled(isToggled);
                mAppearanceFrag.divider_preference.setEnabled(isToggled);
                mAppearanceFrag.row_divider_preference.setEnabled(isToggled);
                mAppearanceFrag.colorize_preference.setEnabled(isToggled);
                mAppearanceFrag.icon_size_preference.setEnabled(isToggled);
            } catch (Exception e) {
            }
        }
    }

    static void updateMoreAppsIcon(Context context) {
        try {
            Drawable d = new BitmapDrawable(context.getResources(), new IconHelper(mContext).cachedResourceIconHelper(MORE_APPS_PACKAGE));
            PrefsFragment mGeneralSettings = (PrefsFragment) mGetFragments.getFragmentByPosition(GENERAL_TAB);
            mGeneralSettings.more_apps_icon_preference.setIcon(d);
        } catch (Exception e) {
        }
    }

    static void updateIconPackIcon(Context context) {
        String iconPackPackage = prefs.prefsGet().getString(ICON_PACK_PREFERENCE, null);
        Drawable icon;

        icon = context.getResources().getDrawable(R.drawable.ic_launcher);
        try {
            Tools.HangarLog("iconPackPackage: " + iconPackPackage);
            icon = new BitmapDrawable(context.getResources(), new IconHelper(mContext).cachedResourceIconHelper(iconPackPackage));
        } catch (Exception e) {
        }
        PrefsFragment mGeneralSettings = (PrefsFragment) mGetFragments.getFragmentByPosition(GENERAL_TAB);
        mGeneralSettings.icon_pack_preference.setIcon(icon);
    }

    private static void setAppsnoSummary(Boolean second_row, Preference appnos_preference) {
        // Update summary of AppNum pref for second row wording
        if (second_row) {
            appnos_preference.setSummary(R.string.summary_appsno_second_row_preference);
        } else {
            appnos_preference.setSummary(R.string.summary_appsno_preference);
        }
    }

    public static void updateRowItem() {
        int start = lv.getFirstVisiblePosition();
        for (int i=start, j=lv.getLastVisiblePosition(); i<=j; i++) {
                View view = lv.getChildAt(i - start);
                mAppRowAdapter.getView(i, view, lv);
        }
        completeRedraw = false;
        mAppRowAdapter.reDraw(false);
    }

    public synchronized static void updateListView(final boolean setAdapter) {
        Runnable runnable = new Runnable() {
            public void run() {
                if (!mAppsLoaded) {
                    RelativeLayout bg = (RelativeLayout) lv.getParent();
                    bg.findViewById(R.id.loading_text).setVisibility(View.GONE);

                    mAppsLoaded = true;
                }
                if (setAdapter) {
                    lv.setAdapter(mAppRowAdapter);
                }
                lv.invalidateViews();
                mAppRowAdapter.notifyDataSetChanged();
            }
        };
        mInstance.runOnUiThread(runnable);
    }


    public static class AppsFragment extends Fragment implements OnItemClickListener {

        public static Fragment newInstance() {
            return new AppsFragment();
        }

        public AppsFragment() {}

        public void onResume() {
            super.onResume();

            if (mAppRowAdapter == null)
                return;

            mAppsLoaded = false;

            mAppRowAdapter.reDraw(completeRedraw);
            lv.invalidateViews();
        }

        public void buildList() {
            Runnable runnable = new Runnable() {
                public void run() {
                    if (mAppRowAdapter == null)
                        return;
                    mAppRowAdapter.mRowItems = createAppTasks();
                    updateListView(true);
                }
            };
            new Thread(runnable).start();
        }

        Spinner.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                final SharedPreferences.Editor editor = prefs.editorGet();

                switch (adapterView.getId()) {
                    case R.id.top_spinner:
                        editor.putInt(APPLIST_TOP_PREFERENCE, i);
                        break;
                    case R.id.sort_spinner:
                        editor.putInt(APPLIST_SORT_PREFERENCE, i);
                        break;
                }
                editor.commit();

                buildList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            setHasOptionsMenu(true);

            mAppsLoaded = false;
            View appsView = inflater.inflate(R.layout.apps_settings, container, false);
            lv = (ListView) appsView.findViewById(R.id.list);

            final SharedPreferences prefs2 = prefs.prefsGet();

            Spinner topSpin = (Spinner) appsView.findViewById(R.id.top_spinner);
            ArrayAdapter<CharSequence> topAdapter = ArrayAdapter.createFromResource(mContext,
                    R.array.entries_top_spinner, R.layout.spinner_item);
            topAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            topSpin.setAdapter(topAdapter);
            topSpin.setOnItemSelectedListener(spinnerListener);
            topSpin.setSelection(prefs2.getInt(APPLIST_TOP_PREFERENCE, APPLIST_TOP_DEFAULT));

            Spinner sortSpin = (Spinner) appsView.findViewById(R.id.sort_spinner);
            ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(mContext,
                    R.array.entries_sort_spinner, R.layout.spinner_item);
            sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sortSpin.setAdapter(sortAdapter);
            sortSpin.setOnItemSelectedListener(spinnerListener);
            sortSpin.setSelection(prefs2.getInt(APPLIST_SORT_PREFERENCE, APPLIST_SORT_DEFAULT));

            ImageView refreshBtn = (ImageView) appsView.findViewById(R.id.refresh);
            refreshBtn.setClickable(true);
            final Animation rotation = AnimationUtils.loadAnimation(getActivity(), R.anim.refresh);
            rotation.setRepeatCount(1);
            rotation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    buildList();
                }
            });
            refreshBtn.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        view.startAnimation(rotation);
                    }
                    return false;
                }
            });

            return appsView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            Tools.HangarLog("onActivityCreated appsFragment");

            lv.setOnItemClickListener(this);

            Runnable runnable = new Runnable() {
                public void run() {
                    List<AppsRowItem> appTasks = createAppTasks();
                    if (appTasks == null)
                        return;

                    mAppRowAdapter = new AppsRowAdapter(mContext, appTasks);
                    updateListView(true);
                }
            };
            new Thread(runnable).start();
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final AppsRowItem rowItem = (AppsRowItem) parent.getItemAtPosition(position);

            PopupMenu popup = new PopupMenu(mContext, view);
            popup.getMenuInflater().inflate(R.menu.app_action, popup.getMenu());
            MenuItem pinItem = popup.getMenu().getItem(0);

            if (rowItem.getPinned()) pinItem.setTitle(R.string.action_unpin);
            PopupMenu.OnMenuItemClickListener menuAction = new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    db = TasksDataSource.getInstance(mContext);
                    db.open();

                    switch (item.getItemId()) {
                        case R.id.action_pin:
                            Boolean isPinned = rowItem.getPinned();
                            rowItem.setPinned(!isPinned);
                            new Tools().togglePinned(mContext, rowItem.getPackageName(), prefs.editorGet());
                            break;
                        case R.id.action_pick_icon:
                            mIconTask = rowItem;
                            pickIcon(mInstance, rowItem);
                            return true;
                        case R.id.action_blacklist:
                            Boolean isBlackListed = rowItem.getBlacklisted();
                            rowItem.setBlacklisted(!isBlackListed);
                            db.blacklistTask(rowItem, !isBlackListed);
                            db.close();
                            break;
                        case R.id.action_reset_stats:
                            rowItem.setStats(null);
                            rowItem.setBarContWidth(0);
                            db.resetTaskStats(rowItem);
                            db.close();
                            break;
                    }
                    lv.invalidateViews();
                    myService.execute(SERVICE_BUILD_REORDER_LAUNCH);
                    return true;
                }
            };
            popup.setOnMenuItemClickListener(menuAction);
            popup.show();
        }

    }

    public static List<AppsRowItem> createAppTasks() {
        db = TasksDataSource.getInstance(mContext);
        db.open();
        int highestSeconds = db.getHighestSeconds();
        List<TasksModel> tasks = db.getAllTasks();

        List<AppsRowItem> appTasks = new ArrayList<AppsRowItem>();

        for (TasksModel task : tasks) {
            try {
                try {
                    ComponentName.unflattenFromString(task.getPackageName() + "/" + task.getClassName());
                } catch (Exception e) {
                    Tools.HangarLog("Could not find Application info for [" + task.getName() + "]");
                    db.deleteTask(task);
                    continue;
                }
                if (new Tools().cachedImageResolveInfo(mContext, task.getPackageName()) != null)
                    appTasks.add(createAppRowItem(task, highestSeconds));
            } catch (Exception e) {
                Tools.HangarLog("could not add taskList item " + e);
            }

            SharedPreferences prefs2 = prefs.prefsGet();
            Collections.sort(appTasks, new Tools.AppRowComparator(prefs2.getInt(APPLIST_TOP_PREFERENCE, APPLIST_TOP_DEFAULT), prefs2.getInt(APPLIST_SORT_PREFERENCE, APPLIST_SORT_DEFAULT)));

        }
        db.close();
        return appTasks;
    }

    public void updateRowItems() {
        List<AppsRowItem> appList = mAppRowAdapter.mRowItems;
        List<AppsRowItem> newAppList = new ArrayList<AppsRowItem>();

        db = TasksDataSource.getInstance(mContext);
        db.open();
        int highestSeconds = db.getHighestSeconds();
        db.close();

        for (AppsRowItem item : appList) {
            AppsRowItem newItem = createAppRowItem(item, highestSeconds);
            newAppList.add(newItem);
        }

        mAppRowAdapter.mRowItems = newAppList;
        updateListView(false);
    }

    public static AppsRowItem createAppRowItem(TasksModel task, int highestSeconds){
        AppsRowItem appTask = new AppsRowItem(task);
        float secondsRatio = (float) task.getSeconds() / highestSeconds;

        int barColor;
        int secondsColor = (Math.round(secondsRatio * 100));
        if (secondsColor >= 80) {
            barColor = 0xFF34B5E2;
        } else if (secondsColor >= 60) {
            barColor = 0xFFAA66CC;
        } else if (secondsColor >= 40) {
            barColor = 0xFF74C353;
        } else if (secondsColor >= 20) {
            barColor = 0xFFFFBB33;
        } else {
            barColor = 0xFFFF4444;
        }
        int[] statsTime = splitToComponentTimes(task.getSeconds());
        String statsString = ((statsTime[0] > 0) ? statsTime[0] + "h " : "") + ((statsTime[1] > 0) ? statsTime[1] + "m " : "") + ((statsTime[2] > 0) ? statsTime[2] + "s " : "");

        int maxWidth = displayWidth - Tools.dpToPx(mContext, 46 + 14 + 90);
        float adjustedWidth = maxWidth * secondsRatio;

        ComponentName componentTask = ComponentName.unflattenFromString(task.getPackageName() + "/" + task.getClassName());
        appTask.setComponentName(componentTask);
        appTask.setPinned(new Tools().isPinned(mContext, task.getPackageName()));
        appTask.setStats(statsString);
        appTask.setBarColor(barColor);
        appTask.setBarContWidth(Math.round(adjustedWidth));

        return appTask;
    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int position) {
            switch (position) {
                case GENERAL_TAB:
                    return PrefsFragment.newInstance(R.layout.general_settings);
                case BEHAVIOR_TAB:
                    return PrefsFragment.newInstance(R.layout.behavior_settings);
                case APPEARANCE_TAB:
                    return PrefsFragment.newInstance(R.layout.appearance_settings);
                case APPS_TAB:
                    return AppsFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case GENERAL_TAB:
                    return mContext.getString(R.string.title_general).toUpperCase(l);
                case BEHAVIOR_TAB:
                    return mContext.getString(R.string.title_behavior).toUpperCase(l);
                case APPEARANCE_TAB:
                    return mContext.getString(R.string.title_appearance).toUpperCase(l);
                case APPS_TAB:
                    return mContext.getString(R.string.title_apps).toUpperCase(l);
            }
            return null;
        }
    }
}
