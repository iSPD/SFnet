//#include "stdafx.h"
#include "sfTunner.h"

#include <stdio.h>
#include <cstring>

#ifndef WIN32
#include <android/log.h>

// log
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "SofTunner", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "SofTunner", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "SofTunner", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "SofTunner", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "SofTunner", __VA_ARGS__)
#else
#define LOGD printf
#endif

#define maxTextSize 300

extern "C" {
    tuneManagersf::tuneManagersf()
    {
        SF_WATER_TUNE *temp1;
        for(int i = 0; i < 4; i++)
        {
            if( i == 0 ) temp1 = &mFastMarkerInfo;
            else if( i == 1 ) temp1 = &mFastOuterInfo;
            else if( i == 2 ) temp1 = &mSlowMarkerInfo;
            else temp1 = &mSlowOuterInfo;

            temp1->resize = 4;
            temp1->scale = 6;
            temp1->moveX = -5.0f;
            temp1->moveY = -5.0f;
            temp1->leftRightScale = 5;
            temp1->onoff = -1;
            temp1->preBlur = 7;
        }

        SF_FEATHER_TUNE *temp2;
        for(int i = 0; i < 2; i++)
        {
            if( i == 0 ) temp2 = &mFastInfo;
            else temp2 = &mSlowInfo;

            temp2->mFStart = -6;
            temp2->mFThickness = 27;
            temp2->mResizeX = 4;
            temp2->mResizeY = 4;
            temp2->mColor = 0.3f;
            temp2->mColorStart = 0.2f;
            temp2->mMovingBlur = 5.0f;
            temp2->mMovingArea = 10.f;
            temp2->mMovingSens = 50.f;
            temp2->mMovingValue = 2.5f;
            temp2->mTransUD = 0.0f;
            temp2->mTransLR = 0.0f;
            temp2->mLRScale = 2.0f;
            temp2->mStopCount = 5.f;
            temp2->mTargetGamma = 150.f;
            temp2->mMaxGamma = 1.5f;
        }

        loadingTuneData();
    }

    tuneManagersf::~tuneManagersf()
    {

    }

    void tuneManagersf::printAllData()
    {
        SF_WATER_TUNE *temp1;

        for( int i = 0; i < 4; i++ )
        {
            if( i == 0 ) temp1 = &mFastMarkerInfo;
            else if( i == 1 ) temp1 = &mFastOuterInfo;
            else if( i == 2 ) temp1 = &mSlowMarkerInfo;
            else temp1 = &mSlowOuterInfo;

            LOGD("Tunning-%d %d %d %f %f %d %d %d", i, temp1->resize, temp1->scale, temp1->moveX, temp1->moveY, temp1->leftRightScale, temp1->onoff, temp1->preBlur);
        }

        SF_FEATHER_TUNE *temp2;

        for( int i = 0; i < 2; i++ )
        {
            if( i == 0 ) temp2 = &mFastInfo;
            else temp2 = &mSlowInfo;

            LOGD("Tunning-%d %d %d %d %d %f %f %f %f %f %f %f %f %f %f %f %f", i, temp2->mFStart, temp2->mFThickness, temp2->mResizeX, temp2->mResizeY, temp2->mColor, temp2->mColorStart,
                 temp2->mMovingBlur, temp2->mMovingArea, temp2->mMovingSens, temp2->mMovingValue, temp2->mTransUD, temp2->mTransLR, temp2->mLRScale, temp2->mStopCount, temp2->mTargetGamma, temp2->mMaxGamma);
        }
    }

    void tuneManagersf::loadingTuneData()
    {
        //char *path = "/data/local/tmp/of/TuningTool27.txt";
        char *path = "/sdcard/sfcam/of/TuningTool27.txt";
        FILE *fd = fopen(path, "rt");
        if(fd)
        {
			char textTemp[maxTextSize];
            char text[maxTextSize];

            fgets(text, maxTextSize, fd);
            fgets(text, maxTextSize, fd);

            SF_WATER_TUNE *temp1;

            for( int i = 0; i < 2; i++ )
            {
                if( i == 0 ) temp1 = &mFastMarkerInfo;
                else temp1 = &mFastOuterInfo;

                fgets(text, maxTextSize, fd);
                sscanf(text, "%s %d %d %f %f %d %d %d", textTemp, &temp1->resize, &temp1->scale, &temp1->moveX, &temp1->moveY, &temp1->leftRightScale, &temp1->onoff, &temp1->preBlur);
            }

            fgets(text, maxTextSize, fd);

            for( int i = 0; i < 2; i++ )
            {
                if( i == 0 ) temp1 = &mSlowMarkerInfo;
                else temp1 = &mSlowOuterInfo;

                fgets(text, maxTextSize, fd);
                sscanf(text, "%s %d %d %f %f %d %d %d", textTemp, &temp1->resize, &temp1->scale, &temp1->moveX, &temp1->moveY, &temp1->leftRightScale, &temp1->onoff, &temp1->preBlur);
            }

            fgets(text, maxTextSize, fd);

            SF_FEATHER_TUNE *temp2;

            for( int i = 0; i < 2; i++ )
            {
                fgets(text, maxTextSize, fd);
                fgets(text, maxTextSize, fd);

                if( i == 0 ) temp2 = &mFastInfo;
                else temp2 = &mSlowInfo;

                fgets(text, maxTextSize, fd);
                sscanf(text, "%d %d %d %d %f %f %f %f %f %f %f %f %f %f %f %f", &temp2->mFStart, &temp2->mFThickness, &temp2->mResizeX, &temp2->mResizeY,
                       &temp2->mColor, &temp2->mColorStart, &temp2->mMovingBlur, &temp2->mMovingArea, &temp2->mMovingSens, &temp2->mMovingValue, &temp2->mTransUD,
                       &temp2->mTransLR, &temp2->mLRScale, &temp2->mStopCount, &temp2->mTargetGamma, &temp2->mMaxGamma);
            }

            fclose(fd);

            printAllData();
        }
        else
        {
            LOGD("%s : open fail\n", path);
        }
    }

    void tuneManagersf::getFastWaterTuneData(SF_WATER_TUNE &marker, SF_WATER_TUNE &outer)
    {
        marker = mFastMarkerInfo;
        outer = mFastOuterInfo;
    }

    void tuneManagersf::getSlowWaterTuneData(SF_WATER_TUNE &marker, SF_WATER_TUNE &outer)
    {
        marker = mSlowMarkerInfo;
        outer = mSlowOuterInfo;
    }

    void tuneManagersf::getFeatherTuneData(SF_FEATHER_TUNE &fastInfo, SF_FEATHER_TUNE &slowInfo)
    {
        fastInfo = mFastInfo;
        slowInfo = mSlowInfo;
    }

    void tuneManagersf::getOnOffAlg(int &fastAlg, int &slowAlg)
    {
        fastAlg = mFastMarkerInfo.onoff;
        slowAlg = mSlowMarkerInfo.onoff;
    }
}