package com.acgist.taoyao.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.acgist.taoyao.client.databinding.ActivitySettingsBinding;

/**
 * 设置界面
 *
 * @author acgist
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 布局
        this.binding = ActivitySettingsBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
        // 设置按钮
        this.binding.connect.setOnClickListener(view -> {
            final String port = this.binding.port.getText().toString();
            final String name = this.binding.name.getText().toString();
            final String host = this.binding.host.getText().toString();
//            final Taoyao taoyao = new Taoyao(
//
//            );
//            Log.d(SettingsActivity.class.getSimpleName(), "连接信令：" + taoyao);
            final Intent main = new Intent(this, MainActivity.class);
            this.startActivity(main);
        });
    }

}