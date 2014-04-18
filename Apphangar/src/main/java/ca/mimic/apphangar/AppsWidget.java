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
    protected static final int SMALL_ICONS = 0;
    protected static final int LARGE_ICONS = 2;

    protected static final int MAX_DB_LOOKUPS = 12;
    protected static final int ICON_ROW_BUFFER = 0;

    protected static final int ICON_SMALL_HEIGHT = 40;
    protected static final int ICON_SMALL_WIDTH = 36;
    protected static final int ICON_MEDIUM_HEIGHT = 54;
    protected static final int ICON_MEDIUM_WIDTH = 50;
    protected static final int ICON_LARGE_HEIGHT = 70;
    protected static final int ICON_LARGE_WIDTH = 64;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Tools.HangarLog("onUpdate");
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BCAST_CONFIGCHANGED);
        context.getApplicationContext().registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Tools.HangarLog("onReceive");
        if (mContext == null) {
            mContext = context;
            IntentFilter filter = new IntentFilter();
            filter.addAction(BCAST_CONFIGCHANGED);
            context.getApplicationContext().registerReceiver(mBroadcastReceiver, filter);
        }

        AppWidgetManager mgr = AppWidgetManager.getInstance(context);

        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, AppsWidget.class));

        for(int id : ids) {
            Tools.HangarLog("per id: " + id);
            try {
                Bundle options=mgr.getAppWidgetOptions(id);
                updateAppWidget(context, mgr, id, options);
            } catch (NullPointerException e) {
                e.printStackTrace();
                Tools.HangarLog("NPE onReceive");
            }
        }
        super.onReceive(context, intent);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle options) {

        Tools.HangarLog("updateAppWidget");
        prefs = new PrefsGet(context.getSharedPreferences("AppsWidget", Context.MODE_PRIVATE));

        SharedPreferences mPrefs = prefs.prefsGet();

        int rowLayout = R.layout.apps_widget_row;
        int itemLayout = R.layout.apps_widget_item;

        int itemHeight = ICON_MEDIUM_HEIGHT;
        int itemWidth = ICON_MEDIUM_WIDTH;

        int iconSize = Integer.parseInt(mPrefs.getString(Settings.ICON_SIZE_PREFERENCE, Integer.toString(Settings.ICON_SIZE_DEFAULT)));

        // setSize
        switch (iconSize) {
            case SMALL_ICONS:
                rowLayout = R.layout.apps_widget_row_small;
                itemHeight = ICON_SMALL_HEIGHT;
                itemWidth = ICON_SMALL_WIDTH;
                break;
            case LARGE_ICONS:
                rowLayout = R.layout.apps_widget_row_large;
                itemHeight = ICON_LARGE_HEIGHT;
                itemWidth = ICON_LARGE_WIDTH;
                break;
        }

        boolean appsNoByWidgetSize = mPrefs.getBoolean(Settings.APPS_BY_WIDGET_SIZE_PREFERENCE, Settings.APPS_BY_WIDGET_SIZE_DEFAULT);
        int appsNoH;
        int appsNoW;
        boolean autoHeight = true;

        Tools.HangarLog("minHeight: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
        Tools.HangarLog("maxHeight: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
        Tools.HangarLog("minWidth: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
        Tools.HangarLog("maxWidth: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
        appsNoH = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) - ICON_ROW_BUFFER) / itemHeight);
        appsNoW = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) - ICON_ROW_BUFFER) / itemWidth);
        Tools.HangarLog("appsNoH: " + appsNoH + " appsNoW: " + appsNoW);

        if (appsNoH == 0) {
            appsNoH = 1;
            autoHeight = false;
        }

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            appsNoH = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) - ICON_ROW_BUFFER) / itemHeight);
            float widgetHeight = (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) - ICON_ROW_BUFFER);
            if (appsNoH == 0) {
                if (widgetHeight > 0) {
                    // add setSize
                    Tools.HangarLog("Widget height > 0 but < 1 for iconSize.  Setting small");
                    rowLayout = R.layout.apps_widget_row_small;
                    itemWidth = ICON_SMALL_WIDTH;
                }
                appsNoH = 1;
                autoHeight = false;
            }
            appsNoW = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) - ICON_ROW_BUFFER) / itemWidth);
            if (appsNoByWidgetSize && appsNoW > 0) {
                Tools.HangarLog("Landscape! appsNoByWidgetSize=true, appsNo=" + appsNoW);
            } else {
                appsNoW = Integer.parseInt(mPrefs.getString(Settings.STATS_WIDGET_APPSNO_LS_PREFERENCE, Integer.toString(Settings.STATS_WIDGET_APPSNO_LS_DEFAULT)));
            }
            Tools.HangarLog("LANDSCAPE");
        } else {
            if (appsNoByWidgetSize && appsNoW > 0) {
                Tools.HangarLog("appsNoByWidgetSize=true, appsNo=" + appsNoH);
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

        int gridSize = (appsNoH * appsNoW);
        int numOfIcons = (appsNoH * appsNoW);
        int lookUpNum = numOfIcons + 3; // 3 = saftey buffer

        if (autoHeight && !appsNoByWidgetSize) {
            // Manual app # is selected.  Icons are split automatically from height.
            numOfIcons = appsNoW;
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
            try {
                Intent intent = pkgm.getLaunchIntentForPackage(taskPackage);
                if (intent == null)
                    throw new PackageManager.NameNotFoundException();

            } catch (PackageManager.NameNotFoundException e) {
                db.deleteTask(taskM);
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
            Tools.HangarLog(" wP: " + weightPriority);
            appList = Tools.reorderTasks(appList, db, weightPriority);
        }

        RemoteViews row = new RemoteViews(context.getPackageName(), rowLayout);
        row.setInt(R.id.viewRow, "setBackgroundColor", getBackgroundColor);

        if (autoHeight && !appsNoByWidgetSize) {
            appsNoW = (int) Math.ceil((double) appsNoW / appsNoH);
            Tools.HangarLog("autoHeight true (start), appsNoW=" + appsNoW);
        }
        int filledConts = 0;
        int filledRows = 1;
        Tools.HangarLog("appsNoW: " + appsNoW + " appList.size(): " + appList.size() + " numOfIcons: " + numOfIcons);
        for (int i=0; i <= gridSize; i++) {
            RemoteViews item = new RemoteViews(context.getPackageName(), itemLayout);

            if (filledConts == appsNoW || i == gridSize) {
                Tools.HangarLog("i: " + i + " filledConts: " + filledConts);
                views.addView(R.id.viewCont, row);
                if (i >= numOfIcons && !appsNoByWidgetSize)
                    break;
                if (filledRows < appsNoH && (filledConts < numOfIcons && appList.size() > i)) {
                    row = new RemoteViews(context.getPackageName(), rowLayout);
                    row.setInt(R.id.viewRow, "setBackgroundColor", getBackgroundColor);

                    filledConts = 0;
                    filledRows++;
                } else {
                    break;
                }
            }

            filledConts += 1;

            int resID = context.getResources().getIdentifier("imageButton", "id", taskPackage);
            int contID = context.getResources().getIdentifier("imageCont", "id", taskPackage);

            if (i >= numOfIcons || i >= appList.size()) {
                item.setViewVisibility(contID, View.INVISIBLE);
                row.addView(R.id.viewRow, item);
                continue;
            }

            // item.setViewVisibility(contID, View.VISIBLE);
            Tools.HangarLog("Setting cont visible: " + filledConts + " [" + appList.get(i).appName + "]");

            Drawable taskIcon, d;
            try {
                ApplicationInfo appInfo = pkgm.getApplicationInfo(appList.get(i).packageName, 0);
                taskIcon = appInfo.loadIcon(pkgm);
            } catch (Exception e) {
                Tools.HangarLog("loadicon exception: " + e);
                continue;
            }

            if (isColorized) {
                d = new BitmapDrawable(ColorHelper.getColoredBitmap(taskIcon, getColor));
            } else {
                d = taskIcon;
            }

            Bitmap bmpIcon = ((BitmapDrawable) d).getBitmap();
            item.setImageViewBitmap(resID, bmpIcon);

            Intent intent;
            PackageManager manager = context.getPackageManager();
            try {
                intent = manager.getLaunchIntentForPackage(appList.get(i).packageName);
                if (intent == null) {
                    Tools.HangarLog("Couldn't get intent for [" + appList.get(i).packageName + "] className:" + appList.get(i).className);
                    filledConts --;
                    item.setViewVisibility(contID, View.GONE);
                    row.addView(R.id.viewRow, item);
                    throw new PackageManager.NameNotFoundException();
                }
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setAction("action" + (i));
                PendingIntent activity = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                item.setOnClickPendingIntent(contID, activity);
            } catch (PackageManager.NameNotFoundException e) {
                // grow numOfIcons since we're skipping this package.
                numOfIcons++;

            }
            row.addView(R.id.viewRow, item);
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
                        Tools.HangarLog("We're in the launcher changing orientation!");
                        AppsWidget.this.onReceive(context, new Intent());
                    }
                }

            }
        }
    };
}


