package rgpio;

import rgpioutils.DeviceMessage;
import udputils.UDPSender;
import utils.JSON2Object;
import utils.JSONString;

public class DeviceHandler {

    public static boolean handleDeviceMessage(String deviceIPAddress, String message) {

        MessageEvent e = new MessageEvent(MessageType.ReceivedMessage);
        e.description = "|" + message + "|";
        e.ipAddress = deviceIPAddress;
        RGPIO.message(e);

        DeviceMessage msg;
        msg = (DeviceMessage) JSON2Object.jsonStringToObject(message, DeviceMessage.class);

        if (msg.command.equals("REPORT")) {
            String HWid = msg.from;
            String model = msg.model;
            Integer upTime = Integer.parseInt(msg.uptime);

            PDevice pdevice;
            pdevice = RGPIO.PDeviceMap.get(HWid);
            if (pdevice == null) {
                pdevice = RGPIO.PDeviceMap.addPDevice(HWid, model);
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
                // The application may have set() a pin before the device reports
                pdevice.ipAddress = deviceIPAddress;  // ip address may have changed
                pdevice.uptime = upTime;
                pdevice.report_received = System.currentTimeMillis();
                pdevice.setActive();             
                sendACKREPORT(pdevice); // send ACKREPORT otherwise device will not respond   
                System.out.println("***Device booted : updating all pins");
                pdevice.updateAllPins();
                pdevice.updateVIOMembers();
            } else {
                // REPORT received for an existing PDevice.
                // If upTime is lower than from previous REPORT, the device has rebooted
                pdevice.ipAddress = deviceIPAddress;  // ip address may have changed
                pdevice.uptime = upTime;
                pdevice.report_received = System.currentTimeMillis();
                pdevice.setActive();
                sendACKREPORT(pdevice);  // send ACKREPORT otherwise device will not respond after reboot
                if (upTime < pdevice.uptime) {
                    System.out.println("***Device rebooted : updating all pins");
                    pdevice.updateAllPins();
                    pdevice.updateVIOMembers();
                }
            }

        } else if (msg.command.equals("EVENT")) {
            String HWid = msg.from;
            String pin = msg.pin;
            String value = msg.value;

            RGPIO.PDeviceMap.deviceEventHandler(HWid, pin, value);

        } else {
            e = new MessageEvent(MessageType.InvalidMessage);
            e.description = "invalid command from device <" + message + ">";
            e.ipAddress = deviceIPAddress;
            RGPIO.message(e);
            return false;
        }
        return true;

    }

    private static void sendACKREPORT(PDevice pdevice) {
        JSONString json = new JSONString();
        json.addProperty("command", "ACKREPORT");
                json.addProperty("id", RGPIO.msgId());
        json.addProperty("from", "RGPIO");
        json.addProperty("to", pdevice.HWid);
        String ackReport = json.asString();
 //       pdevice.sendToDevice(ackReport);
        UDPSender.send(ackReport, pdevice.ipAddress, null, RGPIO.devicePort);
    }
}
