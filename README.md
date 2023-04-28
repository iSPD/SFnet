# SFnet<sup>TM</sup>

S.F 영화 수준의 C.G 효과를 스마트폰에서 실시간 구현하기 위한 고속, 정밀 인공지능 Semantic Segmentation 모델(알고리즘)
<img width="90%" src="https://github.com/iSPD/SFnet/blob/main/images/SFCamIntro.png"/>

---

## 🕰️ **개발 기간**

- 2019년 6월 21일 ~ 2020년 6월 20일

---

## SFCam on Mobile with SFNet<sup>TM</sup>

<img width="40%" src="https://github.com/iSPD/SFnet/blob/main/images/backCamera.gif"/>    <img width="40%" src="https://github.com/iSPD/SFnet/blob/main/images/frontCamera.gif"/>

---

## S/C Depth Extraction(단안 카메라 심도 추출)

<img width="35%" src="https://github.com/iSPD/SFnet/blob/main/images/DepthObje.png"/>    <img width="58%" src="https://github.com/iSPD/SFnet/blob/main/images/DepthPersons.jpg"/>

---

## Motion Recognition

<img width="70%" src="https://github.com/iSPD/SFnet/blob/main/images/mediapipe.gif"/>

---
      
### 인물인식 (Advanced Semantic-Segmentation)

SFnet<sup>TM</sup> 은 semantic segmentation을 최적으로 수행하기 위해 DeepLab V3+ 모델에 Quantization, Output Resizing 을 적용한 후 아래 특허기술을 결합하여 고성능 semantic segmentation을 구현하였다.

  * 개발환경
    ```
    - TensorFlow-Lite   
    - openCV 4.0.x
    ```
    
  * 인물 세그멘테이션 정확도 향상 특허기술
  
    Semantic Segmentation을 이용한 인물 또는 사물 영역 분할 &rarr; 분할된 영역의 Scale-Down을 통한 Marker 생성 &rarr; 분할된 영역의 Scale-Up을 통한 Outer 생성 &rarr; Re-Segmentation을 위한 Marker와 Outer의 배치 &rarr; Marker 와 Outer 사이의 영역을 Re-Segmentation 하여 정확도를 향상하였다.

  <div align="center">
  <img width="45%" src="https://github.com/iSPD/SFnet/blob/main/images/re-segmentation_1.JPG">  <img width="45%" src="https://github.com/iSPD/SFnet/blob/main/images/re-segmentation_2.JPG">
  </div>
  
  * 속도 향상 특허기술
  
    다중 모델을 혼용하여 인공지능 Semantic Segmentation 모델의 속도, 정확도 개선
    
    - 다중 모델 병합 = 고속/저정확도 모델 + 저속/고정확도 모델
    
    - 피사체 움직임 감지 &rarr; 고속/저정확도 모델 사용, 피사체 움직임 미감지 &rarr; 저속/고정확도 모델 사용 
    
  <div align="center">
  <img width="45%" src="https://github.com/iSPD/SFnet/blob/main/images/speed_improved.JPG"> <img width="45%" src="https://github.com/iSPD/SFnet/blob/main/images/speed_improved2.JPG">
  </div>

---

## Filter Effect Using OpenGL ES2.0 Shader

  - `Cartoon Filter` : 인물 또는 배경의 만화 효과.  GL Shader 기반 Cartoon Effect 로써 bilateral blur filter를 가로, 세로로 단독 1회씩 적용 후, blur factor를 fixed value 화 하여 rendering 속도를 높임 (55msec/frame).
  
  - `Studio Effect` : 인물과 배경이미지 합성 효과. 인물/배경 합성 시, edge 처리를 위한 GL Shader 기반 Feathering 개발. lerp blur filter를 이진화된 세그멘테이션 영역에 적용 후 세그멘테이션 인물에 fragment shader 합성.
  
  - `Beauty Filter` : 화사한 피부 효과
  
  - `OutFocus Effect` : 인물/사물의 배경을 흐릿하게 표현
  
  - `HightLight Effect` : 인물/사물의 배경을 어둡게 표현. 아이폰의 카메라 기능 중 무대조명 효과.  
  
  - Code Example (Beauty Filter)
    ``` JAVA
    public static final String SOURCE_DRAW_FS_BEAUTIFY_FILTER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform sampler2D sMaskTexture;\n" +
            "uniform sampler2D sGammaTexture;\n" +
            "uniform vec2 imageStep;\n" +
            "uniform float intensity;\n" +
            "uniform int uUseCartoon;\n" +
            "varying vec2 vTexCoord;\n" +

            "vec4 black_edge_effect(vec2 coord, vec4 color) {\n" +
            "      vec4 effect;\n" +
            "      float bk_rate = 1.0;\n" +

            "      effect.r = texture2D(sGammaTexture, vec2(color.r, 0.0)).r * bk_rate;\n" +
            "      effect.g = texture2D(sGammaTexture, vec2(color.g, 0.0)).g * bk_rate;\n" +
            "      effect.b = texture2D(sGammaTexture, vec2(color.b, 0.0)).b * bk_rate;\n" +
            "      effect.a = color.a;\n" +
            "      return effect;\n" +
            "}\n" +

            "void main()\n" +
            "{\n" +
            ...
    ```

## SFNet 기술 개발 개요

### 사용모델
- [ssd_mobilenet_v2_quantized_coco](https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/tf1_detection_zoo.md#:~:text=ssd_mobilenet_v2_quantized_coco)

<br>  
<div align="left">
<img width="70%" src="https://github.com/iSPD/SFnet/blob/main/images/obj_detection.JPG"/>
</div>
</br>

### 개발 언어
- Java

- C, C++

### 사용 라이브러리
- OpenCV 4.0.x android sdk

- OpenGLES 2.0(Shader)

### 기술 내용
- Android preview callback buffer를 이용하여 Preview Data를 Object Detection Model에서 Inference(Minimum Confidence Rate : 0)하여 모든 객체 위치 검출.

- Preview Data와 Object Detection에서 검출된 객체 위치를 이용하여 Jni(Java대비 속도 이슈 때문에 사용)에서 OpenCV를 이용하여 Target 객체 분석하여 정보 추출.

- Preview와 Object Detection Box위치를 이용하여 **WaterShed**를 통해 영역 검출

- 아래 **특허 2** 기술에 의해 최종 피사체 선택

- 아래 **특허 1** 기술에 의해 Depth를 추출하여 Depth Mask 생성

- Android Camera Preview를 OpenCV에서 분석된 정보로 이용하여, OpenGLES 2.0의 SurfaceTexture를 통해 Shader에 각종 Filter를 적용 후 화면에 그려줌.

- Shader에 Depth Mask를 이용하여 아래 필터 및 효과 적용

  - **`Cartoon효과`** : 사람만 Cartoon, 배경만 Cartoon

  - **`SF효과`** : 객체 및 사람의 배경을 다른 사진으로 변경

  - **`OutFocus효과`** : 객체 및 사람의 배경 Blur

  - **`HighLight효과`** : 객체 및 사람의 배경 어둡게. 아이폰 카메라에 있는 무대조명 효과

  - **`Edge필터`** : Cartoon효과에 사용

  - **`LerpBlur필터`** : 모든 효과에 Feather를 주어서 자연스럽게 함

  - **`Beauty필터`** : 사람 얼굴 아름답게 보정

- 소스 예제(Beauty필터)
```
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
        "      float bk_rate = 1.0;\n" +

        "      effect.r = texture2D(sGammaTexture, vec2(color.r, 0.0)).r * bk_rate;\n" +
        "      effect.g = texture2D(sGammaTexture, vec2(color.g, 0.0)).g * bk_rate;\n" +
        "      effect.b = texture2D(sGammaTexture, vec2(color.b, 0.0)).b * bk_rate;\n" +
        "      effect.a = color.a;\n" +
        "      return effect;\n" +
        "}\n" +

        "void main()\n" +
        "{\n" +
```
