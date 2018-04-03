package udputils;

import rgpio.PInput;
import rgpio.PDevice;
import utils.JSONString;

public class SendGetCommandThread extends Thread {

    PDevice device;
    PInput ip;

    public SendGetCommandThread(PDevice device,PInput ip) {
        super();
        this.device=device;
        this.ip=ip;
    }
    
    public void run() {
        
        JSONString json = new JSONString();
        json.addProperty("to", device.HWid);
                json.addProperty("from", "RGPIO");
        json.addProperty("command", "GET");
        json.addProperty("pin", ip.name);       
 
        device.sendToDevice(json.asString());
    }
}

