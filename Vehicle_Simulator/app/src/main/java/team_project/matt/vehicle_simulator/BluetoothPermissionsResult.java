package team_project.matt.vehicle_simulator;

// used by current activity to return permission result
// must be implemented by BluetoothLE class
public interface BluetoothPermissionsResult
{
    void requestLocationResult(boolean isGranted);
    void enableAdapterResult(boolean isGranted);
}
