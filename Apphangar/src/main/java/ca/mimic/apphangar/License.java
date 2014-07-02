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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class License {
    Context context;
    View mLicense;

    License(Context context) {
        this.context = context;
    }

    @SuppressLint("InflateParams")
    protected View getView() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        mLicense = inflater.inflate(R.layout.license, null);

        TextView mViewSource = (TextView) mLicense.findViewById(R.id.license_view_source);
        mViewSource.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

        LinearLayout mViewSourceCont = (LinearLayout) mViewSource.getParent();
        mViewSourceCont.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(context.getResources().getString(R.string.license_github_url)));
                context.startActivity(i);
            }
        });

        return mLicense;
    }
}
