package com.woodoo.testkiev;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ParentActivity extends AppCompatActivity {
    protected App app;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        app = (App) getApplicationContext();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        //setTheme(app.pref.getThemeRes());
    }



}
