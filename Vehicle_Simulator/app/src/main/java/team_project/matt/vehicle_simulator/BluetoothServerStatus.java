package team_project.matt.vehicle_simulator;

// used by Bluetooth LE to report GAP and GATT status (failed or complete)
// class that calls BLE setup functions should implement this interface (Vehicle Service in this case)
public interface BluetoothServerStatus
{
    void advertiseResult(boolean started);
    void serviceAddedResult(boolean added);
    void GATTResult(boolean started);
}
