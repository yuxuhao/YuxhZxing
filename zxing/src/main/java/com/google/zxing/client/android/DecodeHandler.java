/*
 * Copyright (C) 2010 ZXing authors
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

import android.graphics.Bitmap;


import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.core.BinaryBitmap;
import com.google.zxing.client.android.core.DecodeHintType;
import com.google.zxing.client.android.core.MultiFormatReader;
import com.google.zxing.client.android.core.PlanarYUVLuminanceSource;
import com.google.zxing.client.android.core.ReaderException;
import com.google.zxing.client.android.core.Result;
import com.google.zxing.client.android.core.ResultPoint;
import com.google.zxing.client.android.core.common.GlobalHistogramBinarizer;
import com.google.zxing.client.android.core.common.HybridBinarizer;
import com.google.zxing.client.android.core.qrcode.QRCodeReader;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class DecodeHandler extends Handler {

    private static final String TAG = DecodeHandler.class.getSimpleName();

    private final CaptureActivity activity;
    private final MultiFormatReader multiFormatReader;


    DecodeHandler(CaptureActivity activity, Map<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.activity = activity;
    }

    @Override
    public void handleMessage(Message message) {
//        if (message == null || !running) {
//            return;
//        }
        if (message.what == R.id.decode) {
            decode((byte[]) message.obj, message.arg1, message.arg2);
        }
//        else if (message.what == R.id.quit) {
//            running = false;
//            Looper.myLooper().quit();
//        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     *               zxing项目官方默认使用的是HybridBinarizer二值化方法。然而目前的大部分二维码都是黑色二维码，白色背景的。
     *               不管是二维码扫描还是二维码图像识别，
     *               使用GlobalHistogramBinarizer算法的效果要稍微比HybridBinarizer好一些
     *               ，识别的速度更快，对低分辨的图像识别精度更高。可以在DecodeHandler 中更改算法:
     */
    private void decode(byte[] data, int width, int height) {
        long start = System.nanoTime();
        Result rawResult = null;
        BinaryBitmap bitmap = null;

        //竖屏新增
        byte[] rotatedData = new byte[data.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                rotatedData[x * height + height - y - 1] = data[x + y * width];
            }
        }
        int tmp = width; // Here we are swapping, that's the difference to #11
        width = height;
        height = tmp;
        data = rotatedData;
        //竖屏新增

        PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);

        if (source != null) {
            //优先GlobalHistogramBinarizer解码，解码失败转为HybridBinarizer解码
            bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));

            if (bitmap == null) {
                bitmap = new BinaryBitmap(new HybridBinarizer(source));
            }

            try {
                rawResult = multiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }

        }

        Handler handler = activity.getHandler();
        if (rawResult != null) {
            // Don't log the barcode contents for security.
            long end = System.nanoTime();
            Log.d(TAG, "Found barcode in " + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms");
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
                Bundle bundle = new Bundle();
                bundleThumbnail(source, bundle);
                message.setData(bundle);
                message.sendToTarget();
            }
        } else {
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_failed);
                message.sendToTarget();
            }
        }
    }

    private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
        bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
    }

}
