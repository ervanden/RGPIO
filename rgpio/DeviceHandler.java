package rgpio;

public class DeviceHandler {

    public static boolean handleDeviceMessage(String deviceIPAddress, String message) {

        MessageEvent e = new MessageEvent(MessageType.ReceivedMessage);
        e.description = "|" + message + "|";
        e.ipAddress = deviceIPAddress;
        RGPIO.message(e);

        String[] s = message.split("/");
        String command = s[0];
        String name;
        String value;

        if (command.equals("Report")) {
            String HWid = null;
            String model = null;
            Integer upTime = null;
            String pinName;
            String pinType;
            PDevice pdevice = null;

            for (int i = 1; i < s.length; i++) {
                String nameValue = s[i];
                String[] nv = nameValue.split(":");
                name = nv[0];
                value = nv[1];
                if (name.equals("HWid")) {
                    HWid = value;
                }
                if (name.equals("Model")) {
                    model = value;
                }
                if (name.equals("Uptime")) {
                    upTime = Integer.parseInt(value);
                    pdevice = RGPIO.PDeviceMap.addPDevice(HWid, model, deviceIPAddress, upTime);
                }
                if ((name.equals("Dip"))
                        || (name.equals("Aip"))
                        || (name.equals("Sip"))) {
                    pinType = name;
                    pinName = value;
                    pdevice.addPInput(pinType, pinName, HWid, model);
                }
                if ((name.equals("Dop"))
                        || (name.equals("Aop"))
                        || (name.equals("Sop"))) {
                    pinType = name;
                    pinName = value;
                    pdevice.addPOutput(pinType, pinName, HWid, model);
                }
            }
            // add 1 to the members field of all VIO that contain a pin of this pdevice
            pdevice.updateVIOMembers(); 

        } else if (command.equals("Event")) {

            String HWid = null;
            String model = null;
            String pinLabel = null;
            String pinValue = null;

            for (int i = 1; i < s.length; i++) {
                String nameValue = s[i];
                String[] nv = nameValue.split(":");
                name = nv[0];
                value = nv[1];
                if (name.equals("HWid")) {
                    HWid = value;
                }
                if (name.equals("Model")) {
                    model = value;
                }
                if ((name.equals("Dip")) || (name.equals("Aip")) || (name.equals("Sip"))) {
                    pinLabel = value;
                }
                if (name.equals("Value")) {
                    pinValue = value;
                }
            }
            RGPIO.PDeviceMap.deviceReportedPinEvent(HWid, pinLabel, pinValue);

        } else {
            e = new MessageEvent(MessageType.InvalidMessage);
            e.description = "invalid command from device <" + message + ">";
            e.ipAddress = deviceIPAddress;
            RGPIO.message(e);
            return false;
        }
        return true;

    }

}
