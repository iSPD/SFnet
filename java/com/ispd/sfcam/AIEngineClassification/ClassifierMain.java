/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
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

package com.ispd.sfcam.AIEngineClassification;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import com.ispd.sfcam.AIEngineObjDetection.env.ImageUtils;
import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.gammaManager;

import org.opencv.android.Utils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class ClassifierMain {

  private String TAG = "ClassifierMain";

  private static final boolean MAINTAIN_ASPECT = true;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final float TEXT_SIZE_DIP = 10;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;
  private long lastProcessingTimeMs;
  private Integer sensorOrientation;
  private Classifier classifier;
  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;
  //private BorderedText borderedText;

  private int previewWidth;
  private int previewHeight;

  byte[] mYuvBytes;
  byte[] mResizeYuvByte;
  private int[] mRgbInts = null;

  int mSizeSkipX = 6;
  int mSizeSkipY = 5;

  public ClassifierMain(Activity activity, int width, int height, int rotation) {

    Classifier.Model model = Classifier.Model.QUANTIZED;
    Classifier.Device device = Classifier.Device.CPU;

    recreateClassifier(activity, model, device, 4);
    if (classifier == null) {
      Log.e(TAG, "No classifier on preview!");
      return;
    }

    previewWidth = width;
    previewHeight = height;

    sensorOrientation = 270;

    Log.i(TAG, "Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap =
        Bitmap.createBitmap(
            classifier.getImageSizeX(), classifier.getImageSizeY(), Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth,
            previewHeight,
            classifier.getImageSizeX(),
            classifier.getImageSizeY(),
            sensorOrientation,
            MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    mRgbInts = new int[previewWidth/mSizeSkipX * previewHeight/mSizeSkipY];
  }

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

  public void setPreviewData(final byte[] data)
  {
    //use image Converter...
//    Thread thread = new Thread(new Runnable() {
//      @Override
//      public void run() {
    final long startTime = SystemClock.uptimeMillis();
    resizeByte(data);
    Log.k(TAG, "[time-check] yuv resize time " + (SystemClock.uptimeMillis()-startTime));

    final long startTime2 = SystemClock.uptimeMillis();
    //ImageUtils.convertYUV420SPToARGB8888(data, mPreviewWidth, mPreviewHeight, mRgbInts);
    com.ispd.sfcam.AIEngineSegmentation.env.ImageUtils.convertYUV420SPToARGB8888(mResizeYuvByte, 1440/mSizeSkipX, 1080/mSizeSkipY, mRgbInts);

    Log.d(TAG, "[time-check] yuv2rgb time " + (SystemClock.uptimeMillis()-startTime2));
//      }
//    });
//    thread.start();
  }

  public void processImage() {

    long startTime = SystemClock.uptimeMillis();

    rgbFrameBitmap.setPixels(mRgbInts, 0, previewWidth/mSizeSkipX, 0, 0, previewWidth/mSizeSkipX, previewHeight/mSizeSkipY);
    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

    //front
    int mCameraLocation = 1;
    if (mCameraLocation == 1) {
      Matrix m = new Matrix();
      m.preScale(-1, 1);
      croppedBitmap = Bitmap.createBitmap(croppedBitmap, 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight(), m, false);
    }

//    Thread thread = new Thread(new Runnable() {
//      @Override
//      public void run() {
        if (classifier != null) {
          //final long startTime = SystemClock.uptimeMillis();
          final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);
          lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
          Log.v(TAG, "Detect: %s", results);
          cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);

          showResultsInBottomSheet(results);
        }
//      }
//    });
//    thread.start();

    Log.d(TAG, "processImage : " + (SystemClock.uptimeMillis()-startTime));
  }
  private void recreateClassifier(Activity activity, Classifier.Model model, Classifier.Device device, int numThreads) {
    if (classifier != null) {
      Log.d(TAG, "Closing classifier.");
      classifier.close();
      classifier = null;
    }
    try {
      classifier = Classifier.create(activity, model, device, numThreads);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void showResultsInBottomSheet(List<Classifier.Recognition> results) {
    if (results != null && results.size() >= 3) {
      Classifier.Recognition recognition = results.get(0);
      if (recognition != null) {
        if (recognition.getTitle() != null) Log.d(TAG, recognition.getTitle());
        if (recognition.getConfidence() != null) {
//          Log.d(TAG,
//                  String.format("%.2f", (100 * recognition.getConfidence())) + "%");
//          Log.d(TAG, "" + recognition.getConfidence().toString() + "%");
//          Log.d(TAG, "" + recognition.getConfidence().floatValue() + "%");

          String conf = String.format("%.2f", (100 * recognition.getConfidence()));
          Log.d(TAG, "conf1 : "+conf);
        }
      }

      Classifier.Recognition recognition1 = results.get(1);
      if (recognition1 != null) {
        if (recognition1.getTitle() != null) Log.d(TAG, recognition1.getTitle());
        if (recognition1.getConfidence() != null) {
//          Log.d(TAG,
//                  String.format("%.2f", (100 * recognition1.getConfidence())) + "%");
//          Log.d(TAG, "" + recognition.getConfidence().floatValue() + "%");

          String conf = String.format("%.2f", (100 * recognition.getConfidence()));
          Log.d(TAG, "conf2 : "+conf);
        }
      }

      Classifier.Recognition recognition2 = results.get(2);
      if (recognition2 != null) {
        if (recognition2.getTitle() != null) Log.d(TAG, recognition2.getTitle());
        if (recognition2.getConfidence() != null) {
//          Log.d(TAG,
//                  String.format("%.2f", (100 * recognition2.getConfidence())) + "%");
//          Log.d(TAG, "" + recognition.getConfidence().floatValue() + "%");

          String conf = String.format("%.2f", (100 * recognition.getConfidence()));
          Log.d(TAG, "conf3 : "+conf);
        }
      }
    }
  }
}
