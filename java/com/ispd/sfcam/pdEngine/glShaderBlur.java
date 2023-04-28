package com.ispd.sfcam.pdEngine;

public class glShaderBlur {
    public static final String SOURCE_DRAW_VS_GAUSSIAN_COMMON = "" +
//            "void main(void)\n" +
//            "{\n" +
//            "	 int basicBlur = 5;\n" +
//            "    vec2 texCoordFlip = vec2(texCoord.x, 1.0 - texCoord.y);\n" +
//            "    vec4 mask_color = texture2D(sMaskTexture, texCoordFlip);\n" +
//            "	 vec4 original_color = vec4(0.0, 0.0, 0.0, 0.0);\n" +
//            "	 vec4 blured_color = vec4(0.0, 0.0, 0.0, 0.0);\n" +
//
//            "   vec4 imageOri;\n" +
//            "   vec4 imageFbo;\n" +
//            "   if( uUseOes == 1 ) {\n" +
//            "       imageOri = texture2D(sTextureOriOes, texCoordFlip);\n" +
//            "       imageFbo = texture2D(sTextureOes, texCoordFlip);\n" +
//            "   }\n" +
//            "   else {\n" +
//            "       imageOri = texture2D(sTextureOriNm, texCoordFlip);\n" +
//            "       imageFbo = texture2D(sTextureNm, texCoordFlip);\n" +
//            "   }\n" +

            //sFeatherTexture
            "	     float moreBlur = 1.0;\n" +
            "       if( mask_color.r > 0.03 ) {\n" +
            "           if( mask_color.r > 0.9 && uIterations <= uOfValue7.x ) {\n" +
            "	 				basicBlur = uOfValue7.y;\n" +
//            "               gl_FragColor  = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
//            "               gl_FragColor.a = 1.0;\n" +

            //인물 일 때만 이 루틴 탐. 물체는 안 탐. 그래서 특별 처리함.
            "               vec4 feather = texture2D(sFeatherTexture, texCoordFlip);\n" +
            "               if(feather.r > 0.0) {\n" +
            "                   vec4 blurColor  = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            //"                   vec4 original_color = texture2D(sTexture, texCoordFlip);\n" +
            //"                   gl_FragColor = mix(original_color, blurColor, feather.r);\n" + //sally
            "                   gl_FragColor = blurColor;\n" +
            "                   gl_FragColor.a = 1.0;\n" +
            "               }\n" +
            "               else {\n" +
            //"                   gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0);\n" + //sally
            "                   gl_FragColor = imageFbo;\n" + //sally
            "               }\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.8 && uIterations <= uOfValue6.x ) {\n" +
            "	 				basicBlur = uOfValue6.y;\n" +
            "               gl_FragColor  = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "               gl_FragColor.a = 1.0;\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.7 && uIterations <= uOfValue5.x ) {\n" +
            "	 				basicBlur = uOfValue5.y;\n" +
            "               gl_FragColor  = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "               gl_FragColor.a = 1.0;\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.5 && uIterations <= uOfValue4.x ) {\n" +
            "	 				basicBlur = uOfValue4.y;\n" +
            "               gl_FragColor  = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "               gl_FragColor.a = 1.0;\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.4 && uIterations <= uOfValue3.x ) {\n" +
            "	 				basicBlur = uOfValue3.y;\n" +
            "               gl_FragColor  = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "               gl_FragColor.a = 1.0;\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.3  && uIterations <= uOfValue2.x ) {\n" +
            "	 				basicBlur = uOfValue2.y;\n" +
            "               gl_FragColor  = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "               gl_FragColor.a = 1.0;\n" +
            "           }\n" +
            "           else if ( mask_color.r > 0.2 ){\n" +
            "               gl_FragColor = imageFbo;\n" +
            "               gl_FragColor.a = 0.01;\n" +
            "           }\n" +
            //nothing to do...
            "           else if ( mask_color.r > 0.145 ){\n" +
            "               gl_FragColor = imageFbo;\n" +
            "               gl_FragColor.a = 0.01;\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.12 ){\n" +
            "               if(uDebugMode == 0) {\n" +
            "				if( uIterations <= uOfValue7.x ) {\n" +
            "	 				basicBlur = uOfValue7.y;\n" +
            "               	original_color = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "				}\n" +
            "				else {\n" +
            "					original_color = imageFbo;\n" +
            "				}\n" +
            "				if( uIterations <= uOfValue6.x ) {\n" +
            "	 				basicBlur = uOfValue6.y;\n" +
            "               	blured_color = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "				}\n" +
            "				else {\n" +
            "					blured_color = imageFbo;\n" +
            "				}\n" +

//            "               original_color.rgb *= mask_color.b;\n" +
//            "               blured_color.rgb *= (1.0 - mask_color.b);\n" +
//            "               gl_FragColor = original_color + blured_color;\n" +
//            "               gl_FragColor.a = 1.0;\n" +
            "               gl_FragColor = vec4(mix(blured_color.rgb, original_color.rgb, mask_color.b), 1.0);\n" + //sally

            "               }\n" +
            "				else {\n" +
            "					if( uIterations <= uOfValue7.x ) {\n" +
            "	 					basicBlur = uOfValue7.y;\n" +
            "						gl_FragColor = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "					}\n" +
            "					else {\n" +
            "						gl_FragColor = imageFbo;\n" +
            "					}\n" +
            "					gl_FragColor.a = 1.0;\n" +
            "				}\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.10 ){\n" +
            "               if(uDebugMode == 0) {\n" +
            "				if( uIterations <= uOfValue6.x ) {\n" +
            "	 				basicBlur = uOfValue6.y;\n" +
            "					original_color = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "				}\n" +
            "				else {\n" +
            "					original_color = imageFbo;\n" +
            "				}\n" +
            "				if( uIterations <= uOfValue5.x ) {\n" +
            "	 				basicBlur = uOfValue5.y;\n" +
            "					blured_color = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "				}\n" +
            "				else {\n" +
            "					blured_color = imageFbo;\n" +
            "				}\n" +

//            "               original_color.rgb *= mask_color.b;\n" +
//            "               blured_color.rgb *= (1.0 - mask_color.b);\n" +
//            "				  gl_FragColor = original_color + blured_color;\n" +
//            "				  gl_FragColor.a = 1.0;\n" +
            "               gl_FragColor = vec4(mix(blured_color.rgb, original_color.rgb, mask_color.b), 1.0);\n" + //sally
            "				}\n" +
            "               else {\n" +
            "					if( uIterations <= uOfValue6.x ) {\n" +
            "	 					basicBlur = uOfValue6.y;\n" +
            "						gl_FragColor = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "					}\n" +
            "					else {\n" +
            "               		gl_FragColor = imageFbo;\n" +
            "					}\n" +
            "                   gl_FragColor.a = 1.0;\n" +
            "               }\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.08 ){\n" +
            "               if(uDebugMode == 0) {\n" +
            "				if( uIterations <= uOfValue5.x ) {\n" +
            "	 				basicBlur = uOfValue5.y;\n" +
            "					original_color = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "				}\n" +
            "				else {\n" +
            "					original_color = imageFbo;\n" +
            "				}\n" +
            "				if( uIterations <= uOfValue4.x ) {\n" +
            "	 				basicBlur = uOfValue4.y;\n" +
            "					blured_color = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "				}\n" +
            "				else {\n" +
            "					blured_color = imageFbo;\n" +
            "				}\n" +

//            "               original_color.rgb *= mask_color.b;\n" +
//            "               blured_color.rgb *= (1.0 - mask_color.b);\n" +
//            "				  gl_FragColor = original_color + blured_color;\n" +
//            "				  gl_FragColor.a = 1.0;\n" +
            "               gl_FragColor = vec4(mix(blured_color.rgb, original_color.rgb, mask_color.b), 1.0);\n" + //sally
            "				}\n" +
            "				else {\n" +
            "					if( uIterations <= uOfValue5.x ) {\n" +
            "	 					basicBlur = uOfValue5.y;\n" +
            "						gl_FragColor = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "					}\n" +
            "					else {\n" +
            "						gl_FragColor = imageFbo;\n" +
            "					}\n" +
            "					gl_FragColor.a = 1.0;\n" +
            "				}\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.06 ){\n" +
            "               if(uDebugMode == 0) {\n" +
            "				if( uIterations <= uOfValue4.x ) {\n" +
            "	 				basicBlur = uOfValue4.y;\n" +
            "					original_color = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "				}\n" +
            "				else {\n" +
            "					original_color = imageFbo;\n" +
            "				}\n" +
            "				if( uIterations <= uOfValue3.x ) {\n" +
            "	 				basicBlur = uOfValue3.y;\n" +
            "					blured_color = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "				}\n" +
            "				else {\n" +
            "					blured_color = imageFbo;\n" +
            "				}\n" +

//            "               original_color.rgb *= mask_color.b;\n" +
//            "               blured_color.rgb *= (1.0 - mask_color.b);\n" +
//            "				  gl_FragColor = original_color + blured_color;\n" +
//            "				  gl_FragColor.a = 1.0;\n" +
            "               gl_FragColor = vec4(mix(blured_color.rgb, original_color.rgb, mask_color.b), 1.0);\n" + //sally

            "				}\n" +
            "				else {\n" +
            "					if( uIterations <= uOfValue4.x ) {\n" +
            "	 					basicBlur = uOfValue4.y;\n" +
            "						gl_FragColor = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "					}\n" +
            "					else {\n" +
            "						gl_FragColor = imageFbo;\n" +
            "					}\n" +
            "					gl_FragColor.a = 1.0;\n" +
            "				}\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.04 ){\n" +
            "               if(uDebugMode == 0) {\n" +
            "				if( uIterations <= uOfValue3.x ) {\n" +
            "	 				basicBlur = uOfValue3.y;\n" +
            "					original_color = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "				}\n" +
            "				else {\n" +
            "					original_color = imageFbo;\n" +
            "				}\n" +
            "				if( uIterations <= uOfValue2.x ) {\n" +
            "	 				basicBlur = uOfValue2.y;\n" +
            "					blured_color = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "				}\n" +
            "				else {\n" +
            "					blured_color = imageFbo;\n" +
            "				}\n" +

//            "               original_color.rgb *= mask_color.b;\n" +
//            "               blured_color.rgb *= (1.0 - mask_color.b);\n" +
//            "				  gl_FragColor = original_color + blured_color;\n" +
//            "				  gl_FragColor.a = 1.0;\n" +
            "               gl_FragColor = vec4(mix(blured_color.rgb, original_color.rgb, mask_color.b), 1.0);\n" + //sally
            "				}\n" +
            "				else {\n" +
            "					if( uIterations <= uOfValue3.x ) {\n" +
            "	 					basicBlur = uOfValue3.y;\n" +
            "						gl_FragColor = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "					}\n" +
            "					else {\n" +
            "						gl_FragColor = imageFbo;\n" +
            "					}\n" +
            "					gl_FragColor.a = 1.0;\n" +
            "				}\n" +
            "           }\n" +
            "           else if( mask_color.r > 0.02 ){\n" +
            "               if(uDebugMode == 0) {\n" +

            "					original_color = imageFbo;\n" +

            "				if( uIterations <= uOfValue2.x ) {\n" +
            "	 				basicBlur = uOfValue2.y;\n" +
            "					blured_color = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +

            "                   if( uIterations == uOfValue2.x ) {\n" +
            "                       original_color = imageOri;\n" +

//            "                       original_color.rgb *= (1.0 - mask_color.b);\n" +
//            "                       blured_color.rgb *= mask_color.b;\n" +
//            "                       gl_FragColor = original_color + blured_color;\n" +
//            "                       gl_FragColor.a = 1.0;\n" +
            "                       gl_FragColor = vec4(mix(original_color.rgb, blured_color.rgb, mask_color.b), 1.0);\n" + //sally
            "                   }\n" +
            "                   else {\n" +
            "                       gl_FragColor = blured_color;\n" +
            "                       gl_FragColor.a = 1.0;\n" +
            "                   }\n" +
            "				}\n" +
            "				else {\n" +
            "					gl_FragColor = imageFbo;\n" +
            "                  gl_FragColor.a = 1.0;\n" +
            "				}\n" +
            "               }\n" +
            "               else {\n" +
            "					if( uIterations <= uOfValue2.x ) {\n" +
            "	 					basicBlur = uOfValue2.y;\n" +
            "						gl_FragColor = calcBlur(texCoordFlip, basicBlur, moreBlur);\n" +
            "					}\n" +
            "					else {\n" +
            "               		gl_FragColor = imageFbo;\n" +
            "					}\n" +
            "                   gl_FragColor.a = 1.0;\n" +
            "               }\n" +
            "           }\n" +
            "           else { \n" +
            "               gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
            "           }\n" +
            "       }\n" +
            "       else {\n" +
            //"           gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
            "           gl_FragColor = imageOri;\n" +
            "           gl_FragColor.a = 0.01;\n" +
            "       }\n" +
            "}";

    public static final String SOURCE_DRAW_VS_GAUSSIAN_HORIZON_INIT = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +
            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_GAUSSIAN_HORIZON_INIT = "" +
            "#extension GL_OES_EGL_image_external : require\n" +

            //"uniform sampler2D image;\n" +

            //"precision mediump float;\n" +
            "precision highp float;\n" +
            "uniform samplerExternalOES sTextureOriOes;\n" +
            "uniform sampler2D sTextureOriNm;\n" +
            "uniform samplerExternalOES sTextureOes;\n" +
            "uniform sampler2D sTextureNm;\n" +
            "uniform sampler2D sMaskTexture;\n" +
            "uniform sampler2D sFeatherTexture;\n" +
            "varying vec2 texCoord;\n" +

            "uniform int uFront;\n" +
            "uniform int uDebugMode;\n" +
            "uniform ivec2 uOfValue7;\n" +
            "uniform ivec2 uOfValue6;\n" +
            "uniform ivec2 uOfValue5;\n" +
            "uniform ivec2 uOfValue4;\n" +
            "uniform ivec2 uOfValue3;\n" +
            "uniform ivec2 uOfValue2;\n" +
            "uniform int uIterations;\n" +
            "uniform float ublurWidth;\n" +
            "uniform float ublurHeight;\n" +

            "uniform int uUseOes;\n" +
            "vec4 imageOri;\n" +
            "vec4 imageFbo;\n" +

            //box blur for gaussian
            "vec4 calcBlur(vec2 tex_coord, int kSize, float moreBlur) {\n" +
            "   int maskSize = kSize;\n" +

            "   if( maskSize == 1 ) {\n" +
            "       return imageFbo;\n" +
            "   }\n" +
            "   vec4 sum = vec4(0.0, 0.0, 0.0, 0.0);\n" +

            "   int i, j;\n" +
            "   #pragma unroll\n" +
            "   for( i = -maskSize/2; i <= maskSize/2; i++) {\n" +
            "		vec2 newTexCoord = vec2(tex_coord.x + float(i)*ublurWidth*moreBlur, tex_coord.y);\n" +
            "       if( uUseOes == 1 ) {\n" +
            "		    sum.rgb += texture2D(sTextureOes, newTexCoord).rgb;\n" +
            "       }\n" +
            "       else {\n" +
            "		    sum.rgb += texture2D(sTextureNm, newTexCoord).rgb;\n" +
            "       }\n" +
//            "		sum.rgb += vec3(1.0, 0.0, 1.0);\n" +
            "   }\n" +
            "	 sum.rgb = sum.rgb/float(maskSize);\n" +
            "   return sum;\n" +
            "}\n" +

            "void main(void)\n" +
            "{\n" +
            "	 int basicBlur = 5;\n" +
            "    vec2 texCoordFlip = vec2(texCoord.x, 1.0 - texCoord.y);\n" +
            "    vec4 mask_color = texture2D(sMaskTexture, texCoordFlip);\n" +
            "	 vec4 original_color = vec4(0.0, 0.0, 0.0, 0.0);\n" +
            "	 vec4 blured_color = vec4(0.0, 0.0, 0.0, 0.0);\n" +

            "   if( uUseOes == 1 ) {\n" +
            "       imageOri = texture2D(sTextureOriOes, texCoordFlip);\n" +
            "       imageFbo = texture2D(sTextureOes, texCoordFlip);\n" +
            "   }\n" +
            "   else {\n" +
            "       imageOri = texture2D(sTextureOriNm, texCoordFlip);\n" +
            "       imageFbo = texture2D(sTextureNm, texCoordFlip);\n" +
            "   }\n" +

            SOURCE_DRAW_VS_GAUSSIAN_COMMON;

    public static final String SOURCE_DRAW_VS_GAUSSIAN_HORIZON = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +
            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_GAUSSIAN_HORIZON = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            //"#version 330 core\n" +

            //"precision mediump float;\n" +
            "precision highp float;\n" +
            "uniform samplerExternalOES sTextureOriOes;\n" +
            "uniform sampler2D sTextureOriNm;\n" +
            "uniform samplerExternalOES sTextureOes;\n" +
            "uniform sampler2D sTextureNm;\n" +
            "uniform sampler2D sMaskTexture;\n" +
            "uniform sampler2D sFeatherTexture;\n" +
            "varying vec2 texCoord;\n" +

            "uniform int uFront;\n" +
            "uniform int uDebugMode;\n" +
            "uniform ivec2 uOfValue7;\n" +
            "uniform ivec2 uOfValue6;\n" +
            "uniform ivec2 uOfValue5;\n" +
            "uniform ivec2 uOfValue4;\n" +
            "uniform ivec2 uOfValue3;\n" +
            "uniform ivec2 uOfValue2;\n" +
            "uniform int uIterations;\n" +
            "uniform float ublurWidth;\n" +
            "uniform float ublurHeight;\n" +

            "uniform int uUseOes;\n" +
            "vec4 imageOri;\n" +
            "vec4 imageFbo;\n" +

            //box blur for gaussian
            "vec4 calcBlur(vec2 tex_coord, int kSize, float moreBlur) {\n" +
            "	int maskSize = kSize;\n" +

            "   if( maskSize == 1 ) {\n" +
            "       return imageFbo;\n" +
            "   }\n" +
            "   vec4 sum = vec4(0.0, 0.0, 0.0, 0.0);\n" +

            "	int i, j;\n" +
            "	#pragma unroll\n" +
            "	for( i = -maskSize/2; i <= maskSize/2; i++) {\n" +
            "		vec2 newTexCoord = vec2(tex_coord.x + float(i)*ublurWidth*moreBlur, tex_coord.y);\n" +
            "		sum.rgb += texture2D(sTextureNm, newTexCoord).rgb;\n" +
            "	}\n" +
            "	 sum.rgb /= float(maskSize);\n" +
            "	return sum;\n" +
            "}\n" +

            "void main(void)\n" +
            "{\n" +
            "	 int basicBlur = 5;\n" +
            "    vec2 texCoordFlip = vec2(texCoord.x, 1.0 - texCoord.y);\n" +
            "    vec4 mask_color = texture2D(sMaskTexture, texCoordFlip);\n" +
            "	 vec4 original_color = vec4(0.0, 0.0, 0.0, 0.0);\n" +
            "	 vec4 blured_color = vec4(0.0, 0.0, 0.0, 0.0);\n" +

            "   if( uUseOes == 1 ) {\n" +
            "       imageOri = texture2D(sTextureOriOes, texCoordFlip);\n" +
            //"       imageFbo = texture2D(sTextureOes, texCoordFlip);\n" +
            "       imageFbo = texture2D(sTextureNm, texCoordFlip);\n" +
            "   }\n" +
            "   else {\n" +
            "       imageOri = texture2D(sTextureOriNm, texCoordFlip);\n" +
            "       imageFbo = texture2D(sTextureNm, texCoordFlip);\n" +
            "   }\n" +

            SOURCE_DRAW_VS_GAUSSIAN_COMMON;

    public static final String SOURCE_DRAW_VS_GAUSSIAN_VERTICAL = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +
            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_GAUSSIAN_VERTICAL = "" +
            "#extension GL_OES_EGL_image_external : require\n" +

            //"precision mediump float;\n" +
            "precision highp float;\n" +
            "uniform samplerExternalOES sTextureOriOes;\n" +
            "uniform sampler2D sTextureOriNm;\n" +
            "uniform samplerExternalOES sTextureOes;\n" +
            "uniform sampler2D sTextureNm;\n" +
            "uniform sampler2D sMaskTexture;\n" +
            "uniform sampler2D sFeatherTexture;\n" +
            "varying vec2 texCoord;\n" +

            "uniform int uFront;\n" +
            "uniform int uDebugMode;\n" +
            "uniform ivec2 uOfValue7;\n" +
            "uniform ivec2 uOfValue6;\n" +
            "uniform ivec2 uOfValue5;\n" +
            "uniform ivec2 uOfValue4;\n" +
            "uniform ivec2 uOfValue3;\n" +
            "uniform ivec2 uOfValue2;\n" +
            "uniform int uIterations;\n" +
            "uniform float ublurWidth;\n" +
            "uniform float ublurHeight;\n" +

            "uniform int uUseOes;\n" +
            "vec4 imageOri;\n" +
            "vec4 imageFbo;\n" +

            //box blur for gaussian
            "vec4 calcBlur(vec2 tex_coord, int kSize, float moreBlur) {\n" +
            "   int maskSize = kSize;\n" +

            "   if( maskSize == 1 ) {\n" +
            "       return imageFbo;\n" +
            "   }\n" +
            "   vec4 sum = vec4(0.0, 0.0, 0.0, 0.0);\n" +

            "   int i, j;\n" +
            "   #pragma unroll\n" +
            "   for( j = -maskSize/2; j <= maskSize/2; j++) {\n" +
            "		vec2 newTexCoord = vec2(tex_coord.x, tex_coord.y + float(j)*ublurHeight*moreBlur);\n" +
            "		sum.rgb += texture2D(sTextureNm, newTexCoord).rgb;\n" +
            "   }\n" +
            "	 sum.rgb /= float(maskSize);\n" +
            "   return sum;\n" +
            "}\n" +

            "void main(void)\n" +
            "{\n" +
            "	 int basicBlur = 5;\n" +
            "    vec2 texCoordFlip = vec2(texCoord.x, 1.0 - texCoord.y);\n" +
            "    vec4 mask_color = texture2D(sMaskTexture, texCoordFlip);\n" +
            "	 vec4 original_color = vec4(0.0, 0.0, 0.0, 0.0);\n" +
            "	 vec4 blured_color = vec4(0.0, 0.0, 0.0, 0.0);\n" +

            "   if( uUseOes == 1 ) {\n" +
            "       imageOri = texture2D(sTextureOriOes, texCoordFlip);\n" +
            //"       imageFbo = texture2D(sTextureOes, texCoordFlip);\n" +
            "       imageFbo = texture2D(sTextureNm, texCoordFlip);\n" +
            "   }\n" +
            "   else {\n" +
            "       imageOri = texture2D(sTextureOriNm, texCoordFlip);\n" +
            "       imageFbo = texture2D(sTextureNm, texCoordFlip);\n" +
            "   }\n" +

            SOURCE_DRAW_VS_GAUSSIAN_COMMON;

    public static final String SOURCE_DRAW_VS_GAUSSIAN_RESULT = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +

            "uniform mat4 uMVPMatrix;\n" +

            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_GAUSSIAN_RESULT = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            //"precision mediump float;\n" +
            "precision highp float;\n" +
            "uniform samplerExternalOES sTextureOriOes;\n" +
            "uniform sampler2D sTextureOriNm;\n" +
            "uniform sampler2D sTexture;\n" +
            "uniform sampler2D sMaskTexture;\n" +
            "uniform sampler2D sSegmentTexture;\n" +

            "uniform int uUseOes;\n" +
            "uniform int uUseFbo;\n" +
            "uniform int uObjAlg;\n" +
            "uniform int uFront;\n" +
            "uniform int uDebugMode;\n" +
            "uniform int uSaveStatus;\n" +

            "varying vec2 texCoord;\n" +

            "void main() {\n" +
            "  vec2 tex_coord;\n" +
            "  if( uFront == 1 ) {\n" +
            "       if( uUseFbo > 0 ) {\n"+
            "           tex_coord = vec2(texCoord.x, 1.0-texCoord.y);\n" +
            "       }\n"+
            "       else {\n"+
            "           tex_coord = vec2(1.0-texCoord.x, texCoord.y);\n" +
            "       }\n"+
            "  }\n" +
            "  else {\n" +
            "       if( uUseFbo > 0 ) {\n"+
            "           tex_coord = vec2(texCoord.x, 1.0-texCoord.y);\n" +
            "       }\n"+
            "       else {\n"+
            "           tex_coord = texCoord;\n" +
            "       }\n"+
            "  }\n" +

            "      vec4 oriColor;\n" +
            "      if( uUseOes == 1 ) {\n" +
            "           oriColor = texture2D(sTextureOriOes, tex_coord);\n" +
            "      }\n" +
            "      else {\n" +
            "           oriColor = texture2D(sTextureOriNm, tex_coord);\n" +
            "      }\n" +

            "		if( uSaveStatus == 1 ) {\n" +
            "			gl_FragColor = texture2D(sTexture, tex_coord) * 0.5;\n" +
            "		}\n" +
            "		else {\n" +
            "       	vec4 color = texture2D(sTexture, tex_coord);\n" +
            "       	vec4 mask_color = texture2D(sMaskTexture, tex_coord);\n" +

            "         if( uObjAlg == 1 ) {\n" +
            "       	    if( color.a > 0.02 ) {\n" +
            "           	    gl_FragColor  = vec4(color.r, color.g, color.b, 1.0);\n" +
            "       	    }\n" +
            "       	    else {\n" +
            "           	    vec4 color = oriColor;\n" +
            "         	  	    gl_FragColor  = color;\n" +
            //"         	  	gl_FragColor = vec4(0.0, 0.0, 1.0, 1.0);\n" +
            "       	    }\n" +
//            "           	    gl_FragColor  = oriColor;\n" +
            "       	}\n" +
            "       	else {\n" +
//            "         	  	gl_FragColor  = color;\n" +
            "           	vec4 original = oriColor;\n" +
//            "         	gl_FragColor  = original * (1.0 - mask_color.r) + color * mask_color.r;\n" +
            "             gl_FragColor = vec4(mix(original.rgb, color.rgb, mask_color.r), 1.0);\n" + //sally
            "       	}\n" +

            "		}\n" +

            //"   vec4 seg_color = texture2D(sSegmentTexture, tex_coord);\n" +
            "   vec4 seg_color = texture2D(sMaskTexture, tex_coord);\n" +
            "       if( uDebugMode == 1 ) {\n" +
            "         if( uObjAlg == 0 ) {\n" +
            "   		    if( seg_color.r > 0.0 ) {\n" +
            "           	    vec4 color = oriColor;\n" +
//            "           		vec4 result = color*0.5 + seg_color*0.5;\n" +
            "           		vec4 result = vec4(mix(color.rgb, seg_color.rgb, 0.5), 1.0);\n" + //sally

            "           	    gl_FragColor = vec4(0.0, 0.0, result.r, 0.0);\n" +
            "   		    }\n" +
            "         }\n" +
            "         else {\n" +
//            "   		    if( seg_color.r > 0.0 || seg_color.b > 0.0 ) {\n" +
//            "           	    vec4 color = oriColor;\n" +
//            "           	    vec4 result = color*0.5 + seg_color*0.5;\n" +
//            "           	    gl_FragColor = vec4(seg_color.r, 0.0, result.b, 0.0);\n" +
//            "   		    }\n" +
//            "   		    if( seg_color.g > 0.0 ) {\n" +
//            "           	    vec4 color = oriColor;\n" +
////            "           	    vec4 result = color*0.5 + seg_color*0.5;\n" +
//            "           	    vec4 result = vec4(mix(color.rgb, seg_color.rgb, 0.5), 1.0);\n" + //sally
//            "           	    gl_FragColor = vec4(0.0, result.g, 0.0, 0.0);\n" +
//            "   		    }\n" +

            "   		    if( 0.14 < seg_color.r && seg_color.r < 0.16 ) {\n" +
            "           	    vec4 color = oriColor;\n" +
            "           	    vec4 result = color*0.5 + seg_color*5.0*0.5;\n" +
            "           	    gl_FragColor = vec4(result.r, 0.0, 0.0, 1.0);\n" +
            "   		    }\n" +

            "   		    if( seg_color.b > 0.0 ) {\n" +
            "           	    vec4 color = oriColor;\n" +
            "           	    vec4 result = color*0.5 + seg_color*0.5;\n" +
            "           	    gl_FragColor = vec4(0.0, 0.0, result.b, 1.0);\n" +
            "   		    }\n" +

            "         }\n" +
            "       }\n" +
            "}";

    public static final String SOURCE_DRAW_VS_SAVE_GAUSSIAN_HORIZON = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +
            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_SAVE_GAUSSIAN_HORIZON = "" +
            //"precision mediump float;\n" +
            "precision highp float;\n" +
            //"uniform samplerExternalOES sTextureOri;\n" +
            "uniform sampler2D sTextureOri;\n" +
            "uniform sampler2D sTexture;\n" +
            "uniform sampler2D sMaskTexture;\n" +
            "varying vec2 texCoord;\n" +

            "uniform int uFront;\n" +
            "uniform int uDebugMode;\n" +
            "uniform ivec2 uOfValue7;\n" +
            "uniform ivec2 uOfValue6;\n" +
            "uniform ivec2 uOfValue5;\n" +
            "uniform ivec2 uOfValue4;\n" +
            "uniform ivec2 uOfValue3;\n" +
            "uniform ivec2 uOfValue2;\n" +
            "uniform int uIterations;\n" +
            "uniform float ublurWidth;\n" +
            "uniform float ublurHeight;\n" +

            //box blur for gaussian
            "vec4 calcBlur(vec2 tex_coord, int kSize, float moreBlur) {\n" +
            //"	float width = 1.0/float(float(uWidth)/uResize);\n" +
            "	int maskSize = kSize;\n" +
            "	vec4 sum = vec4(0.0, 0.0, 0.0, 0.0);\n" +
            "	int i, j;\n" +
            "	#pragma unroll\n" +
            "	for( i = -maskSize/2; i <= maskSize/2; i++) {\n" +
            "		vec2 newTexCoord = vec2(tex_coord.x + float(i)*ublurWidth*moreBlur, tex_coord.y);\n" +
            "		sum.rgb += texture2D(sTexture, newTexCoord).rgb;\n" +
            "	}\n" +
            "	 sum.rgb /= float(maskSize);\n" +
            "	return sum;\n" +
            "}\n" +

            SOURCE_DRAW_VS_GAUSSIAN_COMMON;

    public static final String SOURCE_DRAW_VS_SAVE_GAUSSIAN_VERTICAL = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +
            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS_SAVE_GAUSSIAN_VERTICAL = "" +
            //"precision mediump float;\n" +
            "precision highp float;\n" +
            //"uniform samplerExternalOES sTextureOri;\n" +
            "uniform sampler2D sTextureOri;\n" +
            "uniform sampler2D sTexture;\n" +
            "uniform sampler2D sMaskTexture;\n" +
            "varying vec2 texCoord;\n" +

            "uniform int uFront;\n" +
            "uniform int uDebugMode;\n" +
            "uniform ivec2 uOfValue7;\n" +
            "uniform ivec2 uOfValue6;\n" +
            "uniform ivec2 uOfValue5;\n" +
            "uniform ivec2 uOfValue4;\n" +
            "uniform ivec2 uOfValue3;\n" +
            "uniform ivec2 uOfValue2;\n" +
            "uniform int uIterations;\n" +
            "uniform float ublurWidth;\n" +
            "uniform float ublurHeight;\n" +

            //box blur for gaussian
            "vec4 calcBlur(vec2 tex_coord, int kSize, float moreBlur) {\n" +
            "   int maskSize = kSize;\n" +
            "   vec4 sum = vec4(0.0, 0.0, 0.0, 0.0);\n" +
            "   int i, j;\n" +
            "   #pragma unroll\n" +
            "   for( j = -maskSize/2; j <= maskSize/2; j++) {\n" +
            "		vec2 newTexCoord = vec2(tex_coord.x, tex_coord.y + float(j)*ublurHeight*moreBlur);\n" +
            "		sum.rgb += texture2D(sTexture, newTexCoord).rgb;\n" +
            "   }\n" +
            "	 sum.rgb /= float(maskSize);\n" +
            "   return sum;\n" +
            "}\n" +
            SOURCE_DRAW_VS_GAUSSIAN_COMMON;
}
