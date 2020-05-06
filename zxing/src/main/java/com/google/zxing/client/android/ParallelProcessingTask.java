package com.google.zxing.client.android;

import android.graphics.Camera;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ParallelProcessingTask {
    private Camera mCamera;
    private byte[] mData;
    public ParallelProcessingTask() {

    }

    public static ExecutorService newFixedThreadPool(int nThreads){

        return new ThreadPoolExecutor(nThreads,nThreads,0l, TimeUnit.MILLISECONDS,new LinkedBlockingDeque<Runnable>());
    }

}
