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

    private Activity         context;               // used for shared preferences
    private VehicleDashboard vehicleDashboard;      // update interface when vehicle state changes
    private VehicleService   vehicleService;        // also send changes to service for broadcasting over BLE

    // state of warnings
    private int seatbelt;
    private int lowWiperFluid;
    private int lowTyrePressure;
    private int airbag;
    private int brakeWarning;
    private int ABS;
    private int EV;

    // state of vehicle controls
    private BatteryManager battery;                 // manages charge and temperature on background thread
    private SpeedManager   motor;                   // manages speed and distance on another background thread
    private int            range;                   // uses charge and speed to calculate miles left
    private int            lights;
    private int            parkingBrake;
    private int            turnSignal;

    private boolean started = false;

    VehicleManager(VehicleService vehicleService, VehicleDashboard vehicleDashboard)
    {
        this.vehicleService   = vehicleService;
        this.vehicleDashboard = vehicleDashboard;
    }

    void setupBluetooth(BluetoothLE bluetoothDevice) { vehicleService.beginSetup(bluetoothDevice, vehicleDashboard); }

    void setupVehicle(Activity context)
    {
        this.context = context;

        battery = new BatteryManager(context, this, 5, 600, 3, 1000);
        motor   = new SpeedManager(context, this);

        // load defaults
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
        range           = 0;

        // start the service
        vehicleService.start();
    }

    void start()
    {
        started = true;

        battery.turnOn();                   // loads battery defaults and sends out info to service and UI
        motor.start();                      // loads distance and speed 0 and sends that out
        calculateRange(motor.speed());      // load miles left and send that out

        // send other defaults out
        vehicleService.getCharacteristic(VehicleService.Property.SEATBELT).setData(seatbelt);
        vehicleService.getCharacteristic(VehicleService.Property.WIPER_LOW).setData(lowWiperFluid);
        vehicleService.getCharacteristic(VehicleService.Property.TYRE_PRESSURE_LOW).setData(lowTyrePressure);
        vehicleService.getCharacteristic(VehicleService.Property.AIRBAG_ERR).setData(airbag);
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

    void stop()
    {
        if (started && context != null)
        {
            // save state of vehicle
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

            battery.turnOff();      // will save battery state before shutting off
            motor.stop();           // will save distance as well
            vehicleService.stop();  // close service and BLE

            started = false;
        }
    }

    void toggleSeatbelt()
    {
        if (seatbelt == STATE_OFF) seatbelt = STATE_ON;
        else                       seatbelt = STATE_OFF;

        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.SEATBELT).setData(seatbelt);
            vehicleDashboard.toggleSeatbelt(seatbelt);
        }
    }

    void toggleLightsFault()
    {
        if (lights != STATE_LIGHTS_ERR) lights = STATE_LIGHTS_ERR;
        else                            lights = STATE_OFF;

        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.LIGHTS).setData(lights);
            vehicleDashboard.toggleLights(lights);
        }
    }

    void toggleTyrePressure()
    {
        if (lowTyrePressure == STATE_OFF) lowTyrePressure = STATE_ON;
        else                              lowTyrePressure = STATE_OFF;

        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.TYRE_PRESSURE_LOW).setData(lowTyrePressure);
            vehicleDashboard.toggleTyrePressure(lowTyrePressure);
        }
    }

    void toggleWiperFluid()
    {
        if (lowWiperFluid == STATE_OFF) lowWiperFluid = STATE_ON;
        else                            lowWiperFluid = STATE_OFF;

        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.WIPER_LOW).setData(lowWiperFluid);
            vehicleDashboard.toggleWiperFluid(lowWiperFluid);
        }
    }

    void toggleAirbag()
    {
        if (airbag == STATE_OFF) airbag = STATE_ON;
        else                     airbag = STATE_OFF;

        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.AIRBAG_ERR).setData(airbag);
            vehicleDashboard.toggleAirbag(airbag);
        }
    }

    void toggleBrakeFault()
    {
        if (brakeWarning == STATE_OFF)              brakeWarning = STATE_WARNING_LOW;
        else if (brakeWarning == STATE_WARNING_LOW) brakeWarning = STATE_WARNING_HIGH;
        else                                        brakeWarning = STATE_OFF;

        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.BRAKE_ERR).setData(brakeWarning);
            vehicleDashboard.toggleBrakeFault(brakeWarning);
        }
    }

    void toggleABS()
    {
        if (ABS == STATE_OFF)              ABS = STATE_WARNING_LOW;
        else if (ABS == STATE_WARNING_LOW) ABS = STATE_WARNING_HIGH;
        else                               ABS = STATE_OFF;

        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.ABS_ERR).setData(ABS);
            vehicleDashboard.toggleABSFault(ABS);
        }
    }

    void toggleEV()
    {
        if (EV == STATE_OFF)              EV = STATE_WARNING_LOW;
        else if (EV == STATE_WARNING_LOW) EV = STATE_WARNING_HIGH;
        else                              EV = STATE_OFF;

        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.EV_ERR).setData(EV);
            vehicleDashboard.toggleEVFault(EV);
        }
    }

    void setTemperature(int temperature) { battery.setTemperature(temperature); }

    void toggleLights()
    {
        if (lights == STATE_LIGHTS_ERR) toggleLightsFault();

        if (battery.isOn() && (int) battery.chargeLeft() > 0)
        {
            if (lights == STATE_OFF)
            {
                if (!consumingPower()) battery.run();
                lights = STATE_LIGHTS_LOW;
            }

            else if (lights == STATE_LIGHTS_LOW)
            {
                if (!consumingPower()) battery.run();
                lights = STATE_LIGHTS_HIGH;
            }

            else
            {
                lights = STATE_OFF;
                if (!consumingPower()) battery.idle();
            }

            if (started)
            {
                vehicleService.getCharacteristic(VehicleService.Property.LIGHTS).setData(lights);
                vehicleDashboard.toggleLights(lights);
            }
        }
    }

    void toggleParkingBrake()
    {
        if (parkingBrake == STATE_OFF) parkingBrake = STATE_ON;
        else                           parkingBrake = STATE_OFF;

        if (started)
        {
            vehicleService.getCharacteristic(VehicleService.Property.PARKING_BRAKE).setData(parkingBrake);
            vehicleDashboard.toggleParkingBrake(parkingBrake);
        }
    }

    void toggleCharging() { battery.toggleCharging(); }

    void toggleLeftIndicator()
    {
        if (battery.isOn() && (int) battery.chargeLeft() > 0)
        {
            if (turnSignal != STATE_SIGNAL_LEFT) turnSignal = STATE_SIGNAL_LEFT;
            else
            {
                turnSignal = STATE_OFF;
                if (!consumingPower()) battery.idle();
            }

            if (started)
            {
                vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL).setData(turnSignal);
                vehicleDashboard.toggleTurnSignal(turnSignal);
            }
        }
    }

    void toggleRightIndicator()
    {
        if (battery.isOn() && (int) battery.chargeLeft() > 0)
        {
            if (turnSignal != STATE_SIGNAL_RIGHT) turnSignal = STATE_SIGNAL_RIGHT;
            else
            {
                turnSignal = STATE_OFF;
                if (!consumingPower()) battery.idle();
            }

            if (started)
            {
                vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL).setData(turnSignal);
                vehicleDashboard.toggleTurnSignal(turnSignal);
            }
        }
    }

    void accelerate()
    {
        // need power and brake disengaged
        if (battery.isOn() && (int) battery.chargeLeft() > 0 && parkingBrake == STATE_OFF)
        {
            motor.accelerate();

            if (!consumingPower()) battery.run();

            // not cold start, adjust battery consumption to match current speed
            if (motor.speed() > 1) battery.increasePowerLevel(battery.minPowerConsumption() * motor.speed());
        }
    }

    void decelerate()
    {
        motor.decelerate();

        if (!consumingPower()) battery.idle();
        else battery.decreasePowerLevel(battery.minPowerConsumption() * motor.speed());
    }

    void brake()
    {
        motor.brake();

        if (!consumingPower()) battery.idle();
        else battery.decreasePowerLevel(battery.minPowerConsumption() * motor.speed());
    }

    void setBatteryLevel(int batteryLevel) { battery.setBatteryLevel(batteryLevel); }

    boolean lightsHigh()            { return lights == STATE_LIGHTS_HIGH; }
    boolean isParkingBrakeEngaged() { return parkingBrake == STATE_ON; }
    int     speed()                 { if (started) return motor.speed(); else return 0; }
    int     batteryLevel()          { if (started) return (int) battery.chargeLeft(); else return 0; }

    private void calculateRange(int speed)
    {
        if (speed == 0) speed = 1;

        // convert to secs
        double timeToEmpty = battery.timeRemainingMs() / 1000;

        // calculate range
        double milesPerSec = (double) speed / 3600;
        int          range =    (int) Math.round(milesPerSec * timeToEmpty);

        if (started && range != this.range)
        {
            vehicleService.getCharacteristic(VehicleService.Property.RANGE).setData(range);
            vehicleDashboard.updateRange(range);

            this.range = range;
        }
    }

    private boolean consumingPower()
    {
        boolean consumingPower = false;

        if      (motor.state() == SpeedManager.State.ACCELERATING)            consumingPower = true;
        else if (lights == STATE_LIGHTS_LOW  || lights == STATE_LIGHTS_HIGH)  consumingPower = true;

        return consumingPower;
    }

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

            calculateRange(motor.speed());
        }

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
        }

        // accelerating
        if (motor.state() == SpeedManager.State.ACCELERATING && speed > 1)
            battery.increasePowerLevel(battery.minPowerConsumption());
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
