package team_project.matt.vehicle_simulator;

public interface VehicleStatus
{
    void notifyChargingStateChanged(boolean isCharging);
    void notifyBatteryLevelChanged(double batteryLevel);
    void notifyBatteryTemperatureChanged(double temperature);
    void notifySpeedChanged(int speed);
    void notifyDistanceChanged(int distance);
}
