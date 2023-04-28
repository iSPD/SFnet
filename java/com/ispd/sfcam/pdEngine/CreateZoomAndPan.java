package com.ispd.sfcam.pdEngine;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static com.ispd.sfcam.pdEngine.glEngineGL.setProgram;

public class CreateZoomAndPan {

    public static final String SOURCE_DRAW_VS_BASIC_CUSTOM = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +

            "uniform mat4 uMVPMatrix;\n" +

            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_BASIC_CUSTOM = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES sTextureOES;\n" +
            "uniform sampler2D sTexture;\n" +
            "varying vec2 texCoord;\n" +
            "uniform int uFront;\n" +
            "uniform int uUseOes;\n" +

            "void main() {\n" +
            "   vec2 newTexCoord = texCoord;\n" +
            "   if( uFront == 1 ) {\n" +
            "       newTexCoord.x = 1.0 - newTexCoord.x;\n" +
            "   }\n" +
            "   if( uUseOes == 1) { \n" +
            "	    gl_FragColor = texture2D(sTextureOES, newTexCoord);\n" +
            "   } else {\n" +
            "       gl_FragColor = texture2D(sTexture, newTexCoord);\n" +
            "   }\n" +
            "}";

    private static int mBasicCustomProgram = -1;
    private static FloatBuffer mMVPMatrix0;
    private static FloatBuffer mMVPMatrixBuffer90;
    private static FloatBuffer mGLVertexBufferBasic = null, mGLTexCoordBufferBasic = null;
    private static float mScaleFactor = 1.0f;

    public static final float[] vertices = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
    public static final float[] texcoords = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};
    //matrix
    private static float[] mMatrix0 = new float[16];
    private static float[] mMatrixXFlip = new float[16];
    private static float[] mModelMatrix90 = new float[16];
    private static float[] mMatrixCCW90 = new float[16];
    private static float[] mMVPMatrix90 = new float[16];
    private static float[] mMatrixCCW180 = new float[16];
    private static float[] mMatrixScale = new float[16];
    private static float[] mMatrixTrans = new float[16];
    private static float[] mMatrixTransPivot = new float[16];
    private static float[] mMatrixTransMinusPivot = new float[16];
    private static float[] mMatrix = new float[16];
    private static float[] mMatrixOes = new float[16];

    public CreateZoomAndPan() {

    }

    public static boolean Init() {
        mBasicCustomProgram = setProgram(SOURCE_DRAW_VS_BASIC_CUSTOM, SOURCE_DRAW_FS_BASIC_CUSTOM);
        if (mBasicCustomProgram != -1)
            Log.e("CreateZoomAndPan", "CreateZoomAndPan Program Success...");
        else Log.e("CreateZoomAndPan", "CreateZoomAndPan Program failed...");

        mGLVertexBufferBasic = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLVertexBufferBasic.put(vertices);
        mGLVertexBufferBasic.position(0);
        mGLTexCoordBufferBasic = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTexCoordBufferBasic.put(texcoords);
        mGLTexCoordBufferBasic.position(0);

        Matrix.setIdentityM(mMatrixCCW90, 0);
        Matrix.setRotateM(mMatrixCCW90, 0, 90, 0, 0, -1.0f);
//        Matrix.multiplyMM(mMVPMatrix90, 0, mModelMatrix90, 0, mMatrixCCW90, 0);

        Matrix.setIdentityM(mMatrixScale, 0);
        Matrix.scaleM(mMatrixScale, 0, mScaleFactor, mScaleFactor, 1.0f); //sally

        Matrix.setIdentityM(mMatrixCCW180, 0);
        Matrix.setRotateM(mMatrixCCW180, 0, 180, 0, 0, -1.0f);

        Matrix.setIdentityM(mMatrix, 0);
        Matrix.multiplyMM(mMatrix, 0, mMatrixCCW180, 0, mMatrixScale, 0);

        Matrix.setIdentityM(mMatrixOes, 0);
        Matrix.multiplyMM(mMatrixOes, 0, mMatrixCCW90, 0, mMatrixScale, 0);

        UpdateMatrixZoomAndPan(1.0f, 0, 0, 0, 0);
        return true;
    }

    public static void Release() {
        if (mBasicCustomProgram != 0) {
            GLES31.glDeleteProgram(mBasicCustomProgram);
            mBasicCustomProgram = 0;
        }
        mGLVertexBufferBasic = null;
        mGLTexCoordBufferBasic = null;
        mMVPMatrix0 = null;
    }

//    public static void UpdateMatrixZoomAndPan(float scaleFactor, float transX, float transY, float pivotX, float pivotY) {
    public static void UpdateMatrixZoomAndPan(float scaleFactor, float transX, float transY, float pivotX, float pivotY) {
//        Matrix.setIdentityM(matrix, 0);
//        Matrix.translateM(matrix, 0, caliData.mX, caliData.mY, 0.0f);
//        Matrix.rotateM(matrix, 0, caliData.mRotation, 0.0f, 0.0f, 1.0f);
        //scale
        mScaleFactor = scaleFactor;
        Matrix.setIdentityM(mMatrixScale, 0);
        Matrix.scaleM(mMatrixScale, 0, mScaleFactor, mScaleFactor, 1.0f);

        //translate x, y
        Matrix.setIdentityM(mMatrixTrans, 0);
        Matrix.translateM(mMatrixTrans, 0, transX, transY, 0.0f);

//        //translate  -pivot
//        Matrix.setIdentityM(mMatrixTransMinusPivot, 0);
//        Matrix.translateM(mMatrixTransMinusPivot, 0, -pivotX, -pivotY, 0.f);
//
//        //translate  pivot
//        Matrix.setIdentityM(mMatrixTransPivot, 0);
//        Matrix.translateM(mMatrixTransPivot, 0, pivotX, pivotY, 0.f);

        Matrix.setIdentityM(mMatrix, 0);
        Matrix.multiplyMM(mMatrix, 0, mMatrixCCW180, 0, mMatrixTrans, 0);
        Matrix.multiplyMM(mMatrix, 0, mMatrix, 0, mMatrixScale, 0);

        Matrix.setIdentityM(mMatrixTrans, 0);
        Matrix.translateM(mMatrixTrans, 0, transY, -transX, 0.0f);

        Matrix.setIdentityM(mMatrixOes, 0);
        Matrix.multiplyMM(mMatrixOes, 0, mMatrixCCW90, 0, mMatrixTrans, 0);
        Matrix.multiplyMM(mMatrixOes, 0, mMatrixOes, 0, mMatrixScale, 0);

//        Matrix.multiplyMM(mMatrix, 0, mMatrixCCW180, 0, mMatrixScale, 0);
//        Matrix.multiplyMM(mMatrix, 0, mMatrix, 0, mMatrixTrans, 0);
//        Matrix.setIdentityM(mMatrix, 0);
//        Matrix.multiplyMM(mMatrix, 0, mMatrixTransMinusPivot, 0, mMatrixScale, 0);
//        Matrix.multiplyMM(mMatrix, 0, mMatrix, 0, mMatrixCCW180, 0);
//        Matrix.multiplyMM(mMatrix, 0, mMatrix, 0, mMatrixTransPivot, 0);
//        Matrix.multiplyMM(mMatrix, 0, mMatrix, 0, mMatrixTrans, 0);

    }

    public static void RenderToTexture(int fbo, int useOes, int texturesOes, int textures, int width, int height, int rotate, int front) {
        if (fbo > 0) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        }

        GLES20.glUseProgram(mBasicCustomProgram);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        GLES20.glViewport(0, 0, width, height);

        int ph = GLES20.glGetAttribLocation(mBasicCustomProgram, "vPosition");
        int tch = GLES20.glGetAttribLocation(mBasicCustomProgram, "vTexCoord");

        GLES20.glVertexAttribPointer(ph, 2, GL_FLOAT, false, 4 * 2, mGLVertexBufferBasic);
        GLES20.glVertexAttribPointer(tch, 2, GL_FLOAT, false, 4 * 2, mGLTexCoordBufferBasic);
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);

        //rotation
//        if (rotate == 0) {
//            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mBasicCustomProgram, "uMVPMatrix"), 1, false, mMatrix0, 0);
//        } else if (rotate == 90) {
//            //GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mBasicCustomProgram, "uMVPMatrix"), 1, false, mMVPMatrixBuffer90);
//            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mBasicCustomProgram, "uMVPMatrix"), 1, false, mMVPMatrix90, 0);
//
//        }
        if (useOes == 1) {
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mBasicCustomProgram, "uMVPMatrix"), 1, false, mMatrixOes, 0);
        } else {
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mBasicCustomProgram, "uMVPMatrix"), 1, false, mMatrix, 0);
        }
        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texturesOes);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mBasicCustomProgram, "sTextureOES"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mBasicCustomProgram, "sTexture"), 1);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mBasicCustomProgram, "uFront"), front);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mBasicCustomProgram, "uUseOes"), useOes);

        GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(ph);
        GLES20.glDisableVertexAttribArray(tch);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glUseProgram(0);

        if (fbo > 0) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

}
