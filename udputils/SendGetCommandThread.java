package udputils;

import rgpio.IOType;
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
 //       ip.setValue(device.sendToDevice("Get/"+IOType.longToShort(ip.type)+":" + ip.name));
        
        JSONString json = new JSONString();
        json.addProperty("destination", device.HWid);
        json.addProperty("command", "GET");
        json.addProperty("pin", ip.name);       
 
        ip.setValue(device.sendToDevice(json.asString()));
    }
}

