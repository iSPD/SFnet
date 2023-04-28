package com.ispd.sfcam.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by nexus on 2017-12-13.
 */

public class gyroInfo {

    private static final String TAG = "gyroInfo";

    private int mRotationInfo = 0;//-1;

    //Using the Accelometer & Gyroscoper
    private SensorManager mSensorManager = null;

    //Using the Accelometer
    private SensorEventListener mAccLis;
    private Sensor mAccelometerSensor = null;

    public gyroInfo(Context context)
    {
        Log.e(TAG, "gyroInfo");

        //Using the Gyroscope & Accelometer
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        //Using the Accelometer
        mAccelometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccLis = new AccelometerListener();
    }

    public void start()
    {
        Log.e(TAG, "start");

        if( mSensorManager != null ) {
            mSensorManager.registerListener(mAccLis, mAccelometerSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stop()
    {
        Log.e(TAG, "stop");

        if( mSensorManager != null ) {
            mSensorManager.unregisterListener(mAccLis);
        }
    }

    public int getRotateInfo()
    {
        return mRotationInfo;
    }

    private class AccelometerListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {

            double accX = event.values[0];
            double accY = event.values[1];
            double accZ = event.values[2];

            double angleXZ = Math.atan2(accX,  accZ) * 180/Math.PI;
            double angleYZ = Math.atan2(accY,  accZ) * 180/Math.PI;

            Log.e(TAG, "ACCELOMETER           [X]:" + String.format("%.4f", event.values[0])
                    + "           [Y]:" + String.format("%.4f", event.values[1])
                    + "           [Z]:" + String.format("%.4f", event.values[2])
                    + "           [angleXZ]: " + String.format("%.4f", angleXZ)
                    + "           [angleYZ]: " + String.format("%.4f", angleYZ));

//            if( accY > 5.0 && Math.abs(accX) < 5.0 )
//            {
//                //port
//                mRotationInfo = 0;
//            }
//            else if( accY < -5.0 && Math.abs(accX) < 5.0 )
//            {
//                //reverse port
//                mRotationInfo = 180;
//            }
//            else if( accX > 5.0 && Math.abs(accY) < 5.0 )
//            {
//                //land
//                mRotationInfo = 270;
//            }
//            else if( accX < -5.0 && Math.abs(accY) < 5.0 )
//            {
//                //reverse land
//                mRotationInfo = 90;
//            }
//            else
//            {
//                //mRotationInfo = -1;
//                mRotationInfo = 0;
//            }

            if( Math.abs(Math.abs(accY) - Math.abs(accX)) > 2.0f ) {

                if (Math.abs(accY) >= Math.abs(accX)) {
                    if (accY >= 0) {
                        mRotationInfo = 0;
                    } else {
                        mRotationInfo = 180;
                    }
                } else if (Math.abs(accY) < Math.abs(accX)) {
                    if (accX >= 0) {
                        mRotationInfo = 270;
                    } else {
                        mRotationInfo = 90;
                    }
                } else {
                    mRotationInfo = 0;
                }
            }

            Log.d(TAG, "mRotationInfo : "+mRotationInfo);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }
}
