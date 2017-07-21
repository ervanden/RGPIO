package rgpio;

import rgpio.PDevice;
import utils.JSONObject;
import rgpio.*;

public class PInput {

    public String name;
    public String value;

    public PDevice device;
    public IOType type;
    public VInput vinput;

    public void set_value(String newValue) {
        value = newValue;
        RGPIO.updateFeed.writeToClients(toJSON());
    }

    public String get_value() {
        return value;
    }

    public String toJSON() {
        JSONObject json = new JSONObject();
        json.addProperty("object", "PIO");
        json.addProperty("name", name);
        json.addProperty("type", type.name());
        json.addProperty("device", device.HWid);
        json.addProperty("value", value);
        return json.asString();
    }

}
