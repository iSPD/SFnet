package com.ispd.sfcam.encoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.opengl.EGLContext;
import android.os.Environment;

import com.ispd.sfcam.R;
import com.ispd.sfcam.pdEngine.glEngine;
import com.ispd.sfcam.utils.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.ispd.sfcam.MainActivity.ORIENTATION_0;
import static com.ispd.sfcam.MainActivity.ORIENTATION_180;
import static com.ispd.sfcam.MainActivity.ORIENTATION_270;
import static com.ispd.sfcam.MainActivity.ORIENTATION_90;

//import com.primis.app.s3dcamera.stereocreate.engine.StereoCreateEngine;


/**
 * Created by sjkim on 2016-01-22.
 */
public class SofVideoEncoder {

    final static String TAG = "sofVideoIncoder";

    // Video
    final static String MIME_TYPE = "video/avc";
    final static int FRAME_RATE = 23;
    final static int IFRAME_INTERVAL = 1; // 1sec
    final static boolean VERBOSE = true;
    final static int TIMEOUT_USEC = 10000;
    // Video

    // Audio
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;	// 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int BIT_RATE = 64000;
    public static final int SAMPLES_PER_FRAME = 1024;	// AAC, bytes/frame/channel
    public static final int FRAMES_PER_BUFFER = 25; 	// AAC, frame/buffer/sec

    private AudioThread mAudioThread = null;

    // Audio

    private final static int PACKET_TYPE_FORMAT = 0x0302;
    private final static int PACKET_TYPE_DATA = 0x0303;
    private final static int PACKET_TYPE_END_OF_DATA = 0x0310;

    private int mWidth = -1;
    private int mHeight = -1;
    private int mBitRate = -1;
    private ByteBuffer mTempByteBuffer;
    private ByteBuffer[] mInputByteBuffers;
    private ByteBuffer[] mOutputByteBuffers;

    private MediaCodec mEncoder = null;
    private InputGLSurface mInputGLSurface = null;
    private MediaCodec.BufferInfo mMediaCodecBufferInfo = null;

    // output mp4 for test
    private MediaMuxer mMediaMuxer = null;
    private int mMediaMuxerTrackIndex = -1;
    private boolean mMediaMuxerStarted = false;

    // Audio
    private MediaCodec mAudioEncoder = null;
    private MediaCodec.BufferInfo mAudioMediaCodecBufferInfo = null;
    private int mMediaMuxerAudioTrackIndex = -1;

    private long mPrevOutputPTSUs = 0;

    private boolean mStopEncoding = false;

    private boolean mMediaMuxerInit = false;
    private boolean mAudioMediaMuxerInit = false;
    private boolean mAudioWriteStarted = false;
    private String mMp4_path = "";

    private glEngine gSofEngine = null;

    private Thread gDrainThread = null;

    protected Object mSync = new Object();

    private int mOrientation = 0;

    public SofVideoEncoder(int width, int height, int bitrate) {

        mWidth = width;
        mHeight = height;
        mBitRate = bitrate;

        mTempByteBuffer = ByteBuffer.allocate(mWidth*mHeight*4);
    }

    public void init(EGLContext sharedContext, glEngine engine)  {

        try {
            //MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

            //format.setInteger(MediaFormat.KEY_ROTATION, 270);
            //boolean rotValue = format.containsKey(MediaFormat.KEY_ROTATION);
            /// Log.d(TAG, "rotValue : "+rotValue);
            Log.d(TAG, "format=" + format);

            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            mMediaCodecBufferInfo = new MediaCodec.BufferInfo();

            mInputGLSurface = new InputGLSurface(mEncoder.createInputSurface(), sharedContext);
            mInputGLSurface.makeCurrent(true);

            mEncoder.start();

            //audio crack
            //MediaFormat audio_format = new MediaFormat();
            MediaFormat audio_format = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, SAMPLE_RATE, 1);
            audio_format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            audio_format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            audio_format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            audio_format.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);

            mAudioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
            mAudioEncoder.configure(audio_format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            mAudioMediaCodecBufferInfo = new MediaCodec.BufferInfo();

            mAudioEncoder.start();

            // for test
            //String mp4_path = Environment.getExternalStorageDirectory().toString()+"/stereo_cam.mp4";
            File sdCard = Environment.getExternalStorageDirectory();
            //File dir = new File(sdCard.getAbsolutePath() + "/of/movies");
            File dir = new File(sdCard.getAbsolutePath() + "/DCIM/Camera");
//            File dir = new File(sdCard.getAbsolutePath() + getString(R.string.record_dir)); //sally
            if (!dir.exists()) {
                dir.mkdirs();
            }

            long now = System.currentTimeMillis();
            Date date = new Date(now);
            SimpleDateFormat sdfNow = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String fileName = "/rec_" + sdfNow.format(date) + ".mp4";
            File file = new File(dir, fileName);
            mMp4_path = dir + fileName;

            Log.d(TAG, "mp4_path : " + mMp4_path);

            mMediaMuxer = new MediaMuxer(mMp4_path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int degree = 0;
            if (mOrientation == ORIENTATION_0) {
                Log.o(TAG, "ORIENTATION_0");
                degree = 90;
            } else if (mOrientation == ORIENTATION_90) {
                Log.o(TAG, "ORIENTATION_90");
                degree = 180;
            } else if (mOrientation == ORIENTATION_180) {
                Log.o(TAG, "ORIENTATION_180");
                degree = 270;
            } else if (mOrientation == ORIENTATION_270) {
                Log.o(TAG, "ORIENTATION_270");
                degree = 0;
            }
            mMediaMuxer.setOrientationHint(degree);

            mMediaMuxerTrackIndex = -1;
            mMediaMuxerStarted = false;

            mMediaMuxerInit = false;
            mAudioMediaMuxerInit = false;
            //mAudioMediaMuxerInit = true;
            mAudioWriteStarted = false;

        } catch (IOException e) {
            e.printStackTrace();
        }

        mStopEncoding = false;

        //audio crack
        if (mAudioThread == null) {
            mAudioThread = new AudioThread();
            mAudioThread.start();
        }

//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while(mStopEncoding == false) {
//                    // recording surface
//                    if (mInputGLSurface != null) {
//                        mInputGLSurface.makeCurrent(true);
//                        mInputGLSurface.draw(engine);
//                        mInputGLSurface.swapBuffers();
//                        mInputGLSurface.makeCurrent(false);
//                    }
//                }
//            }
//        });
//        thread.start();

        gSofEngine = engine;
        //mInputGLSurface.drawVideo();

        gDrainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(mStopEncoding == false) {
                    long startBitmap = System.currentTimeMillis();
                    drainEncoder(mEncoder, mMediaCodecBufferInfo, false, false);
                    //audio crack
                    drainEncoder(mAudioEncoder, mAudioMediaCodecBufferInfo, false, true);
                    Log.d(TAG, "[movie-sync] compressMP4 time : " + (System.currentTimeMillis() - startBitmap));

                    //count();
                }
            }
        });
        gDrainThread.start();
    }

    public void stopDrainThread()
    {
        mStopEncoding = true;

        try {
            Log.d(TAG, "[movie-sync] join-1");
            gDrainThread.join();
            Log.d(TAG, "[movie-sync] join-2");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void releaseGL()
    {
        if ( mInputGLSurface != null ) {
            mInputGLSurface.release();
            mInputGLSurface = null;
        }
    }

    public void release() {

//        mStopEncoding = true;
//
//        try {
//            gDrainThread.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        // for mediamuxer
        drainEncoder(mEncoder, mMediaCodecBufferInfo, true, false);
        //drainEncoder(mAudioEncoder, mAudioMediaCodecBufferInfo, true, true);

        if ( mMediaMuxer != null ) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }

        if ( mEncoder != null ) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
            mMediaCodecBufferInfo = null;
        }

//        if ( mInputGLSurface != null ) {
//            mInputGLSurface.release();
//            mInputGLSurface = null;
//        }

        //audio crack
        // audio
        if ( mAudioEncoder != null ) {
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mAudioEncoder = null;
            mAudioMediaCodecBufferInfo = null;
        }

        if ( mAudioThread != null ) {
            mAudioThread = null;
        }

        Log.d(TAG, "[movie-sync] Encoding Stopped");
    }


    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < mPrevOutputPTSUs)
            result = (mPrevOutputPTSUs - result) + result;
        return result;
    }

    protected void encodeSound(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        if (mStopEncoding) return;
        final ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();
        while (!mStopEncoding) {
            final int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }
//	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
                if (length <= 0) {
                    // send EOS
                    //mIsEOS = true;
                    Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                    mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, 0,
                            presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    Log.i(TAG, "queueInputBuffer");
                    mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, length,
                            presentationTimeUs, 0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.i(TAG, "INFO_TRY_AGAIN_LATER");
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            }
        }
    }

    public boolean isMediaMuxerOn()
    {
        Log.d(TAG, "mMediaMuxerStarted : "+mMediaMuxerStarted);
        Log.d(TAG, "mAudioWriteStarted : "+mAudioWriteStarted);
        return mMediaMuxerStarted && mAudioWriteStarted;
    }

    /**
     * Extracts all pending data from the encoder.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     */
    private void  drainEncoder(MediaCodec encoder, MediaCodec.BufferInfo bufferinfo, boolean endOfStream, boolean isSound) {

        if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ")");

        if (endOfStream) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            encoder.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        while (true) {
            int encoderBufferIndex = encoder.dequeueOutputBuffer(bufferinfo, TIMEOUT_USEC);

            if (encoderBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    Log.d(TAG, "INFO_TRY_AGAIN_LATER : "+isSound);
                    break;      // out of while
                } else {
                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = encoder.getOutputBuffers();

                Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
            } else if (encoderBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMediaMuxerStarted && mMediaMuxer != null ) {
                    throw new RuntimeException("format changed twice");
                }

                MediaFormat newFormat = encoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

                if( isSound == false ) {
                    Log.d(TAG, "addTrack - video");
                    mMediaMuxerTrackIndex = mMediaMuxer.addTrack(newFormat);
                    mMediaMuxerInit = true;
                }
                else
                {
                    Log.d(TAG, "addTrack - audio");
                    mMediaMuxerAudioTrackIndex = mMediaMuxer.addTrack(newFormat);
                    mAudioMediaMuxerInit = true;
                }

                // now that we have the Magic Goodies, start the muxer
                if ( mMediaMuxer != null && mMediaMuxerInit == true && mAudioMediaMuxerInit == true && mMediaMuxerStarted == false) {
                //audio crack
                //if ( mMediaMuxer != null && mMediaMuxerInit == true && mMediaMuxerStarted == false) {
                    Log.d(TAG, "mMediaMuxer started : "+isSound);
                    mMediaMuxer.start();
                    mMediaMuxerStarted = true;
                }
            } else if ( encoderBufferIndex < 0 ) {
                Log.d(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderBufferIndex);
                // let's ignore it
            } else {
                Log.d(TAG, "encoderOutputBuffers");

                ByteBuffer encodedData = encoderOutputBuffers[encoderBufferIndex];

                boolean is_config = false;
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderBufferIndex +
                            " was null");
                }

                if ((bufferinfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG for output MP4");

                    //bufferinfo.size = 0;
                    is_config = true;
                }

                if (bufferinfo.size != 0) {

                    if( !mAudioMediaMuxerInit || !mMediaMuxerInit )
                    //audio crack
                    //if( !mMediaMuxerInit )
                    {
                        break;
                    }

                    if (!mMediaMuxerStarted && mMediaMuxer != null ) {
                        Log.d(TAG, "muxer hasn't started");
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(bufferinfo.offset);
                    encodedData.limit(bufferinfo.offset + bufferinfo.size);

                    if ( mMediaMuxer != null && is_config == false ) {

                        bufferinfo.presentationTimeUs = getPTSUs();

                        if( isSound == false ) {
                            Log.d(TAG+"-Audio", "writeSampleData video");
                            mMediaMuxer.writeSampleData(mMediaMuxerTrackIndex, encodedData, bufferinfo);
                        }
                        else
                        {
                            Log.d(TAG+"-Audio", "writeSampleData");
                            mMediaMuxer.writeSampleData(mMediaMuxerAudioTrackIndex, encodedData, bufferinfo);
                            mAudioWriteStarted = true;
                        }

                        mPrevOutputPTSUs = bufferinfo.presentationTimeUs;

                        if (VERBOSE) Log.d(TAG, "sent " + bufferinfo.size + " bytes to muxer");
                    }
                }

                encoder.releaseOutputBuffer(encoderBufferIndex, false);

                if ((bufferinfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.d(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (VERBOSE) Log.d(TAG, "end of stream reached");
                    }
                    break;      // out of while
                }
            } // valid encoded data
        } // while
    }

    //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    public int compressMP4(int textureLeft, int textureRight) {
//
//        int ret_size = 0;
//
//        drainEncoder(false);
//
//        // recording surface
//        if ( mInputGLSurface != null ) {
//            mInputGLSurface.makeCurrent(true);
//            mInputGLSurface.draw(textureLeft, textureRight);
//            mInputGLSurface.swapBuffers();
//            //mInputGLSurface.makeCurrent(false);
//        }
//
//        // output data from encoder
//
//
//        return ret_size;
//    }

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
            Log.d("nowFps", "compressMP4 nowFps : "+nowFps);

            frameCount = 0;
            // nowFps를 갱신하고 카운팅을 0부터 다시합니다.
            lastTime = nowTime;
            // 1초가 지났으므로 기준 시간또한 갱신합니다.
        }

        frameCount++;
        // 기준 시간으로 부터 1초가 안지났다면 카운트만 1 올리고 넘깁니다.
    }

    public String getPath()
    {
        return mMp4_path;
    }

    public int compressMP4(glEngine engine, int textureId, boolean isRecord) {

        //synchronized (this) {

            int ret_size = 0;

            long startBitmap = System.currentTimeMillis();
            if ( mInputGLSurface != null ) {
                Log.i(TAG+"-AudioSync", "compressMP4");
                mInputGLSurface.makeCurrent(true);
                mInputGLSurface.draw(textureId);
//                if( isRecord == false )
//                {
//                    com.newer.Log.d(TAG, "[movie-sync] glFinish");
//                    GLES20.glFinish();
//                }
//                if( sofOnOff == true ) {
//                    mInputGLSurface.draw(textureId);
//                }
//                else {
//                    mInputGLSurface.draw(engine);
//                }
//                GLES20.glFinish();
                mInputGLSurface.swapBuffers();
                mInputGLSurface.makeCurrent(false);
            }
            Log.d(TAG, "compressMP4 draw time : " + (System.currentTimeMillis() - startBitmap));

//            try {
//                Thread.sleep(33);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

//            Thread thread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    long startBitmap = System.currentTimeMillis();
//                    drainEncoder(mEncoder, mMediaCodecBufferInfo, false, false);
//                    drainEncoder(mAudioEncoder, mAudioMediaCodecBufferInfo, false, true);
//                    com.newer.Log.d(TAG, "compressMP4 time : " + (System.currentTimeMillis() - startBitmap));
//                }
//            });
//            thread.start();

            count();

            return ret_size;
        //}
    }

    public void drawVideo() {
        synchronized (this) {
//            if (mInputGLSurface != null && gSofEngine != null) {
//                mInputGLSurface.makeCurrent(true);
//                mInputGLSurface.draw(gSofEngine);
//                mInputGLSurface.swapBuffers();
//                mInputGLSurface.makeCurrent(false);
//            }

            if (mInputGLSurface != null){
                drawVideo();
            }
        }
    }

    public void makeCurrent(boolean enabled)
    {
        mInputGLSurface.makeCurrent(enabled);
    }

    public void swapBuffers()
    {
        mInputGLSurface.swapBuffers();
    }

    private static final int[] AUDIO_SOURCES = new int[] {
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };

    private class AudioThread extends Thread {
        @Override
        public void run() {

            while( true )
            {
                if( mMediaMuxerStarted == true ) break;
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            try {
                final int min_buffer_size = AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
                int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
                if (buffer_size < min_buffer_size)
                    buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;

                AudioRecord audioRecord = null;
                for (final int source : AUDIO_SOURCES) {
                    try {
                        audioRecord = new AudioRecord(
                                source, SAMPLE_RATE,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
                        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                            audioRecord = null;
                    } catch (final Exception e) {
                        audioRecord = null;
                    }
                    if (audioRecord != null) break;
                }
                if (audioRecord != null) {
                    try {
                        if (!mStopEncoding) {
                            Log.v(TAG+"-Audio", "AudioThread:start audio recording");
                            final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
                            int readBytes;
                            audioRecord.startRecording();
                            try {
                                for (; !mStopEncoding ;) {
                                    // read audio data from internal mic
                                    buf.clear();
                                    readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
                                    Log.v(TAG+"-Audio", "readBytes : "+readBytes);
                                    if (readBytes > 0) {
                                        // set audio data to encoder
                                        buf.position(readBytes);
                                        buf.flip();
                                        //synchronized (mSync) {
                                            encodeSound(buf, readBytes, getPTSUs());
                                            //Log.v(TAG+"-Audio", "mSync.notify");
                                            //mSync.notify();
                                        //}
                                    }
                                }
                            } finally {
                                audioRecord.stop();
                            }
                        }
                    } finally {
                        audioRecord.release();
                    }
                } else {
                    Log.e(TAG+"-Audio", "failed to initialize AudioRecord");
                }
            } catch (final Exception e) {
                Log.e(TAG+"-Audio", "AudioThread#run", e);
            }
            Log.v(TAG+"-Audio", "AudioThread:finished");
        }
    }

    public void SetCurrentOrientation(int orientation) {
        mOrientation = orientation;
    }
}

