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
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
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

    protected void bindServiceConn() {
        context.bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"),
                mServiceConn, Context.BIND_AUTO_CREATE);
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

    protected View getView(Context settingsContext) {
        mSettingsContext = settingsContext;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        mDonate = inflater.inflate(R.layout.donate, null);

        TextView mJoinUsText = (TextView) mDonate.findViewById(R.id.donate_contribute);

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

        Button mPaypalButton = (Button) mDonate.findViewById(R.id.donate_paypal);
        mPaypalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(Uri.decode(context.getResources().getString(R.string.donate_paypal_url))));
                Settings.launchedPaypal(true);
                mAlert.cancel();
                context.startActivity(i);
            }
        });

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
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

            return pendingIntent;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }
}

