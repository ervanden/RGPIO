package udputils;

import devices.*;

public class SendGetCommandThread extends Thread {

    Device device;
    DeviceDigitalInput dip;

    public SendGetCommandThread(Device device,DeviceDigitalInput dip) {
        super();
        this.device=device;
        this.dip=dip;
    }
    
    public void run() {
        dip.value=device.sendToDevice("Get/Dip:" + dip.name);       
    }
}

