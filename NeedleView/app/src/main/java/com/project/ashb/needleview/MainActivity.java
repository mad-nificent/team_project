package com.project.ashb.needleview;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    ImageView iv;
    EditText et;
    int bottom_iv;
    int right_iv;

    float current_pos;
    final float starting_pos = 105;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iv = (ImageView)findViewById(R.id.needle);
        et = (EditText)findViewById(R.id.edit);

        current_pos = starting_pos;

        Rect rect = new Rect();
        iv.getLocalVisibleRect(rect);
        bottom_iv = rect.bottom;
        right_iv = rect.right;

        RotateAnimation rotateAnimation = new RotateAnimation(0, current_pos, bottom_iv, right_iv);
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setDuration(500);
        iv.startAnimation(rotateAnimation);


    }

    public void onClickbtnSet(View view) {
        RotateAnimation rotateAnimation = new RotateAnimation(current_pos, starting_pos + Integer.parseInt(et.getText().toString()) * 2.0f, bottom_iv, right_iv);
        current_pos = starting_pos + Integer.parseInt(et.getText().toString()) * 2.0f;
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setDuration(2000);
        iv.startAnimation(rotateAnimation);
    }
}
