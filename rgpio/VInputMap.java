package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageEvent;
import java.util.HashMap;

public class VInputMap extends HashMap<String, VInput> {

    public VInputMap() {
        super();
    }

    public VInput add(String name) {
        VInput digitalInput = RGPIO.VDigitalInputMap.get(name);
        if (digitalInput == null) {
            digitalInput = new VInput(name);
            RGPIO.VDigitalInputMap.put(name, digitalInput);
            digitalInput.type = IOType.digitalInput;
        };
        return digitalInput;
    }

    public void print() {
        String formatString = "%12s %1s\n";
        System.out.printf(formatString, "DigitalInput", "device.pin");
        for (VInput d : this.values()) {
            String name = d.name;

            String state = d.get_value();

            String separator = "";
            String devicePins = "<";
            for (PDevice device : RGPIO.PDeviceMap.values()) {
                for (PInput pin : device.digitalInputs.values()) {
                    if (pin.vinput == d) {
                        devicePins = devicePins + separator + device.HWid + "." + pin.name;
                        separator = ",";
                    }
                }
            }
            devicePins = devicePins + ">";
            System.out.printf(formatString, name, devicePins);
        }
    }

    public void digitalInputReported(
            String pinName,
            String HWid,
            String modelName) {

        PDevice d = RGPIO.PDeviceMap.get(HWid);

        // add the pin to the device
        PInput p = d.digitalInputs.get(pinName);
        if (p == null) {
            p = new PInput();
            p.type = IOType.digitalInput;
            p.name = pinName;
            p.value = null;
            p.device = d;
            p.vinput = null;
            d.digitalInputs.put(pinName, p);

            // match to a vinput
            int nrInstances = 0;
            VInput theOnlyDigitalInput = null;
            for (VInput digitalInput : RGPIO.VDigitalInputMap.values()) {
                if (digitalInput.matchesDevicePin(pinName, modelName, HWid)) {
                    theOnlyDigitalInput = digitalInput;

                    nrInstances++;
//                    System.out.println(" pin match : " + pinName + " " + modelName + " " + nrInstances);
                }
            }
            if (nrInstances == 1) {
                p.vinput = theOnlyDigitalInput;

                MessageEvent e = new MessageEvent(MessageType.Info);
                e.description = "device pin assigned to digital input";
                e.HWid = HWid;
                e.pinLabel = pinName;
                e.vinput = p.vinput.name;
                RGPIO.message(e);
            }
            if (nrInstances == 0) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "device pin can not be assigned to digital input";
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
            if (nrInstances > 1) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "device pin matches more than one digital input";
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
            RGPIO.updateFeed.writeToClients(p.toJSON());
        }
    }

}
