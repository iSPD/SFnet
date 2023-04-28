#include "studioMode.h"
#include "sofTunner.h"
#include "module.h"
#include "utils.h"
#include "objAlg.h"
#include "sfPersonTunner.h"
#include <android/log.h>

// log
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "SofStudioCpp", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "SofStudioCpp", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "SofStudioCpp", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "SofStudioCpp", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "SofStudioCpp", __VA_ARGS__)

extern "C" {

Rect gStudioRect = Rect(1440/2 - 10, 1080/2 - 10, 10, 10);
float gPreviewW = 1440.f;
float gPreviewH = 1080.f;
extern tuneManagerBlur gTuneManagerBlur;
extern tuneManagerPersonSf gTuneManagerP;

float threshValue = 1.0f;
int gBlurSize = 3;
bool gVidioStatus = false;

float euclidean_distance(cv::Point center, cv::Point point, float startRadius, int radius){
    float distance = std::sqrt(
            std::pow(center.x - point.x, 2) + std::pow(center.y - point.y, 2));
    // printf("distance : %f\n", distance);

    if (distance > radius) return 0;
    //else if (distance < startRadius) return 0;

    //printf("distance : %f\n", distance);
    //printf("distance - startRadius : %f\n", distance - startRadius);

    float temp = distance - (startRadius);
    if (temp < 0.0f) temp = 0.0f;

//    float value = (temp / (225.f - startRadius)) * 255.f;
////    if (value > 0) value = value + 30.f;
//    if (value > 0) value = value + threshValue;

    //return distance - startRadius;
//    return value;
    return temp;
}

Mat makeCircle(int width, int height, int startRadius, int inradius, Point centerPoint){

    int h = height;
    int w = width;
    int radius = inradius;
    cv::Mat gradient = cv::Mat::zeros(h, w, CV_32F);

    cv::Point center = centerPoint;
    cv::Point point;

    printf("startRadius : %d\n", startRadius);
    for (int row = 0; row<h; ++row){
        for (int col = 0; col<w; ++col){
            point.x = col;
            point.y = row;
            gradient.at<float>(row, col) = euclidean_distance(center, point, (float)startRadius, radius);
        }
    }

//    Mat blackMat = Mat::zeros(gradient.size(), CV_8UC1);
//    Mat save;
//    gradient.convertTo(save, CV_8UC1);
//    threshold(save, save, 0, 255, CV_THRESH_BINARY_INV);
//    cv::normalize(gradient, gradient, (double)threshValue, 255.0, cv::NORM_MINMAX, CV_8U);
//    blackMat.copyTo(gradient, save);

#if 0
    //cv::normalize(gradient, gradient, 0, 400, cv::NORM_MINMAX, CV_8U);
	cv::normalize(gradient, gradient, threshValue, 255 + threshValue, cv::NORM_MINMAX, CV_8U);
	//cv::bitwise_not(gradient, gradient);
	gradient = gradient - threshValue;
#elif 0
    cv::normalize(gradient, gradient, 0, 255, cv::NORM_MINMAX, CV_8U);
    //cv::bitwise_not(gradient, gradient);
    gradient = gradient + threshValue;
    threshold(gradient, gradient, (int)threshValue, 0, CV_THRESH_TOZERO);
#else
    cv::normalize(gradient, gradient, 0, 300, cv::NORM_MINMAX, CV_8U);
#endif

    //cv::normalize(gradient, gradient, 0, 255, cv::NORM_MINMAX, CV_8U);

    return gradient;
}

Rect getBoxPointStudio(Mat input) {
    RNG rng(12345);
    std::vector<std::vector<Point> > contours; //외곽선 배열
    std::vector<Vec4i> hierarchy;//외곽선들 간의 계층구조

    findContours(input, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_NONE);

    /// Approximate contours to polygons + get bounding rects and circles
    std::vector<std::vector<Point> > contours_poly(contours.size());
    std::vector<Rect> boundRect(contours.size());

    LOGD("contours.size() : %d\n", contours.size());

    for (int i = 0; i < contours.size(); i++) {
        //printf("contours.size() 1\n");
        approxPolyDP(Mat(contours[i]), contours_poly[i], 3, true);
        boundRect[i] = boundingRect(Mat(contours_poly[i]));
    }

    /// Draw polygonal contour + bonding rects + circles
    Mat drawing = Mat::zeros(input.size(), CV_8UC3);
    for (int i = 0; i < contours.size(); i++) {
        //printf("contours.size() 2\n");
        Scalar color = Scalar(rng.uniform(0, 255), rng.uniform(0, 255), rng.uniform(0, 255));
        rectangle(drawing, boundRect[i].tl(), boundRect[i].br(), color, 2, 8, 0);
    }
    sofImwrite("/sdcard/studio/drawing.jpg", drawing);

    int i = 0;
    for (i = 0; i < contours.size(); i++) {
        LOGD("rect : %d %d %d %d\n", boundRect[i].tl().x, boundRect[i].tl().y,
               boundRect[i].br().x, boundRect[i].br().y);
    }

    if (i == 0) return Rect(0, 0, 0, 0);
    else
        return Rect(boundRect[i - 1].tl().x, boundRect[i - 1].tl().y,
                    boundRect[i - 1].br().x - boundRect[i - 1].tl().x + 1,
                    boundRect[i - 1].br().y - boundRect[i - 1].tl().y + 1);
}

Mat makeScaleStudio(int stepX, int stepY, Mat originalMat, Rect objRect) {
    Mat result;

    float addX = (float) stepX / 2.0f;
    float addY = (float) stepY / 2.0f;

    LOGD("addX : %f, addY : %f\n", addX, addY);

    float objLeft = (float) (objRect.tl().x);
    float objRight = (float) (objRect.br().x);
    float objTop = (float) (objRect.tl().y);
    float objBottom = (float) (objRect.br().y);

    float ori[8] = {objLeft, objTop, objRight, objTop, objLeft, objBottom, objRight, objBottom};
    float tar[8] = {objLeft - addX, objTop - addY, objRight + addX, objTop - addY, objLeft - addX,
                    objBottom + addY, objRight + addX, objBottom + addY};
    Mat scale1(4, 2, CV_32F, ori);
    Mat scale2(4, 2, CV_32F, tar);

    Mat scaleRvalue = getPerspectiveTransform(scale1, scale2);
    warpPerspective(originalMat, result, scaleRvalue, originalMat.size(), INTER_NEAREST);

    return result;
}

void makeStudioFaderTest(Mat mask, Mat fader, bool front)
{
    static Mat empty_image = Mat::zeros(mask.rows, mask.cols, CV_8UC1);//initial empty layer
    static Mat resultSegment(mask.rows, mask.cols, CV_8UC4);                 //initial blue result

    Mat in1[] = { mask, empty_image, fader, empty_image };    //construct 3 layer Matrix
    int from_to1[] = { 0, 0, 1, 1, 2, 2, 3, 3 };
    mixChannels(in1, 4, &resultSegment, 1, from_to1, 4);          //combine image

    if( front == true )
    {
        flip(resultSegment, resultSegment, 90);
    }

    //creatEGLImage(resultSegment, 2, false);
}

Mat makeStudioFader(Mat input, bool front, bool face, bool useBlur)
{
    Mat resultMask, resultFader;
    Mat newMarkerInner, newMarkerOuter;

    LOGD("front : %d, face : %d", front, face);

    if( face == false) {
        threshold(input, newMarkerOuter, 190, 80, CV_THRESH_BINARY_INV);
        threshold(input, newMarkerInner, 190, 40, CV_THRESH_BINARY);

        resultMask = newMarkerOuter + newMarkerInner;
    }
    else
    {
        threshold(input, newMarkerOuter, 190, 80, CV_THRESH_BINARY_INV);
        threshold(input, newMarkerInner, 190, 40, CV_THRESH_BINARY);

        resultMask = newMarkerOuter + newMarkerInner;

        if( front == true ) {
            flip(resultMask, resultMask, 90);
            //flip(input, resultMask, 90);
            //resultMask = getSmoothSegment(resultMask);
        }
        else {
            //resultMask = getSmoothSegment(input);
        }
    }
    sofImwrite("/sdcard/studio/resultMaskPre.jpg", resultMask);

    int open, close;
    gTuneManagerBlur.getOpenClose(open, close);
    erode(resultMask, resultMask, Mat(), Point(-1, -1), open);
    dilate(resultMask, resultMask, Mat(), Point(-1, -1), close);

    int faderSize = 13;
    int faderStart = -7;
    if( gTuneManagerP.getCurrentStatus() == false ) {
        gTuneManagerBlur.getStudioFaderData(faderStart, faderSize);
    }
    else
    {
        SF_STUDIO_TUNEDATAS datas;
        gTuneManagerP.getStudioTune(datas);

        faderStart = datas.faderStart;
        faderSize = datas.faderCount;
    }
    LOGD("[Mono-Dark-Sync] faderStart : %d, faderSize : %d", faderStart, faderSize);

    //resize(resultMask, resultMask, Size(1440, 1080), 0, 0, INTER_NEAREST);

    Mat newWaterLine = getMeetLine(resultMask, 0, faderSize, faderStart, resultFader);
    //oh my god
    int minusCase = abs(faderStart)-faderSize;
    if( minusCase > 0 ) {
        dilate(resultMask, resultMask, Mat(), Point(-1, -1), minusCase);
    }
    newWaterLine.copyTo(resultMask, newWaterLine);

    static Mat empty_image = Mat::zeros(resultMask.rows, resultMask.cols, CV_8UC1);//initial empty layer
    static Mat resultSegment(resultMask.rows, resultMask.cols, CV_8UC4);                 //initial blue result

    Mat in1[] = { resultMask, empty_image, resultFader, empty_image };    //construct 3 layer Matrix
    int from_to1[] = { 0, 0, 1, 1, 2, 2, 3, 3 };
    mixChannels(in1, 4, &resultSegment, 1, from_to1, 4);          //combine image

//    if( useBlur == false )
//    {
//        LOGD("[Mono-Dark-Sync] block moving");
//        resultSegment.setTo(40);
//    }

    sofImwrite("/sdcard/studio/resultMask.jpg", resultMask);
    sofImwrite("/sdcard/studio/resultFader.jpg", resultFader);
    //imwrite("/sdcard/studio/resultMask.jpg", resultMask);
    //imwrite("/sdcard/studio/resultFader.jpg", resultFader);
    //imwrite("/sdcard/studio/resultSegment.jpg", resultSegment);

    return resultSegment;

    //creatEGLImage(resultSegment, 2, false);
}

void setDarkSnowData(Mat input, int previewW, int previewH, bool face, Rect faceRect)
{
    gPreviewW = (float)previewW;
    gPreviewH = (float)previewH;

    if( face ) {
        gStudioRect = faceRect;
    }
    else
    {
        gStudioRect = getBoxPointStudio(input);
    }
}

void getDarkSnowData(float &x, float &y, float &radius, float &maxValue)
{
    float width = gStudioRect.width;
    float height = gStudioRect.height;
    float startX = gStudioRect.tl().x;
    float endX = gStudioRect.br().x;
    float startY = gStudioRect.tl().y;
    float endY = gStudioRect.br().y;
    float scale = (float)SMALL_FOR_SPEED;

//    float temp;
//    if( getMovingValue(temp) == false )
//    {
//        Rect resetRect = Rect(0,0,0,0);
//        gStudioRect = resetRect;
//
////        x = 1440.f/2.0f;
////        y = 1080.f/2.0f;
////        radius = sqrt(pow(1440.f/2.0f, 2) + pow(1080.f/2.0f, 2));
////        maxValue = 1000.f;
//
//        x = 0.0f;
//        y = 0.0f;
//        radius = 0.0f;
//        maxValue = 0.0f;
//
//        LOGD("[Mono-Dark-Zero-1] x : %f, y : %f, radius : %f, maxValue : %f", x, y, radius, maxValue);
//
//        return;
//    }
//
//    if( gStudioRect.width == 0 )
//    {
//        x = 0.0f;
//        y = 0.0f;
//        radius = 0.0f;
//        maxValue = 0.0f;
//
//        LOGD("[Mono-Dark-Zero-2] x : %f, y : %f, radius : %f, maxValue : %f", x, y, radius, maxValue);
//
//        return;
//    }

    x = startX + width / 2.0f;
    y = startY + height / 2.0f;
    x *= scale;
    y *= scale;

    if(width < height) radius = width / 2.0f;
    else radius = height / 2.0f;
    radius *= scale;

    float radisuRate = 0.5f, bright = 1.5f, reBrightRate = 0.8f;
    if( gTuneManagerP.getCurrentStatus() == false ) {
        gTuneManagerBlur.getStudioDarkData(radisuRate, bright, reBrightRate);
    }
    else
    {
        SF_STUDIO_TUNEDATAS datas;
        gTuneManagerP.getStudioTune(datas);

        radisuRate = datas.circleRate;
        bright = datas.blackRate;
        reBrightRate = datas.blackTuneRate;
    }
    LOGD("[Mono-Dark] radisuRate : %f, bright : %f, reBrightRate : %f", radisuRate, bright, reBrightRate);
    radius = radius * radisuRate;

    int farX, farY;
    if( x <= gPreviewW / 2.0f && y <= gPreviewH / 2.0f )
    {
        farX = gPreviewW;
        farY = gPreviewH;
    }
    else if( x > gPreviewW / 2.0f && y <= gPreviewH / 2.0f )
    {
        farX = 0.0f;
        farY = gPreviewH;
    }
    else if( x <= gPreviewW / 2.0f && y > gPreviewH / 2.0f )
    {
        farX = gPreviewW;
        farY = 0.0f;
    }
    else if( x > gPreviewW / 2.0f && y > gPreviewH / 2.0f )
    {
        farX = 0.0f;
        farY = 0.0f;
    }
    else
    {
        farX = 0.0f;
        farY = 0.0f;
    }

    reBrightRate = (100.0f - reBrightRate) / 100.f;
    LOGD("gBlurSize : %d", gBlurSize);

    if( gVidioStatus == false ) {
        if (gBlurSize == 1) {
            bright *= reBrightRate * reBrightRate * reBrightRate;
        } else if (gBlurSize == 2) {
            bright *= reBrightRate * reBrightRate;
        } else if (gBlurSize == 3) {
            bright *= reBrightRate;
        }
    }

    maxValue = sqrt(pow(x - farX, 2) + pow(y - farY, 2));
    maxValue = maxValue / bright;

    LOGD("[studio-new] x : %f, y : %f, radius : %f, maxValue : %f", x, y, radius, maxValue);
}

void setBlurSize(int size, bool videoStatus)
{
    gBlurSize = size;
    gVidioStatus = videoStatus;
    LOGD("gBlurSize : %d", gBlurSize);
    LOGD("gVidioStatus : %d", gVidioStatus);
}

Mat maskStudioMask(Mat input, int oriWidth, int oriHeight) {
    Rect rect = getBoxPointStudio(input);

    int bigRadius;
    if(rect.width > rect.height) bigRadius = rect.width;
    else bigRadius = rect.height;

//    Mat circle = makeCircle(oriWidth, oriHeight, bigRadius / 2, 360, Point(rect.tl().x + rect.width / 2, rect.tl().y + rect.height / 2));
//    return circle;

#if 1
    Mat circle = makeCircle(oriWidth, oriHeight, bigRadius / 2 / 2, 360, Point(rect.tl().x + rect.width / 2, rect.tl().y + rect.height / 2));
    return circle;
#elif 0
    Mat circle = makeCircle(oriWidth, oriHeight, bigRadius / 2, 360, Point(rect.tl().x + rect.width / 2, rect.tl().y + rect.height / 2));
//    threshold(circle, circle, 29, 0, CV_THRESH_TOZERO);
    threshold(circle, circle, (int)threshValue -1, 0, CV_THRESH_TOZERO);

    Mat out, in, sum;
    threshold(input, in, 30, 0, CV_THRESH_BINARY);
    //threshold(input, out, 30, 30, CV_THRESH_BINARY_INV);
    threshold(input, out, 30, (int)threshValue, CV_THRESH_BINARY_INV);
    //threshold(input, out, 30, (int)255, CV_THRESH_BINARY_INV);
    sum = in + out;

    circle.copyTo(sum, circle);

    return sum;
#else
    int smallC = 2;
    Mat circle = makeCircle(oriWidth, oriHeight, bigRadius / 2 / smallC, 360, Point(rect.tl().x + rect.width / 2, rect.tl().y + rect.height / 2));

    //box.copyTo(circle, box);
    Mat blackMat = Mat::zeros(input.size(), CV_8UC1);
    blackMat.copyTo(circle, input);

    return circle;
#endif

//    input.setTo(0);
//    circle(input, Point(rect.tl().x + rect.width/2, rect.tl().y + rect.height/2), bigRadius/2, Scalar(255), -1, 8, 0);
//    imwrite("/sdcard/studio/input2.jpg", input);
//
//    LOGD("rect w,h : %d %d\n", rect.width, rect.height);
//
//    int refSize, refThresh, refA, refB;
//    if (oriHeight - rect.height > oriWidth - rect.width) {
//        refSize = rect.height;
//        refThresh = oriHeight;
//        refA = rect.tl().y;
//        refB = rect.br().y;
//    } else if (oriHeight - rect.height < oriWidth - rect.width) {
//        refSize = rect.width;
//        refThresh = oriWidth;
//        refA = rect.tl().x;
//        refB = rect.br().x;
//    } else {
//        refSize = rect.width;
//        refThresh = oriWidth;
//        refA = rect.tl().x;
//        refB = rect.br().x;
//    }
//
//    LOGD("refSize : %d, refThresh : %d\n", refSize, refThresh);
//
//    Mat scaleMat, saveMat = input.clone();
//    Mat temp = input.clone();
//    Mat result = Mat(temp.size(), temp.type());
//    result.setTo(255);
//    temp.copyTo(result, temp);
//
//    float calcGap;
//    if (refA > refThresh - refB) {
//        calcGap = refA;
//    } else if (refA < refThresh - refB) {
//        calcGap = refThresh - refB;
//    } else {
//        calcGap = refA;
//    }
//
//    LOGD("calcGap : %f\n", calcGap);
//
//    int count = 1;
//    while( true ) {
//
//        scaleMat = makeScaleStudio(count * 2, count * 2, input, rect);
//        //imshow("scaleMat", scaleMat);
//
//        int scaleValue = (int) ((255.f / calcGap) * (float) count);
//        if (scaleValue > 255) scaleValue = 255;
//        LOGD("scaleValue : %d(%d)\n", scaleValue, count);
//
//        Mat subMat = scaleMat - saveMat;
//        threshold(subMat, subMat, 250, scaleValue, CV_THRESH_BINARY);
//        //imshow("subMat", subMat);
//
//        subMat.copyTo(result, subMat);
//        saveMat = scaleMat.clone();
//
//        if (count > calcGap) break;
//
//        count++;
//    }
//
//    Mat zeroMat = Mat::zeros(input.size(), input.type());
//    zeroMat.copyTo(result, input);
//    return result;
}

}//extern "C"