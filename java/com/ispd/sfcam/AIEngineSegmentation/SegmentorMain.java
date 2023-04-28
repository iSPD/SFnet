/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ispd.sfcam.AIEngineSegmentation;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.ispd.sfcam.AIEngineSegmentation.env.ImageUtils;
import com.ispd.sfcam.AIEngineSegmentation.segmentation.Segmentor;
import com.ispd.sfcam.AIEngineSegmentation.segmentation.TFLiteObjectSegmentationAPIModel;
import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.drawView.drawViewer;
import com.ispd.sfcam.jniController;
import com.ispd.sfcam.pdEngine.glEngine;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.SFTunner;
import com.ispd.sfcam.utils.gammaManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.Core.ROTATE_180;
import static org.opencv.core.Core.ROTATE_90_CLOCKWISE;
import static org.opencv.core.Core.ROTATE_90_COUNTERCLOCKWISE;
import static org.opencv.core.Core.add;
import static org.opencv.core.Core.addWeighted;
import static org.opencv.core.Core.countNonZero;
import static org.opencv.core.Core.flip;
import static org.opencv.core.Core.merge;
import static org.opencv.core.Core.rotate;
import static org.opencv.core.Core.split;
import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2RGBA;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2YCrCb;
import static org.opencv.imgproc.Imgproc.COLOR_RGBA2BGRA;
import static org.opencv.imgproc.Imgproc.COLOR_RGBA2RGB;
import static org.opencv.imgproc.Imgproc.COLOR_YCrCb2RGB;
import static org.opencv.imgproc.Imgproc.INTER_AREA;
import static org.opencv.imgproc.Imgproc.INTER_NEAREST;
import static org.opencv.imgproc.Imgproc.MORPH_CROSS;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY_INV;
import static org.opencv.imgproc.Imgproc.THRESH_TOZERO;
import static org.opencv.imgproc.Imgproc.THRESH_TOZERO_INV;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.equalizeHist;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.imgproc.Imgproc.threshold;
import static org.opencv.imgproc.Imgproc.warpAffine;

/**
 * An activity that uses a TensorFlowMultiBoxsegmentor and ObjectTracker to segment and then track
 * objects.
 */
public class SegmentorMain {
  static private String TAG = "AiCam-SegmentorMain";

  // Configuration values for the prepackaged DeepLab model.
//  private static final int TF_OD_API_INPUT_WIDTH = 305;
//  private static final int TF_OD_API_INPUT_HEIGHT = 305;
  private int TF_OD_API_INPUT_WIDTH = 305;
  private int TF_OD_API_INPUT_HEIGHT = 305;
  private static final int TF_OD_API_NUM_CLASS = 21;
  private static final int TF_OD_API_NUM_OUTPUT = 1;

  //private static final String TF_OD_API_MODEL_FILE = "q_ndm_513_deeplab.tflite";//deeplab.lite
  //private static final String TF_OD_API_MODEL_FILE = "qo_193_deeplab.tflite";//deeplab.lite
  //private static final String TF_OD_API_MODEL_FILE = "q_305_deeplab.tflite";//deeplab.lite
  private String TF_OD_API_MODEL_FILE = "q_305_deeplab.tflite";//deeplab.lite
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/pascal_voc_labels_list.txt";

  private static final boolean MAINTAIN_ASPECT = false;
  private static final boolean SAVE_PREVIEW_BITMAP = false;

  private Segmentor segmentor;

  private long lastProcessingTimeMs;

  //SpeedTest
  //private static Bitmap rgbFrameBitmap = null;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;

  private boolean computingSegmentation = false;

  private long timestamp = 0;

//  private Matrix frameToCropTransformBack0;
//  private Matrix cropToFrameTransformBack0;
//
//  private Matrix frameToCropTransformFront0;
//  private Matrix cropToFrameTransformFront0;
  private int mRotation = 0;
  private Matrix mFrameToCropTransform0;
  private Matrix mFrameToCropTransform90;
  private Matrix mFrameToCropTransform180;
  private Matrix mFrameToCropTransform270;
  private Matrix mCropToFrameTransform0;
  private Matrix mCropToFrameTransform90;
  private Matrix mCropToFrameTransform180;
  private Matrix mCropToFrameTransform270;
  private int mCropSize;

  private int mCameraLocation = 0;

  private Context mContext;
  //SpeedTest
  //private static int mPreviewWidth;
  //private static int mPreviewHeight;

  //private static int[] mRgbInts = null;
  
  private int mPreviewWidth;
  private int mPreviewHeight;

  private int[] mRgbInts = null;

  private AssetManager mAsset;

  private glEngine mGlEngine;
  private Mat mSegmentMat;
  static private Mat mCopySegmentMat = new Mat();
  static private Mat mPersonCheckMat = new Mat();
  static private boolean mCanUseSegmentMat = false;
  private Mat mSegmentResultMat;
  private Mat mSegmentStudioMat;
  private Mat mSegmentProccessMat;
  private byte[] mSegmentByteForSF;
  private byte[] mSegmentByteForBlur;
  private byte[] mSegmentByteForStudio;
  private boolean mDoneInference = true;

  //SpeedTest
  //private static byte[] mYuvBytes;
  //private static byte[] mResizeYuvByte;
  byte[] mYuvBytes;
  byte[] mResizeYuvByte;

  private int mOption;
  private static int mOptionCopy;

  //SpeedTest
  //private static int mSizeSkipX = 6;
  //private static int mSizeSkipY = 5;
  int mSizeSkipX = 6;
  int mSizeSkipY = 5;
//    int mSizeSkipX = 3;
//    int mSizeSkipY = 3;

  //SpeedTest
  //private static Mat previewMat;
  private Mat previewMat;
  private Mat previewProcessMat;
  private static Mat copySlowSegment = null;

  private static int mNoHumanCount = 0;
  private static boolean mCurrentHumanOn = false;

  public SegmentorMain(Context context, AssetManager asset, int width, int height, int option) {

      mOption = option;

      if( option == -1 )
      {
          mSizeSkipX = 6;
          mSizeSkipY = 5;

//          mSizeSkipX = 10;
//          mSizeSkipY = 10;

//          TF_OD_API_INPUT_WIDTH = 97;
//          TF_OD_API_INPUT_HEIGHT = 97;
//          TF_OD_API_MODEL_FILE = "q_97_deeplab.tflite";
          TF_OD_API_INPUT_WIDTH = 193;
          TF_OD_API_INPUT_HEIGHT = 193;
          TF_OD_API_MODEL_FILE = "q_193_deeplab.tflite";
//          TF_OD_API_INPUT_WIDTH = 257;
//          TF_OD_API_INPUT_HEIGHT = 257;
//          TF_OD_API_MODEL_FILE = "q_257_deeplab.tflite";

//          TF_OD_API_INPUT_WIDTH = 193;
//          TF_OD_API_INPUT_HEIGHT = 193;
//          TF_OD_API_MODEL_FILE = "q32_193_deeplab.tflite";
      }
      else if( option == 0 )
      {
//          mSizeSkipX = 6;
//          mSizeSkipY = 5;

          mSizeSkipX = 6;
          mSizeSkipY = 5;

//          TF_OD_API_INPUT_WIDTH = 97;
//          TF_OD_API_INPUT_HEIGHT = 97;
//          TF_OD_API_MODEL_FILE = "q_97_deeplab.tflite";
//          TF_OD_API_INPUT_WIDTH = 129;
//          TF_OD_API_INPUT_HEIGHT = 129;
//          TF_OD_API_MODEL_FILE = "q_129_deeplab.tflite";
//          TF_OD_API_INPUT_WIDTH = 193;
//          TF_OD_API_INPUT_HEIGHT = 193;
//          TF_OD_API_MODEL_FILE = "q_193_deeplab.tflite";
//          TF_OD_API_INPUT_WIDTH = 161;
//          TF_OD_API_INPUT_HEIGHT = 161;
//          TF_OD_API_MODEL_FILE = "q_161_deeplab.tflite";
          TF_OD_API_INPUT_WIDTH = 257;
          TF_OD_API_INPUT_HEIGHT = 257;
          TF_OD_API_MODEL_FILE = "q_257_deeplab.tflite";

//          TF_OD_API_INPUT_WIDTH = 193;
//          TF_OD_API_INPUT_HEIGHT = 193;
//          TF_OD_API_MODEL_FILE = "q32_193_deeplab.tflite";
//          TF_OD_API_INPUT_WIDTH = 257;
//          TF_OD_API_INPUT_HEIGHT = 257;
//          TF_OD_API_MODEL_FILE = "q32_257_deeplab.tflite";
      }
      else if( option == 1)
      {
          mSizeSkipX = 4;
          mSizeSkipY = 3;

          TF_OD_API_INPUT_WIDTH = 305;
          TF_OD_API_INPUT_HEIGHT = 305;
          TF_OD_API_MODEL_FILE = "q_305_deeplab.tflite";
      }
      else { //option == 2 for capture
          mSizeSkipX = 2;
          mSizeSkipY = 2;

          TF_OD_API_INPUT_WIDTH = 513;
          TF_OD_API_INPUT_HEIGHT = 513;
          TF_OD_API_MODEL_FILE = "noq_513_deeplab.tflite";
      }

//      mSizeSkipX = 4;
//      mSizeSkipY = 3;
	  //SpeedTest
      //mSizeSkipX = 2;
      //mSizeSkipY = 2;

    mAsset = asset;
    mContext = context;
    mPreviewWidth = width;
    mPreviewHeight = height;

    int cropHeight = TF_OD_API_INPUT_HEIGHT;
    int cropWidth = TF_OD_API_INPUT_WIDTH;

    mCropSize = cropWidth;

    rgbFrameBitmap = Bitmap.createBitmap(mPreviewWidth/mSizeSkipX, mPreviewHeight/mSizeSkipY, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Config.ARGB_8888);

//      frameToCropTransformBack0 = ImageUtils.getTransformationMatrix(
//              mPreviewWidth/mSizeSkipX, mPreviewHeight/mSizeSkipY,
//              cropWidth, cropHeight,
//              90, MAINTAIN_ASPECT);
//
//      cropToFrameTransformBack0 = new Matrix();
//      frameToCropTransformBack0.invert(cropToFrameTransformBack0);
//
//      frameToCropTransformFront0 = ImageUtils.getTransformationMatrix(
//            mPreviewWidth/mSizeSkipX, mPreviewHeight/mSizeSkipY,
//            cropWidth, cropHeight,
//            270, MAINTAIN_ASPECT);
//
//      cropToFrameTransformFront0 = new Matrix();
//      frameToCropTransformFront0.invert(cropToFrameTransformFront0);

    //mRgbInts = new int[mPreviewWidth * mPreviewHeight];
      mRgbInts = new int[mPreviewWidth/mSizeSkipX * mPreviewHeight/mSizeSkipY];

    mSegmentMat = new Mat(cropWidth, cropHeight, CvType.CV_32FC1);
    mSegmentResultMat = new Mat();
    mSegmentProccessMat = new Mat();
    mSegmentStudioMat = new Mat();
    mSegmentByteForSF = new byte[1440/aiCamParameters.RESIZE_FEATHER_FACTOR*1080/aiCamParameters.RESIZE_FEATHER_FACTOR*4];
    mSegmentByteForBlur = new byte[1440/aiCamParameters.RESIZE_BLUR_MASK_FACTOR*1080/aiCamParameters.RESIZE_BLUR_MASK_FACTOR*4];
    mSegmentByteForStudio = new byte[1440/aiCamParameters.RESIZE_BLUR_MASK_FACTOR*1080/aiCamParameters.RESIZE_BLUR_MASK_FACTOR*4];

    previewMat = new Mat();
    previewProcessMat = new Mat();

    startBackgroundThread();
  }

  public void creatModel()
  {
    try {
      segmentor =
              TFLiteObjectSegmentationAPIModel.create(
                      mAsset,
                      TF_OD_API_MODEL_FILE,
                      TF_OD_API_LABELS_FILE,
                      TF_OD_API_INPUT_WIDTH,
                      TF_OD_API_INPUT_HEIGHT,
                      TF_OD_API_NUM_CLASS,
                      TF_OD_API_NUM_OUTPUT);
    } catch (final IOException e) {

    }
  }

  public void setGlEngine(glEngine engine)
  {
      mGlEngine = engine;
  }

    public void setCameraLocation(int front)
    {
        Log.d("setCameraLocation", "setCameraLocation : "+front);

        mCameraLocation = front;

        if(front == 0) {//back camera
            mFrameToCropTransform0 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth/mSizeSkipX, mPreviewHeight/mSizeSkipY,
                            mCropSize, mCropSize,
                            90, MAINTAIN_ASPECT);
            mCropToFrameTransform0 = new Matrix();
            mFrameToCropTransform0.invert(mCropToFrameTransform0);

            mFrameToCropTransform90 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth/mSizeSkipX, mPreviewHeight/mSizeSkipY,
                            mCropSize, mCropSize,
                            180, MAINTAIN_ASPECT);
            mCropToFrameTransform90 = new Matrix();
            mFrameToCropTransform90.invert(mCropToFrameTransform90);

            mFrameToCropTransform180 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth/mSizeSkipX, mPreviewHeight/mSizeSkipY,
                            mCropSize, mCropSize,
                            270, MAINTAIN_ASPECT);
            mCropToFrameTransform180 = new Matrix();
            mFrameToCropTransform180.invert(mCropToFrameTransform180);

            mFrameToCropTransform270 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth/mSizeSkipX, mPreviewHeight/mSizeSkipY,
                            mCropSize, mCropSize,
                            0, MAINTAIN_ASPECT);
            mCropToFrameTransform270 = new Matrix();
            mFrameToCropTransform270.invert(mCropToFrameTransform270);
        }
        else
        {
            // 0 flip
            mFrameToCropTransform0 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth/mSizeSkipX, mPreviewHeight/mSizeSkipY,
                            mCropSize, mCropSize,
                            270, MAINTAIN_ASPECT);
            mCropToFrameTransform0 = new Matrix();
            mFrameToCropTransform0.invert(mCropToFrameTransform0);

            // 90 no filp
            mFrameToCropTransform90 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth/mSizeSkipX, mPreviewHeight/mSizeSkipY,
                            mCropSize, mCropSize,
                            180, MAINTAIN_ASPECT);
            mCropToFrameTransform90 = new Matrix();
            mFrameToCropTransform90.invert(mCropToFrameTransform90);

            //180 flip
            mFrameToCropTransform180 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth/mSizeSkipX, mPreviewHeight/mSizeSkipY,
                            mCropSize, mCropSize,
                            90, MAINTAIN_ASPECT);
            mCropToFrameTransform180 = new Matrix();
            mFrameToCropTransform180.invert(mCropToFrameTransform180);

            //270 filp
            mFrameToCropTransform270 =
                    ImageUtils.getTransformationMatrix(
                            mPreviewWidth/mSizeSkipX, mPreviewHeight/mSizeSkipY,
                            mCropSize, mCropSize,
                            0, MAINTAIN_ASPECT);
            mCropToFrameTransform270 = new Matrix();
            mFrameToCropTransform270.invert(mCropToFrameTransform270);
        }
    }

    //SpeedTest
    //private static void resizeByte(final byte[] yuvBytes)
	private void resizeByte(final byte[] yuvBytes)
    {
        int previewWidth = aiCamParameters.PREVIEW_WIDTH_I;
        int previewHeight = aiCamParameters.PREVIEW_HEIGHT_I;

        if (mYuvBytes == null) {
            mYuvBytes = new byte[yuvBytes.length];
        }

        if( mResizeYuvByte == null )
        {
            mResizeYuvByte = new byte[previewWidth * (previewHeight * 3 / 2) / (mSizeSkipX * mSizeSkipY)];
        }

//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {

                System.arraycopy(yuvBytes, 0, mYuvBytes, 0, yuvBytes.length);

                //test...
                int fillCount = 0;
                Date startTime = new Date();
                for(int i = 0; i < previewHeight * 3 / 2; i = i + mSizeSkipY)
                {
                    if( i < previewHeight ) {
                        for (int j = 0; j < previewWidth; j = j + mSizeSkipX) {
                            mResizeYuvByte[fillCount++] = mYuvBytes[i * previewWidth + j];
                        }
                    }
                    else
                    {
                        for (int j = 0; j < previewWidth; j = j + (mSizeSkipX * 2)) {
                            mResizeYuvByte[fillCount++] = mYuvBytes[i * previewWidth + j];
                            mResizeYuvByte[fillCount++] = mYuvBytes[i * previewWidth + j + 1];
                        }
                    }
                }
//            }
//        });
//        thread.start();
    }

    private static Mat equalizeIntensity(Mat inputImage)
    {
        if(inputImage.channels() >= 3)
        {
            Mat ycrcb = new Mat();

            cvtColor(inputImage, ycrcb, COLOR_RGBA2RGB);
            cvtColor(inputImage, ycrcb, COLOR_RGB2YCrCb);

            List<Mat> channels  = new ArrayList<>();
            split(ycrcb, channels);

//            imwrite("/sdcard/water/Y.jpg", channels.get(0));
//            imwrite("/sdcard/water/cb.jpg", channels.get(1));
//            imwrite("/sdcard/water/cr.jpg", channels.get(2));

            //equalizeHist(channels.get(0), channels.get(0));
            //Imgproc.adaptiveThreshold(channels.get(0), channels.get(0), 125, Imgproc.ADAPTIVE_THRESH_MEAN_C,
              //      Imgproc.THRESH_BINARY, 11, 12);
            threshold(channels.get(0), channels.get(0), 100, 0, THRESH_TOZERO);
            Mat hairMat = new Mat();
            threshold(channels.get(0), hairMat, 0, 255, THRESH_BINARY_INV);
            hairMat.copyTo(channels.get(0), hairMat);

            Mat result = new Mat();
            merge(channels, ycrcb);

            cvtColor(ycrcb, result, COLOR_YCrCb2RGB);
            cvtColor(inputImage, ycrcb, COLOR_RGB2RGBA);

            return result;
        }

        return new Mat();
    }

    public static int mContrastOnOff = 1;
    public static void onOffContrast()
    {
        mContrastOnOff = 1 - mContrastOnOff;
    }

   //SpeedTest
   //private static boolean mPreviewSetEnd = true;
   private boolean mPreviewSetEnd = true;
   
   //SpeedTest
  //public static void setPreviewData(final byte[] data, int skipX, int skipY)
  public void setPreviewData(final byte[] data)
  {
//      mSizeSkipX = skipX;
//      mSizeSkipY = skipY;

//      if( mPreviewSetEnd == false )
//      {
//          Log.d(TAG, "mPreviewSetEnd : "+mPreviewSetEnd);
//          return;
//      }

      mPreviewSetEnd = false;

    //use image Converter...
//    Thread thread = new Thread(new Runnable() {
//      @Override
//      public void run() {
        final long startTime = SystemClock.uptimeMillis();
        resizeByte(data);
        Log.k(TAG, "[time-check] yuv resize time " + (SystemClock.uptimeMillis()-startTime));

        final long startTime2 = SystemClock.uptimeMillis();
        //ImageUtils.convertYUV420SPToARGB8888(data, mPreviewWidth, mPreviewHeight, mRgbInts);
        ImageUtils.convertYUV420SPToARGB8888(mResizeYuvByte, 1440/mSizeSkipX, 1080/mSizeSkipY, mRgbInts);

        //synchronized (this) {
            rgbFrameBitmap.setPixels(mRgbInts, 0, mPreviewWidth / mSizeSkipX, 0, 0, mPreviewWidth / mSizeSkipX, mPreviewHeight / mSizeSkipY);
            Utils.bitmapToMat(rgbFrameBitmap, previewMat);
            //For Test
//            drawViewer.setAIPreviewBitmapBefore(previewMat);

            Log.d(TAG, "aiCamParameters.WAHT_GAMMA : " + gammaManager.getCurrentValue());
            previewMat = setGammaCorrection(previewMat, Math.round(gammaManager.getCurrentValue() * 10) / 10.0);

            //For Test
//            double contrastValue = 1.2;
//            drawViewer.setAIPreviewBitmapAfter(previewMat, contrastValue, mContrastOnOff);
//
//            if( mContrastOnOff == 1 ) {
//                //previewMat.convertTo(previewMat, -1, contrastValue, 0); //increase the contrast by 4
//                previewMat = equalizeIntensity(previewMat);
//                previewMat.convertTo(previewMat, -1, contrastValue, 0); //increase the contrast by 4
//            }
            Utils.matToBitmap(previewMat, rgbFrameBitmap);
        //}

            mPreviewSetEnd = true;
            //Log.d(TAG, "[time-check] yuv2rgb time " + (SystemClock.uptimeMillis()-startTime));
            Log.d(TAG, "[time-check] yuv2rgb time " + (SystemClock.uptimeMillis()-startTime)+", mOption : "+mOption);
//      }
//    });
//    thread.start();
  }

  public static void resetLastSegment()
  {
      mCanUseSegmentMat = false;
  }

  public static Mat getLastSegmentData()
  {
      Log.d(TAG, "mCanUseSegmentMat : "+mCanUseSegmentMat);

      if( mCanUseSegmentMat == true ) {
          return mCopySegmentMat;
      }
      return null;
  }

    public Bitmap blur(Bitmap image) {
        if (null == image) return null;
        Bitmap outputBitmap = Bitmap.createBitmap(image);
        final RenderScript renderScript = RenderScript.create(mContext);
        Allocation tmpIn = Allocation.createFromBitmap(renderScript, image);
        Allocation tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap);
        //Intrinsic Gausian blur filter
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        theIntrinsic.setRadius(7);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);
        return outputBitmap;
    }

    private static byte saturate(double val) {
        int iVal = (int) Math.round(val);
        iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
        return (byte) iVal;
    }

    private static Mat setGammaCorrection(Mat matImgSrc, double gammaValue)
    {
        Mat lookUpTable = new Mat(1, 256, CvType.CV_8U);
        byte[] lookUpTableData = new byte[(int) (lookUpTable.total()*lookUpTable.channels())];
        for (int i = 0; i < lookUpTable.cols(); i++) {
            lookUpTableData[i] = saturate(Math.pow(i / 255.0, 1.0 / gammaValue) * 255.0);
        }
        lookUpTable.put(0, 0, lookUpTableData);
        Mat img = new Mat();
        Core.LUT(matImgSrc, lookUpTable, img);

//        imwrite("/sdcard/obj/gammaB.jpg", matImgSrc);
//        imwrite("/sdcard/obj/gammaA.jpg", img);

        return img;
    }

  public boolean processImage(int rotate) {

    ++timestamp;
    final long currTimestamp = timestamp;

    Log.d(TAG, "processImage : "+computingSegmentation);

    mRotation = rotate;

    //if (computingSegmentation == false )
    Log.d(TAG, "[mSegmentDetector] computingSegmentation("+mOption+") : "+computingSegmentation);

    // No mutex needed as this method is not reentrant.
    if (computingSegmentation == true || segmentor == null) {
      Log.d(TAG, "processImaging...");
      return false;
    }
//      computingSegmentation = true;
//
//    Thread thread = new Thread(new Runnable() {
//      @Override
//      public void run() {
        Log.d(TAG, "Preparing image " + currTimestamp + " for segmention in bg thread.");

        final long startTime0 = SystemClock.uptimeMillis();
//        rgbFrameBitmap.setPixels(mRgbInts, 0, mPreviewWidth/mSizeSkipX, 0, 0, mPreviewWidth/mSizeSkipX, mPreviewHeight/mSizeSkipY);

//        Utils.bitmapToMat(rgbFrameBitmap, previewMat);
//        jniController.setPreviewData(previewMat.getNativeObjAddr());

//        Utils.bitmapToMat(rgbFrameBitmap, previewMat);
//        Log.o(TAG, "previewC : "+previewMat.channels());
//        imwrite("/sdcard/water/ori.jpg", previewMat);
//        Mat image = new Mat();
//        //Imgproc.GaussianBlur(previewMat, image, new Size(0, 0), 3);
//        //addWeighted(previewMat, 1.5, image, -0.5, 0, image);
//        Imgproc.Canny(previewMat,image, 100,200);
//        Imgproc.threshold(image, image, 100, 255, Imgproc.THRESH_BINARY);
//        Imgproc.cvtColor(image, image, Imgproc.COLOR_GRAY2BGRA);
//        image.copyTo(previewMat, image);
//        //addWeighted(previewMat, 0.5, image, 0.5, 0, image);
//
//        imwrite("/sdcard/water/ori2.jpg", previewMat);
//        Utils.matToBitmap(previewMat, rgbFrameBitmap);

      // For examining the actual TF input.
      if (SAVE_PREVIEW_BITMAP) {
          ImageUtils.saveBitmap(rgbFrameBitmap, "preview.png");
      }

      long startResizeTime = SystemClock.uptimeMillis();
      if (aiCamParameters.mInterpolation == 1) {
          //down-scale with interpolation (use opencv)
          Mat mat1 = new Mat();
          Utils.bitmapToMat(rgbFrameBitmap, mat1);
          Log.d(TAG, "SegmentResize Time1 : "+(SystemClock.uptimeMillis()-startResizeTime));

          if (rotate == 0) {
              rotate(mat1, mat1, ROTATE_90_COUNTERCLOCKWISE);
          } else if (rotate == 90) {
              rotate(mat1, mat1, ROTATE_180);
          } else if (rotate == 180) {
              rotate(mat1, mat1, ROTATE_90_CLOCKWISE);
          } else if (rotate == 270) {
          }

          Log.d(TAG, "SegmentResize Time2 : "+(SystemClock.uptimeMillis()-startResizeTime));
          resize(mat1, mat1, new Size(TF_OD_API_INPUT_WIDTH, TF_OD_API_INPUT_HEIGHT), 0, 0, INTER_AREA);
//          resize(mat1, mat1, new Size(TF_OD_API_INPUT_WIDTH, TF_OD_API_INPUT_HEIGHT), 0, 0, INTER_LANCZOS4); //downscale에서는 INTER_AREA가 더 나음
          Log.d(TAG, "SegmentResize Time3 : "+(SystemClock.uptimeMillis()-startResizeTime));

          //front
          if (mCameraLocation == 1) {
              flip(mat1, mat1, 1);
          }
          Utils.matToBitmap(mat1, croppedBitmap);
          Log.d(TAG, "SegmentResize Time4 : "+(SystemClock.uptimeMillis()-startResizeTime));
          Log.d(TAG, "Interpolation ON");
      } else {
          final Canvas canvas = new Canvas(croppedBitmap);

          synchronized (this) {

              if (rotate == 0) {
                  canvas.drawBitmap(rgbFrameBitmap, mFrameToCropTransform0, null);
              } else if (rotate == 90) {
                  canvas.drawBitmap(rgbFrameBitmap, mFrameToCropTransform90, null);
              } else if (rotate == 180) {
                  canvas.drawBitmap(rgbFrameBitmap, mFrameToCropTransform180, null);
              } else if (rotate == 270) {
                  canvas.drawBitmap(rgbFrameBitmap, mFrameToCropTransform270, null);
              }

              //front
              if (mCameraLocation == 1) {
                  Matrix m = new Matrix();
                  m.preScale(-1, 1);
                  croppedBitmap = Bitmap.createBitmap(croppedBitmap, 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight(), m, false);
              }
          }
      }
      Log.d(TAG, "SegmentResize Time : "+(SystemClock.uptimeMillis()-startResizeTime));

//        final long startTimeGamma = SystemClock.uptimeMillis();
//          //croppedBitmap = blur(croppedBitmap);
//        Utils.bitmapToMat(croppedBitmap, previewMat);
////        imwrite("/sdcard/obj/before.jpg", previewMat);
//        Log.d(TAG, "aiCamParameters.WAHT_GAMMA : "+aiCamParameters.WAHT_GAMMA);
//        previewMat = setGammaCorrection(previewMat, Math.round(aiCamParameters.WAHT_GAMMA*10)/10.0);
////        imwrite("/sdcard/obj/after.jpg", previewMat);
//        Utils.matToBitmap(previewMat, croppedBitmap);
//        Log.d(TAG, "[time-check] gamma time " + (SystemClock.uptimeMillis()-startTimeGamma));

        Log.k(TAG, "[time-check] resize time " + (SystemClock.uptimeMillis()-startTime0));

        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
          ImageUtils.saveBitmap(croppedBitmap, "preview2.png");
        }

        Log.d(TAG, "Running segmention on image " + currTimestamp);

        final long startTime = SystemClock.uptimeMillis();
        final Segmentor.Segmentation result = segmentor.segmentImage(croppedBitmap);
        //lastInferenceTimeMs = result.getInferenceTime();
        //lastNativeTimeMs = result.getNativeTime();
        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

        Log.o(TAG, "[time-check] lastProcessingTimeMs[" + mOption +"] :"+lastProcessingTimeMs);

//        Thread thread = new Thread(new Runnable() {
//          @Override
//          public void run() {
//            drawViewer.handleSegmentation(timestamp, result);
//          }
//        });
//        thread.start();

//        drawViewer.handleSegmentation(timestamp, result);

//        Thread thread2 = new Thread(new Runnable() {
//          @Override
//          public void run() {
//            mSegmentAreaF = result.getPixels();
//            mSegmentMat.put(0, 0, mSegmentAreaF);
//
//            jniController.setSegmentationData(mSegmentMat.getNativeObjAddr());
//
//            //Utils.bitmapToMat(rgbFrameBitmap, mOriginalRgbaMat);
//            //jniController.setSegmentationData(mSegmentMat.getNativeObjAddr(), mOriginalRgbaMat.getNativeObjAddr());
//
//            //imwrite("/sdcard/mSegmentMat.jpg", mSegmentMat.mul(mSegmentMat, 100));
////            try {
////              Thread.sleep(50);
////            } catch (InterruptedException e) {
////              e.printStackTrace();
////            }
//          }
//        });
//        thread2.start();

//          Thread thread2 = new Thread(new Runnable() {
//          @Override
//          public void run() {
          final long startTime2 = SystemClock.uptimeMillis();
          if( mOption == -1 ) {
              drawViewer.handleSegmentationLow(timestamp, result);
          }
          else if( mOption == 0 ) {
              drawViewer.handleSegmentationNormal(timestamp, result);
          }
          else if (mOption == 1) {
              drawViewer.handleSegmentationHigh(timestamp, result);
          }
          else { //mOption == 2)

          }
          Log.k(TAG, "[time-check] draw time " + (SystemClock.uptimeMillis()-startTime2));

            mOptionCopy = mOption;
          //if( mOption != -1 ) {
          if( true ) {

              final long startTime3 = SystemClock.uptimeMillis();
              //setSegmentTexture(result.getPixels());
              //mSegmnetFloat = result.getPixels();

              if (mDoneInference == false) {
                  Log.o(TAG, "[mSegmentDetector] not-yet-done : "+mOption);
//                  computingSegmentation = false;
                  return false;
              }

              mDoneInference = false;

              mSegmentMat.put(0, 0, result.getPixels());

              mCanUseSegmentMat = false;

              if( mCopySegmentMat.empty() == true || countNonZero(mSegmentMat) != 0) {
                  mCopySegmentMat = mSegmentMat.clone();
              }
              else
              {
                  Log.d(TAG, "[PersonTest] countNonZero(mSegmentMat) : "+countNonZero(mSegmentMat));
              }

              mCanUseSegmentMat = true;

              if( mOption ==  0 ) {
                  mPersonCheckMat = mSegmentMat.clone();
              }

              backgroundHandler.post(periodicClassify);
              Log.k(TAG, "[time-check] feather time " + (SystemClock.uptimeMillis() - startTime3));
          }
          else
          {
              mSegmentMat.put(0, 0, result.getPixels());
              mPersonCheckMat = mSegmentMat.clone();
          }
//          }
//        });
//        thread2.start();

//        mSegmentAreaF = result.getPixels();
//        mSegmentMat.put(0, 0, mSegmentAreaF);
//        jniController.setSegmentationData(mSegmentMat.getNativeObjAddr());

//        computingSegmentation = false;
//      }
//    });
//    thread.setPriority(10);
//    thread.start();

    return true;
  }

    Mat setTranslation(Mat input, float targetX, float targetY)
    {
        Mat output = new Mat();

        final float []transValue = {1.0f, 0.0f, targetX, 0.0f, 1.0f, targetY};//구체적으로 초기화
        final ByteBuffer transBuffer = ByteBuffer.allocateDirect(transValue.length * 4);
        transBuffer.order(ByteOrder.nativeOrder());

        for(int i = 0; i < transValue.length; i++)
        {
            transBuffer.putFloat(transValue[i]);
        }

        Mat transMat = new Mat(2, 3, CV_32F, transBuffer);
        warpAffine(input, output, transMat, new Size(input.cols(), input.rows()), INTER_NEAREST);

        return output;
    }

    Mat setTranslation2(Mat input, int upDown, int leftRight)
    {
        //JJabu
        Mat output = new Mat();

        Mat maskUpDownSize = getStructuringElement(MORPH_CROSS, new Size(3, 1), new Point(-1, -1));
        if( upDown < 0 ) {
            erode(input, output, maskUpDownSize, new Point(-1, -1), -upDown);
        }
        else
        {
            dilate(input, output, maskUpDownSize, new Point(-1, -1), upDown);
        }

        Mat maskLeftRightSize = getStructuringElement(MORPH_CROSS, new Size(1, 3), new Point(-1, -1));
        if( leftRight < 0 ) {
            erode(input, output, maskLeftRightSize, new Point(-1, -1), -upDown);
        }
        else
        {
            dilate(input, output, maskLeftRightSize, new Point(-1, -1), upDown);
        }

        return output;
    }

  //private void setSegmentTexture(float[] data)
  private void setSlowSegmentTexture()
  {
      final long startTime = SystemClock.uptimeMillis();

      //Log.d(TAG, "data size : "+data.length);

      //mSegmentMat.put(0, 0, data);
      //mSegmentMat.put(0, 0, mSegmnetFloat);
      mSegmentMat.convertTo(mSegmentProccessMat, CvType.CV_8UC1);
      Mat matRotation = Imgproc.getRotationMatrix2D(new Point(mSegmentProccessMat.cols() / 2, mSegmentProccessMat.rows() / 2), 90, 1);
      warpAffine(mSegmentProccessMat, mSegmentProccessMat, matRotation, mSegmentProccessMat.size(), INTER_NEAREST);

      resize(mSegmentProccessMat, mSegmentProccessMat, new Size(1440/SFTunner.mSlowFeatherTune.mResizeXY, 1080/SFTunner.mSlowFeatherTune.mResizeXY), 0, 0, INTER_NEAREST);

      Imgproc.threshold(mSegmentProccessMat, mSegmentProccessMat, 15, 0, THRESH_TOZERO_INV);
      Imgproc.threshold(mSegmentProccessMat, mSegmentProccessMat, 14, 255, Imgproc.THRESH_BINARY);

      //will delete
      int faderStart = 1;
      if( faderStart < 0)
      {
          erode(mSegmentProccessMat, mSegmentProccessMat, new Mat(), new Point(-1, -1), Math.abs(faderStart));
      }
      else
      {
          dilate(mSegmentProccessMat, mSegmentProccessMat, new Mat(), new Point(-1, -1), Math.abs(faderStart));
      }

      mSegmentProccessMat = setTranslation(mSegmentProccessMat, SFTunner.mSlowFeatherTune.mTransUD, -SFTunner.mSlowFeatherTune.mTransLR);
      //imwrite("/sdcard/test/trans.jpg", mSegmentProccessMat);
      //mSegmentProccessMat = setTranslation2(mSegmentProccessMat, (int)SFTunner.mSlowDataFloat[3], (int)SFTunner.mSlowDataFloat[4]);

      Imgproc.GaussianBlur(mSegmentProccessMat, mSegmentProccessMat, new Size(SFTunner.mSlowFeatherTune.mCartoonThickness, SFTunner.mSlowFeatherTune.mCartoonThickness), 11.0);
      //Imgproc.erode(mSegmentProccessMat, mSegmentProccessMat, new Mat(), new Point(-1, -1), 7);
//      for( int i = 0; i < 50; i++ )
//      {
//          //Imgproc.blur(mSegmentProccessMat, mSegmentProccessMat, new Size(3, 3));
//          Imgproc.GaussianBlur(mSegmentProccessMat, mSegmentProccessMat, new Size(3, 3), 11.0);
//      }

      Imgproc.cvtColor(mSegmentProccessMat, mSegmentProccessMat, Imgproc.COLOR_GRAY2BGRA);
      resize(mSegmentProccessMat, mSegmentProccessMat, new Size(1440, 1080), 0, 0, INTER_NEAREST);
      mSegmentProccessMat.get(0, 0, mSegmentByteForSF);

      if( mGlEngine != null ) {
          mGlEngine.setMaskTextureForSF(mSegmentByteForSF, false, 2);
          mGlEngine.setMovingIndex(1);
      }

      Log.o(TAG, "[time-check] Feather time " + (SystemClock.uptimeMillis()-startTime));
  }

    private void setFastSegmentTexture()
    {

        if( SFTunner.mFastFeatherTune.mResizeXY == 0 || countNonZero(mSegmentMat) == 0 ) return;

        final long startTime = SystemClock.uptimeMillis();

        //Log.d(TAG, "data size : "+data.length);

        //mSegmentMat.put(0, 0, data);
        //mSegmentMat.put(0, 0, mSegmnetFloat);
        mSegmentMat.convertTo(mSegmentProccessMat, CvType.CV_8UC1);
        Mat matRotation = Imgproc.getRotationMatrix2D(new Point(mSegmentProccessMat.cols() / 2, mSegmentProccessMat.rows() / 2), mRotation+90, 1);
        warpAffine(mSegmentProccessMat, mSegmentProccessMat, matRotation, mSegmentProccessMat.size(), INTER_NEAREST);

        Log.o(TAG, "[time-check-w] rot time " + (SystemClock.uptimeMillis()-startTime));

        Imgproc.resize(mSegmentProccessMat, mSegmentProccessMat, new Size(1440/(int)SFTunner.mFastFeatherTune.mResizeXY  , 1080/(int)SFTunner.mFastFeatherTune.mResizeXY), 0, 0, INTER_NEAREST);

        Imgproc.threshold(mSegmentProccessMat, mSegmentProccessMat, 15, 0, THRESH_TOZERO_INV);
        Imgproc.threshold(mSegmentProccessMat, mSegmentProccessMat, 14, 255, Imgproc.THRESH_BINARY);

        Log.o(TAG, "[time-check-w] resize time " + (SystemClock.uptimeMillis()-startTime));

        int faderStart = (int)SFTunner.mFastFeatherTune.mScaleXcartoon;
        if( faderStart < 0)
        {
            erode(mSegmentProccessMat, mSegmentProccessMat, new Mat(), new Point(-1, -1), Math.abs(faderStart));
        }
        else
        {
            dilate(mSegmentProccessMat, mSegmentProccessMat, new Mat(), new Point(-1, -1), Math.abs(faderStart));
        }

        Log.o(TAG, "[time-check-w] erode-dilate time " + (SystemClock.uptimeMillis()-startTime));

        if( mRotation == 0 ) {
            //JJabu
            int scaleLR = (int)SFTunner.mFastFeatherTune.mScaleYcartoon;
            Mat maskErodeSize = getStructuringElement(MORPH_CROSS, new Size(1, 5), new Point(-1, -1));
            if( scaleLR > 0 ) {
                erode(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), scaleLR);
            }
            else {
                dilate(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), -scaleLR);
            }

            mSegmentProccessMat = setTranslation(mSegmentProccessMat, SFTunner.mFastFeatherTune.mTransUD, -SFTunner.mFastFeatherTune.mTransLR);
        }
        else if( mRotation == 90 ) {
            //JJabu
            int scaleLR = (int)SFTunner.mFastFeatherTune.mScaleYcartoon;
            Mat maskErodeSize = getStructuringElement(MORPH_CROSS, new Size(5, 1), new Point(-1, -1));
            if( scaleLR > 0 ) {
                erode(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), scaleLR);
            }
            else {
                dilate(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), -scaleLR);
            }

            mSegmentProccessMat = setTranslation(mSegmentProccessMat, -SFTunner.mFastFeatherTune.mTransLR, -SFTunner.mFastFeatherTune.mTransUD);
        }
        else if( mRotation == 180 ) {
            //JJabu
            int scaleLR = (int)SFTunner.mFastFeatherTune.mScaleYcartoon;
            Mat maskErodeSize = getStructuringElement(MORPH_CROSS, new Size(1, 5), new Point(-1, -1));
            if( scaleLR > 0 ) {
                erode(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), scaleLR);
            }
            else {
                dilate(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), -scaleLR);
            }

            mSegmentProccessMat = setTranslation(mSegmentProccessMat, -SFTunner.mFastFeatherTune.mTransUD, SFTunner.mFastFeatherTune.mTransLR);
        }
        else if( mRotation == 270 ) {
            //JJabu
            int scaleLR = (int)SFTunner.mFastFeatherTune.mScaleYcartoon;
            Mat maskErodeSize = getStructuringElement(MORPH_CROSS, new Size(5, 1), new Point(-1, -1));
            if( scaleLR > 0 ) {
                erode(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), scaleLR);
            }
            else {
                dilate(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), -scaleLR);
            }

            mSegmentProccessMat = setTranslation(mSegmentProccessMat, SFTunner.mFastFeatherTune.mTransLR, SFTunner.mFastFeatherTune.mTransUD);
        }

        Log.o(TAG, "[time-check-w] setTranslation time " + (SystemClock.uptimeMillis()-startTime));

        //Imgproc.GaussianBlur(mSegmentProccessMat, mSegmentProccessMat, new Size(53, 53), 11.0);
        //Imgproc.GaussianBlur(mSegmentProccessMat, mSegmentProccessMat, new Size(17, 17), 11.0);
//        Imgproc.GaussianBlur(mSegmentProccessMat, mSegmentProccessMat, new Size(11/*SFTunner.mFastFeatherTune.mNormalFthickness*/, 11/*SFTunner.mFastFeatherTune.mNormalFthickness*/), 11.0);
        //Imgproc.erode(mSegmentProccessMat, mSegmentProccessMat, new Mat(), new Point(-1, -1), 7);
//      for( int i = 0; i < 50; i++ )
//      {
//          //Imgproc.blur(mSegmentProccessMat, mSegmentProccessMat, new Size(3, 3));
//          Imgproc.GaussianBlur(mSegmentProccessMat, mSegmentProccessMat, new Size(3, 3), 11.0);
//      }

        Imgproc.cvtColor(mSegmentProccessMat, mSegmentProccessMat, Imgproc.COLOR_GRAY2BGRA);
        Imgproc.resize(mSegmentProccessMat, mSegmentProccessMat, new Size(1440/aiCamParameters.RESIZE_FEATHER_FACTOR, 1080/aiCamParameters.RESIZE_FEATHER_FACTOR), 0, 0, INTER_NEAREST);
        //mSegmentProccessMat.get(0, 0, mSegmentByte);

//        final long startTime = SystemClock.uptimeMillis();
//
//        mSegmentMat.convertTo(mSegmentProccessMat, CvType.CV_8UC1);
//        Mat matRotation = Imgproc.getRotationMatrix2D(new Point(mSegmentProccessMat.cols() / 2, mSegmentProccessMat.rows() / 2), mRotation+90, 1);
//        warpAffine(mSegmentProccessMat, mSegmentProccessMat, matRotation, mSegmentProccessMat.size(), INTER_NEAREST);
//        Imgproc.threshold(mSegmentProccessMat, mSegmentProccessMat, 15, 0, Imgproc.THRESH_TOZERO_INV);
//        Imgproc.threshold(mSegmentProccessMat, mSegmentProccessMat, 14, 255, Imgproc.THRESH_BINARY);
//
//        Imgproc.cvtColor(mSegmentProccessMat, mSegmentProccessMat, Imgproc.COLOR_GRAY2BGRA);
//        Imgproc.resize(mSegmentProccessMat, mSegmentProccessMat, new Size(1440/aiCamParameters.RESIZE_FEATHER_FACTOR, 1080/aiCamParameters.RESIZE_FEATHER_FACTOR), 0, 0, INTER_NEAREST);


        if( glEngine.getSFCamMode() == aiCamParameters.SF_MODE || glEngine.getSFCamMode() == aiCamParameters.CARTOON_MODE ) {
            mSegmentProccessMat.get(0, 0, mSegmentByteForSF);
            if (mGlEngine != null) {
                mGlEngine.setMaskTextureForSF(mSegmentByteForSF, false, 1);
                mGlEngine.setMovingIndex(2);
            }
        }
        else if( glEngine.getSFCamMode() == aiCamParameters.OF_MODE || glEngine.getSFCamMode() == aiCamParameters.HIGHLIGHT_MODE )
        {
            resize(mSegmentProccessMat, mSegmentProccessMat,
                    new Size(aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_BLUR_MASK_FACTOR,
                            aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_BLUR_MASK_FACTOR), 0, 0, INTER_NEAREST);
            if( aiCamParameters.mCameraLocationInt == 1 ) {
                flip(mSegmentProccessMat, mSegmentProccessMat, 90);
            }
            cvtColor(mSegmentProccessMat, mSegmentProccessMat, COLOR_RGBA2BGRA);
            Imgproc.threshold(mSegmentProccessMat, mSegmentProccessMat, 200, 255, Imgproc.THRESH_BINARY_INV);

            mSegmentProccessMat.get(0, 0, mSegmentByteForBlur);
            if( mGlEngine != null ) {
                mGlEngine.setMaskTextureForBlur(mSegmentByteForBlur, false, 1);
            }

            if( glEngine.getSFCamMode() == aiCamParameters.HIGHLIGHT_MODE )
            {
                Mat temp = new Mat();
                Imgproc.threshold(mSegmentProccessMat, temp, 200, 255, Imgproc.THRESH_BINARY_INV);

                jniController.setStudioData(temp.getNativeObjAddr(), mSegmentStudioMat.getNativeObjAddr(), 1);
                if( aiCamParameters.mCameraLocationInt == 0 ) {
                    flip(mSegmentStudioMat, mSegmentStudioMat, 90);
                }
                mSegmentStudioMat.get(0, 0, mSegmentByteForStudio);

                if (mGlEngine != null) {
                    mGlEngine.setMaskTextureForStudio(mSegmentByteForStudio, false, 2);
                }
            }
        }

        Log.o(TAG, "[time-check-w] Feather time " + (SystemClock.uptimeMillis()-startTime));
    }

    private void setSuperFastSegmentTexture()
    {
        Log.d(TAG, "[whyk] size : "+SFTunner.mSuperFastFeatherTune.mResizeXY);
        Log.d(TAG, "[whyk] noZero : "+countNonZero(mSegmentMat));

        if( SFTunner.mSuperFastFeatherTune.mResizeXY == 0 || countNonZero(mSegmentMat) == 0 ) return;

        final long startTime = SystemClock.uptimeMillis();

        //Log.d(TAG, "data size : "+data.length);

        //mSegmentMat.put(0, 0, data);
        //mSegmentMat.put(0, 0, mSegmnetFloat);
        mSegmentMat.convertTo(mSegmentProccessMat, CvType.CV_8UC1);
        Mat matRotation = Imgproc.getRotationMatrix2D(new Point(mSegmentProccessMat.cols() / 2, mSegmentProccessMat.rows() / 2), mRotation+90, 1);
        warpAffine(mSegmentProccessMat, mSegmentProccessMat, matRotation, mSegmentProccessMat.size(), INTER_NEAREST);

        Log.o(TAG, "[time-check-w] rot time " + (SystemClock.uptimeMillis()-startTime));

        Imgproc.resize(mSegmentProccessMat, mSegmentProccessMat, new Size(1440/(int)SFTunner.mSuperFastFeatherTune.mResizeXY  , 1080/(int)SFTunner.mSuperFastFeatherTune.mResizeXY), 0, 0, INTER_NEAREST);

        Imgproc.threshold(mSegmentProccessMat, mSegmentProccessMat, 15, 0, THRESH_TOZERO_INV);
        Imgproc.threshold(mSegmentProccessMat, mSegmentProccessMat, 14, 255, Imgproc.THRESH_BINARY);

//        dilate(mSegmentProccessMat, mSegmentProccessMat, new Mat(), new Point(-1, -1), 7);
//        erode(mSegmentProccessMat, mSegmentProccessMat, new Mat(), new Point(-1, -1), 7);

//        final long startTimeSpeed = SystemClock.uptimeMillis();
//        if( copySlowSegment != null ) {
//            jniController.getSegmentForSpeed(mSegmentProccessMat.getNativeObjAddr(), mSegmentProccessMat.getNativeObjAddr(), copySlowSegment.getNativeObjAddr());
//        }
//        Log.o(TAG, "[time-check-w] startTimeSpeed : " + (SystemClock.uptimeMillis()-startTimeSpeed));

        Log.o(TAG, "[time-check-w] resize time " + (SystemClock.uptimeMillis()-startTime));

        int faderStart = (int)SFTunner.mSuperFastFeatherTune.mScaleXcartoon;
        //faderStart = 0;
        if( faderStart < 0)
        {
            erode(mSegmentProccessMat, mSegmentProccessMat, new Mat(), new Point(-1, -1), Math.abs(faderStart));
        }
        else
        {
            dilate(mSegmentProccessMat, mSegmentProccessMat, new Mat(), new Point(-1, -1), Math.abs(faderStart));
        }

        Log.o(TAG, "[time-check-w] erode-dilate time " + (SystemClock.uptimeMillis()-startTime));

        if( mRotation == 0 ) {
            //JJabu
            int scaleLR = (int)SFTunner.mSuperFastFeatherTune.mScaleYcartoon;
            Mat maskErodeSize = getStructuringElement(MORPH_CROSS, new Size(1, 5), new Point(-1, -1));
            if( scaleLR > 0 ) {
                erode(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), scaleLR);
            }
            else {
                dilate(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), -scaleLR);
            }

            mSegmentProccessMat = setTranslation(mSegmentProccessMat, SFTunner.mSuperFastFeatherTune.mTransUD, -SFTunner.mSuperFastFeatherTune.mTransLR);
        }
        else if( mRotation == 90 ) {
            //JJabu
            int scaleLR = (int)SFTunner.mSuperFastFeatherTune.mScaleYcartoon;
            Mat maskErodeSize = getStructuringElement(MORPH_CROSS, new Size(5, 1), new Point(-1, -1));
            if( scaleLR > 0 ) {
                erode(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), scaleLR);
            }
            else {
                dilate(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), -scaleLR);
            }

            mSegmentProccessMat = setTranslation(mSegmentProccessMat, -SFTunner.mSuperFastFeatherTune.mTransLR, -SFTunner.mSuperFastFeatherTune.mTransUD);
        }
        else if( mRotation == 180 ) {
            //JJabu
            int scaleLR = (int)SFTunner.mSuperFastFeatherTune.mScaleYcartoon;
            Mat maskErodeSize = getStructuringElement(MORPH_CROSS, new Size(1, 5), new Point(-1, -1));
            if( scaleLR > 0 ) {
                erode(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), scaleLR);
            }
            else {
                dilate(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), -scaleLR);
            }

            mSegmentProccessMat = setTranslation(mSegmentProccessMat, -SFTunner.mSuperFastFeatherTune.mTransUD, SFTunner.mSuperFastFeatherTune.mTransLR);
        }
        else if( mRotation == 270 ) {
            //JJabu
            int scaleLR = (int)SFTunner.mSuperFastFeatherTune.mScaleYcartoon;
            Mat maskErodeSize = getStructuringElement(MORPH_CROSS, new Size(5, 1), new Point(-1, -1));
            if( scaleLR > 0 ) {
                erode(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), scaleLR);
            }
            else {
                dilate(mSegmentProccessMat, mSegmentProccessMat, maskErodeSize, new Point(-1, -1), -scaleLR);
            }

            mSegmentProccessMat = setTranslation(mSegmentProccessMat, SFTunner.mSuperFastFeatherTune.mTransLR, SFTunner.mSuperFastFeatherTune.mTransUD);
        }

        Log.o(TAG, "[time-check-w] setTranslation time " + (SystemClock.uptimeMillis()-startTime));

        //Imgproc.GaussianBlur(mSegmentProccessMat, mSegmentProccessMat, new Size(53, 53), 11.0);
        //Imgproc.GaussianBlur(mSegmentProccessMat, mSegmentProccessMat, new Size(17, 17), 11.0);
//        Imgproc.GaussianBlur(mSegmentProccessMat, mSegmentProccessMat, new Size(11/*SFTunner.mSuperFastFeatherTune.mNormalFthickness*/, 11/*SFTunner.mSuperFastFeatherTune.mNormalFthickness*/), 11.0);
        //Imgproc.erode(mSegmentProccessMat, mSegmentProccessMat, new Mat(), new Point(-1, -1), 7);
//      for( int i = 0; i < 50; i++ )
//      {
//          //Imgproc.blur(mSegmentProccessMat, mSegmentProccessMat, new Size(3, 3));
//          Imgproc.GaussianBlur(mSegmentProccessMat, mSegmentProccessMat, new Size(3, 3), 11.0);
//      }

        Imgproc.cvtColor(mSegmentProccessMat, mSegmentProccessMat, Imgproc.COLOR_GRAY2BGRA);
        Imgproc.resize(mSegmentProccessMat, mSegmentProccessMat, new Size(1440/aiCamParameters.RESIZE_FEATHER_FACTOR, 1080/aiCamParameters.RESIZE_FEATHER_FACTOR), 0, 0, INTER_NEAREST);
        //mSegmentProccessMat.get(0, 0, mSegmentByte);

//        final long startTime = SystemClock.uptimeMillis();
//
//        mSegmentMat.convertTo(mSegmentProccessMat, CvType.CV_8UC1);
//        Mat matRotation = Imgproc.getRotationMatrix2D(new Point(mSegmentProccessMat.cols() / 2, mSegmentProccessMat.rows() / 2), mRotation+90, 1);
//        warpAffine(mSegmentProccessMat, mSegmentProccessMat, matRotation, mSegmentProccessMat.size(), INTER_NEAREST);
//        Imgproc.threshold(mSegmentProccessMat, mSegmentProccessMat, 15, 0, Imgproc.THRESH_TOZERO_INV);
//        Imgproc.threshold(mSegmentProccessMat, mSegmentProccessMat, 14, 255, Imgproc.THRESH_BINARY);
//
//        Imgproc.cvtColor(mSegmentProccessMat, mSegmentProccessMat, Imgproc.COLOR_GRAY2BGRA);
//        Imgproc.resize(mSegmentProccessMat, mSegmentProccessMat, new Size(1440/aiCamParameters.RESIZE_FEATHER_FACTOR, 1080/aiCamParameters.RESIZE_FEATHER_FACTOR), 0, 0, INTER_NEAREST);


        if( glEngine.getSFCamMode() == aiCamParameters.SF_MODE || glEngine.getSFCamMode() == aiCamParameters.CARTOON_MODE ) {
            mSegmentProccessMat.get(0, 0, mSegmentByteForSF);
            if (mGlEngine != null) {
                mGlEngine.setMaskTextureForSF(mSegmentByteForSF, false, 0);
                mGlEngine.setMovingIndex(3);
            }
        }
        else if( glEngine.getSFCamMode() == aiCamParameters.OF_MODE || glEngine.getSFCamMode() == aiCamParameters.HIGHLIGHT_MODE )
        {
            resize(mSegmentProccessMat, mSegmentProccessMat,
                    new Size(aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_BLUR_MASK_FACTOR,
                            aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_BLUR_MASK_FACTOR), 0, 0, INTER_NEAREST);
            if( aiCamParameters.mCameraLocationInt == 1 ) {
                flip(mSegmentProccessMat, mSegmentProccessMat, 90);
            }
            cvtColor(mSegmentProccessMat, mSegmentProccessMat, COLOR_RGBA2BGRA);
            Imgproc.threshold(mSegmentProccessMat, mSegmentProccessMat, 200, 255, Imgproc.THRESH_BINARY_INV);

            mSegmentProccessMat.get(0, 0, mSegmentByteForBlur);
            if( mGlEngine != null ) {
                mGlEngine.setMaskTextureForBlur(mSegmentByteForBlur, false, 0);
            }

            if( glEngine.getSFCamMode() == aiCamParameters.HIGHLIGHT_MODE )
            {
                Mat temp = new Mat();
                Imgproc.threshold(mSegmentProccessMat, temp, 200, 255, Imgproc.THRESH_BINARY_INV);

                jniController.setStudioData(temp.getNativeObjAddr(), mSegmentStudioMat.getNativeObjAddr(), 1);
                if( aiCamParameters.mCameraLocationInt == 0 ) {
                    flip(mSegmentStudioMat, mSegmentStudioMat, 90);
                }
                mSegmentStudioMat.get(0, 0, mSegmentByteForStudio);

                if (mGlEngine != null) {
                    mGlEngine.setMaskTextureForStudio(mSegmentByteForStudio, false, 2);
                }
            }
        }

        Log.o(TAG, "[time-check-w] Feather time " + (SystemClock.uptimeMillis()-startTime));
    }

    private final Object lock = new Object();
    private boolean runClassifier = false;

    private static final String HANDLE_THREAD_NAME = "CameraBackground";

    /** An additional thread for running tasks that shouldn't block the UI. */
    private HandlerThread backgroundThread;

    /** A {@link Handler} for running tasks in the background. */
    private Handler backgroundHandler;

    /** Starts a background thread and its {@link Handler}. */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        // Start the classification train & load an initial model.
        synchronized (lock) {
            runClassifier = true;
        }
        //backgroundHandler.post(periodicClassify);
    }

    /** Stops the background thread and its {@link Handler}. */
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
            synchronized (lock) {
                runClassifier = false;
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted when stopping background thread", e);
        }
    }

    /** Takes photos and classify them periodically. */
    private Runnable periodicClassify =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        if (runClassifier) {

                            if( mOption == -1 )
                            {
                                Log.d(TAG, "[problem] setSuperFastSegmentTexture");

                                setSuperFastSegmentTexture();

//                                Utils.bitmapToMat(rgbFrameBitmap, previewMat);
//                                jniController.setSegmentationData(previewMat.getNativeObjAddr(), mSegmentMat.getNativeObjAddr(), mSegmentResultMat.getNativeObjAddr(), 1);
//                                mSegmentResultMat.get(0, 0, mSegmentByte);
//                                if( mGlEngine != null ) {
//                                    mGlEngine.setMaskTexture(mSegmentByte);
//                                    mGlEngine.setMovingIndex(3);
//                                }
                            }
                            else if( mOption == 0 )
                            {
                                Log.d(TAG, "[problem] setFastSegmentTexture");

                                setFastSegmentTexture();

//                                Utils.bitmapToMat(rgbFrameBitmap, previewMat);
//                                jniController.setSegmentationData(previewMat.getNativeObjAddr(), mSegmentMat.getNativeObjAddr(), mSegmentResultMat.getNativeObjAddr(), 1);
//                                mSegmentResultMat.get(0, 0, mSegmentByte);
//                                if( mGlEngine != null ) {
//                                    mGlEngine.setMaskTexture(mSegmentByte);
//                                    mGlEngine.setMovingIndex(2);
//                                }
                            }
                            else {

                                Log.d(TAG, "[problem] setSlowSegmentTexture");

//                                setSlowSegmentTexture();

                                Utils.bitmapToMat(rgbFrameBitmap, previewProcessMat);
                                jniController.setSegmentationData(previewProcessMat.getNativeObjAddr(), mSegmentMat.getNativeObjAddr(), mSegmentResultMat.getNativeObjAddr(), mSegmentStudioMat.getNativeObjAddr(), 0, mRotation);

                                copySlowSegment = mSegmentResultMat.clone();

                                if( glEngine.getSFCamMode() == aiCamParameters.SF_MODE || glEngine.getSFCamMode() == aiCamParameters.CARTOON_MODE ) {

                                    resize(mSegmentResultMat, mSegmentResultMat,
                                            new Size(aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_FEATHER_FACTOR,
                                                    aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_FEATHER_FACTOR), 0, 0, INTER_NEAREST);

//                                    if( aiCamParameters.mCameraLocationInt == 0 ) {
//                                        flip(mSegmentResultMat, mSegmentResultMat, 90);
//                                    }
                                    mSegmentResultMat.get(0, 0, mSegmentByteForSF);

                                    if (mGlEngine != null) {
                                        mGlEngine.setMaskTextureForSF(mSegmentByteForSF, false, 2);
                                        mGlEngine.setMovingIndex(1);
                                    }
                                }
                                else if( glEngine.getSFCamMode() == aiCamParameters.OF_MODE || glEngine.getSFCamMode() == aiCamParameters.HIGHLIGHT_MODE )
                                {
                                    resize(mSegmentResultMat, mSegmentResultMat,
                                            new Size(aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_BLUR_MASK_FACTOR,
                                                    aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_BLUR_MASK_FACTOR), 0, 0, INTER_NEAREST);
                                    if( aiCamParameters.mCameraLocationInt == 1 ) {
                                        flip(mSegmentResultMat, mSegmentResultMat, 90);
                                    }
                                    cvtColor(mSegmentResultMat, mSegmentResultMat, COLOR_RGBA2BGRA);
                                    Imgproc.threshold(mSegmentResultMat, mSegmentResultMat, 200, 255, Imgproc.THRESH_BINARY_INV);

                                    mSegmentResultMat.get(0, 0, mSegmentByteForBlur);
                                    if( mGlEngine != null ) {
                                        mGlEngine.setMaskTextureForBlur(mSegmentByteForBlur, false, 2);
                                    }

                                    if( glEngine.getSFCamMode() == aiCamParameters.HIGHLIGHT_MODE ) {
//                                        if( aiCamParameters.mCameraLocationInt == 0 ) {
//                                            flip(mSegmentStudioMat, mSegmentStudioMat, 90);
//                                        }
                                        mSegmentStudioMat.get(0, 0, mSegmentByteForStudio);

                                        if (mGlEngine != null) {
                                            mGlEngine.setMaskTextureForStudio(mSegmentByteForStudio, false, 2);
                                        }
                                    }
                                }

                                //sally-capture
//                                glEngine에 capture하라고 알려준다.
                                if(mOption == 2)  //capture mode
                                {
                                    mGlEngine.StartCaptureGL();
                                }
                            }

                            mDoneInference = true;
                        }
                    }
                    //backgroundHandler.post(periodicClassify);
                }
            };

    public static boolean isHumanOn()
    {
        if( mPersonCheckMat.empty() == true )
        {
            mCurrentHumanOn = false;
            return false;
        }

        Mat humanCheckMat = mPersonCheckMat.clone();
        Imgproc.threshold(humanCheckMat, humanCheckMat, 15, 0, THRESH_TOZERO_INV);
        Imgproc.threshold(humanCheckMat, humanCheckMat, 14, 255, Imgproc.THRESH_BINARY);
        int noZeroCount = countNonZero(humanCheckMat);
        Log.d(TAG, "isHumanOn : "+noZeroCount+", mOption : "+mOptionCopy);

        if( noZeroCount == 0 )
        {
            mNoHumanCount++;
            Log.d(TAG, "[PersonTest] mNoHumanCount : "+mNoHumanCount);
            if( mNoHumanCount > 5 )
            {
                Log.d(TAG, "[PersonTest] Perfect no Human");
                mNoHumanCount = 0;

                mCurrentHumanOn = false;
                return false;
            }
            else
            {
                mCurrentHumanOn = true;
                return true;
            }
        }
        else
        {
            mNoHumanCount = 0;

            float segmentRate = noZeroCount * 100.f / (humanCheckMat.width() * humanCheckMat.height());
            Log.d(TAG, "[PersonTest] segmentRate : "+segmentRate);

            if( segmentRate > 0.f ) {
            //if( segmentRate > 10.f ) {
                mCurrentHumanOn = true;
                return true;
            }
            else{
                mCurrentHumanOn = false;
                return false;
            }
        }
    }
}
