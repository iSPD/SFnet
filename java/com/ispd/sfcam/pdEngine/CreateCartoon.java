package com.ispd.sfcam.pdEngine;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES31;
import android.opengl.Matrix;
import android.util.Log;

import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.utils.SFTunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.ispd.sfcam.pdEngine.glEngineGL.setProgram;

public class CreateCartoon {
    public static final String SOURCE_DRAW_VS_CARTOON = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +

            "uniform mat4 uMVPMatrix;\n" +

            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_CARTOON = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            //"precision mediump float;\n" +
            "precision highp float;\n" +

            "uniform samplerExternalOES sTextureInitOes;\n" +
            "uniform sampler2D sTextureInitNm;\n" +
            "uniform sampler2D sTextureFbo;\n" +
            "uniform sampler2D sMaskTexture;\n" +
            "uniform sampler2D sEdgeTexture;\n" +
            "uniform sampler2D sBackgroundTexture;\n" +

            "varying vec2 texCoord;\n" +

            "uniform int uUseOes;\n" +
            "uniform int uFront;\n" +
            "uniform int uSaveStatus;\n" +
            "uniform vec2 uSamplerSteps;\n" +
            "uniform int uInitStage;\n" +
            "uniform int uCartoonMode;\n" +
            "uniform float uTexRate;\n" +
            "uniform float uColorScale;\n" +
            "uniform int uUseObj;\n" +
            "uniform int uDebugMode;\n" +

            "const int GAUSSIAN_SAMPLES = 9;\n" +
            //"float blurFactors[9] = float[GAUSSIAN_SAMPLES](0.05, 0.09, 0.12, 0.15, 0.18, 0.15, 0.12, 0.09, 0.05);\n" +
            "float blurFactors[9];\n" +
            "float distanceNormalizationFactor = 3.5;\n" +
            "float blurSamplerScale = 10.0 / 4.0;\n" +

            "const int samplerRadius = 4;\n" +

            "float random(vec2 seed)\n" +
            "{\n" +
            "	return fract(sin(dot(seed ,vec2(12.9898,78.233))) * 43758.5453);\n" +
            "}\n" +

            "void main() {\n" +

            "   blurFactors[0] = 0.05;\n" +
            "   blurFactors[1] = 0.09;\n" +
            "   blurFactors[2] = 0.12;\n" +
            "   blurFactors[3] = 0.15;\n" +
            "   blurFactors[4] = 0.18;\n" +
            "   blurFactors[5] = 0.15;\n" +
            "   blurFactors[6] = 0.12;\n" +
            "   blurFactors[7] = 0.09;\n" +
            "   blurFactors[8] = 0.05;\n" +

            "   if( uCartoonMode == 0 ) {\n" +
//            "       blurSamplerScale = 60.0 / 4.0;\n" +
            "       blurSamplerScale = uColorScale / 4.0;\n" + //70
            "   }\n" +
            "   else {\n" +
//            "       blurSamplerScale = 15.0 / 4.0;\n" +
            "       blurSamplerScale = uColorScale / 4.0;\n" + //60
            "   }\n" +

            "   vec2 newTexCoord = texCoord;\n" +
            "   vec2 frontTexCoord = texCoord;\n" +
            "   if( uFront == 1 ) {\n" +
            "       if( uInitStage == 0 ) {\n" +
            "           newTexCoord.x = 1.0 - newTexCoord.x;\n" +
            "       }\n" +
            "       else if( uInitStage == 1 ) {\n" +
            "       }\n" +
            "       else if( uInitStage == 2 ) {\n" +
            "           frontTexCoord.x = 1.0 - frontTexCoord.x;\n" +
            "       }\n" +
            "   }\n" +

            "	if(uSaveStatus == 1) {\n" +
            "       if( uUseOes == 1 ) {\n" +
            "           gl_FragColor = texture2D(sTextureInitOes, newTexCoord) * 0.5;\n" +
            "       }\n" +
            "       else {\n" +
            "           gl_FragColor = texture2D(sTextureInitNm, newTexCoord) * 0.5;\n" +
            "       }\n" +
            "	}\n" +
            "  else if( uInitStage == 2 ) {\n" +
            "       vec4 person_mask = texture2D(sMaskTexture, newTexCoord);\n" +

            "       vec2 coordy2 = vec2(0.0, 0.0);\n" +
            "       coordy2.x = newTexCoord.x;\n" +
            "       coordy2.y = 1.0 - newTexCoord.y;\n" +
            "       vec4 edgy_mask = texture2D(sEdgeTexture, coordy2);\n" +

            "       vec4 front;\n" +
            "       if( uUseOes == 1 ) {\n" +
            "           front = texture2D(sTextureInitOes, frontTexCoord);\n" +
            "       }\n" +
            "       else {\n" +
            "           front = texture2D(sTextureInitNm, frontTexCoord);\n" +
            "       }\n" +
            "       vec4 cartoon = texture2D(sTextureFbo, newTexCoord);\n" +
            "       vec4 background = texture2D(sBackgroundTexture, frontTexCoord);\n" +
            "       vec4 cartoonAndTexture;\n" +
            "       if( uCartoonMode == 0 ) {\n" +
            "           if(edgy_mask.b < 0.8) {\n" +
            "               cartoon.rgb *= 0.0;\n" +
            "           }else {\n" +
//            "               cartoon = vec4(mix(cartoon.rgb, edgy_mask.rgb, uCartoonIntensity), 1.0);\n" +
            "               cartoonAndTexture = mix(cartoon, background, uTexRate);\n" + //tuning point 0.5
            "           }\n" +
            "       }\n" +
            "       else {\n" +
            "           if(edgy_mask.b < 0.8) {\n" +
            "               cartoon.rgb *= 0.0;\n" +
            "           }else {\n" +
//            "               cartoon = vec4(mix(cartoon.rgb, edgy_mask.rgb, uCartoonIntensity), 1.0);\n" +
            "               cartoonAndTexture = mix(cartoon, background, uTexRate);\n" + //tuning point 0.3
            "           }\n" +
            "       }\n" +

            "       float personMask;\n" +
            "       if( uUseObj == 1 ) {\n" +
            "           personMask = person_mask.g;\n" +
            "       }\n" +
            "       else {\n" +
            "           personMask = person_mask.b;\n" +
            "       }\n" +

            "       if( uCartoonMode == 0 ) {\n" +
            //"		    gl_FragColor = (front * personMask) + (cartoonAndTexture * (1.0 - personMask) );\n" +
            "          gl_FragColor = mix(cartoonAndTexture, front, personMask);\n" +
            "       }\n" +
            "       else {\n" +
            //"		    gl_FragColor = (cartoonAndTexture * personMask) + (front * (1.0 - personMask) );\n" +
            "          gl_FragColor = mix(front, cartoonAndTexture, personMask);\n" +
            //"		    gl_FragColor = (edgy_mask * personMask) + (front * (1.0 - personMask) );\n" +
            "       }\n" +

            "       if( uDebugMode == 1 ) {\n" +
            //"           	gl_FragColor = vec4(person_mask.b, person_mask.b, person_mask.b, 1.0) * 0.5 + front * 0.5;\n" +
            //"           	gl_FragColor = vec4(edgy_mask.b, 0.0, 0.0, 1.0) * 0.5 + front * 0.5;\n" +
            //"           	gl_FragColor = edgy_mask;\n" +
            "           	gl_FragColor = vec4(0.0, personMask, 0.0, 1.0) * 0.5 + front * 0.5;\n" +
            "       }\n" +

            "	}\n" +
            "	else {\n" +//uInitStage == 0 or 1
            "	     vec4 centralColor;\n" +
            "       if( uInitStage == 0 ) {\n" +
            "           if( uUseOes == 1 ) {\n" +
            "	            centralColor = texture2D(sTextureInitOes, newTexCoord);\n" +
            "           }\n" +
            "           else {\n" +
            "	            centralColor = texture2D(sTextureInitNm, newTexCoord);\n" +
            "           }\n" +
            "       }\n" +
            "       else {\n" +
            "   	    centralColor = texture2D(sTextureFbo, newTexCoord);\n" +
            "       }\n" +

            "	    float gaussianWeightTotal = blurFactors[4];\n" +
            "	    vec4 sum = centralColor * blurFactors[4];\n" +
            "	    vec2 stepScale = blurSamplerScale * uSamplerSteps;\n" +
            "	    float offset = random(newTexCoord) - 0.5;\n" +

            "      for(int i = 0; i < samplerRadius; ++i)\n" +
            "	    {\n" +
            "		    vec2 dis = (float(i) + offset) * stepScale;\n" +
            "          float blurfactor = blurFactors[samplerRadius-i];\n" +

            "	        vec4 sampleColor1;\n" +
            "          if( uInitStage == 0 ) {\n" +
            "               if( uUseOes == 1 ) {\n" +
            "	                sampleColor1 = texture2D(sTextureInitOes, newTexCoord + dis);\n" +
            "               }\n" +
            "               else {\n" +
            "	                sampleColor1 = texture2D(sTextureInitNm, newTexCoord + dis);\n" +
            "               }\n" +
            "           }\n" +
            "           else {\n" +
            "   	        sampleColor1 = texture2D(sTextureFbo, newTexCoord + dis);\n" +
            "           }\n" +

            "			  float distanceFromCentralColor1 = min(distance(centralColor, sampleColor1) * distanceNormalizationFactor, 1.0);\n" +
            "           float gaussianWeight1 = blurfactor * (1.0 - distanceFromCentralColor1);\n" +
            "			  gaussianWeightTotal += gaussianWeight1;\n" +
            "			  sum += sampleColor1 * gaussianWeight1;\n" +

            "	         vec4 sampleColor2;\n" +
            "           if( uInitStage == 0 ) {\n" +
            "               if( uUseOes == 1 ) {\n" +
            "	                sampleColor2 = texture2D(sTextureInitOes, newTexCoord - dis);\n" +
            "               }\n" +
            "               else {\n" +
            "	                sampleColor2 = texture2D(sTextureInitNm, newTexCoord - dis);\n" +
            "               }\n" +
            "           }\n" +
            "           else {\n" +
            "   	        sampleColor2 = texture2D(sTextureFbo, newTexCoord - dis);\n" +
            "           }\n" +
            "			  float distanceFromCentralColor2 = min(distance(centralColor, sampleColor2) * distanceNormalizationFactor, 1.0);\n" +
            "           float gaussianWeight2 = blurfactor * (1.0 - distanceFromCentralColor2);\n" +
            "			  gaussianWeightTotal += gaussianWeight2;\n" +
            "			  sum += sampleColor2 * gaussianWeight2;\n" +
            "	    }\n" +//for

            "	    gl_FragColor = sum / gaussianWeightTotal;\n" +
            "   }\n" +//else
            "}";//main

    private static Context mContext = null;
    private static int mCartoonProgram = -1;
    private static FloatBuffer mMVPMatrixBuffer90;
    private static FloatBuffer mGLVertexBufferCartoon = null, mGLTexCoordBufferCartoon = null;
    private static int mOffScreenFrameBuffer = -1;
    private static int mOffScreenRenderBuffer = -1;

    private static int mCartoonOption = 0;
    private static float mCartoonColorScale = 60.f; //more cartoony
    private static float mTexRate = 0.0f; //0.0~1.0

    public static final float[] vertices = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
    public static final float[] texcoords = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

    private static int mBackgroundTextureIdFromImage[] = {-1, -1, -1};
    private static int mCurrentBGIndex = 0;
    public static final int mNumOfBG = 3;

    public CreateCartoon (Context context) {
        mContext = context;
    }

    public static boolean Init() {
        mCartoonProgram = setProgram(SOURCE_DRAW_VS_CARTOON, SOURCE_DRAW_FS_CARTOON);
        if (mCartoonProgram != -1)
            Log.e("CreateCartoon", "CreateCartoon init program SUCCESS!!!...");
        else Log.e("CreateCartoon", "CreateCartoon init program failed...");

        mGLVertexBufferCartoon = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLVertexBufferCartoon.put(vertices);
        mGLVertexBufferCartoon.position(0);
        mGLTexCoordBufferCartoon = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTexCoordBufferCartoon.put(texcoords);
        mGLTexCoordBufferCartoon.position(0);

        float []mModelMatrix90 = new float[16];
        float []mRotationMatrix90 = new float[16];
        float []mMVPMatrix90 = new float[16];

        Matrix.setIdentityM(mModelMatrix90, 0);
        Matrix.setRotateM(mRotationMatrix90, 0, 90, 0, 0, -1.0f);
        Matrix.multiplyMM(mMVPMatrix90, 0, mModelMatrix90, 0, mRotationMatrix90, 0);

        mMVPMatrixBuffer90 = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrixBuffer90.put(mMVPMatrix90);
        mMVPMatrixBuffer90.position(0);

        String files[] = {"bg2", "bg3", "bg5"};
        AssetManager assetManager = mContext.getAssets();
        for(int i = 0; i< mNumOfBG; i++) {
            InputStream is = null;
            try {
                is = assetManager.open("images/"+files[i]+".jpg");
            } catch (IOException e) {
                e.printStackTrace();
            }
            Bitmap bmp = BitmapFactory.decodeStream(is);
            mBackgroundTextureIdFromImage[i] = glEngineGL.createTextureBitmap(bmp.getWidth(), bmp.getHeight(), bmp);
        }

        return true;
    }

    public static void Release() {
        if (mCartoonProgram != 0) {
            GLES31.glDeleteProgram(mCartoonProgram);
            mCartoonProgram = 0;
        }
        if (mBackgroundTextureIdFromImage[0] > 0) {
            GLES31.glDeleteTextures(mNumOfBG, mBackgroundTextureIdFromImage, 0);
        }
        mGLVertexBufferCartoon = null;
        mGLTexCoordBufferCartoon = null;
        mMVPMatrixBuffer90 = null;
    }

    public static void RenderToTexture(int fbo, boolean useOes, int textureInitOes, int textureInitNm, int texturesFbo, int textureEdge, int textureMask, int front, int initStage, boolean useObj, boolean movieOn, int debugOn)
    {
        if ( fbo > 0 ) {
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fbo);
        }
        GLES31.glUseProgram (mCartoonProgram);

        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT);

        if( fbo > 0 )
        {
            if( movieOn == true ) {
                GLES31.glViewport(0, 0, aiCamParameters.MOVIE_WIDTH_I, aiCamParameters.MOVIE_HEIGHT_I);
            }
            else {
                GLES31.glViewport(0, 0, (int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_CARTOON_FACTOR_F), (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_CARTOON_FACTOR_F));
            }
        }
        else {
            GLES31.glViewport(0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
        }

        int ph = GLES31.glGetAttribLocation(mCartoonProgram, "vPosition");
        int tch = GLES31.glGetAttribLocation(mCartoonProgram, "vTexCoord");

        GLES31.glVertexAttribPointer(ph, 2, GLES31.GL_FLOAT, false, 4 * 2, mGLVertexBufferCartoon);
        GLES31.glVertexAttribPointer(tch, 2, GLES31.GL_FLOAT, false, 4 * 2, mGLTexCoordBufferCartoon);
        GLES31.glEnableVertexAttribArray(ph);
        GLES31.glEnableVertexAttribArray(tch);

        //rotation
        if( initStage == 0 )
        {
            GLES31.glUniformMatrix4fv(GLES31.glGetUniformLocation(mCartoonProgram, "uMVPMatrix"), 1, false, mMVPMatrixBuffer90);
        }
        else
        {
            GLES31.glUniformMatrix4fv(GLES31.glGetUniformLocation(mCartoonProgram, "uMVPMatrix"), 1, false, mMVPMatrixBuffer90);
        }

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureInitOes);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "sTextureInitOes"), 0);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE1);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureInitNm);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "sTextureInitNm"), 1);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE2);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texturesFbo);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "sTextureFbo"), 2);

//        GLES31.glActiveTexture(GLES31.GL_TEXTURE2);
//        jniController.useEGLImage(0);
//        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "sMaskTexture"), 2);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE3);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureMask);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "sMaskTexture"), 3);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE4);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureEdge);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "sEdgeTexture"), 4);

        if( useObj == true )
        {
            mCartoonOption = 0;
            mCurrentBGIndex = 2;
        }

        if( mCartoonOption == 0 )
        {
            mCurrentBGIndex = 2;
            mCartoonColorScale = SFTunner.sfCommonTune.mCartoonBackSat;
            mTexRate = SFTunner.sfCommonTune.mCartoonBackTexRate;
        }
        else
        {
            mCurrentBGIndex = 1;
            mCartoonColorScale = SFTunner.sfCommonTune.mCartoonFrontSat;
            mTexRate = SFTunner.sfCommonTune.mCartoonFrontTexRate;
        }

        GLES31.glActiveTexture(GLES31.GL_TEXTURE5);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mBackgroundTextureIdFromImage[mCurrentBGIndex]);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "sBackgroundTexture"), 5);


        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "uUseOes"), useOes ? 1 : 0);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "uFront"), front);
        int mSaveStatus = 0;
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "uSaveStatus"), mSaveStatus);

        GLES31.glUniform2f(GLES31.glGetUniformLocation(mCartoonProgram, "uSamplerSteps"), 1.0f/ ((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_CARTOON_FACTOR_F),
                1.0f/ ((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_CARTOON_FACTOR_F));

        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "uInitStage"), initStage);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "uCartoonMode"), mCartoonOption);

        GLES31.glUniform1f(GLES31.glGetUniformLocation(mCartoonProgram, "uColorScale"), mCartoonColorScale); //sally : TUNING-POINT
        GLES31.glUniform1f(GLES31.glGetUniformLocation(mCartoonProgram, "uTexRate"), mTexRate);  //sally : TUNING-POINT
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "uDebugMode"), debugOn);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mCartoonProgram, "uUseObj"), useObj ? 1 : 0);

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4);

        GLES31.glDisableVertexAttribArray(ph);
        GLES31.glDisableVertexAttribArray(tch);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE1);
        GLES31.glActiveTexture(GLES31.GL_TEXTURE2);
        GLES31.glActiveTexture(GLES31.GL_TEXTURE3);
        GLES31.glActiveTexture(GLES31.GL_TEXTURE4);
        GLES31.glActiveTexture(GLES31.GL_TEXTURE5);
        GLES31.glBindTexture (GLES31.GL_TEXTURE_2D, 0);

        GLES31.glUseProgram(0);

        if ( fbo > 0 ) {
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0);
        }
    }

    public void SetCartoonOption(int opt) {
        mCartoonOption = opt;
    }

    public static void SetCurrentBG(int index) {
        mCurrentBGIndex = index;
    }
    /*  too slow
    public static final String SOURCE_DRAW_FS_CARTOON = "" +

        "#extension GL_OES_EGL_image_external : require\n" +
        //"precision mediump float;\n" +
        "precision highp float;\n" +

        "uniform samplerExternalOES sTextureInit;\n" +
        "uniform sampler2D sTextureFbo;\n" +
        "uniform sampler2D sMaskTexture;\n" +
        "uniform sampler2D sEdgeTexture;\n" +

        "varying vec2 texCoord;\n" +

        "uniform int uFront;\n" +
        "uniform int uSaveStatus;\n" +
        "uniform vec2 uSamplerSteps;\n" +
        "uniform int uInitStage;\n" +
        "uniform int uCartoonMode;\n" +

        "const int GAUSSIAN_SAMPLES = 9;\n" +
        //"float blurFactors[9] = float[GAUSSIAN_SAMPLES](0.05, 0.09, 0.12, 0.15, 0.18, 0.15, 0.12, 0.09, 0.05);\n" +
        "float blurFactors[9];\n" +
        "float distanceNormalizationFactor = 3.5;\n" +  //sally 3.5
        "float blurSamplerScale = 4.0 / 4.0;\n" + //sally 10.0

        "const int samplerRadius = 21;\n" +
        "const float arg = 0.5;\n" +

        "float random(vec2 seed)\n" +
        "{\n" +
        "	return fract(sin(dot(seed ,vec2(12.9898,78.233))) * 43758.5453);\n" +
        "}\n" +

        "void main() {\n" +
        "   if( uCartoonMode == 0 ) {\n" +
        "       blurSamplerScale = 60.0 / 4.0;\n" +
        "   }\n" +
        "   else {\n" +
        "       blurSamplerScale = 21.0;\n" + //15.0 / 4.0;\n" +  //sally
        "   }\n" +

        "   vec2 newTexCoord = texCoord;\n" +
        "   if( uFront == 1 ) {\n" +
        "       if( uInitStage == 0 ) {\n" +
        "           newTexCoord.x = 1.0 - newTexCoord.x;\n" +
        "       }\n" +
        "       else if( uInitStage == 1 ) {\n" +
        "       }\n" +
        "       else if( uInitStage == 2 ) {\n" +
        "       }\n" +
        "   }\n" +

        "	if(uSaveStatus == 1) {\n" +
        "       gl_FragColor = texture2D(sTextureInit, newTexCoord) * 0.5;\n" +
        "	}\n" +
        "  else if( uInitStage == 2 ) {\n" +
        "       vec4 person_mask = texture2D(sMaskTexture, newTexCoord);\n" +

        "       vec2 coordy2 = vec2(0.0, 0.0);\n" +
        "       coordy2.x = newTexCoord.x;\n" +
        "       coordy2.y = 1.0 - newTexCoord.y;\n" +
        "       vec4 edgy_mask = texture2D(sEdgeTexture, coordy2);\n" +

        "       vec2 coordy1 = vec2(0.0, 0.0);\n" +
        "       coordy1.x = 1.0 - newTexCoord.x;\n" +
        "       coordy1.y = newTexCoord.y;\n" +
        "       vec4 front = texture2D(sTextureInit, coordy1);\n" +
        "       vec4 cartoon = texture2D(sTextureFbo, newTexCoord);\n" +

        "       if( uCartoonMode == 0 ) {\n" +
        "           if(edgy_mask.b < 0.8) {\n" +
        "               cartoon.rgb *= 0.0;\n" +
        "           }\n" +
        "       }\n" +
        "       else {\n" +
        "           if(edgy_mask.b < 0.8) {\n" +
        "               cartoon.rgb *= 0.0;\n" +
        "           }\n" +
        "       }\n" +

        "       if( uCartoonMode == 0 ) {\n" +
        //"		    gl_FragColor = (front * person_mask.r) + (cartoon * (1.0 - person_mask.r) );\n" +
        //"		    gl_FragColor = front * 0.5 + person_mask * 0.5;\n" +
        "		    gl_FragColor = front * 0.5 + vec4(person_mask.r*3.0, person_mask.g, person_mask.b, 1.0) * 0.5;\n" +
        //"		    gl_FragColor = person_mask;\n" +
        "       }\n" +
        "       else {\n" +
        "		    gl_FragColor = (cartoon * person_mask.b) + (front * (1.0 - person_mask.b) );\n" +
        "       }\n" +
        "	}\n" +
        "	else {\n" +//uInitStage == 0 or 1
        "	     vec4 centralColor;\n" +
        "       if( uInitStage == 0 ) {\n" +
        "	        centralColor = texture2D(sTextureInit, newTexCoord);\n" +
        "       }\n" +
        "       else {\n" +
        "   	    centralColor = texture2D(sTextureFbo, newTexCoord);\n" +
        "       }\n" +

        "       float lum = dot(centralColor.rgb, vec3(0.299, 0.587, 0.114));\n" + //sally
        "       float factor = (1.0 + arg) / (arg + lum) * distanceNormalizationFactor;\n" + //sally

        "	    float gaussianWeightTotal = 1.0;\n" +
        "	    vec4 sum = centralColor * gaussianWeightTotal;\n" +
        "	    vec2 stepScale = blurSamplerScale * uSamplerSteps/ float(samplerRadius);\n" +
        "	    float offset = random(newTexCoord) - 0.5;\n" +

        "      for(int i = 1; i <= samplerRadius; ++i)\n" +
        "	    {\n" +
        "		    vec2 dis = (float(i) + offset) * stepScale;\n" +
//        "          float blurfactor = blurFactors[samplerRadius-i];\n" +
        "          float percent = 1.0 - (float(i) + offset) / float(samplerRadius);\n" +

        "	        vec4 sampleColor1;\n" +
        "          if( uInitStage == 0 ) {\n" +
        "	            sampleColor1 = texture2D(sTextureInit, newTexCoord + dis);\n" +
        "           }\n" +
        "           else {\n" +
        "   	        sampleColor1 = texture2D(sTextureFbo, newTexCoord + dis);\n" +
        "           }\n" +

//        "			  float distanceFromCentralColor1 = min(distance(centralColor, sampleColor1) * distanceNormalizationFactor, 1.0);\n" +
        "           float distanceFromCentralColor1 = min(distance(centralColor, sampleColor1) * factor, 1.0);\n" +
//        "           float gaussianWeight1 = blurfactor * (1.0 - distanceFromCentralColor1);\n" +
        "           float gaussianWeight1 = percent * (1.0 - distanceFromCentralColor1);\n" +

        "			  gaussianWeightTotal += gaussianWeight1;\n" +
        "			  sum += sampleColor1 * gaussianWeight1;\n" +

        "	         vec4 sampleColor2;\n" +
        "           if( uInitStage == 0 ) {\n" +
        "	            sampleColor2 = texture2D(sTextureInit, newTexCoord - dis);\n" +
        "           }\n" +
        "           else {\n" +
        "   	        sampleColor2 = texture2D(sTextureFbo, newTexCoord - dis);\n" +
        "           }\n" +
//        "			  float distanceFromCentralColor2 = min(distance(centralColor, sampleColor2) * distanceNormalizationFactor, 1.0);\n" +
        "           float distanceFromCentralColor2 = min(distance(centralColor, sampleColor2) * factor, 1.0);\n" +
//        "           float gaussianWeight2 = blurfactor * (1.0 - distanceFromCentralColor2);\n" +
        "           float gaussianWeight2 = percent * (1.0 - distanceFromCentralColor2);\n" +

        "			  gaussianWeightTotal += gaussianWeight2;\n" +
        "			  sum += sampleColor2 * gaussianWeight2;\n" +
        "	    }\n" +//for
        "	    gl_FragColor = sum / gaussianWeightTotal;\n" +
        "   }\n" +//else
        "}";//main
//*/
}
