package rgpioexample;

import rgpioutils.MessageListener;
import rgpioutils.MessageEvent;
import rgpio.*;

public class ExampleServer implements VInputListener, MessageListener {


    VDevice allDevices;
    VDigitalInput buttons;
    VDigitalOutput lights;   
    VAnalogInput sensor;
    VAnalogOutput timer;

// when any button is pressed:
//  - the lights are toggled
//  - the timer is set to 2*the value of the sensor
    
    public void onInputEvent(VInput vinput) {

        if (vinput == buttons) {
            if (buttons.nrHigh() > 0) {
                if (lights.getState()) {
                    lights.setState(false);
                } else {
                    lights.setState(true);
                }
                
 //               sensor.get();

                timer.setValue(2*sensor.avg());

            } else { // button released

            }
        }
    }

    public void onMessage(MessageEvent e) throws Exception {
        System.out.println(e.toString());

    }

    public void start() {

        String configDir;
        if (System.getProperty("file.separator").equals("/")) {
            configDir = "/home/pi/git/RGPIO/run/";
        } else {
            configDir = System.getProperty("user.home") + "\\Documents\\RGPIO\\";
        }

        RGPIO.addMessageListener(this);
        RGPIO.initialize(configDir);

        // wait until TCPfeed and TCPclient listeners are ready
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
        }

        allDevices = RGPIO.VDevice("allDevices");
        buttons = RGPIO.VDigitalInput("button");
        lights = RGPIO.VDigitalOutput("light");
        sensor = RGPIO.VAnalogInput("sensor");
        timer = RGPIO.VAnalogOutput("timer");

        allDevices.minMembers = 2;
        buttons.minMembers = 2;
        lights.minMembers = 1;

        buttons.addVinputListener(this);
        
        lights.setState(true);
        timer.setValue(999);


        while (true) {
            try {
                Thread.sleep(3000);
                /*
                 RGPIO.receiveFromDevice("192.168.0.99", "Report/HWid:1625496/Model:RELAY/Uptime:10/Dop:0/Dip:2");
                 Thread.sleep(3000);
                 RGPIO.receiveFromDevice("192.168.0.99", "Event/HWid:1625496/Model:RELAY/Dip:2/Value:Low");
                 Thread.sleep(3000);
                 RGPIO.receiveFromDevice("192.168.0.99", "Event/HWid:1625496/Model:RELAY/Dip:2/Value:High");
                 */
            } catch (InterruptedException ie) {
            }
        }
    }
}
