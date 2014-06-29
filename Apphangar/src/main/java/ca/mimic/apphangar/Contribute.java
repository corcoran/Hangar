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
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Contribute {
    Context context;
    View mContribute;

    Contribute(Context context) {
        this.context = context;
    }

    protected View getView() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        mContribute = inflater.inflate(R.layout.contribute, null);

        TextView mViewSource = (TextView) mContribute.findViewById(R.id.contribute_google);
        mViewSource.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

        LinearLayout mViewSourceCont = (LinearLayout) mViewSource.getParent();
        mViewSourceCont.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(context.getResources().getString(R.string.contribute_google_url)));
                context.startActivity(i);
            }
        });

        return mContribute;
    }
}
