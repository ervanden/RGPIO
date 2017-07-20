package udputils;

import devices.*;

public class SendGetCommandThread extends Thread {

    Device device;
    PInput dip;

    public SendGetCommandThread(Device device,PInput dip) {
        super();
        this.device=device;
        this.dip=dip;
    }
    
    public void run() {
        dip.value=device.sendToDevice("Get/Dip:" + dip.name);       
    }
}

