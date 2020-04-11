package team_project.matt.vehicle_simulator;

public interface BluetoothStatus
{
    void serviceAddedResult(boolean added);
    void advertiseResult(boolean started);
    void GATTResult(boolean started);
}
