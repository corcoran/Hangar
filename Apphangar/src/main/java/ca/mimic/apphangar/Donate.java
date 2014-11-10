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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.vending.billing.IInAppBillingService;

import java.util.ArrayList;
import java.util.List;

public class Donate {
    Context context;
    Context mSettingsContext;

    IInAppBillingService mService;
    ServiceConnection mServiceConn;
    ArrayList<PendingIntent> pIntents;
    AlertDialog mAlert;
    View mDonate;

    Donate(Context donateContext) {
        context = donateContext;
        pIntents = new ArrayList<PendingIntent>();

        mServiceConn = new ServiceConnection() {
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }

            public void onServiceConnected(ComponentName name,
                                           IBinder service) {
                mService = IInAppBillingService.Stub.asInterface(service);
            }

        };
    }

    /***
     * Android L (lollipop, API 21) introduced a new problem when trying to invoke implicit intent,
     * "java.lang.IllegalArgumentException: Service Intent must be explicit"
     *
     * If you are using an implicit intent, and know only 1 target would answer this intent,
     * This method will help you turn the implicit intent into the explicit form.
     *
     * Inspired from SO answer: http://stackoverflow.com/a/26318757/1446466
     * @param context
     * @param implicitIntent - The original implicit intent
     * @return Explicit Intent created from the implicit original intent
     */
    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    protected void bindServiceConn() {
        Intent intent = createExplicitFromImplicitIntent(context, new Intent("com.android.vending.billing.InAppBillingService.BIND"));
        context.bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    protected void unbindServiceConn() {
        context.unbindService(mServiceConn);
    }

    protected void launchBilling(Context context, PendingIntent pendingIntent) {
        try {
            Tools.HangarLog("launchBilling!");
            ((Activity) context).startIntentSenderForResult(pendingIntent.getIntentSender(),
                    1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0));
            mAlert.cancel();
        } catch (IntentSender.SendIntentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @SuppressLint("InflateParams")
    protected View getView(Context settingsContext) {
        mSettingsContext = settingsContext;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        mDonate = inflater.inflate(R.layout.donate, null);

        TextView mJoinUsText = (TextView) mDonate.findViewById(R.id.donate_contribute);
        mJoinUsText.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

        LinearLayout mJoinUsCont = (LinearLayout) mJoinUsText.getParent();
        mJoinUsCont.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Settings.launchContribute(context);
            }
        });

        final Button mDonateButton = (Button) mDonate.findViewById(R.id.donate_google);
        mDonateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Spinner mDonateSpinner = (Spinner) mDonate.findViewById(R.id.donate_spinner);
                int spinnerPos = mDonateSpinner.getSelectedItemPosition() + 1;
                launchBilling(mSettingsContext, getIntent(spinnerPos));
            }
        });

//        Button mPaypalButton = (Button) mDonate.findViewById(R.id.donate_paypal);
//        mPaypalButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent i = new Intent(Intent.ACTION_VIEW);
//                i.setData(Uri.parse(Uri.decode(context.getResources().getString(R.string.donate_paypal_url))));
//                Settings.launchedPaypal(true);
//                mAlert.cancel();
//                context.startActivity(i);
//            }
//        });

        return mDonate;
    }

    protected void setAlert(AlertDialog alert) {
        mAlert = alert;
    }

    protected PendingIntent getIntent(int num) {
        try {
            Tools.HangarLog("getIntent: " + num);
            if (mService == null) {
                Tools.HangarLog("mService is null!");
                return null;
            }

            String sku = "donate_" + num;
            Bundle buyIntentBundle = mService.getBuyIntent(3, context.getPackageName(),
                    sku, "inapp", null);
            Tools.HangarLog("buyIntentBundle: " + buyIntentBundle);
            return buyIntentBundle.getParcelable("BUY_INTENT");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }
}

