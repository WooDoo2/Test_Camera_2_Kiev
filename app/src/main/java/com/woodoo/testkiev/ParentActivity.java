package com.woodoo.testkiev;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.woodoo.testkiev.services.ServiceParams;

public abstract class ParentActivity extends AppCompatActivity {

    protected App app;
    private IntentFilter filterServicePlay;
    private BroadcastReceiver myServiceReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getApplicationContext();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initServiceListener();

    }


    private void initServiceListener() {
        filterServicePlay = new IntentFilter(ServiceParams.BROADCAST_BACK);

        myServiceReceiver = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }
                int action = 0;
                try {
                    action = intent.getIntExtra("action", 0);
                } catch (Exception e) {
                    return;
                }

                anonceFromSevice(action);
            }


        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(myServiceReceiver, filterServicePlay);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(myServiceReceiver);
        } catch (Exception e) {
        }
    }

    public abstract void anonceFromSevice(int action);


}
