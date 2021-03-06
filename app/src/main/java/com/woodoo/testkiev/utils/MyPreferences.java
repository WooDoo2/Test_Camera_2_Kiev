package com.woodoo.testkiev.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class MyPreferences {
    private Context context;
    public float zoomLevel;
    public int cameraId;
    public boolean isFlash;
    public int iso;
    public int size_x;
    public int size_y;
    public long exposure;
    public float fps;
    public int rotate=0;
    public String IP="";


    public MyPreferences(Context ctx) {
        context = ctx;
        //PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
        //prefs = PreferenceManager.getDefaultSharedPreferences(context);
        //default_storage = Uri.fromFile(context.getFilesDir());
        read_preferences();
    }

    private void read_preferences() {
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(context);

        cameraId = 0;
        zoomLevel = 1;
        isFlash = false;
        iso = 100;
        size_x = 640;size_y = 480;
        //size_x = 720;size_y = 480;
        //size_x = 960;size_y = 720;
        //size_x = 1920;size_y = 1080;
        //size_x = 1280;size_y = 720;
        exposure = 0;
        fps = (float) 1;
        rotate = 0;
        IP = sPref.getString("IP", "176.107.187.129");

        /*cameraId = sPref.getInt("cameraId", 0);
        zoomLevel = sPref.getFloat("zoomLevel", 0);
        isFlash = sPref.getBoolean("isFlash", false);
        iso = sPref.getInt("iso", 100);
        size_x = sPref.getInt("size_x", 640);
        size_y = sPref.getInt("size_y", 480);
        exposure = sPref.getLong("exposure", 0);*/
    }

    public void save() {
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = sPref.edit();
        /*ed.putInt("cameraId", cameraId);
        ed.putFloat("zoomLevel", zoomLevel);
        ed.putBoolean("isFlash", isFlash);
        ed.putInt("iso", iso);
        ed.putInt("size_x", size_x);
        ed.putInt("size_y", size_y);
        ed.putLong("exposure", exposure);*/
        ed.putString("IP", IP);

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
