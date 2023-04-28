package com.ispd.sfcam.utils;

/**
 * Created by 경훈 on 2016-03-17.
 */

public class Log {

    static public void d(String tag, String msgFormat, Object... args) {
        android.util.Log.d(tag, String.format(msgFormat, args));
    }

    static public void e(String tag, String msgFormat, Object... args) {
        android.util.Log.e(tag, String.format(msgFormat, args));
    }

    static public void i(String tag, String msgFormat, Object... args) {
        android.util.Log.i(tag, String.format(msgFormat, args));
    }

    static public void w(String tag, String msgFormat, Object... args) {
        //android.util.Log.w(tag, String.format(msgFormat, args));
    }

    static public void v(String tag, String msgFormat, Object... args) {
        //android.util.Log.v(tag, String.format(msgFormat, args));
    }

    static public void k(String tag, String msgFormat, Object... args) {
        android.util.Log.d(tag, String.format(msgFormat, args));
    }

    static public void o(String tag, String msgFormat, Object... args) {
        android.util.Log.d(tag, String.format(msgFormat, args));
    }
}
