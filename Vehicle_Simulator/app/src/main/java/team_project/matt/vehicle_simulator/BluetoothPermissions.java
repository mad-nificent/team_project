package team_project.matt.vehicle_simulator;

public interface BluetoothPermissions
{
    void requestLocation();
    void enableAdapter();
    void setupFailed(String error);
    void setupComplete();
}
