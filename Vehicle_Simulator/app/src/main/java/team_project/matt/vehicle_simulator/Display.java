package team_project.matt.vehicle_simulator;

// generic display interface for an activity to handle UI updates from
public interface Display
{
    void showToast(String message, int length);
    void updateDeviceCount(int noDevices);

    void vehicleStarted();

    void updateChargeMode(boolean isCharging);
    void updateBatteryLevel(int charge);
    void updateBatteryTemperature(int temperature);
    void updateRange(int milesLeft);
    void updateSpeed(int speed);
    void updateDistance(int distance);
}
