package team_project.matt.vehicle_simulator;

// bluetooth LE functionality
import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Home
        extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback
{
    BluetoothManager    bluetoothManager;
    BluetoothGattServer GATTServer;
    
    public static class characteristics { final static int BATTERY = 0, SPEED = 1, INDICATOR = 2; };
    
    List<String> CHARACTERISTIC_UUID = new ArrayList<String>(Collections.unmodifiableList(Arrays.asList
    (
                                            "76a247fb-a76f-42da-91ce-d6a5bdebd0e2"  ,
                                            "7b9b53ff-5421-4bdf-beb0-ca8c949542c1"  ,
                                            "74df0c8f-f3e1-4cf5-b875-56d7ca609a2e"  )));
    final String SERVICE_UUID    =          "0000180f-0000-1000-8000-00805f9b34fb";
    final String DESCRIPTOR_UUID =          "00002902-0000-1000-8000-00805f9b34fb";
    
    // location permissions
    final int   REQUEST_CODE_LOCATION = 1;
    Boolean     hasLocationPermission = false;
    
    Boolean GATTRunning         = false;
    
    // reports the result after starting the advertisement process
    AdvertiseCallback advertiseCallback = new AdvertiseCallback()
    {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect)
        {
            super.onStartSuccess(settingsInEffect);
            
            Toast toast = Toast.makeText(getApplicationContext(), "Started successfully.", Toast.LENGTH_SHORT);
            toast.show();
            
            startGATT();
        }
        
        @Override
        public void onStartFailure(int errorCode)
        {
            super.onStartFailure(errorCode);
            
            Toast toast = Toast.makeText(getApplicationContext(), "Failed to start, error code: " + errorCode, Toast.LENGTH_LONG);
            toast.show();
        }
    };
    
    // manages server operations such as incoming connections, clients reading data etc.
    BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback()
    {
        // a client has connected or disconnected
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState)
        {
            super.onConnectionStateChange(device, status, newState);
            
            if (newState == STATE_CONNECTED)
            {
                Toast toast = Toast.makeText(getApplicationContext(), device.getName().toString() + " has connected", Toast.LENGTH_SHORT);
                toast.show();
            }
            
            else if (newState == STATE_DISCONNECTED)
            {
                Toast toast = Toast.makeText(getApplicationContext(), device.getName().toString() + " has disconnected", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        
        @Override
        public void onServiceAdded(int status, BluetoothGattService service)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                Toast toast = Toast.makeText(getApplicationContext(), "Service: " + service.getUuid().toString() + " added.", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        
        // a client wants to read some data
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            GATTServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }
    };
    
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
        
        else hasLocationPermission = true;
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
                    hasLocationPermission = true;
                
                else
                {
                    // disable app
                }
            }
        }
    }
    
    // handles response after selecting start server button
    public void onStartServerClick(View view)
    {
        if (hasLocationPermission)
            startAdvertising();
    }
    
    public void onUpdateClick(View view)
    {
        if (GATTRunning)
        {
            GATTServer.getService(UUID.fromString(SERVICE_UUID)).getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID.get(characteristics.BATTERY))).setValue("10");
            Toast toast = Toast.makeText(this, GATTServer.getService(UUID.fromString(SERVICE_UUID)).getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID.get(characteristics.BATTERY))).getStringValue(0), Toast.LENGTH_LONG);
            toast.show();
        }
    }
    
    // sets the device to start advertising over ble
    public void startAdvertising()
    {
        // access the bluetooth device
        bluetoothManager         = (BluetoothManager)getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
    
        // bluetooth is turned off
        if (!adapter.isEnabled())
        {
            Toast toast = Toast.makeText(this, "The bluetooth device is disabled. Please enable it and try again.", Toast.LENGTH_LONG);
            toast.show();
        }
    
        else
        {
            // adjust preferences for advertising from this device
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)   // consume least amount of power at the cost of higher latency
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)      // low power consumption and signal range (client phone will be inside vehicle anyway)
                    .setConnectable(true)                                           // devices do not need to connect to receive data
                    .build();
        
            // ?????
            AdvertiseData data = new AdvertiseData.Builder().build();
        
            // create a new ble advertiser and begin advertising
            BluetoothLeAdvertiser advertiser = adapter.getBluetoothLeAdvertiser();
            advertiser.startAdvertising(settings, data, advertiseCallback);
        }
    }
    
    // construct a GATT server with the vehicle service and characteristics to advertise
    public void startGATT()
    {
        // create GATT server
        GATTServer = bluetoothManager.openGattServer(this, gattServerCallback);
        GATTRunning = true;
        
        // create the vehicle simulator service
        BluetoothGattService service = new BluetoothGattService(
                UUID.fromString(SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);
    
        // create each characteristic defined by the class
        for(int i = 0; i < CHARACTERISTIC_UUID.size(); ++i)
        {
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                            UUID.fromString(CHARACTERISTIC_UUID.get(i)),
                    
                    BluetoothGattCharacteristic.PROPERTY_READ |
                            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        
                            BluetoothGattCharacteristic.PERMISSION_READ);
        
            //not sure if descriptor required
            //characteristic.addDescriptor(new BluetoothGattDescriptor(UUID.fromString(DESCRIPTOR_UUID), BluetoothGattCharacteristic.PERMISSION_WRITE);
            
            service.addCharacteristic(characteristic);
        }
    
        GATTServer.addService(service);
    }
}
