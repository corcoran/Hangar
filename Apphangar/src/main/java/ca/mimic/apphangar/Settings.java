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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
    // private Context mContext;

    final static String VERSION_CHECK = "version_check";

    final static String DIVIDER_PREFERENCE = "divider_preference";
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


    protected static View appsView;
    protected static boolean isBound = false;
    protected static boolean mLaunchedPaypal = false;
    boolean newStart;

    static PrefsGet prefs;
    static Context mContext;
    static ServiceCall myService;

    final static boolean DIVIDER_DEFAULT = true;
    final static boolean TOGGLE_DEFAULT = true;
    final static boolean BOOT_DEFAULT = true;
    final static boolean WEIGHTED_RECENTS_DEFAULT = true;
    final static boolean COLORIZE_DEFAULT = false;
    final static boolean APPS_BY_WIDGET_SIZE_DEFAULT = true;

    final static int WEIGHT_PRIORITY_DEFAULT = 0;
    final static int APPSNO_DEFAULT = 7;
    final static int PRIORITY_DEFAULT = 2;
    final static int PRIORITY_BOTTOM = -2;
    final static int ICON_COLOR_DEFAULT = 0xffffffff;
    final static int BACKGROUND_COLOR_DEFAULT = 0x5e000000;
    final static int STATS_WIDGET_APPSNO_DEFAULT = 6;
    final static int STATS_WIDGET_APPSNO_LS_DEFAULT = 3;
    final static int APPS_WIDGET_APPSNO_DEFAULT = 6;
    final static int APPS_WIDGET_APPSNO_LS_DEFAULT = 13;
    final static int ALIGNMENT_DEFAULT = 16; // 16 is middle

    final static int TASKLIST_QUEUE_LIMIT = 40;
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
    final static String STATUSBAR_ICON_DEFAULT = STATUSBAR_ICON_WHITE_WARM;

    final static int ICON_SIZE_DEFAULT = 1;

    final static int SERVICE_RUN_SCAN = 0;
    final static int SERVICE_DESTROY_NOTIFICATIONS = 1;
    final static int SERVICE_BUILD_TASKS = 2;
    final static int SERVICE_CLEAR_TASKS = 3;

    final static int THANK_YOU_GOOGLE = 0;
    final static int THANK_YOU_PAYPAL = 1;

    final static int START_SERVICE = 0;
    final static int STOP_SERVICE = 1;
    static DrawTasks drawT;
    static int displayWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        prefs = new PrefsGet(getSharedPreferences(getPackageName(), Context.MODE_MULTI_PROCESS));

        mContext = this;

        if (showChangelog(prefs)) {
            launchChangelog();
        }

        if (db == null) {
            db = new TasksDataSource(this);
            db.open();
        }

        drawT = new DrawTasks();
        myService = new ServiceCall(mContext);
        myService.setConnection(mConnection);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.title_activity_settings);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(3);

        mGetFragments = new GetFragments();
        mGetFragments.setFm(getFragmentManager());
        mGetFragments.setVp(mViewPager);

        ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
            int pagePosition;
            @Override
            public void onPageScrollStateChanged(int state) {
                try {
                    if (pagePosition == 2 && newStart && state == ViewPager.SCROLL_STATE_IDLE) {
                        newStart = false;
                        drawT.drawTasks(appsView);
                    }
                } catch (NullPointerException e) {
                    // Not yet created
                }
            }
            @Override
            public void onPageSelected(int position) {
                pagePosition = position;
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
        int allTasksSize = db.getAllTasks().size();
        if (allTasksSize == 0) {
            newStart = true;
        }
        pageChangeListener.onPageSelected(0);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Tools.HangarLog("onREsume Settings!");
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
            unbindService(myService.mConnection);
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
                .setIcon(R.drawable.ic_launcher)
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
                .setIcon(R.drawable.ic_launcher)
                .setView(mChg)
                .setNegativeButton(R.string.changelog_donate_button,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            launchDonate();

                            dialog.dismiss();
                        }

                    })
                .setPositiveButton(R.string.changelog_accept_button, null)
                .show();
    }

    protected void launchLicense() {
        License license = new License(this);
        View mLicense = license.getView();
        mLicense.refreshDrawableState();
        new AlertDialog.Builder(Settings.this)
                .setTitle(R.string.license_title)
                .setIcon(R.drawable.ic_launcher)
                .setView(mLicense)
                .setPositiveButton(R.string.license_accept_button, null)
                .show();
    }

    protected void launchDonate() {
        final Donate donate = new Donate(this);
        donate.bindServiceConn();
        View mDonate = donate.getView(mContext);
        mDonate.refreshDrawableState();
        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this)
                .setTitle(R.string.donate_title)
                .setIcon(R.drawable.ic_launcher)
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

        String whichVersion = mPrefs.getString(Settings.VERSION_CHECK, null);

        Tools.HangarLog("savedVer: " + whichVersion + " hangarVersion: " + hangarVersion);
        if (whichVersion != null) {
            if (whichVersion.equals(hangarVersion)) {
                return false;
            } else {
                mEditor.putString(Settings.VERSION_CHECK, hangarVersion);
                mEditor.apply();
                return true;
            }
        } else {
            mEditor.putString(Settings.VERSION_CHECK, hangarVersion);
            mEditor.apply();
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
        ServiceCall() {
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
                    case SERVICE_RUN_SCAN:
                        watchHelper(STOP_SERVICE);
                        watchHelper(START_SERVICE);
                        break;
                    case SERVICE_CLEAR_TASKS:
                        s.clearTasks();
                        break;
                    case SERVICE_BUILD_TASKS:
                        s.buildTasks();
                        return;
                    case SERVICE_DESTROY_NOTIFICATIONS:
                        s.destroyNotification();
                        s.clearTasks();
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
        if (id == R.id.action_stats_widget_settings) {
            startActivity(new Intent(mContext, StatsWidgetSettings.class));
            return true;
        } else if (id == R.id.action_apps_widget_settings) {
            startActivity(new Intent(mContext, AppsWidgetSettings.class));
            return true;
        } else if (id == R.id.action_instructions) {
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

    public boolean fadeTask(View view, TextView text) {
        if ((text.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) > 0) {
            view.setAlpha(1);
            text.setPaintFlags(text.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            return false;
        } else {
            text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            AlphaAnimation aa = new AlphaAnimation(1f, 0.3f);
            aa.setDuration(0);
            aa.setFillAfter(true);
            view.setAlpha((float) 0.5);
            return true;
        }
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
    public class DrawTasks {
        public void drawTasks(View view) {
            final LinearLayout taskRoot = (LinearLayout) view.findViewById(R.id.taskRoot);
            final Context mContext = getApplicationContext();

            new AsyncTask<Void, Void, ArrayList<LinearLayout>>() {
                @Override
                public void onPreExecute() {
                }
                @Override
                public void onPostExecute(ArrayList<LinearLayout> linearLayouts) {
                    taskRoot.removeAllViews();
                    for (LinearLayout taskRL : linearLayouts) {
                        taskRoot.addView(taskRL);
                    }
                }
                @Override
                public ArrayList<LinearLayout> doInBackground(Void... voidp) {
                    int highestSeconds = db.getHighestSeconds();
                    List<TasksModel> tasks = db.getAllTasks();
                    Collections.sort(tasks, new Tools.TasksModelComparator("seconds"));

                    Display display = getWindowManager().getDefaultDisplay();

                    Point size = new Point();
                    try {
                        display.getRealSize(size);
                        displayWidth = size.x;
                    } catch (NoSuchMethodError e) {
                        displayWidth = display.getWidth();
                    }

                    ArrayList<LinearLayout> linearLayouts = new ArrayList<LinearLayout>();

                    for (TasksModel task : tasks) {
                        LinearLayout taskRL = new LinearLayout(getApplicationContext());

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.topMargin = Tools.dpToPx(mContext, 6);
                        taskRL.setLayoutParams(params);
                        taskRL.setTag(task);
                        taskRL.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(final View view) {
                                final TasksModel task = (TasksModel) view.getTag();
                                final TextView text = (TextView) view.findViewWithTag("text");
                                PopupMenu popup = new PopupMenu(getApplicationContext(), view);
                                popup.getMenuInflater().inflate(R.menu.app_action, popup.getMenu());
                                PopupMenu.OnMenuItemClickListener menuAction = new PopupMenu.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        switch (item.getItemId()) {
                                            case R.id.action_blacklist:
                                                db.blacklistTask(task, fadeTask(view, text));
                                                Tools.updateWidget(getApplicationContext());
                                                break;
                                            case R.id.action_reset_stats:
                                                db.resetTaskStats(task);
                                                drawT.drawTasks(appsView);
                                                break;
                                        }
                                        myService.execute(SERVICE_CLEAR_TASKS);
                                        myService.watchHelper(STOP_SERVICE);
                                        myService.watchHelper(START_SERVICE);
                                        return true;
                                    }
                                };
                                popup.setOnMenuItemClickListener(menuAction);
                                popup.show();

                            }
                        });

                        TextView useStats = new TextView(mContext);
                        LinearLayout.LayoutParams useStatsParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        useStatsParams.topMargin = Tools.dpToPx(mContext, 30);
                        useStatsParams.leftMargin = Tools.dpToPx(mContext, 10);
                        useStatsParams.rightMargin = Tools.dpToPx(mContext, 4);
                        useStats.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                        useStats.setTag("usestats");
                        useStats.setLayoutParams(useStatsParams);
                        useStats.setTypeface(null, Typeface.BOLD);

                        LinearLayout textCont = new LinearLayout(mContext);
                        LinearLayout.LayoutParams useStatsLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        textCont.setLayoutParams(useStatsLayout);
                        textCont.setOrientation(LinearLayout.VERTICAL);

                        TextView taskName = new TextView(mContext);
                        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        nameParams.leftMargin = Tools.dpToPx(mContext, 10);
                        nameParams.topMargin = Tools.dpToPx(mContext, 4);
                        taskName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                        taskName.setTag("text");
                        taskName.setLayoutParams(nameParams);

                        LinearLayout barCont = new LinearLayout(mContext);
                        LinearLayout.LayoutParams barContLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        barContLayout.topMargin = Tools.dpToPx(mContext, 10);
                        barContLayout.leftMargin = Tools.dpToPx(mContext, 10);
                        barContLayout.height = Tools.dpToPx(mContext, 5);
                        barCont.setLayoutParams(barContLayout);

                        ImageView taskIcon = new ImageView(mContext);
                        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(Tools.dpToPx(mContext, 46),
                                Tools.dpToPx(mContext, 46));

                        iconParams.leftMargin = Tools.dpToPx(mContext, 6);
                        iconParams.rightMargin = Tools.dpToPx(mContext, 6);
                        iconParams.bottomMargin = Tools.dpToPx(mContext, 6);
                        taskIcon.setLayoutParams(iconParams);

                        try {
                            PackageManager pm = getApplicationContext().getPackageManager();
                            // Tools.HangarLog("Trying to grab AppInfo class[" + task.getClassName()+ "] package[" + task.getPackageName() + "]");
                            ComponentName componentTask = ComponentName.unflattenFromString(task.getPackageName() + "/" + task.getClassName());
                            ApplicationInfo appInfo = pm.getApplicationInfo(componentTask.getPackageName(), 0);

                            taskName.setText(task.getName());
                            taskIcon.setImageDrawable(appInfo.loadIcon(pm));
                        } catch (Exception e) {
                            Tools.HangarLog("Could not find Application info for [" + task.getName() + "]");
                            continue;
                        }

                        textCont.addView(taskName);
                        textCont.addView(barCont);
                        taskRL.addView(textCont);
                        taskRL.addView(useStats);
                        taskRL.addView(taskIcon);

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
                        useStats.setText(statsString);
                        barCont.setBackgroundColor(barColor);
                        int maxWidth = displayWidth - Tools.dpToPx(mContext, 46 + 14 + 90);
                        float adjustedWidth = maxWidth * secondsRatio;
                        barContLayout.width = Math.round(adjustedWidth);

                        // Tools.HangarLog("Blacklisted? [" + task.getBlacklisted() + "]");
                        if (task.getBlacklisted()) {
                            fadeTask(taskRL, taskName);
                        }
                        linearLayouts.add(taskRL);
                    }
                    return linearLayouts;
                }
            }.execute(null, null, null);
        }
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
        CheckBoxPreference divider_preference;
        CheckBoxPreference weighted_recents_preference;
        CheckBoxPreference colorize_preference;
        ColorPickerPreference icon_color_preference;
        SwitchPreference toggle_preference;
        SwitchPreference boot_preference;
        UpdatingListPreference appnos_preference;
        UpdatingListPreference priority_preference;
        UpdatingListPreference weight_priority_preference;
        UpdatingListPreference statusbar_icon_preference;

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
            SharedPreferences prefs2 = prefs.prefsGet();
            SharedPreferences.Editor editor = prefs.editorGet();

            try {
                // *** Appearance ***
                divider_preference = (CheckBoxPreference)findPreference(DIVIDER_PREFERENCE);
                divider_preference.setChecked(prefs2.getBoolean(DIVIDER_PREFERENCE, DIVIDER_DEFAULT));
                divider_preference.setOnPreferenceChangeListener(changeListener);

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

            } catch (NullPointerException e) {
            }
            try {
                // *** Behavior ***
                toggle_preference = (SwitchPreference)findPreference(TOGGLE_PREFERENCE);
                toggle_preference.setChecked(prefs2.getBoolean(TOGGLE_PREFERENCE, TOGGLE_DEFAULT));
                toggle_preference.setOnPreferenceChangeListener(changeListener);

                boot_preference = (SwitchPreference)findPreference(BOOT_PREFERENCE);
                boot_preference.setChecked(prefs2.getBoolean(BOOT_PREFERENCE, BOOT_DEFAULT));
                boot_preference.setOnPreferenceChangeListener(changeListener);

                weighted_recents_preference = (CheckBoxPreference)findPreference(WEIGHTED_RECENTS_PREFERENCE);
                weighted_recents_preference.setChecked(prefs2.getBoolean(WEIGHTED_RECENTS_PREFERENCE, WEIGHTED_RECENTS_DEFAULT));
                weighted_recents_preference.setOnPreferenceChangeListener(changeListener);

                weight_priority_preference = (UpdatingListPreference)findPreference(WEIGHT_PRIORITY_PREFERENCE);
                weight_priority_preference.setValue(prefs2.getString(WEIGHT_PRIORITY_PREFERENCE, Integer.toString(WEIGHT_PRIORITY_DEFAULT)));
                weight_priority_preference.setOnPreferenceChangeListener(changeListener);

                priority_preference = (UpdatingListPreference)findPreference(PRIORITY_PREFERENCE);
                priority_preference.setValue(prefs2.getString(PRIORITY_PREFERENCE, Integer.toString(PRIORITY_DEFAULT)));
                priority_preference.setOnPreferenceChangeListener(changeListener);
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
                    editor.apply();
                    myService.execute(SERVICE_CLEAR_TASKS);
                    myService.watchHelper(STOP_SERVICE);
                    myService.watchHelper(START_SERVICE);
                } else if (preference.getKey().equals(COLORIZE_PREFERENCE)) {
                    editor.putBoolean(COLORIZE_PREFERENCE, (Boolean) newValue);
                    editor.apply();
                    myService.execute(SERVICE_CLEAR_TASKS);
                    myService.watchHelper(STOP_SERVICE);
                    myService.watchHelper(START_SERVICE);
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
                                    editor.apply();
                                    PrefsFragment mBehaviorSettings = (PrefsFragment) mGetFragments.getFragmentByPosition(0);
                                    mBehaviorSettings.priority_preference.setValue(Integer.toString(PRIORITY_BOTTOM));
                                    myService.execute(SERVICE_DESTROY_NOTIFICATIONS);
                                    myService.watchHelper(STOP_SERVICE);
                                    myService.watchHelper(START_SERVICE);
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    statusbar_icon_preference.setValue(prefs2.getString(STATUSBAR_ICON_PREFERENCE, STATUSBAR_ICON_DEFAULT));
                                }
                            }).show();
                        return true;
                    } else {
                        editor.putString(STATUSBAR_ICON_PREFERENCE, (String) newValue);
                        editor.apply();
                        myService.execute(SERVICE_DESTROY_NOTIFICATIONS);
                    }
                } else if (preference.getKey().equals(ICON_COLOR_PREFERENCE)) {
                    String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(newValue)));
                    preference.setSummary(hex);
                    int intHex = ColorPickerPreference.convertToColorInt(hex);
                    editor.putInt(ICON_COLOR_PREFERENCE, intHex);
                    editor.apply();
                    myService.execute(SERVICE_CLEAR_TASKS);
                    myService.watchHelper(STOP_SERVICE);
                    myService.watchHelper(START_SERVICE);
                } else if (preference.getKey().equals(TOGGLE_PREFERENCE)) {
                    editor.putBoolean(TOGGLE_PREFERENCE, (Boolean) newValue);
                    editor.apply();

                    myService.execute(SERVICE_DESTROY_NOTIFICATIONS);
                    myService.watchHelper(STOP_SERVICE);
                    myService.watchHelper(START_SERVICE);

                    return true;
                } else if (preference.getKey().equals(BOOT_PREFERENCE)) {
                    editor.putBoolean(BOOT_PREFERENCE, (Boolean) newValue);
                    editor.apply();
                    return true;
                } else if (preference.getKey().equals(WEIGHT_PRIORITY_PREFERENCE)) {
                    editor.putString(WEIGHT_PRIORITY_PREFERENCE, (String) newValue);
                } else if (preference.getKey().equals(WEIGHTED_RECENTS_PREFERENCE)) {
                    editor.putBoolean(WEIGHTED_RECENTS_PREFERENCE, (Boolean) newValue);
                } else if (preference.getKey().equals(APPSNO_PREFERENCE)) {
                    editor.putString(APPSNO_PREFERENCE, (String) newValue);
                    editor.apply();
                    myService.execute(SERVICE_DESTROY_NOTIFICATIONS);
                    myService.watchHelper(STOP_SERVICE);
                    myService.watchHelper(START_SERVICE);
                    return true;
                } else if (preference.getKey().equals(PRIORITY_PREFERENCE)) {
                    String mPriorityPreference = (String) newValue;
                    editor.putString(PRIORITY_PREFERENCE, mPriorityPreference);
                    PrefsFragment mAppearanceSettings = (PrefsFragment) mGetFragments.getFragmentByPosition(1);
                    if (!mPriorityPreference.equals(PRIORITY_BOTTOM) &&
                            mAppearanceSettings.statusbar_icon_preference.getValue().equals(STATUSBAR_ICON_NONE)) {
                        editor.putString(STATUSBAR_ICON_PREFERENCE, STATUSBAR_ICON_DEFAULT);
                        mAppearanceSettings.statusbar_icon_preference.setValue(STATUSBAR_ICON_DEFAULT);
                    }
                    editor.apply();
                    myService.execute(SERVICE_DESTROY_NOTIFICATIONS);
                    myService.watchHelper(STOP_SERVICE);
                    myService.watchHelper(START_SERVICE);
                    return true;
                }
                editor.apply();
                myService.execute(SERVICE_CLEAR_TASKS);
                myService.watchHelper(STOP_SERVICE);
                myService.watchHelper(START_SERVICE);
                return true;
            }
        };
    };
    public static class AppsFragment extends Fragment {
        public static Fragment newInstance() {
            return new AppsFragment();
        }

        public AppsFragment() {
        }

        public void onResume() {
            super.onResume();
            drawT.drawTasks(appsView);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            setHasOptionsMenu(true);
            appsView = inflater.inflate(R.layout.apps_settings, container, false);
            // drawT.drawTasks(appsView);
            return appsView;
        }

    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter {

        // SharedPreferences prefs;
        // SharedPreferences.Editor editor;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int position) {
            switch (position) {
                case 0:
                    return PrefsFragment.newInstance(R.layout.behavior_settings);
                case 1:
                    return PrefsFragment.newInstance(R.layout.appearance_settings);
                default:
                    return AppsFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return mContext.getString(R.string.title_behavior).toUpperCase(l);
                case 1:
                    return mContext.getString(R.string.title_appearance).toUpperCase(l);
                case 2:
                    return mContext.getString(R.string.title_apps).toUpperCase(l);
            }
            return null;
        }
    }
}
