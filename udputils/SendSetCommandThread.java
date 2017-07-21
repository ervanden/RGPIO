package udputils;

import rgpio.PDevice;

public class SendSetCommandThread extends Thread {

    PDevice device;
    String message;

    public SendSetCommandThread(PDevice device, String message) {
        super();
        this.device=device;
                this.message = message;
    }

    public void run() {
        device.sendToDevice(message);
        
        
    }
}

