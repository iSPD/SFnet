#include "module.h"
#include "sfTunner.h"
#include "sofTunner.h"
#include "objAlg.h"
#include "studioMode.h"
#include "sfPersonTunner.h"

extern "C" {
#include <android/log.h>
#define TAG "SofCpp-JNI"
// log
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , TAG, __VA_ARGS__)

//#define LOGV(...)
//#define LOGD(...)
//#define LOGI(...)
//#define LOGW(...)
//#define LOGE(...)

//extern tuneManagersf gTuneManagerSF;
extern tuneManagerPersonSf gTuneManagerP;
extern tuneManagerBlur gTuneManagerBlur;

void setFloatArray(JNIEnv *env, jfloatArray outData, int dataCount, float *inputData)
{
    jfloat *floatArray = env->GetFloatArrayElements(outData, 0);
    if (floatArray == 0) {
        return;
    }
    jsize len = env->GetArrayLength(outData);

    for (int i = 0; i < dataCount; i++) {
        //LOGD("[tuning-test] float inputData[%d] : %f", i, inputData[i]);
        floatArray[i] = inputData[i];
    }

    //Step3. Release array resources
    env->ReleaseFloatArrayElements(outData, floatArray, 0);
}

void setIntArray(JNIEnv *env, jintArray outData, int dataCount, int *inputData)
{
    jint *intArray = env->GetIntArrayElements(outData, 0);
    if (intArray == 0) {
        return;
    }
    jsize len = env->GetArrayLength(outData);

    for (int i = 0; i < dataCount; i++) {
        //LOGD("[tuning-test] int inputData[%d] : %d", i, inputData[i]);
        intArray[i] = inputData[i];
    }

    //Step3. Release array resources
    env->ReleaseIntArrayElements(outData, intArray, 0);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_getAlTune(JNIEnv *env, jobject, jintArray alDatas) {
    int fastAlg, slowAlg, stopcount;
    gTuneManagerP.getAllTune(fastAlg, slowAlg, stopcount);
    int data[] = {fastAlg, slowAlg, stopcount};
    setIntArray(env, alDatas, 3, data);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_loadingTunnerSF(JNIEnv *env, jobject) {
    loadingTunner();
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_setStudioData(JNIEnv *env, jobject, jlong src, jlong studioMat, int flipMode) {
    Mat &src_input = *(Mat *) src;
    Mat &seg_studio_output = *(Mat *) studioMat;
    setStudioMode(src_input, seg_studio_output, flipMode);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_setSegmentationData(JNIEnv *env, jobject, jlong src, jlong segMat, jlong resultMat, jlong studioMat, jint useFast, jint rotate) {
    Mat &src_input = *(Mat *) src;
    Mat &seg_input = *(Mat *) segMat;
    Mat &seg_output = *(Mat *) resultMat;
    Mat &seg_studio_output = *(Mat *) studioMat;
    setSegmentData(src_input, seg_input, seg_output, seg_studio_output, useFast, rotate);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_readSfCommonTune(JNIEnv *env, jobject, jintArray intData1, jfloatArray floatData1) {

    SF_COMMON_TUNES datas;
    gTuneManagerP.getCommonTune(datas);

    int data1[3] = {datas.movingBlur, datas.movingArea, datas.movingSens};

    setIntArray(env, intData1, 3, data1);

    float data2[11] = {datas.movingThreshold, datas.movingMovieThreshold, datas.TargetGamma, datas.maxGamma, datas.cartoonBackTexRate, datas.cartoonBackSat,
                       datas.cartoonBackEdge, datas.cartoonFrontTexRate, datas.cartoonFrontSat, datas.cartoonFrontEdge, datas.beautyRate};
    setFloatArray(env, floatData1, 11, data2);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_readMovingTuneValue(JNIEnv *env, jobject, jintArray intData) {

    int datas[5];
    gTuneManagerP.getMovingRateTune(datas);

    setIntArray(env, intData, 5, datas);
    LOGD("readMovingTuneVlue : %d %d %d %d %d", datas[0], datas[1], datas[2], datas[3], datas[4]);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_readObjScaleValue(JNIEnv *env, jobject, jfloatArray floatData) {

    float datas[5];
    getObjScaleValue(datas);

    setFloatArray(env, floatData, 5, datas);
}


JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_readCompensationTune(JNIEnv *env, jobject, jfloatArray superFastData, jfloatArray fastData, jfloatArray slowData) {

//    SF_FEATHER_TUNE fastInfo, slowInfo;
//    gTuneManagerSF.getFeatherTuneData(fastInfo, slowInfo);
//
//    setIntArray(env, fastDataInt, 4, &fastInfo.mFStart);
//    setFloatArray(env, fastDataFloat, 12, &fastInfo.mColor);
//    setIntArray(env, slowDataInt, 4, &slowInfo.mFStart);
//    setFloatArray(env, slowDataFloat, 12, &slowInfo.mColor);

    SF_FEATHER_TUNES superFastAl, fastAl, slowAl;
    gTuneManagerP.getCompensationTune(superFastAl, fastAl, slowAl);

    setFloatArray(env, superFastData, 14, &superFastAl.scaleXcartoon);
    setFloatArray(env, fastData, 14, &fastAl.scaleXcartoon);
    setFloatArray(env, slowData, 14, &slowAl.scaleXcartoon);
}

JNIEXPORT int JNICALL
Java_com_ispd_sfcam_jniController_getObjSegment(JNIEnv *env, jobject, jlong input, jlong result, jlong studio_result) {
    Mat &src_input = *(Mat *) input;
    Mat &seg_output = *(Mat *) result;
    Mat &seg_studio = *(Mat *) studio_result;
    process(src_input, seg_output, seg_studio);

    return 0;
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_getAiTuneData(JNIEnv *env, jobject, jintArray outData, jfloatArray outData2, jintArray outData3)
{
    int inData[23] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
    float inData2[4] = {0.f, 0.f, 0.f, 0.f};
    int inData3[2] = {0,0};

    gTuneManagerBlur.getAiTuneData(inData[0], inData[1], inData[2], inData[3], inData[4],
                               inData[5], inData[6], inData[7], inData[8], inData[9], inData[10], inData[11], inData[12], inData[13], inData[14]
            , inData[15], inData[16], inData[17], inData[18], inData[19], inData[20], inData[21], inData[22], inData2[0], inData2[1], inData2[2], inData2[3], inData3[0], inData3[1]);

    setIntArray(env, outData, 23, inData);
    setFloatArray(env, outData2, 4, inData2);
    setIntArray(env, outData3, 2, inData3);
}

JNIEXPORT float JNICALL
Java_com_ispd_sfcam_jniController_getTouchBoundary(JNIEnv *env, jobject) {

    return gTuneManagerBlur.getTouchBoundary();
}

JNIEXPORT int JNICALL
Java_com_ispd_sfcam_jniController_getBlurData(JNIEnv *env, jobject, jintArray blurCount, jintArray blurSize1, jintArray blurSize2, jintArray blurSize3)
{
    int count[4] = {1,2,3,4};
    int size1[4] = {5,5,5,5};
    int size2[4] = {5,5,5,5};
    int size3[4] = {5,5,5,5};
    int maxBlurCount;

    LOGD("[blur-b] gTuneManagerP.getCurrentStatus() : %d", gTuneManagerP.getCurrentStatus());
    if( gTuneManagerP.getCurrentStatus() == false ) {
        maxBlurCount = gTuneManagerBlur.getBlurData(count, size1, size2, size3);
    }
    else
    {
        gTuneManagerP.getBlurTuneData(count[3], size1[3], size2[3], size3[3]);
        LOGD("[blur-b] count : %d, size1 : %d, size2 : %d, size3 : %d", count[3], size1[3], size2[3], size3[3]);
        maxBlurCount = count[3];
    }
    setIntArray(env, blurCount, 4, count);
    setIntArray(env, blurSize1, 4, size1);
    setIntArray(env, blurSize2, 4, size2);
    setIntArray(env, blurSize3, 4, size3);

    return maxBlurCount;
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_getSizeRect(JNIEnv *env, jobject, jintArray rect) {
    int temp[4];
    getSizeRect(temp);

    setIntArray(env, rect, 4, temp);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_getMidRect(JNIEnv *env, jobject, jintArray rect) {
    int temp[4];
    getMidRect(temp);

    setIntArray(env, rect, 4, temp);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_updateObjMoreRect(JNIEnv *env, jobject, int moreRectCount, jintArray rectIndex) {

    jint *intArray = env->GetIntArrayElements(rectIndex, 0);
    if (intArray == 0) {
        setObjectMoreRect(0, 0);
        return;
    }
    jsize len = env->GetArrayLength(rectIndex);

    setObjectMoreRect(moreRectCount, intArray);

    env->ReleaseIntArrayElements(rectIndex, intArray, 0);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_updateObjRect(JNIEnv *env, jobject, int objNum, jfloatArray rects) {

    Rect2f rectF[objNum];

    //step1
    jfloat *floatArray = env->GetFloatArrayElements(rects, 0);
    if (floatArray == 0 || objNum == 0) {
        setObjectRect(0, rectF);
        return;
    }
    jsize len = env->GetArrayLength(rects);

    LOGD("[tensor-test-jni] obj number : %d, read number : %d", objNum, len);
    for(int i = 0; i < objNum; i++)
    {
        LOGD("[tensor-test-jni] objRect[%d] left : %f, top : %f, right : %f, bottom : %f", i, floatArray[i*4+0], floatArray[i*4+1], floatArray[i*4+2], floatArray[i*4+3]);
        float width = floatArray[i*4+2] - floatArray[i*4+0];
        float height = floatArray[i*4+3] - floatArray[i*4+1];
        rectF[i] = Rect2f(floatArray[i*4+0], floatArray[i*4+1], width, height);
    }

    for(int i = 0; i < objNum; i++)
    {
        LOGD("[tensor-test-jni] rectF[%d] left : %f, top : %f, right : %f, bottom : %f", i, rectF[i].tl().x, rectF[i].tl().y, rectF[i].br().x, rectF[i].br().y);
    }

    setObjectRect(objNum, rectF);

    //Step3. Release array resources
    env->ReleaseFloatArrayElements(rects, floatArray, 0);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_setTouchEvent(JNIEnv *, jobject, jfloat x, jfloat y)
{
    setTouchData(x, y);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_setTouchEventForMultiTouch(JNIEnv *, jobject, bool multiTouchOn, bool minusTouchOn, jfloat x, jfloat y)
{
    setTouchDataForMultiTouch(multiTouchOn, minusTouchOn, x, y);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_resetMidRectForTouch(JNIEnv *env, jobject) {
    resetMidRectForTouch();
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_updateObjRectForTouch(JNIEnv *env, jobject, int objNum, jfloatArray rects, bool resetOn) {

    Rect2f rectF[objNum];

    //step1
    jfloat *floatArray = env->GetFloatArrayElements(rects, 0);
    if (floatArray == 0 || objNum == 0) {
        setObjectRectForTouch(0, rectF, resetOn);
        return;
    }
    jsize len = env->GetArrayLength(rects);

    LOGD("[tensor-test-jni] obj number : %d, read number : %d", objNum, len);
    for(int i = 0; i < objNum; i++)
    {
        LOGD("[tensor-test-jni] objRect[%d] left : %f, top : %f, right : %f, bottom : %f", i, floatArray[i*4+0], floatArray[i*4+1], floatArray[i*4+2], floatArray[i*4+3]);
        float width = floatArray[i*4+2] - floatArray[i*4+0];
        float height = floatArray[i*4+3] - floatArray[i*4+1];
        rectF[i] = Rect2f(floatArray[i*4+0], floatArray[i*4+1], width, height);
    }

    for(int i = 0; i < objNum; i++)
    {
        LOGD("[tensor-test-jni] rectF[%d] left : %f, top : %f, right : %f, bottom : %f", i, rectF[i].tl().x, rectF[i].tl().y, rectF[i].br().x, rectF[i].br().y);
    }

    setObjectRectForTouch(objNum, rectF, resetOn);

    //Step3. Release array resources
    env->ReleaseFloatArrayElements(rects, floatArray, 0);
}

//studio Mode..

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_setStudioModeJni(JNIEnv *env, jobject, jint studioMode) {
    gTuneManagerBlur.setStudioMode(studioMode);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_readStudioRect(JNIEnv *env, jobject, jfloatArray studioValues) {
    float jniData[4];
    getDarkSnowData(jniData[0], jniData[1], jniData[2], jniData[3]);

    setFloatArray(env, studioValues, 4, jniData);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_readStudioTuneValue(JNIEnv *env, jobject, jfloatArray faderbright, jfloatArray fadersaturation,
                                                      jfloatArray inbright, jfloatArray outbright, jfloatArray satrate, jfloatArray mono_contrast) {

    float faderBright, faderSaturation, inBright, outBright, satRate, monoContrast;

    if( gTuneManagerP.getCurrentStatus() == false ) {
        gTuneManagerBlur.getStudioTuneValue(faderBright, faderSaturation, inBright, outBright,
                                            satRate, monoContrast);
    }
    else
    {
        SF_STUDIO_TUNEDATAS datas;
        gTuneManagerP.getStudioTune(datas);
        faderBright = datas.faderBright;
        faderSaturation = datas.faderSaturation;
        inBright = datas.inBright;
        outBright = datas.outBright;
        satRate = datas.satRate;
        monoContrast = datas.contrast;
    }

    setFloatArray(env, faderbright, 1, &faderBright);
    setFloatArray(env, fadersaturation, 1, &faderSaturation);
    setFloatArray(env, inbright, 1, &inBright);
    setFloatArray(env, outbright, 1, &outBright);
    setFloatArray(env, satrate, 1, &satRate);
    setFloatArray(env, mono_contrast, 1, &monoContrast);
}

//face set
JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_setFaceRect(JNIEnv *env, jobject, jintArray arr, jint width, jint height) {

    //step1
    jint *intArray = env->GetIntArrayElements(arr, 0);
    if (intArray == 0) {
        return;
    }
    jsize len = env->GetArrayLength(arr);

    FaceLocationMulti(len, intArray, width, height);

    //Step3. Release array resources
    env->ReleaseIntArrayElements(arr, intArray, 0);
}

//current status
JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_setSaveMovieStatus(JNIEnv *env, jobject, jboolean isSave, jboolean isMovie, jboolean isFront, jboolean isPerson, jfloat brightness) {
    setSaveMovieFrontStatus(isSave, isMovie, isFront, isPerson, brightness);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_setRototationInfo(JNIEnv *, jobject, jint rotateInfo) {
    setRotateInfo(rotateInfo);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_setBlurSizeTune(JNIEnv *env, jobject, jint blurSize, jboolean videoCapOn) {
    setBlurSize(blurSize, videoCapOn);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_getObjMovingValue(JNIEnv *env, jobject, jfloatArray movingValues) {
    float sens = (float)gTuneManagerBlur.getMovingSensitivityValue(0);
    float value = gTuneManagerBlur.getMovingValue();
    float touchValue = gTuneManagerBlur.getTouchBoundary();

    float inData[3] = {sens, value, touchValue};

    setFloatArray(env, movingValues, 3, inData);
}

JNIEXPORT void JNICALL
Java_com_ispd_sfcam_jniController_setCurrentAlMode(JNIEnv *env, jobject, jint mode) {
    setCurrentAlMode(mode);
}

}