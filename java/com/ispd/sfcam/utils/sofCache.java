package com.ispd.sfcam.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by khkim on 2018-05-15.
 */

public class sofCache {

    private static String TAG = "sofCache";
    static Context mContext;

    public sofCache(Context ctx)
    {
        mContext = ctx;
    }

    static public File getCacheDir(Context context) {
        File cacheDir = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cacheDir = new File(Environment.getExternalStorageDirectory(), "cachefolder");
            if(!cacheDir.isDirectory()) {
                cacheDir.mkdirs();
            }
        }
        if(!cacheDir.isDirectory()) {
            cacheDir = context.getCacheDir();
        }

        Log.d(TAG, "getCacheDir cacheDir : "+cacheDir);

        return cacheDir;
    }

    static public void Write(String obj) throws IOException {
        File cacheDir = getCacheDir(mContext);
        File cacheFile = new File(cacheDir, "Cache.txt");

        Log.d(TAG, "Write cacheFile : "+cacheFile);
        Log.d(TAG, "Write text : "+obj);

        if(!cacheFile.exists())cacheFile.createNewFile();
        FileWriter fileWriter = new FileWriter(cacheFile);
        fileWriter.write(obj);
        fileWriter.flush();
        fileWriter.close();
    }

    static public String Read() throws IOException {
        File cacheDir = getCacheDir(mContext);
        File cacheFile = new File(cacheDir, "Cache.txt");

        Log.d(TAG, "Read cacheFile : "+cacheFile);

        if(!cacheFile.exists())cacheFile.createNewFile();
        FileInputStream inputStream = new FileInputStream(cacheFile);
        Scanner s = new Scanner(inputStream);
        String text="";
        while(s.hasNext()){
            text+=s.nextLine();
        }
        inputStream.close();

        Log.d(TAG, "Read text : "+text);
        return text;
    }
}
