
package org.smartwarehouse.localization;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by Amy on 12/4/17.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(parameters);
//            mCamera.startPreview();
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
//            Camera.PictureCallback jpgCallback = new Camera.PictureCallback()
//            {
//                public void onPictureTaken(byte[] data, Camera camera)
//                {
//                    try
//                    {
////                        Dialog d=new Dialog();
////                        d.setContentView(0x7f030000);
//                        Log.d("Take photo", "Successful");
//                        BitmapFactory.Options opts = new BitmapFactory.Options();
//                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,opts);
////                        d.show();
//                    }
//                    catch(Exception e)
//                    {
//                        Log.d("Take photo", "Unsuccessful");
//                    }
//                }
//
//            };
//            mCamera.takePicture(null, null, jpgCallback);

        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        } catch (NullPointerException e) {
            Log.d(TAG, "mPreviewSize is empty" + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreviewAndFreeCamera();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
            stopPreviewAndFreeCamera();
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.setParameters(parameters);
//            mCamera.startPreview();
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void takePicture(Camera.PictureCallback mPicture) {
        mCamera.takePicture(null, null, mPicture);
        stopPreviewAndFreeCamera();
    }

    public void stopPreviewAndFreeCamera() {

        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCamera.stopPreview();

            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            mCamera.release();

            mCamera = null;
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.0;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        }
        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
        if (mPreviewSize != null) {
            float ratio;
            if (mPreviewSize.height >= mPreviewSize.width)
                ratio = (float) mPreviewSize.height / (float) mPreviewSize.width;
            else
                ratio = (float) mPreviewSize.width / (float) mPreviewSize.height;

            // One of these methods should be used, second method squishes preview slightly
            setMeasuredDimension(width, (int) (width * ratio));
            //        setMeasuredDimension((int) (width * ratio), height);
        } else {
            setMeasuredDimension(width, height);
        }

    }
}