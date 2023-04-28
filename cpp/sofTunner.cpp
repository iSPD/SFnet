//#include "stdafx.h"
#include "sofTunner.h"

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
    tuneManagerBlur::tuneManagerBlur()
    {
        //obj
        for(int i = 0; i < 3; i++)
        {
			BACK_OBJECT_TUNEDATA *temp = NULL;
            if(i == 0 )
            {
                temp = &mObjTuneData;
            }
            else if(i == 1 )
            {
                temp = &mObjSaveTuneData;
            }
            else if(i == 2 )
            {
                temp = &mObjMovieTuneData;
            }

            temp->touchBoundary = 4;
            temp->openClose[0] = 10;
            temp->openClose[1] = 10;

            for(int j = 0; j < 4; j++)
            {
                TUNEDATA_BRIGHTNESS *temp2 = &(temp->brightness[j]);

                temp2->brightness = 30;
                temp2->preBlur = 3;
                temp2->maskCount = 5;
                temp2->imageCompareTotal = 9;
                temp2->imageCompareCount = 4;
                temp2->markerThickness[0] = 0;
                temp2->markerThickness[1] = 11;
                temp2->markerThickness[2] = 11;
                temp2->markerThickness[3] = 11;
                temp2->outerCompenSize[0] = 1.0f;
                temp2->outerCompenSize[1] = 1.0f;
                temp2->outerCompenSize[2] = 1.0f;
                temp2->outerCompenSize[3] = 1.0f;
                temp2->blurCount[0] = 1;
                temp2->blurCount[1] = 2;
                temp2->blurCount[2] = 3;
                temp2->blurCount[3] = 4;

                temp2->blurSize1[0] = 5;
                temp2->blurSize1[1] = 5;
                temp2->blurSize1[2] = 5;
                temp2->blurSize1[3] = 5;

                temp2->blurSize2[0] = 5;
                temp2->blurSize2[1] = 5;
                temp2->blurSize2[2] = 5;
                temp2->blurSize2[3] = 5;

                temp2->blurSize3[0] = 5;
                temp2->blurSize3[1] = 5;
                temp2->blurSize3[2] = 5;
                temp2->blurSize3[3] = 5;

                temp2->faderThickness[0] = 10;
                temp2->faderThickness[1] = 10;
                temp2->faderThickness[2] = 10;
                temp2->faderThickness[3] = 10;
                temp2->movingValue = 50.f;
            }
        }

        //face
        for(int i = 0; i < 6; i++)
        {
			FACE_TUNEDATA *temp = NULL;
            if(i == 0 )
            {
                temp = &mBackFaceTuneData;
            }
            else if(i == 1 )
            {
                temp = &mBackFaceSaveTuneData;
            }
            else if(i == 2 )
            {
                temp = &mBackFaceMovieTuneData;
            }
            else if(i == 3 )
            {
                temp = &mFrontFaceTuneData;
            }
            else if(i == 4 )
            {
                temp = &mFrontFaceSaveTuneData;
            }
            else if(i == 5 )
            {
                temp = &mFrontFaceMovieTuneData;
            }

            temp->touchBoundary = 4;
            temp->openClose[0] = 10;
            temp->openClose[1] = 10;
            temp->faceSizeCompMinMax[0] = 30.0f;
            temp->faceSizeCompMinMax[1] = 1.f;
            temp->faceSizeCompMinMax[2] = 80.f;
            temp->faceSizeCompMinMax[3] = 1.1f;
            temp->faceBodyXyzCompMinMax[0] = 20.f;
            temp->faceBodyXyzCompMinMax[1] = 0.1f;
            temp->faceBodyXyzCompMinMax[2] = 70.0f;
            temp->faceBodyXyzCompMinMax[3] = 1.0f;

            for(int i = 0; i < 8; i++)
            {
                temp->faceXYZx2[i] = 1.0f;
            }
            temp->faceSizeMinMax[0] = 7.0f;
            temp->faceSizeMinMax[1] = 1.1f;
            temp->faceSizeMinMax[2] = 70.0f;
            temp->faceSizeMinMax[3] = 0.77f;
            for(int i = 0; i < 8; i++)
            {
                temp->bodyXYZx2[i] = 1.0f;
            }
            temp->bodySizeMinMax[0] = 7.0f;
            temp->bodySizeMinMax[1] = 1.4f;
            temp->bodySizeMinMax[2] = 40.f;
            temp->bodySizeMinMax[3] = 0.8f;

            for(int j = 0; j < 3; j++)
            {
                TUNEDATA_BRIGHTNESS *temp2 = &(temp->brightness[j]);

                temp2->brightness = 30;
                temp2->preBlur = 3;
                temp2->maskCount = 5;
                temp2->imageCompareTotal = 3;
                temp2->imageCompareCount = 3;
                temp2->markerThickness[0] = 0;
                temp2->markerThickness[1] = 11;
                temp2->markerThickness[2] = 11;
                temp2->markerThickness[3] = 11;
                temp2->outerCompenSize[0] = 1.0f;
                temp2->outerCompenSize[1] = 1.0f;
                temp2->outerCompenSize[2] = 1.0f;
                temp2->outerCompenSize[3] = 1.0f;
                temp2->blurCount[0] = 1;
                temp2->blurCount[1] = 2;
                temp2->blurCount[2] = 3;
                temp2->blurCount[3] = 4;

                temp2->blurSize1[0] = 5;
                temp2->blurSize1[1] = 5;
                temp2->blurSize1[2] = 5;
                temp2->blurSize1[3] = 5;

                temp2->blurSize2[0] = 5;
                temp2->blurSize2[1] = 5;
                temp2->blurSize2[2] = 5;
                temp2->blurSize2[3] = 5;

                temp2->blurSize3[0] = 5;
                temp2->blurSize3[1] = 5;
                temp2->blurSize3[2] = 5;
                temp2->blurSize3[3] = 5;

                temp2->faderThickness[0] = 10;
                temp2->faderThickness[1] = 10;
                temp2->faderThickness[2] = 10;
                temp2->faderThickness[3] = 10;
                temp2->movingValue = 50.f;
            }
        }

        STUDIO_TUNEDATA *studioTemp;
        for(int i = 0; i < 3; i++)
        {
            if( i == 0)
            {
                studioTemp = &mObjStudioData;
            }
            else if( i == 1)
            {
                studioTemp = &mBackFaceStudioData;
            }
            else if( i == 2)
            {
                studioTemp = &mFrontFaceStudioData;
            }

            studioTemp->faderStart = 0;
            studioTemp->faderCount = 50;
            studioTemp->faderBright = 1.1f;
            studioTemp->faderSaturation = 1.1f;
            studioTemp->inBright = 0.9f;
            studioTemp->outBright = 1.1f;
            studioTemp->satRate = 0.2f;
            studioTemp->circleRateMono = 0.8f;
            studioTemp->blackRateMono = 1.0f;
            studioTemp->contrast = 115.f;
            studioTemp->blackTuneRateMono = 20.f;
            //Black Mode
            studioTemp->circleRateBlack = 0.8f;
            studioTemp->blackRateBlack = 1.0f;
            studioTemp->blackTuneRateBlack = 20.f;
            //Dark Mode
            studioTemp->circleRateDark = 0.8f;
            studioTemp->blackRateDark = 1.0f;
            studioTemp->blackTuneRateDark = 20.f;
        }

        FACE_ROTATE_TUNE *temp;
        for(int i = 0; i < 2; i++)
        {
            if( i== 0 ) temp = &mBackFaceRotData;
            else if( i == 1 ) temp = &mFrontFaceRotData;

            temp->oriX = 0.0f;
            temp->oriY = 0.0f;
            temp->leftX = 0.0f;
            temp->leftY = 0.0f;
            temp->rightX = 0.0f;
            temp->rightY = 0.0f;
            temp->downUpX = 0.0f;
            temp->downUpY = 0.0f;
        }

        mObjMovingSensitivity = 50;
        mBackFaceMovingSensitivity = 15;
        mFrontFaceMovingSensitivity = 50;
        mBackFaceXYZCount = 3;

        //start
        //AI Tune Data
        mAITuneData.aIThreshod = 20;
        mAITuneData.aIMoving = 5.5f;
        mAITuneData.aISizePlus = 5;
        mAITuneData.aIMin = 10;
        mAITuneData.aIMax = 70;
        mAITuneData.personMin = 20;
        mAITuneData.personMax = 120;
        mAITuneData.aIScreen = 7;
        mAITuneData.aISizeBlurMin = 60;
        mAITuneData.aISizeBlurMax = 60;
        mAITuneData.a0Time = 1900;
        mAITuneData.a1Time = 700;

        mAITuneData.aIProcessCount = 3;
        mAITuneData.aIBelowPercent = 20;
        mAITuneData.aIUpPercent = 20;
        mAITuneData.aILeftPercent = 20;
        mAITuneData.aIRightPercent = 20;
        mAITuneData.alWaitCount = 4;
        mAITuneData.touchTime = 1500;
        mAITuneData.touchBoxSize = 80;

        mAITuneData.aIMultiUpRate = 1.2f;
        mAITuneData.aIMultiDownRate = 1.2f;
        mAITuneData.aIMultiSmallRate = 0.8f;
        mAITuneData.aIMultiBigRate = 1.3f;

        mAITuneData.aICornerX = 5;
        mAITuneData.aICornerY = 5;

        mAITuneData.aIMarker[0] = 1.0f;
        mAITuneData.aIMarker[1] = 1.2f;
        mAITuneData.aIOuter[0] = 1.1f;
        mAITuneData.aIOuter[1] = 1.3f;

        mAITuneData.touchMarker[0] = 1.0f;
        mAITuneData.touchMarker[1] = 1.2f;
        mAITuneData.touchOuter[0] = 1.1f;
        mAITuneData.touchOuter[1] = 1.3f;

        mAITuneData.touchMarkerSmall[0] = 1.0f;
        mAITuneData.touchMarkerSmall[1] = 1.2f;
        mAITuneData.touchOuterSmall[0] = 1.1f;
        mAITuneData.touchOuterSmall[1] = 1.3f;

        FACE_AI_TUNEDATA *temp2;
        for( int i = 0; i < 2; i++ )
        {
            if( i== 0 ) temp2 = &mBackFaceAITuneData;
            else if( i == 1 ) temp2 = &mFrontFaceAITuneData;

            temp2->handToSkin = 20;
            temp2->handX = 0.1f;
            temp2->handY = 0.5f;
            temp2->faceBlueTime = 1500;
            temp2->handThreshold = 50;
            temp2->handMarker[0] = 0.3f;
            temp2->handMarker[1] = 1.0f;
            temp2->handOuter[0] = 1.1f;
            temp2->handOuter[1] = 1.1f;
        }

        mCurrentStatus = SOF_BACK_OBJ;//SOF_NEED_NONE;
        mCurrentBright = SOF_NORMAL;
        mStudioMode = -1;

        mBright = 70;
    }

    tuneManagerBlur::~tuneManagerBlur()
    {

    }

    void tuneManagerBlur::printAllData()
    {
		BACK_OBJECT_TUNEDATA *preview = &mObjTuneData;
		BACK_OBJECT_TUNEDATA *save = &mObjSaveTuneData;
		BACK_OBJECT_TUNEDATA *movie = &mObjMovieTuneData;

		BACK_OBJECT_TUNEDATA *temp = NULL;
		for (int i = 0; i < 3; i++)
		{
			if (i == 0) temp = preview;
			else if (i == 1) temp = save;
			else temp = movie;

			LOGD("[printAll] preview(0), save(1), movie(2) : %d\n", i);

			LOGD("[printAll] %f\n", temp->touchBoundary);
			LOGD("[printAll] %d %d\n", temp->openClose[0], temp->openClose[1]);

			for (int j = 0; j < 4; j++) {
				for (int i = 0; i < 4; i++) {
					LOGD("[printAll] %d %d %d %d %d %d %f %d %d %d %d %d %f\n",
						temp->brightness[j].brightness,
						temp->brightness[j].preBlur,
						temp->brightness[j].maskCount,
						temp->brightness[j].imageCompareTotal,
						temp->brightness[j].imageCompareCount,
						temp->brightness[j].markerThickness[i],
						temp->brightness[j].outerCompenSize[i],//common
						temp->brightness[j].blurCount[i],
						temp->brightness[j].blurSize1[i],
                        temp->brightness[j].blurSize2[i],
                        temp->brightness[j].blurSize3[i],
						temp->brightness[j].faderThickness[i],
                        temp->brightness[j].movingValue);
				}
			}
		}

        //moving
        LOGD("[printAll] moving-tune-obj %d", mObjMovingSensitivity);

        //studio
        LOGD("[printAll] %d %d %f %f %f %f %f %f %f %f %f", mObjStudioData.faderStart, mObjStudioData.faderCount, mObjStudioData.faderBright,
             mObjStudioData.faderSaturation, mObjStudioData.inBright, mObjStudioData.outBright, mObjStudioData.satRate, mObjStudioData.circleRateMono, mObjStudioData.blackRateMono, mObjStudioData.contrast, mObjStudioData.blackTuneRateMono);
        LOGD("[printAll] %f %f %f %f %f %f", mObjStudioData.circleRateBlack, mObjStudioData.blackRateBlack, mObjStudioData.blackTuneRateBlack, mObjStudioData.circleRateDark, mObjStudioData.blackRateDark, mObjStudioData.blackTuneRateDark);

		FACE_TUNEDATA *facePreview;
		FACE_TUNEDATA *faceSave;
		FACE_TUNEDATA *faceMovie;
		for (int k = 0; k < 2; k++) {
			if (k == 0) {
				facePreview = &mBackFaceTuneData;
				faceSave = &mBackFaceSaveTuneData;
				faceMovie = &mBackFaceMovieTuneData;
			}
			else {
				facePreview = &mFrontFaceTuneData;
				faceSave = &mFrontFaceSaveTuneData;
				faceMovie = &mFrontFaceMovieTuneData;
			}

			FACE_TUNEDATA *temp = NULL;
			for (int i = 0; i < 3; i++)
			{
				if (i == 0) temp = facePreview;
				else if (i == 1) temp = faceSave;
				else temp = faceMovie;

				LOGD("[printAll] face Back(0), Front(1) : %d, preview(0), save(1), movie(2) : %d\n", k, i);

				LOGD("[printAll] %f\n", temp->touchBoundary);

				LOGD("[printAll] %d %d\n",
					temp->openClose[0], temp->openClose[1]);

				LOGD("[printAll] %f %f %f %f\n",
					temp->faceSizeCompMinMax[0],
					temp->faceSizeCompMinMax[1],
					temp->faceSizeCompMinMax[2],
					temp->faceSizeCompMinMax[3]);//4

				LOGD("[printAll] %f %f %f %f\n",
					temp->faceBodyXyzCompMinMax[0],
					temp->faceBodyXyzCompMinMax[1],
					temp->faceBodyXyzCompMinMax[2],
					temp->faceBodyXyzCompMinMax[3]);//4

				LOGD("[printAll] %f %f %f %f %f %f %f %f\n",
					temp->faceXYZx2[0],
					temp->faceXYZx2[1], temp->faceXYZx2[2],
					temp->faceXYZx2[3],
					temp->faceXYZx2[4], temp->faceXYZx2[5],
					temp->faceXYZx2[6], temp->faceXYZx2[7]);//8

				LOGD("[printAll] %f %f %f %f\n",
					temp->faceSizeMinMax[0],
					temp->faceSizeMinMax[1], temp->faceSizeMinMax[2],
					temp->faceSizeMinMax[3]);//4

				LOGD("[printAll] %f %f %f %f %f %f %f %f\n",
					temp->bodyXYZx2[0],
					temp->bodyXYZx2[1], temp->bodyXYZx2[2],
					temp->bodyXYZx2[3],
					temp->bodyXYZx2[4], temp->bodyXYZx2[5],
					temp->bodyXYZx2[6], temp->bodyXYZx2[7]);//8

				LOGD("[printAll] %f %f %f %f\n",
					temp->bodySizeMinMax[0],
					temp->bodySizeMinMax[1], temp->bodySizeMinMax[2],
					temp->bodySizeMinMax[3]);//4	


				for (int j = 0; j < 3; j++) {
					for (int i = 0; i < 4; i++) {
						LOGD("[printAll] %d %d %d %d %d %d %f %d %d %d %d %d %f\n",
							temp->brightness[j].brightness,
							temp->brightness[j].preBlur,
							temp->brightness[j].maskCount,
							temp->brightness[j].imageCompareTotal,
							temp->brightness[j].imageCompareCount,
							temp->brightness[j].markerThickness[i],
							temp->brightness[j].outerCompenSize[i],//common
							temp->brightness[j].blurCount[i],
							temp->brightness[j].blurSize1[i],
                            temp->brightness[j].blurSize2[i],
                            temp->brightness[j].blurSize3[i],
							temp->brightness[j].faderThickness[i],
                            temp->brightness[j].movingValue);
					}
				}
			}

            //moving
            if(k == 0) {
                LOGD("[printAll] moving-tune-backFace %d %d", mBackFaceMovingSensitivity, mBackFaceXYZCount);
            } else{
                LOGD("[printAll] moving-tune-frontFace %d", mFrontFaceMovingSensitivity);
            }

            //studio
            STUDIO_TUNEDATA *studioFaceTemp;
            if(k == 0) {
                LOGD("[printAll] Back Face Studio");
                studioFaceTemp = &mBackFaceStudioData;
            }
            else {
                LOGD("[printAll] Front Face Studio");
                studioFaceTemp = &mFrontFaceStudioData;
            }
            LOGD("[printAll] %d %d %f %f %f %f %f %f %f %f %f", studioFaceTemp->faderStart, studioFaceTemp->faderCount, studioFaceTemp->faderBright,
                   studioFaceTemp->faderSaturation, studioFaceTemp->inBright, studioFaceTemp->outBright, studioFaceTemp->satRate, studioFaceTemp->circleRateMono, studioFaceTemp->blackRateMono, studioFaceTemp->contrast, studioFaceTemp->blackTuneRateMono);
            LOGD("[printAll] %f %f %f %f %f %f", studioFaceTemp->circleRateBlack, studioFaceTemp->blackRateBlack, studioFaceTemp->blackTuneRateBlack, studioFaceTemp->circleRateDark, studioFaceTemp->blackRateDark, studioFaceTemp->blackTuneRateDark);

            FACE_ROTATE_TUNE *faceRotTemp;
            if( k == 0 ) faceRotTemp = &mBackFaceRotData;
            else if( k == 1 ) faceRotTemp = &mFrontFaceRotData;

            LOGD("[printAll] %f %f %f %f %f %f %f %f", faceRotTemp->oriX, faceRotTemp->oriX, faceRotTemp->leftX, faceRotTemp->leftY, faceRotTemp->rightX, faceRotTemp->rightY, faceRotTemp->downUpX, faceRotTemp->downUpY);

            LOGD("[printAll] [Ai-Tuning] [Obj Ai] %d %f %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %f %f %f %f %d %d %f %f %f %f %f %f %f %f %f %f %f %f",
                mAITuneData.aIThreshod,
                mAITuneData.aIMoving,
                mAITuneData.aISizePlus,
                mAITuneData.aIMin,
                mAITuneData.aIMax,
                mAITuneData.personMin,
                mAITuneData.personMax,
                mAITuneData.aIScreen,
                mAITuneData.aISizeBlurMin,
                mAITuneData.aISizeBlurMax,
                mAITuneData.a0Time,
                mAITuneData.a1Time,

                mAITuneData.aIProcessCount,
                mAITuneData.aIBelowPercent,
                mAITuneData.aIUpPercent,
                mAITuneData.aILeftPercent,
                mAITuneData.aIRightPercent,
                mAITuneData.alWaitCount,
                mAITuneData.touchTime,
                mAITuneData.touchBoxSize,

                mAITuneData.aIMultiUpRate,
                mAITuneData.aIMultiDownRate,
                mAITuneData.aIMultiSmallRate,
                mAITuneData.aIMultiBigRate,

                mAITuneData.aICornerX,
                mAITuneData.aICornerY,

                mAITuneData.aIMarker[0],
                mAITuneData.aIMarker[1],
                mAITuneData.aIOuter[0],
                mAITuneData.aIOuter[1],

                mAITuneData.touchMarker[0],
                mAITuneData.touchMarker[1],
                mAITuneData.touchOuter[0],
                mAITuneData.touchOuter[1],

                 mAITuneData.touchMarkerSmall[0],
                 mAITuneData.touchMarkerSmall[1],
                 mAITuneData.touchOuterSmall[0],
                 mAITuneData.touchOuterSmall[1]);

            FACE_AI_TUNEDATA *faceAiTemp;
            for( int i = 0; i < 2; i++ )
            {
                if( i== 0 ) faceAiTemp = &mBackFaceAITuneData;
                else if( i == 1 ) faceAiTemp = &mFrontFaceAITuneData;

                LOGD("[printAll] [Ai-Tuning] Face Ai[%d] %d %f %f %d %d %f %f %f %f",
                i,
                faceAiTemp->handToSkin,
                faceAiTemp->handX,
                faceAiTemp->handY,
                faceAiTemp->faceBlueTime,
                faceAiTemp->handThreshold,
                faceAiTemp->handMarker[0],
                faceAiTemp->handMarker[1],
                faceAiTemp->handOuter[0],
                faceAiTemp->handOuter[1]);
            }
        }
    }

    void tuneManagerBlur::loadingTuneData()
    {
        //char *path = "E:\\Start\\sof\\paulTuning\\of\\TuningTool40.txt";
        //char *path = "/data/local/tmp/of/TuningTool40.txt";
        //char *path = "/data/local/tmp/of/TuningToolP.txt";
        char *path = "/sdcard/sfcam/of/TuningToolP.txt";
        FILE *fd = fopen(path, "rt");
        if(fd)
        {
			char textTemp[maxTextSize];
            char text[maxTextSize];

            BACK_OBJECT_TUNEDATA *preview = &mObjTuneData;
            BACK_OBJECT_TUNEDATA *save = &mObjSaveTuneData;
            BACK_OBJECT_TUNEDATA *movie = &mObjMovieTuneData;

            LOGD("[Tool-Test] Start\n");
            //fgets(text, maxTextSize, fd);
            for(int i = 0; i < 127; i++)
            {
                fgets(text, maxTextSize, fd);
            }
			
			LOGD("[Tool-Test] text size : %d\n", strlen(text));
			LOGD("[Tool-Test] text : %s\n", text);

			fgets(text, maxTextSize, fd);
			sscanf(text, "%s %f %f %f", textTemp, &preview->touchBoundary, &save->touchBoundary, &movie->touchBoundary);
            LOGD("[Tool-Test-touch] text0 : %f\n", preview->touchBoundary);
            LOGD("[Tool-Test-touch] text : %f\n", mObjTuneData.touchBoundary);
			fgets(text, maxTextSize, fd);
			sscanf(text, "%s %d %d %d %d %d %d", textTemp, &preview->openClose[0], &preview->openClose[1], &save->openClose[0], &save->openClose[1], &movie->openClose[0], &movie->openClose[1]);
			
            fgets(text, maxTextSize, fd);
            LOGD("[Tool-Test] maxTextSize2 : %s\n", text);
            for(int j = 0; j < 4; j++) {
                for (int i = 0; i < 4; i++) {
					fgets(text, maxTextSize, fd);
                    sscanf(text, "%s %d %d %d %d %d %d %f %d %d %d %d %d %f %d %d %d %d %d %d %d %d %f",
						   textTemp,
                           &preview->brightness[j].brightness,
                           &preview->brightness[j].preBlur,
                           &preview->brightness[j].maskCount,
                           &preview->brightness[j].imageCompareTotal,
                           &preview->brightness[j].imageCompareCount,
                           &preview->brightness[j].markerThickness[i],
                           &preview->brightness[j].outerCompenSize[i],//common
                           &preview->brightness[j].blurCount[i],
                           &preview->brightness[j].blurSize1[i],
                           &preview->brightness[j].blurSize2[i],
                           &preview->brightness[j].blurSize3[i],
                           &preview->brightness[j].faderThickness[i],
                           &preview->brightness[j].movingValue,
                           &save->brightness[j].blurCount[i],
                           &save->brightness[j].blurSize1[i],
                           &save->brightness[j].blurSize2[i],
                           &save->brightness[j].blurSize3[i],
                           &save->brightness[j].faderThickness[i],
                           &movie->brightness[j].blurCount[i],
                           &movie->brightness[j].blurSize1[i],
                           &movie->brightness[j].faderThickness[i],
                           &movie->brightness[j].movingValue);
                }

                int size = sizeof(int) * (1+1+1+1+1+4) + sizeof(float) * 4;

                memcpy(&save->brightness[j], &preview->brightness[j], size);
                memcpy(&movie->brightness[j], &preview->brightness[j], size);
                save->brightness[j].movingValue = preview->brightness[j].movingValue;
            }

            //moving
            fgets(text, maxTextSize, fd);//no data
            fgets(text, maxTextSize, fd);//no data
            fgets(text, maxTextSize, fd);
            sscanf(text, "%s %d", textTemp, &mObjMovingSensitivity);

            //studio
            fgets(text, maxTextSize, fd);//no data
            fgets(text, maxTextSize, fd);//no data
            fgets(text, maxTextSize, fd);
            sscanf(text, "%s %d %d %f %f %f %f %f %f %f %f %f", textTemp, &mObjStudioData.faderStart, &mObjStudioData.faderCount, &mObjStudioData.faderBright,
                   &mObjStudioData.faderSaturation, &mObjStudioData.inBright, &mObjStudioData.outBright, &mObjStudioData.satRate,
                   &mObjStudioData.circleRateMono, &mObjStudioData.blackRateMono, &mObjStudioData.contrast, &mObjStudioData.blackTuneRateMono);
            fgets(text, maxTextSize, fd);//no data
            fgets(text, maxTextSize, fd);
            sscanf(text, "%s %f %f %f", textTemp, &mObjStudioData.circleRateBlack, &mObjStudioData.blackRateBlack, &mObjStudioData.blackTuneRateBlack);
            fgets(text, maxTextSize, fd);//no data
            fgets(text, maxTextSize, fd);
            sscanf(text, "%s %f %f %f", textTemp, &mObjStudioData.circleRateDark, &mObjStudioData.blackRateDark, &mObjStudioData.blackTuneRateDark);

            //Obj AI
            fgets(text, maxTextSize, fd);//no data
            fgets(text, maxTextSize, fd);
            fgets(text, maxTextSize, fd);
            sscanf(text, "%d %f %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %f %f %f %f %d %d",
                   &mAITuneData.aIThreshod,
                   &mAITuneData.aIMoving,
                   &mAITuneData.aISizePlus,
                   &mAITuneData.aIMin,
                   &mAITuneData.aIMax,
                   &mAITuneData.personMin,
                   &mAITuneData.personMax,
                   &mAITuneData.aIScreen,
                   &mAITuneData.aISizeBlurMin,
                   &mAITuneData.aISizeBlurMax,
                   &mAITuneData.a0Time,
                   &mAITuneData.a1Time,
                   &mAITuneData.aIProcessCount,
                   &mAITuneData.aIBelowPercent,
                   &mAITuneData.aIUpPercent,
                   &mAITuneData.aILeftPercent,
                   &mAITuneData.aIRightPercent,
                   &mAITuneData.alWaitCount,
                   &mAITuneData.touchTime,
                   &mAITuneData.touchBoxSize,
                   &mAITuneData.aIMultiUpRate,
                   &mAITuneData.aIMultiDownRate,
                   &mAITuneData.aIMultiSmallRate,
                   &mAITuneData.aIMultiBigRate,
                   &mAITuneData.aICornerX,
                   &mAITuneData.aICornerY);

            fgets(text, maxTextSize, fd);
            fgets(text, maxTextSize, fd);
            sscanf(text, "%s %f %f %f %f %f %f",
                    textTemp,
                    &mAITuneData.aIMarker[0],
                    &mAITuneData.aIMarker[1],
                    &mAITuneData.touchMarker[0],
                    &mAITuneData.touchMarker[1],
                    &mAITuneData.touchMarkerSmall[0],
                    &mAITuneData.touchMarkerSmall[1]);
            fgets(text, maxTextSize, fd);
            sscanf(text, "%s %f %f %f %f %f %f",
                   textTemp,
                   &mAITuneData.aIOuter[0],
                   &mAITuneData.aIOuter[1],
                   &mAITuneData.touchOuter[0],
                   &mAITuneData.touchOuter[1],
                   &mAITuneData.touchOuterSmall[0],
                   &mAITuneData.touchOuterSmall[1]);

            FACE_TUNEDATA *facePreview;
            FACE_TUNEDATA *faceSave;
            FACE_TUNEDATA *faceMovie;
            for(int k = 0; k < 2; k++) {
                if(k == 0) {
                    facePreview = &mBackFaceTuneData;
                    faceSave = &mBackFaceSaveTuneData;
                    faceMovie = &mBackFaceMovieTuneData;
                }
                else {
                    facePreview = &mFrontFaceTuneData;
                    faceSave = &mFrontFaceSaveTuneData;
                    faceMovie = &mFrontFaceMovieTuneData;
                }

                fgets(text, maxTextSize, fd);
                LOGD("[Tool-Test] maxTextSize3-%d : %s\n", k, text);
                fgets(text, maxTextSize, fd);
                LOGD("[Tool-Test] maxTextSize4-%d : %s\n", k, text);

				fgets(text, maxTextSize, fd);
				sscanf(text, "%s %f %f %f", textTemp, &facePreview->touchBoundary, &faceSave->touchBoundary,
                       &faceMovie->touchBoundary);
				fgets(text, maxTextSize, fd);
                sscanf(text, "%s %d %d %d %d %d %d",
					   textTemp,
                       &facePreview->openClose[0], &facePreview->openClose[1],
                       &faceSave->openClose[0], &faceSave->openClose[1],
                       &faceMovie->openClose[0], &faceMovie->openClose[1]);

				fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f %f %f",
					   textTemp,
                       &facePreview->faceSizeCompMinMax[0],
                       &facePreview->faceSizeCompMinMax[1],
                       &facePreview->faceSizeCompMinMax[2],
                       &facePreview->faceSizeCompMinMax[3]);//4
				fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f %f %f",
						textTemp,
                       &facePreview->faceBodyXyzCompMinMax[0],
                       &facePreview->faceBodyXyzCompMinMax[1],
                       &facePreview->faceBodyXyzCompMinMax[2],
                       &facePreview->faceBodyXyzCompMinMax[3]);//4

                fgets(text, maxTextSize, fd);
                LOGD("[Tool-Test] maxTextSize5-%d : %s\n", k, text);

				fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f %f %f %f %f %f %f",
						textTemp,
                       &facePreview->faceXYZx2[0],
                       &facePreview->faceXYZx2[1], &facePreview->faceXYZx2[2],
                       &facePreview->faceXYZx2[3],
                       &facePreview->faceXYZx2[4], &facePreview->faceXYZx2[5],
                       &facePreview->faceXYZx2[6], &facePreview->faceXYZx2[7]);//8
				fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f %f %f",
						textTemp,
                       &facePreview->faceSizeMinMax[0],
                       &facePreview->faceSizeMinMax[1], &facePreview->faceSizeMinMax[2],
                       &facePreview->faceSizeMinMax[3]);//4
				fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f %f %f %f %f %f %f",
						textTemp,
                       &facePreview->bodyXYZx2[0],
                       &facePreview->bodyXYZx2[1], &facePreview->bodyXYZx2[2],
                       &facePreview->bodyXYZx2[3],
                       &facePreview->bodyXYZx2[4], &facePreview->bodyXYZx2[5],
                       &facePreview->bodyXYZx2[6], &facePreview->bodyXYZx2[7]);//8
				fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f %f %f",
						textTemp,
                       &facePreview->bodySizeMinMax[0],
                       &facePreview->bodySizeMinMax[1], &facePreview->bodySizeMinMax[2],
                       &facePreview->bodySizeMinMax[3]);//4

                int size = sizeof(float) * (4 + 4 + 8 + 4 + 8 + 4);
                memcpy(faceSave->faceSizeCompMinMax, facePreview->faceSizeCompMinMax, size);
                memcpy(faceMovie->faceSizeCompMinMax, facePreview->faceSizeCompMinMax, size);

                fgets(text, maxTextSize, fd);
                LOGD("[Tool-Test] maxTextSize6-%d : %s\n", k, text);
                for (int j = 0; j < 3; j++) {
                    for (int i = 0; i < 4; i++) {
						fgets(text, maxTextSize, fd);
                        sscanf(text, "%s %d %d %d %d %d %d %f %d %d %d %d %d %f %d %d %d %d %d %d %d %d %f",
								textTemp,
                               &facePreview->brightness[j].brightness,
                               &facePreview->brightness[j].preBlur,
                               &facePreview->brightness[j].maskCount,
                               &facePreview->brightness[j].imageCompareTotal,
                               &facePreview->brightness[j].imageCompareCount,
                               &facePreview->brightness[j].markerThickness[i],
                               &facePreview->brightness[j].outerCompenSize[i],//common
                               &facePreview->brightness[j].blurCount[i],
                               &facePreview->brightness[j].blurSize1[i],
                               &facePreview->brightness[j].blurSize2[i],
                               &facePreview->brightness[j].blurSize3[i],
                               &facePreview->brightness[j].faderThickness[i],
                               &facePreview->brightness[j].movingValue,
                               &faceSave->brightness[j].blurCount[i],
                               &faceSave->brightness[j].blurSize1[i],
                               &faceSave->brightness[j].blurSize2[i],
                               &faceSave->brightness[j].blurSize3[i],
                               &faceSave->brightness[j].faderThickness[i],
                               &faceMovie->brightness[j].blurCount[i],
                               &faceMovie->brightness[j].blurSize1[i],
                               &faceMovie->brightness[j].faderThickness[i],
                               &faceMovie->brightness[j].movingValue);
                    }
                    memcpy(&faceSave->brightness[j], &facePreview->brightness[j], 52);
                    memcpy(&faceMovie->brightness[j], &facePreview->brightness[j], 52);
                    faceSave->brightness[j].movingValue = facePreview->brightness[j].movingValue;
                }

                //moving
                fgets(text, maxTextSize, fd);//no data
                fgets(text, maxTextSize, fd);//no data
                fgets(text, maxTextSize, fd);
                if(k == 0) {
                    sscanf(text, "%s %d %d", textTemp, &mBackFaceMovingSensitivity, &mBackFaceXYZCount);
                } else{
                    sscanf(text, "%s %d", textTemp, &mFrontFaceMovingSensitivity);
                }

                //studio
                STUDIO_TUNEDATA *studioFaceTemp;
                if(k == 0) {
                    studioFaceTemp = &mBackFaceStudioData;
                }
                else {
                    studioFaceTemp = &mFrontFaceStudioData;
                }

                fgets(text, maxTextSize, fd);//no data
                fgets(text, maxTextSize, fd);//no data
                fgets(text, maxTextSize, fd);
                sscanf(text, "%s %d %d %f %f %f %f %f %f %f %f %f", textTemp, &studioFaceTemp->faderStart, &studioFaceTemp->faderCount, &studioFaceTemp->faderBright,
                       &studioFaceTemp->faderSaturation, &studioFaceTemp->inBright, &studioFaceTemp->outBright, &studioFaceTemp->satRate,
                       &studioFaceTemp->circleRateMono, &studioFaceTemp->blackRateMono, &studioFaceTemp->contrast, &studioFaceTemp->blackTuneRateMono);
                fgets(text, maxTextSize, fd);//no data
                fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f %f", textTemp, &studioFaceTemp->circleRateBlack, &studioFaceTemp->blackRateBlack, &studioFaceTemp->blackTuneRateBlack);
                fgets(text, maxTextSize, fd);//no data
                fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f %f", textTemp, &studioFaceTemp->circleRateDark, &studioFaceTemp->blackRateDark, &studioFaceTemp->blackTuneRateDark);

                //studio
                FACE_ROTATE_TUNE *faceRotTemp;
                if(k == 0) {
                    faceRotTemp = &mBackFaceRotData;
                }
                else {
                    faceRotTemp = &mFrontFaceRotData;
                }

                fgets(text, maxTextSize, fd);//no data
                fgets(text, maxTextSize, fd);//no data
                fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f", textTemp, &faceRotTemp->oriX, &faceRotTemp->oriY);
                fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f", textTemp, &faceRotTemp->leftX, &faceRotTemp->leftY);
                fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f", textTemp, &faceRotTemp->rightX, &faceRotTemp->rightY);
                fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f", textTemp, &faceRotTemp->downUpX, &faceRotTemp->downUpY);

                //face Ai
                FACE_AI_TUNEDATA *faceAiTemp;
                if(k == 0) {
                    faceAiTemp = &mBackFaceAITuneData;
                }
                else {
                    faceAiTemp = &mFrontFaceAITuneData;
                }

                fgets(text, maxTextSize, fd);//no data
                fgets(text, maxTextSize, fd);//no data
                fgets(text, maxTextSize, fd);
                sscanf(text, "%d %f %f %d %d", &faceAiTemp->handToSkin, &faceAiTemp->handX, &faceAiTemp->handY, &faceAiTemp->faceBlueTime,  &faceAiTemp->handThreshold);

                fgets(text, maxTextSize, fd);
                fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f", textTemp, &faceAiTemp->handMarker[0], &faceAiTemp->handMarker[1]);

                fgets(text, maxTextSize, fd);
                sscanf(text, "%s %f %f", textTemp, &faceAiTemp->handOuter[0], &faceAiTemp->handOuter[1]);
            }

            fclose(fd);

            LOGD("[Tool-Test-touch] text2 : %f\n", mObjTuneData.touchBoundary);
            printAllData();
        }
        else
        {
            LOGD("%s : open fail\n", path);
        }


    }

    void tuneManagerBlur::getFaceData(float *faceXyz, float *faceSizeMax, float *faceSizeMin, float &faceSizeByY, int which)
    {

    }

    void tuneManagerBlur::getBodyData(float *bodyXyz, float *bodySizeMax, float *bodySizeMin, float &bodySizeByY, int which)
    {

    }

    void tuneManagerBlur::getCurrentStructure(BACK_OBJECT_TUNEDATA &obj, FACE_TUNEDATA &face)
    {
        if (mCurrentStatus == SOF_BACK_OBJ) {
            obj = mObjTuneData;
        } else if (mCurrentStatus == SOF_BACK_OBJ_SAVE) {
            obj = mObjSaveTuneData;
        } else if (mCurrentStatus == SOF_BACK_OBJ_MOVIE) {
            obj = mObjMovieTuneData;
        } else if (mCurrentStatus == SOF_BACK_FACE) {
            face = mBackFaceTuneData;
        } else if (mCurrentStatus == SOF_BACK_FACE_SAVE) {
            face = mBackFaceSaveTuneData;
        } else if (mCurrentStatus == SOF_BACK_FACE_MOVIE) {
            face = mBackFaceMovieTuneData;
        } else if (mCurrentStatus == SOF_FRONT_FACE) {
            face = mFrontFaceTuneData;
        } else if (mCurrentStatus == SOF_FRONT_FACE_SAVE) {
            face = mFrontFaceSaveTuneData;
        } else if (mCurrentStatus == SOF_FRONT_FACE_MOVIE) {
            face = mFrontFaceMovieTuneData;
        }
    }

    float tuneManagerBlur::getMovingValue()
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  )
        {
            return tempObj.brightness[mCurrentBright].movingValue;
        }
        else
        {
            return temp.brightness[mCurrentBright-1].movingValue;
        }

        return 50.f;
    }

//here to go...
    int tuneManagerBlur::getBlurData(int *blurCount, int *blurSize1, int *blurSize2, int *blurSize3)
    {
        int maskCount = 4;
        int maxBlurCount = 4;

        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus == SOF_BACK_OBJ_SAVE || mCurrentStatus == SOF_BACK_OBJ_MOVIE )
        {
            //tempObj = &mObjSaveTuneData;
            tempObj = mObjTuneData;
        }
        else if( mCurrentStatus == SOF_BACK_FACE_SAVE || mCurrentStatus == SOF_BACK_FACE_MOVIE )
        {
            //temp = &mBackFaceSaveTuneData;
            temp = mBackFaceTuneData;
        }
        else if( mCurrentStatus == SOF_FRONT_FACE_SAVE || mCurrentStatus == SOF_FRONT_FACE_MOVIE )
        {
            //temp = &mFrontFaceSaveTuneData;
            temp = mFrontFaceTuneData;
        }

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  ) {

            for (int i = 0; i < 4; i++) {
                blurCount[i] = tempObj.brightness[mCurrentBright].blurCount[i];
                blurSize1[i] = tempObj.brightness[mCurrentBright].blurSize1[i];
                blurSize2[i] = tempObj.brightness[mCurrentBright].blurSize2[i];
                blurSize3[i] = tempObj.brightness[mCurrentBright].blurSize3[i];
            }
            maskCount = tempObj.brightness[mCurrentBright].maskCount;
            maxBlurCount = tempObj.brightness[mCurrentBright].blurCount[maskCount - 1];

            LOGD("[blurTest-Obj] maskCount : %d, maxBlurCount : %d", maskCount, maxBlurCount);
        }
        else
        {
            for (int i = 0; i < 4; i++) {
                blurCount[i] = temp.brightness[mCurrentBright-1].blurCount[i];
                blurSize1[i] = temp.brightness[mCurrentBright-1].blurSize1[i];
                blurSize2[i] = temp.brightness[mCurrentBright-1].blurSize2[i];
                blurSize3[i] = temp.brightness[mCurrentBright-1].blurSize3[i];
            }
            maskCount = temp.brightness[mCurrentBright-1].maskCount;
            maxBlurCount = temp.brightness[mCurrentBright-1].blurCount[maskCount - 1];

            LOGD("[blurTest-Face] maskCount : %d, maxBlurCount : %d", maskCount, maxBlurCount);
        }

        LOGD("[blurTest-Face] maskCount : %d, maxBlurCount : %d", maskCount, maxBlurCount);

        return maxBlurCount;
    }

    int tuneManagerBlur::getBlurDataSave(int *blurCount, int *blurSize1, int *blurSize2, int *blurSize3)
    {
        int maskCount = 4;
        int maxBlurCount = 4;

        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  ) {

            for (int i = 0; i < 4; i++) {
                blurCount[i] = tempObj.brightness[mCurrentBright].blurCount[i];
                blurSize1[i] = tempObj.brightness[mCurrentBright].blurSize1[i];
                blurSize2[i] = tempObj.brightness[mCurrentBright].blurSize2[i];
                blurSize3[i] = tempObj.brightness[mCurrentBright].blurSize3[i];
            }
            maskCount = tempObj.brightness[mCurrentBright].maskCount;
            maxBlurCount = tempObj.brightness[mCurrentBright].blurCount[maskCount - 1];

            LOGD("[blurTest-Obj] maskCount : %d, maxBlurCount : %d", maskCount, maxBlurCount);

        }
        else
        {
            for (int i = 0; i < 4; i++) {
                blurCount[i] = temp.brightness[mCurrentBright-1].blurCount[i];
                blurSize1[i] = temp.brightness[mCurrentBright-1].blurSize1[i];
                blurSize2[i] = temp.brightness[mCurrentBright-1].blurSize2[i];
                blurSize3[i] = temp.brightness[mCurrentBright-1].blurSize3[i];
            }
            maskCount = temp.brightness[mCurrentBright-1].maskCount;
            maxBlurCount = temp.brightness[mCurrentBright-1].blurCount[maskCount - 1];

            LOGD("[blurTest-Face] maskCount : %d, maxBlurCount : %d", maskCount, maxBlurCount);
        }

        LOGD("[blurTest-Face] maskCount : %d, maxBlurCount : %d", maskCount, maxBlurCount);

        return maxBlurCount;
    }

    int tuneManagerBlur::getPreBlur()
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  )
        {
            return tempObj.brightness[mCurrentBright].preBlur;
        }
        else
        {
            return temp.brightness[mCurrentBright-1].preBlur;
        }

        return 1;
    }

    float tuneManagerBlur::getTouchBoundary()
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus == SOF_BACK_OBJ_SAVE )
        {
            //tempObj = &mObjSaveTuneData;
            tempObj = mObjTuneData;
        }
        else if( mCurrentStatus == SOF_BACK_FACE_SAVE )
        {
            //temp = &mBackFaceSaveTuneData;
            temp = mBackFaceTuneData;
        }
        else if( mCurrentStatus == SOF_FRONT_FACE_SAVE )
        {
            //temp = &mFrontFaceSaveTuneData;
            temp = mFrontFaceTuneData;
        }

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  )
        {
            LOGD("mCurrentStatus-1 : %d", mCurrentStatus);
            return tempObj.touchBoundary;
        }
        else
        {
            LOGD("mCurrentStatus-2 : %d", mCurrentStatus);
            return temp.touchBoundary;
        }

        LOGD("mCurrentStatus-3 : %d", mCurrentStatus);
        return 1.0f;
    }

    float tuneManagerBlur::getTouchBoundarySave()
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  )
        {
            LOGD("mCurrentStatus-1 : %d", mCurrentStatus);
            return tempObj.touchBoundary;
        }
        else
        {
            LOGD("mCurrentStatus-2 : %d", mCurrentStatus);
            return temp.touchBoundary;
        }

        LOGD("mCurrentStatus-3 : %d", mCurrentStatus);
        return 1.0f;
    }

    int tuneManagerBlur::getMaskCount()
    {
		BACK_OBJECT_TUNEDATA tempObj;
		FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  )
        {
            return tempObj.brightness[mCurrentBright].maskCount;
        }
        else
        {
            return temp.brightness[mCurrentBright-1].maskCount;
        }

        return 4;
    }

    void tuneManagerBlur::getOuterSize(float *data)
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  ) {
            for (int i = 0; i < 4; i++) {
                data[i] = tempObj.brightness[mCurrentBright].outerCompenSize[i];
            }
        }
        else
        {
            for (int i = 0; i < 4; i++) {
                data[i] = temp.brightness[mCurrentBright-1].outerCompenSize[i];
            }
        }
    }

    void tuneManagerBlur::getFaderCount(int *data)
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        LOGD("[Tool-Test] mCurrentStatus : %d\n", mCurrentStatus);
        LOGD("[Tool-Test] mCurrentBright : %d\n", mCurrentBright);

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  ) {
            for (int i = 0; i < 4; i++) {
                LOGD("[Tool-Test] tempObj->brightness[mCurrentBright].faderThickness[%d] : %d\n", i,
                     tempObj.brightness[mCurrentBright].faderThickness[i]);
            }

            for (int i = 0; i < 4; i++) {
                data[i] = tempObj.brightness[mCurrentBright].faderThickness[i];
            }
        }
        else {
            for (int i = 0; i < 4; i++) {
                LOGD("[Tool-Test] tempObj->brightness[mCurrentBright].faderThickness[%d] : %d\n", i,
                     temp.brightness[mCurrentBright-1].faderThickness[i]);
            }

            for (int i = 0; i < 4; i++) {
                data[i] = temp.brightness[mCurrentBright-1].faderThickness[i];
            }
        }

        for(int i = 0; i < 4; i++) {
            LOGD("[Tool-Test] data[%d] : %d\n", i, data[i]);
        }
    }

    void tuneManagerBlur::getMarkerSize(int *data)
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  )
        {
            for (int i = 1; i < 4; i++) {
                data[i - 1] = tempObj.brightness[mCurrentBright].markerThickness[i];
            }

        }
        else
        {
            for (int i = 1; i < 4; i++) {
                data[i - 1] = temp.brightness[mCurrentBright-1].markerThickness[i];
            }
        }
    }

    int tuneManagerBlur::getFaderStart()
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  )
        {
            return tempObj.brightness[mCurrentBright].markerThickness[0];

        }
        else
        {
            return temp.brightness[mCurrentBright-1].markerThickness[0];
        }
        return 10;
    }

    int tuneManagerBlur::getImageCompareTotal()
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  )
        {
            return tempObj.brightness[mCurrentBright].imageCompareTotal;

        }
        else
        {
            return temp.brightness[mCurrentBright-1].imageCompareTotal;
        }

        return 5;
    }

    int tuneManagerBlur::getImageCompareCount()
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  )
        {
            return tempObj.brightness[mCurrentBright].imageCompareCount;
        }
        else
        {
            return temp.brightness[mCurrentBright-1].imageCompareCount;
        }

        return 5;
    }

    void tuneManagerBlur::getOpenClose(int &open, int &close)
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        open = 1;
        close = 1;

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  )
        {
            open = tempObj.openClose[0];
            close = tempObj.openClose[1];
        }
        else
        {
            //open = temp.openClose[0];
            //close = temp.openClose[1];
            open = 0;
            close = 0;
        }

        LOGD("getOpenClose %d %d", open, close);
    }

    void tuneManagerBlur::getFaceMinMaxSize(float &minSize, float &maxSize, float &minRate, float &maxRate)
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        minSize = temp.faceSizeMinMax[0];
        minRate = temp.faceSizeMinMax[1];
        maxSize = temp.faceSizeMinMax[2];
        maxRate = temp.faceSizeMinMax[3];

        //minSize = 100.0f;
        //minRate = 1.0f;
        //maxSize = 100.0f;
        //maxRate = 1.0f;

        LOGD("minSize : %f, maxSize : &f, minRate : %f, maxRate : &f\n", minSize, maxSize, minRate, maxRate);
    }

    void tuneManagerBlur::getBodyMinMaxSize(float &minSize, float &maxSize, float &minRate, float &maxRate)
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        minSize = temp.bodySizeMinMax[0];
        minRate = temp.bodySizeMinMax[1];
        maxSize = temp.bodySizeMinMax[2];
        maxRate = temp.bodySizeMinMax[3];

        //minSize = 100.0f;
        //minRate = 1.0f;
        //maxSize = 100.0f;
        //maxRate = 1.0f;

        LOGD("minSize : %f, maxSize : &f, minRate : %f, maxRate : &f\n", minSize, maxSize, minRate, maxRate);
    }

void tuneManagerBlur::getFaceCompSize(float &minSize, float &maxSize, float &minRate, float &maxRate) {

    BACK_OBJECT_TUNEDATA tempObj;
    FACE_TUNEDATA temp;

    getCurrentStructure(tempObj, temp);

    LOGD("[getFaceCompSize] mCurrentStatus : %d", mCurrentStatus);

    minSize = temp.faceSizeCompMinMax[0];
    minRate = temp.faceSizeCompMinMax[1];
    maxSize = temp.faceSizeCompMinMax[2];
    maxRate = temp.faceSizeCompMinMax[3];

    //minSize = 100.0f;
    //minRate = 1.0f;
    //maxSize = 100.0f;
    //maxRate = 1.0f;

    LOGD("[getFaceCompSize] minSize : %f, maxSize : %f, minRate : %f, maxRate : %f\n", minSize, maxSize, minRate, maxRate);
}

void tuneManagerBlur::getXyzCompSize(float &minSize, float &maxSize, float &minRate, float &maxRate)
{
    BACK_OBJECT_TUNEDATA tempObj;
    FACE_TUNEDATA temp;

    getCurrentStructure(tempObj, temp);

    minSize = temp.faceBodyXyzCompMinMax[0];
    minRate = temp.faceBodyXyzCompMinMax[1];
    maxSize = temp.faceBodyXyzCompMinMax[2];
    maxRate = temp.faceBodyXyzCompMinMax[3];

    //minSize = 100.0f;
    //minRate = 1.0f;
    //maxSize = 100.0f;
    //maxRate = 1.0f;

    LOGD("minSize : %f, maxSize : &f, minRate : %f, maxRate : &f\n", minSize, maxSize, minRate, maxRate);
}

    void tuneManagerBlur::getFaceXYZData(float &xValueL, float &xValueR, float &yValueL, float &yValueR, float &zValueL, float &zValueR)
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        xValueL = temp.faceXYZx2[0];
        xValueR = temp.faceXYZx2[1];
        yValueL = temp.faceXYZx2[2];
        yValueR = temp.faceXYZx2[4];
        zValueL = temp.faceXYZx2[6];
        zValueR = temp.faceXYZx2[7];

        //xValueL = 1.0f;
        //xValueR = 1.0f;
        //yValueL = 1.0f;
        //yValueR = 1.0f;
        //zValueL = 1.0f;
        //zValueR = 1.0f;

        LOGD("FaceXYZ xValue : %f, yValue : &f, zValue : %f\n", xValueL, yValueL, zValueL);
    }

    void tuneManagerBlur::getBodyXYZData(float &xValueL, float &xValueR, float &yValueL, float &yValueR, float &zValueL, float &zValueR)
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        xValueL = temp.bodyXYZx2[0];
        xValueR = temp.bodyXYZx2[1];
        yValueL = temp.bodyXYZx2[2];
        yValueR = temp.bodyXYZx2[4];
        zValueL = temp.bodyXYZx2[6];
        zValueR = temp.bodyXYZx2[7];

        ///xValueL = 1.0f;
        //xValueR = 1.0f;
        //yValueL = 1.0f;
        //yValueR = 1.0f;
        //zValueL = 1.0f;
        //zValueR = 1.0f;

        LOGD("BodyXYZ xValue : %f, yValue : &f, zValue : %f\n", xValueL, yValueL, zValueL);
    }

    void tuneManagerBlur::getFaceSizeByY(float &sizeValueL, float &sizeValueR)
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        sizeValueL = temp.faceXYZx2[3];
        sizeValueR = temp.faceXYZx2[5];

        //sizeValueL = 1.0f;
        //sizeValueR = 1.0f;

        LOGD("getFaceSizeByY Value: %f %f\n", sizeValueL, sizeValueR);
    }

    void tuneManagerBlur::getBodySizeByY(float &sizeValueL, float &sizeValueR)
    {
        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        sizeValueL = temp.bodyXYZx2[3];
        sizeValueR = temp.bodyXYZx2[5];

        //sizeValueL = 1.0f;
        //sizeValueR = 1.0f;

        LOGD("getFaceSizeByY Value: %f %f\n", sizeValueL, sizeValueR);
    }

    void tuneManagerBlur::getRotTune(float &x, float &y, int rotate)
    {
        x = 0.0f;
        y = 0.0f;

        if( mCurrentStatus <= SOF_BACK_FACE_MOVIE  ) {
            if( rotate == 0 ) {
                x = mBackFaceRotData.oriX;
                y = mBackFaceRotData.oriY;
            }
            else if( rotate == 90 ) {
                x = mBackFaceRotData.rightX;
                y = mBackFaceRotData.rightY;
            }
            else if( rotate == 180 ) {
                x = mBackFaceRotData.downUpX;
                y = mBackFaceRotData.downUpY;
            }
            else if( rotate == 270 ) {
                x = mBackFaceRotData.leftX;
                y = mBackFaceRotData.leftY;
            }
        }
        else if( mCurrentStatus <= SOF_FRONT_FACE_MOVIE  ) {
            if( rotate == 0 ) {
                x = mFrontFaceRotData.oriX;
                y = mFrontFaceRotData.oriY;
            }
            else if( rotate == 90 ) {
                x = mFrontFaceRotData.rightX;
                y = mFrontFaceRotData.rightY;
            }
            else if( rotate == 180 ) {
                x = mFrontFaceRotData.downUpX;
                y = mFrontFaceRotData.downUpY;
            }
            else if( rotate == 270 ) {
                x = mFrontFaceRotData.leftX;
                y = mFrontFaceRotData.leftY;
            }
        }
    }

/*
    int tuneManagerBlur::getFrontStatus()
    {

    }
*/
    void tuneManagerBlur::getStudioFaderData(int &faderStart, int &faderCount)
    {
        faderStart = 0;
        faderCount = 50;

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  ) {
            faderStart = mObjStudioData.faderStart;
            faderCount = mObjStudioData.faderCount;
        }
        else if( mCurrentStatus <= SOF_BACK_FACE_MOVIE  ) {
            faderStart = mBackFaceStudioData.faderStart;
            faderCount = mBackFaceStudioData.faderCount;
        }
        else if( mCurrentStatus <= SOF_FRONT_FACE_MOVIE  ) {
            faderStart = mFrontFaceStudioData.faderStart;
            faderCount = mFrontFaceStudioData.faderCount;
        }
    }

    void tuneManagerBlur::getStudioDarkData(float &circleRate, float &blackBright, float &blackTuneRate)
    {
        circleRate = 1.0f;
        blackBright = 1.0f;

        if( mStudioMode == 0 ) {
            if (mCurrentStatus <= SOF_BACK_OBJ_MOVIE) {
                circleRate = mObjStudioData.circleRateMono;
                blackBright = mObjStudioData.blackRateMono;
                blackTuneRate = mObjStudioData.blackTuneRateMono;
            } else if (mCurrentStatus <= SOF_BACK_FACE_MOVIE) {
                circleRate = mBackFaceStudioData.circleRateMono;
                blackBright = mBackFaceStudioData.blackRateMono;
                blackTuneRate = mBackFaceStudioData.blackTuneRateMono;
            } else if (mCurrentStatus <= SOF_FRONT_FACE_MOVIE) {
                circleRate = mFrontFaceStudioData.circleRateMono;
                blackBright = mFrontFaceStudioData.blackRateMono;
                blackTuneRate = mFrontFaceStudioData.blackTuneRateMono;
            }
        }
        else if( mStudioMode == 1 ) {
            if (mCurrentStatus <= SOF_BACK_OBJ_MOVIE) {
                circleRate = mObjStudioData.circleRateBlack;
                blackBright = mObjStudioData.blackRateBlack;
                blackTuneRate = mObjStudioData.blackTuneRateBlack;
            } else if (mCurrentStatus <= SOF_BACK_FACE_MOVIE) {
                circleRate = mBackFaceStudioData.circleRateBlack;
                blackBright = mBackFaceStudioData.blackRateBlack;
                blackTuneRate = mBackFaceStudioData.blackTuneRateBlack;
            } else if (mCurrentStatus <= SOF_FRONT_FACE_MOVIE) {
                circleRate = mFrontFaceStudioData.circleRateBlack;
                blackBright = mFrontFaceStudioData.blackRateBlack;
                blackTuneRate = mFrontFaceStudioData.blackTuneRateBlack;
            }
        }
        else if( mStudioMode == 2 ) {
            if (mCurrentStatus <= SOF_BACK_OBJ_MOVIE) {
                circleRate = mObjStudioData.circleRateDark;
                blackBright = mObjStudioData.blackRateDark;
                blackTuneRate = mObjStudioData.blackTuneRateDark;
            } else if (mCurrentStatus <= SOF_BACK_FACE_MOVIE) {
                circleRate = mBackFaceStudioData.circleRateDark;
                blackBright = mBackFaceStudioData.blackRateDark;
                blackTuneRate = mBackFaceStudioData.blackTuneRateDark;
            } else if (mCurrentStatus <= SOF_FRONT_FACE_MOVIE) {
                circleRate = mFrontFaceStudioData.circleRateDark;
                blackBright = mFrontFaceStudioData.blackRateDark;
                blackTuneRate = mFrontFaceStudioData.blackTuneRateDark;
            }
        }

        LOGD("[studioMode] mStudioMode : %d - (%f %f %f)", mStudioMode, circleRate, blackBright, blackTuneRate);
    }

    void tuneManagerBlur::getStudioTuneValue(float &faderBright, float &faderSaturation, float &inBright, float &outBright, float &satRate, float &monoContrast)
    {
        faderBright = 1.0f;
        faderSaturation = 1.0f;
        inBright = 1.0f;
        outBright = 1.0f;
        satRate = 0.2;

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  ) {
            faderBright = mObjStudioData.faderBright;
            faderSaturation = mObjStudioData.faderSaturation;
            inBright = mObjStudioData.inBright;
            outBright = mObjStudioData.outBright;
            satRate = mObjStudioData.satRate;
            monoContrast = mObjStudioData.contrast;
        }
        else if( mCurrentStatus <= SOF_BACK_FACE_MOVIE  ) {
            faderBright = mBackFaceStudioData.faderBright;
            faderSaturation = mBackFaceStudioData.faderSaturation;
            inBright = mBackFaceStudioData.inBright;
            outBright = mBackFaceStudioData.outBright;
            satRate = mBackFaceStudioData.satRate;
            monoContrast = mBackFaceStudioData.contrast;
        }
        else if( mCurrentStatus <= SOF_FRONT_FACE_MOVIE  ) {
            faderBright = mFrontFaceStudioData.faderBright;
            faderSaturation = mFrontFaceStudioData.faderSaturation;
            inBright = mFrontFaceStudioData.inBright;
            outBright = mFrontFaceStudioData.outBright;
            satRate = mFrontFaceStudioData.satRate;
            monoContrast = mFrontFaceStudioData.contrast;
        }
    }

    int tuneManagerBlur::getMovingSensitivityValue(int index)
    {
        int value = -1;

        if(index == 0)
        {
            value =  mObjMovingSensitivity;
        }
        else if(index == 1)
        {
            value = mBackFaceMovingSensitivity;
        }
        else if(index == 2)
        {
            value =  mFrontFaceMovingSensitivity;
        }

        LOGD("getMovingSensitivityValue(%d) : %d", index, value);
        return value;
    }

    float tuneManagerBlur::getAiMoving()
    {
        return mAITuneData.aIMoving;
    }

    //what???
    int tuneManagerBlur::getAiSizeBlur()
    {
        return mAITuneData.aISizeBlurMax;
    }

    void tuneManagerBlur::getAiTouchMakerSmall(float &markerX, float &markerY, float &outerX, float &outerY)
    {
        markerX = mAITuneData.touchMarkerSmall[0];
        markerY = mAITuneData.touchMarkerSmall[1];
        outerX = mAITuneData.touchOuterSmall[0];
        outerY = mAITuneData.touchOuterSmall[1];
    }

    void tuneManagerBlur::getAiTouchMaker(float &markerX, float &markerY, float &outerX, float &outerY)
    {
        markerX = mAITuneData.touchMarker[0];
        markerY = mAITuneData.touchMarker[1];
        outerX = mAITuneData.touchOuter[0];
        outerY = mAITuneData.touchOuter[1];
    }

    void tuneManagerBlur::getAiMaker(float &markerX, float &markerY, float &outerX, float &outerY)
    {
        markerX = mAITuneData.aIMarker[0];
        markerY = mAITuneData.aIMarker[1];
        outerX = mAITuneData.aIOuter[0];
        outerY = mAITuneData.aIOuter[1];
    }

    int tuneManagerBlur::getHandToSkin()
    {
        if (SOF_BACK_FACE <= mCurrentStatus && mCurrentStatus <= SOF_BACK_FACE_MOVIE) {
            return mBackFaceAITuneData.handToSkin;
        }
        else if (SOF_FRONT_FACE <= mCurrentStatus && mCurrentStatus <= SOF_FRONT_FACE_MOVIE) {
            return mFrontFaceAITuneData.handToSkin;
        }
    }

    void tuneManagerBlur::getHandMaker(float &x, float &y, float &markerX, float &markerY, float &outerX, float &outerY)
    {
        if (SOF_BACK_FACE <= mCurrentStatus && mCurrentStatus <= SOF_BACK_FACE_MOVIE) {
            x = mBackFaceAITuneData.handX;
            y = mBackFaceAITuneData.handY;
            markerX = mBackFaceAITuneData.handMarker[0];
            markerY = mBackFaceAITuneData.handMarker[1];
            outerX = mBackFaceAITuneData.handOuter[0];
            outerY = mBackFaceAITuneData.handOuter[1];
        }
        else if (SOF_FRONT_FACE <= mCurrentStatus && mCurrentStatus <= SOF_FRONT_FACE_MOVIE) {
            x = mFrontFaceAITuneData.handX;
            y = mFrontFaceAITuneData.handY;
            markerX = mFrontFaceAITuneData.handMarker[0];
            markerY = mFrontFaceAITuneData.handMarker[1];
            outerX = mFrontFaceAITuneData.handOuter[0];
            outerY = mFrontFaceAITuneData.handOuter[1];
        }
    }

    void tuneManagerBlur::getAiTuneData(
            int &aIThreshod
            ,int &aISizePlus
            ,int &aIMin
            ,int &aIMax
            ,int &personMin
            ,int &personMax
            ,int &aIScreen
            ,int &a0Time
            ,int &a1Time
            ,int &backFaceBlueTime
            ,int &frontFaceBlueTime
            ,int &backFaceHandThreshold
            ,int &frontFaceHandThreshold
            ,int &aISizeBlurMin
            ,int &aISizeBlurMax
            ,int &aIProcessCount
            ,int &aIBelowPercent
            ,int &aIUpPercent
            ,int &aILeftPercent
            ,int &aIRightPercent
            ,int &alWaitCount
            ,int &touchTime
            ,int &touchBoxSize
            ,float &aIMultiUpRate
            ,float &aIMultiDownRate
            ,float &aIMultiSmallRate
            ,float &aIMultiBigRate
            ,int &aICornerX
            ,int &aICornerY)
    {
        aIThreshod = mAITuneData.aIThreshod;
        aISizePlus = mAITuneData.aISizePlus;
        aIMin = mAITuneData.aIMin;
        aIMax = mAITuneData.aIMax;
        personMin = mAITuneData.personMin;
        personMax = mAITuneData.personMax;
        aIScreen = mAITuneData.aIScreen;
        a0Time = mAITuneData.a0Time;
        a1Time = mAITuneData.a1Time;
        backFaceBlueTime = mBackFaceAITuneData.faceBlueTime;
        frontFaceBlueTime = mFrontFaceAITuneData.faceBlueTime;
        backFaceHandThreshold = mBackFaceAITuneData.handThreshold;
        frontFaceHandThreshold = mFrontFaceAITuneData.handThreshold;
        aISizeBlurMin = mAITuneData.aISizeBlurMin;
        aISizeBlurMax = mAITuneData.aISizeBlurMax;

        aIProcessCount = mAITuneData.aIProcessCount;
        aIBelowPercent = mAITuneData.aIBelowPercent;
        aIUpPercent = mAITuneData.aIUpPercent;
        aILeftPercent = mAITuneData.aILeftPercent;
        aIRightPercent = mAITuneData.aIRightPercent;
        alWaitCount = mAITuneData.alWaitCount;
        touchTime = mAITuneData.touchTime;
        touchBoxSize = mAITuneData.touchBoxSize;

        aIMultiUpRate = mAITuneData.aIMultiUpRate;
        aIMultiDownRate = mAITuneData.aIMultiDownRate;
        aIMultiSmallRate = mAITuneData.aIMultiSmallRate;
        aIMultiBigRate = mAITuneData.aIMultiBigRate;

        aICornerX = mAITuneData.aICornerX;
        aICornerY = mAITuneData.aICornerY;
    }

    int tuneManagerBlur::getMovingXYZValue()
    {
        LOGD("getMovingXYZValue) : %d", mBackFaceXYZCount);
        return mBackFaceXYZCount;
    }

    float tuneManagerBlur::getMapRate()
    {
        float rate = 1.0f;

        BACK_OBJECT_TUNEDATA tempObj;
        FACE_TUNEDATA temp;

        getCurrentStructure(tempObj, temp);

        if( mCurrentStatus <= SOF_BACK_OBJ_MOVIE  ) {
            //return tempObj.studioByBright[mCurrentBright].zoomRate;
            return 1.0f;
        }
    }

    void tuneManagerBlur::getObjBrightInfo(int &superLow, int &low, int &mid, int &out)
    {
        superLow = mObjTuneData.brightness[0].brightness;
        low = mObjTuneData.brightness[1].brightness;
        mid = mObjTuneData.brightness[2].brightness;
        out = mObjTuneData.brightness[3].brightness;
    }

    void tuneManagerBlur::setCurrentBright(double bright)
    {
        int dLow = 0, low, mid, out;

        if( mCurrentStatus == SOF_BACK_OBJ )
        {
            dLow = mObjTuneData.brightness[0].brightness;
            low = mObjTuneData.brightness[1].brightness;
            mid = mObjTuneData.brightness[2].brightness;
            out = mObjTuneData.brightness[3].brightness;
        }
        else if( mCurrentStatus == SOF_BACK_OBJ_SAVE )
        {
            dLow = mObjSaveTuneData.brightness[0].brightness;
            low = mObjSaveTuneData.brightness[1].brightness;
            mid = mObjSaveTuneData.brightness[2].brightness;
            out = mObjSaveTuneData.brightness[3].brightness;
        }
        else if( mCurrentStatus == SOF_BACK_OBJ_MOVIE )
        {
            dLow = mObjMovieTuneData.brightness[0].brightness;
            low = mObjMovieTuneData.brightness[1].brightness;
            mid = mObjMovieTuneData.brightness[2].brightness;
            out = mObjMovieTuneData.brightness[3].brightness;
        }
        else if( mCurrentStatus == SOF_BACK_FACE )
        {
            low = mBackFaceTuneData.brightness[0].brightness;
            mid = mBackFaceTuneData.brightness[1].brightness;
            out = mBackFaceTuneData.brightness[2].brightness;
        }
        else if( mCurrentStatus == SOF_BACK_FACE_SAVE )
        {
            low = mBackFaceSaveTuneData.brightness[0].brightness;
            mid = mBackFaceSaveTuneData.brightness[1].brightness;
            out = mBackFaceSaveTuneData.brightness[2].brightness;
        }
        else if( mCurrentStatus == SOF_BACK_FACE_MOVIE )
        {
            low = mBackFaceMovieTuneData.brightness[0].brightness;
            mid = mBackFaceMovieTuneData.brightness[1].brightness;
            out = mBackFaceMovieTuneData.brightness[2].brightness;
        }
        else if( mCurrentStatus == SOF_FRONT_FACE )
        {
            low = mFrontFaceTuneData.brightness[0].brightness;
            mid = mFrontFaceTuneData.brightness[1].brightness;
            out = mFrontFaceTuneData.brightness[2].brightness;
        }
        else if( mCurrentStatus == SOF_FRONT_FACE_SAVE )
        {
            low = mFrontFaceSaveTuneData.brightness[0].brightness;
            mid = mFrontFaceSaveTuneData.brightness[1].brightness;
            out = mFrontFaceSaveTuneData.brightness[2].brightness;
        }
        else if( mCurrentStatus == SOF_FRONT_FACE_MOVIE )
        {
            low = mFrontFaceMovieTuneData.brightness[0].brightness;
            mid = mFrontFaceMovieTuneData.brightness[1].brightness;
            out = mFrontFaceMovieTuneData.brightness[2].brightness;
        }

        mBright = bright;

        if( mBright <= dLow )
        {
            mCurrentBright = SOF_DEEP_LOWLIGHT;
        }
        else if( mBright <= low )
        {
            mCurrentBright = SOF_LOWLIGHT;
        }
        else if( mBright <= mid )
        {
            mCurrentBright = SOF_NORMAL;
        }
        else if( mBright <= out )
        {
            mCurrentBright = SOF_OUTDOOR;
        }

        LOGD("[Current-Bright] dLow : %d low : %d mid : %d out : %d", dLow, low, mid, out);
        LOGD("[Current-Bright] mBright : %f, mCurrentBright : %d", mBright, mCurrentBright);
    }

    void tuneManagerBlur::setCurrentStatus(bool isFront, bool isFace, bool isSave, bool isMovie)
    {
        if( isFront == false)
        {
            if( isFace == false )
            {
                if( isSave == true )
                {
                    mCurrentStatus = SOF_BACK_OBJ_SAVE;
                }
                else if(isMovie == true)
                {
                    mCurrentStatus = SOF_BACK_OBJ_MOVIE;
                }
                else
                {
                    mCurrentStatus = SOF_BACK_OBJ;
                }
            }
            else
            {
                if( isSave == true )
                {
                    mCurrentStatus = SOF_BACK_FACE_SAVE;
                }
                else if(isMovie == true)
                {
                    mCurrentStatus = SOF_BACK_FACE_MOVIE;
                }
                else
                {
                    mCurrentStatus = SOF_BACK_FACE;
                }
            }
        }
        else
        {
            if( isFace == true ) {
                if( isSave == true )
                {
                    mCurrentStatus = SOF_FRONT_FACE_SAVE;
                }
                else if(isMovie == true)
                {
                    mCurrentStatus = SOF_FRONT_FACE_MOVIE;
                }
                else
                {
                    mCurrentStatus = SOF_FRONT_FACE;
                }
            }
        }

        LOGD("[Tool-Test] mCurrentStatus : %d\n", mCurrentStatus);
    }

    void tuneManagerBlur::setStudioMode(int studioMode)
    {
        mStudioMode = studioMode;
    }

    int tuneManagerBlur::getStudioMode()
    {
        return mStudioMode;
    }

    BACK_OBJECT_TUNEDATA tuneManagerBlur::getObjData(bool save)
    {
        if(save == false)
        {
            return mObjTuneData;
        }
        else
        {
            return mObjSaveTuneData;
        }
    }

    FACE_TUNEDATA tuneManagerBlur::getFaceData(bool front, bool save)
    {
        if(front == false)
        {
            if(save == false)
            {
                return mBackFaceTuneData;
            }
            else
            {
                return mBackFaceSaveTuneData;
            }
        }
        else
        {
            if(save == false)
            {
                return mFrontFaceTuneData;
            }
            else
            {
                return mFrontFaceSaveTuneData;
            }
        }
    }
}