package team_project.matt.vehicle_simulator;

// used by non-activity classes to report changes that need to be drawn to the UI...
// ...or to update current progress of overall setup
public interface VehicleDashboard
{
    void showToast(String message, int length);
    void vehicleReady();
    void updateDeviceCount(int noDevices);
    void updateBatteryLevel(int newBatteryLevel);
    void updateSpeed(int newSpeed);
    void updateRange(int newRange);
    void updateDistance(int newDistance);
    void toggleSeatbelt(int newState);
    void toggleLights(int newState);
    void toggleTyrePressure(int newState);
    void toggleWiperFluid(int newState);
    void toggleAirbag(int newState);
    void toggleBrakeFault(int newState);
    void toggleABSFault(int newState);
    void toggleEVFault(int newState);
    void updateBatteryTemperature(int newTemperature);
    void toggleParkingBrake(int newState);
    void toggleChargeMode(boolean isCharging);
    void toggleTurnSignal(int newState);
}
