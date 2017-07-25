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
                        dop.set_value(newValue);
                        RGPIO.updateFeed.writeToClients(dop.toJSON());
                        new SendSetCommandThread(device, "Set/Dop:" + dop.name + "/Value:" + dop.get_value()).start();

                    }
                }
            }
            if (type == IOType.analogOutput) {
                for (POutput aop : device.analogOutputs.values()) {
                    if (aop.voutput == this) {
                        aop.set_value(newValue);
                        RGPIO.updateFeed.writeToClients(aop.toJSON());
                        new SendSetCommandThread(device, "Set/Aop:" + aop.name + "/Value:" + aop.get_value()).start();

                    }
                }
            }
            if (type == IOType.analogOutput) {
                for (POutput sop : device.stringOutputs.values()) {
                    if (sop.voutput == this) {
                        sop.set_value(newValue);
                        RGPIO.updateFeed.writeToClients(sop.toJSON());
                        new SendSetCommandThread(device, "Set/Sop:" + sop.name + "/Value:" + sop.get_value()).start();

                    }
                }
            }
        }
    }

    public String get() {
        return value;
    }

}
