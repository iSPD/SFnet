package com.ispd.sfcam.pdEngine;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES10;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.SFTunner;
import com.ispd.sfcam.utils.SFTunner2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_FRAMEBUFFER_COMPLETE;
import static android.opengl.GLES20.GL_LUMINANCE;
import static android.opengl.GLES20.GL_RGB;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;

public class glEngineGL {

    private static String TAG = "SFCam-glEngineGL";

    private static FloatBuffer mMVPMatrixBuffer0;
    private static FloatBuffer mMVPMatrixBuffer90;
    private static FloatBuffer mMVPMatrixBuffer90Xflip;

    private static int mGLProgramBasic = -1;
    private static FloatBuffer mGLVertexBasic = null, mGLTexCoordBasic = null;

    private static int []mGLProgramGaussian = {-1, -1, -1};
    private static FloatBuffer mGLVertexGaussian = null, mGLTexCoordGaussian = null;

    private static int mGLProgramGaussianResult = -1;
    private static FloatBuffer mGLVertexGaussianResult = null, mGLTexCoordGaussianResult = null;

    private static int m_offscreen_colorRenderbuffer[] = { -1, -1, -1, -1 };
    private static int m_offscreen_depthRenderbuffer[] = { -1, -1, -1, -1 };
    private static int m_offscreen_framebuffer[] = { -1, -1, -1, -1  };
    private static int m_offscreen_texture[] = { -1, -1, -1, -1  };
    private static final int NUM_OF_OFFSCREEN_BUFFERS = 4; //feather, blur, beautify, capture

    private static CreateEdgeFilter mEdge = null;
    private static CreateCartoon mCartoon = null;
    private static CreateLerpBlur mLerfBlurForFeatherFast = null;
    private static CreateLerpBlur mLerfBlurForFeatherNormal = null;
    private static CreateLerpBlur mLerfBlurForBlur = null;
    private static CreateBeautifyFilter mBeautify = null;
    private static CreateSF mSF = null;
    private static CreateZoomAndPan mCreateZoomAndPan = null;

    private static ByteBuffer mPixelBufferForFull;
    private static ByteBuffer mPixelBufferForSF;
    private static ByteBuffer mPixelBufferForBlur;

    private static int mCartoonOption = 0;
    private static int mDebugOn = 0;
    private static int mMovingIndex = -1;

    public static void setRotationBuffer()
    {
        //rotation
        float []mMVPMatrix0 = new float[16];

        Matrix.setIdentityM(mMVPMatrix0, 0);

        mMVPMatrixBuffer0 = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrixBuffer0.put(mMVPMatrix0);
        mMVPMatrixBuffer0.position(0);

        float []mModelMatrix90 = new float[16];
        float []mRotationMatrix90 = new float[16];
        float []mMVPMatrix90 = new float[16];
        float []mScaleMatrix = new float[16];

        Matrix.setIdentityM(mModelMatrix90, 0);
        Matrix.setRotateM(mRotationMatrix90, 0, 90, 0, 0, -1.0f);
        Matrix.multiplyMM(mMVPMatrix90, 0, mModelMatrix90, 0, mRotationMatrix90, 0);

        mMVPMatrixBuffer90 = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrixBuffer90.put(mMVPMatrix90);
        mMVPMatrixBuffer90.position(0);

        Matrix.scaleM(mScaleMatrix, 0, mMVPMatrix90, 0, 1.0f, -1.0f, 1.0f); //sally
        mMVPMatrixBuffer90Xflip = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrixBuffer90Xflip.put(mScaleMatrix);
        mMVPMatrixBuffer90Xflip.position(0);

        //input buffer
        byte[] arrFull = new byte[aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR * 4];
        Arrays.fill(arrFull, (byte)0xff);

        mPixelBufferForFull = ByteBuffer.allocateDirect(aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR * 4).order(ByteOrder.nativeOrder());
        mPixelBufferForFull.put(arrFull, 0, aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_FEATHER_FACTOR * 4);
        mPixelBufferForFull.position(0);

        byte[] arrSF = new byte[aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_FEATHER_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_FEATHER_FACTOR * 4];
        Arrays.fill(arrSF, (byte)0xff);

        mPixelBufferForSF = ByteBuffer.allocateDirect(aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_FEATHER_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_FEATHER_FACTOR * 4).order(ByteOrder.nativeOrder());
        mPixelBufferForSF.put(arrSF, 0, aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_FEATHER_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_FEATHER_FACTOR * 4);
        mPixelBufferForSF.position(0);

        byte[] arrBlur = new byte[aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * 4];
        Arrays.fill(arrBlur, (byte)0xff);

        mPixelBufferForBlur = ByteBuffer.allocateDirect(aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * 4).order(ByteOrder.nativeOrder());
        mPixelBufferForBlur.put(arrBlur, 0, aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * 4);
        mPixelBufferForBlur.position(0);
    }

    public static int setProgram(String strVShader, String strFShader) {
        int vshader = GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER);
        GLES31.glShaderSource(vshader, strVShader);
        GLES31.glCompileShader(vshader);
        int[] compiled = new int[1];
        GLES31.glGetShaderiv(vshader, GLES31.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile vshader");
            Log.e(TAG, "Could not compile vshader:" + GLES31.glGetShaderInfoLog(vshader));
            GLES31.glDeleteShader(vshader);
            vshader = 0;
            return -1;
        }

        int fshader = GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER);
        GLES31.glShaderSource(fshader, strFShader);
        GLES31.glCompileShader(fshader);
        GLES31.glGetShaderiv(fshader, GLES31.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile fshader");
            Log.e(TAG, "Could not compile fshader:" + GLES31.glGetShaderInfoLog(fshader));
            GLES31.glDeleteShader(fshader);
            fshader = 0;
            return -1;
        }

        int program = GLES31.glCreateProgram();
        GLES31.glAttachShader(program, vshader);
        GLES31.glAttachShader(program, fshader);
        GLES31.glLinkProgram(program);

        return program;
    }

    public static int initBasic(int width, int height) {

        if ( mGLProgramBasic == -1 ) {
            mGLProgramBasic = setProgram(glShader.SOURCE_DRAW_VS_BASIC, glShader.SOURCE_DRAW_FS_BASIC);

            float[] vtmp = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
            float[] ttmp = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

            mGLVertexBasic = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mGLVertexBasic.put(vtmp);
            mGLVertexBasic.position(0);
            mGLTexCoordBasic = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mGLTexCoordBasic.put(ttmp);
            mGLTexCoordBasic.position(0);
        }

        return mGLProgramBasic;
    }

    public static void initEdge(int width, int height) {
        mEdge = new CreateEdgeFilter();
        mEdge.Init();
//        mEdgeSobel = new CreateSobelFilter();
//        mEdgeSobel.Init();
    }

    public static void initCartoon(Context context, int width, int height) {
        mCartoon = new CreateCartoon(context);
        mCartoon.Init();
    }

    public static void initSF(int width, int height) {
        mSF = new CreateSF();
        mSF.Init();
        mCreateZoomAndPan = new CreateZoomAndPan();
        mCreateZoomAndPan.Init();
    }

    public static void initGaussian(int width, int height) {

        String []gaussianProgramVs = new String[3];
        gaussianProgramVs[0] = glShaderBlur.SOURCE_DRAW_VS_GAUSSIAN_HORIZON_INIT;
        gaussianProgramVs[1] = glShaderBlur.SOURCE_DRAW_VS_GAUSSIAN_HORIZON;
        gaussianProgramVs[2] = glShaderBlur.SOURCE_DRAW_VS_GAUSSIAN_VERTICAL;

        String []gaussianProgramFs = new String[3];
        gaussianProgramFs[0] = glShaderBlur.SOURCE_DRAW_FS_GAUSSIAN_HORIZON_INIT;
        gaussianProgramFs[1] = glShaderBlur.SOURCE_DRAW_FS_GAUSSIAN_HORIZON;
        gaussianProgramFs[2] = glShaderBlur.SOURCE_DRAW_FS_GAUSSIAN_VERTICAL;

        for(int i = 0; i < 3; i++) {
            if (mGLProgramGaussian[i] == -1) {
                mGLProgramGaussian[i] = setProgram(gaussianProgramVs[i], gaussianProgramFs[i]);
                Log.e(TAG, "Create Shader complete["+i+"] : "+mGLProgramGaussian[i]);
            }
        }
        float[] vtmp = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
        float[] ttmp = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

        mGLVertexGaussian = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLVertexGaussian.put(vtmp);
        mGLVertexGaussian.position(0);
        mGLTexCoordGaussian = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTexCoordGaussian.put(ttmp);
        mGLTexCoordGaussian.position(0);
    }

    public static int initGaussianResult(int width, int height) {

        GLES20.glViewport(0, 0, width, height);

        Log.d(TAG, "viewport width : "+width+" height : "+height);

        if ( mGLProgramGaussianResult == -1 ) {
            mGLProgramGaussianResult = setProgram(glShaderBlur.SOURCE_DRAW_VS_GAUSSIAN_RESULT, glShaderBlur.SOURCE_DRAW_FS_GAUSSIAN_RESULT);

            float[] vtmp = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
            float[] ttmp = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

            mGLVertexGaussianResult = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mGLVertexGaussianResult.put(vtmp);
            mGLVertexGaussianResult.position(0);
            mGLTexCoordGaussianResult = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mGLTexCoordGaussianResult.put(ttmp);
            mGLTexCoordGaussianResult.position(0);
        }
        return mGLProgramGaussianResult;
    }

    public static void initLerfBlurForFeather()
    {
        mLerfBlurForFeatherFast = new CreateLerpBlur(1440/4, 1080/4);
        mLerfBlurForFeatherFast.Init();

        mLerfBlurForFeatherNormal = new CreateLerpBlur(1440/2, 1080/2);
        mLerfBlurForFeatherNormal.Init();
    }

    public static void initLerfBlurForBlur()
    {
        mLerfBlurForBlur = new CreateLerpBlur(1440/4, 1080/4);
        mLerfBlurForBlur.Init();
    }

    public static void initBeautifyFilter()
    {
        mBeautify = new CreateBeautifyFilter();
        mBeautify.Init();
    }

    public static void release() {
        if ( mGLProgramBasic   > 0 ) GLES20.glDeleteProgram(mGLProgramBasic);
        mGLProgramBasic  = -1;

        mGLVertexBasic = null;
        mGLTexCoordBasic = null;

        for(int i = 0; i < 3; i++) {
            if (mGLProgramGaussian[i] > 0) GLES20.glDeleteProgram(mGLProgramGaussian[i]);
            mGLProgramGaussian[i] = -1;
        }
        mGLVertexGaussian = null;
        mGLTexCoordGaussian = null;

        if ( mGLProgramGaussianResult   > 0 ) GLES20.glDeleteProgram(mGLProgramGaussianResult);
        mGLProgramGaussianResult  = -1;

        mGLVertexGaussianResult = null;
        mGLTexCoordGaussianResult = null;

        mEdge.Release();
        mCartoon.Release();
        mLerfBlurForFeatherFast.Release();
        mLerfBlurForFeatherNormal.Release();
        mLerfBlurForBlur.Release();
        mBeautify.Release();
        mSF.Release();
        mCreateZoomAndPan.Release();

        mPixelBufferForSF = null;
        mPixelBufferForBlur = null;

        Log.d(TAG, "Released Program & Vertex & TextureCoord");
    }

    public static int createExternalTexture() {
        int texture_name[] = new int[1];

        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glGenTextures(1, texture_name, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture_name[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_NEAREST);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        Log.d(TAG, "createExternalTexture : "+texture_name[0]);

        return texture_name[0];
    }


    public static int createTexture(int width, int height, int depth)
    {
        int name[] = new int[1];
        GLES20.glGenTextures(1, name, 0);

        GLES20.glBindTexture(GL_TEXTURE_2D, name[0]);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        if ( depth == 32 )
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
        else if ( depth == 24 )
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, null);
        else if ( depth == 8 )
        {
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, width, height, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, null);
        }

        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        return name[0];
    }

    public static int createTextureManual(int width, int height, int depth)
    {
        int name[] = new int[1];
        GLES20.glGenTextures(1, name, 0);

        GLES20.glBindTexture(GL_TEXTURE_2D, name[0]);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_NEAREST);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GLES10.GL_REPEAT);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GLES10.GL_REPEAT);

        if ( depth == 32 )
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
        else if ( depth == 24 )
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, null);
        else if ( depth == 8 )
        {
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, width, height, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, null);
        }

        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        return name[0];
    }

    public static int createTextureFull(int width, int height, int depth)
    {
        int name[] = new int[1];
        GLES20.glGenTextures(1, name, 0);

        GLES20.glBindTexture(GL_TEXTURE_2D, name[0]);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_NEAREST);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GLES10.GL_REPEAT);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GLES10.GL_REPEAT);

        if ( depth == 32 )
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, mPixelBufferForFull);
        else if ( depth == 24 )
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, mPixelBufferForFull);
        else if ( depth == 8 )
        {
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, width, height, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, mPixelBufferForFull);
        }

        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        return name[0];
    }

    public static int createTextureBitmap(int width, int height, Bitmap bitmap)
    {
        int name[] = new int[1];
        GLES20.glGenTextures(1, name, 0);

        GLES20.glBindTexture(GL_TEXTURE_2D, name[0]);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

//        bitmap.recycle(); //sally commented
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        return name[0];
    }

    public static int createLuminanceTexture(int width, int height) {

        int textureid[] = new int[1];
        GLES20.glGenTextures(1, textureid, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureid[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return textureid[0];
    }

    public static void updateLuminanceTexture(int texture, int width, int height, IntBuffer buffer) {

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,
                0,0, 0, width, height,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public static int createFBO(int texture)
    {
        int status = 0;
        int fbo[] = new int[1];
        GLES20.glGenFramebuffers(1, fbo, 0);

        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        GLES20.glFramebufferTexture2D(GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);

        // Check FBO status.
        status = GLES20.glCheckFramebufferStatus(GL_FRAMEBUFFER);

        if ( status != GL_FRAMEBUFFER_COMPLETE ) {
            Log.d("gltest", "fail to create FBO\n");
            return 0;
        }

        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);

        return fbo[0];
    }

    public static void setMovingIndex(int index)
    {
        mMovingIndex = index;
        mSF.SetMovingIndex(index);
    }

    public static void copyMaskDataForSF(byte[] image)
    {
        int width = aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_FEATHER_FACTOR;
        int height = aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_FEATHER_FACTOR;

        if( mPixelBufferForSF != null ) {
            mPixelBufferForSF.put(image, 0, width * height * 4);
            mPixelBufferForSF.position(0);
        }
    }

    public static void updateMaskTextureForSF(int []texID) {

        int width = aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_FEATHER_FACTOR;
        int height = aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_FEATHER_FACTOR;

//        mPixelBuffer.put(image, 0, width * height * 4);
//        mPixelBuffer.position(0);

        //GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GL_TEXTURE_2D, texID[0]);
        GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, mPixelBufferForSF);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);
    }


    public static void drawBasic(int fbo, int textures, int width, int height, int rotate, int front)
    {
        if ( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        }

        GLES20.glUseProgram (mGLProgramBasic);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        GLES20.glViewport(0, 0, width, height);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int ph = GLES20.glGetAttribLocation(mGLProgramBasic, "vPosition");
        int tch = GLES20.glGetAttribLocation(mGLProgramBasic, "vTexCoord");

        GLES20.glVertexAttribPointer(ph, 2, GL_FLOAT, false, 4 * 2, mGLVertexBasic);
        GLES20.glVertexAttribPointer(tch, 2, GL_FLOAT, false, 4 * 2, mGLTexCoordBasic);
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);

        //rotation
        if( rotate == 0 ) {
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgramBasic, "uMVPMatrix"), 1, false, mMVPMatrixBuffer0);
        }
        else if( rotate == 90 ) {
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgramBasic, "uMVPMatrix"), 1, false, mMVPMatrixBuffer90);
        }

        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramBasic, "sTexture"), 0);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramBasic, "uFront"), front);

        GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(ph);
        GLES20.glDisableVertexAttribArray(tch);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES20.glUseProgram(0);

        GLES20.glDisable(GLES20.GL_BLEND);

        if ( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

    public static void drawBoxFilter(int fbo, boolean useOes, int textureOri, int textureInitNm, int textureBt, boolean useObj, int front)
    {
        mEdge.RenderToTexture(fbo, useOes, textureOri, textureInitNm, textureBt, useObj, front);
//        mEdgeSobel.RenderToTexture(fbo, textures, front);
    }

    public static void setCartoonOption(int cartoonOption)
    {
        mCartoonOption = cartoonOption;
		mCartoon.SetCartoonOption(cartoonOption);
        mEdge.SetCartoonOption(cartoonOption);
    }

    public static void drawCartoon(int fbo, boolean useOes, int textureInitOes, int textureInitNm, int texturesFbo, int textureEdge, int textureMask, boolean useObj, int front, int initStage, boolean vertical, boolean movieOn)
    {
        mCartoon.RenderToTexture(fbo, useOes, textureInitOes, textureInitNm, texturesFbo, textureEdge, textureMask, front, initStage, useObj, movieOn, mDebugOn);
//        if ( fbo > 0 ) {
//            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, fbo);
//        }
//
//        GLES20.glUseProgram (mGLProgramCartoon);
//
//        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//        GLES20.glClear(GL_COLOR_BUFFER_BIT);
//
//        if( fbo > 0 )
//        {
//            if( movieOn == true ) {
//                GLES20.glViewport(0, 0, aiCamParameters.MOVIE_WIDTH_I, aiCamParameters.MOVIE_HEIGHT_I);
//            }
//            else {
//                GLES20.glViewport(0, 0, (int) ((float) aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_CARTOON_FACTOR_F), (int) ((float) aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_CARTOON_FACTOR_F));
//            }
//        }
//        else {
//            GLES20.glViewport(0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
//        }
//
//        int ph = GLES20.glGetAttribLocation(mGLProgramCartoon, "vPosition");
//        int tch = GLES20.glGetAttribLocation(mGLProgramCartoon, "vTexCoord");
//
//        GLES20.glVertexAttribPointer(ph, 2, GL_FLOAT, false, 4 * 2, mGLVertexCartoon);
//        GLES20.glVertexAttribPointer(tch, 2, GL_FLOAT, false, 4 * 2, mGLTexCoordCartoon);
//        GLES20.glEnableVertexAttribArray(ph);
//        GLES20.glEnableVertexAttribArray(tch);
//
//        //rotation
//        if( initStage == 0 )
//        {
//            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgramCartoon, "uMVPMatrix"), 1, false, mMVPMatrixBuffer90);
//        }
//        else
//        {
//            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgramCartoon, "uMVPMatrix"), 1, false, mMVPMatrixBuffer90);
//        }
//
//        GLES20.glActiveTexture(GL_TEXTURE0);
//        //GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureInit);
//        GLES20.glBindTexture(GL_TEXTURE_2D, textureInit);
//        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramCartoon, "sTextureInit"), 0);
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
//        GLES20.glBindTexture(GL_TEXTURE_2D, texturesFbo);
//        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramCartoon, "sTextureFbo"), 1);
//
////        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
////        jniController.useEGLImage(0);
////        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramCartoon, "sMaskTexture"), 2);
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
//        GLES20.glBindTexture(GL_TEXTURE_2D, textureMask);
//        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramCartoon, "sMaskTexture"), 2);
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
//        GLES20.glBindTexture(GL_TEXTURE_2D, textureEdge);
//        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramCartoon, "sEdgeTexture"), 3);
//
//        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramCartoon, "uFront"), front);
//        int mSaveStatus = 0;
//        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramCartoon, "uSaveStatus"), mSaveStatus);
//        if( vertical == true ) {
//            GLES20.glUniform2f(GLES20.glGetUniformLocation(mGLProgramCartoon, "uSamplerSteps"), 0.0f, 1.0f/ ((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_CARTOON_FACTOR_F));
//        }
//        else
//        {
//            GLES20.glUniform2f(GLES20.glGetUniformLocation(mGLProgramCartoon, "uSamplerSteps"), 1.0f/ ((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_CARTOON_FACTOR_F), 0.0f);
//        }
//        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramCartoon, "uInitStage"), initStage);
//        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramCartoon, "uCartoonMode"), mCartoonOption);
//        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramCartoon, "uDebugMode"), mDebugOn);
//        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramCartoon, "uUseObj"), useObj ? 1 : 0);
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
//
//        GLES20.glDisableVertexAttribArray(ph);
//        GLES20.glDisableVertexAttribArray(tch);
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
//        GLES20.glBindTexture (GL_TEXTURE_2D, 0);
//
//        GLES20.glUseProgram(0);
//
//        if ( fbo > 0 ) {
//            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
//        }
    }

    public static void drawSF(int fbo, int textures, int maskTexture, int backTexture, int gammaTexture, boolean useObj, int front, boolean movieOn){
        mSF.RenderToTexture(fbo, textures, maskTexture, backTexture, gammaTexture, useObj, front, movieOn);
    }

    public static void copyMaskDataForBlur(byte[] image)
    {
        int width = aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR;
        int height = aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR;

        if( mPixelBufferForBlur != null ) {
            mPixelBufferForBlur.put(image, 0, width * height * 4);
            mPixelBufferForBlur.position(0);
        }
    }

    public static void updateMaskTextureForBlur(int []texID) {
        //synchronized (mSyncObject) {

            int width = aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR;
            int height = aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR;

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GL_TEXTURE_2D, texID[0]);
            //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, mPixelBufferForBlur);
            GLES20.glBindTexture(GL_TEXTURE_2D, 0);
        //}
    }

    public static void drawGaussian(int width, int height, int useProg, int fbo, boolean useOes, int textureOri, int texture, int textureMask, int textureFeather, int front, int iteration)
    {
        GLES20.glUseProgram (mGLProgramGaussian[useProg]);

        if ( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, fbo);
            GLES20.glViewport(0, 0, width, height);
        }
        else
        {
            GLES20.glViewport(0, 0, width, height);
        }

        int ph = GLES20.glGetAttribLocation(mGLProgramGaussian[useProg], "vPosition");
        int tch = GLES20.glGetAttribLocation(mGLProgramGaussian[useProg], "vTexCoord");

        GLES20.glVertexAttribPointer(ph, 2, GL_FLOAT, false, 4 * 2, mGLVertexGaussian);
        GLES20.glVertexAttribPointer(tch, 2, GL_FLOAT, false, 4 * 2, mGLTexCoordGaussian);
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);

        if( useProg == 0 ) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureOri);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "sTextureOriOes"), 0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GL_TEXTURE_2D, textureOri);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "sTextureOriNm"), 1);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "sTextureOes"), 2);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GL_TEXTURE_2D, texture);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "sTextureNm"), 3);
        }
        else {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureOri);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "sTextureOriOes"), 0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GL_TEXTURE_2D, textureOri);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "sTextureOriNm"), 1);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "sTextureOes"), 2);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GL_TEXTURE_2D, texture);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "sTextureNm"), 3);
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureMask);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "sMaskTexture"), 4);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureFeather);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "sFeatherTexture"), 5);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "uFront"), front);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "uDebugMode"), mDebugOn);
        GLES20.glUniform2i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "uOfValue7"), SFTunner2.mBlurCount[3], SFTunner2.mBlurSize[3]);
        GLES20.glUniform2i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "uOfValue6"), SFTunner2.mBlurCount[3], SFTunner2.mBlurSize[3]);
        GLES20.glUniform2i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "uOfValue5"), SFTunner2.mBlurCount[3], SFTunner2.mBlurSize[3]);
        GLES20.glUniform2i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "uOfValue4"), SFTunner2.mBlurCount[2], SFTunner2.mBlurSize[2]);
        GLES20.glUniform2i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "uOfValue3"), SFTunner2.mBlurCount[1], SFTunner2.mBlurSize[1]);
        GLES20.glUniform2i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "uOfValue2"), SFTunner2.mBlurCount[0], SFTunner2.mBlurSize[0]);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "uIterations"), iteration);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "ublurWidth"), 1.0f/ (float)width);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "ublurHeight"), 1.0f/ (float)height);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussian[useProg], "uUseOes"), useOes ? 1 : 0);

        GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(ph);
        GLES20.glDisableVertexAttribArray(tch);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture (GL_TEXTURE_2D, 0);
        //if( useProg == 0 ) {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        //}

        GLES20.glUseProgram(0);

        if ( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }

        Log.d(TAG, "drawGaussian");
    }

    public static void drawGaussianResult(int fbo, boolean useOes, int texture_id_ori, int texture_id, int texture_id2, int texute_id3, boolean useObjAlg, int front, boolean movieOn) {

        if ( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, fbo);
            if( movieOn == true )
            {
                GLES20.glViewport(0, 0, aiCamParameters.MOVIE_WIDTH_I, aiCamParameters.MOVIE_HEIGHT_I);
            }
            else {
                GLES20.glViewport(0, 0, aiCamParameters.PREVIEW_WIDTH_I / (int) aiCamParameters.RESIZE_BLUR_FACTOR_F, aiCamParameters.PREVIEW_HEIGHT_I / (int) aiCamParameters.RESIZE_BLUR_FACTOR_F);
            }
        }
        else
        {
            GLES20.glViewport(0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
        }

        GLES20.glUseProgram(mGLProgramGaussianResult);

        int ph = GLES20.glGetAttribLocation(mGLProgramGaussianResult, "vPosition");
        int tch = GLES20.glGetAttribLocation(mGLProgramGaussianResult, "vTexCoord");

        GLES20.glVertexAttribPointer(ph, 2, GL_FLOAT, false, 4 * 2, mGLVertexGaussianResult);
        GLES20.glVertexAttribPointer(tch, 2, GL_FLOAT, false, 4 * 2, mGLTexCoordGaussianResult);
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);

        //rotation
        if ( fbo > 0 ) {
            if( movieOn == true )
            {
                if( front == 0) {
                    GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "uMVPMatrix"), 1, false, mMVPMatrixBuffer90Xflip);
                }
                else {
                    GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "uMVPMatrix"), 1, true, mMVPMatrixBuffer90);
                }
            }
            else {
                GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "uMVPMatrix"), 1, false, mMVPMatrixBuffer0);
            }
        }
        else {
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "uMVPMatrix"), 1, false, mMVPMatrixBuffer90);
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture_id_ori);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "sTextureOriOes"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture_id_ori);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "sTextureOriNm"), 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture_id);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "sTexture"), 2);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture_id2);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "sMaskTexture"), 3);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texute_id3);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "sSegmentTexture"), 4);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "uUseOes"), useOes ? 1 : 0);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "uUseFbo"), fbo > 0 ? 1 : 0);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "uObjAlg"), useObjAlg ? 1 : 0);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "uFront"), front);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "uDebugMode"), mDebugOn);

        int mSaveStatus = 0;
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramGaussianResult, "uSaveStatus"), mSaveStatus);
        mSaveStatus = 0;

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(ph);
        GLES20.glDisableVertexAttribArray(tch);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glUseProgram(0);

        if ( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

    public static void createOffScreenFBO(int fboWidth, int fboHeight) {

        GLES31.glGenRenderbuffers(NUM_OF_OFFSCREEN_BUFFERS, m_offscreen_colorRenderbuffer, 0);
        GLES31.glGenRenderbuffers(NUM_OF_OFFSCREEN_BUFFERS, m_offscreen_depthRenderbuffer, 0);
        GLES31.glGenFramebuffers(NUM_OF_OFFSCREEN_BUFFERS, m_offscreen_framebuffer, 0);

        for(int i = 0; i < NUM_OF_OFFSCREEN_BUFFERS; i++) {
//            GLES31.glGenRenderbuffers(1, m_offscreen_colorRenderbuffer, 0);
            GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, m_offscreen_colorRenderbuffer[i]);
            GLES31.glRenderbufferStorage(GLES31.GL_RENDERBUFFER, GLES31.GL_RGBA8, fboWidth, fboHeight);

            // Create the off screen depth render buffer.
//            GLES31.glGenRenderbuffers(1, m_offscreen_depthRenderbuffer, 0);
            GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, m_offscreen_depthRenderbuffer[i]);
            GLES31.glRenderbufferStorage(GLES31.GL_RENDERBUFFER, GLES31.GL_DEPTH_COMPONENT16, fboWidth, fboHeight);

            // Create the off screen frame render buffer.
//            GLES31.glGenFramebuffers(1, m_offscreen_framebuffer, 0);
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, m_offscreen_framebuffer[i]);
            GLES31.glFramebufferRenderbuffer(GLES31.GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                    GLES31.GL_RENDERBUFFER, m_offscreen_colorRenderbuffer[i]);
            GLES31.glFramebufferRenderbuffer(GLES31.GL_FRAMEBUFFER, GLES31.GL_DEPTH_ATTACHMENT,
                    GLES31.GL_RENDERBUFFER, m_offscreen_depthRenderbuffer[i]);

            m_offscreen_texture[i] = createTexture(fboWidth, fboHeight, 32);
            GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, m_offscreen_texture[i], 0);

            // Check the Framebuffer status
            int status = GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER);
            if (status != GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "Gradient FBO Error ~!!");
            } else {
                Log.e(TAG, "Gradient FBO Success ~!!");
            }
        }
    }

    public static int drawFeatherFast(int texid, int fboWidth, int fboHeight, boolean useObj, int useFast, boolean useBlur) {
        mLerfBlurForFeatherFast.SetFramebuffer(m_offscreen_framebuffer[0]);
        mLerfBlurForFeatherFast.SetRenderbuffer(m_offscreen_colorRenderbuffer[0]);

        CreateLerpBlur.Viewport viewport = new CreateLerpBlur.Viewport(0,0,fboWidth, fboHeight);
        mLerfBlurForFeatherFast.RenderTexture(texid, viewport, useObj, useFast, useBlur);

        return m_offscreen_texture[0];
    }

    public static int drawFeatherNormal(int texid, int fboWidth, int fboHeight, boolean useObj, int useFast, boolean useBlur) {
        mLerfBlurForFeatherNormal.SetFramebuffer(m_offscreen_framebuffer[0]);
        mLerfBlurForFeatherNormal.SetRenderbuffer(m_offscreen_colorRenderbuffer[0]);

        CreateLerpBlur.Viewport viewport = new CreateLerpBlur.Viewport(0,0,fboWidth, fboHeight);
        mLerfBlurForFeatherNormal.RenderTexture(texid, viewport, useObj, useFast, useBlur);

        return m_offscreen_texture[0];
    }

    public static int drawLerfBlur(int cameraTexureId, int fboWidth, int fboHeight) {
        mLerfBlurForBlur.SetFramebuffer(m_offscreen_framebuffer[1]);
        mLerfBlurForBlur.SetRenderbuffer(m_offscreen_colorRenderbuffer[1]);

        CreateLerpBlur.Viewport viewport = new CreateLerpBlur.Viewport(0,0,fboWidth, fboHeight);
        boolean useObj = false;
        mLerfBlurForBlur.RenderTexture(cameraTexureId, viewport, useObj, 2, true);

        return m_offscreen_texture[1];
    }

    public static int drawSFBeauty(int cameraTextureId, int fboWidth, int fboHeight, int gammaTex, int maskTex, int useCartoon) {
        mBeautify.SetFramebuffer(m_offscreen_framebuffer[2]);
        mBeautify.SetRenderbuffer(m_offscreen_colorRenderbuffer[2]);
        mBeautify.RenderTexture(cameraTextureId, fboWidth, fboHeight, gammaTex, maskTex, useCartoon);

        return m_offscreen_texture[2];
    }

    public static int drawSFBeautyForCapture(int cameraTextureId, int fboWidth, int fboHeight, int gammaTex, int maskTex, int useCartoon) {
        mBeautify.SetFramebuffer(m_offscreen_framebuffer[3]);
        mBeautify.SetRenderbuffer(m_offscreen_colorRenderbuffer[3]);
        mBeautify.RenderTexture(cameraTextureId, fboWidth, fboHeight, gammaTex, maskTex, useCartoon);

        return m_offscreen_texture[3];
    }

    public static void setDebugOn(int onoff)
    {
        mDebugOn = onoff;
        CreateStudioEffect.setDebugOn(onoff);
        mSF.SetDebugOn(onoff);
    }

    public static void updateMatrixZoomAndPan(float scaleFactor, float transX, float transY, float pivotX, float pivotY) {
        mCreateZoomAndPan.UpdateMatrixZoomAndPan(scaleFactor, transX, transY, pivotX, pivotY);
    }
	
    public static void drawBasicZoomAndPan(int fbo, int useOes, int texturesOes, int textures, int width, int height, int rotate, int front)
    {
        mCreateZoomAndPan.RenderToTexture(fbo, useOes, texturesOes, textures, width, height, rotate, front);
    }
}
