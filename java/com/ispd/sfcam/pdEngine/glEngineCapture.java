package com.ispd.sfcam.pdEngine;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.ispd.sfcam.R;
import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.movingChecker;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.ispd.sfcam.MainActivity.H_CHECK_SF_CUSTOM_MODE;
import static com.ispd.sfcam.MainActivity.H_REFRESH_GALLERY;
import static com.ispd.sfcam.MainActivity.H_SET_MOVING_HANDLER;
import static com.ispd.sfcam.MainActivity.ORIENTATION_0;
import static com.ispd.sfcam.MainActivity.ORIENTATION_180;
import static com.ispd.sfcam.MainActivity.ORIENTATION_270;
import static com.ispd.sfcam.MainActivity.ORIENTATION_90;
import static org.opencv.core.Core.ROTATE_90_CLOCKWISE;
import static org.opencv.core.Core.flip;
import static org.opencv.core.Core.rotate;
import static org.opencv.core.CvType.CV_32SC1;
import static org.opencv.core.CvType.CV_32SC4;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.COLOR_RGBA2GRAY;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.resize;
import static com.ispd.sfcam.MainActivity.PICK_IMG_FROM_CAMERA;
import static com.ispd.sfcam.MainActivity.PICK_VIDEO_FROM_CAMERA;

public class glEngineCapture {
    private static final String TAG = "SFCam-glEngineCapture";

    private static Context mContext;
    private static Handler mHandler;
    private static SoundPool mSoundPool;
    private static int mSoundShutter;
    private static int mOrientation = 0;

    public glEngineCapture(Context context) {
        mContext = context;
        mSoundPool = new SoundPool( 1, AudioManager.STREAM_MUSIC, 0 );
        mSoundShutter = mSoundPool.load( mContext, R.raw.camera_click,1 );
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public static void PlayShutterSound() {
        AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        float actualVolume=(float) audioManager.getStreamVolume(AudioManager.STREAM_RING);
        float maxVolume=(float) audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        float vol = actualVolume/maxVolume;
        mSoundPool.play(mSoundShutter, vol, vol, 0, 0, 1.0f);
    }

    public void SetCurrentOrientation(int orientation) {
        mOrientation = orientation;
    }

    public void BitmapToJpeg(Bitmap bmp, boolean isOrgCapture, int camId) {
        String path = getPath(isOrgCapture);
        int newWidth;
        int newHeight;

        newWidth = bmp.getHeight();
        newHeight = bmp.getWidth();

        FileOutputStream fileOuputStream = null;
        try {
            fileOuputStream = new FileOutputStream(path);

        Bitmap resizedBmp = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true);
        Matrix rotMat = new Matrix();
        int degree = 0;
        if (mOrientation == ORIENTATION_0) {
            Log.o(TAG, "ORIENTATION_0");
            degree = 0;
        } else if (mOrientation == ORIENTATION_90) {
            Log.o(TAG, "ORIENTATION_90");
            degree = 90;
        } else if (mOrientation == ORIENTATION_180) {
            Log.o(TAG, "ORIENTATION_180");
            degree = 180;
        } else if (mOrientation == ORIENTATION_270) {
            Log.o(TAG, "ORIENTATION_270");
            degree = 270;
        }

        if (degree == 0) {
            resizedBmp.compress(Bitmap.CompressFormat.JPEG, 90, fileOuputStream);
        } else {
            rotMat.postRotate(degree);
            Bitmap rotatedBmp = Bitmap.createBitmap(resizedBmp, 0, 0, newWidth, newHeight, rotMat, true);
            resizedBmp.recycle();
            rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 90, fileOuputStream);
            rotatedBmp.recycle();
        }

        fileOuputStream.close();

		//Refresh Gallery & Load Thumbnail
        //refreshGallery(new File(path));
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putString("picturePath", path);
        msg.what = H_REFRESH_GALLERY;  // MainActivity.H_REFRESH_GALLERY
        msg.setData(data);
        mHandler.sendMessage(msg);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
        e.printStackTrace();
        }

    }

    Mat movingMat = new Mat(aiCamParameters.PREVIEW_HEIGHT_I / 8, aiCamParameters.PREVIEW_WIDTH_I / 8, CV_32SC1);
    //Mat movingMat = new Mat(aiCamParameters.PREVIEW_HEIGHT_I / 8, aiCamParameters.PREVIEW_WIDTH_I / 8, CV_8UC1);
    Mat convMat = new Mat();

    public void CaptureTest(int fbo, int x, int y, int w, int h, boolean isOrgCapture){
        int b[]=new int[w*(y+h)];
        int bt[]=new int[w*h];
        IntBuffer ib = IntBuffer.wrap(b);
//        byte b[]=new byte[w*(y+h)];
//        byte bt[]=new byte[w*h];
//        ByteBuffer ib = ByteBuffer.wrap(b);
        ib.position(0);

        GLES20.glFinish();
        GLES20.glFlush();

        if(fbo != -1) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        }
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        if(fbo != -1) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }

//        for(int i = 0; i < 180*135; i++)
//        {
//            Log.d(TAG, "CaptureTest["+i+"] : "+b[i]);
//        }

        for(int i=0, k=0; i<h; i++, k++)
        {//remember, that OpenGL bitmap is incompatible with Android bitmap
            //and so, some correction need.
            for(int j=0; j<w; j++)
            {
                int pix=b[i*w+j];
                int pb=(pix>>24)&0x000000ff;
                int pr=(pix<<8)&0xffffff00;
                int pix1= pr | pb;
                bt[(h-k-1)*w+j]=pix1;
            }
        }

//        movingMat.put(0, 0, bt);
//        movingMat.convertTo(convMat, CvType.CV_8UC4);
//        imwrite("/sdcard/movingMat.jpg", movingMat);

        Bitmap readPixelsBmp=Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
        Mat temp = new Mat();
        Utils.bitmapToMat(readPixelsBmp, temp);
        resize(temp, temp, new Size(135, 180));
        rotate(temp, temp, ROTATE_90_CLOCKWISE);
        flip(temp, temp, 0);
        cvtColor(temp, temp, COLOR_RGBA2GRAY);
        //imwrite("/sdcard/movingMat.jpg", temp);

        movingChecker.setShaderPreviewMat(temp);
        Message msg = new Message();
        msg.what = H_SET_MOVING_HANDLER;  // MainActivity.H_REFRESH_GALLERY
        mHandler.sendMessage(msg);

//        for(int i=0, k=0; i<h; i++, k++)
//        {//remember, that OpenGL bitmap is incompatible with Android bitmap
//            //and so, some correction need.
//            for(int j=0; j<w; j++)
//            {
//                int pix=b[i*w+j];
//                int pb=(pix>>16)&0xff;
//                int pr=(pix<<16)&0x00ff0000;
//                int pix1=(pix&0xff00ff00) | pr | pb;
//                bt[(h-k-1)*w+j]=pix1;
//            }
//        }

//        Mat movingMat = new Mat(aiCamParameters.PREVIEW_HEIGHT_I / 8, aiCamParameters.PREVIEW_WIDTH_I / 8, CV_32SC1);
//        movingMat.put(0, 0, b);
//        Mat temp = new Mat();
//        movingMat.convertTo(temp, CvType.CV_8UC4);
//        imwrite("/sdcard/movingMat.jpg", temp);

//        Bitmap readPixelsBmp=Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
//        BitmapToJpeg(readPixelsBmp, isOrgCapture, 0);
    }

    public void CaptureGL(int fbo, int x, int y, int w, int h, boolean isOrgCapture){
        int b[]=new int[w*(y+h)];
        int bt[]=new int[w*h];
        IntBuffer ib = IntBuffer.wrap(b);
        ib.position(0);

        GLES20.glFinish();
        GLES20.glFlush();

        if(fbo != -1) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        }
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        if(fbo != -1) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }

        for(int i=0, k=0; i<h; i++, k++)
        {//remember, that OpenGL bitmap is incompatible with Android bitmap
            //and so, some correction need.
            for(int j=0; j<w; j++)
            {
                int pix=b[i*w+j];
                int pb=(pix>>16)&0xff;
                int pr=(pix<<16)&0x00ff0000;
                int pix1=(pix&0xff00ff00) | pr | pb;
                bt[(h-k-1)*w+j]=pix1;
            }
        }

        Bitmap readPixelsBmp=Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
        BitmapToJpeg(readPixelsBmp, isOrgCapture, 0);
    }

    private String getPath(boolean isOrgCapture) {

        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + mContext.getResources().getString(R.string.capture_dir));
        if (!dir.exists()) {
            dir.mkdirs();
        }

        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String fileName;
        if(isOrgCapture) { fileName =  "/img_" + sdfNow.format(date) +"org.jpg"; }
        else            { fileName =  "/img_" + sdfNow.format(date) +".jpg"; }

        File file = new File(dir, fileName);
        String jpeg_path = dir + fileName;

        return jpeg_path;
    }

    public static File CreateTempFile(int mode) throws IOException {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + mContext.getResources().getString(R.string.capture_dir));
        if (!dir.exists()) {
            dir.mkdirs();
        }

        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyyMMdd_HHmmss");

        String fileName = null;
        String suffix = null;
        if(mode == PICK_IMG_FROM_CAMERA) {
            fileName = "img_" + sdfNow.format(date);  //do not use slash : ex) "/img_xxx" -> error
            suffix = ".jpg";
        }
        else if(mode == PICK_VIDEO_FROM_CAMERA) {
            fileName = "video_" + sdfNow.format(date);  //do not use slash : ex) "/img_xxx" -> error
            suffix = ".mp4";
        }
        if(fileName != null && suffix != null) {
            File file = File.createTempFile(fileName, suffix, dir);
            return file;
        }
        else {
            return null;
        }
    }

    public void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        mContext.sendBroadcast(mediaScanIntent);
    }
}
