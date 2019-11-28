package com.ashb.onboarddisplay;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;

public class Display extends AppCompatActivity {
    private static final String TAG = "Display";

    TextView textview_battery;
    BluetoothGatt gatt;
    BluetoothDevice device;

    String SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";
    String BATTERY_LEVEL_CHARACTERISTIC_UUID = "00002a19-0000-1000-8000-00805f9b34fb";

    Packet packet = new Packet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Intent intent = getIntent();
        device = intent.getExtras().getParcelable("device");

        gatt = device.connectGatt(this, false, gatt_callback);

        textview_battery = (TextView) findViewById(R.id.text_battery);
    }

    public BluetoothGattCallback gatt_callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == STATE_CONNECTED) {
                boolean state = gatt.discoverServices();
                Log.d(TAG, "CONNECTED");
            }
            else {
                Log.d(TAG, "NOT CONNECTED");
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

            if (gatt.readCharacteristic(batteryLevel)) {
                gatt.setCharacteristicNotification(batteryLevel, true);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getUuid().toString().equals(BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                    packet.battery_level = characteristic.getValue()[0];
                    Log.d("BATTERY LEVEL (inital)", Integer.toString(packet.battery_level));
                    try  {
                        runGUIThread();
                    } catch (Exception e) {
                        Log.d(TAG, "Error (read)" + e);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d("onCharacteristicChanged", "Running");
            if (characteristic.getUuid().toString().equals(BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                packet.battery_level = characteristic.getValue()[0];
                Log.d("BATTERY LEVEL (changed)", Integer.toString(packet.battery_level));
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
                            textview_battery.setText(Integer.toString(packet.battery_level));
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
