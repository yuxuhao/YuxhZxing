package com.hzanchu.yuxhzxing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.client.android.CaptureActivity;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    protected Button but;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        initView();

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.but) {
            AndPermission.with(this)
                    .runtime()
                    .permission(Permission.CAMERA, Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
                    .onGranted(new Action<List<String>>() {
                        @Override
                        public void onAction(List<String> data) {
                            startActivityForResult(new Intent(MainActivity.this, CaptureActivity.class), 0);
                        }
                    }).onDenied(new Action<List<String>>() {
                @Override
                public void onAction(List<String> data) {
                    ToastUtils.showLong("申请权限失败");
                }
            }).start();

        }
    }

    private void initView() {
        but = (Button) findViewById(R.id.but);
        but.setOnClickListener(MainActivity.this);

    }
}
