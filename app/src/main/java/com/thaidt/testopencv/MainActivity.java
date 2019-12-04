package com.thaidt.testopencv;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

            if (OpenCVLoader.initDebug()) {
                Log.i(TAG, "OpenCV initialize success");
            } else {
                Log.i(TAG, "OpenCV initialize failed");
            }
    }
}
