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

    public String modelName = null;
    public VDevice vdevice = null;
    public String HWid = null;        // unique hardware identifier
    public String ipAddress = null;
    public TimeStamp lastContact = new TimeStamp(0); // timestamp
    public TimeStamp powerOn = new TimeStamp(0);     // timestamp of device power-on

    public HashMap<String, PInput> inputs = new HashMap<>();
    public HashMap<String, POutput> outputs = new HashMap<>();

    /*
     public HashMap<String, PInput> digitalInputs = new HashMap<>(); // key is pin label
     public HashMap<String, PInput> analogInputs = new HashMap<>(); // key is pin label
     public HashMap<String, PInput> stringInputs = new HashMap<>(); // key is pin label
     public HashMap<String, POutput> digitalOutputs = new HashMap<>(); // key is pin label
     public HashMap<String, POutput> analogOutputs = new HashMap<>(); // key is pin label
     public HashMap<String, POutput> stringOutputs = new HashMap<>(); // key is pin label
     */
    public String toJSON() {
        JSONObject json = new JSONObject();
        json.addProperty("object", "PDEV");
        json.addProperty("model", modelName);
        json.addProperty("HWid", HWid);
        json.addProperty("ipAddress", ipAddress);
        json.addProperty("status", status.name());
        return json.asString();
    }

    public PDeviceStatus get_status() {
        return this.status;
    }

    public void setActive() {
        if (status != PDeviceStatus.ACTIVE) {
            System.out.println("pdev " + HWid + " changed from " + status + " to ACTIVE ");
            status = PDeviceStatus.ACTIVE;
            RGPIO.updateFeed.writeToClients(toJSON());
            // if the device was not responding and becomes active again, the state of input pins
            // should be re-read and output pins should be re-set
            setAllPins("UNKNOWN");
        }
        this.lastContact = new TimeStamp();
    }

    public void setNotResponding(String msg) {
        if (status != PDeviceStatus.NOTRESPONDING) {
            status = PDeviceStatus.NOTRESPONDING;
            RGPIO.updateFeed.writeToClients(toJSON());
        }

        MessageEvent e = new MessageEvent(MessageType.DeviceNotResponding);
        e.description = msg;
        e.ipAddress = ipAddress;
        e.HWid = HWid;
        RGPIO.message(e);

        setAllPins("NOTRESPONDING");

    }

    private void setAllPins(String value) {
        for (PInput ip : inputs.values()) {
            ip.set_value(value);
        }
        for (POutput op : outputs.values()) {
            op.set_value(value);
        }

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
                setNotResponding("device did not reply to <" + message + ">");
            } else {
                setActive();
            }
        }
        return reply;
    }

    
    
    public void addPOutput(
            String pinType,
            String pinName,
            String HWid,
            String modelName) {

        IOType type = IOType.shortToLong(pinType);

        PDevice pdevice = RGPIO.PDeviceMap.get(HWid);

        // add the pin to the device output map
        // and to the matching VOutput map
        HashMap<String, VOutput> VOutputs = null;

        if (type == IOType.digitalOutput) {
            VOutputs = RGPIO.VDigitalOutputMap;
        }
        if (type == IOType.analogOutput) {
            VOutputs = RGPIO.VAnalogOutputMap;
        }
        if (type == IOType.stringOutput) {
            VOutputs = RGPIO.VStringOutputMap;
        }

        POutput poutput = pdevice.outputs.get(pinName);
        if (poutput == null) {
            poutput = new POutput();
            poutput.type = type;
            poutput.name = pinName;
            poutput.device = pdevice;
            poutput.voutput = null;
            pdevice.outputs.put(pinName, poutput);

            // match to a voutput
            int nrInstances = 0;
            VOutput theOnlyVOutput = null;
            for (VOutput voutput : VOutputs.values()) {
                if (voutput.matchesDevicePin(pinName, modelName, HWid)) {
                    theOnlyVOutput = voutput;
                    nrInstances++;
//                    System.out.println(" pin match : " + pinName + " " + modelName + " " + nrInstances);
                }
            }
            if (nrInstances == 1) {
                poutput.voutput = theOnlyVOutput;

                MessageEvent e = new MessageEvent(MessageType.Info);
                e.description = "device pin assigned to " + type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                e.voutput = poutput.voutput.name;
                RGPIO.message(e);
            }
            if (nrInstances == 0) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "device pin can not be assigned to " + type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
            if (nrInstances > 1) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "device pin matches more than one " + type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
                        poutput.set_value("UNKNOWN"); // will create an update
//            RGPIO.updateFeed.writeToClients(p.toJSON());
        }
    }

    
    
    public void addPInput(
            String pinType,
            String pinName,
            String HWid,
            String modelName) {

        IOType type = IOType.shortToLong(pinType);

        PDevice pdevice = RGPIO.PDeviceMap.get(HWid);

        // add the pin to the device output map
        // and to the matching VInput map
        HashMap<String, VInput> VInputs = null;

        if (type == IOType.digitalInput) {
            VInputs = RGPIO.VDigitalInputMap;
        }
        if (type == IOType.analogInput) {
            VInputs = RGPIO.VAnalogInputMap;
        }
        if (type == IOType.stringInput) {
            VInputs = RGPIO.VStringInputMap;
        }

        PInput pinput = pdevice.inputs.get(pinName);
        if (pinput == null) {
            pinput = new PInput();
            pinput.type = type;
            pinput.name = pinName;
            pinput.device = pdevice;
            pinput.vinput = null;
            pdevice.inputs.put(pinName, pinput);

            // match to a vinput
            int nrInstances = 0;
            VInput theOnlyVInput = null;
            for (VInput vinput : VInputs.values()) {
                if (vinput.matchesDevicePin(pinName, modelName, HWid)) {
                    theOnlyVInput = vinput;
                    nrInstances++;
//                    System.out.println(" pin match : " + pinName + " " + modelName + " " + nrInstances);
                }
            }
            if (nrInstances == 1) {
                pinput.vinput = theOnlyVInput;

                MessageEvent e = new MessageEvent(MessageType.Info);
                e.description = "device pin assigned to " + type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                e.vinput = pinput.vinput.name;
                RGPIO.message(e);
            }
            if (nrInstances == 0) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "device pin can not be assigned to " + type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
            if (nrInstances > 1) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "device pin matches more than one " + type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
                        pinput.set_value("UNKNOWN"); // will trigger an update
 //          RGPIO.updateFeed.writeToClients(p.toJSON());
        }
    }

}
