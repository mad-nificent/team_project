package team_project.matt.vehicle_simulator;

import android.util.Log;

class BatteryManager
{
    // represents state of the battery and sleep time to simulate drain/charge over time
    final int   IDLE = 1,               // time not used, simply represents idle state
                CHARGING = 600,         // 1 min full charge from 0%
                MAX_POWER = 3000,       // 5 mins full drain at 100%
                MIN_POWER = 360000;     // 1 hr full drain at 100%

    private int state;
    private int charge;

    private VehicleStatus updateStatus;

    BatteryManager(VehicleStatus vehicleStatus)
    {
        updateStatus = vehicleStatus;

        // read saved drain settings and current charge from shared prefs

        // for now use
        state = IDLE;
        charge = 100;
    }

    void consumePower()
    {
        if (state != CHARGING)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    // deplete until drained or car idles/charges
                    while (charge > 0 && state >= MAX_POWER && state <= MIN_POWER)
                    {
                        charge -= 1;

                        updateStatus.reportBatteryLevel(charge);

                        try { Thread.sleep(state); }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }

            }).start();
        }
    }

    boolean setPowerLevel(int power)
    {
        boolean didUpdate = false;

        if (state != CHARGING)
        {
            if (power <= MIN_POWER && power >= MAX_POWER)
            {
                state = power;
                didUpdate = true;
            }

            else Log.e(this.getClass().getName(), "updateDrainSpeed() -> Failed to update. Value not within limits.");
        }

        else Log.e(this.getClass().getName(), "updateDrainSpeed() -> Failed to update. Battery is charging.");

        return didUpdate;
    }

    void idle() { if (state != CHARGING) state = IDLE; }

    void charge()
    {
        if (state == IDLE)
        {
            state = CHARGING;

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    updateStatus.reportChargingState(true);

                    while (charge < 100 && state == CHARGING)
                    {
                        charge += 1;

                        updateStatus.reportBatteryLevel(charge);

                        try { Thread.sleep(state); }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }

                    updateStatus.reportChargingState(false);
                }

            }).start();
        }
    }

    void stopCharging() { state = IDLE; }

    void setCharge(int charge)
    {
        if (state == IDLE && charge > 0 && charge < 100)
            this.charge = charge;
    }

    int getCharge() { return charge; }
    int getState()  { return state; }
}
