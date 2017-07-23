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
            String deviceGroupName = d.groupName;
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
            // device does not yet exist. Create it and match to an device vdevice
            d = new PDevice();
            RGPIO.PDeviceMap.put(HWid, d);
            d.HWid = HWid;
            d.groupName = null;
            d.modelName = modelName;
            d.set_status(PDeviceStatus.UNASSIGNED);
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
            e.description = "received event from unknown digital input pin";
            e.HWid = d.HWid;
            e.pinLabel = pinLabel;
            RGPIO.message(e);
            return;
        }

        if (pinput.vinput == null) {
            MessageEvent e = new MessageEvent(MessageType.InvalidPinName);
            e.description = "received event from unassigned digital input pin";
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
        VDevice theOnlyDeviceGroup = null;
        for (Map.Entry<String, VDevice> e : RGPIO.VDeviceMap.entrySet()) {
            VDevice deviceGroup = e.getValue();
            if (deviceGroup.matchesDevice(device.modelName, device.HWid)) {
                theOnlyDeviceGroup = deviceGroup;
                nrInstances++;
            }
        }
        if (nrInstances == 1) {
            device.groupName = theOnlyDeviceGroup.name;
            device.deviceGroup = theOnlyDeviceGroup;

            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "device assigned to group";
            e.HWid = device.HWid;
            e.vdevice = device.groupName;
            RGPIO.message(e);
        }
        if (nrInstances == 0) {
            MessageEvent e = new MessageEvent(MessageType.UnassignedDevice);
            e.description = "device can not be assigned to a group";
            e.HWid = device.HWid;
            e.model = device.modelName;
            RGPIO.message(e);
        }
        if (nrInstances > 1) {
            MessageEvent e = new MessageEvent(MessageType.UnassignedDevice);
            e.description = "device matches more than one group";
            e.HWid = device.HWid;
            RGPIO.message(e);
        }
    }

}
