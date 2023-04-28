//#include "stdafx.h"
#include "sfPersonTunner.h"

#include <stdio.h>
#include <cstring>

#ifndef WIN32
#include <android/log.h>

// log
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "tuneManagerPersonSf", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "tuneManagerPersonSf", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "tuneManagerPersonSf", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "tuneManagerPersonSf", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "tuneManagerPersonSf", __VA_ARGS__)
#else
#define LOGD printf
#endif

#define maxTextSize 300

extern "C" {
    tuneManagerPersonSf::tuneManagerPersonSf()
    {
        fastAlOn = 1;
        slowAlOn = 1;
        stopCount = 5;

        for( int i = 0; i < 4; i++ )
        {
            backObj.commonTune[i].brightness = -1;
            backObj.commonTune[i].movingBlur = 5;
            backObj.commonTune[i].movingArea = 0;
            backObj.commonTune[i].movingSens = 15;
            backObj.commonTune[i].movingThreshold = 1.f;
            backObj.commonTune[i].movingMovieThreshold = 5.f;
            backObj.commonTune[i].waterBlur = 1;
            backObj.commonTune[i].TargetGamma = 160.f;
            backObj.commonTune[i].maxGamma = 1.7f;
            backObj.commonTune[i].cartoonBackTexRate = 0.5f;
            backObj.commonTune[i].cartoonBackSat = 70.f;
            backObj.commonTune[i].cartoonBackEdge = 10.f;
            backObj.commonTune[i].cartoonFrontTexRate = 0.3f;
            backObj.commonTune[i].cartoonFrontSat = 60.f;
            backObj.commonTune[i].cartoonFrontEdge = 20.f;
            backObj.commonTune[i].beautyRate = -1.f;
            backObj.commonTune[i].blurCount = -1;
            backObj.commonTune[i].previewBlur1 = -1;
            backObj.commonTune[i].previewBlur2 = -1;
            backObj.commonTune[i].previewBlur3 = -1;
            backObj.commonTune[i].captureBlur1 = -1;
            backObj.commonTune[i].captureBlur2 = -1;
            backObj.commonTune[i].captureBlur3 = -1;
            backObj.commonTune[i].videoBlur1 = -1;
            backObj.commonTune[i].videoBlur2 = -1;
            backObj.commonTune[i].videoBlur3 = -1;
            backObj.commonTune[i].fastMoving = 10;
            backObj.commonTune[i].minFaceSize = 8;
            backObj.commonTune[i].maxFaceSize = 20;
            backObj.commonTune[i].minMovingPercent = 50;
            backObj.commonTune[i].maxMovingPercent = 100;

            backObj.fastTune[i].scaleXcartoon = 1.0f;
            backObj.fastTune[i].scaleYcartoon = 1.0f;
            backObj.fastTune[i].scaleXblur = 1.0f;
            backObj.fastTune[i].scaleYblur = 1.0f;
            backObj.fastTune[i].scaleXsf = 1.0f;
            backObj.fastTune[i].scaleYsf = 1.0f;
            backObj.fastTune[i].cartoonThickness = 16.f;
            backObj.fastTune[i].blurThickness = 16.f;
            backObj.fastTune[i].sfThickness = 16.f;
            backObj.fastTune[i].resizeXY = 4.f;
            backObj.fastTune[i].color = 1.f;
            backObj.fastTune[i].colorStart = 0.5f;
            backObj.fastTune[i].transUD = 0.000f;
            backObj.fastTune[i].transLR= 0.000f;

            backObj.slowTune[i].scaleXcartoon = 1.0f;
            backObj.slowTune[i].scaleYcartoon = 1.0f;
            backObj.slowTune[i].scaleXblur = 1.0f;
            backObj.slowTune[i].scaleYblur = 1.0f;
            backObj.slowTune[i].scaleXsf = 1.0f;
            backObj.slowTune[i].scaleYsf = 1.0f;
            backObj.slowTune[i].cartoonThickness = 16.f;
            backObj.slowTune[i].blurThickness = 16.f;
            backObj.slowTune[i].sfThickness = 16.f;
            backObj.slowTune[i].resizeXY = 4.f;
            backObj.slowTune[i].color = 0.f;
            backObj.slowTune[i].colorStart = 0.1f;
            backObj.slowTune[i].transUD = 0.000f;
            backObj.slowTune[i].transLR= 0.000f;
        }

        for( int i = 0; i < 2; i++ )
        {
            SF_PERSON_TUNE *temp;
            if( i == 0 ) temp = &frontPerson;
            else temp = &backPerson;

            temp->studioTune.faderStart = -4;
            temp->studioTune.faderCount = 40;
            temp->studioTune.faderBright = 1;
            temp->studioTune.faderSaturation = 1.4f;
            temp->studioTune.inBright = 1.08f;
            temp->studioTune.outBright = 0.85f;
            temp->studioTune.satRate = 0.0f;
            temp->studioTune.circleRate = 0.5f;
            temp->studioTune.blackRate = 1.1f;
            temp->studioTune.contrast = 105.f;
            temp->studioTune.blackTuneRate = 5.0f;

            for(int j = 0 ; j < 3; j++)
            {
                temp->commonTune[j].brightness = 1500;
                temp->commonTune[j].movingBlur = 7;
                temp->commonTune[j].movingArea = -15;
                temp->commonTune[j].movingSens = 15;
                temp->commonTune[j].movingThreshold = 1.f;
                temp->commonTune[j].movingMovieThreshold = 1.f;
                temp->commonTune[j].waterBlur = 1;
                temp->commonTune[j].TargetGamma = 160.f;
                temp->commonTune[j].maxGamma = 2.f;
                temp->commonTune[j].cartoonBackTexRate = 0.5f;
                temp->commonTune[j].cartoonBackSat = 70.1f;
                temp->commonTune[j].cartoonBackEdge = 10.f;
                temp->commonTune[j].cartoonFrontTexRate = 0.5f;
                temp->commonTune[j].cartoonFrontSat = 70.f;
                temp->commonTune[j].cartoonFrontEdge = 20.f;
                temp->commonTune[j].beautyRate = 0.9f;
                temp->commonTune[j].blurCount = 2;
                temp->commonTune[j].previewBlur1 = 5;
                temp->commonTune[j].previewBlur2 = 7;
                temp->commonTune[j].previewBlur3 = 9;
                temp->commonTune[j].captureBlur1 = 5;
                temp->commonTune[j].captureBlur2 = 7;
                temp->commonTune[j].captureBlur3 = 9;
                temp->commonTune[j].videoBlur1 = 5;
                temp->commonTune[j].videoBlur2 = 7;
                temp->commonTune[j].videoBlur3 = 9;
                temp->commonTune[j].fastMoving = 10;
                temp->commonTune[j].minFaceSize = 8;
                temp->commonTune[j].maxFaceSize = 20;
                temp->commonTune[j].minMovingPercent = 50;
                temp->commonTune[j].maxMovingPercent = 100;

                temp->waterMarkerTune[j].resize = 4;
                temp->waterMarkerTune[j].scale = 3;
                temp->waterMarkerTune[j].moveX = 0;
                temp->waterMarkerTune[j].moveY = 3;
                temp->waterMarkerTune[j].leftRightScale = 2;

                temp->waterOuterTune[j].resize = 4;
                temp->waterOuterTune[j].scale = 3;
                temp->waterOuterTune[j].moveX = 0;
                temp->waterOuterTune[j].moveY = 3;
                temp->waterOuterTune[j].leftRightScale = 2;

                temp->waterMarkerTuneForSave[j].resize = 4;
                temp->waterMarkerTuneForSave[j].scale = 3;
                temp->waterMarkerTuneForSave[j].moveX = 0;
                temp->waterMarkerTuneForSave[j].moveY = 3;
                temp->waterMarkerTuneForSave[j].leftRightScale = 2;

                temp->waterOuterTuneForSave[j].resize = 4;
                temp->waterOuterTuneForSave[j].scale = 3;
                temp->waterOuterTuneForSave[j].moveX = 0;
                temp->waterOuterTuneForSave[j].moveY = 3;
                temp->waterOuterTuneForSave[j].leftRightScale = 2;

                temp->superFastTune[i].scaleXcartoon = 1.0f;
                temp->superFastTune[i].scaleYcartoon = 1.0f;
                temp->superFastTune[i].scaleXblur = 1.0f;
                temp->superFastTune[i].scaleYblur = 1.0f;
                temp->superFastTune[i].scaleXsf = 1.0f;
                temp->superFastTune[i].scaleYsf = 1.0f;
                temp->superFastTune[i].cartoonThickness = 16.f;
                temp->superFastTune[i].blurThickness = 16.f;
                temp->superFastTune[i].sfThickness = 16.f;
                temp->superFastTune[j].resizeXY = 4.f;
                temp->superFastTune[j].color = 0.f;
                temp->superFastTune[j].colorStart = 0.1f;
                temp->superFastTune[j].transUD = 0.0001f;
                temp->superFastTune[j].transLR= 0.0001f;

                temp->fastTune[i].scaleXcartoon = 1.0f;
                temp->fastTune[i].scaleYcartoon = 1.0f;
                temp->fastTune[i].scaleXblur = 1.0f;
                temp->fastTune[i].scaleYblur = 1.0f;
                temp->fastTune[i].scaleXsf = 1.0f;
                temp->fastTune[i].scaleYsf = 1.0f;
                temp->fastTune[i].cartoonThickness = 16.f;
                temp->fastTune[i].blurThickness = 16.f;
                temp->fastTune[i].sfThickness = 16.f;
                temp->fastTune[j].resizeXY = 4.f;
                temp->fastTune[j].color = 0.f;
                temp->fastTune[j].colorStart = 0.1f;
                temp->fastTune[j].transUD = 0.0001f;
                temp->fastTune[j].transLR= 0.0001f;

                temp->slowTune[i].scaleXcartoon = 1.0f;
                temp->slowTune[i].scaleYcartoon = 1.0f;
                temp->slowTune[i].scaleXblur = 1.0f;
                temp->slowTune[i].scaleYblur = 1.0f;
                temp->slowTune[i].scaleXsf = 1.0f;
                temp->slowTune[i].scaleYsf = 1.0f;
                temp->slowTune[i].cartoonThickness = 16.f;
                temp->slowTune[i].blurThickness = 16.f;
                temp->slowTune[i].sfThickness = 16.f;
                temp->slowTune[j].resizeXY = 4.f;
                temp->slowTune[j].color = 0.f;
                temp->slowTune[j].colorStart = 0.1f;
                temp->slowTune[j].transUD = 0.0001f;
                temp->slowTune[j].transLR= 0.0001f;

                temp->saveTune[i].scaleXcartoon = 1.0f;
                temp->saveTune[i].scaleYcartoon = 1.0f;
                temp->saveTune[i].scaleXblur = 1.0f;
                temp->saveTune[i].scaleYblur = 1.0f;
                temp->saveTune[i].scaleXsf = 1.0f;
                temp->saveTune[i].scaleYsf = 1.0f;
                temp->saveTune[i].cartoonThickness = 16.f;
                temp->saveTune[i].blurThickness = 16.f;
                temp->saveTune[i].sfThickness = 16.f;
                temp->saveTune[j].resizeXY = 4.f;
                temp->saveTune[j].color = 0.f;
                temp->saveTune[j].colorStart = 0.1f;
                temp->saveTune[j].transUD = 0.0001f;
                temp->saveTune[j].transLR= 0.0001f;
            }
        }

        mCurrentStatus = SF_BACK_OBJ;
        mCurrentBright = SF_NORMAL;
        loadingTuneData();
    }

    tuneManagerPersonSf::~tuneManagerPersonSf()
    {

    }

    void tuneManagerPersonSf::printAllData()
    {
        LOGD("[Tune-Test] fastAlOn : %d", fastAlOn);
        LOGD("[Tune-Test] slowAlon : %d", slowAlOn);
        LOGD("[Tune-Test] stopCount : %d", stopCount);

        for(int i = 0; i < 4; i++) {
            LOGD("[Tune-Test] %f %f %f %f %f %f %f %f %f %f %f %f %f %f %d %d %d %d %f %f",
                   backObj.commonTune[i].cartoonBackTexRate,
                   backObj.commonTune[i].cartoonBackSat,
                   backObj.commonTune[i].cartoonBackEdge,
                   backObj.fastTune[i].scaleXcartoon,
                   backObj.fastTune[i].scaleYcartoon,
                   backObj.fastTune[i].cartoonThickness,
                   backObj.fastTune[i].scaleXblur,
                   backObj.fastTune[i].scaleYblur,
                   backObj.fastTune[i].blurThickness,
                   backObj.fastTune[i].scaleXsf,
                   backObj.fastTune[i].scaleYsf,
                   backObj.fastTune[i].sfThickness,
                   backObj.fastTune[i].color,
                   backObj.fastTune[i].colorStart,
                   backObj.commonTune[i].minFaceSize,
                   backObj.commonTune[i].maxFaceSize,
                   backObj.commonTune[i].minMovingPercent,
                   backObj.commonTune[i].maxMovingPercent,
                   backObj.commonTune[i].TargetGamma,
                   backObj.commonTune[i].maxGamma);
        }

        for( int i = 0; i < 2; i++ ) {

            SF_PERSON_TUNE *temp;
            if( i == 0 )
            {
                temp = &frontPerson;
                LOGD("[Tune-Test] frontPerson....................................................");
            }
            else
            {
                temp = &backPerson;
                LOGD("[Tune-Test] backPerson....................................................");
            }

            LOGD("[Tune-Test] %d %d %f %f %f %f %f %f %f %f %f",
                   temp->studioTune.faderStart,
                   temp->studioTune.faderCount,
                   temp->studioTune.faderBright,
                   temp->studioTune.faderSaturation,
                   temp->studioTune.inBright,
                   temp->studioTune.outBright,
                   temp->studioTune.satRate,
                   temp->studioTune.circleRate,
                   temp->studioTune.blackRate,
                   temp->studioTune.contrast,
                   temp->studioTune.blackTuneRate);

            for(int j = 0 ; j < 3; j++)
            {
                if( j == 0 ) LOGD("[Tune-Test] bright-1.........................................");
                else if( j == 1 ) LOGD("[Tune-Test] bright-2.........................................");
                else if( j == 2 ) LOGD("[Tune-Test] bright-3.........................................");

                LOGD("[Tune-Test] %d %d %d %d %f %f %d %f %f %f %f %f %f %f %f %f %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d",
                       temp->commonTune[j].brightness,
                       temp->commonTune[j].movingBlur,
                       temp->commonTune[j].movingArea,
                       temp->commonTune[j].movingSens,
                       temp->commonTune[j].movingThreshold,
                       temp->commonTune[j].movingMovieThreshold,
                       temp->commonTune[j].waterBlur,
                       temp->commonTune[j].TargetGamma,
                       temp->commonTune[j].maxGamma,
                       temp->commonTune[j].cartoonBackTexRate,
                       temp->commonTune[j].cartoonBackSat,
                       temp->commonTune[j].cartoonBackEdge,
                       temp->commonTune[j].cartoonFrontTexRate,
                       temp->commonTune[j].cartoonFrontSat,
                       temp->commonTune[j].cartoonFrontEdge,
                       temp->commonTune[j].beautyRate,
                       temp->commonTune[j].blurCount,
                       temp->commonTune[j].previewBlur1,
                       temp->commonTune[j].previewBlur2,
                       temp->commonTune[j].previewBlur3,
                       temp->commonTune[j].captureBlur1,
                       temp->commonTune[j].captureBlur2,
                       temp->commonTune[j].captureBlur3,
                       temp->commonTune[j].videoBlur1,
                       temp->commonTune[j].videoBlur2,
                       temp->commonTune[j].videoBlur3,
                       temp->commonTune[j].fastMoving,
                       temp->commonTune[j].minFaceSize,
                       temp->commonTune[j].maxFaceSize,
                       temp->commonTune[j].minMovingPercent,
                       temp->commonTune[j].maxMovingPercent);

                LOGD("[Tune-Test] %d %d %f %f %d",
                       temp->waterMarkerTune[j].resize,
                       temp->waterMarkerTune[j].scale,
                       temp->waterMarkerTune[j].moveX,
                       temp->waterMarkerTune[j].moveY,
                       temp->waterMarkerTune[j].leftRightScale);

                LOGD("[Tune-Test] %d %d %f %f %d",
                     temp->waterMarkerTuneForSave[j].resize,
                     temp->waterMarkerTuneForSave[j].scale,
                     temp->waterMarkerTuneForSave[j].moveX,
                     temp->waterMarkerTuneForSave[j].moveY,
                     temp->waterMarkerTuneForSave[j].leftRightScale);

                LOGD("[Tune-Test] %d %d %f %f %d",
                       temp->waterOuterTune[j].resize,
                       temp->waterOuterTune[j].scale,
                       temp->waterOuterTune[j].moveX,
                       temp->waterOuterTune[j].moveY,
                       temp->waterOuterTune[j].leftRightScale);

                LOGD("[Tune-Test] %d %d %f %f %d",
                     temp->waterOuterTuneForSave[j].resize,
                     temp->waterOuterTuneForSave[j].scale,
                     temp->waterOuterTuneForSave[j].moveX,
                     temp->waterOuterTuneForSave[j].moveY,
                     temp->waterOuterTuneForSave[j].leftRightScale);

               LOGD("[Tune-Test] %f %f %f %f %f %f %f %f %f %f %f %f %f %f",
                       temp->superFastTune[j].scaleXcartoon,
                       temp->superFastTune[j].scaleYcartoon,
                       temp->superFastTune[j].scaleXblur,
                       temp->superFastTune[j].scaleYblur,
                       temp->superFastTune[j].scaleXsf,
                       temp->superFastTune[j].scaleYsf,
                       temp->superFastTune[j].cartoonThickness,
                       temp->superFastTune[j].blurThickness,
                       temp->superFastTune[j].sfThickness,
                       temp->superFastTune[j].resizeXY,
                       temp->superFastTune[j].color,
                       temp->superFastTune[j].colorStart,
                       temp->superFastTune[j].transUD,
                       temp->superFastTune[j].transLR);

                LOGD("[Tune-Test] %f %f %f %f %f %f %f %f %f %f %f %f %f %f",
                     temp->fastTune[j].scaleXcartoon,
                     temp->fastTune[j].scaleYcartoon,
                     temp->fastTune[j].scaleXblur,
                     temp->fastTune[j].scaleYblur,
                     temp->fastTune[j].scaleXsf,
                     temp->fastTune[j].scaleYsf,
                     temp->fastTune[j].cartoonThickness,
                     temp->fastTune[j].blurThickness,
                     temp->fastTune[j].sfThickness,
                     temp->fastTune[j].resizeXY,
                     temp->fastTune[j].color,
                     temp->fastTune[j].colorStart,
                     temp->fastTune[j].transUD,
                     temp->fastTune[j].transLR);

                LOGD("[Tune-Test] %f %f %f %f %f %f %f %f %f %f %f %f %f %f",
                       temp->slowTune[j].scaleXcartoon,
                       temp->slowTune[j].scaleYcartoon,
                       temp->slowTune[j].scaleXblur,
                       temp->slowTune[j].scaleYblur,
                       temp->slowTune[j].scaleXsf,
                       temp->slowTune[j].scaleYsf,
                       temp->slowTune[j].cartoonThickness,
                       temp->slowTune[j].blurThickness,
                       temp->slowTune[j].sfThickness,
                       temp->slowTune[j].resizeXY,
                       temp->slowTune[j].color,
                       temp->slowTune[j].colorStart,
                       temp->slowTune[j].transUD,
                       temp->slowTune[j].transLR);

                LOGD("[Tune-Test] %f %f %f %f %f %f %f %f %f %f %f %f %f %f",
                     temp->saveTune[j].scaleXcartoon,
                     temp->saveTune[j].scaleYcartoon,
                     temp->saveTune[j].scaleXblur,
                     temp->saveTune[j].scaleYblur,
                     temp->saveTune[j].scaleXsf,
                     temp->saveTune[j].scaleYsf,
                     temp->saveTune[j].cartoonThickness,
                     temp->saveTune[j].blurThickness,
                     temp->saveTune[j].sfThickness,
                     temp->saveTune[j].resizeXY,
                     temp->saveTune[j].color,
                     temp->saveTune[j].colorStart,
                     temp->saveTune[j].transUD,
                     temp->saveTune[j].transLR);
            }
        }
    }

    void tuneManagerPersonSf::loadingTuneData()
    {
        //char *path = "/data/local/tmp/of/TuningToolP.txt";
        char *path = "/sdcard/sfcam/of/TuningToolP.txt";
        FILE *fd = fopen(path, "rt");
        if(fd)
        {
            LOGD("File open is success...");

			char textTemp[maxTextSize];
            char text[maxTextSize];

            fgets(text, maxTextSize, fd);
            sscanf(text, "%s %d", textTemp, &fastAlOn);
            fgets(text, maxTextSize, fd);
            sscanf(text, "%s %d", textTemp, &slowAlOn);
            fgets(text, maxTextSize, fd);
            sscanf(text, "%s %d", textTemp, &stopCount);

            LOGD("fastAlOn : %d, slowAlOn : %d, stopCount : %d", fastAlOn, slowAlOn, stopCount);

//            fgets(text, maxTextSize, fd);
//            fgets(text, maxTextSize, fd);
//            fgets(text, maxTextSize, fd);
//            fgets(text, maxTextSize, fd);

//            fgets(text, maxTextSize, fd);
//            for(int i = 0; i < 3; i++) {
//                sscanf(text, "%f %f %f %f %f %f %f",
//                       &backObj.commonTune[i].cartoonBackTexRate,
//                       &backObj.commonTune[i].cartoonBackSat,
//                       &backObj.commonTune[i].cartoonBackEdge,
//                       &backObj.fastTune[i].scaleX,
//                       &backObj.fastTune[i].scaleY,
//                       &backObj.fastTune[i].nThickness,
//                       &backObj.fastTune[i].bThickness);
//            }

            for( int i = 0; i < 2; i++ ) {
                fgets(text, maxTextSize, fd);
                fgets(text, maxTextSize, fd);
                fgets(text, maxTextSize, fd);
                fgets(text, maxTextSize, fd);
                fgets(text, maxTextSize, fd);

                SF_PERSON_TUNE *temp;
                if( i == 0 ) temp = &frontPerson;
                else temp = &backPerson;

                fgets(text, maxTextSize, fd);
                sscanf(text, "%s %d %d %f %f %f %f %f %f %f %f %f",
                       textTemp,
                       &temp->studioTune.faderStart,
                       &temp->studioTune.faderCount,
                       &temp->studioTune.faderBright,
                       &temp->studioTune.faderSaturation,
                       &temp->studioTune.inBright,
                       &temp->studioTune.outBright,
                       &temp->studioTune.satRate,
                       &temp->studioTune.circleRate,
                       &temp->studioTune.blackRate,
                       &temp->studioTune.contrast,
                       &temp->studioTune.blackTuneRate);

                for(int j = 0 ; j < 3; j++)
                {
                    fgets(text, maxTextSize, fd);
                    fgets(text, maxTextSize, fd);
                    fgets(text, maxTextSize, fd);
                    fgets(text, maxTextSize, fd);

                    fgets(text, maxTextSize, fd);
                    sscanf(text, "%d %d %d %d %f %f %d %f %f %f %f %f %f %f %f %f %d %d %d %d %d %d %d %d %d %d",
                            &temp->commonTune[j].brightness,
                            &temp->commonTune[j].movingBlur,
                            &temp->commonTune[j].movingArea,
                            &temp->commonTune[j].movingSens,
                            &temp->commonTune[j].movingThreshold,
                            &temp->commonTune[j].movingMovieThreshold,
                            &temp->commonTune[j].waterBlur,
                            &temp->commonTune[j].TargetGamma,
                            &temp->commonTune[j].maxGamma,
                            &temp->commonTune[j].cartoonBackTexRate,
                            &temp->commonTune[j].cartoonBackSat,
                            &temp->commonTune[j].cartoonBackEdge,
                            &temp->commonTune[j].cartoonFrontTexRate,
                            &temp->commonTune[j].cartoonFrontSat,
                            &temp->commonTune[j].cartoonFrontEdge,
                            &temp->commonTune[j].beautyRate,
                            &temp->commonTune[j].blurCount,
                            &temp->commonTune[j].previewBlur1,
                            &temp->commonTune[j].previewBlur2,
                            &temp->commonTune[j].previewBlur3,
                            &temp->commonTune[j].captureBlur1,
                            &temp->commonTune[j].captureBlur2,
                            &temp->commonTune[j].captureBlur3,
                            &temp->commonTune[j].videoBlur1,
                            &temp->commonTune[j].videoBlur2,
                            &temp->commonTune[j].videoBlur3);

                    fgets(text, maxTextSize, fd);
                    fgets(text, maxTextSize, fd);
                    fgets(text, maxTextSize, fd);

                    fgets(text, maxTextSize, fd);
                    sscanf(text, "%s %d %d %f %f %d %s %d %d %f %f %d",
                            textTemp,
                            &temp->waterMarkerTune[j].resize,
                            &temp->waterMarkerTune[j].scale,
                            &temp->waterMarkerTune[j].moveX,
                            &temp->waterMarkerTune[j].moveY,
                            &temp->waterMarkerTune[j].leftRightScale,
                            textTemp,
                            &temp->waterMarkerTuneForSave[j].resize,
                            &temp->waterMarkerTuneForSave[j].scale,
                            &temp->waterMarkerTuneForSave[j].moveX,
                            &temp->waterMarkerTuneForSave[j].moveY,
                            &temp->waterMarkerTuneForSave[j].leftRightScale);

                    fgets(text, maxTextSize, fd);
                    sscanf(text, "%s %d %d %f %f %d %s %d %d %f %f %d",
                            textTemp,
                            &temp->waterOuterTune[j].resize,
                            &temp->waterOuterTune[j].scale,
                            &temp->waterOuterTune[j].moveX,
                            &temp->waterOuterTune[j].moveY,
                            &temp->waterOuterTune[j].leftRightScale,
                            textTemp,
                            &temp->waterOuterTuneForSave[j].resize,
                            &temp->waterOuterTuneForSave[j].scale,
                            &temp->waterOuterTuneForSave[j].moveX,
                            &temp->waterOuterTuneForSave[j].moveY,
                            &temp->waterOuterTuneForSave[j].leftRightScale);

                    fgets(text, maxTextSize, fd);
                    fgets(text, maxTextSize, fd);
                    fgets(text, maxTextSize, fd);
                    fgets(text, maxTextSize, fd);

                    fgets(text, maxTextSize, fd);
                    if( i == 0 ) {
                        sscanf(text,
                               "%f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %d %d %d %d %d",
                               &temp->superFastTune[j].scaleXcartoon,
                               &temp->superFastTune[j].scaleYcartoon,
                               &temp->superFastTune[j].cartoonThickness,
                               &temp->superFastTune[j].blurThickness,
                               &temp->superFastTune[j].resizeXY,
                               &temp->superFastTune[j].color,
                               &temp->superFastTune[j].colorStart,
                               &temp->superFastTune[j].transUD,
                               &temp->superFastTune[j].transLR,
                               &temp->fastTune[j].scaleXcartoon,
                               &temp->fastTune[j].scaleYcartoon,
                               &temp->fastTune[j].cartoonThickness,
                               &temp->fastTune[j].blurThickness,
                               &temp->fastTune[j].resizeXY,
                               &temp->fastTune[j].color,
                               &temp->fastTune[j].colorStart,
                               &temp->fastTune[j].transUD,
                               &temp->fastTune[j].transLR,
                               &temp->commonTune[j].fastMoving,
                               &temp->commonTune[j].minFaceSize,
                               &temp->commonTune[j].maxFaceSize,
                               &temp->commonTune[j].minMovingPercent,
                               &temp->commonTune[j].maxMovingPercent);
                    }
                    else
                    {
                        sscanf(text,
                               //"%f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %d %d %d %d %d",
                               "%f %f %f %f %f %f %f %f %f %d %d %d %d %d",
                               &temp->superFastTune[j].scaleXcartoon,
                               &temp->superFastTune[j].scaleYcartoon,
                               &temp->superFastTune[j].cartoonThickness,
                               &temp->superFastTune[j].blurThickness,
                               &temp->superFastTune[j].resizeXY,
                               &temp->superFastTune[j].color,
                               &temp->superFastTune[j].colorStart,
                               &temp->superFastTune[j].transUD,
                               &temp->superFastTune[j].transLR,
//                               &temp->fastTune[j].scaleXcartoon,
//                               &temp->fastTune[j].scaleYcartoon,
//                               &temp->fastTune[j].cartoonThickness,
//                               &temp->fastTune[j].blurThickness,
//                               &temp->fastTune[j].resizeXY,
//                               &temp->fastTune[j].color,
//                               &temp->fastTune[j].colorStart,
//                               &temp->fastTune[j].transUD,
//                               &temp->fastTune[j].transLR,
                               &temp->commonTune[j].fastMoving,
                               &temp->commonTune[j].minFaceSize,
                               &temp->commonTune[j].maxFaceSize,
                               &temp->commonTune[j].minMovingPercent,
                               &temp->commonTune[j].maxMovingPercent);
                    }

                    fgets(text, maxTextSize, fd);
                    fgets(text, maxTextSize, fd);

                    fgets(text, maxTextSize, fd);
                    sscanf(text, "%f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f",
                            &temp->slowTune[j].scaleXcartoon,
                            &temp->slowTune[j].scaleYcartoon,
                            &temp->slowTune[j].cartoonThickness,
                            &temp->slowTune[j].blurThickness,
                            &temp->slowTune[j].resizeXY,
                            &temp->slowTune[j].color,
                            &temp->slowTune[j].colorStart,
                            &temp->slowTune[j].transUD,
                            &temp->slowTune[j].transLR,
                            &temp->saveTune[j].scaleXcartoon,
                            &temp->saveTune[j].scaleYcartoon,
                            &temp->saveTune[j].cartoonThickness,
                            &temp->saveTune[j].blurThickness,
                            &temp->saveTune[j].resizeXY,
                            &temp->saveTune[j].color,
                            &temp->saveTune[j].colorStart,
                            &temp->saveTune[j].transUD,
                            &temp->saveTune[j].transLR);
                }
            }

            for(int i = 0; i < 39; i ++) {
                fgets(text, maxTextSize, fd);
            }

            fgets(text, maxTextSize, fd);
            fgets(text, maxTextSize, fd);
            fgets(text, maxTextSize, fd);
            fgets(text, maxTextSize, fd);
            fgets(text, maxTextSize, fd);

            fgets(text, maxTextSize, fd);
            for(int i = 0; i < 4; i++) {
                sscanf(text, "%f %f %f %f %f %f %f %f %f %f %f %f %f",
                       &backObj.commonTune[i].cartoonBackTexRate,
                       &backObj.commonTune[i].cartoonBackSat,
                       &backObj.commonTune[i].cartoonBackEdge,
                       &backObj.fastTune[i].scaleXcartoon,
                       &backObj.fastTune[i].scaleYcartoon,
                       &backObj.fastTune[i].cartoonThickness,
                       &backObj.fastTune[i].scaleXblur,
                       &backObj.fastTune[i].scaleYblur,
                       &backObj.fastTune[i].scaleXsf,
                       &backObj.fastTune[i].scaleYsf,
                       &backObj.fastTune[i].sfThickness,
                       &backObj.fastTune[i].color,
                       &backObj.fastTune[i].colorStart);
            }

            fgets(text, maxTextSize, fd);
            fgets(text, maxTextSize, fd);
            fgets(text, maxTextSize, fd);

            fgets(text, maxTextSize, fd);
            for(int i = 0; i < 4; i++) {
                sscanf(text, "%d %d %d %d",
                       &backObj.commonTune[i].minFaceSize,
                       &backObj.commonTune[i].maxFaceSize,
                       &backObj.commonTune[i].minMovingPercent,
                       &backObj.commonTune[i].maxMovingPercent);
            }

            fgets(text, maxTextSize, fd);
            fgets(text, maxTextSize, fd);

            fgets(text, maxTextSize, fd);
            for(int i = 0; i < 4; i++) {
                sscanf(text, "%f %f",
                       &backObj.commonTune[i].TargetGamma,
                       &backObj.commonTune[i].maxGamma);
            }

            fclose(fd);

            printAllData();
        }
        else
        {
            LOGD("%s : open fail\n", path);
        }
    }

    bool tuneManagerPersonSf::getCurrentStatus()
    {
        if( mCurrentStatus <= SF_BACK_OBJ_MOVIE )
        {
            LOGD("getCurrentStatus : OBJ");
            return false;
        }
        else
        {
            LOGD("getCurrentStatus : PERSON");
            return true;
        }
    }

    void tuneManagerPersonSf::setObjBrightInfo(int superLowBright, int lowBright, int midBright, int outBright)
    {
        backObj.commonTune[0].brightness = superLowBright;
        backObj.commonTune[1].brightness = lowBright;
        backObj.commonTune[2].brightness = midBright;
        backObj.commonTune[3].brightness = outBright;
    }

    void tuneManagerPersonSf::setCurrentBright(double bright)
    {
        int dLow = -1, low, mid, out;

        if( mCurrentStatus == SF_BACK_OBJ || mCurrentStatus == SF_BACK_OBJ_SAVE || mCurrentStatus == SF_BACK_OBJ_MOVIE)
        {
            dLow = backObj.commonTune[0].brightness;
            low = backObj.commonTune[1].brightness;
            mid = backObj.commonTune[2].brightness;
            out = backObj.commonTune[3].brightness;
        }
        else if( mCurrentStatus == SF_BACK_FACE || mCurrentStatus == SF_BACK_FACE_SAVE || mCurrentStatus == SF_BACK_FACE_MOVIE)
        {
            low = backPerson.commonTune[0].brightness;
            mid = backPerson.commonTune[1].brightness;
            out = backPerson.commonTune[2].brightness;
        }
        else if( mCurrentStatus == SF_FRONT_FACE || mCurrentStatus == SF_FRONT_FACE_SAVE || mCurrentStatus == SF_FRONT_FACE_MOVIE)
        {
            low = frontPerson.commonTune[0].brightness;
            mid = frontPerson.commonTune[1].brightness;
            out = frontPerson.commonTune[2].brightness;
        }

        if( bright <= dLow )
        {
            mCurrentBright = SF_DEEP_LOWLIGHT;
        }
        else if( bright <= low )
        {
            mCurrentBright = SF_LOWLIGHT;
        }
        else if( bright <= mid )
        {
            mCurrentBright = SF_NORMAL;
        }
        else if( bright <= out )
        {
            mCurrentBright = SF_OUTDOOR;
        }

        LOGD("[Current-Bright] dLow : %d low : %d mid : %d out : %d", dLow, low, mid, out);
        LOGD("[Current-Bright] mBright : %f, mCurrentBright : %d", bright, mCurrentBright);
    }

    void tuneManagerPersonSf::setCurrentStatus(bool isFront, bool isFace, bool isSave, bool isMovie)
    {
        if( isFront == false)
        {
            if( isFace == false )
            {
                if( isSave == true )
                {
                    mCurrentStatus = SF_BACK_OBJ_SAVE;
                }
                else if(isMovie == true)
                {
                    mCurrentStatus = SF_BACK_OBJ_MOVIE;
                }
                else
                {
                    mCurrentStatus = SF_BACK_OBJ;
                }
            }
            else
            {
                if( isSave == true )
                {
                    mCurrentStatus = SF_BACK_FACE_SAVE;
                }
                else if(isMovie == true)
                {
                    mCurrentStatus = SF_BACK_FACE_MOVIE;
                }
                else
                {
                    mCurrentStatus = SF_BACK_FACE;
                }
            }
        }
        else
        {
            if( isFace == true ) {
                if( isSave == true )
                {
                    mCurrentStatus = SF_FRONT_FACE_SAVE;
                }
                else if(isMovie == true)
                {
                    mCurrentStatus = SF_FRONT_FACE_MOVIE;
                }
                else
                {
                    mCurrentStatus = SF_FRONT_FACE;
                }
            }
        }

        LOGD("[Tool-Test] mCurrentStatus : %d\n", mCurrentStatus);
    }

    void tuneManagerPersonSf::getAllTune(int &fastOn, int &slowOn, int &stop_count)
    {
        fastOn = fastAlOn;
        slowOn = slowAlOn;
        stop_count = stopCount;
    }

    void tuneManagerPersonSf::getStudioTune(SF_STUDIO_TUNEDATAS &datas)
    {
        if( mCurrentStatus <= SF_BACK_FACE_MOVIE )
        {
            datas = backPerson.studioTune;
        }
        else if( mCurrentStatus <= SF_FRONT_FACE_MOVIE )
        {
            datas = frontPerson.studioTune;
        }
    }

    void tuneManagerPersonSf::getCommonTune(SF_COMMON_TUNES &datas)
    {
        if( mCurrentStatus <= SF_BACK_OBJ_MOVIE )
        {
            datas = backObj.commonTune[mCurrentBright];
        }
        else if( mCurrentStatus <= SF_BACK_FACE_MOVIE )
        {
            datas = backPerson.commonTune[mCurrentBright-1];
        }
        else if( mCurrentStatus <= SF_FRONT_FACE_MOVIE )
        {
            datas = frontPerson.commonTune[mCurrentBright-1];
        }
    }

    void tuneManagerPersonSf::getMovingRateTune(int *datas)
    {
        if( mCurrentStatus <= SF_BACK_OBJ_MOVIE )
        {
            datas[0] = backObj.commonTune[mCurrentBright].fastMoving;
            datas[1] = backObj.commonTune[mCurrentBright].minFaceSize;
            datas[2] = backObj.commonTune[mCurrentBright].maxFaceSize;
            datas[3] = backObj.commonTune[mCurrentBright].minMovingPercent;
            datas[4] = backObj.commonTune[mCurrentBright].maxMovingPercent;
        }
        else if( mCurrentStatus <= SF_BACK_FACE_MOVIE )
        {
            datas[0] = backPerson.commonTune[mCurrentBright-1].fastMoving;
            datas[1] = backPerson.commonTune[mCurrentBright-1].minFaceSize;
            datas[2] = backPerson.commonTune[mCurrentBright-1].maxFaceSize;
            datas[3] = backPerson.commonTune[mCurrentBright-1].minMovingPercent;
            datas[4] = backPerson.commonTune[mCurrentBright-1].maxMovingPercent;
        }
        else if( mCurrentStatus <= SF_FRONT_FACE_MOVIE )
        {
            datas[0] = frontPerson.commonTune[mCurrentBright-1].fastMoving;
            datas[1] = frontPerson.commonTune[mCurrentBright-1].minFaceSize;
            datas[2] = frontPerson.commonTune[mCurrentBright-1].maxFaceSize;
            datas[3] = frontPerson.commonTune[mCurrentBright-1].minMovingPercent;
            datas[4] = frontPerson.commonTune[mCurrentBright-1].maxMovingPercent;
        }
    }

    int tuneManagerPersonSf::getWaterTune(SF_WATER_TUNES &marker, SF_WATER_TUNES &outer)
    {
        if( mCurrentStatus <= SF_BACK_FACE_MOVIE )
        {
            if( mCurrentStatus == SF_BACK_FACE_SAVE ) {
                marker = backPerson.waterMarkerTuneForSave[mCurrentBright - 1];
                outer = backPerson.waterOuterTuneForSave[mCurrentBright - 1];
            }
            else
            {
                marker = backPerson.waterMarkerTune[mCurrentBright - 1];
                outer = backPerson.waterOuterTune[mCurrentBright - 1];
            }

            LOGD("marker resize : %d, scale : %d, moveX : %f, moveY : %f, leftRightScale : %d", marker.resize, marker.scale, marker.moveX, marker.moveY, marker.leftRightScale);
            LOGD("outer resize : %d, scale : %d, moveX : %f, moveY : %f, leftRightScale : %d", outer.resize, outer.scale, outer.moveX, outer.moveY, outer.leftRightScale);

            return backPerson.commonTune[mCurrentBright-1].waterBlur;
        }
        else if( mCurrentStatus <= SF_FRONT_FACE_MOVIE )
        {
            if( mCurrentStatus == SF_FRONT_FACE_SAVE ) {
                marker = frontPerson.waterMarkerTuneForSave[mCurrentBright - 1];
                outer = frontPerson.waterOuterTuneForSave[mCurrentBright - 1];
            }
            else
            {
                marker = frontPerson.waterMarkerTune[mCurrentBright - 1];
                outer = frontPerson.waterOuterTune[mCurrentBright - 1];
            }

            LOGD("marker resize : %d, scale : %d, moveX : %f, moveY : %f, leftRightScale : %d", marker.resize, marker.scale, marker.moveX, marker.moveY, marker.leftRightScale);
            LOGD("outer resize : %d, scale : %d, moveX : %f, moveY : %f, leftRightScale : %d", outer.resize, outer.scale, outer.moveX, outer.moveY, outer.leftRightScale);

            return frontPerson.commonTune[mCurrentBright-1].waterBlur;
        }
    }

    void tuneManagerPersonSf::getCompensationTune(SF_FEATHER_TUNES &superFastAl, SF_FEATHER_TUNES &fastAl, SF_FEATHER_TUNES &slowAl)
    {
        if( mCurrentStatus <= SF_BACK_OBJ_MOVIE )
        {
            superFastAl = backPerson.superFastTune[mCurrentBright];
            fastAl = backObj.fastTune[mCurrentBright];
            slowAl = backObj.slowTune[mCurrentBright];
        }
        else if( mCurrentStatus <= SF_BACK_FACE_MOVIE )
        {
            if( mCurrentStatus == SF_BACK_FACE_SAVE ) {
                superFastAl = backPerson.saveTune[mCurrentBright - 1];
                fastAl = backPerson.saveTune[mCurrentBright - 1];
                slowAl = backPerson.saveTune[mCurrentBright - 1];
            }
            else
            {
                superFastAl = backPerson.superFastTune[mCurrentBright - 1];
                fastAl = backPerson.superFastTune[mCurrentBright - 1];
                slowAl = backPerson.slowTune[mCurrentBright - 1];
            }
        }
        else if( mCurrentStatus <= SF_FRONT_FACE_MOVIE ) {
            if (mCurrentStatus == SF_FRONT_FACE_SAVE) {
                superFastAl = frontPerson.saveTune[mCurrentBright - 1];
                fastAl = frontPerson.saveTune[mCurrentBright - 1];
                slowAl = frontPerson.saveTune[mCurrentBright - 1];
            }
            else
            {
                superFastAl = frontPerson.superFastTune[mCurrentBright - 1];
                fastAl = frontPerson.fastTune[mCurrentBright - 1];
                slowAl = frontPerson.slowTune[mCurrentBright - 1];
            }
        }
    }

    void tuneManagerPersonSf::getBlurTuneData(int &count, int &blur1, int &blur2, int &blur3)
    {
        if( mCurrentStatus == SF_BACK_FACE )
        {
            count = backPerson.commonTune[mCurrentBright-1].blurCount;
            blur1 = backPerson.commonTune[mCurrentBright-1].previewBlur1;
            blur2 = backPerson.commonTune[mCurrentBright-1].previewBlur2;
            blur3 = backPerson.commonTune[mCurrentBright-1].previewBlur3;
        }
        else if( mCurrentStatus == SF_BACK_FACE_SAVE )
        {
            count = backPerson.commonTune[mCurrentBright-1].blurCount;
            blur1 = backPerson.commonTune[mCurrentBright-1].captureBlur1;
            blur2 = backPerson.commonTune[mCurrentBright-1].captureBlur2;
            blur3 = backPerson.commonTune[mCurrentBright-1].captureBlur3;
        }
        else if( mCurrentStatus == SF_BACK_FACE_MOVIE )
        {
            count = backPerson.commonTune[mCurrentBright-1].blurCount;
            blur1 = backPerson.commonTune[mCurrentBright-1].videoBlur1;
            blur2 = backPerson.commonTune[mCurrentBright-1].videoBlur2;
            blur3 = backPerson.commonTune[mCurrentBright-1].videoBlur3;
        }
        else if( mCurrentStatus == SF_FRONT_FACE )
        {
            count = frontPerson.commonTune[mCurrentBright-1].blurCount;
            blur1 = frontPerson.commonTune[mCurrentBright-1].previewBlur1;
            blur2 = frontPerson.commonTune[mCurrentBright-1].previewBlur2;
            blur3 = frontPerson.commonTune[mCurrentBright-1].previewBlur3;
        }
        else if( mCurrentStatus == SF_FRONT_FACE_SAVE )
        {
            count = frontPerson.commonTune[mCurrentBright-1].blurCount;
            blur1 = frontPerson.commonTune[mCurrentBright-1].captureBlur1;
            blur2 = frontPerson.commonTune[mCurrentBright-1].captureBlur2;
            blur3 = frontPerson.commonTune[mCurrentBright-1].captureBlur3;
        }
        else if( mCurrentStatus == SF_FRONT_FACE_MOVIE )
        {
            count = frontPerson.commonTune[mCurrentBright-1].blurCount;
            blur1 = frontPerson.commonTune[mCurrentBright-1].videoBlur1;
            blur2 = frontPerson.commonTune[mCurrentBright-1].videoBlur2;
            blur3 = frontPerson.commonTune[mCurrentBright-1].videoBlur3;
        }
    }
}