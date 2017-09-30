package examples;

import rgpio.*;

class SterreborneRun implements VInputListener, MessageListener {

    VDevice allDevices;
    VDigitalInput button;
    VDigitalOutput light;
    VDigitalOutput pump;
    VDigitalOutput heating;
    VAnalogInput level;
    VAnalogInput temp;

// when any button is pressed,  the lights are toggled
    
    public void onInputEvent(VInput vinput) {

        if (vinput == button) {
            if (button.nrHigh() > 0) {
                if (light.getState()) {
                    light.setState(false);
                } else {
                    light.setState(true);
                }
            } else { // button released

            }
        }
    }

    public void onMessage(MessageEvent e) throws Exception {
        System.out.println(e.toString());

    }

    public void start() {

        RGPIO.addMessageListener(this);
        RGPIO.initialize("/home/pi/RGPIO/");

        allDevices = RGPIO.VDevice("allDevices");
        button = RGPIO.VDigitalInput("button");
        light = RGPIO.VDigitalOutput("light");
        heating = RGPIO.VDigitalOutput("heating");
        pump = RGPIO.VDigitalOutput("pump");
        temp = RGPIO.VAnalogInput("temp");
        level = RGPIO.VAnalogInput("level");

        allDevices.minMembers = 2;
        button.minMembers = 2;
        light.minMembers = 1;

        button.addVinputListener(this);

        light.setState(true);

        while (true) {
            try {
                Thread.sleep(2000);
                temp.get();
                if (temp.avg() <= 18) {
                    heating.setState(true);
                }
                if (temp.avg() >= 22) {
                    heating.setState(false);
                }

                level.get();
                if (level.avg() <= 20) {
                    pump.setState(true);
                }
                if (level.avg() >= 50) {
                    pump.setState(false);
                }

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
