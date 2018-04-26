package rgpio;

import java.util.ArrayList;
import udputils.SendGetCommandThread;
import utils.JSONString;
import udputils.SendSetCommandThread;

public class VOutput extends VIO {

    public String value;
    public Integer members = 0;
    public Integer minMembers = null;

    public String toJSON() {
        JSONString json = new JSONString();
        json.addProperty("object", "VIO");
        json.addProperty("name", name);
        json.addProperty("members", members.toString());
        json.addProperty("value", value);
        json.addProperty("type", type.name());
        return json.asString();
    }

    public void set(String newValue) {

        value = newValue;
        // web update not needed, will receive an EVENT
        //       RGPIO.webSocketServer.sendToAll(toJSON());

        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (POutput p : device.outputs.values()) {
                if (p.voutput == this) {
                    p.set_value(newValue);
                    //new SendSetCommandThread(device, p,0).start();
                    device.sendSetCommand(p);
                }
            }
        }
    }

    public void setSync(String newValue) {

        value = newValue;

        // send a SET command to the devices
        // Start all the SET commands in parallel and wait until all threads finished
        ArrayList<SendSetCommandThread> threads = new ArrayList<>();
        // "creating SET threads..."
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (POutput p : device.outputs.values()) {
                if (p.voutput == this) {
                    p.set_value(newValue);
                    SendSetCommandThread t = new SendSetCommandThread(device, p, 3);
                    threads.add(t);
                    t.start();
                }
            }
        }

        // "waiting for all GET threads to finish..."
        for (SendSetCommandThread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ie) {
            };
        }

        //"... all SET threads finished"
        // send change of value to web interface
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (POutput p : device.outputs.values()) {
                if (p.voutput == this) {
                    RGPIO.webSocketServer.sendToAll(p.toJSON());
                }
            }
        }
        RGPIO.webSocketServer.sendToAll(toJSON());
    }

    public void countMembers() {
        int m = 0;
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (POutput p : device.outputs.values()) {
                if (p.voutput == this) {
//                    System.out.println("  device " + device.HWid + " pin " + p.name + " " + device.get_status().toString());
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

}
