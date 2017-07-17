package devices;

import rgpio.*;

public class DeviceDigitalOutput {

    public String name;
    public String value;

    public Device device;
    public RGPIOOutput digitalOutput;

        public String toJSON() {
        JSONString json = new JSONString();
        json.addString("object", "PIO");
        json.addString("name", name);
        json.addString("type", "digitalOutput");
        json.addString("device", device.HWid);
        json.addString("value", value);
        return json.close();
    }
}
