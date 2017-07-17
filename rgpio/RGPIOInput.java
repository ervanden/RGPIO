package rgpio;

import devices.*;
import java.util.ArrayList;
import java.util.List;
import udputils.SendGetCommandThread;

public class RGPIOInput extends RGPIOSelector {

    public String name;
    public RGPIOIOType type;

//    public String value;
    public Integer nrHigh = null;
    public Integer nrLow = null;

    private String value;

    public Integer minMembers = null;

    public RGPIOInput(String name) {
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
        JSONString json = new JSONString();
        json.addString("object", "VIO");
        json.addString("name", name);
        json.addString("nrHigh", nrHigh.toString());
        json.addString("nrLow", nrLow.toString());
        json.addString("type", type.name());
        return json.close();
    }

    public void get() {
        // send a GET command to the devices
        // this will update the value of the digital input of each device
        // Start all the GET commands in parallel and wait until all threads finished

        ArrayList<SendGetCommandThread> threads = new ArrayList<>();

        for (Device device : RGPIO.deviceMap.values()) {
            for (DeviceDigitalInput dip : device.digitalInputs.values()) {
                if (dip.digitalInput == this) {
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

    private List<RGPIOInputEventListener> digitalListeners = new ArrayList<>();

    public void addDigitalPinListener(RGPIOInputEventListener toAdd) {
        digitalListeners.add(toAdd);
    }

    //  pass change of state to all the listeners
    public void stateChange(RGPIOInputEvent event) {


        int nrHigh = 0;
        int nrLow = 0;

        for (Device device : RGPIO.deviceMap.values()) {
            for (DeviceDigitalInput dip : device.digitalInputs.values()) {
                if (dip.digitalInput == this) {
                    if (dip.value.equals("High")) {
                        nrHigh++;
                    }
                    if (dip.value.equals("Low")) {
                        nrLow++;
                    }
                }
            }
        }
        RGPIO.updateFeed.writeToClients(toJSON());




        for (RGPIOInputEventListener l : digitalListeners) {
            try {
                l.onInputEvent(event);
            } catch (Exception e) {
            }
        }
    }

}
