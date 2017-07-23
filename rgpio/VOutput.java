package rgpio;

import utils.JSONObject;
import udputils.SendSetCommandThread;

public class VOutput extends VSelector {

    public String name;
    public String value;
    public IOType type;
    public Integer minMembers = null;

    public VOutput(String name) {
        this.name = name;
        RGPIO.VDigitalOutputMap.put(name, this);
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

        for (PDevice device : RGPIO.PDeviceMap.values()) {
            if (type == IOType.digitalOutput) {
                for (POutput dop : device.digitalOutputs.values()) {
                    if (dop.voutput == this) {
                        dop.value = newValue;   // store the output value also with the physical dop. (why?)
                        RGPIO.updateFeed.writeToClients(dop.toJSON());
                        new SendSetCommandThread(device, "Set/Dop:" + dop.name + "/Value:" + dop.value).start();

                    }
                }
            }
            if (type == IOType.analogOutput) {
                for (POutput aop : device.analogOutputs.values()) {
                    if (aop.voutput == this) {
                        aop.value = newValue;   // store the output value also with the physical dop. (why?)
                        RGPIO.updateFeed.writeToClients(aop.toJSON());
                        new SendSetCommandThread(device, "Set/Aop:" + aop.name + "/Value:" + aop.value).start();

                    }
                }
            }
            if (type == IOType.analogOutput) {
                for (POutput sop : device.stringOutputs.values()) {
                    if (sop.voutput == this) {
                        sop.value = newValue;   // store the output value also with the physical dop. (why?)
                        RGPIO.updateFeed.writeToClients(sop.toJSON());
                        new SendSetCommandThread(device, "Set/Sop:" + sop.name + "/Value:" + sop.value).start();

                    }
                }
            }
        }
    }

    public String get() {
        return value;
    }

}
