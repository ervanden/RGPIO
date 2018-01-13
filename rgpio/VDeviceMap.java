package rgpio;

import java.util.HashMap;

public class VDeviceMap extends HashMap<String, VDevice> {

    public VDeviceMap() {
        super();
    }

    public VDevice add(String name) {
        VDevice device = RGPIO.VDeviceMap.get(name);
        if (device == null) {
            device = new VDevice(name);
            RGPIO.VDeviceMap.put(name, device);
        }
        return device;
    }

}
