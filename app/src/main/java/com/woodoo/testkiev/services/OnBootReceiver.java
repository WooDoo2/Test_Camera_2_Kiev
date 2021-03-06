package com.woodoo.testkiev.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("mylog", " OnBootReceiver onReceive");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Intent i = new Intent(context, ServiceParams.class);
        i.setAction(ServiceParams.COMMAND_START);
        context.startService(i);
    }
}
