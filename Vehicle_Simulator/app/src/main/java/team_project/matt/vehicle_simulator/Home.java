package team_project.matt.vehicle_simulator;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;

public class Home extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback
{
    final String SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";

    final int REQUEST_LOCATION = 1;
    Boolean hasPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // get location permission
        int hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);

        // does the app have this permission
        if (hasLocationPermission != PackageManager.PERMISSION_GRANTED)
            // request the permission from the user
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);

        else hasPermission = true;
    }

    public void onStartServerClick(View view)
    {
        if (hasPermission)
        {
            BluetoothAdapter adapter = getDefaultAdapter();
            adapter.enable();
            adapter.setName("Matts Phone");

            Toast toast = Toast.makeText(this, adapter.getName(), Toast.LENGTH_SHORT);
            toast.show();

            BluetoothLeAdvertiser advertiser = adapter.getBluetoothLeAdvertiser();
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .setConnectable(true)
                    .build();

            ParcelUuid parcelUuid = new ParcelUuid(UUID.fromString(SERVICE_UUID));
            AdvertiseData data = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .addServiceUuid(parcelUuid)
                    .addServiceData(parcelUuid, "Data".getBytes(Charset.forName("UTF-8")))
                    .build();

            advertiser.startAdvertising(settings, data, advertiseCallback);

            //BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);
            //BluetoothGattServer server = bluetoothManager.openGattServer(this, gattServerCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch(requestCode)
        {
            case REQUEST_LOCATION:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    hasPermission = true;
            }
        }
    }

    AdvertiseCallback advertiseCallback = new AdvertiseCallback()
    {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect)
        {
            super.onStartSuccess(settingsInEffect);

            Toast toast = Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT);
            toast.show();
        }

        @Override
        public void onStartFailure(int errorCode)
        {
            super.onStartFailure(errorCode);

            Toast toast = Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT);
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
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service)
        {

        }
    };
}
