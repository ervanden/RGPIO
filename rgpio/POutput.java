package rgpio;

import utils.JSONObject;

public class POutput {

    public String name;
    private String value;

    public PDevice device;
    public IOType type;
    public VOutput voutput;

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
        if (device.get_status() == PDeviceStatus.ACTIVE) {
            json.addProperty("value", value);
        } else {
            json.addProperty("value", "NOTRESPONDING");
        }
        return json.asString();
    }

}
