package team_project.matt.vehicle_simulator;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

public class Home extends AppCompatActivity implements SendToDisplay, BluetoothRequest, ActivityCompat.OnRequestPermissionsResultCallback
{
    final int          REQUEST_CODE_LOCATION = 1;
    final int REQUEST_CODE_BLUETOOTH_ENABLED = 2;

    // respond to requests etc.
    SendUserResponse sendBluetoothResponse;

    @Override
    public void requestLocationPermission()
    {
        int locationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);

        // not granted yet
        if (locationPermission == PackageManager.PERMISSION_DENIED)
        {
            // ask for user choice and respond to request
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
        }

        // already granted, immediately respond
        else sendBluetoothResponse.locationPermissionResult(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_CODE_LOCATION)
        {
            boolean isPermissionGranted = false;
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) isPermissionGranted = true;
            sendBluetoothResponse.locationPermissionResult(isPermissionGranted);
        }
    }

    @Override
    public void requestEnableBluetoothAdapter()
    {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, REQUEST_CODE_BLUETOOTH_ENABLED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_CODE_BLUETOOTH_ENABLED)
        {
            boolean isEnabled = false;
            if (resultCode == RESULT_OK) isEnabled = true;
            sendBluetoothResponse.adapterStatus(isEnabled);
        }
    }

    @Override
    public void showToast(final String message, final int length)
    {
        Toast.makeText(this, message, length).show();
    }

    @Override
    public void updateDeviceCount(int noDevices)
    {

    }

    class SpeedManager
    {
        private int IDLE = 0, ACCELERATING = 1, BRAKING = 2;

        private int      state = IDLE;
        private EditText txtSpeed;
        private int      speed;

        SpeedManager()
        {
            txtSpeed = findViewById(R.id.txtSpeed);
            speed = Integer.parseInt(txtSpeed.getText().toString());
        }

        void accelerate()
        {
            if (state == IDLE)
            {
                // start
                state = ACCELERATING;

                // run in the background
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        final double increaseRate  = 1.0075;
                        final int    baseSleepTime = 75;

                        // increase speed
                        while (state == ACCELERATING)
                        {
                            speed += 1;
                            Log.d("Speed", "Updated speed: " + Integer.toString(speed));

                            // update UI
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    txtSpeed.setText(String.valueOf(speed));
                                }
                            });

                            try
                            {
                                double sleepTime = baseSleepTime * Math.pow(increaseRate, speed);
                                Log.d("sleeptime", "sleep: " + sleepTime);

                                Thread.sleep((int)Math.ceil(sleepTime));
                            }

                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        }

        void decelerate()
        {
            if (state != IDLE)
            {
                // start
                state = IDLE;

                // run in the background
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // decrease speed
                        while (state == IDLE)
                        {
                            speed -= 1;
                            Log.d("Speed", "Updated speed: " + Integer.toString(speed));

                            // update UI
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    txtSpeed.setText(String.valueOf(speed));
                                }
                            });

                            try
                            {
                                Thread.sleep(500);
                            }

                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        }

        void brake()
        {
            if (state != BRAKING)
            {
                // start
                state = BRAKING;

                // run in the background
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        final int sleepTime = 10;

                        // decrease speed
                        while (state == BRAKING)
                        {
                            speed -= 1;
                            Log.d("Speed", "Updated speed: " + Integer.toString(speed));

                            // update UI
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    txtSpeed.setText(String.valueOf(speed));
                                }
                            });

                            try
                            {
                                Thread.sleep(sleepTime);
                            }

                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        }
    }

    // vehicle service manages characteristics and communicates changes to BLE
    VehicleService vehicleService;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // create a temp screen

        vehicleService = new VehicleService(this);
        sendBluetoothResponse = vehicleService;

        vehicleService.start();

        setContentView(R.layout.prototype_interface);

//        loadVehicleInterface();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void loadVehicleInterface()
    {
        setContentView(R.layout.prototype_interface);
        
        EditText battery = findViewById(R.id.txtBattery);
        EditText speed   = findViewById(R.id.txtSpeed);
    
        VehicleService.Characteristic batteryData = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL);
        battery.setText(String.format(Locale.getDefault(), "%d", batteryData.getData()));
    
        VehicleService.Characteristic speedData = vehicleService.getCharacteristic(VehicleService.Property.SPEED);
        speed.setText(String.format(Locale.getDefault(), "%d", speedData.getData()));

        final SpeedManager speedManager = new SpeedManager();

        Button btnAccelerate = findViewById(R.id.btnIncreaseSpeed);
        btnAccelerate.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    speedManager.accelerate();

                else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_POINTER_UP)
                    speedManager.decelerate();

                return true;
            }
        });

        Button btnBrake = findViewById(R.id.btnDecreaseSpeed);
        btnBrake.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    speedManager.brake();

                else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_POINTER_UP)
                    speedManager.decelerate();

                return true;
            }
        });
    }

    public void onMinusClick(View view)
    {
        EditText txtView = null;
        VehicleService.Characteristic characteristic = null;

        int ID = view.getId();
        switch (ID)
        {
            case R.id.btnDecreaseBattery:
            {
                txtView = findViewById(R.id.txtBattery);
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL);
                break;
            }

            case R.id.btnDecreaseRange:
            {
                txtView = findViewById(R.id.txtRange);
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.RANGE);
                break;
            }

            case R.id.btnDecreaseTemp:
            {
                txtView = findViewById(R.id.txtTemp);
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_TEMP);
                break;
            }

            case R.id.btnDecreaseDistance:
            {
                txtView = findViewById(R.id.txtDistance);
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.DISTANCE);
                break;
            }
        }

        if (characteristic != null && txtView != null)
        {
            int data = characteristic.getData();
            if (data > 0)
            {
                data -= 1;

                // update text view and characteristic
                txtView.setText(String.format(Locale.getDefault(), "%d", data));
                characteristic.setData(data);
            }
        }
    }
    
    public void onPlusClick(View view)
    {
        EditText txtView = null;
        VehicleService.Characteristic characteristic = null;
        int max = 0;

        int ID = view.getId();
        switch (ID)
        {
            case R.id.btnIncreaseBattery:
            {
                txtView = findViewById(R.id.txtBattery);
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL);
                max = 100;
                break;
            }

            case R.id.btnIncreaseRange:
            {
                txtView = findViewById(R.id.txtRange);
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.RANGE);
                max = 500;
                break;
            }

            case R.id.btnIncreaseTemp:
            {
                txtView = findViewById(R.id.txtTemp);
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_TEMP);
                max = 100;
                break;
            }

            case R.id.btnIncreaseDistance:
            {
                txtView = findViewById(R.id.txtDistance);
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.DISTANCE);
                max = Integer.MAX_VALUE;
                break;
            }
        }

        if (characteristic != null && txtView != null)
        {
            int data = characteristic.getData();
            if (data < max)
            {
                data += 1;

                // update text view and characteristic
                txtView.setText(String.format(Locale.getDefault(), "%d", data));
                characteristic.setData(data);
            }
        }
    }
    
    // turn on left indicator
    public void onLeftClick(View view)
    {
        VehicleService.Characteristic turnSignal = vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL);
        
        // toggle off
        if (turnSignal.getData() == vehicleService.STATE_SIGNAL_LEFT) turnSignal.setData(vehicleService.STATE_OFF);
            
        // toggle on
        else turnSignal.setData(vehicleService.STATE_SIGNAL_LEFT);
    }

    public void onLightsClick(View view)
    {
        VehicleService.Characteristic lights = vehicleService.getCharacteristic(VehicleService.Property.LIGHTS);

        if      (lights.getData() == vehicleService.STATE_OFF)        lights.setData(vehicleService.STATE_LIGHTS_LOW);
        else if (lights.getData() == vehicleService.STATE_LIGHTS_LOW) lights.setData(vehicleService.STATE_LIGHTS_HIGH);
        else                                                          lights.setData(vehicleService.STATE_OFF);
    }

    public void onBrakeClick(View view)
    {
        VehicleService.Characteristic brake = vehicleService.getCharacteristic(VehicleService.Property.HANDBRAKE);
        if (brake.getData() == vehicleService.STATE_OFF) brake.setData(vehicleService.STATE_ON);
        else                                             brake.setData(vehicleService.STATE_OFF);
    }
    
    // turn on right indicator
    public void onRightClick(View view)
    {
        VehicleService.Characteristic turnSignal = vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL);
        
        // toggle off
        if (turnSignal.getData() == vehicleService.STATE_SIGNAL_RIGHT)
            turnSignal.setData(vehicleService.STATE_OFF);
            
        // toggle on
        else turnSignal.setData(vehicleService.STATE_SIGNAL_RIGHT);
    }

    public void onWarningClick(View view)
    {
        VehicleService.Characteristic characteristic = null;
        boolean isMultiState = false;

        int ID = view.getId();
        switch (ID)
        {
            case R.id.btnSeatbeltWarning:
            {
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.SEATBELT);
                break;
            }

            case R.id.btnLightsWarning:
            {
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.LIGHTS);

                if (characteristic.getData() == vehicleService.STATE_OFF)
                    characteristic.setData(vehicleService.STATE_LIGHTS_ERR);

                else if (characteristic.getData() == vehicleService.STATE_LIGHTS_ERR)
                    characteristic.setData(vehicleService.STATE_OFF);

                return;
            }

            case R.id.btnWiperWarning:
            {
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.WIPER_LOW);
                break;
            }

            case R.id.btnTyresWarning:
            {
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.TYRE_PRESSURE_LOW);
                break;
            }

            case R.id.btnAirbagWarning:
            {
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.AIRBAG_ERR);
                break;
            }

            case R.id.btnBrakeWarning:
            {
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.BRAKE_ERR);
                isMultiState = true;
                break;
            }

            case R.id.btnAbsWarning:
            {
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.ABS_ERR);
                isMultiState = true;
                break;
            }

            case R.id.btnEngineWarning:
            {
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.ENGIN_ERR);
                isMultiState = true;
                break;
            }
        }

        if (characteristic != null)
        {
            if (characteristic.getData() == vehicleService.STATE_OFF)
                characteristic.setData(vehicleService.STATE_WARNING_LOW);

            else if (characteristic.getData() == vehicleService.STATE_WARNING_LOW)
            {
                if (isMultiState) characteristic.setData(vehicleService.STATE_WARNING_HIGH);
                else              characteristic.setData(vehicleService.STATE_OFF);
            }

            else if (characteristic.getData() == vehicleService.STATE_WARNING_HIGH)
                characteristic.setData(vehicleService.STATE_OFF);
        }
    }
}
