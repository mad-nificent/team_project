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
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Random;

public class Home extends AppCompatActivity implements BluetoothPermissions, VehicleDashboard, ActivityCompat.OnRequestPermissionsResultCallback
{
    final int          REQUEST_CODE_LOCATION = 1;
    final int REQUEST_CODE_BLUETOOTH_ENABLED = 2;

    BluetoothPermissionsResult respond;     // respond to permission request
    VehicleManager             vehicle;     // interact with vehicle

    ImageButton btnOptions;
    Button      btnStop;

    // UI controls
    ImageButton btnSeatbelt,    btnLightsFault,  btnTyrePressure, btnWiperFluid, btnAirbag,      btnBrakeFault, btnABS,   btnEV;    // warnings
    SeekBar     temperatureBar;
    ImageButton btnLights,      btnParkingBrake, btnCharge,       btnLeftSignal, btnRightSignal, btnAccelerate, btnBrake;           // controls

    boolean runScenario = false;

    @Override
    protected void onResume()
    {
        setContentView(R.layout.activity_home_loading);

        // communicates with BLE, including any changes to vehicle state
        VehicleService vehicleService = new VehicleService();

        // manages bluetooth hardware and GATT server, broadcasts data across BLE technology
        // create BLE instance for vehicle service, provide interfaces to communicate results of setup
        BluetoothLE bluetoothDevice = new BluetoothLE(this, this, this, vehicleService);

        // BLE handles responses sent by this activity
        respond = bluetoothDevice;

        // give service to vehicle manager, it will communicate interface changes to the service, and the service can notify BLE
        vehicle = new VehicleManager(vehicleService, this);

        // initiate setup of BLE, if successful, calls setupComplete()
        vehicle.setupBluetooth(bluetoothDevice);

        super.onResume();
    }

    @Override
    protected void onPause()
    {
        vehicle.stop();
        super.onPause();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void startSimulator()
    {
        setContentView(R.layout.activity_home_vehicle_interface);

        ImageView signal = findViewById(R.id.signalIcon);
        btnStop          = findViewById(R.id.stop);
        btnOptions       = findViewById(R.id.options);
        btnSeatbelt      = findViewById(R.id.seatbelt);
        btnLightsFault   = findViewById(R.id.lightWarning);
        btnTyrePressure  = findViewById(R.id.lowTyrePressure);
        btnWiperFluid    = findViewById(R.id.lowWiperFluid);
        btnAirbag        = findViewById(R.id.airbag);
        btnBrakeFault    = findViewById(R.id.brakeWarning);
        btnABS           = findViewById(R.id.absWarning);
        btnEV            = findViewById(R.id.evWarning);
        temperatureBar   = findViewById(R.id.temperatureBar);
        btnLights        = findViewById(R.id.lights);
        btnCharge        = findViewById(R.id.charge);
        btnParkingBrake  = findViewById(R.id.parkingBrake);
        btnLeftSignal    = findViewById(R.id.leftIndicator);
        btnRightSignal   = findViewById(R.id.rightIndicator);
        btnBrake         = findViewById(R.id.brake);
        btnAccelerate    = findViewById(R.id.throttle);

        signal.setForeground(getDrawable(R.drawable.connection_green));

        btnStop.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                runScenario = false;
                btnStop.setVisibility(View.INVISIBLE);
                btnOptions.setVisibility(View.VISIBLE);
                return true;
            }
        });

        btnOptions.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                {
                    PopupMenu popupMenu = new PopupMenu(Home.this, btnOptions);
                    popupMenu.getMenuInflater().inflate(R.menu.options, popupMenu.getMenu());

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                    {
                        @Override
                        public boolean onMenuItemClick(MenuItem item)
                        {
                            btnOptions.setVisibility(View.INVISIBLE);

                            switch(item.getItemId())
                            {
                                case R.id.slowDrive:
                                    drive(30);
                                    break;

                                case R.id.fastDrive:
                                    drive(120);
                                    break;

                                case R.id.crash:
                                    crash();
                                    break;

                                case R.id.outOfBattery:
                                    drive(120, 10);
                                    break;
                            }

                            return true;
                        }
                    });

                    popupMenu.show();
                }

                return true;
            }
        });

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

        temperatureBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
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

        btnLeftSignal.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleLeftIndicator();

                return true;
            }
        });

        btnRightSignal.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleRightIndicator();

                return true;
            }
        });

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

        vehicle.start();
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

    void drive(final int speed)
    {
        runScenario = true;

        btnStop.setVisibility(View.VISIBLE);
        btnLightsFault.setEnabled(false);
        btnLights.setEnabled(false);
        btnParkingBrake.setEnabled(false);
        btnCharge.setEnabled(false);
        btnLeftSignal.setEnabled(false);
        btnRightSignal.setEnabled(false);
        btnAccelerate.setEnabled(false);
        btnBrake.setEnabled(false);

        btnAccelerate.setForeground(getDrawable(R.drawable.throttle_active));

        if (vehicle.isParkingBrakeEngaged()) vehicle.toggleParkingBrake();

        // maintain speed
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (runScenario && vehicle.batteryLevel() > 0)
                {
                    vehicle.accelerate();

                    while (runScenario && vehicle.batteryLevel() > 0 && vehicle.speed() <= speed);

                    vehicle.decelerate();

                    while (runScenario && vehicle.batteryLevel() > 0 && vehicle.speed() > speed);
                }

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        btnAccelerate.setForeground(getDrawable(R.drawable.throttle));

                        btnLightsFault.setEnabled(true);
                        btnLights.setEnabled(true);
                        btnParkingBrake.setEnabled(true);
                        btnCharge.setEnabled(true);
                        btnLeftSignal.setEnabled(true);
                        btnRightSignal.setEnabled(true);
                        btnAccelerate.setEnabled(true);
                        btnBrake.setEnabled(true);
                    }
                });
            }

        }).start();

        // use controls
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try { Thread.sleep(5000); }
                catch (InterruptedException e) { e.printStackTrace(); }

                while (runScenario && vehicle.batteryLevel() > 0)
                {
                    Random random = new Random();
                    int result = random.nextInt(4);

                    switch (result)
                    {
                        case 0:
                            break;

                        case 1:
                            vehicle.toggleLeftIndicator();

                            result = random.nextInt(10000);
                            if (result < 2000) result = 2000;

                            try { Thread.sleep(result); }
                            catch (InterruptedException e) { e.printStackTrace(); }

                            if (runScenario && vehicle.batteryLevel() > 0) vehicle.toggleLeftIndicator();

                            break;

                        case 2:
                            vehicle.toggleRightIndicator();

                            result = random.nextInt(10000);
                            if (result < 2000) result = 2000;

                            try { Thread.sleep(result); }
                            catch (InterruptedException e) { e.printStackTrace(); }

                            if (runScenario && vehicle.batteryLevel() > 0) vehicle.toggleRightIndicator();

                            break;

                        case 3:
                            vehicle.toggleLights();
                            if (vehicle.lightsHigh()) vehicle.toggleLights();
                    }

                    result = random.nextInt(20000);
                    if (result < 5000) result = 5000;

                    try { Thread.sleep(result); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                }
            }

        }).start();
    }

    void crash()
    {
        runScenario = true;

        btnStop.setVisibility(View.VISIBLE);
        btnLightsFault.setEnabled(false);
        btnLights.setEnabled(false);
        btnParkingBrake.setEnabled(false);
        btnCharge.setEnabled(false);
        btnLeftSignal.setEnabled(false);
        btnRightSignal.setEnabled(false);
        btnAccelerate.setEnabled(false);
        btnBrake.setEnabled(false);

        btnAccelerate.setForeground(getDrawable(R.drawable.throttle_active));

        if (vehicle.isParkingBrakeEngaged()) vehicle.toggleParkingBrake();

        // maintain speed
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                vehicle.accelerate();

                try { Thread.sleep(15000); }
                catch (InterruptedException e) { e.printStackTrace(); }

                if (runScenario)
                {
                    vehicle.brake();

                    vehicle.toggleAirbag();
                    vehicle.toggleBrakeFault();
                    vehicle.toggleBrakeFault();
                    vehicle.toggleABS();
                    vehicle.toggleABS();
                    vehicle.toggleEV();
                    vehicle.toggleEV();
                }

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        btnAccelerate.setForeground(getDrawable(R.drawable.throttle));

                        btnLightsFault.setEnabled(true);
                        btnLights.setEnabled(true);
                        btnParkingBrake.setEnabled(true);
                        btnCharge.setEnabled(true);
                        btnLeftSignal.setEnabled(true);
                        btnRightSignal.setEnabled(true);
                        btnAccelerate.setEnabled(true);
                        btnBrake.setEnabled(true);
                    }
                });
            }

        }).start();
    }

    void drive(final int speed, int batteryLevel)
    {
        vehicle.setBatteryLevel(batteryLevel);
        drive(speed);
    }

    // DASHBOARD INTERFACE
    // -----------------------------------------------------------------------------------------------------------------------------
    @Override
    public void showToast(final String message, final int length)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run() { Toast.makeText(Home.this, message, length).show(); }
        });
    }

    @Override
    public void vehicleReady() { startSimulator(); }

    @Override
    public void updateDeviceCount(final int noDevices)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TextView deviceCount = findViewById(R.id.deviceCount);
                deviceCount.setText(String.format(Locale.getDefault(), "%d", noDevices));
            }
        });
    }

    @Override
    public void updateBatteryLevel(final int newBatteryLevel)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TextView battery = findViewById(R.id.batteryLevel);
                battery.setText(String.format(Locale.getDefault(), "%d", newBatteryLevel));
            }
        });
    }

    @Override
    public void updateSpeed(final int newSpeed)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TextView speed = findViewById(R.id.speed);
                speed.setText(String.format(Locale.getDefault(), "%d", newSpeed));

                if (newSpeed > 0)
                {
                    // cannot charge or engage parking brake during driving
                    btnCharge.setForeground(getDrawable(R.drawable.charging_icon_grey));
                    btnCharge.setEnabled(false);

                    btnParkingBrake.setForeground(getDrawable(R.drawable.parking_brake_grey));
                    btnParkingBrake.setEnabled(false);
                }

                else
                {
                    // safe to charge and engage parking brake
                    btnCharge.setForeground(getDrawable(R.drawable.charging_icon_black));
                    btnCharge.setEnabled(true);

                    btnParkingBrake.setForeground(getDrawable(R.drawable.parking_brake_black));
                    btnParkingBrake.setEnabled(true);
                }
            }
        });
    }

    @Override
    public void updateRange(final int newRange)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TextView range = findViewById(R.id.range);
                range.setText(String.format(Locale.getDefault(), "%d", newRange));
            }
        });
    }

    @Override
    public void updateDistance(final int newDistance)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TextView distance = findViewById(R.id.distance);
                distance.setText(String.format(Locale.getDefault(), "%d", newDistance));
            }
        });
    }

    @Override
    public void toggleSeatbelt(final int newState)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton seatbelt = findViewById(R.id.seatbelt);

                if (newState == vehicle.STATE_OFF) seatbelt.setForeground(getDrawable(R.drawable.seatbelt_warning_black));
                else                               seatbelt.setForeground(getDrawable(R.drawable.seatbelt_warning_red));
            }
        });
    }

    @Override
    public void toggleLights(final int newState)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (newState == vehicle.STATE_OFF)
                {
                    // reset warning and control icon
                    btnLights.setForeground(getDrawable(R.drawable.no_lightbeam_black));
                    btnLightsFault.setForeground(getDrawable(R.drawable.no_lightbeam_black));
                }

                else if (newState == vehicle.STATE_LIGHTS_ERR)
                {
                    // reset control icon, highlight warning icon
                    btnLights.setForeground(getDrawable(R.drawable.no_lightbeam_black));
                    btnLightsFault.setForeground(getDrawable(R.drawable.lights_fault_red));
                }

                else
                {
                    // highlight control icon, reset warning icon
                    btnLightsFault.setForeground(getDrawable(R.drawable.no_lightbeam_black));

                    if (newState == vehicle.STATE_LIGHTS_LOW) btnLights.setForeground(getDrawable(R.drawable.med_lightbeam_green));
                    else                                      btnLights.setForeground(getDrawable(R.drawable.high_lightbeam_blue));
                }
            }
        });
    }

    @Override
    public void toggleTyrePressure(final int newState)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton tyrePressure = findViewById(R.id.lowTyrePressure);

                if (newState == vehicle.STATE_OFF) tyrePressure.setForeground(getDrawable(R.drawable.low_tire_pressure_black));
                else                               tyrePressure.setForeground(getDrawable(R.drawable.low_tire_pressure_red));
            }
        });
    }

    @Override
    public void toggleWiperFluid(final int newState)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton wiperFluid = findViewById(R.id.lowWiperFluid);

                if (newState == vehicle.STATE_OFF) wiperFluid.setForeground(getDrawable(R.drawable.low_wiper_fluid_black));
                else                               wiperFluid.setForeground(getDrawable(R.drawable.low_wiper_fluid_red));
            }
        });
    }

    @Override
    public void toggleAirbag(final int newState)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton airbag = findViewById(R.id.airbag);

                if (newState == vehicle.STATE_OFF) airbag.setForeground(getDrawable(R.drawable.airbag_fault_black));
                else                               airbag.setForeground(getDrawable(R.drawable.airbag_fault_red));
            }
        });
    }

    @Override
    public void toggleBrakeFault(final int newState)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton brakeWarning = findViewById(R.id.brakeWarning);

                if      (newState == vehicle.STATE_OFF)         brakeWarning.setForeground(getDrawable(R.drawable.brake_warning_black));
                else if (newState == vehicle.STATE_WARNING_LOW) brakeWarning.setForeground(getDrawable(R.drawable.brake_warning_orange));
                else                                            brakeWarning.setForeground(getDrawable(R.drawable.brake_warning_red));
            }
        });
    }

    @Override
    public void toggleABSFault(final int newState)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton ABSWarning = findViewById(R.id.absWarning);

                if      (newState == vehicle.STATE_OFF)         ABSWarning.setForeground(getDrawable(R.drawable.abs_fault_black));
                else if (newState == vehicle.STATE_WARNING_LOW) ABSWarning.setForeground(getDrawable(R.drawable.abs_fault_orange));
                else                                            ABSWarning.setForeground(getDrawable(R.drawable.abs_fault_red));
            }
        });
    }

    @Override
    public void toggleEVFault(final int newState)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ImageButton EVWarning = findViewById(R.id.evWarning);

                if      (newState == vehicle.STATE_OFF)         EVWarning.setForeground(getDrawable(R.drawable.electric_drive_system_fault_black));
                else if (newState == vehicle.STATE_WARNING_LOW) EVWarning.setForeground(getDrawable(R.drawable.electric_drive_system_fault_orange));
                else                                            EVWarning.setForeground(getDrawable(R.drawable.electric_drive_system_fault_red));
            }
        });
    }

    @Override
    public void updateBatteryTemperature(final int newTemperature)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TextView temperature = findViewById(R.id.temperature);
                temperature.setText(String.format(Locale.getDefault(), "%d", newTemperature));

                if (temperatureBar.getProgress() != newTemperature) temperatureBar.setProgress(newTemperature);
            }
        });
    }

    @Override
    public void toggleChargeMode(final boolean isCharging)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                toggleSpeedControls(!isCharging);

                if (isCharging) btnCharge.setForeground(getDrawable(R.drawable.charging_icon_green));
                else            btnCharge.setForeground(getDrawable(R.drawable.charging_icon_black));
            }
        });
    }

    @Override
    public void toggleParkingBrake(final int newState)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (newState == vehicle.STATE_OFF)
                {
                    toggleSpeedControls(true);
                    btnParkingBrake.setForeground(getDrawable(R.drawable.parking_brake_black));
                }

                else
                {
                    toggleSpeedControls(false);
                    btnParkingBrake.setForeground(getDrawable(R.drawable.parking_brake_red));
                }
            }
        });
    }

    @Override
    public void toggleTurnSignal(final int newState)
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

                if (newState == vehicle.STATE_OFF)
                {
                    // reset both turn signals
                    btnLeftSignal.setForeground(getDrawable(R.drawable.left_indicator_black));
                    btnRightSignal.setForeground(getDrawable(R.drawable.right_indicator_black));

                    btnRightSignal.setAnimation(null);
                    btnLeftSignal.setAnimation(null);
                }

                else if (newState == vehicle.STATE_SIGNAL_RIGHT)
                {
                    // highlight right signal
                    btnLeftSignal.setForeground(getDrawable(R.drawable.left_indicator_black));
                    btnRightSignal.setForeground(getDrawable(R.drawable.right_indicator_green));

                    btnRightSignal.setAnimation(blink);
                    btnLeftSignal.setAnimation(null);
                }

                else
                {
                    // highlight left signal
                    btnLeftSignal.setForeground(getDrawable(R.drawable.left_indicator_green));
                    btnRightSignal.setForeground(getDrawable(R.drawable.right_indicator_black));

                    btnRightSignal.setAnimation(null);
                    btnLeftSignal.setAnimation(blink);
                }
            }
        });
    }
    // -----------------------------------------------------------------------------------------------------------------------------

    // PERMISSIONS INTERFACE
    // ----------------------------------------------------------------------------------------------------------------
    @Override
    public void requestLocation()
    {
        // location permission not granted yet, request
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);

        // already granted
        else respond.requestLocationResult(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results)
    {
        if (requestCode == REQUEST_CODE_LOCATION)
            respond.requestLocationResult(results[0] == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void enableAdapter() { startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_CODE_BLUETOOTH_ENABLED); }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_CODE_BLUETOOTH_ENABLED)
            respond.enableAdapterResult(resultCode == RESULT_OK);
    }

    @Override
    public void setupFailed(String error)
    {
        showToast(error, Toast.LENGTH_LONG);
        finish();
    }

    @Override
    public void setupComplete() { vehicle.setupVehicle(this); }
    // ----------------------------------------------------------------------------------------------------------------
}