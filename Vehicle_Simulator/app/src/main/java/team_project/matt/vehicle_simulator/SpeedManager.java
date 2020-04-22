package team_project.matt.vehicle_simulator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

class SpeedManager
{
    // states of throttle
    enum State { IDLE, ACCELERATING, BRAKING };

    private final int CONVERT_TO_MPS = 3600000;

    // speed limitations
    final int MIN_SPEED = 0, MAX_SPEED = 120;

    // time to sleep before increasing/decreasing speed
    private final double ACCELERATION_MULTIPLIER = 1.0075;
    private final int    BASE_ACCELERATION_RATE  = 75;
    private final int    BASE_DECELERATION_RATE  = 500;
    private final int    BASE_BRAKING_RATE       = 10;

    private boolean started;
    private State   state;
    private int     speed;
    private double  distance;

    private Activity      context;
    private VehicleStatus updateStatus;

    SpeedManager(Activity context, VehicleStatus vehicleStatus)
    {
        this.context = context;
        updateStatus = vehicleStatus;

        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.filename), Context.MODE_PRIVATE);
        distance = sharedPreferences.getInt(context.getResources().getString(R.string.distance_key), 0);
    }

    void start()
    {
        if (!started)
        {
            started = true;
            state   = State.IDLE;
            speed   = MIN_SPEED;

            updateStatus.notifyDistanceChanged((int) distance);

            new Thread(new Runnable() { @Override public void run() { loop(); } }).start();
        }
    }

    void accelerate() { if (started) state = State.ACCELERATING; }
    void decelerate() { if (started) state = State.IDLE; }
    void brake()      { if (started) state = State.BRAKING; }

    void stop()
    {
        if (started)
        {
            SharedPreferences.Editor editor = context.getSharedPreferences(context.getResources().getString(R.string.filename), Context.MODE_PRIVATE).edit();
            editor.putInt(context.getResources().getString(R.string.distance_key), (int) distance);
            editor.apply();

            started = false;
        }
    }

    int   speed() { return speed; }
    State state() { return state; }

    private void loop()
    {
        // state can be affected by other threads, check still throttling
        while (started)
        {
            switch (state)
            {
                case IDLE:
                    reduceSpeed(BASE_DECELERATION_RATE);
                    break;

                case ACCELERATING:
                    // calculate time to sleep based on current speed and rate of acceleration
                    // higher speeds will sleep longer to simulate slowing climb
                    double sleepTime = BASE_ACCELERATION_RATE * Math.pow(ACCELERATION_MULTIPLIER, speed);
                    increaseSpeed((int) Math.ceil(sleepTime));
                    break;

                case BRAKING:
                    reduceSpeed(BASE_BRAKING_RATE);
                    break;
            }
        }
    }

    private void increaseSpeed(int sleepTime)
    {
        if (speed < MAX_SPEED)
        {
            speed += 1;
            updateStatus.notifySpeedChanged(speed);
        }

        // convert mph to mpms, account for number of ms slept
        double mps = (double)speed / CONVERT_TO_MPS;

        int distanceBefore = (int) distance;
        distance += mps * sleepTime;
        int distanceAfter  = (int) distance;

        if (distanceBefore < distanceAfter) updateStatus.notifyDistanceChanged(distanceAfter);

        try { Thread.sleep(sleepTime); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }

    private void reduceSpeed(int sleepTime)
    {
        if (speed > MIN_SPEED)
        {
            speed -= 1;
            updateStatus.notifySpeedChanged(speed);
        }

        // convert mph to mpms, account for number of ms slept
        double mps = (double)speed / CONVERT_TO_MPS;

        int distanceBefore = (int) distance;
        distance += mps * sleepTime;
        int distanceAfter  = (int) distance;

        if (distanceBefore < distanceAfter) updateStatus.notifyDistanceChanged(distanceAfter);

        try { Thread.sleep(sleepTime); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }
}
