package rgpio;

import rgpioutils.DeviceMessage;
import utils.JSON2Object;

public class DeviceHandler {

    public static boolean handleDeviceMessage(String deviceIPAddress, String message) {

        MessageEvent e = new MessageEvent(MessageType.ReceivedMessage);
        e.description = "|" + message + "|";
        e.ipAddress = deviceIPAddress;
        RGPIO.message(e);

        DeviceMessage msg;
        msg = (DeviceMessage) JSON2Object.jsonStringToObject(message, DeviceMessage.class);

        if (msg.command.equals("REPORT")) {
            String HWid = msg.hwid;
            String model = msg.model;
            Integer upTime = Integer.parseInt(msg.uptime);

            PDevice pdevice = null;
            pdevice = RGPIO.PDeviceMap.addPDevice(HWid, model, deviceIPAddress, upTime);
            if (msg.dips != null) {
                for (String pinName : msg.dips) {
                    pdevice.addPInput("Dip", pinName, HWid, model);
                }
            }
            if (msg.dops != null) {
                for (String pinName : msg.dops) {
                    pdevice.addPOutput("Dop", pinName, HWid, model);
                }
            }
            if (msg.aips != null) {
                for (String pinName : msg.aips) {
                    pdevice.addPInput("Aip", pinName, HWid, model);
                }
            }
            if (msg.aops != null) {
                for (String pinName : msg.aops) {
                    pdevice.addPOutput("Aop", pinName, HWid, model);
                }
            }

            pdevice.setActive();

        } else if (msg.command.equals("EVENT")) {
            String HWid = msg.hwid;
            String pin = msg.pin;
            String value = msg.value;

            RGPIO.PDeviceMap.deviceReportedPinEvent(HWid, pin, value);

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
