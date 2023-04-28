package com.ispd.sfcam;

public class aiCamParameters {
    public static int LCD_WIDTH_I = 1080;
    public static int LCD_HEIGHT_I = 1440;
//    public static int LCD_WIDTH_I = 1080;
//    public static int LCD_HEIGHT_I = 1920;

    public static int PREVIEW_WIDTH_I = 1440;
    public static int PREVIEW_HEIGHT_I = 1080;
//    public static int PREVIEW_WIDTH_I = 1920;
//    public static int PREVIEW_HEIGHT_I = 1080;

    public static int SAVE_WIDTH_I = 3264;
    public static int SAVE_HEIGHT_I = 2448;

    public static int MOVIE_WIDTH_I = 1280;
    public static int MOVIE_HEIGHT_I = 960;

    //Feather 255 Mask Size & Feather Input Size...
    public static int RESIZE_BLUR_FEATHER_FACTOR = 4;
    public static int RESIZE_FEATHER_FACTOR = 8;

    //Cartoon Edge Resize & Cartoon Resize...
    public static float RESIZE_EDGE_FACTOR_F = 2.0f;
    public static float RESIZE_CARTOON_FACTOR_F = 4.0f;
    //End

    //Blur Resize & Blur Mask Resize...
    public static float RESIZE_BLUR_FACTOR_F = 2.0f;
    public static int RESIZE_BLUR_MASK_FACTOR = 8;
    //End

    public static float SAVE_RESIZE_FACTOR_F = 2.0f;

    public static boolean gUseEGLImageBool = true;

    public static boolean mOnFaceDetectionBool = true;

    public static int mCameraLocationInt = 0;

    public static boolean mCaptureRunning = false;
    public static boolean mMovieRunning = false;

    public static int SF_MODE = 0;
    public static int CARTOON_MODE = 1;
    public static int OF_MODE = 2;
    public static int HIGHLIGHT_MODE = 3;

    public static int mInterpolation = 0; //for resize with interpolation
}
