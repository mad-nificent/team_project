package com.project.ashb.myapplication;

import java.util.HashMap;
import java.util.Map;

public class DashboardService {

    /* ------ CHARACTERISTIC VALUES ------ */
    public int battery_charge        = 0;
    public int battery_range         = 0;
    public int battery_charge_status = 0;
    public int battery_temp          = 0;

    public int speed                 = 0;
    public int distance_traveled     = 0;
    public int turn_signal           = 0;
    public int lights                = 0;
    public int parking_brake         = 0;

    public int master_warning        = 0;
    public int seat_belt             = 0;
    public int lights_fault          = 0;
    public int low_wiper_fluid       = 0;
    public int low_tire_pressure     = 0;
    public int air_bags              = 0;
    public int brake_system          = 0;
    public int abs                   = 0;
    public int electric_drive_system = 0;

    /* ------ BATTERY ------ */
    final int BATTERY_CHARGE = 0, BATTERY_RANGE = 1, BATTERY_CHARGE_STATUS = 2, BATTERY_TEMP = 3;
    /* ------ CAR STATE ------ */
    final int SPEED  = 4, DISTANCE_TRAVELED = 5, TURN_SIGNAL  = 6, LIGHTS = 7, PARKING_BREAK = 8;
    /* ------ WARNINGS ------ */
    final int MASTER_WARNING = 9, SEAT_BELT = 10, LIGHTS_FAULT = 11, LOW_WIPER_FLUID = 12, LOW_TIRE_PRESSURE  = 13, AIR_BAGS = 14, BRAKE_SYSTEM = 15, ABS = 16, ELECTRIC_DRIVE_SYSTEM = 17;

    String SERVICE_UUID = "dee0e505-9680-430e-a4c4-a225905ce33d";

    final Map<Integer, String> characteristics = new HashMap<Integer, String>() {{
        put(BATTERY_CHARGE,         "76a247fb-a76f-42da-91ce-d6a5bdebd0e2");    // battery charge
        put(BATTERY_RANGE,          "bf252fd6-c1e3-4835-b4be-b5e353e62d7b");    // battery range
        put(BATTERY_CHARGE_STATUS,  "ed88d679-5aba-4fda-a710-42156bc85524");    // battery charge status
        put(BATTERY_TEMP,           "4751b324-3935-4b1e-a4e7-9c0888d03325");    // battery temp
        put(SPEED,                  "7b9b53ff-5421-4bdf-beb0-ca8c949542c1");    // speed
        put(DISTANCE_TRAVELED,      "5bebe839-c2e2-4fad-bb18-65f792ddb16f");    // distance traveled
        put(TURN_SIGNAL,            "74df0c8f-f3e1-4cf5-b875-56d7ca609a2e");    // turn signal
        put(LIGHTS,                 "131223c4-1e5f-486a-9ab5-d85c41984f6f");    // lights
        put(PARKING_BREAK,          "f05976f6-aa9e-4d19-a255-aeda7dbb624f");    // parking brake enabled
        put(MASTER_WARNING,         "9d5a5763-38a0-4eb6-83b6-0a5da1270266");    // master warning
        put(SEAT_BELT,              "da2d9231-ae69-4b5b-b4dc-d7c940e72815");    // seat belt
        put(LIGHTS_FAULT,           "ae86ebf4-b0ef-42ff-9dd2-0f15fb441b2f");    // lights fault
        put(LOW_WIPER_FLUID,        "0b458d6d-4e0c-442d-9c18-febd81281d78");    // low wiper fluid
        put(LOW_TIRE_PRESSURE,      "ce99220c-75ed-4d38-9b25-5a0f7e766016");    // low tire pressure
        put(AIR_BAGS,               "8be2a5a1-5e2e-4344-9304-fb642b55746e");    // air bags
        put(BRAKE_SYSTEM,           "b45ea8a5-7d70-41bc-82a3-51cd35d594cc");    // brake system
        put(ABS,                    "ce3843db-ef55-4b8d-a02a-e0eb4963a1e0");    // abs
        put(ELECTRIC_DRIVE_SYSTEM,  "d93f1c6c-6e14-44b3-95ce-d0a7f71efbb5");    // electric drive system
    }};

}
