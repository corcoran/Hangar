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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

import java.util.Collections;
import java.util.List;

import ca.mimic.apphangar.Settings.PrefsGet;

public class StatsWidget extends AppWidgetProvider {

    protected static TasksDataSource db;
    protected static Context mContext;
    protected static PrefsGet prefs;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Tools.HangarLog("onUpdate [" + this.getClass().getCanonicalName() + "]");
        mContext = context;

        // (re?)start service.  This is specifically so if hangar gets updated the service
        // is restarted
        Intent intent = new Intent(context, WatchfulService.class);
        context.startService(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Tools.HangarLog("onReceive [" + this.getClass().getCanonicalName() + "]");
        if (mContext == null)
            mContext = context;

        AppWidgetManager mgr = AppWidgetManager.getInstance(context);

        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, StatsWidget.class));

        for(int id : ids) {
            Tools.HangarLog("per id: " + id);
            try {
                Bundle options=mgr.getAppWidgetOptions(id);
                updateAppWidget(context, mgr, id, options);
            } catch (NullPointerException e) {
                e.printStackTrace();
                Tools.HangarLog("NPE onReceive");
            }
            // mgr.notifyAppWidgetViewDataChanged(id, R.id.taskRoot);
        }
        super.onReceive(context, intent);
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(Tools.dpToPx(mContext, 100), Tools.dpToPx(mContext, 5), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle options) {

        Tools.HangarLog("updateAppWidget");
        prefs = new PrefsGet(context.getSharedPreferences("StatsWidget", Context.MODE_PRIVATE));

        SharedPreferences mPrefs = prefs.prefsGet();

        int statsLayout;
        int itemHeight;
        if (mPrefs.getBoolean(Settings.DIVIDER_PREFERENCE, Settings.DIVIDER_DEFAULT)) {
            statsLayout = R.layout.stats_widget;
            itemHeight = 36;
        } else {
            statsLayout = R.layout.stats_widget_no_dividers;
            itemHeight = 35; // Not as large w/o dividers
        }
        boolean appsNoByWidgetSize = mPrefs.getBoolean(Settings.APPS_BY_WIDGET_SIZE_PREFERENCE, Settings.APPS_BY_WIDGET_SIZE_DEFAULT);
        int appsNo;
        int appsNoLs;

        Tools.HangarLog("minHeight: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
        Tools.HangarLog("maxHeight: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
        appsNo = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) - 14) / itemHeight);
        appsNoLs = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) - 14) / itemHeight);

        if (appsNoByWidgetSize && appsNo > 0) {
            Tools.HangarLog("appsNoByWidgetSize=true, appsNo=" + appsNo);
        } else {
            appsNo = Integer.parseInt(mPrefs.getString(Settings.STATS_WIDGET_APPSNO_PREFERENCE, Integer.toString(Settings.STATS_WIDGET_APPSNO_DEFAULT)));
        }

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            if (appsNoByWidgetSize && appsNoLs > 0) {
                appsNo = appsNoLs;
                Tools.HangarLog("Landscape! appsNoByWidgetSize=true, appsNo=" + appsNo);
            } else {
                appsNo = Integer.parseInt(mPrefs.getString(Settings.STATS_WIDGET_APPSNO_LS_PREFERENCE, Integer.toString(Settings.STATS_WIDGET_APPSNO_LS_DEFAULT)));
            }
            Tools.HangarLog("LANDSCAPE");
        }

        int getColor = mPrefs.getInt(Settings.BACKGROUND_COLOR_PREFERENCE, Settings.BACKGROUND_COLOR_DEFAULT);

        RemoteViews views = new RemoteViews(context.getPackageName(), statsLayout);
        int mGravity = Integer.parseInt(mPrefs.getString(Settings.ALIGNMENT_PREFERENCE, Integer.toString(Settings.ALIGNMENT_DEFAULT)));
        views.setInt(R.id.taskRoot, "setGravity", mGravity);
        views.removeAllViews(R.id.taskRoot);

        PackageManager pkgm = context.getPackageManager();
        String packageName = context.getPackageName();
        Intent intent;

        if (db == null) {
            db = new TasksDataSource(context);
            db.open();
        }
        int highestSeconds = db.getHighestSeconds();
        List<TasksModel> tasks = db.getAllTasks();

        Collections.sort(tasks, new Tools.TasksModelComparator("seconds"));

        int count = 0;
        for (TasksModel task : tasks) {
            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.stats_widget_row);

            int topPadding = 0;
            int bottomPadding = 0;

            if (count == 0)
                topPadding = Tools.dpToPx(context, 6);
            if (count == (appsNo-1))
                bottomPadding = Tools.dpToPx(context, 6);

            row.setViewPadding(R.id.appCont, 0, topPadding, 0, bottomPadding);

            int clickID = context.getResources().getIdentifier("clickCont", "id", packageName);
            int iconID = context.getResources().getIdentifier("iconCont", "id", packageName);
            int labelID = context.getResources().getIdentifier("appName", "id", packageName);
            int imgID = context.getResources().getIdentifier("barImg", "id", packageName);
            int statsID = context.getResources().getIdentifier("statsCont", "id", packageName);

            if (task.getBlacklisted()) { continue; }
            if (count >= appsNo) {
                Tools.HangarLog("count: " + count + " appsNo: " + appsNo);
                break;
            }

            // Drawable taskIcon;
            Uri uri = null;
            try {
                ApplicationInfo appInfo = pkgm.getApplicationInfo(task.getPackageName(), 0);
                if(appInfo.icon != 0) {
                    uri = Uri.parse("android.resource://" + task.getPackageName() + "/" + appInfo.icon);
                }
                // taskIcon = appInfo.loadIcon(pkgm);
            } catch (Exception e) {
                continue;
            }

            count++;

            // Bitmap bmpIcon = ((BitmapDrawable) taskIcon).getBitmap();
            // row.setImageViewBitmap(iconID, bmpIcon);
            row.setImageViewUri(iconID, uri);
            row.setTextViewText(labelID, task.getName());

            // int maxWidth = dpToPx(250) - dpToPx(32+14+14); // ImageView + Margin? + Stats text?
            float secondsRatio = (float) task.getSeconds() / highestSeconds;
            int barColor;
            int secondsColor = (Math.round(secondsRatio * 100));
            if (secondsColor >= 80 ) {
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
            int[] colors = new int[]{barColor, Tools.dpToPx(context, secondsColor-1), 0x00000000, Tools.dpToPx(mContext, 100-secondsColor)};
            Drawable sd = new BarDrawable(colors);
            Bitmap bmpIcon2 = drawableToBitmap(sd);
            row.setImageViewBitmap(imgID, bmpIcon2);

            int[] statsTime = new Settings().splitToComponentTimes(task.getSeconds());
            String statsString = ((statsTime[0] > 0) ? statsTime[0] + "h " : "") + ((statsTime[1] > 0) ? statsTime[1] + "m " : "") + ((statsTime[2] > 0) ? statsTime[2] + "s " : "");
            row.setTextViewText(statsID, statsString);

            try {
                intent = pkgm.getLaunchIntentForPackage(task.getPackageName());
                if (intent == null) {
                    count --;
                    throw new PackageManager.NameNotFoundException();
                }
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setAction("action" + (count));
                PendingIntent activity = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                row.setOnClickPendingIntent(clickID, activity);
            } catch (PackageManager.NameNotFoundException e) {

            }

            row.setInt(R.id.appCont, "setBackgroundColor", getColor);
            views.addView(R.id.taskRoot, row);
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}


