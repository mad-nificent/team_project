package team_project.matt.vehicle_simulator;

import android.util.Log;

class VehicleManager implements VehicleStatus
{
    private Display display;

    private VehicleService vehicleService;

    // battery data
    private BatteryManager batteryManager;
    private int            range;
    private double         batteryTemp;

    // controls & stats
    private SpeedManager   speedManager;
    private int            turnSignal;
    private int            lights;
    private int            parkingBrake;
    private int            mileage;

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

        speedManager   = new SpeedManager(this);
        batteryManager = new BatteryManager(this);

        // load variables from shared prefs and apply to battery, mileage etc.
    }

    void start()
    {
        vehicleService.start();
    }

    void stop()
    {
        vehicleService.stop();

        // idle any properties
    }

    void charge()
    {
        if (speedManager.getSpeed() == 0)
        {

        }
    }

    void accelerate()
    {
        if (batteryManager.getCharge() > 0)
        {
            // set appropriate power consumption at speed to accelerate from
            batteryManager.setPowerLevel(speedManager.getSpeed() * batteryManager.MAX_POWER);

            // start accelerating and using power
            batteryManager.consumePower();
            speedManager.accelerate();
        }
    }

    void idle()
    {
        batteryManager.idle();
        speedManager.decelerate();
    }

    void brake()
    {
        batteryManager.idle();
        speedManager.brake();
    }

    @Override
    public void reportChargingState(boolean isCharging)
    {
        display.chargeMode(isCharging);

        if (isCharging)
        {
            // block controls
        }
    }

    @Override
    public void reportBatteryLevel(int level)
    {
        // kill power to vehicle
        if (level == 0)
        {
            idle();
        }

        // report to UI
        vehicleService.getCharacteristic(VehicleService.Property.BATTERY_LVL).setData(level);
        display.updateChargeLevel(level);
    }

    @Override
    public void reportSpeed(int speed)
    {
        vehicleService.getCharacteristic(VehicleService.Property.SPEED).setData(speed);
        display.updateSpeed(speed);
        updateBatteryStats(speed);
    }

    private void updateBatteryStats(int speed)
    {
        updatePowerConsumption(speed);
        updateMilesRemaining(speed);
    }

    private void updatePowerConsumption(int speed)
    {
        // moving, calculate power usage -> speed goes up, power goes up and vice versa
        if (speedManager.getState() == SpeedManager.State.ACCELERATING)
            if (speed > 0) batteryManager.setPowerLevel(batteryManager.MIN_POWER / speed);

        else batteryManager.idle();
    }

    private void updateMilesRemaining(int speed)
    {
        int powerConsumption = batteryManager.getState();
        if (powerConsumption != batteryManager.IDLE)
        {
            // calculate mileage
            int charge         = batteryManager.getCharge();
            int timeToEmpty    = (powerConsumption * charge) / 1000;       // time in secs till 0% battery based on current power drain
            double milesPerSec = (double) speed / 3600;
            int milesRemaining = (int) Math.round(milesPerSec * timeToEmpty);

            Log.d(this.getClass().getName(), "updateMilesRemaining() -> Charge: " + charge);
            Log.d(this.getClass().getName(), "updateMilesRemaining() -> Power Consumption: " + powerConsumption);
            Log.d(this.getClass().getName(), "updateMilesRemaining() -> MPS: " + milesPerSec);

            vehicleService.getCharacteristic(VehicleService.Property.RANGE).setData(milesRemaining);
            display.updateRange(milesRemaining);
        }
    }
}
