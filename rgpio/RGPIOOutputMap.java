package rgpio;

import devices.Device;
import devices.DeviceDigitalOutput;
import java.util.HashMap;

public class RGPIOOutputMap extends HashMap<String, RGPIOOutput> {

    public RGPIOOutputMap() {
        super();
    }
    
        public RGPIOOutput add(String name) {
        RGPIOOutput digitalOutput = RGPIO.digitalOutputMap.get(name);
        if (digitalOutput == null) {
            digitalOutput = new RGPIOOutput(name);
            RGPIO.digitalOutputMap.put(name, digitalOutput);
            digitalOutput.type=RGPIOIOType.DigitalOutput;
        };
        return digitalOutput;
    }

    public void print() {
        String formatString = "%12s %5s %1s\n";
        System.out.printf(formatString, "DigitalOutput", "state", "device.pin");
        for (RGPIOOutput d : this.values()) {
            String name = d.name;
            String state = d.value;

            String separator = "";
            String devicePins = "<";
            for (Device device : RGPIO.deviceMap.values()) {
                for (DeviceDigitalOutput pin : device.digitalOutputs.values()) {
                    if (pin.digitalOutput == d) {
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

        Device d = RGPIO.deviceMap.get(HWid);

        // add the Dop to the device
        // test if already exists in case we process REPORT more than once
        DeviceDigitalOutput p = d.digitalOutputs.get(pinName);
        if (p == null) {
            p = new DeviceDigitalOutput();
            p.name = pinName;
            p.value = null;
            p.device = d;
            p.digitalOutput = null;
            d.digitalOutputs.put(pinName, p);

            // match to a digitalOutput
            int nrInstances = 0;
            RGPIOOutput theOnlyDigitalOutput = null;
            for (RGPIOOutput digitalOutput : RGPIO.digitalOutputMap.values()) {
                if (digitalOutput.matchesDevicePin(pinName, modelName, HWid)) {
                    theOnlyDigitalOutput = digitalOutput;
                    nrInstances++;
                }
            }
            if (nrInstances == 1) {
                p.digitalOutput = theOnlyDigitalOutput;

                RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.Info);
                e.description = "device pin assigned to digital output";
                e.HWid = HWid;
                e.pinLabel = pinName;
                e.digitalOutput = p.digitalOutput.name;
                RGPIO.message(e);
            }
            if (nrInstances == 0) {
                RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.UnassignedPin);
                e.description = "device pin can not be assigned to digital output";
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
            if (nrInstances > 1) {
                RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.UnassignedPin);
                e.description = "device pin matches more than one digital output";
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
        }
    }

}
