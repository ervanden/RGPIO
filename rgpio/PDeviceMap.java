package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageEvent;
import utils.TimeStamp;
import utils.*;

import java.util.Calendar;
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

    public void print() {

        String formatString = "%12s %12s  %13s  %12s  %12s  %12s\n";
        System.out.printf(formatString,
                "HWid", "deviceGroup", "status", "ipAddress", "lastContact", "powerOn");
        for (PDevice d : this.values()) {
            String HWid = d.HWid;
            String status = d.get_status().name();
            String deviceGroupName=null;
            if (d.vdevice!=null) deviceGroupName = d.vdevice.name;
            String ipAddress = d.ipAddress;
            String lastContact = "" + d.lastContact.asString();
            String powerOn = "" + d.powerOn.asString();
            System.out.printf(formatString,
                    HWid, deviceGroupName, status, ipAddress, lastContact, powerOn);
        }
    }

    public void deviceReported(
            String HWid,
            String modelName,
            String ipAddress,
            int upTime) {

        PDevice d = get(HWid);
        if (d == null) {
            // pdevice does not yet exist. Create it and match to a vdevice
            d = new PDevice();
            RGPIO.PDeviceMap.put(HWid, d);
            d.HWid = HWid;
            d.vdevice = null;
            d.modelName = modelName;
            this.put(HWid, d);
            matchToGroup(d);
        }
        d.ipAddress = ipAddress;
        d.lastContact = new TimeStamp();  //now
        d.powerOn = new TimeStamp();
        d.powerOn.add(Calendar.SECOND, -upTime);
        d.setActive();
    }

    public void deviceReportedPinEvent(
            String HWid,
            String pinLabel,
            String value) {
System.out.println("deviceReportedPinEvent() "+HWid+" "+pinLabel+" "+value);
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
        if (pinput == null) {
            pinput = d.digitalInputs.get(pinLabel);
        }
        if (pinput == null) {
            pinput = d.analogInputs.get(pinLabel);
        }
        if (pinput == null) {
            pinput = d.stringInputs.get(pinLabel);
        }
        if (pinput == null) {
            MessageEvent e = new MessageEvent(MessageType.InvalidPinName);
            e.description = "received event from unknown input pin";
            e.HWid = d.HWid;
            e.pinLabel = pinLabel;
            RGPIO.message(e);
            return;
        }

        if (pinput.vinput == null) {
            MessageEvent e = new MessageEvent(MessageType.InvalidPinName);
            e.description = "received event from unassigned device pin of type "+pinput.type.name();
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
        } else if (pinput.type == IOType.stringInput) {

        }

        pinput.setDebounced(value);

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
