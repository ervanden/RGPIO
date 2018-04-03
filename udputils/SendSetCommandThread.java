package udputils;

import rgpio.PDevice;
import rgpio.POutput;
import utils.JSONString;

public class SendSetCommandThread extends Thread {

    PDevice device;
    POutput p;

    public SendSetCommandThread(PDevice device,POutput p) {
        super();
        this.device=device;
        this.p = p;

    }

    public void run() {
        
        JSONString json = new JSONString();
        json.addProperty("to", device.HWid);
                json.addProperty("from", "RGPIO");
        json.addProperty("command", "SET");
        json.addProperty("pin", p.name); 
        json.addProperty("value", p.get_value());
 
        device.sendToDevice(json.asString());   
        
    }
}

