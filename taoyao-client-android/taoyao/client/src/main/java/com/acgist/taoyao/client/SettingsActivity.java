package com.acgist.taoyao.client;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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
        Log.i(SettingsActivity.class.getSimpleName(), "onCreate");
        super.onCreate(savedInstanceState);
        // 布局
        this.binding = ActivitySettingsBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
        // 设置按钮
        this.binding.connect.setOnClickListener(this::settingsPersistent);
    }

    @Override
    protected void onStart() {
        Log.i(SettingsActivity.class.getSimpleName(), "onStart");
        super.onStart();
        // 回填配置
        final SharedPreferences sharedPreferences = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        this.binding.port.setText(String.valueOf(sharedPreferences.getInt("settings.port", 9999)));
        this.binding.host.setText(sharedPreferences.getString("settings.host", "192.168.1.100"));
        this.binding.name.setText(sharedPreferences.getString("settings.name", "移动端"));
        this.binding.clientId.setText(sharedPreferences.getString("settings.clientId", "mobile"));
        this.binding.username.setText(sharedPreferences.getString("settings.username", "taoyao"));
        this.binding.password.setText(sharedPreferences.getString("settings.password", "taoyao"));
    }

    @Override
    protected void onStop() {
        Log.i(SettingsActivity.class.getSimpleName(), "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i(SettingsActivity.class.getSimpleName(), "onDestroy");
        super.onDestroy();
    }

    /**
     * 持久化日志
     *
     * @param view View
     */
    private void settingsPersistent(View view) {
        final String port = this.binding.port.getText().toString();
        final String host = this.binding.host.getText().toString();
        final String name = this.binding.name.getText().toString();
        final String clientId = this.binding.clientId.getText().toString();
        final String username = this.binding.username.getText().toString();
        final String password = this.binding.password.getText().toString();
        Log.i(SettingsActivity.class.getSimpleName(), String.format("""
        端口：%s
        地址：%s
        名称：%s
        标识：%s
        用户：%s
        密码：%s
        """, port, host, name, clientId, username, password));
        // 保存配置
        final SharedPreferences sharedPreferences = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("settings.port", Integer.parseInt(port));
        editor.putString("settings.host", host);
        editor.putString("settings.name", name);
        editor.putString("settings.clientId", clientId);
        editor.putString("settings.username", username);
        editor.putString("settings.password", password);
        editor.commit();
        // 重连
        final Intent intent = new Intent(this, MediaService.class);
        intent.setAction(MediaService.Action.RECONNECT.name());
        this.startService(intent);
        // 返回预览页面
        this.startActivity(new Intent(this, MainActivity.class));
        // 结束
        this.finish();
    }

}