package com.woodoo.testkiev.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Switch;

import com.woodoo.testkiev.App;
import com.woodoo.testkiev.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;


public class ServiceParams extends Service {
    public static final String TAG = ServiceParams.class.getSimpleName();

    public final static int ACTION_START = 100;
    public final static int ACTION_NEW_SETTINGS = 200;

    public final static String BROADCAST_BACK = "com.woodoo.testkiev.servicebackbroadcast";
    public static final String COMMAND_START = "COMMAND_START";
    public final static String COMMAND_STOP_SERVER = "COMMAND_STOP_SERVER";
    public final static String COMMAND_CHANGE_SETTINGS = "COMMAND_CHANGE_SETTINGS";

    private Thread timerThread;
    private boolean isStop = false;

    private App app;
    //String lastStringData="";


    @Override
    public void onCreate() {
        app = (App) getApplication();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && (intent.getAction() instanceof String)) {
            Log.d(TAG, "server onStartCommand "+intent.getAction());
            if (intent.getAction().equals(COMMAND_STOP_SERVER)) {
                stopSelf();
            }

            if (intent.getAction().equals(COMMAND_START)) {
                startThread();
            }
        }

        return START_STICKY;
    }

    private void startThread() {
        isStop = false;
        if (timerThread != null) {
            try {
                timerThread.interrupt();
                timerThread = null;
            } catch (Exception e) {}
        }


        timerThread = new Thread(new Runnable() {
            public void run() {
                while (!isStop) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    getCommandFromSocket();



                }
            }
        });
        timerThread.start();
    }




    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }


    @Override
    public void onDestroy() {
        isStop = true;
        if (timerThread != null) {
            //timerThread.stop();
            timerThread.interrupt();
        }

        super.onDestroy();
    }



    private void anons_action(int act) {
        Intent intent = new Intent(BROADCAST_BACK);
        intent.putExtra("action", act);
        sendBroadcast(intent);
    }

    private void getCommandFromSocket() {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("176.107.187.129", 1507), 5000);
            Log.d(TAG, "socket.connected");
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String newString = input.readLine();
            //String newString  = "{\"zoom\": 9, \"iso\": 100, \"exposure\": 0.1, \"size_x\": 640, \"size_y\": 480}";
            if (newString != null) {
                Log.d(TAG, newString);
                JSONObject json = new JSONObject(newString);
                if(json.has("zoom")){
                    app.pref.zoomLevel = (float) json.getDouble("zoom");
                }
                if(json.has("iso")){
                    app.pref.iso = json.getInt("iso");
                }
                if(json.has("exposure")){
                    app.pref.exposure = (long) (json.getDouble("exposure")*1000000000L);
                                                                                //500000000L
                }
                if(json.has("size_x")){
                    app.pref.size_x = json.getInt("size_x");
                }
                if(json.has("size_y")){
                    app.pref.size_y = json.getInt("size_y");
                }
                if(json.has("fps")){
                    double fps = json.getDouble("fps");
                    if(fps>20){fps=20;}
                    if(fps<0.1){fps=1;}
                    app.pref.fps = (float)fps ;
                }
                //app.pref.save();
                //lastStringData = newString;
                anons_action(ACTION_NEW_SETTINGS);

                Intent i = new Intent(this, Camera2Service.class);
                i.setAction(ServiceParams.COMMAND_CHANGE_SETTINGS);
                startService(i);

                //stopSelf();
            }

        } catch (Exception e) {
            //Log.e(TAG, e.toString());
        } finally {
            if(socket!=null){
                try {
                    socket.close();
                    socket=null;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

}
