package rgpio;

import java.util.HashMap;
import java.util.Map;

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

    public void print() {
        String formatString = "%12s %6s %1s\n";
        System.out.printf(formatString, "Device", "min", "<physical devices>");

        for (Map.Entry<String, VDevice> e : this.entrySet()) {
            VDevice deviceGroup = e.getValue();
            String deviceGroupName = deviceGroup.name;
            String min = "" + deviceGroup.minMembers;

            String deviceString = "<";
            String separator = "";
            for (PDevice device : RGPIO.PDeviceMap.values()) {
                if (device.vdevice != null) {  // device assigned to group
                    if (device.vdevice.name.equals(deviceGroupName)) {
                        String HWid = device.HWid;
                        if (HWid != null) {
                            deviceString = deviceString + separator + HWid;
                            separator = ",";
                        }

                    }
                }
            }
            deviceString = deviceString + ">";

            System.out.printf(formatString, deviceGroupName, min, deviceString);
        }
    }

}
