package rgpio;

import utils.JSONString;

public class PInput {

    public String name;
    public String value;
    public long event_received; // see SendGetCommandThread()
    public PDevice device;
    public IOType type;
    public VInput vinput;

    public void setValue(String newValue) {
        value = newValue;
        RGPIO.webSocketServer.sendToAll(toJSON());
        if (vinput!=null) vinput.pinValueChange();
    }

    public String getValue() {
        return value;
    }

    public String toJSON() {
        JSONString json = new JSONString();
        json.addProperty("object", "PIO");
        json.addProperty("name", name);
        json.addProperty("type", type.name());
        json.addProperty("device", device.HWid);
        
        String displayValue;
        if (device.get_status() == PDeviceStatus.NOTRESPONDING) {
            displayValue = "NOTRESPONDING";
        } else {
            if (value == null) {
                displayValue = "null";
            } else {
                displayValue = value;
            }
        }
        json.addProperty("value", displayValue);
        return json.asString();
    }

    public void setDebounced(String value) {
        setValue(value);
    }

}
