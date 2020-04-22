package team_project.matt.vehicle_simulator;

public interface VehicleStatus
{
    void      notifyChargingStateChanged(boolean isCharging);
    void       notifyBatteryLevelChanged(int batteryLevel);
    void notifyBatteryTemperatureChanged(int temperature);
    void              notifySpeedChanged(int speed);
    void           notifyDistanceChanged(int distance);
}
