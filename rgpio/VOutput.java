package rgpio;

import utils.JSONObject;
import udputils.SendSetCommandThread;

public class VOutput extends Selector {

    public String name;
    public String value;
    public IOType type;
    public Integer minMembers = null;

    public VOutput(String name) {
        this.name = name;
        RGPIO.digitalOutputMap.put(name, this);
    }

    public String toJSON() {
        JSONObject json = new JSONObject();
        json.addProperty("object", "VIO");
        json.addProperty("name", name);
        json.addProperty("value", value);
        json.addProperty("type", type.name());
        return json.asString();
    }

    public void set(String newValue) {

        value = newValue;
        RGPIO.updateFeed.writeToClients(toJSON());

        for (PDevice device : RGPIO.deviceMap.values()) {
            for (POutput dop : device.digitalOutputs.values()) {
                if (dop.voutput == this) {
                    dop.value = newValue;   // store the state also with the physical dop. (why?)
                    RGPIO.updateFeed.writeToClients(dop.toJSON());
                    new SendSetCommandThread(device, "Set/Dop:" + dop.name + "/Value:" + dop.value).start();

                }
            }
        }
    }

    public String get() {
        return value;
    }

}
