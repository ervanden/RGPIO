package rgpio;

import utils.JSONString;
import udputils.SendSetCommandThread;

public class VOutput extends VSelector {

    public String name;
    public String value;
    public IOType type;
    public Integer minMembers = null;


    public String toJSON() {
        JSONString json = new JSONString();
        json.addProperty("object", "VIO");
        json.addProperty("name", name);
        json.addProperty("value", value);
        json.addProperty("type", type.name());
        return json.asString();
    }

    public void set(String newValue) {

        value = newValue;
        RGPIO.updateFeed.writeToClients(toJSON());

        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (POutput p : device.outputs.values()) {
                if (p.voutput == this) {
                    p.set_value(newValue);
                    new SendSetCommandThread(device, "Set/" + IOType.longToShort(p.type) + ":" + p.name + "/Value:" + p.get_value()).start();

                }
            }
        }
    }

}
