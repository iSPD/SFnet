package com.ispd.sfcam.encoder;

import android.content.Context;
import android.media.MediaCodec;
import android.opengl.EGLContext;
import android.util.Log;
import android.view.Surface;

public class SofImageEncoder {

    final static String TAG = "SofImageEncoder";

    private EGLContext mSharedContext;
    private InputGLSurface mInputGLSurface = null;
    private Surface mSurface = null;

    // Video
    final static String MIME_TYPE = "video/avc";
    final static int FRAME_RATE = 30;
    final static int IFRAME_INTERVAL = 1; // 1sec
    final static boolean VERBOSE = true;
    final static int TIMEOUT_USEC = 10000;
    private MediaCodec mEncoder = null;
    // Video

    public SofImageEncoder(Context context, Surface surface, EGLContext sharedContext)
    {
        mSharedContext = sharedContext;

        mInputGLSurface = new InputGLSurface((Surface)null, sharedContext);
        mInputGLSurface.makeCurrent(true);
    }

    public void makeCurrent(boolean onoff)
    {
        if ( mInputGLSurface != null ) {
            mInputGLSurface.makeCurrent(onoff);
        }
    }

    public void swapBuffer()
    {
        if ( mInputGLSurface != null ) {
            mInputGLSurface.swapBuffers();
        }
    }

    public void sofEglCopyBuffers()
    {
        if ( mInputGLSurface != null ) {
            mInputGLSurface.sofEglCopyBuffer();
        }
    }

    public void release() {

        if ( mInputGLSurface != null ) {
            mInputGLSurface.release();
            mInputGLSurface = null;
        }

        Log.d(TAG, "Encoding Stopped");
    }
}