package team_project.matt.vehicle_simulator;

public interface SendUserResponse
{
    void locationPermissionResult(boolean isGranted);
    void adapterStatus(boolean isGranted);
}
