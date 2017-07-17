package rgpio;

import devices.Device;
import java.util.HashMap;
import java.util.Map;

public class RGPIODeviceGroupMap extends HashMap<String, RGPIODeviceGroup> {

    public RGPIODeviceGroupMap() {
        super();
    }

    public RGPIODeviceGroup add(String name) {
        RGPIODeviceGroup device = RGPIO.deviceGroupMap.get(name);
        if (device == null) {
            device = new RGPIODeviceGroup(name);
            RGPIO.deviceGroupMap.put(name, device);
        };
        return device;
    }

    public void print() {
        String formatString = "%12s %6s %1s\n";
        System.out.printf(formatString, "Device", "min", "<physical devices>");

        for (Map.Entry<String, RGPIODeviceGroup> e : this.entrySet()) {
            RGPIODeviceGroup deviceGroup = e.getValue();
            String deviceGroupName = deviceGroup.name;
            String min = "" + deviceGroup.minMembers;

            String deviceString = "<";
            String separator = "";
            for (Device device : RGPIO.deviceMap.values()) {
                if (device.groupName != null) {  // device assigned to group
                    if (device.groupName.equals(deviceGroupName)) {
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
