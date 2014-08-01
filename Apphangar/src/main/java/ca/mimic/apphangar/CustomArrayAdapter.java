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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

class CustomArrayAdapter extends ArrayAdapter<HashMap<Integer, String>> {

    Context mContext;
    List<HashMap<Integer, String>> items;
    Integer[] icons = new Integer[] {R.drawable.ic_action_settings, R.drawable.ic_action_apps_widget, R.drawable.ic_action_stats_widget};

    public CustomArrayAdapter(Context context, int resId, List<HashMap<Integer, String>> objects) {
        super(context, resId, objects);
        mContext = context;
        items = objects;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
        if (v == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            v = layoutInflater.inflate(android.R.layout.simple_spinner_item, parent, false);
        }
        ((TextView) v).setText(items.get(position).get(0));

        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View v = layoutInflater.inflate(R.layout.action_spinner_item, parent, false);
        TextView tv = (TextView)v.findViewById(R.id.spinner_text);
        ImageView iv = (ImageView) v.findViewById(R.id.spinner_icon);
        iv.setImageDrawable(mContext.getResources().getDrawable(icons[position]));
        iv.setAlpha(0.8f);
        tv.setText(items.get(position).get(position));

        return v;
    }
}
