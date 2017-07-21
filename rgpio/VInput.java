package rgpio;

import utils.JSONObject;
import java.util.ArrayList;
import java.util.List;
import udputils.SendGetCommandThread;

public class VInput extends Selector {

    public String name;
    public IOType type;

//    public String value;
    public Integer nrHigh = 0;
    public Integer nrLow = 0;

    private String value;

    public Integer minMembers = null;

    public VInput(String name) {
        this.name = name;
        RGPIO.digitalInputMap.put(name, this);
    }

    public void set_value(String newValue) {
        value = newValue;
        RGPIO.updateFeed.writeToClients(toJSON());
    }

    public String get_value() {
        return value;
    }

    public String toJSON() {
        JSONObject json = new JSONObject();
        json.addProperty("object", "VIO");
        json.addProperty("name", name);
        json.addProperty("nrHigh", nrHigh.toString());
        json.addProperty("nrLow", nrLow.toString());
        json.addProperty("type", type.name());
        return json.asString();
    }

    public void get() {
        // send a GET command to the devices
        // this will update the value of the digital input of each device
        // Start all the GET commands in parallel and wait until all threads finished

        ArrayList<SendGetCommandThread> threads = new ArrayList<>();

        for (PDevice device : RGPIO.deviceMap.values()) {
            for (PInput dip : device.digitalInputs.values()) {
                if (dip.vinput == this) {
                    SendGetCommandThread t = new SendGetCommandThread(device, dip);
                    threads.add(t);
                    t.start();
                }
            }
        }

        System.out.println("waiting for all GET threads to finish...");
        for (SendGetCommandThread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ie) {
            };
        }
        System.out.println("... all GET threads finished");
    }

    private List<VInputEventListener> digitalListeners = new ArrayList<>();

    public void addDigitalPinListener(VInputEventListener toAdd) {
        digitalListeners.add(toAdd);
    }

    //  pass change of state to all the listeners
    public void stateChange(VInputEvent event) {

        nrHigh = 0;
        nrLow = 0;
//        System.out.println("---state change for digital input " + name);
        for (PDevice device : RGPIO.deviceMap.values()) {
            for (PInput dip : device.digitalInputs.values()) {
                if (dip.vinput == this) {
//                    System.out.println("---physical pin " + device.HWid + "." + dip.name);
//                    System.out.println("---physical pin value=" + dip.value);
                    if (dip.value != null) { // is null before first GET or EVENT
                        if (dip.value.equals("High")) {
                            nrHigh++;
                        }
                        if (dip.value.equals("Low")) {
                            nrLow++;
                        }
                    }
                }
            }
        }
        RGPIO.updateFeed.writeToClients(toJSON());

        for (VInputEventListener l : digitalListeners) {
            try {
                l.onInputEvent(event);
            } catch (Exception e) {
            }
        }
    }

}
