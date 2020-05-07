/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.core.ResultPoint;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 80L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;

    private CameraManager cameraManager;
    private final Paint paint;
    private Bitmap resultBitmap;
    private final int maskColor;
    private final int resultColor;
    private int scannerAlpha;
    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;
    private boolean aBoolean;
    private int boxWidth;
    private int boxHeight;
    private int borderColor;
    private int borderWidth;
    private int rightAngleWidth;
    private int rightAngleLength;
    private int rightAngleColor;
    private int scanLineColor;
    private int captureColor;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.scan_box_settings);
        //扫描框意以外的背景色
         maskColor = typedArray.getColor(R.styleable.scan_box_settings_mask_color, 0x60000000);
        //扫描线颜色
        scanLineColor = typedArray.getColor(R.styleable.scan_box_settings_scan_line_color, 0xffcc0000);
        //捕捉像素点颜色
         captureColor = typedArray.getColor(R.styleable.scan_box_settings_capture_color, 0xc0ffbd21);
        //扫描框宽高
        boxWidth = typedArray.getInteger(R.styleable.scan_box_settings_box_width, 300);
        boxHeight = typedArray.getInteger(R.styleable.scan_box_settings_box_height, 300);
        //边线颜色
        borderColor = typedArray.getColor(R.styleable.scan_box_settings_border_color, 0x00000000);
        //边线宽度
        borderWidth = typedArray.getInteger(R.styleable.scan_box_settings_border_width, 1);
        //是否自定尺寸，true 的话 boxWidth boxHeight 才生效
        aBoolean = typedArray.getBoolean(R.styleable.scan_box_settings_box_custom_pattern, false);
        //四角的直角
        //直角线长度
        rightAngleLength = typedArray.getInteger(R.styleable.scan_box_settings_right_angle_length, 50);
        //直角线宽度
        rightAngleWidth = typedArray.getInteger(R.styleable.scan_box_settings_right_angle_width, 20);
        //直角线颜色
        rightAngleColor = typedArray.getColor(R.styleable.scan_box_settings_right_angle_color, 0x00000000);
        typedArray.recycle();
        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        resultColor = resources.getColor(R.color.result_view);
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        //自定义尺寸
        if (aBoolean) {
            cameraManager.setManualFramingRect(boxWidth, boxHeight);
        }
        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();


        // Draw the exterior (i.e. outside the framing rect) darkened
        this.paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, this.paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, this.paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, this.paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, this.paint);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            this.paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, this.paint);
        } else {

            // Draw a red "laser scanner" line through the middle to show decoding is active
            this.paint.setColor(scanLineColor);
            this.paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
            int middle = frame.height() / 2 + frame.top;
            canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, this.paint);

            float scaleX = frame.width() / (float) previewFrame.width();
            float scaleY = frame.height() / (float) previewFrame.height();

            List<ResultPoint> currentPossible = possibleResultPoints;
            List<ResultPoint> currentLast = lastPossibleResultPoints;
            int frameLeft = frame.left;
            int frameTop = frame.top;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new ArrayList<>(5);
                lastPossibleResultPoints = currentPossible;
                this.paint.setAlpha(CURRENT_POINT_OPACITY);
                this.paint.setColor(captureColor);
                synchronized (currentPossible) {
                    for (ResultPoint point : currentPossible) {
                        canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                                frameTop + (int) (point.getY() * scaleY),
                                POINT_SIZE, this.paint);
                    }
                }
            }
            if (currentLast != null) {
                this.paint.setAlpha(CURRENT_POINT_OPACITY / 2);
                this.paint.setColor(captureColor);
                synchronized (currentLast) {
                    float radius = POINT_SIZE / 2.0f;
                    for (ResultPoint point : currentLast) {
                        canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                                frameTop + (int) (point.getY() * scaleY),
                                radius, this.paint);
                    }
                }
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY,
                    frame.left - POINT_SIZE,
                    frame.top - POINT_SIZE,
                    frame.right + POINT_SIZE,
                    frame.bottom + POINT_SIZE);
        }


        /**
         * 画边线
         */
        Paint borderPaint = new Paint();
        borderPaint.setStrokeWidth(borderWidth);
        borderPaint.setColor(borderColor);
        borderPaint.setAntiAlias(true);
        //上边线
        canvas.drawLine(frame.left, frame.top - borderWidth / 2, frame.right, frame.top - borderWidth / 2, borderPaint);
        //左边线
        canvas.drawLine(frame.left - borderWidth / 2, frame.top, frame.left - borderWidth / 2, frame.bottom, borderPaint);
        //下边线
        canvas.drawLine(frame.left, frame.bottom + borderWidth / 2, frame.right, frame.bottom + borderWidth / 2, borderPaint);
        //右边线
        canvas.drawLine(frame.right + borderWidth / 2, frame.top, frame.right + borderWidth / 2, frame.bottom, borderPaint);

        /**
         * 画四个直角
         */
        Paint rightAnglePaint = new Paint();
        rightAnglePaint.setStrokeWidth(rightAngleWidth);
        rightAnglePaint.setColor(rightAngleColor);
        rightAnglePaint.setAntiAlias(true);
        //左上直角
        canvas.drawLine(frame.left, frame.top, frame.left + rightAngleLength, frame.top, rightAnglePaint);
        canvas.drawLine(frame.left, frame.top - rightAngleWidth / 2, frame.left, frame.top + rightAngleLength, rightAnglePaint);
        //左下直角
        canvas.drawLine(frame.left, frame.bottom + rightAngleWidth / 2, frame.left, frame.bottom - rightAngleLength, rightAnglePaint);
        canvas.drawLine(frame.left, frame.bottom, frame.left + rightAngleLength, frame.bottom, rightAnglePaint);
        //右上直角
        canvas.drawLine(frame.right, frame.top, frame.right - rightAngleLength, frame.top, rightAnglePaint);
        canvas.drawLine(frame.right, frame.top - rightAngleWidth / 2, frame.right, frame.top + rightAngleLength, rightAnglePaint);
        //右下直角
        canvas.drawLine(frame.right, frame.bottom, frame.right, frame.bottom - rightAngleLength, rightAnglePaint);
        canvas.drawLine(frame.right + rightAngleWidth / 2, frame.bottom, frame.right - rightAngleLength, frame.bottom, rightAnglePaint);
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

}
