package team_project.matt.vehicle_simulator;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.ArrayList;;

public class Vehicle
{
    BluetoothLE bluetoothLE;
    
    private final String UUID = "dee0e505-9680-430e-a4c4-a225905ce33d";
    
    enum Property
    {
        // battery data
        BATTERY_LVL, CHARGING, RANGE, BATTERY_TEMP,
        
        // car data
        SPEED, RPM, DISTANCE, TURN_SIGNAL, LIGHTS, HANDBRAKE,
        
        // warnings
        WARNING, SEATBELT, LIGHTS_ERR, WIPER_LOW, TYPE_PRESSURE_LOW, AIRBAG_ERR, BRAKE_ERR, ABS_ERR, ENGIN_ERR
    }
    
    // property states
    public final int         STATE_OFF = 0,           STATE_ON = 1;
    public final int STATE_SIGNAL_LEFT = 1, STATE_SIGNAL_RIGHT = 2;
    public final int  STATE_LIGHTS_LOW = 1,  STATE_LIGTHS_HIGH = 2;
    public final int STATE_WARNING_LOW = 1, STATE_WARNING_HIGH = 2;
    
    public class Characteristic
    {
        // supported types
        public static final int FORMAT_NUMBER = 0,  // regular numerical value
                                 FORMAT_STATE = 1;  // finite number of states (on, off etc.)
        
        private String             UUID;
        private Property           property;
        private int                format;
        private ArrayList<Integer> supportedValues;
        
        private int data;
        
        Characteristic(String UUID, Property property, int format)
        {
            this.UUID     = UUID;
            this.property = property;
            this.format   = format;
            
            this.supportedValues = new ArrayList<>();
        }
        
        public void addSupportedValue(int supportedValue)
        {
            if (format == FORMAT_STATE) supportedValues.add(supportedValue);
        }
        
        public void removeSupportedValue(int index)
        {
            if (index < supportedValues.size())
                supportedValues.remove(index);
        }
        
        public void setData(int data)
        {
            if (format == FORMAT_NUMBER) this.data = data;
            
            else if (format == FORMAT_STATE)
            {
                for (int value : supportedValues)
                    if (data == value)
                        this.data = data;
            }
            
            // update characteristic on service and notify change
            BluetoothGattCharacteristic characteristic = bluetoothLE.service.getCharacteristic(java.util.UUID.fromString(this.UUID));
            
            characteristic.setValue(Integer.toString(data));
            
            for (int i = 0; i < bluetoothLE.devices.size(); ++i)
                bluetoothLE.GATTServer.notifyCharacteristicChanged(bluetoothLE.devices.get(i), characteristic, false);
        }
        
        public String                        getUUID() { return UUID; }
        public Property                  getProperty() { return property; }
        public int                         getFormat() { return format; }
        public ArrayList<Integer> getSupportedValues() { return supportedValues; }
        public int                           getData() { return data; }
    }
    
    private ArrayList<Characteristic> characteristics = new ArrayList<>();
    
    Vehicle(BluetoothLE bluetoothLE)
    {
        this.bluetoothLE = bluetoothLE;
        
        // battery characteristics
        //------------------------------------------------------------
        Characteristic newCharacteristic = new Characteristic
                ("76a247fb-a76f-42da-91ce-d6a5bdebd0e2",
                        Property.BATTERY_LVL,
                        Characteristic.FORMAT_NUMBER);
        
        characteristics.add(newCharacteristic);
    
        newCharacteristic = new Characteristic
                ("ed88d679-5aba-4fda-a710-42156bc85524",
                        Property.CHARGING,
                        Characteristic.FORMAT_STATE);
        
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);
        
        characteristics.add(newCharacteristic);
    
        newCharacteristic =new Characteristic
                ("bf252fd6-c1e3-4835-b4be-b5e353e62d7b",
                        Property.RANGE,
                        Characteristic.FORMAT_NUMBER);
        
        characteristics.add(newCharacteristic);
        
        newCharacteristic = new Characteristic
                ("4751b324-3935-4b1e-a4e7-9c0888d03325",
                        Property.BATTERY_TEMP,
                        Characteristic.FORMAT_NUMBER);
    
        characteristics.add(newCharacteristic);
        //------------------------------------------------------------

        // car characteristics
        //------------------------------------------------------------
        newCharacteristic = new Characteristic
                ("7b9b53ff-5421-4bdf-beb0-ca8c949542c1",
                        Property.SPEED,
                        Characteristic.FORMAT_NUMBER);
        
        characteristics.add(newCharacteristic);
        
        newCharacteristic = new Characteristic
                ("0d6baf82-a79d-4660-a153-b72c6cbd63ee",
                        Property.RPM,
                        Characteristic.FORMAT_NUMBER);
        
        characteristics.add(newCharacteristic);
        
        newCharacteristic = new Characteristic
                ("5bebe839-c2e2-4fad-bb18-65f792ddb16f",
                        Property.DISTANCE,
                        Characteristic.FORMAT_NUMBER);
        
        characteristics.add(newCharacteristic);
        
        newCharacteristic = new Characteristic
                ("74df0c8f-f3e1-4cf5-b875-56d7ca609a2e",
                        Property.TURN_SIGNAL,
                        Characteristic.FORMAT_STATE);
        
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_SIGNAL_LEFT);
        newCharacteristic.addSupportedValue(STATE_SIGNAL_RIGHT);
        
        characteristics.add(newCharacteristic);
        
        newCharacteristic = new Characteristic
                ("131223c4-1e5f-486a-9ab5-d85c41984f6f",
                        Property.LIGHTS,
                        Characteristic.FORMAT_STATE);
    
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_LIGHTS_LOW);
        newCharacteristic.addSupportedValue(STATE_LIGTHS_HIGH);
        
        characteristics.add(newCharacteristic);
    
        newCharacteristic = new Characteristic
                ("f05976f6-aa9e-4d19-a255-aeda7dbb624f",
                        Property.HANDBRAKE,
                        Characteristic.FORMAT_STATE);
        
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);
        
        characteristics.add(newCharacteristic);
        //------------------------------------------------------------
        
        // warnings
        //------------------------------------------------------------
        newCharacteristic = new Characteristic
                ("9d5a5763-38a0-4eb6-83b6-0a5da1270266",
                        Property.WARNING,
                        Characteristic.FORMAT_STATE);
    
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);
        
        characteristics.add(newCharacteristic);
    
        newCharacteristic = new Characteristic
                ("da2d9231-ae69-4b5b-b4dc-d7c940e72815",
                        Property.SEATBELT,
                        Characteristic.FORMAT_STATE);
    
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);
        
        characteristics.add(newCharacteristic);
    
        newCharacteristic = new Characteristic
                ("ae86ebf4-b0ef-42ff-9dd2-0f15fb441b2f",
                        Property.LIGHTS_ERR,
                        Characteristic.FORMAT_STATE);
    
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);
        
        characteristics.add(newCharacteristic);
    
        newCharacteristic = new Characteristic
                ("0b458d6d-4e0c-442d-9c18-febd81281d78",
                        Property.WIPER_LOW,
                        Characteristic.FORMAT_STATE);
    
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);
    
        characteristics.add(newCharacteristic);
    
        newCharacteristic = new Characteristic
                ("ce99220c-75ed-4d38-9b25-5a0f7e766016",
                        Property.TYPE_PRESSURE_LOW,
                        Characteristic.FORMAT_STATE);
    
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);
    
        characteristics.add(newCharacteristic);
    
        newCharacteristic = new Characteristic
                ("8be2a5a1-5e2e-4344-9304-fb642b55746e",
                        Property.AIRBAG_ERR,
                        Characteristic.FORMAT_STATE);
    
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);
    
        characteristics.add(newCharacteristic);
    
        newCharacteristic = new Characteristic
                ("b45ea8a5-7d70-41bc-82a3-51cd35d594cc",
                        Property.BRAKE_ERR,
                        Characteristic.FORMAT_STATE);
    
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_WARNING_LOW);
        newCharacteristic.addSupportedValue(STATE_WARNING_HIGH);
    
        characteristics.add(newCharacteristic);
    
        newCharacteristic = new Characteristic
                ("ce3843db-ef55-4b8d-a02a-e0eb4963a1e0",
                        Property.ABS_ERR,
                        Characteristic.FORMAT_STATE);
    
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_WARNING_LOW);
        newCharacteristic.addSupportedValue(STATE_WARNING_HIGH);
    
        characteristics.add(newCharacteristic);
    
        newCharacteristic = new Characteristic
                ("d93f1c6c-6e14-44b3-95ce-d0a7f71efbb5",
                        Property.ENGIN_ERR,
                        Characteristic.FORMAT_STATE);
    
        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_WARNING_LOW);
        newCharacteristic.addSupportedValue(STATE_WARNING_HIGH);
    
        characteristics.add(newCharacteristic);
        //------------------------------------------------------------
    }
    
    public String getUUID() { return UUID; }
    
    public Characteristic getCharacteristic(Property property)
    {
        for (Characteristic characteristic : characteristics)
            if (property == characteristic.getProperty())
                return characteristic;
        
        return null;
    }
    
    public ArrayList<Characteristic> getCharacteristics() { return characteristics; }
    
    public ArrayList<String> getCharacteristicUUIDs()
    {
        ArrayList<String> UUIDs =  new ArrayList<>();
        
        for (Characteristic characteristic : characteristics)
            UUIDs.add(characteristic.getUUID());
        
        return UUIDs;
    }
    
    public ArrayList<Integer> getCharacteristicFormats()
    {
        ArrayList<Integer> formats =  new ArrayList<>();
        
        for (Characteristic characteristic : characteristics)
            formats.add(characteristic.getFormat());
        
        return formats;
    }
}
