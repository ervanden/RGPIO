package udputils;

import rgpio.IOType;
import rgpio.PInput;
import rgpio.PDevice;

public class SendGetCommandThread extends Thread {

    PDevice device;
    PInput ip;

    public SendGetCommandThread(PDevice device,PInput ip) {
        super();
        this.device=device;
        this.ip=ip;
    }
    
    public void run() {
        ip.set_value(device.sendToDevice("Get/"+IOType.longToShort(ip.type)+":" + ip.name));
    }
}

