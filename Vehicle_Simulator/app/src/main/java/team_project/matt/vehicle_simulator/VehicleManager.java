package team_project.matt.vehicle_simulator;

class VehicleManager implements VehicleStatus
{
    private Display        display;
    private VehicleService vehicleService;

    private BatteryManager battery;

    // controls & stats
    // -----------------------------------
    private SpeedManager motor;

    private int            turnSignal;
    private int            lights;
    private int            parkingBrake;
    private int            mileage;
    // -----------------------------------

    // warnings
    private int seatbelt;
    private int lowWiperFluid;
    private int lowTyrePressure;
    private int airbagFault;
    private int brakeFault;
    private int ABSFault;
    private int motorFault;

    VehicleManager(VehicleService vehicleService, Display display)
    {
        this.vehicleService = vehicleService;
        this.display = display;
    }

    void initialise()
    {
        vehicleService.start();

        battery = new BatteryManager(this);
        motor   = new SpeedManager(this);
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
        if (parkingBrake == vehicleService.STATE_ON)
        {
            battery.turnOff();
            motor.stop();

            vehicleService.stop();
        }
    }

    void toggleCharging() { battery.toggleCharging(); }

    void setTemperature(int temperature) { battery.setTemperature(temperature); }

    void toggleParkingBrake(boolean engaged)
    {
        if (engaged) parkingBrake = vehicleService.STATE_ON;
        else         parkingBrake = vehicleService.STATE_OFF;
    }

    void accelerate()
    {
        // need power and brake disengaged
        if (battery.isOn() && battery.chargeLeft() > 0 && parkingBrake == vehicleService.STATE_OFF)
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
        display.updateBatteryLevel((int)batteryLevel);
        vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL).setData((int)batteryLevel);

        updateMilesRemaining(motor.speed());
        if (batteryLevel <= 0.0) decelerate();    // kill power to vehicle
    }

    @Override
    public void notifyBatteryTemperatureChanged(double temperature)
    {
        display.updateBatteryTemperature((int)temperature);
        vehicleService.getCharacteristic(VehicleService.Property.BATTERY_TEMP).setData((int)temperature);
    }

    @Override
    public void notifySpeedChanged(int speed)
    {
        display.updateSpeed(speed);
        vehicleService.getCharacteristic(VehicleService.Property.SPEED).setData(speed);

        updatePowerConsumption(speed);
    }

    @Override
    public void notifyDistanceChanged(int distance)
    {
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

        vehicleService.getCharacteristic(VehicleService.Property.RANGE).setData(milesRemaining);
        display.updateRange(milesRemaining);
    }
}
