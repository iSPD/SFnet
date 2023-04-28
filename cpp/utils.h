
#include <opencv2/imgproc/imgproc.hpp>

#define SMALL_FOR_SPEED 8

using namespace cv;

extern "C" {
double _getTickTime();
bool checkTimeFps(void);
void sofImwrite(char *name, Mat input);
}