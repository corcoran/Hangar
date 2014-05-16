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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootStart extends BroadcastReceiver {
    SharedPreferences prefs;

    public void onReceive(Context context, Intent arg1) {
        prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);

        if (prefs.getBoolean(Settings.BOOT_PREFERENCE, Settings.BOOT_DEFAULT)) {
            Tools.HangarLog("Starting WatchfulService on boot");

            Intent intent = new Intent(context, WatchfulService.class);
            context.startService(intent);
        } else {
            Tools.HangarLog("Start on boot [" + prefs.getBoolean(Settings.BOOT_PREFERENCE, Settings.BOOT_DEFAULT)  + "] or Notification disabled [" + prefs.getBoolean(Settings.TOGGLE_PREFERENCE, Settings.TOGGLE_DEFAULT) + "]");
        }
    }
}