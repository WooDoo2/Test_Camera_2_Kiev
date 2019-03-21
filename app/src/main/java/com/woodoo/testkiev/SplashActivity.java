package com.woodoo.testkiev;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;


public class SplashActivity extends ParentActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        justSplashActivity();
    }



    private void justSplashActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                strartMainActivity();
            }
        }, 1000 * 1);
    }

    private void strartMainActivity() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

}
