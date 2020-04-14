package team_project.matt.vehicle_simulator;

// part of a 2 way interface
// bluetoothLE functionality calls these methods during setup process
// an activity class should implement this interface to handle these requests
public interface BluetoothNotification
{
    void requestLocation();
    void enableAdapter();

    void setupFailed(String message);
    void setupComplete();
}
