package team_project.matt.vehicle_simulator;

// part of a 2 way interface
// activity that implements and receives requests should respond with these methods
// bluetoothLe functionality should implement this interface to find out the result of its requests
public interface BluetoothNotificationResponse
{
    void requestLocationResult(boolean isGranted);
    void enableAdapterResult(boolean isGranted);
}
