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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

public class Home extends AppCompatActivity
        implements Display, BluetoothNotification, ActivityCompat.OnRequestPermissionsResultCallback
{
    final int          REQUEST_CODE_LOCATION = 1;
    final int REQUEST_CODE_BLUETOOTH_ENABLED = 2;

    // responds to BLE interface
    BluetoothNotificationResponse respond;

    // manages interface between user and the service, such as managing speed, battery etc.
    VehicleManager vehicle;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_loading);

        // holds all info about characteristics and the service itself, and communicates changes to BLE
        VehicleService vehicleService = new VehicleService();

        // create ble instance, this activity will take requests, vehicle interface will receieve status updates
        BluetoothLE bluetoothDevice = new BluetoothLE(this, vehicleService);

        // ble will receive user responses to requests
        respond = bluetoothDevice;

        // create interface to vehicle
        vehicle = new VehicleManager(vehicleService, this);

        // initiate setup of ble (requests permissions, enable bluetooth device etc.)
        // this runs asynchronously, and results are received through interface
        vehicleService.beginSetup(bluetoothDevice, this);
    }

    // once ble is started, it will call this method to request location permission from the user
    @Override
    public void requestLocation()
    {
        int locationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        // not granted yet
        if (locationPermission == PackageManager.PERMISSION_DENIED)
        {
            // ask for user choice and respond to request
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
        }

        // grant access to ble
        else respond.requestLocationResult(true);
    }

    // if permissions are needed, ask for them and respond to ble with user choice
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_CODE_LOCATION)
        {
            boolean isPermissionGranted = false;
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) isPermissionGranted = true;
            respond.requestLocationResult(isPermissionGranted);
        }
    }

    // once permissions are granted ble will request bluetooth hardware is switched on if its currently off
    @Override
    public void enableAdapter()
    {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, REQUEST_CODE_BLUETOOTH_ENABLED);
    }

    // once user has provided their choice, this method is called and returns result to ble
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_CODE_BLUETOOTH_ENABLED)
        {
            boolean isEnabled = false;
            if (resultCode == RESULT_OK) isEnabled = true;
            respond.enableAdapterResult(isEnabled);
        }
    }

    // ble will call this method if user denies permission or bluetooth access, or if any internal error occurs
    @Override
    public void setupFailed(String message)
    {
        showToast(message, Toast.LENGTH_LONG);
        finish();
    }

    // setup went smoothly, start the vehicle
    @Override
    public void setupComplete()
    {
        vehicle.start();
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

    // this method is called by the vehicle service once it has launched GATT
    @Override
    public void vehicleStarted()
    {
        loadVehicleInterface();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void loadVehicleInterface()
    {
        setContentView(R.layout.prototype_interface);

        Button btnAccelerate = findViewById(R.id.btnIncreaseSpeed);
        btnAccelerate.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.accelerate();

                else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_POINTER_UP)
                    vehicle.idle();

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
                    vehicle.brake();

                else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_POINTER_UP)
                    vehicle.idle();

                return true;
            }
        });
    }

    @Override
    public void chargeMode(boolean isCharging)
    {
        // switch off controls
    }

    @Override
    public void updateChargeLevel(final int charge)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                EditText txtBattery = findViewById(R.id.txtBattery);
                txtBattery.setText(String.format(Locale.getDefault(), "%d", charge) + "%");
            }
        });
    }

    @Override
    public void updateRange(final int milesLeft)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                EditText txtRange = findViewById(R.id.txtRange);
                txtRange.setText(String.format(Locale.getDefault(), "%d", milesLeft) + " miles left");
            }
        });
    }

    @Override
    public void updateSpeed(final int speed)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                EditText txtSpeed = findViewById(R.id.txtSpeed);
                txtSpeed.setText(String.format(Locale.getDefault(), "%d", speed) + " MPH");
            }
        });
    }

    public void onMinusClick(View view)
    {
//        EditText txtView = null;
//        VehicleService.Characteristic characteristic = null;
//
//        int ID = view.getId();
//        switch (ID)
//        {
//            case R.id.btnDecreaseBattery:
//            {
//                txtView = findViewById(R.id.txtBattery);
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL);
//                break;
//            }
//
//            case R.id.btnDecreaseRange:
//            {
//                txtView = findViewById(R.id.txtRange);
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.RANGE);
//                break;
//            }
//
//            case R.id.btnDecreaseTemp:
//            {
//                txtView = findViewById(R.id.txtTemp);
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_TEMP);
//                break;
//            }
//
//            case R.id.btnDecreaseDistance:
//            {
//                txtView = findViewById(R.id.txtDistance);
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.DISTANCE);
//                break;
//            }
//        }
//
//        if (characteristic != null && txtView != null)
//        {
//            int data = characteristic.getData();
//            if (data > 0)
//            {
//                data -= 1;
//
//                // update text view and characteristic
//                txtView.setText(String.format(Locale.getDefault(), "%d", data));
//                characteristic.setData(data);
//            }
//        }
    }

    public void onPlusClick(View view)
    {
//        EditText txtView = null;
//        VehicleService.Characteristic characteristic = null;
//        int max = 0;
//
//        int ID = view.getId();
//        switch (ID)
//        {
//            case R.id.btnIncreaseBattery:
//            {
//                txtView = findViewById(R.id.txtBattery);
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL);
//                max = 100;
//                break;
//            }
//
//            case R.id.btnIncreaseRange:
//            {
//                txtView = findViewById(R.id.txtRange);
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.RANGE);
//                max = 500;
//                break;
//            }
//
//            case R.id.btnIncreaseTemp:
//            {
//                txtView = findViewById(R.id.txtTemp);
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_TEMP);
//                max = 100;
//                break;
//            }
//
//            case R.id.btnIncreaseDistance:
//            {
//                txtView = findViewById(R.id.txtDistance);
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.DISTANCE);
//                max = Integer.MAX_VALUE;
//                break;
//            }
//        }
//
//        if (characteristic != null && txtView != null)
//        {
//            int data = characteristic.getData();
//            if (data < max)
//            {
//                data += 1;
//
//                // update text view and characteristic
//                txtView.setText(String.format(Locale.getDefault(), "%d", data));
//                characteristic.setData(data);
//            }
//        }
    }

    // turn on left indicator
    public void onLeftClick(View view)
    {
//        VehicleService.Characteristic turnSignal = vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL);
//
//        // toggle off
//        if (turnSignal.getData() == vehicleService.STATE_SIGNAL_LEFT) turnSignal.setData(vehicleService.STATE_OFF);
//
//        // toggle on
//        else turnSignal.setData(vehicleService.STATE_SIGNAL_LEFT);
    }

    public void onLightsClick(View view)
    {
//        VehicleService.Characteristic lights = vehicleService.getCharacteristic(VehicleService.Property.LIGHTS);
//
//        if      (lights.getData() == vehicleService.STATE_OFF)        lights.setData(vehicleService.STATE_LIGHTS_LOW);
//        else if (lights.getData() == vehicleService.STATE_LIGHTS_LOW) lights.setData(vehicleService.STATE_LIGHTS_HIGH);
//        else                                                          lights.setData(vehicleService.STATE_OFF);
    }

    public void onBrakeClick(View view)
    {
//        VehicleService.Characteristic brake = vehicleService.getCharacteristic(VehicleService.Property.HANDBRAKE);
//        if (brake.getData() == vehicleService.STATE_OFF) brake.setData(vehicleService.STATE_ON);
//        else                                             brake.setData(vehicleService.STATE_OFF);
    }

    // turn on right indicator
    public void onRightClick(View view)
    {
//        VehicleService.Characteristic turnSignal = vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL);
//
//        // toggle off
//        if (turnSignal.getData() == vehicleService.STATE_SIGNAL_RIGHT)
//            turnSignal.setData(vehicleService.STATE_OFF);
//
//        // toggle on
//        else turnSignal.setData(vehicleService.STATE_SIGNAL_RIGHT);
    }

    public void onWarningClick(View view)
    {
//        VehicleService.Characteristic characteristic = null;
//        boolean isMultiState = false;
//
//        int ID = view.getId();
//        switch (ID)
//        {
//            case R.id.btnSeatbeltWarning:
//            {
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.SEATBELT);
//                break;
//            }
//
//            case R.id.btnLightsWarning:
//            {
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.LIGHTS);
//
//                if (characteristic.getData() == vehicleService.STATE_OFF)
//                    characteristic.setData(vehicleService.STATE_LIGHTS_ERR);
//
//                else if (characteristic.getData() == vehicleService.STATE_LIGHTS_ERR)
//                    characteristic.setData(vehicleService.STATE_OFF);
//
//                return;
//            }
//
//            case R.id.btnWiperWarning:
//            {
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.WIPER_LOW);
//                break;
//            }
//
//            case R.id.btnTyresWarning:
//            {
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.TYRE_PRESSURE_LOW);
//                break;
//            }
//
//            case R.id.btnAirbagWarning:
//            {
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.AIRBAG_ERR);
//                break;
//            }
//
//            case R.id.btnBrakeWarning:
//            {
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.BRAKE_ERR);
//                isMultiState = true;
//                break;
//            }
//
//            case R.id.btnAbsWarning:
//            {
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.ABS_ERR);
//                isMultiState = true;
//                break;
//            }
//
//            case R.id.btnEngineWarning:
//            {
//                characteristic = vehicleService.getCharacteristic(VehicleService.Property.ENGIN_ERR);
//                isMultiState = true;
//                break;
//            }
//        }
//
//        if (characteristic != null)
//        {
//            if (characteristic.getData() == vehicleService.STATE_OFF)
//                characteristic.setData(vehicleService.STATE_WARNING_LOW);
//
//            else if (characteristic.getData() == vehicleService.STATE_WARNING_LOW)
//            {
//                if (isMultiState) characteristic.setData(vehicleService.STATE_WARNING_HIGH);
//                else              characteristic.setData(vehicleService.STATE_OFF);
//            }
//
//            else if (characteristic.getData() == vehicleService.STATE_WARNING_HIGH)
//                characteristic.setData(vehicleService.STATE_OFF);
//        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        vehicle.stop();
    }
}
