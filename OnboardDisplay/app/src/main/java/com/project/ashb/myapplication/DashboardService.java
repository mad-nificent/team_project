package com.project.ashb.myapplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DashboardService {

    // initial values for the characteristics read
    public String battery_level = "0";
    public String speed_level = "0";
    public String indicator = "0";
    public String lights = "0";
    public String distance = "0";

    // UUID's
    String SERVICE_UUID = "dee0e505-9680-430e-a4c4-a225905ce33d"; // unique service ID
    String DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";    // descriptor ID
    // positions in characteristics list
    final int BATTERY_POSITION = 0;
    final int SPEED_POSITION = 1;
    final int INDICATOR_POSITION = 2;
    final int LIGHTS_POSITION = 3;
    final int DISTANCE_POSITION = 4;

    // Characteristic List
    List<String> characteristics_UUIDs = new ArrayList<String>(Arrays.asList(
            "76a247fb-a76f-42da-91ce-d6a5bdebd0e2",     // battery
            "7b9b53ff-5421-4bdf-beb0-ca8c949542c1",     // speed
            "74df0c8f-f3e1-4cf5-b875-56d7ca609a2e",     // indicator
            "131223c4-1e5f-486a-9ab5-d85c41984f6f",     // lights
            "5bebe839-c2e2-4fad-bb18-65f792ddb16f"      // distance
    ));
}
