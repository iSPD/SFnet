package com.ispd.sfcam.utils;

import android.os.SystemClock;

import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.drawView.drawViewer;
import com.ispd.sfcam.jniController;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.graphics.Canvas.EdgeType.AA;
import static org.opencv.core.Core.absdiff;
import static org.opencv.core.Core.add;
import static org.opencv.core.Core.countNonZero;
import static org.opencv.core.Core.flip;
import static org.opencv.core.Core.mean;
import static org.opencv.core.Core.split;
import static org.opencv.core.Core.subtract;
import static org.opencv.core.Core.sumElems;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.COLOR_RGBA2GRAY;
import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_COMPLEX_SMALL;
import static org.opencv.imgproc.Imgproc.INTER_NEAREST;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY_INV;
import static org.opencv.imgproc.Imgproc.THRESH_TOZERO_INV;
import static org.opencv.imgproc.Imgproc.blur;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.putText;
import static org.opencv.imgproc.Imgproc.threshold;
import static org.opencv.imgproc.Imgproc.warpAffine;

public class movingChecker {

    private static final String TAG = "movingChecker";

    private static int mSmaller = 8;
    private static byte[] mYuvBytes;
    private static byte[] mResizeYuvByte;
    private static Mat mPreviewMat = new Mat(aiCamParameters.PREVIEW_HEIGHT_I / mSmaller, aiCamParameters.PREVIEW_WIDTH_I / mSmaller, CV_8UC1);
    private static Mat mZeroMat = Mat.zeros(aiCamParameters.PREVIEW_HEIGHT_I / mSmaller, aiCamParameters.PREVIEW_WIDTH_I / mSmaller, CV_8UC1);
    private static Mat mPreMat = Mat.zeros(aiCamParameters.PREVIEW_HEIGHT_I / mSmaller, aiCamParameters.PREVIEW_WIDTH_I / mSmaller, CV_8UC1);
    private static Mat mPreSegMat = Mat.zeros(aiCamParameters.PREVIEW_HEIGHT_I / mSmaller, aiCamParameters.PREVIEW_WIDTH_I / mSmaller, CV_8UC1);
    private static Mat mDiffMat = new Mat();
    private static Mat mSegmentMat = Mat.ones(aiCamParameters.PREVIEW_HEIGHT_I / mSmaller, aiCamParameters.PREVIEW_WIDTH_I / mSmaller, CV_8UC1);
    private static boolean mMoveRunning = false;
    private static boolean mMoveUIRunning = false;
    private static boolean mMoveCheckEnd = true;
    private static boolean mGammaCheckEnd = true;
    private static float thresholdValue = 2.5f;
    private static int thresholdSensValue = 15;
    private static float mPercentNoZero = 0.0f;

    private static Mat mTemp = new Mat();

    private static Mat mErodeMat = new Mat();
    private static Mat mYvalueMat = new Mat();

    private static double mMeanEntireValue = 0.0;
    private static double mMeanHumanValue = 0.0;
    private static double mMeanBackValue = 0.0;
    private static double mMeanFaceValue = 0.0;
    private static double mCurrFastValue = -1.0;
    private static float mFastCheckValue = -1.0f;
    private static float mFaceNSegValue = -1.0f;

    private static float mSegmentEntire;

    private static long startUITime = 0;

    private static boolean mTouchOn = false;
    private static boolean mObjStatus = true;

    private static Mat mCopySegmentMat = new Mat();

    public static void getMeanValue(double[] values)
    {
        values[0] = mMeanEntireValue;
        values[1] = mMeanHumanValue;
        values[2] = mMeanBackValue;
        values[3] = mMeanFaceValue;
        values[4] = gammaManager.getCurrentValue();
        values[5] = mCurrFastValue;
        values[6] = (double)mFastCheckValue;
        values[7] = (double)mFaceNSegValue;
    }

    private static void resizeByte(final byte[] yuvBytes)
    {
        int previewWidth = aiCamParameters.PREVIEW_WIDTH_I;
        int previewHeight = aiCamParameters.PREVIEW_HEIGHT_I;

        if (mYuvBytes == null) {
            mYuvBytes = new byte[yuvBytes.length];
        }

        if( mResizeYuvByte == null )
        {
            //mResizeYuvByte = new byte[previewWidth * (previewHeight * 3 / 2) / (mSizeSkipX * mSizeSkipY)];
            mResizeYuvByte = new byte[previewWidth / mSmaller * previewHeight / mSmaller];
        }

//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {

        System.arraycopy(yuvBytes, 0, mYuvBytes, 0, yuvBytes.length);

        //test...
        int fillCount = 0;
        Date startTime = new Date();
        //for(int i = 0; i < previewHeight * 3 / 2; i = i + smaller)
        for(int i = 0; i < previewHeight; i = i + mSmaller)
        {
            if( i < previewHeight ) {
                for (int j = 0; j < previewWidth; j = j + mSmaller) {
                    mResizeYuvByte[fillCount++] = mYuvBytes[i * previewWidth + j];
                }
            }
//            else
//            {
//                for (int j = 0; j < previewWidth; j = j + (smaller * 2)) {
//                    mResizeYuvByte[fillCount++] = mYuvBytes[i * previewWidth + j];
//                    mResizeYuvByte[fillCount++] = mYuvBytes[i * previewWidth + j + 1];
//                }
//            }
        }
//            }
//        });
//        thread.start();
    }

    private static byte saturate(double val) {
        int iVal = (int) Math.round(val);
        iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
        return (byte) iVal;
    }

    private static Mat setGammaCorrection(Mat matImgSrc, double gammaValue)
    {
        Mat lookUpTable = new Mat(1, 256, CvType.CV_8U);
        byte[] lookUpTableData = new byte[(int) (lookUpTable.total()*lookUpTable.channels())];
        for (int i = 0; i < lookUpTable.cols(); i++) {
            lookUpTableData[i] = saturate(Math.pow(i / 255.0, 1.0 / gammaValue) * 255.0);
        }
        lookUpTable.put(0, 0, lookUpTableData);
        Mat img = new Mat();
        Core.LUT(matImgSrc, lookUpTable, img);

        //imwrite("/sdcard/obj/gammaB.jpg", matImgSrc);
        //imwrite("/sdcard/obj/gammaA.jpg", img);

        return img;
    }

    public static Date lastTime = new Date();
    // lastTime은 기준 시간입니다.
    // 처음 생성당시의 시간을 기준으로 그 다음 1초가 지날때마다 갱신됩니다.
    public static long frameCount = 0, nowFps = 0;
    // frameCount는 프레임마다 갱신되는 값입니다.
    // nowFps는 1초마다 갱신되는 값입니다.

    public static void count(){
        Date nowTime = new Date();
        long diffTime = nowTime.getTime() - lastTime.getTime();
        // 기준시간 으로부터 몇 초가 지났는지 계산합니다.

        if (diffTime >= 1000) {
            // 기준 시간으로 부터 1초가 지났다면
            nowFps = frameCount;
            Log.d("nowFps", "moving nowFps : "+nowFps);

            frameCount = 0;
            // nowFps를 갱신하고 카운팅을 0부터 다시합니다.
            lastTime = nowTime;
            // 1초가 지났으므로 기준 시간또한 갱신합니다.
        }

        frameCount++;
        // 기준 시간으로 부터 1초가 안지났다면 카운트만 1 올리고 넘깁니다.
    }

    private static Mat shaderPreviewMat = new Mat();

    public static void setShaderPreviewMat(Mat inMat)
    {
        shaderPreviewMat = inMat.clone();
    }

    public static void checkMoving(byte[] data, Mat segmentMat, int []faceRect, Mat objSegmentMat, int rotate)
    {
//        if( mMoveRunning == true )
//        {
//            thresholdValue = 5.0f;
//        }
//        else
//        {
//            thresholdValue = 2.5f;
//        }

        if( segmentMat == null )
        {
            Log.d(TAG, "[Moving-Status] Obj Process");
            mObjStatus = true;

            SFTunner2.getObjMovingValue();
            int sens = SFTunner2.mObjMovingSens;
            int value;
            if( mTouchOn == true )
            {
                value = SFTunner2.mObjMovingTouchValue;
            }
            else {
                value = SFTunner2.mObjMovingValue;
            }
            Log.d(TAG, "Obj Moving sens : "+sens+", value : "+value);

            thresholdValue = value;
            thresholdSensValue = sens;
        }
        else {
            mObjStatus = false;

            if( aiCamParameters.mMovieRunning == true )
            {
                //thresholdValue = SFTunner.mSFCommonTune.mMovingMovieThreshold;
                thresholdValue = SFTunner.mSFCommonTune.mMovingThreshold;
            }
            else {
                thresholdValue = SFTunner.mSFCommonTune.mMovingThreshold;
            }
            thresholdSensValue = SFTunner.mSFCommonTune.mMovingSens;
        }

        if( mMoveCheckEnd == false ) {
            return;
        }

        count();

        mMoveCheckEnd = false;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final long startTime = SystemClock.uptimeMillis();

                if( data != null ) {
                    resizeByte(data);
                    mPreviewMat.put(0, 0, mResizeYuvByte);
                }
                else
                {
                    mPreviewMat = shaderPreviewMat.clone();
                }
//                imwrite("/sdcard/mPreviewMat.jpg", mPreviewMat);

                Mat sumMat = new Mat();

                //if( segmentMat != null )
                if( true )
                {
                    Log.d(TAG, "segmentMat");

                    if( segmentMat != null ) {
                        Log.d(TAG, "[Moving-Moving] segmentMat");

                        segmentMat.convertTo(mSegmentMat, CvType.CV_8UC1);
                        Mat matRotation = Imgproc.getRotationMatrix2D(new Point(mSegmentMat.cols() / 2, mSegmentMat.rows() / 2), rotate + 90, 1);
                        warpAffine(mSegmentMat, mSegmentMat, matRotation, mSegmentMat.size(), INTER_NEAREST);

                        if (aiCamParameters.mCameraLocationInt == 1) {
                            flip(mSegmentMat, mSegmentMat, 90);
                        }

                        Imgproc.resize(mSegmentMat, mSegmentMat, new Size(1440 / mSmaller, 1080 / mSmaller), 0, 0, INTER_NEAREST);

                        Imgproc.threshold(mSegmentMat, mSegmentMat, 15, 0, THRESH_TOZERO_INV);
                        Imgproc.threshold(mSegmentMat, mSegmentMat, 14, 255, THRESH_BINARY_INV);

                        mCopySegmentMat = mSegmentMat.clone();
                        //setFastTrack(mSegmentMat);
                    }
                    else
                    {
                        if( objSegmentMat != null ) {
                            if( objSegmentMat.empty() == false ) {

                                Log.d(TAG, "[Moving-Moving] objSegmentMat");

                                Mat temp = new Mat();
                                //Imgproc.resize(objSegmentMat, temp, new Size(1440 / mSmaller, 1080 / mSmaller), 0, 0, INTER_NEAREST);
                                Imgproc.resize(objSegmentMat, temp, new Size(aiCamParameters.PREVIEW_WIDTH_I / mSmaller, aiCamParameters.PREVIEW_HEIGHT_I / mSmaller), 0, 0, INTER_NEAREST);
                                List<Mat> rgba = new ArrayList<Mat>();
                                split(temp, rgba);
                                mSegmentMat = rgba.get(1).clone();

                                threshold(mSegmentMat, mSegmentMat, 35, 255, THRESH_BINARY_INV);
                            }
                        }
                    }

//                    //y-value S
//                    //mPreviewMat = setGammaCorrection(mPreviewMat, 1.4);
//
////                    Scalar maenEntire = mean(mPreviewMat);
////                    mMeanEntireValue = maenEntire.val[0];
////                    Log.d(TAG, "[y-Value] maenEntireValue : "+mMeanEntireValue);
////                    //여기까지 전체 밝기 평균
//
//                    int noZeroEntireY = countNonZero(mSegmentMat);
//                    mYvalueMat = mPreviewMat.clone();
//                    mZeroMat.copyTo(mYvalueMat, mSegmentMat);
//                    Scalar sumHumanValue = sumElems(mYvalueMat);
//                    mMeanHumanValue = sumHumanValue.val[0] / (mYvalueMat.cols() * mYvalueMat.rows()-noZeroEntireY);
//                    Log.d(TAG, "[y-Value] meanHumanValue : "+mMeanHumanValue);
//                    //여기 까지 사람만 밝기 평균
//
////                    Mat segmentPerson = mSegmentMat.clone();
////                    Imgproc.threshold(segmentPerson, segmentPerson, 200, 255, THRESH_BINARY_INV);
////
////                    Mat copyPreview = mPreviewMat.clone();
////                    mZeroMat.copyTo(copyPreview, segmentPerson);
////                    //imwrite("/sdcard/obj/copyPreview.jpg", copyPreview);
////
////                    Scalar sumBackValue = sumElems(copyPreview);
////                    mMeanBackValue = sumBackValue.val[0] / noZeroEntireY;
////                    Log.d(TAG, "[y-Value] mMeanBackValue : "+mMeanBackValue);
////                    //여기 까지 배경만 밝기 평균
//
//                    long startGammaTime = SystemClock.uptimeMillis();
//                    //if( faceRect[0] > 0 ) {
//                    //if( true ) {
//                    if( noZeroEntireY < (mSegmentMat.cols() * mSegmentMat.rows()) ) {
//                        //face rect 위치 카메라 위치에 따라 잘 맞춰주기 해라
//
//                        Mat faceRoiMat = new Mat();
//
//                        Mat copyPreview2 = mPreviewMat.clone();
//                        int x, y, w, h;
//                        if( faceRect[0] > 0 ) {
//                            x = faceRect[1] / mSmaller;
//                            y = faceRect[3] / mSmaller;
//                            w = faceRect[2] / mSmaller - x;
//                            h = faceRect[4] / mSmaller - y;
//
//                            Log.d(TAG, "face : "+x+", "+y+", "+w+", "+h);
//
//                            if( x + w > 1440 / 8 ) {
//                                Log.d(TAG, "face : 1440 / 8");
//                                w = 1440 / 8 - x - 1;
//                            }
//                            if( y + h > 1080 / 8 ) {
//                                Log.d(TAG, "face : 1080 / 8");
//                                h = 1080 / 8 - y - 1;
//                            }
//
//                            Rect roiRect = new Rect(x, y, w, h);
//                            faceRoiMat = new Mat(copyPreview2, roiRect);
//                            //imwrite("/sdcard/obj/faceRoiMat.jpg", faceRoiMat);
//                            Scalar maenFace = mean(faceRoiMat);
//                            mMeanFaceValue = maenFace.val[0];
//                            //여기까지 얼굴만 밝기 평균
//                        }
//                        else
//                        {
////                            x = 0;
////                            y = 0;
////                            w = mSegmentMat.cols();
////                            h = mSegmentMat.rows();
//
//                            faceRoiMat = mYvalueMat.clone();
//                            mMeanFaceValue = mMeanHumanValue;
//
//                            //imwrite("/sdcard/human/faceRoiMat.jpg", faceRoiMat);
//                        }
//
//                        Log.d("tuneMean", "faceRect : "+faceRect[0]);
//                        Log.d("tuneMean", "Before mMeanFaceValue : "+mMeanFaceValue);
//                        Log.d("tuneMean", "SFTunner.mSFCommonTune.mTargetGamma : "+SFTunner.mSFCommonTune.mTargetGamma);
//
//                        if( mMeanFaceValue <  (double)SFTunner.mSFCommonTune.mTargetGamma) {
//
//                            double processMeanValue = mMeanFaceValue;
//                            double tuneMean = (double) SFTunner.mSFCommonTune.mTargetGamma;
//                            double whatGamma = 1.0;
//                            double addGamma = 0.1;
//                            while (true) {
//
////                                if( mObjStatus == true )// || processMeanValue < 0.01)
////                                {
////                                    Log.d(TAG, "[Moving-Status] Obj Process(Thread)");
////                                    break;
////                                }
//
//                                if (tuneMean - 10.0 < processMeanValue && processMeanValue < tuneMean + 10.0) {
//                                    Log.d("tuneMean", "Detect Correct Gamma... : " + processMeanValue + ", whatGamma : " + whatGamma);
//                                    break;
//                                } else {
//                                    if (whatGamma > 15.0) {
//                                        Log.d("tuneMean", "whatGamma > 2.0");
//                                        addGamma = -Math.abs(addGamma);
//                                    } else if (whatGamma < 0.01) {
//                                        Log.d("tuneMean", "whatGamma < 0.0");
//                                        addGamma = Math.abs(addGamma);
//                                    }
//
//                                    Log.d("tuneMean", "addGamma : " + addGamma);
//
//                                    whatGamma = whatGamma + addGamma;
//                                    Log.d("tuneMean", "whatGamma : " + whatGamma);
//
//                                    if( faceRect[0] > 0 ) {
//                                        Mat faceRoiMatTemp = setGammaCorrection(faceRoiMat, whatGamma);
//                                        Scalar maenFaceTemp = mean(faceRoiMatTemp);
//                                        processMeanValue = maenFaceTemp.val[0];
//                                    }
//                                    else
//                                    {
//                                        Mat faceRoiMatTemp = setGammaCorrection(faceRoiMat, whatGamma);
//                                        //imwrite("/sdcard/human/faceRoiMatTemp.jpg", faceRoiMatTemp);
//
//                                        mZeroMat.copyTo(faceRoiMatTemp, mSegmentMat);
//                                        //imwrite("/sdcard/human/faceRoiMatTemp2.jpg", faceRoiMatTemp);
//
//                                        Scalar sumValue = sumElems(faceRoiMatTemp);
//                                        processMeanValue = sumValue.val[0] / (faceRoiMat.cols() * faceRoiMat.rows()-noZeroEntireY);
//                                    }
//                                    Log.d("tuneMean", "processMeanValue : " + processMeanValue);
//                                }
//                            }
//
//                            Log.d("tuneMean", "mSFCommonTune.mMaxGamma : "+(double) SFTunner.mSFCommonTune.mMaxGamma);
//                            if (whatGamma > (double) SFTunner.mSFCommonTune.mMaxGamma) {
//                                whatGamma = (double) SFTunner.mSFCommonTune.mMaxGamma;
//                            }
//
//                            gammaManager.setCurrentValue(whatGamma);
//                            //mPreviewMat = setGammaCorrection(mPreviewMat, whatGamma);
//                        }
//                        else
//                        {
//                            Log.d("tuneMean", "default mMeanFaceValue : " + mMeanFaceValue);
//                            gammaManager.setCurrentValue(1.0);
//                        }
//                    }
//                    Log.o(TAG, "[time-check] gamma time " + (SystemClock.uptimeMillis()-startGammaTime));
//                    //y-value E

                    setGammaCalc(faceRect);

                    //imwrite("/sdcard/obj/mSegmentMat1.jpg", mSegmentMat);
                    if( segmentMat != null ) {
                        int movingArea = (int) SFTunner.mSFCommonTune.mMovingArea;
                        Log.d(TAG, "[test-tune] mMovingArea : " + movingArea);

                        if (movingArea > 0) {
                            erode(mSegmentMat, mSegmentMat, new Mat(), new Point(-1, -1), movingArea);
                        } else {
                            dilate(mSegmentMat, mSegmentMat, new Mat(), new Point(-1, -1), -movingArea);
                        }
                    }

                    //imwrite("/sdcard/obj/mSegmentMat2.jpg", mSegmentMat);
                    //imwrite("/sdcard/obj/mPreSegMat.jpg", mPreSegMat);
                    add(mSegmentMat, mPreSegMat, sumMat);

//                    mErodeMat = mPreviewMat.clone();
//                    mZeroMat.copyTo(mErodeMat, sumMat);
                    //imwrite("/sdcard/obj/mErodeMat.jpg", mErodeMat);
                }
                else
                {
                    Log.d(TAG, "segmentMat : null");
                    sumMat = mSegmentMat.clone();
                    //mMoveCheckEnd = true;
                    //return;
                }

                //imwrite("/sdcard/obj/mSegmentMatPreSeg.jpg", mPreSegMat);
                //imwrite("/sdcard/obj/mSegmentMatSum.jpg", sumMat);

                Log.d(TAG, "moving exception : "+countNonZero(mSegmentMat)+", "+countNonZero(sumMat));
                if( countNonZero(mSegmentMat) > 0 )
                {
                    if(countNonZero(sumMat) == sumMat.cols() * sumMat.rows() )
                    {
                        Log.d(TAG, "moving exception");
                        sumMat = mSegmentMat.clone();
                    }
                }

                if( segmentMat != null ) {
                    int movingBlur = (int) SFTunner.mSFCommonTune.mMovingBlur;
                    //mPreviewMat = setGammaCorrection(mPreviewMat, 0.4);
                    blur(mPreviewMat, mPreviewMat, new Size(movingBlur, movingBlur));
                }
                else
                {
                    //물체 무빙 블러 가져다 쓰세요...
                    blur(mPreviewMat, mPreviewMat, new Size(5, 5));
                }

                absdiff(mPreviewMat, mPreMat, mDiffMat);
//                mPreMat = mPreviewMat.clone();

                threshold(mDiffMat, mDiffMat, thresholdSensValue, 255, THRESH_BINARY);
                //imwrite("/sdcard/obj/mPreviewMat.jpg", mPreviewMat);
                //imwrite("/sdcard/obj/mSegmentMat.jpg", mSegmentMat);
                //imwrite("/sdcard/obj/mPreMat.jpg", mPreMat);
                //imwrite("/sdcard/obj/mDiffMat.jpg", mDiffMat);

                //imwrite("/sdcard/obj/mDiffMatB.jpg", mDiffMat);
                //if( segmentMat != null ) {
                if( true ) {
                    mZeroMat.copyTo(mDiffMat, sumMat);
                }
                //imwrite("/sdcard/obj/mSegmentMatDiff.jpg", mDiffMat);
//                imwrite("/sdcard/moving/sumMat.jpg", sumMat);
//                imwrite("/sdcard/moving/mDiffMat.jpg", mDiffMat);

                Mat previewGrayMat = mPreviewMat.clone();
                mDiffMat.copyTo(previewGrayMat, mDiffMat);
                sumMat.copyTo(previewGrayMat, sumMat);
                //imwrite("/sdcard/moving/previewGrayMat.jpg", previewGrayMat);
                drawViewer.setMovingBitmap(previewGrayMat);

                //이거 안쓰나 그냥 테스트 용도 인가봐
//                mTemp = mPreviewMat.clone();
//                mDiffMat.copyTo(mTemp, mDiffMat);
                //imwrite("/sdcard/obj/mTemp.jpg", mTemp);

                //imwrite("/sdcard/obj/mDiffMatA.jpg", mDiffMat);

//                int noZero = countNonZero(mDiffMat);
//                mPercentNoZero = ((float) noZero / (float) (mDiffMat.cols() * mDiffMat.rows())) * 100.0f;

                int noZeroEntireM = 0;
                //if( segmentMat != null ) {
                if( true ) {
                    noZeroEntireM = countNonZero(sumMat);
                }
                int noZero = countNonZero(mDiffMat);
                Log.d(TAG, "Entire : "+(mDiffMat.cols() * mDiffMat.rows())+", NoSeg : "+noZeroEntireM+", Entire - NoSeg : "+(mDiffMat.cols() * mDiffMat.rows()-noZeroEntireM)+", noZero : "+noZero);

                mSegmentEntire = (float) (mDiffMat.cols() * mDiffMat.rows() - noZeroEntireM );

//                if( mSegmentEntire == 0.0f )
//                {
//                    Log.o(TAG, "[Moving-Check2] mSegmentEntire == 0.0f to 1.0f");
//                    //mSegmentEntire = 1.0f;
//                    return;
//                }

                if( mSegmentEntire != 0.0f ) {
                    mPercentNoZero = ((float) noZero / mSegmentEntire) * 100.0f;

                    Log.o(TAG, "[Moving-Check2] percentNoZero : " + mPercentNoZero + "(" + thresholdValue + ")");
                    mPreMat = mPreviewMat.clone();
                    mPreSegMat = mSegmentMat.clone();

                    if( mObjStatus == false && aiCamParameters.mCameraLocationInt == 0 ) {
                        thresholdValue = getMovingValueBySize(thresholdValue, mCopySegmentMat);
                        mFastCheckValue = thresholdValue;
                    }

                    if (mPercentNoZero > thresholdValue) {
                        mMoveRunning = true;

                        mMoveUIRunning = true;
                        startUITime = SystemClock.uptimeMillis();
                    } else {
                        mMoveRunning = false;

                        if( SystemClock.uptimeMillis() - startUITime > 500)
                        {
                            mMoveUIRunning = false;
                        }
                    }
                }
                else
                {
                    Log.o(TAG, "[Moving-Check2] mSegmentEntire == 0.0f");
                    mMoveRunning = true;
                }

                mMoveCheckEnd = true;

                Log.o(TAG, "[time-check] moving time " + (SystemClock.uptimeMillis()-startTime));
            }
        });
        thread.start();
    }

    private static void setGammaCalc(int []faceRect)
    {
        Mat previewMat = mPreviewMat.clone();
        Mat segmentMat = mSegmentMat.clone();

        if( mGammaCheckEnd == false ) {
            return;
        }

        mGammaCheckEnd = false;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //y-value S
                //previewMat = setGammaCorrection(previewMat, 1.4);

//                    Scalar maenEntire = mean(previewMat);
//                    mMeanEntireValue = maenEntire.val[0];
//                    Log.d(TAG, "[y-Value] maenEntireValue : "+mMeanEntireValue);
//                    //여기까지 전체 밝기 평균

                int noZeroEntireY = countNonZero(segmentMat);
                mYvalueMat = previewMat.clone();
                mZeroMat.copyTo(mYvalueMat, segmentMat);
                Scalar sumHumanValue = sumElems(mYvalueMat);
                mMeanHumanValue = sumHumanValue.val[0] / (mYvalueMat.cols() * mYvalueMat.rows()-noZeroEntireY);
                Log.d(TAG, "[y-Value] meanHumanValue : "+mMeanHumanValue);
                //여기 까지 사람만 밝기 평균

//                    Mat segmentPerson = segmentMat.clone();
//                    Imgproc.threshold(segmentPerson, segmentPerson, 200, 255, THRESH_BINARY_INV);
//
//                    Mat copyPreview = previewMat.clone();
//                    mZeroMat.copyTo(copyPreview, segmentPerson);
//                    //imwrite("/sdcard/obj/copyPreview.jpg", copyPreview);
//
//                    Scalar sumBackValue = sumElems(copyPreview);
//                    mMeanBackValue = sumBackValue.val[0] / noZeroEntireY;
//                    Log.d(TAG, "[y-Value] mMeanBackValue : "+mMeanBackValue);
//                    //여기 까지 배경만 밝기 평균

                long startGammaTime = SystemClock.uptimeMillis();
                //if( faceRect[0] > 0 ) {
                //if( true ) {
                if( noZeroEntireY < (segmentMat.cols() * segmentMat.rows()) ) {
                    //face rect 위치 카메라 위치에 따라 잘 맞춰주기 해라

                    Mat faceRoiMat = new Mat();

                    Mat copyPreview2 = previewMat.clone();
                    int x, y, w, h;
                    if( faceRect[0] > 0 ) {
                        x = faceRect[1] / mSmaller;
                        y = faceRect[3] / mSmaller;
                        w = faceRect[2] / mSmaller - x;
                        h = faceRect[4] / mSmaller - y;

                        Log.d(TAG, "face : "+x+", "+y+", "+w+", "+h);

                        if( x + w > 1440 / 8 ) {
                            Log.d(TAG, "face : 1440 / 8");
                            w = 1440 / 8 - x - 1;
                        }
                        if( y + h > 1080 / 8 ) {
                            Log.d(TAG, "face : 1080 / 8");
                            h = 1080 / 8 - y - 1;
                        }

                        Rect roiRect = new Rect(x, y, w, h);
                        faceRoiMat = new Mat(copyPreview2, roiRect);
                        //imwrite("/sdcard/obj/faceRoiMat.jpg", faceRoiMat);
                        Scalar maenFace = mean(faceRoiMat);
                        mMeanFaceValue = maenFace.val[0];
                        //여기까지 얼굴만 밝기 평균
                    }
                    else
                    {
//                            x = 0;
//                            y = 0;
//                            w = segmentMat.cols();
//                            h = segmentMat.rows();

                        faceRoiMat = mYvalueMat.clone();
                        mMeanFaceValue = mMeanHumanValue;

                        //imwrite("/sdcard/human/faceRoiMat.jpg", faceRoiMat);
                    }

                    Log.d("tuneMean", "faceRect : "+faceRect[0]);
                    Log.d("tuneMean", "Before mMeanFaceValue : "+mMeanFaceValue);
                    Log.d("tuneMean", "SFTunner.mSFCommonTune.mTargetGamma : "+SFTunner.mSFCommonTune.mTargetGamma);

                    if( mMeanFaceValue <  (double)SFTunner.mSFCommonTune.mTargetGamma) {

                        double processMeanValue = mMeanFaceValue;
                        double tuneMean = (double) SFTunner.mSFCommonTune.mTargetGamma;
                        double whatGamma = 1.0;
                        double addGamma = 0.1;
                        while (true) {

//                                if( mObjStatus == true )// || processMeanValue < 0.01)
//                                {
//                                    Log.d(TAG, "[Moving-Status] Obj Process(Thread)");
//                                    break;
//                                }

                            if (tuneMean - 10.0 < processMeanValue && processMeanValue < tuneMean + 10.0) {
                                Log.d("tuneMean", "Detect Correct Gamma... : " + processMeanValue + ", whatGamma : " + whatGamma);
                                break;
                            } else {
                                if (whatGamma > 15.0) {
                                    Log.d("tuneMean", "whatGamma > 2.0");
                                    addGamma = -Math.abs(addGamma);
                                } else if (whatGamma < 0.01) {
                                    Log.d("tuneMean", "whatGamma < 0.0");
                                    addGamma = Math.abs(addGamma);
                                }

                                Log.d("tuneMean", "addGamma : " + addGamma);

                                whatGamma = whatGamma + addGamma;
                                Log.d("tuneMean", "whatGamma : " + whatGamma);

                                if( faceRect[0] > 0 ) {
                                    Mat faceRoiMatTemp = setGammaCorrection(faceRoiMat, whatGamma);
                                    Scalar maenFaceTemp = mean(faceRoiMatTemp);
                                    processMeanValue = maenFaceTemp.val[0];
                                }
                                else
                                {
                                    Mat faceRoiMatTemp = setGammaCorrection(faceRoiMat, whatGamma);
                                    //imwrite("/sdcard/human/faceRoiMatTemp.jpg", faceRoiMatTemp);

                                    mZeroMat.copyTo(faceRoiMatTemp, segmentMat);
                                    //imwrite("/sdcard/human/faceRoiMatTemp2.jpg", faceRoiMatTemp);

                                    Scalar sumValue = sumElems(faceRoiMatTemp);
                                    processMeanValue = sumValue.val[0] / (faceRoiMat.cols() * faceRoiMat.rows()-noZeroEntireY);
                                }
                                Log.d("tuneMean", "processMeanValue : " + processMeanValue);
                            }
                        }

                        Log.d("tuneMean", "mSFCommonTune.mMaxGamma : "+(double) SFTunner.mSFCommonTune.mMaxGamma);
                        if (whatGamma > (double) SFTunner.mSFCommonTune.mMaxGamma) {
                            whatGamma = (double) SFTunner.mSFCommonTune.mMaxGamma;
                        }

                        gammaManager.setCurrentValue(whatGamma);
                        //previewMat = setGammaCorrection(previewMat, whatGamma);
                    }
                    else
                    {
                        Log.d("tuneMean", "default mMeanFaceValue : " + mMeanFaceValue);
                        gammaManager.setCurrentValue(1.0);
                    }
                }

                mGammaCheckEnd = true;
                Log.o(TAG, "[time-check] gamma time " + (SystemClock.uptimeMillis()-startGammaTime));
                //y-value E
            }
        });
        thread.start();
    }

    private static float getMovingValueBySize(float thresholdValue, Mat segMat)
    {
        Mat segCopyMat = new Mat();
        threshold(segMat, segCopyMat, 200, 255, THRESH_BINARY_INV);

        int segCount = countNonZero(segCopyMat);
        float segPercent = (float)segCount * 100.f / (float)(segCopyMat.cols() * segCopyMat.rows());
        mFaceNSegValue = segPercent;
        Log.d(TAG, "[segPercent] segPercent : "+segPercent);

        for(int i = 0; i < 5; i++) {
            Log.d(TAG, "[segPersonPercent] SFTunner.movingTuneValues["+i+"] : "+SFTunner.movingTuneValues[i]);
        }

//        float minFace = 8.f;
//        float maxFace = 25.f;
//        float minPercent = 50.f;
//        float maxPercent = 100.f;

        float minFace = SFTunner.movingTuneValues[1];
        float maxFace = SFTunner.movingTuneValues[2];
        float minPercent = SFTunner.movingTuneValues[3];
        float maxPercent = SFTunner.movingTuneValues[4];

        float calcA = (maxPercent - minPercent) / (maxFace - minFace);
        float calcB = minPercent - (calcA * minFace);

        float usePercent = calcA * segPercent + calcB;
        Log.d(TAG, "[segPercent] usePercent : "+usePercent);

        if(usePercent > maxPercent) usePercent = maxPercent;
        else if(usePercent < minPercent) usePercent = minPercent;

        thresholdValue = thresholdValue  * usePercent / 100.f;

        Log.d(TAG, "[segPercent] thresholdValue : "+thresholdValue);
        return thresholdValue;
    }

    public static void resetMoving()
    {
        mMoveRunning = true;
        mMoveUIRunning = true;
    }

    public static boolean getMovingRunning()
    {
        return mMoveRunning;
    }

    public static boolean getUIMovingRunning()
    {
        return mMoveUIRunning;
    }

    public static float getMovingValue()
    {
        return mPercentNoZero;
    }

    public static void setTouchOn(float x, float y)
    {
        if (x > -1.0f && y > -1.0f) {
            mTouchOn = true;
        }
        else
        {
            mTouchOn = false;
        }
    }

    public static boolean isHumanRunning()
    {
        if( mSegmentEntire != 0.0f )
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private static int mFaceAlgFlag = 0;

    public static int getFastAlgFlag()
    {
        return mFaceAlgFlag;
    }

    public static void setFastAlgFlag(int mode)
    {
        mFaceAlgFlag = mode;
    }

    private static Mat mCopySegmentPreMat = null;
    private static float mPreZoCount = -1;
    private static boolean mFastTrackOn = false;

    private static long mStartCheckTime = SystemClock.uptimeMillis();

    private static void setFastTrack(Mat input)
    {
        Mat currSegMat = new Mat();

        threshold(input, currSegMat, 200, 255, THRESH_BINARY_INV);

        if( mCopySegmentPreMat == null )
        {
            mCopySegmentPreMat = currSegMat.clone();
            Log.d(TAG, "[moving-z] oing???");
        }

        int preNoZeroCount = countNonZero(mCopySegmentPreMat);
        Log.d(TAG, "[moving-z] preNoZeroCount noZeroCount : "+preNoZeroCount);

        Mat sumMat = new Mat();
        add(mCopySegmentPreMat, currSegMat, sumMat);

        int sumNoZeroCount = countNonZero(sumMat);
        Log.d(TAG, "[moving-z] sumNoZeroCount noZeroCount : "+sumNoZeroCount);

        mCopySegmentPreMat = currSegMat.clone();

        int diffCount = sumNoZeroCount - preNoZeroCount;
        Log.d(TAG, "[moving-z] diffCount : "+diffCount);

        if( diffCount > 10 )
        {
            mFastTrackOn = true;
        }
        else
        {
            mFastTrackOn = false;
        }
    }

    public static boolean getFastTrack(int []faceArray)
    {
//        boolean timeToGo = false;
//        if( SystemClock.uptimeMillis() - mStartCheckTime > 30 )
//        {
//            timeToGo = true;
//            mStartCheckTime = SystemClock.uptimeMillis();
//        }
//
//        Mat currSegMat = new Mat();
//
//        //if( mCopySegmentMat.empty() == false && timeToGo == true ) {
//        if( mCopySegmentMat.empty() == false && mMoveCheckEnd == true ) {
//            threshold(mCopySegmentMat, currSegMat, 200, 255, THRESH_BINARY_INV);
//
//            if( mCopySegmentPreMat == null )
//            {
//                mCopySegmentPreMat = currSegMat.clone();
//                Log.d(TAG, "[moving-z] oing???");
//            }
//
//            int preNoZeroCount = countNonZero(mCopySegmentPreMat);
//            Log.d(TAG, "[moving-z] preNoZeroCount noZeroCount : "+preNoZeroCount);
//
//            Mat sumMat = new Mat();
//            add(mCopySegmentPreMat, currSegMat, sumMat);
//
//            int sumNoZeroCount = countNonZero(sumMat);
//            Log.d(TAG, "[moving-z] sumNoZeroCount noZeroCount : "+sumNoZeroCount);
//
//            mCopySegmentPreMat = currSegMat.clone();
//
//            int diffCount = sumNoZeroCount - preNoZeroCount;
//            Log.d(TAG, "[moving-z] diffCount : "+diffCount);
//
//            if( diffCount > 10 )
//            {
//                mFastTrackOn = true;
//            }
//            else
//            {
//                mFastTrackOn = false;
//            }
//        }

//        return mFastTrackOn;

//        Log.d(TAG, "[problem-check] getFastTrack : "+mPercentNoZero);
////        if( thresholdValue * 15.0f < mPercentNoZero ) {//&& mPercentNoZero < thresholdValue * 5.0f) {
//////        if( thresholdValue * 15.0f < mPercentNoZero && mPercentNoZero < thresholdValue * 30.0f) {//&& mPercentNoZero < thresholdValue * 5.0f) {
////            return true;
////        }
////        else
////        {
////            return false;
////        }
//
        long startTime = SystemClock.uptimeMillis();
        mPreZoCount = 0.f;//mPercentNoZero;
        float maxMovingValue = -1;
        int count = 0;

//        while( true ) {

            //if (mMoveCheckEnd == true) {
            if( true ) {

//                //Log.d(TAG, "[Moving-K] mPercentNoZero : "+mPercentNoZero+", mPreZoCount : " + mPreZoCount);
//                count++;
//
//                if( mPercentNoZero > maxMovingValue )
//                {
//                    maxMovingValue = mPercentNoZero;
//                    Log.d(TAG, "[Moving-K] maxMovingValue : "+maxMovingValue+", count : "+count);
//                }
//
//                Log.d(TAG, "[Time-What] SystemClock.uptimeMillis() - startTime : "+(SystemClock.uptimeMillis() - startTime)+", count : "+count+", mPercentNoZero : "+mPercentNoZero);
//
//                if ( SystemClock.uptimeMillis() - startTime > 0.0 ) {

                    Log.d(TAG, "[Moving-K] count : "+count+", SystemClock.uptimeMillis() - startTime : "+(SystemClock.uptimeMillis() - startTime));
                    Log.d(TAG, "[Moving-K] Time-Check........mPercentNoZero : "+maxMovingValue+", mPreZoCount : " + mPreZoCount);

                    //mCurrFastValue = (double)(Math.abs(maxMovingValue - mPreZoCount));
                    mCurrFastValue = (double)(Math.abs(mPercentNoZero - mPreZoCount));

                    for(int i = 0; i < 5; i++) {
                        Log.d(TAG, "[segPersonPercent] SFTunner.movingTuneValues["+i+"] : "+SFTunner.movingTuneValues[i]);
                    }

                    //Tuning Point Start
                    //float fastCheckValue = 10.f;
                    float fastCheckValue = SFTunner.movingTuneValues[0];

                    float facePercent = 100.f;
                    if( faceArray[0] > 0 ) {
                        int x = faceArray[1] / mSmaller;
                        int y = faceArray[3] / mSmaller;
                        int w = faceArray[2] / mSmaller - x;
                        int h = faceArray[4] / mSmaller - y;

                        facePercent = (float)(w * h ) * 100.f / (float)(aiCamParameters.PREVIEW_WIDTH_I / mSmaller * aiCamParameters.PREVIEW_WIDTH_I / mSmaller);
                    }
                    else
                    {
//                        if( mCopySegmentMat.empty() == false && mMoveCheckEnd == true ) {
//                            Mat segCopyMat = new Mat();
//                            threshold(mCopySegmentMat, segCopyMat, 200, 255, THRESH_BINARY_INV);
//
//                            int segCount = countNonZero(segCopyMat);
//                            facePercent = (float)segCount * 100.f / (float)(segCopyMat.cols() * segCopyMat.rows());
//                        }
                    }
                    mFaceNSegValue = facePercent;
                    Log.d(TAG, "[facePercent] faceSize : "+facePercent);

                    //float usePercent = (50.f/17.f) * facePercent + 26.470588f;
//                    float minFace = 8.f;
//                    float maxFace = 25.f;
//                    float minPercent = 50.f;
//                    float maxPercent = 100.f;

                    float minFace = SFTunner.movingTuneValues[1];
                    float maxFace = SFTunner.movingTuneValues[2];
                    float minPercent = SFTunner.movingTuneValues[3];
                    float maxPercent = SFTunner.movingTuneValues[4];

                    float calcA = (maxPercent - minPercent) / (maxFace - minFace);
                    float calcB = minPercent - (calcA * minFace);
                    Log.d(TAG, "[facePercent] calcA : "+calcA+", calcB : "+calcB);

                    float usePercent = calcA * facePercent + calcB;

                    if(usePercent > maxPercent) usePercent = maxPercent;
                    else if(usePercent < minPercent) usePercent = minPercent;

                    fastCheckValue = fastCheckValue  * usePercent / 100.f;
                    mFastCheckValue = fastCheckValue;

                    Log.d(TAG, "[facePercent] usePercent : "+usePercent);
                    Log.d(TAG, "[facePercent] fastCheckValue : "+fastCheckValue);
                    //Tuning Point End

                    if( mCurrFastValue > fastCheckValue )
                    {
                        Log.d(TAG, "[Moving-K] Time-Check........FAST mPercentNoZero : "+maxMovingValue+", mPreZoCount : " + mPreZoCount);
                        Log.d(TAG, "[Moving-K] Time-Check......USE Fast-Track mPercentNoZero - mPreZoCount : "+(maxMovingValue - mPreZoCount));
                        mFastTrackOn = true;
                    }
                    else
                    {
                        Log.d(TAG, "[Moving-K] Time-Check........NORMAL mPercentNoZero : "+maxMovingValue+", mPreZoCount : " + mPreZoCount);
                        Log.d(TAG, "[Moving-K] Normal mPercentNoZero - mPreZoCount : "+(maxMovingValue - mPreZoCount));
                        mFastTrackOn = false;
                    }

                    Log.d(TAG, "[Moving-K] mFastTrackOn : "+mFastTrackOn);

//                    break;
//                }
            } else {
                //Log.d(TAG, "[Moving-K] mMoveCheckEnd : " + mMoveCheckEnd);
            }
//        }

        //Log.d(TAG, "[Moving-K] mFastTrackOn : "+mFastTrackOn);

        return mFastTrackOn;

//        //if( thresholdValue * 500.5f < mPercentNoZero ) {
//        if( thresholdValue * 10.5f < mPercentNoZero ) {
//
//            Log.d(TAG, "[problem-check] mPercentNoZero : "+mPercentNoZero+", mPreZoCount : "+mPreZoCount);
//
//            if( mPreZoCount !=  mPercentNoZero ) {
//                if (mPercentNoZero >= mPreZoCount) {
//
//                    mPreZoCount = mPercentNoZero;
//                    mFastTrackOn = true;
//                    //return true;
//                } else {
//
//                    mPreZoCount = mPercentNoZero;
//                    //mFastTrackOn = false;
//                    mFastTrackOn = true;
//                    //return false;
//                }
//            }
//
//            Log.d(TAG, "[problem-check] mPercentNoZero : " + mPercentNoZero + ", mPreZoCount : " + mPreZoCount+", mFastTrackOn : "+mFastTrackOn);
//        }
//        else
//        {
//            mPreZoCount = -1;
//            mFastTrackOn = false;
//            //return false;
//        }
//
//        return mFastTrackOn;
    }

    private static boolean stopStared = false;
    public static boolean getStopSignal(int inFlag)
    {
        int flag = movingChecker.getFastAlgFlag();
        float movingValue2 = movingChecker.getMovingValue();

        //Log.d(TAG, "movingValue2 SFTunner.mSFCommonTune.mMovingMovieThreshold : "+SFTunner.mSFCommonTune.mMovingMovieThreshold+", thresholdValue : "+thresholdValue);

        boolean movingFlag = true;
        if( flag == inFlag )
        {
            if( movingValue2 < SFTunner.mSFCommonTune.mMovingMovieThreshold )
            {
                Log.d(TAG, "movingValue2 stop1 : "+movingValue2);
                movingFlag = false;

                stopStared = true;
            }
            else
            {
                Log.d(TAG, "movingValue2 start-0 : "+movingValue2);
                movingFlag = true;
            }
        }
        else if( flag == 2 )
        {
            if( stopStared == true ) {

                if( movingValue2 < SFTunner.mSFCommonTune.mMovingMovieThreshold )
                {
                    Log.d(TAG, "movingValue2 stop-2 : "+movingValue2);
                    movingFlag = false;
                }
                else
                {
                    Log.d(TAG, "movingValue2 start-1 : " + movingValue2);
                    movingFlag = true;
                }

                if( movingValue2 < 0.01f ) {
                    Log.d(TAG, "movingValue2 stop-3 : "+movingValue2);
                    stopStared = false;
                }
            }
            else
            {
                if (movingValue2 > thresholdValue) {
                    Log.d(TAG, "movingValue2 start-2 : " + movingValue2);
                    movingFlag = true;
                } else {
                    Log.d(TAG, "movingValue2 stop-4 : " + movingValue2);
                    movingFlag = false;
                }
            }
        }

        if( mSegmentEntire < 0.01f )
        {
            Log.d(TAG, "movingValue2 except");
            movingFlag = true;
        }

        return movingFlag;
    }
}
