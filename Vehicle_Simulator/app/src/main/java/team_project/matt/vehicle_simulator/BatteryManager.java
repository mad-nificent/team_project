package team_project.matt.vehicle_simulator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

class BatteryManager
{
    private Activity      context;
    private VehicleStatus updateStatus;

    // internal data, affects battery operation
    // ----------------------------------------
    enum State { IDLE, CHARGING, RUNNING };

    private boolean isOn;
    private State   state;
    private double  powerLevel;
    // ----------------------------------------

    // data shown to user
    private double chargeLevel;
    private int    temperature;

    BatteryManager(Activity context, VehicleStatus vehicleStatus, int minBatteryLifeMins, int maxBatteryLifeMins, int chargeTimeMins, int updateRateMs)
    {
        this.context = context;
        updateStatus = vehicleStatus;

        BatteryConstants.setSleepTime(updateRateMs);

        final int CONVERT_TO_MS = 60000;

        // calculate max power level based on provided battery life time
        BatteryConstants.setmaxDrainSpeedMs(minBatteryLifeMins * CONVERT_TO_MS);                   // convert mins to ms
        double msToDrainOnePercentAtMax = (double) BatteryConstants.getmaxDrainSpeedMs() / 100;    // time in ms to drain 1%
        BatteryConstants.setMaxPower(BatteryConstants.getSleepTime() / msToDrainOnePercentAtMax);  // how much to drain at each cycle to achieve this rate

        // do the same for min power level
        BatteryConstants.setminDrainSpeedMs(maxBatteryLifeMins * CONVERT_TO_MS);
        double msToDrainOnePercentAtMin = (double) BatteryConstants.getminDrainSpeedMs() / 100;
        BatteryConstants.setMinPower(BatteryConstants.getSleepTime() / msToDrainOnePercentAtMin);

        // and the same for charging speed
        int            chargeTimeMs = chargeTimeMins * CONVERT_TO_MS;
        double msToChargeOnePercent = (double) chargeTimeMs / 100;
        BatteryConstants.setChargingPower(BatteryConstants.getSleepTime() / msToChargeOnePercent);

        // set defaults
        // ------------------------------------------------------------------------------------------------------------------------------------------
        isOn       = false;                            // waits for user to run battery cycles
        powerLevel = BatteryConstants.getMinPower();

        // read saved state of vehicle
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.filename), Context.MODE_PRIVATE);
        chargeLevel = sharedPreferences.getInt(context.getResources().getString(R.string.battery_level_key), 100);
        temperature = sharedPreferences.getInt(context.getResources().getString(R.string.temperature_key), 20);
        boolean isCharging  = sharedPreferences.getBoolean(context.getResources().getString(R.string.charging_key), false);

        if (isCharging) state = State.CHARGING;
        else            state = State.IDLE;
        // ------------------------------------------------------------------------------------------------------------------------------------------
    }

    void turnOn()
    {
        if (!isOn)
        {
            isOn = true;

            // send out initial state
            updateStatus.notifyChargingStateChanged(state == State.CHARGING);
            updateStatus.notifyBatteryLevelChanged((int) chargeLevel);
            updateStatus.notifyBatteryTemperatureChanged(temperature);

            new Thread(new Runnable() { @Override public void run() { startPowerCycle(); } }).start();
        }
    }

    private void startPowerCycle()
    {
        while (isOn)
        {
            // idle will just loop with no changes
            if (state != State.IDLE)
            {
                int chargeBefore = (int) chargeLevel;

                switch (state)
                {
                    case CHARGING:
                        if (chargeLevel < 100) chargeLevel += BatteryConstants.getChargingPower();
                        break;

                    case RUNNING:
                        if (chargeLevel > 0) chargeLevel -= powerLevel;
                        break;
                }

                int chargeAfter = (int) chargeLevel;

                // only notify when whole number changes
                if (chargeAfter != chargeBefore)
                    updateStatus.notifyBatteryLevelChanged(chargeAfter);

                try { Thread.sleep(BatteryConstants.getSleepTime()); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
    }

    void idle()
    {
        // moves to idle state, will no longer consume power
        if (state != State.IDLE)
        {
            state = State.IDLE;
            powerLevel = BatteryConstants.getMinPower();
        }
    }

    void toggleCharging()
    {
        boolean isCharging = false;

        if (state == State.CHARGING) idle();      // toggle off
        else
        {
            // toggle on
            state = State.CHARGING;
            isCharging = true;
        }

        updateStatus.notifyChargingStateChanged(isCharging);
    }

    void turnOff()
    {
        // save state of battery
        SharedPreferences.Editor editor = context.getSharedPreferences(context.getResources().getString(R.string.filename), Context.MODE_PRIVATE).edit();
        editor.putBoolean(context.getResources().getString(R.string.charging_key), state == State.CHARGING);
        editor.putInt(context.getResources().getString(R.string.battery_level_key), (int) chargeLevel);
        editor.putInt(context.getResources().getString(R.string.temperature_key), temperature);
        editor.apply();

        // stop consuming power and stop background thread
        idle();
        isOn = false;
    }

    void increasePowerLevel(double additionalPower)
    {
        if (additionalPower > 0 && chargeLevel >  0 && state != State.CHARGING)
        {
            // now consuming power
            if (state == State.IDLE) state = State.RUNNING;

            // must be in range
            if ((powerLevel + additionalPower) <= BatteryConstants.getMaxPower())
                powerLevel += additionalPower;
        }
    }

    void decreasePowerLevel(double reducedPower)
    {
        if (reducedPower > 0 && chargeLevel >  0 && state != State.CHARGING)
        {
            // must be in range
            if ((powerLevel - reducedPower) >= BatteryConstants.getMinPower())
                powerLevel -= reducedPower;
        }
    }

    void setTemperature(int newTemperature)
    {
        temperature = newTemperature;
        updateStatus.notifyBatteryTemperatureChanged(temperature);
    }

    void setBatteryLevel(int newBatteryLevel)
    {
        chargeLevel = newBatteryLevel;
        updateStatus.notifyBatteryLevelChanged((int) chargeLevel);
    }

    boolean isOn()                    { return isOn; }
    double  currentPowerConsumption() { return powerLevel; }
    double  minPowerConsumption()     { return BatteryConstants.getMinPower(); }
    double  maxPowerConsumption()     { return BatteryConstants.getMaxPower(); }
    double  chargeLeft()              { return chargeLevel; }
    double  temperature()             { return temperature; }

    double timeRemainingMs()
    {
        double drainOnePercentMs = BatteryConstants.getSleepTime() / powerLevel;
        double timeRemaining     = drainOnePercentMs * chargeLevel;

        // value cannot exceed slowest drain time
        if (timeRemaining > BatteryConstants.getminDrainSpeedMs())
            timeRemaining = BatteryConstants.getminDrainSpeedMs();

        // or min drain time
        else if (powerLevel == BatteryConstants.getMaxPower() && timeRemaining > BatteryConstants.getmaxDrainSpeedMs())
            timeRemaining = BatteryConstants.getmaxDrainSpeedMs();

        return timeRemaining;
    }
}
