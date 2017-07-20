package rgpio;

import udputils.SendSetCommandThread;
import devices.*;

public class RGPIOOutput extends RGPIOSelector {

    public String name;
    public RGPIOIOType type;
    public String value;
    public Integer minMembers = null;

    public RGPIOOutput(String name) {
        this.name = name;
        RGPIO.digitalOutputMap.put(name, this);
    }

    public String toJSON() {
        JSONString json = new JSONString();
        json.addString("object", "VIO");
        json.addString("name", name);
        json.addString("value", value);
        json.addString("type", type.name());
        return json.close();
    }

    public void set(String newValue) {

        value = newValue;
        RGPIO.updateFeed.writeToClients(toJSON());

        for (Device device : RGPIO.deviceMap.values()) {
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
