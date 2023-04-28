package com.ispd.sfcam.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by khkim on 2018-03-20.
 */

public class sensorInfo implements SensorEventListener {
    private String Tag = "sensorInfo";

    private SensorManager mSensorManager;
    private Sensor mLight;
    public float mLightValue;

    public sensorInfo(Context context)
    {
        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    public float getCurrentLight()
    {
        return mLightValue;
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        mLightValue = event.values[0];
        Log.d(Tag, "mLightValue : "+mLightValue);
        // Do something with this sensor data.
    }

    public void start()
    {
        // Register a listener for the sensor.
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop()
    {
        // Be sure to unregister the sensor when the activity pauses.
        mSensorManager.unregisterListener(this);
    }
}