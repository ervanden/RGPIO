package rgpio;

import utils.JSONString;

public class PInput {

    public String name;
    public String value;

    public PDevice device;
    public IOType type;
    public VInput vinput;

    public void set_value(String newValue) {
        value = newValue;
        RGPIO.webSocketServer.sendToAll(toJSON());
        vinput.pinValueChange();
    }

    public String get_value() {
        return value;
    }

    public String toJSON() {
        JSONString json = new JSONString();
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
