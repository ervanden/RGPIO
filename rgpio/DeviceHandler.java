package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageEvent;

public class DeviceHandler {

    public static boolean handleDeviceMessage(String deviceIPAddress, String message) {

        MessageEvent e = new MessageEvent(MessageType.ReceivedMessage);
        e.description = "<" + message + ">";
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
                /*
                if (name.equals("Dip")) {
                    pinName = value;
                    RGPIO.VDigitalInputMap.deviceInputReported(pinName, HWid, model);
                }
                if (name.equals("Aip")) {
                    pinName = value;
                    RGPIO.VAnalogInputMap.deviceInputReported(pinName, HWid, model);
                }
                if (name.equals("Sip")) {
                    pinName = value;
                    RGPIO.VStringInputMap.deviceInputReported(pinName, HWid, model);
                }
*/
                if (name.equals("Dop")) {
                    pinName = value;
                    RGPIO.VDigitalOutputMap.deviceOutputReported(pinName, HWid, model);
                }

                if (name.equals("Aop")) {
                    pinName = value;
                    RGPIO.VAnalogOutputMap.deviceOutputReported(pinName, HWid, model);
                }
                if (name.equals("Sop")) {
                    pinName = value;
                    RGPIO.VStringOutputMap.deviceOutputReported(pinName, HWid, model);
                }
            }

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
