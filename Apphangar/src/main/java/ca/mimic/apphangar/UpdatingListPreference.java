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

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class UpdatingListPreference extends ListPreference
{
    public UpdatingListPreference(Context context)
    {
        super(context);
    }

    public UpdatingListPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public void setValue( final String value )
    {
        super.setValue(value);
        notifyChanged();
    }
    public void goDefault(String string) {
        super.setSummary(string);
        super.setValue(super.getValue());
    }
}