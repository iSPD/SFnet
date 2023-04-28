package com.ispd.sfcam.pdEngine;

import android.opengl.GLES11Ext;
import android.opengl.GLES31;
import android.opengl.Matrix;
import android.util.Log;

import com.ispd.sfcam.aiCamParameters;
import com.ispd.sfcam.utils.SFTunner;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.ispd.sfcam.pdEngine.glEngineGL.setProgram;

public class CreateEdgeFilter {
    public static final String SOURCE_DRAW_VS_ADT = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +

            "uniform mat4 uMVPMatrix;\n" +

            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_ADT = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            //"precision mediump float;\n" +
            "precision highp float;\n" +

            "uniform samplerExternalOES sTextureOri;\n" +
            "uniform sampler2D sTextureBt;\n" +
            "uniform sampler2D sTextureInitNm;\n" +
            "uniform sampler2D sIntegralTexture;\n" +

            "varying vec2 texCoord;\n" +
            "uniform int uFront;\n" +
            "uniform int uSaveStatus;\n" +

            "uniform int uCartoonMode;\n" +
            "uniform float uWidth;\n" +
            "uniform float uHeight;\n" +
            "uniform float uBackEdgeTs;\n" +
            "uniform float uFrontEdgeTs;\n" +

            "uniform int uUseBt;\n" +
            "uniform int uUseOes;\n" +

            "int blockSize = 3;\n" +
            "float blurWeight = 10.0;\n" +
//            "float threshold = 10.0 * (1.0 / 255.0);\n" +
            "float threshold = 15.0 * (1.0 / 255.0);\n" +

//            "float xStep = 1.0 / 720.0;\n" +
//            "float yStep = 1.0 / 540.0;\n" +

            "vec4 calcBlur(vec2 tex_coord, int kSize) {\n" +
            "   float xStep = 1.0 / uWidth;\n" +
            "   float yStep = 1.0 / uHeight;\n" +
            "   float width = xStep;\n" +
            "   float height = yStep;\n" +
            "   int maskSize = kSize;\n" +
            "   vec4 sum = vec4(0.0, 0.0, 0.0, 0.0);\n" +
            "   int i, j;\n" +
            "   #pragma unroll\n" +
            "   for( i = -maskSize/2; i <= maskSize/2; i++) {\n" +
            "   #pragma unroll\n" +
            "       for( j = -maskSize/2; j <= maskSize/2; j++) {\n" +
            "           if( uUseBt == 1 ) {\n" +
            "               sum += texture2D(sTextureBt, vec2(tex_coord.x + float(i)*width*blurWeight, tex_coord.y + float(j)*height*blurWeight));\n" +
            "           }\n" +
            "           else {\n" +
            "               if( uUseOes == 1 ) {\n" +
            "                   sum += texture2D(sTextureOri, vec2(tex_coord.x + float(i)*width*blurWeight, tex_coord.y + float(j)*height*blurWeight));\n" +
            "               }else {\n" +
            "                   sum += texture2D(sTextureInitNm, vec2(tex_coord.x + float(i)*width*blurWeight, tex_coord.y + float(j)*height*blurWeight));\n" +
            "               }\n" +
            "           }\n" +
            "       }\n" +
            "   }\n" +
            "   return sum/float(maskSize*maskSize);\n" +
            "}\n" +

            "void main() {\n" +

            "   if( uCartoonMode == 0 ) {\n" +
            "       threshold = uBackEdgeTs * (1.0 / 255.0);\n" +
            "   }\n" +
            "   else {\n" +
            "       threshold = uFrontEdgeTs * (1.0 / 255.0);\n" +
            "   }\n" +

            "   vec2 newTexCoord = texCoord;\n" +
            "   if( uFront == 1 ) {\n" +
            "       newTexCoord.x = 1.0 - newTexCoord.x;\n" +
            "   }\n" +
            "   else {\n" +
            "       if( uUseOes == 0 ) {\n" +
            "           newTexCoord.x = 1.0 - newTexCoord.x;\n" +
            "       }\n" +
            "   }\n" +

            "	if(uSaveStatus == 1) {\n" +
            "           if( uUseBt == 1 ) {\n" +
            "   	        gl_FragColor = texture2D(sTextureBt, newTexCoord) * 0.5;\n" +
            "           }\n" +
            "           else {\n" +
            "               if( uUseOes == 1 ) {\n" +
            "   	             gl_FragColor = texture2D(sTextureOri, newTexCoord) * 0.5;\n" +
            "               }else {\n" +
            "   	             gl_FragColor = texture2D(sTextureInitNm, newTexCoord) * 0.5;\n" +
            "               }\n" +
            "           }\n" +
            "	}\n" +
            "	else {\n" +
            "   	vec4 filterd = calcBlur(newTexCoord, blockSize);\n" +
            "   	float filterdGray = 0.299 * filterd.r + 0.587 * filterd.g + 0.114 * filterd.b;\n" +
            "   	filterdGray = filterdGray - threshold;\n" +

            "   	     vec4 image;\n" +
            "           if( uUseBt == 1 ) {\n" +
            "   	        image = texture2D(sTextureBt, newTexCoord);\n" +
            "           }\n" +
            "           else {\n" +
            "               if( uUseOes == 1 ) {\n" +
            "   	             image = texture2D(sTextureOri, newTexCoord);\n" +
            "               }else {\n" +
            "   	             image = texture2D(sTextureInitNm, newTexCoord);\n" +
            "               }\n" +
            "           }\n" +

            "      float imageGray = 0.299 * image.r + 0.587 * image.g + 0.114 * image.b;\n" +

            "      if( imageGray >= filterdGray ) {\n" +
            "   	    gl_FragColor = vec4(1.0, 1.0, 1.0, 0.0);\n" +
            "      }\n" +
            "      else {\n" +
            "   	    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);\n" +
            "      }\n" +
            "	}\n" +
            "}";

    private static int mEdgeFilterProgram = -1;
    private static FloatBuffer mMVPMatrix0;
    private static FloatBuffer mMVPMatrixBuffer90;
    private static FloatBuffer mGLVertexBufferEdge = null, mGLTexCoordBufferEdge = null;
    private static int mCartoonOption = 0;

    public static final float[] vertices = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
    public static final float[] texcoords = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

    public CreateEdgeFilter() {

    }

    public static boolean Init() {
        mEdgeFilterProgram = setProgram(SOURCE_DRAW_VS_ADT, SOURCE_DRAW_FS_ADT);
        if (mEdgeFilterProgram != -1)
            Log.e("CreateEdgeFilter", "EdgeFilter Program Success...");
        else Log.e("CreateEdgeFilter", "EdgeFilter Program failed...");

        mGLVertexBufferEdge = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLVertexBufferEdge.put(vertices);
        mGLVertexBufferEdge.position(0);
        mGLTexCoordBufferEdge = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTexCoordBufferEdge.put(texcoords);
        mGLTexCoordBufferEdge.position(0);

        //matrix
        float[] mMatrix0 = new float[16];
        float[] mMatrixXFlip = new float[16];
        Matrix.setIdentityM(mMatrix0, 0);
        Matrix.scaleM(mMatrixXFlip, 0, mMatrix0, 0, -1.0f, 1.0f, 1.0f); //sally
        mMVPMatrix0 = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrix0.put(mMatrix0);
        mMVPMatrix0.position(0);

        float []mModelMatrix90 = new float[16];
        float []mRotationMatrix90 = new float[16];
        float []mMVPMatrix90 = new float[16];

        Matrix.setIdentityM(mModelMatrix90, 0);
        Matrix.setRotateM(mRotationMatrix90, 0, 90, 0, 0, -1.0f);
        Matrix.multiplyMM(mMVPMatrix90, 0, mModelMatrix90, 0, mRotationMatrix90, 0);

        mMVPMatrixBuffer90 = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrixBuffer90.put(mMVPMatrix90);
        mMVPMatrixBuffer90.position(0);

        return true;
    }

    public static void Release() {
        if (mEdgeFilterProgram != 0) {
            GLES31.glDeleteProgram(mEdgeFilterProgram);
            mEdgeFilterProgram = 0;
        }
        mGLVertexBufferEdge = null;
        mGLTexCoordBufferEdge = null;
        mMVPMatrix0 = null;
    }

    public static void RenderToTexture(int fbo, boolean useOes, int textureOri, int textureInitNm, int textureBt, boolean useObj, int front)
    {
        if ( fbo > 0 ) {
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fbo);
        }

        GLES31.glUseProgram (mEdgeFilterProgram);

        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT);

        GLES31.glViewport(0, 0, (int)((float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_EDGE_FACTOR_F), (int)((float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_EDGE_FACTOR_F));

        int ph = GLES31.glGetAttribLocation(mEdgeFilterProgram, "vPosition");
        int tch = GLES31.glGetAttribLocation(mEdgeFilterProgram, "vTexCoord");

        GLES31.glVertexAttribPointer(ph, 2, GLES31.GL_FLOAT, false, 4 * 2, mGLVertexBufferEdge);
        GLES31.glVertexAttribPointer(tch, 2, GLES31.GL_FLOAT, false, 4 * 2, mGLTexCoordBufferEdge);
        GLES31.glEnableVertexAttribArray(ph);
        GLES31.glEnableVertexAttribArray(tch);
        if(useOes == true) {
            //rotation
            GLES31.glUniformMatrix4fv(GLES31.glGetUniformLocation(mEdgeFilterProgram, "uMVPMatrix"), 1, false, mMVPMatrix0);
        }else {
            GLES31.glUniformMatrix4fv(GLES31.glGetUniformLocation(mEdgeFilterProgram, "uMVPMatrix"), 1, false, mMVPMatrixBuffer90);
        }
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureOri);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mEdgeFilterProgram, "sTextureOri"), 0);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE1);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureBt);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mEdgeFilterProgram, "sTextureBt"), 1);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE2);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureInitNm);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mEdgeFilterProgram, "sTextureInitNm"), 2);


        GLES31.glUniform1i(GLES31.glGetUniformLocation(mEdgeFilterProgram, "uFront"), front);

        if( useObj == true ) mCartoonOption = 0;
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mEdgeFilterProgram, "uCartoonMode"), mCartoonOption);

        GLES31.glUniform1f(GLES31.glGetUniformLocation(mEdgeFilterProgram, "uWidth"), (float)aiCamParameters.PREVIEW_WIDTH_I/aiCamParameters.RESIZE_EDGE_FACTOR_F);
        GLES31.glUniform1f(GLES31.glGetUniformLocation(mEdgeFilterProgram, "uHeight"), (float)aiCamParameters.PREVIEW_HEIGHT_I/aiCamParameters.RESIZE_EDGE_FACTOR_F);
        GLES31.glUniform1f(GLES31.glGetUniformLocation(mEdgeFilterProgram, "uBackEdgeTs"), SFTunner.sfCommonTune.mCartoonBackEdge);
        GLES31.glUniform1f(GLES31.glGetUniformLocation(mEdgeFilterProgram, "uFrontEdgeTs"), SFTunner.sfCommonTune.mCartoonFrontEdge);

        GLES31.glUniform1i(GLES31.glGetUniformLocation(mEdgeFilterProgram, "uUseBt"), useTestOption);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mEdgeFilterProgram, "uUseOes"), useOes ? 1 : 0);

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4);

        GLES31.glDisableVertexAttribArray(ph);
        GLES31.glDisableVertexAttribArray(tch);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glActiveTexture(GLES31.GL_TEXTURE1);
        GLES31.glActiveTexture(GLES31.GL_TEXTURE2);
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES31.glUseProgram(0);

        if ( fbo > 0 ) {
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0);
        }
    }

    public void SetCartoonOption(int opt) {
        mCartoonOption = opt;
    }

    public static int useTestOption = 0;
    public static void testButton()
    {
        useTestOption = 1 - useTestOption;
    }
}
