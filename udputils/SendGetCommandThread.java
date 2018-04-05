package udputils;

import rgpio.*;
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

        long commandSent = System.currentTimeMillis();
        Integer retry = 0;
        while ((retry <= 3) && !(p.event_received > commandSent)) {
            JSONString json = new JSONString();
            json.addProperty("command", "GET");
                                   json.addProperty("id", RGPIO.msgId());
            json.addProperty("from", "RGPIO");
            json.addProperty("to", device.HWid);
            json.addProperty("retry", retry.toString());
            json.addProperty("pin", p.name);
            device.sendToDevice(json.asString());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
            };
            retry = retry + 1;
        }

    }
}
