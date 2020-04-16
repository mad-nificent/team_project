package team_project.matt.vehicle_simulator;

public interface VehicleStatus
{
    void reportChargingState(boolean isCharging);
    void reportBatteryLevel(int level);
    void reportSpeed(int speed);
}
