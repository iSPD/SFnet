package com.ispd.sfcam;

/**
 * Created by khkim on 2018-06-13.
 */

public class jniController {
    public native static void loadTuningData();
    public native static void setTouchEventForMultiTouch(boolean multiTouchOn, boolean minusTouchOn, float x, float y);//1
    public native static void setTouchEvent(float x, float y);//1
    public native static void setSaveStatus(boolean saveOn);
    public native static void setSaveMovieStatus(boolean isSave, boolean isMovie, boolean isFront, boolean isPerson, float brightness);
//    public native static int convertNativeLib(long matAddrInput, long matAddrResult, long matSegment,
//                                              int original, boolean needMask, boolean backAlOn);//1
    public native static int getObjSegment(long input, long result, long studio_result);

    public native static void setRototationInfo(int rotateInfo);//1

    public native static void initEglExt(int width, int height);//1
    public native static void deInitEglExt();//1
    public native static void makeEGLImage();//1
    public native static void updateEGLImage(int offset);//1
    public native static void useEGLImage(int offset);//1
    public native static int getTexName(int offset);
    public native static void updateMasksSync();

    public native static void setUseFaceIndex(int []array);//1
    public native static void setFaceRect(int []array, int width, int height);//1
    public native static void setFaceRectBlue(int []array, int width, int height);//1
    public native static void setFaceTuningValue( float[] compenX, float[] compenY, double[] degree);//1
    public native static float getTouchBoundary();
    public native static int getBlurData(int []blurCount, int []blurSize1, int []blurSize2, int []blurSize3);
    public native static float getTouchBoundarySave();
    public native static int getBlurDataSave(int []blurCount, int []blurSize1, int []blurSize2, int []blurSize3);
    public native static void getTuningRefData(float []data);
    public native static double getCurrentBright();
    public native static void setMovingValue(long matAddrInput, float thresholdValue);
    public native static boolean getBlurOn(float []movingValue);
    public native static boolean getBlurUiOn(float []movingValue);
    public native static boolean getMoving40Check();
    public native static void resetMovingAlg();

    //save
    public native static void initEGLSave(int width, int height);
    public native static void makeEGLImageSave(int width, int height);
    public native static void copyBufferSave(byte []data, int width, int height);
    public native static void useEGLImageSave();
    public native static int getTexNameSave();
    public native static void deInitEGLSave();

    //save output
    public native static void initEGLSaveOutput(int width, int height);
    public native static int makeEGLImageSaveOutput(int width, int height);
    public native static void copyBufferSaveOutput(byte []data, int width, int height);
    public native static void deInitEGLSaveOutput();

    public native static void sofInitEGL(long sharedContext);
    public native static void sofMakeCurrent(boolean onoff);
    public native static void sofSwapBuffer();
    public native static void sofDeinitEGL();
    public native static void sofEglCopyBuffer(long display, long surface);
    public native static void setStudioModeJni(int studioMode);
    public native static void readStudioTuneValue(float []data1, float []data2, float []data3, float []data4, float []data5, float []data6);
    public native static void readStudioRect(float []data);
    public native static void setBlurSizeTune(int blurSize, boolean videoCapOn);

    //object detection
    public native static void updateObjMoreRect(int moreRectCount, int []rectIndex);
    public native static void updateObjRect(int objNum, float []rects);
    public native static boolean getAIOn();

    //object detection for touch
    public native static void updateObjRectForTouch(int objNum, float []rects, boolean resetOn);

    //
    public native static void getMidRect(int []rect);
    public native static boolean getMidRectForTouch(int []rect);
    public native static void resetMidRectForTouch();
    public native static void getSizeRect(int []rect);

    //Segmentation
    //public native static void setSegmentationData(long segMat);
    public native static void setPreviewData(long oriMat);
    public native static void setSegmentationData(long src, long segMat, long resultMa, long studioMat, int useFast, int rotate);
    public native static void setStudioData(long srcMat, long studioMat, int flipMode);

    public native static void getAiTuneData(int []aiTuneData, float []aiTuneData2, int []aiTuneData3);

    public native static void setSFImage(boolean onOff);

    //AI Segmentation
    public native static void getAlTune(int []alDatas);
    public native static void readCompensationTune(float []superFastData, float []fastData, float []slowData);
    public native static void loadingTunnerSF();
    public native static void readSfCommonTune(int []data1, float []data2);
    public native static void readMovingTuneValue(int []datas);

    //Obj Moving Values
    public native static void getObjMovingValue(float []movingValues);

    public native static void setCurrentAlMode(int mode);

    public native static void readObjScaleValue(float []datas);

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("imported-lib");
        System.loadLibrary("native-lib");
    }
}
