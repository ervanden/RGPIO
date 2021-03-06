package rgpio;

import utils.JSONString;

public class POutput {

    public String name;
    private String value;

    public long event_received=0; // see SendSetCommandThread()

    public PDevice device;
    public IOType type;
    public VOutput voutput;

    public void set_value(String newValue) {
        value = newValue;
        RGPIO.webSocketServer.sendToAll(toJSON());
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

}
