package team_project.matt.vehicle_simulator;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class Home extends AppCompatActivity implements Display, BluetoothNotification, ActivityCompat.OnRequestPermissionsResultCallback
{
    final int          REQUEST_CODE_LOCATION = 1;
    final int REQUEST_CODE_BLUETOOTH_ENABLED = 2;

    // responds to BLE interface
    BluetoothNotificationResponse respond;

    // manages interface between user and the service, such as managing speed, battery etc.
    VehicleManager vehicle;

    // warnings
    ImageButton btnSeatbelt, btnLightsFault, btnTyrePressure, btnWiperFluid, btnAirbag, btnBrakeFault, btnABS, btnEV;
    ImageView temperatureIcon;
    SeekBar   temperature;

    // controls
    ImageButton btnLights, btnParkingBrake, btnCharge, btnLeftIndicator, btnRightIndicator, btnAccelerate, btnBrake;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_loading);

        // holds all info about characteristics and the service itself, and communicates changes to BLE
        VehicleService vehicleService = new VehicleService();

        // create ble instance, this activity will take requests, vehicle interface will receieve status updates
        BluetoothLE bluetoothDevice = new BluetoothLE(this, vehicleService);

        // ble will receive user responses to requests
        respond = bluetoothDevice;

        // create interface to vehicle
        vehicle = new VehicleManager(this, vehicleService, this);

        // initiate setup of ble (requests permissions, enable bluetooth device etc.)
        // this runs asynchronously, and results are received through interface
        vehicleService.beginSetup(bluetoothDevice, this);
    }

    // once ble is started, it will call this method to request location permission from the user
    @Override
    public void requestLocation()
    {
        int locationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        // not granted yet
        if (locationPermission == PackageManager.PERMISSION_DENIED)
        {
            // ask for user choice and respond to request
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
        }

        // grant access to ble
        else respond.requestLocationResult(true);
    }

    // if permissions are needed, ask for them and respond to ble with user choice
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_CODE_LOCATION)
        {
            boolean isPermissionGranted = false;
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) isPermissionGranted = true;
            respond.requestLocationResult(isPermissionGranted);
        }
    }

    // once permissions are granted ble will request bluetooth hardware is switched on if its currently off
    @Override
    public void enableAdapter()
    {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, REQUEST_CODE_BLUETOOTH_ENABLED);
    }

    // once user has provided their choice, this method is called and returns result to ble
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_CODE_BLUETOOTH_ENABLED)
        {
            boolean isEnabled = false;
            if (resultCode == RESULT_OK) isEnabled = true;
            respond.enableAdapterResult(isEnabled);
        }
    }

    // ble will call this method if user denies permission or bluetooth access, or if any internal error occurs
    @Override
    public void setupFailed(String message)
    {
        showToast(message, Toast.LENGTH_LONG);
        finish();
    }

    // setup went smoothly, start the vehicle
    @Override
    public void setupComplete()
    {
        vehicle.initialise();
    }

    @Override
    public void showToast(final String message, final int length)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(Home.this, message, length).show();
            }
        });
    }

    @Override
    public void updateDeviceCount(final int noDevices)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageView signal = findViewById(R.id.signalIcon);

                if (noDevices > 0) signal.setForeground(getDrawable(R.drawable.connection_green));
                else               signal.setForeground(getDrawable(R.drawable.connection_grey));

                TextView txtDeviceCount = findViewById(R.id.deviceCount);
                txtDeviceCount.setText(String.format(Locale.getDefault(), "%d", noDevices));
            }
        });
    }

    // this method is called by the vehicle service once it has launched GATT
    @Override
    public void vehicleStarted()
    {
        loadVehicleInterface();
        vehicle.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void loadVehicleInterface()
    {
        setContentView(R.layout.activity_home_vehicle_interface);

        btnSeatbelt = findViewById(R.id.seatbelt);
        btnSeatbelt.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleSeatbelt();

                return true;
            }
        });

        btnLightsFault = findViewById(R.id.lightWarning);
        btnLightsFault.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleLightsFault();

                return true;
            }
        });

        btnTyrePressure = findViewById(R.id.lowTyrePressure);
        btnTyrePressure.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleTyrePressure();

                return true;
            }
        });

        btnWiperFluid = findViewById(R.id.lowWiperFluid);
        btnWiperFluid.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleWiperFluid();

                return true;
            }
        });

        btnAirbag = findViewById(R.id.airbag);
        btnAirbag.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleAirbag();

                return true;
            }
        });

        btnBrakeFault = findViewById(R.id.brakeWarning);
        btnBrakeFault.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleBrakeFault();

                return true;
            }
        });

        btnABS = findViewById(R.id.absWarning);
        btnABS.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleABS();

                return true;
            }
        });

        btnEV = findViewById(R.id.evWarning);
        btnEV.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleEV();

                return true;
            }
        });

        temperatureIcon = findViewById(R.id.temperatureIcon);

        temperature = findViewById(R.id.temperatureBar);
        temperature.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                vehicle.setTemperature(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnLights = findViewById(R.id.lights);
        btnLights.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleLights();

                return true;
            }
        });

        btnCharge = findViewById(R.id.charge);
        btnCharge.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleCharging();

                return true;
            }
        });

        btnParkingBrake = findViewById(R.id.parkingBrake);
        btnParkingBrake.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleParkingBrake();

                return true;
            }
        });

        btnLeftIndicator = findViewById(R.id.leftIndicator);
        btnLeftIndicator.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleLeftIndicator();

                return true;
            }
        });

        btnRightIndicator = findViewById(R.id.rightIndicator);
        btnRightIndicator.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleRightIndicator();

                return true;
            }
        });

        btnBrake = findViewById(R.id.brake);
        btnBrake.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                {
                    btnBrake.setForeground(getDrawable(R.drawable.brake_active));
                    vehicle.brake();
                }

                else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_POINTER_UP)
                {
                    btnBrake.setForeground(getDrawable(R.drawable.brake));
                    vehicle.decelerate();
                }

                return true;
            }
        });

        btnAccelerate = findViewById(R.id.throttle);
        btnAccelerate.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                {
                    btnAccelerate.setForeground(getDrawable(R.drawable.throttle_active));
                    vehicle.accelerate();
                }

                else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_POINTER_UP)
                {
                    btnAccelerate.setForeground(getDrawable(R.drawable.throttle));
                    vehicle.decelerate();
                }

                return true;
            }
        });
    }

    @Override
    public void updateBatteryLevel(final int charge)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                String text = String.format(Locale.getDefault(), "%d", charge) + "%";

                TextView txtBattery = findViewById(R.id.batteryLevel);
                txtBattery.setText(text);
            }
        });
    }

    @Override
    public void updateSpeed(final int speed)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TextView txtSpeed = findViewById(R.id.speed);
                txtSpeed.setText(String.format(Locale.getDefault(), "%d", speed));

                if (speed > 0)
                {
                    btnCharge.setBackground(getDrawable(R.drawable.charging_icon_grey));
                    btnCharge.setEnabled(false);

                    btnParkingBrake.setBackground(getDrawable(R.drawable.parking_brake_grey));
                    btnParkingBrake.setEnabled(false);

                }

                else
                {
                    btnCharge.setBackground(getDrawable(R.drawable.charging_icon_black));
                    btnCharge.setEnabled(true);

                    btnParkingBrake.setBackground(getDrawable(R.drawable.parking_brake_black));
                    btnParkingBrake.setEnabled(true);
                }
            }
        });
    }

    @Override
    public void updateRange(final int milesLeft)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TextView txtRange = findViewById(R.id.range);
                txtRange.setText(String.format(Locale.getDefault(), "%d", milesLeft));
            }
        });
    }

    @Override
    public void updateDistance(final int distance)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TextView txtDistance = findViewById(R.id.distance);
                txtDistance.setText(String.format(Locale.getDefault(), "%d", distance));
            }
        });
    }

    @Override
    public void toggleWarning(final int state)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageView warning = findViewById(R.id.masterWarning);

                if (state == vehicle.STATE_OFF) warning.setBackground(getDrawable(R.drawable.master_warning_grey));
                else                            warning.setBackground(getDrawable(R.drawable.master_warning_red));
            }
        });
    }

    @Override
    public void toggleSeatbelt(final int state)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton seatbelt = findViewById(R.id.seatbelt);

                if (state == vehicle.STATE_OFF) seatbelt.setBackground(getDrawable(R.drawable.seatbelt_warning_black));
                else                            seatbelt.setBackground(getDrawable(R.drawable.seatbelt_warning_red));
            }
        });
    }

    @Override
    public void toggleLights(final int state)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (state == vehicle.STATE_OFF)
                {
                    btnLights.setBackground(getDrawable(R.drawable.no_lightbeam_black));
                    btnLightsFault.setBackground(getDrawable(R.drawable.no_lightbeam_black));
                }

                else if (state == vehicle.STATE_LIGHTS_ERR)
                {
                    btnLights.setBackground(getDrawable(R.drawable.no_lightbeam_black));
                    btnLightsFault.setBackground(getDrawable(R.drawable.lights_fault_red));
                }

                else
                {
                    btnLightsFault.setBackground(getDrawable(R.drawable.no_lightbeam_black));

                    if (state == vehicle.STATE_LIGHTS_LOW) btnLights.setBackground(getDrawable(R.drawable.med_lightbeam_green));
                    else                                   btnLights.setBackground(getDrawable(R.drawable.high_lightbeam_blue));
                }
            }
        });
    }

    @Override
    public void toggleTyrePressure(final int state)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton tyrePressure = findViewById(R.id.lowTyrePressure);

                if (state == vehicle.STATE_OFF) tyrePressure.setBackground(getDrawable(R.drawable.low_tire_pressure_black));
                else                            tyrePressure.setBackground(getDrawable(R.drawable.low_tire_pressure_red));
            }
        });
    }

    @Override
    public void toggleWiperFluid(final int state)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton wiperFluid = findViewById(R.id.lowWiperFluid);

                if (state == vehicle.STATE_OFF) wiperFluid.setBackground(getDrawable(R.drawable.low_wiper_fluid_black));
                else                            wiperFluid.setBackground(getDrawable(R.drawable.low_wiper_fluid_red));
            }
        });
    }

    @Override
    public void toggleAirbag(final int state)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton airbag = findViewById(R.id.airbag);

                if (state == vehicle.STATE_OFF) airbag.setBackground(getDrawable(R.drawable.airbag_fault_black));
                else                            airbag.setBackground(getDrawable(R.drawable.airbag_fault_red));
            }
        });
    }

    @Override
    public void toggleBrakeFault(final int state)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton brakeFault = findViewById(R.id.brakeWarning);

                if      (state == vehicle.STATE_OFF)         brakeFault.setBackground(getDrawable(R.drawable.brake_warning_black));
                else if (state == vehicle.STATE_WARNING_LOW) brakeFault.setBackground(getDrawable(R.drawable.brake_warning_orange));
                else                                         brakeFault.setBackground(getDrawable(R.drawable.brake_warning_red));
            }
        });
    }

    @Override
    public void toggleABSFault(final int state)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton ABSFault = findViewById(R.id.absWarning);

                if      (state == vehicle.STATE_OFF)         ABSFault.setBackground(getDrawable(R.drawable.abs_fault_black));
                else if (state == vehicle.STATE_WARNING_LOW) ABSFault.setBackground(getDrawable(R.drawable.abs_fault_orange));
                else                                         ABSFault.setBackground(getDrawable(R.drawable.abs_fault_red));
            }
        });
    }

    @Override
    public void toggleEVFault(final int state)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton EVFault = findViewById(R.id.evWarning);

                if      (state == vehicle.STATE_OFF)         EVFault.setBackground(getDrawable(R.drawable.electric_drive_system_fault_black));
                else if (state == vehicle.STATE_WARNING_LOW) EVFault.setBackground(getDrawable(R.drawable.electric_drive_system_fault_orange));
                else                                         EVFault.setBackground(getDrawable(R.drawable.electric_drive_system_fault_red));
            }
        });
    }

    @Override
    public void updateBatteryTemperature(final int temperature)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if      (temperature > 20) temperatureIcon.setBackground(getDrawable(R.drawable.temp_red));
                else if (temperature < 10) temperatureIcon.setBackground(getDrawable(R.drawable.temp_blue));
                else                       temperatureIcon.setBackground(getDrawable(R.drawable.temp_grey));

                TextView txtTemperature = findViewById(R.id.temperature);
                txtTemperature.setText(String.format(Locale.getDefault(), "%d", temperature));
            }
        });
    }

    void toggleSpeedControls(boolean enabled)
    {
        if (enabled)
        {
            btnAccelerate.setForeground(getDrawable(R.drawable.throttle));
            btnBrake.setForeground(getDrawable(R.drawable.brake));
        }

        else
        {
            btnAccelerate.setForeground(getDrawable(R.drawable.throttle_disabled));
            btnBrake.setForeground(getDrawable(R.drawable.brake_disabled));
        }

        btnAccelerate.setEnabled(enabled);
        btnBrake.setEnabled(enabled);
    }

    @Override
    public void updateChargeMode(final boolean isCharging)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                toggleSpeedControls(!isCharging);

                if (isCharging) btnCharge.setBackground(getDrawable(R.drawable.charging_icon_green));
                else            btnCharge.setBackground(getDrawable(R.drawable.charging_icon_black));
            }
        });
    }

    @Override
    public void toggleParkingBrake(final int state)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton parkingBrake = findViewById(R.id.parkingBrake);

                if (state == vehicle.STATE_OFF)
                {
                    toggleSpeedControls(true);
                    parkingBrake.setBackground(getDrawable(R.drawable.parking_brake_black));
                }

                else
                {
                    toggleSpeedControls(false);
                    parkingBrake.setBackground(getDrawable(R.drawable.parking_brake_red));
                }
            }
        });
    }

    @Override
    public void toggleIndicator(final int state)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Animation blink = new AlphaAnimation(1.0f, 0.0f);
                blink.setDuration(500);
                blink.setRepeatCount(Animation.INFINITE);
                blink.setRepeatMode(Animation.REVERSE);

                if (state == vehicle.STATE_OFF)
                {
                    btnLeftIndicator.setBackground(getDrawable(R.drawable.left_indicator_black));
                    btnRightIndicator.setBackground(getDrawable(R.drawable.right_indicator_black));

                    btnRightIndicator.setAnimation(null);
                    btnLeftIndicator.setAnimation(null);
                }

                else if (state == vehicle.STATE_SIGNAL_RIGHT)
                {
                    btnLeftIndicator.setBackground(getDrawable(R.drawable.left_indicator_black));
                    btnRightIndicator.setBackground(getDrawable(R.drawable.right_indicator_green));

                    btnRightIndicator.setAnimation(blink);
                    btnLeftIndicator.setAnimation(null);
                }

                else
                {
                    btnLeftIndicator.setBackground(getDrawable(R.drawable.left_indicator_green));
                    btnRightIndicator.setBackground(getDrawable(R.drawable.right_indicator_black));

                    btnRightIndicator.setAnimation(null);
                    btnLeftIndicator.setAnimation(blink);
                }
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        vehicle.stop();
    }
}
