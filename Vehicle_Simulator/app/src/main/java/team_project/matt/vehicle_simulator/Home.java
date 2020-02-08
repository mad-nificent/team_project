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
            if (bluetoothLE.startAdvertising())
            {
                Button btnPlus = findViewById(R.id.btnPlus);
                Button btnMinus = findViewById(R.id.btnMinus);
                Button btnUpdate = findViewById(R.id.btnUpdate);
                Button btnLeft = findViewById(R.id.btnLeft);
                Button btnRight = findViewById(R.id.btnRight);
    
                btnPlus.setEnabled(true);
                btnMinus.setEnabled(true);
                btnUpdate.setEnabled(true);
                btnLeft.setEnabled(true);
                btnRight.setEnabled(true);
            }
        }
    }
    
    public void onMinusClick(View view)
    {
        EditText batteryLevel = findViewById(R.id.txtBattery);
        EditText speed        = findViewById(R.id.txtSpeed);
    
        batteryLevel.setText(BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.BATTERY).getStringValue(0));
        speed.setText(BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.SPEED).getStringValue(0));
    
        // update values
        BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.BATTERY).setValue(Integer.toString(Integer.parseInt(batteryLevel.getText().toString()) - 1));
        BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.SPEED).setValue(Integer.toString(Integer.parseInt(speed.getText().toString()) - 1));
    
        for (int i = 0; i < bluetoothLE.devices.size(); ++i)
        {
            bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(i), BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.BATTERY), false);
            bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(i), BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.SPEED), false);
        }
    }
    
    public void onPlusClick(View view)
    {
        EditText batteryLevel = findViewById(R.id.txtBattery);
        EditText speed        = findViewById(R.id.txtSpeed);
        
        batteryLevel.setText(BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.BATTERY).getStringValue(0));
        speed.setText(BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.SPEED).getStringValue(0));
    
        // update values
        BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.BATTERY).setValue(Integer.toString(Integer.parseInt(batteryLevel.getText().toString()) + 1));
        BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.SPEED).setValue(Integer.toString(Integer.parseInt(speed.getText().toString()) + 1));
    
        for (int i = 0; i < bluetoothLE.devices.size(); ++i)
        {
            bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(i), BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.BATTERY), false);
            bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(i), BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.SPEED), false);
        }
    }
    
    // turn on left indicator
    public void onLeftClick(View view)
    {
        BluetoothGattCharacteristic indicator = BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.INDICATOR);
        
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
        BluetoothGattCharacteristic indicator = BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.INDICATOR);
    
        // toggle off
        if (indicator.getStringValue(0).equals(BluetoothLE.Characteristics.INDICATOR_RIGHT))
            indicator.setValue(BluetoothLE.Characteristics.INDICATOR_NONE);
        
        // toggle on
        else indicator.setValue(BluetoothLE.Characteristics.INDICATOR_RIGHT);
    
        // notify all devices of change
        for (int i = 0; i < bluetoothLE.devices.size(); ++i)
            bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(i), indicator, false);
    }
    
    // update all modified data and send out a notification
    public void onUpdateClick(View view)
    {
        EditText batteryLevel = findViewById(R.id.txtBattery);
        EditText speed        = findViewById(R.id.txtSpeed);

        // dont send blank string
        if (batteryLevel.getText().toString().equals("")) batteryLevel.setText("0");
        if (speed.getText().toString().equals(""))        speed.setText("0");

        // update values
        BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.BATTERY).setValue(batteryLevel.getText().toString());
        BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.SPEED).setValue(speed.getText().toString());

        for (int i = 0; i < bluetoothLE.devices.size(); ++i)
        {
            bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(i), BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.BATTERY), false);
            bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(i), BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.SPEED), false);
        }
        
//        for (int i = 0; i < 101; ++i)
//        {
//            BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.BATTERY).setValue(Integer.toString(i));
//            BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.SPEED).setValue(Integer.toString(i));
//
//            for (int j = 0; i < bluetoothLE.devices.size(); ++j)
//            {
//                bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(j), BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.BATTERY), false);
//                bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(j), BluetoothLE.Characteristics.data.get(BluetoothLE.Characteristics.SPEED), false);
//            }
//        }
    }
}
