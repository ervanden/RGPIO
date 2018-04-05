package udputils;

import rgpio.*;
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

        long commandSent = System.currentTimeMillis();
        Integer retry = 0;
        while ((retry <= 3) && !(p.event_received > commandSent)) {
            JSONString json = new JSONString();
            json.addProperty("command", "SET");
                        json.addProperty("id", RGPIO.msgId());
            json.addProperty("from", "RGPIO");
            json.addProperty("to", device.HWid);
            json.addProperty("retry", retry.toString());
            json.addProperty("pin", p.name);
            json.addProperty("value", p.get_value());
            device.sendToDevice(json.asString());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
            };
            retry = retry + 1;
        }

    }
}
