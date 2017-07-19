package rgpio;

import devices.Device;
import devices.DeviceDigitalInput;
import java.util.HashMap;

public class RGPIOInputMap extends HashMap<String, RGPIOInput> {

    public RGPIOInputMap() {
        super();
    }

    public RGPIOInput add(String name) {
        RGPIOInput digitalInput = RGPIO.digitalInputMap.get(name);
        if (digitalInput == null) {
            digitalInput = new RGPIOInput(name);
            RGPIO.digitalInputMap.put(name, digitalInput);
            digitalInput.type=RGPIOIOType.DigitalInput;
        };
        return digitalInput;
    }

    public void print() {
        String formatString = "%12s %1s\n";
        System.out.printf(formatString, "DigitalInput",  "device.pin");
        for (RGPIOInput d : this.values()) {
            String name = d.name;

            String state = d.get_value();


            String separator = "";
            String devicePins = "<";
            for (Device device : RGPIO.deviceMap.values()) {
                for (DeviceDigitalInput pin : device.digitalInputs.values()) {
                    if (pin.digitalInput == d) {
                        devicePins = devicePins + separator + device.HWid + "." + pin.name;
                        separator = ",";
                    }
                }
            }
            devicePins = devicePins + ">";
            System.out.printf(formatString, name,  devicePins);
        }
    }

    public void digitalInputReported(
            String pinName,
            String HWid,
            String modelName) {

        Device d = RGPIO.deviceMap.get(HWid);

        // add the pin to the device
        DeviceDigitalInput p = d.digitalInputs.get(pinName);
        if (p == null) {
            p = new DeviceDigitalInput();
            p.name = pinName;
            p.value = null;
            p.device = d;
            p.digitalInput = null;
            d.digitalInputs.put(pinName, p);

            // match to a digitalInput
            int nrInstances = 0;
            RGPIOInput theOnlyDigitalInput = null;
            for (RGPIOInput digitalInput : RGPIO.digitalInputMap.values()) {
                if (digitalInput.matchesDevicePin(pinName, modelName, HWid)) {
                    theOnlyDigitalInput = digitalInput;
  
                    nrInstances++;
//                    System.out.println(" pin match : " + pinName + " " + modelName + " " + nrInstances);
                }
            }
            if (nrInstances == 1) {
                p.digitalInput = theOnlyDigitalInput;

                RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.Info);
                e.description = "device pin assigned to digital input";
                e.HWid = HWid;
                e.pinLabel = pinName;
                e.digitalInput = p.digitalInput.name;
                RGPIO.message(e);
            }
            if (nrInstances == 0) {
                RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.UnassignedPin);
                e.description = "device pin can not be assigned to digital input";
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
            if (nrInstances > 1) {
                RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.UnassignedPin);
                e.description = "device pin matches more than one digital input";
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
                    RGPIO.updateFeed.writeToClients(p.toJSON());
        }
    }

}
