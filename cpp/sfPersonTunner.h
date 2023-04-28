#ifndef SFTUNNER_P__H__
#define SFTUNNER_P__H__

extern "C" {

    #define MAX_OBJ_BRIGHT_COUNT 4
    #define MAX_PERSON_BRIGHT_COUNT 3

    enum SF_BRIGHT_STATUS
    {
        SF_DEEP_LOWLIGHT = 0,
        SF_LOWLIGHT,
        SF_NORMAL,
        SF_OUTDOOR,
        SF_BRIGHT_NONE
    };

    enum SF_NEED_STATUS
    {
        SF_BACK_OBJ = 0,
        SF_BACK_OBJ_SAVE,
        SF_BACK_OBJ_MOVIE,
        SF_BACK_FACE,
        SF_BACK_FACE_SAVE,
        SF_BACK_FACE_MOVIE,
        SF_FRONT_FACE,
        SF_FRONT_FACE_SAVE,
        SF_FRONT_FACE_MOVIE,
        SF_NEED_NONE
    };

    typedef struct
    {
        int faderStart;
        int faderCount;
        float faderBright;
        float faderSaturation;
        float inBright;
        float outBright;
        float satRate;
        float circleRate;
        float blackRate;
        float contrast;
        float blackTuneRate;
    } SF_STUDIO_TUNEDATAS;

    typedef struct
    {
        int brightness;
        int movingBlur;
        int movingArea;
        int movingSens;
        float movingThreshold;
        float movingMovieThreshold;
        int waterBlur;
        float TargetGamma;
        float maxGamma;
        float cartoonBackTexRate;
        float cartoonBackSat;
        float cartoonBackEdge;
        float cartoonFrontTexRate;
        float cartoonFrontSat;
        float cartoonFrontEdge;
        float beautyRate;
        int blurCount;
        int previewBlur1;
        int previewBlur2;
        int previewBlur3;
        int captureBlur1;
        int captureBlur2;
        int captureBlur3;
        int videoBlur1;
        int videoBlur2;
        int videoBlur3;
        int fastMoving;
        int minFaceSize;
        int maxFaceSize;
        int minMovingPercent;
        int maxMovingPercent;
    } SF_COMMON_TUNES;

    typedef struct
    {
        int resize;
        int scale;
        float moveX;
        float moveY;
        int leftRightScale;
    } SF_WATER_TUNES;

    typedef struct
    {
        float scaleXcartoon;
        float scaleYcartoon;
        float scaleXblur;
        float scaleYblur;
        float scaleXsf;
        float scaleYsf;
        float cartoonThickness;
        float blurThickness;
        float sfThickness;
        float resizeXY;
        float color;
        float colorStart;
        float transUD;
        float transLR;
    } SF_FEATHER_TUNES;

    typedef struct
    {
        SF_STUDIO_TUNEDATAS studioTune;
        SF_COMMON_TUNES commonTune[MAX_OBJ_BRIGHT_COUNT];
        SF_WATER_TUNES waterMarkerTune[MAX_OBJ_BRIGHT_COUNT];
        SF_WATER_TUNES waterOuterTune[MAX_OBJ_BRIGHT_COUNT];
        SF_FEATHER_TUNES fastTune[MAX_OBJ_BRIGHT_COUNT];
        SF_FEATHER_TUNES slowTune[MAX_OBJ_BRIGHT_COUNT];
    } SF_OBJECT_TUNE;

    typedef struct
    {
        SF_STUDIO_TUNEDATAS studioTune;
        SF_COMMON_TUNES commonTune[MAX_PERSON_BRIGHT_COUNT];
        SF_WATER_TUNES waterMarkerTune[MAX_PERSON_BRIGHT_COUNT];
        SF_WATER_TUNES waterOuterTune[MAX_PERSON_BRIGHT_COUNT];
        SF_WATER_TUNES waterMarkerTuneForSave[MAX_PERSON_BRIGHT_COUNT];
        SF_WATER_TUNES waterOuterTuneForSave[MAX_PERSON_BRIGHT_COUNT];
        SF_FEATHER_TUNES superFastTune[MAX_PERSON_BRIGHT_COUNT];
        SF_FEATHER_TUNES fastTune[MAX_PERSON_BRIGHT_COUNT];
        SF_FEATHER_TUNES slowTune[MAX_PERSON_BRIGHT_COUNT];
        SF_FEATHER_TUNES saveTune[MAX_PERSON_BRIGHT_COUNT];
    } SF_PERSON_TUNE;

    class tuneManagerPersonSf
    {
        public:
        tuneManagerPersonSf();
        ~tuneManagerPersonSf();

        void printAllData();
        void loadingTuneData();

        private:
        int fastAlOn;
        int slowAlOn;
        int stopCount;
        SF_OBJECT_TUNE backObj;
        SF_PERSON_TUNE frontPerson;
        SF_PERSON_TUNE backPerson;

        int mCurrentStatus;
        int mCurrentBright;

        public:
        void setObjBrightInfo(int superLowBright, int lowBright, int midBright, int outBright);
        bool getCurrentStatus();
        void setCurrentBright(double bright);
        void setCurrentStatus(bool isFront, bool isFace, bool isSave, bool isMovie);

        void getAllTune(int &fastOn, int &slowOn, int &stop_count);
        void getStudioTune(SF_STUDIO_TUNEDATAS &datas);
        void getCommonTune(SF_COMMON_TUNES &datas);
        int getWaterTune(SF_WATER_TUNES &marker, SF_WATER_TUNES &outer);
        void getCompensationTune(SF_FEATHER_TUNES &superFastAl, SF_FEATHER_TUNES &fastAl, SF_FEATHER_TUNES &slowAl);
        void getBlurTuneData(int &count, int &blur1, int &blur2, int &blur3);
        void getMovingRateTune(int *datas);
    };
}

#endif //SFTUNNER_P__H__