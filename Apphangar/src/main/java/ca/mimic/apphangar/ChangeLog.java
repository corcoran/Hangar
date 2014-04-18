package ca.mimic.apphangar;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChangeLog {
    Context context;

    ChangeLog(Context chgContext) {
        context = chgContext;
    }
    protected View getView() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View mChangeLog = inflater.inflate(R.layout.changelog, null);
        LinearLayout mChangeLogRoot = (LinearLayout) mChangeLog.findViewById(R.id.changeParent);

        String[] versionNumbers = context.getResources().getStringArray(R.array.versionNumbers);
        String[] versionSummaries = context.getResources().getStringArray(R.array.versionSummaries);

        for (int i = 0; i < versionNumbers.length; i++) {
            String version = versionNumbers[i];
            String summary = versionSummaries[i];

            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            llp.topMargin = Tools.dpToPx(context, 10);
            llp.leftMargin = Tools.dpToPx(context, 10);
            LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);

            ll.setLayoutParams(llp);

            LinearLayout.LayoutParams llttv = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            llttv.bottomMargin = Tools.dpToPx(context, 2);
            TextView titletv = new TextView(context);
            titletv.setLayoutParams(llttv);
            titletv.setText(version);
            titletv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            titletv.setTypeface(null, Typeface.BOLD);
            titletv.setGravity(Gravity.CENTER_VERTICAL);
            titletv.setSingleLine();

            LinearLayout.LayoutParams llstv = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            TextView summarytv = new TextView(context);
            llstv.bottomMargin = Tools.dpToPx(context, 8);
            summarytv.setLayoutParams(llstv);
            summarytv.setGravity(Gravity.CENTER_VERTICAL);

            summarytv.setText(summary);
            summarytv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

            ll.addView(titletv);
            ll.addView(summarytv);
            mChangeLogRoot.addView(ll);
        }
        mChangeLog.invalidate();
        return mChangeLog;
    }
}

