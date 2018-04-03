package udputils;

import rgpio.PInput;
import rgpio.PDevice;
import utils.JSONString;

public class SendGetCommandThread extends Thread {

    PDevice device;
    PInput p;

    public SendGetCommandThread(PDevice device, PInput p) {
        super();
        this.device = device;
        this.p = p;
    }

    public void run() {

        JSONString json = new JSONString();
        json.addProperty("command", "GET");
        json.addProperty("from", "RGPIO");
        json.addProperty("to", device.HWid);
        json.addProperty("pin", p.name);

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
