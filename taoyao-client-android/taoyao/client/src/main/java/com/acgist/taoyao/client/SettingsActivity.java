package com.acgist.taoyao.client;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
        Log.d(SettingsActivity.class.getSimpleName(), "onCreate");
        super.onCreate(savedInstanceState);
        this.binding = ActivitySettingsBinding.inflate(this.getLayoutInflater());
        final View root = this.binding.getRoot();
        root.setZ(100F);
        this.setContentView(root);
        this.binding.connect.setOnClickListener(this::settingsPersistent);
    }

    @Override
    protected void onStart() {
        Log.d(SettingsActivity.class.getSimpleName(), "onStart");
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
        Log.d(SettingsActivity.class.getSimpleName(), "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(SettingsActivity.class.getSimpleName(), "onDestroy");
        super.onDestroy();
    }

    /**
     * 保存配置
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
        if(editor.commit()) {
            // 重连信令
            final Intent serviceIntent = new Intent(this, MediaService.class);
            serviceIntent.setAction(MediaService.Action.RECONNECT.name());
            this.startService(serviceIntent);
            // 预览页面
            final Intent activityIntent = new Intent(this, MainActivity.class);
            this.startActivity(activityIntent);
            // 结束
            this.finish();
        } else {
            Toast.makeText(this.getApplicationContext(), "配置保存失败", Toast.LENGTH_SHORT).show();
        }
    }

}
