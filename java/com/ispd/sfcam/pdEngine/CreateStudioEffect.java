package com.ispd.sfcam.pdEngine;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.jniController;
import com.ispd.sfcam.utils.Log;
import com.ispd.sfcam.utils.SFTunner2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;


public class CreateStudioEffect {

    private static String TAG = "CreateStudioEffect";

    private static int mGLProgramStudio = -1;
    private static FloatBuffer mGLVertexStudio = null, mGLTexCoordStudio = null;
    private static FloatBuffer mMVPMatrixBufferStudio;

    private static ByteBuffer mPixelBufferForStudio;

    private static float []gStudioData = {0.0f, 0.0f, 0.0f, 0.0f};
    private static int mDebugOn = 0;

    public static int initStudioMode(int width, int height) {

        GLES20.glViewport(0, 0, width, height);

        Log.d(TAG, "viewport width : "+width+" height : "+height);

        if ( mGLProgramStudio == -1 ) {

            int vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
            GLES20.glShaderSource(vshader, SOURCE_DRAW_STUDIO_VS);
            GLES20.glCompileShader(vshader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile vshader");
                Log.e(TAG, "Could not compile vshader:" + GLES20.glGetShaderInfoLog(vshader));
                GLES20.glDeleteShader(vshader);
                vshader = 0;
                return 0;
            }

            int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            GLES20.glShaderSource(fshader, SOURCE_DRAW_STUDIO_FS);
            GLES20.glCompileShader(fshader);
            GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile fshader");
                Log.e(TAG, "Could not compile fshader:" + GLES20.glGetShaderInfoLog(fshader));
                GLES20.glDeleteShader(fshader);
                fshader = 0;
                return 0;
            }

            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vshader);
            GLES20.glAttachShader(program, fshader);
            GLES20.glLinkProgram(program);

            mGLProgramStudio = program;

            float[] vtmp = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
            float[] ttmp = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

            mGLVertexStudio = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mGLVertexStudio.put(vtmp);
            mGLVertexStudio.position(0);
            mGLTexCoordStudio = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mGLTexCoordStudio.put(ttmp);
            mGLTexCoordStudio.position(0);

            //rotation
            float []mModelMatrix = new float[16];
            float []mRotationMatrix = new float[16];
            float []mMVPMatrix = new float[16];

            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.setRotateM(mRotationMatrix, 0, 90, 0, 0, -1.0f);
            Matrix.multiplyMM(mMVPMatrix, 0, mModelMatrix, 0, mRotationMatrix, 0);

            mMVPMatrixBufferStudio = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mMVPMatrixBufferStudio.put(mMVPMatrix);
            mMVPMatrixBufferStudio.position(0);
        }

        byte[] arrStudio = new byte[aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * 4];
        Arrays.fill(arrStudio, (byte)0xff);

        mPixelBufferForStudio = ByteBuffer.allocateDirect(aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * 4).order(ByteOrder.nativeOrder());
        mPixelBufferForStudio.put(arrStudio, 0, aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR * 4);
        mPixelBufferForStudio.position(0);

        return mGLProgramStudio;
    }

    public static void release()
    {
        if ( mGLProgramStudio   > 0 ) GLES20.glDeleteProgram(mGLProgramStudio  );
        mGLProgramStudio  = -1;
    }

    public static void copyMaskDataForStudio(byte[] image)
    {
        int width = aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR;
        int height = aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR;

        if( mPixelBufferForStudio != null ) {
            mPixelBufferForStudio.put(image, 0, width * height * 4);
            mPixelBufferForStudio.position(0);
        }
    }

    public static void updateMaskTextureForStudio(int []texID) {
        //synchronized (mSyncObject) {

        int width = aiCamParameters.PREVIEW_WIDTH_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR;
        int height = aiCamParameters.PREVIEW_HEIGHT_I / aiCamParameters.RESIZE_BLUR_MASK_FACTOR;

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GL_TEXTURE_2D, texID[0]);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, mPixelBufferForStudio);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);
        //}
    }

    public static void drawStudio(int fbo, boolean useOes, int texture_id1, int texture_id2, int texture_id3, int texute_id4, int texute_id5, int front, int face, boolean isMovie) {

        if ( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, fbo);
            if( isMovie == true )
            {
                GLES20.glViewport(0, 0, aiCamParameters.MOVIE_WIDTH_I, aiCamParameters.MOVIE_HEIGHT_I);
            }
        }
        else {
            GLES20.glViewport(0, 0, aiCamParameters.PREVIEW_WIDTH_I, aiCamParameters.PREVIEW_HEIGHT_I);
        }

        GLES20.glUseProgram(mGLProgramStudio);

        int ph = GLES20.glGetAttribLocation(mGLProgramStudio, "vPosition");
        int tch = GLES20.glGetAttribLocation(mGLProgramStudio, "vTexCoord");

        GLES20.glVertexAttribPointer(ph, 2, GL_FLOAT, false, 4 * 2, mGLVertexStudio);
        GLES20.glVertexAttribPointer(tch, 2, GL_FLOAT, false, 4 * 2, mGLTexCoordStudio);
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);

        //rotation
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgramStudio, "uMVPMatrix"), 1, false, mMVPMatrixBufferStudio);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture_id1);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramStudio, "sTextureOriOes"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture_id1);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramStudio, "sTextureOriNm"), 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture_id2);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramStudio, "sTexture"), 2);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture_id3);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramStudio, "sMaskTexture"), 3);//sStudioMaskTexture2

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texute_id4);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramStudio, "sSegmentTexture"), 4);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texute_id5);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramStudio, "sStudioMaskTexture"), 5);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramStudio, "uFront"), front);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramStudio, "uFace"), face);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramStudio, "uDebugMode"), mDebugOn);

        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uInBright"), SFTunner2.studioInBright[0]);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uOutBright"), SFTunner2.studioOutBright[0]);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uRedC"), 1.0f);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uGreenC"), 1.0f);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uBlueC"), 1.0f);

        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uOfValue5"), 0.0f);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uOfValue4"), 0.0f);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uOfValue3"), 0.0f);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uOfValue2"), 0.0f);

        //test
        int mSaveStatus = 0;
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramStudio, "uSaveStatus"), mSaveStatus);
        mSaveStatus = 0;

        //test
        int mStudioModeStatus = 0;
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramStudio, "uStudioMode"), mStudioModeStatus);

        jniController.readStudioRect(gStudioData);
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mGLProgramStudio, "uStudioData"), gStudioData[0], gStudioData[1], gStudioData[2], gStudioData[3]);

        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uFaderBright"), SFTunner2.studioFaderBright[0]);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uFaderSat"), SFTunner2.studioFaderSaturation[0]);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uSatRate"), SFTunner2.studioSatRate[0]);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgramStudio, "uContrast"), SFTunner2.studioMonoContrast[0]);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgramStudio, "uUseOes"), useOes ? 1 : 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(ph);
        GLES20.glDisableVertexAttribArray(tch);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glUseProgram(0);

        if ( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

    public static void setDebugOn(int onoff)
    {
        mDebugOn = onoff;
    }

    public static final String SOURCE_DRAW_STUDIO_COMMON_FS = "" +

            "float calcDistance(vec2 tex_coord, float midX, float midY, float maxDistance, float radius) {\n" +
            "   if(maxDistance == 0.0) {\n" +
            "       return 0.0;\n" +
            "   }\n" +
            "   float x = tex_coord.x * 1440.0;\n" +//current point x
            "   float y = tex_coord.y * 1080.0;\n" +//current point y
            "   float distance = sqrt(((x-midX) * (x-midX)) + ((y-midY) * (y-midY)));\n" +//middle point
            "   distance = distance - radius;\n" +
            "   if(distance < 0.0) distance = 0.0;\n" +
            "   return distance / (maxDistance - radius );\n" +
            "}\n" +

            "vec4 makeBlackWhite(vec4 color, float rate) {\n" +
            "	vec4 mix;\n" +
            "	float rate_f = rate / 100.0;\n" +
            "	if( uStudioMode == 0 || uStudioMode == 2 ) {\n" +
            "		vec4 black = vec4(0.0, 0.0, 0.0, 1.0);\n" +
//            "		mix = black * rate_f + color * (1.0 - rate_f);\n" +
            "		mix = vec4(mix(color.rgb, black.rgb, rate_f), 1.0);\n" + //sally

//			"		mix = color * (1.0 - rate_f);\n"+
            "	}\n" +
            "	else if( uStudioMode == 3 ) {\n" +
            "		vec4 white = vec4(1.0, 1.0, 1.0, 1.0);\n" +
//            "		mix = white * rate_f + color * (1.0 - rate_f);\n" +
            "		mix = vec4(mix(color.rgb, white.rgb, rate_f), 1.0);\n" + //sally
//			"		mix = color * (1.0 + rate_f);\n"+
            "	}\n" +
            "	return mix;\n" +
            "}\n" +

            "vec4 makeStudioMode(vec4 color, vec4 mask_color) {\n" +
            "		 vec4 high_color;\n" +
            "		 vec4 low_color;\n" +
            "       if( mask_color.r > 0.03 ) {\n" +
            "			if( mask_color.r > 0.7 ) {\n" +
            "				return makeBlackWhite(color, uOfValue5);\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.5 ) {\n" +
            "				return makeBlackWhite(color, uOfValue4);\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.4 ) {\n" +
            "				return makeBlackWhite(color, uOfValue3);\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.3 ) {\n" +
            "				return makeBlackWhite(color, uOfValue2);\n" +
            //"				return vec4(1.0, 0.0, 0.0, 1.0);\n" +
            "           }\n" +
            "           else if ( mask_color.r > 0.2 ){\n" +
            "				return color;\n" +
            "           }\n" +
            //nothing to do...
            "           else if ( mask_color.r > 0.145 ){\n" +
            "				return color;\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.08 ){\n" +
            "           	if(uDebugMode == 0) {\n" +
            "					high_color = makeBlackWhite(color, uOfValue5);\n" +
            "					low_color = makeBlackWhite(color, uOfValue4);\n" +
            "              	high_color.rgb *= mask_color.b;\n" +
            "               	low_color.rgb *= (1.0 - mask_color.b);\n" +

            "					return high_color + low_color;\n" +
            "				}\n" +
            "				else {\n" +
            "					return makeBlackWhite(color, uOfValue5);\n" +
            "				}\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.06 ){\n" +
            "           	if(uDebugMode == 0) {\n" +
            "					high_color = makeBlackWhite(color, uOfValue4);\n" +
            "					low_color = makeBlackWhite(color, uOfValue3);\n" +
            "              	high_color.rgb *= mask_color.b;\n" +
            "               	low_color.rgb *= (1.0 - mask_color.b);\n" +

            "					return high_color + low_color;\n" +
            "				}\n" +
            "				else {\n" +
            "					return makeBlackWhite(color, uOfValue4);\n" +
            "				}\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.04 ){\n" +
            "           	if(uDebugMode == 0) {\n" +
            "					high_color = makeBlackWhite(color, uOfValue3);\n" +
            "					low_color = makeBlackWhite(color, uOfValue2);\n" +
            "              	high_color.rgb *= mask_color.b;\n" +
            "               	low_color.rgb *= (1.0 - mask_color.b);\n" +

            "					return high_color + low_color;\n" +
            "				}\n" +
            "				else {\n" +
            "					return makeBlackWhite(color, uOfValue3);\n" +
            "				}\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.02 ){\n" +
            "           	if(uDebugMode == 0) {\n" +
            "               vec2 tex_coord;\n" +
            "               if( uFront == 1 ) {\n" +
            "                   tex_coord = vec2(1.0-texCoord.x, texCoord.y);\n" +
            "               }\n" +
            "               else {\n" +
            "                   tex_coord = texCoord;\n" +
            "               }\n" +

            "					high_color = makeBlackWhite(color, uOfValue2);\n" +
            "					low_color = color;\n" +

            "                  float rate = calcDistance(tex_coord, 1440.0/2.0, 1080.0/2.0, 900.0, 360.0);\n" +//max, radius
            "              	high_color.rgb *= rate;\n" +
            "               	low_color.rgb *= (1.0 - rate);\n" +

            "					return high_color + low_color;\n" +

            "				}\n" +
            "				else {\n" +
            "					return makeBlackWhite(color, uOfValue2);\n" +
            "				}\n" +
            "           }\n" +
            "           else { \n" +
            "               return vec4(1.0, 0.0, 0.0, 1.0);\n" +
            "           }\n" +
            "       }\n" +
            "}\n" +

            "vec4 makeRefColorFaceBack(vec4 color) {\n" +
            "	vec4 color_c = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "	if( uStudioMode == 0 || uStudioMode == 1 || uStudioMode == 3 ) {\n" +
            "		color_c = color;\n" +
            "	}\n" +
            "	else if( uStudioMode == 2 ) {\n" +
            "		float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));\n" +
            "		color_c = vec4(vec3(gray), 1.0);\n" +
            "	}\n" +
            "	return color_c;\n" +
            "}\n" +

            "vec4 makeRefColorFaceFore(vec4 color) {\n" +
            "	vec4 color_c = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "	if( uStudioMode == 0 || uStudioMode == 1 || uStudioMode == 3 ) {\n" +
            "		color_c = color;\n" +//Brightness
            "	}\n" +
            "	else if( uStudioMode == 2 ) {\n" +
            "		float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));\n" +
            "		color_c = vec4(vec3(gray), 1.0);\n" +
            "	}\n" +
            "	return color_c;\n" +
            "}\n" +

            "vec3 rgb2hsv(vec3 c)\n"+
            "{\n"+
            "    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n"+
            "    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));\n"+
            "    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n"+

            "    float d = q.x - min(q.w, q.y);\n"+
            "    float e = 1.0e-10;\n"+
            "    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);\n"+
            "}\n"+

            "vec3 hsv2rgb(vec3 c)\n"+
            "{\n"+
            "    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n"+
            "    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n"+
            "    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n"+
            "}\n"+

            //here
            "vec4 makeRefColorBack(vec4 color, vec4 mask_color) {\n" +
            "	vec4 color_c = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "	if( uStudioMode == 0 ) {\n" +
            "       vec3 hsvColor = rgb2hsv(color.rgb);\n" +
            "       hsvColor.g *= uSatRate;\n" +
            "       vec3 rgbColor = hsv2rgb(hsvColor.rgb);\n" +
            "		color_c = vec4(rgbColor.r, rgbColor.g, rgbColor.b, 1.0);\n" +

            "	}\n" +
            "	else if( uStudioMode == 1 ) {\n" +
            "		float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));\n" +
            "		color_c = vec4(vec3(gray), 1.0);\n" +
            "	}\n" +
            "	return color_c;\n" +
            "}\n" +

            "vec4 makeRefColorFore(vec4 color) {\n" +
            "	vec4 color_c = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "	if( uStudioMode == 0 ) {\n" +
            "		color_c = color;\n" +
            "	}\n" +
            "	else if( uStudioMode == 1 ) {\n" +
            "		float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));\n" +
            "		color_c = vec4(vec3(gray), 1.0);\n" +
            "	}\n" +
            "	return color_c;\n" +
            "}\n" +

            "vec4 makeContrastMode(vec4 color, float bright, float contrast) {\n" +

            "   float brightValue = bright - 100.0;\n" +
            "   float contrastValue = contrast - 100.0;\n" +

            "	float delta = 0.0;\n" +
            "	float alpha = 1.0;\n" +
            "	float beta = 0.0;\n" +

            "	if( contrastValue > 0.0 ) {\n" +
            "	    delta = 127.0 * contrastValue / 100.0;\n" +
            "       alpha = 255.0 / ( 255.0 - delta * 2.0 );\n" +
            "       beta = alpha * ( brightValue - delta );\n" +
            "	}\n" +
            "	else {\n" +
            "	    delta = -128.0 * contrastValue / 100.0;\n" +
            "       alpha = ( 256.0 - delta * 2.0 ) / 255.0;\n" +
            "       beta = alpha * brightValue + delta;\n" +
            "	}\n" +

            "   beta = beta / 255.0;\n" +
            "   return (color * alpha) + beta;\n" +
            "}\n" +

            "vec4 makeDarkSnowMode(vec2 tex_coord, vec4 color, float x, float y, float maxValue, float radius) {\n" +

            "   float rate = calcDistance(tex_coord, x, y, maxValue, radius);\n" +//max, radius

            "	if( uStudioMode == 0 || uStudioMode == 1 || uStudioMode == 2 ) {\n" +
            "	    vec4 black = vec4(0.0, 0.0, 0.0, 1.0);\n" +
//            "       return (black * rate) + (color * (1.0 - rate));\n" +
            "       return vec4(mix(color.rgb, black.rgb, rate), 1.0);\n" +//sally
            "	}\n" +
            "	else if( uStudioMode == 3 ) {\n" +
            "	    vec4 white = vec4(1.0, 1.0, 1.0, 1.0);\n" +
//            "       return (white * rate) + (color * (1.0 - rate));\n" +
            "       return vec4(mix(color.rgb, white.rgb, rate), 1.0);\n" +//sally
            "	}\n" +
            "}\n" +

            "vec4 makeBrightFader(vec4 oriColor, vec4 blurColor, vec4 mask_color, float brightIn, float brightOut, float colorR, float colorG, float colorB) {\n" +

            "	vec4 color2 = vec4(blurColor.r * colorR, blurColor.g * colorG, blurColor.b * colorB, blurColor.a);\n" +

            "	if( 0.04 > mask_color.r && mask_color.r > 0.02 ){\n" +
            "		if(uDebugMode == 0) {\n" +
            "			vec4 high_color = color2 * brightOut;\n" +
            "			vec4 low_color = oriColor * brightIn;\n" +

            "           mask_color.b *= uFaderSat;\n" +
            "           if( mask_color.b > 1.0 ) mask_color.b = 1.0;\n" +

            "          high_color.rgb *= mask_color.b;\n" +
            "          low_color.rgb *= (1.0 - mask_color.b);\n" +

            "			return (high_color + low_color) * uFaderBright;\n" +
            "		}\n" +
            "		else {\n" +
            "       	return (color2 * brightOut);\n" +
            "		}\n" +
            "   }\n" +
            "	else {\n" +
            "   		return (color2 * brightOut);\n" +
            "	}\n" +
            "}\n" +

            "vec4 makeBrightFaderForFace(vec4 oriColor, vec4 blurColor, vec4 mask_color, float brightIn, float brightOut, float colorR, float colorG, float colorB) {\n" +

            "	vec4 color2 = vec4(blurColor.r * colorR, blurColor.g * colorG, blurColor.b * colorB, blurColor.a);\n" +

            "	if( mask_color.r > 0.001 ){\n" +
            "		if(uDebugMode == 0) {\n" +
            "			vec4 high_color = color2 * brightOut;\n" +
            "			vec4 low_color = oriColor * brightIn;\n" +

            "           mask_color.r *= uFaderSat;\n" +
            "           if( mask_color.r > 1.0 ) mask_color.r = 1.0;\n" +

            "          high_color.rgb *= mask_color.r;\n" +
            "          low_color.rgb *= 1.0 - mask_color.r;\n" +

            "			return (high_color + low_color) * uFaderBright;\n" +
            "		}\n" +
            "		else {\n" +
            "       	return (color2 * brightOut);\n" +
            "		}\n" +
            "   }\n" +
            "	else {\n" +
            "   		return (oriColor * brightIn);\n" +
            "	}\n" +
            "}\n";

    public static final String SOURCE_DRAW_STUDIO_COMMON2_FS = "" +
            "	if( uSaveStatus == 1 ) {\n" +
            "		gl_FragColor = texture2D(sTexture, tex_coord) * 0.5;\n" +
            "	}\n" +
            "	else {\n" +
            "		vec4 studio_mask_color = texture2D(sStudioMaskTexture, tex_coord);\n" +
            "		vec4 mask_color = texture2D(sMaskTexture, tex_coord);\n" +

            "		if( uStudioMode == 3 ) {\n" +
            "		     vec4 oriColor = inputColor;\n" +
            "		     vec4 greenColor = vec4(mask_color.a, mask_color.a, mask_color.a, 1.0);\n" +
            "           vec4 mixColor = oriColor;\n" +
            "           if( mask_color.r > 0.0 ) {\n" +
            //"               mixColor = oriColor * 0.5 + greenColor * 0.5;\n" +
            "               mixColor = oriColor + greenColor;\n" +
            "           };\n" +
            "           gl_FragColor =  vec4(oriColor.r, mixColor.g, oriColor.b, 1.0);\n" +
            "           return;\n"+
            "       }\n"+

//            "		 gl_FragColor = texture2D(sTexture, tex_coord);\n" +
//            "       return;\n"+

            "		 if( uFace == 1 ) {\n" +

            "           if( studio_mask_color.r > 0.18 || studio_mask_color.r < 0.14 ) {\n" +//40> || <35
            //"           if( studio_mask_color.r > 0.001 ) {\n" +//40> || <35
            "		         vec4 resultColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "		         vec4 oriColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "		         vec4 color = vec4(1.0, 1.0, 1.0, 1.0);\n" +

//            "               if( mask_color.r > 0.18 || mask_color.r < 0.14 ) {\n" +//40> || <35
//            "		                oriColor = texture2D(sTexture, tex_coord);\n" +
//            "		                color = makeRefColorBack(texture2D(sTexture, tex_coord), studio_mask_color);\n" +
//            "				 }\n" +
//            "               else {\n" +//40> || <35
//            "		                oriColor = inputColor;\n" +
//            "		                color = makeRefColorBack(inputColor, studio_mask_color);\n" +
//            "				 }\n" +

            "               if( mask_color.r > 0.001 ) {\n" +//40> || <35
            "		                oriColor = texture2D(sTexture, tex_coord);\n" +
            "		                color = makeRefColorBack(texture2D(sTexture, tex_coord), studio_mask_color);\n" +
            "				 }\n" +
            "               else {\n" +//40> || <35
            "		                oriColor = inputColor;\n" +
            "		                color = makeRefColorBack(inputColor, studio_mask_color);\n" +
            "				 }\n" +

            "				    if( uStudioMode == 0 ) {\n" +
            "				          vec4 firstColor = makeBrightFader(oriColor, color, studio_mask_color, uInBright, uOutBright, uRedC, uGreenC, uBlueC);\n" +
            //"				          vec4 firstColor = makeBrightFaderForFace(oriColor, color, studio_mask_color, uInBright, uOutBright, uRedC, uGreenC, uBlueC);\n" +
            "                       firstColor = makeContrastMode(firstColor, 100.0, uContrast);\n" +
            "                       resultColor =  makeDarkSnowMode(tex_coord, firstColor, uStudioData.x, uStudioData.y, uStudioData.w, uStudioData.z);\n" +
            "				    }\n" +
            "				    else if( uStudioMode == 1 ) {\n" +
            "                       resultColor =  makeDarkSnowMode(tex_coord, color, uStudioData.x, uStudioData.y, uStudioData.w, uStudioData.z);\n" +
            "				    }\n" +
            "				    else if( uStudioMode == 2 ) {\n" +
            "                       resultColor =  makeDarkSnowMode(tex_coord, oriColor, uStudioData.x, uStudioData.y, uStudioData.w, uStudioData.z);\n" +
            "				    }\n" +

            "       		    gl_FragColor  = resultColor;\n" +
            "       	}\n" +
            "       	else {\n" +
            "		         vec4 resultColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "		         vec4 oriColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "		         vec4 color = vec4(1.0, 1.0, 1.0, 1.0);\n" +

            "               float inBright = uInBright;\n"+
            "               if( uStudioMode == 1 ) {\n" +
            "       		    inBright  = 1.0;\n" +
            "				 }\n" +

//            "               if( mask_color.r > 0.18 || mask_color.r < 0.14 ) {\n" +//40> || <35
//            "		            oriColor = texture2D(sTexture, tex_coord);\n" +
//            "		            color = makeRefColorFore(texture2D(sTexture, tex_coord)) * inBright;\n" +
//            "				 }\n" +
//            "               else {\n" +//40> || <35
//            "		            oriColor = inputColor;\n" +
//            "		            color = makeRefColorFore(inputColor) * inBright;\n" +
//            "				 }\n" +

            "              if( mask_color.r > 0.001 ) {\n" +//40> || <35
            "		            oriColor = texture2D(sTexture, tex_coord);\n" +
//            "		            oriColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
            "		            color = makeRefColorFore(texture2D(sTexture, tex_coord)) * inBright;\n" +
            "				 }\n" +
            "               else {\n" +//40> || <35
            "		            oriColor = inputColor;\n" +
            "		            color = makeRefColorFore(inputColor) * inBright;\n" +
            "				 }\n" +

            "               if( uStudioMode == 0 ) {\n" +
            "                  color = makeContrastMode(color, 100.0, uContrast);\n" +
            "                  resultColor =  makeDarkSnowMode(tex_coord, color, uStudioData.x, uStudioData.y, uStudioData.w, uStudioData.z);\n" +
            "				 }\n" +
            "				 else if( uStudioMode == 1 ) {\n" +
            "                  resultColor =  makeDarkSnowMode(tex_coord, color, uStudioData.x, uStudioData.y, uStudioData.w, uStudioData.z);\n" +
            "				 }\n" +
            "				 else if( uStudioMode == 2 ) {\n" +
            "                  resultColor =  makeDarkSnowMode(tex_coord, oriColor, uStudioData.x, uStudioData.y, uStudioData.w, uStudioData.z);\n" +
            "				 }\n" +

            "       		 gl_FragColor  = resultColor;\n" +
            "       	}\n" +

            "		 }\n" +
            "		 else {\n" +
            "           if( studio_mask_color.r > 0.18 || studio_mask_color.r < 0.14 ) {\n" +//40> || <35
            "		         vec4 resultColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "		         vec4 oriColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "		         vec4 color = vec4(1.0, 1.0, 1.0, 1.0);\n" +

            "               if( mask_color.r > 0.18 || mask_color.r < 0.14 ) {\n" +//40> || <35
            "		                oriColor = texture2D(sTexture, tex_coord);\n" +
            "		                color = makeRefColorBack(texture2D(sTexture, tex_coord), studio_mask_color);\n" +
            "				 }\n" +
            "               else {\n" +//40> || <35
            "		                oriColor = inputColor;\n" +
            "		                color = makeRefColorBack(inputColor, studio_mask_color);\n" +
            "				 }\n" +

            "				    if( uStudioMode == 0 ) {\n" +
            //"				         gl_FragColor = makeBrightFader(oriColor, color, studio_mask_color, uInBright, uOutBright, uRedC, uGreenC, uBlueC);\n" +
            "				         vec4 firstColor = makeBrightFader(oriColor, color, studio_mask_color, uInBright, uOutBright, uRedC, uGreenC, uBlueC);\n" +
            "                       firstColor = makeContrastMode(firstColor, 100.0, uContrast);\n" +
            "                       resultColor =  makeDarkSnowMode(tex_coord, firstColor, uStudioData.x, uStudioData.y, uStudioData.w, uStudioData.z);\n" +
            "				    }\n" +
            "				    else if( uStudioMode == 1 ) {\n" +
            "                       resultColor =  makeDarkSnowMode(tex_coord, color, uStudioData.x, uStudioData.y, uStudioData.w, uStudioData.z);\n" +
            "				    }\n" +
            "				    else if( uStudioMode == 2 ) {\n" +
            "                       resultColor =  makeDarkSnowMode(tex_coord, oriColor, uStudioData.x, uStudioData.y, uStudioData.w, uStudioData.z);\n" +
            "				    }\n" +

            "       		    gl_FragColor  = resultColor;\n" +
            "       	}\n" +
            "       	else {\n" +
            "		         vec4 resultColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "		         vec4 oriColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "		         vec4 color = vec4(1.0, 1.0, 1.0, 1.0);\n" +

            "               float inBright = uInBright;\n"+
            "               if( uStudioMode == 1 ) {\n" +
            "       		    inBright  = 1.0;\n" +
            "				 }\n" +

            "               if( mask_color.r > 0.18 || mask_color.r < 0.14 ) {\n" +//40> || <35
            "		            oriColor = texture2D(sTexture, tex_coord);\n" +
            "		            color = makeRefColorFore(texture2D(sTexture, tex_coord)) * inBright;\n" +
            //"		            color = vec4(1.0, 0.0, 0.0, 1.0);\n" +
            "				 }\n" +
            "               else {\n" +//40> || <35
            "		            oriColor = inputColor;\n" +
            "		            color = makeRefColorFore(inputColor) * inBright;\n" +
            //"		            color = vec4(0.0, 0.0, 1.0, 1.0);\n" +
            "				 }\n" +

            "               if( uStudioMode == 0 ) {\n" +
            "                   color = makeContrastMode(color, 100.0, uContrast);\n" +
            "                  resultColor =  makeDarkSnowMode(tex_coord, color, uStudioData.x, uStudioData.y, uStudioData.w, uStudioData.z);\n" +
            //"                  resultColor =  color;\n" +
            "				 }\n" +
            "				 else if( uStudioMode == 1 ) {\n" +
            "                  resultColor =  makeDarkSnowMode(tex_coord, color, uStudioData.x, uStudioData.y, uStudioData.w, uStudioData.z);\n" +
            "				 }\n" +
            "				 else if( uStudioMode == 2 ) {\n" +
            "                  resultColor =  makeDarkSnowMode(tex_coord, oriColor, uStudioData.x, uStudioData.y, uStudioData.w, uStudioData.z);\n" +
            "				 }\n" +
            "       		 gl_FragColor  = resultColor;\n" +
            "       	}\n" +
            "       }\n" +
            "	}\n" +

            "   vec4 seg_color = texture2D(sMaskTexture, tex_coord);\n" +
            "   vec4 seg_color2 = texture2D(sStudioMaskTexture, tex_coord);\n" +
            "       if( uDebugMode == 1 ) {\n" +
            //"   		 if( (seg_color.r > 0.0  && seg_color.r < 0.16) || seg_color.b > 0.0 ) {\n" +
//            "   		 if( ( (0.0 < seg_color.b && seg_color.b < 40.0/255.0) || 40.0/255.0 < seg_color.b) || ( (0.0 < seg_color2.b && seg_color2.b < 40.0/255.0) || 40.0/255.0 < seg_color2.b) ) {\n" +
//            "           	gl_FragColor = vec4(seg_color.b, seg_color2.b, 0.0, 1.0);\n" +
//            "           }\n" +
            "   		 if( (0.0 < seg_color2.b && seg_color2.b < 40.0/255.0) || 40.0/255.0 < seg_color2.b ) {\n" +
            "           	gl_FragColor = vec4(0.0, seg_color2.b, 0.0, 1.0);\n" +
            "           }\n" +
            "       }\n" +
            "}";

    public static final String SOURCE_DRAW_STUDIO_VS = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +
            "uniform mat4 uMVPMatrix;\n" +

            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_STUDIO_FS = "" +
            "#extension GL_OES_EGL_image_external : require\n" +

            //"precision mediump float;\n" +
            "precision highp float;\n" +
            "uniform samplerExternalOES sTextureOriOes;\n" +
            "uniform sampler2D sTextureOriNm;\n" +
            "uniform sampler2D sTexture;\n" +
            "uniform sampler2D sMaskTexture;\n" +
            "uniform sampler2D sSegmentTexture;\n" +
            "uniform sampler2D sStudioMaskTexture;\n" +
            "uniform int uFront;\n" +
            "uniform int uFace;\n" +
            "uniform int uDebugMode;\n" +

            "uniform float uInBright;\n" +
            "uniform float uOutBright;\n" +
            "uniform float uRedC;\n" +
            "uniform float uGreenC;\n" +
            "uniform float uBlueC;\n" +

            "uniform float uOfValue5;\n" +
            "uniform float uOfValue4;\n" +
            "uniform float uOfValue3;\n" +
            "uniform float uOfValue2;\n" +
            "uniform int uSaveStatus;\n" +
            "uniform int uStudioMode;\n" +
            "uniform vec4 uStudioData;\n" +

            "uniform float uFaderBright;\n" +
            "uniform float uFaderSat;\n" +
            "uniform float uSatRate;\n" +

            "uniform float uContrast;\n" +

            "uniform int uUseOes;\n" +

            "varying vec2 texCoord;\n" +
            SOURCE_DRAW_STUDIO_COMMON_FS +
            "void main() {\n" +
            "  vec2 tex_coord;\n" +
            "  vec2 tex_blur_coord;\n" +
            "  if( uFront == 1 ) {\n" +
            "       tex_coord = vec2(1.0-texCoord.x, texCoord.y);\n" +
            "       tex_blur_coord = vec2(texCoord.x, 1.0-texCoord.y);\n" +
            "  }\n" +
            "  else {\n" +
            //"       tex_coord = vec2(1.0-texCoord.x, texCoord.y);\n" +
            "       tex_coord = vec2(texCoord.x, texCoord.y);\n" +

            "		 if( uFace == 1 ) {\n" +
            //"           tex_blur_coord = vec2(texCoord.x, 1.0-texCoord.y);\n" +
            "           tex_blur_coord = vec2(texCoord.x, texCoord.y);\n" +
            "       }\n" +
            "		 else {\n" +
            "           tex_blur_coord = vec2(texCoord.x, texCoord.y);\n" +
            "       }\n" +
            "  }\n" +

            "   vec4 inputColor;\n" +
            "   if( uUseOes == 1 ) {\n" +
            "       inputColor = texture2D(sTextureOriOes, tex_coord);\n" +
            "   }\n" +
            "   else {\n" +
            "       inputColor = texture2D(sTextureOriNm, tex_coord);\n" +
            "   }\n" +
            SOURCE_DRAW_STUDIO_COMMON2_FS;
}
