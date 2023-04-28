#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

using namespace cv;

extern "C" {
    void loadingTunner();
    //void setSegmentData(Mat &segMat, Mat &result);
    void setStudioMode(Mat &input, Mat &output, int flipMode);
    void setSegmentData(Mat &srcMat, Mat &segMat, Mat &result, Mat &studio_result, int useFast, int rotate);
    int process(Mat &img_input, Mat &img_result, Mat &img_studio_result);
    void setObjectMoreRect(int number, int *index);
    void setObjectRect(int number, Rect2f *rectf);
    void setObjectRectForTouch(int number, Rect2f *rectf, bool resetOn);
    void setRotateInfo(int rotateInfo);
    void setTouchData(float x, float y);
    void setTouchDataForMultiTouch(bool multiTouchOn, bool minusTouchOn, float x, float y);
    void FaceLocationMulti(int length, int *faceArray, int previewWidth, int previewHeight);
    void setSaveMovieFrontStatus(bool isSave, bool isMovie, bool isFront, bool isPerson, float brightness);
    void setRotateInfo(int rotateInfo);
    void setCurrentAlMode(int mode);
    void getObjScaleValue(float *datas);
}