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
        if (msg.trace != null) {
            // special trace packet
            // it contains no other fields
            // example {"trace":"8650083(-51)8650811(-52)"}
            // Parse the trace packet and add the topology information to the device tree
            String previousHop = null;
            String currentHop = null;
            String[] traceSplit = msg.trace.split("\\)");
            for (String hop : traceSplit) {
                String[] hopSplit = hop.split("\\(");
                currentHop = hopSplit[0];
                if (previousHop != null) {
                    //System.out.println(previousHop + " -> " + currentHop);
                    // previousHop and currentHop are HWid of physical devices.
                    RGPIO.deviceTree.addLink(
                            RGPIO.PDeviceMap.vDeviceName(previousHop), 
                            RGPIO.PDeviceMap.vDeviceName(currentHop));
                }
                previousHop = currentHop;
            }
            //         System.out.println(previousHop + " -> " + "RGPIO");
            RGPIO.deviceTree.addLink(RGPIO.PDeviceMap.vDeviceName(previousHop), "RGPIO");

        } else if (msg.command.equals("REPORT")) {
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
                pdevice.sendACKREPORT(); // send ACKREPORT otherwise device will not respond   
//                System.out.println("***Device booted : (not)updating all pins");
//                pdevice.updateAllPins();
//                pdevice.updateVIOMembers();
            } else {
                // REPORT received for an existing PDevice.
                // If upTime is lower than from previous REPORT, the device has rebooted
                pdevice.ipAddress = deviceIPAddress;  // ip address may have changed
                pdevice.uptime = upTime;
                pdevice.report_received = System.currentTimeMillis();
                pdevice.setActive();
                pdevice.sendACKREPORT();  // send ACKREPORT otherwise device will not respond after reboot
                if (upTime < pdevice.uptime) {
                    //                   System.out.println("***Device rebooted : (not)updating all pins");
                    //                   pdevice.updateAllPins();
                    //                   pdevice.updateVIOMembers();
                }
            }

        } else if (msg.command.equals("EVENT")) {
            String HWid = msg.from;
            String pin = msg.pin;
            String value = msg.value;
            RGPIO.PDeviceMap.deviceEventHandler(HWid, pin, value);
        } else if (msg.command.equals("MESSAGE")) {
            String HWid = msg.from;
            RGPIO.PDeviceMap.deviceMessageHandler(HWid, msg.message);
        } else {
            e = new MessageEvent(MessageType.InvalidMessage);
            e.description = "invalid command from device <" + msg.message + ">";
            e.ipAddress = deviceIPAddress;
            RGPIO.message(e);
            return false;
        }
        return true;

    }

    

}
