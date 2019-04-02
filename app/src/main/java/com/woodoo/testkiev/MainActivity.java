package com.woodoo.testkiev;

import android.Manifest;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.io.DataOutputStream;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.woodoo.testkiev.services.Camera2Service;
import com.woodoo.testkiev.services.ServiceParams;
import com.woodoo.testkiev.services.SocketExampleThread;
import com.woodoo.testkiev.utils.ImageUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ParentActivity /*implements TextureView.SurfaceTextureListener*/ {
    public static final String TAG = "mylog";

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private TextView tvDetails;
    private DrawerLayout drawer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initMain();

        if(checkPermissions()){
            serviceParamsStart();
            cameraServiceStart();
        }
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

    private void serviceParamsStart() {
        Intent i = new Intent(this, ServiceParams.class);
        i.setAction(ServiceParams.COMMAND_START);
        startService(i);
    }

    private void serviceParamsStop() {
        Intent i = new Intent(this, ServiceParams.class);
        i.setAction(ServiceParams.COMMAND_STOP_SERVER);
        startService(i);
    }

    @Override
    public void anonceFromSevice(int action) {
        /*switch (action) {
            case ACTION_NEW_SETTINGS:
                closeCamera();
                openCamera();
                break;
            *//*case ACTION_NEW_SETTINGS: break;*//*

        }*/

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

                Intent i = new Intent(this, ServiceParams.class);
                i.setAction(ServiceParams.COMMAND_STOP_SERVER);
                startService(i);

                Intent i2 = new Intent(this, Camera2Service.class);
                i2.setAction(ServiceParams.COMMAND_STOP_SERVER);
                startService(i2);

                finish();
            }else{
                serviceParamsStart();
                cameraServiceStart();
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
        Log.d("mylog", "OnChaneSettingsClick");
        switch (view.getId()) {
            case R.id.btnChangeSettings1:
                app.pref.zoomLevel = 0;
                app.pref.iso = 100;
                app.pref.exposure = 0;
                app.pref.size_x = 640;
                app.pref.size_y = 480;
                app.pref.fps = 1;
                break;
            case R.id.btnChangeSettings2:
                app.pref.zoomLevel = 10;
                app.pref.iso = 10000;
                app.pref.exposure = 500000000L;
                app.pref.size_x = 300;
                app.pref.size_y = 300;
                app.pref.fps = 10;
                break;
        }
        /*Intent i = new Intent(this, Camera2Service.class);
        i.setAction(ServiceParams.COMMAND_STOP_SERVER);
        startService(i);
*/
        Intent i2 = new Intent(this, Camera2Service.class);
        i2.setAction(ServiceParams.COMMAND_CHANGE_SETTINGS);
        startService(i2);
    }

    public void OnStartPref(View view) {
        serviceParamsStart();
    }
}
