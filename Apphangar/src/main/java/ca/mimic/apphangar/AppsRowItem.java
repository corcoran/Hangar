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

import android.content.ComponentName;

public class AppsRowItem extends TasksModel {
    public Boolean mPinned;
    public String mStats;
    public int mBarColor;
    public int mBarContWidth;
    public ComponentName mComponentName;

    AppsRowItem (TasksModel task) {
        setId(task.getId());
        setName(task.getName());
        setPackageName(task.getPackageName());
        setClassName(task.getClassName());
        setSeconds(task.getSeconds());
        setBlacklisted(task.getBlacklisted());
    }

    public void setComponentName(ComponentName componentName) {
        mComponentName = componentName;
    }

    public ComponentName getComponentName() {
        return mComponentName;
    }

    public void setPinned(Boolean pinned) {
        mPinned = pinned;
    }

    public Boolean getPinned() {
        return mPinned;
    }

    public void setStats(String stats) {
        mStats = stats;
    }

    public String getStats() {
        return mStats;
    }

    public void setBarColor(int barColor) {
        mBarColor = barColor;
    }

    public int getBarColor() {
        return mBarColor;
    }

    public void setBarContWidth(int barContWidth) {
        mBarContWidth = barContWidth;
    }

    public int getBarContWidth() {
        return mBarContWidth;
    }
}
