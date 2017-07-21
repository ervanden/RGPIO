package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageEvent;
import java.util.HashMap;

public class VOutputMap extends HashMap<String, VOutput> {

    public VOutputMap() {
        super();
    }
    
        public VOutput add(String name) {
        VOutput digitalOutput = RGPIO.VDigitalOutputMap.get(name);
        if (digitalOutput == null) {
            digitalOutput = new VOutput(name);
            RGPIO.VDigitalOutputMap.put(name, digitalOutput);
            digitalOutput.type=IOType.digitalOutput;
        };
        return digitalOutput;
    }

    public void print() {
        String formatString = "%12s %5s %1s\n";
        System.out.printf(formatString, "DigitalOutput", "state", "device.pin");
        for (VOutput d : this.values()) {
            String name = d.name;
            String state = d.value;

            String separator = "";
            String devicePins = "<";
            for (PDevice device : RGPIO.PDeviceMap.values()) {
                for (POutput pin : device.digitalOutputs.values()) {
                    if (pin.voutput == d) {
                        devicePins = devicePins + separator + device.HWid + "." + pin.name;
                        separator = ",";
                    }
                }
            }
            devicePins = devicePins + ">";
            System.out.printf(formatString, name, state, devicePins);
        }
    }

    public void digitalOutputReported(
            String pinName,
            String HWid,
            String modelName) {

        PDevice d = RGPIO.PDeviceMap.get(HWid);

        // add the Dop to the device
        // test if already exists in case we process REPORT more than once
        POutput p = d.digitalOutputs.get(pinName);
        if (p == null) {
            p = new POutput();
            p.name = pinName;
            p.type=IOType.digitalOutput;
            p.value = null;
            p.device = d;
            p.voutput = null;
            d.digitalOutputs.put(pinName, p);

            // match to a voutput
            int nrInstances = 0;
            VOutput theOnlyDigitalOutput = null;
            for (VOutput digitalOutput : RGPIO.VDigitalOutputMap.values()) {
                if (digitalOutput.matchesDevicePin(pinName, modelName, HWid)) {
                    theOnlyDigitalOutput = digitalOutput;
                    nrInstances++;
                }
            }
            if (nrInstances == 1) {
                p.voutput = theOnlyDigitalOutput;

                MessageEvent e = new MessageEvent(MessageType.Info);
                e.description = "device pin assigned to digital output";
                e.HWid = HWid;
                e.pinLabel = pinName;
                e.voutput = p.voutput.name;
                RGPIO.message(e);
            }
            if (nrInstances == 0) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "device pin can not be assigned to digital output";
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
            if (nrInstances > 1) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "device pin matches more than one digital output";
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
                              RGPIO.updateFeed.writeToClients(p.toJSON());
        }
    }

}
