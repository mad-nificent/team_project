package team_project.matt.vehicle_simulator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

class VehicleManager implements VehicleStatus
{
    // states of controls and warnings
    final int STATE_OFF          = VehicleService.STATE_OFF;
    final int STATE_ON           = VehicleService.STATE_ON;
    final int STATE_SIGNAL_LEFT  = VehicleService.STATE_SIGNAL_LEFT;
    final int STATE_SIGNAL_RIGHT = VehicleService.STATE_SIGNAL_RIGHT;
    final int STATE_LIGHTS_LOW   = VehicleService.STATE_LIGHTS_LOW;
    final int STATE_LIGHTS_HIGH  = VehicleService.STATE_LIGHTS_HIGH;
    final int STATE_LIGHTS_ERR   = VehicleService.STATE_LIGHTS_ERR;
    final int STATE_WARNING_LOW  = VehicleService.STATE_WARNING_LOW;
    final int STATE_WARNING_HIGH = VehicleService.STATE_WARNING_HIGH;

    private Activity         context;               // required for shared prefs
    private VehicleDashboard vehicleDashboard;      // send updates to UI about state of vehicle
    private VehicleService   vehicleService;        // also send these changes to GATT service

    // track state of warnings
    private int seatbelt;
    private int lowWiperFluid;
    private int lowTyrePressure;
    private int airbag;
    private int brakeWarning;
    private int ABS;
    private int EV;

    // track state of vehicle controls
    private BatteryManager battery;         // manages charge and temperature on background thread
    private SpeedManager   engine;           // manages speed and distance on another background thread
    private int            range;           // uses charge and speed to calculate miles left
    private int            lights;
    private int            parkingBrake;
    private int            turnSignal;

    // blocks functionality when stopped
    private boolean started = false;

    // calculate miles left on current charge
    private void calculateRange(int speed)
    {
        if (started)
        {
            // dont waste time calculating if battery dead
            if (batteryLevel() > 0)
            {
                // allows range to be calculated at a stop
                if (speed == 0) speed = 1;

                double timeToEmpty = battery.chargeLeftMs();
                if (timeToEmpty > 0)
                {
                           timeToEmpty = battery.chargeLeftMs() / 1000;                     // convert time left in ms to secs
                    double milesPerSec = (double) speed / 3600;                             // how far is car going per second
                    int    range       = (int) Math.round(milesPerSec * timeToEmpty);       // multiply to get distance left

                    // dont waste time updating if range hasnt changed
                    if (range != this.range)
                    {
                        vehicleService.getCharacteristic(VehicleService.Property.RANGE).setData(range);
                        vehicleDashboard.updateRange(range);

                        this.range = range;
                    }
                }
            }

            // dead battery, 0 miles left
            else
            {
                vehicleService.getCharacteristic(VehicleService.Property.RANGE).setData(0);
                vehicleDashboard.updateRange(0);

                range = 0;
            }
        }
    }

    // figure out if anything in the car is using the battery
    private boolean usingBattery()
    {
        boolean usingBattery = false;

        // speed, lights and turn signals use battery
        if      (engine.state() == SpeedManager.State.ACCELERATING)                   usingBattery = true;
        else if (lights == STATE_LIGHTS_LOW  || lights == STATE_LIGHTS_HIGH)          usingBattery = true;
        else if (turnSignal == STATE_SIGNAL_LEFT || turnSignal == STATE_SIGNAL_RIGHT) usingBattery = true;

        // charging is providing power, dont want the state changed
        else if (battery.isCharging())                                                usingBattery = true;

        return usingBattery;
    }

    VehicleManager(VehicleService vehicleService, VehicleDashboard dashboardInterface)
    {
        // cannot notify changes to service or UI without instances
        if (vehicleService != null && dashboardInterface != null)
            this.vehicleService = vehicleService; this.vehicleDashboard = dashboardInterface;
    }

    // attempt to enable Bluetooth hardware, result returns to permission interface
    void setupBluetooth(BluetoothLE bluetoothDevice) { vehicleService.beginSetup(bluetoothDevice, vehicleDashboard); }

    // load vehicle state and start vehicle service
    void setupVehicle(Activity context)
    {
        // cannot load shared prefs without context
        if (context != null)
        {
            this.context = context;

            // build battery and engine managers
            battery = new BatteryManager(context, this, 5, 600, 3, 1000);
            engine  = new SpeedManager(context, this);

            // load saved state of vehicle if it exists
            // ------------------------------------------------------------------------------------------------------
            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.filename), Context.MODE_PRIVATE);

            seatbelt        = sharedPreferences.getInt(context.getResources().getString(R.string.seatbelt_key), STATE_OFF);
            lowWiperFluid   = sharedPreferences.getInt(context.getResources().getString(R.string.wiper_fluid_key), STATE_OFF);
            lowTyrePressure = sharedPreferences.getInt(context.getResources().getString(R.string.tyre_pressure_key), STATE_OFF);
            airbag          = sharedPreferences.getInt(context.getResources().getString(R.string.airbag_key), STATE_OFF);
            brakeWarning    = sharedPreferences.getInt(context.getResources().getString(R.string.brake_key), STATE_OFF);
            ABS             = sharedPreferences.getInt(context.getResources().getString(R.string.abs_key), STATE_OFF);
            EV              = sharedPreferences.getInt(context.getResources().getString(R.string.ev_key), STATE_OFF);
            lights          = sharedPreferences.getInt(context.getResources().getString(R.string.lights_key), STATE_OFF);
            parkingBrake    = sharedPreferences.getInt(context.getResources().getString(R.string.parking_brake_key), STATE_ON);
            turnSignal      = sharedPreferences.getInt(context.getResources().getString(R.string.turn_signal_key), STATE_OFF);
            // ------------------------------------------------------------------------------------------------------

            vehicleService.start();
        }
    }

    // send initial vehicle state to GATT and UI, start running threads
    void start()
    {
        if (vehicleService != null && vehicleDashboard != null)
        {
            started = true;

            battery.switchOn();               // loads battery defaults and sends out info to service and UI
            engine.start();                   // loads distance and speed 0 and sends that out
            calculateRange(engine.speed());   // load miles left and send that out

            // send other defaults out
            vehicleService.getCharacteristic(VehicleService.Property.SEATBELT).setData(seatbelt);
            vehicleService.getCharacteristic(VehicleService.Property.WIPER_LOW).setData(lowWiperFluid);
            vehicleService.getCharacteristic(VehicleService.Property.TYRE_PRESSURE_LOW).setData(lowTyrePressure);
            vehicleService.getCharacteristic(VehicleService.Property.AIRBAG).setData(airbag);
            vehicleService.getCharacteristic(VehicleService.Property.BRAKE_ERR).setData(brakeWarning);
            vehicleService.getCharacteristic(VehicleService.Property.ABS_ERR).setData(ABS);
            vehicleService.getCharacteristic(VehicleService.Property.EV_ERR).setData(EV);
            vehicleService.getCharacteristic(VehicleService.Property.LIGHTS).setData(lights);
            vehicleService.getCharacteristic(VehicleService.Property.PARKING_BRAKE).setData(parkingBrake);
            vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL).setData(turnSignal);

            // update UI
            vehicleDashboard.toggleSeatbelt(seatbelt);
            vehicleDashboard.toggleWiperFluid(lowWiperFluid);
            vehicleDashboard.toggleTyrePressure(lowTyrePressure);
            vehicleDashboard.toggleAirbag(airbag);
            vehicleDashboard.toggleBrakeFault(brakeWarning);
            vehicleDashboard.toggleABSFault(ABS);
            vehicleDashboard.toggleEVFault(EV);
            vehicleDashboard.toggleLights(lights);
            vehicleDashboard.toggleParkingBrake(parkingBrake);
            vehicleDashboard.toggleTurnSignal(turnSignal);
        }
    }

    void stop()
    {
        if (started && context != null)
        {
            // save state of vehicle first
            SharedPreferences.Editor editor = context.getSharedPreferences(context.getResources().getString(R.string.filename), Context.MODE_PRIVATE).edit();
            editor.putInt(context.getResources().getString(R.string.seatbelt_key), seatbelt);
            editor.putInt(context.getResources().getString(R.string.wiper_fluid_key), lowWiperFluid);
            editor.putInt(context.getResources().getString(R.string.tyre_pressure_key), lowTyrePressure);
            editor.putInt(context.getResources().getString(R.string.airbag_key), airbag);
            editor.putInt(context.getResources().getString(R.string.brake_key), brakeWarning);
            editor.putInt(context.getResources().getString(R.string.abs_key), ABS);
            editor.putInt(context.getResources().getString(R.string.ev_key), EV);
            editor.putInt(context.getResources().getString(R.string.lights_key), lights);
            editor.putInt(context.getResources().getString(R.string.parking_brake_key), parkingBrake);
            editor.putInt(context.getResources().getString(R.string.turn_signal_key), turnSignal);
            editor.apply();

            battery.switchOff();     // will save battery state before shutting off
            engine.stop();           // will save distance as well
            vehicleService.stop();   // close service and BLE

            started = false;
        }
    }

    void toggleSeatbelt()
    {
        if (started)
        {
            if (seatbelt == STATE_OFF) seatbelt = STATE_ON;
            else                       seatbelt = STATE_OFF;

            vehicleService.getCharacteristic(VehicleService.Property.SEATBELT).setData(seatbelt);
            vehicleDashboard.toggleSeatbelt(seatbelt);
        }
    }

    void toggleLightsFault()
    {
        if (started)
        {
            if (lights != STATE_LIGHTS_ERR) lights = STATE_LIGHTS_ERR;
            else                            lights = STATE_OFF;

            vehicleService.getCharacteristic(VehicleService.Property.LIGHTS).setData(lights);
            vehicleDashboard.toggleLights(lights);
        }
    }

    void toggleLowTyrePressure()
    {
        if (started)
        {
            if (lowTyrePressure == STATE_OFF) lowTyrePressure = STATE_ON;
            else                              lowTyrePressure = STATE_OFF;

            vehicleService.getCharacteristic(VehicleService.Property.TYRE_PRESSURE_LOW).setData(lowTyrePressure);
            vehicleDashboard.toggleTyrePressure(lowTyrePressure);
        }
    }

    void toggleLowWiperFluid()
    {
        if (started)
        {
            if (lowWiperFluid == STATE_OFF) lowWiperFluid = STATE_ON;
            else                            lowWiperFluid = STATE_OFF;

            vehicleService.getCharacteristic(VehicleService.Property.WIPER_LOW).setData(lowWiperFluid);
            vehicleDashboard.toggleWiperFluid(lowWiperFluid);
        }
    }

    void toggleAirbag()
    {
        if (started)
        {
            if (airbag == STATE_OFF) airbag = STATE_ON;
            else                     airbag = STATE_OFF;


            vehicleService.getCharacteristic(VehicleService.Property.AIRBAG).setData(airbag);
            vehicleDashboard.toggleAirbag(airbag);
        }
    }

    void toggleBrakeFault()
    {
        if (started)
        {
            if      (brakeWarning == STATE_OFF)         brakeWarning = STATE_WARNING_LOW;
            else if (brakeWarning == STATE_WARNING_LOW) brakeWarning = STATE_WARNING_HIGH;
            else                                        brakeWarning = STATE_OFF;


            vehicleService.getCharacteristic(VehicleService.Property.BRAKE_ERR).setData(brakeWarning);
            vehicleDashboard.toggleBrakeFault(brakeWarning);
        }
    }

    void toggleABS()
    {
        if (started)
        {
            if (ABS == STATE_OFF)              ABS = STATE_WARNING_LOW;
            else if (ABS == STATE_WARNING_LOW) ABS = STATE_WARNING_HIGH;
            else                               ABS = STATE_OFF;

            vehicleService.getCharacteristic(VehicleService.Property.ABS_ERR).setData(ABS);
            vehicleDashboard.toggleABSFault(ABS);
        }
    }

    void toggleEV()
    {
        if (started)
        {
            if (EV == STATE_OFF)              EV = STATE_WARNING_LOW;
            else if (EV == STATE_WARNING_LOW) EV = STATE_WARNING_HIGH;
            else                              EV = STATE_OFF;

            vehicleService.getCharacteristic(VehicleService.Property.EV_ERR).setData(EV);
            vehicleDashboard.toggleEVFault(EV);
        }
    }

    void setBatteryTemp(int temperature) { battery.setTemperature(temperature); }

    void toggleLights()
    {
        if (started)
        {
            // turn warning off first
            if (lights == STATE_LIGHTS_ERR) toggleLightsFault();

            // requires power
            if (battery.isOn() && (int) battery.chargeLeft() > 0)
            {
                if (lights == STATE_OFF)
                {
                    if (!usingBattery()) battery.run();   // run the battery if it was idle
                    lights = STATE_LIGHTS_LOW;            // turn on after checking power
                }

                else if (lights == STATE_LIGHTS_LOW)
                {
                    if (!usingBattery()) battery.run();   // run the battery if it was idle
                    lights = STATE_LIGHTS_HIGH;           // turn on after checking power
                }

                else
                {
                    lights = STATE_OFF;                   // turn off before checking power
                    if (!usingBattery()) battery.idle();  // idle the battery if it was not in use
                }

                vehicleService.getCharacteristic(VehicleService.Property.LIGHTS).setData(lights);
                vehicleDashboard.toggleLights(lights);
            }
        }
    }

    void toggleParkingBrake()
    {
        if (started)
        {
            if (parkingBrake == STATE_OFF) parkingBrake = STATE_ON;
            else                           parkingBrake = STATE_OFF;

            vehicleService.getCharacteristic(VehicleService.Property.PARKING_BRAKE).setData(parkingBrake);
            vehicleDashboard.toggleParkingBrake(parkingBrake);
        }
    }

    void toggleCharging() { battery.toggleChargingState(); }

    void toggleLeftTurnSignal()
    {
        if (started)
        {
            // requires power
            if (battery.isOn() && (int) battery.chargeLeft() > 0)
            {
                if (turnSignal != STATE_SIGNAL_LEFT)
                {
                    if (!usingBattery()) battery.run();   // run the battery if it was idle
                    turnSignal = STATE_SIGNAL_LEFT;       // turn on after checking power
                }

                else
                {
                    turnSignal = STATE_OFF;               // turn off before checking power
                    if (!usingBattery()) battery.idle();  // idle the battery if it was not in use
                }

                vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL).setData(turnSignal);
                vehicleDashboard.toggleTurnSignal(turnSignal);
            }
        }
    }

    void toggleRightTurnSignal()
    {
        if (started)
        {
            // requires power
            if (battery.isOn() && (int) battery.chargeLeft() > 0)
            {
                if (turnSignal != STATE_SIGNAL_RIGHT)
                {
                    if (!usingBattery()) battery.run();   // run the battery if it was idle
                    turnSignal = STATE_SIGNAL_RIGHT;      // turn on after checking power
                }

                else
                {
                    turnSignal = STATE_OFF;               // turn off before checking power
                    if (!usingBattery()) battery.idle();  // idle the battery if it was not in use
                }

                vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL).setData(turnSignal);
                vehicleDashboard.toggleTurnSignal(turnSignal);
            }
        }
    }

    void accelerate()
    {
        // need power, brake disengaged and not charging
        if (started && battery.isOn() && (int) battery.chargeLeft() > 0 && parkingBrake == STATE_OFF && !battery.isCharging())
        {
            calculateRange(engine.speed());

            // run the battery if it was idle
            if (!usingBattery()) battery.run();

            engine.accelerate();

            // not cold start, adjust battery consumption to match current speed
            if (engine.speed() > 1) battery.increasePowerLevel(battery.minPowerConsumption() * engine.speed());
        }
    }

    void decelerate()
    {
        engine.decelerate();

        // idle the battery if it was not in use
        if (!usingBattery()) battery.idle();

        // else reduce power by how much engine was consuming
        else battery.decreasePowerLevel(battery.minPowerConsumption() * engine.speed());
    }

    void brake()
    {
        engine.brake();

        // idle the battery if it was not in use
        if (!usingBattery()) battery.idle();

        // else reduce power by how much engine was consuming
        else battery.decreasePowerLevel(battery.minPowerConsumption() * engine.speed());
    }

    void setBatteryLevel(int batteryLevel) { battery.setBatteryLevel(batteryLevel); }

    boolean areLightsHigh() { return lights == STATE_LIGHTS_HIGH; }
    int     speed()         { if (started) return engine.speed(); else return 0; }
    int     batteryLevel()  { if (started) return (int) battery.chargeLeft(); else return 0; }

    // VEHICLE STATUS INTERFACE
    // -----------------------------------------------------------------------------------------------------------------------
    @Override
    public void notifyChargingStateChanged(boolean isCharging) { if (started) vehicleDashboard.toggleChargeMode(isCharging); }

    @Override
    public void notifyBatteryLevelChanged(int batteryLevel)
    {
        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL).setData(batteryLevel);
            vehicleDashboard.updateBatteryLevel(batteryLevel);

            calculateRange(engine.speed());

            // kill power to vehicle
            if (batteryLevel <= 0)
            {
                decelerate();

                turnSignal = STATE_OFF;
                vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL).setData(turnSignal);
                vehicleDashboard.toggleTurnSignal(turnSignal);

                if (lights != STATE_LIGHTS_ERR)
                {
                    lights = STATE_OFF;
                    vehicleService.getCharacteristic(VehicleService.Property.LIGHTS).setData(lights);
                    vehicleDashboard.toggleLights(lights);
                }

                battery.idle();
            }
        }
    }

    @Override
    public void notifyBatteryTemperatureChanged(int temperature)
    {
        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.BATTERY_TEMP).setData(temperature);
            vehicleDashboard.updateBatteryTemperature(temperature);
        }
    }

    @Override
    public void notifySpeedChanged(int speed)
    {
        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.SPEED).setData(speed);
            vehicleDashboard.updateSpeed(speed);

            // accelerating, increase power
            if (engine.state() == SpeedManager.State.ACCELERATING && speed > 1)
                battery.increasePowerLevel(battery.minPowerConsumption());
        }
    }

    @Override
    public void notifyDistanceChanged(int distance)
    {
        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.DISTANCE).setData(distance);
            vehicleDashboard.updateDistance(distance);
        }
    }
    // -----------------------------------------------------------------------------------------------------------------------
}
