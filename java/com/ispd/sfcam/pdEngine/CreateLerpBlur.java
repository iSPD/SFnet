package com.ispd.sfcam.pdEngine;

import android.opengl.GLES11Ext;
import android.opengl.GLES31;
import android.opengl.Matrix;
import android.util.Log;

import com.ispd.sfcam.utils.SFTunner;
import com.ispd.sfcam.utils.movingChecker;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.ispd.sfcam.pdEngine.glEngineGL.setProgram;

public class CreateLerpBlur {

    public static class commonBlurSize
    {
        private static int mPreIntensity = -1;
        private static int mIntensity = 5;
    }

    private static boolean mSaveStatus;
    private static int mRotationInfo;
    private int mPreRotationInfo = -1;

    private int width = 1440/4;
    private int height = 1080/4;

//    private int mPreIntensity = -1;
//    private int mIntensity = 5;
    private final int mLevel = 16;
    private int[] mTextureDownScale;
    private Viewport mTexViewport;
    private int mScaleProgram;
    private int mFramebufferID;
    protected int mVertexBuffer;
    public static final float[] vertices = {-1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f};
    //for Lerp Blur
    public static final String SOURCE_DRAW_VS_LERP_BLUR = "" +
            "attribute vec2 vPosition;\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "   vTexCoord = vPosition / 2.0 + 0.5;\n" +
            "   gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_LERP_BLUR = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform sampler2D sTexture;\n" +
            "void main() {\n" +
            "   gl_FragColor = texture2D(sTexture, vTexCoord);\n" +
            "}";

    private static FloatBuffer mMVPMatrixBufferFastMask;
    private static FloatBuffer mMVPMatrixBufferSlowMask;
    private static int mPositionHandle;

    private static int mOffScreenFrameBuffer = -1;
    private static int mOffScreenRenderBuffer = -1;

    private static int mObjCurrentMode = 2;

    public CreateLerpBlur(int inputWidth, int inputHeight) {
        width = inputWidth;
        height = inputHeight;
    }

    public boolean Init() {

        genMipmaps(mLevel, width, height);

        //frame buffer
        int[] buf = new int[1];
        GLES31.glGenFramebuffers(1, buf, 0);
        mFramebufferID = buf[0];

        //create program
        mScaleProgram = setProgram(SOURCE_DRAW_VS_LERP_BLUR, SOURCE_DRAW_FS_LERP_BLUR);
        if (mScaleProgram != -1) Log.e("CreateFeather", "Lerp blur initLocal SUCCESS!!!...");
        else Log.e("CreateFeather", "Lerp blur initLocal failed...");

        //vertex
        int[] vertexBuffer = new int[1];
        GLES31.glGenBuffers(1, vertexBuffer, 0);
        mVertexBuffer = vertexBuffer[0];

        if (mVertexBuffer == 0) {
            Log.e("CreateFeather", "Invalid VertexBuffer!");
        }

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, mVertexBuffer);
        FloatBuffer buffer = FloatBuffer.allocate(vertices.length);
        buffer.put(vertices).position(0);
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, 32, buffer, GLES31.GL_STATIC_DRAW);

        float []mMVPMatrix0 = new float[16];
        float []mTransMatrix = new float[16];
        float []mScaleMatrix = new float[16];

        Matrix.setIdentityM(mMVPMatrix0, 0);
        Matrix.translateM(mTransMatrix, 0, mMVPMatrix0, 0, 0.0f, 0.0f, 1.0f);
        Matrix.scaleM(mScaleMatrix, 0, mTransMatrix, 0, 1.0f, 1.0f, 1.0f); //sally
        mMVPMatrixBufferFastMask = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrixBufferFastMask.put(mScaleMatrix);
        mMVPMatrixBufferFastMask.position(0);

        Matrix.setIdentityM(mMVPMatrix0, 0);
        Matrix.translateM(mTransMatrix, 0, mMVPMatrix0, 0, 0.0f, 0.0f, 1.0f);
        Matrix.scaleM(mScaleMatrix, 0, mTransMatrix, 0, 1.0f, 1.0f, 1.0f); //sally
        mMVPMatrixBufferSlowMask = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrixBufferSlowMask.put(mScaleMatrix);
        mMVPMatrixBufferSlowMask.position(0);

        return true;
    }

    public static void setSaveStatus(boolean status)
    {
        mSaveStatus = status;
    }

    public static void setRotationInfo(int rotate)
    {
        mRotationInfo = rotate;
    }

    public static void setObjCurrentMode(int mode)
    {
        mObjCurrentMode  = mode;
//        updateMatrix();
    }

    public static void updateMatrix()
    {
        float transFastX = 1.0f;
        float transFastY = 1.0f;
        float scaleFastX = 1.0f;
        float scaleFastY = 1.0f;
        float transSlowX = 1.0f;
        float transSlowY = 1.0f;
        float scaleSlowX = 1.0f;
        float scaleSlowY = 1.0f;

        transFastX = SFTunner.mFastFeatherTune.mTransLR;
        transFastY = SFTunner.mFastFeatherTune.mTransUD;

        if( mObjCurrentMode == 1 ) {
            scaleFastX = SFTunner.mFastFeatherTune.mScaleXsf;
            scaleFastY = SFTunner.mFastFeatherTune.mScaleYsf;
        }
        else if( mObjCurrentMode == 2 ) {
            scaleFastX = SFTunner.mFastFeatherTune.mScaleXcartoon;
            scaleFastY = SFTunner.mFastFeatherTune.mScaleYcartoon;
        }
        else if( mObjCurrentMode == 3 ) {
            scaleFastX = SFTunner.mFastFeatherTune.mScaleXblur;
            scaleFastY = SFTunner.mFastFeatherTune.mScaleYblur;
        }

        transSlowX = SFTunner.mSlowFeatherTune.mTransLR;
        transSlowY = SFTunner.mSlowFeatherTune.mTransUD;
        scaleSlowX = SFTunner.mSlowFeatherTune.mScaleXcartoon;
        scaleSlowY = SFTunner.mSlowFeatherTune.mScaleYcartoon;

//        transFastX = 0.f;
//        transFastY = 0.f;
//        scaleFastX = 0.995f;
//        scaleFastY = 1.0f;
//
//        transSlowX = 0.f;
//        transSlowY = 0.f;
//        scaleSlowX = 1.0f;
//        scaleSlowY = 1.0f;

        Log.d("updateMatrix", "transFastX : "+transFastX);
        Log.d("updateMatrix", "transFastY : "+transFastY);
        Log.d("updateMatrix", "scaleFastX : "+scaleFastX);
        Log.d("updateMatrix", "scaleFastY : "+scaleFastY);

        float []mMVPMatrix0 = new float[16];
        float []mTransMatrix = new float[16];
        float []mScaleMatrix = new float[16];

        if( mRotationInfo == 0 )
        {
            Matrix.setIdentityM(mMVPMatrix0, 0);
            Matrix.translateM(mTransMatrix, 0, mMVPMatrix0, 0, -transFastY, -transFastX, 1.0f);
            Matrix.scaleM(mScaleMatrix, 0, mTransMatrix, 0, scaleFastY, scaleFastX, 1.0f); //sally
        }
        else if( mRotationInfo == 90 )
        {
            Matrix.setIdentityM(mMVPMatrix0, 0);
            Matrix.translateM(mTransMatrix, 0, mMVPMatrix0, 0, -transFastX, transFastY, 1.0f);
            Matrix.scaleM(mScaleMatrix, 0, mTransMatrix, 0, scaleFastX, scaleFastY, 1.0f); //sally
        }
        else if( mRotationInfo == 180 )
        {
            Matrix.setIdentityM(mMVPMatrix0, 0);
            Matrix.translateM(mTransMatrix, 0, mMVPMatrix0, 0, transFastY, transFastX, 1.0f);
            Matrix.scaleM(mScaleMatrix, 0, mTransMatrix, 0, scaleFastY, scaleFastX, 1.0f); //sally
        }
        else if( mRotationInfo == 270 )
        {
            Matrix.setIdentityM(mMVPMatrix0, 0);
            Matrix.translateM(mTransMatrix, 0, mMVPMatrix0, 0, transFastX, -transFastY, 1.0f);
            Matrix.scaleM(mScaleMatrix, 0, mTransMatrix, 0, scaleFastX, scaleFastY, 1.0f); //sally
        }
        mMVPMatrixBufferFastMask = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrixBufferFastMask.put(mScaleMatrix);
        mMVPMatrixBufferFastMask.position(0);

//        if( mRotationInfo == 0 )
//        {
//            Matrix.setIdentityM(mMVPMatrix0, 0);
//            Matrix.translateM(mTransMatrix, 0, mMVPMatrix0, 0, -transSlowY, -transSlowX, 1.0f);
//            Matrix.scaleM(mScaleMatrix, 0, mTransMatrix, 0, scaleSlowY, scaleSlowX, 1.0f); //sally
//        }
//        else if( mRotationInfo == 90 )
//        {
//            Matrix.setIdentityM(mMVPMatrix0, 0);
//            Matrix.translateM(mTransMatrix, 0, mMVPMatrix0, 0, -transSlowX, transSlowY, 1.0f);
//            Matrix.scaleM(mScaleMatrix, 0, mTransMatrix, 0, scaleSlowX, scaleSlowY, 1.0f); //sally
//        }
//        else if( mRotationInfo == 180 )
//        {
//            Matrix.setIdentityM(mMVPMatrix0, 0);
//            Matrix.translateM(mTransMatrix, 0, mMVPMatrix0, 0, transSlowY, transSlowX, 1.0f);
//            Matrix.scaleM(mScaleMatrix, 0, mTransMatrix, 0, scaleSlowY, scaleSlowX, 1.0f); //sally
//        }
//        else if( mRotationInfo == 270 )
//        {
//            Matrix.setIdentityM(mMVPMatrix0, 0);
//            Matrix.translateM(mTransMatrix, 0, mMVPMatrix0, 0, transSlowX, -transSlowY, 1.0f);
//            Matrix.scaleM(mScaleMatrix, 0, mTransMatrix, 0, scaleSlowX, scaleSlowY, 1.0f); //sally
//        }
//        mMVPMatrixBufferSlowMask = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
//        mMVPMatrixBufferSlowMask.put(mScaleMatrix);
//        mMVPMatrixBufferSlowMask.position(0);
    }

    public void Release() {
        if (mVertexBuffer != 0) {
            GLES31.glDeleteBuffers(1, new int[]{mVertexBuffer}, 0);
            mVertexBuffer = 0;
        }
        GLES31.glDeleteFramebuffers(1, new int[]{mFramebufferID}, 0);
        if (mScaleProgram != 0) {
            GLES31.glDeleteProgram(mScaleProgram);
            mScaleProgram = 0;
        }
        GLES31.glDeleteTextures(mTextureDownScale.length, mTextureDownScale, 0);
    }

    private void genMipmaps(int level, int width, int height) {
        mTextureDownScale = new int[level];
        GLES31.glGenTextures(level, mTextureDownScale, 0);

        for (int i = 0; i < level; ++i) {
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mTextureDownScale[i]);
            GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA, calcMips(width, i + 1), calcMips(height, i + 1), 0, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE, null);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);
        }

        mTexViewport = new Viewport(0, 0, width, height);
    }

    private int calcMips(int len, int level) {
        return len / (level + 1);
    }

//    //intensity >= 0
//    public void SetIntensity(int intensity) {
//
//        if (intensity == mIntensity)
//            return;
//
//        mIntensity = intensity;
//        if (mIntensity > mLevel)
//            mIntensity = mLevel;
//    }

    private void renderTextureDefault(int texID, Viewport viewport, boolean useObj, int useFast) {

        //mProgram.bind(); //GLES31.glUseProgram(mProgramID); //sally : 일단 같은 program을 씀.
        GLES31.glUseProgram(mScaleProgram);

        if (viewport != null) {
            GLES31.glViewport(viewport.x, viewport.y, viewport.width, viewport.height);
        }

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texID);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mScaleProgram, "sTexture"), 0);

        mPositionHandle = GLES31.glGetAttribLocation(mScaleProgram, "vPosition");
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, mVertexBuffer);
//        GLES31.glVertexAttribPointer(ph, 2, GL_FLOAT, false, 4 * 2, mGLVertexCartoon);
        GLES31.glVertexAttribPointer(mPositionHandle, 2, GLES31.GL_FLOAT, false, 0, 0);
        GLES31.glEnableVertexAttribArray(mPositionHandle);

        if( useObj == true ) {
            if ( useFast == 0 || useFast == 1) {
                GLES31.glUniformMatrix4fv(GLES31.glGetUniformLocation(mScaleProgram, "uMVPMatrix"), 1, false, mMVPMatrixBufferFastMask);
            } else {
                GLES31.glUniformMatrix4fv(GLES31.glGetUniformLocation(mScaleProgram, "uMVPMatrix"), 1, false, mMVPMatrixBufferSlowMask);
            }
        }
        else
        {
            GLES31.glUniformMatrix4fv(GLES31.glGetUniformLocation(mScaleProgram, "uMVPMatrix"), 1, false, mMVPMatrixBufferSlowMask);
        }

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, 4);
    }

    private void bindTexture(int texID) {

        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, mFramebufferID);
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, texID, 0);
        if (GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER) != GLES31.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("CreateFeather", "CreateFeather - Frame buffer is not valid!");
        }

    }

    public void RenderTexture(int texID, Viewport viewport, boolean useObj, int useFast, boolean useBlur) {

//        if( useObj == true ) {
//            if (mPreRotationInfo != mRotationInfo) {
//                updateMatrix();
//                mPreRotationInfo = mRotationInfo;
//            }
//        }

        int featherMode = movingChecker.getFastAlgFlag();

        //if( useFast == 0 )
        if( featherMode == 0 )
        {
//            if( useBlur == true )
//            {
//                mIntensity = 16;
//            }
//            else {
//                mIntensity = 16;
//            }

            int useIntensity = -1;
            if( useBlur == true )
            {
                useIntensity = (int) SFTunner.mSuperFastFeatherTune.mBlurThickness;
            }
            else
            {
                useIntensity = (int) SFTunner.mSuperFastFeatherTune.mCartoonThickness;
            }
            Log.d("intensity-test", "featherMode : "+featherMode+", width : "+width+", useIntensity : "+useIntensity);

//            if( useIntensity < mPreIntensity )
//            {
//                mPreIntensity = mPreIntensity - 1;
//                mIntensity = mPreIntensity;
//
//                if( mIntensity < useIntensity ) mIntensity = useIntensity;
//
//                Log.d("RenderTexture", "if useFast : "+useFast+", mPreIntensity : "+mPreIntensity);
//            }
//            else
//            {
//                mPreIntensity = mPreIntensity + 2;
//                mIntensity = mPreIntensity;
//
//                if( mIntensity > useIntensity ) mIntensity = useIntensity;
//
//                Log.d("RenderTexture", "else useFast : "+useFast+", mPreIntensity : "+mPreIntensity);
//            }
//            Log.d("RenderTexture", "useFast : "+useFast+", mIntensity : "+mIntensity);

            commonBlurSize.mIntensity = useIntensity;
            commonBlurSize.mPreIntensity = commonBlurSize.mIntensity;
        }
        //else if( useFast == 1 )
        //else if( movingChecker.getMovingRunning() == true )
        else if( featherMode == 1 )
        {
//            if( useBlur == true )
//            {
//                mIntensity = (int) SFTunner.mFastFeatherTune.mBlurFthickness;
//            }
//            else {
//                mIntensity = (int) SFTunner.mFastFeatherTune.mNormalFthickness;
//            }

            Log.d("obj-feather", "useObj : "+useObj+", mObjCurrentMode : "+mObjCurrentMode);
            Log.d("obj-feather", "SFTunner.mFastFeatherTune.mSfThickness : "+SFTunner.mFastFeatherTune.mSfThickness);
            Log.d("obj-feather", "SFTunner.mFastFeatherTune.mCartoonThickness : "+SFTunner.mFastFeatherTune.mCartoonThickness);
            Log.d("obj-feather", "SFTunner.mFastFeatherTune.mBlurThickness : "+SFTunner.mFastFeatherTune.mBlurThickness);

            int useIntensity = -1;
            if( useObj == true )
            {
                if( mObjCurrentMode == 1 ) {
                    useIntensity = (int) SFTunner.mFastFeatherTune.mSfThickness;
                }
                else if( mObjCurrentMode == 2 ) {
                    useIntensity = (int) SFTunner.mFastFeatherTune.mCartoonThickness;
                }
                else if( mObjCurrentMode == 3 ) {
                    useIntensity = (int) SFTunner.mFastFeatherTune.mBlurThickness;
                }
            }
            else
            {
                if( useBlur == true )
                {
                    useIntensity = (int) SFTunner.mFastFeatherTune.mBlurThickness;
                }
                else
                {
                    useIntensity = (int) SFTunner.mFastFeatherTune.mCartoonThickness;
                }
            }
            Log.d("intensity-test", "featherMode : "+featherMode+", width : "+width+", useIntensity : "+useIntensity);
//
//            if( useIntensity < mPreIntensity )
//            {
//                mPreIntensity = mPreIntensity - 1;
//                mIntensity = mPreIntensity;
//
//                if( mIntensity < useIntensity ) mIntensity = useIntensity;
//
//                Log.d("RenderTexture", "if useFast : "+useFast+", mPreIntensity : "+mPreIntensity);
//            }
//            else
//            {
//                mPreIntensity = mPreIntensity + 3;
//                mIntensity = mPreIntensity;
//
//                if( mIntensity > useIntensity ) mIntensity = useIntensity;
//
//                Log.d("RenderTexture", "else useFast : "+useFast+", mPreIntensity : "+mPreIntensity);
//            }
//            Log.d("RenderTexture", "useFast : "+useFast+", mIntensity : "+mIntensity);

            commonBlurSize.mIntensity = useIntensity;
            commonBlurSize.mPreIntensity = commonBlurSize.mIntensity;
        }
        else
        {
//            if( useBlur == true )
//            {
//                mIntensity = (int) SFTunner.mSlowFeatherTune.mBlurFthickness;
//            }
//            else {
//                mIntensity = (int) SFTunner.mSlowFeatherTune.mNormalFthickness;
//            }

            int useIntensity = -1;
            if( useBlur == true )
            {
                useIntensity = (int) SFTunner.mSlowFeatherTune.mBlurThickness;
            }
            else
            {
                useIntensity = (int) SFTunner.mSlowFeatherTune.mCartoonThickness;
            }
            Log.d("intensity-test", "featherMode : "+featherMode+", width : "+width+", useIntensity : "+useIntensity);

            if( mSaveStatus == false ) {
            //if( false ) {
                if (useIntensity < commonBlurSize.mPreIntensity) {
                    commonBlurSize.mPreIntensity = commonBlurSize.mPreIntensity - 1;
                    commonBlurSize.mIntensity = commonBlurSize.mPreIntensity;

                    if (commonBlurSize.mIntensity < useIntensity) commonBlurSize.mIntensity = useIntensity;

                    Log.d("RenderTexture", "if useFast : " + useFast + ", mPreIntensity : " + commonBlurSize.mPreIntensity);
                } else {
                    commonBlurSize.mPreIntensity = commonBlurSize.mPreIntensity + 1;
                    commonBlurSize.mIntensity = commonBlurSize.mPreIntensity;

                    if (commonBlurSize.mIntensity > useIntensity) commonBlurSize.mIntensity = useIntensity;

                    Log.d("RenderTexture", "else useFast : " + useFast + ", mPreIntensity : " + commonBlurSize.mPreIntensity);
                }
                Log.d("RenderTexture", "useFast : " + useFast + ", mIntensity : " + commonBlurSize.mIntensity);

                commonBlurSize.mPreIntensity = commonBlurSize.mIntensity;
            }
            else
            {
                commonBlurSize.mIntensity = useIntensity;
                commonBlurSize.mPreIntensity = useIntensity;
            }
        }

        GLES31.glClearColor(0.0f, 1.0f, 0.0f, 0.5f);
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT);
        GLES31.glViewport(0, 0, viewport.width, viewport.height);

        //mIntensity 는 항상 0보다 큰 값으로 가정함.
        if(commonBlurSize.mIntensity == 0) {
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0);
            renderTextureDefault(texID, viewport, useObj, useFast);
            return;
        }

//        if(mShouldUpdateTexture) {
//            updateTexture();
//        }

//*
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);

        bindTexture(mTextureDownScale[0]);

        //down scale

        mTexViewport.width = calcMips(width, 1);
        mTexViewport.height = calcMips(height, 1);
        renderTextureDefault(texID, mTexViewport, useObj, useFast);
//*
        GLES31.glUseProgram(mScaleProgram);
        if( useObj == true ) {
            if (useFast == 0 || useFast == 1) {
                GLES31.glUniformMatrix4fv(GLES31.glGetUniformLocation(mScaleProgram, "uMVPMatrix"), 1, false, mMVPMatrixBufferFastMask);
            } else {
                GLES31.glUniformMatrix4fv(GLES31.glGetUniformLocation(mScaleProgram, "uMVPMatrix"), 1, false, mMVPMatrixBufferSlowMask);
            }
        }
        else
        {
            GLES31.glUniformMatrix4fv(GLES31.glGetUniformLocation(mScaleProgram, "uMVPMatrix"), 1, false, mMVPMatrixBufferSlowMask);
        }

        for (int i = 1; i < commonBlurSize.mIntensity; ++i) {
            bindTexture(mTextureDownScale[i]);
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mTextureDownScale[i - 1]);
            GLES31.glViewport(0, 0, calcMips(width, i + 1), calcMips(height, i + 1));
            GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, 4);
        }

        for (int i = commonBlurSize.mIntensity - 1; i > 0; --i) {
            bindTexture(mTextureDownScale[i - 1]);
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mTextureDownScale[i]);
            GLES31.glViewport(0, 0, calcMips(width, i), calcMips(height, i));
            GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, 4);
        }

        if(mOffScreenFrameBuffer != -1 && mOffScreenRenderBuffer != -1) {
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, mOffScreenFrameBuffer);
            GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, mOffScreenRenderBuffer);
            GLES31.glViewport(viewport.x, viewport.y, viewport.width, viewport.height);

//            mTexViewport.width = 1440;
//            mTexViewport.height = 1080;
//            renderTextureDefault(mTextureDownScale[0], mTexViewport);

            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mTextureDownScale[0]);
            GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, 4);

            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0);
            GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, 0);
        }
//        GLES31.glViewport(viewport.x, viewport.y, viewport.width, viewport.height);
//        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0);
//        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mTextureDownScale[0]);
//        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, 4);

        GLES31.glDisableVertexAttribArray(mPositionHandle);
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture (GLES31.GL_TEXTURE_2D, 0);
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, 0);
        GLES31.glUseProgram(0);
    }

    public static class Viewport {
        public int x, y;
        public int width, height;

        public Viewport() {
        }

        public Viewport(int _x, int _y, int _width, int _height) {
            x = _x;
            y = _y;
            width = _width;
            height = _height;
        }
    }

    public void SetFramebuffer(int fboId) {
        mOffScreenFrameBuffer = fboId;
    }
    public void SetRenderbuffer(int rboId) {
        mOffScreenRenderBuffer = rboId;
    }
}
