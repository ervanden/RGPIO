package rgpio;

import utils.JSONString;
import java.util.ArrayList;
import java.util.List;
import udputils.SendGetCommandThread;

public class VInput extends VSelector {

    public String name;
    public IOType type;
    public String value;
    public Integer minMembers = null;

    public String get_value() {
        return value;
    }

    public String toJSON() {
        JSONString json = new JSONString();
        json.addProperty("object", "VIO");
        json.addProperty("name", name);
        json.addProperty("type", type.name());
        if (type == IOType.digitalInput) {
            json.addProperty("nrHigh", nrHigh().toString());
            json.addProperty("nrLow", nrLow().toString());
        }
        if (type == IOType.analogInput) {
            json.addProperty("avg", avg().toString());
            json.addProperty("min", min().toString());
            json.addProperty("max", max().toString());
        }
        return json.asString();
    }

    public void get() {
        // send a GET command to the devices
        // this will update the value of the digital input of each device
        // Start all the GET commands in parallel and wait until all threads finished

        ArrayList<SendGetCommandThread> threads = new ArrayList<>();

//        System.out.println("creating GET threads...");
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput ip : device.inputs.values()) {
                if (ip.vinput == this) {
                    SendGetCommandThread t = new SendGetCommandThread(device, ip);
                    threads.add(t);
                    t.start();
                }
            }
        }

//        System.out.println("waiting for all GET threads to finish...");
        for (SendGetCommandThread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ie) {
            };
        }
//        System.out.println("... all GET threads finished");

        // send change of value to updateFeed
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput ip : device.inputs.values()) {
                if (ip.vinput == this) {
                    RGPIO.updateFeed.writeToClients(ip.toJSON());
                }
            }
        }
        RGPIO.updateFeed.writeToClients(toJSON());
    }

    private List<VInputListener> listeners = new ArrayList<>();

    public void addVinputListener(VInputListener toAdd) {
        listeners.add(toAdd);
    }

    public void pinValueChange() {

        /* the value of one of the pins in the vinput changed (after EVENT or GET)
         - forward the the web interface
         - forward to the application via the listener
         */
        RGPIO.updateFeed.writeToClients(toJSON());

        for (VInputListener l : listeners) {
            try {
                l.onInputEvent(this);
            } catch (Exception e) {
            }
        }
    }

    public Integer nrHigh() {
        int nrHigh = 0;
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput dip : device.inputs.values()) {
                if (dip.vinput == this) {
//                    System.out.println("---physical pin " + device.HWid + "." + dip.name);
//                    System.out.println("---physical pin value=" + dip.value);
                    if (dip.value != null) { // is null before first GET or EVENT
                        if (dip.value.equals("High")) {
                            nrHigh++;
                        }
                    }
                }
            }
        }
        return nrHigh;
    }

    public Integer nrLow() {
        int nrLow = 0;
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput dip : device.inputs.values()) {
                if (dip.vinput == this) {
//                    System.out.println("---physical pin " + device.HWid + "." + dip.name);
//                    System.out.println("---physical pin value=" + dip.value);
                    if (dip.value != null) { // is null before first GET or EVENT
                        if (dip.value.equals("Low")) {
                            nrLow++;
                        }
                    }
                }
            }
        }
        return nrLow;
    }

    public Float avg() {
        float sum = 0;
        int n = 0;
 //       System.out.println("---AVG calculation for " + name);
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput dip : device.inputs.values()) {
                if (dip.vinput == this) {
//                    System.out.println("---physical pin " + device.HWid + "." + dip.name);
//                    System.out.println("---physical pin value=" + dip.value);
                    if (dip.value != null) { // is null before first GET or EVENT
//                        if (!dip.value.equals("UNKNOWN")) { // is UNKNOWN after device was NOTRESPONDING
                            float f = Float.parseFloat(dip.value);
                            sum = sum + f;
                            n = n + 1;
//                        }
                    }
                }
            }
        }
        if (n > 0) {
            return sum / n;
        } else {
            return Float.MAX_VALUE;
        }
    }

    public Float min() {
        float result = Float.MAX_VALUE;
//        System.out.println("---MIN calculation for " + name);
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput dip : device.inputs.values()) {
                if (dip.vinput == this) {
 //                   System.out.println("---physical pin " + device.HWid + "." + dip.name);
 //                   System.out.println("---physical pin value=" + dip.value);
                    if (dip.value != null) { // is null before first GET or EVENT
 //                       if (!dip.value.equals("UNKNOWN")) { // is UNKNOWN after device was NOTRESPONDING
                            float f = Float.parseFloat(dip.value);
                            if (f < result) {
                                result = f;
//                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public Float max() {
        float result = Float.MIN_VALUE;
//        System.out.println("---MAX calculation for " + name);
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput dip : device.inputs.values()) {
                if (dip.vinput == this) {
//                    System.out.println("---physical pin " + device.HWid + "." + dip.name);
//                    System.out.println("---physical pin value=" + dip.value);
                    if (dip.value != null) { // is null before first GET or EVENT
 //                       if (!dip.value.equals("UNKNOWN")) { // is UNKNOWN after device was NOTRESPONDING
                            float f = Float.parseFloat(dip.value);
                            if (f > result) {
                                result = f;
 //                           }
                        }
                    }
                }
            }
        }
        return result;
    }
}
