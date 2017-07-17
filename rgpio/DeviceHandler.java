
package rgpio;

import static rgpio.RGPIO.deviceMap;
import static rgpio.RGPIO.digitalInputMap;
import static rgpio.RGPIO.digitalOutputMap;


public class DeviceHandler {
 
        public static boolean handleDeviceMessage(String deviceIPAddress, String message) {

        /*  Messages :
        
         Report/HWid:xxx/Model:xxx/Uptime:xxx
         Event/HWid:%s/Model:%s/Pin:%s/%p       (event = HIGH or LOW or number)
        
         return value = true : valid command
         return value = false : invalid command
 
         */
        RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.ReceivedMessage);
        e.description = "<" + message + ">";
        e.ipAddress = deviceIPAddress;
        RGPIO.message(e);

        //       try {
        String[] s = message.split("/");
        String command = s[0];
        String name;
        String value;

        if (command.equals("Report")) {
            String HWid = null;
            String model = null;
            Integer upTime = null;
            String pinName;

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
                    deviceMap.deviceReported(HWid, model, deviceIPAddress, upTime);
                }
                if (name.equals("Dip")) {
                    pinName = value;
                    digitalInputMap.digitalInputReported(pinName, HWid, model);
                }
                if (name.equals("Dop")) {
                    pinName = value;
                    digitalOutputMap.digitalOutputReported(pinName, HWid, model);
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
                if (name.equals("Dip")) {
                    pinLabel = value;
                }
                if (name.equals("Value")) {
                    pinValue = value;
                }
            }
            deviceMap.deviceReportedPinEvent(HWid, pinLabel, pinValue);

        } else {
            e = new RGPIOMessageEvent(RGPIOMessageType.InvalidMessage);
            e.description = "invalid command from device <" + message + ">";
            e.ipAddress = deviceIPAddress;
            RGPIO.message(e);
            return false;
        }
        return true;

    }

}
