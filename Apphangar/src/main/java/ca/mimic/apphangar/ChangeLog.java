package ca.mimic.apphangar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChangeLog extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.changelog);
        LinearLayout v = (LinearLayout) findViewById(R.id.changeParent);

        String[] versionNumbers = getResources().getStringArray(R.array.versionNumbers);
        String[] versionSummaries = getResources().getStringArray(R.array.versionSummaries);

        for (int i = 0; i < versionNumbers.length; i++) {
            String version = versionNumbers[i];
            String summary = versionSummaries[i];

            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            llp.topMargin = Tools.dpToPx(this, 10);
            llp.leftMargin = Tools.dpToPx(this, 10);
            LinearLayout ll = new LinearLayout(this);
            ll.setOrientation(LinearLayout.VERTICAL);

            ll.setLayoutParams(llp);

            LinearLayout.LayoutParams llttv = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            llttv.bottomMargin = Tools.dpToPx(this, 2);
            TextView titletv = new TextView(this);
            titletv.setLayoutParams(llttv);
            titletv.setText(version);
            titletv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            titletv.setTypeface(null, Typeface.BOLD);
            titletv.setGravity(Gravity.CENTER_VERTICAL);
            titletv.setSingleLine();

            LinearLayout.LayoutParams llstv = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            TextView summarytv = new TextView(this);
            llstv.bottomMargin = Tools.dpToPx(this, 8);
            summarytv.setLayoutParams(llstv);
            summarytv.setGravity(Gravity.CENTER_VERTICAL);

            summarytv.setText(summary);
            summarytv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

            ll.addView(titletv);
            ll.addView(summarytv);
            v.addView(ll);
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Intent i = new Intent(this, Settings.class);
            startActivityForResult(i, 0);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
