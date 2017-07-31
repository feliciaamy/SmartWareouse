package org.smartwarehouse.scanner;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.scandit.barcodepicker.BarcodePicker;
import com.scandit.barcodepicker.OnScanListener;
import com.scandit.barcodepicker.ProcessFrameListener;
import com.scandit.barcodepicker.ScanOverlay;
import com.scandit.barcodepicker.ScanSession;
import com.scandit.barcodepicker.ScanSettings;
import com.scandit.barcodepicker.ScanditLicense;
import com.scandit.recognition.Barcode;
import com.scandit.recognition.TrackedBarcode;

import org.smartwarehouse.localization.MainActivity;
import org.smartwarehouse.localization.Type;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by Amy on 2/6/17.
 */

public class BarcodeScanner extends Activity implements OnScanListener, ProcessFrameListener {

    // Enter your Scandit SDK App key here.
    // Your Scandit SDK App key is available via your Scandit SDK web account.
    public static final String sScanditSdkAppKey = "AWv6LTpvIAfOHM7GrgECOZo2+u5sIIwqmgoShP9SsP/NAw/MUgH23MZopSOlUefNKXQqqMhFQVEcRBaxhHPvqXpUeyJvVkUnNSfqScJjByBseJina21nIAk5YEf5jCLQ5dOtrUmh28y/2Q4oSDCvBkM/XNal3vW3cgFauDRnAZV9VoOMO0+v84el1j7hBhsvNsQ2Z8ZcBpG/T/VBJjhJ3EqfaR/Oz3iWHgnSJoiHBSUXT68uqzv37rmfIUYuPwOnquY72Xg2x1Pg+PwATU/HmoZ5DFVkAu0YehXFU6OvXBOpstW0MqOfith7IY1kaePRTv9BLmcKT3ZCi8Q8qRBunFP375JEcSmQ4C+tz/NaAP/UGh9NBstNqGdM/3IEUuMwv8/2GMIC77ihuj/cN75ZSewjYEmIFisXl8WhNl1jEN1SVDz+m8qDXsfQI1SvcwlzrruAoLxakdVcPfSF174/e0f8MLpLh7KXiBN6eItt1J9e5rufLp///5b2fJSPzjil/NXvRMapwNWkH4leMtErb9JtBHccowsvkd0KZEYwwlNPtrkf9RfwhaSdZOxY/ZsqD1EE/dPFXKBmKAP1zA+6zVQyhj65TtEY99rI2qUCvUZFUftIZsP/65V9i6fWc9KYBc2uFxXqcJPUIvxt2Z6kYZlHeRGph4HvCz8r212ohaHNhDIMxczs+JpCnHpu4BZpN6GQsrVTklH5/Lp5im+YjXP3uKRVxP/MI+qO3lbRaBqmz0f/+kdDO78/Hlio9lHPwgwnZgzF4IdD9JjlieOaMdzZ0jceN1c6hdol048=";

    private final int CAMERA_PERMISSION_REQUEST = 0;

    // The main object for recognizing a displaying barcodes.
    private BarcodePicker mBarcodePicker;
    private boolean mDeniedCameraAccess = false;
    private boolean mPaused = true;

    // Newly added
    private Button mBarcodeSplash = null;
    private UIHandler mHandler = null;

    private Type type;

    private int counter;
    private Timer timer = new Timer("ScanditTimer");//create a new Timer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new UIHandler(this);

        ScanditLicense.setAppKey(sScanditSdkAppKey);
        String sType = getIntent().getStringExtra("type");
        Log.d("sType", sType);
        if (sType.equals(Type.BINLABEL.toString())) {
            type = Type.BINLABEL;
        } else {
            type = Type.BOX;
        }

        // Initialize and start the bar code recognition.
        initializeAndStartBarcodeScanning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();
        // When the activity is in the background immediately stop the
        // scanning to save resources and free the camera.
        mBarcodePicker.stopScanning();
        mPaused = true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void grantCameraPermissionsThenStartScanning() {
        if (this.checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (mDeniedCameraAccess == false) {
                // it's pretty clear for why the camera is required. We don't need to give a
                // detailed reason.
                this.requestPermissions(new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST);
            }

        } else {
            // we already have the permission
            mBarcodePicker.startScanning();
//            setZoom(0.8);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mDeniedCameraAccess = false;
                if (!mPaused) {
                    mBarcodePicker.startScanning();
                }
            } else {
                mDeniedCameraAccess = true;
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPaused = false;
        // handle permissions for Marshmallow and onwards...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            grantCameraPermissionsThenStartScanning();
        } else {
            // Once the activity is in the foreground again, restart scanning.
            mBarcodePicker.startScanning();

        }
    }

    /**
     * Initializes and starts the MatrixScan
     */
    public void initializeAndStartBarcodeScanning() {
        counter = 0;
        // Switch to full screen.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // The scanning behavior of the barcode picker is configured through scan
        // settings. We start with empty scan settings and enable a generous set
        // of 1D symbologies. MatrixScan is currently only supported for 1D
        // symbologies, enabling 2D symbologies will result in unexpected results.
        // In your own apps, only enable the symbologies you actually need.
        ScanSettings settings = ScanSettings.create();
        // if they match an already decoded barcode in the session
//        settings.setRelativeZoom(0.9f);
        settings.setCodeDuplicateFilter(0);

//        settings.setMicroDataMatrixEnabled();
        // the maximum number of codes to be decoded every frame
        settings.setMaxNumberOfCodesPerFrame(1);
        settings.setRelativeZoom(1f);
        if (type == Type.BINLABEL) {
            settings.setSymbologyEnabled(Barcode.SYMBOLOGY_CODE128, true);
            Log.d("SCANDIT ZOOM", "" + settings.getRelativeZoom());

        } else {
            settings.setSymbologyEnabled(Barcode.SYMBOLOGY_DATA_MATRIX, true);
            settings.setForce2dRecognitionEnabled(true);
            Log.d("SCANDIT ZOOM", "" + settings.getRelativeZoom());
//            settings.setSymbologyEnabled(Barcode.SYMBOLOGY_CODE128, true);
        }

        // Enable MatrixScan and set the max number of barcodes that can be recognized per frame
        // to some reasonable number for your use case. The max number of codes per frame does not
        // limit the number of codes that can be tracked at the same time, it only limits the
        // number of codes that can be newly recognized per frame.
        settings.setMatrixScanEnabled(false);
        settings.setHighDensityModeEnabled(true);

        settings.setRestrictedAreaScanningEnabled(true);
        // Prefer the back-facing camera, if there is any.
        settings.setCameraFacingPreference(ScanSettings.CAMERA_FACING_BACK);

        // Some Android 2.3+ devices do not support rotated camera feeds. On these devices, the
        // barcode picker emulates portrait mode by rotating the scan UI.
        boolean emulatePortraitMode = !BarcodePicker.canRunPortraitPicker();
        if (emulatePortraitMode) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        mBarcodePicker = new BarcodePicker(this, settings);
        mBarcodePicker.applyScanSettings(settings);
        // Set the GUI style to MatrixScan to see a visualization of the tracked barcodes. If you
        // would like to visualize it yourself, set it to ScanOverlay.GUI_STYLE_NONE and update your
        // visualization in the didProcess() callback.
        mBarcodePicker.getOverlayView().setGuiStyle(ScanOverlay.GUI_STYLE_MATRIX_SCAN);

        // When using MatrixScan vibrating is often not desired.
        mBarcodePicker.getOverlayView().setVibrateEnabled(false);

        setContentView(mBarcodePicker);

        // Register listener, in order to be notified about relevant events
        // (e.g. a successfully scanned bar code).
        mBarcodePicker.setOnScanListener(this);

        // Register a process frame listener to be able to reject tracked codes.
        mBarcodePicker.setProcessFrameListener(this);

        TimerTask timerTask = new TimerTask() {

            @Override
            public void run() {
                Log.d("TimerTask", "" + counter);
                if (counter >= 5) {
                    onPause();
                    backToMain();
                }
                counter++;//increments the counter
            }
        };

        timer.scheduleAtFixedRate(timerTask, 30, 3000);
    }

    private void backToMain(){
        Intent resultData = new Intent(this, MainActivity.class);
        setResult(Activity.RESULT_CANCELED, resultData);
        finish();
    }
    @Override
    public void didScan(ScanSession session) {
        // This callback acts the same as when not tracking and can be used for the events such as
        // when a code is newly recognized. Rejecting tracked codes has to be done in didProcess().
        // number of expected barcodes
        counter = 0;
        int numExpectedCodes = 1;
        // get all the scanned barcodes from the session
        List<Barcode> allCodes = session.getAllRecognizedCodes();

        // if the number of scanned codes is greater or equal than the number of expected barcodes
        // pause the scanning and clear the session (to remove recognized barcodes).
        if (allCodes.size() >= numExpectedCodes) {
            // pause scanning and clear the session. The scanning itself is resumed
            // when the user taps the screen.
//            Message msg = mHandler.obtainMessage(UIHandler.SHOW_BARCODES,
//                    allCodes);
            String data = "";
            for (Barcode b : allCodes) {
                Log.d("Scandit barcode", b.getData());
                data = data + b.getData();
            }

            Intent resultData = new Intent(this, MainActivity.class);
            resultData.putExtra("barcodes", data);
            setResult(Activity.RESULT_OK, resultData);
            session.stopScanning();
            session.clear();
            finish();
        }

    }

    @Override
    public void didProcess(byte[] imageBuffer, int width, int height, ScanSession session) {
        for (TrackedBarcode code : session.getTrackedCodes().values()) {
            if (code.getSymbology() == Barcode.SYMBOLOGY_EAN8) {
                session.rejectTrackedCode(code);
            }
        }
    }

    @Override
    public void onBackPressed() {
        mBarcodePicker.stopScanning();
        finish();
    }

    static private class UIHandler extends Handler {
        public static final int SHOW_BARCODES = 0;
        private WeakReference<BarcodeScanner> mActivity;

        UIHandler(BarcodeScanner activity) {
            mActivity = new WeakReference<BarcodeScanner>(activity);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_BARCODES:
                    showSplash(createMessage((List<Barcode>) msg.obj));
                    break;
            }

        }

        private String createMessage(List<Barcode> codes) {
            String message = "";
            for (Barcode code : codes) {
                String data = code.getData();
                // truncate code to certain length
                String cleanData = null;
                if (data.length() > 30) {
                    cleanData = data.substring(0, 25) + "[...]";
                } else {
                    cleanData = data;
                }
                if (message.length() > 0) {
                    message += "\n";
                }
                message += cleanData;
                message += "\n(" + code.getSymbologyName().toUpperCase(Locale.US) + ")";
            }
            return message;
        }


        private void showSplash(String msg) {
            BarcodeScanner activity = mActivity.get();
            activity.mBarcodeSplash = new Button(activity);
            activity.mBarcodeSplash.setTextColor(Color.WHITE);
            activity.mBarcodeSplash.setTextSize(20);
            activity.mBarcodeSplash.setGravity(Gravity.CENTER);
            activity.mBarcodeSplash.setBackgroundColor(0x0039C1CC);
            activity.mBarcodeSplash.setText(msg);
            RelativeLayout layout = activity.mBarcodePicker;
            layout.addView(activity.mBarcodeSplash, WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            activity.mBarcodePicker.pauseScanning();

            activity.mBarcodeSplash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BarcodeScanner activity = mActivity.get();
                    activity.mBarcodePicker.resumeScanning();
                    activity.mBarcodePicker.removeView(activity.mBarcodeSplash);
                    activity.mBarcodeSplash = null;
                }
            });
            activity.mBarcodeSplash.requestFocus();
        }
    }

}
