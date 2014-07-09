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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import java.util.Random;

public class AppDrawer {
    Context mContext;
    RemoteViews mRowView;
    RemoteViews mLastItem;
    IconHelper ih;

    boolean isColorized;
    int getColor;

    int mImageButtonLayout;
    int mImageContLayout;
    int mRowId;

    int pendingNum;
    String mTaskPackage;


    AppDrawer(String packageName) {
        mTaskPackage = packageName;
    }

    protected void createRow(int rowLayout, int rowId) {
        mRowId = rowId;

        mRowView = new RemoteViews(mTaskPackage, rowLayout);
        mRowView.removeAllViews(mRowId);

        // Generate random number for pendingIntent
        Random r = new Random();
        pendingNum = r.nextInt(99 - 1 + 1) + 1;
    }

    protected void setImageLayouts(int imageButtonLayout, int imageContLayout) {
        mImageButtonLayout = imageButtonLayout;
        mImageContLayout = imageContLayout;
    }

    public void setContext(Context context) {
        mContext = context;
        ih = new IconHelper(context);
    }

    protected void setRowBackgroundColor(int color) {
        mRowView.setInt(mRowId, "setBackgroundColor", color);
    }

    protected void setPrefs(SharedPreferences prefs) {
        // set Prefs for mLastItems
        isColorized = prefs.getBoolean(Settings.COLORIZE_PREFERENCE, Settings.COLORIZE_DEFAULT);
        getColor = prefs.getInt(Settings.ICON_COLOR_PREFERENCE, Settings.ICON_COLOR_DEFAULT);
    }

    protected boolean newItem(Tools.TaskInfo taskItem, int mLastItemLayout) {
        mLastItem = new RemoteViews(mTaskPackage, mLastItemLayout);
        if (taskItem.packageName == null) {
            // Dummy invisible item
            return true;
        }

        PackageManager pkgm;
        Bitmap cachedIcon;
        try {
            pkgm = mContext.getPackageManager();
            ComponentName componentTask = ComponentName.unflattenFromString(taskItem.packageName + "/" + taskItem.className);

            cachedIcon = ih.cachedIconHelper(componentTask, taskItem.appName);

        } catch (Exception e) {
            Tools.HangarLog("newItem failed! " + e);
            return false;
        }



        if (isColorized) {
            Bitmap bmpIcon = ColorHelper.getColoredBitmap(cachedIcon, getColor);
            int size = Tools.dpToPx(mContext, 128);
            mLastItem.setImageViewBitmap(mImageButtonLayout, Bitmap.createScaledBitmap(bmpIcon, size, size, false));
        } else {
            mLastItem.setImageViewBitmap(mImageButtonLayout, cachedIcon);
        }

        Intent intent;
        try {
            intent = pkgm.getLaunchIntentForPackage(taskItem.packageName);
            if (intent == null) {
                Tools.HangarLog("Couldn't get intent for ["+ taskItem.packageName +"] className:" + taskItem.className);
                throw new PackageManager.NameNotFoundException();
            }
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setAction(Intent.ACTION_MAIN);
            PendingIntent activity = PendingIntent.getActivity(mContext, pendingNum, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            mLastItem.setOnClickPendingIntent(mImageContLayout, activity);
            mLastItem.setContentDescription(mImageButtonLayout, taskItem.appName);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        return true;
    }
    protected void addItem() {
        mRowView.addView(mRowId, mLastItem);
    }
    protected void setItemVisibility(int visibility) {
        mLastItem.setViewVisibility(mImageContLayout, visibility);
    }
    protected RemoteViews getRow() {
        return mRowView;
    }
}
