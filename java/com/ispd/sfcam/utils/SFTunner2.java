package com.ispd.sfcam.utils;

//import android.util.Log;

import com.ispd.sfcam.jniController;

/**
 * Created by nexus on 2017-07-18.
 */

public class SFTunner2 {

    private static String Tag = "sofTunner2";

    private static boolean loadingTunningFile = false;

    public static float mTouchBoundary = 4.0f;//use

    public static int mMaxBlurCount = 4;//use

    public static int mBlurCount[] = { 1, 2, 3, 4 };//use
    public static int mBlurSize[] = { 5, 5, 5, 5 };//use
    public static int mBlurSize1[] = { 5, 5, 5, 5 };//use
    public static int mBlurSize2[] = { 5, 5, 5, 5 };//use
    public static int mBlurSize3[] = { 5, 5, 5, 5 };//use

    //Save
    public static float mTouchBoundarySave = 4.0f;//use

    public static int mMaxBlurCountSave = 4;//use

    public static int mBlurCountSave[] = { 1, 2, 3, 4 };//use
    public static int mBlurSizeSave[] = { 5, 5, 5, 5 };//use
    public static int mBlurSizeSave1[] = { 5, 5, 5, 5 };//use
    public static int mBlurSizeSave2[] = { 5, 5, 5, 5 };//use
    public static int mBlurSizeSave3[] = { 5, 5, 5, 5 };//use

    public static float studioFaderBright[] = {1.0f};//use
    public static float studioFaderSaturation[] = {1.0f};//use
    public static float studioInBright[] = {1.0f};//use
    public static float studioOutBright[] = {1.0f};//use
    public static float studioSatRate[] = {0.2f};//use
    public static float studioMonoContrast[] = {0.2f};//use

    //AI Tune
    private static int []mAiTuneDataTemp = new int[23];
    private static float []mAiTuneDataTemp2 = new float[4];
    private static int []mAiTuneDataTemp3 = new int[2];
    public static int mAIThreshod;
    public static int mAISizePlus;
    public static int mAIMin;
    public static int mAIMax;
    public static int mPersonMin;
    public static int mPersonMax;
    public static int mAIScreen = 10;//????
    public static int mA0Time = 2000;
    public static int mA1Time = 1000;
    public static int mBackFaceBlueTime;
    public static int mFrontFaceBlueTime;
    public static int mBackFaceHandThreshold;
    public static int mFrontFaceHandThreshold;
    public static int mAiSizeBlurMin;
    public static int mAiSizeBlurMax;
    public static int mAiProcessingCount = 3;
    public static int mAiBelowPercent = 20;
    public static int mAiUpPercent = 20;
    public static int mAiLeftPercent = 20;
    public static int mAiRightPercent = 20;
    public static int mAiWaitCount = 4;
    public static int mAiTouchTime = 1500;
    public static int mAiTouchBoxSize = 80;

    public static float mAIMultiUpRate = 0.0f;
    public static float mAIMultiDownRate = 0.0f;
    public static float mAIMultiSmallRate = 0.0f;
    public static float mAIMultiBigRate = 0.0f;

    public static int mAiCornerX = 10;
    public static int mAiCornerY = 10;

    //Obj Moving Values
    private static float []mObjMovingValues = new float[3];
    public static int mObjMovingSens = 50;
    public static int mObjMovingValue = 1;
    public static int mObjMovingTouchValue = 6;

    public SFTunner2()
    {

    }

    public static void readTuneData()
    {
        //jniController.loadTuningData();

        loadingTunningFile = true;

        mTouchBoundary = jniController.getTouchBoundary();
        mMaxBlurCount = jniController.getBlurData(mBlurCount, mBlurSize1, mBlurSize2, mBlurSize3);

        getAiTuneData();
    }

    public static void readNeedTuneData()
    {
        if( loadingTunningFile == true ) {
            mTouchBoundary = jniController.getTouchBoundary();
            mMaxBlurCount = jniController.getBlurData(mBlurCount, mBlurSize1, mBlurSize2, mBlurSize3);

            Log.d(Tag, "mTouchBoundary : " + mTouchBoundary);
            for (int i = 0; i < 4; i++) {
                Log.d(Tag, "mBlurCount[" + i + "] : " + mBlurCount[i]);
                Log.d(Tag, "mBlurSize1[" + i + "] : " + mBlurSize1[i]);
                Log.d(Tag, "mBlurSize2[" + i + "] : " + mBlurSize2[i]);
                Log.d(Tag, "mBlurSize3[" + i + "] : " + mBlurSize3[i]);
            }
            Log.d(Tag, "mMaxBlurCount : " + mMaxBlurCount);
        }
    }

    public static void readNeedTuneDataSave()
    {
        if( loadingTunningFile == true ) {
            mTouchBoundarySave = jniController.getTouchBoundarySave();
            mMaxBlurCountSave = jniController.getBlurDataSave(mBlurCountSave, mBlurSizeSave1, mBlurSizeSave2, mBlurSizeSave3);

            Log.d(Tag, "mTouchBoundarySave : " + mTouchBoundarySave);
            for (int i = 0; i < 4; i++) {
                Log.d(Tag, "mBlurCountSave[" + i + "] : " + mBlurCountSave[i]);
                Log.d(Tag, "mBlurCountSave[" + i + "] : " + mBlurSizeSave1[i]);
                Log.d(Tag, "mBlurCountSave[" + i + "] : " + mBlurSizeSave2[i]);
                Log.d(Tag, "mBlurCountSave[" + i + "] : " + mBlurSizeSave3[i]);
            }
            Log.d(Tag, "mMaxBlurCountSave : " + mMaxBlurCountSave);
        }
    }

    public static void readStudioTuneValue()
    {
        jniController.readStudioTuneValue(studioFaderBright, studioFaderSaturation, studioInBright, studioOutBright, studioSatRate, studioMonoContrast);

        Log.d(Tag, "[Mono-Dark] studioFaderBright : " + studioFaderBright[0]);
        Log.d(Tag, "[Mono-Dark] studioFaderSaturation : " + studioFaderSaturation[0]);
        Log.d(Tag, "[Mono-Dark] studioInBright : " + studioInBright[0]);
        Log.d(Tag, "[Mono-Dark] studioOutBright : " + studioOutBright[0]);
        Log.d(Tag, "[Mono-Dark] studioSatRate : " + studioSatRate[0]);
        Log.d(Tag, "[Mono-Dark] studioMonoContrast : " + studioMonoContrast[0]);
    }

    public static void getObjMovingValue()
    {
        jniController.getObjMovingValue(mObjMovingValues);
        mObjMovingSens = (int)mObjMovingValues[0];
        mObjMovingValue = (int)mObjMovingValues[1];
        mObjMovingTouchValue = (int)mObjMovingValues[2];
    }

    public static void getAiTuneData()
    {
        jniController.getAiTuneData(mAiTuneDataTemp, mAiTuneDataTemp2, mAiTuneDataTemp3);

        mAIThreshod = mAiTuneDataTemp[0];
        mAISizePlus = mAiTuneDataTemp[1];
        mAIMin = mAiTuneDataTemp[2];
        mAIMax = mAiTuneDataTemp[3];
        mPersonMin = mAiTuneDataTemp[4];
        mPersonMax = mAiTuneDataTemp[5];
        mAIScreen = mAiTuneDataTemp[6];
        mA0Time = mAiTuneDataTemp[7];
        mA1Time = mAiTuneDataTemp[8];
        mBackFaceBlueTime = mAiTuneDataTemp[9];
        mFrontFaceBlueTime = mAiTuneDataTemp[10];
        mBackFaceHandThreshold = mAiTuneDataTemp[11];
        mFrontFaceHandThreshold = mAiTuneDataTemp[12];
        mAiSizeBlurMin = mAiTuneDataTemp[13];
        mAiSizeBlurMax = mAiTuneDataTemp[14];

        mAiProcessingCount = mAiTuneDataTemp[15];
        mAiBelowPercent = mAiTuneDataTemp[16];
        mAiUpPercent = mAiTuneDataTemp[17];
        mAiLeftPercent = mAiTuneDataTemp[18];
        mAiRightPercent = mAiTuneDataTemp[19];
        mAiWaitCount = mAiTuneDataTemp[20];
        mAiTouchTime = mAiTuneDataTemp[21];
        mAiTouchBoxSize = mAiTuneDataTemp[22];

        for(int i = 0; i < 23; i++)
        {
            Log.d(Tag, "mAiTuneDataTemp["+i+"] : "+mAiTuneDataTemp[i]);
        }

        mAIMultiUpRate = mAiTuneDataTemp2[0];
        mAIMultiDownRate = mAiTuneDataTemp2[1];
        mAIMultiSmallRate = mAiTuneDataTemp2[2];
        mAIMultiBigRate = mAiTuneDataTemp2[3];

        for(int i = 0; i < 4; i++)
        {
            Log.d(Tag, "mAiTuneDataTemp2["+i+"] : "+mAiTuneDataTemp2[i]);
        }

        mAiCornerX = mAiTuneDataTemp3[0];
        mAiCornerY = mAiTuneDataTemp3[1];

        for(int i = 0; i < 2; i++)
        {
            Log.d(Tag, "mAiTuneDataTemp3["+i+"] : "+mAiTuneDataTemp3[i]);
        }
    }
}
