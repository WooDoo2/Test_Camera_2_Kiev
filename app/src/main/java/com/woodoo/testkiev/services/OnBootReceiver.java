package com.woodoo.testkiev.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, ServiceParams.class);
        i.setAction(ServiceParams.COMMAND_START);
        context.startService(i);
    }
}
