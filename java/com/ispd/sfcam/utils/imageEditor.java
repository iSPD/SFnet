package com.ispd.sfcam.utils;

import com.ispd.sfcam.aiCamParameters;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.Date;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;

public class imageEditor {

    static byte[] mYuvBytes;
    static Mat mYuvMat;
    static Mat mRgbaMat = new Mat();

    public static void resizeByte(byte[] yuvBytes, byte[] resizeBytes, int skipX, int skipY)
    {
        int previewWidth = aiCamParameters.PREVIEW_WIDTH_I;
        int previewHeight = aiCamParameters.PREVIEW_HEIGHT_I;

        if (mYuvBytes == null) {
            mYuvBytes = new byte[yuvBytes.length];
        }

//        if( resizeBytes == null )
//        {
//            resizeBytes = new byte[previewWidth * (previewHeight * 3 / 2) / (skipX * skipY)];
//        }

        System.arraycopy(yuvBytes, 0, mYuvBytes, 0, yuvBytes.length);

        int fillCount = 0;
        for(int i = 0; i < previewHeight * 3 / 2; i = i + skipY)
        {
            if( i < previewHeight ) {
                for (int j = 0; j < previewWidth; j = j + skipX) {
                    resizeBytes[fillCount++] = mYuvBytes[i * previewWidth + j];
                }
            }
            else
            {
                for (int j = 0; j < previewWidth; j = j + (skipX * 2)) {
                    resizeBytes[fillCount++] = mYuvBytes[i * previewWidth + j];
                    resizeBytes[fillCount++] = mYuvBytes[i * previewWidth + j + 1];

//                    if( fillCount >=  resizeBytes.length)
//                    {
//                        break;
//                    }
                }
            }
        }
    }

    public static Mat byteToMatToRgb(byte[] input, int width, int height)
    {
        if( mYuvMat == null ) {
            mYuvMat = new Mat(height + (height / 2), width, CV_8UC1);
        }

        Log.d("mat-size", "input : "+input.length+", mYuvMat : "+mYuvMat.size());

        mYuvMat.put(0, 0, input);
        //imwrite("/sdcard/obj/resize1.jpg", mYuvMat);
        Imgproc.cvtColor(mYuvMat, mRgbaMat, Imgproc.COLOR_YUV2RGB_NV21, 3);
        //imwrite("/sdcard/obj/resize2.jpg", mRgbaMat);

        return mRgbaMat;
    }
}
