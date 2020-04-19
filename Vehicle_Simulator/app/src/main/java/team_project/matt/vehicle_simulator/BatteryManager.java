package team_project.matt.vehicle_simulator;

import android.util.Log;

class BatteryManager
{
    enum State { IDLE, CHARGING, RUNNING };

    private final int CONVERT_TO_MS = 60000;

    // globals, not to be changed once set
    private int    SLEEP_TIME;
    private double CHARGING_POWER, MAX_POWER_PER_CYCLE, MIN_POWER_PER_CYCLE;
    private int    MAX_POWER_DRAIN_SPEED_MS, MIN_POWER_DRAIN_SPEED_MS;

    private boolean isOn;
    private State   state;
    private double  powerLevel;

    private double charge;
    private double temperature;

    private VehicleStatus updateStatus;

    BatteryManager(VehicleStatus vehicleStatus)
    {
        SLEEP_TIME = 1000;

        // how fast should max power drain? - 5 mins
        MAX_POWER_DRAIN_SPEED_MS        = 5 * CONVERT_TO_MS;                        // 300000
        double msToDrainOnePercentAtMax = (double) MAX_POWER_DRAIN_SPEED_MS / 100;  // 3000
        MAX_POWER_PER_CYCLE             = SLEEP_TIME / msToDrainOnePercentAtMax;    // 0.333...

        // how fast should min power drain? - 10 hours (600 mins)
        MIN_POWER_DRAIN_SPEED_MS        = 600 * CONVERT_TO_MS;                      // 36000000
        double msToDrainOnePercentAtMin = (double) MIN_POWER_DRAIN_SPEED_MS / 100;  // 360000
        MIN_POWER_PER_CYCLE             = SLEEP_TIME / msToDrainOnePercentAtMin;    // 0.002777...

        // how fast should charge? - 3 mins
        int chargeTime    = 3 * CONVERT_TO_MS;          // 180000
        double chargeGain = (double) chargeTime / 100;  // 1800
        CHARGING_POWER    = SLEEP_TIME / chargeGain;    // 0.555...

        isOn       = false;                 // waits for user to run battery cycles
        state      = State.IDLE;            // battery does not consume power initially
        powerLevel = MIN_POWER_PER_CYCLE;   // when it does start, consume min power until increased

        charge      = 10; // TODO: read charge from shared prefs, ALSO check if charging, if so toggle that on and update charge state on turn on
        temperature = 20;

        updateStatus = vehicleStatus;
    }

    void turnOn()
    {
        if (!isOn)
        {
            isOn = true;

            // send out initial charge state
            updateStatus.notifyBatteryLevelChanged(charge);
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
                switch (state)
                {
                    case CHARGING:
                        if (charge < 100) charge += CHARGING_POWER;
                        break;

                    case RUNNING:
                        if (charge > 0) charge -= powerLevel;
                        break;
                }

                updateStatus.notifyBatteryLevelChanged(charge);

                try { Thread.sleep(SLEEP_TIME); }
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
            powerLevel = MIN_POWER_PER_CYCLE;
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
        isOn = false;
        idle();
    }

    void increasePowerLevel(double additionalPower)
    {
        if (additionalPower > 0 && charge >  0 && state != State.CHARGING)
        {
            // now consuming power
            if (state == State.IDLE) state = State.RUNNING;

            // must be in range
            if ((powerLevel + additionalPower) <= MAX_POWER_PER_CYCLE)
            {
                powerLevel += additionalPower;
                //Log.d(this.getClass().getName(), "increasePowerLevel() -> Adding: " + additionalPower + ", New Total: " + powerLevel);
            }
        }
    }

    void decreasePowerLevel(double reducedPower)
    {
        if (reducedPower > 0 && charge >  0 && state != State.CHARGING)
        {
            // must be in range
            if ((powerLevel - reducedPower) >= MIN_POWER_PER_CYCLE)
            {
                powerLevel -= reducedPower;
                Log.d(this.getClass().getName(), "decreasePowerLevel() -> Reducing: " + reducedPower + ", New Total: " + powerLevel);
            }
        }
    }

    void setTemperature(double newTemperature)
    {
        temperature = newTemperature;
        updateStatus.notifyBatteryTemperatureChanged(temperature);
    }

    boolean isOn()                    { return isOn; }
    double  currentPowerConsumption() { return powerLevel; }
    double  minPowerConsumption()     { return MIN_POWER_PER_CYCLE; }
    double  maxPowerConsumption()     { return MAX_POWER_PER_CYCLE; }
    double  chargeLeft()              { return charge; }
    double  temperature()             { return temperature; }

    double timeRemaining()
    {
        double drainOnePercentMs = SLEEP_TIME / powerLevel;
        double timeRemaining     = drainOnePercentMs * charge;

        // value cannot exceed slowest drain time
        if (timeRemaining > MIN_POWER_DRAIN_SPEED_MS)
            timeRemaining = MIN_POWER_DRAIN_SPEED_MS;

        // precision can be lost when converting back
        else if (powerLevel == MAX_POWER_PER_CYCLE && timeRemaining > MAX_POWER_DRAIN_SPEED_MS)
            timeRemaining = MAX_POWER_DRAIN_SPEED_MS;

        return timeRemaining;
    }
}
