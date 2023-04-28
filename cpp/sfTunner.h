#ifndef SFTUNNER__H__
#define SFTUNNER__H__

extern "C" {

    typedef struct
    {
        int resize;
        int scale;
        float moveX;
        float moveY;
        int leftRightScale;
        int onoff;
        int preBlur;
    } SF_WATER_TUNE;

    typedef struct
    {
        int mFStart;
        int mFThickness;
        int mResizeX;
        int mResizeY;
        float mColor;
        float mColorStart;
        float mMovingBlur;
        float mMovingArea;
        float mMovingSens;
        float mMovingValue;
        float mTransUD;
        float mTransLR;
        float mLRScale;
        float mStopCount;
        float mTargetGamma;
        float mMaxGamma;
    } SF_FEATHER_TUNE;

    class tuneManagersf
    {
        public:
        tuneManagersf();
            ~tuneManagersf();

            void printAllData();
            void loadingTuneData();
            void getFastWaterTuneData(SF_WATER_TUNE &marker, SF_WATER_TUNE &outer);
            void getSlowWaterTuneData(SF_WATER_TUNE &marker, SF_WATER_TUNE &outer);
            void getFeatherTuneData(SF_FEATHER_TUNE &fastInfo, SF_FEATHER_TUNE &slowInfo);
            void getOnOffAlg(int &fastAlg, int &slowAlg);

        private:
            SF_WATER_TUNE mFastMarkerInfo;
            SF_WATER_TUNE mFastOuterInfo;
            SF_WATER_TUNE mSlowMarkerInfo;
            SF_WATER_TUNE mSlowOuterInfo;

            SF_FEATHER_TUNE mFastInfo;
            SF_FEATHER_TUNE mSlowInfo;
    };
}

#endif