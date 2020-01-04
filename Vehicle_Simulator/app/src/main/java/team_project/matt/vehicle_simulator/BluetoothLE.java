package team_project.matt.vehicle_simulator;

import android.app.Activity;
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
import android.content.Context;
import android.os.ParcelUuid;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

// contains all relevant objects and data required for bluetooth communication over the vehicle service
public class BluetoothLE extends Activity
{
    Context context;
    
    private List<String> CHARACTERISTIC_UUID = new ArrayList<String>(Collections.unmodifiableList(Arrays.asList
    (
                                            "76a247fb-a76f-42da-91ce-d6a5bdebd0e2",
                                            "7b9b53ff-5421-4bdf-beb0-ca8c949542c1",
                                            "74df0c8f-f3e1-4cf5-b875-56d7ca609a2e")));
    private final String SERVICE_UUID    =   "dee0e505-9680-430e-a4c4-a225905ce33d";
    private final String DESCRIPTOR_UUID =   "00002902-0000-1000-8000-00805f9b34fb";
    
    public BluetoothManager         bluetoothManager;
    public BluetoothAdapter         bluetoothAdapter;
    public BluetoothGattServer      GATTServer;
    public List<BluetoothDevice>    devices = new ArrayList<BluetoothDevice>();
    public BluetoothGattService     vehicleService;
    
    public static class Characteristics
    {
        public static final int    BATTERY_INDEX  = 0,      SPEED_INDEX    = 1,      INDICATOR_INDEX = 2;
        public static final String INDICATOR_NONE = "None", INDICATOR_LEFT = "Left", INDICATOR_RIGHT = "Right";
        
        public static List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
    }
    
    // reports the result after starting the advertisement process, and runs a GATT server if successful
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback()
    {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect)
        {
            super.onStartSuccess(settingsInEffect);
            startGATT();
        }
        
        @Override
        public void onStartFailure(int errorCode)
        {
            super.onStartFailure(errorCode);
            showToast("Failed to start, error code: " + errorCode);
        }
    };
    
    // manages server operations such as incoming connections, clients reading data etc.
    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback()
    {
        // a client has connected or disconnected
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState)
        {
            super.onConnectionStateChange(device, status, newState);
            
            // add device to list of connected devices
            if (newState == STATE_CONNECTED)
            {
                devices.add(device);
                showToast(device.getName() + " connected successfully.");
            }
            
            // remove device
            else if (newState == STATE_DISCONNECTED)
            {
                devices.remove(device);
                showToast(device.getName() + " has disconnected.");
            }
        }
        
        @Override
        public void onServiceAdded(int status, BluetoothGattService service)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
                showToast("Service: " + service.getUuid().toString() + " added.");
        }
        
        // a client wants to read some data
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            
            // send the value to the device
            GATTServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }
    };
    
    public BluetoothLE(Context context)
    {
        this.context = context;
    }
    
    private void showToast(final String message)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }
    
    // begin advertising the device
    public void startAdvertising()
    {
        // access the bluetooth device
        bluetoothManager = (BluetoothManager)context.getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        
        // must be switched on
        if (bluetoothAdapter.isEnabled())
        {
            bluetoothAdapter.setName("Vehicle");
            
            // adjust preferences for advertising from this device
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)   // consume least amount of power at the cost of higher latency
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)      // low power consumption and signal range (client device will be inside vehicle anyway)
                    .setConnectable(true)                                           // devices can connect to subscribe to updates
                    .build();
            
            // include name and UUID in advertisement so devices can identify this peripheral
            AdvertiseData data = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .addServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
                    .build();
            
            // begin advertising, if successful, GATT server is started
            BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            advertiser.startAdvertising(settings, data, advertiseCallback);
        }
        
        else showToast("The bluetooth device is disabled. Please enable it and try again.");
    }
    
    // construct a GATT server with the vehicle service and characteristics to advertise
    public void startGATT()
    {
        // create GATT server
        GATTServer = bluetoothManager.openGattServer(context, gattServerCallback);
        
        // create the vehicle simulator service
        vehicleService = new BluetoothGattService(UUID.fromString(SERVICE_UUID),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        
        // add each characteristic to the service
        for(int i = 0; i < CHARACTERISTIC_UUID.size(); ++i)
        {
            // save characteristic object to list
            Characteristics.characteristics.add(new BluetoothGattCharacteristic(
                    
                    // characteristic UUID
                    UUID.fromString(CHARACTERISTIC_UUID.get(i)),
                    
                    // value supports reading, writing and notification
                    BluetoothGattCharacteristic.PROPERTY_READ |
                            BluetoothGattCharacteristic.PROPERTY_WRITE |
                            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    
                    // client can only read
                    BluetoothGattCharacteristic.PERMISSION_READ));
            
            Characteristics.characteristics.get(i).setValue("0");
            vehicleService.addCharacteristic(Characteristics.characteristics.get(i));
        }
        
        GATTServer.addService(vehicleService);
        showToast("Started successfully.");
    }
}
