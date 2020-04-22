package team_project.matt.vehicle_simulator;

public interface BluetoothPermissionsResult
{
    void requestLocationResult(boolean isGranted);
    void   enableAdapterResult(boolean isGranted);
}
