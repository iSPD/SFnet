package com.ispd.sfcam;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.ispd.sfcam.pdEngine.glEngineGL;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BitmapStorage {
    private static final String TAG = "BitmapStorage";
    private static ArrayList mBitmapList = null;
    private Context mContext = null;
    private static Bitmap mCustomPicture = null;
    private static String mCustomVideoPath = null;

    public BitmapStorage(Context context) {
        mContext = context;
        mBitmapList = new ArrayList();
        for( int i = 0; i < 16; i++ ) {
            addBitmap("images/" + (i + 1) + ".jpg");
        }
        for( int i = 0; i < 50; i++) {
            addBitmap("movie/"+(i+1)+".jpg");
        }
    }

    public ArrayList GetBitmapList() {
        if(mBitmapList != null) {
            return mBitmapList;
        }
        else {
            return null;
        }
    }

    public void Release() {
        for( int i = 0; i < 16; i++ ) {
            ((Bitmap)mBitmapList.get(i)).recycle();
        }
        for( int i = 16; i < 50 + 16; i++) {
            ((Bitmap)mBitmapList.get(i)).recycle();
        }
        mBitmapList.clear();
        mBitmapList = null;

        if(mCustomPicture != null)
            mCustomPicture.recycle();
    }

    private Bitmap decodeImage(String path) {
        AssetManager assetManager = mContext.getAssets();
        InputStream is = null;
        try {
            is = assetManager.open(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BitmapFactory.decodeStream(is);
    }

    private void addBitmap(String path) {
        Bitmap image;

        image = decodeImage(path);

        if (image != null) {
            mBitmapList.add(image);
        }
    }


    public void SetCustomBitmap(Bitmap bitmap) {
        if(bitmap == null) {
            Log.d(TAG, "custom bitmap is null");
            return;
        }

        if(mCustomPicture != null) {
            mCustomPicture.recycle();
            Log.d(TAG, "old custom bitmap is recycled");
        }

        mCustomPicture = bitmap;
    }

    public Bitmap GetCustomBitmap() {
        return mCustomPicture;
    }

    public void SetCustomVideoPath(String path) {
        if(path != null) {
            mCustomVideoPath = path;
        }
    }

    public String GetCustomVideoPath() {
        return mCustomVideoPath;
    }

}
