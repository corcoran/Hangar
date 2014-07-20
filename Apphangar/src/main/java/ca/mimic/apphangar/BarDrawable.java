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

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class BarDrawable extends Drawable {

    private int[] themeColors;

    public BarDrawable(int[] themeColors) {
        this.themeColors = themeColors;
    }

    @Override
    public void draw(Canvas canvas) {

        // get drawable dimensions
        Rect bounds = getBounds();

        int height = bounds.bottom - bounds.top;

        // draw background gradient
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(themeColors[0]);
        canvas.drawRect(0, 0, themeColors[1], height, backgroundPaint);
        if (themeColors[3] > 0) {
            backgroundPaint.setColor(themeColors[2]);
            canvas.drawRect(themeColors[1], 0, (themeColors[1] + themeColors[3]), height, backgroundPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

}
