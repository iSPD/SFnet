package com.ispd.sfcam.AIEngineObjDetection;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

import com.ispd.sfcam.AIEngineObjDetection.env.ImageUtils;
import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.cameraFragment;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.SFTunner2;

import org.tensorflow.demo.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by khkim on 2018-08-05.
 */

public class personDetection {

    private String TAG = "personDetection";

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 320;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "mobile_ssd_v2_float_coco.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";

    private Context mContext;
    private AssetManager mAsset;

    private boolean computingDetection = false;

    private Classifier mObjDetector;
    private MultiBoxTracker mTracker;
    private Matrix mFrameToCropTransform0;
    private Matrix mFrameToCropTransform90;
    private Matrix mFrameToCropTransform180;
    private Matrix mFrameToCropTransform270;
    private Matrix mCropToFrameTransform0;
    private Matrix mCropToFrameTransform90;
    private Matrix mCropToFrameTransform180;
    private Matrix mCropToFrameTransform270;

    private long mTimestamp = 0;
    private int mYRowStride;

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private int mCropSize = 0;

    private static final boolean MAINTAIN_ASPECT = false;
    private int mCameraLocation;

    private byte[] mYuvBytes = null;
    private int[] mRgbBytes = null;
    private Bitmap mRgbFrameBitmap = null;
    private Bitmap mCroppedBitmap = null;

    private List<Classifier.Recognition> mResults = new ArrayList<>(10);
    private Classifier.Recognition mResultsCopy[] = new Classifier.Recognition[10];

    private int []mObjectNoTrack = {0};
    private int []mObjectNoThread = {0};

    private boolean []mObjMiddleTrack = {false};
    private boolean []mObjMiddleThread = {false};

    //normal
    private RectF mObjRect[] = new RectF[10];
    private float mObjConfidence[] = new float[10];
    private String mObjName[] = new String[10];
    private int mObjColor[] = new int[10];
    private String mObjId[] = new String[10];

    //thread
    private RectF mObjRectThread[] = new RectF[10];
    private float mObjConfidenceThread[] = new float[10];
    private String mObjNameThread[] = new String[10];
    private int mObjColorThread[] = new int[10];
    private String mObjIdThread[] = new String[10];

    public personDetection(Context context, AssetManager asset, int previewWidth, int previewHeight)
    {
        previewWidth = previewWidth / 2;
        previewHeight = previewHeight / 2;

        mContext = context;
        mAsset = asset;

        //mTracker = new MultiBoxTracker(mContext);

//        try {
//            mObjDetector =
//                    TFLitePersonAPIModel.create(
//                            asset,
//                            TF_OD_API_MODEL_FILE,
//                            TF_OD_API_LABELS_FILE,
//                            TF_OD_API_INPUT_SIZE,
//                            TF_OD_API_IS_QUANTIZED);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        mYRowStride = previewWidth;

        mCropSize = TF_OD_API_INPUT_SIZE;
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;

        mRgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        mCroppedBitmap = Bitmap.createBitmap(mCropSize, mCropSize, Bitmap.Config.ARGB_8888);

        mRgbBytes = new int[mPreviewWidth * mPreviewHeight];

        Classifier.Recognition temp = new Classifier.Recognition("0", "temp", 0.0f, new RectF(0.0f, 0.0f, 0.0f, 0.0f));
        for(int i = 0; i < 10; i++) {
            mResults.add(temp);
            mResultsCopy[i] = temp;
        }
    }

    public void creatModel()
    {
        try {
            mObjDetector =
                TFLitePersonAPIModel.create(
                        mAsset,
                        TF_OD_API_MODEL_FILE,
                        TF_OD_API_LABELS_FILE,
                        TF_OD_API_INPUT_SIZE,
                        TF_OD_API_IS_QUANTIZED);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCameraLocation(int front)
    {
        Log.d("setCameraLocation", "setCameraLocation : "+front);

        mCameraLocation = front;

        if(front == 0) {//back camera
            mFrameToCropTransform0 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth, mPreviewHeight,
                            mCropSize, mCropSize,
                            90, MAINTAIN_ASPECT);
            mCropToFrameTransform0 = new Matrix();
            mFrameToCropTransform0.invert(mCropToFrameTransform0);

            mFrameToCropTransform90 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth, mPreviewHeight,
                            mCropSize, mCropSize,
                            180, MAINTAIN_ASPECT);
            mCropToFrameTransform90 = new Matrix();
            mFrameToCropTransform90.invert(mCropToFrameTransform90);

            mFrameToCropTransform180 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth, mPreviewHeight,
                            mCropSize, mCropSize,
                            270, MAINTAIN_ASPECT);
            mCropToFrameTransform180 = new Matrix();
            mFrameToCropTransform180.invert(mCropToFrameTransform180);

            mFrameToCropTransform270 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth, mPreviewHeight,
                            mCropSize, mCropSize,
                            0, MAINTAIN_ASPECT);
            mCropToFrameTransform270 = new Matrix();
            mFrameToCropTransform270.invert(mCropToFrameTransform270);
        }
        else
        {
            // 0 flip
            mFrameToCropTransform0 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth, mPreviewHeight,
                            mCropSize, mCropSize,
                            270, MAINTAIN_ASPECT);
            mCropToFrameTransform0 = new Matrix();
            mFrameToCropTransform0.invert(mCropToFrameTransform0);

            // 90 no filp
            mFrameToCropTransform90 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth, mPreviewHeight,
                            mCropSize, mCropSize,
                            180, MAINTAIN_ASPECT);
            mCropToFrameTransform90 = new Matrix();
            mFrameToCropTransform90.invert(mCropToFrameTransform90);

            //180 flip
            mFrameToCropTransform180 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth, mPreviewHeight,
                            mCropSize, mCropSize,
                            90, MAINTAIN_ASPECT);
            mCropToFrameTransform180 = new Matrix();
            mFrameToCropTransform180.invert(mCropToFrameTransform180);

            //270 filp
            mFrameToCropTransform270 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth, mPreviewHeight,
                            mCropSize, mCropSize,
                            0, MAINTAIN_ASPECT);
            mCropToFrameTransform270 = new Matrix();
            mFrameToCropTransform270.invert(mCropToFrameTransform270);
        }
    }

    public void setPreviewData(final byte[] yuvBytes)
    {
        if (mYuvBytes == null) {
            mYuvBytes = new byte[yuvBytes.length];
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                System.arraycopy(yuvBytes, 0, mYuvBytes, 0, yuvBytes.length);

                int skip = 2;
                int fillCount = 0;
                Date startTime = new Date();
                for(int i = 0; i < aiCamParameters.PREVIEW_HEIGHT_I * 3 / 2; i = i + skip)
                {
                    if( i < aiCamParameters.PREVIEW_HEIGHT_I ) {
                        for (int j = 0; j < aiCamParameters.PREVIEW_WIDTH_I; j = j + skip) {
                            mResizeYuvByte[fillCount++] = mYuvBytes[i * aiCamParameters.PREVIEW_WIDTH_I + j];
                        }
                    }
                    else
                    {
                        for (int j = 0; j < aiCamParameters.PREVIEW_WIDTH_I; j = j + (skip * 2)) {
                            mResizeYuvByte[fillCount++] = mYuvBytes[i * aiCamParameters.PREVIEW_WIDTH_I + j];
                            mResizeYuvByte[fillCount++] = mYuvBytes[i * aiCamParameters.PREVIEW_WIDTH_I + j + 1];
                        }
                    }
                }

                int yWidth = aiCamParameters.PREVIEW_WIDTH_I / skip;
                int yHeight = aiCamParameters.PREVIEW_HEIGHT_I / skip;

                int yCount = 0;
                for( int i = yHeight-1; i >= 0; i-- )
                {
                    for( int j = 0; j < yWidth; j++ )
                    {
                        mTransByte[yCount++] = mResizeYuvByte[i * yWidth + j];
                    }
                }

                Log.d("tensor-test2", "setPreviewData : " + (new Date().getTime() - startTime.getTime()));
            }
        });
        thread.start();
    }

    byte[] mResizeYuvByte = new byte[aiCamParameters.PREVIEW_WIDTH_I / 2 * (aiCamParameters.PREVIEW_HEIGHT_I * 3 / 2) / 2];
    byte []mTransByte = new byte[aiCamParameters.PREVIEW_WIDTH_I / 2 * aiCamParameters.PREVIEW_HEIGHT_I / 2];

    private int objectNoCheckTrack = 0;
    private int objectNoCheckThread = 0;

    public void processImage(final int rotate)
    {
        // No mutex needed as this method is not reentrant.
        if (computingDetection == true || mObjDetector == null) {
            Log.d(TAG, "processImaging...");
            return;
        }

        ++mTimestamp;
        final long currTimestamp = mTimestamp;

        Log.d(TAG, "mCameraLocation : "+mCameraLocation);

//        if( mCameraLocation == 1 ) {
//            mTracker.onFrame(
//                    aiCamParameters.PREVIEW_WIDTH_I / 2,
//                    aiCamParameters.PREVIEW_HEIGHT_I / 2,
//                    mYRowStride,
//                    270/*sensorOrientation*/,
//                    mTransByte/*originalLuminance*/,
//                    mTimestamp);
//
//            objectNoCheckTrack = mTracker.getTrackedObj(mObjRect, mObjConfidence, mObjName, mObjColor, mObjId, mCameraLocation);
//        }
//        else
//        {
//            mTracker.onFrame(
//                    aiCamParameters.PREVIEW_WIDTH_I / 2,
//                    aiCamParameters.PREVIEW_HEIGHT_I / 2,
//                    mYRowStride,
//                    90/*sensorOrientation*/,
//                    mResizeYuvByte/*originalLuminance*/,
//                    mTimestamp);
//
//            objectNoCheckTrack = mTracker.getTrackedObj(mObjRect, mObjConfidence, mObjName, mObjColor, mObjId, mCameraLocation);
//        }

        if (computingDetection) {
            Log.d(TAG, "obj is not available");
            return;
        }
        computingDetection = true;

//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
                ImageUtils.convertYUV420SPToARGB8888(mResizeYuvByte, mPreviewWidth, mPreviewHeight, mRgbBytes);

                mRgbFrameBitmap.setPixels(mRgbBytes, 0, mPreviewWidth, 0, 0, mPreviewWidth, mPreviewHeight);

                final Canvas canvas = new Canvas(mCroppedBitmap);
                if( rotate == 0 ) {
                    canvas.drawBitmap(mRgbFrameBitmap, mFrameToCropTransform0, null);
                }
                else if( rotate == 90 ) {
                    canvas.drawBitmap(mRgbFrameBitmap, mFrameToCropTransform90, null);
                }
                else if( rotate == 180 ) {
                    canvas.drawBitmap(mRgbFrameBitmap, mFrameToCropTransform180, null);
                }
                else if( rotate == 270 ) {
                    canvas.drawBitmap(mRgbFrameBitmap, mFrameToCropTransform270, null);
                }

                //front
                if (mCameraLocation == 1) {
                    Matrix m = new Matrix();
                    m.preScale(-1, 1);
                    mCroppedBitmap = Bitmap.createBitmap(mCroppedBitmap, 0, 0, mCroppedBitmap.getWidth(), mCroppedBitmap.getHeight(), m, false);
                }

                Log.d(TAG, "recognizeImage");
                mResults = mObjDetector.recognizeImage(mCroppedBitmap);
                Log.d(TAG, "recognizeImage Ok");

                float  minimumConfidence = 0.7f;

                final List<Classifier.Recognition> mappedRecognitions =
                        new LinkedList<Classifier.Recognition>();

                Log.d(TAG, "[Check-Person] fine : "+mResults.size());

                for (final Classifier.Recognition result : mResults) {
                    final RectF location = result.getLocation();

                    //mCropToFrameTransform0.mapRect(location);
                    if( rotate == 0 ) {
                        mCropToFrameTransform0.mapRect(location);
                    }
                    else if( rotate == 90 ) {
                        mCropToFrameTransform90.mapRect(location);
                    }
                    else if( rotate == 180 ) {
                        mCropToFrameTransform180.mapRect(location);
                    }
                    else if( rotate == 270 ) {
                        mCropToFrameTransform270.mapRect(location);
                    }

                    //Log.d(TAG, "[Check-Person] This is "+result.getTitle());

                    if (location != null && result.getConfidence() >= minimumConfidence ) {

                        result.setLocation(location);
                        mappedRecognitions.add(result);

                        if( result.getTitle().equals("person") == true )
                        {
                            Log.d(TAG, "[Check-Person] This is Person");
                        }
                        else
                        {
                            Log.d(TAG, "[Check-Person] This is other "+result.getTitle());
                        }
                    }
                }

//                if( mCameraLocation == 1 ) {
//                    mTracker.trackResults(mappedRecognitions, mTransByte, currTimestamp);
//                }
//                else {
//                    mTracker.trackResults(mappedRecognitions, mResizeYuvByte, currTimestamp);
//                }
//
//                objectNoCheckThread = mTracker.getTrackedObj(mObjRectThread, mObjConfidenceThread, mObjNameThread, mObjColorThread, mObjIdThread, mCameraLocation);

                computingDetection = false;
//            }
//        });
//        thread.start();
    }
}
