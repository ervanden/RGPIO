package rgpioexample;

import rgpioutils.MessageListener;
import rgpioutils.MessageEvent;
import rgpio.*;

public class ExampleServer implements VInputEventListener, MessageListener {


    VDevice allDevices;
// digital test: Pressing any button toggles the lights
    VInput buttons;
    VOutput lights;
// analog test: When the button is pressed, read the sensor and set the timer    
    VInput sensor;
    VOutput timer;

    public void onInputEvent(VInput vinput) {

        if (vinput == buttons) {
            if (buttons.nrHigh() > 0) {
                if (lights.get().equals("High")) {
                    lights.set("Low");
                } else {
                    lights.set("High");
                }
                
                sensor.get();
                System.out.println("SENSOR avg = "+sensor.avg());
                timer.set(Float.toString(sensor.avg()));

            } else { // button released

            }
        }
    }

    public void onMessage(MessageEvent e) throws Exception {
        System.out.println(e.toString());

    }

    public void start() {

        String deviceFile;
        if (System.getProperty("file.separator").equals("/")) {
            deviceFile = System.getProperty("user.home") + "/RGPIO/devices.txt";
        } else {
            deviceFile = System.getProperty("user.home") + "\\Documents\\RGPIO\\devices.txt";
        }

        RGPIO.addMessageListener(this);
        RGPIO.initialize(deviceFile);

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

        buttons.addDigitalPinListener(this);

        // get and set all virtual io to be in a known state
        
        buttons.get();
                                sensor.get();
        lights.set("Low");
        timer.set("7777");


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
