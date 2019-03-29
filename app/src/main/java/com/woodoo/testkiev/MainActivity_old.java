package com.woodoo.testkiev;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity_old extends ParentActivity /*implements TextureView.SurfaceTextureListener*/  {
    private TextureView textureView;
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private String filePath;
    private TextView tvDetails;
    private DrawerLayout drawer;

    private Timer mTimer;

    private int iterationTime = 4000;
    Socket socket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initMain();
    }

    @Override
    public void anonceFromSevice(int action) {

    }


    private void initMain() {
        app.overrideFonts(findViewById(R.id.root));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        filePath = Environment.getExternalStorageDirectory() + "/photo_to_send.jpg";
        textureView = (TextureView) findViewById(R.id.texture);
        textureView.setSurfaceTextureListener(textureListener);

        findViewById(R.id.btnMakePhoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePictureAndSend();
            }
        });

        findViewById(R.id.btnJustSocket).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SocketExampleThread socketExampleThread = new SocketExampleThread();
                socketExampleThread.start();
            }
        });

        ((Switch) findViewById(R.id.switchFlash)).setChecked(app.pref.isFlash);
        tvDetails = findViewById(R.id.tvDetails);


        Button btnTimer = findViewById(R.id.btnTimer);
        btnTimer.setTag(false);
        btnTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean b = !(boolean) v.getTag();
                v.setTag(b);
                if (mTimer != null) {
                    mTimer.cancel();
                }

                if(b){
                    ((Button)v).setText("Stop timer");
                }else{
                    ((Button)v).setText("Start timer");
                    return;
                }


                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.i("mylog", "mTimer run");
                        takePictureAndSend();
                        Log.i("mylog", "mTimer end");
                    }
                }, 0, iterationTime);
            }
        });


        SeekBar seekZoom = (SeekBar) findViewById(R.id.seekZoom);
        //seekZoom.setProgress(app.pref.zoomLevel);
        final TextView tvZoom = findViewById(R.id.tvZoom);
        tvZoom.setText(app.pref.zoomLevel+"");
        seekZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {


            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                app.pref.zoomLevel= progress;
                tvZoom.setText(app.pref.zoomLevel+"");
                closeCamera();
                openCamera();
            }
        });
    }


    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };


    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void takePictureAndSend() {
        if (null == cameraDevice) {
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            /*CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }*/
            int width = 640;
            int height = 480;
            /*if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }*/
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
            final File file = new File(filePath);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    //app.makeToast("Saved2:" + filePath);
                    //new FileToServer(filePath);
                    Log.d("mylog", "onCaptureCompleted");
                    sendFileToServer(filePath);
                    createCameraPreview();
                }
            };


            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        Log.d("mylog", "onConfigured");
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("mylog", e.getMessage());
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.d("mylog", "onConfigureFailed");
                }
            }, mBackgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("mylog", e.getMessage());
        }
    }



    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            if (app.pref.isFlash) {
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            } else {
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }

            if(app.pref.zoomLevel>0){
                Rect zoomRect = getZoomRect(app.pref.zoomLevel);
                if(app.pref.zoomLevel==9){
                    zoomRect = new Rect(0, 0, 200, 200);
                }
                //zoomCropPreview = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
                captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
                try {
                    tvDetails.append(zoomRect.width()+" / "+zoomRect.height());
                }catch (Exception e){}
            }




            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    app.makeToast("Configuration change");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[app.pref.cameraId];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            float maxZoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM));
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity_old.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);

            tvDetails.setText("camera id " + cameraId + "\n");
            tvDetails.append(imageDimension + "\n");
            tvDetails.append(maxZoom + "\n");

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    protected void updatePreview() {
        if (cameraDevice==null) {
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("mylog", e.getMessage());
        }
    }

    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                app.makeToast("need permisions");
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }

    }

    @Override
    protected void onPause() {
        //closeCamera();
        stopBackgroundThread();
        if (mTimer != null) {
            mTimer.cancel();
        }

        app.pref.save();
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        closeCamera();
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

        closeCamera();
        openCamera();
        //drawer.closeDrawer(GravityCompat.START);
    }


    private Rect getZoomRect(float zoomLevel) {
        try {
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

    private void sendFileToServer(String filePath) {
        DataOutputStream dataOutputStream = null;
        File file = new File(filePath);
        try {
            if(socket==null || socket.isClosed()){
                //socket = new Socket("176.107.187.129", 1502);
                socket = new Socket();
                socket.setKeepAlive(true);
                socket.connect(new InetSocketAddress("176.107.187.129", 1500), 5000);
                Log.d("mylog", "socket.connected");
            }
            /*DataInputStream in=new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            String message=in.readUTF();
            Log.d("mylog", message);*/

            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            FileInputStream fileInputStream = new FileInputStream(file);

            byte[] buffer = new byte[4096];

            while (fileInputStream.read(buffer) > 0) {
                dataOutputStream.write(buffer);
            }
            fileInputStream.close();
            dataOutputStream.flush();
            dataOutputStream.writeUTF("EOF");
            dataOutputStream.flush();
            Log.d("mylog", "send successs");

            //Get the return message from the server
            /*BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String stringData = input.readLine();
            Log.d("mylog", stringData);*/
            /*InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String message = br.readLine();*/
            //read the response


        } catch (IOException e) {
            //e.printStackTrace();
            Log.e("mylog", e.toString());
            if (socket != null) {
                try {
                    socket.close();
                    socket=null;
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




}
