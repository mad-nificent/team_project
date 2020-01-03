package team_project.matt.vehicle_simulator;

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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
import static android.content.Context.BLUETOOTH_SERVICE;

// contains all relevant objects and data required for bluetooth communication over the vehicle service
public class BluetoothLE
{
    Context context;
    
    public List<String> CHARACTERISTIC_UUID = new ArrayList<String>(Collections.unmodifiableList(Arrays.asList
    (
                                            "76a247fb-a76f-42da-91ce-d6a5bdebd0e2",
                                            "7b9b53ff-5421-4bdf-beb0-ca8c949542c1",
                                            "74df0c8f-f3e1-4cf5-b875-56d7ca609a2e")));
    public final String SERVICE_UUID    =   "0000180f-0000-1000-8000-00805f9b34fb";
    public final String DESCRIPTOR_UUID =   "00002902-0000-1000-8000-00805f9b34fb";
    
    public BluetoothManager     bluetoothManager;
    public BluetoothGattServer  GATTServer;
    public BluetoothGattService vehicleService;
    
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
            
            Toast toast = Toast.makeText(context, "Started successfully.", Toast.LENGTH_SHORT);
            toast.show();
            
            startGATT();
        }
        
        @Override
        public void onStartFailure(int errorCode)
        {
            super.onStartFailure(errorCode);
            
            Toast toast = Toast.makeText(context, "Failed to start, error code: " + errorCode, Toast.LENGTH_LONG);
            toast.show();
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
            
            if (newState == STATE_CONNECTED)
            {
                Toast toast = Toast.makeText(context, device.getName().toString() + " has connected", Toast.LENGTH_SHORT);
                toast.show();
            }
            
            else if (newState == STATE_DISCONNECTED)
            {
                Toast toast = Toast.makeText(context, device.getName().toString() + " has disconnected", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        
        @Override
        public void onServiceAdded(int status, BluetoothGattService service)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                Toast toast = Toast.makeText(context, "Service: " + service.getUuid().toString() + " added.", Toast.LENGTH_SHORT);
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
    
    public BluetoothLE(Context context)
    {
        this.context = context;
    }
    
    // sets the device to start advertising over ble
    public void startAdvertising()
    {
        // access the bluetooth device
        bluetoothManager         = (BluetoothManager)context.getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        
        if (adapter.isEnabled())
        {
            // adjust preferences for advertising from this device
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)   // consume least amount of power at the cost of higher latency
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)      // low power consumption and signal range (client phone will be inside vehicle anyway)
                    .setConnectable(true)                                           // devices do not need to connect to receive data
                    .build();
            
            // ?????
            AdvertiseData data = new AdvertiseData.Builder().build();
            
            // create a new ble advertiser and begin advertising, if successful, GATT server is started
            BluetoothLeAdvertiser advertiser = adapter.getBluetoothLeAdvertiser();
            advertiser.startAdvertising(settings, data, advertiseCallback);
        }
        
        // bluetooth is turned off
        else
        {
            Toast toast = Toast.makeText(context, "The bluetooth device is disabled. Please enable it and try again.", Toast.LENGTH_LONG);
            toast.show();
        }
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
            
            //not sure if descriptor required
            //characteristic.addDescriptor(new BluetoothGattDescriptor(UUID.fromString(DESCRIPTOR_UUID), BluetoothGattCharacteristic.PERMISSION_WRITE);
            
            vehicleService.addCharacteristic(Characteristics.characteristics.get(i));
        }
        
        GATTServer.addService(vehicleService);
    }
}
