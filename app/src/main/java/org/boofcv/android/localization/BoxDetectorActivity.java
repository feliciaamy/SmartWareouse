package org.boofcv.android.localization;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;

import org.boofcv.android.R;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.os.Handler;
import android.widget.ImageView;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * Created by Amy on 20/5/17.
 */

public class BoxDetectorActivity extends Activity {

    //Bluetooth Functions
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private boolean safeToTakePicture = false;

    private ImageView mImageView;
    private File lastPictureTaken;
    private Camera mCamera;
    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            lastPictureTaken = pictureFile;
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions");
                safeToTakePicture = true;
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Log.d(TAG, "Successfully save picture");
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        checkCameraHardware(this);
        setContentView(R.layout.camera_localization);

        //Bluetooth automatic connection
        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device
        new ConnectBT().execute(); //Call the class to connect

        // Create an instance of Camera
        mCamera = getCameraInstance();
        Camera.Parameters params = mCamera.getParameters();
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        } else {
            Log.e("AUTO FOCUS", "doesn't work");
        }
        mCamera.setParameters(params);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        final Button captureButton = (Button) findViewById(R.id.button_capture);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    takePicture(mPicture);
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                captureButton.performClick();
            }
        }, 3000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                close();
            }
        }, 6000);


        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        Button in = (Button) findViewById(R.id.in);
        in.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        Log.e("Max Zoom", mCamera.getParameters().getMaxZoom() + "");
                        Log.e("Focal Length", mCamera.getParameters().getFocalLength() + "");
                        Log.e("Current Zoom", mCamera.getParameters().getZoom() + "");
                        int current = mCamera.getParameters().getZoom();
                        if (current < 10) {
                            Camera.Parameters param = mCamera.getParameters();
                            param.setZoom(current + 1);
                            mCamera.setParameters(param);
                        }
                    }
                }
        );

        Button out = (Button) findViewById(R.id.out);
        out.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        Log.e("Max Zoom", mCamera.getParameters().getMaxZoom() + "");
                        Log.e("Focal Length", mCamera.getParameters().getFocalLength() + "");
                        Log.e("Current Zoom", mCamera.getParameters().getZoom() + "");
                        Log.e("Zoom Supported", mCamera.getParameters().isZoomSupported() + "");
                        int current = mCamera.getParameters().getZoom();
                        if (current > 0) {
                            Camera.Parameters param = mCamera.getParameters();
                            param.setZoom(current - 1);
                            mCamera.setParameters(param);
                        }
                    }
                }
        );
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
//            Log.e(Camera.;)
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "shelves");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void close() {
        mPreview.stopPreviewAndFreeCamera();

        // Code
        Bitmap bitmap = BitmapFactory.decodeFile(lastPictureTaken.toString());

        Mat ImageMat = new Mat();
        Bitmap myBitmap32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(myBitmap32, ImageMat);

        mPreview.stopPreviewAndFreeCamera();
        setContentView(R.layout.templatematching);

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageResource(android.R.color.transparent);

        boolean done = false;
        while (!done) {
            if (mImageView.isShown()) {
                done = true;
                // Color Detection
                ColorBlobDetector colorBlobDetector = new ColorBlobDetector(ImageMat);
                colorBlobDetector.findMarkers();
                List<Dimension> foundMarkers = colorBlobDetector.getMarkers();
                List<Dimension> boundaries = colorBlobDetector.getBoundaries();
                Dimension bottomBoundary = colorBlobDetector.getBottomBoundary();

                // Bin Label Detection
                BinLabelDetector binLabelDetector = new BinLabelDetector(ImageMat);
                binLabelDetector.findPotentialLabels();
                List<Dimension> binLabels = binLabelDetector.getEliminatedLabels(bottomBoundary);
                List<Dimension> binLabelCentroids = binLabelDetector.getEliminatedCentroids(bottomBoundary);

                // Labels Detection
                LabelDetector labelDetector = new LabelDetector(ImageMat);
                labelDetector.findLabels();
                List<Dimension> labels = labelDetector.getLabels();
                List<Dimension> boxCentroids = labelDetector.getCentroids();
                // Template Matching
//                List<Dimension> boxes = Localization.runTemplateMatching(baseImg);
//
//                for (Dimension d : boxes) {
//                    paint.setColor(d.color);
//                    cnvs.drawRect((float) d.left, (float) d.top, (float) d.right, (float) d.bottom, paint);
//                }

                // Initial Setting
                Mat temp = labelDetector.getFilteredMat();
                Bitmap resultBitmap = Bitmap.createBitmap(temp.cols(), temp.rows(), Bitmap.Config.ARGB_8888);

                Canvas cnvs = new Canvas(myBitmap32);
                cnvs.drawBitmap(myBitmap32, 0, 0, null);
                Paint paint = new Paint();

                // Drawing markers
                for (Dimension d : foundMarkers) {
                    Log.d("Dimension", d.toString());
                    paint.setColor(d.color);
                    cnvs.drawCircle((float) d.x, (float) d.y, (float) d.r, paint);
                }

                // Drawing Lines
                paint.setTextSize(62f);
                paint.setStrokeWidth(20f);
                for (Dimension d : boundaries) {
                    Log.d("Dimension", d.toString());
                    paint.setColor(d.color);
                    if (d.orientation == Orientation.HORIZONTAL) {
                        cnvs.drawLine((float) d.start, (float) d.center, (float) d.end, (float) d.center, paint);
                    } else if (d.orientation == Orientation.VERTICAL) {
                        cnvs.drawLine((float) d.center, (float) d.start, (float) d.center, (float) d.end, paint);
                    }
                }
                paint.setStyle(Paint.Style.STROKE);
                // Drawing Bin Labels
                for (Dimension d : binLabels) {
                    paint.setColor(d.color);
                    cnvs.drawRect((float) d.left, (float) d.top, (float) d.right, (float) d.bottom, paint);
                }
                // Drawing Boxes' Labels
                for (Dimension d : labels) {
                    paint.setColor(d.color);
                    cnvs.drawRect((float) d.left, (float) d.top, (float) d.right, (float) d.bottom, paint);
                }
                // Drawing centroids
                String data = "";
                paint.setStyle(Paint.Style.FILL);
                for (Dimension d : boxCentroids) {
                    paint.setColor(d.color);
                    cnvs.drawCircle((float) d.x, (float) d.y, (float) d.r, paint);
                    data = data + d.x + "," + d.y + ";";
                }

                for (Dimension d : binLabelCentroids) {
                    paint.setColor(d.color);
                    cnvs.drawCircle((float) d.x, (float) d.y, (float) d.r, paint);
                    data = data + d.x + "," + d.y + ";";
                }

                Log.d("Data", data);
                mImageView.setImageBitmap(myBitmap32);
                sendCoor(data);
            }
        }
    }

    public void takePicture(final Camera.PictureCallback callback) throws Exception {
        if (mCamera == null)
            throw new IllegalStateException("Camera unavailable!");

        // TODO lock camera here?

        // Use auto focus if the camera supports it
        String focusMode = mCamera.getParameters().getFocusMode();
        if (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focusMode.equals(Camera.Parameters.FOCUS_MODE_FIXED)) {
            if (safeToTakePicture) {
                mCamera.takePicture(null, null, callback);
                safeToTakePicture = false;
            }
        } else
            Log.d("FOCUS", "NOT IN AUTOFOCUS");
        mCamera.takePicture(null, null, callback);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    try {
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    //Bluetooth Functions
    private void Disconnect() {
        if (btSocket != null) //If the btSocket is busy
        {
            try {
                btSocket.close(); //close connection
            } catch (IOException e) {
                msg("Error");
            }
        }
        finish(); //return to the first layout

    }

    private void turnOffLed() {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write("O".toString().getBytes());
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void sendCoor(String data) {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(data.toString().getBytes());
                Log.d("debug", "message send");
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(BoxDetectorActivity.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

}
