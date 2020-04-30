package team_project.matt.vehicle_simulator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

class SpeedManager
{
    private Activity      context;          // required for shared prefs
    private VehicleStatus updateStatus;     // send updates about movement state

    // speed limitations
    private final int MIN_SPEED = 0;
    private final int MAX_SPEED = 120;

    // converts miles per hour to miles per millisecond
    private final int CONVERT_TO_MPMS = 3600000;

    // time to sleep in ms after adjusting speed
    private final int    BASE_ACCELERATION_RATE  = 75;       // starting sleep time
    private final double ACCELERATION_MULTIPLIER = 1.0075;   // rate to increase sleep time as speed increases (makes speed increase slower and slower)
    private final int    DECELERATION_RATE       = 500;      // flat sleep time for releasing throttle (slow stoppage)
    private final int    BRAKING_RATE            = 10;       // flat sleep time for braking (fast stoppage)

    private boolean started;    // blocks functionality when off
    private State   state;      // track movement state

    // data which vehicle interface has access to
    // -------------------------------------------
    private int     speed;      // MPH
    private double  distance;   // miles travelled
    // -------------------------------------------

    // determines current movement state
    enum State
    {
        IDLE,           // not moving
        ACCELERATING,   // moving
        BRAKING         // stopping
    };

    // runs a new thread that manages the current speed and state of movement
    private void run()
    {
        Thread engine = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // if engine stops, the thread gets killed
                while (started)
                {
                    // determine how to manipulate speed based on current movement state
                    switch (state)
                    {
                        // reduces speed and waits for moderate time
                        case IDLE: reduceSpeed(DECELERATION_RATE); break;

                        // increases speed and waits for calculated time
                        case ACCELERATING:
                            double sleepTime = BASE_ACCELERATION_RATE * Math.pow(ACCELERATION_MULTIPLIER, speed);
                            increaseSpeed((int) Math.ceil(sleepTime));
                            break;

                        // reduces speed and waits for a short time
                        case BRAKING: reduceSpeed(BRAKING_RATE); break;
                    }
                }
            }
        });

        // run the thread
        engine.start();
    }

    // increment speed and wait for given time
    private void increaseSpeed(int sleepTime)
    {
        // update the speed if within limits
        if (speed < MAX_SPEED) { speed += 1; updateStatus.notifySpeedChanged(speed); }

        // update distance
        calculateDistance(sleepTime);

        // sleep for given time to simulate resistance in climbing speed
        try { Thread.sleep(sleepTime); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }

    // decrement speed and wait for given time
    private void reduceSpeed(int sleepTime)
    {
        // update the speed if within limits
        if (speed > MIN_SPEED) { speed -= 1; updateStatus.notifySpeedChanged(speed); }

        // update distance
        calculateDistance(sleepTime);

        try { Thread.sleep(sleepTime); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }

    // calculates how far vehicle will travel during sleep based on current speed
    private void calculateDistance(int sleepTime)
    {
        // track whole number value of distance before changing
        int distanceBefore = (int) distance;

        // increase distance travelled based on how fast currently going
        // accounts for sleep time as it will not be updated again until wake
        double mpms = (double) speed / CONVERT_TO_MPMS;
        distance += mpms * sleepTime;

        // track updated whole number value
        int distanceAfter  = (int) distance;

        // user only sees int value, only send an update to interface if value before decimal place changes
        if (distanceBefore < distanceAfter) updateStatus.notifyDistanceChanged(distanceAfter);
    }

    // load total distance travelled so far
    SpeedManager(Activity context, VehicleStatus statusInterface)
    {
        // cannot load shared prefs without context
        // cannot notify changes to movement without vehicle status implementation
        if (context != null && statusInterface != null)
        {
            this.context = context; updateStatus = statusInterface;

            // load total distance travelled from shared prefs
            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.filename), Context.MODE_PRIVATE);
            distance = sharedPreferences.getInt(context.getResources().getString(R.string.distance_key), 0);
        }
    }

    void start()
    {
        if (!started && context != null && updateStatus != null)
        {
            started = true;

            // set initial state of movement
            state = State.IDLE; speed = MIN_SPEED;

            // send out total distance travelled
            updateStatus.notifyDistanceChanged((int) distance);

            // run engine (operates on another thread)
            run();
        }
    }

    void stop()
    {
        if (started)
        {
            // save the total distance travelled first
            SharedPreferences.Editor editor = context.getSharedPreferences(context.getResources().getString(R.string.filename), Context.MODE_PRIVATE).edit();
            editor.putInt(context.getResources().getString(R.string.distance_key), (int) distance);
            editor.apply();

            // stop movement and turn off
            speed = MIN_SPEED; state = State.IDLE; started = false;
        }
    }

    // change movement state of vehicle
    void accelerate() { if (started) state = State.ACCELERATING; }
    void decelerate() { if (started) state = State.IDLE; }
    void brake()      { if (started) state = State.BRAKING; }

    int   speed() { return speed; }
    State state() { return state; }
}
