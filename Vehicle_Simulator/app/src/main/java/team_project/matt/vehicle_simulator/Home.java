package team_project.matt.vehicle_simulator;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

public class Home
        extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback
{
    // vehicle service manages characteristics and communicates changes to BLE
    VehicleService vehicleService;
    
    // location permission required for BLE to work
    final int   REQUEST_CODE_LOCATION = 1;
    boolean     hasLocationPermission = false;      // write this to shared prefs later
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        // does the app have location permissions?
        int currentLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        
        // no, request the permission from the user
        if (currentLocationPermission != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
        
        initialiseVehicle();
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
    
    public void initialiseVehicle()
    {
        hasLocationPermission = true;
        vehicleService = new VehicleService(new BluetoothLE(getApplicationContext()));
    }
    
    // starts the server and advertising process
    public void onStartServerClick(View view)
    {
        if (hasLocationPermission)
        {
            if (vehicleService.bluetoothLE.startAdvertising(vehicleService.getUUID()))
            {
                if (vehicleService.bluetoothLE.startGATT(vehicleService.getUUID(), vehicleService.getCharacteristicUUIDs(), vehicleService.getCharacteristicFormats()))
                {
                    setContentView(R.layout.prototype_interface);
                    
                    vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL).setData(100);
                    vehicleService.getCharacteristic(VehicleService.Property.SPEED).setData(0);
                    vehicleService.getCharacteristic(VehicleService.Property.DISTANCE).setData(0);
                    vehicleService.getCharacteristic(VehicleService.Property.LIGHTS).setData(vehicleService.STATE_OFF);
                    vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL).setData(vehicleService.STATE_OFF);
                }
            }
        }
    }
    
    // update all modified data and send out a notification
    public void onUpdateClick(View view)
    {
        EditText txtBatteryLevel = findViewById(R.id.txtBattery);
        EditText txtSpeed        = findViewById(R.id.txtSpeed);
        
        VehicleService.Characteristic battery = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL);
        VehicleService.Characteristic speed   = vehicleService.getCharacteristic(VehicleService.Property.SPEED);
        
        // dont send blank string
        if (txtBatteryLevel.getText().toString().equals("")) txtBatteryLevel.setText("0");
        if (txtSpeed.getText().toString().equals(""))        txtSpeed.setText("0");
        
        // update values
        battery.setData(Integer.parseInt(txtBatteryLevel.getText().toString()));
        speed.setData(Integer.parseInt(txtSpeed.getText().toString()));
    }
    
    public void onMinusClick(View view)
    {
        EditText txtView;
        Button btn = findViewById(view.getId());
        VehicleService.Characteristic characteristic = null;
        
        if (view.getId() == R.id.btnDecreaseBattery)
        {
            txtView = findViewById(R.id.txtBattery);
            characteristic = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL);
        }
        
        else if (view.getId() == R.id.btnDecreaseSpeed)
        {
            txtView = findViewById(R.id.txtSpeed);
            characteristic = vehicleService.getCharacteristic(VehicleService.Property.SPEED);
        }
        
        else return;
        
        // update text view and characteristic
        txtView.setText(String.format(Locale.getDefault(), "%d", characteristic.getData() - 1));
        characteristic.setData(Integer.parseInt(txtView.getText().toString()));
    }
    
    public void onPlusClick(View view)
    {
        EditText txtView;
        Button btn = findViewById(view.getId());
        VehicleService.Characteristic characteristic = null;
        
        if (view.getId() == R.id.btnIncreaseBattery)
        {
            txtView = findViewById(R.id.txtBattery);
            characteristic = vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL);
        }
        
        else if (view.getId() == R.id.btnIncreaseSpeed)
        {
            txtView = findViewById(R.id.txtSpeed);
            characteristic = vehicleService.getCharacteristic(VehicleService.Property.SPEED);
        }
        
        else return;
        
        // update text view and characteristic
        txtView.setText(String.format(Locale.getDefault(), "%d", characteristic.getData() + 1));
        characteristic.setData(Integer.parseInt(txtView.getText().toString()));
    }
    
    // turn on left indicator
    public void onLeftClick(View view)
    {
        VehicleService.Characteristic turnSignal = vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL);
        
        // toggle off
        if (turnSignal.getData() == vehicleService.STATE_SIGNAL_LEFT)
            turnSignal.setData(vehicleService.STATE_OFF);
            
            // toggle on
        else turnSignal.setData(vehicleService.STATE_SIGNAL_LEFT);
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
}