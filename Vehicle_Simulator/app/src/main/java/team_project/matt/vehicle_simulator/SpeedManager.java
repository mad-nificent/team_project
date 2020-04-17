package team_project.matt.vehicle_simulator;

class SpeedManager
{
    // states of throttle
    enum State { IDLE, ACCELERATING, BRAKING };

    // speed limitations
    final int MIN_SPEED = 0, MAX_SPEED = 120;

    // time to sleep before increasing/decreasing speed
    private final double ACCELERATION_MULTIPLIER = 1.0075;
    private final int    BASE_ACCELERATION_RATE  = 75;
    private final int    BASE_DECELERATION_RATE  = 500;
    private final int    BASE_BRAKING_RATE       = 10;

    private boolean started = false;
    private State   state   = State.IDLE;
    private int     speed   = MIN_SPEED;

    private VehicleStatus updateStatus;

    SpeedManager(VehicleStatus vehicleStatus) { updateStatus = vehicleStatus; }

    void start()
    {
        if (!started)
        {
            started = true;
            state = State.IDLE;
            new Thread(new Runnable() { @Override public void run() { loop(); } }).start();
        }
    }

    void accelerate() { state = State.ACCELERATING; }
    void decelerate() { state = State.IDLE; }
    void brake()      { state = State.BRAKING; }
    void stop()       { started = false; }

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

        try { Thread.sleep(sleepTime); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }
}
