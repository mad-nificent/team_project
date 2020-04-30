package team_project.matt.vehicle_simulator;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

@SuppressLint("ClickableViewAccessibility")
public class Start extends AppCompatActivity implements View.OnTouchListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Button start = findViewById(R.id.btnStart);
        start.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            // launch vehicle interface
            Intent intent = new Intent(this, VehicleInterface.class);
            startActivity(intent);
        }

        return true;
    }
}