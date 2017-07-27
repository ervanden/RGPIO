package rgpio;

import utils.JSONObject;

public class PInput {

    public String name;
    public String value;

    public PDevice device;
    public IOType type;
    public VInput vinput;

    public void set_value(String newValue) {
        value = newValue;
        RGPIO.updateFeed.writeToClients(toJSON());
        vinput.pinValueChange();
    }
    
    public String get_value(){
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

    public void setDebounced(String value) {

        if (type == IOType.digitalInput) {
            if (value.equals("High")) {
            }
            if (value.equals("Low")) {
            }
        } else if (type == IOType.analogInput) {

        } else if (type == IOType.stringInput) {

        }
        set_value(value);
    }

}
