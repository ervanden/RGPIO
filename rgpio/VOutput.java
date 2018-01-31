package rgpio;

import utils.JSONString;
import udputils.SendSetCommandThread;

public class VOutput extends VIO {

    public String value;
    public Integer members = 0;
    public Integer minMembers = null;

    public String toJSON() {
        JSONString json = new JSONString();
        json.addProperty("object", "VIO");
        json.addProperty("name", name);
        json.addProperty("members", members.toString());
        json.addProperty("value", value);
        json.addProperty("type", type.name());
        return json.asString();
    }

    public void set(String newValue) {

        value = newValue;
        RGPIO.webSocketServer.sendToAll(toJSON());

        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (POutput p : device.outputs.values()) {
                if (p.voutput == this) {
                    p.set_value(newValue);
                    new SendSetCommandThread(device, p).start();

                }
            }
        }
    }

    public void countMembers() {
        int m = 0;
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (POutput p : device.outputs.values()) {
                if (p.voutput == this) {
                    System.out.println("  device " + device.HWid + " pin " + p.name + " " + device.get_status().toString());
                    if (device.get_status() == PDeviceStatus.ACTIVE) {
                        m++;
                    }
                }
            }
        }
        if (members != m) {
            members = m;
            RGPIO.webSocketServer.sendToAll(toJSON());
        }
    }

}
