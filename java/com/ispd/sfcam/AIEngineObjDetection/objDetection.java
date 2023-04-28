package com.ispd.sfcam.AIEngineObjDetection;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.ispd.sfcam.AIEngineObjDetection.env.ImageUtils;
import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.cameraFragment;
import com.ispd.sfcam.drawView.drawViewer;
import com.ispd.sfcam.jniController;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.SFTunner2;
import com.ispd.sfcam.utils.movingChecker;

import org.tensorflow.demo.tracking.MultiBoxTracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by khkim on 2018-08-05.
 */

public class objDetection {

    private String TAG = "objDetection";

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detectCoco.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";

    private static final String TF_HD_API_MODEL_FILE = "detectHand.tflite";
    private static final String TF_HD_API_LABELS_FILE = "file:///android_asset/hand_labels_list.txt";

    // Minimum detection confidence to track a detection.
    //private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.35f;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.2f;
    private static final float MINIMUM_CONFIDENCE_TF_HD_API = 0.7f;

    private Context mContext;

    private boolean computingDetection = false;

    private Classifier mObjDetector;
    private Classifier mHandDetector;
    private MultiBoxTracker mTracker;
    private MultiBoxTracker mTrackerHand;
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

    private byte[] mZeroYuvBytes = null;
    private byte[] mYuvBytes = null;
    private int[] mRgbBytes = null;
    private Bitmap mRgbFrameBitmap = null;
    private Bitmap mCroppedBitmap = null;

    private List<Classifier.Recognition> mResults = new ArrayList<>(10);
    private Classifier.Recognition mResultsCopy[] = new Classifier.Recognition[10];

    private int mObjectNoResult = 0;
    private int []mObjectNoTrack = {0};
    private int []mObjectNoThread = {0};

    private boolean mObjMiddleResult = false;
    private boolean []mObjMiddleTrack = {false};
    private boolean []mObjMiddleThread = {false};

    //normal
    private RectF mObjRect[] = new RectF[10];
    private float mObjConfidence[] = new float[10];
    private String mObjName[] = new String[10];
    private int mObjColor[] = new int[10];
    private float mObjMadeSize[] = new float[10];
    private String mObjId[] = new String[10];
    private boolean mObjDisplay[] = new boolean[10];

    private int mMoreRect[] = new int[10];
    private int mMoreRectCount = 0;

    private float []mLastObjRect = {0.f, 0.f, 0.f, 0.f};

    //thread
    private RectF mObjRectThread[] = new RectF[10];
    private float mObjConfidenceThread[] = new float[10];
    private String mObjNameThread[] = new String[10];
    private int mObjColorThread[] = new int[10];
    private float mObjMadeSizeThread[] = new float[10];
    private String mObjIdThread[] = new String[10];
    private boolean mObjDisplayThread[] = new boolean[10];

    private int mMoreRectThread[] = new int[10];
    private int mMoreRectCountThread = 0;

    private boolean mPersonDetected = false;
    private int mPersonCount = 0;

    int count = 0;

    //hardCoding?
    byte[] mResizeYuvByte = new byte[aiCamParameters.PREVIEW_WIDTH_I / 2 * (aiCamParameters.PREVIEW_HEIGHT_I * 3 / 2) / 2];
    byte []mTransByte = new byte[aiCamParameters.PREVIEW_WIDTH_I / 2 * aiCamParameters.PREVIEW_HEIGHT_I / 2];

    private int objectNoCheckResult = 0;
    private int objectNoCheckTrack = 0;
    private int objectNoCheckThread = 0;

    public objDetection(Context context, AssetManager asset, int previewWidth, int previewHeight)
    {
        //???
        previewWidth = previewWidth / 2;
        previewHeight = previewHeight / 2;

        mContext = context;

        mTracker = new MultiBoxTracker(mContext);
        mTrackerHand = new MultiBoxTracker(mContext);

        try {
            mObjDetector =
                    TFLiteObjectDetectionAPIModel.create(
                            asset,
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mHandDetector =
                    TFLiteObjectDetectionAPIModel.create(
                            asset,
                            TF_HD_API_MODEL_FILE,
                            TF_HD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
            //mResultsCopy.add(temp);
            mResultsCopy[i] = temp;
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
        //여기서 강제로 setPreviewData 1080x1440으로 변환

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

    //public void processImage(byte[] yByte, byte[] yuvBytes)
    public void processImage(final int rotate, boolean []isPerson)
    {
        ++mTimestamp;
        final long currTimestamp = mTimestamp;

        Log.d("tensor-test", "mCameraLocation : "+mCameraLocation);

        if( mCameraLocation == 1 ) {
            mTracker.onFrame(
                    aiCamParameters.PREVIEW_WIDTH_I / 2,
                    aiCamParameters.PREVIEW_HEIGHT_I / 2,
                    mYRowStride,
                    270/*sensorOrientation*/,
                    mTransByte/*originalLuminance*/,
                    mTimestamp);

            objectNoCheckTrack = mTracker.getTrackedObj(mObjRect, mObjConfidence, mObjName, mObjColor, mObjId, mCameraLocation);
            objectNoCheckTrack = sortObjectBig(mObjRect, mObjMadeSize, mObjConfidence, mObjName, mObjColor, mObjId, objectNoCheckTrack);

            Log.d("tensor-test8", "1 objNo : " + objectNoCheckTrack);
            mMoreRectCount = setObjInfoToJni(mObjectNoTrack, mObjMiddleTrack, mMoreRect, mObjRect, mObjColor, objectNoCheckTrack, mObjMadeSize, mObjName, mObjId, rotate);
            drawViewer.setObjectRect(mMoreRectCount, mMoreRect, mObjMiddleTrack[0], mObjDisplay, mObjRect, mObjMadeSize, mObjConfidence, mObjName, mObjColor, objectNoCheckTrack, false);

            mObjMiddleResult = mObjMiddleTrack[0];
            objectNoCheckResult = objectNoCheckTrack;
            mObjectNoResult = mObjectNoTrack[0];
        }
        else
        {
            mTracker.onFrame(
                    aiCamParameters.PREVIEW_WIDTH_I / 2,
                    aiCamParameters.PREVIEW_HEIGHT_I / 2,
                    mYRowStride,
                    90/*sensorOrientation*/,
                    mResizeYuvByte/*originalLuminance*/,
                    mTimestamp);

            objectNoCheckTrack = mTracker.getTrackedObj(mObjRect, mObjConfidence, mObjName, mObjColor, mObjId, mCameraLocation);
            objectNoCheckTrack = sortObjectBig(mObjRect, mObjMadeSize, mObjConfidence, mObjName, mObjColor, mObjId, objectNoCheckTrack);

            Log.d("tensor-test8", "1 objNo : " + objectNoCheckTrack);
            Log.d("mSaveObjId", "Tracking");
            mMoreRectCount = setObjInfoToJni(mObjectNoTrack, mObjMiddleTrack, mMoreRect, mObjRect, mObjColor, objectNoCheckTrack, mObjMadeSize, mObjName, mObjId, rotate);
            drawViewer.setObjectRect(mMoreRectCount, mMoreRect, mObjMiddleTrack[0], mObjDisplay, mObjRect, mObjMadeSize, mObjConfidence, mObjName, mObjColor, objectNoCheckTrack, false);

            mObjMiddleResult = mObjMiddleTrack[0];
            objectNoCheckResult = objectNoCheckTrack;
            mObjectNoResult = mObjectNoTrack[0];
        }

        isPersonDetected(mObjName, mObjConfidence, mObjRect, isPerson);

        if (computingDetection) {
            Log.d("tensor-test2", "obj is not available");
            return;
        }
        computingDetection = true;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Date startTime = new Date();

                count++;

                //ImageUtils.convertYUV420SPToARGB8888(mYuvBytes, mPreviewWidth, mPreviewHeight, mRgbBytes);
                //khkim hard coding...
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

                mResults = mObjDetector.recognizeImage(mCroppedBitmap);

                //float  minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                float  minimumConfidence = (float)(SFTunner2.mAIThreshod) / 100.f;

                final List<Classifier.Recognition> mappedRecognitions =
                        new LinkedList<Classifier.Recognition>();

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

                    //khkim hard coding...
                    float boxRate = (location.width() * location.height()) * 100.f / ((aiCamParameters.PREVIEW_WIDTH_I / 2) * (aiCamParameters.PREVIEW_HEIGHT_I / 2));
                    Log.d("box-rate", "boxSize : "+location.width()+", "+location.height());
                    Log.d("box-rate", "boxRate : "+boxRate);
                    if (location != null && result.getConfidence() >= minimumConfidence && boxRate > 0.0f) {

                        result.setLocation(location);
                        mappedRecognitions.add(result);
                    }
                }

                if( mCameraLocation == 1 ) {
                    mTracker.trackResults(mappedRecognitions, mTransByte, currTimestamp);
                }
                else {
                    mTracker.trackResults(mappedRecognitions, mResizeYuvByte, currTimestamp);
                }

                objectNoCheckThread = mTracker.getTrackedObj(mObjRectThread, mObjConfidenceThread, mObjNameThread, mObjColorThread, mObjIdThread, mCameraLocation);
                objectNoCheckThread = sortObjectBig(mObjRectThread, mObjMadeSizeThread, mObjConfidenceThread, mObjNameThread, mObjColorThread, mObjIdThread, objectNoCheckThread);

                isPersonDetected(mObjNameThread, mObjConfidenceThread, mObjRectThread, isPerson);

                Log.d("tensor-test-how", "mSaveObjColor : "+mObjColorThread[0]+", "+mObjColorThread[1]+", "+mObjColorThread[2]+", "+mObjColorThread[3]+", "+mObjColorThread[4]);
                Log.d("tensor-test8", "2 objNo : " + objectNoCheckThread);

                //test...
                mSaveObjNo = objectNoCheckThread;
                mSaveObjId = mObjIdThread[0];
                Log.d("tensor-test9", "Save objectNo : "+objectNoCheckThread);
                Log.d("mSaveObjId", "mSaveObjId : "+mSaveObjId);
                for(int i = 0; i < objectNoCheckThread; i++)
                {
                    Log.d("tensor-test9", "Save mObjName["+i+"] : "+mObjNameThread[i]);
                    Log.d("tensor-test9", "Save mObjName["+i+"] : "+mObjColorThread[i]);
                    Log.d("tensor-test9", "Save mObjConfidence["+i+"] : "+mObjConfidenceThread[i]);
                    Log.d("tensor-test9", "Save mObjRect["+i+"] : "+mObjRectThread[i]);
                }
                //test...

                Log.d("mSaveObjId", "Analisys..............................................");
                mMoreRectCountThread = setObjInfoToJni(mObjectNoThread, mObjMiddleThread, mMoreRectThread, mObjRectThread, mObjColorThread, objectNoCheckThread, mObjMadeSizeThread, mObjNameThread, mObjIdThread, rotate);
                drawViewer.setObjectRect(mMoreRectCountThread, mMoreRectThread, mObjMiddleThread[0], mObjDisplayThread, mObjRectThread,
                        mObjMadeSizeThread, mObjConfidenceThread, mObjNameThread, mObjColorThread, objectNoCheckThread, true);

                mObjMiddleResult = mObjMiddleThread[0];
                objectNoCheckResult = objectNoCheckThread;
                mObjectNoResult = mObjectNoThread[0];

                computingDetection = false;

                count--;

                Log.d("tensor-test2", "[detectMovingObj-Sync] mObjDetector.processImage real : " + (new Date().getTime() - startTime.getTime()));
                Log.d("moving-ai-sync", "[detectMovingObj-Sync] mBlurOn Done");

                cameraFragment.setAutoFocusOn(true);
            }
        });

        boolean aiOn = ! movingChecker.getMovingRunning();

        if (aiOn == true) {
            Log.d("moving-ai-sync", "[detectMovingObj-Sync] mBlurOnOff : "+mBlurOnOff);
            if (mBlurOnOff == true) {

                if( mObjAnaCount == 0 )
                {
                    Log.d("mSaveObjId", "[detectMovingObj-test] Moving to true");
                }

                int delayCount = SFTunner2.mAiWaitCount;

                mObjAnaCount++;
                //Log.d("moving-ai-sync", "[detectMovingObj-Sync] mObjAnaCount : "+mObjAnaCount);
                if( mObjAnaCount > delayCount )
                {
                    Log.d("moving-ai-sync", "[detectMovingObj-Sync] mBlurOn Do");
                    thread.start();
                }
                else
                {
                    computingDetection = false;
                }

                if ( mObjAnaCount >= SFTunner2.mAiProcessingCount + delayCount ) {

                    mBlurOnOff = false;
                }
            } else {
                mObjAnaCount = 0;
                computingDetection = false;
            }
        } else {
            mBlurOnOff = true;
            mObjAnaCount = 0;
            computingDetection = false;
        }
    }

    public void resetTracker()
    {
//        mTracker.release();
//        mTracker = null;
//        mTracker = new MultiBoxTracker(mContext);

        for(int i = 0; i < 10; i++)
        {
           mObjRect[i] = new RectF(0.0f, 0.0f, 0.0f, 0.0f);
           mObjConfidence[i] = 0.0f;
           mObjName[i] = "";
        }

        if( mZeroYuvBytes == null ) {
            mZeroYuvBytes = new byte[aiCamParameters.PREVIEW_WIDTH_I * aiCamParameters.PREVIEW_HEIGHT_I * 3 / 2];
            Arrays.fill(mZeroYuvBytes, (byte) 0x00);
        }
        setPreviewData(mZeroYuvBytes);
    }

    public void isPersonDetected(String []objName, float []objConfidence, RectF []objRect, boolean []isPerson)
    {
        int personCount = 0;
        for(int i = 0; i < 10; i++)
        {
            if( objName[i] != null && objName[i].equals("person") == true && objConfidence[i] > 0.60f )
            {
                Log.d(TAG, "[PersonTest] mObjName["+i+"] : "+objName[i]+", mObjConfidence : "+objConfidence[i]);

                //khkim Incorrect modify to PREVIEW_WIDTH_I/2 correct here...
                //float boxRate = (objRect[i].width() * objRect[i].height()) * 100.f / (aiCamParameters.PREVIEW_WIDTH_I * aiCamParameters.PREVIEW_HEIGHT_I);
                float boxRate = (objRect[i].width() * objRect[i].height()) * 100.f / (1440 * 1080);
                Log.d(TAG, "[PersonTest] boxRate : "+boxRate);
                if( boxRate > 10.0f ) {
                    personCount++;
                }
            }
            else if( objName[i] != null && objName[i].equals("person") == true )
            {
                Log.d(TAG, "[PersonTest] mObjName["+i+"] : "+objName[i]+", mObjConfidence : "+objConfidence[i]);
            }
        }
        if( personCount > 0 )
        {
            Log.d(TAG, "[PersonTest] This is Person");
            mPersonDetected = true;

        }
        else
        {
            Log.d(TAG, "[PersonTest] This is Not Person");
            mPersonDetected = false;
        }

        if( mPersonDetected == true )
        {
            mPersonCount++;
            Log.d(TAG, "[PersonTest] mPersonCount : "+mPersonCount);
            if( mPersonCount >= 1 )
            {
                Log.d(TAG, "[PersonTest] This is Real Person");
                isPerson[0] = true;
                mPersonCount = 0;

                //resetTracker();
            }
            else
            {
                isPerson[0] = false;
            }
        }
        else
        {
            mPersonCount = 0;
            isPerson[0] = false;
        }
    }

    public void processImageForHand(final int []faceRect, final int rotate)
    {
        ++mTimestamp;
        final long currTimestamp = mTimestamp;

        if (computingDetection) {
            Log.d("tensor-test2", "obj is not available");
            return;
        }
        computingDetection = true;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Date startTime = new Date();

                count++;

                ImageUtils.convertYUV420SPToARGB8888(mResizeYuvByte, mPreviewWidth, mPreviewHeight, mRgbBytes);
                mRgbFrameBitmap.setPixels(mRgbBytes, 0, mPreviewWidth, 0, 0, mPreviewWidth, mPreviewHeight);

                for(int i = 0; i < faceRect[0]; i++)
                {
                    Paint paint = new Paint();
                    paint.setColor(Color.BLACK);
                    paint.setStyle(Paint.Style.FILL);

                    float resizeW = (float)mPreviewWidth / (float)aiCamParameters.PREVIEW_WIDTH_I;
                    float resizeH = (float)mPreviewHeight / (float)aiCamParameters.PREVIEW_HEIGHT_I;

                    int left = (int)((float)faceRect[1 + i*4 + 0] * resizeW);
                    int right = (int)((float)faceRect[1 + i*4 + 1] * resizeW);
                    int top = (int)((float)faceRect[1 + i*4 + 2] * resizeH);
                    int bottom = (int)((float)faceRect[1 + i*4 + 3] * resizeH);

                    if (mCameraLocation == 1) {
                        if (rotate == 0) left = 0;
                        else if (rotate == 90) top = 0;
                        else if (rotate == 180) right = mPreviewWidth;
                        else if (rotate == 270) bottom = mPreviewHeight;
                    }
                    else
                    {
                        if (rotate == 0) right = mPreviewWidth;
                        else if (rotate == 90) top = 0;
                        else if (rotate == 180) left = 0;
                        else if (rotate == 270) bottom = mPreviewHeight;
                    }

                    Rect rect = new Rect(left, top, right, bottom);

                    final Canvas faceCanvas = new Canvas(mRgbFrameBitmap);
                    faceCanvas.drawRect(rect, paint);
                }
                //storeImage(mRgbFrameBitmap, "/sdcard/face.jpg");

                final Canvas canvas = new Canvas(mCroppedBitmap);

                //storeImage(mCroppedBitmap, "/sdcard/before.jpg");

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
                    if( rotate == 0 || rotate == 90 ||rotate == 180 ||rotate == 270 ) {
                        Matrix m = new Matrix();
                        m.preScale(-1, 1);
                        mCroppedBitmap = Bitmap.createBitmap(mCroppedBitmap, 0, 0, mCroppedBitmap.getWidth(), mCroppedBitmap.getHeight(), m, false);
                    }
                }

                mResults = mHandDetector.recognizeImage(mCroppedBitmap);

                float  minimumConfidence = MINIMUM_CONFIDENCE_TF_HD_API;
                if( mCameraLocation == 0 )
                {
                    minimumConfidence = (float)(SFTunner2.mBackFaceHandThreshold) / 100.f;
                }
                else
                {
                    minimumConfidence = (float)(SFTunner2.mFrontFaceHandThreshold) / 100.f;
                }

                Log.d(TAG, "minimumConfidence : "+minimumConfidence);

                int rectCount = 0;
                for (final Classifier.Recognition result : mResults) {

                    if ( result.getConfidence() >= minimumConfidence ) {

                        final RectF location = result.getLocation();
                        final float confidence = result.getConfidence();
                        final String name = result.getTitle();
                        Log.d("tensor-test", "check left : " + location.left + ", top : " + location.top + ", right : " + location.right + ", bottom : " + location.bottom);

                        Paint paint = new Paint();
                        paint.setColor(Color.BLACK);
                        canvas.drawRect(location, paint);

                        mObjRect[rectCount] = location;
                        mObjConfidence[rectCount] = confidence;
                        mObjName[rectCount] = name;

                        rectCount++;
                    }
                }
                drawViewer.setObjectRect(mMoreRectCount, mMoreRect, false, mObjDisplay, mObjRect, mObjMadeSize, mObjConfidence, mObjName, mObjColor, rectCount, true);
                setObjHandInfoToJni(mObjRect, mObjColor, rectCount, mObjMadeSize, mObjName, mObjId);

                computingDetection = false;

                count--;

                Log.d("tensor-test2", "mObjDetector.processImage real : " + (new Date().getTime() - startTime.getTime()));
            }
        });
        thread.start();
    }

    //khkim
    public boolean getObjBox(Rect []objRect)
    {
        Rect temp;
        if( mObjectNoResult > 0) {
            temp = new Rect((int) mLastObjRect[0] * 8, (int) mLastObjRect[1] * 8, (int) mLastObjRect[2] * 8, (int) mLastObjRect[3] * 8);
        }
        else
        {
            temp = new Rect(0,0,0,0);
        }
        objRect[0] = temp;

        return mObjMiddleResult;
    }

    public int getObjectNumber()
    {
        Log.d("test", "[detectMovingObj-check] getObjectNumber : "+mObjectNoResult);
        return mObjectNoResult;
    }

    static int mSaveObjNumber = -1;
    static int mReAnaCount = 0;
    public boolean needObjectReset()
    {
        Log.d("test", "[detectMovingObj-test] mSaveObjNumber : "+mSaveObjNumber+", objectNoCheck : "+objectNoCheckResult);

        if (mSaveObjNumber == 0) {
            if (objectNoCheckResult > 0) {
                Log.d("test", "[detectMovingObj-zero] objectNoCheck 0 to big");
                mSaveObjNumber = objectNoCheckResult;
                mReAnaCount = 0;
                return true;
            } else {
                Log.d("mSaveObjId", "[detectMovingObj-zero] mBlurOnOff to true : " + mSaveObjId + ", mReAnaCount : " + mReAnaCount);

                if (mBlurOnOff == false) {

                    mReAnaCount++;
                    if (mReAnaCount == 1) {
                        Log.d("moving-ai-sync", "[detectMovingObj-Sync] mBlurOnOff to true Do...");
                        Log.d("moving-ai-sync", "[detectMovingObj-zero] mBlurOn Do for Zero");

                        mObjAnaCount = 0;
                        mBlurOnOff = true;
                    }
                }
            }
        }
        else
        {
            mReAnaCount = 0;
        }
        mSaveObjNumber = objectNoCheckResult;

        return false;
    }

    //color matching...
    private boolean mBlurOn = true;
    private int mSaveObjColor = -1;

    //normal check...
    private boolean mBlurOnOff = true;
    private int mObjAnaCount = 0;
    private int mSaveObjNo = -1;
    private String mSaveObjId = "-1";

    //khkim hard coding...
    private float getMadeSize(RectF rect)
    {
        double middleX = (double)(1080.f / 2.0f);
        double middleY = (double)(1440.f / 2.0f);
        double maxDistance = Math.sqrt(Math.pow(middleX - 0, 2.0) + Math.pow(middleY - 0, 2.0));

        double objMiddleX = (double)(rect.centerX());
        double objMiddleY = (double)(rect.centerY());
        double distance = Math.sqrt(Math.pow(middleX - objMiddleX, 2.0) + Math.pow(middleY - objMiddleY, 2.0));
        float distanceRate = (float)distance * 100.f / (float)maxDistance;

        Log.d("distance-weight", "distanceRate : "+distanceRate);

        //기억이 안남
//        float weight;
//        if( distanceRate < 33.412f )
//        {
//            weight = (float)(Math.pow((double)distanceRate, 2.0)) / -16.0f + 100.f;
//        }
//        else
//        {
//            weight = -10.f * (float)(Math.log((double)distanceRate - 30.0)) + 42.5f;
//        }

        float weight;
        if( distanceRate < 21.262f )
        {
            weight = (float)(Math.pow((double)distanceRate, 2.0)) / -8.0f + 100.f;
        }
        else
        {
            weight = -10.f * (float)(Math.log((double)distanceRate - 20.466)) + 42.5f;
        }

        Log.d("distance-weight", "weight : "+weight);
        float madeSize = ((rect.width() * rect.height()) * 100.f / (1080.f * 1440.f)) * weight / 100.f;
        //madeSize += 5.0f;
        madeSize += SFTunner2.mAISizePlus;

        return madeSize;
    }

    //check here...
    private int sortObjectBig(RectF []rect, float []madeSize, float []confidence, String []name, int []color, String []id, int size)
    {
        RectF []rectResult = new RectF[size];
        for(int i = 0 ; i < size; i++)
        {
            rectResult[i] = new RectF(-1.f, -1.f, -1.f, -1.f);
        }

        float []confidenceResult = new float[size];
        String []nameResult = new String[size];
        int []colorResult = new int[size];
        String []idResult = new String[size];

        Log.d("sortObjP", "ori size : "+size);

        int personCount = 0;
        for(int i = 0; i < size; i++)
        {
            Log.d("sortObjP", "getMadeSize["+i+"] : "+getMadeSize(rect[i]));
            Log.d("sortObjP", "name["+i+"] : "+name[i]);

            if(name[i].equals("person") == true)
            {
                rectResult[personCount] = rect[i];
                confidenceResult[personCount] = confidence[i];
                nameResult[personCount] = name[i];
                colorResult[personCount] = color[i];
                idResult[personCount] = id[i];

                personCount++;
            }
        }

        if(personCount > 0)
        {
            Log.d("sortObjP", "personCount : "+personCount);

            for(int i = 0; i < personCount - 1; i++)
            {
                for(int j = i + 1; j < personCount; j++)
                {
                    float curSize = getMadeSize(rectResult[j]);
                    float preSize = getMadeSize(rectResult[i]);

                    Log.d("sortObjP", "curSize : "+curSize);
                    Log.d("sortObjP", "preSize : "+preSize);

                    if(curSize > preSize)
                    {
                        RectF temp = rectResult[i];
                        rectResult[i] = rectResult[j];
                        rectResult[j] = temp;

                        float temp2 = confidenceResult[i];
                        confidenceResult[i] = confidenceResult[j];
                        confidenceResult[j] = temp2;

                        String temp3 = nameResult[i];
                        nameResult[i] = nameResult[j];
                        nameResult[j] = temp3;

                        int temp4 = colorResult[i];
                        colorResult[i] = colorResult[j];
                        colorResult[j] = temp4;

                        String temp5 = idResult[i];
                        idResult[i] = idResult[j];
                        idResult[j] = temp5;
                    }
                }
            }
        }

        int objCount = personCount;

        Log.d("sortObjP-test", "size : "+size+"objCount : "+objCount);

        for(int i = 0; i < size; i++)
        {
            if(name[i].equals("person") == false)
            {
                //error???
                rectResult[objCount] = rect[i];
                confidenceResult[objCount] = confidence[i];
                nameResult[objCount] = name[i];
                colorResult[objCount] = color[i];
                idResult[objCount] = id[i];

                objCount++;
            }
        }

        Log.d("sortObjP", "objCount : "+objCount+", personCount : "+personCount +", size : "+size);

        for(int i = personCount; i < size - 1; i++)
        {
            for(int j = i + 1; j < size; j++)
            {
                Log.d("sortObjP","["+j+"] rectResult[j] : "+rectResult[j]);
                Log.d("sortObjP","["+i+"] rectResult[i] : "+rectResult[i]);
                Log.d("sortObjP","["+j+"] rectResult[j].center : "+rectResult[j].centerX());
                Log.d("sortObjP","["+i+"] rectResult[i].center : "+rectResult[i].centerX());

                float curSize = getMadeSize(rectResult[j]);
                float preSize = getMadeSize(rectResult[i]);

                if(curSize > preSize)
                {
                    RectF temp = rectResult[i];
                    rectResult[i] = rectResult[j];
                    rectResult[j] = temp;

                    float temp2 = confidenceResult[i];
                    confidenceResult[i] = confidenceResult[j];
                    confidenceResult[j] = temp2;

                    String temp3 = nameResult[i];
                    nameResult[i] = nameResult[j];
                    nameResult[j] = temp3;

                    int temp4 = colorResult[i];
                    colorResult[i] = colorResult[j];
                    colorResult[j] = temp4;

                    String temp5 = idResult[i];
                    idResult[i] = idResult[j];
                    idResult[j] = temp5;
                }
            }
        }

        Log.d("sortObjP", "<--------------------------------------->");

        //last result...
        for(int i = 0; i < size; i++)
        {
            rect[i] = rectResult[i];
            confidence[i] = confidenceResult[i];
            name[i] = nameResult[i];
            color[i] = colorResult[i];
            id[i] = idResult[i];

            madeSize[i] = getMadeSize(rectResult[i]);

            Log.d("sortObjP", "2madeSize["+i+"] : "+madeSize[i]);
            Log.d("sortObjP", "2name["+i+"] : "+nameResult[i]);
        }

        Log.d("sortObjP", "size : "+size+", objCount : "+objCount);
        return objCount;
    }

    private void storeImage(Bitmap image, String filePath) {
        File pictureFile = new File(filePath);
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.JPEG, 50, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    private void setObjHandInfoToJni(final RectF[] rect, final int []color, final int number, float []madeSize, String []name, String []id)
    {
        Date startTime = new Date();

        Matrix transScaleMat =
                ImageUtils.getTransformationMatrix(
                        300, 300,
                        180, 135,
                        270, MAINTAIN_ASPECT);

        int count = 0;
        final float []rectFloat = new float[number*4];

        if( number > 0 ) {
            mObjectNoResult = number;
            for (int i = 0; i < number; i++) {
                Log.d("tensor-test-hand", "Java rect[" + i + "] left : " + rect[i].left + ", top : " + rect[i].top + ", right : " + rect[i].right + ", top : " + rect[i].bottom);

                RectF result = new RectF();
                transScaleMat.mapRect(result, rect[i]);

                rectFloat[count++] = result.left;
                rectFloat[count++] = result.top;
                rectFloat[count++] = result.right;
                rectFloat[count++] = result.bottom;
            }
            jniController.updateObjRect(number, rectFloat);
        }
        else
        {
            Log.d("tensor-test-hand", "mObjectNo is Zero");
            mObjectNoResult = 0;
            jniController.updateObjRect(number, rectFloat);
        }

        Log.d("tensor-test-jni", "copy speed : " + (new Date().getTime() - startTime.getTime()));
        Log.d("test", "[detectMovingObj-check] mObjectNo : "+mObjectNoResult);
    }


    private int setObjInfoToJni(final int []lastObjNo, final boolean []objMiddle, final int []moreRect, final RectF[] rect, final int []color, final int number, float []madeSize, String []name, String []id, int rotate)
    {
        Date startTime = new Date();

        Matrix transScaleMat = new Matrix();
        if (mCameraLocation == 1) {
            transScaleMat =
                    ImageUtils.getTransformationMatrix(
                            1080, 1440,
                            180, 135,
                            270, MAINTAIN_ASPECT);
        }
        else
        {
            transScaleMat =
                    ImageUtils.getTransformationMatrix(
                            1080, 1440,
                            180, 135,
                            270, MAINTAIN_ASPECT);
        }

        Log.d("no-obj-test", "[Remove-Below] number : "+number);

        int personCount = 0;
        int personStatus = 2;

        //remove below objects
        for(int i = 0; i < number; i++)
        {
            mObjDisplay[i] = true;
        }

        if( number > 0 ) {
            float belowPercent = (float) SFTunner2.mAiBelowPercent;
            float upPercent = (float) SFTunner2.mAiUpPercent;
            float leftPercent = (float) SFTunner2.mAiLeftPercent;
            float rightPercent = (float) SFTunner2.mAiRightPercent;

            float cornerX = (float) SFTunner2.mAiCornerX;
            float cornerY = (float) SFTunner2.mAiCornerY;

            int belowRegion = (int) (1440.f * (100.f - belowPercent) / 100.f);
            int upRegion = (int) (1440.f * upPercent / 100.f);
            int leftRegion = (int) (1080.f * leftPercent / 100.f);
            int rightRegion = (int) (1080.f * (100.f - rightPercent) / 100.f);

            int cornerXValue = (int) (1080.f * cornerX / 100.f);
            int cornerYValue = (int) (1440.f * cornerY / 100.f);

            if( rotate == 90 )
            {
                belowRegion = (int) (1440.f * (100.f - leftPercent) / 100.f);
                upRegion = (int) (1440.f * rightPercent / 100.f);
                leftRegion = (int) (1080.f * upPercent / 100.f);
                rightRegion = (int) (1080.f * (100.f - belowPercent) / 100.f);

                cornerXValue = (int) (1080.f * cornerY / 100.f);
                cornerYValue = (int) (1440.f * cornerX / 100.f);
            }
            if( rotate == 180 )
            {
                belowRegion = (int) (1440.f * (100.f - upPercent) / 100.f);
                upRegion = (int) (1440.f * belowPercent / 100.f);
                leftRegion = (int) (1080.f * rightPercent / 100.f);
                rightRegion = (int) (1080.f * (100.f - leftPercent) / 100.f);

                cornerXValue = (int) (1080.f * cornerX / 100.f);
                cornerYValue = (int) (1440.f * cornerY / 100.f);
            }
            if( rotate == 270 )
            {
                belowRegion = (int) (1440.f * (100.f - rightPercent) / 100.f);
                upRegion = (int) (1440.f * leftPercent / 100.f);
                leftRegion = (int) (1080.f * belowPercent / 100.f);
                rightRegion = (int) (1080.f * (100.f - upPercent) / 100.f);

                cornerXValue = (int) (1080.f * cornerY / 100.f);
                cornerYValue = (int) (1440.f * cornerX / 100.f);
            }

            for (int i = 0; i < number; i++) {
                if( name[i].equals("person") == false ) {

                    //4D
                    if( (rect[i].left < leftRegion && rect[i].top < upRegion && rect[i].right > rightRegion && rect[i].bottom > belowRegion ) )
                    {
                        mObjDisplay[i] = false;
                    }
                    //3D
                    else if( (rect[i].left < leftRegion && rect[i].top < upRegion && rect[i].right > rightRegion && rect[i].bottom < belowRegion ) )
                    {
                        if( rotate == 0 || rotate == 180 ) {
                            mObjDisplay[i] = false;
                        }
                        else
                        {
                            if( (rect[i].centerX() < cornerXValue && rect[i].centerY() < cornerYValue)
                                    || (rect[i].centerX() > 1080 - cornerXValue && rect[i].centerY() < cornerYValue) || rect[i].centerY() < upRegion)
                            {
                                Log.d("below-remove", "3 Region 1 : true");
                                mObjDisplay[i] = false;
                            }
                            else {
                                Log.d("below-remove", "3 Region 1 : false");
                                mObjDisplay[i] = true;
                            }
                        }
                    }
                    else if( (rect[i].left < leftRegion && rect[i].bottom > belowRegion && rect[i].right > rightRegion && rect[i].top > upRegion) )
                    {
                        if( rotate == 0 || rotate == 180 ) {
                            mObjDisplay[i] = false;
                        }
                        else
                        {
                            if( (rect[i].centerX() < cornerXValue && rect[i].centerY() > 1440 - cornerYValue)
                                    || (rect[i].centerX() > 1080 - cornerXValue && rect[i].centerY() > 1440 - cornerYValue) || rect[i].centerY() > belowRegion )
                            {
                                Log.d("below-remove", "1 Region left : true");
                                mObjDisplay[i] = false;
                            }
                            else
                            {
                                Log.d("below-remove", "1 Region left : false");
                                mObjDisplay[i] = true;
                            }
                        }
                    }
                    //check rot
                    else if( (rect[i].top < upRegion && rect[i].left < leftRegion && rect[i].bottom > belowRegion && rect[i].right < rightRegion) )
                    {
                        if( rotate == 0 || rotate == 180 ) {

                            if( (rect[i].centerX() < cornerXValue && rect[i].centerY() < cornerYValue)
                                    || (rect[i].centerX() < cornerXValue && rect[i].centerY() > 1440 - cornerYValue) || rect[i].centerX() < leftRegion )
                            {
                                Log.d("below-remove", "1 Region left : true");
                                mObjDisplay[i] = false;
                            }
                            else
                            {
                                Log.d("below-remove", "1 Region left : false");
                                mObjDisplay[i] = true;
                            }
                        }
                        else
                        {
                            mObjDisplay[i] = false;
                        }
                    }
                    else if( (rect[i].top < upRegion && rect[i].right > rightRegion && rect[i].bottom > belowRegion && rect[i].left > leftRegion) )
                    {
                        if( rotate == 0 || rotate == 180 ) {

                            if( (rect[i].centerX() > 1080 - cornerXValue && rect[i].centerY() < cornerYValue)
                                    || (rect[i].centerX() > 1080 - cornerXValue && rect[i].centerY() > 1440 - cornerYValue) || rect[i].centerX() > rightRegion )
                            {
                                Log.d("below-remove", "1 Region 3D left : true");
                                mObjDisplay[i] = false;
                            }
                            else
                            {
                                Log.d("below-remove", "1 Region 3D left : false");
                                mObjDisplay[i] = true;
                            }
                        }
                        else
                        {
                            mObjDisplay[i] = false;
                        }
                    }
                    //2D
                    else if( rect[i].left < leftRegion && rect[i].top < upRegion && rect[i].right < rightRegion && rect[i].bottom < belowRegion )
                    {
                        Log.d("below-remove", "left top");

                        if( (rect[i].centerX() < cornerXValue && rect[i].centerY() < cornerYValue) || rect[i].centerX() < leftRegion || rect[i].centerY() < upRegion)
                        {
                            Log.d("below-remove", "left top : true");
                            mObjDisplay[i] = false;
                        }
                        else
                        {
                            Log.d("below-remove", "left top : false");
                            mObjDisplay[i] = true;
                        }
                    }
                    else if( rect[i].right > rightRegion && rect[i].top < upRegion && rect[i].left > leftRegion && rect[i].bottom < belowRegion )
                    {
                        Log.d("below-remove", "right top");

                        if( (rect[i].centerX() > (1080.f-cornerXValue) && rect[i].centerY() < cornerYValue) || rect[i].centerX() > rightRegion || rect[i].centerY() < upRegion )
                        {
                            Log.d("below-remove", "right top : true");
                            mObjDisplay[i] = false;
                        }
                        else
                        {
                            Log.d("below-remove", "right top : false");
                            mObjDisplay[i] = true;
                        }
                    }
                    else if( rect[i].left < leftRegion && rect[i].bottom > belowRegion && rect[i].right < rightRegion && rect[i].top > upRegion )
                    {
                        Log.d("below-remove", "left bottom");

                        if( (rect[i].centerX() < cornerXValue && rect[i].centerY() > (1440.f-cornerYValue)) || rect[i].centerX() < leftRegion || rect[i].centerY() > belowRegion )
                        {
                            Log.d("below-remove", "left bottom : true");
                            mObjDisplay[i] = false;
                        }
                        else
                        {
                            Log.d("below-remove", "left bottom : false");
                            mObjDisplay[i] = true;
                        }
                    }
                    else if( rect[i].right > rightRegion && rect[i].bottom > belowRegion && rect[i].left > leftRegion && rect[i].top > upRegion )
                    {
                        Log.d("below-remove", "right bottom");

                        if( (rect[i].centerX() > (1080.f-cornerXValue) && rect[i].centerY() > (1440.f-cornerYValue)) || rect[i].centerX() > rightRegion && rect[i].centerY() > belowRegion )
                        {
                            Log.d("below-remove", "right bottom : true");
                            mObjDisplay[i] = false;
                        }
                        else
                        {
                            Log.d("below-remove", "right bottom : false");
                            mObjDisplay[i] = true;
                        }
                    }
                    //1D
                    else if( rect[i].left < leftRegion && rect[i].top > upRegion && rect[i].right < rightRegion && rect[i].bottom < belowRegion )
                    {
                        Log.d("below-remove", "1 Region left");

                        if( (rect[i].centerX() < cornerXValue && rect[i].centerY() < cornerYValue)
                                || (rect[i].centerX() < cornerXValue && rect[i].centerY() > 1440 - cornerYValue) || rect[i].centerX() < leftRegion )
                        {
                            Log.d("below-remove", "1 Region left : true");
                            mObjDisplay[i] = false;
                        }
                        else
                        {
                            Log.d("below-remove", "1 Region left : false");
                            mObjDisplay[i] = true;
                        }
                    }
                    else if( rect[i].left > leftRegion && rect[i].top < upRegion && rect[i].right < rightRegion && rect[i].bottom < belowRegion )
                    {
                        Log.d("below-remove", "1 Region top");

                        if( (rect[i].centerX() < cornerXValue && rect[i].centerY() < cornerYValue)
                                || (rect[i].centerX() > 1080 - cornerXValue && rect[i].centerY() < cornerYValue) || rect[i].centerY() < upRegion)
                        {
                            Log.d("below-remove", "1 Region left : true");
                            mObjDisplay[i] = false;
                        }
                        else
                        {
                            Log.d("below-remove", "1 Region left : false");
                            mObjDisplay[i] = true;
                        }
                    }
                    else if( rect[i].left > leftRegion && rect[i].top > upRegion && rect[i].right > rightRegion && rect[i].bottom < belowRegion )
                    {
                        Log.d("below-remove", "1 Region right");

                        if( (rect[i].centerX() > 1080 - cornerXValue && rect[i].centerY() < cornerYValue)
                                || (rect[i].centerX() > 1080 - cornerXValue && rect[i].centerY() > 1440 - cornerYValue) || rect[i].centerX() > rightRegion )
                        {
                            Log.d("below-remove", "1 Region left : true");
                            mObjDisplay[i] = false;
                        }
                        else
                        {
                            Log.d("below-remove", "1 Region left : false");
                            mObjDisplay[i] = true;
                        }
                    }
                    else if( rect[i].left > leftRegion && rect[i].top > upRegion && rect[i].right < rightRegion && rect[i].bottom > belowRegion )
                    {
                        Log.d("below-remove", "1 Region bottom");

                        if( (rect[i].centerX() < cornerXValue && rect[i].centerY() > 1440 - cornerYValue)
                                || (rect[i].centerX() > 1080 - cornerXValue && rect[i].centerY() > 1440 - cornerYValue) || rect[i].centerY() > belowRegion )
                        {
                            Log.d("below-remove", "1 Region left : true");
                            mObjDisplay[i] = false;
                        }
                        else
                        {
                            Log.d("below-remove", "1 Region left : false");
                            mObjDisplay[i] = true;
                        }
                    }
                    else
                    {
                        Log.d("below-remove", "good");
                        mObjDisplay[i] = true;
                    }
                }
                else if( name[i].equals("person") == true ) {
                    personCount++;
                }
            }
        }

        int first = -1, second = -1;
        for(int i = 0; i < number; i++)
        {
            if( mObjDisplay[i] == true && name[i].equals("person") == false )
            {
                first = i;
                for(int j = first+1; j < number; j++)
                {
                    if( mObjDisplay[j] == true && name[j].equals("person") == false )
                    {
                        second = j;
                        break;
                    }
                }
                break;
            }
        }

        Log.d("below-remove", "first : "+first+", second : "+second);

        objMiddle[0] = false;

        int count = 0;

        //bug 1
        float[] rectFloat = new float[10 * 4];

        float sizeCheckSmall = SFTunner2.mAIMin;
        float sizeCheckBig = SFTunner2.mAIMax;
        float sizeCheckSmallPerson = SFTunner2.mPersonMin;
        float sizeCheckBigPerson = SFTunner2.mPersonMax;

        for( int personIndex = 0; personIndex < personCount; personIndex++ )
        {
            if (madeSize[personIndex] <= sizeCheckSmallPerson ) {

                mObjDisplay[personIndex] = false;
                personStatus = 2;
                continue;
            }
            else if( sizeCheckBigPerson <= madeSize[personIndex] )
            {
                mObjDisplay[personIndex] = false;
                personStatus = 3;
                continue;
            }
            else
            {
                mObjDisplay[personIndex] = true;

                for (int i = personIndex; i < number; i++) {

                    if( mObjDisplay[i] == true ) {

                        Log.d("tensor-test-jni", "Java reRect[" + i + "] left : " + rect[i].left + ", top : " + rect[i].top + ", right : " + rect[i].right + ", top : " + rect[i].bottom);

                        RectF result = new RectF();
                        transScaleMat.mapRect(result, rect[i]);

                        rectFloat[count++] = result.left;
                        rectFloat[count++] = result.top;
                        rectFloat[count++] = result.right;
                        rectFloat[count++] = result.bottom;
                    }
                }

                lastObjNo[0] = count / 4;
                jniController.updateObjRect(lastObjNo[0], rectFloat);

                personStatus = 1;
                break;
            }
        }

        if( personStatus == 3 )
        {
            //center
            lastObjNo[0] = 1;

            Log.d("tensor-test-middle", "go middle");
            jniController.updateObjRect(0, rectFloat);

            objMiddle[0] = true;

            return 0;
        }
        else if( personStatus == 2 ) {

            if (first != -1 && number > 0) {
                if (name[first].equals("person")) {
                    sizeCheckSmall = SFTunner2.mPersonMin;
                    sizeCheckBig = SFTunner2.mPersonMax;
                }

                Log.d("tensor-test-middle", "sizeCheckSmall : " + sizeCheckSmall + ", sizeCheckBig : " + sizeCheckBig);


                if (madeSize[first] <= sizeCheckSmall || sizeCheckBig <= madeSize[first]) {

                    if (second != -1) {
                        if ((madeSize[second] <= sizeCheckSmall || sizeCheckBig <= madeSize[second])) {
                            //need to test 20180905
                            //mObjectNo = 0;
                            lastObjNo[0] = 1;

                            Log.d("tensor-test-middle", "go middle");
                            jniController.updateObjRect(0, rectFloat);

                            mObjDisplay[first] = false;
                            mObjDisplay[second] = false;

                            objMiddle[0] = true;

                            return 0;
                        } else {

                            mObjDisplay[first] = false;

                            for (int i = second; i < number; i++) {

                                if (mObjDisplay[i] == true) {

                                    Log.d("tensor-test-jni", "Java reRect[" + i + "] left : " + rect[i].left + ", top : " + rect[i].top + ", right : " + rect[i].right + ", top : " + rect[i].bottom);

                                    RectF result = new RectF();
                                    transScaleMat.mapRect(result, rect[i]);

                                    rectFloat[count++] = result.left;
                                    rectFloat[count++] = result.top;
                                    rectFloat[count++] = result.right;
                                    rectFloat[count++] = result.bottom;
                                }
                            }

                            lastObjNo[0] = count / 4;

                            jniController.updateObjRect(lastObjNo[0], rectFloat);
                        }
                    } else {
                        //need to test 20180905
                        //mObjectNo = 0;
                        lastObjNo[0] = 1;

                        Log.d("tensor-test-middle", "go middle");
                        jniController.updateObjRect(0, rectFloat);

                        mObjDisplay[first] = false;

                        objMiddle[0] = true;

                        return 0;
                    }
                } else {
                    for (int i = first; i < number; i++) {

                        if (mObjDisplay[i] == true) {
                            Log.d("tensor-test-jni", "Java reRect[" + i + "] left : " + rect[i].left + ", top : " + rect[i].top + ", right : " + rect[i].right + ", top : " + rect[i].bottom);

                            RectF result = new RectF();
                            transScaleMat.mapRect(result, rect[i]);

                            rectFloat[count++] = result.left;
                            rectFloat[count++] = result.top;
                            rectFloat[count++] = result.right;
                            rectFloat[count++] = result.bottom;
                        }

                        lastObjNo[0] = count / 4;
                    }
                    jniController.updateObjRect(lastObjNo[0], rectFloat);
                }
            } else {
                Log.d("no-obj-test", "no object : "+number);
                //mObjectNo = 0;
                //jniController.updateObjRect(number, rectFloat);
                //mObjMiddle = true;

                lastObjNo[0] = 1;
                jniController.updateObjRect(0, rectFloat);
                objMiddle[0] = true;

                return 0;
            }
            Log.d("tensor-test-jni", "copy speed : " + (new Date().getTime() - startTime.getTime()));
            Log.d("test", "[detectMovingObj-check] mObjectNo : " + lastObjNo[0]);

        }

        int moreCount = detectMoreRect(objMiddle[0], lastObjNo[0], moreRect, rectFloat, rotate);

        if( lastObjNo[0] > 0 ) {
            mLastObjRect[0] = rectFloat[0];
            mLastObjRect[1] = rectFloat[1];
            mLastObjRect[2] = rectFloat[2];
            mLastObjRect[3] = rectFloat[3];
        }

        return moreCount;
    }

    private int detectMoreRect(boolean objMiddle, int objNumber, int moreRect[], float rectFloat[], int rotate)
    {
        //int mMoreRect[] = new int[10];

        float width = ((rectFloat[2] - rectFloat[0]) * 1440.f / 180.f);
        float height = ((rectFloat[3] - rectFloat[1]) * 1080.f / 135.f);
        int a0Size = (int)((width * height) * 100f / (1440.f * 1080.f) );

        int moreRectCount = 0;

        if( objMiddle == false && objNumber > 1 && a0Size < SFTunner2.mAiTouchBoxSize )
        {
            //int mMoreRectCount = 0;
            //mMoreRectCount = 0;

//            float upRate = 1.1f;
//            float downRate = 1.1f;
//            float vertMinRate = 0.7f;
//            float vertMaxRate = 1.3f;

            float upRate = SFTunner2.mAIMultiUpRate;
            float downRate = SFTunner2.mAIMultiDownRate;
            float vertMinRate = SFTunner2.mAIMultiSmallRate;
            float vertMaxRate = SFTunner2.mAIMultiBigRate;

            if( rotate == 0 ) {

                int upLine = (int) (rectFloat[0] * (2.0f - upRate));
                int downLine = (int) (rectFloat[2] * downRate);
                int vertMinLine = (int) ((rectFloat[2] - rectFloat[0]) * vertMinRate);
                int vertMaxLine = (int) ((rectFloat[2] - rectFloat[0]) * vertMaxRate);

                for (int i = 4; i < objNumber * 4; i = i + 4) {
                    //if( upLine < rectFloat[i] && rectFloat[i+2] < downLine )
                    if (upLine < rectFloat[i] + ((rectFloat[i + 2] - rectFloat[i]) / 2) && rectFloat[i] + ((rectFloat[i + 2] - rectFloat[i]) / 2) < downLine) {
                        if (vertMinLine < (rectFloat[i + 2] - rectFloat[i]) && (rectFloat[i + 2] - rectFloat[i]) < vertMaxLine) {
                            moreRect[i / 4 - 1] = i / 4;
                            moreRectCount++;
                        }
                    }
                }

                for (int i = 0; i < moreRectCount; i++) {
                    Log.d("more-rect", "moreRect0[" + i + "] : " + moreRect[i]);
                }

                jniController.updateObjMoreRect(moreRectCount, moreRect);
            }
            else if( rotate == 90 ) {

                int upLine = (int) (rectFloat[1] * (2.0f - downRate));
                int downLine = (int) (rectFloat[3] * upRate);
                int vertMinLine = (int) ((rectFloat[3] - rectFloat[1]) * vertMinRate);
                int vertMaxLine = (int) ((rectFloat[3] - rectFloat[1]) * vertMaxRate);

                for (int i = 4; i < objNumber * 4; i = i + 4) {
                    //if( upLine < rectFloat[i] && rectFloat[i+2] < downLine )
                    if (upLine < rectFloat[i+1] + ((rectFloat[i + 3] - rectFloat[i+1]) / 2) && rectFloat[i+1] + ((rectFloat[i + 3] - rectFloat[i+1]) / 2) < downLine) {
                        if (vertMinLine < (rectFloat[i + 3] - rectFloat[i + 1]) && (rectFloat[i + 3] - rectFloat[i + 1]) < vertMaxLine) {
                            moreRect[i / 4 - 1] = i / 4;
                            moreRectCount++;
                        }
                    }
                }

                for (int i = 0; i < moreRectCount; i++) {
                    Log.d("more-rect", "moreRect90[" + i + "] : " + moreRect[i]);
                }

                jniController.updateObjMoreRect(moreRectCount, moreRect);
            }
            else if( rotate == 180 ) {

                int upLine = (int) (rectFloat[0] * (2.0f - downRate));
                int downLine = (int) (rectFloat[2] * upRate);
                int vertMinLine = (int) ((rectFloat[2] - rectFloat[0]) * vertMinRate);
                int vertMaxLine = (int) ((rectFloat[2] - rectFloat[0]) * vertMaxRate);

                for (int i = 4; i < objNumber * 4; i = i + 4) {
                    //if( upLine < rectFloat[i] && rectFloat[i+2] < downLine )
                    if (upLine < rectFloat[i] + ((rectFloat[i + 2] - rectFloat[i]) / 2) && rectFloat[i] + ((rectFloat[i + 2] - rectFloat[i]) / 2) < downLine) {
                        if (vertMinLine < (rectFloat[i + 2] - rectFloat[i]) && (rectFloat[i + 2] - rectFloat[i]) < vertMaxLine) {
                            moreRect[i / 4 - 1] = i / 4;
                            moreRectCount++;
                        }
                    }
                }

                for (int i = 0; i < moreRectCount; i++) {
                    Log.d("more-rect", "moreRect0[" + i + "] : " + moreRect[i]);
                }

                jniController.updateObjMoreRect(moreRectCount, moreRect);
            }
            else if( rotate == 270 ) {

                int upLine = (int) (rectFloat[1] * (2.0f - upRate));
                int downLine = (int) (rectFloat[3] * downRate);
                int vertMinLine = (int) ((rectFloat[3] - rectFloat[1]) * vertMinRate);
                int vertMaxLine = (int) ((rectFloat[3] - rectFloat[1]) * vertMaxRate);

                for (int i = 4; i < objNumber * 4; i = i + 4) {
                    //if( upLine < rectFloat[i] && rectFloat[i+2] < downLine )
                    if (upLine < rectFloat[i+1] + ((rectFloat[i + 3] - rectFloat[i+1]) / 2) && rectFloat[i+1] + ((rectFloat[i + 3] - rectFloat[i+1]) / 2) < downLine) {
                        if (vertMinLine < (rectFloat[i + 3] - rectFloat[i + 1]) && (rectFloat[i + 3] - rectFloat[i + 1]) < vertMaxLine) {
                            moreRect[i / 4 - 1] = i / 4;
                            moreRectCount++;
                        }
                    }
                }

                for (int i = 0; i < moreRectCount; i++) {
                    Log.d("more-rect", "moreRect90[" + i + "] : " + moreRect[i]);
                }

                jniController.updateObjMoreRect(moreRectCount, moreRect);
            }
        }
        else
        {
            moreRectCount = 0;
            jniController.updateObjMoreRect(moreRectCount, moreRect);
        }

        return moreRectCount;
    }
}
