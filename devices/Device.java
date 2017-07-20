package devices;

import java.util.HashMap;
import udputils.UDPSender;
import rgpio.*;

public class Device {

    // status changes are forwarded to updateFeed. All access must be via methods. status field is private
    private DeviceStatus status = DeviceStatus.NULL;

    public String groupName = null;
    public String modelName = null;
    public RGPIODeviceGroup deviceGroup = null;
    public String HWid = null;        // unique hardware identifier
    public String ipAddress = null;
    public TimeStamp lastContact = new TimeStamp(0); // timestamp
    public TimeStamp powerOn = new TimeStamp(0);     // timestamp of device power-on

    public HashMap<String, PInput> digitalInputs = new HashMap<>(); // key is pin label
    public HashMap<String, POutput> digitalOutputs = new HashMap<>(); // key is pin label

    public String toJSON() {
        JSONString json = new JSONString();
        json.addString("object", "PDEV");
        json.addString("model", modelName);
        json.addString("HWid", HWid);
        json.addString("ipAddress", ipAddress);
        json.addString("status", status.name());
        return json.close();
    }

    public void set_status(DeviceStatus status) {
        if (status != this.status) {
            this.status = status;
            RGPIO.updateFeed.writeToClients(toJSON());
        }
    }

    public DeviceStatus get_status() {
        return this.status;
    }

    public void setActive() {
        set_status(DeviceStatus.ACTIVE);
        this.lastContact = new TimeStamp();
    }

    public void setNotResponding(String msg) {
        set_status(DeviceStatus.NOTRESPONDING);

        RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.DeviceNotResponding);
        e.description = msg;
        e.ipAddress = ipAddress;
        e.HWid = HWid;
        RGPIO.message(e);

    }

    public String sendToDevice(String message) {

        // delay because ESP misses packet if it receives SET  too fast after sending EVENT
        // (UDP is stopped and started on ESP after sending = blackout of a few msec)
        try {
            Thread.sleep(50);
        } catch (InterruptedException ie) {
        };

        String reply = null;
        if (ipAddress == null) {
            RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.SendWarning);
            e.description = "cannot send message to unreported device";
            e.HWid = HWid;
            RGPIO.message(e);
        } else if (status == DeviceStatus.ACTIVE) {
            reply = UDPSender.send(message, ipAddress, this, RGPIO.devicePort, 2000, 3);
            if (reply == null) {
                set_status(DeviceStatus.NOTRESPONDING);
                RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.DeviceNotResponding);
                e.description = "device did not reply to <" + message + ">";
                e.HWid = HWid;
                RGPIO.message(e);
            } else {
                setActive();
            }
        }
        return reply;
    }

   
}
