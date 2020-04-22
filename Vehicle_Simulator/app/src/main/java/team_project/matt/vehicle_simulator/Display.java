package team_project.matt.vehicle_simulator;

// generic display interface for an activity to handle UI updates from
public interface Display
{
    void showToast(String message, int length);
    void updateDeviceCount(int noDevices);

    void vehicleStarted();

    void updateBatteryLevel(int charge);
    void updateSpeed(int speed);
    void updateDistance(int distance);
    void updateRange(int milesLeft);

    void toggleWarning(int state);
    void toggleSeatbelt(int state);
    void toggleLights(int state);
    void toggleTyrePressure(int state);
    void toggleWiperFluid(int state);
    void toggleAirbag(int state);
    void toggleBrakeFault(int state);
    void toggleABSFault(int state);
    void toggleEVFault(int state);

    void updateBatteryTemperature(int temperature);

    void toggleParkingBrake(int state);
    void toggleIndicator(int state);

    void updateChargeMode(boolean isCharging);
}
