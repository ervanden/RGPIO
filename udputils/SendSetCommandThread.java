package udputils;

import rgpio.PDevice;
import rgpio.POutput;
import utils.JSONString;

public class SendSetCommandThread extends Thread {

    PDevice device;
    POutput p;

    public SendSetCommandThread(PDevice device, POutput p) {
        super();
        this.device = device;
        this.p = p;

    }

    public void run() {

        JSONString json = new JSONString();
        json.addProperty("command", "SET");
        json.addProperty("from", "RGPIO");
        json.addProperty("to", device.HWid);
        json.addProperty("pin", p.name);
        json.addProperty("value", p.get_value());

        long set_sent = System.currentTimeMillis();
        int endLoop = 3;
        while (endLoop > 0) {
            device.sendToDevice(json.asString());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
            };
            endLoop = endLoop - 1;
            if (p.event_received>set_sent) {
                endLoop = 0;
            }
        }

    }
}
