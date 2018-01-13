package rgpio;

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
            if (type == IOType.digitalInput) {
                vinput = new VDigitalInput();
            }
            if (type == IOType.analogInput) {
                vinput = new VAnalogInput();
            }
            if (type == IOType.stringInput) {
                vinput = new VStringInput();
            }
            vinput.name = name;
            this.put(name, vinput);
            vinput.type = type;
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
                for (PInput pin : device.inputs.values()) {
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
    
}
