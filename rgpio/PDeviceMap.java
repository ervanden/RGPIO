package rgpio;


import utils.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PDeviceMap extends ConcurrentHashMap<String, PDevice> {

    // key is HWid
    // concurrent because devices can report and be added to the map at any time
    String deviceMapFileName;
    int unknownDeviceCounter = 0;

    public PDeviceMap() {
        super();
        Console.verbose(true);
    }

    public PDevice addPDevice(String HWid, String modelName) {
        // Create it and match to a vdevice
        PDevice pdevice = new PDevice();
        RGPIO.PDeviceMap.put(HWid, pdevice);
        pdevice.HWid = HWid;
        pdevice.vdevice = null;
        pdevice.modelName = modelName;
        this.put(HWid, pdevice);
        matchToGroup(pdevice);
        return pdevice;
    }
    
    static String vDeviceName(String HWid) {
        // returns the name of the vdevice where this pdevice is assigned to
        // if there is no vdevice, just return HWid
        PDevice pdevice;
        String name = HWid;
        pdevice = RGPIO.PDeviceMap.get(HWid);
        if (pdevice != null) {
            //System.out.println(" pdevice exists for " + HWid);
            if (pdevice.vdevice != null) {
                //System.out.println(" vdevice exists for " + HWid);
                name = pdevice.vdevice.name;
            }
        }
        return name;
    }

    public void deviceEventHandler(
            String HWid,
            String pinLabel,
            String value) {
        PDevice d = this.get(HWid);
        if (d == null) {
            MessageEvent e = new MessageEvent(MessageType.UnreportedDevice);
            e.description = "received event from an unreported device";
            e.HWid = HWid;
            e.pinLabel = pinLabel;
            e.value = value;
            RGPIO.message(e);
            return;
        }

        d.setActive();

        PInput pinput = null;
        pinput = d.inputs.get(pinLabel);
        POutput poutput = null;
        poutput = d.outputs.get(pinLabel);

        if (pinput != null) {
            pinput.event_received = System.currentTimeMillis();
        }
        if (poutput != null) {
            poutput.event_received = System.currentTimeMillis();
        }

        if ((pinput == null) && (poutput == null)) {
            MessageEvent e = new MessageEvent(MessageType.InvalidPinName);
            e.description = "received event from unknown pin";
            e.HWid = d.HWid;
            e.pinLabel = pinLabel;
            RGPIO.message(e);
            return;
        }

        if (pinput != null) { // event for an input pin

            if (pinput.vinput == null) {
                MessageEvent e = new MessageEvent(MessageType.InvalidPinName);
                e.description = "received event from unassigned device pin of type " + pinput.type.name();
                e.HWid = d.HWid;
                e.pinLabel = pinLabel;
                RGPIO.message(e);
            }

            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "Pinput value change";
            e.HWid = d.HWid;
            e.pinLabel = pinLabel;
            e.value = value;
            RGPIO.message(e);

            if (pinput.type == IOType.digitalInput) {
                if (!(value.equals("High")) && !(value.equals("Low"))) {
                    e.type = MessageType.InvalidPinValue;
                    e.description = "received event with invalid digital input value";
                    e.HWid = d.HWid;
                    e.pinLabel = pinLabel;
                    e.value = value;
                    RGPIO.message(e);
                    return;
                }
            } else if (pinput.type == IOType.analogInput) {
                try {
                    Float.parseFloat(value);
                } catch (NumberFormatException nfe) {
                    e.type = MessageType.InvalidPinValue;
                    e.description = "received event with invalid analog input value";
                    e.HWid = d.HWid;
                    e.pinLabel = pinLabel;
                    e.value = value;
                    RGPIO.message(e);
                    return;
                }
            } 

            pinput.setValue(value);
        }

        if (poutput != null) { // event for an output pin ( device sends EVENT when receiving SET )
            if (poutput.voutput == null) {
                MessageEvent e = new MessageEvent(MessageType.InvalidPinName);
                e.description = "received event from unassigned device pin of type " + poutput.type.name();
                e.HWid = d.HWid;
                e.pinLabel = pinLabel;
                RGPIO.message(e);
            }
            // nothing useful to do , only the event_received time is important
        }

    }
    
        public void deviceMessageHandler(String HWid, String message) {
        PDevice pdevice = this.get(HWid);
        if (pdevice == null) {
            MessageEvent e = new MessageEvent(MessageType.UnreportedDevice);
            e.description = "received message from an unreported device";
            e.HWid = HWid;
            e.message = message;
            RGPIO.message(e);
            return;
        } else if (pdevice.vdevice == null) {
                MessageEvent e = new MessageEvent(MessageType.InvalidPinName);
                e.description = "received event from unassigned device";
                e.HWid = pdevice.HWid;
                e.message=message;
                RGPIO.message(e);
            } else pdevice.vdevice.deliverMessage(message);

        pdevice.setActive();

    }


    public static void matchToGroup(PDevice device) {

        int nrInstances = 0;
        VDevice theOnlyVDevice = null;
        for (Map.Entry<String, VDevice> e : RGPIO.VDeviceMap.entrySet()) {
            VDevice vdevice = e.getValue();
            if (vdevice.matchesDevice(device.modelName, device.HWid)) {
                theOnlyVDevice = vdevice;
                nrInstances++;
            }
        }
        if (nrInstances == 1) {
            device.vdevice = theOnlyVDevice;

            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "PDevice assigned to VDevice";
            e.HWid = device.HWid;
            e.vdevice = device.vdevice.name;
            RGPIO.message(e);
        }
        if (nrInstances == 0) {
            MessageEvent e = new MessageEvent(MessageType.UnassignedDevice);
            e.description = "PDevice can not be matched to a VDevice";
            e.HWid = device.HWid;
            e.model = device.modelName;
            RGPIO.message(e);
        }
        if (nrInstances > 1) {
            MessageEvent e = new MessageEvent(MessageType.UnassignedDevice);
            e.description = "PDevice matches more than one VDevice";
            e.HWid = device.HWid;
            RGPIO.message(e);
        }
    }

}
