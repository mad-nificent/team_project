package team_project.matt.vehicle_simulator;

// bluetoothLE functionality can use this interface to broadcast its status (i.e if GATT started)
// classes that want to communicate with bluetoothLE should implement this to retrieve relevant results
public interface BluetoothServerStatus
{
    void serviceAddedResult(boolean added);
    void    advertiseResult(boolean started);
    void         GATTResult(boolean started);
}
