/*
* Copyright (C) 2013 SlimRoms Project
*
* Modifications to original by Jeff Corcoran
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package ca.mimic.apphangar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

public class ColorHelper {

    public static Bitmap getColoredBitmap(Drawable d, int color) {
        if (d == null) {
            return null;
        }
        Bitmap colorBitmap = ((BitmapDrawable) d).getBitmap();
        Bitmap brighterBitmap = toBrighter(colorBitmap);
        Bitmap grayscaleBitmap = toGrayscale(colorBitmap);
        Paint pp = new Paint();
        PorterDuffColorFilter frontFilter =
            new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
        pp.setColorFilter(frontFilter);
        Canvas cc = new Canvas(grayscaleBitmap);
        cc.drawBitmap(grayscaleBitmap, 0, 0, pp);
        return grayscaleBitmap;
    }

    private static Bitmap toBrighter(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpBrighter = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpBrighter);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        int e = 2;
        float e2 = (-.5f * e + .5f) * 255.f;
        float[] colorTransform = {
            e, 1f, 0, 0, e2,
            0, e, 0f, 0, e2,
            0, 0, e, 0f, e2,
            0, 0, 0, 1f, 0};
        cm.set(colorTransform);

        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpBrighter;
    }

    private static Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);

        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public static Drawable resize(Context context, Drawable image, int size) {
        if (image == null || context == null) {
            return null;
        }
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size,
                context.getResources().getDisplayMetrics());

        Bitmap d = ((BitmapDrawable) image).getBitmap();
        Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, px, px, false);
        return new BitmapDrawable(context.getResources(), bitmapOrig);
    }

}
