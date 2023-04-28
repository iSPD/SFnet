/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package com.ispd.sfcam.touchView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.ispd.sfcam.AIEngineObjDetection.env.ImageUtils;
import com.ispd.sfcam.R;
import com.ispd.sfcam.cameraFragment;
import com.ispd.sfcam.jniController;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.SFTunner2;

import java.util.ArrayList;
import java.util.Date;

public class DrawView extends View {

    static private String TAG = "TouchView";

    static boolean mTouched = false;
    static boolean mBoxTouched = false;

    static boolean mTouchedClone = false;
    static boolean mBoxTouchedClone = false;

    static float mTouchX = -1.f;
    static float mTouchY = -1.f;

    Point point1, point3;
    Point point2, point4;
    Point pointClone1, pointClone3;
    Point pointClone2, pointClone4;

    Point sortPoint[] = new Point[4];
    static Point startMovePoint;
    static Point startMovePointClone;
    private static boolean mTouchMoved = false;
    private static boolean mTouchMovedClone = false;

    static Date mTouchProcessStartTime;
    static Date mTouchProcessStartTimeClone;
    //???
    private static boolean mPlusButtonPressed = false;
    private static boolean mMultiTouchOn = false;
    private static boolean mMinusTouchOn = false;

    /**
     * point1 and point 3 are of same group and same as point 2 and point4
     */
    static int groupId = 2;
    static int groupIdClone = 2;
    static private ArrayList<ColorBall> colorballs;
    static private ArrayList<ColorBall> colorballsClone;
    // array that holds the balls
    static private int balID = 0;
    static private int balIDClone = 0;
    // variable to know what ball is being dragged
    static Paint paint;
    static Paint paintPoint;
    static Paint allowPaint;
    static Paint linePaint;
    Canvas canvas;
    Bitmap mBitmapTouch;

    public DrawView(Context context) {
        super(context);
        init(context);
        Log.d(TAG, "DrawView-1");
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Log.d(TAG, "DrawView-2");
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        Log.d(TAG, "DrawView-3");
    }

    private void init(Context context) {

        Log.d(TAG, "init");

        paint = new Paint();
        paintPoint = new Paint();

        allowPaint = new Paint();
        linePaint = new Paint();

        allowPaint.setAntiAlias(true);
        allowPaint.setDither(true);
        allowPaint.setColor(Color.parseColor("#FFFFFFFF"));
        allowPaint.setStyle(Paint.Style.STROKE);
        allowPaint.setStrokeJoin(Paint.Join.ROUND);
        allowPaint.setStrokeWidth(10);

        linePaint.setAntiAlias(true);
        linePaint.setDither(true);
        linePaint.setColor(Color.parseColor("#88FFFFFF"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeWidth(2.5f);

        //setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();

        point1 = new Point();
        point1.x = -10;
        point1.y = -10;

        point2 = new Point();
        point2.x = -10;
        point2.y = -10;

        point3 = new Point();
        point3.x = -10;
        point3.y = -10;

        point4 = new Point();
        point4.x = -10;
        point4.y = -10;

        colorballs = new ArrayList<ColorBall>();
//        colorballs.add(0,new ColorBall(context, R.drawable.gray_circle_size, point1,0));
//        colorballs.add(1,new ColorBall(context, R.drawable.gray_circle_size, point2,1));
//        colorballs.add(2,new ColorBall(context, R.drawable.gray_circle_plus, point3,2));
//        colorballs.add(3,new ColorBall(context, R.drawable.gray_circle_size, point4,3));
        colorballs.add(0,new ColorBall(context, R.drawable.gray_circle, point1,0));
        colorballs.add(1,new ColorBall(context, R.drawable.gray_circle, point2,1));
        colorballs.add(2,new ColorBall(context, R.drawable.gray_circle, point3,2));
        colorballs.add(3,new ColorBall(context, R.drawable.gray_circle, point4,3));

        pointClone1 = new Point();
        pointClone1.x = 1080+10;
        pointClone1.y = -10;

        pointClone2 = new Point();
        pointClone2.x = 1080+10;
        pointClone2.y = -10;

        pointClone3 = new Point();
        pointClone3.x = 1080+10;
        pointClone3.y = -10;

        pointClone4 = new Point();
        pointClone4.x = 1080+10;
        pointClone4.y = -10;

        colorballsClone = new ArrayList<ColorBall>();
        colorballsClone.add(0,new ColorBall(context, R.drawable.gray_circle_size, pointClone1,0));
        colorballsClone.add(1,new ColorBall(context, R.drawable.gray_circle_size, pointClone2,1));
        colorballsClone.add(2,new ColorBall(context, R.drawable.gray_circle_size, pointClone3,2));
        colorballsClone.add(3,new ColorBall(context, R.drawable.gray_circle_minus, pointClone4,3));

        mBitmapTouch = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.touch_grid);

        mTouchProcessStartTime = new Date();
        mTouchProcessStartTimeClone = new Date();
    }

    static public void setTouch(float x, float y)
    {
        Log.d(TAG, "[touch-region] setTouch : "+mTouched);

        mTouched = true;
        mTouchX = x;
        mTouchY = y;

        mBoxTouched = false;
        mMultiTouchOn = false;

        cameraFragment.setMultiTouch(false, false);

//        float []rect = {0.f, 0.f, 0.f, 0.f};
//        jniController.updateObjRectForTouch(0, rect);

        setTouchRectReset();
    }

    static public void setTouchRectReset()
    {
        int gap = 72*4 / 2;
        int left = (int)mTouchX - gap;
        int right = (int)mTouchX + gap;
        int top = (int)mTouchY - gap;
        int bottom = (int)mTouchY + gap;

        if( left < 0 )
        {
            left = 0;
            right = 72*4;
        }
        else if(right > 1080)
        {
            left = 1080 - 72*4;
            right = 1080;
        }

        if( top < 0 )
        {
            top = 0;
            bottom = 72*4;
        }
        else if(bottom > 1440)
        {
            top = 1440 - 72*4;
            bottom = 1440;
        }

        colorballs.get(0).setPoint(left, top);
        colorballs.get(1).setPoint(right, top);
        colorballs.get(2).setPoint(right, bottom);
        colorballs.get(3).setPoint(left, bottom);

        float []rect = new float[4];
        sortTouchRectnResize(rect, colorballs);
        //Original Touch...
        jniController.updateObjRectForTouch(1, rect, true);

        cameraFragment.setAutoFocusOn(true);
    }

    static public void getTouchRect()
    {
//        int []rect = new int[4];
//
//        Log.d(TAG, "[touch-region] getTouchRect");
//        Log.d(TAG, "[touch-region] mTouched : "+mTouched);
//        Log.d(TAG, "[touch-region] jniController.getMidRectForTouch(rect) : "+jniController.getMidRectForTouch(rect));
//        Log.d(TAG, "[touch-region] mBoxTouched : "+mBoxTouched);
//
//
//        if (mTouched == true && jniController.getMidRectForTouch(rect) == true && mBoxTouched == false) {
//
//                Log.d(TAG, "[touch-region] getTouchRect(getValue)");
//
//                int left = rect[0] ;
//                int top = rect[1];
//                int right = rect[2];
//                int bottom = rect[3];
//
//                Log.d(TAG, "[touch-region] else left : " + left);
//                Log.d(TAG, "[touch-region] else top : " + top);
//                Log.d(TAG, "[touch-region] else right : " + right);
//                Log.d(TAG, "[touch-region] else bottom : " + bottom);
//
//                int rate = sofTunner2.mAiTouchBoxSize;
//                int widthGap = (int)((float)(right - left) - ((float)(right - left) * (float)rate / 100.f));
//                int heightGap = (int)((float)(bottom - top) - ((float)(bottom - top) * (float)rate / 100.f));
//
//                left = left + (widthGap / 2);
//                top = top + (heightGap / 2);
//                right = right - (widthGap / 2);
//                bottom = bottom - (heightGap / 2);
//
//                colorballs.get(0).setPoint(left, top);
//                colorballs.get(1).setPoint(right, top);
//                colorballs.get(2).setPoint(right, bottom);
//                colorballs.get(3).setPoint(left, bottom);
//        }

//        if (mTouched == true && mBoxTouched == false) {
//
//            int gap = 72*4 / 2;
//            int left = (int)mTouchX - gap;
//            int right = (int)mTouchX + gap;
//            int top = (int)mTouchY - gap;
//            int bottom = (int)mTouchY + gap;
//
//            if( left < 0 )
//            {
//                left = 0;
//                right = 72*4;
//            }
//            else if(right > 1080)
//            {
//                left = 1080 - 72*4;
//                right = 1080;
//            }
//
//            if( top < 0 )
//            {
//                top = 0;
//                bottom = 72*4;
//            }
//            else if(bottom > 1440)
//            {
//                top = 1440 - 72*4;
//                bottom = 1440;
//            }
//
//            colorballs.get(0).setPoint(left, top);
//            colorballs.get(1).setPoint(right, top);
//            colorballs.get(2).setPoint(right, bottom);
//            colorballs.get(3).setPoint(left, bottom);
//
//            float []rect = new float[4];
//            sortTouchRectnResize(rect, colorballs);
//            //Original Touch...
//            jniController.updateObjRectForTouch(1, rect);
//
//            MainActivity.setAutoFocusOn(true);
//        }
    }

    static public boolean getTouchRectClone()
    {
        //mBoxTouchedClone

        if (mPlusButtonPressed == true ) {

            int gapX = 1080 / 6;
            int gapY = 1440 / 6;

            int left = 1080 / 2 - gapX;
            int top = 1440 / 2 - gapY;
            int right = 1080 / 2 + gapX;
            int bottom = 1440 / 2 + gapY;

            Log.d(TAG, "[touch-region-clone] mPlusButtonPressed left : " + left);
            Log.d(TAG, "[touch-region-clone] mPlusButtonPressed top : " + top);
            Log.d(TAG, "[touch-region-clone] mPlusButtonPressed right : " + right);
            Log.d(TAG, "[touch-region-clone] mPlusButtonPressed bottom : " + bottom);

            colorballsClone.get(0).setPoint(left, top);
            colorballsClone.get(1).setPoint(right, top);
            colorballsClone.get(2).setPoint(right, bottom);
            colorballsClone.get(3).setPoint(left, bottom);

            mPlusButtonPressed = false;
        }

        return mPlusButtonPressed;
    }

    public static void resetMultiTouch()
    {
        mMultiTouchOn = false;
    }

    // the method that draws the balls
    @Override
    protected void onDraw(Canvas canvas) {

        Log.d(TAG, "onDraw : "+groupId);

        getTouchRect();
        getTouchRectClone();

        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.parseColor("#55000000"));
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(5);

        Log.d(TAG, "SFTunner2.mAiTouchTime : "+SFTunner2.mAiTouchTime);
        if( new Date().getTime() - mTouchProcessStartTime.getTime() > SFTunner2.mAiTouchTime )
        //if( false )
        {
            paint.setColor(Color.parseColor("#00FFFFFF"));
            paintPoint.setAlpha(0);
            //allowPaint.setAlpha(0);
            //linePaint.setAlpha(0);

            colorballs.get(0).setPoint(-100, -100);
            colorballs.get(1).setPoint(-100, -100);
            colorballs.get(2).setPoint(-100, -100);
            colorballs.get(3).setPoint(-100, -100);

            jniController.resetMidRectForTouch();
        }
        else
        {
            //paint.setColor(Color.parseColor("#22FFFFFF"));
            paint.setColor(Color.parseColor("#33FFFFFF"));

            Log.d(TAG, "colorballs.get(0).getX() : "+colorballs.get(0).getX());
            Log.d(TAG, "colorballs.get(0).getY() : "+colorballs.get(0).getY());

            if( colorballs.get(0).getX() == -1 &&  colorballs.get(0).getY() == -1)
            {
                paintPoint.setAlpha(0);
            }
            else
            {
                paintPoint.setAlpha(255);
                //allowPaint.setAlpha(255);
                //linePaint.setAlpha(255);
            }
        }

        Log.d("test-point", "x1 : "+point1.x+", y1 : "+point1.y);
        Log.d("test-point", "x2 : "+point2.x+", y2 : "+point2.y);
        Log.d("test-point", "x3 : "+point3.x+", y3 : "+point3.y);
        Log.d("test-point", "x4 : "+point4.x+", y4 : "+point4.y);

        //sortTouchRect(colorballs);

        RectF roundRect;
        float cornerSize;
        //if( colorballs.get(0).getX() != 0 && colorballs.get(0).getY() != 0 && colorballs.get(2).getX() != 0 && colorballs.get(2).getY() != 0) {
            //roundRect = new RectF(sortPoint[0].x, sortPoint[0].y, sortPoint[3].x + colorballs.get(0).getWidthOfBall(), sortPoint[3].y + colorballs.get(0).getWidthOfBall());
            //roundRect = new RectF(sortPoint[0].x, sortPoint[0].y, sortPoint[3].x, sortPoint[3].y);

            //Rect oriRect = new Rect(0, 0, mBitmapTouch.getWidth(), mBitmapTouch.getHeight());
            //canvas.drawBitmap(mBitmapTouch, oriRect, roundRect, new Paint());

            int left = colorballs.get(0).getX();
            int top = colorballs.get(0).getY();
            int right = colorballs.get(2).getX();
            int bottom = colorballs.get(2).getY();
            int width = right - left;
            int height = bottom - top;

            roundRect = new RectF(left, top, right, bottom);
            cornerSize = Math.min(roundRect.width(), roundRect.height()) / 8.0f;
            //canvas.drawRoundRect(roundRect, cornerSize, cornerSize, paint);
            canvas.drawRect(roundRect, paint);

            //draw box
            canvas.drawRect(roundRect, linePaint);

            //draw allow
            int allowSize = 72;

            canvas.drawLine(left-5, top, left + allowSize, top, allowPaint);
            canvas.drawLine(left, top-5, left, top + allowSize, allowPaint);

            canvas.drawLine(right+5, top, right - allowSize, top, allowPaint);
            canvas.drawLine(right, top-5, right, top + allowSize, allowPaint);

            canvas.drawLine(right+5, bottom, right - allowSize, bottom, allowPaint);
            canvas.drawLine(right, bottom+5, right, bottom - allowSize, allowPaint);

            canvas.drawLine(left-5, bottom, left + allowSize, bottom, allowPaint);
            canvas.drawLine(left, bottom+5, left, bottom - allowSize, allowPaint);

            //draw lines
            canvas.drawLine(left, top + height/3, left + width, top + height/3, linePaint);
            canvas.drawLine(left, top + height/3 * 2, left + width, top + height/3 * 2, linePaint);

            canvas.drawLine(left + width/3, top, left + width/3, top + height, linePaint);
            canvas.drawLine(left + width/3 * 2, top, left + width/3 * 2, top + height, linePaint);
        //}

        Log.d(TAG, "mMultiTouchOn : "+mMultiTouchOn);

        if( mMultiTouchOn == true ) {

            Log.d(TAG, "[touch-region-clone] mPlusButtonPressed left : " + colorballsClone.get(0).getX());
            Log.d(TAG, "[touch-region-clone] mPlusButtonPressed top : " + colorballsClone.get(0).getY());
            Log.d(TAG, "[touch-region-clone] mPlusButtonPressed right : " + (colorballsClone.get(2).getX()+colorballs.get(0).getWidthOfBall()));
            Log.d(TAG, "[touch-region-clone] mPlusButtonPressed bottom : " + (colorballsClone.get(2).getY()+colorballs.get(0).getWidthOfBall()));

            roundRect = new RectF(colorballsClone.get(0).getX(), colorballsClone.get(0).getY(),
                    colorballsClone.get(2).getX()+colorballs.get(0).getWidthOfBall(), colorballsClone.get(2).getY()+colorballs.get(0).getWidthOfBall());
            cornerSize = Math.min(roundRect.width(), roundRect.height()) / 8.0f;
            canvas.drawRoundRect(roundRect, cornerSize, cornerSize, paint);
        }

        // draw the balls on the canvas
        /*
        int i = 0;
        for (ColorBall ball : colorballs) {
            Log.d("test-point", "i : "+(i++)+", getX : "+ball.getX()+", getY : "+ball.getY());

            canvas.drawBitmap(ball.getBitmap(), ball.getX(), ball.getY(),
                    paintPoint);
        }
        */

        if( mMultiTouchOn == true ) {

            for ( int j = 0; j < 4; j++ ) {
                canvas.drawBitmap(colorballsClone.get(j).getBitmap(), colorballsClone.get(j).getX(), colorballsClone.get(j).getY(),
                        paintPoint);
            }
        }

        invalidate();
    }

    void sortTouchRect(ArrayList<ColorBall> balls)
    {
        for(int i = 0; i < balls.size(); i++)
        {
            sortPoint[i] = balls.get(i).getPoint();
            Log.d(TAG, "pre sortPoint["+i+"] : "+sortPoint[i]);
        }


        for(int i = 0; i < sortPoint.length-1; i++)
        {
            for( int j = i+1; j < sortPoint.length; j++) {
                int preSize = sortPoint[i].x + sortPoint[i].y;
                int curSize = sortPoint[j].x + sortPoint[j].y;

                if (preSize > curSize)
                {
                    Point temp = sortPoint[i];
                    sortPoint[i] = sortPoint[j];
                    sortPoint[j] = temp;
                }
            }
        }

        //correct...
        sortPoint[1].x = sortPoint[3].x;
        sortPoint[1].y = sortPoint[0].y;

        sortPoint[2].x = sortPoint[0].x;
        sortPoint[2].y = sortPoint[3].y;

        for(int i = 0; i < sortPoint.length; i++)
        {
            Log.d(TAG, "post sortPoint["+i+"] : "+sortPoint[i]);
        }
    }

    static void sortTouchRectnResize(float []result, ArrayList<ColorBall> balls)
    {
        final Point[]tempPoints = new Point[4];

        for(int i = 0; i < balls.size(); i++)
        {
            tempPoints[i] = balls.get(i).getPoint();
        }


        for(int i = 0; i < tempPoints.length-1; i++)
        {
            for( int j = i+1; j < tempPoints.length; j++) {
                int preSize = tempPoints[i].x + tempPoints[i].y;
                int curSize = tempPoints[j].x + tempPoints[j].y;

                if (preSize > curSize)
                {
                    Point temp = tempPoints[i];
                    tempPoints[i] = tempPoints[j];
                    tempPoints[j] = temp;
                }
            }
        }

        for(int i = 0; i < tempPoints.length; i++)
        {
            Log.d(TAG, "tempPoints["+i+"] : "+tempPoints[i]);
        }

        Matrix transScaleMat = new Matrix();
        transScaleMat =
                ImageUtils.getTransformationMatrix(
                        1080, 1440,
                        180, 135,
                        270, false);

        RectF temp = new RectF(tempPoints[0].x, tempPoints[0].y, tempPoints[3].x, tempPoints[3].y);
        transScaleMat.mapRect(temp);

        result[0] = temp.left;
        result[1] = temp.top;
        result[2] = temp.right;
        result[3] = temp.bottom;
    }

    //need to clone
    public static boolean getPointTouched(float x, float y)
    {
        int centerPlusX = colorballs.get(2).getX() + colorballs.get(2).getWidthOfBall();
        int centerPlusY = colorballs.get(2).getY() + colorballs.get(2).getHeightOfBall();
        int centerMinuxX = colorballsClone.get(3).getX() + colorballsClone.get(3).getWidthOfBall();
        int centerMinuxY = colorballsClone.get(3).getY() + colorballsClone.get(3).getHeightOfBall();

        double radCirclePlus = Math
                .sqrt((double) (((centerPlusX - x) * (centerPlusX - x)) + (centerPlusY - y)
                        * (centerPlusY - y)));

        double radCircleMinus = Math
                .sqrt((double) (((centerMinuxX - x) * (centerMinuxX - x)) + (centerMinuxY - y)
                        * (centerMinuxY - y)));

//        if ( radCirclePlus < colorballs.get(2).getWidthOfBall() )
//        {
//            Log.d(TAG,"[getPointTouched] Plus True");
//            //mPlusButtonPressed = true;
//            //mMultiTouchOn = true;
//            return false;
//        }
//        else if( radCircleMinus < colorballsClone.get(3).getWidthOfBall() )
//        {
//            Log.d(TAG,"[getPointTouched] Minus True");
//            //mPlusButtonPressed = false;
//            //mMultiTouchOn = false;
//            //mMinusTouchOn = true;
//            return false;
//        }

        return true;
    }

        // events when touching the screen
    public static boolean touchEventFromParent(MotionEvent event) {

        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();

        Log.d(TAG, "touchEventFromParent : "+X+", "+Y+", eventaction : "+eventaction);

        mTouchProcessStartTime = new Date();

        switch (eventaction) {

        case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on

            Log.d("test-point2", "ACTION_DOWN");

            mTouchMoved = false;

            // a ball
            balID = -1;
            startMovePoint = new Point(X,Y);
            for (ColorBall ball : colorballs) {
                // check if inside the bounds of the ball (circle)
                // get the center for the ball
//                int centerX = ball.getX() + ball.getWidthOfBall();
//                int centerY = ball.getY() + ball.getHeightOfBall();
                int centerX = ball.getX();
                int centerY = ball.getY();
                //paint.setColor(Color.CYAN);
                // calculate the radius from the touch to the center of the ball
                double radCircle = Math
                        .sqrt((double) (((centerX - X) * (centerX - X)) + (centerY - Y)
                                * (centerY - Y)));

                Log.d("test-point2", "ACTION_DOWN - radCircle : "+radCircle+", getWidthOfBall : "+ball.getWidthOfBall());

                if (radCircle < ball.getWidthOfBall() * 2) {

                    balID = ball.getID();
                    if (balID == 1 || balID == 3) {
                        groupId = 2;
                        //canvas.drawRect(point1.x, point3.y, point3.x, point1.y,
                          //      paint);
                    } else {
                        groupId = 1;
                        //canvas.drawRect(point2.x, point4.y, point4.x, point2.y,
                          //      paint);
                    }
                    //invalidate();
                    break;
                }
                //invalidate();
            }

            /*
            if( balID == 2 )
            {
                MainActivity.setMultiTouch(true);
            }
            else if( balID == 3 )
            {
                mMultiTouchOn = false;
                MainActivity.setMultiTouch(false);
            }
            */

            Log.d("test-point2", "ACTION_DOWN - balID : "+balID);

            break;

        case MotionEvent.ACTION_MOVE: // touch drag with the ball

            Log.d("test-point2", "ACTION_MOVE");

            if( colorballs.get(0).getX() == -1 &&  colorballs.get(0).getY() == -1 )
            {
                return true;
            }

            mBoxTouched = true;
            mTouchMoved = true;

            // move the balls the same as the finger
            if (balID > -1) {
                colorballs.get(balID).setX(X);
                colorballs.get(balID).setY(Y);

                //paint.setColor(Color.CYAN);

                if (groupId == 1) {
                    colorballs.get(3).setX(colorballs.get(0).getX());
                    colorballs.get(3).setY(colorballs.get(2).getY());
                    colorballs.get(1).setX(colorballs.get(2).getX());
                    colorballs.get(1).setY(colorballs.get(0).getY());
                    //canvas.drawRect(point1.x, point3.y, point3.x, point1.y,
                      //      paint);
                } else {
                    colorballs.get(2).setX(colorballs.get(1).getX());
                    colorballs.get(2).setY(colorballs.get(3).getY());
                    colorballs.get(0).setX(colorballs.get(3).getX());
                    colorballs.get(0).setY(colorballs.get(1).getY());
                    //canvas.drawRect(point2.x, point4.y, point4.x, point2.y,
                      //      paint);
                }

                //check...
                int minSize = 4;
                if( balID == 0)
                {
                    if( colorballs.get(1).getX() - colorballs.get(0).getX() < 72 * minSize )
                    {
                        colorballs.get(0).setX(colorballs.get(1).getX() - 72 * minSize);
                        colorballs.get(3).setX(colorballs.get(1).getX() - 72 * minSize);
                    }

                    if( colorballs.get(3).getY() - colorballs.get(0).getY() < 72 * minSize )
                    {
                        colorballs.get(0).setY(colorballs.get(3).getY() - 72 * minSize);
                        colorballs.get(1).setY(colorballs.get(3).getY() - 72 * minSize);
                    }
                }
                else if(balID == 1)
                {
                    if( colorballs.get(1).getX() - colorballs.get(0).getX() < 72 * minSize )
                    {
                        colorballs.get(1).setX(colorballs.get(0).getX() + 72 * minSize);
                        colorballs.get(2).setX(colorballs.get(0).getX() + 72 * minSize);
                    }

                    if( colorballs.get(2).getY() - colorballs.get(1).getY() < 72 * minSize )
                    {
                        colorballs.get(0).setY(colorballs.get(2).getY() - 72 * minSize);
                        colorballs.get(1).setY(colorballs.get(2).getY() - 72 * minSize);
                    }
                }
                else if( balID == 2)
                {
                    if( colorballs.get(2).getX() - colorballs.get(3).getX() < 72 * minSize )
                    {
                        colorballs.get(2).setX(colorballs.get(3).getX() + 72 * minSize);
                        colorballs.get(1).setX(colorballs.get(3).getX() + 72 * minSize);
                    }

                    if( colorballs.get(2).getY() - colorballs.get(1).getY() < 72 * minSize)
                    {
                        colorballs.get(3).setY(colorballs.get(1).getY() + 72 * minSize);
                        colorballs.get(2).setY(colorballs.get(1).getY() + 72 * minSize);
                    }
                }
                else if(balID == 3)
                {
                    if( colorballs.get(2).getX() - colorballs.get(3).getX() < 72 * minSize )
                    {
                        colorballs.get(0).setX(colorballs.get(1).getX() - 72 * minSize);
                        colorballs.get(3).setX(colorballs.get(1).getX() - 72 * minSize);
                    }

                    if( colorballs.get(3).getY() - colorballs.get(0).getY() < 72 * minSize )
                    {
                        colorballs.get(3).setY(colorballs.get(0).getY() + 72 * minSize);
                        colorballs.get(2).setY(colorballs.get(0).getY() + 72 * minSize);
                    }
                }

                //invalidate();
            }else{
                if (startMovePoint!=null) {

                    if( colorballs.get(0).getX() < startMovePoint.x && startMovePoint.x < colorballs.get(1).getX()
                            && colorballs.get(0).getY() < startMovePoint.y && startMovePoint.y < colorballs.get(3).getY() ) {

                        //paint.setColor(Color.CYAN);
                        int diffX = X - startMovePoint.x;
                        int diffY = Y - startMovePoint.y;
                        startMovePoint.x = X;
                        startMovePoint.y = Y;

//                        colorballs.get(0).addX(diffX);
//                        colorballs.get(1).addX(diffX);
//                        colorballs.get(2).addX(diffX);
//                        colorballs.get(3).addX(diffX);
//                        colorballs.get(0).addY(diffY);
//                        colorballs.get(1).addY(diffY);
//                        colorballs.get(2).addY(diffY);
//                        colorballs.get(3).addY(diffY);
//                        //if(groupId==1)
//                        //  canvas.drawRect(point1.x, point3.y, point3.x, point1.y,
//                        //        paint);
//                        //else
//                        //  canvas.drawRect(point2.x, point4.y, point4.x, point2.y,
//                        //      paint);
//                        //invalidate();

                        if( 0 <= colorballs.get(0).getX() + diffX && colorballs.get(0).getX() + diffX <= 1080
                                && 0 <= colorballs.get(2).getX() + diffX && colorballs.get(2).getX() + diffX <= 1080 )
                        {

                            colorballs.get(0).addX(diffX);
                            colorballs.get(1).addX(diffX);
                            colorballs.get(2).addX(diffX);
                            colorballs.get(3).addX(diffX);
                        }

                        if( 0 <= colorballs.get(0).getY() + diffY && colorballs.get(0).getY() + diffY <= 1440
                                && 0 <= colorballs.get(2).getY() + diffY && colorballs.get(2).getY() + diffY <= 1440)
                        {
                            colorballs.get(0).addY(diffY);
                            colorballs.get(1).addY(diffY);
                            colorballs.get(2).addY(diffY);
                            colorballs.get(3).addY(diffY);
                        }

                    }
                }
            }

            break;

        case MotionEvent.ACTION_UP:
            Log.d("test-point2", "ACTION_UP");

            if( mTouchMoved == true )
            {
                //do something...
                Log.d("test-point2", "Do Ai Mode");

                float []rect = new float[4];
                sortTouchRectnResize(rect, colorballs);
                //Original Touch...
                jniController.updateObjRectForTouch(1, rect, false);

                cameraFragment.setAutoFocusOn(true);
            }
            else
            {
                if( balID == 2 )
                {
                    //MainActivity.setMultiTouch(true, false);
                    //MainActivity.setAutoFocusOn(true);

                    //On Multi box...here
//                    mMultiTouchOn = true;
//                    mPlusButtonPressed = true;
                }
                else if( balID == 3 )
                {
                    //mMultiTouchOn = false;
                    //MainActivity.setMultiTouch(true, true);
                    //MainActivity.setAutoFocusOn(true);
                }
            }

            // touch drop - just do things here after dropping

            break;
        }
        // redraw the canvas
        //invalidate();

        return true;
    }

    // events when touching the screen
    public static boolean touchEventFromParentClone(MotionEvent event) {

        if( mMultiTouchOn == true ) {

            int eventaction = event.getAction();

            int X = (int) event.getX();
            int Y = (int) event.getY();

            Log.d(TAG, "touchEventFromParentClone : " + X + ", " + Y + ", eventaction : " + eventaction);

            mTouchProcessStartTimeClone = new Date();

            switch (eventaction) {

                case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on

                    Log.d("test-pointClone", "ACTION_DOWN");

                    mTouchMovedClone = false;

                    // a ball
                    balIDClone = -1;
                    startMovePointClone = new Point(X, Y);
                    for (ColorBall ball : colorballsClone) {
                        // check if inside the bounds of the ball (circle)
                        // get the center for the ball
                        int centerX = ball.getX() + ball.getWidthOfBall();
                        int centerY = ball.getY() + ball.getHeightOfBall();
                        //paint.setColor(Color.CYAN);
                        // calculate the radius from the touch to the center of the ball
                        double radCircle = Math
                                .sqrt((double) (((centerX - X) * (centerX - X)) + (centerY - Y)
                                        * (centerY - Y)));

                        Log.d("test-pointClone", "ACTION_DOWN - radCircle : " + radCircle + ", getWidthOfBall : " + ball.getWidthOfBall());

                        if (radCircle < ball.getWidthOfBall()) {

                            balIDClone = ball.getID();
                            if (balIDClone == 1 || balIDClone == 3) {
                                groupIdClone = 2;
                                //canvas.drawRect(point1.x, point3.y, point3.x, point1.y,
                                //      paint);
                            } else {
                                groupIdClone = 1;
                                //canvas.drawRect(point2.x, point4.y, point4.x, point2.y,
                                //      paint);
                            }
                            //invalidate();
                            break;
                        }
                        //invalidate();
                    }

            /*
            if( balID == 2 )
            {
                MainActivity.setMultiTouch(true);
            }
            else if( balID == 3 )
            {
                mMultiTouchOn = false;
                MainActivity.setMultiTouch(false);
            }
            */

                    Log.d("test-pointClone", "ACTION_DOWN - balID : " + balID);

                    break;

                case MotionEvent.ACTION_MOVE: // touch drag with the ball

                    Log.d("test-pointClone", "ACTION_MOVE");

                    if (colorballsClone.get(0).getX() == -1 && colorballsClone.get(0).getY() == -1) {
                        return true;
                    }

                    //mBoxTouchedClone = true;
                    mTouchMovedClone = true;

                    // move the balls the same as the finger
                    if (balIDClone > -1) {
                        colorballsClone.get(balIDClone).setX(X);
                        colorballsClone.get(balIDClone).setY(Y);

                        //paint.setColor(Color.CYAN);

                        if (groupId == 1) {
                            colorballsClone.get(3).setX(colorballsClone.get(0).getX());
                            colorballsClone.get(3).setY(colorballsClone.get(2).getY());
                            colorballsClone.get(1).setX(colorballsClone.get(2).getX());
                            colorballsClone.get(1).setY(colorballsClone.get(0).getY());
                            //canvas.drawRect(point1.x, point3.y, point3.x, point1.y,
                            //      paint);
                        } else {
                            colorballsClone.get(2).setX(colorballsClone.get(1).getX());
                            colorballsClone.get(2).setY(colorballsClone.get(3).getY());
                            colorballsClone.get(0).setX(colorballsClone.get(3).getX());
                            colorballsClone.get(0).setY(colorballsClone.get(1).getY());
                            //canvas.drawRect(point2.x, point4.y, point4.x, point2.y,
                            //      paint);
                        }

                        //invalidate();
                    } else {
                        if (startMovePoint != null) {

                            if (colorballsClone.get(0).getX() < startMovePointClone.x && startMovePointClone.x < colorballsClone.get(1).getX()
                                    && colorballsClone.get(0).getY() < startMovePointClone.y && startMovePointClone.y < colorballsClone.get(3).getY()) {

                                //paint.setColor(Color.CYAN);
                                int diffX = X - startMovePointClone.x;
                                int diffY = Y - startMovePointClone.y;
                                startMovePointClone.x = X;
                                startMovePointClone.y = Y;
                                colorballsClone.get(0).addX(diffX);
                                colorballsClone.get(1).addX(diffX);
                                colorballsClone.get(2).addX(diffX);
                                colorballsClone.get(3).addX(diffX);
                                colorballsClone.get(0).addY(diffY);
                                colorballsClone.get(1).addY(diffY);
                                colorballsClone.get(2).addY(diffY);
                                colorballsClone.get(3).addY(diffY);
                                //if(groupId==1)
                                //  canvas.drawRect(point1.x, point3.y, point3.x, point1.y,
                                //        paint);
                                //else
                                //  canvas.drawRect(point2.x, point4.y, point4.x, point2.y,
                                //      paint);
                                //invalidate();
                            }
                        }
                    }

                    break;

                case MotionEvent.ACTION_UP:
                    Log.d("test-pointClone", "ACTION_UP");

                    if (mTouchMovedClone == true) {
                        //do something...
                        Log.d("test-pointClone", "Do Ai Mode");

                        cameraFragment.setAutoFocusOn(true);

                        float[] rect = new float[4];
                        sortTouchRectnResize(rect, colorballsClone);
                        //Clone Touch...
                        jniController.updateObjRectForTouch(1, rect, false);
                    } else {
                        if (balIDClone == 2) {
                            //MainActivity.setMultiTouch(true, false);
                            //MainActivity.setAutoFocusOn(true);
                        } else if (balIDClone == 3) {
                            //need to ???
                            //mMultiTouchOn = false;
                            //MainActivity.setMultiTouch(true, true);
                            //MainActivity.setAutoFocusOn(true);

                            mMultiTouchOn = false;
                            mMinusTouchOn = true;
                        }
                    }

                    // touch drop - just do things here after dropping

                    break;
            }
            // redraw the canvas
            //invalidate();
        }

        return true;
    }

    public void shade_region_between_points() {
        canvas.drawRect(point1.x, point3.y, point3.x, point1.y, paint);
    }
}
