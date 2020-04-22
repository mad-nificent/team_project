package team_project.matt.vehicle_simulator;

import android.app.Activity;

class VehicleManager extends Activity implements VehicleStatus
{
    final int STATE_OFF = VehicleService.STATE_OFF;
    final int STATE_ON  = VehicleService.STATE_ON;

    final int STATE_SIGNAL_LEFT  = VehicleService.STATE_SIGNAL_LEFT;
    final int STATE_SIGNAL_RIGHT = VehicleService.STATE_SIGNAL_RIGHT;

    final int STATE_LIGHTS_LOW   = VehicleService.STATE_LIGHTS_LOW;
    final int STATE_LIGHTS_HIGH  = VehicleService.STATE_LIGHTS_HIGH;
    final int STATE_LIGHTS_ERR   = VehicleService.STATE_LIGHTS_ERR;

    final int STATE_WARNING_LOW  = VehicleService.STATE_WARNING_LOW;
    final int STATE_WARNING_HIGH = VehicleService.STATE_WARNING_HIGH;

    private Activity       context;
    private Display        display;
    private VehicleService vehicleService;

    // warnings
    private int warningsActive = 0;
    private int seatbelt;
    private int lowWiperFluid;
    private int lowTyrePressure;
    private int airbagFault;
    private int brakeFault;
    private int ABSFault;
    private int EVFault;

    private BatteryManager battery;

    // controls & stats
    // -----------------------------------
    private SpeedManager motor;

    private int turnSignal;
    private int lights;
    private int parkingBrake;
    private int mileage;
    // -----------------------------------

    VehicleManager(Activity context, VehicleService vehicleService, Display display)
    {
        this.vehicleService = vehicleService;
        this.display = display;
    }

    void initialise()
    {
        vehicleService.start();

        seatbelt        = STATE_OFF;
        lowWiperFluid   = STATE_OFF;
        lowTyrePressure = STATE_OFF;
        airbagFault     = STATE_OFF;
        brakeFault      = STATE_OFF;
        ABSFault        = STATE_OFF;
        EVFault         = STATE_OFF;

        battery = new BatteryManager(context, this);
        motor   = new SpeedManager(this);

        mileage = 0;
    }

    void start()
    {
        battery.turnOn();
        motor.start();


        updateMilesRemaining(motor.speed());
        // load variables from shared prefs and apply to battery, mileage etc.
    }

    void stop()
    {
        if (parkingBrake == STATE_ON)
        {
            battery.turnOff();
            motor.stop();

            vehicleService.stop();
        }
    }

    void toggleSeatbelt()
    {
        if (seatbelt == STATE_OFF)
        {
            seatbelt = STATE_ON;
            increaseWarningCount();
        }

        else
        {
            seatbelt = STATE_OFF;
            decreaseWarningCount();
        }

        vehicleService.getCharacteristic(VehicleService.Property.SEATBELT).setData(seatbelt);
        display.toggleSeatbelt(seatbelt);
    }

    void toggleLightsFault()
    {
        if (lights != STATE_LIGHTS_ERR)
        {
            lights = STATE_LIGHTS_ERR;
            increaseWarningCount();
        }

        else
        {
            lights = STATE_OFF;
            decreaseWarningCount();
        }

        vehicleService.getCharacteristic(VehicleService.Property.LIGHTS).setData(lights);
        display.toggleLights(lights);
    }

    void toggleTyrePressure()
    {
        if (lowTyrePressure == STATE_OFF)
        {
            lowTyrePressure = STATE_ON;
            increaseWarningCount();
        }

        else
        {
            lowTyrePressure = STATE_OFF;
            decreaseWarningCount();
        }

        vehicleService.getCharacteristic(VehicleService.Property.TYRE_PRESSURE_LOW).setData(lowTyrePressure);
        display.toggleTyrePressure(lowTyrePressure);
    }

    void toggleWiperFluid()
    {
        if (lowWiperFluid == STATE_OFF)
        {
            lowWiperFluid = STATE_ON;
            increaseWarningCount();
        }

        else
        {
            lowWiperFluid = STATE_OFF;
            decreaseWarningCount();
        }

        vehicleService.getCharacteristic(VehicleService.Property.WIPER_LOW).setData(lowWiperFluid);
        display.toggleWiperFluid(lowWiperFluid);
    }

    void toggleAirbag()
    {
        if (airbagFault == STATE_OFF)
        {
            airbagFault = STATE_ON;
            increaseWarningCount();
        }

        else
        {
            airbagFault = STATE_OFF;
            decreaseWarningCount();
        }

        vehicleService.getCharacteristic(VehicleService.Property.AIRBAG_ERR).setData(airbagFault);
        display.toggleAirbag(airbagFault);
    }

    void toggleBrakeFault()
    {
        if (brakeFault == STATE_OFF)
        {
            brakeFault = STATE_WARNING_LOW;
            increaseWarningCount();
        }
        else if (brakeFault == STATE_WARNING_LOW) brakeFault = STATE_WARNING_HIGH;
        else
        {
            brakeFault = STATE_OFF;
            decreaseWarningCount();
        }

        vehicleService.getCharacteristic(VehicleService.Property.BRAKE_ERR).setData(brakeFault);
        display.toggleBrakeFault(brakeFault);
    }

    void toggleABS()
    {
        if (ABSFault == STATE_OFF)
        {
            ABSFault = STATE_WARNING_LOW;
            increaseWarningCount();
        }
        else if (ABSFault == STATE_WARNING_LOW) ABSFault = STATE_WARNING_HIGH;
        else
        {
            ABSFault = STATE_OFF;
            decreaseWarningCount();
        }

        vehicleService.getCharacteristic(VehicleService.Property.ABS_ERR).setData(ABSFault);
        display.toggleABSFault(ABSFault);
    }

    void toggleEV()
    {
        if (EVFault == STATE_OFF)
        {
            EVFault = STATE_WARNING_LOW;
            increaseWarningCount();
        }
        else if (EVFault == STATE_WARNING_LOW) EVFault = STATE_WARNING_HIGH;
        else
        {
            EVFault = STATE_OFF;
            decreaseWarningCount();
        }

        vehicleService.getCharacteristic(VehicleService.Property.EV_ERR).setData(EVFault);
        display.toggleEVFault(EVFault);
    }

    private void increaseWarningCount()
    {
        if (warningsActive == 0) display.toggleWarning(STATE_ON);

        warningsActive++;
    }

    private void decreaseWarningCount()
    {
        warningsActive--;

        if (warningsActive == 0) display.toggleWarning(STATE_OFF);
    }

    void setTemperature(int temperature) { battery.setTemperature(temperature); }

    void toggleLights()
    {
        if (lights == STATE_LIGHTS_ERR) toggleLightsFault();

        if      (lights == STATE_OFF)        lights = STATE_LIGHTS_LOW;
        else if (lights == STATE_LIGHTS_LOW) lights = STATE_LIGHTS_HIGH;
        else                                 lights = STATE_OFF;

        vehicleService.getCharacteristic(VehicleService.Property.LIGHTS).setData(lights);
        display.toggleLights(lights);
    }

    void toggleParkingBrake()
    {
        if (parkingBrake == STATE_OFF) parkingBrake = STATE_ON;
        else                           parkingBrake = STATE_OFF;

        vehicleService.getCharacteristic(VehicleService.Property.PARKING_BRAKE).setData(parkingBrake);
        display.toggleParkingBrake(parkingBrake);
    }

    void toggleCharging() { battery.toggleCharging(); }

    void toggleLeftIndicator()
    {
        if (turnSignal != STATE_SIGNAL_LEFT) turnSignal = STATE_SIGNAL_LEFT;
        else                                 turnSignal = STATE_OFF;

        vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL).setData(turnSignal);
        display.toggleIndicator(turnSignal);
    }

    void toggleRightIndicator()
    {
        if (turnSignal != STATE_SIGNAL_RIGHT) turnSignal = STATE_SIGNAL_RIGHT;
        else                                  turnSignal = STATE_OFF;

        vehicleService.getCharacteristic(VehicleService.Property.TURN_SIGNAL).setData(turnSignal);
        display.toggleIndicator(turnSignal);
    }

    void accelerate()
    {
        // need power and brake disengaged
        if (battery.isOn() && battery.chargeLeft() > 0 && parkingBrake == STATE_OFF)
        {
            // not cold start, adjust battery consumption to match current speed
            if (motor.speed() > 1) battery.increasePowerLevel(battery.minPowerConsumption() * motor.speed());

            motor.accelerate();
        }
    }

    void decelerate()
    {
        if (!consumingPower()) battery.idle();
        else battery.decreasePowerLevel(battery.minPowerConsumption() * motor.speed());

        motor.decelerate();
    }

    void brake()
    {
        if (!consumingPower()) battery.idle();
        else battery.decreasePowerLevel(battery.minPowerConsumption() * motor.speed());

        motor.brake();
    }

    private boolean consumingPower()
    {
        return false;
    }

    @Override
    public void notifyChargingStateChanged(boolean isCharging)
    {
        display.updateChargeMode(isCharging);
    }

    @Override
    public void notifyBatteryLevelChanged(double batteryLevel)
    {
        // TODO: only update battery when int value changes
        vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL).setData((int)batteryLevel);
        display.updateBatteryLevel((int)batteryLevel);

        updateMilesRemaining(motor.speed());
        if (batteryLevel <= 0.0) decelerate();    // kill power to vehicle
    }

    @Override
    public void notifyBatteryTemperatureChanged(double temperature)
    {
        vehicleService.getCharacteristic(VehicleService.Property.BATTERY_TEMP).setData((int)temperature);
        display.updateBatteryTemperature((int)temperature);
    }

    @Override
    public void notifySpeedChanged(int speed)
    {
        vehicleService.getCharacteristic(VehicleService.Property.SPEED).setData(speed);
        display.updateSpeed(speed);

        updatePowerConsumption(speed);
    }

    @Override
    public void notifyDistanceChanged(int distance)
    {
        vehicleService.getCharacteristic(VehicleService.Property.DISTANCE).setData(distance);
        display.updateDistance(distance);
    }

    private void updatePowerConsumption(int speed)
    {
        // accelerating
        if (motor.state() == SpeedManager.State.ACCELERATING && speed > 1)
            battery.increasePowerLevel(battery.minPowerConsumption());
    }

    private void updateMilesRemaining(int speed)
    {
        if (speed == 0) speed = 1;

        // time in secs till 0% battery
        double timeToEmpty = battery.timeRemaining() / 1000;

        // calculate mileage
        double milesPerSec = (double) speed / 3600;
        int milesRemaining = (int) Math.round(milesPerSec * timeToEmpty);

        //Log.d(this.getClass().getName(), "updateMilesRemaining() -> TTE: " + timeToEmpty + " @ " + speed + " MPH");
        //Log.d(this.getClass().getName(), "updateMilesRemaining() -> MPS: " + milesPerSec);

        if (milesRemaining != mileage)
        {
            vehicleService.getCharacteristic(VehicleService.Property.RANGE).setData(milesRemaining);
            display.updateRange(milesRemaining);

            mileage = milesRemaining;
        }
    }
}
