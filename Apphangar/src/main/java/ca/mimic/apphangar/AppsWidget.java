package ca.mimic.apphangar;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

import ca.mimic.apphangar.Settings.PrefsGet;

public class AppsWidget extends AppWidgetProvider {

    protected static TasksDataSource db;
    protected static Context mContext;
    protected static PrefsGet prefs;

    protected static final String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";
    protected static final int MAX_DB_LOOKUPS = 12;
    protected static final int ICON_ROW_BUFFER = 10;
    protected static final int ICON_ROW_HEIGHT = 28; // 40 for small?
    protected static final int ICON_ROW_WIDTH = 28;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("Apphangar", "onUpdate");
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BCAST_CONFIGCHANGED);
        context.getApplicationContext().registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Apphangar", "onReceive");
        if (mContext == null) {
            mContext = context;
            IntentFilter filter = new IntentFilter();
            filter.addAction(BCAST_CONFIGCHANGED);
            context.getApplicationContext().registerReceiver(mBroadcastReceiver, filter);
        }

        AppWidgetManager mgr = AppWidgetManager.getInstance(context);

        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, AppsWidget.class));

        for(int id : ids) {
            Log.d("Apphangar", "per id: " + id);
            try {
                Bundle options=mgr.getAppWidgetOptions(id);
                updateAppWidget(context, mgr, id, options);
            } catch (NullPointerException e) {
                e.printStackTrace();
                Log.d("Apphangar", "NPE onReceive");
            }
        }
        super.onReceive(context, intent);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle options) {

        Log.d("Apphangar", "updateAppWidget");
        prefs = new PrefsGet(context.getSharedPreferences("AppsWidget", Context.MODE_PRIVATE));

        SharedPreferences mPrefs = prefs.prefsGet();

        int itemHeight = ICON_ROW_HEIGHT;
        int itemWidth = ICON_ROW_WIDTH;

        boolean appsNoByWidgetSize = mPrefs.getBoolean(Settings.APPS_BY_WIDGET_SIZE_PREFERENCE, Settings.APPS_BY_WIDGET_SIZE_DEFAULT);
        int appsNoH;
        int appsNoW;
        boolean autoHeight = true;

        Log.d(Settings.TAG, "jeff minHeight: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
        Log.d(Settings.TAG, "jeff maxHeight: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
        Log.d(Settings.TAG, "jeff minWidth: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
        Log.d(Settings.TAG, "jeff maxWidth: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
        appsNoH = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) - ICON_ROW_BUFFER) / itemHeight);
        appsNoW = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) - ICON_ROW_BUFFER) / itemWidth);
        Log.d(Settings.TAG, "jeff appsNoH: "  + appsNoH + " appsNoW: " + appsNoW);

        if (appsNoH == 0) {
            appsNoH = 1;
            autoHeight = false;
        }

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            appsNoH = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) - ICON_ROW_BUFFER) / itemHeight);
            if (appsNoH == 0) {
                appsNoH = 1;
                autoHeight = false;
            }
            appsNoW = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) - ICON_ROW_BUFFER) / itemWidth);
            if (appsNoByWidgetSize && appsNoW > 0) {
                Log.d("Apphangar", "Landscape! appsNoByWidgetSize=true, appsNo=" + appsNoW);
            } else {
                appsNoW = Integer.parseInt(mPrefs.getString(Settings.STATS_WIDGET_APPSNO_LS_PREFERENCE, Integer.toString(Settings.STATS_WIDGET_APPSNO_LS_DEFAULT)));
            }
            Log.d("Apphangar", "LANDSCAPE");
        } else {
            if (appsNoByWidgetSize && appsNoW > 0) {
                Log.d("Apphangar", "appsNoByWidgetSize=true, appsNo=" + appsNoH);
            } else {
                appsNoW = Integer.parseInt(mPrefs.getString(Settings.STATS_WIDGET_APPSNO_PREFERENCE, Integer.toString(Settings.STATS_WIDGET_APPSNO_DEFAULT)));
            }
        }

        int getBackgroundColor = mPrefs.getInt(Settings.BACKGROUND_COLOR_PREFERENCE, Settings.BACKGROUND_COLOR_DEFAULT);
        int getColor = mPrefs.getInt(Settings.ICON_COLOR_PREFERENCE, Settings.ICON_COLOR_DEFAULT);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.apps_widget);
        views.removeAllViews(R.id.viewCont);
        PackageManager pkgm = context.getPackageManager();

        if (db == null) {
            db = new TasksDataSource(context);
            db.open();
        }

        ArrayList<Tools.TaskInfo> appList = new ArrayList<Tools.TaskInfo>();

        int origWidth = (appsNoH * appsNoW);
        int lookUpNum = origWidth + 3; // 3 = saftey buffer

        if (autoHeight && !appsNoByWidgetSize) {
            // Manual app # is selected.  Icons are split automatically from height.
            origWidth = appsNoW;
            lookUpNum = appsNoW + 3;
        }
        List<TasksModel> tasks = db.getAllTasks((lookUpNum < MAX_DB_LOOKUPS) ? MAX_DB_LOOKUPS : lookUpNum);

        for (TasksModel taskM : tasks) {
            String taskPackage = taskM.getPackageName();

            if (isBlacklisted(taskPackage))
                continue;

            Tools.TaskInfo dbTask = new Tools.TaskInfo();
            dbTask.appName = taskM.getName();
            dbTask.packageName = taskPackage;
            dbTask.className = taskM.getClassName();
            dbTask.launches = taskM.getLaunches();
            dbTask.totalseconds = taskM.getSeconds();
            try {
                pkgm.getApplicationInfo(dbTask.packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                db.deleteTask(taskM);
                continue;
            }

            appList.add(dbTask);
        }

        String taskPackage = context.getPackageName();

        boolean isColorized = mPrefs.getBoolean(Settings.COLORIZE_PREFERENCE, Settings.COLORIZE_DEFAULT);
        boolean weightedRecents = mPrefs.getBoolean(Settings.WEIGHTED_RECENTS_PREFERENCE,
                Settings.WEIGHTED_RECENTS_DEFAULT);
        int weightPriority = Integer.parseInt(mPrefs.getString(Settings.WEIGHT_PRIORITY_PREFERENCE,
                Integer.toString(Settings.WEIGHT_PRIORITY_DEFAULT)));
        if (weightedRecents) {
            Log.d(Settings.TAG, " wP: " + weightPriority);
            appList = Tools.reorderTasks(appList, db, weightPriority);
        }

        RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.apps_widget_row);
        row.setInt(R.id.viewRow, "setBackgroundColor", getBackgroundColor);

        if (autoHeight && !appsNoByWidgetSize) {
            appsNoW = (int) Math.ceil((double) appsNoW / appsNoH);
            Log.d(Settings.TAG, "autoHeight true (start), appsNoW=" + appsNoW);
        }
        int filledConts = 0;
        int filledRows = 1;
        for (int i=0; i < appList.size(); i++) {

            if (filledConts == appsNoW || i == (appList.size()-1)) {
                views.addView(R.id.viewCont, row);
                if (filledRows < appsNoH && filledConts < origWidth) {
                    row = new RemoteViews(context.getPackageName(), R.layout.apps_widget_row);
                    row.setInt(R.id.viewRow, "setBackgroundColor", getBackgroundColor);
//                    if (autoHeight && !appsNoByWidgetSize) {
//                        leftoverWidth -= appsNoW;
//                        appsNoW = (int) Math.ceil((double) leftoverWidth / (appsNoH - filledRows));
//                        Log.d(Settings.TAG, "autoHeight true, appsNoW=" + appsNoW);
//                    }
                    filledConts = 0;
                    filledRows++;
                } else {
                    // Log.d(TAG, "filledConts [" + filledConts + "] == maxButtons [" + maxButtons + "]");
                    break;
                }
            }

            filledConts += 1;

            int resID = context.getResources().getIdentifier("imageButton" + (filledConts), "id", taskPackage);
            int contID = context.getResources().getIdentifier("imageCont" + (filledConts), "id", taskPackage);

            if (i >= origWidth) {
                row.setViewVisibility(contID, View.INVISIBLE);
                continue;
            }

            row.setViewVisibility(contID, View.VISIBLE);
            Log.d(Settings.TAG, "Setting cont visible: " + filledConts + " [" + appList.get(i).appName + "]");

            Drawable taskIcon, d;
            try {
                ApplicationInfo appInfo = pkgm.getApplicationInfo(appList.get(i).packageName, 0);
                taskIcon = appInfo.loadIcon(pkgm);
            } catch (Exception e) {
                Log.d(Settings.TAG, "loadicon exception: " + e);
                appList.remove(i);
                continue;
            }

            if (isColorized) {
                d = new BitmapDrawable(ColorHelper.getColoredBitmap(taskIcon, getColor));
            } else {
                d = taskIcon;
            }

            Bitmap bmpIcon = ((BitmapDrawable) d).getBitmap();
            row.setImageViewBitmap(resID, bmpIcon);

            Intent intent;
            PackageManager manager = context.getPackageManager();
            try {
                intent = manager.getLaunchIntentForPackage(appList.get(i).packageName);
                if (intent == null) {
                    Log.d(Settings.TAG, "Couldn't get intent for ["+ appList.get(i).packageName +"] className:" + appList.get(i).className);
                    filledConts --;
                    row.setViewVisibility(contID, View.GONE);
                    throw new PackageManager.NameNotFoundException();
                }
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setAction("action" + (i));
                PendingIntent activity = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                row.setOnClickPendingIntent(contID, activity);
            } catch (PackageManager.NameNotFoundException e) {

            }
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    protected static boolean isBlacklisted(String packageName) {
        for (String blTask : Tools.getBlacklisted(mContext, db)) {
            if (packageName.equals(blTask)) {
                return true;
            }
        }
        return false;
    }

    public BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent myIntent) {

            if ( myIntent.getAction().equals( BCAST_CONFIGCHANGED ) ) {
                final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(1);
                if (recentTasks.size() > 0) {
                    ComponentName task = recentTasks.get(0).baseActivity;
                    String taskPackage = task.getPackageName();
                    if (taskPackage.equals(Tools.getLauncher(context))) {
                        Log.d("Apphangar", "We're in the launcher changing orientation!");
                        AppsWidget.this.onReceive(context, new Intent());
                    }
                }

            }
        }
    };
}


