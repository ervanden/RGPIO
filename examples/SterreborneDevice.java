package examples;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.*;
import java.time.LocalDateTime;
import pidevice.*;


class SensorThread extends Thread {
    
    // thread that simulates a fluctuating sensor value

    int interval;

    public SensorThread(int interval) {
        super("SensorThread");
        this.interval = interval;
    }

    public void run() {
        // sensor value goes up and down between 0 and 30
        while (true) {
            try {
                Thread.sleep(interval * 1000);
                LocalDateTime now = LocalDateTime.now();
                int sec = now.getSecond();
                if (sec > 30) {
                    SterreborneDeviceRun.sensorValue = 60 - sec;
                } else {
                    SterreborneDeviceRun.sensorValue = sec;
                }
            } catch (InterruptedException ie) {
            }
        }
    }
}

class SterreborneDeviceRun implements SetCommandListener, GetCommandListener, GpioPinListenerDigital {

    /* This class implements a RGPIO device on Raspberry PI.
     Sending and receiving of REPORT,EVENT,GET,SET.. is handled by PiDevice().
     This class implements :
       - GpioListener to create an event when a physical GPIO input pin changes state.
       - SetCommandListener (onSetCommand()) to set a physical GPIO pin when a SET command is received.
       - GetCommandListener (onGetCommand()) to return the value of a pin on a GET command.
     */
    
    DeviceInput button;    // digital input
    DeviceOutput buzzer;   // digital output

    DeviceInput sensor;    // analog input
    DeviceOutput timer;    // analog output

    static Integer sensorValue = 0;

    // GPIO pins corresponding to the digital input and digital output
    static GpioController gpio;
    static GpioPinDigitalInput Gpio2;
    static GpioPinDigitalOutput Gpio3;

    public void onSetCommand(DeviceOutput deviceOutput, String newValue) {
        // set GPIO pin corresponding to this deviceOutput to newValue
        if (deviceOutput == buzzer) {
            System.out.println("GPIO 3 set to " + newValue);
        }
        if (deviceOutput == timer) {
            System.out.println("analog output <timer> set to " + newValue);
        }
    }

    public String onGetCommand(DeviceInput deviceInput) {
        // set GPIO pin corresponding to this deviceOutput to newValue
        if (deviceInput == button) {
            if (Gpio2.isHigh()) {
                return "High";
            }
            if (Gpio2.isLow()) {
                return "Low";
            }
            return ("?");   // impossible?
        }
        if (deviceInput == sensor) {
            return (sensorValue.toString());
        }
        return ("impossible!");
    }

    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        System.out.println("--> GPIO PIN <" + event.getPin().getName()
                + "> STATE CHANGE: " + event.getState());
//System.out.println("2? "+(event.getPin()==Gpio2));
//System.out.println("high? "+event.getState().isHigh());

        if (event.getState().isHigh()) {
            PiDevice.sendEvent("button", "High");
        } else {
            PiDevice.sendEvent("button", "Low");
        }
    }

    public void start() {
        gpio = GpioFactory.getInstance();
        Gpio2 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);
        Gpio3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "BUZZER", PinState.LOW);
        Gpio2.addListener(this);

        // pass the model and the device pins to piDevice
        // From this information a Report string can be generated
        // PiDevice will call onSetCommand() and onGetCommand() when GET or SET is received
        PiDevice.deviceModel = "RASPBERRY";
        button = PiDevice.addDigitalInput("button");
        buzzer = PiDevice.addDigitalOutput("buzzer");
        sensor = PiDevice.addAnalogInput("sensor");
        timer = PiDevice.addAnalogOutput("timer");
        PiDevice.printDevicePins();

        // 'this' can execute the GET and SET commands
        buzzer.setCommandListener = this;
        button.getCommandListener = this;
        timer.setCommandListener = this;
        sensor.getCommandListener = this;

        (new SensorThread(1)).start();  // reads and modifies the value of the sensor every second

        PiDevice.runDevice(2600,2500);

// todo : convert listener thread back to in line so runDevice does not exit
        
        try {
            Thread.sleep(999999999);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

public class SterreborneDevice {

    public static void main(String[] args) {
        new SterreborneDeviceRun().start();
    }
}
