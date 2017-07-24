package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageEvent;
import java.util.HashMap;

public class VInputMap extends HashMap<String, VInput> {

    IOType type;

    public VInputMap(IOType type) {
        super();
        this.type = type;
    }

    public VInput add(String name) {
        VInput vinput = this.get(name);
        if (vinput == null) {
            vinput = new VInput(name);
            this.put(name, vinput);
            vinput.type = IOType.digitalInput;
        }
        return vinput;
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

    public void deviceInputReported(
            String pinName,
            String HWid,
            String modelName) {

        PDevice d = RGPIO.PDeviceMap.get(HWid);

        // add the pin to the device in the corresponding device input map
        // and to the matching VInput map
        
        HashMap<String, PInput> deviceInputs = null;
        HashMap<String, VInput> VInputs = null;
        if (type == IOType.digitalInput) {
            deviceInputs = d.digitalInputs;
            VInputs = RGPIO.VDigitalInputMap;
        }
        if (type == IOType.analogInput) {
            deviceInputs = d.analogInputs;
            VInputs = RGPIO.VAnalogInputMap;
        }
        if (type == IOType.stringInput) {
            deviceInputs = d.stringInputs;
            VInputs = RGPIO.VStringInputMap;
        }

        PInput p = deviceInputs.get(pinName);
        if (p == null) {
            p = new PInput();
            p.type = type;
            p.name = pinName;
            p.value = null;
            p.device = d;
            p.vinput = null;
            deviceInputs.put(pinName, p);

            // match to a vinput
            int nrInstances = 0;
            VInput theOnlyVInput = null;
            for (VInput vinput : VInputs.values()) {
                if (vinput.matchesDevicePin(pinName, modelName, HWid)) {
                    theOnlyVInput = vinput;
                    nrInstances++;
//                    System.out.println(" pin match : " + pinName + " " + modelName + " " + nrInstances);
                }
            }
            if (nrInstances == 1) {
                p.vinput = theOnlyVInput;

                MessageEvent e = new MessageEvent(MessageType.Info);
                e.description = "device pin assigned to "+type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                e.vinput = p.vinput.name;
                RGPIO.message(e);
            }
            if (nrInstances == 0) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "device pin can not be assigned to "+type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
            if (nrInstances > 1) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "device pin matches more than one "+type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
            RGPIO.updateFeed.writeToClients(p.toJSON());
        }
    }

}