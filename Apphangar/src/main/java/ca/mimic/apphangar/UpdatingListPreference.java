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
}