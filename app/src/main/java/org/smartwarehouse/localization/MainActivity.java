package org.smartwarehouse.localization;

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

import org.smartwarehouse.R;
import org.smartwarehouse.scanner.BarcodeScanner;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.os.Handler;
import android.widget.ImageView;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.ContentValues.TAG;

/**
 * Created by Amy on 20/5/17.
 */

public class MainActivity extends Activity {
    // Variable
    private final int IMAGE_HEIGHT = 2916;
    private final int IMAGE_WIDTH = 5184;
    private double height = 0;

    // Reading Barcodes
    final int GET_BARCODE = 1;
    private Bin currentBin;
    private List<Bin> result = new ArrayList<>();
    private List<Bin> queue = new ArrayList<>();
    private Coordinate currentCoor;

    //Bluetooth Functions
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = Overall_Interface.myBluetooth;
    BluetoothSocket btSocket = Overall_Interface.btSocket;
    private boolean isBtConnected = Overall_Interface.isBtConnected;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Camera & File
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
        startScanning();
    }

    private void startScanning() {
        if (!isPrinterReady()) {
            Log.e("3D printer", "is not set");
        }
        checkCameraHardware(this);
        setContentView(R.layout.camera_localization);

        //Bluetooth automatic connection
//        Intent newint = getIntent();
//        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device
//        new ConnectBT().execute(); //Call the class to connect

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
        queue = processData(lastPictureTaken);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    queueing();
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                captureButton.performClick();
            }
        }, 2000);

    }

    private void queueing() {
        if (!queue.isEmpty() || currentBin != null) {
            if (currentBin == null) {
                currentBin = queue.get(0);
                queue.remove(0);
            }

            // Read bin label
            if (currentBin.getBinLabelBarcode() == null) {
                currentCoor = currentBin.getBinLabelCoordinate();
                readBarcodes();
            } else if (currentBin.hasNext()) {
                // Read Box
                currentCoor = currentBin.nextBox();
                readBarcodes();
            } else {
                // Next Bin
                result.add(currentBin);
                currentBin = null;
                queueing();
            }
        } else {
            Log.d("Read Barcode", "Finish reading");
            currentBin = null;
            currentCoor = null;

            startScanning();
        }
    }

    private boolean isPrinterReady() {
        // Zeroing 3D printer
        if (height == -1) {
            sendCoor("l0,\n");
            height = 0;
        }
        sendCoor("z," + 0 + "," + (height * 0.8) + "\n");
        if (!receiveMessage().equals("1")) {
            return false;
        }
        // Centering 3D printer
        sendCoor("c,\n");
        if (!receiveMessage().equals("1")) {
            return false;
        }
        return true;
    }

    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private void readBarcodes() {

        Log.d("Send Coordinate", "Sent " + currentCoor.toString());
        sendCoor("s," + currentCoor.x + "," + currentCoor.y + "\n");


        // Format: X,Y  |   SEND TO ARDUINO

        // WAIT UNTIL IT MOVES
        while (true) {
            Log.d("debuga", "Waiting for 3D Positioning");
//                String temp = receiveCoor();
            String temp = "hai";
            if (temp.length() > 1) {
                Log.d("debuga", "Sending Next Coordinate");
                break;
            }
        }

        // GO TO SCANDIT ACTIVITY
        Intent intent = new Intent(this, BarcodeScanner.class);
        intent.putExtra("type", currentCoor.type.toString());
        startActivityForResult(intent, GET_BARCODE);

    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == GET_BARCODE) {
            if (resultCode == RESULT_OK) {
                String barcodes = data.getStringExtra("barcodes");
                Log.d("BARCODE", barcodes);
                Barcodes barcodeList = new Barcodes(currentCoor.type);
                if (currentCoor.type == Type.BINLABEL) {
                    barcodeList.setBarcode(barcodes);
                    currentBin.setBinLabelBarcode(barcodeList);
                } else {
                    // Regex for barcode
                    String pattern = "(\\BX9\\d+)(\\B1T1E\\d+\\w+)(\\B9D\\d+)(\\BQ\\d+)(\\B1PSP\\d+)";
                    Pattern r = Pattern.compile(pattern);

                    Matcher m = r.matcher(barcodes);
                    if (m.find()) {
                        barcodeList.setX(m.group(1));
                        barcodeList.setT(m.group(2));
                        barcodeList.setD9(m.group(3));
                        barcodeList.setQ(m.group(4));
                        barcodeList.setP(m.group(5));
                    }
                    Log.d("Barcode result", barcodeList.toString());
                    currentBin.mapBoxes(currentCoor, barcodeList);
                }
                Log.d("Coordinate, barcodes", currentCoor.toString() + ", " + barcodes);

//                result.put(currentCoor, barcodeList);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            queueing();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                captureButton.performClick();
                    }
                }, 2000);

            } else {
                Log.e("SCANDIT INTENT", "NO RETURN");
            }
        }
    }

    private List<Bin> processData(File file) {
//        List<Coordinate> coordinates = new ArrayList<Coordinate>();
        List<Bin> queue = new ArrayList<>();

        // Code
        Bitmap bitmap = BitmapFactory.decodeFile(file.toString());

        Mat ImageMat = new Mat();
        Bitmap resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(resultBitmap, ImageMat);

        mPreview.stopPreviewAndFreeCamera();
        setContentView(R.layout.templatematching);

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageResource(android.R.color.transparent);

        boolean done = false;
        while (!done) {
            if (mImageView.isShown()) {
                done = true;
                // Boundaries Detection
                BoundaryDetector boundaryDetector = new BoundaryDetector(ImageMat);
                List<Dimension> foundMarkers = boundaryDetector.getMarkers();
                List<Dimension> boundaries = boundaryDetector.getBoundaries();
                List<Dimension> bottomMarkers = boundaryDetector.getBottomMarkers();
                Dimension bottomBoundary = boundaryDetector.getBottomBoundary();
                Dimension rightBoundary = boundaryDetector.getRightBoundary();
                Dimension leftBoundary = boundaryDetector.getLeftBoundary();
                Dimension topBoundary = boundaryDetector.getTopBoundary();
                if (bottomBoundary.getCenter() != -1 && topBoundary.getCenter() != -1) {
                    height = Math.abs(bottomBoundary.getCenter() - topBoundary.getCenter());
                } else {
                    height = -1;
                }

                // Bin Labels Detection
                BinLabelDetector binLabelDetector = new BinLabelDetector(ImageMat);
                List<Dimension> binLabels = binLabelDetector.getEliminatedLabels(bottomBoundary, rightBoundary, leftBoundary);
                List<Dimension> binLabelCentroids = binLabelDetector.getEliminatedCentroids(binLabels);

                // Boxes Detection
                BoxDetector boxDetector = new BoxDetector(ImageMat);
                List<Dimension> labels = boxDetector.getEliminatedBoxes(boundaries);
                List<Dimension> boxCentroids = boxDetector.getEliminatedCentroids(labels);

                // Create Queue
                queue = createQueue(bottomMarkers, binLabelCentroids, boxCentroids);

                // Initial Setting
                Canvas cnvs = new Canvas(resultBitmap);
                cnvs.drawBitmap(resultBitmap, 0, 0, null);
                Paint paintStroke = new Paint();
                Paint paintFill = new Paint();
                paintStroke.setStyle(Paint.Style.STROKE);
                paintStroke.setStrokeWidth(20f);

                // Drawing markers
                for (Dimension d : foundMarkers) {
                    Log.d("Dimension", d.toString());
                    paintFill.setColor(d.getColor());
                    cnvs.drawCircle((float) d.getX(), (float) d.getY(), (float) d.getR(), paintFill);
                }

                // Drawing Lines
                for (Dimension d : boundaries) {
                    Log.d("Dimension", d.toString());
                    paintStroke.setColor(d.getColor());
                    if (d.getOrientation() == Orientation.HORIZONTAL) {
                        cnvs.drawLine((float) d.getStart(), (float) d.getCenter(), (float) d.getEnd(), (float) d.getCenter(), paintStroke);
                    } else if (d.getOrientation() == Orientation.VERTICAL) {
                        cnvs.drawLine((float) d.getCenter(), (float) d.getStart(), (float) d.getCenter(), (float) d.getEnd(), paintStroke);
                    }
                }

                // Drawing Bin Labels
                for (Dimension d : binLabels) {
                    paintStroke.setColor(d.getColor());
                    cnvs.drawRect((float) d.getLeft(), (float) d.getTop(), (float) d.getRight(), (float) d.getBottom(), paintStroke);
                }
                // Drawing Boxes' Labels
                for (Dimension d : labels) {
                    paintStroke.setColor(d.getColor());
                    cnvs.drawRect((float) d.getLeft(), (float) d.getTop(), (float) d.getRight(), (float) d.getBottom(), paintStroke);
                }
                // Drawing centroids
                String data = "";
                for (Dimension d : boxCentroids) {
                    paintFill.setColor(d.getColor());
                    cnvs.drawCircle((float) d.getX(), (float) d.getY(), (float) d.getR(), paintFill);
                    data = data + d.getX() + "," + d.getY() + ";";
//                    coordinates.add(new Coordinate(Type.BOX, d.getX(), d.getY()));
                }

                for (Dimension d : binLabelCentroids) {
                    paintFill.setColor(d.getColor());
                    cnvs.drawCircle((float) d.getX(), (float) d.getY(), (float) d.getR(), paintFill);
                    data = data + d.getX() + "," + d.getY() + ";";
//                    coordinates.add(new Coordinate(Type.BINLABEL, d.getX(), d.getY()));
                }

                Log.d("Data", data);
                mImageView.setImageBitmap(resultBitmap);
                storeImage(resultBitmap);
            }
        }
        return queue;
    }

    private List<Bin> createQueue(List<Dimension> bottomMarkers, List<Dimension> binLabelCentroids,
                                  List<Dimension> boxCentroids) {
        List<Bin> queue = new ArrayList<>();

        for (int i = 0; i < bottomMarkers.size() - 1; i++) {
            List<Coordinate> temp = new ArrayList<>();
            if (!binLabelCentroids.isEmpty()) {
                Dimension binLabel = binLabelCentroids.get(0);
                Log.d("Queue bottom markers", bottomMarkers.get(i).toString() + "; " + bottomMarkers.get(i + 1).toString());
                Log.d("Queue Binlabel", binLabel.toString());
                if (binLabel.getX() > bottomMarkers.get(i).getX() && binLabel.getX() < bottomMarkers.get(i + 1).getX()) {
                    binLabelCentroids.remove(0);
                    while (!boxCentroids.isEmpty()) {
                        Dimension box = boxCentroids.get(0);
                        if (box.getX() > bottomMarkers.get(i).getX() && box.getX() < bottomMarkers.get(i + 1).getX()) {
                            temp.add(new Coordinate(Type.BOX, box.getX(), box.getY()));
                            Log.d("Add box", box.toString());
                            boxCentroids.remove(0);
                        } else {
                            break;
                        }
                    }
                    queue.add(new Bin(new Coordinate(Type.BINLABEL, binLabel.getX(), binLabel.getY()), temp));
                }
            } else {
                break;
            }
        }

        Log.d("Queue", "Size: " + queue.size());
        for (Bin bin : queue) {
            Log.d("QUEUE", bin.toString());
        }
        return queue;
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

    private void sendCoor(String data) {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(data.toString().getBytes());
                Log.d("sendCoor", data);
            } catch (IOException e) {
                msg("Error");
            }
        }
    }


    private String receiveMessage() {
        byte[] buffer = new byte[1024];
        int bytes;
        String readMessage = "";

        if (btSocket != null) {
            try {
                InputStream inFromServer = btSocket.getInputStream();
                bytes = inFromServer.read(buffer);
                readMessage = new String(buffer, 0, bytes);
                Log.d("Bluetooth", readMessage);
            } catch (IOException e) {
                msg("Error");
            }
        }
        return readMessage;
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
            progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait!!!");  //show a progress dialog
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

    private void saveBitmap(Bitmap bitmap) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "result");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(mediaStorageDir.getPath() + File.separator + lastPictureTaken.getName()));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

}