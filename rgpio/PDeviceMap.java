package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageEvent;
import rgpio.PDevice;
import utils.TimeStamp;
import rgpio.*;
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
            // device does not yet exist. Create it and match to an device group
            d = new PDevice();
            RGPIO.deviceMap.put(HWid, d);
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
            e.state = value;
            RGPIO.message(e);
            return;
        }

        d.setActive();
        PInput deviceDigitalInput = d.digitalInputs.get(pinLabel);
        if (deviceDigitalInput == null) {
            MessageEvent e = new MessageEvent(MessageType.InvalidPinName);
            e.description = "received event from unknown digital input pin";
            e.HWid = d.HWid;
            e.pinLabel = pinLabel;
            RGPIO.message(e);
            return;
        }

        if (!(value.equals("High")) && !(value.equals("Low"))) {
            MessageEvent e = new MessageEvent(MessageType.InvalidPinValue);
            e.description = "received event with invalid digital input value";
            e.HWid = d.HWid;
            e.pinLabel = pinLabel;
            e.state = value;
            RGPIO.message(e);
            return;
        }
        
        
        deviceDigitalInput.set_value(value);

        if (deviceDigitalInput.vinput == null) {
            MessageEvent e = new MessageEvent(MessageType.InvalidPinName);
            e.description = "received event from unassigned digital input pin";
            e.HWid = d.HWid;
            e.pinLabel = pinLabel;
            RGPIO.message(e);
            return;
        }

        MessageEvent e = new MessageEvent(MessageType.Info);
        e.description = "state change";
        e.HWid = d.HWid;
        e.pinLabel = pinLabel;
        e.state = value;
        RGPIO.message(e);

        VInputEvent ev = new VInputEvent();
        ev.device = d;
        ev.rgpioInput = deviceDigitalInput.vinput;
        ev.rgpioInput.stateChange(ev);
    }

    public static void matchToGroup(PDevice device) {

        int nrInstances = 0;
        VDevice theOnlyDeviceGroup = null;
        for (Map.Entry<String, VDevice> e : RGPIO.deviceGroupMap.entrySet()) {
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
            e.group = device.groupName;
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
