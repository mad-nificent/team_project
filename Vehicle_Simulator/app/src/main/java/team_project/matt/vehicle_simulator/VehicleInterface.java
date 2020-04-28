package team_project.matt.vehicle_simulator;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
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

public class VehicleInterface extends AppCompatActivity implements BluetoothPermissions, VehicleDashboard, ActivityCompat.OnRequestPermissionsResultCallback
{
    private BluetoothPermissionsResult respond;     // send results of permission request
    private VehicleManager             vehicle;     // send user commands to the vehicle, updates its state and communicates changes to BLE

    // need to track these states at all times to limit speed controls
    private boolean isCharging;
    private boolean parkingBrakeOn;

    // indicates if a background thread is running a scenario
    private boolean runScenario = false;

    // UI controls
    // ---------------------------------
    private ImageButton btnOptions;
    private Button      btnStop;

    // warnings
    private ImageButton btnSeatbelt;
    private ImageButton btnLightsFault;
    private ImageButton btnTyrePressure;
    private ImageButton btnWiperFluid;
    private ImageButton btnAirbag;
    private ImageButton btnBrakeFault;
    private ImageButton btnABS;
    private ImageButton btnEV;

    // temperature slider
    private SeekBar     temperatureBar;

    // vehicle controls
    private ImageButton btnLights;
    private ImageButton btnParkingBrake;
    private ImageButton btnCharge;
    private ImageButton btnLeftSignal;
    private ImageButton btnRightSignal;
    private ImageButton btnAccelerate;
    private ImageButton btnBrake;
    // ---------------------------------

    // build the vehicle interface and power on the vehicle
    @SuppressLint("ClickableViewAccessibility")
    private void startSimulator()
    {
        setContentView(R.layout.activity_home_vehicle_interface);

        ImageView signal = findViewById(R.id.signalIcon);
        signal.setForeground(getDrawable(R.drawable.connection_green));

        btnStop         = findViewById(R.id.stop);
        btnOptions      = findViewById(R.id.options);
        btnSeatbelt     = findViewById(R.id.seatbelt);
        btnLightsFault  = findViewById(R.id.lightWarning);
        btnTyrePressure = findViewById(R.id.lowTyrePressure);
        btnWiperFluid   = findViewById(R.id.lowWiperFluid);
        btnAirbag       = findViewById(R.id.airbag);
        btnBrakeFault   = findViewById(R.id.brakeWarning);
        btnABS          = findViewById(R.id.absWarning);
        btnEV           = findViewById(R.id.evWarning);
        temperatureBar  = findViewById(R.id.temperatureBar);
        btnLights       = findViewById(R.id.lights);
        btnCharge       = findViewById(R.id.charge);
        btnParkingBrake = findViewById(R.id.parkingBrake);
        btnLeftSignal   = findViewById(R.id.leftIndicator);
        btnRightSignal  = findViewById(R.id.rightIndicator);
        btnBrake        = findViewById(R.id.brake);
        btnAccelerate   = findViewById(R.id.throttle);

        // shows options for scenarios that can be run
        btnOptions.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                {
                    // build popup menu from layout file
                    PopupMenu popupMenu = new PopupMenu(VehicleInterface.this, btnOptions);
                    popupMenu.getMenuInflater().inflate(R.menu.options, popupMenu.getMenu());

                    // set response for selecting an item
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                    {
                        @Override
                        public boolean onMenuItemClick(MenuItem item)
                        {
                            // hide options button and show stop button
                            btnOptions.setVisibility(View.INVISIBLE);
                            btnStop.setVisibility(View.VISIBLE);

                            // find the selection and run scenario
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

        // stops scenario currently running
        btnStop.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                {
                    runScenario = false;                        // kill thread
                    btnStop.setVisibility(View.INVISIBLE);      // hide this button
                    btnOptions.setVisibility(View.VISIBLE);     // show options again
                }

                return true;
            }
        });

        // toggles seatbelt warning
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

        // toggles lights warning
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

        // toggles low tyre pressure warning
        btnTyrePressure.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleLowTyrePressure();

                return true;
            }
        });

        // toggles low wiper fluid warning
        btnWiperFluid.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleLowWiperFluid();

                return true;
            }
        });

        // toggles airbag warning
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

        // toggles brake warning
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

        // toggles ABS warning
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

        // toggles EV warning
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

        // update temperature with new slider value
        temperatureBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            // called when slider is moved, updates temperature with new progress of slider
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { vehicle.setBatteryTemp(progress); }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // toggles lights off/low/high
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

        // toggles charge state
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

        // toggles parking brake state
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

        // toggles left turn signal state
        btnLeftSignal.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleLeftTurnSignal();

                return true;
            }
        });

        // toggles right turn signal state
        btnRightSignal.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_DOWN)
                    vehicle.toggleRightTurnSignal();

                return true;
            }
        });

        // engages the brake when held down
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

        // engages the throttle when held down
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

        // begin vehicle setup
        vehicle.start();
    }

    // show/dim throttle and brake buttons
    private void toggleSpeedControls(boolean enabled)
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

    // automate a driving scenario that maxes out at given speed
    // randomly enables lights and turn signals as well
    private void drive(final int speed)
    {
        runScenario = true;

        // disable controls as they are used automatically
        btnLightsFault.setEnabled(false);
        btnLights.setEnabled(false);
        btnParkingBrake.setEnabled(false);
        btnCharge.setEnabled(false);
        btnLeftSignal.setEnabled(false);
        btnRightSignal.setEnabled(false);
        btnAccelerate.setEnabled(false);
        btnBrake.setEnabled(false);

        // simulate throttle press
        btnAccelerate.setForeground(getDrawable(R.drawable.throttle_active));

        // stop anything that will prevent throttle
        if (parkingBrakeOn) vehicle.toggleParkingBrake();
        if (isCharging)     vehicle.toggleCharging();

        // run a thread that manages braking and throttle
        Thread accelerator = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // kill thread if stopped or battery dies
                while (runScenario && vehicle.batteryLevel() > 0)
                {
                    // keeps a steady speed
                    // -------------------------------------------------------------------------------------------------
                    // accelerate until max speed is hit
                    vehicle.accelerate(); while (runScenario && vehicle.batteryLevel() > 0 && vehicle.speed() <= speed);

                    // slow down until speed steadies out
                    vehicle.decelerate(); while (runScenario && vehicle.batteryLevel() > 0 && vehicle.speed() > speed);
                    // -------------------------------------------------------------------------------------------------
                }

                // thread stopped, return control to user
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // simulate throttle release
                        btnAccelerate.setForeground(getDrawable(R.drawable.throttle));

                        // re-enable controls
                        btnLightsFault.setEnabled(true);
                        btnLights.setEnabled(true);
                        btnParkingBrake.setEnabled(true);
                        btnCharge.setEnabled(true);
                        btnLeftSignal.setEnabled(true);
                        btnRightSignal.setEnabled(true);
                        btnAccelerate.setEnabled(true);
                        btnBrake.setEnabled(true);

                        // toggle options and stop button back
                        if (btnStop.getVisibility() == View.VISIBLE)
                        {
                            btnStop.setVisibility(View.INVISIBLE);
                            btnOptions.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });

        // run a thread that manages using controls
        Thread controller = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // wait for a few seconds for vehicle to accelerate before doing anything
                try { Thread.sleep(3000); }
                catch (InterruptedException e) { e.printStackTrace(); }

                // kill thread if stopped or battery dies
                while (runScenario && vehicle.batteryLevel() > 0)
                {
                    // generate a random number to simulate user choosing a control to use
                    Random random = new Random();
                    int result = random.nextInt(4);

                    // choose control
                    switch (result)
                    {
                        // chose to do nothing
                        case 0: break;

                        // chose left signal
                        case 1:
                            // toggle on
                            vehicle.toggleLeftTurnSignal();

                            // sleep between 2-10 seconds
                            // ----------------------------------------------------
                            result = random.nextInt(10000);
                            if (result < 2000) result = 2000;

                            try { Thread.sleep(result); }
                            catch (InterruptedException e) { e.printStackTrace(); }
                            // ----------------------------------------------------

                            // toggle off if scenario still running after sleep
                            if (runScenario) vehicle.toggleLeftTurnSignal();

                            break;

                        // chose right signal
                        case 2:
                            vehicle.toggleRightTurnSignal();

                            // sleep between 2-10 seconds
                            // ----------------------------------------------------
                            result = random.nextInt(10000);
                            if (result < 2000) result = 2000;

                            try { Thread.sleep(result); }
                            catch (InterruptedException e) { e.printStackTrace(); }
                            // ----------------------------------------------------

                            // toggle off if scenario still running after sleep
                            if (runScenario) vehicle.toggleRightTurnSignal();

                            break;

                        // chose lights
                        case 3:
                            // toggle between on and off, no high beams
                            vehicle.toggleLights();
                            if (vehicle.areLightsHigh()) vehicle.toggleLights();
                    }

                    // sleep between 5-20 seconds before next choice
                    // ----------------------------------------------------
                    result = random.nextInt(20000);
                    if (result < 5000) result = 5000;

                    try { Thread.sleep(result); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                    // ----------------------------------------------------
                }
            }
        });

        // start threads
        accelerator.start();
        controller.start();
    }

    // automate a driving scenario that throttles to full speed and crashes
    // speed will drop to 0 quickly, airbag will engage and several faults will show
    private void crash()
    {
        runScenario = true;

        // disable controls as they are used automatically
        btnLightsFault.setEnabled(false);
        btnLights.setEnabled(false);
        btnParkingBrake.setEnabled(false);
        btnCharge.setEnabled(false);
        btnLeftSignal.setEnabled(false);
        btnRightSignal.setEnabled(false);
        btnAccelerate.setEnabled(false);
        btnBrake.setEnabled(false);

        // simulate throttle press
        btnAccelerate.setForeground(getDrawable(R.drawable.throttle_active));

        // stop anything that will prevent throttle
        if (parkingBrakeOn) vehicle.toggleParkingBrake();
        if (isCharging)     vehicle.toggleCharging();

        // run thread that begins throttling
        Thread drive = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // accelerate for 20 seconds
                //----------------------------------------------------------
                vehicle.accelerate();

                int totalSleep = 20000;
                int currentSleep = 0;

                // every 100th/second check the user hasnt stopped scenario
                while (runScenario && currentSleep < totalSleep)
                {
                    currentSleep += 100;

                    try { Thread.sleep(100); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                }
                //----------------------------------------------------------

                // crash is ignored if user cancelled
                if (runScenario)
                {
                    // simulate hitting a wall
                    vehicle.brake();

                    // toggle airbag and severe faults
                    vehicle.toggleAirbag();
                    vehicle.toggleBrakeFault();
                    vehicle.toggleBrakeFault();
                    vehicle.toggleABS();
                    vehicle.toggleABS();
                    vehicle.toggleEV();
                    vehicle.toggleEV();

                    runScenario = false;
                }

                // slowly stop instead of crashing
                else vehicle.decelerate();

                // thread finished, return control to user
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // simulate throttle release
                        btnAccelerate.setForeground(getDrawable(R.drawable.throttle));

                        // re-enable controls
                        btnLightsFault.setEnabled(true);
                        btnLights.setEnabled(true);
                        btnParkingBrake.setEnabled(true);
                        btnCharge.setEnabled(true);
                        btnLeftSignal.setEnabled(true);
                        btnRightSignal.setEnabled(true);
                        btnAccelerate.setEnabled(true);
                        btnBrake.setEnabled(true);

                        // toggle options and stop button back
                        if (btnStop.getVisibility() == View.VISIBLE)
                        {
                            btnStop.setVisibility(View.INVISIBLE);
                            btnOptions.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });

        // run thread
        drive.start();
    }

    // set limited battery level before starting drive scenario
    private void drive(final int speed, int batteryLevel)
    {
        vehicle.setBatteryLevel(batteryLevel);
        drive(speed);
    }

    @Override
    protected void onResume()
    {
        setContentView(R.layout.activity_home_loading);

        // needs to be passed to BLE as it will receive status updates on GAP and GATT
        VehicleService vehicleService = new VehicleService();

        // manages Bluetooth hardware and GATT server, created here as it needs to give this class as context and the permission and dashboard interface
        BluetoothLE bluetoothDevice = new BluetoothLE(this, this, this, vehicleService);

        // BLE will receive request responses
        respond = bluetoothDevice;

        // give service to vehicle manager so it can communicate changes to BLE
        vehicle = new VehicleManager(vehicleService, this);

        // initiate setup of Bluetooth hardware, if successful, calls setupComplete()
        vehicle.setupBluetooth(bluetoothDevice);

        super.onResume();
    }

    @Override
    protected void onPause()
    {
        // stop operation of everything
        vehicle.stop();
        super.onPause();
    }

    // DASHBOARD INTERFACE
    // -----------------------------------------------------------------------------------------------------------------------------
    @Override
    public void showToast(final String message, final int length)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(VehicleInterface.this, message, length).show();
            }
        });
    }

    // called once GATT is setup and vehicle has initialised
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
                if (newState == vehicle.STATE_OFF) btnSeatbelt.setForeground(getDrawable(R.drawable.seatbelt_warning_black));
                else                               btnSeatbelt.setForeground(getDrawable(R.drawable.seatbelt_warning_red));
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
                if (newState == vehicle.STATE_OFF) btnTyrePressure.setForeground(getDrawable(R.drawable.low_tire_pressure_black));
                else                               btnTyrePressure.setForeground(getDrawable(R.drawable.low_tire_pressure_red));
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
                if (newState == vehicle.STATE_OFF) btnWiperFluid.setForeground(getDrawable(R.drawable.low_wiper_fluid_black));
                else                               btnWiperFluid.setForeground(getDrawable(R.drawable.low_wiper_fluid_red));
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
                if (newState == vehicle.STATE_OFF) btnAirbag.setForeground(getDrawable(R.drawable.airbag_fault_black));
                else                               btnAirbag.setForeground(getDrawable(R.drawable.airbag_fault_red));
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
                if      (newState == vehicle.STATE_OFF)         btnBrakeFault.setForeground(getDrawable(R.drawable.brake_warning_black));
                else if (newState == vehicle.STATE_WARNING_LOW) btnBrakeFault.setForeground(getDrawable(R.drawable.brake_warning_orange));
                else                                            btnBrakeFault.setForeground(getDrawable(R.drawable.brake_warning_red));
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
                if      (newState == vehicle.STATE_OFF)         btnABS.setForeground(getDrawable(R.drawable.abs_fault_black));
                else if (newState == vehicle.STATE_WARNING_LOW) btnABS.setForeground(getDrawable(R.drawable.abs_fault_orange));
                else                                            btnABS.setForeground(getDrawable(R.drawable.abs_fault_red));
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
                if      (newState == vehicle.STATE_OFF)         btnEV.setForeground(getDrawable(R.drawable.electric_drive_system_fault_black));
                else if (newState == vehicle.STATE_WARNING_LOW) btnEV.setForeground(getDrawable(R.drawable.electric_drive_system_fault_orange));
                else                                            btnEV.setForeground(getDrawable(R.drawable.electric_drive_system_fault_red));
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
        this.isCharging = isCharging;

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (isCharging)
                {
                    // cannot drive while charging
                    toggleSpeedControls(false);
                    btnCharge.setForeground(getDrawable(R.drawable.charging_icon_green));
                }

                else
                {
                    // give drive controls back if parking brake off as well
                    if (!parkingBrakeOn) toggleSpeedControls(true);
                    btnCharge.setForeground(getDrawable(R.drawable.charging_icon_black));
                }
            }
        });
    }

    @Override
    public void toggleParkingBrake(final int newState)
    {
        parkingBrakeOn = newState == vehicle.STATE_ON;

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (newState == vehicle.STATE_OFF)
                {
                    // give drive controls back if not charging as well
                    if (!isCharging) toggleSpeedControls(true);
                    btnParkingBrake.setForeground(getDrawable(R.drawable.parking_brake_black));
                }

                else
                {
                    // cannot drive while parking brake on
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
                // create simple blink animation
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
    // used to ID the request made
    final int          REQUEST_CODE_LOCATION = 1;
    final int REQUEST_CODE_BLUETOOTH_ENABLED = 2;

    @Override
    public void requestLocation()
    {
        // location permission not granted yet, request
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);

        // already granted
        else respond.requestLocationResult(true);
    }

    // called once user responds to location request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results)
    {
        // respond to BLE with result
        if (requestCode == REQUEST_CODE_LOCATION) respond.requestLocationResult(results[0] == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void enableAdapter() { startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_CODE_BLUETOOTH_ENABLED); }

    // called once user responds to Bluetooth prompt
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // respond to BLE with result
        if (requestCode == REQUEST_CODE_BLUETOOTH_ENABLED) respond.enableAdapterResult(resultCode == RESULT_OK);
    }

    @Override
    public void setupFailed(String error) { showToast(error, Toast.LENGTH_LONG); finish(); }

    // Bluetooth hardware setup successful, start GATT and vehicle initialisation
    @Override
    public void setupComplete() { vehicle.setupVehicle(this); }
    // ----------------------------------------------------------------------------------------------------------------
}