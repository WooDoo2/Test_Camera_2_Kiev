package com.woodoo.testkiev.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class MyPreferences {
    public int zoomLevel;
    private Context context;
    public int cameraId;
    public boolean isFlash;


    public MyPreferences(Context ctx) {
        context = ctx;
        //PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
        //prefs = PreferenceManager.getDefaultSharedPreferences(context);
        //default_storage = Uri.fromFile(context.getFilesDir());
        read_preferences();
    }

    private void read_preferences() {
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(context);
        String str = sPref.getString("storageUri", null);
        cameraId = sPref.getInt("cameraId", 0);
        zoomLevel = sPref.getInt("zoomLevel", 0);
        isFlash = sPref.getBoolean("isFlash", false);
    }

    public void save() {
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putInt("cameraId", cameraId);
        ed.putInt("zoomLevel", zoomLevel);
        ed.putBoolean("isFlash", isFlash);
        /*ed.putString("storageUri", storageUri.toString());
        ed.putString("deleteRecords", deleteRecords);
        ed.putString("methodRecord", methodRecord);
        ed.putBoolean("isEnabled", isEnabled);
        ed.putBoolean("isFirstTime", isFirstTime);
        ed.putBoolean("isThemeBlack", isThemeBlack);
        ;*/

        ed.apply();

    }



}
