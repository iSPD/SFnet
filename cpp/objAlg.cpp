#include "objAlg.h"
#include "sofTunner.h"
#include "utils.h"
#include "watershedSegmentation.h"

extern tuneManagerBlur gTuneManagerBlur;
extern int gObjTouchNumber;
extern Rect2f gObjTouchRect[10];
extern bool gObjTouchResetOn;
extern int gRotateInfo;
extern int gObjNumber;
extern Rect2f gObjRect[10];
extern Mat gFaderMat;

//size rect
static Mat gSegmentAnalisysForTouch;
static int gSizeRect[4];

//mid rect
static int gMidRectForTouch[4];
static int gMidRect[4];
static bool gTouchRealOn = false;

float preXTouch = -1.0f;
float preYTouch = -1.0f;

void waterShed(Mat &image, Mat &segment, Mat &watershed, Mat marker, bool useObj) {
    Mat temp;
    temp = image.clone();

    if( useObj == true ) {
        int preBlur = gTuneManagerBlur.getPreBlur();
        LOGD("[alg-test] preBlur : %d", preBlur);
        blur(temp, temp, Size(preBlur, preBlur));
    }

    WatershedSegmenter segmenter;

    segmenter.setMarkers(marker);
    segmenter.process(temp);

    segment = segmenter.getSegmentation();
    watershed = segmenter.getWatersheds();
}

void setSizeRect(int left, int top, int right, int bottom) {
    gSizeRect[0] = left;
    gSizeRect[1] = top;
    gSizeRect[2] = right;
    gSizeRect[3] = bottom;
}

//return
void getSizeRect(int *rect)
{
    for(int i = 0; i < 4; i++)
    {
        rect[i] = gSizeRect[i];
    }
}

void setMidRect(int left, int top, int right, int bottom) {
    gMidRect[0] = left;
    gMidRect[1] = top;
    gMidRect[2] = right;
    gMidRect[3] = bottom;
}

//return
void getMidRect(int *rect)
{
    for(int i = 0; i < 4; i++)
    {
        rect[i] = gMidRect[i];
    }
}

void setMidRectForTouch(int left, int top, int right, int bottom, bool touchOn, float x, float y) {
    gMidRectForTouch[0] = left;
    gMidRectForTouch[1] = top;
    gMidRectForTouch[2] = right;
    gMidRectForTouch[3] = bottom;

    LOGD("[Touch-P] preTouch : %f %f", preXTouch, preYTouch);
    LOGD("[Touch-P] curTouch : %f %f", x, y);

    if (touchOn == true) {
        if (preXTouch != x && preYTouch != y) {
            gTouchRealOn = true;
        } else {
            gTouchRealOn = false;
        }

        LOGD("[Touch-P] Set gTouchRealOn : %d", gTouchRealOn);

        preXTouch = x;
        preYTouch = y;
    }
}

//return
bool getMidRectForTouch(int *rect)
{
    for(int i = 0; i < 4; i++)
    {
        rect[i] = gMidRectForTouch[i];
    }

    //LOGD("[Touch-P] Get gTouchRealOn : %d", gTouchRealOn);
    return gTouchRealOn;
}

//return
void resetMidRectForTouch()
{
    for(int i = 0; i < 4; i++)
    {
        gMidRectForTouch[i] = -1;
    }
}

Rect getBoxPoint(Mat input, Mat &output) {
    RNG rng(12345);
    std::vector<std::vector<Point> > contours; //외곽선 배열
    std::vector<Vec4i> hierarchy;//외곽선들 간의 계층구조

    findContours(input, contours, hierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);

    /// Approximate contours to polygons + get bounding rects and circles
    std::vector<std::vector<Point> > contours_poly(contours.size());
    std::vector<Rect> boundRect(contours.size());

    LOGD("contours.size() : %d", contours.size());

    for (int i = 0; i < contours.size(); i++) {
        //printf("contours.size() 1\n");
        approxPolyDP(Mat(contours[i]), contours_poly[i], 3, true);
        boundRect[i] = boundingRect(Mat(contours_poly[i]));
    }

    output = input.clone();
    for (int i = 0; i < contours.size(); i++) {
        Scalar color = Scalar(rng.uniform(0, 255), rng.uniform(0, 255), rng.uniform(0, 255));
        rectangle(output, boundRect[i].tl(), boundRect[i].br(), color, 2, 8, 0);
    }

    int i = 0;
    for (i = 0; i < contours.size(); i++) {
        LOGD("rect : %d %d %d %d\n", boundRect[i].tl().x, boundRect[i].tl().y, boundRect[i].br().x,
             boundRect[i].br().y);
    }

    if (i == 0) return Rect(0, 0, 0, 0);
    else
        return Rect(boundRect[i - 1].tl().x, boundRect[i - 1].tl().y,
                    boundRect[i - 1].br().x - boundRect[i - 1].tl().x + 1,
                    boundRect[i - 1].br().y - boundRect[i - 1].tl().y + 1);
}

Mat makeTrans(Mat src, Rect inRect, Rect outRect, float touchX, float touchY) {
    Mat result;
    Mat srcTemp = src.clone();

    LOGD("makeTrans touchX : %f, touchY : %f", touchX, touchY);

    float touchPoint = 4.0f;
    float targetX = touchX - (inRect.tl().x + (inRect.width / touchPoint));
    float targetY = touchY - src.rows / 2;

    LOGD("makeTrans inRectX1 : %d, inRectY1 : %d", inRect.tl().x, inRect.tl().y);
    LOGD("makeTrans inRectX2 : %d, inRectY2 : %d", inRect.br().x, inRect.br().y);
    LOGD("makeTrans targetX : %f, targetY : %f", targetX, targetY);

    //check boundary here...
    float inX1 = inRect.tl().x + targetX;
    float inY1 = inRect.tl().y + targetY;
    float inX2 = inRect.br().x + targetX;
    float inY2 = inRect.br().y + targetY;

    float outX1 = 0;
    float outY1 = 0;
    float outX2 = src.cols;
    float outY2 = src.rows;

    if (inX1 < outX1) targetX = targetX + (outX1 - inX1) + 1;
    else if (inX2 > outX2) targetX = targetX + (outX2 - inX2) - 1;

    if (inY1 < outY1) targetY = targetY + (outY1 - inY1) + 1;
    else if (inY2 > outY2) targetY = targetY + (outY2 - inY2) - 1;

    LOGD("makeTrans modify targetX : %f, targetY : %f", targetX, targetY);

    double start = _getTickTime();
    Mat transR;
    float transValue[6] = {1.0f, 0.0f, targetX, 0.0f, 1.0f, targetY};//구체적으로 초기화
    Mat transMat(2, 3, CV_32F, transValue);
    warpAffine(srcTemp, result, transMat, Size(src.cols, src.rows), INTER_NEAREST);

    LOGD("[time-check] makeTrans DrawGoodBox Trans : %f", _getTickTime() - start);

    return result;
}

Mat scaleNtransForObject(Mat src, Rect inRect, Rect outRect, Rect2f objRect, bool touchOn) {
    Mat srcClone = src.clone();
    threshold(srcClone, srcClone, 250, 0, CV_THRESH_TOZERO_INV);
    Mat scaleMat;
    Mat resultMat;

    float markerX, markerY, outerX, outerY;
    if( touchOn == true )
    {
        LOGD("gObjTouchResetOn : %d", gObjTouchResetOn);
        if( gObjTouchResetOn == true )
        {
            gTuneManagerBlur.getAiTouchMakerSmall(markerX, markerY, outerX, outerY);
        }
        else {
            gTuneManagerBlur.getAiTouchMaker(markerX, markerY, outerX, outerY);
        }
    }
    else
    {
        gTuneManagerBlur.getAiMaker(markerX, markerY, outerX, outerY);
    }

//    float resizeWidthRate = 1.2f;
//    float resizeHeightRate = 1.0f;
    float resizeWidthRate = markerY;
    float resizeHeightRate = markerX;
    if( gRotateInfo == 90 || gRotateInfo == 270 )
    {
        resizeWidthRate = markerX;
        resizeHeightRate = markerY;
    }

    float resizeWidth = objRect.width * resizeWidthRate;
    float resizeHeight = objRect.height * resizeHeightRate;
    float resizeX = objRect.tl().x - (resizeWidth - objRect.width) / 2.0f;
    float resizeY = objRect.tl().y - (resizeHeight - objRect.height) / 2.0f;

//    if (gFaceCount > 0) {
//        resizeWidth = srcClone.cols - resizeX;
//    }

    Rect2f objRectResize = Rect2f(resizeX, resizeY, resizeWidth, resizeHeight);

    float addX = (objRectResize.width - (float) inRect.width) / 2.0f;
    float addY = (objRectResize.height - (float) inRect.height) / 2.0f;
    float oriWidth = (float) inRect.width;

    float ori[8] = {(float) inRect.tl().x, (float) inRect.tl().y, (float) inRect.tl().x + oriWidth,
                    (float) inRect.tl().y, (float) inRect.br().x - oriWidth, (float) inRect.br().y,
                    (float) inRect.br().x, (float) inRect.br().y};

    float tar[8] = {(float) inRect.tl().x - addX, (float) inRect.tl().y - addY,
                    (float) inRect.tl().x + oriWidth + addX, (float) inRect.tl().y - addY,
                    (float) inRect.br().x - oriWidth - addX, (float) inRect.br().y + addY,
                    (float) inRect.br().x + addX, (float) inRect.br().y + addY};
    Mat scale1(4, 2, CV_32F, ori);
    Mat scale2(4, 2, CV_32F, tar);

    Mat scaleRvalue = getPerspectiveTransform(scale1, scale2);
    warpPerspective(srcClone, scaleMat, scaleRvalue, srcClone.size(), INTER_NEAREST);

    float targetX = objRectResize.tl().x - ((float) inRect.tl().x - addX);
    float targetY = objRectResize.tl().y - ((float) inRect.tl().y - addY);

    float transValue[6] = {1.0f, 0.0f, targetX, 0.0f, 1.0f, targetY};//구체적으로 초기화
    Mat transMat(2, 3, CV_32F, transValue);
    warpAffine(scaleMat, resultMat, transMat, Size(srcClone.cols, srcClone.rows), INTER_NEAREST);

    LOGD("[test-test] pre objRectResize : %f %f %f %f", objRectResize.tl().x, objRectResize.tl().y,
         objRectResize.width, objRectResize.height);

//    float scaleX = 1.3f;
//    float scaleY = 1.1f;
    float scaleX = outerY;
    float scaleY = outerX;
    if( gRotateInfo == 90 || gRotateInfo == 270 )
    {
        scaleX = outerX;
        scaleY = outerY;
    }

    float w = objRect.width * scaleX;
    float h = objRect.height * scaleY;
    float x = objRect.tl().x - ((w - objRect.width) / 2.f);
    float y = objRect.tl().y - ((h - objRect.height) / 2.f);


//    if (gFaceCount > 0) {
//        w = srcClone.cols - x;
//    }

    Rect2f reSizeOuter = Rect2f(x, y, w, h);
    LOGD("[test-test] objRectResize : %f %f %f %f", reSizeOuter.tl().x, reSizeOuter.tl().y,
         reSizeOuter.width, reSizeOuter.height);
    rectangle(resultMat, reSizeOuter, cv::Scalar(255), 3);

    Rect outReRect = Rect(0, 0, srcClone.cols, srcClone.rows);
    rectangle(resultMat, outReRect, cv::Scalar(255), 3);

    return resultMat;
}

void doObjectJob(Mat *imageMask, Mat *output, int imageCount, Rect2f *objRect, int objIndex, bool touchOn) {

    static Rect inRect, outRect;
    static Mat outMat;
    static bool init = false;
    if (init == false) {
        Mat sum = Mat::zeros(imageMask[0].size(), imageMask[0].type());
        Mat temp;
        for (int i = 0; i < imageCount; i++) {
            threshold(imageMask[i], temp, 250, 0, CV_THRESH_TOZERO_INV);
            sum += temp;
        }

        Mat boxMat;
        threshold(sum, sum, 30, 250, CV_THRESH_BINARY);
        inRect = getBoxPoint(sum, boxMat);

        sofImwrite("/sdcard/touchJob/inMat.jpg", boxMat);
        LOGD("[Touch-Job] rect : %d %d %d %d", inRect.x, inRect.y, inRect.width, inRect.height);

        threshold(imageMask[0], temp, 250, 250, CV_THRESH_BINARY);
        outRect = getBoxPoint(temp, boxMat);
        sofImwrite("/sdcard/touchJob/outMat.jpg", boxMat);

        init = true;
    }

    for (int i = 0; i < imageCount; i++) {
        Mat temp;
        temp = scaleNtransForObject(imageMask[i], inRect, outRect, objRect[objIndex], touchOn);
        temp.copyTo(output[i]);
    }
}

void doTouchJob(Mat *imageMask, Mat *output, int imageCount, float touchX, float touchY) {

    static Rect inRect, outRect;
    static Mat outMat;
    static bool init = false;
    if (init == false) {
        Mat sum = Mat::zeros(imageMask[0].size(), imageMask[0].type());
        Mat temp;
        for (int i = 0; i < imageCount; i++) {
            threshold(imageMask[i], temp, 250, 0, CV_THRESH_TOZERO_INV);
            sum += temp;
        }

        Mat boxMat;
        threshold(sum, sum, 30, 250, CV_THRESH_BINARY);
        inRect = getBoxPoint(sum, boxMat);

        //sum.copyTo(boxMat);
        sofImwrite("/sdcard/touchJob/inMat.jpg", boxMat);
        LOGD("[Touch-Job] rect : %d %d %d %d", inRect.x, inRect.y, inRect.width, inRect.height);

        threshold(imageMask[0], temp, 250, 250, CV_THRESH_BINARY);
        outRect = getBoxPoint(temp, boxMat);
        sofImwrite("/sdcard/touchJob/outMat.jpg", boxMat);

        init = true;
    }

    for (int i = 0; i < imageCount; i++) {
        Mat temp = makeTrans(imageMask[i], inRect, outRect, touchX, touchY);
        temp.copyTo(output[i]);
    }
}

int getObjnTouchMask(Mat input, Mat &output, bool touchOn, float touchX, float touchY, Mat &touchMat, Mat &analisysMat, int objIndex, bool multiTouchOn, bool minusTouchOn) {

    int imageCount = gTuneManagerBlur.getImageCompareTotal();
    int compareCount = gTuneManagerBlur.getImageCompareCount();

    LOGD("[getTouchPoint] imageCount : %d, compareCount : %d \n", imageCount, compareCount);

    Mat src;
    input.copyTo(src);

    static Mat imageMask[13];
    static Mat useImageMask[13];
    static Mat useImageMaskForTouuch[13];
    Mat segment[imageCount];
    Mat watershed[imageCount];
    Mat segmentForTouch[imageCount];
    Mat watershedForTouch[imageCount];

    Mat segmentSum = Mat::zeros(src.rows, src.cols, CV_8U);

    static bool loadingOnce = false;
    if (loadingOnce == false) {
        LOGD("[getTouchPoint] loadingOnce\n");
        loadingOnce = true;

        //char aiName[128] = "/data/local/tmp/of/ai/aif_outer.jpg";
        char aiName[128] = "/sdcard/sfcam/of/ai/aif_outer.jpg";
        Mat front, back;

        back = imread(aiName, 0);
        LOGD("[getTouchPoint] back : %d %d %d", back.cols, back.rows, back.channels());
        resize(back, back, Size(back.cols / 2, back.rows / 2), 0, 0, INTER_NEAREST);
        threshold(back, back, 250, 255, CV_THRESH_BINARY);

        for (int i = 0; i < imageCount; i++) {
            //sprintf(aiName, "/data/local/tmp/of/ai/aif_%d.jpg", i);
            sprintf(aiName, "/sdcard/sfcam/of/ai/aif_%d.jpg", i);
            front = imread(aiName, 0);
            LOGD("[getTouchPoint] front : %d %d %d", front.cols, front.rows, front.channels());

            resize(front, front, Size(front.cols / 2, front.rows / 2), 0, 0, INTER_NEAREST);
            threshold(front, front, 120, 40, CV_THRESH_BINARY);

            Mat sum = front + back;
            sum.copyTo(imageMask[i]);

            //test sofImwrite
            sprintf(aiName, "/sdcard/aif%d.jpg", i);
            sofImwrite(aiName, imageMask[i]);
        }
    }

    if (touchOn == true) {
        if (gObjTouchNumber > 0) {

            LOGD("[touch-test] gObjTouchNumber");

            doObjectJob(imageMask, useImageMask, imageCount, gObjTouchRect, 0, true);

            Mat sum = Mat::zeros(useImageMask[0].size(), useImageMask[0].type());
            for (int i = 0; i < imageCount; i++) {
                sum += useImageMask[i];
            }
            sum.copyTo(touchMat);
            sofImwrite("/sdcard/touchJob/touchMat.jpg", touchMat);
        } else {

            LOGD("[touch-test] touchOn");

            //do multi touch here...
            Mat sumForSave;
            doTouchJob(imageMask, useImageMask, imageCount, touchX, touchY);

            Mat sum = Mat::zeros(useImageMask[0].size(), useImageMask[0].type());
            for (int i = 0; i < imageCount; i++) {
                sum += useImageMask[i];
            }

            sum.copyTo(touchMat);
            imwrite("/sdcard/touchMat.jpg", touchMat);
        }
    } else if (gObjNumber > 0) {

        LOGD("[touch-test] gObjNumber");

        doObjectJob(imageMask, useImageMask, imageCount, gObjRect, objIndex, false);

        Mat sum = Mat::zeros(useImageMask[0].size(), useImageMask[0].type());
        for (int i = 0; i < imageCount; i++) {
            sum += useImageMask[i];
        }
        sum.copyTo(touchMat);
        sofImwrite("/sdcard/touchJob/touchMat.jpg", touchMat);
    } else {

        LOGD("[touch-test] else");

        for (int i = 0; i < imageCount; i++) {
            imageMask[i].copyTo(useImageMask[i]);
        }
    }

    for (int count = 0; count < imageCount; count++) {

        LOGD("watertypes1 : %d %d %d %d", CV_8UC1, CV_8UC2, CV_8UC3, CV_8UC4);
        LOGD("watertypes2 : %d %d %d %d", src.type(), segment[count].type(), watershed[count].type(), useImageMask[count].type());
        waterShed(src, segment[count], watershed[count], useImageMask[count], true);

        //test sofImwrite
        char aiName[128];
        sprintf(aiName, "/sdcard/aifSegment%d.jpg", count);
        Mat transMat;
        transpose(segment[count], transMat);
        flip(transMat, transMat, 90);
        sofImwrite(aiName, transMat);
        //test sofImwrite

        threshold(segment[count], segment[count], 200, 10, CV_THRESH_BINARY_INV);
    }

    //Save this....
    Mat segmentAnalisys = Mat::zeros(segment[0].rows, segment[0].cols, segment[0].type());
    for (int i = 0; i < imageCount; i++) {
        segmentAnalisys += segment[i];
    }

    //analisys test
    static int showCount = -1;
    showCount++;
    if (showCount >= imageCount) {
        showCount = 0;
    }

    if (showCount == 0) analisysMat.setTo(10);

    Mat temp;
    Mat maskSeg = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(5, 5), cv::Point(1, 1));
    morphologyEx(segment[showCount], temp, CV_MOP_OPEN, maskSeg);

    analisysMat = analisysMat + (temp * 5 / 10);

    cv::rectangle(analisysMat, cv::Point(0, 0), cv::Point(analisysMat.cols, analisysMat.rows),
                  cv::Scalar(10), 5);
    //analisys test

    threshold(segmentAnalisys, segmentAnalisys, 10 * compareCount - 1, 200, CV_THRESH_BINARY);
    cv::rectangle(segmentAnalisys, cv::Point(0, 0), cv::Point(src.cols, src.rows),
                  cv::Scalar(0), 5);

    if (gSegmentAnalisysForTouch.empty() == true) {
        LOGD("gSegmentAnalisysForTouch empty");
        gSegmentAnalisysForTouch = Mat::zeros(segment[0].rows, segment[0].cols, segment[0].type());
    }

    if (touchOn == true && multiTouchOn == false) {
        gSegmentAnalisysForTouch = segmentAnalisys.clone();
    }


    LOGD("touchOn : %d gObjTouchNumber : %d gObjNumber : %d", touchOn, gObjTouchNumber, gObjNumber);

    if( touchOn == true )
    {
        Mat outputBox;
        Rect temp = getBoxPoint(segmentAnalisys, outputBox);

        int left = 1080 - (temp.br().y * SMALL_FOR_SPEED);
        int right = 1080 - (temp.tl().y * SMALL_FOR_SPEED);
        int top = temp.tl().x * SMALL_FOR_SPEED;
        int bottom = temp.br().x * SMALL_FOR_SPEED;

        setMidRectForTouch(left, top, right, bottom, touchOn, touchX, touchY);
    }
    else if( gObjNumber <= 0 )
    {
        Mat outputBox;
        Rect temp = getBoxPoint(segmentAnalisys, outputBox);

        //imwrite("/sdcard/test.jpg", outputBox);

        int left = 1080 - (temp.br().y * SMALL_FOR_SPEED);
        int right = 1080 - (temp.tl().y * SMALL_FOR_SPEED);
        int top = temp.tl().x * SMALL_FOR_SPEED;
        int bottom = temp.br().x * SMALL_FOR_SPEED;

        setMidRect(left, top, right, bottom);
    }
    else
    {
        setMidRectForTouch(-1, -1, -1, -1, touchOn, touchX, touchY);
        setMidRect(-1, -1, -1, -1);
    }

    //check object Size
    Mat outputBox;
    Rect sizeTemp = getBoxPoint(segmentAnalisys, outputBox);

    int left = 1080 - (sizeTemp.br().y * SMALL_FOR_SPEED);
    int right = 1080 - (sizeTemp.tl().y * SMALL_FOR_SPEED);
    int top = sizeTemp.tl().x * SMALL_FOR_SPEED;
    int bottom = sizeTemp.br().x * SMALL_FOR_SPEED;

    setSizeRect(left, top, right, bottom);
    //check object Size

    if( touchOn == true && multiTouchOn == true )
    {
        LOGD("[gMultiTouchOn] output = segmentAnalisys + segmentAnalisysForTouch-1");
        if( minusTouchOn == true )
        {
            output = gSegmentAnalisysForTouch;
        }
        else {
            output = segmentAnalisys + gSegmentAnalisysForTouch;
        }
        LOGD("[gMultiTouchOn] output = segmentAnalisys + segmentAnalisysForTouch-2");
    }
    else
    {
        output = segmentAnalisys;
    }

    return 0;
}

Mat getSmoothSegment(Mat input) {
    Mat segment = input;
    Mat segment2;
    Mat maskSeg = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(3, 3), cv::Point(1, 1));
    morphologyEx(segment, segment2, CV_MOP_CLOSE, maskSeg);

    return segment2;
}

Mat makeFaderLines(Mat segment3, int faderSize, int faderStart) {
    double start = _getTickTime();

    float linearValue = 255.f / (float) (faderSize - 1);
    Mat newFader = Mat::zeros(segment3.rows, segment3.cols, segment3.type());
    int faderCount = 0;

    int innerCount = faderStart;

    if (innerCount < 0) {
        innerCount = abs(faderStart);

        Mat dilateMat[innerCount];
        Mat dilateMatNew[innerCount];

        if (abs(faderStart) - faderSize > 0) {
            dilate(segment3, segment3, Mat(), Point(-1, -1), abs(faderStart) - faderSize);
        }

        dilate(segment3, dilateMat[0], Mat(), Point(-1, -1), 1);
        dilateMatNew[0] = dilateMat[0] - segment3;//4
        threshold(dilateMatNew[0], dilateMatNew[0], 0,
                  (int) (linearValue * (float) (innerCount - 1)),
                  CV_THRESH_BINARY);
        newFader += dilateMatNew[0];

        faderCount++;

        int innnerCount2 = abs(faderStart) - faderSize;
        if (innnerCount2 < 0) innnerCount2 = 0;
        int maxInner = innerCount - innnerCount2;

        for (int i = 1; i < maxInner; i++) {
            dilate(dilateMat[i - 1], dilateMat[i], Mat(), Point(-1, -1), 1);
            dilateMatNew[i] = dilateMat[i] - dilateMat[i - 1];
            if (i == maxInner - 1) {
                threshold(dilateMatNew[i], dilateMatNew[i], 0, 1, CV_THRESH_BINARY);
            } else {
                threshold(dilateMatNew[i], dilateMatNew[i], 0,
                          (int) (linearValue * (float) (maxInner - 1 - i)),
                          CV_THRESH_BINARY);
            }
            newFader += dilateMatNew[i];

            faderCount++;
        }
    }

    if (faderSize - innerCount > 0) {
        Mat erodeMat[faderSize - innerCount];
        Mat erodeMatNew[faderSize - innerCount];

        erode(segment3, erodeMat[0], Mat(), Point(-1, -1), 1);
        erodeMatNew[0] = segment3 - erodeMat[0];//4
        threshold(erodeMatNew[0], erodeMatNew[0], 0,
                  faderCount == 0 ? 1 : (int) ((float) faderCount * linearValue), CV_THRESH_BINARY);
        newFader += erodeMatNew[0];

        faderCount++;

        for (int i = 1; i < faderSize - innerCount; i++) {
            erode(erodeMat[i - 1], erodeMat[i], Mat(), Point(-1, -1), 1);
            erodeMatNew[i] = erodeMat[i - 1] - erodeMat[i];

            threshold(erodeMatNew[i], erodeMatNew[i], 0, (int) (linearValue * (float) faderCount),
                      CV_THRESH_BINARY);
            LOGD("[time-total] i : %d, linearValue : %f, faderCount : %d, total : %d", i,
                 linearValue,
                 faderCount, (int) (linearValue * (float) faderCount));

            newFader += erodeMatNew[i];

            faderCount++;
        }
    }
    return newFader;

    LOGD("[time-total] newFader Time : %f", _getTickTime() - start);
}

Mat getMeetLine(Mat input, int option, int faderSize, int faderStart, Mat &resultFader) {
    if (option == 0) {
        Mat segment = input;
        Mat segment2;

        {
            Mat reSeg7, reSeg6, reSeg5, reSeg4, reSeg3, reSeg2, reSeg1;
            threshold(segment, reSeg7, 250, 255, CV_THRESH_BINARY);//35

            threshold(segment, reSeg6, 250, 0, CV_THRESH_TOZERO_INV);
            threshold(reSeg6, reSeg6, 210, 220, CV_THRESH_BINARY);//30

            threshold(segment, reSeg5, 210, 0, CV_THRESH_TOZERO_INV);
            threshold(reSeg5, reSeg5, 175, 190, CV_THRESH_BINARY);//24

            threshold(segment, reSeg4, 175, 0, CV_THRESH_TOZERO_INV);
            threshold(reSeg4, reSeg4, 140, 166, CV_THRESH_BINARY);//19

            threshold(segment, reSeg3, 140, 0, CV_THRESH_TOZERO_INV);
            threshold(reSeg3, reSeg3, 105, 147, CV_THRESH_BINARY);//13

            threshold(segment, reSeg2, 105, 0, CV_THRESH_TOZERO_INV);
            threshold(reSeg2, reSeg2, 70, 134, CV_THRESH_BINARY);//8

            threshold(segment, reSeg1, 70, 0, CV_THRESH_TOZERO_INV);
            threshold(reSeg1, reSeg1, 30, 126, CV_THRESH_BINARY);

            Mat segment2Total = reSeg7 + reSeg6 + reSeg5 + reSeg4 + reSeg3 + reSeg2 + reSeg1;

            segment2 = segment2Total;
        }

        Mat erodeMat, dilateMat;
        if (faderStart < 0) {
            dilate(segment2, dilateMat, Mat(), Point(-1, -1), abs(faderStart));
        } else {
            dilateMat = segment2.clone();
        }

        if (faderSize - abs(faderStart) > 0) {
            erode(segment2, erodeMat, Mat(), Point(-1, -1), faderSize - abs(faderStart));
        } else {
            if (faderSize - abs(faderStart) < 0) {
                dilate(segment2, erodeMat, Mat(), Point(-1, -1), abs(faderStart) - faderSize);
            } else {
                erodeMat = segment2.clone();
            }
        }

        Mat faderTemp = makeFaderLines(segment2, faderSize, faderStart);
        faderTemp.copyTo(resultFader);

        Mat totalLine = dilateMat - erodeMat;

        Mat remSeg6, remSeg5, remSeg4, remSeg3, remSeg2, remSeg1;

        threshold(totalLine, remSeg6, 64, 30, CV_THRESH_BINARY);//65 //220

        threshold(totalLine, remSeg5, 94, 0, CV_THRESH_TOZERO_INV);
        threshold(remSeg5, remSeg5, 53, 24, CV_THRESH_BINARY);//54 //185

        threshold(totalLine, remSeg4, 64, 0, CV_THRESH_TOZERO_INV);
        threshold(remSeg4, remSeg4, 42, 19, CV_THRESH_BINARY);//43 //150

        threshold(totalLine, remSeg3, 40, 0, CV_THRESH_TOZERO_INV);
        threshold(remSeg3, remSeg3, 39, 13, CV_THRESH_BINARY);//40 //115

        threshold(totalLine, remSeg2, 32, 0, CV_THRESH_TOZERO_INV);
        threshold(remSeg2, remSeg2, 31, 13, CV_THRESH_BINARY);//32 //115

        threshold(totalLine, remSeg1, 21, 0, CV_THRESH_TOZERO_INV);
        threshold(remSeg1, remSeg1, 20, 8, CV_THRESH_BINARY);//21 //80

        remSeg6.copyTo(totalLine, remSeg6);
        remSeg5.copyTo(totalLine, remSeg5);
        remSeg4.copyTo(totalLine, remSeg4);
        remSeg3.copyTo(totalLine, remSeg3);
        remSeg2.copyTo(totalLine, remSeg2);
        remSeg1.copyTo(totalLine, remSeg1);

        return totalLine;
    } else {
        int faderSizeMulti[4];
        gTuneManagerBlur.getFaderCount(faderSizeMulti);

        int faderSize = faderSizeMulti[0];
        int faderStart = gTuneManagerBlur.getFaderStart();

        Mat outMat, faderMat;

        Mat MatA, MatB;
        threshold(input, MatA, 70, 115, CV_THRESH_BINARY);
        threshold(input, MatB, 70, 0, CV_THRESH_TOZERO_INV);

        makeFaderLines(MatA + MatB, faderSize, faderStart);

        Mat erodeMat, dilateMat;
        dilate(input, dilateMat, Mat(), Point(-1, -1), abs(faderStart));
        erode(input, erodeMat, Mat(), Point(-1, -1), faderSize - abs(faderStart));
        Mat totalLine = dilateMat - erodeMat;
        threshold(totalLine, totalLine, 10, 8, CV_THRESH_BINARY);//8

        return totalLine;
    }
}

Mat makeCloseMask(Mat &image, Mat &input, Mat &middleMat, Mat &maskMat)
{
    Mat result;
    Mat new40Mat;
    Mat sumNewMat;

    sofImwrite("/sdcard/multiTest/backAlgMat.jpg", input);

    //1. make 80, 115 mask
    Mat newMarkerInner, newMarkerOuter;
    threshold(input, newMarkerOuter, 190, 80, CV_THRESH_BINARY_INV);
    threshold(input, newMarkerInner, 190, 40, CV_THRESH_BINARY);

    sumNewMat = newMarkerOuter + newMarkerInner;

    //2. smooth marker
    int open, close;
    gTuneManagerBlur.getOpenClose(open, close);
    LOGD("getOpenClose-2 %d %d", open, close);

    if( open > 0 ) {
        erode(sumNewMat, sumNewMat, Mat(), Point(-1, -1), open);
    }
    if(close > 0)
    {
        dilate(sumNewMat, sumNewMat, Mat(), Point(-1, -1), close);
    }

    //3. make 40 marker
    threshold(sumNewMat, new40Mat, 70, 0, CV_THRESH_TOZERO_INV);
    threshold(new40Mat, new40Mat, 30, 40, CV_THRESH_BINARY);
    sofImwrite("/sdcard/multiTest/new40Mat.jpg", new40Mat);

    //4. make marker2 start here...
    int markerSizeMulti[] = {0, 3, 6, 9, 15, 11, 11};
    int faderSizeMulti[] = {0, 20, 10, 10, 10, 10, 10};
    int faderStartMulti[] = {0, 0, 0, 0, 0, 0, 0};
    float outerCompensation[] = {0, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};

    gTuneManagerBlur.getMarkerSize(&markerSizeMulti[1]);
    gTuneManagerBlur.getFaderCount(&faderSizeMulti[1]);
    faderStartMulti[1] = gTuneManagerBlur.getFaderStart();
    gTuneManagerBlur.getOuterSize(&outerCompensation[1]);

    int markerCount = 4;
    markerCount = gTuneManagerBlur.getMaskCount();

    int thValueBack[] = { 40, 80, 115, 150, 185, 220, 255};
    Mat newMarker[markerCount];
    newMarker[markerCount - 1] = Mat::zeros(input.size(), input.type());
    Mat preMat = sumNewMat.clone();

    char fileName[128];
    sumNewMat.setTo(0);

    Mat totalFaderMat = Mat::zeros(input.size(), input.type());
    Mat totalFaderDetailMat = Mat::zeros(input.size(), input.type());

    for(int i = 1; i <= markerCount; i++) {//marker count 4
        Mat newSegment, newWatershed;
        Mat innnerMat, outerMat;
        Mat sumMat;

        threshold(preMat, outerMat, thValueBack[i] - 5, thValueBack[i + 1], CV_THRESH_BINARY);
        threshold(preMat, innnerMat, thValueBack[i] - 5, 0, CV_THRESH_TOZERO_INV);
        threshold(innnerMat, innnerMat, thValueBack[i - 1] - 5, thValueBack[i], CV_THRESH_BINARY);

        sumMat = outerMat + innnerMat;

        //int makeSize = faderSizeMulti[i] - abs(faderStartMulti[i]) + abs(faderStartMulti[i+1]) + 1;
        int makeSize = markerSizeMulti[i];
        LOGD("getOpenClose-2 makeSize[%d] : %d", i, makeSize);
        erode(sumMat, sumMat, Mat(), Point(-1, -1), makeSize);
        sprintf(fileName, "/sdcard/multiTest/sumMat%d.jpg", i);
        sofImwrite(fileName, sumMat);

        threshold(sumMat, sumMat, thValueBack[i + 1] - 5/*110*/, 0, CV_THRESH_TOZERO_INV);//only 80

        int lineThin = 5;
        Mat boxMat;
        Rect rect = getBoxPoint(sumMat, boxMat);

        int newWidth = (int) ((float) rect.width * outerCompensation[i]);
        int newHeight = (int) ((float) rect.height * outerCompensation[i]);
        int newX = rect.x - ((newWidth - rect.width) / 2);
        int newY = rect.y - ((newHeight - rect.height) / 2);

        LOGD("outer x : %d y : %d width : %d height : %d", newX, newY, newWidth, newHeight);

        Rect newRect = Rect(newX - lineThin, newY - lineThin, newWidth + (lineThin * 2),
                            newHeight + (lineThin * 2));

        Mat outLine = Mat::zeros(sumMat.size(), sumMat.type());
        rectangle(outLine, newRect, cv::Scalar(thValueBack[i + 1]/*115*/), lineThin);//draw 115
        outLine.copyTo(sumMat, outLine);

        Mat outerMask;
        threshold(sumMat, outerMask, thValueBack[i + 1] - 5, thValueBack[i + 1], CV_THRESH_BINARY);
        sumNewMat += outerMask;

        sprintf(fileName, "/sdcard/multiTest/sumMat2%d.jpg", i);
        sofImwrite(fileName, sumMat);

        //5. run watershed for marker2
        waterShed(image, newSegment, newWatershed, sumMat, true);
        newSegment = getSmoothSegment(newSegment);
        sprintf(fileName, "/sdcard/multiTest/newSegment%d.jpg", i);
        sofImwrite(fileName, newSegment);

        //6. smooth marker2 end here...
        if (open > 0) {
            erode(newSegment, newSegment, Mat(), Point(-1, -1), open);
        }
        if( close > 0 ) {
            dilate(newSegment, newSegment, Mat(), Point(-1, -1), close);
        }

        sprintf(fileName, "/sdcard/multiTest/newSegment2%d.jpg", i);
        sofImwrite(fileName, newSegment);

        //7. make fader
        gFaderMat.setTo(0);
        if( i < markerCount ) {
            Mat faderMat = getMeetLine(newSegment, 0, faderSizeMulti[i + 1],
                                       faderStartMulti[i + 1], gFaderMat);
            sprintf(fileName, "/sdcard/multiTest/faderMat%d.jpg", i);
            sofImwrite(fileName, faderMat);
            sprintf(fileName, "/sdcard/multiTest/gFaderMat%d.jpg", i);
            sofImwrite(fileName, gFaderMat);

            totalFaderMat += faderMat;
            totalFaderDetailMat += gFaderMat;
        }

        preMat = newSegment;

        if( markerCount == i )
        {
            newMarker[i - 1].setTo(thValueBack[i]);
        }
        else {
            threshold(newSegment, newMarker[i - 1], thValueBack[i + 1] - 5, 0,
                      CV_THRESH_TOZERO_INV);
        }
        sprintf(fileName, "/sdcard/multiTest/newMarker%d.jpg", i-1);
        sofImwrite(fileName, newMarker[i-1]);
    }

    for(int i = markerCount-2; i >= 0; i--)
    {
        newMarker[i].copyTo(newMarker[markerCount-1], newMarker[i]);
    }

    //7. sum marker1 & marker2
    new40Mat.copyTo(newMarker[markerCount-1], new40Mat);
    sofImwrite("/sdcard/multiTest/newSegmentTotal.jpg", newMarker[markerCount-1]);

    //make 40mat's fader
    Mat temp = Mat(new40Mat.size(), new40Mat.type());
    temp.setTo(80);
    new40Mat.copyTo(temp, new40Mat);
    sofImwrite("/sdcard/multiTest/temp.jpg", temp);
    Mat faderMat = getMeetLine(temp, 0, faderSizeMulti[1], faderStartMulti[1], gFaderMat);
    sofImwrite("/sdcard/multiTest/faderMat00.jpg", faderMat);
    totalFaderMat += faderMat;
    totalFaderDetailMat += gFaderMat;

    //8, 13, 19, 24, 30, 35 // 21, 32, 43, 54, 65
    sofImwrite("/sdcard/multiTest/totalFaderMatBefore.jpg", totalFaderMat);
    Mat rem1, rem2, rem3, rem4, rem5;
    threshold(totalFaderMat, rem1, 60, 30, CV_THRESH_BINARY);

    threshold(totalFaderMat, rem2, 60, 0, CV_THRESH_TOZERO_INV);
    threshold(rem2, rem2, 50, 24, CV_THRESH_BINARY);

    threshold(totalFaderMat, rem3, 50, 0, CV_THRESH_TOZERO_INV);
    threshold(rem3, rem3, 40, 19, CV_THRESH_BINARY);

    threshold(totalFaderMat, rem4, 34, 0, CV_THRESH_TOZERO_INV);
    threshold(rem4, rem4, 31, 13, CV_THRESH_BINARY);

    threshold(totalFaderMat, rem5, 22, 0, CV_THRESH_TOZERO_INV);
    threshold(rem5, rem5, 20, 8, CV_THRESH_BINARY);

    rem5.copyTo(totalFaderMat, rem5);
    rem4.copyTo(totalFaderMat, rem4);
    rem3.copyTo(totalFaderMat, rem3);
    rem2.copyTo(totalFaderMat, rem2);
    rem1.copyTo(totalFaderMat, rem1);
    //make 40mat's fader

    totalFaderMat.copyTo(newMarker[markerCount-1], totalFaderMat);
    newMarker[markerCount-1].copyTo(result);
    totalFaderDetailMat.copyTo(gFaderMat);
    sofImwrite("/sdcard/multiTest/totalFaderMatAfter.jpg", totalFaderMat);
    sofImwrite("/sdcard/multiTest/result.jpg", result);
    sofImwrite("/sdcard/multiTest/gFaderMat.jpg", gFaderMat);

    middleMat = new40Mat.clone();
    maskMat = sumNewMat.clone();

    return result;
}