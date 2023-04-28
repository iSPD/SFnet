#include "module.h"
#include <android/log.h>
#include <opencv2/imgproc/types_c.h>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>

#include "watershedSegmentation.h"
#include "sfTunner.h";
#include "sofTunner.h";
#include "utils.h"
#include "objAlg.h"
#include "studioMode.h"
#include "sfPersonTunner.h"

// log
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "SofCpp", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "SofCpp", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "SofCpp", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "SofCpp", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "SofCpp", __VA_ARGS__)

//#define LOGV(...)
//#define LOGD(...)
//#define LOGI(...)
//#define LOGW(...)
//#define LOGE(...)

extern "C" {

tuneManagersf gTuneManagerSF;
tuneManagerBlur gTuneManagerBlur;
tuneManagerPersonSf gTuneManagerP;

//SF_WATER_TUNE mMarkerData, mOuterData;

static bool gIsSave;
static bool gIsMovie;
static bool gIsPesron;

Mat mSegmentationData;
RNG rng(12345);

bool gFrontStatus = false;
int gTouchX = -1;
int gTouchY = -1;

//object more
int gObjMoreNumber = 0;
int gObjMoreIndex[10];

//object detection
int gObjNumber = 0;
Rect2f gObjRect[10];

//object detection for touch
int gObjTouchNumber = 0;
Rect2f gObjTouchRect[10];
bool gObjTouchResetOn = true;

//face array
static int gFaceCount = 0;
static float gFaceArray[4 * 5];

int gRotateInfo = 0;

static bool gMultiTouchOn = false;
static bool gMinusTouchOn = false;
static float gSaveTouchX = -1.0f;
static float gSaveTouchY = -1.0f;

static int gStartMoving = 1;
static Mat gDiffImg;

Mat gFaderMat(1080 / SMALL_FOR_SPEED, 1440 / SMALL_FOR_SPEED, CV_8UC1);

void loadingTunner() {
    //gTuneManagerSF.loadingTuneData();
    gTuneManagerBlur.loadingTuneData();
    gTuneManagerP.loadingTuneData();

    int slow, low, mid, out;
    gTuneManagerBlur.getObjBrightInfo(slow, low, mid, out);
    gTuneManagerP.setObjBrightInfo(slow, low, mid, out);
}

/** @function thresh_callback */
Mat convexull(Mat src) {
//    Mat src_copy, src_gray;
//    src_copy = src.clone();
//    cvtColor( src, src_gray, CV_BGR2GRAY );
//    Mat threshold_output;
//    std::vector<std::vector<Point> > contours;
//    std::vector<Vec4i> hierarchy;
//
//    /// Detect edges using Threshold
//    threshold( src_gray, threshold_output, 14, 255, THRESH_BINARY );

    Mat scr_copy = src.clone();
    std::vector<std::vector<Point> > contours;
    std::vector<Vec4i> hierarchy;

    /// Find contours
    findContours(src, contours, hierarchy, CV_RETR_TREE, CV_CHAIN_APPROX_NONE, Point(0, 0));

    /// Find the convex hull object for each contour
    std::vector<std::vector<Point> > hull(contours.size());
    for (int i = 0; i < contours.size(); i++) { convexHull(Mat(contours[i]), hull[i], false); }

    /// Draw contours + hull results
    Mat drawing = Mat::zeros(scr_copy.size(), CV_8UC3);
    for (int i = 0; i < contours.size(); i++) {
        Scalar color = Scalar(rng.uniform(0, 255), rng.uniform(0, 255), rng.uniform(0, 255));
        drawContours(drawing, contours, i, color, 1, 8, std::vector<Vec4i>(), 0, Point());
        drawContours(drawing, hull, i, color, 1, 8, std::vector<Vec4i>(), 0, Point());
    }

    return drawing;
}

bool getContoursLine(Mat input, Mat &output) {
    double start = _getTickTime();

    if (countNonZero(input) == 0) {
        LOGD("getContoursLine(countNonZero)");
        return false;
    }

    std::vector<std::vector<Point> > contours; //외곽선 배열
    std::vector<Vec4i> hierarchy;//외곽선들 간의 계층구조

    findContours(input, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_NONE);

    output = Mat::zeros(input.size(), CV_8UC1);
    for (int i = 0; i < contours.size(); i++) {
        Scalar color = Scalar(80);
        drawContours(output, contours, i, color, 1, 8, hierarchy, 0, Point2i());
    }

    LOGD("[time-check] getContoursLine : %f", _getTickTime() - start);
    return true;
}

Rect getContoursRect(Mat input, Mat &output) {
    double start = _getTickTime();

    RNG rng(12345);
    std::vector<std::vector<Point> > contours; //외곽선 배열
    std::vector<Vec4i> hierarchy;//외곽선들 간의 계층구조

    findContours(input, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_NONE);

    /// Approximate contours to polygons + get bounding rects and circles
    std::vector<std::vector<Point> > contours_poly(contours.size());
    std::vector<Rect> boundRect(contours.size());

    LOGD("contours.size() : %d", contours.size());

    for (int i = 0; i < contours.size(); i++) {
        approxPolyDP(Mat(contours[i]), contours_poly[i], 3, true);
        boundRect[i] = boundingRect(Mat(contours_poly[i]));
    }

    /// Draw polygonal contour + bonding rects + circles
    output = Mat::zeros(input.size(), CV_8UC1);
    for (int i = 0; i < contours.size(); i++) {
        Scalar color = Scalar(rng.uniform(0, 255), rng.uniform(0, 255), rng.uniform(0, 255));
        rectangle(output, boundRect[i].tl(), boundRect[i].br(), color, 2, 8, 0);
    }

    int i = 0;
    for (i = 0; i < contours.size(); i++) {
        LOGD("rect[%d] : %d %d %d %d\n", i, boundRect[i].tl().x, boundRect[i].tl().y,
             boundRect[i].br().x,
             boundRect[i].br().y);
    }

    LOGD("[time-check] getBoxPoint : %f", _getTickTime() - start);

    if (i == 0) return Rect(0, 0, 0, 0);
    else
        return Rect(boundRect[i - 1].tl().x, boundRect[i - 1].tl().y,
                    boundRect[i - 1].br().x - boundRect[i - 1].tl().x + 1,
                    boundRect[i - 1].br().y - boundRect[i - 1].tl().y + 1);
}

float gObjectSize = -1.f;
float gOriginalScaleX = -1.f;
float gOriginalScaleY = -1.f;
float gAfterScaleX = -1.f;
float gAfterScaleY = -1.f;

void reszieScale(Mat input, Rect boxRect, float &scaleX, float &scaleY)
{
    float objSizePercent = (float)(boxRect.width * boxRect.height) * 100.f / (float)(input.cols * input.rows);
    gObjectSize = objSizePercent;

    SF_COMMON_TUNES datas;
    gTuneManagerP.getCommonTune(datas);

    float X1 = (float)datas.minFaceSize;
    float X2 = (float)datas.maxFaceSize;
    float Y1 = (float)datas.minMovingPercent;
    float Y2 = (float)datas.maxMovingPercent;
    LOGD("[objPercent] objSizePercent : %f", objSizePercent);
    LOGD("[objPercent] X1 : %f, Y1 : %f, X2 : %f, Y2 : %f", X1, Y1, X2, Y2);

    float calcA = (Y2 - Y1) / (X2 - X1);
    float calcB = Y1 - (calcA * X1);
    LOGD("[objPercent] calcA : %f, calcB : %f", calcA, calcB);

    float usePercent = calcA * objSizePercent + calcB;
    LOGD("[objPercent] usePercent : %f", usePercent);

    if( Y1 > Y2 )
    {
        if(usePercent > Y1) usePercent = Y1;
        else if(usePercent < Y2) usePercent = Y2;
    }
    else
    {
        if(usePercent > Y2) usePercent = Y2;
        else if(usePercent < Y1) usePercent = Y1;
    }

    LOGD("[objPercent] before scaleX : %f, scaleY : %f", scaleX, scaleY);

    gOriginalScaleX = scaleX;
    gOriginalScaleY = scaleY;

    scaleX = scaleX * usePercent / 100.f;
    scaleY = scaleY * usePercent / 100.f;
//    scaleX = 1.0f + ((scaleX - 1.0f) * usePercent / 100.f);
//    scaleY = 1.0f + ((scaleY - 1.0f) * usePercent / 100.f);

    gAfterScaleX = scaleX;
    gAfterScaleY = scaleY;
    LOGD("[objPercent] after scaleX : %f, scaleY : %f", scaleX, scaleY);
}

void getObjScaleValue(float *datas)
{
    datas[0] = gObjectSize;
    datas[1] = gOriginalScaleX;
    datas[2] = gOriginalScaleY;
    datas[3] = gAfterScaleX;
    datas[4] = gAfterScaleY;
}

void setScale2(Mat &input, Mat &output, float rateX, float rateY, Mat &contours) {
    double start = _getTickTime();

    LOGD("[setScale2] Before rateX : %f, rateY : %f", rateX, rateY);

    Mat contoursMat;
    Rect boxRect = getContoursRect(input, contoursMat);
    contours = contoursMat.clone();

    reszieScale(input, boxRect, rateX, rateY);
    LOGD("[setScale2] After rateX : %f, rateY : %f", rateX, rateY);

    float useRateX = rateX;
    float useRateY = rateY;
    if( gRotateInfo == 0 || gRotateInfo == 180 )
    {
        useRateX = rateY;
        useRateY = rateX;
    }

    float addX = ((boxRect.width * useRateX) - boxRect.width) / 2.0f;
    //float addY = 0.f;//((boxRect.height * rate) - boxRect.height) / 2.0f;
    float addY = ((boxRect.height * useRateY) - boxRect.height) / 2.0f;

    float ori[8] = {(float) boxRect.tl().x, (float) boxRect.tl().y,
                    (float) (boxRect.tl().x + boxRect.width), (float) boxRect.tl().y,
                    (float) (boxRect.br().x - boxRect.width), (float) boxRect.br().y,
                    (float) boxRect.br().x, (float) boxRect.br().y};

    float tar[8] = {(float) boxRect.tl().x - addX, (float) boxRect.tl().y - addY,
                    (float) boxRect.tl().x + (float) boxRect.width + addX,
                    (float) boxRect.tl().y - addY,
                    (float) boxRect.br().x - (float) boxRect.width - addX,
                    (float) boxRect.br().y + addY, (float) boxRect.br().x + addX,
                    (float) boxRect.br().y + addY};

    Mat scale1(4, 2, CV_32F, ori);
    Mat scale2(4, 2, CV_32F, tar);

    Mat scaleRvalue = getPerspectiveTransform(scale1, scale2);
    warpPerspective(input, output, scaleRvalue, input.size(), INTER_LINEAR);

    LOGD("[time-check] setScale : %f", _getTickTime() - start);
}

void setScale(Mat &input, Mat &output, float rate) {
    double start = _getTickTime();

    Rect2f boxRect = Rect2f(0.f, 0.f, (float) input.cols, (float) input.rows);

    float addX = ((boxRect.width * rate) - boxRect.width) / 2.0f;
    float addY = 0.f;//((boxRect.height * rate) - boxRect.height) / 2.0f;

    float ori[8] = {boxRect.tl().x, boxRect.tl().y,
                    boxRect.tl().x + boxRect.width, boxRect.tl().y,
                    boxRect.br().x - boxRect.width, boxRect.br().y,
                    boxRect.br().x, boxRect.br().y};

    float tar[8] = {boxRect.tl().x - addX, boxRect.tl().y - addY,
                    boxRect.tl().x + boxRect.width + addX,
                    boxRect.tl().y - addY,
                    boxRect.br().x - boxRect.width - addX,
                    boxRect.br().y + addY, boxRect.br().x + addX,
                    boxRect.br().y + addY};

    Mat scale1(4, 2, CV_32F, ori);
    Mat scale2(4, 2, CV_32F, tar);

    Mat scaleRvalue = getPerspectiveTransform(scale1, scale2);
    warpPerspective(input, output, scaleRvalue, input.size(), INTER_NEAREST);

    LOGD("[time-check] setScale : %f", _getTickTime() - start);
}

void setTranslation(Mat &input, Mat &output, float targetX, float targetY) {
    float transValue[6] = {1.0f, 0.0f, targetX, 0.0f, 1.0f, targetY};//구체적으로 초기화
    Mat transMat(2, 3, CV_32F, transValue);
    warpAffine(input, output, transMat, Size(input.cols, input.rows), INTER_NEAREST);
}

void tuneSegmentSlow(Mat &input, Mat &output, int rotate)
{
    //SF_FEATHER_TUNE fastInfo, slowInfo;
    //gTuneManagerSF.getFeatherTuneData(fastInfo, slowInfo);

    SF_FEATHER_TUNES superFastInfo, fastInfo, slowInfo;
    gTuneManagerP.getCompensationTune(superFastInfo, fastInfo, slowInfo);

    resize(input, input, Size(1440/slowInfo.resizeXY, 1080/slowInfo.resizeXY), 0, 0, INTER_NEAREST);

    int faderStart = slowInfo.scaleXcartoon;
    if( faderStart < 0)
    {
        erode(input, input, Mat(), Point(-1, -1), abs(faderStart));
    }
    else
    {
        dilate(input, input, Mat(), Point(-1, -1), abs(faderStart));
    }

    if( rotate == 0 ) {
        Mat maskErodeSize = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(1, 5),
                                                      cv::Point(-1, -1));
        if (slowInfo.scaleYcartoon > 0) {
            erode(input, input, maskErodeSize, Point(-1, -1), slowInfo.scaleYcartoon );
        } else {
            dilate(input, input, maskErodeSize, Point(-1, -1), -slowInfo.scaleYcartoon );
        }

        setTranslation(input, input, slowInfo.transUD, -slowInfo.transLR);
    }
    else if( rotate == 90 ) {
        Mat maskErodeSize = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(5, 1),
                                                      cv::Point(-1, -1));
        if (slowInfo.scaleYcartoon > 0) {
            erode(input, input, maskErodeSize, Point(-1, -1), slowInfo.scaleYcartoon );
        } else {
            dilate(input, input, maskErodeSize, Point(-1, -1), -slowInfo.scaleYcartoon );
        }

        setTranslation(input, input, -slowInfo.transLR, -slowInfo.transUD);
    }
    else if( rotate == 180 ) {
        Mat maskErodeSize = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(1, 5),
                                                      cv::Point(-1, -1));
        if (slowInfo.scaleYcartoon > 0) {
            erode(input, input, maskErodeSize, Point(-1, -1), slowInfo.scaleYcartoon );
        } else {
            dilate(input, input, maskErodeSize, Point(-1, -1), -slowInfo.scaleYcartoon );
        }

        setTranslation(input, input, -slowInfo.transUD, slowInfo.transLR);
    }
    else if( rotate == 270 ) {
        Mat maskErodeSize = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(5, 5),
                                                      cv::Point(-1, -1));
        if (slowInfo.scaleYcartoon > 0) {
            erode(input, input, maskErodeSize, Point(-1, -1), slowInfo.scaleYcartoon );
        } else {
            dilate(input, input, maskErodeSize, Point(-1, -1), -slowInfo.scaleYcartoon );
        }

        setTranslation(input, input, slowInfo.transLR, slowInfo.transUD);
    }

    //GaussianBlur(input, output, Size(slowInfo.mFThickness, slowInfo.mFThickness), 11.0);
}

void tuneSegmentFast(Mat &input, Mat &output)
{
    SF_FEATHER_TUNE fastInfo, slowInfo;
    gTuneManagerSF.getFeatherTuneData(fastInfo, slowInfo);

    resize(input, input, Size(1440/fastInfo.mResizeX, 1080/fastInfo.mResizeY), 0, 0, INTER_NEAREST);

    if( fastInfo.mFStart < 0)
    {
        erode(input, input, Mat(), Point(-1, -1), abs(fastInfo.mFStart));
    }
    else
    {
        dilate(input, input, Mat(), Point(-1, -1), abs(fastInfo.mFStart));
    }

    setTranslation(input, input, fastInfo.mTransUD, -fastInfo.mTransLR);

    GaussianBlur(input, output, Size(fastInfo.mFThickness, fastInfo.mFThickness), 11.0);
}

void setStudioMode(Mat &input, Mat &output, int flipMode)
{
    int smallForSpeed = SMALL_FOR_SPEED;

    float *faceArray;
    int i = 0;
    faceArray = &(gFaceArray[i * 4]);
    Rect rect = Rect(faceArray[0] / (float) smallForSpeed,
                     faceArray[2] / (float) smallForSpeed,
                     (faceArray[1] - faceArray[0]) / (float) smallForSpeed,
                     (faceArray[3] - faceArray[2]) / (float) smallForSpeed);

    setDarkSnowData(Mat(), 1440, 1080, true, rect);

    Mat studioMat = input.clone();
    if( flipMode == 1 )
    {
        flip(studioMat, studioMat, 90);
        cvtColor(studioMat, studioMat, CV_RGBA2GRAY);
    }

    //gFrontStatus = true;
    resize(studioMat, studioMat, Size(1440/smallForSpeed, 1080/smallForSpeed), 0, 0, INTER_NEAREST);
//    output = makeStudioFader(studioMat, gFrontStatus, gFaceCount > 0 ? true : false,
//                    false);
    output = makeStudioFader(studioMat, gFrontStatus, gIsPesron,
                             false);
}

void setSegmentData(Mat &srcMat, Mat &segMat, Mat &result, Mat &studio_result, int useFast, int rotate) {

    double startTime = _getTickTime();
    int waterBlur;
    SF_WATER_TUNES markerTune, outerTune;
    if( useFast == 1 )
    {
        //not use
        //gTuneManagerSF.getFastWaterTuneData(mMarkerData, mOuterData);
    }
    else
    {
        //gTuneManagerSF.getSlowWaterTuneData(mMarkerData, mOuterData);
        waterBlur = gTuneManagerP.getWaterTune(markerTune, outerTune);
    }

    LOGD("segMat : %d %d %d", segMat.size().width, segMat.size().height, countNonZero(segMat));

    mSegmentationData = segMat.clone();
    mSegmentationData.convertTo(mSegmentationData, CV_8UC1);
    Mat matRotation = getRotationMatrix2D(Point(mSegmentationData.cols / 2, mSegmentationData.rows / 2), rotate+90, 1);
    warpAffine(mSegmentationData, mSegmentationData, matRotation, mSegmentationData.size(), INTER_NEAREST);
    threshold(mSegmentationData, mSegmentationData, 15, 0, CV_THRESH_TOZERO_INV);
    threshold(mSegmentationData, mSegmentationData, 14, 40, CV_THRESH_BINARY);

    resize(mSegmentationData, mSegmentationData, Size(1440/markerTune.resize, 1080/markerTune.resize), 0, 0, INTER_NEAREST);
//    if( gFrontStatus == 0 )
//    {
//        flip(mSegmentationData, mSegmentationData, 90);
//    }
    //imwrite("/sdcard/water/mSegment.jpg", mSegmentationData*5);

    Mat src, segment, watershed, mask;
    Mat marker, outer1, outer2, outer3, outer4;
    Mat feather;

    double startTimeX = _getTickTime();
    src = srcMat.clone();
    resize(src, src, mSegmentationData.size());
    if( gFrontStatus == 1 ) {
        flip(src, src, 90);
    }
    cvtColor(src, src, CV_RGBA2BGR);
    //imwrite("/sdcard/tensorflow/src.jpg", src);
    LOGD("[time-how] Make Src Rotation : %f", _getTickTime() - startTimeX);

    //...
    Mat copySrcMat;
    copySrcMat = src.clone();
    int humanBlur = waterBlur;
    LOGD("humanBlur : %d", humanBlur);
    blur(copySrcMat,  copySrcMat, Size(humanBlur, humanBlur));
    copySrcMat.copyTo(src, mSegmentationData);
    //imwrite("/sdcard/humanBlur.jpg", src);

    double startTimeS = _getTickTime();

    LOGD("[TuneValues] markerTune.scale : %d, markerTune.leftRightScale : %d, markerTune.moveX : %f, markerTune.moveY : %f", markerTune.scale, markerTune.leftRightScale, markerTune.moveX, markerTune.moveY);

    //scale
    Mat maskErodeScale = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(3, 3), cv::Point(-1, -1));
    erode(mSegmentationData, marker, maskErodeScale, Point(-1, -1), markerTune.scale);

    //???
//    erode(mSegmentationData, outer3, maskErodeScale, Point(-1, -1), 25/*mMarkerData.scale*/);
//    static Mat zeroMat = Mat::zeros(mSegmentationData.size(), mSegmentationData.channels());
//    LOGD("zeroMat channel : %d", zeroMat.channels());
//
//    //zeroMat.copyTo(marker, outer3);
//    marker = marker - outer3;
//    getContoursLine(outer3, outer4);
//    //threshold(outer3, outer3, 30, 80, CV_THRESH_BINARY);

    //JJabu
    if( rotate == 0 ) {
        Mat maskErodeSize = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(1, 5),
                                                      cv::Point(-1, -1));
        if (markerTune.leftRightScale > 0) {
            erode(marker, marker, maskErodeSize, Point(-1, -1), markerTune.leftRightScale);
        } else {
            dilate(marker, marker, maskErodeSize, Point(-1, -1), -markerTune.leftRightScale);
        }

        setTranslation(marker, marker, markerTune.moveY, -markerTune.moveX);
    } else if( rotate == 90 ) {
        Mat maskErodeSize = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(5, 1),
                                                      cv::Point(-1, -1));
        if (markerTune.leftRightScale > 0) {
            erode(marker, marker, maskErodeSize, Point(-1, -1), markerTune.leftRightScale);
        } else {
            dilate(marker, marker, maskErodeSize, Point(-1, -1), -markerTune.leftRightScale);
        }

        setTranslation(marker, marker, -markerTune.moveX, -markerTune.moveY);
    } else if( rotate == 180 ) {
        Mat maskErodeSize = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(1, 5),
                                                      cv::Point(-1, -1));
        if (markerTune.leftRightScale > 0) {
            erode(marker, marker, maskErodeSize, Point(-1, -1), markerTune.leftRightScale);
        } else {
            dilate(marker, marker, maskErodeSize, Point(-1, -1), -markerTune.leftRightScale);
        }

        setTranslation(marker, marker, -markerTune.moveY, markerTune.moveX);
    } else if( rotate == 270 ) {
        Mat maskErodeSize = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(5, 1),
                                                      cv::Point(-1, -1));
        if (markerTune.leftRightScale > 0) {
            erode(marker, marker, maskErodeSize, Point(-1, -1), markerTune.leftRightScale);
        } else {
            dilate(marker, marker, maskErodeSize, Point(-1, -1), -markerTune.leftRightScale);
        }

        setTranslation(marker, marker, markerTune.moveX, markerTune.moveY);
    }

    LOGD("[TuneValues] outerTune.scale : %d, outerTune.leftRightScale : %d, outerTune.moveX : %f, outerTune.moveY : %f", outerTune.scale, outerTune.leftRightScale, outerTune.moveX, outerTune.moveY);

    //scale
    Mat maskDilateScale = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(3, 3), cv::Point(-1, -1));
    dilate(mSegmentationData, outer1, maskDilateScale, Point(-1, -1), outerTune.scale);

    //JJabu
    if( rotate == 0 ) {
        Mat maskDilateSize = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(1, 5),
                                                       cv::Point(-1, -1));
        if (outerTune.leftRightScale > 0) {
            erode(outer1, outer1, maskDilateSize, Point(-1, -1), outerTune.leftRightScale);
        } else {
            dilate(outer1, outer1, maskDilateSize, Point(-1, -1), -outerTune.leftRightScale);
        }

//        setTranslation(outer1, outer1, outerTune.moveY, -outerTune.moveX);
        //imwrite("/sdcard/water/outer1.jpg", outer1);
        Mat temp;
        setTranslation(outer1, temp, outerTune.moveY, -outerTune.moveX);
        temp.copyTo(outer1, temp);
        //imwrite("/sdcard/water/outer2.jpg", outer1);
    } else if( rotate == 90 ) {
        Mat maskDilateSize = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(5, 1),
                                                       cv::Point(-1, -1));
        if (outerTune.leftRightScale > 0) {
            erode(outer1, outer1, maskDilateSize, Point(-1, -1), outerTune.leftRightScale);
        } else {
            dilate(outer1, outer1, maskDilateSize, Point(-1, -1), -outerTune.leftRightScale);
        }

        //setTranslation(outer1, outer1, -outerTune.moveX, -outerTune.moveY);
        Mat temp;
        setTranslation(outer1, temp, -outerTune.moveX, -outerTune.moveY);
        temp.copyTo(outer1, temp);
    } else if( rotate == 180 ) {
        Mat maskDilateSize = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(1, 5),
                                                       cv::Point(-1, -1));
        if (outerTune.leftRightScale > 0) {
            erode(outer1, outer1, maskDilateSize, Point(-1, -1), outerTune.leftRightScale);
        } else {
            dilate(outer1, outer1, maskDilateSize, Point(-1, -1), -outerTune.leftRightScale);
        }

        //setTranslation(outer1, outer1, -outerTune.moveY, outerTune.moveX);
        Mat temp;
        setTranslation(outer1, temp, -outerTune.moveY, outerTune.moveX);
        temp.copyTo(outer1, temp);
    } else if( rotate == 270 ) {
        Mat maskDilateSize = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(5, 1),
                                                       cv::Point(-1, -1));
        if (outerTune.leftRightScale > 0) {
            erode(outer1, outer1, maskDilateSize, Point(-1, -1), outerTune.leftRightScale);
        } else {
            dilate(outer1, outer1, maskDilateSize, Point(-1, -1), -outerTune.leftRightScale);
        }

        //setTranslation(outer1, outer1, outerTune.moveX, outerTune.moveY);
        Mat temp;
        setTranslation(outer1, temp, outerTune.moveX, outerTune.moveY);
        temp.copyTo(outer1, temp);
    }

    //here fix...
    mask = marker + outer1;

    //imwrite("/sdcard/water/mask.jpg", mask);
    threshold(mask, mask, 70, 40, CV_THRESH_BINARY);
    //imwrite("/sdcard/water/mask2.jpg", mask);

    bool resultStatus = getContoursLine(outer1, outer2);
    LOGD("[time-how] Make getStructuringElement : %f", _getTickTime() - startTimeS);

    if( resultStatus == true ) {
        //mask = marker + outer2;
        outer2.copyTo(mask, outer2);
    }
    else
    {
        mask = marker;
    }

    //imwrite("/sdcard/water/maskPre.jpg", mask);
    //imwrite("/sdcard/water/maskPost.jpg", marker + outer2);

    double startTimeW = _getTickTime();
    waterShed(src, segment, watershed, mask, false);
    if( gIsSave == true )
    {
        //imwrite("/sdcard/water/src.jpg", src);
        //imwrite("/sdcard/water/mask.jpg", mask);
        Mat temp;
        cvtColor(mask, temp, CV_GRAY2RGB);
        Mat dst;

        LOGD("src channel : %d mask : %d", src.channels(), mask.channels());

        addWeighted( src, 0.3, temp, 0.7, 0.0, dst);

        Mat water;
        threshold(watershed, water, 200, 255, CV_THRESH_BINARY_INV);
        cvtColor(water, water, CV_GRAY2RGB);
        water.copyTo(dst, water);

        imwrite("/sdcard/water/preview.jpg", dst);
        imwrite("/sdcard/water/segment.jpg", segment);
        imwrite("/sdcard/water/watershed.jpg", water);
    }
    //imwrite("/sdcard/temp/src.jpg", src);
    //imwrite("/sdcard/water/mask.jpg", mask);
    LOGD("[time-how] Make waterShed : %f", _getTickTime() - startTimeW);

    //imwrite("/sdcard/water/segment.jpg", segment);

    int resizeSeg = 8;

    static Mat empty_image = Mat::zeros(1080/resizeSeg, 1440/resizeSeg, CV_8UC1);//initial empty layer
    static Mat resultSegment(1080/resizeSeg, 1440/resizeSeg, CV_8UC4);//initial blue result
    static Mat in[4];

    double startTimeT = _getTickTime();
    threshold(segment, feather, 70, 0, CV_THRESH_TOZERO_INV);
    threshold(feather, feather, 30, 255, CV_THRESH_BINARY);

    //resize(feather, feather, Size(1440, 1080), 0, 0, INTER_LINEAR);
    LOGD("Make threshold : %f", _getTickTime() - startTimeT);
    double startTimeG = _getTickTime();
    //GaussianBlur(feather, feather, Size(5, 5), 11.0);
    //blur(feather, feather, Size(53, 53));
    if( useFast == 1 )
    {
        tuneSegmentFast(feather, feather);
    }
    else
    {
        tuneSegmentSlow(feather, feather, rotate);
    }

    LOGD("[time-how] Make GaussianBlur : %f", _getTickTime() - startTimeG);

    double startTimeF = _getTickTime();

    resize(feather, feather, Size(1440/resizeSeg, 1080/resizeSeg), 0, 0, INTER_NEAREST);
    if( gTuneManagerBlur.getStudioMode() > -1 )
    {
        double startTime = _getTickTime();
        setStudioMode(feather, studio_result, 0);
        LOGD("[time-how] Make Studio : %f", _getTickTime() - startTime);
    }
    else
    {
        static Mat empty_image = Mat::zeros(feather.rows, feather.cols, CV_8UC4);//initial empty layer
        studio_result = empty_image;
    }

    resize(mask, mask, Size(1440/resizeSeg, 1080/resizeSeg), 0, 0, INTER_NEAREST);
    threshold(watershed, watershed, 100, 255, CV_THRESH_BINARY_INV);
    resize(watershed, watershed, Size(1440/resizeSeg, 1080/resizeSeg), 0, 0, INTER_NEAREST);
    resize(segment, segment, Size(1440/resizeSeg, 1080/resizeSeg), 0, 0, INTER_NEAREST);
    LOGD("[time-how] Make feather : %f", _getTickTime() - startTimeF);

    in[0] = mask;
    in[1] = watershed;
    in[2] = feather;
    in[3] = empty_image;

    double startTimeM = _getTickTime();
    int from_to[] = {0, 0, 1, 1, 2, 2, 3, 3};
    mixChannels(in, 4, &resultSegment, 1, from_to, 4);//combine image
    LOGD("[time-how] Make Mix-1 : %f", _getTickTime() - startTimeM);

    //imwrite("/sdcard/water/feather.jpg", feather);
    //imwrite("/sdcard/obj/resultSegment.jpg", resultSegment);
    result = resultSegment.clone();
    LOGD("[time-how] Make Mix-2 : %f", _getTickTime() - startTimeM);

    LOGD("[time-how] Make Feather : %f", _getTickTime() - startTime);

//    double startTime = _getTickTime();
//    mSegmentationData = segMat.clone();
//    mSegmentationData.convertTo(mSegmentationData, CV_8UC1);
//    Mat matRotation = getRotationMatrix2D(
//            Point(mSegmentationData.cols / 2, mSegmentationData.rows / 2), 90, 1);
//    warpAffine(mSegmentationData, mSegmentationData, matRotation, mSegmentationData.size(),
//               INTER_NEAREST);
//    threshold(mSegmentationData, mSegmentationData, 14, 255, CV_THRESH_BINARY);
//
//    resize(mSegmentationData, mSegmentationData, Size(1440 / 2, 1080 / 2), 0, 0, INTER_CUBIC);
//    GaussianBlur(mSegmentationData, mSegmentationData, Size(53, 53), 11.0);
//
//    resize(mSegmentationData, mSegmentationData, Size(1440, 1080), 0, 0, INTER_NEAREST);
//    cvtColor(mSegmentationData, mSegmentationData, CV_RGB2BGRA);
//    result = mSegmentationData.clone();

    return;
}

void setSaveMovieFrontStatus(bool isSave, bool isMovie, bool isFront, bool isPerson, float brightness) {
    gIsSave = isSave;
    gIsMovie = isMovie;
    gIsPesron = isPerson;
    gFrontStatus = isFront;

//    gTuneManagerBlur.setCurrentStatus(isFront, (gFaceCount > 0 ? true : false), gIsSave,
//                                  gIsMovie);//front, face, save
    gTuneManagerBlur.setCurrentStatus(isFront, isPerson, gIsSave,
                                      gIsMovie);//front, face, save
    gTuneManagerBlur.setCurrentBright(brightness);

//    gTuneManagerP.setCurrentStatus(isFront, (gFaceCount > 0 ? true : false), gIsSave,
//                                      gIsMovie);//front, face, save
    gTuneManagerP.setCurrentStatus(isFront, isPerson, gIsSave,
                                   gIsMovie);//front, face, save
    gTuneManagerP.setCurrentBright(brightness);
}

int gCurrentAlMode = 1;
void setCurrentAlMode(int mode)
{
    gCurrentAlMode = mode;
}

int process(Mat &img_input, Mat &img_result, Mat &img_studio_result) {

//    if (gIsSave == true) {
//        LOGD("gIsSave : %d", gIsSave);
//        return 0;
//    }

    static int fpsCount = 0;
    if (checkTimeFps() == true) {
        LOGD("process fps : %d ", fpsCount);
        fpsCount = 0;
    } else {
        fpsCount++;
    }

    int smallForSpeed = SMALL_FOR_SPEED;

    Mat image = img_input;
    resize(image, image, Size(1440 / smallForSpeed, 1080 / smallForSpeed), 0, 0, INTER_NEAREST);

    imwrite("/sdcard/test/image.jpg", image);

    if (gFrontStatus == true) {
        LOGD("flip image");
        flip(image, image, 90);
    }

    //cvtColor(image, image, CV_RGBA2BGR);

    static Mat backAlgMat = Mat::zeros(image.rows, image.cols, image.type());
    static Mat zeroMat = Mat::zeros(image.rows, image.cols, image.type());
    static Mat segment, watershed;
    Mat segment2 = Mat::zeros(image.rows, image.cols, CV_8UC1);
    Mat touchMat = Mat::zeros(image.rows, image.cols, CV_8UC1);
    static Mat analisysMat = Mat::zeros(image.rows, image.cols, CV_8UC1);
    static Mat analisysFaceMat = Mat::zeros(image.rows, image.cols, CV_8UC1);
    static Mat analisysResultMat = Mat::zeros(image.rows, image.cols, CV_8UC1);

    bool touchOn = false;
    if ((gTouchX > -1.0f && gTouchY > -1.0f)) {
        touchOn = true;
    }

    if (gObjMoreNumber > 0 && touchOn == false) {
        Mat temp = Mat::zeros(backAlgMat.rows, backAlgMat.cols, backAlgMat.type());

        getObjnTouchMask(image, backAlgMat, touchOn, gTouchX, gTouchY, touchMat, analisysMat,
                         0, gMultiTouchOn, gMinusTouchOn);
        temp += backAlgMat;

        for (int i = 0; i < gObjMoreNumber; i++) {
            int idx = getObjnTouchMask(image, backAlgMat, touchOn, gTouchX, gTouchY, touchMat,
                                       analisysMat,
                                       gObjMoreIndex[i], gMultiTouchOn, gMinusTouchOn);

            temp += backAlgMat;
        }

        threshold(temp, temp, 200, 200, CV_THRESH_TRUNC);
        backAlgMat = temp.clone();

        //analisysMat += analisysMat;
        //touchMat += touchMat;
    } else {
        int idx = getObjnTouchMask(image, backAlgMat, touchOn, gTouchX, gTouchY, touchMat,
                                   analisysMat,
                                   0, gMultiTouchOn, gMinusTouchOn);
    }

    //Scale
    SF_FEATHER_TUNES superFast, fast, slow;
    gTuneManagerP.getCompensationTune(superFast, fast, slow);
    Mat temp;

    //imwrite("/sdcard/scale/backAlgMatB.jpg", backAlgMat);
    if( gCurrentAlMode == 1 )
    {
        setScale2(backAlgMat, backAlgMat, fast.scaleXsf, fast.scaleYsf, temp);
    }
    else if( gCurrentAlMode == 2 )
    {
        setScale2(backAlgMat, backAlgMat, fast.scaleXcartoon, fast.scaleYcartoon, temp);
    }
    else if( gCurrentAlMode == 3 )
    {
        setScale2(backAlgMat, backAlgMat, fast.scaleXblur, fast.scaleYblur, temp);
    }
    //imwrite("/sdcard/scale/backAlgMatA.jpg", backAlgMat);

    //Mat newWaterLine;

    //test 20180227
    Mat useAlgMat;
    Mat faceOriMat;
    Mat new40Mat = Mat::zeros(segment2.size(), segment2.type());
    Mat newMaskMat = Mat::zeros(segment2.size(), segment2.type());

    LOGD("[SofAlg] new alg");
    watershed = zeroMat;

    segment2 = makeCloseMask(image, backAlgMat, new40Mat, newMaskMat);
    if (gTuneManagerBlur.getStudioMode() > -1) {
        setDarkSnowData(backAlgMat, 1440, 1080, false, Rect(0, 0, 0, 0));
        //need to test...
        //makeStudioFader(backAlgMat, gFrontStatus, gFaceCount > 0 ? true : false, gUseBlurAlg);
        img_studio_result = makeStudioFader(backAlgMat, gFrontStatus, false, false);
    }
    else
    {
        static Mat empty_image = Mat::zeros(backAlgMat.rows, backAlgMat.cols, CV_8UC4);//initial empty layer
        img_studio_result = empty_image;
    }

    static Mat empty_image = Mat::zeros(segment2.rows, segment2.cols, CV_8UC1);//initial empty layer
    static Mat resultSegment(segment2.rows, segment2.cols,
                             CV_8UC4);                 //initial blue result

//    LOGD("[size-test] newWaterLine : %d %d", newWaterLine.cols, newWaterLine.rows,
//         newWaterLine.type());
    LOGD("[size-test] segment2 : %d %d %d", segment2.cols, segment2.rows, segment2.type());
    LOGD("[size-test] gFaderMat : %d %d %d", gFaderMat.cols, gFaderMat.rows, gFaderMat.type());

    //need to test
    analisysResultMat.setTo(0);

    Mat in1[] = {segment2, new40Mat, gFaderMat, analisysResultMat};    //construct 3 layer Matrix
    int from_to1[] = {0, 0, 1, 1, 2, 2, 3, 3};
    mixChannels(in1, 4, &resultSegment, 1, from_to1, 4);          //combine image

//    cvtColor(newWaterLine, newWaterLine, CV_GRAY2RGBA);

//    LOGD("[gIsSave] gUseBlurAlg : %d, gIsSave : %d", gUseBlurAlg, gIsSave);
//
//    if (gUseBlurAlg == false && gIsSave == false) {
//        //if( gFaceCount > 0 )
//        if (gTuneManagerBlur.getStudioMode() != 3) {
//            LOGD("[Moving-Test-Sync] here1???");
//            resultSegment.setTo(40);
//        } else {
//            LOGD("[Moving-Test-Sync] here2???");
//            resultSegment.setTo(0);
//            analisysMat.setTo(15);
//            analisysFaceMat.setTo(15);
//        }
//    }

    sofImwrite("/sdcard/test/segment2.jpg", segment2);
    sofImwrite("/sdcard/test/gFaderMat.jpg", gFaderMat);
    sofImwrite("/sdcard/test/what.jpg", resultSegment);

    if (gFrontStatus == true) {
        LOGD("[flip] flip");
        //flip(newWaterLine, newWaterLine, 90);
        //mongmi???
        flip(segment, segment, 90);
        flip(watershed, watershed, 90);
        //flip(imageMask, imageMask, 90);

        flip(segment2, segment2, 90);
        flip(resultSegment, resultSegment, 90);

        flip(gFaderMat, gFaderMat, 90);
        flip(backAlgMat, backAlgMat, 90);

        flip(new40Mat, new40Mat, 90);
        flip(newMaskMat, newMaskMat, 90);

        flip(touchMat, touchMat, 90);
    }

//    //if( gInitEGLExtension == true && stopMaskProccess == false ) {
//    if (gInitEGLExtension == true) {
//        LOGD("gInitEGLExtension1 : %d", gInitEGLExtension);
//        creatEGLImage(resultSegment, 0, false);
//        img_result = resultSegment;
//    } else {
//        LOGD("gInitEGLExtension1 : %d", gInitEGLExtension);
//    }

    img_result = resultSegment;
    //imwrite("/sdcard/obj/backAlgMat.jpg", backAlgMat);
    //imwrite("/sdcard/obj/segment2.jpg", segment2);
    //imwrite("/sdcard/obj/new40Mat.jpg", new40Mat);

//    static Mat empty_image2 = Mat::zeros(resultSegment.rows, resultSegment.cols,
//                                         CV_8UC1);//initial empty layer
//    static Mat resultSegment2(resultSegment.rows, resultSegment.cols,
//                              CV_8UC4);                 //initial blue result
//    static Mat in2[4];
//
//    threshold(watershed, watershed, 100, 255, CV_THRESH_BINARY_INV);
//
//    //modify here...
//    int gFaceCount = 0;
//    if (gFaceCount > 0) {
//        threshold(new40Mat, new40Mat, 70, 0, CV_THRESH_TOZERO_INV);
//        threshold(new40Mat, new40Mat, 30, 200, CV_THRESH_BINARY);
//
//        in2[0] = gFaderMat;
//        in2[1] = touchMat;
//        in2[2] = new40Mat;//newMaskMat;//empty_image2;//new40Mat;
//        in2[3] = empty_image2;
//    } else {
//        threshold(new40Mat, new40Mat, 30, 200, CV_THRESH_BINARY);
//
//        in2[0] = touchMat;
//        in2[1] = empty_image2;
//        in2[2] = new40Mat;
//        in2[3] = empty_image2;
//    }
//
//    int from_to2[] = {0, 0, 1, 1, 2, 2, 3, 3};
//    mixChannels(in2, 4, &resultSegment2, 1, from_to2, 4);//combine image
//    imwrite("/sdcard/obj/objSegment.jpg", resultSegment2);
//
//    img_result = resultSegment2;

    return 0;
}

//java to jni...
void setObjectMoreRect(int number, int *index) {
    gObjMoreNumber = number;
    for (int i = 0; i < gObjMoreNumber; i++) {
        gObjMoreIndex[i] = index[i];
        LOGD("[more-rect] gObjMoreIndex[%d] : %d", i, gObjMoreIndex[i]);
    }
}

void setObjectRect(int number, Rect2f *rectf) {
    gObjNumber = number;
    for (int i = 0; i < number; i++) {
        gObjRect[i] = rectf[i];
        LOGD("[tensor-test-jni] rectF[%d] left : %f, top : %f, right : %f, bottom : %f", i,
             gObjRect[i].tl().x, gObjRect[i].tl().y, gObjRect[i].br().x, gObjRect[i].br().y);
    }
}

void setObjectRectForTouch(int number, Rect2f *rectf, bool resetOn) {
    gObjTouchNumber = number;
    for (int i = 0; i < number; i++) {
        gObjTouchRect[i] = rectf[i];
        LOGD("[tensor-test-jni] rectF[%d] left : %f, top : %f, right : %f, bottom : %f", i,
             gObjTouchRect[i].tl().x, gObjTouchRect[i].tl().y, gObjTouchRect[i].br().x,
             gObjTouchRect[i].br().y);
    }

    LOGD("setObjectRectForTouch : %d %d", (int) rectf[0].width, (int) rectf[0].height);
    gObjTouchResetOn = false;
    if ((int) rectf[0].width == 36 && (int) rectf[0].height == 36) {
        gObjTouchResetOn = true;
    }
}

void setRotateInfo(int rotateInfo) {
    gRotateInfo = rotateInfo;
    LOGD("gRotateInfo : %d", gRotateInfo);
}

void setTouchData(float x, float y) {
    gTouchX = x;
    gTouchY = y;

    if (gTouchX > -1.0f && gTouchY > -1.0f) {
        gTouchX /= (float) SMALL_FOR_SPEED;
        gTouchY /= (float) SMALL_FOR_SPEED;
    }

    LOGD("[setTouchData] gTouchX : %f, gTouchY : %f", gTouchX, gTouchY);
}

void setTouchDataForMultiTouch(bool multiTouchOn, bool minusTouchOn, float x, float y) {

    gMultiTouchOn = multiTouchOn;
    gMinusTouchOn = minusTouchOn;
    gSaveTouchX = x;
    gSaveTouchY = y;

    if (gSaveTouchX > -1.0f && gSaveTouchY > -1.0f) {
        gSaveTouchX /= (float) SMALL_FOR_SPEED;
        gSaveTouchY /= (float) SMALL_FOR_SPEED;
    }

    LOGD("[setTouchData] gTouchX : %f, gTouchY : %f", gTouchX, gTouchY);
}
}

void FaceLocationMulti(int length, int *faceArray, int previewWidth, int previewHeight) {
    int faceCount = -1;

    LOGD("[Face-Multi] Count : %d", faceArray[0]);

    if (faceArray[0] > 5) {
        faceArray[0] = 5;
    }

    for (int i = 1; i < faceArray[0] * 4; i += 4) {
        LOGD("[Face-Multi] No : %d left:%d right:%d top:%d bottom:%d", i, faceArray[i],
             faceArray[i + 1], faceArray[i + 2], faceArray[i + 3]);

        int width = faceArray[i + 1] - faceArray[i];
        int height = faceArray[i + 3] - faceArray[i + 2];

        gFaceArray[++faceCount] = faceArray[i] > previewWidth ? previewWidth : (float) faceArray[i];
        gFaceArray[++faceCount] =
                faceArray[i + 1] > previewWidth ? previewWidth : (float) faceArray[i + 1];
        gFaceArray[++faceCount] =
                faceArray[i + 2] > previewHeight ? previewHeight : (float) faceArray[i + 2];
        gFaceArray[++faceCount] =
                faceArray[i + 3] > previewHeight ? previewHeight : (float) faceArray[i + 3];
    }

    for (int i = 0; i < faceCount + 1; i++) {
        LOGD("[Face-Multi] mFaceArray[%d] : %f", i, gFaceArray[i]);
    }

    gFaceCount = faceArray[0];
}

//void detectMovingObj(Mat &input, float thresholdValue) {
//    LOGD("[imageClone] %d %d %d", input.empty(), input.cols, input.rows);
//    Mat img = input.clone();
//
//    int smallForSpeed = SMALL_FOR_SPEED;
//    resize(img, img, Size(1440 / smallForSpeed, 1080 / smallForSpeed), 0, 0, INTER_NEAREST);
//    cvtColor(img, img, CV_RGBA2GRAY);
//    blur(img, img, Size(13, 13));
//
//    static Mat preImg;
//
//    if (gStartMoving == 1) {
//        gStartMoving = 0;
//
//        preImg = img.clone();
//        preImg.setTo(0);
//        LOGD("[imageClone] [detectMovingObj-face] preImg size : %d %d", preImg.cols, preImg.rows);
//        absdiff(img, preImg, gDiffImg);
//    } else {
//        absdiff(img, preImg, gDiffImg);
//        LOGD("[imageClone] [detectMovingObj-face] img2 size : %d %d", img.cols, img.rows);
//        LOGD("[imageClone] [detectMovingObj-face] preImg2 size : %d %d", preImg.cols, preImg.rows);
//        preImg = img.clone();
//    }
//
//    float percentNoZero = 0.0f;
////    if (gFaceCount > 0)
////    {
////        //threshold(gDiffImg, gDiffImg, 10, 255, CV_THRESH_BINARY);
////        //threshold(gDiffImg, gDiffImg, 50, 255, CV_THRESH_BINARY);
////        if (gFrontStatus == true) {
////            int thresholdValue = 50;
////            thresholdValue = gTuneManagerBlur.getMovingSensitivityValue(2);
////            threshold(gDiffImg, gDiffImg, thresholdValue, 255, CV_THRESH_BINARY);
////        } else {
////            int thresholdValue = 15;
////            thresholdValue = gTuneManagerBlur.getMovingSensitivityValue(1);
////            threshold(gDiffImg, gDiffImg, thresholdValue, 255, CV_THRESH_BINARY);
////        }
////
////        LOGD("[imageClone] gFaceArray : %f %f %f %f", gFaceArray[0], gFaceArray[1], gFaceArray[2],
////             gFaceArray[3]);
////
////        static Rect preFaceRect = Rect(0, 0, 0, 0);
////
////        Rect faceRect = Rect(gFaceArray[0] / smallForSpeed, gFaceArray[2] / smallForSpeed,
////                             (gFaceArray[1] - gFaceArray[0]) / smallForSpeed,
////                             (gFaceArray[3] - gFaceArray[2]) / smallForSpeed);
////
////        int newX, newY, newX2, newY2;
////        if (faceRect.tl().x < preFaceRect.tl().x) newX = faceRect.tl().x;
////        else newX = preFaceRect.tl().x;
////
////        if (faceRect.tl().y < preFaceRect.tl().y) newY = faceRect.tl().y;
////        else newY = preFaceRect.tl().y;
////
////        if (faceRect.br().x > preFaceRect.br().x) newX2 = faceRect.br().x;
////        else newX2 = preFaceRect.br().x;
////
////        if (faceRect.br().y > preFaceRect.br().y) newY2 = faceRect.br().y;
////        else newY2 = preFaceRect.br().y;
////
////        Rect newRect = Rect(newX, newY, newX2 - newX, newY2 - newY);
////
////        preFaceRect = faceRect;
////
////        Mat faceRoiMat = gDiffImg(newRect);
////        sofImwrite("/sdcard/bitrun.jpg", faceRoiMat);
////
////        Mat faceRoiMat2 = img(newRect);
////        sofImwrite("/sdcard/bitrun2.jpg", faceRoiMat2);
////
////        int noZero = cv::countNonZero(faceRoiMat);
////        percentNoZero = ((float) noZero / (float) (faceRoiMat.cols * faceRoiMat.rows)) * 100.0f;
////    }
//    if(false)
//    {
//    }
//    else {
//        LOGD("[imageClone] No gFaceArray");
//
//        int thresholdValue = 50;
//        thresholdValue = gTuneManagerBlur.getMovingSensitivityValue(0);
//        threshold(gDiffImg, gDiffImg, thresholdValue, 255, CV_THRESH_BINARY);
//        int noZero = cv::countNonZero(gDiffImg);
//        percentNoZero = ((float) noZero / (float) (gDiffImg.cols * gDiffImg.rows)) * 100.0f;
//    }
//
//    gPercentNoZero = percentNoZero;
//    thresholdValue = gTuneManagerBlur.getMovingValue();
//
//    if (gTouchX > -1.0f && gTouchY > -1.0f) {
//        thresholdValue = gTuneManagerBlur.getTouchBoundary();
//    }
//
//    //if( gObjNumber > 0 )//khkim moving
//    {
//        gUseAIAlg = true;
//
////        float thresholdValueForAi = 5.0f;
//        //float thresholdValueForAi = gTuneManagerBlur.getAiMoving();
//        float thresholdValueForAi = gTuneManagerBlur.getMovingValue();
//        if (gPercentNoZero > thresholdValueForAi) {
//            gUseAIAlg = false;
//        }
//    }
//
//    LOGD("[Moving-Test] gPercentNoZero : %f thresholdValue : %f", gPercentNoZero, thresholdValue);
//    if (gPercentNoZero > thresholdValue) {
//        LOGD("[Moving-Test] gUseBlurAlg : false");
//
//        gUseBlurAlg = false;
//        gUseBlurUiAlg = false;
//
//        LOGD("[Moving-Test-Sync] fMat.setTo(40)-gUseBlurAlg-1 : %d", gUseBlurAlg);
//
//        //if (gFaceCount > 0)
//        {
//            if (gInitEGLExtension == true) {
//                LOGD("gInitEGLExtension2 : %d", gInitEGLExtension);
//                //static Mat fMat = Mat(img.size(), img.type());
//                static Mat fMat = Mat(img.size(), CV_8UC4);
//                fMat.setTo(40);
//
//                if (gTuneManagerBlur.getStudioMode() != 3) {
//                    LOGD("[Moving-Test-Sync] fMat.setTo(40)-gUseBlurAlg-2 : %d", gUseBlurAlg);
//                    creatEGLImage(fMat, 0, true);
//                    //creatEGLImage(fMat, 2);
//                }
//            } else {
//                LOGD("gInitEGLExtension2 : %d", gInitEGLExtension);
//            }
//        }
//
//        LOGD("[Moving-Test-Why] checkTimeCT Start");
//        checkTimeCT(true, 500);
//    } else {
//        LOGD("[Moving-Test] gUseBlurAlg : true");
//        gUseBlurAlg = true;
//        LOGD("[Moving-Test-Sync] fMat.setTo(40)-gUseBlurAlg-3 : %d", gUseBlurAlg);
//
//        //LOGD("[Moving-Test-Why] checkTimeCT End : %d", checkTimeCT(false, 500));
//        if (checkTimeCT(false, 500)) {
//            gUseBlurUiAlg = true;
//        }
//    }
//}