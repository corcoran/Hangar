package ca.mimic.apphangar;

import android.app.ActivityManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
    protected static final int MEDIUM_ICONS = 1;
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

        // (re?)start service.  This is specifically so if hangar gets updated the service
        // is restarted
        Intent intent = new Intent(context, WatchfulService.class);
        context.startService(intent);

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

    static int findDimensions(int widgetDimension, int itemDimension) {
        return (int) Math.floor(widgetDimension / itemDimension);
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
        int mGravity = Integer.parseInt(mPrefs.getString(Settings.ALIGNMENT_PREFERENCE, Integer.toString(Settings.ALIGNMENT_DEFAULT)));

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
        appsNoH = findDimensions(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT), itemHeight);
        appsNoW = findDimensions(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH), itemWidth);
        Tools.HangarLog("appsNoH: " + appsNoH + " appsNoW: " + appsNoW);

        if (appsNoH == 0) {
            appsNoH = 1;
            autoHeight = false;
        }

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            float widgetHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

            appsNoH = findDimensions((int) widgetHeight, itemHeight);
            int origWidgetHeight = appsNoH;

            if (iconSize > SMALL_ICONS) {
                if (iconSize == LARGE_ICONS) {
                    appsNoH = findDimensions(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT), itemHeight);

                    if (appsNoH == 0 && widgetHeight > 0) {
                        Tools.HangarLog("Widget height > 0 but < 1 for iconSize.  Setting lower size");
                        rowLayout = R.layout.apps_widget_row;
                        itemWidth = ICON_MEDIUM_WIDTH;
                        mGravity = Settings.ALIGNMENT_DEFAULT;
                    }
                }
                // Are we medium (either originally or via the above if?
                if (itemWidth == ICON_MEDIUM_WIDTH) {
                    appsNoH = findDimensions(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT), ICON_MEDIUM_HEIGHT);

                    if (appsNoH == 0 && widgetHeight > 0) {
                        Tools.HangarLog("Widget height > 0 but < 1 for iconSize.  Setting lower size");
                        rowLayout = R.layout.apps_widget_row_small;
                        itemWidth = ICON_SMALL_WIDTH;
                        mGravity = Settings.ALIGNMENT_DEFAULT;
                    }
                }
            }

            if (origWidgetHeight == 0) {
                appsNoH = 1;
                autoHeight = false;
            }

            appsNoW = findDimensions(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH), itemWidth);
            if (appsNoByWidgetSize && appsNoW > 0) {
                Tools.HangarLog("Landscape! appsNoByWidgetSize=true, appsNo=" + appsNoW);
            } else {
                appsNoW = Integer.parseInt(mPrefs.getString(Settings.STATS_WIDGET_APPSNO_LS_PREFERENCE, Integer.toString(Settings.APPS_WIDGET_APPSNO_LS_DEFAULT)));
            }
            Tools.HangarLog("LANDSCAPE");
        } else {
            if (appsNoByWidgetSize && appsNoW > 0) {
                Tools.HangarLog("appsNoByWidgetSize=true, appsNo=" + appsNoH);
            } else {
                appsNoW = Integer.parseInt(mPrefs.getString(Settings.STATS_WIDGET_APPSNO_PREFERENCE, Integer.toString(Settings.APPS_WIDGET_APPSNO_DEFAULT)));
            }
        }

        int getBackgroundColor = mPrefs.getInt(Settings.BACKGROUND_COLOR_PREFERENCE, Settings.BACKGROUND_COLOR_DEFAULT);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.apps_widget);

        views.setInt(R.id.viewCont, "setGravity", mGravity);

        if (db == null) {
            db = new TasksDataSource(context);
            db.open();
        }

        int gridSize = (appsNoH * appsNoW);
        // numOfIcons should not exceed 35 (CPU reasons, etc)
        int numOfIcons = (appsNoH * appsNoW > 35) ? 35 : (appsNoH * appsNoW);

        if (autoHeight && !appsNoByWidgetSize) {
            // Manual app # is selected.  Icons are split automatically from height.
            numOfIcons = appsNoW;
        }
        int queueSize = (Math.ceil(numOfIcons * 1.2f)) < 14 ? 14 : (int) Math.ceil(numOfIcons * 1.2f);

        ArrayList<Tools.TaskInfo> appList = Tools.buildTaskList(context, db, queueSize);

        String taskPackage = context.getPackageName();

        boolean weightedRecents = mPrefs.getBoolean(Settings.WEIGHTED_RECENTS_PREFERENCE,
                Settings.WEIGHTED_RECENTS_DEFAULT);
        int weightPriority = Integer.parseInt(mPrefs.getString(Settings.WEIGHT_PRIORITY_PREFERENCE,
                Integer.toString(Settings.WEIGHT_PRIORITY_DEFAULT)));
        if (weightedRecents) {
            appList = Tools.reorderTasks(appList, db, weightPriority);
        }

        int imageButtonLayout = context.getResources().getIdentifier("imageButton", "id", taskPackage);
        int imageContLayout = context.getResources().getIdentifier("imageCont", "id", taskPackage);

        AppDrawer appDrawer = new AppDrawer(taskPackage);
        appDrawer.createRow(rowLayout, R.id.viewRow);
        appDrawer.setImageLayouts(imageButtonLayout, imageContLayout);
        appDrawer.setPrefs(mPrefs);
        appDrawer.setContext(mContext);
        appDrawer.setRowBackgroundColor(getBackgroundColor);

        if (autoHeight && !appsNoByWidgetSize) {
            appsNoW = (int) Math.ceil((double) appsNoW / appsNoH);
            Tools.HangarLog("autoHeight true (start), appsNoW=" + appsNoW);
        }
        int filledConts = 0;
        int filledRows = 1;

        Tools.HangarLog("appsNoW: " + appsNoW + " appList.size(): " + appList.size() + " numOfIcons: " + numOfIcons);

        views.removeAllViews(R.id.viewCont);

        for (int i=0; i <= gridSize; i++) {
            if (filledConts == appsNoW || i == gridSize) {
                Tools.HangarLog("i: " + i + " filledConts: " + filledConts);
                views.addView(R.id.viewCont, appDrawer.getRow());
                if (i >= numOfIcons && !appsNoByWidgetSize)
                    break;
                boolean lineBreak = (appsNoByWidgetSize && appList.size() > i) || (!appsNoByWidgetSize && numOfIcons > i);
                if (filledRows < appsNoH && filledConts < numOfIcons && lineBreak) {
                    appDrawer.createRow(rowLayout, R.id.viewRow);
                    appDrawer.setRowBackgroundColor(getBackgroundColor);

                    filledConts = 0;
                    filledRows++;
                } else {
                    break;
                }
            }

            Tools.TaskInfo newItemTask;
            if (i >= appList.size()) {
                newItemTask = new Tools.TaskInfo(null);
            } else {
                newItemTask = appList.get(i);
            }

            boolean newItem = appDrawer.newItem(newItemTask, itemLayout, i);

            if (!newItem) {
                numOfIcons++;
                continue;
            }

            filledConts += 1;

            if (i >= numOfIcons || i >= appList.size()) {
                appDrawer.setItemVisibility(View.INVISIBLE);
                appDrawer.addItem();
                continue;
            }

            appDrawer.addItem();
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
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


