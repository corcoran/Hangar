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
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.mimic.apphangar.Settings.PrefsGet;

public class AppsWidget extends AppWidgetProvider {

    protected static TasksDataSource db;
    protected static Context mContext;
    protected static PrefsGet prefs;

    protected static final String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";
    protected static final int MAX_DB_LOOKUPS = 20;

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
        prefs = new PrefsGet(context.getSharedPreferences("StatsWidget", Context.MODE_PRIVATE));

        SharedPreferences mPrefs = prefs.prefsGet();

        int itemHeight = 35;

        // boolean appsNoByWidgetSize = mPrefs.getBoolean(Settings.APPS_BY_WIDGET_SIZE_PREFERENCE, Settings.APPS_BY_WIDGET_SIZE_DEFAULT);
        int appsNo;

        Log.d("Apphangar", "minHeight: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
        Log.d("Apphangar", "maxHeight: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
        appsNo = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) - 14) / itemHeight);

//        if (appsNoByWidgetSize && appsNo > 0) {
//            Log.d("Apphangar", "appsNoByWidgetSize=true, appsNo=" + appsNo);
//        } else {
//            appsNo = Integer.parseInt(mPrefs.getString(Settings.STATS_WIDGET_APPSNO_PREFERENCE, Integer.toString(Settings.STATS_WIDGET_APPSNO_DEFAULT)));
//        }

//        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
//            if (appsNoByWidgetSize && appsNoLs > 0) {
//                appsNo = appsNoLs;
//                Log.d("Apphangar", "Landscape! appsNoByWidgetSize=true, appsNo=" + appsNo);
//            } else {
//                appsNo = Integer.parseInt(mPrefs.getString(Settings.STATS_WIDGET_APPSNO_LS_PREFERENCE, Integer.toString(Settings.STATS_WIDGET_APPSNO_LS_DEFAULT)));
//            }
//            Log.d("Apphangar", "LANDSCAPE");
//        }

        int getColor = mPrefs.getInt(Settings.BACKGROUND_COLOR_PREFERENCE, Settings.BACKGROUND_COLOR_DEFAULT);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.apps_widget);
        PackageManager pkgm = context.getPackageManager();

        views.setInt(R.id.taskRoot, "setBackgroundColor", getColor);

        if (db == null) {
            db = new TasksDataSource(context);
            db.open();
        }

        ArrayList<Tools.TaskInfo> taskList = new ArrayList<Tools.TaskInfo>();
        List<TasksModel> tasks = db.getAllTasks(MAX_DB_LOOKUPS);

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

            taskList.add(dbTask);
        }

        String taskPackage = context.getPackageName();

        boolean weightedRecents = mPrefs.getBoolean(Settings.WEIGHTED_RECENTS_PREFERENCE,
                Settings.WEIGHTED_RECENTS_DEFAULT);
        int weightPriority = Integer.parseInt(mPrefs.getString(Settings.WEIGHT_PRIORITY_PREFERENCE,
                Integer.toString(Settings.WEIGHT_PRIORITY_DEFAULT)));
        if (weightedRecents)
            taskList = Tools.reorderTasks(taskList, db, weightPriority);

        int filledConts = 0;
        for (int i=0; i < taskList.size(); i++) {
            int resID = context.getResources().getIdentifier("imageButton" + (filledConts+1), "id", taskPackage);
            int contID = context.getResources().getIdentifier("imageCont" + (filledConts+1), "id", taskPackage);

            if (filledConts == 9) {
                // Log.d(TAG, "filledConts [" + filledConts + "] == maxButtons [" + maxButtons + "]");
                break;
            }

            filledConts += 1;
            views.setViewVisibility(contID, View.VISIBLE);
            Log.d(Settings.TAG, "Setting cont visible: " + filledConts + " [" + taskList.get(i).appName + "]");

            Drawable taskIcon, d;
            try {
                ApplicationInfo appInfo = pkgm.getApplicationInfo(taskList.get(i).packageName, 0);
                taskIcon = appInfo.loadIcon(pkgm);
            } catch (Exception e) {
                Log.d(Settings.TAG, "loadicon exception: " + e);
                taskList.remove(i);
                continue;
            }

//            if (isColorized) {
//                d = new BitmapDrawable(ColorHelper.getColoredBitmap(taskIcon, getColor));
//            } else {
              d = taskIcon;
//            }

            Bitmap bmpIcon = ((BitmapDrawable) d).getBitmap();
            views.setImageViewBitmap(resID, bmpIcon);

            Intent intent;
            PackageManager manager = context.getPackageManager();
            try {
                intent = manager.getLaunchIntentForPackage(taskList.get(i).packageName);
                if (intent == null) {
                    Log.d(Settings.TAG, "Couldn't get intent for ["+ taskList.get(i).packageName +"] className:" + taskList.get(i).className);
                    filledConts --;
                    views.setViewVisibility(contID, View.GONE);
                    throw new PackageManager.NameNotFoundException();
                }
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setAction("action" + (i));
                PendingIntent activity = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                views.setOnClickPendingIntent(contID, activity);
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


