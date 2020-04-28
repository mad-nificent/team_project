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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

class BluetoothLE implements BluetoothPermissionsResult
{
    private Context               context;           // required to setup some bluetooth services
    private BluetoothPermissions  permission;        // send requests for location permission and to enable Bluetooth adapter
    private BluetoothServerStatus GATTStatus;        // send result of advertisement and GATT
    private VehicleDashboard      vehicleDashboard;  // send messages to interface

    private BluetoothManager      bluetoothManager = null;                // high level management (get devices etc.)
    private BluetoothAdapter      bluetoothAdapter = null;                // local bluetooth device
    private BluetoothGattServer   GATTServer       = null;                // run GATT protocol in server mode
    private BluetoothGattService  service          = null;                // current GATT service
    private List<BluetoothDevice> devices          = new ArrayList<>();   // remote devices connected

    private boolean isAdvertising = false;

    // finds local adapter and tries to turn it on
    private void initialiseAdapter()
    {
        // get instance of the local bluetooth adapter
        if (context != null && bluetoothManager == null) bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothAdapter == null)                    bluetoothAdapter = bluetoothManager.getAdapter();

        // prompt user to turn Bluetooth adapter on
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled())
            permission.enableAdapter();

            // adapter already on, notify interface Bluetooth is ready
        else permission.setupComplete();
    }

    // sends characteristic updates to all connected devices
    private void notifyDevices(BluetoothGattCharacteristic characteristic)
    {
        for (BluetoothDevice device : devices) GATTServer.notifyCharacteristicChanged(device, characteristic, false);
    }

    BluetoothLE(Context context, BluetoothPermissions permissionsInterface, VehicleDashboard dashboardInterface, BluetoothServerStatus statusInterface)
    {
        // cannot start certain bluetooth services without context or relevant permissions
        // cannot update status once finished
        if (context != null && permissionsInterface != null && dashboardInterface != null && statusInterface != null)
            this.context = context; permission = permissionsInterface; vehicleDashboard = dashboardInterface; GATTStatus = statusInterface;
    }

    // use activity that implements permissions interface to request location permission
    void beginSetup() { if (permission != null) permission.requestLocation(); }

    // advertise service UUID so devices can automatically connect
    void startAdvertising(String UUID)
    {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled())
        {
            // adjust preferences for advertising from this device
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) // send out advertisement packet frequently
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)   // regular signal range
                    .setConnectable(true)                                           // devices can connect once advertisement picked up
                    .build();

            // include service UUID in advertisement so devices can immediately unpack service after connecting
            AdvertiseData data = new AdvertiseData.Builder().addServiceUuid(ParcelUuid.fromString(UUID)).build();

            // begin advertising
            BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            advertiser.startAdvertising(settings, data, advertiseCallback);
        }
    }

    // opens a GATT server and constructs a GATT service:
    //      using provided characteristics
    //      characteristics are given the CCCD (allows client to turn off updates per characteristic)
    //      initial values for each characteristic are also given
    // service is then added to server
    void startGATT(String serviceUUID, ArrayList<String> characteristicUUIDs, String CCCDescriptorUUID, ArrayList<Integer> initialValues)
    {
        // adapter needs to be setup first
        if (bluetoothManager == null || bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
        {
            Log.e(this.getClass().getName(), "startGATT() -> Could not start GATT: Bluetooth adapter not initialised.");
            GATTStatus.GATTResult(false);
        }

        // server is already running
        else if (GATTServer != null || this.service != null)
        {
            Log.e(this.getClass().getName(), "startGATT() -> Could not start GATT: Already started.");
            GATTStatus.GATTResult(false);
        }

        else
        {
            // open a new GATT server
            GATTServer = bluetoothManager.openGattServer(context, gattServerCallback);

            // construct a GATT service
            // -------------------------------------------------------------------------------------------------------------------------------------------------------
            // create primary service and set its UUID
            this.service = new BluetoothGattService(UUID.fromString(serviceUUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);

            // add each characteristic to the service
            int i = 0; for (String characteristicUUID : characteristicUUIDs)
            {
                // create characteristic and set its UUID
                BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(characteristicUUID),

                        // supported functionality
                        BluetoothGattCharacteristic.PROPERTY_READ
                                | BluetoothGattCharacteristic.PROPERTY_WRITE
                                | BluetoothGattCharacteristic.PROPERTY_NOTIFY,  // allowed to send notifications when updated

                        // client access
                        BluetoothGattCharacteristic.PERMISSION_READ);

                // set the client config descriptor
                characteristic.addDescriptor(new BluetoothGattDescriptor(UUID.fromString(CCCDescriptorUUID), BluetoothGattDescriptor.PERMISSION_READ));

                characteristic.setValue(Integer.toString(initialValues.get(i)));    // set starting value
                this.service.addCharacteristic(characteristic);                     // add characteristic to the service

                ++i;
            }
            // -------------------------------------------------------------------------------------------------------------------------------------------------------

            // add the service to the service and notify that setup is complete
            GATTServer.addService(this.service); GATTStatus.GATTResult(true);
        }
    }

    // update the characteristics value and send out a notification
    void updateCharacteristic(String UUID, String data)
    {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(java.util.UUID.fromString(UUID));
        characteristic.setValue(data);
        notifyDevices(characteristic);
    }

    void shutDown()
    {
        if (isAdvertising)
        {
            // stop advertising
            BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            advertiser.stopAdvertising(advertiseCallback);
            isAdvertising = false;

            // close the server
            if (GATTServer != null) GATTServer.close();

            // reset all references so setup can be run again when requested
            bluetoothManager = null; bluetoothAdapter = null; GATTServer = null; service = null; devices.clear();

            // notify interface that no devices are connected
            vehicleDashboard.updateDeviceCount(0);
        }
    }

    boolean isEnabled()
    {
        return bluetoothAdapter.isEnabled();
    }

    // BLUETOOTH_PERMISSION_RESULT INTERFACE
    // ---------------------------------------------------------------------------------------------------------
    // when permission result is given, setup continues here
    @Override
    public void requestLocationResult(boolean isGranted)
    {
        // show rationale to user for why location is required
        if (!isGranted) permission.setupFailed("Location is required for Bluetooth Low Energy functionality.");

            // continue and set up Bluetooth adapter
        else initialiseAdapter();
    }

    // if user was prompted to turn on adapter, result is given here
    @Override
    public void enableAdapterResult(boolean isGranted)
    {
        // show rationale to user for why Bluetooth is required
        if (!isGranted) permission.setupFailed("Bluetooth is required to communicate with dashboard.");

        // notify interface Bluetooth is ready
        else permission.setupComplete();
    }
    // ---------------------------------------------------------------------------------------------------------

    // ADVERTISE_CALLBACK INTERFACE
    // -------------------------------------------------------------------------------------------------
    // when advertising is started, result is reported here
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback()
    {
        // advertisements sending out, report they have started
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect)
        {
            super.onStartSuccess(settingsInEffect);
            GATTStatus.advertiseResult(true);   // notify advertisement started
            isAdvertising = true;
        }

        // advertisements are not sending out, report error
        @Override
        public void onStartFailure(int errorCode)
        {
            super.onStartFailure(errorCode);

            // figure out the error
            String errorMsg = ""; switch (errorCode)
            {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    errorMsg = "already advertising!"; break;

                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    errorMsg = "packet is too large!"; break;

                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    errorMsg = "feature not supported."; break;

                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    errorMsg = "internal error."; break;

                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    errorMsg = "no advertising instance available."; break;
            }

            // log the error and notify that advertisement didnt start
            Log.e(this.getClass().getName(), "onStartFailure() -> Advertisement failed: " + errorMsg);
            GATTStatus.advertiseResult(false);
        }
    };
    // -------------------------------------------------------------------------------------------------

    // GATT_SERVER_CALLBACK INTERFACE
    // -------------------------------------------------------------------------------------------------
    // when GATT server has been opened, any events will be handled here
    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback()
    {
        // a client has connected or disconnected, update device list
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState)
        {
            super.onConnectionStateChange(device, status, newState);

            // add new device
            if (newState == STATE_CONNECTED)
            {
                devices.add(device); vehicleDashboard.updateDeviceCount(devices.size());

                // send current state of characteristics to new device
                for (int i = 0; i < service.getCharacteristics().size(); ++i)
                    GATTServer.notifyCharacteristicChanged(device, service.getCharacteristics().get(i), false);
            }

            // remove device
            else if (newState == STATE_DISCONNECTED) { devices.remove(device); vehicleDashboard.updateDeviceCount(devices.size()); }
        }

        // attempt to add a service was made, report result
        @Override
        public void onServiceAdded(int status, BluetoothGattService service)
        {
            // failed to add service, log error
            if (status != BluetoothGatt.GATT_SUCCESS)
                Log.e(this.getClass().getName(), "onServiceAdded() -> Failed to add service. Code: " + status);

            // report result of success or failure
            GATTStatus.serviceAddedResult(status == BluetoothGatt.GATT_SUCCESS);
        }

        // a client wants to read a characteristic value, respond
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            // send value of characteristic back with response that access granted
            GATTServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }
    };
}