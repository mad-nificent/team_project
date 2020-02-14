package com.project.ashb.myapplication;

import java.util.HashMap;
import java.util.Map;

public class DashboardService {

    /* ------ CHARACTERISTIC VALUES ------ */
    public int battery_charge        = 0;
    public String battery_range         = "0";
    public String battery_charge_status = "0";
    public String battery_temp          = "0";

    public int speed                 = 0;
    public int distance_traveled     = 0;
    public int turn_signal           = 0;
    public int lights                = 0;
    public String parking_break         = "0";

    public String master_warning        = "0";
    public String seat_belt             = "0";
    public String lights_fault          = "0";
    public String low_wiper_fluid       = "0";
    public String low_tire_pressure     = "0";
    public String air_bags              = "0";
    public String abs                   = "0";
    public String motor                 = "0";

    /* ------ CHARACTERISTIC POSITIONS ------ */
    final int BATTERY_CHARGE        = 0;
    final int BATTERY_RANGE         = 1;
    final int BATTERY_CHARGE_STATUS = 2;
    final int BATTERY_TEMP          = 3;

    final int SPEED                 = 4;
    final int DISTANCE_TRAVELED     = 5;
    final int TURN_SIGNAL           = 6;
    final int LIGHTS                = 7;
    final int PARKING_BREAK         = 8;

    final int MASTER_WARNING        = 9;
    final int SEAT_BELT             = 10;
    final int LIGHTS_FAULT          = 11;
    final int LOW_WIPER_FLUID       = 12;
    final int LOW_TIRE_PRESSURE     = 13;
    final int AIR_BAGS              = 14;
    final int BRAKE_SYSTEM          = 15;
    final int ABS                   = 16;
    final int MOTOR                 = 17;

    String SERVICE_UUID = "dee0e505-9680-430e-a4c4-a225905ce33d";

    Map<Integer, String> characteristics = new HashMap<Integer, String>();

    DashboardService() {
        characteristics.put(BATTERY_CHARGE,         "76a247fb-a76f-42da-91ce-d6a5bdebd0e2");
        characteristics.put(BATTERY_RANGE,          "bf252fd6-c1e3-4835-b4be-b5e353e62d7b");    // battery range (mileage)
        characteristics.put(BATTERY_CHARGE_STATUS,  "ed88d679-5aba-4fda-a710-42156bc85524");    // battery charging status
        characteristics.put(BATTERY_TEMP,           "4751b324-3935-4b1e-a4e7-9c0888d03325");    // battery temp
        /* ------ CAR STATE ------ */
        characteristics.put(SPEED,                  "7b9b53ff-5421-4bdf-beb0-ca8c949542c1");    // speed
        characteristics.put(DISTANCE_TRAVELED,      "5bebe839-c2e2-4fad-bb18-65f792ddb16f");    // distance traveled
        characteristics.put(TURN_SIGNAL,            "74df0c8f-f3e1-4cf5-b875-56d7ca609a2e");    // turn signal
        characteristics.put(LIGHTS,                 "131223c4-1e5f-486a-9ab5-d85c41984f6f");    // lights
        characteristics.put(PARKING_BREAK,          "f05976f6-aa9e-4d19-a255-aeda7dbb624f");    // parking break
        /* ------ WARNINGS ------ */
        characteristics.put(MASTER_WARNING,         "9d5a5763-38a0-4eb6-83b6-0a5da1270266");    // master warning
        characteristics.put(SEAT_BELT,              "da2d9231-ae69-4b5b-b4dc-d7c940e72815");    // seat belt
        characteristics.put(LIGHTS_FAULT,           "ae86ebf4-b0ef-42ff-9dd2-0f15fb441b2f");    // lights fault
        characteristics.put(LOW_WIPER_FLUID,        "0b458d6d-4e0c-442d-9c18-febd81281d78");    // low wiper fluid
        characteristics.put(LOW_TIRE_PRESSURE,      "ce99220c-75ed-4d38-9b25-5a0f7e766016");    // low tire pressure
        characteristics.put(AIR_BAGS,               "8be2a5a1-5e2e-4344-9304-fb642b55746e");    // air bags
        characteristics.put(BRAKE_SYSTEM,           "b45ea8a5-7d70-41bc-82a3-51cd35d594cc");    // brake system
        characteristics.put(ABS,                    "ce3843db-ef55-4b8d-a02a-e0eb4963a1e0");    // abs
        characteristics.put(MOTOR,                  "d93f1c6c-6e14-44b3-95ce-d0a7f71efbb5");    // motor

    }
}
