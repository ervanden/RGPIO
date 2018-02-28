package rgpio;

import utils.JSONString;
import java.util.ArrayList;
import java.util.List;
import udputils.SendGetCommandThread;

public class VInput extends VIO {

    public Integer members = 0;
    public Integer minMembers = null;

    private String integerToString(Integer i) {
        if (i == null) {
            return "NaN";
        } else {
            return i.toString();
        }
    }

    public String toJSON() {
        JSONString json = new JSONString();
        json.addProperty("object", "VIO");
        json.addProperty("name", name);
        json.addProperty("members", members.toString());
        json.addProperty("type", type.name());
        if (type == IOType.digitalInput) {
            json.addProperty("nrHigh", nrHigh().toString());
            json.addProperty("nrLow", nrLow().toString());
        }
        if (type == IOType.analogInput) {
            json.addProperty("avg", integerToString(avg()));
            json.addProperty("min", integerToString(min()));
            json.addProperty("max", integerToString(max()));
        }
        return json.asString();
    }

    public void get() {
        // send a GET command to the devices
        // this will update the value of the input pin of each device
        // Start all the GET commands in parallel and wait until all threads finished

        ArrayList<SendGetCommandThread> threads = new ArrayList<>();
        // "creating GET threads..."
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput ip : device.inputs.values()) {
                if (ip.vinput == this) {
                    SendGetCommandThread t = new SendGetCommandThread(device, ip);
                    threads.add(t);
                    t.start();
                }
            }
        }

        // "waiting for all GET threads to finish..."
        for (SendGetCommandThread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ie) {
            };
        }

        //"... all GET threads finished"
        // send change of values (avg,min,max) to web interface
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput ip : device.inputs.values()) {
                if (ip.vinput == this) {
                    RGPIO.webSocketServer.sendToAll(ip.toJSON());
                }
            }
        }
        RGPIO.webSocketServer.sendToAll(toJSON());
    }

    private List<VInputListener> listeners = new ArrayList<>();

    public void addVinputListener(VInputListener toAdd) {
        listeners.add(toAdd);
    }

    public void pinValueChange() {
        /* the value of one of the pins in the vinput changed (after EVENT or GET)
         - forward to the web interface
         - forward to the application via the listener
         */
        RGPIO.webSocketServer.sendToAll(toJSON());

        for (VInputListener l : listeners) {
            try {
                l.onInputEvent(this);
            } catch (Exception e) {
            }
        }
    }

    public void countMembers() {
        int m = 0;
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput p : device.inputs.values()) {
                if (p.vinput == this) {
                    if (device.get_status() == PDeviceStatus.ACTIVE) {
                        m++;
                    }
                }
            }
        }
        if (members != m) {
            members = m;
            RGPIO.webSocketServer.sendToAll(toJSON());
        }
    }

    // when a SET or GET time out, the value of the pin is set to null (return value of sendToDevice())
    // so pins on non-responding devices are not taken into account in all the functions below
    public Integer nrHigh() {
        int nrHigh = 0;
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput dip : device.inputs.values()) {
                if (dip.vinput == this) {
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

    // for avg(), min(), max()   Integer.MIN_VALUE or MAXVALUE mean : NaN
    public Integer avg() {
        float sum = 0;
        int n = 0;
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput dip : device.inputs.values()) {
                if (dip.vinput == this) {
                    if (dip.value != null) { // is null before first GET or EVENT
                        float f = Float.parseFloat(dip.value);
                        sum = sum + f;
                        n = n + 1;
                    }
                }
            }
        }
        if (n > 0) {
            return Math.round(sum / n);
        } else {
            return null;
        }
    }

    public Integer min() {
        Integer result = Integer.MAX_VALUE;
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput dip : device.inputs.values()) {
                if (dip.vinput == this) {
                    if (dip.value != null) { // is null before first GET or EVENT
                        int f = Integer.parseInt(dip.value);
                        if (f < result) {
                            result = f;
                        }
                    }
                }
            }
        }
        if (result < Integer.MAX_VALUE) {
            return result;
        } else {
            return null;
        }

    }

    public Integer max() {
        Integer result = Integer.MIN_VALUE;
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput dip : device.inputs.values()) {
                if (dip.vinput == this) {
                    if (dip.value != null) { // is null before first GET or EVENT
                        int f = Integer.parseInt(dip.value);
                        if (f > result) {
                            result = f;
                        }
                    }
                }
            }
        }
        if (result > Integer.MIN_VALUE) {
            return result;
        } else {
            return null;
        }
    }

}
