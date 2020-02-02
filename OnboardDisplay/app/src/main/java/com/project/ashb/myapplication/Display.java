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
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Display extends AppCompatActivity {
    private static final String TAG = "Display";

    // attributes to update the GUI
    ImageView img_indicator_left;
    ImageView img_indicator_right;
    TextView textView_connected;

    ImageView iv_needle;
    ImageView iv_gauge;
    ImageView iv_gaugeBattery;
    ImageView iv_needleBattery;

    // bluetooth attributes
    BluetoothGatt gatt;
    BluetoothDevice device;

    // stores the characteristic values
    DashboardService dashboard_service = new DashboardService();

    // data structures for bluetooth
    List<BluetoothGattDescriptor> device_descriptors = new ArrayList<>();
    List<BluetoothGattCharacteristic> device_characteristics = new ArrayList<>();

    // defined animations for the indicators
    Animation animation_right = new AlphaAnimation(1, 0);
    Animation animation_left = new AlphaAnimation(1, 0);
    Animation connected_animation = new AlphaAnimation(1,0);

    int bottom_iv;
    int right_iv;
    int bottom_iv_battery;
    int right_iv_battery;

    float current_pos;
    final float starting_pos = 105;
    float current_pos_battery;
    final float starting_pos_battery = 125;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        // sets the screen orientation to lock landscape
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // gets extra variables passed from MainActivity.java
        Intent intent = getIntent();
        device = intent.getExtras().getParcelable("device");

        // starts the GATT service
        gatt = device.connectGatt(getApplicationContext(), false, gatt_callback, 2);

        // initialises all of the GUI attributes
        img_indicator_left = (ImageView) findViewById(R.id.img_indicator_left);
        img_indicator_right = (ImageView) findViewById(R.id.img_indicator_right);
        textView_connected = (TextView) findViewById(R.id.text_connected);
        iv_gauge = (ImageView) findViewById(R.id.gauge);
        iv_needle = (ImageView) findViewById(R.id.needle);
        iv_gaugeBattery = (ImageView) findViewById(R.id.gaugeBattery);
        iv_needleBattery = (ImageView) findViewById(R.id.needleBattery);

        current_pos = starting_pos;
        current_pos_battery = starting_pos_battery;

        Rect rect = new Rect();
        iv_needle.getLocalVisibleRect(rect);
        bottom_iv = rect.bottom;
        right_iv = rect.right;

        RotateAnimation rotateAnimation = new RotateAnimation(0, current_pos, bottom_iv, right_iv);
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setDuration(500);
        iv_needle.startAnimation(rotateAnimation);

        iv_needleBattery.getLocalVisibleRect(rect);
        bottom_iv_battery = rect.bottom;
        right_iv_battery = rect.right;


        // starts the animations for the indicators (initially hidden)
        createIndicatorAnimations();

        connected_animation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation arg0) {
            }
            @Override
            public void onAnimationRepeat(Animation arg0) {
            }
            @Override
            public void onAnimationEnd(Animation arg0) {
                connected_animation.cancel();
                textView_connected.setVisibility(View.GONE);
            }
        });

    }

    public BluetoothGattCallback gatt_callback = new BluetoothGattCallback() {

        // when the connection state is changed, this will be called
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            // checks if the bluetooth device has disconnected
            if (status == BluetoothGatt.GATT_FAILURE || status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnects the service
                gatt.disconnect();
            }

            // checks if the device is connected
            else if (newState == BluetoothProfile.STATE_CONNECTED) {
                // waits for any running GATT services to finish
                try { Thread.sleep(600); }
                catch (InterruptedException e) { e.printStackTrace(); }
                // attempts to discover services the device is advertising
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("BluetoothLeService", "onServicesDiscovered()");
            if (status == BluetoothGatt.GATT_SUCCESS) {

                // updates the GUI
                runGUIThread();

                // gets the services and the services characteristics and stores them as attributes
                BluetoothGattService service = gatt.getService(UUID.fromString(dashboard_service.SERVICE_UUID));
                device_characteristics.add(service.getCharacteristic(UUID.fromString(dashboard_service.characteristics_UUIDs.get(dashboard_service.BATTERY_POSITION))));
                device_characteristics.add(service.getCharacteristic(UUID.fromString(dashboard_service.characteristics_UUIDs.get(dashboard_service.SPEED_POSITION))));
                device_characteristics.add(service.getCharacteristic(UUID.fromString(dashboard_service.characteristics_UUIDs.get(dashboard_service.INDICATOR_POSITION))));

                // runs through each characteristic and sets a notification and acquires the descriptors for each
                for(int i = 0; i < device_characteristics.size(); i++){
                    gatt.setCharacteristicNotification(device_characteristics.get(i), true);
                    device_descriptors.add(device_characteristics.get(i).getDescriptor(UUID.fromString(dashboard_service.DESCRIPTOR_UUID)));
                }

                // enables notifications for each of the characteristic descriptors
                for(int i = 0; i < device_descriptors.size(); i++) device_descriptors.get(i).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                setNotifications();
            }
        }

        // method for listing the device characteristics (only used if needed)
        public void listServiceCharacteristics() {
            BluetoothGattService service = gatt.getService(UUID.fromString(dashboard_service.SERVICE_UUID));
            List<BluetoothGattCharacteristic> gattchars = service.getCharacteristics();
            Log.d("onCharDiscovered", "Char count: " + gattchars.size());

            for (BluetoothGattCharacteristic gattchar : gattchars) {
                String charUUID = gattchar.getUuid().toString();
                Log.d("onCharsDiscovered", "Char uuid " + charUUID);
            }
        }

        // writes each descriptor to the gatt service (descriptors removed recursively from onDescriptorWrite())
        public void setNotifications() {
            gatt.writeDescriptor(device_descriptors.get(device_descriptors.size()-1));
        }

        // reads each characteristic  to to update the GUI with the initial device characteristic values
        //      (characteristics removed recursively from onCharacteristicRead())
        public void readCharacteristics() {
            gatt.readCharacteristic(device_characteristics.get(device_characteristics.size()-1));
        }

        // when a characteristic is successfully initially read, this method will be called
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            // checks the characteristic passed in against each of the available characteristics then updates the relevant values
            if (characteristic.getUuid().toString().equals(dashboard_service.characteristics_UUIDs.get(dashboard_service.BATTERY_POSITION)))
                dashboard_service.battery_level = new String(characteristic.getValue(), StandardCharsets.UTF_8);
            else if (characteristic.getUuid().toString().equals(dashboard_service.characteristics_UUIDs.get(dashboard_service.SPEED_POSITION)))
                dashboard_service.speed_level = new String(characteristic.getValue(), StandardCharsets.UTF_8);
            else if (characteristic.getUuid().toString().equals(dashboard_service.characteristics_UUIDs.get(dashboard_service.INDICATOR_POSITION)))
                dashboard_service.indicator = new String(characteristic.getValue(), StandardCharsets.UTF_8);

            // updates the GUI on the UI thread
            try { runGUIThread(); } catch (Exception e) { Log.d(TAG, "Error (read)" + e); }

            // recursively removes each characteristic until all have been read
            device_characteristics.remove(device_characteristics.get(device_characteristics.size() - 1));
            if (device_characteristics.size() > 0) readCharacteristics();
        }

        // when a descriptor is successfully written, this method will be called
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            // recursively removes each descriptor and calls this method when reading
            device_descriptors.remove(device_descriptors.get(device_descriptors.size() - 1));
            if (device_descriptors.size() > 0) setNotifications();
            else readCharacteristics();
        }

        // when a characteristic value has changed on the device, this method will be called
        //      (called via a notification on each characteristic)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            // checks the characteristic passed in against each of the available characteristics then updates the relevant values
            if (characteristic.getUuid().toString().equals(dashboard_service.characteristics_UUIDs.get(dashboard_service.BATTERY_POSITION)))
                dashboard_service.battery_level = new String(characteristic.getValue(), StandardCharsets.UTF_8);
            else if (characteristic.getUuid().toString().equals(dashboard_service.characteristics_UUIDs.get(dashboard_service.SPEED_POSITION)))
                dashboard_service.speed_level = new String(characteristic.getValue(), StandardCharsets.UTF_8);
            else if (characteristic.getUuid().toString().equals(dashboard_service.characteristics_UUIDs.get(dashboard_service.INDICATOR_POSITION)))
                dashboard_service.indicator = new String(characteristic.getValue(), StandardCharsets.UTF_8);

            // updates the GUI on the UI thread
            try { runGUIThread(); } catch (Exception e) { Log.d(TAG, "Error (read)" + e); }
        }
    };

    // this initialises the animations for the indicators on the GUI
    private void createIndicatorAnimations() {
        // sets the duration
        animation_right.setDuration(1000);
        animation_left.setDuration(1000);

        animation_right.setInterpolator(new LinearInterpolator());
        animation_left.setInterpolator(new LinearInterpolator());

        // sets to repeat infinitely
        animation_right.setRepeatCount(Animation.INFINITE);
        animation_left.setRepeatCount(Animation.INFINITE);

        animation_right.setRepeatMode(Animation.REVERSE);
        animation_left.setRepeatMode(Animation.REVERSE);

        // starts the animation
        img_indicator_right.startAnimation(animation_right);
        img_indicator_left.startAnimation(animation_left);
    }

    // runs a separate thread from the GattCallback to update the GUI on the UI Thread (used for updating the values throughout)
    private void runGUIThread() {
        new Thread() {
            public void run() {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!textView_connected.getText().equals("Connected")) {
                                textView_connected.setText("Connected");
                                connected_animation.setDuration(3000);
                                textView_connected.startAnimation(connected_animation);
                            }

                            //textview_battery.setText(dashboard_service.battery_level);
                            //textview_speed.setText(dashboard_service.speed_level);

                            // checks the indicator and starts/stops the relevant animations
                            if (dashboard_service.indicator.equals("Right")) {
                                animation_left.cancel();
                                animation_right.reset();
                                img_indicator_right.startAnimation(animation_right);

                                img_indicator_right.setVisibility(View.VISIBLE);
                                img_indicator_left.setVisibility(View.GONE);
                            }
                            else if (dashboard_service.indicator.equals("Left")) {
                                animation_right.cancel();
                                animation_left.reset();
                                img_indicator_left.startAnimation(animation_left);

                                img_indicator_left.setVisibility(View.VISIBLE);
                                img_indicator_right.setVisibility(View.GONE);
                            }
                            else if (dashboard_service.indicator.equals("None")) {
                                animation_right.cancel();
                                animation_left.cancel();

                                img_indicator_left.setVisibility(View.GONE);
                                img_indicator_right.setVisibility(View.GONE);
                            }

                            RotateAnimation rotateAnimation = new RotateAnimation(current_pos, starting_pos + Integer.parseInt(dashboard_service.speed_level) * 2.0f, bottom_iv, right_iv);
                            rotateAnimation.setFillAfter(true);
                            rotateAnimation.setDuration(2000);
                            iv_needle.startAnimation(rotateAnimation);
                            rotateAnimation.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animation arg0) {
                                    current_pos = starting_pos + Integer.parseInt(dashboard_service.speed_level) * 2.0f;
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {

                                }
                            });

                            RotateAnimation rotateAnimation_battery = new RotateAnimation(current_pos_battery, starting_pos_battery + Integer.parseInt(dashboard_service.battery_level) * 2.0f, bottom_iv, right_iv);
                            rotateAnimation_battery.setFillAfter(true);
                            rotateAnimation_battery.setDuration(2000);
                            iv_needleBattery.startAnimation(rotateAnimation_battery);
                            rotateAnimation_battery.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animation arg0) {
                                    current_pos_battery = starting_pos_battery + Integer.parseInt(dashboard_service.battery_level) * 2.0f;
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {

                                }
                            });

                        }
                    });
                    Thread.sleep(300);
                } catch (Exception e) { Log.d(TAG, "Error " + e); }
            }
        }.start();
    }
}
