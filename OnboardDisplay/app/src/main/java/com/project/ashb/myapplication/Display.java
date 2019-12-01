package com.project.ashb.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class Display extends AppCompatActivity {
    private static final String TAG = "Display";

    TextView textview_battery;
    BluetoothGatt gatt;

    BluetoothDevice device;

    String SERVICE_UUID = "dee0e505-9680-430e-a4c4-a225905ce33d"; // iPad Peripheral
    //String SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";   // BLExplorer
    String BATTERY_LEVEL_CHARACTERISTIC_UUID = "76a247fb-a76f-42da-91ce-d6a5bdebd0e2";  // iPad Peripheral
    //String BATTERY_LEVEL_CHARACTERISTIC_UUID = "00002a19-0000-1000-8000-00805f9b34fb";  // BLExplorer

    Packet packet = new Packet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Intent intent = getIntent();
        device = intent.getExtras().getParcelable("device");

        //gatt = device.connectGatt(getApplicationContext(), false, gatt_callback);
        gatt = device.connectGatt(getApplicationContext(), false, gatt_callback, 2);

        textview_battery = (TextView) findViewById(R.id.text_battery);
    }

    public BluetoothGattCallback gatt_callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            BluetoothDevice bluetoothDevice = gatt.getDevice();

            // if these conditions == true, then we have a disconnect
            if (status == BluetoothGatt.GATT_FAILURE ||
                    status != BluetoothGatt.GATT_SUCCESS ||
                    newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, String.format(Locale.getDefault(),
                        "Disconnected from %s (%s) - status %d - state %d",
                        bluetoothDevice.getName(),
                        bluetoothDevice.getAddress(),
                        status,
                        newState
                ));

                gatt.disconnect();
                // if these conditions == true, then we have a successful connection
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, String.format(Locale.getDefault(),
                        "Connected to %s (%s) - status %d - state %d",
                        bluetoothDevice.getName(),
                        bluetoothDevice.getAddress(),
                        status,
                        newState
                ));
                // this sleep is here to avoid TONS of problems in BLE, that occur whenever we start
                // service discovery immediately after the connection is established
                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("BluetoothLeService", "onServicesDiscovered()");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> gattServices = gatt.getServices();
                Log.d("onServicesDiscovered", "Services count: " + gattServices.size());

                for (BluetoothGattService gattService : gattServices) {
                    String serviceUUID = gattService.getUuid().toString();
                    Log.d("onServicesDiscovered", "Service uuid " + serviceUUID);
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }

            BluetoothGattService battery_service = gatt.getService(UUID.fromString(SERVICE_UUID));
            BluetoothGattCharacteristic batteryLevel = battery_service.getCharacteristic(UUID.fromString(BATTERY_LEVEL_CHARACTERISTIC_UUID));

            List<BluetoothGattCharacteristic> gattchars = battery_service.getCharacteristics();
            Log.d("onCharDiscovered", "Char count: " + gattchars.size());

            for (BluetoothGattCharacteristic gattchar : gattchars) {
                String charUUID = gattchar.getUuid().toString();
                Log.d("onCharsDiscovered", "Char uuid " + charUUID);
            }
            gatt.setCharacteristicNotification(batteryLevel, true);
            BluetoothGattDescriptor desc = batteryLevel.getDescriptor(UUID.fromString(BATTERY_LEVEL_CHARACTERISTIC_UUID));
            batteryLevel.setValue(desc.ENABLE_INDICATION_VALUE);
            gatt.readCharacteristic(batteryLevel);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (characteristic.getUuid().toString().equals(BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                try {
                    packet.battery_level = new String(characteristic.getValue(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    Log.d(TAG, "Error (read)" + e);
                }
                try  {
                    runGUIThread();
                } catch (Exception e) {
                    Log.d(TAG, "Error (read)" + e);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d("onCharacteristicChanged", "Running");
            if (characteristic.getUuid().toString().equals(BATTERY_LEVEL_CHARACTERISTIC_UUID)) {

                packet.battery_level = new String(characteristic.getValue(), StandardCharsets.UTF_8);
                try  {
                    runGUIThread();
                } catch (Exception e) {
                    Log.d(TAG, "Error (changed)" + e);
                }
            }
        }
    };

    private void runGUIThread() {
        new Thread() {
            public void run() {
                try {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            textview_battery.setText(packet.battery_level);
                        }
                    });
                    Thread.sleep(300);
                } catch (Exception e) {
                    Log.d(TAG, "Error " + e);
                }
            }
        }.start();
    }

}
