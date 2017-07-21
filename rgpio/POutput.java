package rgpio;

import rgpio.PDevice;
import utils.JSONObject;
import rgpio.*;

public class POutput {

    public String name;
    public String value;

    public PDevice device;
        public IOType type;
    public VOutput voutput;

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
