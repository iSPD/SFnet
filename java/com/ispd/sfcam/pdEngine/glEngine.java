package com.ispd.sfcam.pdEngine;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;

import com.ispd.sfcam.BitmapStorage;
import com.ispd.sfcam.MainActivity;
import com.ispd.sfcam.R;
import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.encoder.SofImageEncoder;
import com.ispd.sfcam.encoder.SofVideoEncoder;
import com.ispd.sfcam.jniController;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.SFTunner2;
import com.ispd.sfcam.utils.gammaManager;
import com.ispd.sfcam.utils.movingChecker;
import com.ispd.sfcam.utils.timeCheck;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Date;

import static android.os.Environment.getExternalStorageDirectory;
import static com.ispd.sfcam.MainActivity.H_ENABLE_CAPTURE_BUTTON;
import static com.ispd.sfcam.MainActivity.H_REFRESH_GALLERY;
import static com.ispd.sfcam.MainActivity.H_TOPLEFT_INFO;
import static com.ispd.sfcam.MainActivity.SF_OPT_FG_CUSTOM_BG_CUSTOM;
import static com.ispd.sfcam.MainActivity.SF_OPT_FG_PREVIEW_BG_CUSTOM;
import static com.ispd.sfcam.MainActivity.SF_OPT_FG_PREVIEW_BG_MOVIE;
import static com.ispd.sfcam.MainActivity.SF_OPT_FG_PREVIEW_BG_PICTURES;

public class glEngine implements SurfaceTexture.OnFrameAvailableListener {

    private static String TAG = "SFCam-glEngine";

    private Context mContext;
    private Surface mSurface;

    private static final int EGL_OPENGL_ES2_BIT = 4;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;

    // Camera Data
    private SurfaceTexture mCameraSurfaceTexture = null;
    private int mCameraTextureName[] = {-1};
    private int mInputCopyTexture[] = {-1};
    private int mInputCopyFBO[] = {-1};
    private int mInputCartoonTextures[] = {-1, -1};
    private int mInputCartoonFBOs[] = {-1, -1};
    private int mInputCartoonEdgeTextures[] = {-1};
    private int mInputCartoonEdgeFBOs[] = {-1};
    private int mInputSFTextures[] = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
    private int mInputSFBlurTextures[] = new int[16];
    private int mInputSFBlurFbo[] = new int[16];
    private boolean mSFBlurDone[] = new boolean[16];

    private int mInputSFTextures2[] = new int[50];
    private int mInputSFBlurTextures2[] = new int[50];
    private int mInputSFBlurFbo2[] = new int[50];
    private boolean mSFBlurDone2[] = new boolean[50];
    private int mInputSFTextureCustom[] = {-1};
    //    private int mInputSFTexturesCustom[] = { -1 };  //sally : user's picture
    private int mInputMaskTextureForSF[] = { -1 };
    private int mGammaTextureID = -1;
    //linear Gaussian
    private int mInputGaussianTextures[] = { -1, -1 };
    private int mInputGaussianFBOs[] = { -1, -1 };
    private int mInputMaskTextureForBlur[] =  { -1 };
    private int mInputMaskTextureForStudio[] =  { -1 };
    private int mCameraDebugTextureNameForBlur[] =  { -1 };
    //Feather
    private int mFullTexture[] =  { -1 };
    private int mInputGaussianTexturesFeather[] = { -1, -1 };
    private int mInputGaussianFBOsFeather[] = { -1, -1 };
    //For Moving
    private int mMovingTexture[] = { -1 };
    private int mMovingFbo[] = { -1 };

    // Gamma buffer
    private IntBuffer mGammaTablebuffer = null;

    // Encoder
    private int mVideoOutputTextures[] = { -1, -1 };
    private int mVideoOutputFBO[] = { -1, -1 };
    private int mUseVideoTexture = 0;

    private boolean mEncoderInit = false;
    private static SofVideoEncoder mEncoder =  null;
    private SofImageEncoder mImageEncoder;
    private EGLContext mCurrentEGLConext;
    private boolean isRecorded = false;
    private boolean mRecordThreadStop = false;

    //preview & movie sync
    private Object mPreviewSyncObject = new Object();
    private boolean mPreviewPrepared = false;

    private int mSfCount = 1;
    private int mSfCount2 = 0;
    private static boolean mSFCamOn = true;

    private static int mSFCamMode = aiCamParameters.CARTOON_MODE;
    private static int mCartoonOption = 0;
    private static int mSFOption = 0;
    private static int mStudioMode = -1;

    private static int mBlurTuneSize = 3;
    private static int mSaveBlurSize = -1;

    private boolean mUseObjAlg = true;
    private int mUseFastAlg = 2;

    private Handler mMainActiviyHandler;

    //For Capture
    private static glEngineCapture mGLCapture = null;
    private static boolean mFlagGLCapture = false;
    private static boolean mSaveCurrTextureForCapture = false;
    private static boolean mDoNotDrawSFForCapture = false;
    private static int mCurrSFTextureIdx1 = 0;
    private static int mCurrSFTextureIdx2 = 0;
    private int mTexBeautyForCapture = -1;
    private int mCamTextureForCapture[] = { -1 };
    private int mCamFBOForCapture[] = { -1 };

    //info
    private static int mFace = 0;

    private boolean mFlagHumanAppear = false;

    public glEngine(Context context, int width, int height, Surface surface) {

        mContext = context;

        if ( surface == null ) {
            throw new NullPointerException();
        }
        mSurface = surface;

        eglSetup();

        makeCurrent(true);

        //create Texture & FBO
        glEngineGL.setRotationBuffer();
        glEngineGL.initBasic(width, height);
        glEngineGL.initCartoon(context, width, height);
        glEngineGL.initEdge(width, height);
        glEngineGL.initSF(width, height);
        glEngineGL.initGaussian(width, height);
        glEngineGL.initGaussianResult(width, height);
        glEngineGL.initLerfBlurForFeather();
        glEngineGL.initLerfBlurForBlur();
        glEngineGL.initBeautifyFilter();
        CreateStudioEffect.initStudioMode(width, height);
        mGammaTextureID = makeGammaTableTexture(1.4f);

        createSurfaceTexture();
        createSurfaceTextureForCartoon();
        createSurfaceTextureForSF();
        createSurfaceTextureForBlur();
        createSurfaceTextureForMaskSF();
        createSurfaceTextureForFeather();
        createSurfaceTextureForLerpBlur();
        createSurfaceTextureForCapture();
        createSurfaceTextureForSFCustom();
        createSurfaceTextureForCustomVideo();

        mCurrentEGLConext = EGL14.eglGetCurrentContext();
        createSurfaceTextureForMovie();
        createMovingFbo();
        Log.d(TAG, "init");

        makeCurrent(false);
		mGLCapture = new glEngineCapture(context);
    }

    private void eglSetup() {

        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }

        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }

        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                //EGL_RECORDABLE_ANDROID, 0,
                EGL14.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
        }

        Log.d(TAG, "Configs Num=" + numConfigs[0]);

        // Configure context for OpenGL ES 2.0.
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };

        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT, attrib_list, 0);
        checkEglError("eglCreateContext");

        if (mEGLContext == null) {
            throw new RuntimeException("null context");
        }

        Log.d(TAG, "Context=" + mEGLContext);

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };

        mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface, surfaceAttribs, 0);
        checkEglError("eglCreateWindowSurface");
        if (mEGLSurface == null) {
            throw new RuntimeException("surface was null");
        }

        Log.d(TAG, "Surface=" + mEGLSurface);
    }

    public void makeCurrent(boolean enable) {

        if ( enable ) {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                throw new RuntimeException("eglMakeCurrent failed");
            }
        }
        else {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
                throw new RuntimeException("eglMakeCurrent failed (0)");
            }
        } // !enable
    }

    public boolean swapBuffers() {
        return EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
    }

    private void checkEglError(String msg) {
        boolean failed = false;
        int error;
        while ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            Log.e(TAG, msg + ": EGL error: 0x" + Integer.toHexString(error));
            failed = true;
        }

        if (failed) {
            throw new RuntimeException("EGL error encountered (see log)");
        }
    }

    public void createSurfaceTexture() {
        if ( mCameraSurfaceTexture != null ) {
            mCameraSurfaceTexture.release();
            mCameraSurfaceTexture = null;
        }

        mCameraTextureName[0] = glEngineGL.createExternalTexture();

        mCameraSurfaceTexture = new SurfaceTexture(mCameraTextureName[0]);
        mCameraSurfaceTexture.setOnFrameAvailableListener(this);

        mInputCopyTexture[0] = glEngineGL.createTexture(aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 32);
        mInputCopyFBO[0] = glEngineGL.createFBO(mInputCopyTexture[0]);
    }


    public SurfaceTexture getSurfaceTexture()
    {
        return mCameraSurfaceTexture;
    }

    public void createSurfaceTextureForCartoon() {
        // Cartoon
        mInputCartoonTextures[0] = glEngineGL.createTexture((int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_CARTOON_FACTOR_F), (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_CARTOON_FACTOR_F), 32);
        mInputCartoonFBOs[0] = glEngineGL.createFBO(mInputCartoonTextures[0]);
        mInputCartoonTextures[1] = glEngineGL.createTexture((int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_CARTOON_FACTOR_F), (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_CARTOON_FACTOR_F), 32);
        mInputCartoonFBOs[1] = glEngineGL.createFBO(mInputCartoonTextures[1]);

        mInputCartoonEdgeTextures[0] = glEngineGL.createTextureManual((int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_EDGE_FACTOR_F), (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_EDGE_FACTOR_F), 32);
        mInputCartoonEdgeFBOs[0] = glEngineGL.createFBO(mInputCartoonEdgeTextures[0]);
    }

    public void createSurfaceTextureForSF() {
        BitmapStorage bitmapStorage = ((MainActivity)mContext).GetBitmapStorage();
        ArrayList bitmapList = bitmapStorage.GetBitmapList();

        for( int i = 0; i < 16; i++ )
        {
            Bitmap bitmap = (Bitmap)bitmapList.get(i);
            mInputSFTextures[i] = glEngineGL.createTextureBitmap(bitmap.getWidth(),  bitmap.getHeight(), bitmap);

            mInputSFBlurTextures[i] = glEngineGL.createTextureManual((int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_BLUR_FACTOR_F), (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_BLUR_FACTOR_F), 32);
            mInputSFBlurFbo[i] = glEngineGL.createFBO(mInputSFBlurTextures[i]);

            mSFBlurDone[i] = false;
        }

        for( int i = 0; i < 50; i++ )
        {
            Bitmap bitmap = (Bitmap)bitmapList.get(i+16);
            Log.o(TAG, "images2["+i+"] : "+bitmap.getWidth()+", "+bitmap.getHeight());
            mInputSFTextures2[i] = glEngineGL.createTextureBitmap(bitmap.getWidth(),  bitmap.getHeight(), bitmap);

            mInputSFBlurTextures2[i] = glEngineGL.createTextureManual((int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_BLUR_FACTOR_F), (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_BLUR_FACTOR_F), 32);
            mInputSFBlurFbo2[i] = glEngineGL.createFBO(mInputSFBlurTextures2[i]);

            mSFBlurDone2[i] = false;
        }
    }

    public void createSurfaceTextureForBlur()
    {
        // gaussian
        mInputGaussianTextures[0] = glEngineGL.createTexture((int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_BLUR_FACTOR_F), (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_BLUR_FACTOR_F), 32);
        mInputGaussianFBOs[0] = glEngineGL.createFBO(mInputGaussianTextures[0]);
        mInputGaussianTextures[1] = glEngineGL.createTexture((int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_BLUR_FACTOR_F), (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_BLUR_FACTOR_F), 32);
        mInputGaussianFBOs[1] = glEngineGL.createFBO(mInputGaussianTextures[1]);

        mInputMaskTextureForBlur[0] = glEngineGL.createTextureManual(aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR, aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR, 32);
        mInputMaskTextureForStudio[0] = glEngineGL.createTextureManual(aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR, aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR, 32);
        mCameraDebugTextureNameForBlur[0] = glEngineGL.createTextureManual(aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR, aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR, 32);
    }

    public void createSurfaceTextureForMaskSF()
    {
        mInputMaskTextureForSF[0] = glEngineGL.createTextureManual((int)((float)aiCamParameters.PREVIEW_WIDTH_I / (float)aiCamParameters.RESIZE_FEATHER_FACTOR),
                (int)((float)aiCamParameters.PREVIEW_HEIGHT_I / (float)aiCamParameters.RESIZE_FEATHER_FACTOR), 32);
    }

    public void createSurfaceTextureForFeather()
    {
        mFullTexture[0] = glEngineGL.createTextureFull((int)((float)aiCamParameters.PREVIEW_WIDTH_I / (float)aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR),
                (int)((float)aiCamParameters.PREVIEW_HEIGHT_I / (float)aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR), 32);

        mInputGaussianTexturesFeather[0] = glEngineGL.createTexture((int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR), (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR), 32);
        mInputGaussianFBOsFeather[0] = glEngineGL.createFBO(mInputGaussianTexturesFeather[0]);
        mInputGaussianTexturesFeather[1] = glEngineGL.createTexture((int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR), (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR), 32);
        mInputGaussianFBOsFeather[1] = glEngineGL.createFBO(mInputGaussianTexturesFeather[1]);
    }

    public void createSurfaceTextureForLerpBlur()
    {
        //glEngineGL.createOffScreenFBO(aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_FEATHER_FACTOR, aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_FEATHER_FACTOR); //sally
        glEngineGL.createOffScreenFBO(aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I); //sally
    }

    public void createMovingFbo()
    {
        mMovingTexture[0] = glEngineGL.createTexture(aiCamParameters.PREVIEW_WIDTH_I/8, aiCamParameters.PREVIEW_HEIGHT_I/8, 32);
        mMovingFbo[0] = glEngineGL.createFBO(mMovingTexture[0]);
    }

    public void createSurfaceTextureForMovie() {
        mVideoOutputTextures[0] = glEngineGL.createTexture(aiCamParameters.MOVIE_WIDTH_I, aiCamParameters.MOVIE_HEIGHT_I, 32);
        mVideoOutputFBO[0] = glEngineGL.createFBO(mVideoOutputTextures[0]);

        mVideoOutputTextures[1] = glEngineGL.createTexture(aiCamParameters.MOVIE_WIDTH_I, aiCamParameters.MOVIE_HEIGHT_I, 32);
        mVideoOutputFBO[1] = glEngineGL.createFBO(mVideoOutputTextures[1]);
    }

    public void createSurfaceTextureForCapture() {
        mCamTextureForCapture[0] = glEngineGL.createTexture((int)((float)aiCamParameters.PREVIEW_WIDTH_I), (int)((float)aiCamParameters.PREVIEW_HEIGHT_I), 32);
        mCamFBOForCapture[0] = glEngineGL.createFBO(mCamTextureForCapture[0]);
    }

    public void createSurfaceTextureForCustomVideo() {
        mCustomVideoTextureId[0] = glEngineGL.createTexture((int) ((float) aiCamParameters.PREVIEW_WIDTH_I), (int) ((float) aiCamParameters.PREVIEW_HEIGHT_I), 32);
        mCustomVideoFBOId[0] = glEngineGL.createFBO(mCustomVideoTextureId[0]);

        SetCustomVideoMode();
    }

    public void createSurfaceTextureForSFCustom() {
        BitmapStorage bitmapStorage = ((MainActivity) mContext).GetBitmapStorage();
        Bitmap bitmap = bitmapStorage.GetCustomBitmap();

        if (bitmap == null) {
            Log.d(TAG, "custom bitmap is null - glEngine");
            return;
        }

        if (mInputSFTextureCustom[0] > 0) {

            GLES20.glDeleteBuffers(1, mInputSFTextureCustom, 0);
        }
        mInputSFTextureCustom[0] = glEngineGL.createTextureBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap);
    }

    private void updateGammaTableTexture(int texture, float gamma) {

        int width = 256;
        int height = 1;

        // luminance texture
        if ( mGammaTablebuffer == null ) // create luminance buffer
            mGammaTablebuffer = ByteBuffer.allocateDirect(width*height*4).order(ByteOrder.nativeOrder()).asIntBuffer();
        else mGammaTablebuffer.position(0);

        int luminance = 0;
        int power = 0;

        for(int i = 0; i < height; ++i) {
            for(int j = 0; j < width; ++j) {
                power = (int)(Math.pow(j / 255.0f, 1.0f / gamma) * 255);
                power = power & 0x000000FF;
                luminance =  0xFF000000 | (power << 16) | (power << 8) | power;
                //Log.d(TAG, "w : "+j+", h : "+i+", power : "+power);
                mGammaTablebuffer.put(luminance);
            } // for j
        } // for i

//        for(int i = height; i > 0; --i) {
//            for(int j = width; j > 0; --j) {
//                power = (int)(Math.pow(j / 255.0f, 1.0f / gamma) * 255);
//                power = power & 0x000000FF;
//                luminance =  0xFF000000 | (power << 16) | (power << 8) | power;
//                //Log.d(TAG, "w : "+j+", h : "+i+", power : "+power);
//                mGammaTablebuffer.put(luminance);
//            } // for j
//        } // for i

        mGammaTablebuffer.position(0);

        glEngineGL.updateLuminanceTexture(texture, width, height, mGammaTablebuffer);

        //Log.d(TAG, "GammaTableTexture (update) Texture=" + texture + " w=" + width + " h=" + height + " gamma=" + gamma);

    }

    private int makeGammaTableTexture(float gamma) {

        int width = 256;
        int height = 1;

        int texture = glEngineGL.createLuminanceTexture(width, height);
        updateGammaTableTexture(texture, gamma);

        return texture;
    }

    public void release() {

        synchronized (this) {

            makeCurrent(true);

            ReleaseCustomVideoMode();

            if (mCameraSurfaceTexture != null) {
                mCameraSurfaceTexture.setOnFrameAvailableListener(null);
                mCameraSurfaceTexture.release();
                mCameraSurfaceTexture = null;
            }

            if (mCameraTextureName[0] > 0) {

                GLES20.glDeleteBuffers(1, mCameraTextureName, 0);
            }

            if (mInputCopyTexture[0] > 0) {

                GLES20.glDeleteBuffers(1, mInputCopyTexture, 0);
            }

            if (mInputCopyFBO[0] > 0) {

                GLES20.glDeleteBuffers(1, mInputCopyFBO, 0);
            }

            if (mInputCartoonTextures[0] > 0) {

                GLES20.glDeleteBuffers(2, mInputCartoonTextures, 0);
            }

            if (mInputCartoonFBOs[0] > 0) {

                GLES20.glDeleteBuffers(2, mInputCartoonFBOs, 0);
            }

            if (mInputCartoonEdgeTextures[0] > 0) {

                GLES20.glDeleteBuffers(1, mInputCartoonEdgeTextures, 0);
            }

            if (mInputCartoonEdgeFBOs[0] > 0) {

                GLES20.glDeleteBuffers(1, mInputCartoonEdgeFBOs, 0);
            }

            if (mInputMaskTextureForSF[0] > 0) {

                GLES20.glDeleteBuffers(1, mInputMaskTextureForSF, 0);
            }

            if (mInputSFTextures[0] > 0) {

                GLES20.glDeleteBuffers(16, mInputSFTextures, 0);
            }

            if (mInputSFBlurTextures[0] > 0) {
                GLES20.glDeleteBuffers(16, mInputSFBlurTextures, 0);
            }

            if (mInputSFBlurFbo[0] > 0) {
                GLES20.glDeleteBuffers(16, mInputSFBlurFbo, 0);
            }

            if (mInputSFTextures2[0] > 0) {

                GLES20.glDeleteBuffers(50, mInputSFTextures2, 0);
            }

            if (mInputSFBlurTextures2[0] > 0) {
                GLES20.glDeleteBuffers(50, mInputSFBlurTextures2, 0);
            }

            if (mInputSFBlurFbo2[0] > 0) {
                GLES20.glDeleteBuffers(50, mInputSFBlurFbo2, 0);
            }

            if (mInputGaussianTextures[0] > 0) {

                GLES20.glDeleteBuffers(2, mInputGaussianTextures, 0);
            }

            if (mInputGaussianFBOs[0] > 0) {

                GLES20.glDeleteBuffers(2, mInputGaussianFBOs, 0);
            }

            if (mInputMaskTextureForBlur[0] > 0) {

                GLES20.glDeleteBuffers(1, mInputMaskTextureForBlur, 0);
            }

            if (mInputMaskTextureForStudio[0] > 0) {

                GLES20.glDeleteBuffers(1, mInputMaskTextureForStudio, 0);
            }

            if (mCameraDebugTextureNameForBlur[0] > 0) {

                GLES20.glDeleteBuffers(1, mCameraDebugTextureNameForBlur, 0);
            }

            if (mFullTexture[0] > 0) {

                GLES20.glDeleteBuffers(1, mFullTexture, 0);
            }

            //For Moving
            if (mMovingTexture[0] > 0) {

                GLES20.glDeleteBuffers(1, mMovingTexture, 0);
            }

            if (mMovingFbo[0] > 0) {

                GLES20.glDeleteBuffers(1, mMovingFbo, 0);
            }

            //For Movie
            if (mVideoOutputTextures[0] > 0) {

                GLES20.glDeleteBuffers(2, mVideoOutputTextures, 0);
            }

            if (mVideoOutputFBO[0] > 0) {

                GLES20.glDeleteBuffers(2, mVideoOutputFBO, 0);
            }

            //For Capture
            if (mCamTextureForCapture[0] > 0) {

                GLES20.glDeleteBuffers(1, mCamTextureForCapture, 0);
            }

            if (mCamFBOForCapture[0] > 0) {

                GLES20.glDeleteBuffers(1, mCamFBOForCapture, 0);
            }

            //For Custom Image input
            if (mInputSFTextureCustom[0] > 0) {

                GLES20.glDeleteBuffers(1, mInputSFTextureCustom, 0);
            }

            //For Custom Video input
            if (mCustomVideoFBOId[0] > 0) {
                GLES20.glDeleteBuffers(1, mCustomVideoFBOId, 0);
            }

            if (mCustomVideoTextureId[0] > 0) {
                GLES20.glDeleteBuffers(1, mCustomVideoTextureId, 0);
            }

            glEngineGL.release();
            CreateStudioEffect.release();

            makeCurrent(false);

            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglTerminate(mEGLDisplay);

            // null everything out so future attempts to use this object will cause an NPE
            mEGLDisplay = null;
            mEGLContext = null;
            mEGLSurface = null;
            mSurface = null;
        }
    }

    private Date lastTime = new Date();
    // lastTime은 기준 시간입니다.
    // 처음 생성당시의 시간을 기준으로 그 다음 1초가 지날때마다 갱신됩니다.
    private long frameCount = 0, nowFps = 0;
    // frameCount는 프레임마다 갱신되는 값입니다.
    // nowFps는 1초마다 갱신되는 값입니다.
    void count(){
        Date nowTime = new Date();
        long diffTime = nowTime.getTime() - lastTime.getTime();
        // 기준시간 으로부터 몇 초가 지났는지 계산합니다.

        if (diffTime >= 1000) {
            // 기준 시간으로 부터 1초가 지났다면
            nowFps = frameCount;
            Log.d("nowFps", "gpu nowFps : "+nowFps);

            Message msg = new Message();
            msg.what = H_TOPLEFT_INFO;
            msg.arg1 = (int)nowFps;
            mMainActiviyHandler.sendMessage(msg);

            frameCount = 0;
            // nowFps를 갱신하고 카운팅을 0부터 다시합니다.
            lastTime = nowTime;
            // 1초가 지났으므로 기준 시간또한 갱신합니다.
        }

        frameCount++;
        // 기준 시간으로 부터 1초가 안지났다면 카운트만 1 올리고 넘깁니다.
    }

    private Date lastTime2 = new Date();

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

        synchronized (this) {
            count();
            if(surfaceTexture == mCustomVideoSurfaceTexture) {
                return; //draw 하지 않고 리턴
           }
            if( mSFCamOn == true ) {

                if (mSFCamMode == aiCamParameters.SF_MODE) {
                    //if (mUseObjAlg == true) {
                    if (false) {
                        mCartoonOption = mSFOption;
                        glEngineGL.setCartoonOption(mCartoonOption);
                        if (mFlagGLCapture == false) {
                            drawCartoon(surfaceTexture);
                        }
                        else { //sally-capture
                            drawCartoonForCapture();
                        }
                    } else {
                        if (mFlagGLCapture == false) {
                            drawSF(surfaceTexture);
                        }
                        else {
                            if(mUseObjAlg == true) {
                                drawSF(surfaceTexture);
                            }
                            else {
                                drawSFForCapture(); //sally-capture
                            }
                        }
                    }
                } else if (mSFCamMode == aiCamParameters.CARTOON_MODE) {
                    if (mFlagGLCapture == false) {
                        drawCartoon(surfaceTexture);
                    }
                    else { //sally-capture
                        if(mUseObjAlg == true) {
                            drawCartoon(surfaceTexture);
                        }
                        else {
                            drawCartoonForCapture();
                        }
                    }
                } else if (mSFCamMode == aiCamParameters.OF_MODE || mSFCamMode == aiCamParameters.HIGHLIGHT_MODE) {
                    if (mFlagGLCapture == false) {
                        drawBlur(surfaceTexture);
                        //drawLerfBlur(surfaceTexture);
                    }
                    else { //sally-capture
                        if (mUseObjAlg == true) {
                            drawBlur(surfaceTexture);
                        } else {
                            drawBlurForCapture();
                        }
                    }
                }
            }
            else
            {
                drawBasic();
            }
        }
    }

    public void drawBasic()
    {
        if (mCameraSurfaceTexture != null && mCameraTextureName[0] > 0) {
            makeCurrent(true);

            mCameraSurfaceTexture.updateTexImage();

            int cameraIndex = aiCamParameters.mCameraLocationInt;
            glEngineGL.drawBasic(-1, mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);

            swapBuffers();
            makeCurrent(false);
        }
    }

//    public int drawFeatherByBlur(int []textureID)
//    {
//        int cameraIndex = aiCamParameters.mCameraLocationInt;
//
//        int horiCount = 0;
//        int vertiCount = 0;
//
//        int width = (int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR);
//        int height = (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR);
//
//        //시작
//        int iterations = 4;
//        SFTunner2.mBlurCount[3] = 4;
//        SFTunner2.mBlurSize3[3] = 17;
//
//        horiCount++;
//        glEngineGL.drawGaussian(width, height, 1, mInputGaussianFBOsFeather[0], mCameraTextureName[0], textureID[0], mFullTexture[0], -1, cameraIndex, horiCount);
//
//        for (int i = 1; i < iterations; i++)
//        {
//            vertiCount++;
//            glEngineGL.drawGaussian(width, height, 2, mInputGaussianFBOsFeather[1], mCameraTextureName[0], mInputGaussianTexturesFeather[0], mFullTexture[0], -1, cameraIndex, vertiCount);
//            horiCount++;
//            glEngineGL.drawGaussian(width, height, 1, mInputGaussianFBOsFeather[0], mCameraTextureName[0], mInputGaussianTexturesFeather[1], mFullTexture[0], -1, cameraIndex, horiCount);
//        }
//
//        vertiCount++;
//        glEngineGL.drawGaussian(width, height, 2, mInputGaussianFBOsFeather[1], mCameraTextureName[0], mInputGaussianTexturesFeather[0], mFullTexture[0], -1, cameraIndex, vertiCount);
//
//        return mInputGaussianTexturesFeather[1];
//    }

    public void drawBlurForSF(int target, int inputTex)
    {
        int cameraIndex = aiCamParameters.mCameraLocationInt;

        if ( mBlurTuneSize != 0 ) {

            int iterations = 1;

            SFTunner2.readNeedTuneData();
            iterations = SFTunner2.mMaxBlurCount;
            Log.d("iterations", "iterations : " + iterations);

            for (int i = 0; i < 4; i++) {
                if (mBlurTuneSize == 4) {
                    SFTunner2.mBlurSize[i] = 7;//SFTunner2.mBlurSize3[i] + 2;
                } else if (mBlurTuneSize == 3) {
                    SFTunner2.mBlurSize[i] = 5;//SFTunner2.mBlurSize3[i];
                } else if (mBlurTuneSize == 2) {
                    SFTunner2.mBlurSize[i] = 3;//SFTunner2.mBlurSize2[i];
                } else if (mBlurTuneSize == 1) {
                    SFTunner2.mBlurSize[i] = 1;//SFTunner2.mBlurSize1[i];
                }
            }

            for (int k = 0; k < 4; k++) {
                Log.d(TAG, "mBlurSize[" + k + "] : " + SFTunner2.mBlurSize[k]);
            }


            int horiCount = 0;
            int vertiCount = 0;

            horiCount++;

            int width = (int) ((float) aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_FACTOR_F);
            int height = (int) ((float) aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_FACTOR_F);

            boolean useOes = false;
            int tex_mask = mFullTexture[0];
            int featherTexId = mFullTexture[0];

            glEngineGL.drawGaussian(width, height, 0, mInputGaussianFBOs[0], useOes, inputTex, inputTex,
                    tex_mask, featherTexId, cameraIndex, horiCount);

            for (int i = 1; i < iterations; i++) {
                vertiCount++;
                glEngineGL.drawGaussian(width, height, 2, mInputGaussianFBOs[1], useOes, inputTex, mInputGaussianTextures[0],
                        tex_mask, featherTexId, cameraIndex, vertiCount);
                horiCount++;
                glEngineGL.drawGaussian(width, height, 1, mInputGaussianFBOs[0], useOes, inputTex, mInputGaussianTextures[1],
                        tex_mask, featherTexId, cameraIndex, horiCount);
            }

            vertiCount++;
            glEngineGL.drawGaussian(width, height, 2, target, useOes, inputTex, mInputGaussianTextures[0],
                    tex_mask, featherTexId, cameraIndex, vertiCount);
        }
    }

    int tex_beauty = -1;
    public void drawSF(SurfaceTexture surfaceTexture)
    {
        if (mCameraSurfaceTexture != null && mCameraTextureName[0] > 0) {
            makeCurrent(true);

            mCameraSurfaceTexture.updateTexImage();

            if(mCustomVideoStarted == true) {
                mCustomVideoSurfaceTexture.updateTexImage(); //video-code
                Log.d(TAG,"video- updateTexImage()");
            }
            int cameraIndex = aiCamParameters.mCameraLocationInt;

            //Moving Start
//            long startMovingTime = SystemClock.uptimeMillis();
//            glEngineGL.drawBasic(mMovingFbo[0], mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I/8, aiCamParameters.PREVIEW_HEIGHT_I/8, 90, cameraIndex);
//            mGLCapture.CaptureTest(mMovingFbo[0], 0, 0, aiCamParameters.PREVIEW_WIDTH_I/8, aiCamParameters.PREVIEW_HEIGHT_I/8, false);
//            Log.d(TAG, "moving gl time : "+(SystemClock.uptimeMillis()-startMovingTime));
            //Moving End

            if( mSFCamOn == true && mDoNotDrawSFForCapture == false)
            {
//                glEngineGL.updateMaskTextureForSF(mInputMaskTextureForSF);

                double gamma = gammaManager.calcValueAnimation(gammaManager.getCurrentValue());
                gammaManager.setAnimationGamma(gamma);
                Log.d(TAG, "gammaManager.getCurrentValue() : " + gammaManager.getCurrentValue() + ", gamma : " + gamma);
                updateGammaTableTexture(mGammaTextureID, (float) gamma);

                //int tex_beauty = -1;
                int useCartoon = 0;
                if( mUseObjAlg == false ) {
                    useCartoon = 1;

                    Date nowTime = new Date();
                    long diffTime = nowTime.getTime() - lastTime2.getTime();

                    if (diffTime >= 0) { //170
                        Log.d(TAG, "skip frame");
                        lastTime2 = nowTime;

                        glEngineGL.updateMaskTextureForSF(mInputMaskTextureForSF);
                        tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, mInputMaskTextureForSF[0], useCartoon);
                    }

//                    if( movingChecker.getMovingRunning() == true ) {
//                        if (diffTime >= 100) { //170
//                            Log.d(TAG, "skip frame");
//                            lastTime2 = nowTime;
//                            tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, mInputMaskTextureForSF[0], 1);
//                        }
//                    }
//                    else
//                    {
//                        lastTime2 = nowTime;
//                        tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, mInputMaskTextureForSF[0], 1);
//                    }
                }
                else {
                    useCartoon = -1;
					
					glEngineGL.updateMaskTextureForSF(mInputMaskTextureForSF);
                    tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, mInputMaskTextureForSF[0], useCartoon);
                }
				
                if(mSaveCurrTextureForCapture == true) {
                    mTexBeautyForCapture = glEngineGL.drawSFBeautyForCapture(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, mInputMaskTextureForSF[0], useCartoon);
                }

                //texid = drawFeatherByBlur(mInputMaskTextureForSF);
                int tex_feather = -1;
                if( mUseFastAlg == 0) {
                    Log.d(TAG, "[drawSF] mUseFastAlg1 : "+mUseFastAlg);
                    tex_feather = glEngineGL.drawFeatherFast(mInputMaskTextureForSF[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mUseObjAlg, mUseFastAlg, false);
                }
                else
                {
                    if( aiCamParameters.mCameraLocationInt == 0 )
                    {
                        //if( mUseFastAlg == 1 && mUseObjAlg == false ) {
                        if( mUseFastAlg == 1 ) {
                            Log.d(TAG, "[drawSF] mUseFastAlg2 : "+mUseFastAlg);
                            tex_feather = glEngineGL.drawFeatherFast(mInputMaskTextureForSF[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mUseObjAlg, mUseFastAlg, false);
                        }
                        else
                        {
                            Log.d(TAG, "[drawSF] mUseFastAlg3 : "+mUseFastAlg);
                            tex_feather = glEngineGL.drawFeatherNormal(mInputMaskTextureForSF[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mUseObjAlg, mUseFastAlg, false);
                        }
                    }
                    else {
                        Log.d(TAG, "[drawSF] mUseFastAlg4 : "+mUseFastAlg);
                        tex_feather = glEngineGL.drawFeatherNormal(mInputMaskTextureForSF[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mUseObjAlg, mUseFastAlg, false);
                    }
                }

//                    double gamma2 = gammaManager.calcValueAnimation(gammaManager.getCurrentValue());
//                    gammaManager.setAnimationGamma(gamma2);
//                    Log.d(TAG, "gammaManager.getCurrentValue() : " + gammaManager.getCurrentValue() + ", gamma2 : " + gamma2);
//
//                    updateGammaTableTexture(mGammaTextureID, (float) gamma2);
                int tex_background = -1;

                if (mSFOption == SF_OPT_FG_PREVIEW_BG_PICTURES) {
                    if (timeCheck.checkTime(2000) == true) {
                        mSfCount++;

                        if (mSfCount > 14) {
                            mSfCount = 1;
                        }
                    }

                    //glEngineGL.drawSF(-1, mCameraTextureName[0], mInputMaskTextureForSF[0], mInputSFTextures[mSfCount], mGammaTextureID, cameraIndex);
                    //glEngineGL.drawSF(-1, mCameraTextureName[0], mInputGaussianTexturesFeather[1], mInputSFTextures[mSfCount], mGammaTextureID, cameraIndex);
                    //glEngineGL.drawSF(-1, mCameraTextureName[0], texid, mInputSFTextures[mSfCount], mGammaTextureID, cameraIndex);

//                    glEngineGL.drawSF(-1, tex_beauty, tex_feather, mInputSFTextures[mSfCount], mGammaTextureID, mUseObjAlg, cameraIndex, false);
                    //glEngineGL.drawSF(-1, tex_beauty, mInputMaskTextureForSF[0], mInputSFTextures[mSfCount], mGammaTextureID, mUseObjAlg, cameraIndex, false);

                    if( mSaveBlurSize != mBlurTuneSize )
                    {
                        for(int i = 0; i < 16; i++)
                        {
                            mSFBlurDone[i] = false;
                        }
                        mSaveBlurSize = mBlurTuneSize;
                    }

                    if( mSFBlurDone[mSfCount] == false ) {
                        Log.d(TAG, "mSFBlurDone : "+mSfCount);
                        drawBlurForSF(mInputSFBlurFbo[mSfCount], mInputSFTextures[mSfCount]);

                        mSFBlurDone[mSfCount] = true;
                    }
					tex_background = mInputSFBlurTextures[mSfCount];

                } else if (mSFOption == SF_OPT_FG_PREVIEW_BG_MOVIE) {
                    if (timeCheck.checkTime(100) == true) {
                        mSfCount2++;

                        if (mSfCount2 > 49) {
                            mSfCount2 = 0;
                        }
                    }

                    //glEngineGL.drawSF(-1, mCameraTextureName[0], mInputMaskTextureForSF[0], mInputSFTextures2[mSfCount2], mGammaTextureID, cameraIndex);
//                    glEngineGL.drawSF(-1, tex_beauty, tex_feather, mInputSFTextures2[mSfCount2], mGammaTextureID, mUseObjAlg, cameraIndex, false);

                    if( mSaveBlurSize != mBlurTuneSize )
                    {
                        for(int i = 0; i < 50; i++)
                        {
                            mSFBlurDone2[i] = false;
                        }
                        mSaveBlurSize = mBlurTuneSize;
                    }

                    if( mSFBlurDone2[mSfCount2] == false ) {
                        Log.d(TAG, "mSFBlurDone2 : "+mSfCount2);
                        drawBlurForSF(mInputSFBlurFbo2[mSfCount2], mInputSFTextures2[mSfCount2]);

                        mSFBlurDone2[mSfCount2] = true;
                    }
					
					tex_background = mInputSFBlurTextures2[mSfCount2];
					 
                } else if (mSFOption == SF_OPT_FG_PREVIEW_BG_CUSTOM) {
                    if(mCustomVideoStarted == true) {
                        //FBO에 따로 그려서 텍스처 가져다 다시 그려야함.
                        if(mCustomVideoTextureOESId[0] > 0 && mCamFBOForCapture[0] > 0) {
                            glEngineGL.drawBasicZoomAndPan(mCamFBOForCapture[0], 1, mCustomVideoTextureOESId[0], -1, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                            tex_background = mCamTextureForCapture[0];
                        }

                    }
                    else {
                        //앱 시작 직후 custom 모드 진입시 tex_background의 값은 -1 이므로 일반 프리뷰 화면을 그리게 됨.
                        tex_background = mInputSFTextureCustom[0];
                        if(mInputSFTextureCustom[0] > 0 && mCamFBOForCapture[0] > 0 ) {
                            glEngineGL.drawBasicZoomAndPan(mCamFBOForCapture[0], 0, -1, mInputSFTextureCustom[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                            tex_background = mCamTextureForCapture[0];
                        }
                    }
                } else if (mSFOption == SF_OPT_FG_CUSTOM_BG_CUSTOM) {
                    //TODO : tex_background =
                }

                if(tex_background > 0) {
                    glEngineGL.drawSF(-1, tex_beauty, tex_feather, tex_background, mGammaTextureID, mUseObjAlg, cameraIndex, false);

                    if (isRecorded == true) {

                        synchronized (mPreviewSyncObject) {
                            mUseVideoTexture = 1 - mUseVideoTexture;
                            glEngineGL.drawSF(mVideoOutputFBO[mUseVideoTexture], tex_beauty, tex_feather, tex_background, mGammaTextureID, mUseObjAlg, cameraIndex, true);
                            Log.d(TAG, "[movie-sync] draw preview : " + (mUseVideoTexture));

                            mPreviewPrepared = true;
                            mPreviewSyncObject.notify();
                        }
                    }
                    if(mFlagGLCapture == true) {
                        mGLCapture.CaptureGL(-1, 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, false);
                        mFlagGLCapture = false;

                        mDoNotDrawSFForCapture = false;
                        mSaveCurrTextureForCapture = false;
                        mMainActiviyHandler.sendEmptyMessage(H_ENABLE_CAPTURE_BUTTON);
                    }
                }
                else {
                    glEngineGL.drawBasic(-1, mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex); //임시주석
                }
            } else {
                if(mDoNotDrawSFForCapture == false) {
                    glEngineGL.drawBasic(-1, mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                }
            }

			if(mSaveCurrTextureForCapture == true) {
                glEngineGL.drawBasic(mCamFBOForCapture[0], mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                mSaveCurrTextureForCapture = false;
                mDoNotDrawSFForCapture = true;
                mCurrSFTextureIdx1 = mSfCount;
                mCurrSFTextureIdx2 = mSfCount2;
            }
			//TODO : 이 코드가 머지 하면서 추가됨 위 코드랑 중복되는지 확인하기
			if (mFlagGLCapture == true && mUseObjAlg == true) {

                mGLCapture.CaptureGL(-1, 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, false);
                glEngineGL.drawBasic(mCamFBOForCapture[0], mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                mGLCapture.CaptureGL(mCamFBOForCapture[0], 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, true);

                Log.d(TAG, "sendEmptyMessage1");
                mFlagGLCapture = false;
                mMainActiviyHandler.sendEmptyMessage(H_ENABLE_CAPTURE_BUTTON);

            }
            if(mDoNotDrawSFForCapture == false) {
                swapBuffers();
            }
            makeCurrent(false);
        }
    }

    public void drawSFForCapture() {
        if (mCameraSurfaceTexture != null && mCameraTextureName[0] > 0) {
            makeCurrent(true);

            mCameraSurfaceTexture.updateTexImage();

            if(mCustomVideoStarted == true) {
                mCustomVideoSurfaceTexture.updateTexImage(); //video-code
                Log.d(TAG,"video- updateTexImage() capture routine");
            }

            int cameraIndex = aiCamParameters.mCameraLocationInt;

            if( mSFCamOn == true )
            {
                glEngineGL.updateMaskTextureForSF(mInputMaskTextureForSF);

                double gamma = gammaManager.calcValueAnimation(gammaManager.getCurrentValue());
                gammaManager.setAnimationGamma(gamma);
                Log.d(TAG, "gammaManager.getCurrentValue() : " + gammaManager.getCurrentValue() + ", gamma : " + gamma);
                updateGammaTableTexture(mGammaTextureID, (float) gamma);

//                int tex_feather = glEngineGL.drawFeather(mInputMaskTextureForSF[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mUseFastAlg, false);
                int tex_feather = glEngineGL.drawFeatherNormal(mInputMaskTextureForSF[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mUseObjAlg, mUseFastAlg, false);

                if (mSFOption == 0) {
                    glEngineGL.drawSF(-1, mTexBeautyForCapture, tex_feather, mInputSFBlurTextures[mCurrSFTextureIdx1], mGammaTextureID, mUseObjAlg, cameraIndex, false);
                } else {
                    glEngineGL.drawSF(-1, mTexBeautyForCapture, tex_feather, mInputSFBlurTextures2[mCurrSFTextureIdx2], mGammaTextureID, mUseObjAlg, cameraIndex, false);
                }
            }

            if (mFlagGLCapture) {
                mGLCapture.CaptureGL(-1, 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, false);
                mGLCapture.CaptureGL(mCamFBOForCapture[0], 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, true);

                Log.d(TAG, "sendEmptyMessage2");
                mFlagGLCapture = false;
                mMainActiviyHandler.sendEmptyMessage(5);
            }

            swapBuffers();
            makeCurrent(false);
        }

        mDoNotDrawSFForCapture = false;
    }

    public void drawCartoon(SurfaceTexture surfaceTexture)
    {
        if (mCameraSurfaceTexture != null && mCameraTextureName[0] > 0) {
            makeCurrent(true);

            mCameraSurfaceTexture.updateTexImage();

            int cameraIndex = aiCamParameters.mCameraLocationInt;

            if( mSFCamOn == true && mDoNotDrawSFForCapture == false) {
                if(mFlagHumanAppear == false && mUseObjAlg == false) {
                    glEngineGL.drawBasic(-1, mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                }
                else {

                    glEngineGL.updateMaskTextureForSF(mInputMaskTextureForSF);

                    //????
                    GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0);
                    GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, 0);

                    double gamma = gammaManager.calcValueAnimation(gammaManager.getCurrentValue());
                    gammaManager.setAnimationGamma(gamma);
                    Log.d(TAG, "gammaManager.getCurrentValue() : " + gammaManager.getCurrentValue() + ", gamma : " + gamma);
                    updateGammaTableTexture(mGammaTextureID, (float) gamma);
                    
					boolean useOes;
					int tex_beauty = -1;
                    int useCartoon = 0;
                    if (mUseObjAlg == false) {
                        if (mCartoonOption == 0) {
                            useCartoon = 1;
                        } else {
                            useCartoon = -1;
                        }
						useOes = false;
						tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, -1, useCartoon);
                    }
                    else
                    {
                        useCartoon = -1;

                        tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, -1, useCartoon);
                        useOes = false;
                        //useOes = true;
                    }

                    if(mSaveCurrTextureForCapture == true && mUseObjAlg == false) {
                        mTexBeautyForCapture = glEngineGL.drawSFBeautyForCapture(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, -1, useCartoon);
                    }

                    boolean vertical = true;
                    glEngineGL.drawCartoon(mInputCartoonFBOs[0], useOes, mCameraTextureName[0], tex_beauty, mInputCartoonTextures[0], mInputCartoonEdgeTextures[0], mInputMaskTextureForSF[0], mUseObjAlg, cameraIndex, 0, vertical, false);
                    vertical = false;
                    glEngineGL.drawCartoon(mInputCartoonFBOs[1], useOes, mCameraTextureName[0], tex_beauty, mInputCartoonTextures[0], mInputCartoonEdgeTextures[0], mInputMaskTextureForSF[0], mUseObjAlg, cameraIndex, 1, vertical, false);
                    //vertical = true;
                    //glEngineGL.drawCartoon(mInputCartoonFBOs[0], tex_beauty/*mCameraTextureName[0]*/, mInputCartoonTextures[1], mInputCartoonEdgeTextures[0], mInputMaskTextureForSF[0], mUseObjAlg, cameraIndex, 1, vertical, false);
                    //vertical = false;
                    //glEngineGL.drawCartoon(mInputCartoonFBOs[1], tex_beauty/*mCameraTextureName[0]*/, mInputCartoonTextures[0], mInputCartoonEdgeTextures[0], mInputMaskTextureForSF[0], mUseObjAlg, cameraIndex, 1, vertical, false);

                    int tex_feather = -1;
                    if( mUseFastAlg == 0 ) {
                        tex_feather = glEngineGL.drawFeatherFast(mInputMaskTextureForSF[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mUseObjAlg, mUseFastAlg, false);
                    }
                    else
                    {
                        if( aiCamParameters.mCameraLocationInt == 0 )
                        {
                            //if( mUseFastAlg == 1 && mUseObjAlg == false ) {
                            if( mUseFastAlg == 1 ) {
                                tex_feather = glEngineGL.drawFeatherFast(mInputMaskTextureForSF[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mUseObjAlg, mUseFastAlg, false);
                            }
                            else
                            {
                                tex_feather = glEngineGL.drawFeatherNormal(mInputMaskTextureForSF[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mUseObjAlg, mUseFastAlg, false);
                            }
                        }
                        else {
                            tex_feather = glEngineGL.drawFeatherNormal(mInputMaskTextureForSF[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mUseObjAlg, mUseFastAlg, false);
                        }
                    }

    //                if( mUseObjAlg == false ) {
    //                    if( mCartoonOption == 0 ) {
    //                        tex_beauty = glEngineGL.drawSFBeauty(mInputCartoonTextures[1], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, -1, 1);
    //                    }
    //                    else
    //                    {
    //                        //tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, -1, -1);
    //                        tex_beauty = glEngineGL.drawSFBeauty(mInputCartoonTextures[1], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, -1, -1);
    //                    }
    //                }
    //                else
    //                {
    //                    tex_beauty = glEngineGL.drawSFBeauty(mInputCartoonTextures[1], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, -1, -1);
    //                }

                    //glEngineGL.drawBoxFilter(mInputCartoonEdgeFBOs[0], mCameraTextureName[0], mUseObjAlg, cameraIndex);
//                    glEngineGL.drawBoxFilter(mInputCartoonEdgeFBOs[0], mCameraTextureName[0], tex_beauty, mUseObjAlg, cameraIndex);
                    glEngineGL.drawBoxFilter(mInputCartoonEdgeFBOs[0], true, mCameraTextureName[0], -1,  tex_beauty, mUseObjAlg, cameraIndex);

                    //glEngineGL.drawCartoon(-1, mCameraTextureName[0], mInputCartoonTextures[1], mInputCartoonEdgeTextures[0], mInputMaskTextureForSF[0], cameraIndex, 2, vertical, false);
                    //glEngineGL.drawCartoon(-1, mCameraTextureName[0], mInputCartoonTextures[1], mInputCartoonEdgeTextures[0], texid, mUseObjAlg, cameraIndex, 2, vertical, false);

                    glEngineGL.drawCartoon(-1, useOes, mCameraTextureName[0], tex_beauty, mInputCartoonTextures[1], mInputCartoonEdgeTextures[0], tex_feather, mUseObjAlg, cameraIndex, 2, vertical, false);
                    //glEngineGL.drawCartoon(-1, true, mCameraTextureName[0], tex_beauty, tex_beauty, mInputCartoonEdgeTextures[0], tex_feather, mUseObjAlg, cameraIndex, 2, vertical, false);

                    if (isRecorded == true) {

                        synchronized (mPreviewSyncObject) {
                            mUseVideoTexture = 1 - mUseVideoTexture;
                            glEngineGL.drawCartoon(mVideoOutputFBO[mUseVideoTexture], useOes, mCameraTextureName[0], tex_beauty, mInputCartoonTextures[1], mInputCartoonEdgeTextures[0], tex_feather, mUseObjAlg, cameraIndex, 2, vertical, true);
                            Log.d(TAG, "[movie-sync] draw preview : " + (mUseVideoTexture));

                            mPreviewPrepared = true;
                            mPreviewSyncObject.notify();
                        }
                    }
                }
            }
            else
            {
                if(mDoNotDrawSFForCapture == false) {
                    glEngineGL.drawBasic(-1, mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                }
            }

            if(mSaveCurrTextureForCapture == true && mUseObjAlg == false) {
                glEngineGL.drawBasic(mCamFBOForCapture[0], mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                mSaveCurrTextureForCapture = false;
                mDoNotDrawSFForCapture = true;
                Log.d(TAG,"capture-routine Save Current Texture !!");
            }

			if (mFlagGLCapture == true && mUseObjAlg == true) {

                mGLCapture.CaptureGL(-1, 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, false);
                glEngineGL.drawBasic(mCamFBOForCapture[0], mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                mGLCapture.CaptureGL(mCamFBOForCapture[0], 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, true);

                mFlagGLCapture = false;
                mMainActiviyHandler.sendEmptyMessage(H_ENABLE_CAPTURE_BUTTON);

            }

            if(mDoNotDrawSFForCapture == false) {
                swapBuffers();
            }
            makeCurrent(false);
        }
    }

    public void drawCartoonForCapture() {
        if (mCameraSurfaceTexture != null && mCameraTextureName[0] > 0) {
            makeCurrent(true);

            mCameraSurfaceTexture.updateTexImage();

            int cameraIndex = aiCamParameters.mCameraLocationInt;

            if( mSFCamOn == true ) {
                if(mFlagHumanAppear == false && mUseObjAlg == false) {
                    glEngineGL.drawBasic(-1, mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                }
                else {

                    glEngineGL.updateMaskTextureForSF(mInputMaskTextureForSF);

                    GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0);
                    GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, 0);

                    boolean vertical = true;
                    glEngineGL.drawCartoon(mInputCartoonFBOs[0], false, mCamTextureForCapture[0], mTexBeautyForCapture, mInputCartoonTextures[0], mInputCartoonEdgeTextures[0], mInputMaskTextureForSF[0], mUseObjAlg, cameraIndex, 0, vertical, false);
                    vertical = false;
                    glEngineGL.drawCartoon(mInputCartoonFBOs[1], false, mCamTextureForCapture[0], mTexBeautyForCapture, mInputCartoonTextures[0], mInputCartoonEdgeTextures[0], mInputMaskTextureForSF[0], mUseObjAlg, cameraIndex, 1, vertical, false);

//                    int tex_feather = glEngineGL.drawFeather(mInputMaskTextureForSF[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mUseFastAlg, false);
                    int tex_feather = glEngineGL.drawFeatherNormal(mInputMaskTextureForSF[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mUseObjAlg, mUseFastAlg, false);

                    glEngineGL.drawBoxFilter(mInputCartoonEdgeFBOs[0], false, -1, mCamTextureForCapture[0],  mTexBeautyForCapture, mUseObjAlg, cameraIndex);

                    glEngineGL.drawCartoon(-1, false, mCamTextureForCapture[0], mTexBeautyForCapture, mInputCartoonTextures[1], mInputCartoonEdgeTextures[0], tex_feather, mUseObjAlg, cameraIndex, 2, vertical, false);

                }
            }

            if (mFlagGLCapture) {

                mGLCapture.CaptureGL(-1, 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, false);
                mGLCapture.CaptureGL(mCamFBOForCapture[0], 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, true);

                mFlagGLCapture = false;
                mMainActiviyHandler.sendEmptyMessage(H_ENABLE_CAPTURE_BUTTON);

            }

            swapBuffers();
            makeCurrent(false);
        }
        mDoNotDrawSFForCapture = false;
    }

	public static void StartCaptureGL() {
        mFlagGLCapture = true;
    }

    public static boolean ReadyToCapture() {
        return !mFlagGLCapture;
    }

    public static void SaveCurrTextureForCapture(boolean flag) {
        mSaveCurrTextureForCapture = flag;
    }

    public static void SetCurrentOrientation(int orientation) {
        if(mGLCapture != null) {
            mGLCapture.SetCurrentOrientation(orientation);
		}
        if(mEncoder != null) {
            mEncoder.SetCurrentOrientation(orientation);
        }
    }

//    public void drawLerfBlur(SurfaceTexture surfaceTexture) {
//
//        if ( mCameraSurfaceTexture != null && mCameraTextureName[0] > 0 ) {
//            makeCurrent(true);
//            mCameraSurfaceTexture.updateTexImage();
//
//            int cameraIndex = aiCamParameters.mCameraLocationInt;
//
//            if ( mBlurTuneSize != 0 ) {
//
//                glEngineGL.updateMaskTextureForBlur(mInputMaskTextureForBlur);
//
//                boolean moving = movingChecker.getMovingRunning();
//                if( moving == false ) {
//
//                }
//                else
//                {
//
//                }
//
//                if( mStudioMode == -1 ) {
////                    int maskWidth = aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_BLUR_MASK_FACTOR;
////                    int maskHeight = aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_BLUR_MASK_FACTOR;
//                    int maskWidth = aiCamParameters.PREVIEW_WIDTH_I;
//                    int maskHeight = aiCamParameters.PREVIEW_HEIGHT_I;
//
//                    int featherTexId = -1, blurTexId = -1;
//                    featherTexId = glEngineGL.drawFeather(mInputMaskTextureForBlur[0], maskWidth, maskHeight, mUseFastAlg, true);
//
//                    glEngineGL.drawBasic(mInputCopyFBO[0], mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 0, 0);
//                    //blurTexId = glEngineGL.drawLerfBlur(mCameraTextureName[0], maskWidth, maskHeight);
//                    blurTexId = glEngineGL.drawLerfBlur(mInputCopyTexture[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
//
//                    glEngineGL.drawGaussianResult(-1, mCameraTextureName[0], blurTexId, featherTexId, mCameraDebugTextureNameForBlur[0], mUseObjAlg, cameraIndex, false);
//
////                        if( isRecorded == true ) {
////
////                            synchronized (mPreviewSyncObject) {
////                                mUseVideoTexture = 1 - mUseVideoTexture;
////                                SofEngineGL.draw(mVideoOutputFBO[mUseVideoTexture], mCameraTextureName[0], mInputGaussianTextures[1], mCameraMaskTextureName[0], mCameraSegmentTextureName[0], mFront);
////                                Log.d(TAG, "[movie-sync] draw preview : "+(mUseVideoTexture));
////
////                                mPreviewPrepared = true;
////                                mPreviewSyncObject.notify();
////                            }
////                        }
//                }
//                else
//                {
//                    //Non Blur
////                        SFTunner2.readStudioTuneValue();
////                        glEngineGL.drawStudio(-1, mCameraTextureName[0], mInputGaussianTextures[1], mCameraMaskTextureName[0], mCameraSegmentTextureName[0], mFront, mFace);
//
////                        if( isRecorded == true ) {
////
////                            synchronized (mPreviewSyncObject) {
////                                mUseVideoTexture = 1 - mUseVideoTexture;
////                                SofEngineGL.drawStudio(mVideoOutputFBO[mUseVideoTexture], mCameraTextureName[0], mInputGaussianTextures[1], mCameraMaskTextureName[0], mCameraSegmentTextureName[0], mFront, mFace);
////                                Log.d(TAG, "[movie-sync] draw preview : "+(mUseVideoTexture));
////
////                                mPreviewPrepared = true;
////                                mPreviewSyncObject.notify();
////                            }
////                        }
//
//                    //Use Blur
//                    //SofEngineGL.drawStudio(mInputGaussianFBOs[0], mCameraTextureName[0], mInputGaussianTextures[1], mCameraMaskTextureName[0], mCameraSegmentTextureName[0], mFront, mFace);
//                    //SofEngineGL.drawBlurForStudio(mCameraTextureName[0], mInputGaussianTextures[0], mCameraMaskTextureName[0], mCameraSegmentTextureName[0], mFront, mFace);
//                }
//            }
//            else {
////                    glEngineGL.drawBasic(-1, mCameraTextureName[0], mFront);
////
////                    if( isRecorded == true ) {
////                        synchronized (mPreviewSyncObject) {
////                            mUseVideoTexture = 1 - mUseVideoTexture;
////                            SofEngineGL.drawBasic(mVideoOutputFBO[mUseVideoTexture], mCameraTextureName[0], mFront);
////                            Log.d(TAG, "[movie-sync] draw preview normal : " + (mUseVideoTexture));
////
////                            mPreviewPrepared = true;
////                            mPreviewSyncObject.notify();
////                        }
////                    }
//            }
//
//            swapBuffers();
//            makeCurrent(false);
//        } // camera texture
//        Log.d(TAG, "onFrameAvailable");
//    }

    public void drawBlur(SurfaceTexture surfaceTexture) {

        if (mCameraSurfaceTexture != null && mCameraTextureName[0] > 0) {
            makeCurrent(true);
            mCameraSurfaceTexture.updateTexImage();

            int cameraIndex = aiCamParameters.mCameraLocationInt;
            Log.d(TAG, "[camera-info] cameraIndex : " + cameraIndex);
            if (mDoNotDrawSFForCapture == false) {
                if (mFlagHumanAppear == false && mUseObjAlg == false) {
                    glEngineGL.drawBasic(-1, mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                } else {
                    if (mBlurTuneSize != 0) {

                        glEngineGL.updateMaskTextureForBlur(mInputMaskTextureForBlur);
//                    if (gDebugModeOn == true) {
//                        SofEngineGL.updateSegmentTexture(mCameraSegmentTextureName);
//                    }

                        boolean moving = movingChecker.getMovingRunning();
                        int iterations = 1;

                        if (moving == false) {
                            //if( true ) {
                            SFTunner2.readNeedTuneData();
                            iterations = SFTunner2.mMaxBlurCount;
                            Log.d("iterations", "iterations : " + iterations);

                            for (int i = 0; i < 4; i++) {
                                if (mBlurTuneSize == 4) {
                                    SFTunner2.mBlurSize[i] = SFTunner2.mBlurSize3[i] + 2;
                                } else if (mBlurTuneSize == 3) {
                                    SFTunner2.mBlurSize[i] = SFTunner2.mBlurSize3[i];
                                } else if (mBlurTuneSize == 2) {
                                    SFTunner2.mBlurSize[i] = SFTunner2.mBlurSize2[i];
                                } else if (mBlurTuneSize == 1) {
                                    SFTunner2.mBlurSize[i] = SFTunner2.mBlurSize1[i];
                                }

                                int sizeRect[] = {0, 0, 0, 0};
                                jniController.getSizeRect(sizeRect);
                                int objSize = (sizeRect[2] - sizeRect[0]) * (sizeRect[3] - sizeRect[1]);
                                int sizePercent = objSize * 100 / (aiCamParameters.PREVIEW_WIDTH_I * aiCamParameters.PREVIEW_HEIGHT_I);

                                Log.d(TAG, "SFTunner2.sizePercent : " + sizePercent);
                                Log.d(TAG, "SFTunner2.mAiSizeBlurMax : " + SFTunner2.mAiSizeBlurMax);
                                Log.d(TAG, "SFTunner2.mAiSizeBlurMin : " + SFTunner2.mAiSizeBlurMin);

                                if (sizePercent > SFTunner2.mAiSizeBlurMax) {
                                    SFTunner2.mBlurSize[i] += 2;
                                } else if (sizePercent < SFTunner2.mAiSizeBlurMin) {
                                    SFTunner2.mBlurSize[i] -= 2;
                                }

                                if (SFTunner2.mBlurSize[i] < 1) SFTunner2.mBlurSize[i] = 1;
                            }

                            for (int k = 0; k < 4; k++) {
                                Log.d(TAG, "mBlurSize[" + k + "] : " + SFTunner2.mBlurSize[k]);
                            }
                        } else if (mUseObjAlg == true) {
                            iterations = SFTunner2.mMaxBlurCount;

                            for (int i = 0; i < 4; i++) {
                                SFTunner2.mBlurSize[i] -= 2;
                                if (SFTunner2.mBlurSize[i] < 1) SFTunner2.mBlurSize[i] = 1;
                                Log.d("blur-count", "down[" + i + "] : " + SFTunner2.mBlurSize[i]);
                            }
                        }

                        double gamma = gammaManager.calcValueAnimation(gammaManager.getCurrentValue());
                        gammaManager.setAnimationGamma(gamma);
                        Log.d(TAG, "gammaManager.getCurrentValue() : " + gammaManager.getCurrentValue() + ", gamma : " + gamma);
                        updateGammaTableTexture(mGammaTextureID, (float) gamma);

                    	int tex_mask;

                    	boolean useOes;
                    	int inputTexture;

                    	int featherTexId = -1;
                        int useCartoon = 0;
                    	if( mUseObjAlg == false ) {
                            useCartoon = 0;
                            int tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, -1, useCartoon);
                            tex_mask = mFullTexture[0];
                            useOes = false;
                            inputTexture = tex_beauty;
							
                        	int maskWidth = aiCamParameters.PREVIEW_WIDTH_I;
                        	int maskHeight = aiCamParameters.PREVIEW_HEIGHT_I;

                        	featherTexId = glEngineGL.drawFeatherNormal(mInputMaskTextureForBlur[0], maskWidth, maskHeight, mUseObjAlg, mUseFastAlg, true);
                        } 
						else 
						{
                            useCartoon = -1;
                        	int tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, -1, useCartoon);
                        	tex_mask = mInputMaskTextureForBlur[0];

							//useOes = true;
                        	useOes = false;
                        	inputTexture = tex_beauty;

                        	featherTexId = mInputMaskTextureForBlur[0];
                        }

                    if(mSaveCurrTextureForCapture == true && mUseObjAlg == false) {
                        mTexBeautyForCapture = glEngineGL.drawSFBeautyForCapture(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mGammaTextureID, -1, useCartoon);
                    }

                    int horiCount = 0;
                    int vertiCount = 0;

                    horiCount++;

                    int width = (int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_BLUR_FACTOR_F);
                    int height = (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_BLUR_FACTOR_F);

                    glEngineGL.drawGaussian(width, height, 0, mInputGaussianFBOs[0], useOes, inputTexture, inputTexture,
                            tex_mask, featherTexId, cameraIndex, horiCount);

                    for (int i = 1; i < iterations; i++) {
                        vertiCount++;
                        glEngineGL.drawGaussian(width, height, 2, mInputGaussianFBOs[1], useOes, inputTexture, mInputGaussianTextures[0],
                                tex_mask, featherTexId, cameraIndex, vertiCount);
                        horiCount++;
                        glEngineGL.drawGaussian(width, height, 1, mInputGaussianFBOs[0], useOes, inputTexture, mInputGaussianTextures[1],
                                tex_mask, featherTexId, cameraIndex, horiCount);
                    }

                    vertiCount++;
                    glEngineGL.drawGaussian(width, height, 2, mInputGaussianFBOs[1], useOes, inputTexture, mInputGaussianTextures[0],
                            tex_mask, featherTexId, cameraIndex, vertiCount);
//                    int tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, mInputMaskTextureForBlur[0]);

                    if( mStudioMode == -1 ) {
//                        drawFeatherByBlur(mInputMaskTextureForBlur);
//                        //glEngineGL.drawGaussianResult(-1, mCameraTextureName[0], mInputGaussianTextures[1], mInputMaskTextureForBlur[0], mCameraDebugTextureNameForBlur[0], cameraIndex);
//                        glEngineGL.drawGaussianResult(-1, mCameraTextureName[0], mInputGaussianTextures[1], mInputGaussianTexturesFeather[1], mCameraDebugTextureNameForBlur[0], cameraIndex);

//                        int maskWidth = aiCamParameters.PREVIEW_WIDTH_I;
//                        int maskHeight = aiCamParameters.PREVIEW_HEIGHT_I;
//                        int featherTexId = -1;
//                        featherTexId = glEngineGL.drawFeather(mInputMaskTextureForBlur[0], maskWidth, maskHeight, mUseFastAlg, true);
//                        int tex_beauty;
//                        if( mUseObjAlg == false ) {
//                            tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, featherTexId, 0);
//                        }
//                        else
//                        {
//                            tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, featherTexId, -1);
//                        }

                        //glEngineGL.drawGaussianResult(-1, mCameraTextureName[0], mInputGaussianTextures[1],featherTexId, mCameraDebugTextureNameForBlur[0], mUseObjAlg, cameraIndex, false);
                        glEngineGL.drawGaussianResult(-1, useOes, inputTexture, mInputGaussianTextures[1], featherTexId, mCameraDebugTextureNameForBlur[0], mUseObjAlg, cameraIndex, false);

                        if( isRecorded == true ) {

                            synchronized (mPreviewSyncObject) {
                                mUseVideoTexture = 1 - mUseVideoTexture;
                                glEngineGL.drawGaussianResult(mVideoOutputFBO[mUseVideoTexture], useOes, inputTexture, mInputGaussianTextures[1],featherTexId, mCameraDebugTextureNameForBlur[0], mUseObjAlg, cameraIndex, true);
                                Log.d(TAG, "[movie-sync] draw preview : "+(mUseVideoTexture));

                                mPreviewPrepared = true;
                                mPreviewSyncObject.notify();
                            }
                        }
                    }
                    else
                    {
                        Log.d(TAG, "studio-mode");
                        //int tex_beauty = -1;

                        if( mUseObjAlg == true ) {
                            //Non Blur
                            CreateStudioEffect.updateMaskTextureForStudio(mInputMaskTextureForStudio);
                            SFTunner2.readStudioTuneValue();

                            //tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, -1, -1);
                            CreateStudioEffect.drawStudio(-1, useOes, inputTexture, mInputGaussianTextures[1], mInputMaskTextureForBlur[0], mCameraDebugTextureNameForBlur[0], mInputMaskTextureForStudio[0], cameraIndex, mFace, false);
                        }
                        else {
                            CreateStudioEffect.updateMaskTextureForStudio(mInputMaskTextureForStudio);
                            SFTunner2.readStudioTuneValue();

//                            int maskWidth = aiCamParameters.PREVIEW_WIDTH_I;
//                            int maskHeight = aiCamParameters.PREVIEW_HEIGHT_I;
//                            int featherTexId = -1;
//                            featherTexId = glEngineGL.drawFeather(mInputMaskTextureForBlur[0], maskWidth, maskHeight, mUseFastAlg, true);
                            //tex_beauty = glEngineGL.drawSFBeauty(mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, featherTexId, 0);

                            //glEngineGL.drawGaussianResult(mInputGaussianFBOs[0], mCameraTextureName[0], mInputGaussianTextures[1], featherTexId, mCameraDebugTextureNameForBlur[0], mUseObjAlg, cameraIndex, false);
                            glEngineGL.drawGaussianResult(mInputGaussianFBOs[0], useOes, inputTexture, mInputGaussianTextures[1], featherTexId, mCameraDebugTextureNameForBlur[0], mUseObjAlg, cameraIndex, false);

//                            int studioTexId = -1;
//                            studioTexId = glEngineGL.drawFeather(mInputMaskTextureForBlur[0], maskWidth, maskHeight, mUseFastAlg, true);

                            //CreateStudioEffect.drawStudio(-1, mCameraTextureName[0], mInputGaussianTextures[0], mInputMaskTextureForBlur[0], mCameraDebugTextureNameForBlur[0], mInputMaskTextureForStudio[0], cameraIndex, mFace, false);
                            CreateStudioEffect.drawStudio(-1, useOes, inputTexture, mInputGaussianTextures[0], featherTexId/*mInputMaskTextureForBlur[0]*/, mCameraDebugTextureNameForBlur[0], mInputMaskTextureForStudio[0], cameraIndex, mFace, false);
                            //CreateStudioEffect.drawStudio(-1, tex_beauty, mInputGaussianTextures[0], featherTexId/*mInputMaskTextureForBlur[0]*/, mCameraDebugTextureNameForBlur[0], studioTexId/*mInputMaskTextureForStudio[0]*/, cameraIndex, mFace, false);
                        }
                        if( isRecorded == true ) {

                            synchronized (mPreviewSyncObject) {
                                mUseVideoTexture = 1 - mUseVideoTexture;
                                CreateStudioEffect.drawStudio(mVideoOutputFBO[mUseVideoTexture], useOes, inputTexture, mInputGaussianTextures[0], mInputMaskTextureForBlur[0], mCameraDebugTextureNameForBlur[0], mInputMaskTextureForStudio[0], cameraIndex, mFace, true);
                                Log.d(TAG, "[movie-sync] draw preview : "+(mUseVideoTexture));

                                mPreviewPrepared = true;
                                mPreviewSyncObject.notify();
                            }
                        }

                        //Use Blur
                        //SofEngineGL.drawStudio(mInputGaussianFBOs[0], mCameraTextureName[0], mInputGaussianTextures[1], mCameraMaskTextureName[0], mCameraSegmentTextureName[0], mFront, mFace);
                        //SofEngineGL.drawBlurForStudio(mCameraTextureName[0], mInputGaussianTextures[0], mCameraMaskTextureName[0], mCameraSegmentTextureName[0], mFront, mFace);
                    }
                }
                else {
//                    glEngineGL.drawBasic(-1, mCameraTextureName[0], mFront);
//
//                    if( isRecorded == true ) {
//                        synchronized (mPreviewSyncObject) {
//                            mUseVideoTexture = 1 - mUseVideoTexture;
//                            SofEngineGL.drawBasic(mVideoOutputFBO[mUseVideoTexture], mCameraTextureName[0], mFront);
//                            Log.d(TAG, "[movie-sync] draw preview normal : " + (mUseVideoTexture));
//
//                            mPreviewPrepared = true;
//                            mPreviewSyncObject.notify();
//                        }
//                    }
                    }
                }//if(mFlagHumanAppear == false && mUseObjAlg == false)

                if(mSaveCurrTextureForCapture == true && mUseObjAlg == false) {
                    glEngineGL.drawBasic(mCamFBOForCapture[0], mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                    mSaveCurrTextureForCapture = false;
                    mDoNotDrawSFForCapture = true;
                    Log.d(TAG,"capture-routine Save Current Texture !!");
                }

                if (mFlagGLCapture == true && mUseObjAlg == true) {

                    mGLCapture.CaptureGL(-1, 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, false);
                    glEngineGL.drawBasic(mCamFBOForCapture[0], mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                    mGLCapture.CaptureGL(mCamFBOForCapture[0], 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, true);

                    mFlagGLCapture = false;
                    mMainActiviyHandler.sendEmptyMessage(H_ENABLE_CAPTURE_BUTTON);
                }

                swapBuffers();

            } //if( mDoNotDrawSFForCapture == false )
            makeCurrent(false);
        } // camera texture
        Log.d(TAG, "onFrameAvailable");
    }

    public void drawBlurForCapture() {
        if (mCameraSurfaceTexture != null && mCameraTextureName[0] > 0) {
            makeCurrent(true);
            mCameraSurfaceTexture.updateTexImage();

            int cameraIndex = aiCamParameters.mCameraLocationInt;
            Log.d(TAG, "[camera-info] cameraIndex : " + cameraIndex);
            if (mDoNotDrawSFForCapture == false) {
                if (mFlagHumanAppear == false && mUseObjAlg == false) {
                    glEngineGL.drawBasic(-1, mCameraTextureName[0], aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, 90, cameraIndex);
                }
                else {
                    if (mBlurTuneSize != 0) {

                        glEngineGL.updateMaskTextureForBlur(mInputMaskTextureForBlur);
                        boolean moving = movingChecker.getMovingRunning();
                        int iterations = 1;

                        if (moving == false) {
                            //if( true ) {
                            SFTunner2.readNeedTuneData();
                            iterations = SFTunner2.mMaxBlurCount;
                            Log.d("iterations", "iterations : " + iterations);

                            for (int i = 0; i < 4; i++) {
                                if (mBlurTuneSize == 4) {
                                    SFTunner2.mBlurSize[i] = SFTunner2.mBlurSize3[i] + 2;
                                } else if (mBlurTuneSize == 3) {
                                    SFTunner2.mBlurSize[i] = SFTunner2.mBlurSize3[i];
                                } else if (mBlurTuneSize == 2) {
                                    SFTunner2.mBlurSize[i] = SFTunner2.mBlurSize2[i];
                                } else if (mBlurTuneSize == 1) {
                                    SFTunner2.mBlurSize[i] = SFTunner2.mBlurSize1[i];
                                }

                                int sizeRect[] = {0, 0, 0, 0};
                                jniController.getSizeRect(sizeRect);
                                int objSize = (sizeRect[2] - sizeRect[0]) * (sizeRect[3] - sizeRect[1]);
                                int sizePercent = objSize * 100 / (aiCamParameters.PREVIEW_WIDTH_I * aiCamParameters.PREVIEW_HEIGHT_I);

                                Log.d(TAG, "SFTunner2.sizePercent : " + sizePercent);
                                Log.d(TAG, "SFTunner2.mAiSizeBlurMax : " + SFTunner2.mAiSizeBlurMax);
                                Log.d(TAG, "SFTunner2.mAiSizeBlurMin : " + SFTunner2.mAiSizeBlurMin);

                                if (sizePercent > SFTunner2.mAiSizeBlurMax) {
                                    SFTunner2.mBlurSize[i] += 2;
                                } else if (sizePercent < SFTunner2.mAiSizeBlurMin) {
                                    SFTunner2.mBlurSize[i] -= 2;
                                }

                                if (SFTunner2.mBlurSize[i] < 1) SFTunner2.mBlurSize[i] = 1;
                            }

                            for (int k = 0; k < 4; k++) {
                                Log.d(TAG, "mBlurSize[" + k + "] : " + SFTunner2.mBlurSize[k]);
                            }
                        } else if (mUseObjAlg == true) {
                            iterations = SFTunner2.mMaxBlurCount;

                            for (int i = 0; i < 4; i++) {
                                SFTunner2.mBlurSize[i] -= 2;
                                if (SFTunner2.mBlurSize[i] < 1) SFTunner2.mBlurSize[i] = 1;
                                Log.d("blur-count", "down[" + i + "] : " + SFTunner2.mBlurSize[i]);
                            }
                        }

                        boolean useOes;
                        int inputTexture;

                        int tex_mask;
                        if (mUseObjAlg == false) {
                            tex_mask = mFullTexture[0];
                            useOes = false;
                            inputTexture = mTexBeautyForCapture;
                        } else {
                            tex_mask = mInputMaskTextureForBlur[0];
                            useOes = false;
                            inputTexture = mCamTextureForCapture[0];
                        }

                        int maskWidth = aiCamParameters.PREVIEW_WIDTH_I;
                        int maskHeight = aiCamParameters.PREVIEW_HEIGHT_I;
                        int featherTexId = -1;
//                        featherTexId = glEngineGL.drawFeather(mInputMaskTextureForBlur[0], maskWidth, maskHeight, mUseFastAlg, true);
                        featherTexId = glEngineGL.drawFeatherNormal(mInputMaskTextureForBlur[0], maskWidth, maskHeight, mUseObjAlg, mUseFastAlg, true);

                        int horiCount = 0;
                        int vertiCount = 0;

                        horiCount++;

                        int width = (int) ((float) aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_FACTOR_F);
                        int height = (int) ((float) aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_FACTOR_F);

                        glEngineGL.drawGaussian(width, height, 0, mInputGaussianFBOs[0], useOes, inputTexture, inputTexture,
                                tex_mask, featherTexId, cameraIndex, horiCount);

                        for (int i = 1; i < iterations; i++) {
                            vertiCount++;
                            glEngineGL.drawGaussian(width, height, 2, mInputGaussianFBOs[1], useOes, inputTexture, mInputGaussianTextures[0],
                                    tex_mask, featherTexId, cameraIndex, vertiCount);
                            horiCount++;
                            glEngineGL.drawGaussian(width, height, 1, mInputGaussianFBOs[0], useOes, inputTexture, mInputGaussianTextures[1],
                                    tex_mask, featherTexId, cameraIndex, horiCount);
                        }

                        vertiCount++;
                        glEngineGL.drawGaussian(width, height, 2, mInputGaussianFBOs[1], useOes, inputTexture, mInputGaussianTextures[0],
                                tex_mask, featherTexId, cameraIndex, vertiCount);

                        if (mStudioMode == -1) {
                            glEngineGL.drawGaussianResult(-1, useOes, inputTexture, mInputGaussianTextures[1], featherTexId, mCameraDebugTextureNameForBlur[0], mUseObjAlg, cameraIndex, false);
                        } else {
                            Log.d(TAG, "studio-mode");

                            if (mUseObjAlg == true) {
                                //Non Blur
                                CreateStudioEffect.updateMaskTextureForStudio(mInputMaskTextureForStudio);
                                SFTunner2.readStudioTuneValue();

                                CreateStudioEffect.drawStudio(-1, useOes, inputTexture, mInputGaussianTextures[1], mInputMaskTextureForBlur[0], mCameraDebugTextureNameForBlur[0], mInputMaskTextureForStudio[0], cameraIndex, mFace, false);
                            } else {
                                CreateStudioEffect.updateMaskTextureForStudio(mInputMaskTextureForStudio);
                                SFTunner2.readStudioTuneValue();

                                glEngineGL.drawGaussianResult(mInputGaussianFBOs[0], useOes, inputTexture, mInputGaussianTextures[1], featherTexId, mCameraDebugTextureNameForBlur[0], mUseObjAlg, cameraIndex, false);

                                CreateStudioEffect.drawStudio(-1, useOes, inputTexture, mInputGaussianTextures[0], featherTexId/*mInputMaskTextureForBlur[0]*/, mCameraDebugTextureNameForBlur[0], mInputMaskTextureForStudio[0], cameraIndex, mFace, false);
                            }
                        }
                    }
                }//if(mFlagHumanAppear == false && mUseObjAlg == false)

                if (mFlagGLCapture) {

                    mGLCapture.CaptureGL(-1, 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, false);
                    mGLCapture.CaptureGL(mCamFBOForCapture[0], 0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I, true);

                    mFlagGLCapture = false;
                    mMainActiviyHandler.sendEmptyMessage(H_ENABLE_CAPTURE_BUTTON);

                }
                swapBuffers();

            } //if( mDoNotDrawSFForCapture == false )
            makeCurrent(false);
        } // camera texture
        Log.d(TAG, "onFrameAvailable");
        mDoNotDrawSFForCapture = false;
    }

    //set informations...

    public void setMainActivityHandler(Handler handler)
    {
        mMainActiviyHandler = handler;
        mGLCapture.setHandler(handler);
    }

    public void setMaskTextureForSF(byte[] data, boolean useObj, int useFast)
    {
        glEngineGL.copyMaskDataForSF(data);
        mUseObjAlg = useObj;
        mUseFastAlg = useFast;
    }

    public void setMaskTextureForBlur(byte[] data, boolean useObj, int useFast)
    {
        glEngineGL.copyMaskDataForBlur(data);
        mUseObjAlg = useObj;
        mUseFastAlg = useFast;
    }

    public void setMaskTextureForStudio(byte[] data, boolean useObj, int useFast)
    {
        CreateStudioEffect.copyMaskDataForStudio(data);
        mUseObjAlg = useObj;
        mUseFastAlg = useFast;
    }

    public void setMovingIndex(int index)
    {
        glEngineGL.setMovingIndex(index);
    }

    public static void setSFCamMode(int cartoonOn, int cartoonMode)
    {
        mSFCamMode = cartoonOn;

        if( mSFCamMode == aiCamParameters.CARTOON_MODE )
        {
            mCartoonOption = cartoonMode;
            glEngineGL.setCartoonOption(mCartoonOption);
        }
        else if( mSFCamMode == aiCamParameters.SF_MODE )
        {
            mSFOption = cartoonMode;
        }
        else if( mSFCamMode == aiCamParameters.OF_MODE )
        {
            mStudioMode = -1;
            mSFOption = cartoonMode;
        }
        else if( mSFCamMode == aiCamParameters.HIGHLIGHT_MODE )
        {
            mStudioMode = 0;
            mSFOption = cartoonMode;
        }
    }

    public static int getSFCamMode()
    {
        return mSFCamMode;
    }

    public void setFaceInfo(int info)
    {
        Log.d(TAG, "[camera-info] mFace : "+mFace);
        mFace = info;
    }

    public static void setBlurSize(int size)
    {
        mBlurTuneSize = size;
    }

    public static void setSFOnOff(int onoff)
    {
        mSFCamOn = onoff > 0 ? true : false;
    }

    public void startVideoRecord() {

        isRecorded = true;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                startRecord();
            }
        });
        thread.start();
    }

    public boolean isMediaMuxerOn()
    {
        if( mEncoder != null )
        {
            return mEncoder.isMediaMuxerOn();
        }
        else
        {
            return false;
        }
    }

    public void startRecord() {

        if( mEncoderInit == false ) {
            mEncoder = new SofVideoEncoder(aiCamParameters.MOVIE_WIDTH_I, aiCamParameters.MOVIE_HEIGHT_I, 8000000); // 8Mbps
        }
        mEncoder.init(mCurrentEGLConext, this);
        Log.d("Video-Test", "Encoder Initialized");

        mRecordThreadStop = false;

        while( true ) {
            synchronized (mPreviewSyncObject) {

                while( !mPreviewPrepared && isRecorded == true)
                {
                    Log.d(TAG, "[movie-sync] draw record wait...");
                    try {
                        mPreviewSyncObject.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if( mPreviewPrepared == true )
                {
                    Log.d(TAG, "[movie-sync] draw record : "+(1-mUseVideoTexture));
                    mEncoder.compressMP4(this, mVideoOutputTextures[mUseVideoTexture], isRecorded);
                    mPreviewPrepared = false;
                }
            }

            if( isRecorded == false )
            {
                Log.d(TAG, "[movie-sync] glFinish");
                break;
            }
        }

        mRecordThreadStop = true;

        String mp4Path = mEncoder.getPath();
        mEncoder.release();

        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString("picturePath", mp4Path);
        msg.what = H_REFRESH_GALLERY;
        msg.setData(data);
        mMainActiviyHandler.sendMessage(msg);

        mEncoder.releaseGL();
        mEncoder = null;
    }

    public void stopVideoRecord() {
        mEncoder.stopDrainThread();

        isRecorded = false;
        synchronized (mPreviewSyncObject) {
            mPreviewSyncObject.notify();
        }
    }

    public void SetHumanFlag(boolean flag) {
        mFlagHumanAppear = flag;
    }

    private SurfaceTexture mCustomVideoSurfaceTexture = null;
    private int[] mCustomVideoTextureOESId = { -1 };
    private int[] mCustomVideoTextureId = { -1 };
    private int[] mCustomVideoFBOId = { -1 };
    private MediaPlayer mCustomVideoPlayer = null;
    //    private boolean mCustomVideoUpdateSurface;
    private boolean mCustomVideoStarted = false;
    public void ReleaseCustomVideoMode(){
        mCustomVideoStarted = false;
        if(mCustomVideoPlayer != null) {
            if(mCustomVideoPlayer.isPlaying() == true) {
                mCustomVideoPlayer.stop();
            }
            mCustomVideoPlayer.release();
            mCustomVideoPlayer = null;
            Log.d(TAG,"custom video player released");
        }
        if (mCustomVideoSurfaceTexture != null) {
            mCustomVideoSurfaceTexture.release();
            mCustomVideoSurfaceTexture = null;
        }
        if (mCustomVideoTextureOESId[0] > 0) {
            GLES20.glDeleteBuffers(1, mCustomVideoTextureOESId, 0);
            mCustomVideoTextureOESId[0] = -1;
        }

        Log.d(TAG,"custom Release CustomVideoMode()");
    }

    public void StartCustomVideo() {
        BitmapStorage bitmapStorage = ((MainActivity) mContext).GetBitmapStorage();
        String pathLocal = bitmapStorage.GetCustomVideoPath();
        if (pathLocal != null) {
            Log.d(TAG, "custom StartCustomVideo path=" + pathLocal);
            if (mCustomVideoPlayer != null) {
                try {
                    mCustomVideoPlayer.setDataSource(pathLocal); 
                    mCustomVideoPlayer.prepare();
                    mCustomVideoPlayer.setLooping(true);
                } catch (IOException e) {
                    Log.e(TAG, "MediaPlayer Prepare failed");
                }

                mCustomVideoPlayer.start();
                mCustomVideoStarted = true;
                Log.d(TAG, "custom Start CustomVideo");
            }
        }
    }
    public void SetCustomVideoMode() {

        if(mCustomVideoPlayer != null) return;
        if(mCustomVideoSurfaceTexture != null) return;
        if(mCustomVideoTextureOESId[0] > 0) return;

        mCustomVideoTextureOESId[0] = glEngineGL.createExternalTexture();
        mCustomVideoSurfaceTexture = new SurfaceTexture(mCustomVideoTextureOESId[0]);
        mCustomVideoSurfaceTexture.setOnFrameAvailableListener(this);
        Surface surface = new Surface(mCustomVideoSurfaceTexture);

        mCustomVideoPlayer = new MediaPlayer();
        mCustomVideoPlayer.setSurface(surface);
        surface.release();

    }

    public void SetScaleFactorSFCustom(float scaleFactor, float transX, float transY, float pivotX, float pivotY) {
        glEngineGL.updateMatrixZoomAndPan(scaleFactor, transX, transY, pivotX, pivotY);
    }
}
