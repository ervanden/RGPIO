package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageEvent;
import utils.TimeStamp;
import utils.JSONObject;
import java.util.HashMap;
import udputils.UDPSender;

public class PDevice {

    // status changes are forwarded to updateFeed. All access must be via methods. status field is private
    private PDeviceStatus status = PDeviceStatus.NULL;

 //   public String groupName = null;
    public String modelName = null;
    public VDevice vdevice = null;
    public String HWid = null;        // unique hardware identifier
    public String ipAddress = null;
    public TimeStamp lastContact = new TimeStamp(0); // timestamp
    public TimeStamp powerOn = new TimeStamp(0);     // timestamp of device power-on

    public HashMap<String, PInput> digitalInputs = new HashMap<>(); // key is pin label
    public HashMap<String, PInput> analogInputs = new HashMap<>(); // key is pin label
    public HashMap<String, PInput> stringInputs = new HashMap<>(); // key is pin label
    public HashMap<String, POutput> digitalOutputs = new HashMap<>(); // key is pin label
    public HashMap<String, POutput> analogOutputs = new HashMap<>(); // key is pin label
    public HashMap<String, POutput> stringOutputs = new HashMap<>(); // key is pin label

    public String toJSON() {
        JSONObject json = new JSONObject();
        json.addProperty("object", "PDEV");
        json.addProperty("model", modelName);
        json.addProperty("HWid", HWid);
        json.addProperty("ipAddress", ipAddress);
        json.addProperty("status", status.name());
        return json.asString();
    }

    public void set_status(PDeviceStatus status) {
        if (status != this.status) {
            this.status = status;
            RGPIO.updateFeed.writeToClients(toJSON());
        }
    }

    public PDeviceStatus get_status() {
        return this.status;
    }

    public void setActive() {
        set_status(PDeviceStatus.ACTIVE);
        this.lastContact = new TimeStamp();
    }

    public void setNotResponding(String msg) {
        set_status(PDeviceStatus.NOTRESPONDING);

        MessageEvent e = new MessageEvent(MessageType.DeviceNotResponding);
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
            MessageEvent e = new MessageEvent(MessageType.SendWarning);
            e.description = "cannot send message to unreported device";
            e.HWid = HWid;
            RGPIO.message(e);
        } else if (status == PDeviceStatus.ACTIVE) {
            reply = UDPSender.send(message, ipAddress, this, RGPIO.devicePort, 2000, 3);
            if (reply == null) {
                set_status(PDeviceStatus.NOTRESPONDING);
                MessageEvent e = new MessageEvent(MessageType.DeviceNotResponding);
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
