package team_project.matt.vehicle_simulator;

// utility class for BatteryManager - set constants at runtime
class BatteryConstants
{
    static private final int NOT_SET = -1;

    static private int    SLEEP_TIME         = NOT_SET;
    static private double CHARGING_POWER     = NOT_SET;
    static private double MIN_POWER          = NOT_SET;
    static private double MAX_POWER          = NOT_SET;
    static private int    MIN_DRAIN_SPEED_MS = NOT_SET;
    static private int    MAX_DRAIN_SPEED_MS = NOT_SET;

    // do not allow overwrites once set
    static void setSleepTime(int sleepTime)             { if (SLEEP_TIME == NOT_SET) SLEEP_TIME = sleepTime; }
    static void setChargingPower(double chargingPower)  { if (CHARGING_POWER == NOT_SET) CHARGING_POWER = chargingPower; }
    static void setMaxPower(double maxPower)            { if (MAX_POWER == NOT_SET) MAX_POWER = maxPower; }
    static void setMinPower(double minPower)            { if (MIN_POWER == NOT_SET) MIN_POWER = minPower; }
    static void setMaxDrainSpeedMs(int maxDrainSpeedMs) { if (MAX_DRAIN_SPEED_MS == NOT_SET) MAX_DRAIN_SPEED_MS = maxDrainSpeedMs; }
    static void setMinDrainSpeedMs(int minDrainSpeedMs) { if (MIN_DRAIN_SPEED_MS == NOT_SET) MIN_DRAIN_SPEED_MS = minDrainSpeedMs; }

    static int    getSleepTime()       { return SLEEP_TIME; }
    static double getChargingPower()   { return CHARGING_POWER; }
    static double getMaxPower()        { return MAX_POWER; }
    static double getMinPower()        { return MIN_POWER; }
    static int    getMaxDrainSpeedMs() { return MAX_DRAIN_SPEED_MS; }
    static int    getMinDrainSpeedMs() { return MIN_DRAIN_SPEED_MS; }
}
