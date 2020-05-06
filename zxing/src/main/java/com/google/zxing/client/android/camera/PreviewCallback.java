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

package com.google.zxing.client.android.camera;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.zxing.client.android.ParallelProcessingTask;
import com.google.zxing.client.android.R;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation") // camera APIs
final class PreviewCallback implements Camera.PreviewCallback {

  private static final String TAG = PreviewCallback.class.getSimpleName();

  private final CameraConfigurationManager configManager;
  private Handler previewHandler;
  private int previewMessage;

  PreviewCallback(CameraConfigurationManager configManager) {
    this.configManager = configManager;
  }

  void setHandler(Handler previewHandler, int previewMessage) {
    this.previewHandler = previewHandler;
    this.previewMessage = previewMessage;
  }

  @Override
  public void onPreviewFrame(final byte[] data, Camera camera) {
    final Point cameraResolution = configManager.getCameraResolution();
    final Handler thePreviewHandler = previewHandler;
//    if (cameraResolution != null && thePreviewHandler != null) {
//      Message message = thePreviewHandler.obtainMessage(previewMessage, cameraResolution.x,
//          cameraResolution.y, data);
//      message.sendToTarget();
//      previewHandler = null;
//    } else {
//      Log.d(TAG, "Got preview callback, but no handler or resolution available");
//    }

    //串行改为并行
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 10, 0l,
            TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>());
    threadPoolExecutor.execute(new Runnable() {
      @Override
      public void run() {
        if (cameraResolution != null && thePreviewHandler != null) {
          Message message = thePreviewHandler.obtainMessage(previewMessage, cameraResolution.x,
                  cameraResolution.y, data);
          message.sendToTarget();
          previewHandler = null;
        }
      }
    });
  }

}
