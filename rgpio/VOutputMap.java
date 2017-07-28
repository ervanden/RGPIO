package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageEvent;
import java.util.HashMap;

public class VOutputMap extends HashMap<String, VOutput> {

    IOType type;

    public VOutputMap(IOType type) {
        super();
        this.type = type;
    }

    public VOutput add(String name) {
        VOutput voutput = this.get(name);
        if (voutput == null) {
            if (type == IOType.digitalOutput) {
                voutput = new VDigitalOutput();
            }
            if (type == IOType.analogOutput) {
                voutput = new VAnalogOutput();
            }
            if (type == IOType.stringOutput) {
                voutput = new VStringOutput();
            }
            voutput.name=name;
            this.put(name, voutput);
            voutput.type = type;
        };
        return voutput;
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
                for (POutput pin : device.outputs.values()) {
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

    public void deviceOutputReported(
            String pinName,
            String HWid,
            String modelName) {

        PDevice pdevice = RGPIO.PDeviceMap.get(HWid);

        // add the pin to the device in the corresponding device output map
        // and to the matching VOutput map
        HashMap<String, VOutput> VOutputs = null;
        if (type == IOType.digitalOutput) {
            VOutputs = RGPIO.VDigitalOutputMap;
        }
        if (type == IOType.analogOutput) {
            VOutputs = RGPIO.VAnalogOutputMap;
        }
        if (type == IOType.stringOutput) {
            VOutputs = RGPIO.VStringOutputMap;
        }

        POutput p = pdevice.outputs.get(pinName);
        if (p == null) {
            p = new POutput();
            p.type = type;
            p.name = pinName;
            p.device = pdevice;
            p.voutput = null;
            pdevice.outputs.put(pinName, p);

            // match to a voutput
            int nrInstances = 0;
            VOutput theOnlyVOutput = null;
            for (VOutput voutput : VOutputs.values()) {
                if (voutput.matchesDevicePin(pinName, modelName, HWid)) {
                    theOnlyVOutput = voutput;
                    nrInstances++;
//                    System.out.println(" pin match : " + pinName + " " + modelName + " " + nrInstances);
                }
            }
            if (nrInstances == 1) {
                p.voutput = theOnlyVOutput;

                MessageEvent e = new MessageEvent(MessageType.Info);
                e.description = "device pin assigned to " + type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                e.voutput = p.voutput.name;
                RGPIO.message(e);
            }
            if (nrInstances == 0) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "device pin can not be assigned to " + type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
            if (nrInstances > 1) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "device pin matches more than one " + type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
            p.set_value(null); // trigger update 
        }
    }

}
