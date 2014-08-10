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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;

import ca.mimic.apphangar.Settings.PrefsGet;

public class AppsWidget extends AppWidgetProvider {
    protected static final int SMALL_ICONS = 0;
    protected static final int LARGE_ICONS = 2;

    protected static final int ICON_SMALL_HEIGHT = 40;
    protected static final int ICON_SMALL_WIDTH = 36;
    protected static final int ICON_MEDIUM_HEIGHT = 54;
    protected static final int ICON_MEDIUM_WIDTH = 50;
    protected static final int ICON_LARGE_HEIGHT = 70;
    protected static final int ICON_LARGE_WIDTH = 64;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Tools.HangarLog("onUpdate [" + this.getClass().getCanonicalName() + "]");

        // (re?)start service.  This is specifically so if hangar gets updated the service
        // is restarted
        Intent intent = new Intent(context, WatchfulService.class);
        context.startService(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Tools.HangarLog("onReceive [" + this.getClass().getCanonicalName() + "]");
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);

        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, AppsWidget.class));

        for(int id : ids) {
            Tools.HangarLog("per id: " + id);
            try {
                Bundle options=mgr.getAppWidgetOptions(id);
                updateAppWidget(context, mgr, id, options);
            } catch (Exception e) {
                e.printStackTrace();
                Tools.HangarLog("NPE onReceive");
            }
        }
        super.onReceive(context, intent);
    }

    static int findDimensions(int widgetDimension, int itemDimension) {
        return (int) Math.floor(widgetDimension / itemDimension);
    }

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle options) {

        Tools.HangarLog("updateAppWidget (AppsWidget)");
        PrefsGet prefs = new PrefsGet(context.getSharedPreferences("AppsWidget", Context.MODE_PRIVATE));

        SharedPreferences mPrefs = prefs.prefsGet();

        int rowLayout = R.layout.apps_widget_row;
        int itemLayout = R.layout.apps_widget_item;

        int itemHeight = ICON_MEDIUM_HEIGHT;
        int itemWidth = ICON_MEDIUM_WIDTH;

        final int TOP_ROW = 1;
        final int BOT_ROW = 2;

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
                    }
                }
                // Are we medium (either originally or via the above if?
                if (itemWidth == ICON_MEDIUM_WIDTH) {
                    appsNoH = findDimensions(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT), ICON_MEDIUM_HEIGHT);

                    if (appsNoH == 0 && widgetHeight > 0) {
                        Tools.HangarLog("Widget height > 0 but < 1 for iconSize.  Setting lower size");
                        rowLayout = R.layout.apps_widget_row_small;
                        itemWidth = ICON_SMALL_WIDTH;
                        // Get rid of the alignment wonkiness when forcing small icons
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

        TasksDataSource db = TasksDataSource.getInstance(context);
        db.open();

        int gridSize = (appsNoH * appsNoW);
        // numOfIcons should not exceed 35 (CPU reasons, etc)
        int numOfIcons = (appsNoH * appsNoW > Settings.TASKLIST_QUEUE_SIZE) ? Settings.TASKLIST_QUEUE_SIZE : (appsNoH * appsNoW);

        if (autoHeight && !appsNoByWidgetSize) {
            // Manual app # is selected.  Icons are split automatically from height.
            numOfIcons = appsNoW;
        }
        int queueSize = (Math.ceil(numOfIcons * 1.2f)) < Settings.APPLIST_QUEUE_SIZE ? Settings.APPLIST_QUEUE_SIZE : (int) Math.ceil(numOfIcons * 1.2f);

        String taskPackage = context.getPackageName();

        boolean weightedRecents = mPrefs.getBoolean(Settings.WEIGHTED_RECENTS_PREFERENCE,
                Settings.WEIGHTED_RECENTS_DEFAULT);
        ArrayList<Tools.TaskInfo> appList = Tools.buildTaskList(context, db, queueSize, weightedRecents, true);

        boolean ignorePinned = mPrefs.getBoolean(Settings.IGNORE_PINNED_PREFERENCE,
                Settings.IGNORE_PINNED_DEFAULT);
        if (!ignorePinned) {
            ArrayList<Tools.TaskInfo> pinnedList = Tools.buildPinnedList(context, db);
            appList = new Tools().getPinnedTasks(context, pinnedList, appList, numOfIcons, false);
        }

        db.close();

        int imageButtonLayout = context.getResources().getIdentifier("imageButton", "id", taskPackage);
        int imageContLayout = context.getResources().getIdentifier("imageCont", "id", taskPackage);

        AppDrawer appDrawer = new AppDrawer(taskPackage);
        appDrawer.createRow(rowLayout, R.id.viewRow);
        appDrawer.setImageLayouts(imageButtonLayout, imageContLayout);
        appDrawer.setPrefs(mPrefs);
        appDrawer.setContext(context);
        appDrawer.setRowBackgroundColor(getBackgroundColor, TOP_ROW);
        appDrawer.setCount(numOfIcons, Settings.TASKLIST_QUEUE_SIZE, true);

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

            boolean newItem = appDrawer.newItem(newItemTask, itemLayout);

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
        // Last row is bottom row
        appDrawer.setRowBackgroundColor(getBackgroundColor, BOT_ROW);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

}


