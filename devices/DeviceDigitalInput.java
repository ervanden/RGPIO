package devices;

import rgpio.*;

public class DeviceDigitalInput {

    public String name;
    public String value;

    public Device device;
    public RGPIOInput digitalInput;

    public void set_value(String newValue) {
        value = newValue;
        RGPIO.updateFeed.writeToClients(toJSON());
    }

    public String get_value() {
        return value;
    }

    public String toJSON() {
        JSONString json = new JSONString();
        json.addString("object", "PIO");
        json.addString("name", name);
        json.addString("type", "digitalInput");
        json.addString("device", device.HWid);
        json.addString("value", value);
        return json.close();
    }

}
