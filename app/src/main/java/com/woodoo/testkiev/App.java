package com.woodoo.testkiev;

import android.app.Application;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.woodoo.testkiev.utils.MyException;
import com.woodoo.testkiev.utils.MyPreferences;


public class App extends Application {
    public Typeface fontMain;
    public int screenWidth;
    public int screenHeight;
    public MyPreferences pref;


    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new MyException(this));
        fontMain = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/ubuntu_r.ttf");
        pref = new MyPreferences(this);
    }


    public void setScreenSize(int width, int height) {
        screenWidth = Math.min(width, height);
        screenHeight = Math.max(width, height);
    }

    public int koefWidth(double d) {
        return (int) Math.round(screenWidth * d);
    }

    public int koefHeight(double d) {
        return (int) Math.round(screenHeight * d);
    }


    public void makeToast(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        toast.show();
    }

    public void makeToast(int strRes) {
        makeToast(this.getString(strRes));
    }


    public void overrideFonts(final View v) {

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                overrideFonts(child);
            }
        } else if (v instanceof TextView) {
            TextView tv = (TextView) v;
            if (tv.getTypeface() != null) {
                if (tv.getTypeface().isBold()) {
                    tv.setTypeface(fontMain, Typeface.BOLD);
                } else {
                    tv.setTypeface(fontMain, Typeface.NORMAL);
                }
            } else {
                tv.setTypeface(fontMain, Typeface.NORMAL);
            }
            //tv.setTypeface(App.fontMain);
        }

    }
}
