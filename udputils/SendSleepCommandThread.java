package udputils;

import rgpio.PDevice;
import utils.JSONString;

public class SendSleepCommandThread extends Thread {

    PDevice device;
    Integer sleepTime;

    public SendSleepCommandThread(PDevice device,int sleepTime) {
        super();
        this.device=device;
        this.sleepTime=sleepTime;

    }

    public void run() {
        
        JSONString json = new JSONString();
        json.addProperty("destination", device.HWid);
        json.addProperty("command", "SLEEP");
        json.addProperty("sleeptime", sleepTime.toString());
 
        device.sendToDevice(json.asString());   
        
    }
}

