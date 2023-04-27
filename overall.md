### SFCam on Mobile with SFNet<sup>TM</sup>

  - 데모화면 몇 개

### Object Detection
  
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
  
### Filtering Live Video

  #### Linked Libraries
    
    - openCV 4.0.x   
    - openGLES 2.0
  
  #### Implemented Filters
  - Cartoon Filter : 인물 또는 배경의 만화 효과.  GL Shader 기반 Cartoon Effect 로써 bilateral blur filter를 가로, 세로로 단독 1회씩 적용 후, blur factor를 fixed value 화 하여 rendering 속도를 높임 (55msec/frame).
  - Studio Effect : 인물과 배경이미지 합성 효과. 인물/배경 합성 시, edge 처리를 위한 GL Shader 기반 Feathering 개발. lerp blur filter를 이진화된 세그멘테이션 영역에 적용 후 세그멘테이션 인물에 fragment shader 합성.
  - Beauty Filter : 화사한 인물 효과
    
    
  <br>
  <div align="left">
  <img width="70%" src="https://github.com/iSPD/SFnet/blob/main/images/frontCamera.png"/>
  </div>
  </br>
  
  
