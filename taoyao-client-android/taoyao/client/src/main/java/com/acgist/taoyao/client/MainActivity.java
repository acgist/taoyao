package com.acgist.taoyao.client;

import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.acgist.taoyao.client.databinding.ActivityMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * 预览界面
 *
 * @author acgist
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 媒体服务
        final Intent mediaService = new Intent(this, MediaService.class);
        this.startService(mediaService);
        // 布局
        this.binding = ActivityMainBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
        // 设置按钮
        this.binding.settings.setOnClickListener(view -> {
            final Intent settings = new Intent(this, SettingsActivity.class);
            this.startService(settings);
        });
    }

}