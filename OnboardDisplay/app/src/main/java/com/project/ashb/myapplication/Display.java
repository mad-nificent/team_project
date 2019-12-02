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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;

public class Display extends AppCompatActivity {
    private static final String TAG = "Display";

    TextView textview_battery;
    TextView textview_speed;
    BluetoothGatt gatt;

    BluetoothDevice device;

    String SERVICE_UUID = "dee0e505-9680-430e-a4c4-a225905ce33d"; // iPad Peripheral
    //String SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";   // BLExplorer
    String BATTERY_LEVEL_CHARACTERISTIC_UUID = "76a247fb-a76f-42da-91ce-d6a5bdebd0e2";  // iPad Peripheral
    String SPEED_LEVEL_CHARACTERISTIC_UUID = "7b9b53ff-5421-4bdf-beb0-ca8c949542c1";  // iPad Peripheral
    String DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    //String BATTERY_LEVEL_CHARACTERISTIC_UUID = "00002a19-0000-1000-8000-00805f9b34fb";  // BLExplorer

    Packet packet = new Packet();
    List<BluetoothGattDescriptor> desc = new ArrayList<>();
    List<BluetoothGattCharacteristic> chars = new ArrayList<>();
    boolean finished = false;

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
        textview_speed = (TextView) findViewById(R.id.text_speed);
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

            BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
            BluetoothGattCharacteristic batteryLevel = service.getCharacteristic(UUID.fromString(BATTERY_LEVEL_CHARACTERISTIC_UUID));
            BluetoothGattCharacteristic speedLevel = service.getCharacteristic(UUID.fromString(SPEED_LEVEL_CHARACTERISTIC_UUID));

            List<BluetoothGattCharacteristic> gattchars = service.getCharacteristics();
            Log.d("onCharDiscovered", "Char count: " + gattchars.size());

            for (BluetoothGattCharacteristic gattchar : gattchars) {
                String charUUID = gattchar.getUuid().toString();
                Log.d("onCharsDiscovered", "Char uuid " + charUUID);
            }

            for (BluetoothGattDescriptor descriptor:batteryLevel.getDescriptors()){
                Log.e(TAG, "BluetoothGattDescriptor Battery: "+descriptor.getUuid().toString());
            }

            for (BluetoothGattDescriptor descriptor:speedLevel.getDescriptors()){
                Log.e(TAG, "BluetoothGattDescriptor Speed: "+descriptor.getUuid().toString());
            }

            chars.add(batteryLevel);
            chars.add(speedLevel);

            gatt.setCharacteristicNotification(batteryLevel, true);
            gatt.setCharacteristicNotification(speedLevel, true);

            desc.add(batteryLevel.getDescriptor(UUID.fromString(DESCRIPTOR_UUID)));
            desc.add(speedLevel.getDescriptor(UUID.fromString(DESCRIPTOR_UUID)));

            desc.get(0).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            desc.get(1).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            setNotifications(0);

        }

        public void setNotifications(int pos) {
            gatt.writeDescriptor(desc.get(pos));
        }

        public void readChars() {
            gatt.readCharacteristic(chars.get(chars.size()-1));
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
            }
            else if (characteristic.getUuid().toString().equals(SPEED_LEVEL_CHARACTERISTIC_UUID)) {
                try {
                    packet.speed_level = new String(characteristic.getValue(), StandardCharsets.UTF_8);

                } catch (Exception e) {
                    Log.d(TAG, "Error (read)" + e);
                }
            }
            try {
                runGUIThread();
            } catch (Exception e) {
                Log.d(TAG, "Error (read)" + e);
            }

            chars.remove(chars.get(chars.size() - 1));

            if (chars.size() > 0) {
                readChars();
            }

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite :" + ((status == BluetoothGatt.GATT_SUCCESS) ? "Sucess" : "false"));
            if (!finished) {
                setNotifications(1);
                finished = true;
            }
            if (finished) {
                readChars();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d("onCharacteristicChanged", "Running");
            if (characteristic.getUuid().toString().equals(BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                packet.battery_level = new String(characteristic.getValue(), StandardCharsets.UTF_8);
            }
            else if (characteristic.getUuid().toString().equals(SPEED_LEVEL_CHARACTERISTIC_UUID)) {
                packet.speed_level = new String(characteristic.getValue(), StandardCharsets.UTF_8);
            }
            try {
                runGUIThread();
            } catch (Exception e) {
                Log.d(TAG, "Error (read)" + e);
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
                            textview_speed.setText(packet.speed_level);
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
