package team_project.matt.vehicle_simulator;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class Home
        extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback
{
    // vehicle service manages characteristics and communicates changes to BLE
    VehicleService vehicleService;
    
    // location permission required for BLE to work
    final int          REQUEST_CODE_LOCATION = 1;
    final int REQUEST_CODE_BLUETOOTH_ENABLED = 2;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        requestPermissions();   // will close down if permission denied
        startBluetoothDevice();
    }
    
    private void requestPermissions()
    {
        // does the app have location permissions?
        int currentLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
    
        // no, request the permission from the user
        if (currentLocationPermission != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch(requestCode)
        {
            // location permission
            case REQUEST_CODE_LOCATION:
            {
                // will exit if not granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED)
                {
                    Toast toast = Toast.makeText(this, "App requires location permissions to function. Closing down.", Toast.LENGTH_LONG);
                    toast.show();
                    
                    finish();
                }
            }
        }
    }
    
    public void startBluetoothDevice()
    {
        vehicleService = new VehicleService(new BluetoothLE(getApplicationContext()));
        
        // start bluetooth hardware in app, returns false if bluetooth off
        if (!vehicleService.bluetoothLE.setDefaultBluetoothAdapter())
        {
            // request bluetooth be turned on
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_CODE_BLUETOOTH_ENABLED);
        }
        
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch(requestCode)
        {
            case REQUEST_CODE_BLUETOOTH_ENABLED:
            {
                if (resultCode != RESULT_OK)
                {
                    Toast toast = Toast.makeText(this, "App requires bluetooth to function.", Toast.LENGTH_LONG);
                    toast.show();
                }
    
                break;
            }
        }
    }
    
    // starts the server and advertising process
    public void onStartServerClick(View view)
    {
        // start advertising, returns false if bluetooth disabled
        if (!vehicleService.bluetoothLE.startAdvertising(vehicleService.getUUID()))
            return;
        
        // start GATT, returns false if already started
        if (!vehicleService.bluetoothLE.startGATT(vehicleService.getUUID(), vehicleService.getCharacteristicUUIDs(), vehicleService.getCharacteristicFormats(), vehicleService.getDescriptor()))
            return;
        
        vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL).setData(100);
        vehicleService.getCharacteristic(VehicleService.Property.RANGE).setData(0);                             //get from shared prefs later
        vehicleService.getCharacteristic(VehicleService.Property.BATTERY_TEMP).setData(20);                     // safe temp 20-45c
        vehicleService.getCharacteristic(VehicleService.Property.SPEED).setData(0);
        vehicleService.getCharacteristic(VehicleService.Property.RPM).setData(0);
        vehicleService.getCharacteristic(VehicleService.Property.DISTANCE).setData(0);
        vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL).setData(vehicleService.STATE_OFF);
        vehicleService.getCharacteristic(VehicleService.Property.LIGHTS).setData(vehicleService.STATE_OFF);
        vehicleService.getCharacteristic(VehicleService.Property.HANDBRAKE).setData(vehicleService.STATE_ON);
        vehicleService.getCharacteristic(VehicleService.Property.SEATBELT).setData(vehicleService.STATE_OFF);
        vehicleService.getCharacteristic(VehicleService.Property.TYRE_PRESSURE_LOW).setData(vehicleService.STATE_OFF);
        vehicleService.getCharacteristic(VehicleService.Property.WIPER_LOW).setData(vehicleService.STATE_OFF);
        vehicleService.getCharacteristic(VehicleService.Property.AIRBAG_ERR).setData(vehicleService.STATE_OFF);
        vehicleService.getCharacteristic(VehicleService.Property.BRAKE_ERR).setData(vehicleService.STATE_OFF);
        vehicleService.getCharacteristic(VehicleService.Property.ABS_ERR).setData(vehicleService.STATE_OFF);
        vehicleService.getCharacteristic(VehicleService.Property.ENGIN_ERR).setData(vehicleService.STATE_OFF);
        
        loadVehicleInterface();
    }
    
    private void loadVehicleInterface()
    {
        setContentView(R.layout.prototype_interface);
        
        EditText battery = findViewById(R.id.txtBattery);
        EditText speed   = findViewById(R.id.txtSpeed);
    
        VehicleService.Characteristic batteryData = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL);
        battery.setText(String.format(Locale.getDefault(), "%d", batteryData.getData()));
    
        VehicleService.Characteristic speedData = vehicleService.getCharacteristic(VehicleService.Property.SPEED);
        speed.setText(String.format(Locale.getDefault(), "%d", speedData.getData()));
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

            case R.id.btnDecreaseSpeed:
            {
                txtView = findViewById(R.id.txtSpeed);
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.SPEED);
                break;
            }

            case R.id.btnDecreaseRPM:
            {
                txtView = findViewById(R.id.txtRPM);
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.RPM);
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

            case R.id.btnIncreaseSpeed:
            {
                txtView = findViewById(R.id.txtSpeed);
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.SPEED);
                max = 120;
                break;
            }

            case R.id.btnIncreaseRPM:
            {
                txtView = findViewById(R.id.txtRPM);
                characteristic = vehicleService.getCharacteristic(VehicleService.Property.RPM);
                max = 4000;
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
