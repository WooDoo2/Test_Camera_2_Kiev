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
    public static final String COMMAND_START = "ACTION_PLAY";
    public final static String COMMAND_STOP_SERVER = "ACTION_STOP_SERVER";

    private Thread timerThread;
    private boolean isStop = false;

    private App app;
    String stringData;


    @Override
    public void onCreate() {
        app = (App) getApplication();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "server onStartCommand "+intent.getAction());

        if (intent != null && (intent.getAction() instanceof String)) {
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
                    getCommandFromSocket();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
            /*socket = new Socket();
            socket.connect(new InetSocketAddress("176.107.187.129", 1507), 5000);
            Log.d(TAG, "socket.connected");
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            stringData = input.readLine();*/
            stringData = "{\"zoom\": 1.1, \"iso\": 100, \"exposure\": 0.1, \"size_x\": 640, \"size_y\": 480}";
            if (stringData != null) {
                Log.d(TAG, stringData);
                JSONObject json = new JSONObject(stringData);
                if(json.has("zoom")){
                    app.pref.zoomLevel = (float) json.getDouble("zoom");
                }
                if(json.has("iso")){
                    app.pref.iso = json.getInt("iso");
                }
                if(json.has("exposure")){
                    app.pref.exposure = json.getDouble("exposure");
                }
                if(json.has("size_x")){
                    app.pref.size_x = json.getInt("size_x");
                }
                if(json.has("size_y")){
                    app.pref.size_y = json.getInt("size_y");
                }
                app.pref.save();

                anons_action(ACTION_NEW_SETTINGS);
            }

        } catch (Exception e) {
            Log.e(TAG, e.toString());
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
