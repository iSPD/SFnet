package com.ispd.sfcam.utils;

import com.ispd.sfcam.jniController;

public class SFTunner {

    public static String TAG = "SFTunner";

    public static class sfCommonTune
    {
        public static int mMovingBlur;
        public static int mMovingArea;
        public static int mMovingSens;
        public static float mMovingThreshold;
        public static float mMovingMovieThreshold;
        public static float mTargetGamma;
        public static float mMaxGamma;
        public static float mCartoonBackTexRate;
        public static float mCartoonBackSat;
        public static float mCartoonBackEdge;
        public static float mCartoonFrontTexRate;
        public static float mCartoonFrontSat;
        public static float mCartoonFrontEdge;
        public static float mBeautyRate;
        public static int mPreviewBlur1;
        public static int mPreviewBlur2;
        public static int mPreviewBlur3;
        public static int mCaptureBlur1;
        public static int mCaptureBlur2;
        public static int mCaptureBlur3;
        public static int mVideoBlur1;
        public static int mVideoBlur2;
        public static int mVideoBlur3;
    }

    public static class superfastfeatherTune
    {
        public static float mScaleXcartoon;
        public static float mScaleYcartoon;
        public static float mScaleXblur;
        public static float mScaleYblur;
        public static float mScaleXsf;
        public static float mScaleYsf;
        public static float mCartoonThickness;
        public static float mBlurThickness;
        public static float mSfThickness;
        public static float mResizeXY;
        public static float mColor;
        public static float mColorStart;
        public static float mTransUD;
        public static float mTransLR;
    };

    public static class fastfeatherTune
    {
        public static float mScaleXcartoon;
        public static float mScaleYcartoon;
        public static float mScaleXblur;
        public static float mScaleYblur;
        public static float mScaleXsf;
        public static float mScaleYsf;
        public static float mCartoonThickness;
        public static float mBlurThickness;
        public static float mSfThickness;
        public static float mResizeXY;
        public static float mColor;
        public static float mColorStart;
        public static float mTransUD;
        public static float mTransLR;
    };

    public static class slowfeatherTune
    {
        public static float mScaleXcartoon;
        public static float mScaleYcartoon;
        public static float mScaleXblur;
        public static float mScaleYblur;
        public static float mScaleXsf;
        public static float mScaleYsf;
        public static float mCartoonThickness;
        public static float mBlurThickness;
        public static float mSfThickness;
        public static float mResizeXY;
        public static float mColor;
        public static float mColorStart;
        public static float mTransUD;
        public static float mTransLR;
    };

    public static sfCommonTune mSFCommonTune = new sfCommonTune();
    public static superfastfeatherTune mSuperFastFeatherTune = new superfastfeatherTune();
    public static fastfeatherTune mFastFeatherTune = new fastfeatherTune();
    public static slowfeatherTune mSlowFeatherTune = new slowfeatherTune();

    public static int []mCommonDataInt1 = new int[3];
    public static float []mCommonDataFloat1 = new float[11];

    public static float []mSuperFastDataFloat = new float[14];
    public static float []mFastDataFloat = new float[14];
    public static float []mSlowDataFloat = new float[14];

    public static int []movingTuneValues = new int[5];

    public static void readSFTuneData()
    {
        jniController.loadingTunnerSF();
    }

    public static void readSFDatas()
    {
        jniController.readCompensationTune(mSuperFastDataFloat, mFastDataFloat, mSlowDataFloat);
        for(int i = 0; i < 14; i++) {
            Log.o(TAG, "mSuperFastDataFloat["+i+"] : "+mSuperFastDataFloat[i]);
        }
        for(int i = 0; i < 14; i++) {
            Log.o(TAG, "mFastDataFloat["+i+"] : "+mFastDataFloat[i]);
        }
        for(int i = 0; i < 14; i++) {
            Log.o(TAG, "mSlowDataFloat["+i+"] : "+mSlowDataFloat[i]);
        }

        //super fast feather
        mSuperFastFeatherTune.mScaleXcartoon = mSuperFastDataFloat[0];
        mSuperFastFeatherTune.mScaleYcartoon = mSuperFastDataFloat[1];
        mSuperFastFeatherTune.mScaleXblur = mSuperFastDataFloat[2];
        mSuperFastFeatherTune.mScaleYblur = mSuperFastDataFloat[3];
        mSuperFastFeatherTune.mScaleXsf = mSuperFastDataFloat[4];
        mSuperFastFeatherTune.mScaleYsf = mSuperFastDataFloat[5];
        mSuperFastFeatherTune.mCartoonThickness = mSuperFastDataFloat[6];
        mSuperFastFeatherTune.mBlurThickness = mSuperFastDataFloat[7];
        mSuperFastFeatherTune.mSfThickness = mSuperFastDataFloat[8];
        mSuperFastFeatherTune.mResizeXY = mSuperFastDataFloat[9];
        mSuperFastFeatherTune.mColor = mSuperFastDataFloat[10];
        mSuperFastFeatherTune.mColorStart = mSuperFastDataFloat[11];
        mSuperFastFeatherTune.mTransUD = mSuperFastDataFloat[12];
        mSuperFastFeatherTune.mTransLR = mSuperFastDataFloat[13];

        //fast feather
        mFastFeatherTune.mScaleXcartoon = mFastDataFloat[0];
        mFastFeatherTune.mScaleYcartoon = mFastDataFloat[1];
        mFastFeatherTune.mScaleXblur = mFastDataFloat[2];
        mFastFeatherTune.mScaleYblur = mFastDataFloat[3];
        mFastFeatherTune.mScaleXsf = mFastDataFloat[4];
        mFastFeatherTune.mScaleYsf = mFastDataFloat[5];
        mFastFeatherTune.mCartoonThickness = mFastDataFloat[6];
        mFastFeatherTune.mBlurThickness = mFastDataFloat[7];
        mFastFeatherTune.mSfThickness = mFastDataFloat[8];
        mFastFeatherTune.mResizeXY = mFastDataFloat[9];
        mFastFeatherTune.mColor = mFastDataFloat[10];
        mFastFeatherTune.mColorStart = mFastDataFloat[11];
        mFastFeatherTune.mTransUD = mFastDataFloat[12];
        mFastFeatherTune.mTransLR = mFastDataFloat[13];

        //slow feather
        mSlowFeatherTune.mScaleXcartoon = mSlowDataFloat[0];
        mSlowFeatherTune.mScaleYcartoon = mSlowDataFloat[1];
        mSlowFeatherTune.mScaleXblur = mSlowDataFloat[2];
        mSlowFeatherTune.mScaleYblur = mSlowDataFloat[3];
        mSlowFeatherTune.mScaleXsf = mSlowDataFloat[4];
        mSlowFeatherTune.mScaleYsf = mSlowDataFloat[5];
        mSlowFeatherTune.mCartoonThickness = mSlowDataFloat[6];
        mSlowFeatherTune.mBlurThickness = mSlowDataFloat[7];
        mSlowFeatherTune.mSfThickness = mSlowDataFloat[8];
        mSlowFeatherTune.mResizeXY = mSlowDataFloat[9];
        mSlowFeatherTune.mColor = mSlowDataFloat[10];
        mSlowFeatherTune.mColorStart = mSlowDataFloat[11];
        mSlowFeatherTune.mTransUD = mSlowDataFloat[12];
        mSlowFeatherTune.mTransLR = mSlowDataFloat[13];

        jniController.readSfCommonTune(mCommonDataInt1, mCommonDataFloat1);
        for(int i = 0; i < 3; i++) {
            Log.o(TAG, "mCommonDataInt1["+i+"] : "+mCommonDataInt1[i]);
        }
        for(int i = 0; i < 11; i++) {
            Log.o(TAG, "mCommonDataFloat1["+i+"] : "+mCommonDataFloat1[i]);
        }

        mSFCommonTune.mMovingBlur = mCommonDataInt1[0];
        mSFCommonTune.mMovingArea = mCommonDataInt1[1];
        mSFCommonTune.mMovingSens = mCommonDataInt1[2];
        mSFCommonTune.mMovingThreshold = mCommonDataFloat1[0];
        mSFCommonTune.mMovingMovieThreshold = mCommonDataFloat1[1];
        mSFCommonTune.mTargetGamma = mCommonDataFloat1[2];
        mSFCommonTune.mMaxGamma = mCommonDataFloat1[3];
        mSFCommonTune.mCartoonBackTexRate = mCommonDataFloat1[4];
        mSFCommonTune.mCartoonBackSat = mCommonDataFloat1[5];
        mSFCommonTune.mCartoonBackEdge = mCommonDataFloat1[6];
        mSFCommonTune.mCartoonFrontTexRate = mCommonDataFloat1[7];
        mSFCommonTune.mCartoonFrontSat = mCommonDataFloat1[8];
        mSFCommonTune.mCartoonFrontEdge = mCommonDataFloat1[9];
        mSFCommonTune.mBeautyRate = mCommonDataFloat1[10];

        jniController.readMovingTuneValue(movingTuneValues);

//        mSFCommonTune.mPreviewBlur1 = mCommonDataInt2[0];
//        mSFCommonTune.mPreviewBlur2 = mCommonDataInt2[1];
//        mSFCommonTune.mPreviewBlur3 = mCommonDataInt2[2];
//        mSFCommonTune.mCaptureBlur1 = mCommonDataInt2[3];
//        mSFCommonTune.mCaptureBlur2 = mCommonDataInt2[4];
//        mSFCommonTune.mCaptureBlur3 = mCommonDataInt2[5];
//        mSFCommonTune.mVideoBlur1 = mCommonDataInt2[6];
//        mSFCommonTune.mVideoBlur2 = mCommonDataInt2[7];
//        mSFCommonTune.mVideoBlur3 = mCommonDataInt2[8];
    }
}
