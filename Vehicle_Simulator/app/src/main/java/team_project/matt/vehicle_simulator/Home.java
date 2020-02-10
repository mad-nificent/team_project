package team_project.matt.vehicle_simulator;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.widget.Toast;

import java.util.Locale;

public class Home
        extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback
{
    // vehicle service manages characteristics and communicates changes to BLE
    Vehicle vehicleService;
    
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
        vehicleService = new Vehicle(new BluetoothLE(getApplicationContext()));
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
                    Button btnPlus   = findViewById(R.id.btnPlus);
                    Button btnMinus  = findViewById(R.id.btnMinus);
                    Button btnUpdate = findViewById(R.id.btnUpdate);
                    Button btnLeft   = findViewById(R.id.btnLeft);
                    Button btnRight  = findViewById(R.id.btnRight);
    
                    btnPlus.setEnabled(true);
                    btnMinus.setEnabled(true);
                    btnUpdate.setEnabled(true);
                    btnLeft.setEnabled(true);
                    btnRight.setEnabled(true);
                }
            }
        }
    }
    
    // update all modified data and send out a notification
    public void onUpdateClick(View view)
    {
        EditText txtBatteryLevel = findViewById(R.id.txtBattery);
        EditText txtSpeed        = findViewById(R.id.txtSpeed);
        
        Vehicle.Characteristic battery = vehicleService.getCharacteristic(Vehicle.Property.BATTERY_LVL);
        Vehicle.Characteristic speed   = vehicleService.getCharacteristic(Vehicle.Property.SPEED);
        
        // dont send blank string
        if (txtBatteryLevel.getText().toString().equals("")) txtBatteryLevel.setText("0");
        if (txtSpeed.getText().toString().equals(""))        txtSpeed.setText("0");
        
        // update values
        battery.setData(Integer.parseInt(txtBatteryLevel.getText().toString()));
        speed.setData(Integer.parseInt(txtSpeed.getText().toString()));
    }
    
    public void onMinusClick(View view)
    {
        EditText txtBatteryLevel = findViewById(R.id.txtBattery);
        EditText txtSpeed        = findViewById(R.id.txtSpeed);
    
        Vehicle.Characteristic battery = vehicleService.getCharacteristic(Vehicle.Property.BATTERY_LVL);
        Vehicle.Characteristic speed   = vehicleService.getCharacteristic(Vehicle.Property.SPEED);
    
        txtBatteryLevel.setText(String.format(Locale.getDefault(), "%d", battery.getData() - 1));
        txtSpeed.setText(String.format(Locale.getDefault(), "%d", speed.getData() - 1));
    
        // update values
        battery.setData(Integer.parseInt(txtBatteryLevel.getText().toString()));
        speed.setData(Integer.parseInt(txtSpeed.getText().toString()));
    }
    
    public void onPlusClick(View view)
    {
        EditText txtBatteryLevel = findViewById(R.id.txtBattery);
        EditText txtSpeed        = findViewById(R.id.txtSpeed);
    
        Vehicle.Characteristic battery = vehicleService.getCharacteristic(Vehicle.Property.BATTERY_LVL);
        Vehicle.Characteristic speed   = vehicleService.getCharacteristic(Vehicle.Property.SPEED);
    
        txtBatteryLevel.setText(String.format(Locale.getDefault(), "%d", battery.getData() + 1));
        txtSpeed.setText(String.format(Locale.getDefault(), "%d", speed.getData() + 1));
    
        // update values
        battery.setData(Integer.parseInt(txtBatteryLevel.getText().toString()));
        speed.setData(Integer.parseInt(txtSpeed.getText().toString()));
    }
    
    // turn on left indicator
    public void onLeftClick(View view)
    {
        Vehicle.Characteristic turnSignal = vehicleService.getCharacteristic(Vehicle.Property.TURN_SIGNAL);
        
        // toggle off
        if (turnSignal.getData() == vehicleService.STATE_SIGNAL_LEFT)
            turnSignal.setData(vehicleService.STATE_OFF);
        
        // toggle on
        else turnSignal.setData(vehicleService.STATE_SIGNAL_LEFT);
    }
    
    // turn on right indicator
    public void onRightClick(View view)
    {
        Vehicle.Characteristic turnSignal = vehicleService.getCharacteristic(Vehicle.Property.TURN_SIGNAL);
    
        // toggle off
        if (turnSignal.getData() == vehicleService.STATE_SIGNAL_RIGHT)
            turnSignal.setData(vehicleService.STATE_OFF);
        
            // toggle on
        else turnSignal.setData(vehicleService.STATE_SIGNAL_RIGHT);
    }
}
