package com.acgist.taoyao.client;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.acgist.taoyao.client.databinding.ActivityMainBinding;

import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("taoyao");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());
    }

    public native String stringFromJNI();

}