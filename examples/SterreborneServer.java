
package examples;

import rgpio.*;

/* Simple program that uses RGPIO
   Create a project with this as only java file. Call the package 'sterreborne' instead of 'example'.
   Copy RGPIO.jar from the PI to be able to compile the java code.
   Create a jar file e.g. SterreborneServer.jar and copy to the PI.
   It will run using the following command :

java -cp ./Sterreborne.jar:/home/pi/git/RGPIO/lib/'*' sterreborne/Sterreborne
 
*/

/* The program that implements the control logic should have classes that implement the following listeners:
   
MessageListener : code that is called when the RGPIO library emits a message.
VInputListener : code that is called when RGPIO receives an EVENT from a device

In this example, alle events and messages are handled by SterreborneRun

*/
class SterreborneRun implements VInputListener, MessageListener {

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
            } else { // button released

            }
        }
    }
    
        public void onMessage(MessageEvent e) throws Exception {
        System.out.println(e.toString());

    }



    public void start() {

        // call the listener first to capture also messages during initialization
        RGPIO.addMessageListener(this);
        // the argument is the directory with devices.txt and RGPIO.txt 
        RGPIO.initialize("/home/pi/RGPIO/");
        
        // declare the The VDEV and VIO's that are defined in devices.txt
        // by referring to their name in this file.
        allDevices = RGPIO.VDevice("allDevices");
        buttons = RGPIO.VDigitalInput("button");
        lights = RGPIO.VDigitalOutput("light");
        sensor = RGPIO.VAnalogInput("sensor");
        timer = RGPIO.VAnalogOutput("timer");

        allDevices.minMembers = 2;
        buttons.minMembers = 2;
        lights.minMembers = 1;

        buttons.addVinputListener(this);  // 'this' is the object that handles events from 'buttons'.
        
        lights.setState(true);
        timer.setValue(999);


        while (true) {
            try {
                Thread.sleep(5000);
                sensor.get();
                timer.setValue(2*sensor.avg());
            } catch (InterruptedException ie) {
            }
        }
    }

}


public class SterreborneServer {
    public static void main(String[] args) {
      new SterreborneRun().start();
    }
}

