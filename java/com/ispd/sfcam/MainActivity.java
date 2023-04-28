package com.ispd.sfcam;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
//import android.support.v4.content.FileProvider;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ispd.sfcam.AIEngineSegmentation.SegmentorMain;
import com.ispd.sfcam.drawView.drawViewer;
import com.ispd.sfcam.pdEngine.CreateCartoon;
import com.ispd.sfcam.pdEngine.CreateLerpBlur;
import com.ispd.sfcam.pdEngine.glEngine;
import com.ispd.sfcam.pdEngine.glEngineCapture;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.SFTunner;
import com.ispd.sfcam.utils.SFTunner2;
import com.ispd.sfcam.utils.sofCache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

import static android.os.Environment.getExternalStorageDirectory;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;


public class MainActivity extends AppCompatActivity {

    private static String TAG = "SFCam-MainActivity";

    private static boolean useCamera2API = false;

    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String PERMISSION_MIC = Manifest.permission.RECORD_AUDIO;

    public static final int ORIENTATION_0 = 1;
    public static final int ORIENTATION_90 = 2;
    public static final int ORIENTATION_180 = 3;
    public static final int ORIENTATION_270 = 4;

    private static final int NUMBER_OF_SF_OPTION = 4;
    public static final int SF_OPT_FG_PREVIEW_BG_PICTURES = 0; //Fix it
    public static final int SF_OPT_FG_PREVIEW_BG_MOVIE = 1; //Fix it
    public static final int SF_OPT_FG_PREVIEW_BG_CUSTOM = 2; //Fix it
    public static final int SF_OPT_FG_CUSTOM_BG_CUSTOM = 3; //Fix it
    private Fragment mFragment;

    private static View mMainLayout;
    private ImageButton mBtnCamSwitch;
    private ImageButton mBtnCapture;
    private Button mBtnSF, mBtnCartoon, mBtnOutFocus, mBtnHighLight;

    private ImageButton mBtnSFOnOff;
    private int mSFOnOff = 1;

    private int mCartoonOption = 0;
    private int mSFOption = SF_OPT_FG_PREVIEW_BG_PICTURES; //0:Demo Picture, 1:Demo Movie, 2:BG is User's Picture/Movie, 3:Edit mode
    private int mSFReset = 1;

    private ImageButton mBtnThumbnail;
    private Bitmap mThumbnailBitmap;
    private ImageView mImgThumbnailView;

    private ImageButton mBtnBlurSize;
    private TextView mTextBlurLevelView;
    private static int mBlurSize = 3;
    private static int mBlurSizeSaveForVideo;

    private TextView mTextTopLeftView;

    private glEngine mGlEngine;

    private ImageButton mBtnVideoCap;
    private boolean mVideoStatusBool = false;
    private boolean mCaptureOnBool = false;
    private boolean mMovieOnBool = false;

    //need to modify...
    private boolean mPictureSaved = true;

    //Handler Messages
    public static final int H_REFRESH_GALLERY = 1;
    public static final int H_TOPLEFT_INFO = 2;
    public static final int H_THUMBNAIL = 4;
    public static final int H_ENABLE_CAPTURE_BUTTON = 5;
    public static final int H_SET_MOVING_HANDLER = 6;
    public static final int H_CHECK_SF_CUSTOM_MODE = 7;

    private static int mCurrentMode = aiCamParameters.CARTOON_MODE;
    private OrientationEventListener mOrientationListener;

    private Animation mAnimationRotate;

    private Button mImgBtnSFSubMenu1;
    private Button mImgBtnSFSubMenu2;
    private Button mImgBtnSFSubMenu3;
    private Button mImgBtnSFSubMenu4;

    @Override
    protected void onPause() {
        mOrientationListener.disable();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOrientationListener.enable();
    }

    @Override
    protected void onDestroy() {
        mOrientationListener.disable();
        releaseBitmaps();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        loadBitmaps(this);

        //좀 이상한데???
        while( true ) {
            if (hasPermission()) {
                Log.d(TAG, "setFragment");
                setFragment();
                break;
            } else {
                Log.d(TAG, "requestPermission");
                requestPermission();
            }
        }

        ((cameraFragment) mFragment).setMainHandler(mHandler);
        SFTunner.readSFDatas();
        SFTunner.readSFTuneData();
        SFTunner2.readTuneData();
        //CreateLerpBlur.updateMatrix();

        LayoutInflater inflater = getLayoutInflater();
        mMainLayout = (View)inflater.inflate(R.layout.ui_main, null);

        RelativeLayout.LayoutParams previewLayout2 = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addContentView(mMainLayout, previewLayout2);
        buttonSettings();

        loadThumbnail();
        mOrientationListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int arg0) {

                //arg0: 기울기 값

                Log.o(TAG, "Orientation: "
                        + String.valueOf(arg0));
                // 0˚ (portrait)
                if (arg0 >= 315 || arg0 < 45) {
                    glEngine.SetCurrentOrientation(ORIENTATION_0);
                }
                // 90˚
                else if (arg0 >= 45 && arg0 < 135) {
                    glEngine.SetCurrentOrientation(ORIENTATION_90);
                }
                // 180˚
                else if (arg0 >= 135 && arg0 < 225) {
                    glEngine.SetCurrentOrientation(ORIENTATION_180);
                }
                // 270˚ (landscape)
                else if (arg0 >= 225 && arg0 < 315) {
                    glEngine.SetCurrentOrientation(ORIENTATION_270);
                }
            }
        };
        mOrientationListener.enable();
        mAnimationRotate = AnimationUtils.loadAnimation(this, R.anim.anim_rotate);
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_MIC) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                    shouldShowRequestPermissionRationale(PERMISSION_STORAGE) ||
                        shouldShowRequestPermissionRationale(PERMISSION_MIC)) {
                Toast.makeText(MainActivity.this,
                        "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE, PERMISSION_MIC}, PERMISSIONS_REQUEST);
        }
    }

    protected void setFragment() {
        checkCameraLegacy();

        if (useCamera2API) {
            Log.d(TAG, "Use Camera 2 API");
            mFragment = new camera2Fragment();
        } else {
            Log.d(TAG, "Use Camera 1 API");
            mFragment = new cameraFragment();
        }

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, mFragment)
                .commit();
    }

    public void checkCameraLegacy() {
        boolean legacy = false;
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
            legacy = true;
        } else {
            CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
            try {
                if (manager != null) {
                    for (String cameraId : manager.getCameraIdList()) {
                        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                        Integer deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                        if (deviceLevel != null && deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                            legacy = true;
                        }
                    }
                }
            } catch (CameraAccessException | NullPointerException e) {
                Log.w(TAG, "Camera Manger Error", e);
            }
        }

        Log.d(TAG, "legacy : "+legacy);

        if (legacy) {
            useCamera2API = false;
        } else {
            useCamera2API = true;
        }
    }

    private static int mCartoonBGIndex = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (useCamera2API) {

        }
        else
        {
            ((cameraFragment)mFragment).onKeyDown(keyCode, event);
            SFTunner.readSFTuneData();
            SFTunner2.readTuneData();
            //CreateLerpBlur.updateMatrix();
        }

        //for cartoon bg test
        if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            mCartoonBGIndex += 1;
            if (mCartoonBGIndex == CreateCartoon.mNumOfBG) mCartoonBGIndex = 0;
            CreateCartoon.SetCurrentBG(mCartoonBGIndex);
        }

        return true;
    }

    private void buttonSettings()
    {
        mBtnCamSwitch = (ImageButton)findViewById(R.id.ibtn_camera_switch);
        mBtnSF = (Button)findViewById(R.id.btn_sf);
        //mBtnSF.setVisibility(View.GONE);
        mBtnCartoon = (Button)findViewById(R.id.btn_cartoon);
        mBtnOutFocus = (Button)findViewById(R.id.btn_outfocus);
        mBtnHighLight = (Button)findViewById(R.id.btn_highlight);
        mBtnCapture = (ImageButton) findViewById(R.id.ibtn_capture);

        mBtnSF.setTextColor(Color.parseColor("#FFBEBDBD"));
        mBtnCartoon.setTextColor(Color.parseColor("#FFFFBB33"));
        mBtnOutFocus.setTextColor(Color.parseColor("#FFBEBDBD"));
        mBtnHighLight.setTextColor(Color.parseColor("#FFBEBDBD"));

        mBtnCamSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (useCamera2API) {

                }
                else
                {
                    aiCamParameters.mCameraLocationInt = 1 - aiCamParameters.mCameraLocationInt;

//                    if( aiCamParameters.mCameraLocationInt == 0 )
//                    {
//                        mBtnSF.setVisibility(View.GONE);
//                    }
//                    else
//                    {
//                        mBtnSF.setVisibility(View.VISIBLE);
//                    }

                    ((cameraFragment) mFragment).cameraChanged();

                    stopCustomVideoMode();

                }

                //movingChecker.resetMoving();
                //((cameraFragment)mFragment).resetMoving();
            }
        });

        mBtnSF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mSFReset == 0) {
//                    mSFOption = 1 - mSFOption;
                    mSFOption = ++mSFOption % NUMBER_OF_SF_OPTION; //NUMBER_OF_SF_OPTION = 4

                } else {
                    mSFOption = 0;
                }

                mBtnCartoon.setTextColor(Color.parseColor("#FFBEBDBD"));
                mBtnOutFocus.setTextColor(Color.parseColor("#FFBEBDBD"));
                mBtnHighLight.setTextColor(Color.parseColor("#FFBEBDBD"));
                mBtnSF.setTextColor(Color.parseColor("#FFFFBB33"));

                mBlurSize = 1;
                setBlurModeSize();
                glEngine.setBlurSize(mBlurSize);

                CreateLerpBlur.setObjCurrentMode(1);
                jniController.setCurrentAlMode(1);

                mCurrentMode = aiCamParameters.SF_MODE;
                glEngine.setSFCamMode(aiCamParameters.SF_MODE, mSFOption);

                //movingChecker.resetMoving();
                ((cameraFragment)mFragment).resetMoving();

                mSFReset = 0;

                if(mSFOption == SF_OPT_FG_PREVIEW_BG_CUSTOM) {
                    ((cameraFragment) mFragment).SetCustomEditFlag(true);
                    mImgBtnSFSubMenu1.setVisibility(View.VISIBLE); //pick image from Gallery
                    mImgBtnSFSubMenu2.setVisibility(View.VISIBLE); //pick image from Camera
                    mImgBtnSFSubMenu3.setVisibility(View.VISIBLE); //pick video from Gallery
                    mImgBtnSFSubMenu4.setVisibility(View.VISIBLE); //pick video from Camera
//                    mImgBtnSFSubMenu5.setVisibility(View.VISIBLE);
                }
                else if(mSFOption == SF_OPT_FG_CUSTOM_BG_CUSTOM) {
                    ((cameraFragment) mFragment).SetCustomEditFlag(true);
                    mImgBtnSFSubMenu1.setVisibility(View.VISIBLE);
                    mImgBtnSFSubMenu2.setVisibility(View.VISIBLE);
                    mImgBtnSFSubMenu3.setVisibility(View.VISIBLE);
                    mImgBtnSFSubMenu4.setVisibility(View.VISIBLE);
//                    mImgBtnSFSubMenu5.setVisibility(View.VISIBLE);
                }
                else {
                    ((cameraFragment) mFragment).SetCustomEditFlag(false);
                    mImgBtnSFSubMenu1.setVisibility(View.INVISIBLE);
                    mImgBtnSFSubMenu2.setVisibility(View.INVISIBLE);
                    mImgBtnSFSubMenu3.setVisibility(View.INVISIBLE);
                    mImgBtnSFSubMenu4.setVisibility(View.INVISIBLE);
//                    mImgBtnSFSubMenu5.setVisibility(View.VISIBLE);
                }
            }
        });

        mBtnCartoon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCustomVideoMode();

                mBtnSF.setTextColor(Color.parseColor("#FFBEBDBD"));
                mBtnOutFocus.setTextColor(Color.parseColor("#FFBEBDBD"));
                mBtnHighLight.setTextColor(Color.parseColor("#FFBEBDBD"));
                mBtnCartoon.setTextColor(Color.parseColor("#FFFFBB33"));

                mBlurSize = 3;
                setBlurModeSize();
                glEngine.setBlurSize(mBlurSize);

                CreateLerpBlur.setObjCurrentMode(2);
                jniController.setCurrentAlMode(2);

                mCurrentMode = aiCamParameters.CARTOON_MODE;
                mCartoonOption = 1 - mCartoonOption;
                glEngine.setSFCamMode(aiCamParameters.CARTOON_MODE, mCartoonOption);

                //movingChecker.resetMoving();
                ((cameraFragment)mFragment).resetMoving();

                mSFReset = 1;
            }
        });

        mBtnOutFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCustomVideoMode();

                mBtnSF.setTextColor(Color.parseColor("#FFBEBDBD"));
                mBtnCartoon.setTextColor(Color.parseColor("#FFBEBDBD"));
                mBtnHighLight.setTextColor(Color.parseColor("#FFBEBDBD"));
                mBtnOutFocus.setTextColor(Color.parseColor("#FFFFBB33"));

                mBlurSize = 3;
                setBlurModeSize();
                glEngine.setBlurSize(mBlurSize);

                CreateLerpBlur.setObjCurrentMode(3);
                jniController.setCurrentAlMode(3);

                mCurrentMode = aiCamParameters.OF_MODE;
                glEngine.setSFCamMode(aiCamParameters.OF_MODE, -1);

                //movingChecker.resetMoving();
                ((cameraFragment)mFragment).resetMoving();

                mSFReset = 1;
            }
        });

        mBtnHighLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCustomVideoMode();

                mBtnSF.setTextColor(Color.parseColor("#FFBEBDBD"));
                mBtnCartoon.setTextColor(Color.parseColor("#FFBEBDBD"));
                mBtnOutFocus.setTextColor(Color.parseColor("#FFBEBDBD"));
                mBtnHighLight.setTextColor(Color.parseColor("#FFFFBB33"));

                mBlurSize = 3;
                setBlurModeSize();
                glEngine.setBlurSize(mBlurSize);

                CreateLerpBlur.setObjCurrentMode(3);
                jniController.setCurrentAlMode(3);

                Log.d(TAG, "studio-mode-1");
                mCurrentMode = aiCamParameters.HIGHLIGHT_MODE;
                glEngine.setSFCamMode(aiCamParameters.HIGHLIGHT_MODE, -1);
                jniController.setStudioModeJni(0);

                //movingChecker.resetMoving();
                ((cameraFragment)mFragment).resetMoving();

                mSFReset = 1;
            }
        });

        CreateLerpBlur.setObjCurrentMode(2);
        jniController.setCurrentAlMode(2);

        Log.d(TAG, "mCurrentMode : "+mCurrentMode);
        if( mCurrentMode == aiCamParameters.SF_MODE )
        {
            //mBtnSF.callOnClick();
            mBtnCartoon.setTextColor(Color.parseColor("#FFBEBDBD"));
            mBtnOutFocus.setTextColor(Color.parseColor("#FFBEBDBD"));
            mBtnHighLight.setTextColor(Color.parseColor("#FFBEBDBD"));
            mBtnSF.setTextColor(Color.parseColor("#FFFFBB33"));
        }
        else if( mCurrentMode == aiCamParameters.CARTOON_MODE )
        {
            //mBtnCartoon.callOnClick();
            mBtnSF.setTextColor(Color.parseColor("#FFBEBDBD"));
            mBtnOutFocus.setTextColor(Color.parseColor("#FFBEBDBD"));
            mBtnHighLight.setTextColor(Color.parseColor("#FFBEBDBD"));
            mBtnCartoon.setTextColor(Color.parseColor("#FFFFBB33"));
        }
        else if( mCurrentMode == aiCamParameters.OF_MODE )
        {
            //mBtnOutFocus.callOnClick();
            mBtnSF.setTextColor(Color.parseColor("#FFBEBDBD"));
            mBtnCartoon.setTextColor(Color.parseColor("#FFBEBDBD"));
            mBtnHighLight.setTextColor(Color.parseColor("#FFBEBDBD"));
            mBtnOutFocus.setTextColor(Color.parseColor("#FFFFBB33"));

        }
        else if( mCurrentMode == aiCamParameters.HIGHLIGHT_MODE )
        {
            //mBtnHighLight.callOnClick();
            mBtnSF.setTextColor(Color.parseColor("#FFBEBDBD"));
            mBtnCartoon.setTextColor(Color.parseColor("#FFBEBDBD"));
            mBtnOutFocus.setTextColor(Color.parseColor("#FFBEBDBD"));
            mBtnHighLight.setTextColor(Color.parseColor("#FFFFBB33"));
        }

        mBtnThumbnail = (ImageButton) findViewById(R.id.ibtn_thumbnail);
        mBtnThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mPictureSaved == true) {
                    Intent intent = new Intent();
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    intent.setType("image/*");
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivityForResult(intent, 100);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "Processing for Saving", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
        mImgThumbnailView = (ImageView)findViewById(R.id.imgv_thumbnail);

        mBtnBlurSize = (ImageButton) findViewById(R.id.ibtn_depth_level);
        mBtnBlurSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mBlurSize++;
                if (mBlurSize > 4) mBlurSize = 1;

                setBlurModeSize();
                glEngine.setBlurSize(mBlurSize);
            }
        });
        mTextBlurLevelView = (TextView) findViewById(R.id.tv_depth_level);

        mBtnVideoCap = (ImageButton) findViewById(R.id.ibtn_record);
        mBtnVideoCap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SegmentorMain.onOffContrast();
                drawViewer.onOffContrast();

//                mGlEngine = ((cameraFragment)mFragment).getGlEngine();
//                if( mGlEngine == null )
//                {
//                    Log.d(TAG, "mGlEngine : null");
//                }
//
//                if (mVideoStatusBool == false) {
//                    mVideoStatusBool = true;
//
//                    mCaptureOnBool = false;
//                    mMovieOnBool = true;
//                    aiCamParameters.mCaptureRunning = false;
//                    aiCamParameters.mMovieRunning = true;
//
//                    mBtnVideoCap.setImageDrawable(getResources().getDrawable(R.drawable.btn_recoding_stop, getApplicationContext().getTheme()));
//
//                    mBlurSizeSaveForVideo = mBlurSize;
//                    mBlurSize = 3;
//                    setBlurModeSize();
//                    glEngine.setBlurSize(mBlurSize);
//
//                    mBtnCamSwitch.setVisibility(View.INVISIBLE);
//                    mBtnBlurSize.setVisibility(View.INVISIBLE);
//                    mTextBlurLevelView.setText("");
//                    mBtnCapture.setEnabled(false);  //disable capture button
//
//                    if( mGlEngine != null )
//                    {
//                        mGlEngine.startVideoRecord();
//                    }
//                    Log.d("Video-Test", "startVideoCapture");
//                } else {
//                    Log.d("Video-Test", "mSofEngine.isMediaMuxerOn() : " + mGlEngine.isMediaMuxerOn());
//                    if (mGlEngine.isMediaMuxerOn() == false) {
//                        Toast toast = Toast.makeText(getApplicationContext(), "Video is not started", Toast.LENGTH_SHORT);
//                        toast.show();
//                        return;
//                    }
//
//                    mVideoStatusBool = false;
//                    mMovieOnBool = false;
//                    aiCamParameters.mCaptureRunning = false;
//                    aiCamParameters.mMovieRunning = false;
//
//                    mBtnVideoCap.setImageDrawable(getResources().getDrawable(R.drawable.btn_video_nor, getApplicationContext().getTheme()));
//                    mGlEngine.stopVideoRecord();
//
//                    mBtnCamSwitch.setVisibility(View.VISIBLE);
//                    mBtnBlurSize.setVisibility(View.VISIBLE);
//                    mBtnCapture.setEnabled(true);  //enable capture button
//
//                    mBlurSize = mBlurSizeSaveForVideo;
//                    setBlurModeSize();
//                    mTextBlurLevelView.setText("Level " + mBlurSize);
//
//                    glEngine.setBlurSize(mBlurSize);
//
//                    Log.d("Video-Test", "stopVideoCapture");
//                }
            }
        });

        mTextTopLeftView = (TextView)findViewById(R.id.tv_title);

		mBtnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                CreateEdgeFilter.testButton();
                if (glEngine.ReadyToCapture() == true) {

                    aiCamParameters.mCaptureRunning = true;
                    aiCamParameters.mMovieRunning = false;
                    CreateLerpBlur.setSaveStatus(true);

                    mBtnCapture.setAnimation(mAnimationRotate);
                    mBtnCapture.startAnimation(mAnimationRotate);

                    mBtnCapture.setEnabled(false);
                    mBtnVideoCap.setEnabled(false);

                    Thread soundThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            glEngineCapture.PlayShutterSound();
                        }
                    });
                    soundThread.start();

                    Thread captureThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if(((cameraFragment) mFragment).IsObjectDetectionMode()) {
                                glEngine.StartCaptureGL();
                            }
                            else {
                                ((cameraFragment) mFragment).StartCapture();
                                glEngine.SaveCurrTextureForCapture(true); //Do not update.
                            }
                        }
                    });
                    captureThread.start();
                }
            }
        });

        mBtnSFOnOff = (ImageButton)findViewById(R.id.ibtn_before_after);
        mBtnSFOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSFOnOff = 1 - mSFOnOff;

                if( mSFOnOff == 1 ) {
                    mBtnSFOnOff.setImageDrawable(getResources().getDrawable(R.drawable.bluron, getApplicationContext().getTheme()));
                }
                else
                {
                    mBtnSFOnOff.setImageDrawable(getResources().getDrawable(R.drawable.bluoff, getApplicationContext().getTheme()));
                }

                glEngine.setSFOnOff(mSFOnOff);
            }
        });

        mImgBtnSFSubMenu1 = findViewById(R.id.btn_sf_sub1);
        mImgBtnSFSubMenu1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSFOption == SF_OPT_FG_PREVIEW_BG_CUSTOM) {
                    customSFGotoGallery(PICK_IMG_FROM_GALLERY);


                } else if (mSFOption == SF_OPT_FG_CUSTOM_BG_CUSTOM) {
                    //TODO
                }
            }
        });
        mImgBtnSFSubMenu2 = findViewById(R.id.btn_sf_sub2);
        mImgBtnSFSubMenu2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSFOption == SF_OPT_FG_PREVIEW_BG_CUSTOM) {
                    customSFGotoCamera(PICK_IMG_FROM_CAMERA);
                }
            }
        });
        mImgBtnSFSubMenu3 = findViewById(R.id.btn_sf_sub3);
        mImgBtnSFSubMenu3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSFOption == SF_OPT_FG_PREVIEW_BG_CUSTOM) {
                    customSFGotoGallery(PICK_VIDEO_FROM_GALLERY);
                }
            }
        });
        mImgBtnSFSubMenu4 = findViewById(R.id.btn_sf_sub4);
        mImgBtnSFSubMenu4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSFOption == SF_OPT_FG_PREVIEW_BG_CUSTOM) {
                    customSFGotoCamera(PICK_VIDEO_FROM_CAMERA);
                }
            }
        });

    }

    private void loadThumbnail() {
        try {
            String imagePath = sofCache.Read();

            Log.d(TAG, "imagePath : " + imagePath);
            Log.d(TAG, "imagePath.contains(\"mp4\") : " + imagePath.contains("mp4"));

            if (imagePath.equals("") == false && imagePath.contains("mp4") == false) {

                ExifInterface exif = new ExifInterface(imagePath);
                String jpgWidth = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
                String jpgLength = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
                int width = Integer.parseInt(jpgWidth);
                int height = Integer.parseInt(jpgLength);
                Log.d(TAG, "[jpg-exif] width : " + width);
                Log.d(TAG, "[jpg-exif] length : " + height);

                int useLength;
                if (width < height) useLength = width;
                else useLength = height;

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = useLength / 138;

                mThumbnailBitmap = BitmapFactory.decodeFile(imagePath, options);

                Log.d(TAG, "mThumbnailBitmap w : " + mThumbnailBitmap.getWidth());
                Log.d(TAG, "mThumbnailBitmap h : " + mThumbnailBitmap.getHeight());

                mHandler.sendEmptyMessage(H_THUMBNAIL);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refreshGallery(File file) {

        try {
            sofCache.Write(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    private void setBlurModeSize() {
        mTextBlurLevelView.setText("Level " + mBlurSize);
        //jniController.setBlurSizeTune(mBlurSize, mVideoStatusBool);
        jniController.setBlurSizeTune(mBlurSize, false);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message inputMessage) {
            switch (inputMessage.what) {
                case H_REFRESH_GALLERY:
                    String picturePath = inputMessage.getData().getString("picturePath");
                    File outFile = new File(picturePath);
                    mPictureSaved = true;

                    refreshGallery(outFile);
                    loadThumbnail();
                    break;

                case H_TOPLEFT_INFO:
                    String fps = String.valueOf(inputMessage.arg1);
                    mTextTopLeftView.setText("SFCam-"+fps);
                    break;

                case H_THUMBNAIL:
                    mImgThumbnailView.setMaxWidth(125);
                    mImgThumbnailView.setMaxHeight(125);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    mThumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    Glide.with(MainActivity.this)
                            .asBitmap()
                            .load(stream.toByteArray())
                            //.asBitmap()
                            .centerCrop()
                            .transform(new CropCircleTransformation())
                            //.thumbnail(0.01f)
                            //.override(70, 70)
                            .into(mImgThumbnailView);
                    break;
                case H_ENABLE_CAPTURE_BUTTON:
                    Log.d(TAG, "H_ENABLE_CAPTURE_BUTTON : "+H_ENABLE_CAPTURE_BUTTON);

                    if( mBtnCapture.getAnimation() != null ) {
                        mBtnCapture.getAnimation().cancel();
                        mBtnCapture.clearAnimation();
                        mBtnCapture.setEnabled(true);
                        mBtnVideoCap.setEnabled(true);

                        aiCamParameters.mCaptureRunning = false;
                        aiCamParameters.mMovieRunning = false;
                        CreateLerpBlur.setSaveStatus(false);
                    }
                    break;
                case H_SET_MOVING_HANDLER:
                    ((cameraFragment) mFragment).setMovingHandler();
                    break;	
                case H_CHECK_SF_CUSTOM_MODE:
                    // glEngine 초기화가 끝난 직후에 보내는 메세지.
                    // 현재SFOption 을 보고 glEngine에 알려줘야함.
                    if(mCurrentMode == aiCamParameters.SF_MODE) {
                        mGlEngine = ((cameraFragment) mFragment).getGlEngine();
                        if (mGlEngine != null) {
                            if (mSFOptionSubBG == PICK_VIDEO_FROM_GALLERY ||
                                mSFOptionSubBG == PICK_VIDEO_FROM_CAMERA) {
                                mGlEngine.StartCustomVideo();
                            }
                        }
                    }
                default:
                    break;
            }
        }
    };

    private static BitmapStorage mBitmapStorage = null;
    private void loadBitmaps(Context context) {
        mBitmapStorage = new BitmapStorage(context);
    }

    public BitmapStorage GetBitmapStorage() {
        return mBitmapStorage;
    }

    private void releaseBitmaps() {
        mBitmapStorage.Release();
        mBitmapStorage = null;
    }

    public static final int PICK_IMG_FROM_GALLERY = 1;
    public static final int PICK_IMG_FROM_CAMERA = 2;
    public static final int PICK_VIDEO_FROM_GALLERY = 3;
    public static final int PICK_VIDEO_FROM_CAMERA = 4;
    private int mSFOptionSubBG = 0;
    private File mPickedFile;


    private void customSFGotoGallery(int mode) {

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        if (mode == PICK_IMG_FROM_GALLERY) {
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Get Album"), PICK_IMG_FROM_GALLERY);
        }
        else if(mode == PICK_VIDEO_FROM_GALLERY) {
            intent.setType("video/*");
            startActivityForResult(Intent.createChooser(intent, "Get Album"), PICK_VIDEO_FROM_GALLERY);
        }

    }

    private void customSFGotoCamera(int mode) {
        Intent intent = null;
        int pickMode = -1;
        if (mode == PICK_IMG_FROM_CAMERA) {
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            pickMode = PICK_IMG_FROM_CAMERA;
        } else if (mode == PICK_VIDEO_FROM_CAMERA) {
            intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            pickMode = PICK_VIDEO_FROM_CAMERA;
        }
        if(intent != null && pickMode != -1) {
            if (intent.resolveActivity(getPackageManager()) != null) {
                try {
                    mPickedFile = glEngineCapture.CreateTempFile(pickMode);
                    Log.d(TAG, "custom  : current Photo or Video Path = " + mPickedFile.getAbsolutePath());

                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }

                if (mPickedFile != null) {
                    String authority = "com.ispd.sfcam" + ".fileprovider"; //sally added
                    Uri photoUri = FileProvider.getUriForFile(this, authority, mPickedFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(intent, pickMode);
                } else {
                    Log.d(TAG, "custom : mPickedFile create fail");
                }
            }
        }
    }

    private void stopCustomVideoMode() {
        mGlEngine = ((cameraFragment) mFragment).getGlEngine();
        if (mGlEngine != null) {
            mGlEngine.ReleaseCustomVideoMode();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mGlEngine = ((cameraFragment) mFragment).getGlEngine();

        if (resultCode != Activity.RESULT_OK) { //사용자 선택이 취소된 경우임
            if (mPickedFile != null) {
                if (mPickedFile.exists()) {
                    if (mPickedFile.delete()) {
                        mPickedFile = null;
                    }
                }
            }

            Log.d(TAG, "PICK ACTION canceled");

        } else { //resultCode == Activity.RESULT_OK

            mSFOptionSubBG = requestCode;  //glEngine에 알려주기 위해 현재 상태 저장

            if (requestCode == PICK_IMG_FROM_GALLERY) {
//                if(path.contains(".jpg") || path.contains(".png")) //굳이 format을 제한할 필요 없음.
                {
                    Log.d(TAG, "custom data is Image");

                    Bitmap result = null;
                    try {
                        result = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());

                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }

                    mBitmapStorage.SetCustomBitmap(result); //저장할 필요가 있음. app pause & resume시 로드하기 위함.
                }
            }
            else if (requestCode == PICK_IMG_FROM_CAMERA) {
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Bitmap result = BitmapFactory.decodeFile(mPickedFile.getAbsolutePath(), bmOptions);

                mBitmapStorage.SetCustomBitmap(result); //저장할 필요가 있음. app pause & resume시 로드하기 위함.
                mPickedFile = null;
            }
            else if (requestCode == PICK_VIDEO_FROM_GALLERY ) {
                //URI to path
                Uri photoUri = data.getData();

                Cursor cursor = getContentResolver().query(photoUri, null, null, null, null);
                cursor.moveToFirst();
                int colidx = cursor.getColumnIndex(MediaStore.Video.Media.DATA);

                if(colidx > -1) {
//                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    String path = cursor.getString(colidx);
                    Log.d(TAG, "custom video file path : " + path); //for debug
                    mBitmapStorage.SetCustomVideoPath(path);
                }
               else {
                    //TODO :일단 경로를 못 얻어왔을 경우의 처리해야함. 사진일때도 처리해야함. 해당앱에서 아예 경로 정보를 주지 않음.
                    //TODO : 동영상파일앱, 파일탐색기 등에서 사진 및 동영상 불러올 때 경로가 제외된 파일이름만 넘어오는 문제 해결해야함.

                    Log.e(TAG, "custom - Failed to load video file");
                }
                cursor.close();
            }
            else if (requestCode == PICK_VIDEO_FROM_CAMERA) {
                String path = mPickedFile.getAbsolutePath();
                Log.d(TAG, "custom data path : " + path); //for debug

                mBitmapStorage.SetCustomVideoPath(path);
            }

            else {
                finishActivity(requestCode);
            }
        }
    }
}
