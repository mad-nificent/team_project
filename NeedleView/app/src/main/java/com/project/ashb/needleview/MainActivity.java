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

    int current_pos = 135;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

         iv = (ImageView)findViewById(R.id.needle);
         et = (EditText)findViewById(R.id.edit);


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
        RotateAnimation rotateAnimation = new RotateAnimation(current_pos, 135 + Integer.parseInt(et.getText().toString()), bottom_iv, right_iv);
        current_pos = 135 + Integer.parseInt(et.getText().toString());
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setDuration(2000);
        iv.startAnimation(rotateAnimation);
    }
}
