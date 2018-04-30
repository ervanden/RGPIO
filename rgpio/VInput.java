package rgpio;

import utils.JSONString;
import java.util.ArrayList;
import java.util.List;
import udputils.SendGetCommandThread;
import udputils.SendSetCommandThread;

public class VInput extends VIO {

    public Integer members = 0;
    public Integer minMembers = null;

    
    /* 
       physical devices return a string in response to GET of an analog pin
       This string is stored in the Pinput object.
       Exernally the value of an analog input is used via the functions avg, min, max
       The string is converted to integer in these functions.
       Any string that can not be converted to a number is ignored. If none of the physical pins
       have a numerical value, avg,min,max return null. 
       null is converted to "NaN" in the JSON representation that is sent to the web interface
    
       So a device should not send 0 or 999 if it has no value. It should send "NaN" or any other string
       that does not have the number format
    */


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

        return json.asString();
    }

    public void get() {
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput ip : device.inputs.values()) {
                if (ip.vinput == this) {
 //                   new SendGetCommandThread(device, ip,0).start();
                    device.sendGetCommand(ip);
                }
            }
        }
    }
        
        
        
    public void getSync() {
        // send a GET command to the devices
        // this will update the value of the input pin of each device
        // Start all the GET commands in parallel and wait until all threads finished

        ArrayList<SendGetCommandThread> threads = new ArrayList<>();
        // "creating GET threads..."
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput ip : device.inputs.values()) {
                if (ip.vinput == this) {
                    SendGetCommandThread t = new SendGetCommandThread(device, ip,3);
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

    // when a SET or GET time out, the value of the pin is set to null (return value of sendPacket())
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



}
