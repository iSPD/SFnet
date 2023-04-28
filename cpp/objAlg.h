#include <android/log.h>
#include <opencv2/imgproc/types_c.h>
#include <opencv2/core/core.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>

// log
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "objAlg", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "objAlg", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "objAlg", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "objAlg", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "objAlg", __VA_ARGS__)

//#define LOGV(...)
//#define LOGD(...)
//#define LOGI(...)
//#define LOGW(...)
//#define LOGE(...)

using namespace cv;

extern "C" {
int getObjnTouchMask(Mat input, Mat &output, bool touchOn, float touchX, float touchY, Mat &touchMat,
                  Mat &analisysMat, int objIndex, bool multiTouchOn, bool minusTouchOn);
Mat makeCloseMask(Mat &image, Mat &input, Mat &middleMat, Mat &maskMat);
Mat getSmoothSegment(Mat input);
Mat getMeetLine(Mat input, int option, int faderSize, int faderStart, Mat &resultFader);
void waterShed(Mat &image, Mat &segment, Mat &watershed, Mat marker, bool useObj);

void getSizeRect(int *rect);
void getMidRect(int *rect);
bool getMidRectForTouch(int *rect);
void resetMidRectForTouch();
}