package com.project.ashb.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // handles the bluetooth
    BluetoothManager bluetooth_manager;
    BluetoothAdapter bluetooth_adapter;
    BluetoothLeScanner bluetooth_scanner;

    // view objects
    Button btn_start_scanning;
    Button btn_stop_scanning;
    ListView listview_peripheral;

    // constants
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // devices
    ArrayList<String> list_devices = new ArrayList<String>();
    ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initalises the view objects
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list_devices);
        listview_peripheral = (ListView) findViewById(R.id.listview_peripheral);
        listview_peripheral.setAdapter(adapter);

        listview_peripheral.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), Display.class);
                intent.putExtra("device", devices.get(position));
                startActivity(intent);
            }
        });

        btn_start_scanning = (Button) findViewById(R.id.btn_start_scanning);
        btn_start_scanning.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        btn_stop_scanning = (Button) findViewById(R.id.btn_stop_scanning);
        btn_stop_scanning.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        btn_stop_scanning.setVisibility(View.INVISIBLE);

        bluetooth_manager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bluetooth_adapter = bluetooth_manager.getAdapter();
        bluetooth_scanner = bluetooth_adapter.getBluetoothLeScanner();

        // checks if the bluetooth on the device is enabled
        if (bluetooth_adapter != null && !bluetooth_adapter.isEnabled()) {
            Intent enable_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable_intent, REQUEST_ENABLE_BT);
        }

        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location Access Needed");
            builder.setMessage("Grant Location Access to Continue");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

    }

    private ScanCallback le_scan_callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (!list_devices.contains(result.getDevice().getAddress())) {
                list_devices.add(result.getDevice().getAddress());
                devices.add(result.getDevice());
                adapter.notifyDataSetChanged();
            }
        }
    };

    public void onRequestPermissionsResult(int request_code, String permissions[], int[] grant_results) {
        switch (request_code) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grant_results[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Coarse Location Permission granted");
                }
                else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startScanning() {
        System.out.println("start scanning");
        list_devices.clear();
        devices.clear();
        adapter.notifyDataSetChanged();

        btn_start_scanning.setVisibility(View.INVISIBLE);
        btn_stop_scanning.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bluetooth_scanner.startScan(le_scan_callback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        btn_start_scanning.setVisibility(View.VISIBLE);
        btn_stop_scanning.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bluetooth_scanner.stopScan(le_scan_callback);
                Log.d(TAG, "Devices:" + devices);
            }
        });
    }
}
