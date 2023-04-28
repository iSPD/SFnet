#include <android/log.h>
#include <sys/time.h>
#include <opencv2/imgcodecs.hpp>
#include "utils.h"

// log
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "utils", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "utils", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "utils", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "utils", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "utils", __VA_ARGS__)

//#define LOGV(...)
//#define LOGD(...)
//#define LOGI(...)
//#define LOGW(...)
//#define LOGE(...)

double _getTickTime() {
    struct timeval t1;

    gettimeofday(&t1, NULL);

    return t1.tv_sec * 1000.0 + t1.tv_usec * 0.001;
}

bool checkTimeFps(void) {
    static double sPreTime = 0.0;
    static const double sOverTime = 1000.0;

    double cur_time = _getTickTime();
    if (sPreTime == 0.0) sPreTime = cur_time;

    if ((cur_time - sPreTime) > sOverTime) {
        sPreTime = cur_time;
        return true;
    }

    return false;
}

void sofImwrite(char *name, Mat input) {
    LOGD("imwrite is not operate");
    //imwrite(name, input);
}