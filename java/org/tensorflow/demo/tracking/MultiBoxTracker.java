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

package org.tensorflow.demo.tracking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;

import com.ispd.sfcam.AIEngineObjDetection.Classifier;
import com.ispd.sfcam.AIEngineObjDetection.env.BorderedText;
import com.ispd.sfcam.AIEngineObjDetection.env.ImageUtils;
import com.ispd.sfcam.utils.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * A tracker wrapping ObjectTracker that also handles non-max suppression and matching existing
 * objects to new detections.
 */
public class MultiBoxTracker {

  private static String TAG = "MultiBoxTracker";

  private static final float TEXT_SIZE_DIP = 18;

  // Maximum percentage of a box that can be overlapped by another box at detection time. Otherwise
  // the lower scored box (new or old) will be removed.
  private static final float MAX_OVERLAP = 0.2f;

  private static final float MIN_SIZE = 16.0f;

  // Allow replacement of the tracked box with new results if
  // correlation has dropped below this level.
  private static final float MARGINAL_CORRELATION = 0.75f;

  // Consider object to be lost if correlation falls below this threshold.
  private static final float MIN_CORRELATION = 0.3f;

  private static final int[] COLORS = {
    Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE,
    Color.parseColor("#55FF55"), Color.parseColor("#FFA500"), Color.parseColor("#FF8888"),
    Color.parseColor("#AAAAFF"), Color.parseColor("#FFFFAA"), Color.parseColor("#55AAAA"),
    Color.parseColor("#AA33AA"), Color.parseColor("#0D0068")
  };

  private final Queue<Integer> availableColors = new LinkedList<Integer>();

  public ObjectTracker objectTracker;

  final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();

  private static class TrackedRecognition {
    ObjectTracker.TrackedObject trackedObject;
    RectF location;
    float detectionConfidence;
    int color;
    String title;
    //khkim
    String id;
  }

  private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
  //khkim
  private TrackedRecognition []mTrackedObjects = new TrackedRecognition[15];

  private final Paint boxPaint = new Paint();

  private final float textSizePx;
  private final BorderedText borderedText;

  private Matrix frameToCanvasMatrix;

  private int frameWidth;
  private int frameHeight;

  private int sensorOrientation;
  private Context context;

  public MultiBoxTracker(final Context context) {
    this.context = context;
    for (final int color : COLORS) {
      availableColors.add(color);
    }

    boxPaint.setColor(Color.RED);
    boxPaint.setStyle(Style.STROKE);
    boxPaint.setStrokeWidth(12.0f);
    boxPaint.setStrokeCap(Cap.ROUND);
    boxPaint.setStrokeJoin(Join.ROUND);
    boxPaint.setStrokeMiter(100);

    textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
  }

  private Matrix getFrameToCanvasMatrix() {
    return frameToCanvasMatrix;
  }

  public synchronized void drawDebug(final Canvas canvas) {
    final Paint textPaint = new Paint();
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(60.0f);

    final Paint boxPaint = new Paint();
    boxPaint.setColor(Color.RED);
    boxPaint.setAlpha(200);
    boxPaint.setStyle(Style.STROKE);

    for (final Pair<Float, RectF> detection : screenRects) {
      final RectF rect = detection.second;
      canvas.drawRect(rect, boxPaint);
      canvas.drawText("" + detection.first, rect.left, rect.top, textPaint);
      borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first);
    }

    if (objectTracker == null) {
      return;
    }

    // Draw correlations.
    for (final TrackedRecognition recognition : trackedObjects) {
      final ObjectTracker.TrackedObject trackedObject = recognition.trackedObject;

      final RectF trackedPos = trackedObject.getTrackedPositionInPreviewFrame();

      if (getFrameToCanvasMatrix().mapRect(trackedPos)) {
        final String labelString = String.format("%.2f", trackedObject.getCurrentCorrelation());
        borderedText.drawText(canvas, trackedPos.right, trackedPos.bottom, labelString);
      }
    }

    final Matrix matrix = getFrameToCanvasMatrix();
    objectTracker.drawDebug(canvas, matrix);
  }

  public synchronized void trackResults(
          final List<Classifier.Recognition> results, final byte[] frame, final long timestamp) {
    Log.i(TAG, "Processing %d results from %d", results.size(), timestamp);
    processResults(timestamp, results, frame);
  }

  private RectF flipY(RectF rect)
  {
    final boolean rotated = sensorOrientation % 180 == 90;
    final float multiplier =
            Math.min(1920.f / (float) (rotated ? frameWidth : frameHeight),
                    1080.f / (float) (rotated ? frameHeight : frameWidth));

    Log.d(TAG,"rect.left : "+rect.left+", rect.top : "+rect.top+", rect.right : "+rect.right+", rect.bottom : "+rect.bottom);
    RectF temp = new RectF(rect.left, multiplier * (float)frameWidth - rect.bottom, rect.right, multiplier * (float)frameWidth - rect.top);
    Log.d(TAG,  "temp.left : "+temp.left+", temp.top : "+temp.top+", temp.right : "+temp.right+", temp.bottom : "+temp.bottom);
    return temp;
  }

  public synchronized int getTrackedObj(RectF rect[], float confidence[], String name[], int color[], String id[], int frontCam)
  {
    Log.d(TAG,"sensorOrientationObj : "+sensorOrientation);
    Log.d(TAG,"frameWidth : "+frameWidth);
    Log.d(TAG,"frameHeight : "+frameHeight);

    final boolean rotated = sensorOrientation % 180 == 90;
    final float multiplier =
            Math.min(1920.f / (float) (rotated ? 1440 : 1080),
                    1080.f / (float) (rotated ? 1080 : 1440));

    Log.d(TAG,"targetObj width : "+(int) (multiplier * (rotated ? 1080 : 1440)));
    Log.d(TAG,"targetObj height : "+(int) (multiplier * (rotated ? 1440 : 1080)));

    frameToCanvasMatrix =
            ImageUtils.getTransformationMatrix(
                    frameWidth,
                    frameHeight,
                    (int) (multiplier * (rotated ? 1080 : 1440)),
                    (int) (multiplier * (rotated ? 1440 : 1080)),
                    sensorOrientation,
                    false);

//    Log.d(TAG,"sensorOrientationObj : "+sensorOrientation);
//    Log.d(TAG,"frameWidth : "+frameWidth);
//    Log.d(TAG,"frameHeight : "+frameHeight);
//
//      final boolean rotated = sensorOrientation % 180 == 90;
//      final float multiplier =
//              Math.min(1920.f / (float) (rotated ? frameWidth : frameHeight),
//                      1080.f / (float) (rotated ? frameHeight : frameWidth));
//
//    Log.d(TAG,"target width : "+(int) (multiplier * (rotated ? frameHeight : frameWidth)));
//    Log.d(TAG,"target height : "+(int) (multiplier * (rotated ? frameWidth : frameHeight)));
//
//      frameToCanvasMatrix =
//              ImageUtils.getTransformationMatrix(
//                      frameWidth,
//                      frameHeight,
//                      (int) (multiplier * (rotated ? frameHeight : frameWidth)),
//                      (int) (multiplier * (rotated ? frameWidth : frameHeight)),
//                      sensorOrientation,
//                      false);

//    frameToCanvasMatrix =
//            ImageUtils.getTransformationMatrix(
//                    frameWidth,
//                    frameHeight,
//                    1080,
//                    1440,
//                    90,
//                    false);

      int count = 0;

      for (final TrackedRecognition recognition : trackedObjects) {

        RectF trackedPos =
                (objectTracker != null)
                        ? recognition.trackedObject.getTrackedPositionInPreviewFrame()
                        : new RectF(recognition.location);
        getFrameToCanvasMatrix().mapRect(trackedPos);

        if(count > 9) break;

        rect[count] = trackedPos;
        confidence[count] = recognition.detectionConfidence;
        name[count] = recognition.title;
        color[count] = recognition.color;
        id[count] = recognition.trackedObject.getCurrentId();

        Log.v(TAG, "[object-check] %d Object id : %s", count, id[count]);

        count++;
      }

      return count;
  }

  public synchronized void draw(final Canvas canvas) {

    Log.d(TAG,"sensorOrientation : "+sensorOrientation);

    //front camera
    sensorOrientation = 270;
//    sensorOrientation = 0;

    final boolean rotated = sensorOrientation % 180 == 90;
    final float multiplier =
        Math.min(canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
                 canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
    frameToCanvasMatrix =
        ImageUtils.getTransformationMatrix(
            frameWidth,
            frameHeight,
            (int) (multiplier * (rotated ? frameHeight : frameWidth)),
            (int) (multiplier * (rotated ? frameWidth : frameHeight)),
            sensorOrientation,
            false);

    //front camera
//    frameToCanvasMatrix.preScale(1, -1);

    for (final TrackedRecognition recognition : trackedObjects) {
      final RectF trackedPos =
          (objectTracker != null)
              ? recognition.trackedObject.getTrackedPositionInPreviewFrame()
              : new RectF(recognition.location);

      getFrameToCanvasMatrix().mapRect(trackedPos);
      boxPaint.setColor(recognition.color);

      final float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
      canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);

      final String labelString =
          !TextUtils.isEmpty(recognition.title)
              ? String.format("%s %.2f", recognition.title, recognition.detectionConfidence)
              : String.format("%.2f", recognition.detectionConfidence);
      borderedText.drawText(canvas, trackedPos.left + cornerSize, trackedPos.bottom, labelString);
    }
  }

  public synchronized void release()
  {
    objectTracker.release();
    objectTracker = null;
    initialized = false;
  }

  private boolean initialized = false;

  public synchronized void onFrame(
      final int w,
      final int h,
      final int rowStride,
      final int sensorOrientation,
      final byte[] frame,
      final long timestamp) {
    if (objectTracker == null && !initialized) {
      ObjectTracker.clearInstance();

      Log.i(TAG, "Initializing ObjectTracker: %dx%d", w, h);
      objectTracker = ObjectTracker.getInstance(w, h, rowStride, true);
      frameWidth = w;
      frameHeight = h;
      this.sensorOrientation = sensorOrientation;
      initialized = true;

      if (objectTracker == null) {
        String message =
            "Object tracking support not found. "
                + "See tensorflow/examples/android/README.md for details.";
        //Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
      }
    }

    if (objectTracker == null) {
      return;
    }

    objectTracker.nextFrame(frame, null, timestamp, null, true);

    // Clean up any objects not worth tracking any more.
    final LinkedList<TrackedRecognition> copyList =
        new LinkedList<TrackedRecognition>(trackedObjects);
    for (final TrackedRecognition recognition : copyList) {
      final ObjectTracker.TrackedObject trackedObject = recognition.trackedObject;
      final float correlation = trackedObject.getCurrentCorrelation();
      if (correlation < MIN_CORRELATION) {
//        Log.v("Removing tracked object %s because NCC is %.2f", trackedObject, correlation);
        Log.v(TAG, "[object-check] Removing tracked object[%d] id:%s %s because NCC is %.2f", copyList.indexOf(recognition), trackedObject.getCurrentId(), trackedObject, correlation);
        trackedObject.stopTracking();
        trackedObjects.remove(recognition);

        availableColors.add(recognition.color);
      }
    }
  }

  private void processResults(
          final long timestamp, final List<Classifier.Recognition> results, final byte[] originalFrame) {
    final List<Pair<Float, Classifier.Recognition>> rectsToTrack = new LinkedList<Pair<Float, Classifier.Recognition>>();

    screenRects.clear();
    final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

    for (final Classifier.Recognition result : results) {
      if (result.getLocation() == null) {
        continue;
      }
      final RectF detectionFrameRect = new RectF(result.getLocation());

      final RectF detectionScreenRect = new RectF();
      rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

      Log.v(TAG,
              "Result! Frame: " + result.getLocation() + " mapped to screen:" + detectionScreenRect);

      screenRects.add(new Pair<Float, RectF>(result.getConfidence(), detectionScreenRect));

      if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
        Log.w(TAG, "Degenerate rectangle! " + detectionFrameRect);
        continue;
      }

      rectsToTrack.add(new Pair<Float, Classifier.Recognition>(result.getConfidence(), result));
    }

    if (rectsToTrack.isEmpty()) {
      Log.v(TAG, "Nothing to track, aborting.");
      return;
    }

    if (objectTracker == null) {
      trackedObjects.clear();
      for (final Pair<Float, Classifier.Recognition> potential : rectsToTrack) {
        final TrackedRecognition trackedRecognition = new TrackedRecognition();
        trackedRecognition.detectionConfidence = potential.first;
        trackedRecognition.location = new RectF(potential.second.getLocation());
        trackedRecognition.trackedObject = null;
        trackedRecognition.title = potential.second.getTitle();
        trackedRecognition.color = COLORS[trackedObjects.size()];
        trackedObjects.add(trackedRecognition);

        if (trackedObjects.size() >= COLORS.length) {
          break;
        }
      }
      return;
    }

    Log.i(TAG, "%d rects to track", rectsToTrack.size());

    //khkim
    int count = 0;
    for (final Pair<Float, Classifier.Recognition> potential : rectsToTrack) {
      handleDetection(originalFrame, timestamp, potential, count);
      //khkim
      count++;
    }
  }

  private void handleDetection(
          final byte[] frameCopy, final long timestamp, final Pair<Float, Classifier.Recognition> potential, int count) {
    final ObjectTracker.TrackedObject potentialObject =
            objectTracker.trackObject(potential.second.getLocation(), timestamp, frameCopy);

    final float potentialCorrelation = potentialObject.getCurrentCorrelation();
    Log.v(TAG,
            "[track-test] Tracked object went from %s to %s with correlation %.2f",
            potential.second, potentialObject.getTrackedPositionInPreviewFrame(), potentialCorrelation);

    if (potentialCorrelation < MARGINAL_CORRELATION) {
      Log.v(TAG, "[track-test] Correlation too low to begin tracking %s.", potentialObject);
      potentialObject.stopTracking();
      return;
    }

    final List<TrackedRecognition> removeList = new LinkedList<TrackedRecognition>();

    float maxIntersect = 0.0f;

    // This is the current tracked object whose color we will take. If left null we'll take the
    // first one from the color queue.
    TrackedRecognition recogToReplace = null;

    // Look for intersections that will be overridden by this object or an intersection that would
    // prevent this one from being placed.
    for (final TrackedRecognition trackedRecognition : trackedObjects) {
      final RectF a = trackedRecognition.trackedObject.getTrackedPositionInPreviewFrame();
      final RectF b = potentialObject.getTrackedPositionInPreviewFrame();
      final RectF intersection = new RectF();
      final boolean intersects = intersection.setIntersect(a, b);

      final float intersectArea = intersection.width() * intersection.height();
      final float totalArea = a.width() * a.height() + b.width() * b.height() - intersectArea;
      final float intersectOverUnion = intersectArea / totalArea;

      // If there is an intersection with this currently tracked box above the maximum overlap
      // percentage allowed, either the new recognition needs to be dismissed or the old
      // recognition needs to be removed and possibly
      // replaced with the new one.
      if (intersects && intersectOverUnion > MAX_OVERLAP) {
        if (potential.first < trackedRecognition.detectionConfidence
                && trackedRecognition.trackedObject.getCurrentCorrelation() > MARGINAL_CORRELATION) {
          // If track for the existing object is still going strong and the detection score was
          // good, reject this new object.
          potentialObject.stopTracking();
          Log.v(TAG, "[track-test] stopTracking");
          return;
        } else {
          removeList.add(trackedRecognition);

          // Let the previously tracked object with max intersection amount donate its color to
          // the new object.
          if (intersectOverUnion > maxIntersect) {
            maxIntersect = intersectOverUnion;
            recogToReplace = trackedRecognition;
          }
        }
      }
    }

    // If we're already tracking the max object and no intersections were found to bump off,
    // pick the worst current tracked object to remove, if it's also worse than this candidate
    // object.
    if (availableColors.isEmpty() && removeList.isEmpty()) {
      for (final TrackedRecognition candidate : trackedObjects) {
        if (candidate.detectionConfidence < potential.first) {
          if (recogToReplace == null
                  || candidate.detectionConfidence < recogToReplace.detectionConfidence) {
            // Save it so that we use this color for the new object.
            recogToReplace = candidate;
          }
        }
      }
      if (recogToReplace != null) {
        Log.v(TAG, "[track-test] Found non-intersecting object to remove.");
        removeList.add(recogToReplace);
      } else {
        Log.v(TAG, "[track-test] No non-intersecting object found to remove");
      }
    }

    // Remove everything that got intersected.
    for (final TrackedRecognition trackedRecognition : removeList) {
      Log.v(TAG,
              "[track-test] Removing tracked object %s with detection confidence %.2f, correlation %.2f",
              trackedRecognition.trackedObject,
              trackedRecognition.detectionConfidence,
              trackedRecognition.trackedObject.getCurrentCorrelation());
      trackedRecognition.trackedObject.stopTracking();

      Log.v(TAG, "[track-test3] trackedRecognition %d removed %s, indexOf : %d, color : %d", trackedObjects.size(), trackedRecognition.title, trackedObjects.indexOf(trackedRecognition), trackedRecognition.color);

      trackedObjects.remove(trackedRecognition);
      if (trackedRecognition != recogToReplace) {
        availableColors.add(trackedRecognition.color);
      }
    }

    if (recogToReplace == null && availableColors.isEmpty()) {
      Log.e(TAG, "[track-test] No room to track this object, aborting.");
      potentialObject.stopTracking();
      return;
    }

    // Finally safe to say we can track this object.
    Log.v(TAG, "[track-test2] count %d", count);
    Log.v(TAG,
            "[track-test2] %s Tracking object %s (%s) with detection confidence %.2f at position %s",
            potential.second.getId(),
            potentialObject,
            potential.second.getTitle(),
            potential.first,
            potential.second.getLocation());
    final TrackedRecognition trackedRecognition = new TrackedRecognition();
    trackedRecognition.detectionConfidence = potential.first;
    trackedRecognition.trackedObject = potentialObject;
    trackedRecognition.title = potential.second.getTitle();
    //khkim
    trackedRecognition.id = potential.second.getId();

    // Use the color from a replaced object before taking one from the color queue.
    trackedRecognition.color =
            recogToReplace != null ? recogToReplace.color : availableColors.poll();

    for (final TrackedRecognition temp : trackedObjects) {
      Log.v(TAG, "[track-test3] pre temp[%d] : %s, id : %s", trackedObjects.indexOf(temp), temp.title, trackedRecognition.id);
    }

    Log.v("[track-test3] pre trackedRecognition added %s, size %d, color : %d, id : %s", trackedRecognition.title, trackedObjects.size(), trackedRecognition.color, trackedRecognition.id);
    trackedObjects.add(trackedRecognition);
    Log.v(TAG, "[track-test2] trackedObjects %d", trackedObjects.size());

    Log.v(TAG, "[track-test3] post size %d",trackedObjects.size());
    for (final TrackedRecognition temp : trackedObjects) {
      Log.v(TAG, "[track-test3] post temp[%d] : %s, id : %s", trackedObjects.indexOf(temp), temp.title, trackedRecognition.id);
    }
  }
}
