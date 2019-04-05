package com.woodoo.testkiev;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.woodoo.testkiev.services.Camera2Service;
import com.woodoo.testkiev.services.ServiceParams;
import com.woodoo.testkiev.utils.AlarmReceiver;

public class MainActivity extends ParentActivity /*implements TextureView.SurfaceTextureListener*/ {
    public static final String TAG = "mylog";

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private TextView tvDetails;
    private DrawerLayout drawer;
    private Button btnStartStop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        initMain();
        initMain2();

        if (checkPermissions()) {
            serviceParamsStart(null);
            //cameraServiceStart();
        }


    }

    private void initMain2() {
        final EditText editIP = findViewById(R.id.editIP);
        editIP.setText(app.pref.IP);

        btnStartStop = findViewById(R.id.btnStartStop);
        btnStartStop.setTag(false);
        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editIP.getText().equals("")) {
                    return;
                }
                app.pref.IP = editIP.getText().toString();
                app.pref.save();

                boolean status = !(boolean) v.getTag();
                if (status) {
                    //serviceParamsStart();
                    cameraServiceStart();
                    btnStartStop.setText("STOP");
                } else {
                    //serviceParamsStop();
                    cameraServiceStop();
                    btnStartStop.setText("START");
                }
                btnStartStop.setTag(status);

            }
        });
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
            return false;
        }
        return true;
    }


    private void cameraServiceStart() {
        Intent i = new Intent(this, Camera2Service.class);
        i.setAction(ServiceParams.COMMAND_START);
        startService(i);
    }

    private void cameraServiceStop() {
        Intent i = new Intent(this, Camera2Service.class);
        i.setAction(ServiceParams.COMMAND_STOP_SERVER);
        startService(i);
    }

    public void serviceParamsStart(View v) {
        Intent i = new Intent(this, ServiceParams.class);
        i.setAction(ServiceParams.COMMAND_START);
        startService(i);


        /*Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, 1000 * 6, pendingIntent);
*/

    }

    private void serviceParamsStop() {
        Intent i = new Intent(this, ServiceParams.class);
        i.setAction(ServiceParams.COMMAND_STOP_SERVER);
        startService(i);
    }

    @Override
    public void anonceFromSevice(int action) {
        updateTvDetails();
    }


    private void initMain() {
        app.overrideFonts(findViewById(R.id.root));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        ((Switch) findViewById(R.id.switchFlash)).setChecked(app.pref.isFlash);
        tvDetails = findViewById(R.id.tvDetails);
        updateTvDetails();


        Button btnTimer = findViewById(R.id.btnTimer);
        btnTimer.setTag(false);
        btnTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean b = !(boolean) v.getTag();
                v.setTag(b);


                if (b) {
                    ((Button) v).setText("Stop timer");
                } else {
                    ((Button) v).setText("Start timer");
                    return;
                }


            }
        });


        SeekBar seekZoom = (SeekBar) findViewById(R.id.seekZoom);
        seekZoom.setProgress((int) app.pref.zoomLevel);
        final TextView tvZoom = findViewById(R.id.tvZoom);
        tvZoom.setText(app.pref.zoomLevel + "");
        seekZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {


            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                app.pref.zoomLevel = progress;
                tvZoom.setText(app.pref.zoomLevel + "");

                //openCamera();
            }
        });
    }





    /*private void openCamera() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
            return;
        }


    }*/


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                app.makeToast("need permisions");

                serviceParamsStop();
                cameraServiceStop();

                finish();
            } else {
                app.makeToast("Thanks");
                //serviceParamsStart();
                //cameraServiceStart();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }*/
        //checkPermissions();

    }

    @Override
    protected void onPause() {

        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();


    }


    public void SetCameraSettings(View v) {
        switch (v.getId()) {
            case R.id.btnCameraFront:
                app.pref.cameraId = 1;
                break;
            case R.id.btnCameraBack:
                app.pref.cameraId = 0;
                break;
            case R.id.switchFlash:
                Switch aSwitch = (Switch) v;
                app.pref.isFlash = aSwitch.isChecked();
                break;
        }


        //openCamera();
        drawer.closeDrawer(GravityCompat.START);
    }


    private boolean intArray(int[] intArray, int value) {
        for (int i = 0; i < intArray.length; i++) {
            if (intArray[i] == value) {
                return true;
            }
        }
        return false;
    }


    /*public void OnDisconectSocket(View view) {
        isSocketInProgress = false;
        if (socket != null) {
            try {
                socket.close();
                socket = null;

                Log.i("mylog", "socket=null");
            } catch (Exception e1) {
                Log.e("mylog", e1.getMessage());
            }
        }
    }*/


    public void OnChaneSettingsClick(View view) {
        switch (view.getId()) {
            case R.id.btnChangeSettings1:
                app.pref.zoomLevel = 0;
                app.pref.iso = 100;
                app.pref.exposure = 0;
                app.pref.size_x = 300;app.pref.size_y = 300;
                //app.pref.size_x = 960;app.pref.size_y = 720;

                app.pref.fps = 1;
                app.pref.rotate = 45;

                break;
            case R.id.btnChangeSettings2:
                app.pref.zoomLevel = 10;
                app.pref.iso = 10000;
                app.pref.exposure = 500000000L;
                //app.pref.size_x = 700;app.pref.size_y = 700;
                app.pref.size_x = 1920;
                app.pref.size_y = 1080;
                app.pref.fps = 10;
                app.pref.rotate = 90;
                break;
        }

        Intent i2 = new Intent(this, Camera2Service.class);
        i2.setAction(ServiceParams.COMMAND_CHANGE_SETTINGS);
        startService(i2);

        updateTvDetails();
    }

    private void updateTvDetails() {
        tvDetails.setText("zoomLevel=" + app.pref.zoomLevel + "\n");
        tvDetails.append("size_x=" + app.pref.size_x + "\n");
        tvDetails.append("size_y=" + app.pref.size_y + "\n");
        tvDetails.append("fps=" + app.pref.fps + "\n");
        tvDetails.append("rotate=" + app.pref.rotate + "\n");
        tvDetails.append("iso=" + app.pref.iso + "\n");
        tvDetails.append("exposure=" + app.pref.exposure + "\n");

    }


}
