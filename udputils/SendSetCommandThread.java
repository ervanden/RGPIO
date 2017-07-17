package udputils;

import devices.*;

public class SendSetCommandThread extends Thread {

    Device device;
    String message;

    public SendSetCommandThread(Device device, String message) {
        super();
        this.device=device;
                this.message = message;
    }

    public void run() {
        device.sendToDevice(message);
        
        
    }
}

