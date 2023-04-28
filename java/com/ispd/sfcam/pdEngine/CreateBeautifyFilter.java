package com.ispd.sfcam.pdEngine;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.opengl.Matrix;
import android.util.Log;

import com.ispd.sfcam.utils.SFTunner;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.ispd.sfcam.pdEngine.glEngineGL.setProgram;

public class CreateBeautifyFilter {

    public static final String SOURCE_DRAW_VS_BEAUTIFY_FILTER = "" +
            "attribute vec2 vPosition;\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "   vTexCoord = vPosition / 2.0 + 0.5;\n" +
            "   gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_BEAUTIFY_FILTER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            //"uniform sampler2D sTexture;\n" +
            "uniform sampler2D sMaskTexture;\n" +
            "uniform sampler2D sGammaTexture;\n" +
            "uniform vec2 imageStep;\n" +
            "uniform float intensity;\n" +
            "uniform int uUseCartoon;\n" +
            "varying vec2 vTexCoord;\n" +

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

            "void main()\n" +
            "{\n" +
            "    vec2 blurCoordinates[20];\n" +

            "   float useColor;\n" +

            "   if( uUseCartoon == 1 ) {\n" +
            "       vec4 mask_color = texture2D(sMaskTexture, vec2(1.0-vTexCoord.x, vTexCoord.y));\n" +
            "       useColor = 1.0 - mask_color.b;\n" +
            "   }\n" +
            "   else if( uUseCartoon == 0 ) {\n" +
            "       vec4 mask_color = texture2D(sMaskTexture, vTexCoord);\n" +
            "       useColor = mask_color.r;\n" +
            "   }\n" +
            "   else {\n" +
            "       gl_FragColor = black_edge_effect(vTexCoord, texture2D(sTexture, vTexCoord));\n" +
            //"       gl_FragColor = texture2D(sTexture, vTexCoord);\n" +
            "       return;\n" +
            "   }\n" +

            //"   if( useColor < 1.0 ) {\n" +
            "   if( true ) {\n" +

            "    blurCoordinates[0] = vTexCoord + vec2(0.0, -10.0) * imageStep;\n" +
            "    blurCoordinates[1] = vTexCoord + vec2(5.0, -8.0) * imageStep;\n" +
            "    blurCoordinates[2] = vTexCoord + vec2(8.0, -5.0) * imageStep;\n" +
            "    blurCoordinates[3] = vTexCoord + vec2(10.0, 0.0) * imageStep;\n" +
            "    blurCoordinates[4] = vTexCoord + vec2(8.0, 5.0) * imageStep;\n" +
            "    blurCoordinates[5] = vTexCoord + vec2(5.0, 8.0) * imageStep;\n" +
            "    blurCoordinates[6] = vTexCoord + vec2(0.0, 10.0) * imageStep;\n" +
            "    blurCoordinates[7] = vTexCoord + vec2(-5.0, 8.0) * imageStep;\n" +
            "    blurCoordinates[8] = vTexCoord + vec2(-8.0, 5.0) * imageStep;\n" +
            "    blurCoordinates[9] = vTexCoord + vec2(-10.0, 0.0) * imageStep;\n" +
            "    blurCoordinates[10] = vTexCoord + vec2(-8.0, -5.0) * imageStep;\n" +
            "    blurCoordinates[11] = vTexCoord + vec2(-5.0, -8.0) * imageStep;\n" +
            "    blurCoordinates[12] = vTexCoord + vec2(0.0, -6.0) * imageStep;\n" +
            "    blurCoordinates[13] = vTexCoord + vec2(-4.0, -4.0) * imageStep;\n" +
            "    blurCoordinates[14] = vTexCoord + vec2(-6.0, 0.0) * imageStep;\n" +
            "    blurCoordinates[15] = vTexCoord + vec2(-4.0, 4.0) * imageStep;\n" +
            "    blurCoordinates[16] = vTexCoord + vec2(0.0, 6.0) * imageStep;\n" +
            "    blurCoordinates[17] = vTexCoord + vec2(4.0, 4.0) * imageStep;\n" +
            "    blurCoordinates[18] = vTexCoord + vec2(6.0, 0.0) * imageStep;\n" +
            "    blurCoordinates[19] = vTexCoord + vec2(4.0, -4.0) * imageStep;\n" +

            "    vec3 centralColor = texture2D(sTexture, vTexCoord).rgb;\n" +

            "    float sampleColor = centralColor.g * 24.0;\n" +

            "    sampleColor += texture2D(sTexture, blurCoordinates[0]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[1]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[2]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[3]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[4]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[5]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[6]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[7]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[8]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[9]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[10]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[11]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[12]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[13]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[14]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[15]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[16]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[17]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[18]).g;\n" +
            "    sampleColor += texture2D(sTexture, blurCoordinates[19]).g;\n" +

            "    sampleColor = sampleColor/44.0;\n" +

            "    float dis = centralColor.g - sampleColor + 0.5;\n" +

            "    if(dis <= 0.5) {\n" +
            "        dis = dis * dis * 2.0;\n" +
            "    }\n" +
            "	 else {\n" +
            "        dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
            "    }\n" +

            "    if(dis <= 0.5) {\n" +
            "        dis = dis * dis * 2.0;\n" +
            "    }\n" +
            "    else {\n" +
            "        dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
            "    }\n" +
            "    if(dis <= 0.5) {\n" +
            "        dis = dis * dis * 2.0;\n" +
            "    }\n" +
            "    else {\n" +
            "        dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
            "    }\n" +
            "    if(dis <= 0.5) {\n" +
            "        dis = dis * dis * 2.0;\n" +
            "    }\n" +
            "    else {\n" +
            "        dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
            "    }\n" +
            "    if(dis <= 0.5) {\n" +
            "        dis = dis * dis * 2.0;\n" +
            "    }\n" +
            "    else {\n" +
            "        dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
            "    }\n" +

            "    vec3 result = centralColor * 1.065 - dis * 0.065;\n" +

            "    float hue = dot(result, vec3(0.299,0.587,0.114)) - 0.3;\n" +

            "    hue = pow(clamp(hue, 0.0, 1.0), 0.3);\n" +

            "    result = centralColor * (1.0 - hue) + result * hue;\n" +
            "    result = (result - 0.8) * 1.06 + 0.8;\n" +

            "    result = pow(result, vec3(0.75));\n" +

            "    result = mix(centralColor, result, intensity);\n" +

            //"    vec4 preview = texture2D(sTexture, vTexCoord);\n" +

            //"    gl_FragColor = vec4(result, 1.0);\n" +
            //"    gl_FragColor = vec4(result, 1.0) * (1.0 - mask_color.r) + preview * mask_color.r;\n" +

            "    gl_FragColor = black_edge_effect(vTexCoord, vec4(result, 1.0));\n" +
            "   }\n" +
            "   else {\n" +
            "    gl_FragColor = texture2D(sTexture, vTexCoord);\n" +
            "   }\n" +
            "}";

    private static FloatBuffer mMVPMatrixBeautify;

    public static int mBeautifyFilterProgram;
    protected static int mVertexBuffer;
    public static final float[] vertices = {-1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f};
    private static int mOffScreenFrameBuffer = -1;
    private static int mOffScreenRenderBuffer = -1;
    private static final float BEAUTIFY_INTENSITY = 1.0f;
    private static final float BEAUTIFY_MUL = 1.5f;

    public CreateBeautifyFilter() {

    }

    public static boolean Init() {
        //create program
        mBeautifyFilterProgram = setProgram(SOURCE_DRAW_VS_BEAUTIFY_FILTER, SOURCE_DRAW_FS_BEAUTIFY_FILTER);
        if (mBeautifyFilterProgram != -1)
            Log.e("BeautifyFilter", "BeautifyFilter init SUCCESS!!!...");
        else Log.e("BeautifyFilter", "BeautifyFilter init failed...");

        //vertex
        int[] vertexBuffer = new int[1];
        GLES31.glGenBuffers(1, vertexBuffer, 0);
        mVertexBuffer = vertexBuffer[0];

        if (mVertexBuffer == 0) {
            Log.e("CreateFeather", "Invalid VertexBuffer!");
            return false;
        }

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, mVertexBuffer);
        FloatBuffer buffer = FloatBuffer.allocate(vertices.length);
        buffer.put(vertices).position(0);
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, 32, buffer, GLES31.GL_STATIC_DRAW);

        //matrix
        float[] mMVPMatrix0 = new float[16];
        float[] mRotationMatrix90 = new float[16];
        float[] mScaleMatrix = new float[16];

        Matrix.setRotateM(mRotationMatrix90, 0, 90, 0, 0, -1.0f);
        Matrix.setIdentityM(mMVPMatrix0, 0);
//        Matrix.scaleM(mScaleMatrix, 0, mRotationMatrix90, 0, -1.0f, -1.0f, 1.0f); //sally
        Matrix.scaleM(mScaleMatrix, 0, mMVPMatrix0, 0, 1.0f, 1.0f, 1.0f); //sally
//        Matrix.scaleM(mScaleMatrix, 0, mMVPMatrix0, 0, 1.0f, -1.0f, 1.0f); //sally
        mMVPMatrixBeautify = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrixBeautify.put(mScaleMatrix);
        mMVPMatrixBeautify.position(0);

//        Matrix.scaleM(mScaleMatrix, 0, mMVPMatrix0, 0, 1.0f, -1.0f, 1.0f); //sally
//        mMVPMatrixBeautify = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
//        mMVPMatrixBeautify.put(mScaleMatrix);
//        mMVPMatrixBeautify.position(0);

        return true;
    }

    public static void Release() {
        if (mVertexBuffer != 0) {
            GLES31.glDeleteBuffers(1, new int[]{mVertexBuffer}, 0);
            mVertexBuffer = 0;
        }

        if (mBeautifyFilterProgram != 0) {
            GLES31.glDeleteProgram(mBeautifyFilterProgram);
            mBeautifyFilterProgram = 0;
        }
    }

    public void RenderTexture(int texID, int fboWidth, int fboHeight, int gammaTex, int maskTex, int useCartoon) {
        if (mOffScreenFrameBuffer != -1 && mOffScreenRenderBuffer != -1) {
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, mOffScreenFrameBuffer);
            GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, mOffScreenRenderBuffer);
        }

        GLES31.glUseProgram(mBeautifyFilterProgram);

        GLES31.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT);

        GLES31.glViewport(0, 0, fboWidth, fboHeight);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texID);
        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texID);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mBeautifyFilterProgram, "sTexture"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTex);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mBeautifyFilterProgram, "sMaskTexture"), 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gammaTex);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mBeautifyFilterProgram, "sGammaTexture"), 2);

        int positionHandle = GLES31.glGetAttribLocation(mBeautifyFilterProgram, "vPosition");
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, mVertexBuffer);
//        GLES31.glVertexAttribPointer(ph, 2, GL_FLOAT, false, 4 * 2, mGLVertexCartoon);
        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 0, 0);
        GLES31.glEnableVertexAttribArray(positionHandle);

        GLES31.glUniformMatrix4fv(GLES31.glGetUniformLocation(mBeautifyFilterProgram, "uMVPMatrix"), 1, false, mMVPMatrixBeautify);

        float beuatyRate = SFTunner.sfCommonTune.mBeautyRate;
        GLES31.glUniform1f(GLES31.glGetUniformLocation(mBeautifyFilterProgram, "intensity"), beuatyRate); //TODO : 값조정
        //GLES31.glUniform1f(GLES31.glGetUniformLocation(mBeautifyFilterProgram, "intensity"), BEAUTIFY_INTENSITY); //TODO : 값조정
        GLES31.glUniform2f(GLES31.glGetUniformLocation(mBeautifyFilterProgram, "imageStep"), BEAUTIFY_MUL / fboWidth, BEAUTIFY_MUL / fboHeight);
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mBeautifyFilterProgram, "uUseCartoon"), useCartoon);

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, 4);

        GLES31.glDisableVertexAttribArray(positionHandle);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, 0);

        GLES31.glUseProgram(0);

        if (mOffScreenFrameBuffer != -1 && mOffScreenRenderBuffer != -1) {
            GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0);
            GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, 0);
        }
    }

    public void SetFramebuffer(int fboId) {
        mOffScreenFrameBuffer = fboId;
    }

    public void SetRenderbuffer(int rboId) {
        mOffScreenRenderBuffer = rboId;
    }
}