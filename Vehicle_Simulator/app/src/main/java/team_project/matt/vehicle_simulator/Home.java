package team_project.matt.vehicle_simulator;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

public class Home extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback
{
    enum characteristics { BATTERY, SPEED, INDICATOR };
    
    // unique IDs that identify this app's advertisements
    // --------------------------------------------------------------------
    // service describes the set of vehicle states to broadcast
    final String SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";
    
    // each individual state to broadcast
    List<String> CHARACTERISTIC_UUIDS = new ArrayList<String>(
        Collections.unmodifiableList(Arrays.asList
        (
                "76a247fb-a76f-42da-91ce-d6a5bdebd0e2",
                "7b9b53ff-5421-4bdf-beb0-ca8c949542c1",
                "74df0c8f-f3e1-4cf5-b875-56d7ca609a2e"
        )));
    
    //final String DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    // --------------------------------------------------------------------
    
    // location permissions
    Boolean hasLocationPermission = false;
    final int REQUEST_CODE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        // does the app have location permissions?
        int hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasLocationPermission != PackageManager.PERMISSION_GRANTED)
        {
            // no, request the permission from the user
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
        }
        
        else this.hasLocationPermission = true;
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
        {
            // access bluetooth device on phone
            BluetoothManager manager = (BluetoothManager) getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = manager.getAdapter();
            
            // bluetooth is turned off
            if (!adapter.isEnabled())
            {
                Toast toast = Toast.makeText(this, "The bluetooth device is disabled. Please enable it and try again.", Toast.LENGTH_LONG);
                toast.show();
            }
            
            else
            {
                // change the name that appears for other devices
                adapter.setName("Vehicle");
    
                // adjust preferences for advertising from this device
                AdvertiseSettings settings = new AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)   // consume least amount of power at the cost of higher latency
                        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)      // low power consumption and signal range
                        .setConnectable(false)                                          // devices do not need to connect to receive data
                        .build();
    
                // must create a wrapper for UUID to be used in advertisement
                ParcelUuid parcelUuid = new ParcelUuid(UUID.fromString(SERVICE_UUID));
                
                // create the data packet to advertise
                AdvertiseData data = new AdvertiseData.Builder()
                        .addServiceUuid(parcelUuid)                                                 // UUID identifies data is coming from this app
                        .setIncludeDeviceName(true)                                                 // device name is broadcast alongside data
                        .addServiceData(parcelUuid, "Data".getBytes(Charset.forName("UTF-8")))      // data to broadcast
                        .build();
    
                // create a new BLE advertiser and begin advertising
                BluetoothLeAdvertiser advertiser = adapter.getBluetoothLeAdvertiser();
                advertiser.startAdvertising(settings, data, advertiseCallback);
                
                BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
                BluetoothGattServer server = bluetoothManager.openGattServer(this, gattServerCallback);
                
                // work on creating service and service callback
                // then add service
                //server.addService()
            }
        }
    }

    AdvertiseCallback advertiseCallback = new AdvertiseCallback()
    {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect)
        {
            super.onStartSuccess(settingsInEffect);

            Toast toast = Toast.makeText(getApplicationContext(), "Started successfully.", Toast.LENGTH_SHORT);
            toast.show();
        }

        @Override
        public void onStartFailure(int errorCode)
        {
            super.onStartFailure(errorCode);

            Toast toast = Toast.makeText(getApplicationContext(), "Failed to start, error code: " + errorCode, Toast.LENGTH_LONG);
            toast.show();
        }
    };

    BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback()
    {
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

        }
    
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
            int offset, BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }
    
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
             BluetoothGattCharacteristic characteristic, boolean preparedWrite,
             boolean responseNeeded, int offset, byte[] value)
        {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }
    };
}
