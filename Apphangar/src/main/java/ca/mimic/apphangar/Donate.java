package ca.mimic.apphangar;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Donate {
    Context context;

    Donate(Context donateContext) {
        context = donateContext;
    }
    protected View getView() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View mDonate = inflater.inflate(R.layout.donate, null);
        return mDonate;
    }
}

