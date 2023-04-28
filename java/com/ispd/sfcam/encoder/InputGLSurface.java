package com.ispd.sfcam.encoder;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.jniController;
import com.ispd.sfcam.pdEngine.glEngine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;

//import com.primis.app.s3dcamera.stereocreate.engine.StereoCreateEngine;

/**
 * Created by sjkim on 2016-01-08.
 */
public class InputGLSurface {

    private boolean useNativeGL = false;

    private static final String TAG = "InputGLSurface";
    private static final boolean VERBOSE = false;
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLContext mEGLContextShared;
    private EGLSurface mEGLSurface;
    private Surface mSurface = null;
    private SurfaceTexture mSurfaceTexture = null;

    private float mColorAnimation[] = {0.0f, 0.0f, 0.0f};
    private int[] mTextureName =  {-1};


    // GL Engine
    //private StereoCreateEngine mStereoCreateEngine;


    private final String mGLVS = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    private final String mGLFS = "" +
            "precision mediump float;\n" +
            "uniform sampler2D sTexture;\n" +
            "varying vec2 texCoord;\n" +
            "uniform int uFront;\n" +
            "void main() {\n" +
            "  vec2 tex_coord = vec2(1.0-texCoord.x, texCoord.y);\n" +
            "  vec4 color = texture2D(sTexture,tex_coord);\n" +
            "  gl_FragColor  = color;\n" +
            "}";

    private FloatBuffer mGLVertex;
    private FloatBuffer mGLTexCoord;
    private static FloatBuffer mMVPMatrixBuffer;
    private int mGLProgram = -1;

    public InputGLSurface(Surface surface, EGLContext sharedContext) {

//        if ( surface == null ) {
//            throw new NullPointerException();
//        }
        mSurface = surface;
        mEGLContextShared = sharedContext;

        eglSetup();

        makeCurrent(true);
        prepareRender();
        makeCurrent(false);
    }

    public InputGLSurface(SurfaceTexture surface, EGLContext sharedContext) {

//        if ( surface == null ) {
//            throw new NullPointerException();
//        }
        mSurfaceTexture = surface;
        mEGLContextShared = sharedContext;

        eglSetup();

        makeCurrent(true);
        prepareRender();
        makeCurrent(false);
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
     */
    private void eglSetup() {

        if( useNativeGL ) {
            jniController.sofInitEGL(mEGLContextShared.getNativeHandle());
        }
        else
        {
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
                    EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                    EGL_RECORDABLE_ANDROID, 1,
                    EGL14.EGL_NONE
            };

//        int[] attribList = {
//                EGL14.EGL_RED_SIZE, 8,
//                EGL14.EGL_GREEN_SIZE, 8,
//                EGL14.EGL_BLUE_SIZE, 8,
//                EGL14.EGL_ALPHA_SIZE, 8,
//                EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
//                //EGL_RECORDABLE_ANDROID, 0,
//                EGL14.EGL_NONE
//        };

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

            //mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT, attrib_list, 0);
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], mEGLContextShared, attrib_list, 0);
            checkEglError("eglCreateContext");

            if (mEGLContext == null) {
                throw new RuntimeException("null context");
            }

            Log.d(TAG, "Context=" + mEGLContext);

            // Create a window surface, and attach it to the Surface we received.
            int[] surfaceAttribs = {
                    EGL14.EGL_NONE
            };

            if (mSurfaceTexture != null) {
                mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurfaceTexture, surfaceAttribs, 0);
            } else if (mSurface != null) {
                mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface, surfaceAttribs, 0);
            } else {
                mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs, 0);//eglCreatePixmapSurface
            }

            checkEglError("eglCreateWindowSurface");
            if (mEGLSurface == null) {
                throw new RuntimeException("surface was null");
            }

            Log.d(TAG, "Surface=" + mEGLSurface);

            //EGL14.eglCopyBuffers()

            makeCurrent(true);

            // Engine
            //mStereoCreateEngine = new StereoCreateEngine();
            //mStereoCreateEngine.init();

            makeCurrent(false);
        }
    }

    public void sofEglCopyBuffer()
    {
        //MainActivity.sofEglCopyBuffer(mEGLDisplay.getNativeHandle(), mEGLSurface.getNativeHandle());
        jniController.sofEglCopyBuffer(0, 0);
    }

    /**
     * Discard all resources held by this class, notably the EGL context.  Also releases the
     * Surface that was passed to our constructor.
     */
    public void release() {

        if( useNativeGL ) {
            jniController.sofDeinitEGL();
        }
        else {
            if (mTextureName[0] >= 0) {

                GLES20.glDeleteBuffers(1, mTextureName, 0);
            }


            if (EGL14.eglGetCurrentContext().equals(mEGLContext)) {
                // Clear the current context and surface to ensure they are discarded immediately.
                EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT);
            }
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            //EGL14.eglTerminate(mEGLDisplay);

            if (mSurface != null) {
                mSurface.release();
            }

            if (mSurfaceTexture != null) {
                mSurfaceTexture.release();
            }

            // null everything out so future attempts to use this object will cause an NPE
            mEGLDisplay = null;
            mEGLContext = null;
            mEGLSurface = null;

            mSurface = null;
        }
    }

    /**
     * Makes our EGL context and surface current.
     */
    public void makeCurrent(boolean enabled) {

        if( useNativeGL ) {
            jniController.sofMakeCurrent(enabled);
        }
        else {
            if (enabled) {
                if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                    throw new RuntimeException("eglMakeCurrent failed (enabled)");
                }
            } else {
                if (!EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
                    throw new RuntimeException("eglMakeCurrent failed (disabled)");
                }
            }
        }
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     */
    public boolean swapBuffers() {
        if( useNativeGL ) {
            jniController.sofSwapBuffer();
            return true;
        }
        else {
            return EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
        }
    }

    /**
     * Returns the Surface that the MediaCodec receives buffers from.
     */
    public Surface getSurface() {
        return mSurface;
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    public void setPresentationTime(long nsecs) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
    }

    /**
     * Checks for EGL errors.
     */
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

    private void prepareRender() {

        if ( mGLProgram == -1 ) {

            int vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
            GLES20.glShaderSource(vshader, mGLVS);
            GLES20.glCompileShader(vshader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("Shader", "Could not compile vshader");
                Log.v("Shader", "Could not compile vshader:" + GLES20.glGetShaderInfoLog(vshader));
                GLES20.glDeleteShader(vshader);
                vshader = 0;
            }

            int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            GLES20.glShaderSource(fshader, mGLFS);
            GLES20.glCompileShader(fshader);
            GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("Shader", "Could not compile fshader");
                Log.v("Shader", "Could not compile fshader:" + GLES20.glGetShaderInfoLog(fshader));
                GLES20.glDeleteShader(fshader);
                fshader = 0;
            }

            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vshader);
            GLES20.glAttachShader(program, fshader);
            GLES20.glLinkProgram(program);

            mGLProgram = program;

            //float[] vtmp = {-1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f};
            //float[] ttmp = {1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};

            float[] vtmp = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
            float[] ttmp = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

            mGLVertex = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mGLVertex.put(vtmp);
            mGLVertex.position(0);
            mGLTexCoord = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mGLTexCoord.put(ttmp);
            mGLTexCoord.position(0);

            //rotation
            float []mModelMatrix = new float[16];
            float []mRotationMatrix = new float[16];
            float []mMVPMatrix = new float[16];

            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.setRotateM(mRotationMatrix, 0, 90, 0, 0, -1.0f);
            Matrix.multiplyMM(mMVPMatrix, 0, mModelMatrix, 0, mRotationMatrix, 0);

//            Matrix.setIdentityM(mMVPMatrix, 0);

            mMVPMatrixBuffer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mMVPMatrixBuffer.put(mMVPMatrix);
            mMVPMatrixBuffer.position(0);
        }


    }


    private void bindTexture(Bitmap bmp) {

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        if ( mTextureName[0] == -1 ) {

            GLES20.glGenTextures(1, mTextureName, 0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureName[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
        else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureName[0]);
        }

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);

    }

    private float mColor = 0.0f;

    public void draw(int textureLeft, int textureRIght) {

        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        prepareRender();

        /*
        GLES20.glUseProgram(mGLProgram);

        int ph = GLES20.glGetAttribLocation(mGLProgram, "vPosition");
        int tch = GLES20.glGetAttribLocation(mGLProgram, "vTexCoord");

        GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, mGLVertex);
        GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, mGLTexCoord);
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);

        bindTexture(bmp);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "sTexture"), 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        */



//        GLES20.glClearColor(0.0f, mColor, 0.0f, 1.0f);
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//
//        mColor += 0.1f;
//        if ( mColor >= 1.0f ) mColor = 0.0f;
//
//        GLES20.glFlush();


        //mStereoCreateEngine.drawPreview(textureLeft, textureRIght, 1920, 1080);
        //mStereoCreateEngine.drawSBS(textureLeft, textureRIght, 1920, 1080);

    }

    public void draw(int texture) {
        GLES20.glUseProgram (mGLProgram);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        GLES20.glViewport(0, 0, aiCamParameters.MOVIE_WIDTH_I, aiCamParameters.MOVIE_HEIGHT_I);

        int ph = GLES20.glGetAttribLocation(mGLProgram, "vPosition");
        int tch = GLES20.glGetAttribLocation(mGLProgram, "vTexCoord");

        GLES20.glVertexAttribPointer(ph, 2, GL_FLOAT, false, 4 * 2, mGLVertex);
        GLES20.glVertexAttribPointer(tch, 2, GL_FLOAT, false, 4 * 2, mGLTexCoord);
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);

        //rotation
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgram, "uMVPMatrix"), 1, false, mMVPMatrixBuffer);

        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "sTexture"), 0);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "uFront"), 0);

        GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(ph);
        GLES20.glDisableVertexAttribArray(tch);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glUseProgram(0);
    }

    public void draw(glEngine engine) {

        //prepareRender();


        //mStereoCreateEngine.drawPreview(textureLeft, textureRIght, 1920, 1080);

        //don't need
        //engine.drawForVideo();

    }

    public void drawPicture(glEngine engine) {


        //prepareRender();


        //mStereoCreateEngine.drawPreview(textureLeft, textureRIght, 1920, 1080);

        //don't need
        //engine.drawForVideo();

    }

    public void drawVideo()
    {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while( true )
                {
                    makeCurrent(true);

                    swapBuffers();
                    makeCurrent(false);

                    try {
                        Thread.sleep(33);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }
}
