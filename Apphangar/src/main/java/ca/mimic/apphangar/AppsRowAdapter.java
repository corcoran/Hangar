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

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class AppsRowAdapter extends BaseAdapter {
    final int taskNameColor = 0xFFBBBBBB;

    Context mContext;
    List<AppsRowItem> mRowItems;
    IconHelper ih;
    boolean completeRedraw = false;

    public AppsRowAdapter(Context context, List<AppsRowItem> rowItems) {
        mContext = context;
        mRowItems = rowItems;
        ih = new IconHelper(context);
    }

    private class ViewHolder {
        ImageView taskIcon;
        ImageView pinIcon;
        LinearLayout barCont;
        LinearLayout rowCont;
        TextView taskName;
        TextView useStats;
    }

    protected void reDraw(boolean which) {
        completeRedraw = which;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        AppsRowItem rowItem = (AppsRowItem) getItem(position);

        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.apps_settings_row,
                    parent, false);

            holder = new ViewHolder();
            holder.rowCont = (LinearLayout) convertView.findViewById(R.id.row_cont);
            holder.taskIcon = (ImageView) convertView.findViewById(R.id.task_icon);
            holder.pinIcon = (ImageView) convertView.findViewById(R.id.pin_icon);
            holder.barCont = (LinearLayout) convertView.findViewById(R.id.bar_cont);
            holder.taskName = (TextView) convertView.findViewById(R.id.task_name);
            holder.useStats = (TextView) convertView.findViewById(R.id.use_stats);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.taskName.setText(rowItem.getName());
        holder.pinIcon.setVisibility(rowItem.getPinned() ? ImageView.VISIBLE : ImageView.INVISIBLE);
        holder.taskName.setTextColor(rowItem.getPinned() ? Color.WHITE : taskNameColor);
        holder.useStats.setText(rowItem.getStats());
        holder.barCont.setBackgroundColor(rowItem.getBarColor());
        holder.barCont.setMinimumWidth(rowItem.getBarContWidth());
        holder.rowCont.setAlpha(1);
        holder.taskName.setPaintFlags(holder.taskName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        if (completeRedraw) {
            holder.taskIcon.setImageBitmap(null);
        }
        ih.cachedIconHelper(holder.taskIcon, rowItem.getComponentName(), rowItem.getName());

        if (rowItem.getBlacklisted()) {
            fadeCross(holder.rowCont);
        }

        return convertView;
    }

    public void fadeCross(View view) {
        TextView text = (TextView) view.findViewById(R.id.task_name);
        text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        AlphaAnimation aa = new AlphaAnimation(1f, 0.3f);
        aa.setDuration(0);
        aa.setFillAfter(true);
        view.setAlpha((float) 0.5);
    }

    @Override
    public int getCount() {
        return mRowItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mRowItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mRowItems.indexOf(getItem(position));
    }
}
