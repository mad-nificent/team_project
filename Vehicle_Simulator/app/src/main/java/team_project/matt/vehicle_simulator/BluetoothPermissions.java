package team_project.matt.vehicle_simulator;

// used by BluetoothLE class to request permissions
// interface must be implemented by current activity
public interface BluetoothPermissions
{
    void requestLocation();
    void enableAdapter();
    void setupFailed(String error);
    void setupComplete();
}
