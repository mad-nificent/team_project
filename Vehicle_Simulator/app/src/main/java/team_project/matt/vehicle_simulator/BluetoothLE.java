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
import android.content.Intent;
import android.os.ParcelUuid;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

// contains all relevant objects and data required for bluetooth communication over the vehicle service
public class BluetoothLE
        extends Activity
{
    Context context;
    
    public BluetoothManager      bluetoothManager = null;                // high level management (get devices etc.)
    public BluetoothAdapter      bluetoothAdapter = null;                // local bluetooth device
    public BluetoothGattServer   GATTServer       = null;                // run GATT operations
    public BluetoothGattService  service          = null;                // current GATT service
    public List<BluetoothDevice> devices          = new ArrayList<>();   // remote devices connected
    
    // reports the result after starting the advertisement process, and runs a GATT server if successful
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback()
    {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect)
        {
            super.onStartSuccess(settingsInEffect);
        }
        
        @Override
        public void onStartFailure(int errorCode)
        {
            super.onStartFailure(errorCode);
            
            String errorMsg = "";
            
            switch (errorCode)
            {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    errorMsg = "already started!";
                    break;
                    
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    errorMsg = "packet is too large!";
                    break;
                    
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    errorMsg = "not supported.";
                    break;
                    
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    errorMsg = "internal error.";
                    break;
                    
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    errorMsg = "no advertising instance available";
                    break;
            }
            
            showToast("Failed to start advertising: " + errorMsg, Toast.LENGTH_LONG);
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
            
            // add device to list
            if (newState == STATE_CONNECTED)
            {
                devices.add(device);
    
                // send data to new device
                for (int i = 0; i < service.getCharacteristics().size(); ++i)
                    GATTServer.notifyCharacteristicChanged(device, service.getCharacteristics().get(i), false);
    
                showToast("A new device has connected.", Toast.LENGTH_SHORT);
            }
            
            // remove device
            else if (newState == STATE_DISCONNECTED)
            {
                devices.remove(device);
                showToast("A device has disconnected.", Toast.LENGTH_SHORT);
            }
        }
        
        @Override
        public void onServiceAdded(int status, BluetoothGattService service)
        {
            if (status != BluetoothGatt.GATT_SUCCESS)
                showToast("Could not add service, error code: " + status,Toast.LENGTH_LONG);
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
    
    private void showToast(final String message, final int length)
    {
        if (length == Toast.LENGTH_SHORT || length == Toast.LENGTH_LONG)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast toast = Toast.makeText(context, message, length);
                    toast.show();
                }
            });
        }
    }
    
    public Boolean setDefaultBluetoothAdapter()
    {
        // get instance of the devices bluetooth hardware
        if (bluetoothManager == null) bluetoothManager = (BluetoothManager)context.getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothAdapter == null) bluetoothAdapter = bluetoothManager.getAdapter();
        
        return bluetoothAdapter.isEnabled();
    }
    
    // begin advertising the device
    public boolean startAdvertising(String UUID)
    {
        if (!bluetoothAdapter.isEnabled()) return false;
        
        // adjust preferences for advertising from this device
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) // consume least amount of power at the cost of higher latency
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)     // low power consumption and signal range (client device will be inside vehicle anyway)
                .setConnectable(true)                                           // devices can connect to subscribe to updates
                .build();
        
        // include UUID in advertisement so devices can identify this peripheral
        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid.fromString(UUID))
                .build();
        
        // begin advertising
        BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        advertiser.startAdvertising(settings, data, advertiseCallback);
        
        return true;
    }
    
    // construct a GATT server with the vehicle service and data to advertise
    public boolean startGATT(String serviceUUID, ArrayList<String> characteristicUUIDS, ArrayList<Integer> data)
    {
        if (bluetoothManager == null || bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
        {
            showToast("Cannot start GATT server, bluetooth not initialised", Toast.LENGTH_LONG);
            return false;
        }
        
        if (GATTServer != null || service != null)
        {
            showToast("GATT already running!", Toast.LENGTH_LONG);
            return false;
        }
        
        // create new GATT server
        GATTServer = bluetoothManager.openGattServer(context, gattServerCallback);

        // create new GATT service
        service = new BluetoothGattService(UUID.fromString(serviceUUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // add each characteristic to the service
        int i = 0;
        for (String cUUID : characteristicUUIDS)
        {
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                    // UUID (obtained from UUID map)
                    UUID.fromString(cUUID),
            
                    // supported functionality
                  BluetoothGattCharacteristic.PROPERTY_READ  |
                            BluetoothGattCharacteristic.PROPERTY_WRITE |
                            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            
                    // client access
                    BluetoothGattCharacteristic.PERMISSION_READ);
    
            // set starting value
            characteristic.setValue(Integer.toString(data.get(i)));
    
            // save characteristic to service
            service.addCharacteristic(characteristic);
            
            ++i;
        }

        GATTServer.addService(service);
        
        return true;
    }
}