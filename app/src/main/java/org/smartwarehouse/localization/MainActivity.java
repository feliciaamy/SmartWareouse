package org.smartwarehouse.localization;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.widget.FrameLayout;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.features2d.Feature2D;
import org.opencv.imgproc.Imgproc;
import org.smartwarehouse.R;
import org.smartwarehouse.object.*;
import org.smartwarehouse.scanner.BarcodeScanner;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.R.attr.bitmap;
import static android.content.ContentValues.TAG;

/**
 * <h1>Smart Warehouse</h1>
 * This program is to support the scanning and controlling
 * function of our inventory count device.
 * The code will start with connecting the phone with the bluetooth
 * of the device. After the connection is settled, a picture of the
 * shelf will be taken and processed to attain all the bin labels and boxes
 * in the shelf. With the bin labels and boxes coordinate, the phone will
 * send a command (in the form of coordinate) to the device, so that the
 * dual axis can move to the expected location that allows the phone
 * to scan the label accordingly.
 * <p>
 *
 * @author Felicia Amy
 * @author Ryan Lim
 * @version 1.0
 * @since 2017-05-31
 */

public class MainActivity extends Activity {
    // Output
    private List<Bin> aisleList = new ArrayList<>();
    private String errorLog = "";
    private String aisle = "D";
    private int startBin = -1;
    private int endBin = -1;
    private int level = 1;

    // Variable
    private final int IMAGE_HEIGHT = 2916;
    private final int IMAGE_WIDTH = 5184;
    private double height = 0;//2103;
    private double baseHeight; // The shelf base in y axis
    private double avgLengthBox = 0;
    private double avgHeightBox = 0;

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

    /**
     * The main method of this activity that start the scanning
     * process by taking picture of the shelf first.
     *
     * @return Nothing.
     */
    private void startScanning() {
        if (!isMachineReady()) {
            Log.e("Machine", "is not set");
            return;
        } else {
            Log.d("Machine", "is ready");
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
        params.setZoom(0);
        Log.d("ZOOM", params.getZoom() + "");
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
        Log.d("Time stamp", "Finish taking image");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                close();
            }
        }, 5000);
        Log.d("Time stamp", "Finish closing camera");
    }

    /**
     * Check if this device has a camera, return true if there is
     * and false otherwise
     *
     * @param context
     * @return boolean
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
     *
     * @return Camera
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
     * Create a File for saving an image or video.
     * The file is created in the internal storage /Picture/shelves.
     *
     * @return File
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

    /**
     * Safely close the camera.
     */
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
            }
        }, 2000);
    }

    /**
     * Detect the boxes, bin labels, markers, and boundaries
     * in the image from the file and return a list of clustered bins.
     * It will also draw the detected objects in the canvas that will
     * be set in the ImageView after it finishes processing the image.
     *
     * @param file
     * @return List<Bin>
     * @see Bin
     * @see BoundaryDetector
     * @see BoxDetector
     * @see BinLabelDetector
     * @see ImageView
     */
    private List<Bin> processData(File file) {
//        List<Coordinate> coordinates = new ArrayList<Coordinate>();
        List<Bin> queue = new ArrayList<>();

        // Code
        Bitmap bitmap = BitmapFactory.decodeFile(file.toString());

        Mat ImageMat = new Mat();
        Bitmap resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(resultBitmap, ImageMat);

//        ImageMat = getUndistortedImage(ImageMat);

        mPreview.stopPreviewAndFreeCamera();
        setContentView(R.layout.templatematching);

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageResource(android.R.color.transparent);

        boolean done = false;
        while (!done) {
            if (mImageView.isShown()) {
                done = true;
                // Boundary Detection
                BoundaryDetector boundaryDetector = new BoundaryDetector(ImageMat);
                List<Centroid> foundMarkers = boundaryDetector.getMarkers();
                List<Boundary> boundaries = boundaryDetector.getBoundaries();
                List<Centroid> bottomMarkers = boundaryDetector.getBottomMarkers();
                Boundary bottomBoundary = boundaryDetector.getBottomBoundary();
                Boundary rightBoundary = boundaryDetector.getRightBoundary();
                Boundary leftBoundary = boundaryDetector.getLeftBoundary();
                Boundary topBoundary = boundaryDetector.getTopBoundary();
                if (bottomBoundary.getCenter() != -1 && topBoundary.getCenter() != -1) {
                    height = Math.abs(bottomBoundary.getCenter() - topBoundary.getCenter());
                } else {
                    height = -1;
                }
                baseHeight = bottomBoundary.getCenter();

                Log.d("Time stamp", "Finish detecting boundaries");

                // Bin Labels Detection
                BinLabelDetector binLabelDetector = new BinLabelDetector(ImageMat);
                List<Label> binLabels = binLabelDetector.getEliminatedLabels(bottomBoundary, rightBoundary, leftBoundary);
                List<Centroid> binLabelCentroids = binLabelDetector.getEliminatedCentroids(binLabels);
                Log.d("Time stamp", "Finish detecting labels");

                // Boxes Detection
                BoxDetector boxDetector = new BoxDetector(ImageMat, false);
//                List<Label> labels = boxDetector.getBoxes();
                List<Label> labels = boxDetector.getEliminatedBoxes(topBoundary, bottomBoundary, rightBoundary, leftBoundary);
                List<Centroid> boxCentroids = boxDetector.getEliminatedCentroids(labels);
                avgHeightBox = boxDetector.getAvgEliminatedHeight();
                avgLengthBox = boxDetector.getAvgEliminatedLength();
                Log.d("Centroid", boxCentroids.size() + "");
                Log.d("Time stamp", "Finish detecting boxes");

                // Initial Setting
                Canvas cnvs = new Canvas(resultBitmap);
                cnvs.drawBitmap(resultBitmap, 0, 0, null);
                Paint paintStroke = new Paint();
                Paint paintFill = new Paint();
                paintStroke.setStyle(Paint.Style.STROKE);
                paintStroke.setStrokeWidth(20f);

                // Drawing markers
                for (Centroid d : foundMarkers) {
                    Log.d("Dimension", d.toString());
                    paintFill.setColor(d.getColor());
                    cnvs.drawCircle((float) d.getX(), (float) d.getY(), (float) d.getR(), paintFill);
                }

                // Drawing Lines
                for (Boundary d : boundaries) {
                    Log.d("Dimension", d.toString());
                    paintStroke.setColor(d.getColor());
                    if (d.getOrientation() == Orientation.HORIZONTAL) {
                        cnvs.drawLine((float) d.getStart(), (float) d.getCenter(), (float) d.getEnd(), (float) d.getCenter(), paintStroke);
                    } else if (d.getOrientation() == Orientation.VERTICAL) {
                        cnvs.drawLine((float) d.getCenter(), (float) d.getStart(), (float) d.getCenter(), (float) d.getEnd(), paintStroke);
                    }
                }

                // Drawing Bin Labels
                for (Label d : binLabels) {
                    paintStroke.setColor(d.getColor());
                    cnvs.drawRect((float) d.getLeft(), (float) d.getTop(), (float) d.getRight(), (float) d.getBottom(), paintStroke);
                }
                // Drawing Boxes' Labels
                for (Label d : labels) {
                    paintStroke.setColor(d.getColor());
                    cnvs.drawRect((float) d.getLeft(), (float) d.getTop(), (float) d.getRight(), (float) d.getBottom(), paintStroke);
                }
                // Drawing centroids
                String data = "";
                for (Centroid d : boxCentroids) {
                    paintFill.setColor(d.getColor());
                    cnvs.drawCircle((float) d.getX(), (float) d.getY(), (float) d.getR(), paintFill);
                    data = data + d.getX() + "," + d.getY() + ";";
//                    coordinates.add(new Coordinate(Type.BOX, d.getX(), d.getY()));
                }

                for (Centroid d : binLabelCentroids) {
                    paintFill.setColor(d.getColor());
                    cnvs.drawCircle((float) d.getX(), (float) d.getY(), (float) d.getR(), paintFill);
                    data = data + d.getX() + "," + d.getY() + ";";
//                    coordinates.add(new Coordinate(Type.BINLABEL, d.getX(), d.getY()));
                }

                cnvs.drawCircle((float) 5312 / 2, (float) 2988 / 2, (float) 20, paintFill);

//                for(Centroid i : boxCentroids)
//                    Log.d("BOXES", (i.getX() + "," + i.getY()));
//
//                for(Centroid i : binLabelCentroids)
//                    Log.d("BIN", (i.getX() + "," + i.getY()));

                Log.d("Time stamp", "Finish drawing");
                Log.d("Data", data);
                mImageView.setImageBitmap(resultBitmap);
                Log.d("Time stamp", "Finish setting image view");
//                storeImage(resultBitmap);

                // Create Queue
                queue = createQueue(bottomMarkers, binLabelCentroids, boxCentroids);
                Log.d("Time stamp", "Finish creating queue");
            }
        }
        return queue;
    }

    private Mat getUndistortedImage(Mat ImageMat) {

        Mat mCameraMatrix = new Mat();
        Mat mDistortionCoefficients = new Mat();
        Mat undistorted = new Mat();
        Mat newCameraMatrix = new Mat();

        int CAMERA_MATRIX_ROWS = 3;
        int CAMERA_MATRIX_COLS = 3;
        int DISTORTION_COEFFICIENTS_SIZE = 5;
        double[] cameraMatrixArray = new double[]{4.04113950e+04, 0.00000000e+00, 2.65743957e+03, 0.00000000e+00, 4.22524546e+04, 1.49933395e+03, 0.00000000e+00, 0.00000000e+00, 1.00000000e+00};
        mCameraMatrix.put(0, 0, cameraMatrixArray);
        double[] distortionMatrixArray = new double[]{2.96857686e+01, 1.47129153e-01, 1.66132097e-01, 6.81668569e-02, 1.02986663e-04};
        mDistortionCoefficients.put(0, 0, distortionMatrixArray);
        newCameraMatrix = Calib3d.getOptimalNewCameraMatrix(mCameraMatrix, mDistortionCoefficients, ImageMat.size(), 1);
        Imgproc.undistort(ImageMat, undistorted, mCameraMatrix, mDistortionCoefficients, newCameraMatrix);

        return undistorted;
    }

    /**
     * Given the markers, bin labels, and boxes, this method will
     * create a queue of bins where a bin is defined when there are
     * two markers and a bin label between them, and the boxes on top
     * of that region are belong to that bin.
     *
     * @param bottomMarkers     The bottom most set of markers.
     * @param binLabelCentroids The centroid of bin labels that are in line with the bottom boundary.
     * @param boxCentroids      The boxes found within the boundaries (left, right, top, bottom).
     * @return List<Bin>
     */
    private List<Bin> createQueue(List<Centroid> bottomMarkers, List<Centroid> binLabelCentroids,
                                  List<Centroid> boxCentroids) {
        List<Bin> queue = new ArrayList<>();

        for (int i = 0; i < bottomMarkers.size() - 1; i++) {
            double binWidth;
            List<Coordinate> boxesTemp = new ArrayList<>();
            Centroid binLabel = new Centroid(-1, -1, 0, Color.BLACK);
            if (!binLabelCentroids.isEmpty()) {
                binLabel = binLabelCentroids.get(0);
            }
            binWidth = Math.abs(bottomMarkers.get(i).getX() - bottomMarkers.get(i + 1).getX());
            Log.d("Queue bottom markers", i + " and " + i + 1);
            Log.d("Queue Binlabel", binLabel.toString());
            Coordinate binCoor;
            if (binLabel.getX() > bottomMarkers.get(i).getX() && binLabel.getX() < bottomMarkers.get(i + 1).getX()) {
                binLabelCentroids.remove(0);
                binCoor = new Coordinate(Type.BINLABEL, binLabel.getX(), binLabel.getY());
            } else {
                //Todo this won't work because there is no currentBin yet
                Log.e("Queue", "OMG");
                binCoor = null;
            }
            while (!boxCentroids.isEmpty()) {
                Centroid box = boxCentroids.get(0);
                if (box.getX() > bottomMarkers.get(i).getX() && box.getX() < bottomMarkers.get(i + 1).getX()) {
                    boxesTemp.add(new Coordinate(Type.BOX, box.getX(), box.getY()));
                    Log.d("Add box", box.toString());
                    boxCentroids.remove(0);
                } else if (box.getX() < bottomMarkers.get(i).getX()) {
                    boxCentroids.remove(0);
                } else {
                    break;
                }
            }

            Bin tempBin = new Bin(binCoor, boxesTemp, binWidth, height, level);
            if (binCoor == null) {
                Date date = new Date();
                String errorMsg = String.format("[WARNING] Missing bin label at aisle %s level %s.", aisle, level);
                errorLog = errorLog + date + ": " + errorMsg;
                Toast.makeText(this, "Missing bin label", Toast.LENGTH_LONG).show();
                tempBin.addError(errorMsg);
            }
            if ((binCoor != null && !boxesTemp.isEmpty()) || binCoor != null) {
                queue.add(tempBin);
            }
        }


        Log.d("Queue", "Size: " + queue.size());
        for (Bin bin : queue) {
            Log.d("QUEUE", bin.toString());
        }
        return queue;
    }

    /**
     * A method to arrange the order of labels to be scanned.
     */
    private void queueing() {
        if (!queue.isEmpty() || currentBin != null) {
            if (currentBin == null) {
                currentBin = queue.get(0);
                queue.remove(0);
            }

            // Read bin label
            if (currentBin.getBinLabelBarcode() == null && currentBin.getBinLabelCoordinate() != null) {
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
//            Log.d("Output", toJson());

            // Resetting
            startBin = -1;
            endBin = -1;
            aisle = null;
            currentBin = null;
            currentCoor = null;
            startScanning();
            level++;
        }
    }

    /**
     * This method will return true if the dual axis is
     * in the correct height and centered.
     *
     * @return boolean
     */
    private boolean isMachineReady() {
        // Zeroing 3D printer
        if (height == 0) {
            sendData("m0,\n");
            while (!receiveMessage().equals("f")) {
                // Wait until the cart find the stop marker
            }
        }
        if (height == -1) {
            // Move cart
            // Move the le down to 0
            sendData("l0,\n");
            // for now give output
            Log.d("Output", "End of aisle");
            String json = toJson();
            Log.d("Output", json);
            String html = toHtml();
            Log.d("Output", html);
            height = 0;
            startScanning();
            return false;
        } else {
            sendData(String.format("le,%s\n", height / 71.5));
            while (!receiveMessage().equals("4")) {
            }

            Log.d("Bluetooth", "LE moved to " + height / 71.5);

        }
        if (height == 0) {
            sendData(String.format("z,%s,%s\n", 0, 0));
            while (!receiveMessage().equals("1")) {
//                return false;
            }
        }

//         Centering 3D printer
        sendData("c,\n");
        while (!receiveMessage().equals("1")) {
//            return false;
        }
        //todo probably need to replace this
        if (height == -1) {
            height = 0;
        }
        return true;
    }

    /**
     * Send the next coordinate in the queue to the dual axis and
     * start the BarcodeScanner activity to get the result.
     *
     * @see BarcodeScanner
     */
    private void readBarcodes() {
        if (currentCoor == null) {
            queueing();
        } else {
            Log.d("Send Coordinate", "Sent " + currentCoor.toString());
            if (currentCoor.type == Type.BOX) {
//            sendData("s," + currentCoor.x + "," + currentCoor.y + "\n");
                sendData("s," + (currentCoor.x + avgLengthBox / 2 + 125) + "," + (currentCoor.y + avgHeightBox / 2 - 125) + "\n");
            } else {
                sendData("s," + currentCoor.x + "," + currentCoor.y + "\n");
            }

            // Wait until the dual axis finish positioning
            while (!receiveMessage().equals("2")) {
            }

            // GO TO SCANDIT ACTIVITY
            Intent intent = new Intent(this, BarcodeScanner.class);
            intent.putExtra("type", currentCoor.type.toString());

            startActivityForResult(intent, GET_BARCODE);
        }
    }

    /**
     * Process the result(barcode) from BarcodeScanner activity
     * and store the barcode in the correct Bin.
     *
     * @see Bin
     * @see Activity
     */
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        Barcodes barcodeList = new Barcodes(currentCoor.type);
        if (requestCode == GET_BARCODE) {
            if (resultCode == RESULT_OK) {
                String barcodes = data.getStringExtra("barcodes");
                Log.d("BARCODE", barcodes);
                if (currentCoor.type == Type.BINLABEL) {
                    Date date = new Date();
                    String pattern = "^([A-Z])\\w\\d+\\-(\\d+)\\-(\\d+)";
                    Pattern r = Pattern.compile(pattern);

                    Matcher m = r.matcher(barcodes);
                    if (m.find()) {
                        Log.d("Barcodes", m.group(1));
                        if (aisle == null) {
                            aisle = m.group(1);
                        } else if (!m.group(1).equals(aisle)) {
                            String errorMsg = String.format("[WARNING] The aisle in the bin label is inconsistent " +
                                    "with the aisle name at aisle %s level %s.", aisle, level);
                            errorLog = errorLog + date + ": " + errorMsg;
                            Toast.makeText(this, "Wrong aisle name", Toast.LENGTH_LONG).show();
                            currentBin.addError(errorMsg);
                        }

                        if (level == -1) {
                            level = Integer.parseInt(m.group(2));
                        } else if (Integer.parseInt(m.group(2)) != level) {
                            String errorMsg = String.format("[WARNING] The level in the bin label is inconsistent " +
                                    "with the current level at aisle %s level %s.", aisle, level);
                            errorLog = errorLog + date + ": " + errorMsg;
                            Toast.makeText(this, "Wrong level", Toast.LENGTH_LONG).show();
                            currentBin.addError(errorMsg);
                        }

                        if (startBin == -1) {
                            startBin = Integer.parseInt(m.group(3));
                            endBin = startBin;
                        } else if (Integer.parseInt(m.group(2)) != endBin + 1) {
                            String errorMsg = String.format("[WARNING] The bin number in the bin label is not in order " +
                                    "at aisle %s level %s.", aisle, level);
                            errorLog = errorLog + date + ": " + errorMsg;
                            Toast.makeText(this, "Wrong bin order", Toast.LENGTH_LONG).show();
                            currentBin.addError(errorMsg);
                            endBin++;
                        } else {
                            endBin++;
                        }
                    }

                    barcodeList.setBarcode(barcodes);
                    currentBin.setBinLabelBarcode(barcodeList);
                } else {
                    // Regex for barcode
                    try {
                        byte[] b = barcodes.getBytes("UTF-8");
                        barcodes = new String(b, "US-ASCII");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    Log.d("BARCODE", barcodes);
                    barcodes = barcodes.replaceAll("[^\\x0A\\x0D\\x20-\\x7E]", "");

                    String pattern = "(\\BX9\\d+)(\\B1T1E\\d+\\w+)(\\B9D\\d+)(\\BQ\\d+)(\\B1PSP\\d+)";
                    Pattern r = Pattern.compile(pattern);

                    Matcher m = r.matcher(barcodes);
                    if (m.find()) {
                        barcodeList.setX(m.group(1));
                        barcodeList.setT(m.group(2));
                        barcodeList.setD9(m.group(3));
                        barcodeList.setQ(m.group(4));
                        barcodeList.setP(m.group(5));
                    } else {
                        barcodeList.setX(barcodes.substring(5, 14));
                        barcodeList.setT(barcodes.substring(14, 27));
                        barcodeList.setD9(barcodes.substring(27, 33));
                        barcodeList.setQ(barcodes.substring(33, 37));
                        barcodeList.setP(barcodes.substring(37, 50));
                    }
                    Log.d("Barcode result", barcodeList.toString());
//                    currentBin.mapBoxes(currentCoor, barcodeList);
                }
                Log.d("Coordinate, barcodes", currentCoor.toString() + ", " + barcodes);

                // Finish shaking
                while (!receiveMessage().equals("3")) {
                }
            } else {
                Log.e("SCANDIT INTENT", "NO RETURN");
                Date date = new Date();
                String errorMsg = String.format("[ERROR] Cannot read the label " +
                        "at aisle %s level %s.", aisle, level);
                errorLog = errorLog + date + ": " + errorMsg;
                Toast.makeText(this, "Cannot read barcode", Toast.LENGTH_LONG).show();
                currentBin.addError(errorMsg);
            }
            if (currentCoor.type == Type.BINLABEL) {
                currentBin.setBinLabelBarcode(barcodeList);
            } else {
                currentBin.mapBoxes(currentCoor, barcodeList);
            }
            queueing();
        } else {
            Log.e("VERY WRONG HERE", "AFKAJDFJA ;DJKF ;OIAJSDO ;FJA;ODIJF A");
        }
    }

    /**
     * Take picture.
     *
     * @param callback Picture Callback
     * @throws IllegalStateException On camera unavailable.
     * @see Camera
     */
    public void takePicture(final Camera.PictureCallback callback) {
        if (mCamera == null)
            throw new IllegalStateException("Camera unavailable!");

        // Use auto focus if the camera supports it
        String focusMode = mCamera.getParameters().getFocusMode();
        if (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focusMode.equals(Camera.Parameters.FOCUS_MODE_FIXED)) {
            if (safeToTakePicture) {
                mCamera.takePicture(null, null, callback);
                safeToTakePicture = false;
            }
        } else
            Log.e("FOCUS", "NOT IN AUTOFOCUS");
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

    /**
     * Disconnect bluetooth from the device.
     */
    private void Disconnect() {
        if (btSocket != null) //If the btSocket is busy
        {
            try {
                btSocket.close(); //close connection
            } catch (IOException e) {
                toast("Error");
            }
        }
        finish(); //return to the first layout

    }

    /**
     * Send data via bluetooth
     *
     * @throws IOException On socket failed
     */
    private void sendData(String data) {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(data.toString().getBytes());
                Log.d("sendData", data);
            } catch (IOException e) {
                toast("Error");
            }
        }
    }

    /**
     * Receive message from bluetooth
     *
     * @throws IOException On socket failed.
     */
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
                toast("Error");
            }
        }
        return readMessage;
    }

    /**
     * Method to make a toast
     *
     * @param msg Message.
     */
    private void toast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
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
                toast("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                toast("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }

    }

    /**
     * Save bitmap in /Picture/shelves
     *
     * @param bitmap Bitmap.
     */
    public static void storeImage(Bitmap bitmap) {
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    private String toJson() {
        String output = String.format("{" +
                "\"aisle\": {" +
                "\"name\": \"%s\"," +
                "\"bins\": [", aisle);
        boolean firstBin = true;
        for (Bin bin : result) {
            if (!firstBin) {
                output = output + ",";
            } else {
                firstBin = false;
            }
            output = output + String.format(
                    "{\"name\": \"%s\"," +
                            "\"occupancy_level\": \"%s%%\"," +
                            "\"level\": \"%s\"," +
                            "\"items\": [", bin.getBinLabelBarcode(), bin.getOccupancyLevel(), bin.getLevel());

            boolean firstBox = true;
            for (Map.Entry<Coordinate, Barcodes> entry : bin.getBoxesBarcodes().entrySet()) {
                if (!firstBox) {
                    output = output + ", ";
                } else {
                    firstBox = false;
                }
                Barcodes box = entry.getValue();
                output = output + String.format(
                        "{\"1T\": \"%s\"," +
                                "\"1P\": \"%s\"," +
                                "\"9D\": \"%s\"," +
                                "\"Q\": \"%s\"," +
                                "\"X\": \"%s\"" +
//                                "\t\t\t\t\t\"13D\": \"%s\"\n" +
                                "}", box.getT(), box.getP(), box.getD9(), box.getQ(), box.getX());
            }
            output = output + String.format("], \"errors\": \"%s\"}", bin.getError());
        }
        output = output + "]}}";
        try {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            File file = new File("/storage/emulated/0/Pictures/shelves/Aisle" + aisle + "-" + dateFormat.format(date) + ".json");
            Log.d("Output", file.toString());
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(output);
            bw.close();
        } catch (FileNotFoundException e) {
            Log.e("Output", e.toString());
        } catch (IOException e) {
            Log.e("Output", e.toString());
        }
        return output;
    }

    private String toHtml() {
        Date date = new Date();
        String output = String.format("<!DOCTYPE html>\n" +
                "<html>\n" +
                "<meta charset=\"utf-8\"/>\n" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"css/jquery.dynatable.css\">\n" +
                "<script src=\"js/jquery-1.7.2.min.js\" type=\"text/javascript\"></script>\n" +
                "<script src=\"js/jquery.dynatable.js\" type=\"text/javascript\"></script>\n" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"css/tablestyle.css\">\n" +
                "\n" +
                "<script src=\"js/script.js\"></script>\n" +
                "<h2>%s</h2>\n" +
                "<h3>%s</h3><table>\n" +
                "<thead>\n" +
                "  <tr>\n" +
                "    <th rowspan=\"2\">Name</th>\n" +
                "    <th rowspan=\"2\", colspan = \"1\">Occupancy Level</th>\n" +
                "    <th rowspan=\"2\">Level</th>\n" +
                "    <th rowspan=\"2\">Errors</th>\n" +
                "    <th rowspan=\"2\">Link</th>\n" +
                "    <th colspan = \"5\">Item\n" +
                "      <tr>\n" +
                "        <th>1T</th>\n" +
                "        <th>1P</th>\n" +
                "        <th>9D</th>\n" +
                "        <th>Q</th>\n" +
                "        <th>X</th>\n" +
                "      </tr>\n" +
                "    </th>\n" +
                "  </tr>\n" +
                "</thead>\n" +
                "<tbody>", "Aisle " + aisle, new SimpleDateFormat("yy/mm/dd").format(date));
        boolean firstBin = true;
        for (Bin bin : result) {
            int totalBoxes = bin.getBoxesBarcodes().size();
            if (totalBoxes == 0) {
                totalBoxes = 1;
            }
            output = output + String.format(
                    "<tr>\n" +
                            "      <td rowspan= \"%s\">%s</td>\n" +
                            "      <td rowspan= \"%s\">%s %%</td>\n" +
                            "      <td rowspan= \"%s\">%s</td>\n" +
                            "      <td rowspan= \"%s\">%s</td>\n" +
                            "      <td rowspan= \"%s\",class = \"link\"><img src=\"%s\"></td>\n",
                    totalBoxes, bin.getBinLabelBarcode(), totalBoxes, bin.getOccupancyLevel(), totalBoxes, bin.getLevel(),
                    totalBoxes, bin.getError(), totalBoxes, lastPictureTaken.getName());

            for (Map.Entry<Coordinate, Barcodes> entry : bin.getBoxesBarcodes().entrySet()) {
                Barcodes box = entry.getValue();
                output = output + String.format(
                        "<td>%s</td>\n" +
                                "<td>%s</td>\n" +
                                "<td>%s</td>\n" +
                                "<td>%s</td>\n" +
                                "<td>%s</td>\n",
                        box.getT(), box.getP(), box.getD9(), box.getQ(), box.getX());
            }
            output = output + "</tr>";
        }
        output = output + "</tbody>\n" +
                "</table>\n" +
                "</html>";

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            File file = new File("/storage/emulated/0/Pictures/shelves/Aisle" + aisle + "-" + dateFormat.format(date) + ".html");
            Log.d("Output", file.toString());
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(output);
            bw.close();
        } catch (FileNotFoundException e) {
            Log.e("Output", e.toString());
        } catch (IOException e) {
            Log.e("Output", e.toString());
        }
        return output;
    }
}