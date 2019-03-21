package com.woodoo.testkiev;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ParentActivity extends AppCompatActivity {
    protected App app;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //setTheme(android.R.style.Theme_Black);
        app = (App) getApplicationContext();
        super.onCreate(savedInstanceState);
        //setTheme(app.pref.getThemeRes());
    }



}
