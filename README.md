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

### Object Detection

  <br>  
  <div align="left">
  <img width="35%" src="https://github.com/iSPD/SFnet/blob/main/images/obj.gif"/>
  </div>
  </br>
    
### 사용 라이브러리
  - OpenCV 4.0.x android sdk

  - OpenGLES 2.0(Shader)
  
### 사용모델
  - [ssd_mobilenet_v2_quantized_coco](https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/tf1_detection_zoo.md#:~:text=ssd_mobilenet_v2_quantized_coco)

### 기술 내용

  <img width="70%" src="https://github.com/iSPD/SFnet/blob/main/images/ObjectDetection.png"/>

  - Android preview callback buffer를 이용하여 Preview Data를 Object Detection Model에서 Inference(Minimum Confidence Rate : 0)하여 모든 객체 위치 검출.
  
  - Preview Data와 Object Detection에서 검출된 객체 위치를 이용하여 Jni(Java대비 속도 이슈 때문에 사용)에서 OpenCV를 이용하여 Target 객체 분석하여 정보 추출.

    - Preview와 Object Detection Box위치를 이용하여 **WaterShed**를 통해 영역 검출
    
    - 아래 **특허 2** 기술에 의해 최종 피사체 선택

    - 아래 **특허 1** 기술에 의해 Depth를 추출하여 Depth Mask 생성

  - Android Camera Preview를 OpenCV에서 분석된 정보로 이용하여, OpenGLES 2.0의 SurfaceTexture를 통해 Shader에 각종 Filter를 적용 후 화면에 그려줌.

    - Shader에 Depth Mask를 이용하여 Cartoon Effect, SF Moview Effect, OutFocus, HighLight Effect 효과 적용

    - 소스 코드 간략히 추가

  <br>  
  <div align="left">
  <img width="70%" src="https://github.com/iSPD/SFnet/blob/main/images/obj_detection.JPG"/>
  </div>
  </br>
  
### Advanced Semantic-Segmentation

  * 정확도 향상
  
  <div align="left">
  <img width="45%" src="https://github.com/iSPD/SFnet/blob/main/images/re-segmentation_1.JPG">  <img width="45%" src="https://github.com/iSPD/SFnet/blob/main/images/re-segmentation_2.JPG">
  </div>
  
  * 속도 향상
  
  <div align="left">
  <img width="45%" src="https://github.com/iSPD/SFnet/blob/main/images/speed_improved.JPG"> <img width="45%" src="https://github.com/iSPD/SFnet/blob/main/images/speed_improved2.JPG">
  </div>
  
### Filters

  #### Linked Libraries
    
    - openCV 4.0.x 
  
    - openGLES 2.0
  
  <br>
  <div align="left">
  <img width="70%" src="https://github.com/iSPD/SFnet/blob/main/images/frontCamera.png"/>
  </div>
  </br>
