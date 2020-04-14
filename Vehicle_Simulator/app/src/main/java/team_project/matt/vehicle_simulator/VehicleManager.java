package team_project.matt.vehicle_simulator;

class VehicleManager
{
    private VehicleService vehicleService;
    private SpeedManager speedManager;

    VehicleManager(VehicleService vehicleService)
    {
        this.vehicleService = vehicleService;

        speedManager = new SpeedManager(vehicleService);
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

    void accelerate()
    {
        speedManager.accelerate();
    }

    void decelerate()
    {
        speedManager.decelerate();
    }

    void brake()
    {
        speedManager.brake();
    }
}
