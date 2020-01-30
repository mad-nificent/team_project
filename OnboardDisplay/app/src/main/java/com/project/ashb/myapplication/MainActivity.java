package com.project.ashb.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // handles the bluetooth
    BluetoothManager bluetooth_manager;
    BluetoothAdapter bluetooth_adapter;
    BluetoothLeScanner bluetooth_scanner;

    DashboardService dashboard_service = new DashboardService();

    // view objects
    Button btn_start_scanning;
    ListView listview_peripheral;

    // constants
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // devices
    ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        bluetooth_manager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bluetooth_adapter = bluetooth_manager.getAdapter();
        bluetooth_adapter.setName("Display");


        btn_start_scanning = (Button) findViewById(R.id.btn_start_scanning);
        btn_start_scanning.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

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
            if (!devices.contains(result.getDevice())) {
                btn_start_scanning.setText("Connection Found: Connecting to " + result.getDevice().getName());
                stopScanning();
                Intent intent = new Intent(getApplicationContext(), Display.class);
                intent.putExtra("device", result.getDevice());
                startActivity(intent);
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
        btn_start_scanning.setText("Attempting Connection...");
        btn_start_scanning.setEnabled(false);
        System.out.println("start scanning");
        devices.clear();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(dashboard_service.SERVICE_UUID)).build();
                List<ScanFilter> filters = new ArrayList<>();
                filters.add(filter);
                ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                bluetooth_scanner.startScan(filters, settings, le_scan_callback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bluetooth_scanner.stopScan(le_scan_callback);
                Log.d(TAG, "Devices:" + devices);
            }
        });
    }
}
