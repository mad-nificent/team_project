package team_project.matt.vehicle_simulator;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

// manages bluetooth LE communication
class BluetoothLE implements BluetoothNotificationResponse
{
    private Context context;

    private BluetoothNotification sendNotification;     // sends requests for setup and notifies result
    private BluetoothServerStatus updateStatus;         // update result of advertisement and GATT
    private Display               display;              // send updates to interface

    private BluetoothManager      bluetoothManager = null;                // high level management (get devices etc.)
    private BluetoothAdapter      bluetoothAdapter = null;                // local bluetooth device
    private BluetoothGattServer   GATTServer       = null;                // run GATT operations
    private BluetoothGattService  service          = null;                // current GATT service
    private List<BluetoothDevice> devices          = new ArrayList<>();   // remote devices connected

    private boolean isAdvertising = false;

    BluetoothLE(Activity activity, BluetoothServerStatus statusInterface)
    {
        context      = activity;
        updateStatus = statusInterface;

        // activity must handle bluetooth requests
        if (activity instanceof BluetoothNotification) this.sendNotification = (BluetoothNotification) activity;
        else Log.e(this.getClass().getName(), "activity is not instance of " + BluetoothNotification.class.getName());

        // and handle GUI updates
        if (activity instanceof Display) this.display = (Display) activity;
        else Log.e(this.getClass().getName(), "activity is not instance of " + Display.class.getName());
    }

    void enable()
    {
        sendNotification.requestLocation();
    }

    @Override
    public void requestLocationResult(boolean isGranted)
    {
        if (!isGranted) sendNotification.setupFailed("Location is required for Bluetooth Low Energy functionality.");
        else            initialiseAdapter();
    }

    private void initialiseAdapter()
    {
        // get instance of the device bluetooth hardware
        if (bluetoothManager == null) bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothAdapter == null) bluetoothAdapter = bluetoothManager.getAdapter();

        if (!bluetoothAdapter.isEnabled()) sendNotification.enableAdapter();        // request bluetooth turned on
        else                               sendNotification.setupComplete();        // already on, ready
    }

    @Override
    public void enableAdapterResult(boolean isGranted)
    {
        if (!isGranted) sendNotification.setupFailed("Bluetooth is required to communicate with dashboard.");
        else            sendNotification.setupComplete();
    }

    void startAdvertising(String UUID)
    {
        if (bluetoothAdapter.isEnabled())
        {
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
        }

        // otherwise request access again?
    }

    // start advertising, report result if error occurred
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback()
    {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect)
        {
            super.onStartSuccess(settingsInEffect);
            updateStatus.advertiseResult(true);
            isAdvertising = true;
        }

        @Override
        public void onStartFailure(int errorCode)
        {
            super.onStartFailure(errorCode);

            String errorMsg = "";
            switch (errorCode)
            {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    errorMsg = "already advertising!";
                    break;

                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    errorMsg = "packet is too large!";
                    break;

                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    errorMsg = "feature not supported.";
                    break;

                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    errorMsg = "internal error.";
                    break;

                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    errorMsg = "no advertising instance available.";
                    break;
            }

            updateStatus.advertiseResult(false);
            Log.e(this.getClass().getName(), "onStartFailure() -> Advertisement failed: " + errorMsg);
        }
    };

    void startGATT(String serviceUUID, ArrayList<String> characteristicUUIDS, String descriptorUUID, ArrayList<Integer> startingValues)
    {
        if (bluetoothManager == null || bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
        {
            Log.e(this.getClass().getName(), "startGATT() -> Could not start GATT: Bluetooth adapter not initialised.");
            updateStatus.GATTResult(false);
        }
        
        else if (GATTServer != null || service != null)
        {
            Log.e(this.getClass().getName(), "startGATT() -> Could not start GATT: Already started.");
            updateStatus.GATTResult(false);
        }

        else
        {
            GATTServer = bluetoothManager.openGattServer(context, gattServerCallback);
            service = new BluetoothGattService(UUID.fromString(serviceUUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);

            // add each characteristic to the service
            int i = 0;
            for (String characteristicUUID : characteristicUUIDS)
            {
                BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                        UUID.fromString(characteristicUUID),

                        // supported functionality
                        BluetoothGattCharacteristic.PROPERTY_READ |
                                BluetoothGattCharacteristic.PROPERTY_WRITE |
                                BluetoothGattCharacteristic.PROPERTY_NOTIFY,

                        // client access
                        BluetoothGattCharacteristic.PERMISSION_READ);

                // set starting value
                characteristic.setValue(Integer.toString(startingValues.get(i)));
                characteristic.addDescriptor(new BluetoothGattDescriptor(UUID.fromString(descriptorUUID), BluetoothGattDescriptor.PERMISSION_READ));
                service.addCharacteristic(characteristic);

                ++i;
            }

            GATTServer.addService(service);
            updateStatus.GATTResult(true);
        }
    }

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

                display.showToast("A new device has connected.", Toast.LENGTH_SHORT);
                display.updateDeviceCount(devices.size());

                // send data to new device
                for (int i = 0; i < service.getCharacteristics().size(); ++i)
                    GATTServer.notifyCharacteristicChanged(device, service.getCharacteristics().get(i), false);
            }

            // remove device
            else if (newState == STATE_DISCONNECTED)
            {
                devices.remove(device);

                display.showToast("A device has disconnected.", Toast.LENGTH_SHORT);
                display.updateDeviceCount(devices.size());
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service)
        {
            boolean serviceAdded = false;

            if (status == BluetoothGatt.GATT_SUCCESS) serviceAdded = true;
            else Log.e(this.getClass().getName(), "onServiceAdded() -> Failed to add service. Code: " + status);

            updateStatus.serviceAddedResult(serviceAdded);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            GATTServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }
    };

    void stop()
    {
        if (isAdvertising)
        {
            BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            advertiser.stopAdvertising(advertiseCallback);
            isAdvertising = false;

            if (GATTServer != null) GATTServer.close();
        }
    }

    boolean isEnabled()
    {
        return bluetoothAdapter.isEnabled();
    }

    void updateCharacteristic(String UUID, String data)
    {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(java.util.UUID.fromString(UUID));
        characteristic.setValue(data);
        notifyDevices(characteristic);
    }

    private void notifyDevices(BluetoothGattCharacteristic characteristic)
    {
        for (int i = 0; i < devices.size(); ++i)
            GATTServer.notifyCharacteristicChanged(devices.get(i), characteristic, false);
    }
}