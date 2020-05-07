# YuxhZxing

``` 
目前优化的地方有
  1. 减少编码格式目前只支持 条码格式 code128 、 EAN13 、二维码，
     这三种格式基本上已经满足市场上90%需求
  2. 优化了只能识别扫描框内二维码提高解码速度
  3. 串行改为并行提高识别效率
   
   
   	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
	
	
	
	dependencies {
	        implementation 'com.github.yuxuhao:YuxhZxing:V1.0'
	}
	
	
```



```
 <com.google.zxing.client.android.ViewfinderView

      android:id="@+id/viewfinder_view"

      android:layout_width="fill_parent"

      android:layout_height="fill_parent"

      app:border_color="@color/result_points"

      app:border_width="2"

      app:box_custom_pattern="true"

      app:box_width="800"

      app:box_height="800"

      app:right_angle_color="@color/viewfinder_laser"

      app:right_angle_length="100"

      app:right_angle_width="30"

      app:scan_line_color="@color/result_points"

      />

```

 

| box_custom_pattern | true时表示要自定义扫描框宽高              |
| :----------------- | ----------------------------------------- |
| box_width          | 扫描框宽度/box_custom_pattern/ true时生效 |
| box_height         | 扫描框高度/box_custom_pattern/ true时生效 |
| border_color       | 扫描框边线颜色                            |
| border_width       | 扫描框边线宽度                            |
| right_angle_width  | 添加四个角的包括直角的边角线宽度          |
| right_angle_length | 边角线长度                                |
| right_angle_color  | 边角线颜色                                |
| scan_line_color    | 扫描线颜色                                |
| capture_color      | 动态捕捉点的颜色                          |
| mask_color         | 除扫描框页面背景色                        |





## 异常

```
 如果抛出这个异常请检查自定义扫描框尺寸大小是不是超出屏幕尺寸
 
 java.lang.IllegalArgumentException: Crop rectangle does not fit within image data.
        at com.google.zxing.client.android.core.PlanarYUVLuminanceSource.<init>(PlanarYUVLuminanceSource.java:51)
        at com.google.zxing.client.android.camera.CameraManager.buildLuminanceSource(CameraManager.java:336)
        at com.google.zxing.client.android.DecodeHandler.decode(DecodeHandler.java:103)
        at com.google.zxing.client.android.DecodeHandler.handleMessage(DecodeHandler.java:65)
        at android.os.Handler.dispatchMessage(Handler.java:107)
        at android.os.Looper.loop(Looper.java:213)
        at com.google.zxing.client.android.DecodeThread.run(DecodeThread.java:115)
```

