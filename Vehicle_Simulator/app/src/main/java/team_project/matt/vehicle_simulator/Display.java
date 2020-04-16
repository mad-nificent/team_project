package team_project.matt.vehicle_simulator;

// generic display interface for an activity to handle UI updates from
public interface Display
{
    void showToast(String message, int length);
    void updateDeviceCount(int noDevices);

    void vehicleStarted();

    void chargeMode(boolean isCharging);
    void updateChargeLevel(int charge);
    void updateRange(int milesLeft);
    void updateSpeed(int speed);
}
