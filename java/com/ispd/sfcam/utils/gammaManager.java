package com.ispd.sfcam.utils;

import android.os.SystemClock;

public class gammaManager {
    private static String TAG = "gammaManager";

    private static double mInValue = 1.0;
    public static double mAnmationGamma = 1.0;

    private static double mPreValue = -1.0;
    private static double mGapValue;
    private static double mCurrentValue;

    private static boolean mReset = true;

    private static long mStartTime = -1000;
    private static long mPreTime = -1000;
    private static long mAnimaitionTime = 1000;

    private static long getCurrentTime()
    {
        return SystemClock.uptimeMillis();
    }

    public static void setAnimationGamma(double inValue)
    {
        mAnmationGamma = inValue;
    }

    public static void setCurrentValue(double inValue)
    {
        mInValue = inValue;
    }

    public static double getCurrentValue()
    {
        return mInValue;
    }

    public static double calcValueAnimation(double inValue)
    {
        if( mPreValue != inValue )
        {
            Log.d(TAG, "[makeAnimationRGBGain] mPreValue != inGains[0]");

            mPreValue = inValue;

            if( mStartTime != -1000 )
            {
                Log.d(TAG, "[makeAnimationRGBGain] mStartTime != -1000 mPreValue != inGains[0]");
                mReset = true;
            }
        }

        if( mReset == true )
        {
            mGapValue = (inValue - mCurrentValue) / mAnimaitionTime * 1.05;

            Log.d(TAG, "[makeAnimationRGBGain] mReset == true redGap : "+mGapValue);

            //first
            mStartTime = mPreTime = getCurrentTime();

            Log.d(TAG, "[makeAnimationRGBGain] [timing-test-smart] mReset == true mStartTime time : "+mStartTime);

            mCurrentValue = mCurrentValue + mGapValue;

            mReset = false;
        }
        else
        {
            Log.d(TAG, "[makeAnimationRGBGain] else");

            long currentTime = getCurrentTime();
            long gapTime = currentTime - mPreTime;

            mPreTime = currentTime;

            if( getCurrentTime() - mStartTime > mAnimaitionTime )
            {
                Log.d(TAG, "[makeAnimationRGBGain-test] [timing-test-smart] _getTickTime() - mStartTime > 3000");
                Log.d(TAG, "[makeAnimationRGBGain-test] inGainss[0] : "+inValue);
                Log.d(TAG, "[makeAnimationRGBGain-test] LAST redGap : "+(mGapValue*gapTime));
                gapTime = 0;

                Log.d(TAG, "[makeAnimationRGBGain2] last : "+(inValue-mCurrentValue));

                mCurrentValue = inValue;
            }

            Log.d(TAG, "[makeAnimationRGBGain-test] else pre mCurrentValue : "+mCurrentValue);
            Log.d(TAG, "[makeAnimationRGBGain-test] else redGap : "+mGapValue*gapTime);

            Log.d(TAG, "[makeAnimationRGBGain] else gapTime : "+gapTime);

            double saveRed = mCurrentValue;

            mCurrentValue = mCurrentValue + mGapValue * gapTime;

            Log.d(TAG, "[makeAnimationRGBGain2] doing : "+(mCurrentValue-saveRed));

            Log.d(TAG, "[makeAnimationRGBGain] else post mCurrentValue : "+mCurrentValue);
            Log.d(TAG, "[makeAnimationRGBGain] else inGains[0] : "+inValue);


            if( mGapValue > 0.f )
            {
                if(mCurrentValue > inValue )
                {
                    Log.d(TAG, "[makeAnimationRGBGain] [timing-test-smart] plus _getTickTime() - mStartTime  : "+(getCurrentTime() - mStartTime));
                    mCurrentValue = inValue;
                }
            }
            else
            {
                if(mCurrentValue < inValue )
                {
                    Log.d(TAG, "[makeAnimationRGBGain] [timing-test-smart] minus _getTickTime() - mStartTime  : "+(getCurrentTime() - mStartTime));
                    mCurrentValue = inValue;
                }
            }
        }

        return mCurrentValue;
    }
}
