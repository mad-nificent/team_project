package team_project.matt.vehicle_simulator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

class BatteryManager
{
    private Activity      context;          // required for shared prefs
    private VehicleStatus updateStatus;     // send updates about battery state

    // determines where power flows
    private enum State
    {
        IDLE,       // none
        CHARGING,   // power -> battery
        RUNNING     // power -> vehicle
    }

    private boolean isOn;           // blocks functionality when off
    private State   state;          // track the battery state
    private double  powerLevel;     // energy flowing to/from battery

    // data which vehicle interface has access to
    // -------------------------------------------------------
    private double batteryLevel;    // 0-100%
    private int    temperature;     // °C
    // -------------------------------------------------------

    // runs a new thread that manages the flow of power by monitoring the battery state
    private void startPowerCycle()
    {
        Thread battery = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // if the battery is switched off, the thread gets killed
                while (isOn)
                {
                    // idle sends power nowhere
                    if (state != State.IDLE)
                    {
                        // track whole number value of battery level before changing
                        int chargeBefore = (int) batteryLevel;

                        switch (state)
                        {
                            case CHARGING: if (batteryLevel < 100) batteryLevel += BatteryConstants.getChargingPower(); break;  // increase battery level
                            case RUNNING:  if (batteryLevel > 0)   batteryLevel -= powerLevel;                          break;  // decrease battery level
                        }

                        // track updated whole number value
                        int chargeAfter = (int) batteryLevel;

                        // user only sees int value, only send an update if value before decimal place changes
                        if (chargeAfter != chargeBefore) updateStatus.notifyBatteryLevelChanged(chargeAfter);

                        // sleep for given time to simulate operation of battery cycle
                        try { Thread.sleep(BatteryConstants.getSleepTime()); }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }
            }
        });

        // run the thread
        battery.start();
    }

    // initialise the battery settings:
    //      charge speed
    //      drain speed at low power and at high power (separately)
    //      time in ms to complete one cycle (how long thread sleeps for)
    // used to calculate power levels when charging, at low power, and at max power
    // --------------------------------------------------------------
    // also loads initial battery state
    BatteryManager(Activity context, VehicleStatus statusInterface, int minBatteryLifeMins, int maxBatteryLifeMins, int chargeTimeMins, int cycleTimeMs)
    {
        // cannot load shared prefs without context
        // cannot notify changes to battery without vehicle status implementation
        if (context != null && statusInterface != null)
        {
            this.context = context; updateStatus = statusInterface;

            // calculate power levels
            // -----------------------------------------------------------------------------------------------------------------------------------------------
            final int CONVERT_TO_MS = 60000;    // converts minutes to milliseconds

            // calculate how much power must be consumed to match specified max power drain time
            BatteryConstants.setMaxDrainSpeedMs(minBatteryLifeMins * CONVERT_TO_MS);                   // mins to ms
            double msToDrainOnePercentAtMax = (double) BatteryConstants.getMaxDrainSpeedMs() / 100;    // ms to drain 1% of battery
            BatteryConstants.setMaxPower(cycleTimeMs / msToDrainOnePercentAtMax);                      // power drain per cycle

            // do the same for min power level
            BatteryConstants.setMinDrainSpeedMs(maxBatteryLifeMins * CONVERT_TO_MS);
            double msToDrainOnePercentAtMin = (double) BatteryConstants.getMinDrainSpeedMs() / 100;
            BatteryConstants.setMinPower(cycleTimeMs / msToDrainOnePercentAtMin);

            // and the same for charging speed
            int    chargeTimeMs         = chargeTimeMins * CONVERT_TO_MS;
            double msToChargeOnePercent = (double) chargeTimeMs / 100;
            BatteryConstants.setChargingPower(cycleTimeMs / msToChargeOnePercent);

            BatteryConstants.setSleepTime(cycleTimeMs);     // save the cycle time as time thread must sleep for
            // -----------------------------------------------------------------------------------------------------------------------------------------------

            isOn       = false;
            powerLevel = BatteryConstants.getMinPower();

            // load the previous battery state if it exists
            // ------------------------------------------------------------------------------------------------------
            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.filename), Context.MODE_PRIVATE);

            boolean isCharging = sharedPreferences.getBoolean(context.getResources().getString(R.string.charging_key), false);
            if (isCharging) state = State.CHARGING; else state = State.IDLE;

            batteryLevel = sharedPreferences.getInt(context.getResources().getString(R.string.battery_level_key), 100);
            temperature  = sharedPreferences.getInt(context.getResources().getString(R.string.temperature_key), 20);
            // ------------------------------------------------------------------------------------------------------
        }
    }

    void switchOn()
    {
        if (!isOn && context != null && updateStatus != null)
        {
            isOn = true;

            // send out initial state of battery
            updateStatus.notifyChargingStateChanged(state == State.CHARGING);
            updateStatus.notifyBatteryLevelChanged((int) batteryLevel);
            updateStatus.notifyBatteryTemperatureChanged(temperature);

            // run battery cycle (operates on another thread)
            startPowerCycle();
        }
    }

    void switchOff()
    {
        if (isOn)
        {
            // save the state of battery first
            SharedPreferences.Editor editor = context.getSharedPreferences(context.getResources().getString(R.string.filename), Context.MODE_PRIVATE).edit();
            editor.putBoolean(context.getResources().getString(R.string.charging_key), state == State.CHARGING);
            editor.putInt(context.getResources().getString(R.string.battery_level_key), (int) batteryLevel);
            editor.putInt(context.getResources().getString(R.string.temperature_key), temperature);
            editor.apply();

            // stop flow of power and shut down cycle
            idle(); isOn = false;
        }
    }

    // begin consuming power
    void run() { if (state != State.RUNNING) powerLevel = BatteryConstants.getMinPower(); state = State.RUNNING; }

    // reduce power level and stop flow of power
    void idle() { if (state != State.IDLE) { powerLevel = 0; state = State.IDLE; } }

    void toggleChargingState()
    {
        if (state == State.CHARGING) idle();    // stop charging
        else state = State.CHARGING;            // start charging

        updateStatus.notifyChargingStateChanged(state == State.CHARGING);
    }

    void increasePowerLevel(double additionalPower)
    {
        // cannot consume power if battery is off or dead
        // cannot change flow of power while charging
        if (isOn && batteryLevel > 0 && state != State.CHARGING && additionalPower > 0)
        {
            // start running battery if not done so already
            if (state == State.IDLE) run();

            // ensure new power consumption does not exceed max power level
            if ((powerLevel + additionalPower) <= BatteryConstants.getMaxPower()) powerLevel += additionalPower;
        }
    }

    void decreasePowerLevel(double reducedPower)
    {
        // cannot consume power if battery is off or dead
        // cannot change flow of power while charging
        if (isOn && batteryLevel > 0 && state != State.CHARGING && reducedPower > 0)
            // ensure new power consumption does not go below min power level
            if ((powerLevel - reducedPower) >= BatteryConstants.getMinPower()) powerLevel -= reducedPower;
    }

    void setTemperature(int newTemperature)   { temperature  = newTemperature;  updateStatus.notifyBatteryTemperatureChanged(temperature);  }
    void setBatteryLevel(int newBatteryLevel) { batteryLevel = newBatteryLevel; updateStatus.notifyBatteryLevelChanged((int) batteryLevel); }

    boolean isOn()                { return isOn; }
    boolean isCharging()          { return state == State.CHARGING; }
    double  minPowerConsumption() { return BatteryConstants.getMinPower(); }
    double  chargeLeft()          { return batteryLevel; }

    // calculate time remaining in ms until battery dies
    double chargeLeftMs()
    {
        double timeRemaining = 0;

        if (batteryLevel >  0 && powerLevel > 0)
        {
            double drainOnePercentMs = BatteryConstants.getSleepTime() / powerLevel;    // ms 1% battery takes to drain at current power level
                       timeRemaining = drainOnePercentMs * batteryLevel;                // multiply by remaining % for total ms

            // value cannot exceed slowest drain time
            if (timeRemaining > BatteryConstants.getMinDrainSpeedMs())
                timeRemaining = BatteryConstants.getMinDrainSpeedMs();

            // or min drain time
            else if (powerLevel == BatteryConstants.getMaxPower() && timeRemaining > BatteryConstants.getMaxDrainSpeedMs())
                timeRemaining = BatteryConstants.getMaxDrainSpeedMs();

        }

        return timeRemaining;
    }
}
