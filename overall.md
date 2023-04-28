
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
  
  
### Filtering Live Video

  단안(싱글) 카메라 입력 영상으로 사물/배경 또는 인물/배경을 실시간으로 Semantic Segmentation(SFnet<sup>TM</sup>) 을 이용해 분리한 후 다양한 특수 효과를 적용함
  
  #### Linked Libraries
    
    - openCV 4.0.x   
    - openGLES 2.0
  
  #### Implemented Filters
  
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
  #### Demo Images
  
  <br>
  <div align="center">
  <img width="70%" src="https://github.com/iSPD/SFnet/blob/main/images/frontCamera.png"/>
  </div>
  </br>
  
  
