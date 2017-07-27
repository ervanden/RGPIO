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
        String displayValue = value;
        if (value == null) {
            displayValue = "null";
        }
        if (device.get_status() == PDeviceStatus.NOTRESPONDING) {
            displayValue = "NOTRESPONDING";
        }
        json.addProperty("value", displayValue);
        return json.asString();
    }

}
