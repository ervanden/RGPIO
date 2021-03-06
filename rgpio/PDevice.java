package rgpio;

import utils.JSONString;
import java.util.HashMap;
import udputils.UDPSender;

public class PDevice {

    // status field is private, all access via methods so we can react on changes
    private PDeviceStatus status = PDeviceStatus.NULL;

    public String modelName = null;
    public VDevice vdevice = null;
    public String HWid = null;        // unique hardware identifier
    public String ipAddress = null;
    public int uptime;   // last reported uptime
    public long report_received = 0; // time stamp of last report

    public HashMap<String, PInput> inputs = new HashMap<>();
    public HashMap<String, POutput> outputs = new HashMap<>();

    public String toJSON() {
        JSONString json = new JSONString();
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
            status = PDeviceStatus.ACTIVE;
            RGPIO.webSocketServer.sendToAll(toJSON());
            if (vdevice != null) {
                vdevice.stateChange();
            }
        }
    }

    public void setNotResponding(String msg) {
        if (status != PDeviceStatus.NOTRESPONDING) {
            status = PDeviceStatus.NOTRESPONDING;
            RGPIO.webSocketServer.sendToAll(toJSON());
            if (vdevice != null) {
                vdevice.stateChange();
            }
            updateVIOMembers();

            MessageEvent e = new MessageEvent(MessageType.DeviceNotResponding);
            e.description = msg;
            e.ipAddress = ipAddress;
            e.HWid = HWid;
            RGPIO.message(e);

            // web update all the pins to display NOTRESPONDING
            for (PInput ip : inputs.values()) {
                RGPIO.webSocketServer.sendToAll(ip.toJSON());
            }
            for (POutput op : outputs.values()) {
                RGPIO.webSocketServer.sendToAll(op.toJSON());
            }
        }
    }

    public void updateAllPins() {

        System.out.println("*** updating all input pins of " + HWid);
        for (PInput ip : inputs.values()) {
            if (ip.vinput != null) {
                System.out.println(ip.vinput.name + ".get()");
                ip.vinput.get();   // includes web update
            }
        }

        System.out.println("*** updating all output pins of " + HWid);
        for (POutput op : outputs.values()) {
            if (op.voutput != null) {
                if (op.voutput.value != null) {
                    System.out.println(op.voutput.name + ".set(" + op.voutput.value + ")");
                    op.voutput.set(op.voutput.value); // includes web update
                }
            }
        }
    }

    public void updateVIOMembers() {

        for (PInput ip : inputs.values()) {
            if (ip.vinput != null) {
                ip.vinput.countMembers();   // includes web update
            }
        }

        for (POutput op : outputs.values()) {
            if (op.voutput != null) {
                op.voutput.countMembers();   // includes web update
            }
        }
    }

    public void sendGetCommand(PInput p) {
        JSONString json = new JSONString();
        json.addProperty("command", "GET");
        json.addProperty("id", RGPIO.msgId());
        json.addProperty("from", "RGPIO");
        json.addProperty("to", HWid);
        json.addProperty("pin", p.name);
        sendPacket(json.asString());
    }

    public void sendSetCommand(POutput p) {

        JSONString json = new JSONString();
        json.addProperty("command", "SET");
        json.addProperty("id", RGPIO.msgId());
        json.addProperty("from", "RGPIO");
        json.addProperty("to", HWid);
        json.addProperty("pin", p.name);
        json.addProperty("value", p.get_value());
        sendPacket(json.asString());
    }

    public void sendMessage(String message) {
        JSONString json = new JSONString();
        json.addProperty("command", "MESSAGE");
        json.addProperty("id", RGPIO.msgId());
        json.addProperty("from", "RGPIO");
        json.addProperty("to", HWid);
        json.addProperty("message", message);
        sendPacket(json.asString());
    }

    public void sendACKREPORT() {
        JSONString json = new JSONString();
        json.addProperty("command", "ACKREPORT");
        json.addProperty("id", RGPIO.msgId());
        json.addProperty("from", "RGPIO");
        json.addProperty("to", HWid);
        sendPacket(json.asString());
    }

    public void sendPacket(String message) {

        // throttle sending packets to a device
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
        }

        if (ipAddress == null) {
            MessageEvent e = new MessageEvent(MessageType.SendWarning);
            e.description = "cannot send message to unreported device";
            e.HWid = HWid;
            RGPIO.message(e);
        } else if (status == PDeviceStatus.ACTIVE) {
            UDPSender.send(message, ipAddress, this, RGPIO.devicePort);
        }
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
                e.description = "PDevice pin assigned to " + type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                e.voutput = poutput.voutput.name;
                RGPIO.message(e);
            }
            if (nrInstances == 0) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "PDevice pin can not be assigned to " + type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
            if (nrInstances > 1) {
                MessageEvent e = new MessageEvent(MessageType.UnassignedPin);
                e.description = "PDevice pin matches more than one " + type.name();
                e.HWid = HWid;
                e.pinLabel = pinName;
                RGPIO.message(e);
            }
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

        }
    }

}
