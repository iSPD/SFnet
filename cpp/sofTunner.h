#ifndef SOFTUNNER__H__
#define SOFTUNNER__H__

extern "C" {

    enum BRIGHT_STATUS
    {
        SOF_DEEP_LOWLIGHT = 0,
        SOF_LOWLIGHT,
        SOF_NORMAL,
        SOF_OUTDOOR,
        SOF_BRIGHT_NONE
    } ;

    enum NEED_STATUS
    {
        SOF_BACK_OBJ = 0,
        SOF_BACK_OBJ_SAVE,
        SOF_BACK_OBJ_MOVIE,
        SOF_BACK_FACE,
        SOF_BACK_FACE_SAVE,
        SOF_BACK_FACE_MOVIE,
        SOF_FRONT_FACE,
        SOF_FRONT_FACE_SAVE,
        SOF_FRONT_FACE_MOVIE,
        SOF_NEED_NONE
    } ;

    typedef struct
    {
        int brightness;//4
        int preBlur;//4
        int maskCount;//4
        int imageCompareTotal;//4
        int imageCompareCount;//4
        int markerThickness[4];//4*4
        float outerCompenSize[4];//8*4 = 20+16+16
        int blurCount[4];
        int blurSize1[4];
        int blurSize2[4];
        int blurSize3[4];
        int faderThickness[4];
        float movingValue;
    } TUNEDATA_BRIGHTNESS;

    typedef struct
    {
        int aIThreshod;//java
        float aIMoving;//c
        int aISizePlus;//java
        int aIMin;//java
        int aIMax;//java
        int personMin;//java
        int personMax;//java
        int aIScreen;//java
        int aISizeBlurMin;//java
        int aISizeBlurMax;//java
        int a0Time;//java
        int a1Time;//java

        int aIProcessCount;//java
        int aIBelowPercent;//java
        int aIUpPercent;//java
        int aILeftPercent;//java
        int aIRightPercent;//java
        int alWaitCount;
        int touchTime;//java
        int touchBoxSize;//java

        float aIMultiUpRate;
        float aIMultiDownRate;
        float aIMultiSmallRate;
        float aIMultiBigRate;

        int aICornerX;
        int aICornerY;

        float aIMarker[2];//c
        float aIOuter[2];//c
        float touchMarker[2];//c
        float touchOuter[2];//c
        float touchMarkerSmall[2];//c
        float touchOuterSmall[2];//c
    } BACK_AI_TUNEDATA;

    typedef struct
    {
        float touchBoundary;
        int openClose[2];
        TUNEDATA_BRIGHTNESS brightness[4];
    } BACK_OBJECT_TUNEDATA;

    typedef struct
    {
        float touchBoundary;
        int openClose[2];
        float faceSizeCompMinMax[4];//8*4
        float faceBodyXyzCompMinMax[4];//8*4
        float faceXYZx2[8];//8*8
        float faceSizeMinMax[4];//8*4
        float bodyXYZx2[8];//8*8
        float bodySizeMinMax[4];//8*4
        TUNEDATA_BRIGHTNESS brightness[3];
    } FACE_TUNEDATA;

    typedef struct
    {
        int handToSkin;//c
        float handX;//c
        float handY;//c
        int faceBlueTime;//java
        int handThreshold;
        float handMarker[2];//c
        float handOuter[2];//c
    } FACE_AI_TUNEDATA;

    typedef struct
    {
        float oriX;
        float oriY;
        float leftX;
        float leftY;
        float rightX;
        float rightY;
        float downUpX;
        float downUpY;
    } FACE_ROTATE_TUNE;

    typedef struct
    {
        //Mono Mode
        int faderStart;
        int faderCount;
        float faderBright;
        float faderSaturation;
        float inBright;
        float outBright;
        float satRate;
        float circleRateMono;
        float blackRateMono;
        float contrast;
        float blackTuneRateMono;
        //Black Mode
        float circleRateBlack;
        float blackRateBlack;
        float blackTuneRateBlack;
        //Dark Mode
        float circleRateDark;
        float blackRateDark;
        float blackTuneRateDark;
    } STUDIO_TUNEDATA;

    class tuneManagerBlur
    {
        public:
        tuneManagerBlur();
        ~tuneManagerBlur();

        void printAllData();
        void loadingTuneData();
        void getObjBrightInfo(int &superLow, int &low, int &mid, int &out);
        void setCurrentBright(double bright);
        void setCurrentStatus(bool isFront, bool isFace, bool isSave, bool isMovie);
        void setStudioMode(int studioMode);
        int getStudioMode();

        void getFaceData(float *faceXyz, float *faceSizeMax, float *faceSizeMin, float &faceSizeByY, int which);
        void getBodyData(float *bodyXyz, float *bodySizeMax, float *bodySizeMin, float &bodySizeByY, int which);
        int getBlurData(int *blurCount, int *blurSize1, int *blurSize2, int *blurSize3);
        int getBlurDataSave(int *blurCount, int *blurSize1, int *blurSize2, int *blurSize3);
        float getMovingValue();

        void getCurrentStructure(BACK_OBJECT_TUNEDATA &obj, FACE_TUNEDATA &face);

        int getPreBlur();
        float getTouchBoundary();
        float getTouchBoundarySave();
        int getMaskCount();
        void getOuterSize(float *data);
        void getFaderCount(int *data);
        void getMarkerSize(int *data);
        int getFaderStart();
        int getImageCompareTotal();
        int getImageCompareCount();
        void getOpenClose(int &open, int &close);
        void getFaceMinMaxSize(float &minSize, float &maxSize, float &minRate, float &maxRate);
        void getBodyMinMaxSize(float &minSize, float &maxSize, float &minRate, float &maxRate);

        void getFaceCompSize(float &minSize, float &maxSize, float &minRate, float &maxRate);
        void getXyzCompSize(float &minSize, float &maxSize, float &minRate, float &maxRate);

        void getFaceXYZData(float &xValueL, float &xValueR, float &yValueL, float &yValueR, float &zValueL, float &zValueR);
        void getBodyXYZData(float &xValueL, float &xValueR, float &yValueL, float &yValueR, float &zValueL, float &zValueR);

        void getFaceSizeByY(float &sizeValueL, float &sizeValueR);
        void getBodySizeByY(float &sizeValueL, float &sizeValueR);

        void getRotTune(float &x, float &y, int rotate);

        void getStudioFaderData(int &faderStart, int &faderCount);
        void getStudioDarkData(float &circleRate, float &blackBright, float &blackTuneRate);
        void getStudioTuneValue(float &faderBright, float &faderSaturation, float &inBright, float &outBright, float &satRate, float &monoContrast);
        int getMovingSensitivityValue(int index);

        float getAiMoving();
        int getAiSizeBlur();
        void getAiTouchMakerSmall(float &markerX, float &markerY, float &outerX, float &outerY);
        void getAiTouchMaker(float &markerX, float &markerY, float &outerX, float &outerY);
        void getAiMaker(float &markerX, float &markerY, float &outerX, float &outerY);

        int getHandToSkin();
        void getHandMaker(float &x, float &y, float &markerX, float &markerY, float &outerX, float &outerY);

        void getAiTuneData(
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
                ,int &alCornerY);

        int getMovingXYZValue();
        float getMapRate();

        BACK_OBJECT_TUNEDATA getObjData(bool save);
        FACE_TUNEDATA getFaceData(bool front, bool save);

        BACK_OBJECT_TUNEDATA mObjTuneData;
        BACK_OBJECT_TUNEDATA mObjSaveTuneData;
        BACK_OBJECT_TUNEDATA mObjMovieTuneData;

        BACK_AI_TUNEDATA mAITuneData;

        FACE_TUNEDATA mBackFaceTuneData;
        FACE_TUNEDATA mBackFaceSaveTuneData;
        FACE_TUNEDATA mBackFaceMovieTuneData;

        FACE_TUNEDATA mFrontFaceTuneData;
        FACE_TUNEDATA mFrontFaceSaveTuneData;
        FACE_TUNEDATA mFrontFaceMovieTuneData;

        STUDIO_TUNEDATA mObjStudioData;
        STUDIO_TUNEDATA mBackFaceStudioData;
        STUDIO_TUNEDATA mFrontFaceStudioData;

        FACE_ROTATE_TUNE mBackFaceRotData;
        FACE_ROTATE_TUNE mFrontFaceRotData;

        FACE_AI_TUNEDATA mBackFaceAITuneData;
        FACE_AI_TUNEDATA mFrontFaceAITuneData;

        int mObjMovingSensitivity;
        int mBackFaceMovingSensitivity;
        int mFrontFaceMovingSensitivity;
        int mBackFaceXYZCount;

        NEED_STATUS mCurrentStatus;
        BRIGHT_STATUS mCurrentBright;
        int mStudioMode;

        //java Interface
        float mBlueFaceSize;
        float mBlueFaceCompRate;
        float mBlueFaceCompValue;
        float mFaceCompAvgValue;
        float mXyzCompValue;
        double mBright;
    };
}

#endif