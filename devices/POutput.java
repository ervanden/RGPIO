package devices;

import rgpio.*;

public class POutput {

    public String name;
    public String value;

    public Device device;
        public RGPIOIOType type;
    public RGPIOOutput voutput;

        public String toJSON() {
        JSONString json = new JSONString();
        json.addString("object", "PIO");
        json.addString("name", name);
        json.addString("type", type.name());
        json.addString("device", device.HWid);
        json.addString("value", value);
        return json.close();
    }
}
