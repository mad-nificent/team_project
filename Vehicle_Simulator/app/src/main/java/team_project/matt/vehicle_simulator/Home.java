package team_project.matt.vehicle_simulator;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.widget.Toast;

public class Home
        extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback
{
    // required for bluetooth communication to client application
    BluetoothLE bluetoothLE;
    
    // location permission required for BLE to work
    final int   REQUEST_CODE_LOCATION = 1;
    boolean     hasLocationPermission = false;
    
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
        
        else
        {
            // yes, setup BLE
            bluetoothLE = new BluetoothLE(getApplicationContext());
            hasLocationPermission = true;
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch(requestCode)
        {
            // location permission
            case REQUEST_CODE_LOCATION:
            {
                // granted by user
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    bluetoothLE = new BluetoothLE(getApplicationContext());
                    hasLocationPermission = true;
                }
                
                else
                {
                    Toast toast = Toast.makeText(this, "App requires location permissions to function. Closing down.", Toast.LENGTH_LONG);
                    toast.show();
                    
                    finish();
                }
            }
        }
    }
    
    // starts the server and advertising process
    public void onStartServerClick(View view)
    {
        if (hasLocationPermission)
        {
            bluetoothLE.startAdvertising();
            
            Button btnUpdate = findViewById(R.id.btnUpdate);
            Button btnLeft   = findViewById(R.id.btnLeft);
            Button btnRight  = findViewById(R.id.btnRight);
            
            btnUpdate.setEnabled(true);
            btnLeft.setEnabled(true);
            btnRight.setEnabled(true);
        }
    }
    
    // turn on left indicator
    public void onLeftClick(View view)
    {
        BluetoothGattCharacteristic indicator = BluetoothLE.Characteristics.characteristics.get(BluetoothLE.Characteristics.INDICATOR_INDEX);
        
        // toggle off
        if (indicator.getStringValue(0).equals(BluetoothLE.Characteristics.INDICATOR_LEFT))
            indicator.setValue(BluetoothLE.Characteristics.INDICATOR_NONE);
        
        // toggle on
        else indicator.setValue(BluetoothLE.Characteristics.INDICATOR_LEFT);
        
        // notify all devices of change
        for (int i = 0; i < bluetoothLE.devices.size(); ++i)
            bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(i), indicator, false);
    }
    
    // turn on right indicator
    public void onRightClick(View view)
    {
        BluetoothGattCharacteristic indicator = BluetoothLE.Characteristics.characteristics.get(BluetoothLE.Characteristics.INDICATOR_INDEX);
    
        // toggle off
        if (indicator.getStringValue(0).equals(BluetoothLE.Characteristics.INDICATOR_RIGHT))
            indicator.setValue(BluetoothLE.Characteristics.INDICATOR_NONE);
        
        // toggle on
        else indicator.setValue(BluetoothLE.Characteristics.INDICATOR_RIGHT);
    
        // notify all devices of change
        for (int i = 0; i < bluetoothLE.devices.size(); ++i)
            bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(i), indicator, false);
    }
    
    // update all modified characteristics and send out a notification
    public void onUpdateClick(View view)
    {
        EditText batteryLevel = findViewById(R.id.txtBattery);
        EditText speed        = findViewById(R.id.txtSpeed);
    
        // update values
        BluetoothLE.Characteristics.characteristics.get(BluetoothLE.Characteristics.BATTERY_INDEX).setValue(batteryLevel.getText().toString());
        BluetoothLE.Characteristics.characteristics.get(BluetoothLE.Characteristics.SPEED_INDEX).setValue(speed.getText().toString());
    
        for (int i = 0; i < bluetoothLE.devices.size(); ++i)
        {
            bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(i), BluetoothLE.Characteristics.characteristics.get(BluetoothLE.Characteristics.BATTERY_INDEX), false);
            bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(i), BluetoothLE.Characteristics.characteristics.get(BluetoothLE.Characteristics.SPEED_INDEX), false);
        }
    }
}
