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
import android.graphics.Point;

import com.ispd.sfcam.utils.Log;

public class ColorBall {	private static String TAG = "ColorBall";

	Bitmap bitmap;
	Context mContext;
	Point point;
	Point savePoint;
	int id;
	static int count = 0;

	static int mLcdWidth = 1080;
	static int mLcdHeight = 1440;

	public ColorBall(Context context, int resourceId, Point point, int id) {
		this.id = id;
		bitmap = BitmapFactory.decodeResource(context.getResources(),
				resourceId);
		Log.d(TAG, "ColorBall Size-1 : "+bitmap.getWidth());
		//bitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() * 2.5f), (int)(bitmap.getHeight() * 2.5f), false);
		bitmap = Bitmap.createScaledBitmap(bitmap, (int)((float)bitmap.getWidth() * 1.5f), (int)((float)bitmap.getHeight() * 1.5f), false);
		mContext = context;
		this.point = point;

		savePoint = new Point();
	}

	public int getWidthOfBall() {
		return bitmap.getWidth();
	}

	public int getHeightOfBall() {
		Log.d(TAG, "ColorBall Size-2 : "+bitmap.getWidth());
		return bitmap.getHeight();
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public Point getPoint()
	{
		return point;
	}

	public int getX() {
		return point.x;
	}

	public int getY() {
		return point.y;
	}

	public int getID() {
		return id;
	}

	public void setPoint(int x, int y) {
		point.x = x;
		point.y = y;

//		if( point.x >  mLcdWidth)
//		{
//			point.x = mLcdWidth;
//		}
//		else if(point.x < 0 )
//		{
//			point.x = 0;
//		}
//
//		if( point.y >  mLcdHeight)
//		{
//			point.y = mLcdHeight;
//		}
//		else if(point.y < 0)
//		{
//			point.y = 0;
//		}

//		if( point.x >  mLcdWidth - bitmap.getWidth() )
//		{
//			//point.x = mLcdWidth - bitmap.getWidth();
//			point.x = mLcdWidth;
//		}
//		else if(point.x < 0 + bitmap.getWidth() )
//		{
//			point.x = 0;
//		}
//
//		if( point.y >  mLcdHeight - bitmap.getHeight() )
//		{
//			//point.y = mLcdHeight - bitmap.getHeight();
//			point.y = mLcdHeight;
//		}
//		else if(point.y < 0 + bitmap.getHeight() )
//		{
//			point.y = 0;
//		}
	}

	public void setX(int x) {
		point.x = x;

		if( point.x >  mLcdWidth )
		{
			point.x = mLcdWidth;
		}
		else if(point.x < 0)
		{
			point.x = 0;
		}

//		if( point.x >  mLcdWidth - bitmap.getWidth() )
//		{
//			//point.x = mLcdWidth - bitmap.getWidth();
//			point.x = mLcdWidth;
//		}
//		else if(point.x < 0 + bitmap.getWidth() )
//		{
//			point.x = 0;
//		}
	}

	public void setY(int y) {
		point.y = y;

		if( point.y >  mLcdHeight)
		{
			point.y = mLcdHeight;
		}
		else if(point.y < 0 )
		{
			point.y = 0;
		}

//		if( point.y >  mLcdHeight - bitmap.getHeight() )
//		{
//			//point.y = mLcdHeight - bitmap.getHeight();
//			point.y = mLcdHeight;
//		}
//		else if(point.y < 0 + bitmap.getHeight() )
//		{
//			point.y = 0;
//		}
	}

	public void addY(int y){
		point.y = point.y + y;

		if( point.y >  mLcdHeight )
		{
			point.y = mLcdHeight;
		}
		else if( point.y < 0 )
		{
			point.y = 0;
		}

//		Log.d(TAG, "["+id+"] [add-point] pre point.y : "+point.y+", addy : "+y);
//
//		if( point.y >  mLcdHeight - bitmap.getHeight() )
//		{
//			Log.d(TAG, "["+id+"] [add-point] maxY");
//			point.y = mLcdHeight - bitmap.getHeight();
//		}
//		else if( y < 0 && point.y < 0 + bitmap.getHeight() )
//		{
//			Log.d(TAG, "["+id+"] [add-point] minY");
//			point.y = 0;
//		}
//
//		Log.d(TAG, "["+id+"] [add-point] post point.y : "+point.y);
	}

	public void addX(int x){
		point.x = point.x + x;

		if( point.x >  mLcdWidth )
		{
			point.x = mLcdWidth;
		}
		else if( point.x < 0 )
		{
			point.x = 0;
		}

//		Log.d(TAG, "["+id+"] [add-point-x] pre point.x : "+point.x+", addx : "+x);
//
//		if( point.x >  mLcdWidth - bitmap.getWidth() )
//		{
//			Log.d(TAG, "["+id+"] [add-point-x] maxX");
//			point.x = mLcdWidth - bitmap.getWidth();
//		}
//		else if( x < 0 && point.x < 0 + bitmap.getWidth() )
//		{
//			Log.d(TAG, "["+id+"] [add-point-x] minX");
//			point.x = 0;
//		}
//
//		Log.d(TAG, "["+id+"] [add-point-x] post point.x : "+point.x);
	}

	//private int checkBoxSize()

	public Point getSavePoint()
	{
		return savePoint;
	}

	public void save()
	{
		savePoint.set(point.x, point.y);
	}

	public void restore()
	{
		point.set(savePoint.x, savePoint.y);
	}
}
