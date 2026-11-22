package cn.edu.xxu.seat;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/** Full-screen seat map with pinch-to-zoom and one-finger panning. */
public final class SeatMapActivity extends Activity {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    setTitle("七楼北座位图（双指缩放，单指拖动）");
    ZoomImageView map = new ZoomImageView();
    map.setImageResource(R.drawable.library_seventh_north);
    setContentView(map);
  }

  private final class ZoomImageView extends ImageView {
    private final Matrix matrix = new Matrix();
    private final ScaleGestureDetector scaleDetector;
    private float lastX, lastY, scale = 1f;

    ZoomImageView() {
      super(SeatMapActivity.this);
      setBackgroundColor(0xff111111);
      setScaleType(ScaleType.MATRIX);
      scaleDetector = new ScaleGestureDetector(SeatMapActivity.this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override public boolean onScale(ScaleGestureDetector detector) {
          float next = Math.max(1f, Math.min(6f, scale * detector.getScaleFactor()));
          matrix.postScale(next / scale, next / scale, detector.getFocusX(), detector.getFocusY());
          scale = next;
          setImageMatrix(matrix);
          return true;
        }
      });
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
      super.onSizeChanged(width, height, oldWidth, oldHeight);
      Drawable image = getDrawable();
      if (image == null || width == 0 || height == 0) return;
      float fit = Math.min((float) width / image.getIntrinsicWidth(), (float) height / image.getIntrinsicHeight());
      matrix.reset();
      matrix.postScale(fit, fit);
      matrix.postTranslate((width - image.getIntrinsicWidth() * fit) / 2f, (height - image.getIntrinsicHeight() * fit) / 2f);
      scale = 1f;
      setImageMatrix(matrix);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
      scaleDetector.onTouchEvent(event);
      if (event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) { lastX = event.getX(); lastY = event.getY(); }
        else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
          matrix.postTranslate(event.getX() - lastX, event.getY() - lastY);
          lastX = event.getX(); lastY = event.getY(); setImageMatrix(matrix);
        }
      }
      return true;
    }
  }
}
