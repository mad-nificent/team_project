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
import android.widget.Button;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // handles the bluetooth
    BluetoothManager bluetooth_manager;
    BluetoothAdapter bluetooth_adapter;
    BluetoothLeScanner bluetooth_scanner;

    // holds vehicle values
    DashboardService dashboard_service = new DashboardService();

    // objects
    Button btn_start_scanning;

    // constants
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // devices
    ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        performChecks();
        Intent intent = new Intent(getApplicationContext(), Display.class);
        startActivity(intent);
    }

    public void performChecks() {
        bluetooth_manager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bluetooth_adapter = bluetooth_manager.getAdapter();

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


    public void onRequestPermissionsResult(int request_code, String permissions[], int[] grant_results) {
        switch (request_code) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grant_results[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Coarse Location Permission granted");
                }
                else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("App may not work correctly as location is not enabled");
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

}
