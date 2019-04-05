package com.woodoo.testkiev.services;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Switch;

import com.woodoo.testkiev.App;
import com.woodoo.testkiev.MainActivity;
import com.woodoo.testkiev.R;
import com.woodoo.testkiev.utils.AlarmReceiver;

import org.json.JSONException;
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
    private boolean isStop = true;

    private App app;
    private int NID = 1;
    public static final String SECONDARY_CHANNEL = "channel1";

    private PowerManager.WakeLock wl;
    private WifiManager.WifiLock wfl;
    //String lastStringData="";


    @Override
    public void onCreate() {
        app = (App) getApplication();
        initEnergy();
        initAlarm();
    }

    @SuppressLint("InvalidWakeLockTag")
    private void initEnergy() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sync_all_cpu");
            wl.acquire();
        } catch (Exception e) {
        }


        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wfl = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "sync_all_wifi");
            wfl.acquire();
        } catch (Exception e) {
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && (intent.getAction() instanceof String)) {
            Log.d(TAG, "server onStartCommand " + intent.getAction() + " " + flags + "  " + startId + "  " + isStop);
            if (intent.getAction().equals(COMMAND_STOP_SERVER)) {
                stopSelf();
            }
            if (!isStop) return START_STICKY;

            if (intent.getAction().equals(COMMAND_START)) {
                startThread();
                showNotifications();
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
            } catch (Exception e) {
            }
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


    private void anons_action(int act) {
        Intent intent = new Intent(BROADCAST_BACK);
        intent.putExtra("action", act);
        sendBroadcast(intent);
    }

    private void getCommandFromSocket() {
        Socket socket = null;
        try {
            socket = new Socket();
            //socket.connect(new InetSocketAddress("176.107.187.129", 1507), 5000);
            socket.connect(new InetSocketAddress(app.pref.IP, 1507), 5000);
            Log.d(TAG, "socket.connected");
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String newString = input.readLine();
            input.close();
            //String newString  = "{\"zoom\": 9, \"iso\": 100, \"exposure\": 0.1, \"size_x\": 640, \"size_y\": 480}";
            if (newString != null) {
                boolean isNeedChange = false;
                Log.d(TAG, newString);
                JSONObject json = new JSONObject(newString);
                if (json.has("zoom")) {
                    float zoomLevel = (float) json.getDouble("zoom");
                    if (zoomLevel != app.pref.zoomLevel) {
                        isNeedChange = true;
                    }
                    app.pref.zoomLevel = zoomLevel;
                }
                if (json.has("iso")) {
                    int iso = json.getInt("iso");
                    if (iso != app.pref.iso) {
                        isNeedChange = true;
                    }
                    app.pref.iso = iso;
                }
                if (json.has("exposure")) {
                    long exposure = (long) (json.getDouble("exposure") * 1000000000L);
                    if (exposure != app.pref.exposure) {
                        isNeedChange = true;
                    }
                    app.pref.exposure = exposure;
                    //500000000L
                }
                if (json.has("size_x")) {
                    int size_x = json.getInt("size_x");
                    if(size_x!=app.pref.size_x){
                        isNeedChange = true;
                    }
                    app.pref.size_x = size_x;
                }
                if (json.has("size_y")) {
                    int size_y = json.getInt("size_y");
                    if(size_y!=app.pref.size_y){
                        isNeedChange = true;
                    }
                    app.pref.size_y = size_y;
                }
                if (json.has("fps")) {
                    double fps = json.getDouble("fps");
                    if (fps > 20) {
                        fps = 20;
                    }
                    if (fps < 0.1) {
                        fps = 1;
                    }
                    app.pref.fps = (float) fps;
                }
                if (json.has("rotate")) {
                    app.pref.rotate = json.getInt("rotate");
                }
                //app.pref.save();
                //lastStringData = newString;
                anons_action(ACTION_NEW_SETTINGS);

                if (json.has("command")) {
                    String cc = json.getString("command");
                    if (cc.equals("start")) {
                        sendCommandToCamera(ServiceParams.COMMAND_START);
                    } else if (cc.equals("stop")) {
                        sendCommandToCamera(ServiceParams.COMMAND_STOP_SERVER);
                    } else if (cc.equals("restart")) {
                        sendCommandToCamera(ServiceParams.COMMAND_STOP_SERVER);
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        sendCommandToCamera(ServiceParams.COMMAND_START);
                    }

                    return;
                }
                if(isNeedChange){
                    sendCommandToCamera(ServiceParams.COMMAND_CHANGE_SETTINGS);
                }

                //stopSelf();
            }


        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        } catch (Exception e) {
            //Log.e(TAG, e.toString());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                    socket = null;
                } catch (IOException e1) {

                }
            }
        }
    }

    private void sendCommandToCamera(String command) {
        Intent i = new Intent(this, Camera2Service.class);
        i.setAction(command);
        startService(i);
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved");
        Intent intent = new Intent("restartApps");
        sendBroadcast(intent);

        /*Intent restartService = new Intent(getApplicationContext(), this.getClass());
        restartService.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(this, 1, restartService, PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        //alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+1000, restartServicePI);
        alarmService.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000*6, restartServicePI);
*/

        //initAlarm();

        /*AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("YOUR_ACTION_NAME");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0);*/
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");

        Intent intent = new Intent("restartApps");
        sendBroadcast(intent);

        isStop = true;
        if (timerThread != null) {
            //timerThread.stop();
            timerThread.interrupt();
        }

        try {
            wl.release();
            wfl.release();
        } catch (Exception e) {
        }

        //NotificationManager notificationmanager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //notificationmanager.cancel(NID);

        super.onDestroy();
    }


    private void showNotifications() {
        NotificationManager notificationmanager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan2 = new NotificationChannel(SECONDARY_CHANNEL, "noti_channel_second", NotificationManager.IMPORTANCE_LOW);
            chan2.setLightColor(Color.BLUE);
            chan2.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationmanager.createNotificationChannel(chan2);
        }

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);

        Notification noti = new NotificationCompat.Builder(this, SECONDARY_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setContentTitle("camera work")
                //.setContentText("camera work long")
                .setAutoCancel(true)
                .setSound(null)
                //.setWhen(when)
                .setContentIntent(pIntent)
                .build();
        noti.flags = Notification.FLAG_NO_CLEAR;
        //notificationmanager.notify(NID, noti);
        startForeground(NID, noti);
        stopForeground(false);

    }


    private void initAlarm() {
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        alarmIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 60, 1000 * 60, pendingIntent);

    }


}
