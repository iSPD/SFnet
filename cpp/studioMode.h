#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

using namespace cv;

extern "C" {
    Mat maskStudioMask(Mat input, int oriWidth, int oriHeight);

    void makeStudioFaderTest(Mat mask, Mat fader, bool front);
    Mat makeStudioFader(Mat input, bool front, bool face, bool useBlur);
    void setDarkSnowData(Mat input, int previewW, int previewH, bool face, Rect faceRect);
    void getDarkSnowData(float &x, float &y, float &radius, float &maxValue);
    void setBlurSize(int size, bool videoStatus);
}