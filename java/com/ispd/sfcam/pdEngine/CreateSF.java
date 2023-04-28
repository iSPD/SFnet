package com.ispd.sfcam.pdEngine;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.SFTunner;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static com.ispd.sfcam.pdEngine.glEngineGL.setProgram;

public class CreateSF {
//    private static Context mContext = null;
    private static int mGLProgramSF = -1;
    private static FloatBuffer mGLVertexBufferSF = null, mGLTexCoordBufferSF = null;
    private static FloatBuffer mMVPMatrixBuffer90;
    private static int mMovingIndex = -1;
    private static int mDebugOn = 0;

    public CreateSF() {  }

    public void Init() {

        mGLProgramSF = setProgram(SOURCE_DRAW_VS_SF, SOURCE_DRAW_FS_SF);

        float[] vtmp = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
        float[] ttmp = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

        mGLVertexBufferSF = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLVertexBufferSF.put(vtmp);
        mGLVertexBufferSF.position(0);
        mGLTexCoordBufferSF = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTexCoordBufferSF.put(ttmp);
        mGLTexCoordBufferSF.position(0);

        float[] mModelMatrix90 = new float[16];
        float[] mRotationMatrix90 = new float[16];
        float[] mMVPMatrix90 = new float[16];

        Matrix.setIdentityM(mModelMatrix90, 0);
        Matrix.setRotateM(mRotationMatrix90, 0, 90, 0, 0, -1.0f);
        Matrix.multiplyMM(mMVPMatrix90, 0, mModelMatrix90, 0, mRotationMatrix90, 0);

        mMVPMatrixBuffer90 = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrixBuffer90.put(mMVPMatrix90);
        mMVPMatrixBuffer90.position(0);
    }

    public void Release() {
        if ( mGLProgramSF   > 0 ) GLES20.glDeleteProgram(mGLProgramSF);
        mGLProgramSF  = -1;

        mGLVertexBufferSF = null;
        mGLTexCoordBufferSF = null;
    }

    public static void SetMovingIndex(int index)
    {
        mMovingIndex = index;
    }

    public static void SetDebugOn(int onoff)
    {
        mDebugOn = onoff;
    }

    public static void RenderToTexture(int fbo, int textures, int maskTexture, int backTexture, int gammaTexture, boolean useObj, int front, boolean movieOn)
    {
        if ( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        }

        GLES20.glUseProgram (mGLProgramSF);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        if( fbo > 0 )
        {
            if( movieOn == true )
            {
                GLES20.glViewport(0, 0, aiCamParameters.MOVIE_WIDTH_I, aiCamParameters.MOVIE_HEIGHT_I);
            }
            else {
                GLES20.glViewport(0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
            }
        }
        else
        {
            GLES20.glViewport(0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
        }

        int ph = GLES20.glGetAttribLocation(mGLProgramSF, "vPosition");
        int tch = GLES20.glGetAttribLocation(mGLProgramSF, "vTexCoord");

        GLES20.glVertexAttribPointer(ph, 2, GL_FLOAT, false, 4 * 2, mGLVertexBufferSF);
        GLES20.glVertexAttribPointer(tch, 2, GL_FLOAT, false, 4 * 2, mGLTexCoordBufferSF);
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgramSF, "uMVPMatrix"), 1, false, mMVPMatrixBuffer90);

        GLES20.glActiveTexture(GL_TEXTURE0);
        //GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures);
        GLES20.glBindTexture(GL_TEXTURE_2D, textures);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramSF, "sTexture"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GL_TEXTURE_2D, maskTexture);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramSF, "sMaskTexture"), 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GL_TEXTURE_2D, backTexture);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramSF, "sBackTexture"), 2);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GL_TEXTURE_2D, gammaTexture);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramSF, "sGammaTexture"), 3);

//        Log.d("sf-moving", "mMovingIndex : "+mMovingIndex);
//        Log.d("sf-moving", "SFTunner.mSuperFastFeatherTune.mColor : "+SFTunner.mSuperFastFeatherTune.mColor);
//        Log.d("sf-moving", "SFTunner.mSuperFastFeatherTune.mColorStart : "+SFTunner.mSuperFastFeatherTune.mColorStart);
//        Log.d("sf-moving", "SFTunner.mFastFeatherTune.mColor : "+SFTunner.mFastFeatherTune.mColor);
//        Log.d("sf-moving", "SFTunner.mFastFeatherTune.mColorStart : "+SFTunner.mFastFeatherTune.mColorStart);
//        Log.d("sf-moving", "SFTunner.mSlowFeatherTune.mColor : "+SFTunner.mSlowFeatherTune.mColor);
//        Log.d("sf-moving", "SFTunner.mSlowFeatherTune.mColorStart : "+SFTunner.mSlowFeatherTune.mColorStart);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramSF, "uMovingIndex"), mMovingIndex);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramSF, "uSuperFastColor"), SFTunner.mSuperFastFeatherTune.mColor);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramSF, "uSuperFastColorStart"), SFTunner.mSuperFastFeatherTune.mColorStart);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramSF, "uFastColor"), SFTunner.mFastFeatherTune.mColor);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramSF, "uFastColorStart"), SFTunner.mFastFeatherTune.mColorStart);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramSF, "uSlowColor"), SFTunner.mSlowFeatherTune.mColor);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramSF, "uSlowColorStart"), SFTunner.mSlowFeatherTune.mColorStart);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramSF, "uFront"), front);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramSF, "uUseObj"), useObj ? 1 : 0);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramSF, "uDebugOn"), mDebugOn);

        int mSaveStatus = 0;
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramSF, "uSaveStatus"), mSaveStatus);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(ph);
        GLES20.glDisableVertexAttribArray(tch);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture (GL_TEXTURE_2D, 0);

        GLES20.glUseProgram(0);

        if ( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

    //    public static final String SOURCE_DRAW_VS_ADT = "" +
//            "attribute vec2 vPosition;\n" +
//            "attribute vec2 vTexCoord;\n" +
//            "varying vec2 texCoord;\n" +
//
//            "uniform mat4 uMVPMatrix;\n" +
//
//            "void main() {\n" +
//            "  texCoord = vTexCoord;\n" +
//            "  gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
//            "}";
//
//    public static final String SOURCE_DRAW_FS_ADT = "" +
//            "#extension GL_OES_EGL_image_external : require\n" +
//            //"precision mediump float;\n" +
//            "precision highp float;\n" +
//
//            "uniform samplerExternalOES sTexture;\n" +
//            "uniform sampler2D sIntegralTexture;\n" +
//
//            "varying vec2 texCoord;\n" +
//            "uniform int uFront;\n" +
//            "uniform int uSaveStatus;\n" +
//
//            "uniform int uCartoonMode;\n" +
//
//            "int blockSize = 3;\n" +
//            "float blurWeight = 10.0;\n" +
////            "float threshold = 10.0 * (1.0 / 255.0);\n" +
//            "float threshold = 15.0 * (1.0 / 255.0);\n" +
//
//            "float xStep = 1.0 / 720.0;\n" +
//            "float yStep = 1.0 / 540.0;\n" +
//
//            "vec4 calcBlur(vec2 tex_coord, int kSize) {\n" +
//            "   float width = xStep;\n" +
//            "   float height = yStep;\n" +
//            "   int maskSize = kSize;\n" +
//            "   vec4 sum = vec4(0.0, 0.0, 0.0, 0.0);\n" +
//            "   int i, j;\n" +
//            "   #pragma unroll\n" +
//            "   for( i = -maskSize/2; i <= maskSize/2; i++) {\n" +
//            "   #pragma unroll\n" +
//            "       for( j = -maskSize/2; j <= maskSize/2; j++) {\n" +
//            "           sum += texture2D(sTexture, vec2(tex_coord.x + float(i)*width*blurWeight, tex_coord.y + float(j)*height*blurWeight));\n" +
//            "       }\n" +
//            "   }\n" +
//            "   return sum/float(maskSize*maskSize);\n" +
//            "}\n" +
//
//            "void main() {\n" +
//
//            "   if( uCartoonMode == 0 ) {\n" +
//            "       threshold = 10.0 * (1.0 / 255.0);\n" +
//            "   }\n" +
//            "   else {\n" +
//            "       threshold = 20.0 * (1.0 / 255.0);\n" +
//            "   }\n" +
//
//            "   vec2 newTexCoord = texCoord;\n" +
//            "   if( uFront == 1 ) {\n" +
//            "       newTexCoord.x = 1.0 - newTexCoord.x;\n" +
//            "   }\n" +
//
//            "	if(uSaveStatus == 1) {\n" +
//            "   	gl_FragColor = texture2D(sTexture, newTexCoord) * 0.5;\n" +
//            "	}\n" +
//            "	else {\n" +
//            "   	vec4 filterd = calcBlur(newTexCoord, blockSize);\n" +
//            "   	float filterdGray = 0.299 * filterd.r + 0.587 * filterd.g + 0.114 * filterd.b;\n" +
//            "   	filterdGray = filterdGray - threshold;\n" +
//
//            "   	vec4 image = texture2D(sTexture, newTexCoord);\n" +
//            "      float imageGray = 0.299 * image.r + 0.587 * image.g + 0.114 * image.b;\n" +
//
//            "      if( imageGray >= filterdGray ) {\n" +
//            "   	    gl_FragColor = vec4(1.0, 1.0, 1.0, 0.0);\n" +
//            "      }\n" +
//            "      else {\n" +
//            "   	    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);\n" +
//            "      }\n" +
//            "	}\n" +
//            "}";
//
//    public static final String SOURCE_DRAW_VS_CARTOON = "" +
//            "attribute vec2 vPosition;\n" +
//            "attribute vec2 vTexCoord;\n" +
//            "varying vec2 texCoord;\n" +
//
//            "uniform mat4 uMVPMatrix;\n" +
//
//            "void main() {\n" +
//            "  texCoord = vTexCoord;\n" +
//            "  gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
//            "}";
//
//    public static final String SOURCE_DRAW_FS_CARTOON = "" +
//            "#extension GL_OES_EGL_image_external : require\n" +
//            //"precision mediump float;\n" +
//            "precision highp float;\n" +
//
//            //"uniform samplerExternalOES sTextureInit;\n" +
//            "uniform sampler2D sTextureInit;\n" +
//            "uniform sampler2D sTextureFbo;\n" +
//            "uniform sampler2D sMaskTexture;\n" +
//            "uniform sampler2D sEdgeTexture;\n" +
//
//            "varying vec2 texCoord;\n" +
//
//            "uniform int uFront;\n" +
//            "uniform int uSaveStatus;\n" +
//            "uniform vec2 uSamplerSteps;\n" +
//            "uniform int uInitStage;\n" +
//            "uniform int uCartoonMode;\n" +
//            "uniform int uUseObj;\n" +
//            "uniform int uDebugMode;\n" +
//
//            "const int GAUSSIAN_SAMPLES = 9;\n"+
//            //"float blurFactors[9] = float[GAUSSIAN_SAMPLES](0.05, 0.09, 0.12, 0.15, 0.18, 0.15, 0.12, 0.09, 0.05);\n" +
//            "float blurFactors[9];\n" +
//            "float distanceNormalizationFactor = 3.5;\n" +
//            "float blurSamplerScale = 10.0 / 4.0;\n" +
//
//            "const int samplerRadius = 4;\n" +
//
//            "float xStep = 1.0 / 720.0;\n" +
//            "float yStep = 1.0 / 540.0;\n" +
//
//            "float random(vec2 seed)\n" +
//            "{\n" +
//            "	return fract(sin(dot(seed ,vec2(12.9898,78.233))) * 43758.5453);\n" +
//            "}\n" +
//
//            "vec4 calcBlur(vec2 tex_coord, int kSize) {\n" +
//            "   float width = xStep;\n" +
//            "   float height = yStep;\n" +
//            "   int maskSize = kSize;\n" +
//            "   vec4 sum = vec4(0.0, 0.0, 0.0, 0.0);\n" +
//            "   int i, j;\n" +
//            "   #pragma unroll\n" +
//            "   for( i = -maskSize/2; i <= maskSize/2; i++) {\n" +
//            "   #pragma unroll\n" +
//            "       for( j = -maskSize/2; j <= maskSize/2; j++) {\n" +
//            "           sum += texture2D(sTextureFbo, vec2(tex_coord.x + float(i)*width*1.0, tex_coord.y + float(j)*height*1.0));\n" +
//            "       }\n" +
//            "   }\n" +
//            "   return sum/float(maskSize*maskSize);\n" +
//            "}\n" +
//
//            "void main() {\n" +
//
//            "   blurFactors[0] = 0.05;\n" +
//            "   blurFactors[1] = 0.09;\n" +
//            "   blurFactors[2] = 0.12;\n" +
//            "   blurFactors[3] = 0.15;\n" +
//            "   blurFactors[4] = 0.18;\n" +
//            "   blurFactors[5] = 0.15;\n" +
//            "   blurFactors[6] = 0.12;\n" +
//            "   blurFactors[7] = 0.09;\n" +
//            "   blurFactors[8] = 0.05;\n" +
//
//            "   if( uCartoonMode == 0 ) {\n" +
//            //"       blurSamplerScale = 60.0 / 4.0;\n" +
//            "       blurSamplerScale = 70.0 / 4.0;\n" +
//            "   }\n" +
//            "   else {\n" +
//            //"       blurSamplerScale = 10.0 / 4.0;\n" +
//            //"       blurSamplerScale = 20.0 / 4.0;\n" +
//            "       blurSamplerScale = 15.0 / 4.0;\n" +
//            "   }\n" +
//
//            "   vec2 newTexCoord = texCoord;\n" +
//            "   vec2 frontTexCoord = texCoord;\n" +
//            "   if( uFront == 1 ) {\n" +
//            "       if( uInitStage == 0 ) {\n" +
//            "           newTexCoord.x = 1.0 - newTexCoord.x;\n" +
//            "       }\n" +
//            "       else if( uInitStage == 1 ) {\n" +
//            "       }\n" +
//            "       else if( uInitStage == 2 ) {\n" +
//            "           frontTexCoord.x = 1.0 - frontTexCoord.x;\n" +
//            "       }\n" +
//            "   }\n" +
//
//            "	if(uSaveStatus == 1) {\n" +
//            "       gl_FragColor = texture2D(sTextureInit, newTexCoord) * 0.5;\n" +
//            "	}\n" +
//            "  else if( uInitStage == 2 ) {\n" +
//            "       vec4 person_mask = texture2D(sMaskTexture, newTexCoord);\n" +
//
//            "       vec2 coordy2 = vec2(0.0, 0.0);\n" +
//            "       coordy2.x = newTexCoord.x;\n" +
//            "       coordy2.y = 1.0 - newTexCoord.y;\n" +
//            "       vec4 edgy_mask = texture2D(sEdgeTexture, coordy2);\n" +
//
//            "       vec4 front = texture2D(sTextureInit, frontTexCoord);\n" +
//            "       vec4 cartoon = texture2D(sTextureFbo, newTexCoord);\n" +
//
//            "       if( uCartoonMode == 0 ) {\n" +
//            "           if(edgy_mask.b < 0.8) {\n"+
//            "               cartoon.rgb *= 0.0;\n" +
//            "           }\n" +
//            "       }\n" +
//            "       else {\n" +
//            "           if(edgy_mask.b < 0.8) {\n"+
//            "               cartoon.rgb *= 0.0;\n" +
//            "           }\n" +
//            "       }\n" +
//
//            "       float personMask;\n" +
//            "       if( uUseObj == 1 ) {\n" +
//            "           personMask = person_mask.g;\n" +
//            "       }\n" +
//            "       else {\n" +
//            "           personMask = person_mask.b;\n" +
//            "       }\n" +
//
//            "       if( uCartoonMode == 0 ) {\n" +
//            "		    gl_FragColor = (front * personMask) + (cartoon * (1.0 - personMask) );\n" +
//            //"		    gl_FragColor = front * 0.5 + person_mask * 0.5;\n" +
//            //"		    gl_FragColor = front * 0.5 + vec4(person_mask.r*3.0, person_mask.g, person_mask.b, 1.0) * 0.5;\n" +
//            //"		    gl_FragColor = person_mask;\n" +
//            //"		    if( 0.1 < person_mask.g && person_mask.g < 50.0 / 255.0 ) {\n" +
////            "		    if( 0.1 < person_mask.g ) {\n" +
////            "		        gl_FragColor = front;\n" +
////            "		    }\n" +
////            "		    else {\n" +
////            "		        gl_FragColor = cartoon;\n" +
////            "		    }\n" +
//            "       }\n" +
//            "       else {\n" +
//            "		    gl_FragColor = (cartoon * personMask) + (front * (1.0 - personMask) );\n" +
//            "       }\n" +
//
//            "       if( uDebugMode == 1 ) {\n" +
//            "           	gl_FragColor = vec4(person_mask.b, person_mask.b, person_mask.b, 1.0);\n" +
//            "       }\n" +
//            "	}\n" +
//            "	else {\n" +//uInitStage == 0 or 1
//            "	     vec4 centralColor;\n" +
//            "       if( uInitStage == 0 ) {\n" +
//            "	        centralColor = texture2D(sTextureInit, newTexCoord);\n" +
//            "       }\n" +
//            "       else {\n" +
//            "   	    centralColor = texture2D(sTextureFbo, newTexCoord);\n" +
//            "       }\n" +
//
//            "	    float gaussianWeightTotal = blurFactors[4];\n" +
//            "	    vec4 sum = centralColor * blurFactors[4];\n" +
//            "	    vec2 stepScale = blurSamplerScale * uSamplerSteps;\n" +
//            "	    float offset = random(newTexCoord) - 0.5;\n" +
//
//            "      for(int i = 0; i < samplerRadius; ++i)\n" +
//            "	    {\n" +
//            "		    vec2 dis = (float(i) + offset) * stepScale;\n" +
//            "          float blurfactor = blurFactors[samplerRadius-i];\n" +
//
//            "	        vec4 sampleColor1;\n" +
//            "          if( uInitStage == 0 ) {\n" +
//            "	            sampleColor1 = texture2D(sTextureInit, newTexCoord + dis);\n" +
//            "           }\n" +
//            "           else {\n" +
//            "   	        sampleColor1 = texture2D(sTextureFbo, newTexCoord + dis);\n" +
//            "           }\n" +
//
//            "			  float distanceFromCentralColor1 = min(distance(centralColor, sampleColor1) * distanceNormalizationFactor, 1.0);\n" +
//            "           float gaussianWeight1 = blurfactor * (1.0 - distanceFromCentralColor1);\n" +
//            "			  gaussianWeightTotal += gaussianWeight1;\n" +
//            "			  sum += sampleColor1 * gaussianWeight1;\n" +
//
//            "	         vec4 sampleColor2;\n" +
//            "           if( uInitStage == 0 ) {\n" +
//            "	            sampleColor2 = texture2D(sTextureInit, newTexCoord - dis);\n" +
//            "           }\n" +
//            "           else {\n" +
//            "   	        sampleColor2 = texture2D(sTextureFbo, newTexCoord - dis);\n" +
//            "           }\n" +
//            "			  float distanceFromCentralColor2 = min(distance(centralColor, sampleColor2) * distanceNormalizationFactor, 1.0);\n" +
//            "           float gaussianWeight2 = blurfactor * (1.0 - distanceFromCentralColor2);\n" +
//            "			  gaussianWeightTotal += gaussianWeight2;\n" +
//            "			  sum += sampleColor2 * gaussianWeight2;\n" +
//            "	    }\n" +//for
//
//            "	    gl_FragColor = sum / gaussianWeightTotal;\n" +
//            "   }\n" +//else
//            "}";//main

    public static final String SOURCE_DRAW_VS_SF = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +

            "uniform mat4 uMVPMatrix;\n" +

            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_SF = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            //"precision mediump float;\n" +
            "precision highp float;\n" +

            //"uniform samplerExternalOES sTexture;\n" +
            "uniform sampler2D sTexture;\n" +
            "uniform sampler2D sMaskTexture;\n" +
            "uniform sampler2D sBackTexture;\n" +
            "uniform sampler2D sGammaTexture;\n" +

            "varying vec2 texCoord;\n" +
            "uniform int uMovingIndex;\n" +

            "uniform float uSuperFastColor;\n" +
            "uniform float uSuperFastColorStart;\n" +
            "uniform float uFastColor;\n" +
            "uniform float uFastColorStart;\n" +
            "uniform float uSlowColor;\n" +
            "uniform float uSlowColorStart;\n" +

            "uniform int uFront;\n" +
            "uniform int uUseObj;\n" +
            "uniform int uSaveStatus;\n" +
            "uniform int uDebugOn;\n" +

//            "vec4 mul_color_matrix(vec4 c) {\n" +
//            "   vec4 color_new;\n" +
//            "   color_new.r = c.r * uColorMatrix[0] + c.g * uColorMatrix[1] + c.b * uColorMatrix[2] + c.a * uColorMatrix[3] + uColorMatrix[4];\n" +
//            "   color_new.g = c.r * uColorMatrix[5] + c.g * uColorMatrix[6] + c.b * uColorMatrix[7] + c.a * uColorMatrix[8] + uColorMatrix[9];\n" +
//            "   color_new.b = c.r * uColorMatrix[10] + c.g * uColorMatrix[11] + c.b * uColorMatrix[12] + c.a * uColorMatrix[13] + uColorMatrix[14];\n" +
//            "   color_new.a = c.r * uColorMatrix[15] + c.g * uColorMatrix[16] + c.b * uColorMatrix[17] + c.a * uColorMatrix[18] + uColorMatrix[19];\n" +
//
////            "   color_new = c * uColorMatrix[0];\n" +
////            "   color_new.r = c.r * uColorMatrix[0];\n" +
////            "   color_new.g = c.g * uColorMatrix[6];\n" +
////            "   color_new.b = c.b * uColorMatrix[12];\n" +
////            "   color_new.a = c.a + uColorMatrix[18];\n" +
//            "   return color_new;\n" +
//            "}" +

//            "float calcu_black_rate(vec2 txcoord) {\n" +
////            "   float R_start = 0.15;\n" +
////            "   float R_end = 0.95;;\n" +
//
//            "   float R_start = uBlackParams[0];\n" +
//            "   float R_end = uBlackParams[1];\n" +
//
//            "   float black_rate = 1.0;\n" +
//            "   float fx = txcoord.x - 0.5;\n" +
//            "   float fy = txcoord.y - 0.5;\n" +
//            //"   fy *= 16.0 / 9.0;\n" +
//            "   float R_norm = sqrt(fx*fx+fy*fy);\n" +
//            "   if ( R_norm < R_start ) black_rate = 1.0;\n" +
//            "   else if ( R_norm > R_end ) black_rate = 0.0;\n" +
//            "   else {\n" +
//            // should modify the function.. -> smoothly
//            "       black_rate = cos( ((R_norm - R_start) / (R_end - R_start)) * 1.570796 );\n" +
//            "   }\n" +
//            "   return black_rate;\n" +
//            "}" +

            "vec4 black_edge_effect(vec2 coord, vec4 color) {\n" +
            "      vec4 effect;\n" +
//            "      float bk_rate = calcu_black_rate(coord);\n" +
            "      float bk_rate = 1.0;\n" +
//            "      effect.r = texture2D(sGammaTexture, vec2(color.r * 255.0 / 1440.0, 0.0)).r * bk_rate;\n" +
//            "      effect.g = texture2D(sGammaTexture, vec2(color.g * 255.0 / 1440.0, 0.0)).g * bk_rate;\n" +
//            "      effect.b = texture2D(sGammaTexture, vec2(color.b * 255.0 / 1440.0, 0.0)).b * bk_rate;\n" +
            "      effect.r = texture2D(sGammaTexture, vec2(color.r, 0.0)).r * bk_rate;\n" +
            "      effect.g = texture2D(sGammaTexture, vec2(color.g, 0.0)).g * bk_rate;\n" +
            "      effect.b = texture2D(sGammaTexture, vec2(color.b, 0.0)).b * bk_rate;\n" +
            "      effect.a = color.a;\n" +
            "      return effect;\n" +
            "}\n" +

            "void main() {\n" +
            "  vec2 newTexCoord = texCoord;\n" +
            "  vec2 tex_coord = texCoord;\n" +

            "   if( uFront == 1 ) {\n" +
            "       newTexCoord.x = 1.0 - newTexCoord.x;\n" +
            "       tex_coord.x = 1.0 - texCoord.y;\n" +
            "       tex_coord.y = texCoord.x;\n" +
            "   }\n" +
            "   else {\n" +
            "       tex_coord.x = 1.0 - texCoord.y;\n" +
            "       tex_coord.y = texCoord.x;\n" +
            "   }\n" +

            "	if(uSaveStatus == 1) {\n" +
            "   	gl_FragColor = texture2D(sTexture, newTexCoord) * 0.5;\n" +
            "	}\n" +
            "	else {\n" +
            "      vec4 front = texture2D(sTexture, newTexCoord);\n" +
            "      vec4 mask = texture2D(sMaskTexture, texCoord);\n" +
            "      vec4 back = texture2D(sBackTexture, tex_coord);\n" +

//            "		if( uMovingIndex == 1 ) {\n" +
//            "           front = black_edge_effect(newTexCoord, front);\n" +
//            "		}\n" +

            //Move to Beauty mode...
//            "      front = black_edge_effect(newTexCoord, front);\n" +

            //"		gl_FragColor = texture2D(sGammaTexture, texCoord);\n" +
            //"		gl_FragColor = texture2D(sGammaTexture, vec2(0.0, float(i)/255.0));\n" +
            //"		gl_FragColor = texture2D(sGammaTexture, vec2(1.0/1440.0), 0.0);\n" +
            //"		gl_FragColor = mask;\n" +

            //"		return;\n" +

            "		 vec4 black;\n" +
            "		 float colorStart = 0.0;\n" +

            "		 if( uMovingIndex == 3 ) {\n" +
            "		    black = vec4(uSuperFastColor, uSuperFastColor, uSuperFastColor, 1.0);\n" +
            "		    colorStart = uSuperFastColorStart;\n" +
            "       }\n" +
            "		 else if( uMovingIndex == 2 ) {\n" +
            "		    black = vec4(uFastColor, uFastColor, uFastColor, 1.0);\n" +
            "		    colorStart = uFastColorStart;\n" +
            "       }\n" +
            "		 else if( uMovingIndex == 1 ) {\n" +
            "		    black = vec4(uSlowColor, uSlowColor, uSlowColor, 1.0);\n" +
            "		    colorStart = uSlowColorStart;\n" +
            "       }\n" +

            "		 vec4 feather = vec4(1.0, 1.0, 1.0, 1.0);\n" +

//            "      if( mask.b <= 0.2 ) {\n" +
//            "		    feather = (front * 0.2) + (back * 0.8);\n" +
//            "      }\n" +
//            "      else {\n" +
//            "		    feather = (front * mask.b) + (back * (1.0 - mask.b) );\n" +
//            "      }\n" +

            "      float maskColor;\n" +
            "      if( uUseObj == 1 ) {\n" +
            //"          back = vec4(0.5, 0.5, 0.5, 1.0);\n" +
            "          maskColor = mask.g;\n" +
            "		    feather = (front * maskColor) + (back * (1.0 - maskColor) );\n" +
            "      }\n" +
            "      else {\n" +
            "          maskColor = mask.b;\n" +
            "		    feather = (front * maskColor) + (back * (1.0 - maskColor) );\n" +
            "      }\n" +

            "      if( 0.0 < maskColor && maskColor < 1.0 ) {\n" +
//            "		    float rate = mask.b * 0.2;\n" +
//            "		    gl_FragColor = (feather * (1.0 - rate) ) + (black * rate );\n" +

            "           if( maskColor <= 0.5 ) {\n" +
            "		        float rate = maskColor * colorStart / 0.5;\n" +
            "		        gl_FragColor = (feather * (1.0 - rate) ) + (black * rate );\n" +
            "           }\n" +
            "           else {\n" +
            "		        float rate = 1.0 - maskColor;\n" +
            "		        rate = rate * colorStart / 0.5;\n" +
            "		        gl_FragColor = (feather * (1.0 - rate) ) + (black * rate );\n" +
            "           }\n" +
            "      }\n" +
            "      else {\n" +
            "		    gl_FragColor = feather;\n" +
            "      }\n" +

//            "		    gl_FragColor = feather;\n" +

            "      if( uDebugOn == 1 ){\n" +
//            "           if( mask.b < 0.9999 ){\n" +
//            "		        gl_FragColor = vec4(mask.b, 0.0, 0.0, 1.0) * 0.5 + front * 0.5;\n" +
//            "           }\n" +
            //"		        gl_FragColor = vec4(mask.r*3.0, mask.g, 0.0, 1.0) * 0.5 + front * 0.5;\n" +
            //"		        gl_FragColor = vec4(maskColor, 0.0, 0.0, 1.0);\n" +
            //"		        gl_FragColor = vec4(mask.r * 5.0, 0.0, mask.g * 5.0, 1.0) * 0.5 + front * 0.5;\n" +
            "		        gl_FragColor = vec4(0.0, mask.b, 0.0, 1.0) * 0.5 + front * 0.5;\n" +
            "      }\n" +

            "	}\n" +
            "}";

}
