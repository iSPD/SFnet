package com.ispd.sfcam;

/*
 * Copyright 2017 The TensorFlow Authors. All Rights Reserved.
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

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ispd.sfcam.AIEngineClassification.ClassifierMain;
import com.ispd.sfcam.AIEngineObjDetection.objDetection;
import com.ispd.sfcam.AIEngineObjDetection.personDetection;
import com.ispd.sfcam.AIEngineSegmentation.SegmentorMain;
import com.ispd.sfcam.AIEngineSegmentation.segmentation.Segmentor;
import com.ispd.sfcam.drawView.drawViewer;
import com.ispd.sfcam.pdEngine.CreateLerpBlur;
import com.ispd.sfcam.pdEngine.glEngine;
import com.ispd.sfcam.pdEngine.glEngineGL;
import com.ispd.sfcam.touchView.DrawView;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.SFTunner;
import com.ispd.sfcam.utils.fpsChecker;
import com.ispd.sfcam.utils.gammaManager;
import com.ispd.sfcam.utils.gyroInfo;
import com.ispd.sfcam.utils.imageEditor;
import com.ispd.sfcam.utils.movingChecker;
import com.ispd.sfcam.utils.sensorInfo;

import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.ispd.sfcam.MainActivity.H_CHECK_SF_CUSTOM_MODE;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.INTER_NEAREST;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.imgproc.Imgproc.threshold;

public class cameraFragment extends Fragment {

  private static String TAG = "SFCam-cameraFragment";

  private static SurfaceView mCameraSurfaceView = null;
  private static SurfaceHolder mCameraSurfaceHolder = null;
  private static int mCameraSurfaceWidth;
  private static int mCameraSurfaceHeight;
//  private static View mMainLayout;

  private static glEngine mSFEngine;

  private Camera mCamera = null;
  private static byte mPreviewBufferByte[] = null;

  private final Object lock = new Object();
  private boolean runClassifier = false;

  private static final String HANDLE_THREAD_NAME = "CameraBackground";

  private static boolean mCustomEditMode = false;

  /**
   * An additional thread for running tasks that shouldn't block the UI.
   */
  private HandlerThread backgroundThread;

  /**
   * A {@link Handler} for running tasks in the background.
   */
  private Handler backgroundHandler;

  private static Handler mMainActivityHandler;

  private boolean mCameraChangedOn = false;

  private static gyroInfo mGyroInfo;
  private static int mPreRotStatusInt = -1;
  private static sensorInfo mSensorInfo = null;

  //private ClassifierMain mPersonClassifier;
  //private personDetection mPersonDetector;
  private objDetection mObjDetector;
  private SegmentorMain mSegmentDetectorLow;
  private SegmentorMain mSegmentDetector;
  private SegmentorMain mSegmentDetectorHigh;
  //sally-capture
  private SegmentorMain mSegmentDetectorForCapture;
  private boolean mFlagStartCapture = false;
  private boolean mSetCapturePreviewData = false;

  private TextView mFpsTextView;
  private boolean mDebugScreenOn = false;
  private int mDebugOn = 0;

  private boolean mFaceDetected = false;
  private boolean mPersonDetected = false;

  private byte[] mPreviewByte;
  private Mat mPreviewMat;
  private Mat mObjSegmentMat;
  private Mat mObjSegmentSaveMat;
  private Mat mObjStudioMat;
  private int mSegmentStopCount = 0;
  //private boolean mSegmentStopped = false;
  private boolean mSegmentStopped = true;
  private boolean mObjUsed = true;

  private boolean mObjProcessEnd = true;
  byte[] mSegmentByteForSF = new byte[aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_FEATHER_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_FEATHER_FACTOR * 4];
  byte[] mSegmentByteForBlur = new byte[aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * 4];
  byte[] mStudioByteForBlur = new byte[aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * 4];

  //private int []mFaceArray = new int[4*5];
  private int[] mFaceArray = {0, 0, 185, 0, 135, 0, 185, 0, 135, 0, 185, 0, 135, 0, 185, 0, 135, 0, 185, 0, 135};

  private static boolean mMultiTouchAlOn = false;
  private static float mTouchSaveXFloat = -1.0f;
  private static float mTouchSaveYFloat = -1.0f;
  private static float mTouchXFloat = -1.0f;
  private static float mTouchYFloat = -1.0f;
  private boolean mTouchPressed = false;

  //moving detection
  private boolean gMovingOn = false;
  private int gMovingOnCount = 0;
  private boolean mResetProcess = false;
  private boolean mResetPersonProcess = false;

  private static boolean mAutoFocusStatusBool = false;

  private static boolean mStartCapture = false;

  private static ScaleGestureDetector mScaleGestureDetector;
  private static float mScaleFactor = 1.0f;

  private boolean mTouchFocused = true;

  public cameraFragment() {
    Log.d(TAG, "cameraFragment");
  }

  @Override
  public View onCreateView(
          final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

    Log.d(TAG, "onCreateView");
    return inflater.inflate(R.layout.activity_camera, container, false);
  }

  @Override
  public void onViewCreated(final View view, final Bundle savedInstanceState) {
    Log.d(TAG, "onViewCreated");

    if (mGyroInfo == null) {
      mGyroInfo = new gyroInfo(getActivity());
    }

    if (mSensorInfo == null) {
      mSensorInfo = new sensorInfo(getActivity());
    }

    mCameraSurfaceView = (SurfaceView) view.findViewById(R.id.camera_surface_view);
    mCameraSurfaceHolder = mCameraSurfaceView.getHolder();
    mCameraSurfaceHolder.setFixedSize(aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
    mCameraSurfaceHolder.addCallback(mSurfaceListener);

    int[] drawPreviewSize = getPixelData();
    RelativeLayout.LayoutParams previewLayout = new RelativeLayout.LayoutParams(drawPreviewSize[0], drawPreviewSize[1]);
    previewLayout.topMargin = drawPreviewSize[2];

    mCameraSurfaceView.setLayoutParams(previewLayout);

//    LayoutInflater inflater = getActivity().getLayoutInflater();
//    mMainLayout = (View)inflater.inflate(R.layout.ui_main, null);
//
//    RelativeLayout.LayoutParams previewLayout2 = new RelativeLayout.LayoutParams
//            (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//    getActivity().addContentView(mMainLayout, previewLayout2);

    mFpsTextView = (TextView) getActivity().findViewById(R.id.textView_fps);
    mScaleGestureDetector = new ScaleGestureDetector(getActivity(), new ScaleListener());
  }

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Log.d(TAG, "onActivityCreated");
  }

  @Override
  public void onResume() {
    super.onResume();

    Log.d(TAG, "onResume");

//    mSegmentDetector = new SegmentorMain(getActivity(), getActivity().getAssets(), aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 0);
//    mSegmentDetectorHigh = new SegmentorMain(getActivity(), getActivity().getAssets(), aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 1);
  }

  @Override
  public void onPause() {
    Log.d(TAG, "onPause");

    //stopBackgroundThread();
    //mSegmentDetector = null;

    super.onPause();
  }

  public void setMainHandler(Handler handler)
  {
    mMainActivityHandler = handler;
  }

  private void stopAllSession() {

    mSensorInfo.stop();
    mGyroInfo.stop();

//    mPersonClassifier = null;
//    mPersonDetector = null;
    mObjDetector = null;

//    stopPersonBackgroundThread();
    stopBackgroundThread();

    mSegmentDetectorLow.setGlEngine(null);
    mSegmentDetectorLow = null;

    mSegmentDetector.setGlEngine(null);
    mSegmentDetector = null;

    mSegmentDetectorHigh.setGlEngine(null);
    mSegmentDetectorHigh = null;

    mSegmentDetectorForCapture.setGlEngine(null);
    mSegmentDetectorForCapture = null;

    if( mCameraChangedOn == false ) {
      if (mSFEngine != null) {
        mSFEngine.release();
        mSFEngine = null;
      }
    }

    mCameraSurfaceView.setOnTouchListener(null);

    if (mCamera != null) {

//sally : 중간에 다른 카메라 앱으로 전환시 여기서 죽음. 추후 디버깅
//      if (aiCamParameters.mOnFaceDetectionBool == true) {
//        stopFaceDetection();
//      }

      mCamera.addCallbackBuffer(null);
      mCamera.setPreviewCallbackWithBuffer(null);

      mCamera.stopPreview();
      mCamera.release();
      mCamera = null;
    }
  }

  public void resetMoving()
  {
      mResetProcess = true;
      mResetPersonProcess = true;
  }

  private void doFaceFocus()
  {
    if (mFaceArray[0] > 0) {
      Rect faceRect = new Rect(mFaceArray[1], mFaceArray[3], mFaceArray[2], mFaceArray[4]);
      Log.d(TAG, "[Moving-Check-Focus]  Face : " + faceRect);
      doTouchFocus(faceRect);
    }
    else
    {
      //how to
      Rect faceRect = new Rect(mFaceArray[1], mFaceArray[3], mFaceArray[2], mFaceArray[4]);
      Log.d(TAG, "[Moving-Check-Focus]  Face : " + faceRect);
      doTouchFocus(faceRect);
    }
  }

  private void checkSomething()
  {
    //here k
    boolean aiOn = !movingChecker.getMovingRunning();
    if (aiOn == true || mResetProcess == true) {
      Log.d(TAG, "[Moving-Check] aiOn(reset) : "+mResetProcess);
      if( mResetProcess == true ) {
          mResetProcess = false;
          gMovingOn = true;
          gMovingOnCount = 0;
      }

      int objDetected = 0;
      if (mObjDetector != null) {
        objDetected = mObjDetector.getObjectNumber();
      }

      if (mObjDetector != null && mObjDetector.needObjectReset() == true) {
        Log.d(TAG, "[Moving-Check]  needObjectReset");
        gMovingOn = true;
      }

      if (gMovingOn == true) {//|| moving40On == true) {

        if (objDetected > 0) {
          Log.d(TAG, "[Moving-Check]  objDetected > 0");

          gMovingOnCount++;
          if (gMovingOnCount > 3) {
            gMovingOn = false;
          }
        } else {
          Log.d(TAG, "[Moving-Check]  else");

          gMovingOn = false;
          gMovingOnCount = 1;
        }
        mAutoFocusStatusBool = true;

        if (mCamera != null && aiCamParameters.mCameraLocationInt == 0 && gMovingOnCount == 1) {

          //check here...
          if( mObjUsed == false ) {
//            if (mFaceArray[0] > 0) {
//              Rect faceRect = new Rect(mFaceArray[1], mFaceArray[3], mFaceArray[2], mFaceArray[4]);
//              Log.d(TAG, "[Moving-Check-Focus]  Face : " + faceRect);
//              doTouchFocus(faceRect);
//            }
//            else
//            {
//              //how to
//              Rect faceRect = new Rect(mFaceArray[1], mFaceArray[3], mFaceArray[2], mFaceArray[4]);
//              Log.d(TAG, "[Moving-Check-Focus]  Face : " + faceRect);
//              doTouchFocus(faceRect);
//            }

//            doFaceFocus();
          }
          else if (objDetected > 0) {
            Rect temp[] = new Rect[1];
            boolean center = mObjDetector.getObjBox(temp);

            if (center == true) {
              Log.d(TAG, "[Moving-Check] objDetected Center");

              Camera.Parameters parameters = mCamera.getParameters();

              parameters.setFocusAreas(null);
              parameters.setMeteringAreas(null);

              parameters.setFocusMode(parameters.FOCUS_MODE_AUTO);
              mCamera.setParameters(parameters);
              mCamera.autoFocus(new gAutoFocusCallback());
            } else {
              Log.d(TAG, "[Moving-Check] objDetected Ai : " + temp[0]);
              doTouchFocus(temp[0]);
            }
          } else {
            Log.d(TAG, "[Moving-Check] No Ai");

            Camera.Parameters parameters = mCamera.getParameters();

            parameters.setFocusAreas(null);
            parameters.setMeteringAreas(null);

            parameters.setFocusMode(parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(parameters);
            mCamera.autoFocus(new gAutoFocusCallback());
          }

          //what this???
          mTouchXFloat = -1.0f;
          mTouchYFloat = -1.0f;

          setMultiTouch(false, false);
          DrawView.resetMultiTouch();

          //if (mFaceArray[0] == 0) {
          //drawViewer.setTouchData(-1.0f, -1.0f, true, mUseFaceArray[0], false);
          //}
        }
      }
    } else {
      Log.d(TAG, "[Moving-Check] gMovingOn = true");
      gMovingOn = true;
      gMovingOnCount = 0;
    }

    //UI관련 구현 해주세요...얼굴은 이쪽이 아니여...UI용 무빙필요...
    if( movingChecker.getUIMovingRunning() == false ) {
      //check here...
      //if (mFaceArray[0] > 0) {
      if( mObjUsed == false ) {
        Message msg = new Message();
        msg.what = 3;
        msg.arg1 = 3;
        mMainHandler.sendMessage(msg);
      } else if (aiCamParameters.mCameraLocationInt == 0) {
        Message msg = new Message();
        msg.what = 3;
        msg.arg1 = 1;
        mMainHandler.sendMessage(msg);
      } else if (aiCamParameters.mCameraLocationInt == 1) {
        Message msg = new Message();
        msg.what = 3;
        msg.arg1 = 4;
        mMainHandler.sendMessage(msg);
      }
    }
    else {
      //if (mFaceArray[0] > 0) {
      if( mObjUsed == false ) {
        Message msg = new Message();
        msg.what = 3;
        msg.arg1 = 4;
        mMainHandler.sendMessage(msg);
      } else {
        Message msg = new Message();
        msg.what = 3;
        msg.arg1 = 2;
        mMainHandler.sendMessage(msg);
      }
    }

    //check here...
    //if (mFaceArray[0] > 0) {
    if( mObjUsed == false ) {
      if (mFaceArray[0] > 0) {
        for (int i = 0; i < mFaceArray[0]; i++) {
          drawViewer.setFaceData(mFaceArray[i * 4 + 1], mFaceArray[i * 4 + 2], mFaceArray[i * 4 + 3], mFaceArray[i * 4 + 4], i, mFaceArray[0], aiCamParameters.mCameraLocationInt, true);
        }
      }
      else
      {
        drawViewer.setFaceData(mFaceArray[1], mFaceArray[2], mFaceArray[3], mFaceArray[4], 0, 1, aiCamParameters.mCameraLocationInt, true);
      }
    } else {
      drawViewer.setFaceData(-1, -1, -1, -1, -1, -1, aiCamParameters.mCameraLocationInt, false);
    }

    jniController.setTouchEvent(mTouchXFloat, mTouchYFloat);
    movingChecker.setTouchOn(mTouchXFloat, mTouchYFloat);
//          jniController.setSaveMovieStatus(mCaptureOnBool, mMovieOnBool, aiCamParameters.mCameraLocationInt == 1 ? true : false, mSensorInfo.getCurrentLight());

    boolean isPerson = mObjUsed ? false : true;
    jniController.setSaveMovieStatus(aiCamParameters.mCaptureRunning, aiCamParameters.mMovieRunning, aiCamParameters.mCameraLocationInt == 1 ? true : false, isPerson, mSensorInfo.getCurrentLight());
    drawViewer.setRotationData(mGyroInfo.getRotateInfo());
    CreateLerpBlur.setRotationInfo(mGyroInfo.getRotateInfo());
    //아이러니...
    SFTunner.readSFDatas();
  }


  private void doObjectJob(byte[] frame) {
    if (mObjProcessEnd == false) return;

    mObjProcessEnd = false;
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        if (mPreviewByte == null) {
          mPreviewByte = new byte[aiCamParameters.PREVIEW_WIDTH_I / 6 * (aiCamParameters.PREVIEW_HEIGHT_I / 6 * 3 / 2)];
        }
        imageEditor.resizeByte(frame, mPreviewByte, 6, 6);
        if (mPreviewMat == null) {
          //mPreviewMat = new Mat(aiCamParameters.PREVIEW_HEIGHT_I/6, aiCamParameters.PREVIEW_WIDTH_I/6, CV_8UC3);
          mPreviewMat = new Mat();
        }
        mPreviewMat = imageEditor.byteToMatToRgb(mPreviewByte, aiCamParameters.PREVIEW_WIDTH_I / 6, aiCamParameters.PREVIEW_HEIGHT_I / 6);
        if (mObjSegmentMat == null) {
          mObjSegmentMat = new Mat(mPreviewMat.rows(), mPreviewMat.cols(), mPreviewMat.type());
        }
        if (mObjStudioMat == null) {
          mObjStudioMat = new Mat(mPreviewMat.rows(), mPreviewMat.cols(), mPreviewMat.type());
        }

        checkSomething();
        Log.d(TAG, "mAutoFocusStatusBool : " + mAutoFocusStatusBool);
        //if (mPreRotStatusInt != mGyroInfo.getRotateInfo() || mAutoFocusStatusBool == true || (aiCamParameters.mOnFaceDetectionBool == true && mFaceArray[0] > 0) || (mStudioOnBool == true && mStudioModeInt == 3)) {
        if ( mPreRotStatusInt != mGyroInfo.getRotateInfo() ||  mAutoFocusStatusBool == true ) {
          jniController.getObjSegment(mPreviewMat.getNativeObjAddr(), mObjSegmentMat.getNativeObjAddr(), mObjStudioMat.getNativeObjAddr());

          mAutoFocusStatusBool = false;

          mPreRotStatusInt = mGyroInfo.getRotateInfo();
          jniController.setRototationInfo(mGyroInfo.getRotateInfo());
          //don't need???
          //jniController.setFaceRect(mFaceArray, PREVIEW_WIDTH_I, PREVIEW_HEIGHT_I);
        }

        if( mObjSegmentSaveMat == null )
        {
          mObjSegmentSaveMat = new Mat();
        }
        mObjSegmentSaveMat = mObjSegmentMat.clone();

        if (mSFEngine != null) {
          if (glEngine.getSFCamMode() == aiCamParameters.OF_MODE || glEngine.getSFCamMode() == aiCamParameters.HIGHLIGHT_MODE) {
            resize(mObjSegmentMat, mObjSegmentMat, new Size(aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR, aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR), 0, 0, INTER_NEAREST);
            mObjSegmentMat.get(0, 0, mSegmentByteForBlur);

            if (mSFEngine != null) {
              mSFEngine.setMaskTextureForBlur(mSegmentByteForBlur, true, 1);
            }

            if (glEngine.getSFCamMode() == aiCamParameters.HIGHLIGHT_MODE) {
              resize(mObjStudioMat, mObjStudioMat, new Size(aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR, aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR), 0, 0, INTER_NEAREST);
              mObjStudioMat.get(0, 0, mStudioByteForBlur);

              if (mSFEngine != null) {
                mSFEngine.setMaskTextureForStudio(mStudioByteForBlur, true, 1);
              }
            }
          } else if (glEngine.getSFCamMode() == aiCamParameters.SF_MODE || glEngine.getSFCamMode() == aiCamParameters.CARTOON_MODE) {
            //resize(mObjSegmentMat, mObjSegmentMat, new Size(aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I), 0, 0, INTER_NEAREST);
            resize(mObjSegmentMat, mObjSegmentMat, new Size(aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_FEATHER_FACTOR, aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_FEATHER_FACTOR), 0, 0, INTER_NEAREST);
            threshold(mObjSegmentMat, mObjSegmentMat, 30, 255, THRESH_BINARY);
            mObjSegmentMat.get(0, 0, mSegmentByteForSF);

            if (mSFEngine != null) {
              mSFEngine.setMaskTextureForSF(mSegmentByteForSF, true, 1);
              mSFEngine.setMovingIndex(2);
            }
          }
        }
        movingChecker.setFastAlgFlag(1);

        mObjProcessEnd = true;
      }
    });
    thread.start();
  }

  public void setMovingHandler()
  {
    movingChecker.checkMoving(null, SegmentorMain.getLastSegmentData(), mFaceArray, mObjSegmentSaveMat, mGyroInfo.getRotateInfo());
  }

  long startMovingTime = SystemClock.uptimeMillis();

  class mPreviewCameraListener implements Camera.PreviewCallback {

    @Override
    public void onPreviewFrame(final byte[] frame, Camera arg1) {

      count();

      long startTime = SystemClock.uptimeMillis();

      if( SystemClock.uptimeMillis() -  startMovingTime > 0 ) {
        //if( mTouchFocused == true ) {
          movingChecker.checkMoving(frame, SegmentorMain.getLastSegmentData(), mFaceArray, mObjSegmentSaveMat, mGyroInfo.getRotateInfo());
          Log.d(TAG, "onPreviewTime1 : " + (SystemClock.uptimeMillis() - startTime));
        //}

        Log.d(TAG, "startMovingTime : " + (SystemClock.uptimeMillis() - startMovingTime));
        startMovingTime = SystemClock.uptimeMillis();
      }

//      if( mPersonClassifier != null ) {
//        mPersonClassifier.setPreviewData(frame);
//      }

//      if (mPersonDetector != null) {
//        mPersonDetector.setPreviewData(frame);
//      }

      if (mObjDetector != null) {
        //if (mFaceDetected == false && aiCamParameters.mCameraLocationInt == 0) {
        if (mObjUsed == true && aiCamParameters.mCameraLocationInt == 0) {
          mObjDetector.setPreviewData(frame);
          doObjectJob(frame);
          SegmentorMain.resetLastSegment();
        }
      }

      Log.d(TAG, "[save-check] mObjUsed : "+mObjUsed+", mFlagStartCapture : "+mFlagStartCapture);

      if( mObjUsed == false ) {
        if ( mFlagStartCapture == false ) {
//          SegmentorMain.setPreviewData(frame, 4, 3);
          if (mSegmentDetectorLow != null) {
            mSegmentDetectorLow.setPreviewData(frame);
          }

          if (mSegmentDetector != null) {
            mSegmentDetector.setPreviewData(frame);
          }

          if (mSegmentDetectorHigh != null) {
            mSegmentDetectorHigh.setPreviewData(frame);
          }
        }
        else {
          if (mSegmentDetectorForCapture != null) {
            //mSegmentDetectorForCapture.setPreviewData(frame, 2, 2);
            mSegmentDetectorForCapture.setPreviewData(frame);
            mFlagStartCapture = false;
            mSetCapturePreviewData = true;
            Log.d(TAG, "setPreviewData4 yuv2rgb time ");
          }
        }
      }

      if (mCamera != null)
        mCamera.addCallbackBuffer(mPreviewBufferByte);
    }
  }

    public Date lastTime = new Date();
    // lastTime은 기준 시간입니다.
    // 처음 생성당시의 시간을 기준으로 그 다음 1초가 지날때마다 갱신됩니다.
    public long frameCount = 0, nowFps = 0;
    // frameCount는 프레임마다 갱신되는 값입니다.
    // nowFps는 1초마다 갱신되는 값입니다.

    public void count(){
      Date nowTime = new Date();
      long diffTime = nowTime.getTime() - lastTime.getTime();
      // 기준시간 으로부터 몇 초가 지났는지 계산합니다.

      if (diffTime >= 1000) {
        // 기준 시간으로 부터 1초가 지났다면
        nowFps = frameCount;
        Log.d("nowFps", "cpu nowFps : "+nowFps);

        frameCount = 0;
        // nowFps를 갱신하고 카운팅을 0부터 다시합니다.
        lastTime = nowTime;
        // 1초가 지났으므로 기준 시간또한 갱신합니다.
      }

      frameCount++;
      // 기준 시간으로 부터 1초가 안지났다면 카운트만 1 올리고 넘깁니다.
    }

  /**
   * Starts a background thread and its {@link Handler}.
   */
  private void startBackgroundThread() {
    backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
    backgroundThread.start();
    backgroundHandler = new Handler(backgroundThread.getLooper());
    // Start the classification train & load an initial model.
    synchronized (lock) {
      runClassifier = true;
    }
    backgroundHandler.post(periodicClassify);

    backgroundHandler.post(() -> {

      if (mSegmentDetectorLow != null) {
        mSegmentDetectorLow.creatModel();
      }

      if (mSegmentDetector != null) {
        mSegmentDetector.creatModel();
      }

      if (mSegmentDetectorHigh != null) {
        mSegmentDetectorHigh.creatModel();
      }

      if (mSegmentDetectorForCapture != null) {
        mSegmentDetectorForCapture.creatModel();
      }
    });
  }

  /**
   * Stops the background thread and its {@link Handler}.
   */
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

  /**
   * Takes photos and classify them periodically.
   */
   
  private boolean mUseFastAlgForHuman = true;
  private boolean mFastUsed = false;

  private boolean mMovingFocus = false;
  private boolean mMovingFocusDo = true;

  private Runnable periodicClassify =
          new Runnable() {
            @Override
            public void run() {
              synchronized (lock) {
                if (runClassifier) {

                  Log.d(TAG, "mFaceDetected : "+mFaceDetected);

                  if( mPersonDetected == false && aiCamParameters.mCameraLocationInt == 0 ) {

                    if(mSFEngine != null) {
                      mSFEngine.SetHumanFlag(false);
                    }

                    movingChecker.setFastAlgFlag(1);

                    if (mObjDetector != null) {
                      if( mObjUsed == false ) {
                        mObjUsed = true;
                        Log.d(TAG, "[PersonTest] mPersonDetected : "+mPersonDetected);
                      }

                      boolean []isPesrson = {false};
                      mObjDetector.processImage(mGyroInfo.getRotateInfo(), isPesrson);
                      drawViewer.setObjFaceStatus(0);

                      mPersonDetected = isPesrson[0] == true;// && SegmentorMain.isHumanOn() == true;
                      Log.d(TAG, "[PersonTest] isPesrson : "+mPersonDetected);
                    }

                    Message msg = mMainHandler.obtainMessage();
                    msg.what = 0;
                    Bundle bundle = new Bundle();
                    msg.setData(bundle);
                    mMainHandler.sendMessage(msg);

                    Log.d(TAG, "[problem] mObjDetector");
                  } else {

                    if(mSFEngine != null) {
                      mSFEngine.SetHumanFlag(true);
                    }

                    if( mObjUsed == true ) {
                      Log.d(TAG, "[PersonTest] resetTracker");
                      mObjDetector.resetTracker();
                    }

                    Log.d(TAG, "[problem] no mObjDetector : " + mObjUsed);
                    checkSomething();

                    drawViewer.setObjFaceStatus(1);

                    int[] tunes = {-1, -1, -1};
                    jniController.getAlTune(tunes);
                    int fastOn = tunes[0];
                    int slowOn = tunes[1];
                    int stopCount = tunes[2];

                    Log.d(TAG, "fastOn : " + fastOn + ", slowOn : " + slowOn + ", stopCount : " + stopCount);

                    if (mSetCapturePreviewData) { //sally-capture
                      mSegmentDetectorForCapture.processImage(mGyroInfo.getRotateInfo());
                      mSetCapturePreviewData = false;
                    }
                    else {

                      if (aiCamParameters.mCameraLocationInt == 0) {
                        mUseFastAlgForHuman = false;
                      } else {
                        if (movingChecker.getFastTrack(mFaceArray) == true) {
                          mUseFastAlgForHuman = true;
                        } else {
                          if (mFastUsed == true) {
                            mUseFastAlgForHuman = true;
                          } else {
                            mUseFastAlgForHuman = false;
                          }
                        }
                      }

                      //mUseFastAlgForHuman = false;

                      float FocusThresholdValue = SFTunner.movingTuneValues[0];
                      float movingValue = movingChecker.getMovingValue();
                      mMovingFocus = false;
                      if (movingValue < FocusThresholdValue) {
                        mMovingFocus = true;
                      }

                      if (mMovingFocus == true)
                      {
                        if( mMovingFocusDo == true )
                        {
                          Log.d(TAG, "doFaceFocus : "+FocusThresholdValue);
                          doFaceFocus();
                          mMovingFocusDo = false;
                        }
                      }
                      else
                      {
                        mMovingFocusDo = true;
                      }

                        Log.d(TAG, "[Moving-Test-Go] mUseFastAlgForHuman : "+mUseFastAlgForHuman);

                        if( mUseFastAlgForHuman == true ) {

                          mFastUsed = true;

//                          int flag = movingChecker.getFastAlgFlag();
//                          float movingValue = movingChecker.getMovingValue();
//
//                          boolean movingFlag = true;
//                          if( flag == 0 )
//                          {
//                            if( movingValue < 5.f )
//                            {
//                              Log.d(TAG, "movingValue : "+movingValue);
//                              movingFlag = false;
//                            }
//                            else
//                            {
//                              movingFlag = true;
//                            }
//                          }
//                          else if( flag == 2 )
//                          {
//                            if( movingValue > 1.0f )
//                            {
//                              movingFlag = true;
//                            }
//                            else
//                            {
//                              movingFlag = false;
//                            }
//                          }

                          boolean movingFlag = movingChecker.getStopSignal(0);

                          if (mObjUsed == true || movingFlag == true || mResetPersonProcess == true) {
                          //if (mObjUsed == true || movingChecker.getMovingRunning() == true || mResetPersonProcess == true) {
                            //if( false ) {

                            movingChecker.setFastAlgFlag(0);

                            mObjUsed = false;

                            Log.d(TAG, "aiOn(reset-person) : " + mResetPersonProcess);
                            mResetPersonProcess = false;

                            if (mSegmentDetectorLow != null) {

                              long fps = fpsChecker.count();

                              Message msg = mMainHandler.obtainMessage();
                              msg.what = 0;
                              Bundle bundle = new Bundle();
                              bundle.putLong("fps", fps);
                              bundle.putString("status", "Low");
                              msg.setData(bundle);
                              mMainHandler.sendMessage(msg);

                              boolean ret = mSegmentDetectorLow.processImage(mGyroInfo.getRotateInfo());
                              Log.d(TAG, "[problem-check3] (fast)mSegmentDetectorLow : " + movingChecker.getMovingValue() + ", ret : " + ret);

                              mSegmentStopCount = 0;
                              mSegmentStopped = false;
                            }
                          }
                          else {

                            movingChecker.setFastAlgFlag(2);

                            if (mSegmentDetectorHigh != null) {

                              long fps = fpsChecker.count();

                              Message msg = mMainHandler.obtainMessage();
                              msg.what = 0;
                              Bundle bundle = new Bundle();
                              bundle.putLong("fps", fps);
                              bundle.putString("status", "High");
                              msg.setData(bundle);
                              mMainHandler.sendMessage(msg);

//                              mSegmentStopCount++;

                              if (mSegmentStopCount < stopCount) {
                                Log.d(TAG, "[problem-check] [mSegmentDetector] start segmentation");
                                boolean ret = mSegmentDetectorHigh.processImage(mGyroInfo.getRotateInfo());
                                Log.d(TAG, "[problem-check3] (fast)mSegmentDetectorHigh : "+movingChecker.getMovingValue()+", ret : "+ret);

                                if( ret == true )
                                {
                                  mSegmentStopCount++;
                                }

                                mSegmentStopped = false;
                              } else {
                                Log.d(TAG, "[problem-check] [mSegmentDetector] stop segmentation");
                                mSegmentStopped = true;
                              }
                            }

                            mFastUsed = false;
                          } //end of if (mObjUsed == true || movingChecker.getMovingRunning() == true || mResetPersonProcess == true)
                        } else {

                          boolean movingFlag = movingChecker.getStopSignal(1);

                          //here k
                          if (mObjUsed == true || movingFlag == true || mResetPersonProcess == true) {
//                          if (mObjUsed == true || movingChecker.getMovingRunning() == true || mResetPersonProcess == true) {
                            //if( false ) {

                            movingChecker.setFastAlgFlag(1);

//                            if (movingChecker.getFastTrack() == true) {
//                              mUseFastAlgForHuman = true;
//                              movingChecker.setFastAlgFlag(0);
//                              Log.d(TAG, "[Moving-Test-Go] mUseFastAlgForHuman to true");
//                            }

//                            Thread thread = new Thread(new Runnable() {
//                              @Override
//                              public void run() {
//                                while( true ) {
//                                  if (movingChecker.getFastTrack() == true) {
//                                    mUseFastAlgForHuman = true;
//                                    movingChecker.setFastAlgFlag(0);
//                                    Log.d(TAG, "[Moving-Test-Go] mUseFastAlgForHuman to true");
//                                    break;
//                                  }
//
//                                  try {
//                                    Thread.sleep(10);
//                                  } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                  }
//                                }
//                              }
//                            });
//                            thread.start();

                            mObjUsed = false;

                            Log.d(TAG, "aiOn(reset-person) : " + mResetPersonProcess);
                            mResetPersonProcess = false;

                            if (mSegmentDetector != null) {

                              long fps = fpsChecker.count();

                              Message msg = mMainHandler.obtainMessage();
                              msg.what = 0;
                              Bundle bundle = new Bundle();
                              bundle.putLong("fps", fps);
                              bundle.putString("status", "Mid");
                              msg.setData(bundle);
                              mMainHandler.sendMessage(msg);

                              boolean ret = mSegmentDetector.processImage(mGyroInfo.getRotateInfo());
                              Log.d(TAG, "[problem-check3] (slow)mSegmentDetector : "+movingChecker.getMovingValue()+", ret : "+ret);

                              mSegmentStopCount = 0;
                              mSegmentStopped = false;
                            }
                          } else {

                            movingChecker.setFastAlgFlag(2);

                            if (mSegmentDetectorHigh != null) {

                              long fps = fpsChecker.count();

                              Message msg = mMainHandler.obtainMessage();
                              msg.what = 0;
                              Bundle bundle = new Bundle();
                              bundle.putLong("fps", fps);
                              bundle.putString("status", "High");
                              msg.setData(bundle);
                              mMainHandler.sendMessage(msg);

//                              mSegmentStopCount++;

                              if (mSegmentStopCount < stopCount) {
                                Log.d(TAG, "[problem-check] [mSegmentDetector] start segmentation");
                                boolean ret = mSegmentDetectorHigh.processImage(mGyroInfo.getRotateInfo());
                                Log.d(TAG, "[problem-check3] (slow)mSegmentDetectorHigh : "+movingChecker.getMovingValue()+", ret : "+ret);

                                if( ret == true )
                                {
                                  mSegmentStopCount++;
                                }

                                mSegmentStopped = false;
                              } else {
                                Log.d(TAG, "[problem-check] [mSegmentDetector] stop segmentation");
                                mSegmentStopped = true;
                              }
                            }
                          } //end of if (mObjUsed == true || movingChecker.getMovingRunning() == true || mResetPersonProcess == true)
                        }
                    } //end of if (mFlagStartCapture)

                    //바로 못 잡으니 이런..잘생각해바라...
                    mPersonDetected = SegmentorMain.isHumanOn();
                    Log.d(TAG, "[PersonTest] isHumanOn : "+mPersonDetected);
                  }
                }
              }
              backgroundHandler.post(periodicClassify);
            }
          };

//  private Runnable periodicClassify =
//          new Runnable() {
//            @Override
//            public void run() {
//              synchronized (lock) {
//                if (runClassifier) {
//
//                  Log.d(TAG, "mFaceDetected : "+mFaceDetected);
//
////                  Thread thread = new Thread(new Runnable() {
////                    @Override
////                    public void run() {
////                      if (mSegmentDetectorLow != null) {
////                        Log.d(TAG, "[problem] mSegmentDetectorLow");
////                        mSegmentDetectorLow.processImage(mGyroInfo.getRotateInfo());
////                      }
////
////                      try {
////                        Thread.sleep(33);
////                      } catch (InterruptedException e) {
////                        e.printStackTrace();
////                      }
////                    }
////                  });
////                  thread.start();
//
////                  if( mObjUsed == true ) {
////                    if (mSegmentDetectorLow != null) {
////                      Log.d(TAG, "[problem] mSegmentDetectorLow");
////                      mSegmentDetectorLow.processImage(mGyroInfo.getRotateInfo());
////                    }
////                  }
//
////                  if (mPersonDetector != null) {
////                    mPersonDetector.processImage(mGyroInfo.getRotateInfo());
////                  }
//
//                  //if( movingChecker.isHumanRunning() == false ) {
//                  //if (mFaceDetected == false && aiCamParameters.mCameraLocationInt == 0) {
//                  //if(SegmentorMain.isHumanOn() == false ) {
//                  //if( false ) {
//                  if( mPersonDetected == false && aiCamParameters.mCameraLocationInt == 0 ) {
//                    if (mObjDetector != null) {
//                      if( mObjUsed == false ) {
//                          mObjUsed = true;
//                        //mObjDetector.resetTracker();
//                        Log.d(TAG, "[PersonTest] mPersonDetected : "+mPersonDetected);
//                      }
//
//                      boolean []isPesrson = {false};
//                      mObjDetector.processImage(mGyroInfo.getRotateInfo(), isPesrson);
//                      drawViewer.setObjFaceStatus(0);
//
//                      mPersonDetected = isPesrson[0] == true;// && SegmentorMain.isHumanOn() == true;
//                      Log.d(TAG, "[PersonTest] isPesrson : "+mPersonDetected);
//                    }
//
//                    Message msg = mMainHandler.obtainMessage();
//                    msg.what = 0;
//                    Bundle bundle = new Bundle();
//                    msg.setData(bundle);
//                    mMainHandler.sendMessage(msg);
//
//                    Log.d(TAG, "[problem] mObjDetector");
//                  } else {
//
//                    if( mObjUsed == true ) {
//                      Log.d(TAG, "[PersonTest] resetTracker");
//                      mObjDetector.resetTracker();
//                    }
//
////                    //바로 못 잡으니 이런..잘생각해바라...
////                    mPersonDetected = SegmentorMain.isHumanOn();
////                    Log.d(TAG, "[PersonTest] isHumanOn : "+mPersonDetected);
//
//                    Log.d(TAG, "[problem] no mObjDetector : " + mObjUsed);
//                    checkSomething();
//
//                    drawViewer.setObjFaceStatus(1);
//
//                    int[] tunes = {-1, -1, -1};
//                    jniController.getAlTune(tunes);
//                    int fastOn = tunes[0];
//                    int slowOn = tunes[1];
//                    int stopCount = tunes[2];
//
//                    Log.d(TAG, "fastOn : " + fastOn + ", slowOn : " + slowOn + ", stopCount : " + stopCount);
//
//                    if (mFlagStartCapture) { //sally-capture
//                      mSegmentDetectorForCapture.processImage(mGyroInfo.getRotateInfo());
//                      mFlagStartCapture = false;
//                    }
//                    else {
//                      if (fastOn == 1 && slowOn == 1) {
//                        //if (movingChecker.getFastTrack() == true) {
//                        if (false) {
//
//                          mObjUsed = false;
//
//                          if (mSegmentDetectorLow != null) {
//
//                            long fps = fpsChecker.count();
//
//                            Message msg = mMainHandler.obtainMessage();
//                            msg.what = 0;
//                            Bundle bundle = new Bundle();
//                            bundle.putLong("fps", fps);
//                            bundle.putString("status", "Low");
//                            msg.setData(bundle);
//                            mMainHandler.sendMessage(msg);
//
//                            boolean ret  = mSegmentDetectorLow.processImage(mGyroInfo.getRotateInfo());
//                            Log.d(TAG, "[problem-check2] mSegmentDetectorLow : "+movingChecker.getMovingValue()+", ret : "+ret);
//
//                            mSegmentStopCount = 0;
//                            mSegmentStopped = false;
//                          }
//                        } else {
//
//                          //here k
//                          if (mObjUsed == true || movingChecker.getMovingRunning() == true || mResetPersonProcess == true) {
//                            //if( false ) {
//
//                            mObjUsed = false;
//
//                            Log.d(TAG, "aiOn(reset-person) : " + mResetPersonProcess);
//                            mResetPersonProcess = false;
//
//                            if (mSegmentDetector != null) {
//
//                              long fps = fpsChecker.count();
//
//                              Message msg = mMainHandler.obtainMessage();
//                              msg.what = 0;
//                              Bundle bundle = new Bundle();
//                              bundle.putLong("fps", fps);
//                              bundle.putString("status", "Mid");
//                              msg.setData(bundle);
//                              mMainHandler.sendMessage(msg);
//
//                              boolean ret = mSegmentDetector.processImage(mGyroInfo.getRotateInfo());
//                              Log.d(TAG, "[problem-check2] mSegmentDetector : "+movingChecker.getMovingValue()+", ret : "+ret);
//
//                              mSegmentStopCount = 0;
//                              mSegmentStopped = false;
//                            }
//                          } else {
//                            if (mSegmentDetectorHigh != null) {
//
//                              long fps = fpsChecker.count();
//
//                              Message msg = mMainHandler.obtainMessage();
//                              msg.what = 0;
//                              Bundle bundle = new Bundle();
//                              bundle.putLong("fps", fps);
//                              bundle.putString("status", "High");
//                              msg.setData(bundle);
//                              mMainHandler.sendMessage(msg);
//
////                              mSegmentStopCount++;
//
//                              if (mSegmentStopCount < stopCount) {
//                                Log.d(TAG, "[problem-check] [mSegmentDetector] start segmentation");
//                                boolean ret = mSegmentDetectorHigh.processImage(mGyroInfo.getRotateInfo());
//                                Log.d(TAG, "[problem-check2] mSegmentDetectorHigh : "+movingChecker.getMovingValue()+", ret : "+ret);
//
//                                if( ret == true )
//                                {
//                                  mSegmentStopCount++;
//                                }
//
//                                mSegmentStopped = false;
//                              } else {
//                                Log.d(TAG, "[problem-check] [mSegmentDetector] stop segmentation");
//                                mSegmentStopped = true;
////                                try {
////                                  Thread.sleep(33);
////                                } catch (InterruptedException e) {
////                                  e.printStackTrace();
////                                }
//                              }
//
//                              //                              if (movingChecker.getMovingValue() > 0.02f) {
//                              //                                Log.d(TAG, "start segmentation");
//                              //                                mSegmentDetectorHigh.processImage();
//                              //                              } else {
//                              //                                Log.d(TAG, "stop segmentation");
//                              //                                try {
//                              //                                  Thread.sleep(33);
//                              //                                } catch (InterruptedException e) {
//                              //                                  e.printStackTrace();
//                              //                                }
//                              //                              }
//                            }
//                          } //end of if (mObjUsed == true || movingChecker.getMovingRunning() == true || mResetPersonProcess == true)
//
//                        }
//                      } else if (fastOn == 1 && slowOn == 0) {
//                        long fps = fpsChecker.count();
//
//                        mObjUsed = false;
//
//                        Message msg = mMainHandler.obtainMessage();
//                        msg.what = 0;
//                        Bundle bundle = new Bundle();
//                        bundle.putLong("fps", fps);
//                        msg.setData(bundle);
//                        mMainHandler.sendMessage(msg);
//
//                        mSegmentDetector.processImage(mGyroInfo.getRotateInfo());
//                      } else if (fastOn == 0 && slowOn == 1) {
//                        long fps = fpsChecker.count();
//
//                        mObjUsed = false;
//
//                        Message msg = mMainHandler.obtainMessage();
//                        msg.what = 0;
//                        Bundle bundle = new Bundle();
//                        bundle.putLong("fps", fps);
//                        msg.setData(bundle);
//                        mMainHandler.sendMessage(msg);
//
//                        mSegmentDetectorHigh.processImage(mGyroInfo.getRotateInfo());
//                      } else {
//                        try {
//                          Thread.sleep(33);
//                        } catch (InterruptedException e) {
//                          e.printStackTrace();
//                        }
//                      }
//                    } //end of if (mFlagStartCapture)
//
//                    //바로 못 잡으니 이런..잘생각해바라...
//                    mPersonDetected = SegmentorMain.isHumanOn();
//                    Log.d(TAG, "[PersonTest] isHumanOn : "+mPersonDetected);
//                  }
//                }
//              }
//              backgroundHandler.post(periodicClassify);
//            }
//          };

  /**
   * Takes photos and classify them periodically.
   */
  private final Object lockPerson = new Object();
  private boolean runPersonClassifier = false;
  private static final String HANDLE_PERSON_THREAD_NAME = "CameraBackground";
  private HandlerThread backgroundPersonThread;
  private Handler backgroundPersonHandler;

  /**
   * Starts a background thread and its {@link Handler}.
   */
  private void startPersonBackgroundThread() {
    backgroundPersonThread = new HandlerThread(HANDLE_PERSON_THREAD_NAME);
    backgroundPersonThread.start();
    backgroundPersonHandler = new Handler(backgroundPersonThread.getLooper());
    // Start the classification train & load an initial model.
    synchronized (lock) {
      runPersonClassifier = true;
    }
    backgroundPersonHandler.post(periodicDetectPerson);

//    backgroundPersonHandler.post(() -> {
//
//      if( mPersonDetector != null ) {
//        mPersonDetector.creatModel();
//      }
//    });
  }

  /**
   * Stops the background thread and its {@link Handler}.
   */
  private void stopPersonBackgroundThread() {
    backgroundPersonThread.quitSafely();
    try {
      backgroundPersonThread.join();
      backgroundPersonThread = null;
      backgroundPersonHandler = null;
      synchronized (lockPerson) {
        runPersonClassifier = false;
      }
    } catch (InterruptedException e) {
      Log.e(TAG, "Interrupted when stopping background thread", e);
    }
  }

  private Runnable periodicDetectPerson =
          new Runnable() {
            @Override
            public void run() {
              synchronized (lockPerson) {
                if (runPersonClassifier) {
//                  if (mPersonDetector != null) {
//                    mPersonDetector.processImage(mGyroInfo.getRotateInfo());
//                  }

//                  if( mPersonClassifier != null )
//                  {
//                    mPersonClassifier.processImage();
//                  }

//                  try {
//                    Thread.sleep(50);
//                  } catch (InterruptedException e) {
//                    e.printStackTrace();
//                  }
                }
              }
              backgroundPersonHandler.post(periodicDetectPerson);
            }
          };

  private SurfaceHolder.Callback mSurfaceListener = new SurfaceHolder.Callback() {

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

      stopAllSession();

      Log.i(TAG, "Release Camera Functions (released camera object)");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
      Log.i(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

      Log.i(TAG, "surfaceChanged");

      mCameraSurfaceHolder = holder;
      mCameraSurfaceWidth = width;
      mCameraSurfaceHeight = height;

      try {
        if (mCamera == null) {
          mCamera = Camera.open(aiCamParameters.mCameraLocationInt);
          sofCamInit(holder, width, height);
        }
      } catch (Exception e) {
        Log.e(getString(R.string.app_name), "failed to open Camera");
        e.printStackTrace();
      }
      Log.i(TAG, "Open Camera Device");
    }
  };

  void sofCamInit(SurfaceHolder holder, int width, int height) {
    if (mCamera != null) {

      Log.d(TAG, "Camera Open Start");

      long startTime = SystemClock.uptimeMillis();
      mGyroInfo.start();
      mSensorInfo.start();

//      mPersonClassifier = new ClassifierMain(getActivity(), aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 0);
//      mPersonDetector = new personDetection(getActivity(), getActivity().getAssets(), aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
//      mPersonDetector.setCameraLocation(aiCamParameters.mCameraLocationInt);
      mObjDetector = new objDetection(getActivity(), getActivity().getAssets(), aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
      mObjDetector.setCameraLocation(aiCamParameters.mCameraLocationInt);
      mSegmentDetectorLow = new SegmentorMain(getActivity(), getActivity().getAssets(), aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, -1);
      mSegmentDetectorLow.setCameraLocation(aiCamParameters.mCameraLocationInt);
      mSegmentDetector = new SegmentorMain(getActivity(), getActivity().getAssets(), aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 0);
      mSegmentDetector.setCameraLocation(aiCamParameters.mCameraLocationInt);
      mSegmentDetectorHigh = new SegmentorMain(getActivity(), getActivity().getAssets(), aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 1);
      mSegmentDetectorHigh.setCameraLocation(aiCamParameters.mCameraLocationInt);
      mSegmentDetectorForCapture = new SegmentorMain(getActivity(), getActivity().getAssets(), aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 2);
      mSegmentDetectorForCapture.setCameraLocation(aiCamParameters.mCameraLocationInt);
      Log.d(TAG, "[time-Load] AI Loading Time : " + (SystemClock.uptimeMillis()-startTime));

      Camera.Parameters parameters = mCamera.getParameters();
      parameters.setPreviewSize(aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
      parameters.setPictureSize(aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I); //sally

      Log.d(TAG, "max zoom : "+parameters.getMaxZoom());
      if( aiCamParameters.mCameraLocationInt == 0 )
      {
        parameters.setZoom(20);
      }

//      List<int[]> fpsList = parameters.getSupportedPreviewFpsRange();
//      for(int i = 0; i < fpsList.size(); i++) {
//        Log.d(TAG, "getSupportedPreviewFpsRange : " + fpsList.get(i)[0]);
//        Log.d(TAG, "getSupportedPreviewFpsRange : " + fpsList.get(i)[1]);
//      }
//      parameters.setPreviewFpsRange(15000, 15000);

      try {
        Log.d(TAG, "SofEngine");
        if (mCamera != null) {
          long startTimeEn = SystemClock.uptimeMillis();
          if( mSFEngine == null ) {
            mSFEngine = new glEngine(getActivity(), aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, holder.getSurface());
            mSFEngine.setMainActivityHandler(mMainActivityHandler);
            mMainActivityHandler.sendEmptyMessage(H_CHECK_SF_CUSTOM_MODE); //sally
          }
          mSegmentDetectorLow.setGlEngine(mSFEngine);
          mSegmentDetector.setGlEngine(mSFEngine);
          mSegmentDetectorHigh.setGlEngine(mSFEngine);
          mSegmentDetectorForCapture.setGlEngine(mSFEngine);
          mCamera.setPreviewTexture(mSFEngine.getSurfaceTexture());
          Log.d(TAG, "[time-Load] En Loading Time : " + (SystemClock.uptimeMillis()-startTimeEn));
//          mSofEngine.setMainHandler(mMainHandler);
//          mSofEngine.setFront(aiCamParameters.mCameraLocationInt);

//          mCamera.setPreviewDisplay(holder);
//          mCamera.setDisplayOrientation(90);
        }
      } catch (Exception e) {
        Log.d(TAG, "Camera Set Surface Error");
        e.printStackTrace();
      }

      long startTimeCam = SystemClock.uptimeMillis();
      int size = aiCamParameters.PREVIEW_WIDTH_I * aiCamParameters.PREVIEW_HEIGHT_I;
      size = size * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
      if (mPreviewBufferByte == null) {
        mPreviewBufferByte = new byte[size];
      }

      mCamera.addCallbackBuffer(mPreviewBufferByte);
      mCamera.setPreviewCallbackWithBuffer(new mPreviewCameraListener());
      mCamera.setParameters(parameters);

      if (aiCamParameters.mOnFaceDetectionBool == true) {
        mCamera.setFaceDetectionListener(new MyFaceDetectionListener());
      }

	  //initialize zoom & pan factors
      mScaleFactor = 1.0f;
      mPositionX = 0.f;
      mPositionY = 0.f;

      mCamera.startPreview();

      if (aiCamParameters.mOnFaceDetectionBool == true) {
        startFaceDetection();
      }

      mCameraSurfaceView.setOnTouchListener(mTouchPointListener);

//      startPersonBackgroundThread();
      startBackgroundThread();
      Log.d(TAG, "[time-Load] Cam Loding Time : " + (SystemClock.uptimeMillis()-startTimeCam));
      Log.d(TAG, "[time-Load] Total Loding Time : " + (SystemClock.uptimeMillis()-startTime));

      Log.i(TAG, "Start Camera Preview");

    } else {
      Context context = getActivity();
      CharSequence text = "Camera Open Error";
      int duration = Toast.LENGTH_SHORT;

      Toast toast = Toast.makeText(context, text, duration);
      toast.show();
    }
  }

  public void cameraChanged() {

    mCameraChangedOn = true;

    stopAllSession();

//    try {
//      Thread.sleep(200, 1000);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }

    if (mCamera == null) {
      try {
        mCamera = Camera.open(aiCamParameters.mCameraLocationInt);
      } catch (Exception e) {
        Log.e(TAG, "failed to open Camera");
        e.printStackTrace();
      }

      if (mCameraSurfaceHolder != null) {
        sofCamInit(mCameraSurfaceHolder, mCameraSurfaceWidth, mCameraSurfaceHeight);
      }
    }

    resetMoving();

    mCameraChangedOn = false;
  }

  class MyFaceDetectionListener implements Camera.FaceDetectionListener {

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {

      Log.d("FaceDetection", "onFaceDetection : " + faces.length);

      if (mSFEngine != null) {
        mSFEngine.setFaceInfo(faces.length);
      }
      if (faces.length > 0) {

        Log.d("FaceDetection", "face detected: " + faces.length +
                " Face left : " + faces[0].rect.left +
                " Face right : " + faces[0].rect.right +
                " Face top : " + faces[0].rect.top +
                " Face bottom : " + faces[0].rect.bottom);

        mFaceDetected = true;

        int useFaceLength = faces.length;
        mFaceArray[0] = useFaceLength;
        int arrayCount = 0;

        for (int i = 0; i < useFaceLength; i++) {
          float rate;
          Rect rect = faces[i].rect;
          Rect newRect = new Rect();

          newRect.left = rect.left + 1000;
          newRect.top = rect.top + 1000;
          newRect.right = rect.right + 1000;
          newRect.bottom = rect.bottom + 1000;

          rate = (float) aiCamParameters.LCD_HEIGHT_I / 2000f;
          newRect.left = (int) (newRect.left * rate);
          newRect.right = (int) (newRect.right * rate);

          rate = (float) aiCamParameters.LCD_WIDTH_I / 2000f;
          newRect.top = (int) (newRect.top * rate);
          newRect.bottom = (int) (newRect.bottom * rate);

          mFaceArray[++arrayCount] = newRect.left;
          mFaceArray[++arrayCount] = newRect.right;
          mFaceArray[++arrayCount] = newRect.top;
          mFaceArray[++arrayCount] = newRect.bottom;
        }

        jniController.setFaceRect(mFaceArray, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
      } else {
        mFaceArray[0] = 0;
        jniController.setFaceRect(mFaceArray, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
        mFaceDetected = false;
      }
    }
  }

  public void startFaceDetection() {
    // Try starting Face Detection
    if (mCamera != null) {
      Camera.Parameters params = mCamera.getParameters();

      // start face detection only *after* preview has started
      if (params.getMaxNumDetectedFaces() > 0) {
        // camera supports face detection, so can start it:
        Log.d("FaceDetection", "startFaceDetection");
        mCamera.startFaceDetection();
      }
    }
  }

  public void stopFaceDetection() {
    // Try starting Face Detection
    if (mCamera != null) {
      Camera.Parameters params = mCamera.getParameters();

      // start face detection only *after* preview has started
      if (params.getMaxNumDetectedFaces() > 0) {
        // camera supports face detection, so can start it:
        Log.d("FaceDetection", "stopFaceDetection");
        mCamera.stopFaceDetection();
        mCamera.setFaceDetectionListener(null);
      }
    }
  }

  Handler mMainHandler = new Handler() {
    @Override
    public void handleMessage(Message inputMessage) {

      switch (inputMessage.what) {
        case 0:
          Bundle bundle = inputMessage.getData();
          Long fps = bundle.getLong("fps");
          String status = bundle.getString("status");

          double yValues[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
          movingChecker.getMeanValue(yValues);

          float bright = mSensorInfo.getCurrentLight();
//          mFpsTextView.setText(String.valueOf(fps) + ", M : " + movingChecker.getMovingValue()
//                  + "\n"+ "B:" +bright+ "Y : " + (int) yValues[0] + "(H:" + (int) yValues[1] + ", B:" + (int) yValues[2] + ", F:" + (int) yValues[3] + ")"
//                  + "\n Gamma : " + Math.round(yValues[4] * 10) / 10.0 + "(" + Math.round(gammaManager.mAnmationGamma * 100) / 100.0 + ")"
//                  + "\n" + status+", Face:"+mFaceArray[0]
//                  +  ", fv : "+yValues[5]);

          float objScales[] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
          jniController.readObjScaleValue(objScales);
          for(int i = 0; i < 5; i++)
          {
            objScales[i] = Math.round(objScales[i] * 100.f) / 100.f;
          }

          mFpsTextView.setText("Moving : " + Math.round(movingChecker.getMovingValue() * 100) / 100.0
                  + "\n"+ "Bright:" +bright+", 얼굴밝기:" + (int) yValues[3] + ")"
                  + "\nGamma : " + Math.round(yValues[4] * 10) / 10.0 + "(" + Math.round(gammaManager.mAnmationGamma * 100) / 100.0 + ")"
                  + "\n" +status+", fastMoving : "+Math.round(yValues[5] * 100) / 100.0
                  + "\nFaceSize : "+Math.round(yValues[7] * 100) / 100.0+", 가변무빙 : "+Math.round(yValues[6] * 100) / 100.0
                  + "\nObjSize : "+objScales[0]+", ObjX : "+objScales[1]+"("+objScales[3]+")"+", ObjY : "+objScales[2]+"("+objScales[4]+")");
          //mFpsTextView.setText("");
          break;
        case 3:
          drawViewer.setDepthStatus(inputMessage.arg1);
          break;
        default:
          break;
      }
    }
  };

  public boolean onKeyDown(int keyCode, KeyEvent event) {

    Log.d(TAG, "keyCode : " + keyCode);

    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      //Do something
      final Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
      vibrator.vibrate(100);

      if (mDebugScreenOn == false) {
        mDebugScreenOn = true;
      } else {
        mDebugScreenOn = false;
      }
      drawViewer.setDebugScreenOn(mDebugScreenOn);
    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      mDebugOn = 1 - mDebugOn;
      glEngineGL.setDebugOn(mDebugOn);
    } else if (keyCode == KeyEvent.KEYCODE_BACK ) {
        //don't do this...
        //stopAllSession();
        getActivity().onBackPressed();
    }
    return true;
  }

  public static void setMultiTouch(boolean multiTouchOn, boolean minusTouchOn) {
    mMultiTouchAlOn = multiTouchOn;

    mTouchSaveXFloat = mTouchXFloat;
    mTouchSaveYFloat = mTouchYFloat;

    jniController.setTouchEventForMultiTouch(mMultiTouchAlOn, minusTouchOn, mTouchSaveXFloat, mTouchSaveYFloat);
  }

  void doTouchFocus(MotionEvent event) {
    float x = event.getY();
    float y = mCameraSurfaceView.getLayoutParams().width - event.getX();

    Rect touchRect = new Rect(
            (int) (x - 100),
            (int) (y - 100),
            (int) (x + 100),
            (int) (y + 100));

    //if (mFaceArray[0] == 0) {
    if( mObjUsed == true ) {
      doTouchFocus(touchRect);
    }
  }

  private static float mPositionX;
  private static float mPositionY;
  private static float mLastTouchX;
  private static float mLastTouchY;
  private static float mLastGestureX;
  private static float mLastGestureY;

  private static final int INVALID_POINTER_ID = -1;
  private int mActivePointerID = INVALID_POINTER_ID;

  private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      mScaleFactor *= mScaleGestureDetector.getScaleFactor();
      mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 4.0f));
      Log.d(TAG,"gesture scale factor : " + mScaleFactor);

      setTranslate();
      return true;
    }

  }

  private static float mTransX = 0.f;
  private static float mTransY = 0.f;
  private static float mPivotX = 0.f;
  private static float mPivotY = 0.f;

  private static void setTranslate() {

    int viewWidth = mCameraSurfaceView.getWidth();
    int viewHeight = mCameraSurfaceView.getHeight();
    int mImageWidth = 1080;
    int mImageHeight = 1440;
    float divider = 1.0f;


    mTransX = mPositionX  / (mImageWidth) * -1.f;;
    mTransY = mPositionY / (mImageHeight) * -1.f;

    if(mTransX < (mScaleFactor - 1.0f) * -1.0f) {
        mTransX = (mScaleFactor - 1.0f) * -1.0f;
        mPositionX = mTransX * mImageWidth * -1.f;
    }
    else if(mTransX > (mScaleFactor - 1.0f)) {
        mTransX = (mScaleFactor - 1.0f);
        mPositionX = mTransX * mImageWidth * -1.f;
    }

    if(mTransY < (mScaleFactor - 1.0f) * -1.0f) {
        mTransY = (mScaleFactor - 1.0f) * -1.0f;
        mPositionY = mTransY * mImageHeight * -1.f;
    }
    else if(mTransY > (mScaleFactor - 1.0f)) {
        mTransY = (mScaleFactor - 1.0f);
        mPositionY = mTransY * mImageHeight * -1.f;
    }
    mSFEngine.SetScaleFactorSFCustom(mScaleFactor, mTransX, mTransY, 0.f, 0.f);

  }

  private static void setTranslate_new() {
    int viewWidth = mCameraSurfaceView.getWidth();
    int viewHeight = mCameraSurfaceView.getHeight();
    int mImageWidth = 1080;
    int mImageHeight = 1440;

    if ((mPositionX * -1) < 0) {
      mPositionX = 0;
    } else if ((mPositionX * -1) > mImageWidth * mScaleFactor - viewWidth) {
      mPositionX = (mImageWidth * mScaleFactor - viewWidth) * -1;
    }
    if ((mPositionY * -1) < 0) {
      mPositionY = 0;
    } else if ((mPositionY * -1) > mImageHeight * mScaleFactor - viewHeight) {
      mPositionY = (mImageHeight * mScaleFactor - viewHeight) * -1;
    }

    if ((mImageHeight * mScaleFactor) < viewHeight) {
      mPositionY = 0;
    }


    mTransX = mPositionX / mImageWidth * mScaleFactor * -1.f;
    mTransY = mPositionY / mImageHeight * mScaleFactor * -1.f;

    float pivotX = 0.f;
    float pivotY = 0.f;
    if (mScaleGestureDetector.isInProgress()) {
      pivotX = mScaleGestureDetector.getFocusX() / mImageWidth * mScaleFactor * -1.f + 0.f;
      pivotY = mScaleGestureDetector.getFocusY() / mImageHeight * mScaleFactor * -1.f + 0.f;
    }
    else {
      pivotX = mLastGestureX / mImageWidth * mScaleFactor * -1.f + 0.f;
      pivotY = mLastGestureY / mImageHeight * mScaleFactor * -1.f + 0.f;
    }

    mSFEngine.SetScaleFactorSFCustom(mScaleFactor, mTransX, mTransY, pivotX, pivotY);
  }

  View.OnTouchListener mTouchPointListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
      if (mCustomEditMode == true) {
        mScaleGestureDetector.onTouchEvent(event);

        final int action = event.getAction();

        switch (action & MotionEvent.ACTION_MASK) {

          case MotionEvent.ACTION_DOWN: {

            //get x and y cords of where we touch the screen
            final float x = event.getX();
            final float y = event.getY();

            //remember where touch event started
            mLastTouchX = x;
            mLastTouchY = y;

            //save the ID of this pointer
            mActivePointerID = event.getPointerId(0);

            break;
          }
          case MotionEvent.ACTION_MOVE: {

            //find the index of the active pointer and fetch its position
            final int pointerIndex = event.findPointerIndex(mActivePointerID);
            final float x = event.getX(pointerIndex);
            final float y = event.getY(pointerIndex);

            if (!mScaleGestureDetector.isInProgress()) {

              //calculate the distance in x and y directions
              final float distanceX = x - mLastTouchX;
              final float distanceY = y - mLastTouchY;

              mPositionX += distanceX;
              mPositionY += distanceY;

              //redraw canvas call onDraw method
              Log.d(TAG, "translate X : " + mPositionX + ", Y : " + mPositionY);
              setTranslate();

            }
            //remember this touch position for next move event
            mLastTouchX = x;
            mLastTouchY = y;

            break;
          }

          case MotionEvent.ACTION_UP: {
            mActivePointerID = INVALID_POINTER_ID;
            break;
          }

          case MotionEvent.ACTION_CANCEL: {
            mActivePointerID = INVALID_POINTER_ID;
            break;
          }

          case MotionEvent.ACTION_POINTER_UP: {
            //Extract the index of the pointer that left the screen
            final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            final int pointerId = event.getPointerId(pointerIndex);
            if (pointerId == mActivePointerID) {
              //Our active pointer is going up Choose another active pointer and adjust
              final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
              mLastTouchX = event.getX(newPointerIndex);
              mLastTouchY = event.getY(newPointerIndex);
              mActivePointerID = event.getPointerId(newPointerIndex);
            }
            break;
          }
        }
      } else { //mCustomEditMode == false
        event.getSize();
        float X = event.getX();
        float Y = event.getY();

        float X2 = event.getRawX();
        float Y2 = event.getRawY();

        Log.d("touch-test", "Touch X : " + X + ", Y : " + Y);
        Log.d("touch-test", "Touch X2 : " + X2 + ", Y2 : " + Y2);
        Log.d("touch-test", "event.getAction() : " + event.getAction());

        DrawView.touchEventFromParent(event);
        DrawView.touchEventFromParentClone(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          mTouchPressed = false;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {

          if (mTouchPressed == false && DrawView.getPointTouched(X, Y) == true) {

            //check here...
            //if (mUseFaceArray[0] == 0) {
            //if (mFaceArray[0] == 0) {
            if (mObjUsed == true) {
              DrawView.setTouch(X, Y);
              doTouchFocus(event);
            } else {
              //drawViewer.setTouchData(X, Y);
            }

            mTouchXFloat = X * aiCamParameters.PREVIEW_HEIGHT_I / mCameraSurfaceView.getLayoutParams().width;
            mTouchYFloat = Y * aiCamParameters.PREVIEW_WIDTH_I / mCameraSurfaceView.getLayoutParams().height;

            //Portrait Layout...
            mTouchYFloat = (float) aiCamParameters.PREVIEW_HEIGHT_I - mTouchXFloat;
            mTouchXFloat = Y;

            mAutoFocusStatusBool = true;
          }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
          mTouchPressed = true;
        }
      }
      return true;
    }
  };

  public static void setAutoFocusOn(boolean on) {
    mAutoFocusStatusBool = on;
  }

  class gAutoFocusCallback implements Camera.AutoFocusCallback {
    @Override
    public void onAutoFocus(boolean success, Camera camera) {
      Log.d("Auto-Middle", "Middle Done : " + success);
      //mCamera.cancelAutoFocus();

      mTouchFocused = true;
    }
  }

  public void doTouchFocus(final Rect tfocusRect) {
    try {
      mTouchFocused = false;

      final Rect targetFocusRect = new Rect(
              tfocusRect.left * 2000 / mCameraSurfaceView.getLayoutParams().height - 1000,
              tfocusRect.top * 2000 / mCameraSurfaceView.getLayoutParams().width - 1000,
              tfocusRect.right * 2000 / mCameraSurfaceView.getLayoutParams().height - 1000,
              tfocusRect.bottom * 2000 / mCameraSurfaceView.getLayoutParams().width - 1000);

      List<Camera.Area> focusList = new ArrayList<Camera.Area>();
      Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
      focusList.add(focusArea);

      Camera.Parameters param = mCamera.getParameters();
      param.setFocusAreas(focusList);
      //param.setMeteringAreas(focusList);
      mCamera.setParameters(param);

      mCamera.autoFocus(new gAutoFocusCallback());
    } catch (Exception e) {
      e.printStackTrace();
      Log.i(TAG, "Unable to autofocus");
    }
  }

  public glEngine getGlEngine()
  {
    return mSFEngine;
  }

  private int[] getPixelData()
  {
    final float scale = getResources().getDisplayMetrics().density; // 화면의 밀도를 구한다.

    Display display = getActivity().getWindowManager().getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    Log.d(TAG, "[DPI] LCD Size : "+size.x+", "+size.y);

    int pixel = aiCamParameters.PREVIEW_HEIGHT_I;
    float dipX = (float)pixel / scale;
    pixel = aiCamParameters.PREVIEW_WIDTH_I;
    float dipY = (float)pixel / scale;
    pixel = size.x;
    float LCDx = (float)pixel / scale;
    pixel = size.y;
    float LCDy = (float)pixel / scale;

    Log.d(TAG, "[DPI] scale : "+scale);
    Log.d(TAG, "[DPI] dizX : "+dipX);
    Log.d(TAG, "[DPI] dizY : "+dipY);
    Log.d(TAG, "[DPI] LCDx : "+LCDx);
    Log.d(TAG, "[DPI] LCDy : "+LCDy);

    int []drawPreviewSize = new int[3];
    float rate = (float)size.x / (float)aiCamParameters.PREVIEW_HEIGHT_I;
    drawPreviewSize[0] = size.x;
    drawPreviewSize[1] = (int) ((float)aiCamParameters.PREVIEW_WIDTH_I * rate);
    drawPreviewSize[2] = 0;//(size.y - drawPreviewSize[1]) / 2;

    aiCamParameters.LCD_WIDTH_I = drawPreviewSize[0];
    aiCamParameters.LCD_HEIGHT_I = drawPreviewSize[1];

    Log.d(TAG,"[LCD_] LCD_WIDTH_I : "+aiCamParameters.LCD_WIDTH_I);
    Log.d(TAG,"[LCD_] LCD_HEIGHT_I : "+aiCamParameters.LCD_HEIGHT_I);

    DisplayMetrics metrix = new DisplayMetrics();
    getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrix);
    int phoneDpi = metrix.densityDpi; // 화면의 밀도를 구한다.
    Log.d(TAG, "[DPI] phoneDpi : "+phoneDpi);

    return drawPreviewSize;
  }

  public void StartCapture() {
     mFlagStartCapture = true;
  }

  public boolean IsObjectDetectionMode() {
//    if (mFaceDetected == false && aiCamParameters.mCameraLocationInt == 0) {
    if (mObjUsed == true && aiCamParameters.mCameraLocationInt == 0) {
      return true;
    }
    else {
      return false;
    }
  }

  public void SetCustomEditFlag(boolean flag) {
      //SF_MODE 이면서 SF_OPT_FG_PREVIEW_BG_CUSTOM, SF_OPT_FG_CUSTOM_BG_CUSTOM 옵션일때만 true가 됨.
     mCustomEditMode = flag;
  }
}
