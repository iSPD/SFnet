/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.ispd.sfcam.AIEngineSegmentation.segmentation;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.os.Trace;

import com.ispd.sfcam.utils.Log;

import org.tensorflow.lite.Interpreter;
//import org.tensorflow.lite.experimental.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Vector;

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * github.com/tensorflow/models/tree/master/research/object_detection
 */
public class TFLiteObjectSegmentationAPIModel implements Segmentor {

  private String TAG = "AiCam-TFLiteObjectSegmentationAPIModel";

  // Float model
  private static final float IMAGE_MEAN = 128.0f;
  private static final float IMAGE_STD = 128.0f;
  private static final int MODEL_INPUT_SIZE_FOR_CAPTURE = 513;  //sally

  // Config values.
  private int inputWidth;
  private int inputHeight;
  private int numClass;
  public Vector<String> labels = new Vector<String>();

  // Pre-allocated buffers.
  private int[] intValues;
  private byte[] byteValues; //sally
  private byte[] byteValues2; //sally

  private float[][] pixelClassesFloat; //sally
  //private byte[][][][] pixelClasses;
  //this
  private long[][] pixelClasses;

  //private byte[][] pixelClasses;
  //private float[][][] pixelClasses;

  protected ByteBuffer imgData = null;
  protected ByteBuffer imgDataRgba = null; //sally

  private float[][] pixelClassesSort;
  //private byte[][] pixelClassesSort;

  private int outputWidth;
  private int outputHeight;

  private Interpreter tfLite;

  private int segCount = 0;
  private long meanSpeed = 0;

  /** Memory-map the model file in Assets. */
  private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
      throws IOException {
    AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
    FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
    FileChannel fileChannel = inputStream.getChannel();
    long startOffset = fileDescriptor.getStartOffset();
    long declaredLength = fileDescriptor.getDeclaredLength();
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
  }

  /**
   * Initializes a native TensorFlow session for classifying images.
   *
   * @param assetManager The asset manager to be used to load assets.
   * @param modelFilename The filepath of the model GraphDef protocol buffer.
   */
  public static Segmentor create(
      final AssetManager assetManager,
      final String modelFilename,
      final String labelFilename,
      final int inputWidth,
      final int inputHeight,
      final int numClass, final int numOutput) throws IOException {
    final TFLiteObjectSegmentationAPIModel d = new TFLiteObjectSegmentationAPIModel();

    d.inputWidth = inputWidth;
    d.inputHeight = inputHeight;
    d.numClass = numClass;

    d.outputWidth = inputWidth;
    d.outputHeight = inputHeight;
//    d.outputWidth = 32;
//    d.outputHeight = 32;


// CPU Version
    try {
      d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename));
      d.tfLite.setNumThreads(4);
      d.tfLite.setUseNNAPI(true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

//    // Ensure a valid EGL rendering context.
//    EGLContext eglContext = eglGetCurrentContext();
//    if (eglContext.equals(EGL_NO_CONTEXT)) //return;
//    {
//      Log.d("tensorflow-gpu", "error");
//    }
//
//    int inputSize = 1440*1080*4;
//
//// Create an SSBO.
//    int[] id = new int[1];
//    glGenBuffers(id.length, id, 0);
//    glBindBuffer(GL_SHADER_STORAGE_BUFFER, id[0]);
//    glBufferData(GL_SHADER_STORAGE_BUFFER, inputSize, null, GL_STREAM_COPY);
//    glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);  // unbind
//    int inputSsboId = id[0];
//
//// Create interpreter.
//    Interpreter interpreter = new Interpreter(loadModelFile(assetManager, modelFilename));
//    Tensor inputTensor = interpreter.getInputTensor(0);
//    GpuDelegate gpuDelegate = new GpuDelegate();
//// The buffer must be bound before the delegate is installed.
//    gpuDelegate.bindGlBufferToTensor(inputTensor, inputSsboId);
//    interpreter.modifyGraphWithDelegate(gpuDelegate);

//GPU Version
//    GpuDelegate delegate = new GpuDelegate();
//    Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
//    //Interpreter.Options options = new Interpreter.Options();
//    //options.setNumThreads(4);
//    //options.setUseNNAPI(true);
//    options.setAllowFp16PrecisionForFp32(true);
//    d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename), options);

    InputStream labelsInput = null;
    String actualFilename = labelFilename.split("file:///android_asset/")[1];
    labelsInput = assetManager.open(actualFilename);
    BufferedReader br = null;
    br = new BufferedReader(new InputStreamReader(labelsInput));
    String line;
    while ((line = br.readLine()) != null) {
      d.labels.add(line);
    }

    // Pre-allocate buffers.
    d.imgData = ByteBuffer.allocateDirect(d.inputWidth*d.inputHeight*3);
    //d.imgData = ByteBuffer.allocateDirect(d.inputWidth*d.inputHeight*3*4);
    d.imgData.order(ByteOrder.nativeOrder());

    d.imgDataRgba = ByteBuffer.allocateDirect(d.inputWidth*d.inputHeight*4);

    d.intValues = new int[d.inputWidth * d.inputHeight];
    d.byteValues = new byte[d.inputWidth*d.inputHeight*4];
    d.byteValues2 = new byte[d.inputWidth*d.inputHeight*3];

    //d.pixelClasses = new float[1][d.inputHeight*d.inputWidth*numOutput];
    //d.pixelClasses = new float[128][128][1];
    //d.pixelClasses = new byte[1][d.inputHeight*d.inputWidth*numOutput];

    d.pixelClassesFloat = new float[d.outputWidth][d.outputHeight]; //sally
      //this
    d.pixelClasses = new long[d.outputWidth][d.outputHeight];
    //d.pixelClasses = new byte[1][d.outputWidth][d.outputHeight][21];
    //d.pixelClasses = new byte[d.inputHeight][d.inputWidth];

    //d.pixelClassesSort = new float[1][128*128];
    d.pixelClassesSort = new float[1][d.inputHeight * d.inputWidth];
    //d.pixelClassesSort = new byte[1][d.inputHeight * d.inputWidth];
    //d.pixelClassesSort = new byte[1][d.outputWidth * d.outputHeight];

    return d;
  }

  private TFLiteObjectSegmentationAPIModel() {}

  public Vector<String> getLabels() {
    return labels;
  }

  public Segmentation segmentImage(final Bitmap bitmap) {
    if (imgData != null) {
      imgData.rewind();
    }

    // Log this method so that it can be analyzed with systrace.
    Trace.beginSection("segmentImage");

    Trace.beginSection("preprocessBitmap");

    final long startTime1 = SystemClock.uptimeMillis();
//    bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    Log.k(TAG, "[time-check] input time0 " + (SystemClock.uptimeMillis()-startTime1));

//    for (int j = 0; j < inputHeight; ++j) {
//      for (int i = 0; i < inputWidth; ++i) {
//        int pixel = intValues[j*inputWidth + i];
//        imgData.put((byte) ((pixel >> 16) & 0xFF));
//        imgData.put((byte) ((pixel >> 8) & 0xFF));
//        imgData.put((byte) (pixel & 0xFF));
//
////        imgData.putFloat((((pixel >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
////        imgData.putFloat((((pixel >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
////        imgData.putFloat(((pixel & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//      }
//    }

      if(imgDataRgba != null) {
          imgDataRgba.rewind();
      }

      bitmap.copyPixelsToBuffer(imgDataRgba);
      byteValues = imgDataRgba.array();

      int numOfBytes = inputHeight * inputWidth;
      for (int i = 0; i < numOfBytes; ++i) {
//        imgData.put(imgDataRgba.get(i*4+0));
//        imgData.put(imgDataRgba.get(i*4+1));
//        imgData.put(imgDataRgba.get(i*4+2));
          byteValues2[i * 3] = byteValues[i * 4];
          byteValues2[i * 3 + 1] = byteValues[i * 4 + 1];
          byteValues2[i * 3 + 2] = byteValues[i * 4 + 2];
//      imgData.put(byteValues[i * 4]);
//      imgData.put(byteValues[i * 4 + 1]);
//      imgData.put(byteValues[i * 4 + 2]);
      }
      imgData = ByteBuffer.wrap(byteValues2);

    Log.o(TAG, "[time-check] input time " + (SystemClock.uptimeMillis()-startTime1));

    Trace.endSection(); // preprocessBitmap

    // Run the inference call.
    Trace.beginSection("run");
    long startTime = SystemClock.uptimeMillis();

    segCount++;
    if(inputWidth == MODEL_INPUT_SIZE_FOR_CAPTURE) { //sally
      tfLite.run(imgData, pixelClassesFloat); //sally
    }
    else {
      tfLite.run(imgData, pixelClasses);
    }
    meanSpeed += ( SystemClock.uptimeMillis() - startTime );
    Log.k(TAG, "inference meantime : "+(meanSpeed / segCount));
    if( segCount > 10 ) {
      segCount = 0;
      meanSpeed = 0;
    }

    long endTime = SystemClock.uptimeMillis();
    Log.o(TAG, "inference time : "+(endTime-startTime));
    Trace.endSection(); // run

    Trace.endSection(); // segmentImage

//    float temp[][] = new float[1][128*128];
//    for(int i = 0; i < 128*128; i++)
//    {
//      temp[0][i] = 10;
//    }

//    int count = 0;
//    for(int i = 0; i < 128; i++)
//    {
//      for(int j = 0; j < 128; j++)
//      {
//        pixelClassesSort[0][count] = pixelClasses[i][j][0];
//        count++;
//      }
//    }

    final long startTime2 = SystemClock.uptimeMillis();
    int count = 0;
    if(inputWidth == MODEL_INPUT_SIZE_FOR_CAPTURE) { //sally - for capture (float model)
      for (int i = 0; i < inputHeight; i++) {
        for (int j = 0; j < inputWidth; j++) {
          pixelClassesSort[0][count] = pixelClassesFloat[i][j];
          count++;
        }
      }
    }
    else {
      for (int i = 0; i < inputHeight; i++) {
        for (int j = 0; j < inputWidth; j++) {
          pixelClassesSort[0][count] = pixelClasses[i][j];
          count++;
        }
      }
    }
    Log.k(TAG, "[time-check] output time " + (SystemClock.uptimeMillis()-startTime2));


//    int count = 0;
//    for(int i = 0; i < outputWidth; i++)
//    {
//      for(int j = 0; j < outputHeight; j++)
//      {
//        pixelClassesSort[0][count] = pixelClasses[0][i][j][14];
//        count++;
//      }
//    }

    return new Segmentation(
                //pixelClasses[0],
                pixelClassesSort[0],
                numClass,
                inputWidth, inputHeight, endTime - startTime,
            tfLite.getLastNativeInferenceDurationNanoseconds() / 1000 / 1000);

//    return new Segmentation(
//            //pixelClasses[0],
//            pixelClassesSort[0],
//            numClass,
//            outputWidth, outputHeight, endTime - startTime,
//            tfLite.getLastNativeInferenceDurationNanoseconds() / 1000 / 1000);
  }
}
