package devices;

import rgpio.*;
import utils.*;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/*
 deviceMap entry syntax

 HWid:deviceModelName:groupName

 'deviceGroup' is the modelName by which this device is referred to in the application

 */
public class DeviceMap extends ConcurrentHashMap<String, Device> {

    // key is HWid
    // concurrent because devices can report and be added to the map at any time
    String deviceMapFileName;
    int unknownDeviceCounter = 0;

    public DeviceMap() {
        super();
        Console.verbose(true);
    }

    public void print() {

        String formatString = "%12s %12s  %13s  %12s  %12s  %12s\n";
        System.out.printf(formatString,
                "HWid", "deviceGroup", "status", "ipAddress", "lastContact", "powerOn");
        for (Device d : this.values()) {
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

        Device d = get(HWid);
        if (d == null) {
            // device does not yet exist. Create it and match to an device group
            d = new Device();
            RGPIO.deviceMap.put(HWid, d);
            d.HWid = HWid;
            d.groupName = null;
            d.modelName = modelName;
            d.set_status(DeviceStatus.UNASSIGNED);
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

        Device d = this.get(HWid);
        if (d == null) {
            RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.UnreportedDevice);
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
            RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.InvalidPinName);
            e.description = "received event from unknown digital input pin";
            e.HWid = d.HWid;
            e.pinLabel = pinLabel;
            RGPIO.message(e);
            return;
        }

        if (!(value.equals("High")) && !(value.equals("Low"))) {
            RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.InvalidPinValue);
            e.description = "received event with invalid digital input value";
            e.HWid = d.HWid;
            e.pinLabel = pinLabel;
            e.state = value;
            RGPIO.message(e);
            return;
        }
        
        
        deviceDigitalInput.set_value(value);

        if (deviceDigitalInput.vinput == null) {
            RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.InvalidPinName);
            e.description = "received event from unassigned digital input pin";
            e.HWid = d.HWid;
            e.pinLabel = pinLabel;
            RGPIO.message(e);
            return;
        }

        RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.Info);
        e.description = "state change";
        e.HWid = d.HWid;
        e.pinLabel = pinLabel;
        e.state = value;
        RGPIO.message(e);

        RGPIOInputEvent ev = new RGPIOInputEvent();
        ev.device = d;
        ev.rgpioInput = deviceDigitalInput.vinput;
        ev.rgpioInput.stateChange(ev);
    }

    public static void matchToGroup(Device device) {

        int nrInstances = 0;
        RGPIODeviceGroup theOnlyDeviceGroup = null;
        for (Map.Entry<String, RGPIODeviceGroup> e : RGPIO.deviceGroupMap.entrySet()) {
            RGPIODeviceGroup deviceGroup = e.getValue();
            if (deviceGroup.matchesDevice(device.modelName, device.HWid)) {
                theOnlyDeviceGroup = deviceGroup;
                nrInstances++;
            }
        }
        if (nrInstances == 1) {
            device.groupName = theOnlyDeviceGroup.name;
            device.deviceGroup = theOnlyDeviceGroup;

            RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.Info);
            e.description = "device assigned to group";
            e.HWid = device.HWid;
            e.group = device.groupName;
            RGPIO.message(e);
        }
        if (nrInstances == 0) {
            RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.UnassignedDevice);
            e.description = "device can not be assigned to a group";
            e.HWid = device.HWid;
            e.model = device.modelName;
            RGPIO.message(e);
        }
        if (nrInstances > 1) {
            RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.UnassignedDevice);
            e.description = "device matches more than one group";
            e.HWid = device.HWid;
            RGPIO.message(e);
        }
    }

}
