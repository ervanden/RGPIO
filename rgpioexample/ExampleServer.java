package rgpioexample;

import rgpioutils.MessageListener;
import rgpioutils.MessageEvent;
import rgpio.*;

public class ExampleServer implements VInputEventListener, MessageListener {

    /*
     Pressing any button switches the lights off if they were on, and off if they were on.
     PIRDevices is only used to get a message if there are less than 2 PIR's.
     */
    VDevice allDevices;
    VInput buttons;
    VOutput lights;

    public void onInputEvent(VInputEvent event) {

        if (event.rgpioInput == buttons) {
            if (event.rgpioInput.nrHigh > 0) {
                if (lights.get().equals("High")) {
                    lights.set("Low");
                } else {
                    lights.set("High");
                }

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

        allDevices.minMembers = 2;
        buttons.minMembers = 2;
        lights.minMembers = 1;

        buttons.addDigitalPinListener(this);

        // switch all lights off to be in a known state
        lights.set("Low");

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
//            System.out.println("GET button value = "+buttons.);
        }
        /*
         while (true) {
         try {
         Thread.sleep(3000);
         lights.set("High");
         Thread.sleep(3000);
         lights.set("Low");
         System.out.println();
         RGPIO.printMaps("");
         System.out.println();
         } catch (InterruptedException ie) {
         }
         }
         */
    }
}
