package udputils;

import rgpio.PInput;
import rgpio.PDevice;

public class SendGetCommandThread extends Thread {

    PDevice device;
    PInput dip;

    public SendGetCommandThread(PDevice device,PInput dip) {
        super();
        this.device=device;
        this.dip=dip;
    }
    
    public void run() {
        dip.value=device.sendToDevice("Get/Dip:" + dip.name);       
    }
}

