package team_project.matt.vehicle_simulator;

class SpeedManager
{
    // states of throttle
    enum State { IDLE, ACCELERATING, BRAKING };

    // speed limitations
    private final int MIN_SPEED = 0, MAX_SPEED = 120;

    // time to sleep before increasing/decreasing speed
    private final double ACCELERATION_MULTIPLIER = 1.0075;
    private final int    BASE_ACCELERATION_RATE  = 75;
    private final int    BASE_DECELERATION_RATE  = 500;
    private final int    BASE_BRAKING_RATE       = 10;

    private State state = State.IDLE;

    private VehicleService vehicleService;

    SpeedManager(VehicleService vehicleService)
    {
        this.vehicleService = vehicleService;
    }

    void accelerate()
    {
        if (state == State.IDLE)
        {
            // start
            state = State.ACCELERATING;

            // run in the background to not block UI
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    int speed = vehicleService.getCharacteristic(VehicleService.Property.SPEED).getData();

                    // state can be affected by other threads, check still throttling
                    while (state == State.ACCELERATING)
                    {
                        if (speed < MAX_SPEED)
                        {
                            speed += 1;
                            vehicleService.getCharacteristic(VehicleService.Property.SPEED).setData(speed);
                        }

                        try
                        {
                            // calculate time to sleep based on current speed and rate of acceleration
                            // higher speeds will sleep longer to simulate slowing climb
                            double sleepTime = BASE_ACCELERATION_RATE * Math.pow(ACCELERATION_MULTIPLIER, speed);
                            Thread.sleep((int)Math.ceil(sleepTime));
                        }

                        catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }

            }).start();
        }
    }

    void decelerate()
    {
        if (state != State.IDLE)
        {
            // start
            state = State.IDLE;

            // run in the background to not block UI
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    int speed = vehicleService.getCharacteristic(VehicleService.Property.SPEED).getData();

                    // state can be affected by other threads, check still idling
                    while (state == State.IDLE)
                    {
                        if (speed > MIN_SPEED)
                        {
                            speed -= 1;
                            vehicleService.getCharacteristic(VehicleService.Property.SPEED).setData(speed);
                        }

                        // slowly decelerate
                        try { Thread.sleep(BASE_DECELERATION_RATE); }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }

            }).start();
        }
    }

    void brake()
    {
        if (state != State.BRAKING)
        {
            // start
            state = State.BRAKING;

            // run in the background to not block UI
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    int speed = vehicleService.getCharacteristic(VehicleService.Property.SPEED).getData();

                    // state can be affected by other threads, check still braking
                    while (state == State.BRAKING)
                    {
                        if (speed > MIN_SPEED)
                        {
                            speed -= 1;
                            vehicleService.getCharacteristic(VehicleService.Property.SPEED).setData(speed);
                        }

                        // quickly decelerate
                        try { Thread.sleep(BASE_BRAKING_RATE); }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }

            }).start();
        }
    }
}
