package com.woodoo.testkiev.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.woodoo.testkiev.App;
import com.woodoo.testkiev.MainActivity;
import com.woodoo.testkiev.R;
import com.woodoo.testkiev.utils.ImageUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class Camera2Service extends Service {
    protected static final String TAG = "Camera2Service";
    protected static final int CAMERA_CALIBRATION_DELAY = 500;
    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_BACK;
    protected static long cameraCaptureStartTime;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession session;
    protected ImageReader imageReader;
    //byte[] jpegData;
    private ArrayList<byte[]> myStack;
    //private Matrix matrix;
    App app;

    private Thread socketThread;
    private boolean isStop = false;

    Socket socket = null;
    private boolean isSocketInProgress = false;
    //private boolean isCreationImage = false;
    private int NID = 1;
    public static final String SECONDARY_CHANNEL = "channel1";

    //private PowerManager.WakeLock wl;
    //private WifiManager.WifiLock wfl;

    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "CameraDevice.StateCallback onOpened");
            cameraDevice = camera;
            actOnReadyCameraDevice();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
            //cameraDevice=null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "CameraDevice.StateCallback onError " + error);
            switch (error) {
                case ERROR_CAMERA_IN_USE:
                    Log.e(TAG, "ERROR_CAMERA_IN_USE ");
                    break;
                case ERROR_MAX_CAMERAS_IN_USE:
                    Log.e(TAG, "ERROR_MAX_CAMERAS_IN_USE ");
                    break;
                case ERROR_CAMERA_DISABLED:
                    Log.e(TAG, "ERROR_CAMERA_DISABLED ");
                    restartCamera();
                    break;
                case ERROR_CAMERA_DEVICE:
                    Log.e(TAG, "ERROR_CAMERA_DEVICE ");
                    //The camera device needs to be re-opened to be used again.
                    restartCamera();
                    break;
                case ERROR_CAMERA_SERVICE:
                    Log.e(TAG, "ERROR_CAMERA_SERVICE ");
                    break;
            }
            //readyCamera();
        }
    };


    private void restartCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        /*if(session!=null){
            try {
                session.abortCaptures();
            } catch (Exception e){
                Log.e(TAG, "restartCamera " + e.getMessage());
            }
            session.close();
        }*/


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        readyCamera();
    }

    protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onReady(CameraCaptureSession session) {

            Camera2Service.this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
                cameraCaptureStartTime = System.currentTimeMillis();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                restartCamera();
            }
        }


        @Override
        public void onConfigured(CameraCaptureSession session) {
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {

        }
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
             //Log.d(TAG, "onImageAvailable "+cameraCaptureStartTime);
            //if(myStack.size()==2){return;}
            try {
                Image img = reader.acquireLatestImage();
                if (img != null && !isSocketInProgress) {
                    if (System.currentTimeMillis() > cameraCaptureStartTime + CAMERA_CALIBRATION_DELAY) {
                        processImage(img);
                        //byte[] jpegData = new byte[buffer.capacity()];
                        //buffer.get(bytes);
                        //byte[] jpegData = new byte[buffer.remaining()];

                        /*ByteBuffer buffer = img.getPlanes()[0].getBuffer();;
                        jpegData = new byte[buffer.remaining()]; // makes byte array large enough to hold image
                        buffer.get(jpegData);*/

                        byte[] bytes = ImageUtil.imageToByteArray(img);
                        if(app.pref.rotate>0){
                            //bytes = ImageUtil.imageRotate(bytes, app.pref.rotate);
                            bytes = rotateBytes(bytes);
                        }
                        //isCreationImage= true;
                        //jpegData = bytes;

                        myStack.add(0, bytes);
                        myStack.remove(2);
                        //isCreationImage= false;
                        //jpegData = ImageUtil.rotateBytes(jpegData, img.getWidth(), img.getHeight(), 180);
                        //jpegData = ImageUtil.rotateYUV420Degree90(jpegData.clone(), img.getWidth(), img.getHeight());

                    }

                } else {
                    //Log.e(TAG, "not avalible");
                }
                img.close();
            } catch (Exception e) {

            }


            /*try {
                Thread.sleep((long) (1000/app.pref.fps));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        }
    };



    public void readyCamera() {
        isSocketInProgress = false;
        myStack.clear();



        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String pickedCamera = getCamera(manager);
            manager.openCamera(pickedCamera, cameraStateCallback, null);
            //imageReader = ImageReader.newInstance(1920, 1088, ImageFormat.JPEG, 2 /* images buffered */);
            //imageReader = ImageReader.newInstance(app.pref.size_x, app.pref.size_y, ImageFormat.JPEG, 5 );
            imageReader = ImageReader.newInstance(app.pref.size_x, app.pref.size_y, ImageFormat.YUV_420_888, 5);
            //imageReader = ImageReader.newInstance(app.pref.size_x, app.pref.size_y, ImageFormat.YV12, 5);
            //imageReader = ImageReader.newInstance(app.pref.size_x, app.pref.size_y, PixelFormat.RGBA_8888, 1);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            //Log.d(TAG, "imageReader created");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public String getCamera(CameraManager manager) {
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int rotation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CAMERACHOICE) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d(TAG, "onStartCommand flags " + flags + " startId " + startId);
        if (intent != null && (intent.getAction() instanceof String)) {
            Log.i(TAG, "server onStartCommand " + intent.getAction());
            if (intent.getAction().equals(ServiceParams.COMMAND_STOP_SERVER)) {
                stopSelf();
            }

            if (intent.getAction().equals(ServiceParams.COMMAND_START)) {
                readyCamera();
                startThread();
                showNotifications();
            }

            if (intent.getAction().equals(ServiceParams.COMMAND_CHANGE_SETTINGS)) {
                restartCamera();
                /*if (cameraDevice != null) {
                    cameraDevice.close();
                    cameraDevice = null;
                }

                readyCamera();*/

            }

        }

        return START_STICKY;
        //return super.onStartCommand(intent, flags, startId);
        //return Service.START_NOT_STICKY;
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate() {
        super.onCreate();
        //Log.d(TAG,"onCreate service");
        app = (App) getApplication();

        /*try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sync_all_cpu");
            wl.acquire();
        } catch (Exception e){}


        try{
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wfl = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "sync_all_wifi");
            wfl.acquire();
        }catch (Exception e){}*/

        myStack = new ArrayList<>();
    }

    public void actOnReadyCameraDevice() {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if (session != null) {
            try {
                session.abortCaptures();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            session.close();
        }
        session = null;



        isStop = true;
        if (socketThread != null) {
            //timerThread.stop();
            socketThread.interrupt();
        }

        NotificationManager notificationmanager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationmanager.cancel(NID);

        /*try {
            wl.release();
            wfl.release();
        }catch (Exception e){

        }*/

        if (socket != null) {
            try {
                socket.close();
                socket = null;
                //isSocketInProgress = false;
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }


    }


    protected CaptureRequest createCaptureRequest() {
        //Log.d(TAG, "createCaptureRequest");
        try {
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureBuilder.addTarget(imageReader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            //captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);

            //set iso
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, app.pref.iso);


            //set zoom
            if (app.pref.zoomLevel > 0) {
                Rect zoomRect = getZoomRect(app.pref.zoomLevel);
                /*if (app.pref.zoomLevel == 9) {
                    zoomRect = new Rect(0, 0, 200, 200);
                }*/
                //zoomCropPreview = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            }

            //set exposure
            //or by just disabling auto-exposure, leaving auto-focus and auto-white-balance running:
            if (app.pref.exposure > 0) {
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_MODE_OFF);
                //captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 500000000L);//0.5s
                captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, app.pref.exposure);
            }

            //captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, app.pref.rotate);

            return captureBuilder.build();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void saveFile(byte[] bytes) {
        File file = new File(Environment.getExternalStorageDirectory() + "/image.jpg");
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(bytes);    // write the byte array to file
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processImage(Image image) {
        //Process image data
        ByteBuffer buffer;
        byte[] bytes;
        boolean success = false;
        //File file = new File(Environment.getExternalStorageDirectory() + "/Pictures/image.jpg");
        File file = new File(Environment.getExternalStorageDirectory() + "/image.jpg");
        FileOutputStream output = null;

        if (image.getFormat() == ImageFormat.JPEG) {
            buffer = image.getPlanes()[0].getBuffer();
            bytes = new byte[buffer.remaining()]; // makes byte array large enough to hold image
            buffer.get(bytes); // copies image from buffer to byte array
            try {
                output = new FileOutputStream(file);
                output.write(bytes);    // write the byte array to file
                success = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                image.close(); // close this to free up buffer for other images
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }


    }

    private void sendFileToServer(byte[] bytes) {
        DataOutputStream dataOutputStream = null;
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket();
                socket.setKeepAlive(true);
                socket.setSoTimeout(10000);
                //socket.connect(new InetSocketAddress("176.107.187.129", 1500), 5000);
                socket.connect(new InetSocketAddress(app.pref.IP, 1500), 5000);
                Log.d(TAG, "socket.connected");
            }
            /*DataInputStream in=new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            String message=in.readUTF();
            Log.d(TAG, message);*/


            //isSocketInProgress = true;

            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write(bytes);
            dataOutputStream.flush();
            //dataOutputStream.writeUTF("%%EOF%%");
            //dataOutputStream.flush();
            //Log.d(TAG, "send success " + bytes.length);
            //dataOutputStream.close();

            //BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //String message = input.readLine();
            /*InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String message = br.readLine();*/
            //Log.d(TAG, "read buffer " + message);



            //Get the return message from the server
            /*BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String stringData = input.readLine();
            Log.d(TAG, stringData);*/
            /*InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String message = br.readLine();*/
            //read the response


        } catch (Exception e) {
            e.printStackTrace();
            //Log.e(TAG, e.getMessage());
            if (socket != null) {
                try {
                    socket.close();
                    socket = null;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        } finally {

            /*if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/
        }
    }

    private Rect getZoomRect(float zoomLevel) {
        try {
            //zoomLevel=zoomLevel*10;
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            float maxZoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) * 10;
            Rect activeRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            zoomLevel = maxZoom / 100 * zoomLevel;
            if ((zoomLevel <= maxZoom) && (zoomLevel > 1)) {
                int minW = (int) (activeRect.width() / maxZoom);
                int minH = (int) (activeRect.height() / maxZoom);
                int difW = activeRect.width() - minW;
                int difH = activeRect.height() - minH;
                int cropW = difW / 100 * (int) zoomLevel;
                int cropH = difH / 100 * (int) zoomLevel;
                cropW -= cropW & 3;
                cropH -= cropH & 3;
                return new Rect(cropW, cropH, activeRect.width() - cropW, activeRect.height() - cropH);
            } else if (zoomLevel == 0) {
                return new Rect(0, 0, activeRect.width(), activeRect.height());
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error during camera init");
            return null;
        }
    }


    private void startThread() {
        isStop = false;
        if (socketThread != null) {
            try {
                socketThread.interrupt();
                socketThread = null;
            } catch (Exception e) {
            }
        }


        socketThread = new Thread(new Runnable() {
            public void run() {
                while (!isStop) {
                    //if(jpegData!=null && jpegData.length>0 && !isCreationImage){
                    if(myStack!=null && myStack.size()>1){
                        isSocketInProgress = true;
                        //sendFileToServer(jpegData);
                        //sendFileToServer(jpegData.clone());
                        //sendFileToServer(myStack.get(0));
                        sendFileToServer(getBytesFromFile());
                        isSocketInProgress = false;
                    }
                    try {
                        Thread.sleep((long) (1000 / app.pref.fps));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        socketThread.start();
    }

    private byte[] getBytesFromFile() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/not_pay.jpg");
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }


    private void showNotifications() {
        NotificationManager notificationmanager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan2 = new NotificationChannel(SECONDARY_CHANNEL, "noti_channel_second", NotificationManager.IMPORTANCE_HIGH);
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

    }

    private byte[] rotateBytes(byte[] data) {
        Matrix matrix = new Matrix();
        matrix.postRotate(app.pref.rotate);

        Bitmap storedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, null);
        storedBitmap = Bitmap.createBitmap(storedBitmap, 0, 0, storedBitmap.getWidth(), storedBitmap.getHeight(), matrix, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        storedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] byteArray = stream.toByteArray();
        storedBitmap.recycle();
        return byteArray;
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved");
        Intent intent = new Intent("restartApps");
        sendBroadcast(intent);
    }
}