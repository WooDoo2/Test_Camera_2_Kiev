package com.woodoo.testkiev.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.woodoo.testkiev.services.ServiceParams;


public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ServiceParams", " AlarmReceiver onReceive");

        Intent i = new Intent(context, ServiceParams.class);
        i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        i.setPackage(context.getPackageName());
        i.setAction(ServiceParams.COMMAND_START);
        context.startService(i);


    }
}
