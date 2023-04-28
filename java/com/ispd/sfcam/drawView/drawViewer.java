package com.ispd.sfcam.drawView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.ispd.sfcam.AIEngineObjDetection.env.BorderedText;
import com.ispd.sfcam.AIEngineObjDetection.env.ImageUtils;
import com.ispd.sfcam.AIEngineSegmentation.segmentation.Segmentor;
import com.ispd.sfcam.R;
import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.jniController;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.SFTunner2;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import static org.opencv.core.Core.ROTATE_90_CLOCKWISE;
import static org.opencv.core.Core.ROTATE_90_COUNTERCLOCKWISE;
import static org.opencv.core.Core.flip;
import static org.opencv.core.Core.merge;
import static org.opencv.core.Core.rotate;
import static org.opencv.core.Core.split;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.COLOR_GRAY2RGBA;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2RGBA;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2YCrCb;
import static org.opencv.imgproc.Imgproc.COLOR_RGBA2RGB;
import static org.opencv.imgproc.Imgproc.COLOR_YCrCb2RGB;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY_INV;
import static org.opencv.imgproc.Imgproc.THRESH_TOZERO;
import static org.opencv.imgproc.Imgproc.THRESH_TOZERO_INV;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.equalizeHist;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.imgproc.Imgproc.threshold;

/**
 * Created by nexus on 2017-07-18.
 */

public class drawViewer extends View {

    static String TAG = "drawViewer";

    Context mContext;

    private static int mRotation = 0;

    private static int mObjNumber = 0;
    private static boolean mObjProcessed = false;
    private static boolean mObjMiddle = false;
    private static int mMoreRectCount = 0;
    private static int mMoreRect[] = new int[10];
    private static Date mObjProcessStartTime;
    private static RectF mObjectRect[] = new RectF[10];
    private static float mObjectConfidence[] = new float[10];
    private static String mObjectName[] = new String[10];
    private static int mObjectColor[] = new int[10];
    private static float mMadeSize[] = new float[10];
    private static boolean mObjDisplay[] = new boolean[10];

    private BorderedText mCenterBorderedText;
    private Paint mCenterObjBoxPaint = new Paint();

    private BorderedText mBorderedText;
    private Paint mObjBoxPaint = new Paint();

    private static Bitmap mSegmentLowBmp;
    private static int[] mSegmentLowPixels;
    private static float[] mSegmentLowResultPixels;

    private static Bitmap mSegmentNormalBmp;
    private static int[] mSegmentNormalPixels;
    private static float[] mSegmentNormalResultPixels;

    private static Bitmap mSegmentHighBmp;
    private static int[] mSegmentHighPixels;
    private static float[] mSegmentHighResultPixels;

    private static Bitmap mSegmentBmpForCapture;
    private static int[] mSegmentPixelsForCapture;
    private static float[] mSegmentResultPixelsForCapture;
    private static int mSegmentColors[];

    private static int mObjFaceStatus = -1;
    private static boolean mDebugScreenOn = false;

    private static int gDepthStatus = -1;
    Paint paintDepth = new Paint();
    Bitmap gDepth;
    Bitmap gOnePersonImage;
    Bitmap gTwoPersonImage;
    Bitmap gNotThreePersonImage;

    static boolean gDrawFace = false;
    static int gFaceTotal = -1;
    static int gCameraFront = 0;

    static float gTouchXCheck = -1.0f;
    static float gTouchYCheck = -1.0f;

    static int []gFaceLeft = {0,0,0,0,0,0,0,0,0,0};
    static int []gFaceRight = {0,0,0,0,0,0,0,0,0,0};
    static int []gFaceTop = {0,0,0,0,0,0,0,0,0,0};
    static int []gFaceBottom = {0,0,0,0,0,0,0,0,0,0};
    Paint facePaint = new Paint();

    public drawViewer(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        Log.d(TAG, "drawViewer");

        int colors[] = new int[21];
        int alpha = 100;
        //colors[0] = Color.argb(alpha, 0, 0, 0);
        colors[0] = Color.argb(0, 0, 0, 0);
        colors[1] = Color.argb(alpha, 128, 0, 0);
        colors[2] = Color.argb(alpha, 0, 128, 0);
        colors[3] = Color.argb(alpha, 128, 128, 0);
        colors[4] = Color.argb(alpha, 0, 0, 128);
        colors[5] = Color.argb(alpha, 128, 0, 128);
        colors[6] = Color.argb(alpha, 0, 128, 128);
        colors[7] = Color.argb(alpha, 128, 128, 128);
        colors[8] = Color.argb(alpha, 64, 0, 0);
        colors[9] = Color.argb(alpha, 192, 0, 0);
        colors[10] = Color.argb(alpha, 64, 128, 0);
        colors[11] = Color.argb(alpha, 192, 128, 0);
        colors[12] = Color.argb(alpha, 64, 0, 128);
        colors[13] = Color.argb(alpha, 192, 0, 128);
        colors[14] = Color.argb(alpha, 64, 128, 128);
        colors[15] = Color.argb(alpha, 192, 128, 128);
        colors[16] = Color.argb(alpha, 0, 64, 0);
        colors[17] = Color.argb(alpha, 128, 64, 0);
        colors[18] = Color.argb(alpha, 0, 192, 0);
        colors[19] = Color.argb(alpha, 128, 192, 0);
        colors[20] = Color.argb(alpha, 0, 64, 128);
        this.mSegmentColors = colors;

        mCenterObjBoxPaint.setStyle(Paint.Style.STROKE);
        mCenterObjBoxPaint.setStrokeWidth(6.0f);
        mCenterObjBoxPaint.setStrokeCap(Paint.Cap.ROUND);
        mCenterObjBoxPaint.setStrokeJoin(Paint.Join.ROUND);
        mCenterObjBoxPaint.setStrokeMiter(100);
        mCenterObjBoxPaint.setColor(Color.WHITE);

        mObjBoxPaint.setColor(Color.RED);
        mObjBoxPaint.setStyle(Paint.Style.STROKE);
        mObjBoxPaint.setStrokeWidth(6.0f);
        mObjBoxPaint.setStrokeCap(Paint.Cap.ROUND);
        mObjBoxPaint.setStrokeJoin(Paint.Join.ROUND);
        mObjBoxPaint.setStrokeMiter(100);

        float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 12/*18*/, mContext.getResources().getDisplayMetrics());
        mBorderedText = new BorderedText(textSizePx);
        mCenterBorderedText = new BorderedText(textSizePx);
        mCenterBorderedText.setInteriorColor(Color.WHITE);

        gDepth = BitmapFactory.decodeResource(context.getResources(), R.drawable.depth);
        gOnePersonImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.face_1person);
        gTwoPersonImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.face_2person);
        gNotThreePersonImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.face_3not);

        mObjProcessStartTime = new Date();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        Log.d(TAG, "onMeasure");

        // 측정된 폭과 높이를 출력해 보자
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);

        // 패딩값을 측정값의 10%를 주어 뺀다.
        int paddingWidth = width / 10;
        int paddingHeight = height / 10;

        setMeasuredDimension(width - paddingWidth, height - paddingHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.d(TAG, "rect : (x, y, w, h) : " + left + " " + top + " " + (right-left) + " " + (bottom-top));
    }

    public void doRotation(Canvas canvas)
    {
        Log.d(TAG, "[canvas-test] mRotation ; "+mRotation+", canvas w : "+canvas.getWidth()+", h : "+canvas.getHeight());
        int bitmapCenterX = canvas.getWidth() / 2;
        int bitmapCenterY = canvas.getHeight() / 2;
        if( mRotation == 270 ) {
            canvas.rotate(90.f, bitmapCenterX, bitmapCenterY);
            canvas.translate(-180.f, 180.f);
        }
        else if( mRotation == 180 )
        {
            canvas.rotate(180.f, bitmapCenterX, bitmapCenterY);
        }
        else if( mRotation == 90 )
        {
            canvas.rotate(270.f, bitmapCenterX, bitmapCenterY);
            canvas.translate(-180.f, 180.f);
        }
        Log.d(TAG, "[canvas-test] mRotation2 ; "+mRotation+", canvas w : "+canvas.getWidth()+", h : "+canvas.getHeight());
    }

    public static Rect pointBoxRotate(int width, int height, int left, int top, int right, int bottom, int rotate)
    {
        int newLeft = 0, newRight = 0, newTop = 0, newBottom = 0;
        if( rotate == 90 )
        {
            newLeft = height - bottom;
            newTop = left;
            newRight = height - top;
            newBottom = right;
        }
        else if( rotate == 180 )
        {
            newLeft = width - right;
            newTop = height - bottom;
            newRight = width - left;
            newBottom = height - top;
        }
        else if( rotate == 270 )
        {
            newLeft = top;
            newTop = width - right;
            newRight = bottom;
            newBottom = width - left;
        }
        else
        {
            newLeft = left;
            newTop = top;
            newRight = right;
            newBottom = bottom;
        }

        return new Rect(newLeft, newTop, newRight, newBottom);
    }

    public static void setObjectRect(int moreRectCount, int moreRect[], boolean midObj, boolean displayOn[], RectF rect[], float madeSize[], float confidence[], String name[], int color[], int objNo, boolean run)
    {
        Matrix scaleMat =
                ImageUtils.getTransformationMatrix(
                        1080, 1440,
                        aiCamParameters.LCD_WIDTH_I, aiCamParameters.LCD_HEIGHT_I,
                        0, false/*MAINTAIN_ASPECT*/);

        mMoreRectCount = moreRectCount;
        for(int i = 0; i < mMoreRectCount; i++)
        {
            mMoreRect[i] = moreRect[i];
        }

        mObjMiddle = midObj;

        mObjProcessed = run;
        if( mObjProcessed == true )
        {
            mObjProcessStartTime = new Date();
            Log.d("boxSync", "mObjProcessed started : "+mObjProcessStartTime.getTime());
        }
        mObjNumber = objNo;

        for (int i = 0; i < mObjNumber; i++) {

            RectF result = new RectF();
            if( rect[i] != null ) {
                scaleMat.mapRect(result, rect[i]);
                rect[i] = result;
            }

            mObjectRect[i] = new RectF(pointBoxRotate(aiCamParameters.LCD_WIDTH_I, aiCamParameters.LCD_HEIGHT_I, (int)rect[i].left, (int)rect[i].top, (int)rect[i].right, (int)rect[i].bottom, mRotation));
            mObjectConfidence[i] = confidence[i];
            mObjectName[i] = name[i];
            mObjectColor[i] = color[i];
            mMadeSize[i] = madeSize[i];
            mObjDisplay[i] = displayOn[i];
        }
    }

    public static void handleSegmentationLow(final long timestamp, final Segmentor.Segmentation potential) {
        int width = potential.getWidth();
        int height = potential.getHeight();

        if(mSegmentLowBmp == null) {
            mSegmentLowBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        if(mSegmentLowPixels == null) {
            mSegmentLowPixels = new int[mSegmentLowBmp.getHeight()*mSegmentLowBmp.getWidth()];
        }
        mSegmentLowResultPixels = potential.getPixels();

        Log.o(TAG, "Use Sally width : " + mSegmentLowBmp.getWidth() + ", height : " + mSegmentLowBmp.getHeight());

        int numClass = potential.getNumClass();
        int[] visitedLabels = new int[numClass];
        for(int i = 0; i < width; i++) {
            for(int j = 0; j < height; j++) {
                int classNo = (int)mSegmentLowResultPixels[j*height+i]; // very tricky part
                if( classNo == 15 ) {
                    mSegmentLowPixels[j * mSegmentLowBmp.getWidth() + i] = Color.argb(128, 250, 0, 0);//mSegmentColors[classNo];
                }
                else
                {
                    mSegmentLowPixels[j * mSegmentLowBmp.getWidth() + i] = mSegmentColors[0];
                }
                visitedLabels[classNo] = 1;
            }
        }

        mSegmentLowBmp.setPixels(mSegmentLowPixels, 0, mSegmentLowBmp.getWidth(), 0, 0, mSegmentLowBmp.getWidth(), mSegmentLowBmp.getHeight());
    }

    public static void handleSegmentationNormal(final long timestamp, final Segmentor.Segmentation potential) {
        int width = potential.getWidth();
        int height = potential.getHeight();

        if(mSegmentNormalBmp == null) {
            mSegmentNormalBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        if(mSegmentNormalPixels == null) {
            mSegmentNormalPixels = new int[mSegmentNormalBmp.getHeight()*mSegmentNormalBmp.getWidth()];
        }
        mSegmentNormalResultPixels = potential.getPixels();

        Log.o(TAG, "Use Sally width : " + mSegmentNormalBmp.getWidth() + ", height : " + mSegmentNormalBmp.getHeight());

        int numClass = potential.getNumClass();
        int[] visitedLabels = new int[numClass];
        for(int i = 0; i < width; i++) {
            for(int j = 0; j < height; j++) {
                int classNo = (int)mSegmentNormalResultPixels[j*height+i]; // very tricky part
                if( classNo == 15 ) {
                    mSegmentNormalPixels[j * mSegmentNormalBmp.getWidth() + i] = Color.argb(128, 0, 250, 0);//mSegmentColors[classNo];
                }
                else
                {
                    mSegmentNormalPixels[j * mSegmentNormalBmp.getWidth() + i] = mSegmentColors[0];
                }
                visitedLabels[classNo] = 1;
            }
        }

        mSegmentNormalBmp.setPixels(mSegmentNormalPixels, 0, mSegmentNormalBmp.getWidth(), 0, 0, mSegmentNormalBmp.getWidth(), mSegmentNormalBmp.getHeight());
    }

    public static void handleSegmentationHigh(final long timestamp, final Segmentor.Segmentation potential) {
        int width = potential.getWidth();
        int height = potential.getHeight();

        if(mSegmentHighBmp == null) {
            mSegmentHighBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        if(mSegmentHighPixels == null) {
            mSegmentHighPixels = new int[mSegmentHighBmp.getHeight()*mSegmentHighBmp.getWidth()];
        }
        mSegmentHighResultPixels = potential.getPixels();
        
        int numClass = potential.getNumClass();
        int[] visitedLabels = new int[numClass];
        for(int i = 0; i < width; i++) {
            for(int j = 0; j < height; j++) {
                int classNo = (int)mSegmentHighResultPixels[j*height+i]; // very tricky part
                if( classNo == 15 ) {
                    mSegmentHighPixels[j * mSegmentHighBmp.getWidth() + i] = Color.argb(128, 0, 0, 250);//mSegmentColors[classNo+1];
                }
                else
                {
                    mSegmentHighPixels[j * mSegmentHighBmp.getWidth() + i] = mSegmentColors[0];
                }
                visitedLabels[classNo] = 1;
            }
        }

        mSegmentHighBmp.setPixels(mSegmentHighPixels, 0, mSegmentHighBmp.getWidth(), 0, 0, mSegmentHighBmp.getWidth(), mSegmentHighBmp.getHeight());
    }

    public static void setDebugScreenOn(boolean onoff)
    {
        mDebugScreenOn = onoff;
    }

    public static void setObjFaceStatus(int status)
    {
        mObjFaceStatus = status;
    }

    public static void setDepthStatus(int status)
    {
        gDepthStatus = status;
    }

    public static void setFaceData(int left, int right, int top, int bottom, int faceCount, int faceTotal, int front, boolean draw) {
        Log.d("setFaceData", "gFaceTotal : " + gFaceTotal);

        gDrawFace = draw;
        gFaceTotal = faceTotal;
        gCameraFront = front;

        if( gDrawFace == true ) {

            gFaceLeft[faceCount] = aiCamParameters.LCD_WIDTH_I - bottom;
            gFaceRight[faceCount] = aiCamParameters.LCD_WIDTH_I - top;
            if(front == 1)
            {
                gFaceTop[faceCount] = aiCamParameters.LCD_HEIGHT_I - right;
                gFaceBottom[faceCount] = aiCamParameters.LCD_HEIGHT_I - left;
            }
            else {
                gFaceTop[faceCount] = left;
                gFaceBottom[faceCount] = right;
            }
        }
    }

    public static void setTouchData(float x, float y)
    {
        gTouchXCheck = x;
        gTouchYCheck = y;
    }

    private static Bitmap mAIPreviewBmBefore = null;
    private static Bitmap mAIPreviewBmAfter = null;
    private static Bitmap mMovingBmp = null;

    private static byte saturate(double val) {
        int iVal = (int) Math.round(val);
        iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
        return (byte) iVal;
    }

    private static Mat setContrast( Mat image )
    {
        Mat newImage = Mat.zeros(image.size(), image.type());
        double alpha = 1.3; /*< Simple contrast control 1.0 - 3.0*/
        int beta = 40;       /*< Simple brightness control 0-100*/

        byte[] imageData = new byte[(int) (image.total()*image.channels())];
        image.get(0, 0, imageData);
        byte[] newImageData = new byte[(int) (newImage.total()*newImage.channels())];
        for (int y = 0; y < image.rows(); y++) {
            for (int x = 0; x < image.cols(); x++) {
                for (int c = 0; c < image.channels(); c++) {
                    double pixelValue = imageData[(y * image.cols() + x) * image.channels() + c];
                    pixelValue = pixelValue < 0 ? pixelValue + 256 : pixelValue;
                    newImageData[(y * image.cols() + x) * image.channels() + c]
                            = saturate(alpha * pixelValue + beta);
                }
            }
        }
        newImage.put(0, 0, newImageData);

        return newImage;
    }

    public static void setAIPreviewBitmapBefore(Mat input)
    {
        Log.d(TAG, "setAIPreviewBitmapBefore : "+input.cols()+", "+input.rows());

        Mat input2 = new Mat();
        rotate(input, input2, ROTATE_90_COUNTERCLOCKWISE);
        flip(input2, input2, 1);
        resize(input2, input2, new Size(1080/3, 1440/3));

        if(mAIPreviewBmBefore == null) {
            mAIPreviewBmBefore = Bitmap.createBitmap(input2.cols(), input2.rows(), Bitmap.Config.ARGB_8888);
        }
        Utils.matToBitmap(input2, mAIPreviewBmBefore);
    }

    private static Mat equalizeIntensity(Mat inputImage)
    {
        if(inputImage.channels() >= 3)
        {
            Mat ycrcb = new Mat();

            cvtColor(inputImage, ycrcb, COLOR_RGBA2RGB);
            cvtColor(inputImage, ycrcb, COLOR_RGB2YCrCb);

            List<Mat> channels  = new ArrayList<>();
            split(ycrcb, channels);

//            imwrite("/sdcard/water/Y.jpg", channels.get(0));
//            imwrite("/sdcard/water/cb.jpg", channels.get(1));
//            imwrite("/sdcard/water/cr.jpg", channels.get(2));

            //equalizeHist(channels.get(0), channels.get(0));
            //th3 = cv2.adaptiveThreshold(img,255,cv2.ADAPTIVE_THRESH_GAUSSIAN_C,\
              //      cv2.THRESH_BINARY,11,2)

//            Imgproc.adaptiveThreshold(channels.get(0), channels.get(0), 125, Imgproc.ADAPTIVE_THRESH_MEAN_C,
//                    Imgproc.THRESH_BINARY, 11, 12);
            threshold(channels.get(0), channels.get(0), 100, 0, THRESH_TOZERO);
            Mat hairMat = new Mat();
            threshold(channels.get(0), hairMat, 0, 255, THRESH_BINARY_INV);
            hairMat.copyTo(channels.get(0), hairMat);

            Mat result = new Mat();
            merge(channels, ycrcb);

            cvtColor(ycrcb, result, COLOR_YCrCb2RGB);
            cvtColor(inputImage, ycrcb, COLOR_RGB2RGBA);

            return result;
        }

        return new Mat();
    }

    public static void setAIPreviewBitmapAfter(Mat input, double contrastValue, int onOff)
    {
        Log.d(TAG, "setAIPreviewBitmapAfter : "+input.cols()+", "+input.rows());

        Mat input2 = new Mat();
        rotate(input, input2, ROTATE_90_COUNTERCLOCKWISE);

        if( onOff == 1 ) {
            //input2 = setContrast(input2);
            //input2.convertTo(input2, -1, contrastValue, 0); //increase the contrast by 4
            Log.d(TAG, "input2 channels : "+input2.channels());
            input2 = equalizeIntensity(input2);
            input2.convertTo(input2, -1, contrastValue, 0); //increase the contrast by 4
        }

        flip(input2, input2, 1);
        resize(input2, input2, new Size(1080/3, 1440/3));

        if(mAIPreviewBmAfter == null) {
            mAIPreviewBmAfter = Bitmap.createBitmap(input2.cols(), input2.rows(), Bitmap.Config.ARGB_8888);
        }
        Utils.matToBitmap(input2, mAIPreviewBmAfter);
    }

    public static void setMovingBitmap(Mat input)
    {
        Mat input2 = new Mat();

        if( aiCamParameters.mCameraLocationInt == 1 ) {
            rotate(input, input2, ROTATE_90_COUNTERCLOCKWISE);
            flip(input2, input2, 1);
            cvtColor(input2, input2, COLOR_GRAY2RGBA);
        }
        else
        {
            rotate(input, input2, ROTATE_90_CLOCKWISE);
            //flip(input2, input2, );
            cvtColor(input2, input2, COLOR_GRAY2RGBA);
        }
        resize(input2, input2, new Size(1080/1, 1440/1));

        if(mMovingBmp == null) {
            mMovingBmp = Bitmap.createBitmap(input2.cols(), input2.rows(), Bitmap.Config.ARGB_8888);
        }
        Utils.matToBitmap(input2, mMovingBmp);
    }

    public static void setRotationData(int rotate)
    {
        mRotation = rotate;
    }

    public static int mContrastOnOff = 0;
    public static void onOffContrast()
    {
        mContrastOnOff = 1 - mContrastOnOff;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw");
        canvas.save();

        doRotation(canvas);

        Log.d("segDraw", "canvas.getWidth() : "+canvas.getWidth());
        Log.d("segDraw", "canvas.getHeight() : "+canvas.getHeight());

        //if( mDebugScreenOn == true ) {
            //For Test
             paintDepth.setAlpha(255);
            if (mAIPreviewBmBefore != null) {
                canvas.drawBitmap(mAIPreviewBmBefore, 0, 0/*1440 - mAIPreviewBmBefore.getHeight()*/, paintDepth);
            }

            if (mAIPreviewBmAfter != null) {
                canvas.drawBitmap(mAIPreviewBmAfter, 1080 - mAIPreviewBmAfter.getWidth(), 0/*1440 - mAIPreviewBmAfter.getHeight()*/, paintDepth);
            }

            if( mContrastOnOff == 1 ) {
                if (mMovingBmp != null) {
                    paintDepth.setAlpha(127);
                    canvas.drawBitmap(mMovingBmp, 1080 - mMovingBmp.getWidth(), 0/*1440 - mAIPreviewBmAfter.getHeight()*/, paintDepth);
                }
            }
        //}

        if( mDebugScreenOn == true ) {
            facePaint.setAlpha(255);
            facePaint.setColor(Color.MAGENTA);
            facePaint.setStyle(Paint.Style.STROKE);
            facePaint.setStrokeWidth(8);
            facePaint.setTextSize(48.f);

            for (int i = 0; i < gFaceTotal; i++) {
                Rect rect = pointBoxRotate(aiCamParameters.LCD_WIDTH_I, aiCamParameters.LCD_HEIGHT_I, gFaceLeft[i], gFaceTop[i], gFaceRight[i], gFaceBottom[i], mRotation);

//            String text = "" + (int) ratePink;
//            float addX = ((float) rect.right - (float) rect.left) * 0.5f;
//            float addY = ((float) rect.bottom - (float) rect.top) * 0.5f;
//            canvas.drawText(text, (float) rect.left + addX, (float) rect.top + addY, facePaint);

                //if (gDrawFace == true && mInfoOn == true) {
                if (gDrawFace == true) {
                    canvas.drawRect(rect, facePaint);
                }
            }
        }

        if(mSegmentLowBmp != null && mDebugScreenOn == true) {

            Log.d("segDraw", "mSegmentBmp.getWidth() : "+mSegmentLowBmp.getWidth());
            Log.d("segDraw", "mSegmentBmp.getWidth() : "+mSegmentLowBmp.getHeight());

            final Matrix matrix = new Matrix();
            float multiplierX = canvas.getWidth()/(float)mSegmentLowBmp.getWidth();
            float multiplierY = multiplierX*(float)aiCamParameters.PREVIEW_WIDTH_I/(float)aiCamParameters.PREVIEW_HEIGHT_I;
            matrix.postRotate(360-mRotation);
            matrix.postScale(multiplierX, multiplierY);
            matrix.postTranslate(0, 0);
            canvas.drawBitmap(mSegmentLowBmp, matrix, new Paint(Paint.FILTER_BITMAP_FLAG));
        }

        if(mSegmentNormalBmp != null && mDebugScreenOn == true) {

            Log.d("segDraw", "mSegmentBmp.getWidth() : "+mSegmentNormalBmp.getWidth());
            Log.d("segDraw", "mSegmentBmp.getWidth() : "+mSegmentNormalBmp.getHeight());

            final Matrix matrix = new Matrix();
            float multiplierX = canvas.getWidth()/(float)mSegmentNormalBmp.getWidth();
            float multiplierY = multiplierX*(float)aiCamParameters.PREVIEW_WIDTH_I/(float)aiCamParameters.PREVIEW_HEIGHT_I;
            matrix.postRotate(360-mRotation);
            matrix.postScale(multiplierX, multiplierY);
            matrix.postTranslate(0, 0);
            canvas.drawBitmap(mSegmentNormalBmp, matrix, new Paint(Paint.FILTER_BITMAP_FLAG));
        }

        if(mSegmentHighBmp != null && mDebugScreenOn == true) {

            Log.d("segDraw", "mSegmentBmp.getWidth() : "+mSegmentHighBmp.getWidth());
            Log.d("segDraw", "mSegmentBmp.getWidth() : "+mSegmentHighBmp.getHeight());

            final Matrix matrix = new Matrix();
            float multiplierX = canvas.getWidth()/(float)mSegmentHighBmp.getWidth();
            float multiplierY = multiplierX*(float)aiCamParameters.PREVIEW_WIDTH_I/(float)aiCamParameters.PREVIEW_HEIGHT_I;
            matrix.postRotate(360-mRotation);
            matrix.postScale(multiplierX, multiplierY);
            matrix.postTranslate(0, 0);
            canvas.drawBitmap(mSegmentHighBmp, matrix, new Paint(Paint.FILTER_BITMAP_FLAG));
        }

        int width;
        int height;
        if (mRotation == 90 || mRotation == 270) {
            width = aiCamParameters.LCD_HEIGHT_I;
            height = aiCamParameters.LCD_WIDTH_I;
        } else {
            width = aiCamParameters.LCD_WIDTH_I;
            height = aiCamParameters.LCD_HEIGHT_I;
        }

        int detphImgWidth = gDepth.getWidth() / 2;
        int detphImgHeight = 0;

        Log.d(TAG, "gDepthStatus : "+gDepthStatus+", gFaceTotal : "+gFaceTotal);

        if (gDepthStatus == 1) {
            paintDepth.setAlpha(255);
            canvas.drawBitmap(gDepth, width / 2 - detphImgWidth, (height / 6 * 5) - detphImgHeight, paintDepth);
        } else if (gDepthStatus == 2) {

        } else if (gDepthStatus == 3) {
            if (gFaceTotal == 1) {
                canvas.drawBitmap(gOnePersonImage, width / 2 - gOnePersonImage.getWidth() / 2, (height / 6 * 5) - gOnePersonImage.getHeight() / 2, null);
            } else if (gFaceTotal == 2) {
                canvas.drawBitmap(gTwoPersonImage, width / 2 - gTwoPersonImage.getWidth() / 2, (height / 6 * 5) - gTwoPersonImage.getHeight() / 2, null);
            } else if (gFaceTotal > 2) {
                Rect rect = new Rect(width / 2 - gNotThreePersonImage.getWidth() / 3 / 2, (height / 6 * 5) - gNotThreePersonImage.getHeight() / 3 / 2,
                        width / 2 + gNotThreePersonImage.getWidth() / 3 / 2, (height / 6 * 5) + gNotThreePersonImage.getHeight() / 3 / 2);
                canvas.drawBitmap(gNotThreePersonImage, null, rect, null);
            }
        } else if (gDepthStatus == 4) {

        }

        if( mObjFaceStatus == 0 ) {
            drawObject(canvas);
        }

        canvas.restore();
        invalidate();
    }

    private void drawObject(Canvas canvas)
    {
        boolean mInfoOn = true;

        if( (gCameraFront == 0 || gCameraFront == 1) && gTouchXCheck == -1.0f ) {
            Matrix scaleMat =
                    ImageUtils.getTransformationMatrix(
                            300, 300,
                            canvas.getWidth(), 1440/*canvas.getHeight()*/,
                            0, false/*MAINTAIN_ASPECT*/);

            Matrix scaleCustomMat =
                    ImageUtils.getTransformationMatrix(
                            1080, 1440,
                            aiCamParameters.LCD_WIDTH_I, aiCamParameters.LCD_HEIGHT_I,
                            0, false/*MAINTAIN_ASPECT*/);

            RectF centerResult = new RectF();

            if( mObjNumber == 0 )
            {
                if( mObjMiddle == true)
                {
                    if (mObjProcessed == true) {
                        Log.d("mObjMiddle", "mObjProcessed started : "+mObjProcessStartTime.getTime());

                        mCenterObjBoxPaint.setAlpha(255);
                        mCenterBorderedText.setAlpha(255);
                    }
                    else {
                        if (new Date().getTime() - mObjProcessStartTime.getTime() > (double) SFTunner2.mA0Time) {
                            mCenterObjBoxPaint.setAlpha(0);
                            mCenterBorderedText.setAlpha(0);
                        } else if (new Date().getTime() - mObjProcessStartTime.getTime() > (double) SFTunner2.mA1Time) {
                            mCenterObjBoxPaint.setAlpha(127);
                            mCenterBorderedText.setAlpha(127);
                        } else if (new Date().getTime() - mObjProcessStartTime.getTime() > (double) SFTunner2.mA1Time / 2) {
                            mCenterObjBoxPaint.setAlpha(127);
                            mCenterBorderedText.setAlpha(127);
                        }
                    }

                    int []rect = new int[4];
                    jniController.getMidRect(rect);

                    RectF mid = new RectF(rect[0], rect[1], rect[2], rect[3]);

                    RectF result = new RectF();
                    if( mid != null ) {
                        scaleCustomMat.mapRect(result, mid);
                        mid = result;
                    }

                    centerResult = mid;

                    float cornerSizeCenter = Math.min(centerResult.width(), centerResult.height()) / 8.0f;

                    canvas.drawRoundRect(centerResult, cornerSizeCenter, cornerSizeCenter, mCenterObjBoxPaint);

                    int sizeRect[] = {0,0,0,0};
                    jniController.getSizeRect(sizeRect);
                    int objSize = (sizeRect[2] - sizeRect[0]) * (sizeRect[3] - sizeRect[1]);
                    int sizePercent = objSize * 100 / (aiCamParameters.PREVIEW_WIDTH_I * aiCamParameters.PREVIEW_HEIGHT_I);

                    String labelStringMiddle = String.format("AI(c)-%d", sizePercent);
                    mCenterBorderedText.drawText(canvas, centerResult.left + cornerSizeCenter, centerResult.bottom, labelStringMiddle);
                }
            }

            for (int i = 0; i < mObjNumber; i++) {
                if (mObjectRect[i] != null) {
                    RectF result = new RectF();

                    float cornerSize = 0.f;
                    float cornerSizeCenter = 0.f;

                    int firstObj = -1;

                    if( gFaceTotal == -1 ) {
                        result = mObjectRect[i];

                        Log.d(TAG, "1-mObjMiddle : "+mObjMiddle);
                        if( mObjMiddle == true)
                        {
                            int []rect = new int[4];
                            jniController.getMidRect(rect);
                            RectF temp = new RectF(rect[0], rect[1], rect[2], rect[3]);

                            RectF resultCenter = new RectF();
                            if( temp != null ) {
                                scaleCustomMat.mapRect(resultCenter, temp);
                                temp = resultCenter;
                            }

                            RectF mid = new RectF(pointBoxRotate(aiCamParameters.LCD_WIDTH_I, aiCamParameters.LCD_HEIGHT_I, (int)temp.left, (int)temp.top, (int)temp.right, (int)temp.bottom, mRotation));
                            centerResult = mid;

                            cornerSizeCenter = Math.min(centerResult.width(), centerResult.height()) / 8.0f;
                        }

                        cornerSize = Math.min(result.width(), result.height()) / 8.0f;

                        mObjBoxPaint.setColor(mObjectColor[i]);
                        mBorderedText.setInteriorColor(mObjectColor[i]);

                        if( gCameraFront == 0 ) {

                            if (mObjProcessed == true) {
                                Log.d("boxSync", "mObjProcessed started full draw : "+mObjProcessStartTime.getTime());
                                mObjBoxPaint.setAlpha(255);
                                mBorderedText.setAlpha(255);

                                mCenterObjBoxPaint.setAlpha(255);
                                mCenterBorderedText.setAlpha(255);
                            } else {
                                //if (new Date().getTime() - mObjProcessStartTime.getTime() > 1900/*1600*/) {
                                if (new Date().getTime() - mObjProcessStartTime.getTime() > (double)SFTunner2.mA0Time) {

                                    Log.d("boxSync", "mObjProcessed End ");

                                    mObjBoxPaint.setAlpha(0);
                                    mBorderedText.setAlpha(0);

                                    mCenterObjBoxPaint.setAlpha(0);
                                    mCenterBorderedText.setAlpha(0);
                                }
                                //else if (new Date().getTime() - mObjProcessStartTime.getTime() > 700/*1000*/) {
                                else if (new Date().getTime() - mObjProcessStartTime.getTime() > (double)SFTunner2.mA1Time) {

                                    //int firstObj = -1;
                                    for(int no = 0; no < mObjNumber; no++)
                                    {
                                        if( mObjDisplay[no] == true )
                                        {
                                            firstObj = no;
                                            break;
                                        }
                                    }

                                    int sortCount = 0;
                                    int []moreSortIndex = new int[mObjNumber];
                                    int []moreRectIndex = new int[mMoreRectCount];
                                    if( mMoreRectCount > 0 )
                                    {
                                        for( int count = firstObj+1; count < mObjNumber; count++ )
                                        {
                                            if( mObjDisplay[count] == true )
                                            {
                                                moreSortIndex[sortCount] = count;
                                                sortCount++;
                                            }
                                        }

                                        for( int count = 0; count < mMoreRectCount; count++ )
                                        {
                                            int index = mMoreRect[count]-1;
                                            if( index > -1 ) {
                                                moreRectIndex[count] = moreSortIndex[index];
                                            }
                                        }
                                    }

                                    if( mObjMiddle == true )
                                    {
                                        Log.d("boxSync", "mObjProcessed mObjMiddle End ");

                                        mObjBoxPaint.setAlpha(0);
                                        mBorderedText.setAlpha(0);

                                        mCenterObjBoxPaint.setAlpha(127);
                                        mCenterBorderedText.setAlpha(127);
                                    }
                                    else
                                    {
                                        if (i != firstObj) {

                                            Log.d("boxSync", "mObjProcessed Extra End ");

                                            mObjBoxPaint.setAlpha(0);
                                            mBorderedText.setAlpha(0);
                                        } else {
                                            Log.d("boxSync", "mObjProcessed obj End Yet");

                                            mObjBoxPaint.setAlpha(127);
                                            mBorderedText.setAlpha(127);
                                        }

                                        if( mMoreRectCount > 0 )
                                        {
                                            for( int count = 0; count < mMoreRectCount; count++ )
                                            {
                                                if( i == moreRectIndex[count] )
                                                {
                                                    mObjBoxPaint.setAlpha(127);
                                                    mBorderedText.setAlpha(127);
                                                }
                                            }
                                        }
                                    }
                                }
                                else if (new Date().getTime() - mObjProcessStartTime.getTime() > (double)SFTunner2.mA1Time/2) {

                                    Log.d("boxSync", "mObjProcessed Half End");

                                    mObjBoxPaint.setAlpha(127);
                                    mBorderedText.setAlpha(127);

                                    mCenterObjBoxPaint.setAlpha(127);
                                    mCenterBorderedText.setAlpha(127);
                                }
                            }
                        }
                        else
                        {
                            mObjBoxPaint.setAlpha(0);
                            mBorderedText.setAlpha(0);
                        }
                    }
                    else
                    {
                        if( mInfoOn == true ) {
                            scaleMat.mapRect(result, mObjectRect[i]);
                            cornerSize = Math.min(result.width(), result.height()) / 8.0f;

                            mObjBoxPaint.setColor(Color.YELLOW);
                            mBorderedText.setInteriorColor(Color.YELLOW);

                            mObjBoxPaint.setAlpha(255);
                            mBorderedText.setAlpha(255);
                        }
                        else
                        {
                            mObjBoxPaint.setAlpha(0);
                            mBorderedText.setAlpha(0);
                        }
                    }

                    if( mInfoOn == true )
                    {
                        //box center
                        canvas.drawPoint(result.centerX(), result.centerY(), mObjBoxPaint);
                    }

                    canvas.drawRoundRect(result, cornerSize, cornerSize, mObjBoxPaint);
                    Log.d(TAG, "2-mObjMiddle : "+mObjMiddle);
                    if( mObjMiddle == true )
                    {
                        Log.d(TAG, "2-mObjMiddle Alpha : "+mCenterObjBoxPaint.getAlpha());
                        canvas.drawRoundRect(centerResult, cornerSizeCenter, cornerSizeCenter, mCenterObjBoxPaint);
                    }


                    double middleX = (double) (1080.f / 2.0f);
                    double middleY = (double) (1440.f / 2.0f);
                    double objMiddleX = (double) (result.centerX());
                    double objMiddleY = (double) (result.centerY());
                    double distance = Math.sqrt(Math.pow(middleX - objMiddleX, 2.0) + Math.pow(middleY - objMiddleY, 2.0));
                    double maxDistance = Math.sqrt(Math.pow(middleX - 0, 2.0) + Math.pow(middleY - 0, 2.0));

                    if( gFaceTotal == -1 ) {

                        Log.d(TAG, "mObjectName["+i+"] : "+mObjectName[i]);
                        int sizeRect[] = {0,0,0,0};
                        jniController.getSizeRect(sizeRect);
                        int objSize = (sizeRect[2] - sizeRect[0]) * (sizeRect[3] - sizeRect[1]);
                        Log.d(TAG, "[sizePercent] objSize : "+objSize);
                        int sizePercent = objSize * 100 / (aiCamParameters.PREVIEW_WIDTH_I * aiCamParameters.PREVIEW_HEIGHT_I);
                        Log.d(TAG, "sizePercent : "+sizePercent);

                        int boxSize = (int)((result.width() * result.height()) * 100.f / (1440.f * 1080.f));


                        String labelString;
                        if( !TextUtils.isEmpty(mObjectName[i]) )
                        {
                            if( mObjectName[i].equals("person") ==true ) {
                                if (mObjDisplay[i] == false)
                                {
                                    labelString = String.format("AI(p%dX)%.0f%%-%d", i, mMadeSize[i], boxSize);
                                }
                                else {
                                    if( firstObj == i )
                                    {
                                        labelString = String.format("AI(p%d)%.0f%%-%d(%d)", i, mMadeSize[i], boxSize, sizePercent);
                                    }
                                    else {
                                        labelString = String.format("AI(p%d)%.0f%%-%d", i, mMadeSize[i], boxSize);
                                    }
                                }
                            }
                            else
                            {
                                if( mObjDisplay[i] == false )
                                {
                                    labelString = String.format("AI(%dX)%.0f%%-%d", i, mMadeSize[i], boxSize);
                                }
                                else {
                                    if( firstObj == i )
                                    {
                                        labelString = String.format("AI(%d)%.0f%%-%d(%d)", i, mMadeSize[i], boxSize, sizePercent);
                                    }
                                    else {
                                        labelString = String.format("AI(%d)%.0f%%-%d", i, mMadeSize[i], boxSize);
                                    }
                                }
                            }
                        }
                        else
                        {
                            labelString = String.format("%.2f", mObjectConfidence[i]);
                        }

                        if ( mMadeSize[i] > SFTunner2.mAIScreen )
                        {
                            mBorderedText.drawText(canvas, result.left + cornerSize, result.bottom, labelString);
                        }

                        Log.d(TAG, "3-mObjMiddle : "+mObjMiddle);
                        if( mObjMiddle == true)
                        {
                            String labelStringMiddle = String.format("AI(c)-%d", sizePercent);
                            mCenterBorderedText.drawText(canvas, centerResult.left + cornerSizeCenter, centerResult.bottom, labelStringMiddle);
                        }
                    }
                    else
                    {
                        final String labelString =
                                !TextUtils.isEmpty(mObjectName[i])
                                        ? String.format("%s-%.2f%%", mObjectName[i], mObjectConfidence[i])
                                        : String.format("%.2f", mObjectConfidence[i]);

                        mBorderedText.drawText(canvas, result.left + cornerSize, result.bottom, labelString);
                    }
                }
            }
        }
    }
}
