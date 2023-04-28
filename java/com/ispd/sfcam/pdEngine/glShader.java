package com.ispd.sfcam.pdEngine;

public class glShader {
    public static final String SOURCE_DRAW_VS_BASIC = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +

            "uniform mat4 uMVPMatrix;\n" +

            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_BASIC = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            //"precision highp float;\n" +

            "uniform samplerExternalOES sTexture;\n" +
            "varying vec2 texCoord;\n" +
            "uniform int uFront;\n" +

            "void main() {\n" +
            "   vec2 newTexCoord = texCoord;\n" +
            "   if( uFront == 1 ) {\n" +
            "       newTexCoord.x = 1.0 - newTexCoord.x;\n" +
            "   }\n" +

            "	 gl_FragColor = texture2D(sTexture, newTexCoord);\n" +
            "}";
}
