package com.woodoo.testkiev.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
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
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.woodoo.testkiev.App;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Camera2Service extends Service
{
    protected static final int CAMERA_CALIBRATION_DELAY = 500;
    protected static final String TAG = "mylog";
    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_BACK;
    protected static long cameraCaptureStartTime;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession session;
    protected ImageReader imageReader;
    byte[] jpegData;
    App app;

    Socket socket = null;
    private boolean isSocketInProgress = false;

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
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "CameraDevice.StateCallback onError " + error);
            //readyCamera();
        }
    };

    protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onReady(CameraCaptureSession session) {
            Camera2Service.this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
                cameraCaptureStartTime = System.currentTimeMillis ();
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
            }
        }


        @Override
        public void onConfigured(CameraCaptureSession session) {

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable "+cameraCaptureStartTime);
            Image img = reader.acquireLatestImage();
            if (img != null) {
                if (System.currentTimeMillis () > cameraCaptureStartTime + CAMERA_CALIBRATION_DELAY) {
                    //processImage(img);
                    //
                    //byte[] jpegData = new byte[buffer.capacity()];
                    //buffer.get(bytes);
                    //byte[] jpegData = new byte[buffer.remaining()];
                    ByteBuffer buffer = img.getPlanes()[0].getBuffer();;
                    jpegData = new byte[buffer.remaining()]; // makes byte array large enough to hold image
                    buffer.get(jpegData);
                    if(!isSocketInProgress){
                        Thread t = new Thread(new Runnable() {
                            public void run() {
                                sendFileToServer(jpegData);
                            }
                        });
                        t.start();
                    }
                }
                img.close();
            }
        }
    };

    public void readyCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String pickedCamera = getCamera(manager);

            manager.openCamera(pickedCamera, cameraStateCallback, null);
            //imageReader = ImageReader.newInstance(1920, 1088, ImageFormat.JPEG, 2 /* images buffered */);
            imageReader = ImageReader.newInstance(app.pref.size_x, app.pref.size_y, ImageFormat.JPEG, 2 /* images buffered */);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            Log.d(TAG, "imageReader created");
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    public String getCamera(CameraManager manager){
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CAMERACHOICE) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand flags " + flags + " startId " + startId);

        readyCamera();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"onCreate service");
        super.onCreate();
        app = (App) getApplication();
    }

    public void actOnReadyCameraDevice()
    {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        try {
            session.abortCaptures();
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
        session.close();
    }




    protected CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureBuilder.addTarget(imageReader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //set iso
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, app.pref.iso);


            //set zoom
            if (app.pref.zoomLevel > 0) {
                Rect zoomRect = getZoomRect(app.pref.zoomLevel);
                if (app.pref.zoomLevel == 9) {
                    zoomRect = new Rect(0, 0, 200, 200);
                }
                //zoomCropPreview = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            }

            //set exposure
            //or by just disabling auto-exposure, leaving auto-focus and auto-white-balance running:
            if(app.pref.exposure!=0){
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_MODE_OFF);
                //captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 500000000L);//0.5s
                captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, app.pref.exposure);
            }

            return captureBuilder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void processImage(Image image){
        //Process image data
        ByteBuffer buffer;
        byte[] bytes;
        boolean success = false;
        //File file = new File(Environment.getExternalStorageDirectory() + "/Pictures/image.jpg");
        File file = new File(Environment.getExternalStorageDirectory() + "/image.jpg");
        FileOutputStream output = null;

        if(image.getFormat() == ImageFormat.JPEG) {
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
                //socket = new Socket("176.107.187.129", 1502);
                socket = new Socket();
                socket.setKeepAlive(true);
                socket.connect(new InetSocketAddress("176.107.187.129", 1500), 5000);
                Log.d("mylog", "socket.connected");
                isSocketInProgress = false;
            }
            /*DataInputStream in=new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            String message=in.readUTF();
            Log.d("mylog", message);*/


            isSocketInProgress = true;
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write(bytes);
            dataOutputStream.flush();
            dataOutputStream.writeUTF("EOF");
            dataOutputStream.flush();
            Log.d("mylog", "send success " + bytes.length);
            isSocketInProgress = false;

            //Get the return message from the server
            /*BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String stringData = input.readLine();
            Log.d("mylog", stringData);*/
            /*InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String message = br.readLine();*/
            //read the response


        } catch (Exception e) {
            e.printStackTrace();
            //Log.e("mylog", e.getMessage());
            if (socket != null) {
                try {
                    socket.close();
                    socket = null;
                    isSocketInProgress = false;
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
            zoomLevel=zoomLevel*5;
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            float maxZoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) * 10;
            Rect activeRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
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
            Log.e("mylog", "Error during camera init");
            return null;
        }
    }
}