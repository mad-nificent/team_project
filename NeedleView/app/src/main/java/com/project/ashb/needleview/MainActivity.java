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

    ImageView iv_speed;
    ImageView iv_battery;

    EditText et;
    EditText et_battery;
    int bottom_iv;
    int right_iv;
    int bottom_iv1;
    int right_iv1;

    float current_pos;
    float current_pos1;
    final float starting_pos = 105;
    final float starting_pos1 = 125;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iv_speed = (ImageView)findViewById(R.id.needle);
        iv_battery = (ImageView)findViewById(R.id.needleBattery);
        et = (EditText)findViewById(R.id.edit);
        et_battery = (EditText)findViewById(R.id.editBattery);

        current_pos = starting_pos;
        current_pos1 = starting_pos1;

        Rect rect = new Rect();
        iv_speed.getLocalVisibleRect(rect);
        bottom_iv = rect.bottom;
        right_iv = rect.right;

        RotateAnimation rotateAnimation = new RotateAnimation(0, current_pos, bottom_iv, right_iv);
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setDuration(500);
        iv_speed.startAnimation(rotateAnimation);

        Rect rect1 = new Rect();
        iv_battery.getLocalVisibleRect(rect1);
        bottom_iv1 = rect1.bottom;
        right_iv1 = rect1.right;

        RotateAnimation rotateAnimation1 = new RotateAnimation(0, current_pos1, bottom_iv1, right_iv1);
        rotateAnimation1.setFillAfter(true);
        rotateAnimation1.setDuration(500);
        iv_battery.startAnimation(rotateAnimation1);


    }

    public void onClickbtnSet(View view) {
        RotateAnimation rotateAnimation = new RotateAnimation(current_pos, starting_pos + Integer.parseInt(et.getText().toString()) * 2.0f, bottom_iv, right_iv);
        current_pos = starting_pos + Integer.parseInt(et.getText().toString()) * 2.0f;
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setDuration(2000);
        iv_speed.startAnimation(rotateAnimation);

        RotateAnimation rotateAnimation1 = new RotateAnimation(current_pos1, starting_pos1 + Integer.parseInt(et_battery.getText().toString()) * 2.0f, bottom_iv1, right_iv1);
        current_pos1 = starting_pos1 + Integer.parseInt(et.getText().toString()) * 2.0f;
        rotateAnimation1.setFillAfter(true);
        rotateAnimation1.setDuration(2000);
        iv_battery.startAnimation(rotateAnimation1);
    }
}
