package rgpioexample;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.*;
import java.time.LocalDateTime;
import pidevice.*;

class SensorThread extends Thread {
    
    int interval;

    public SensorThread(int interval) {
        super("SensorThread");
        this.interval=interval;
    }

    public void run() {
        // sensor value goes up and down between 0 and 30
        while (true) {
            try {
                Thread.sleep(interval*1000);
                LocalDateTime now = LocalDateTime.now();
                int sec = now.getSecond();
                if (sec > 30) {
                    ExampleDevice.sensorValue = 60 - sec;
                } else {
                    ExampleDevice.sensorValue = sec;
                }
            } catch (InterruptedException ie) {
            }
        }
    }
}

public class ExampleDevice implements SetCommandListener, GetCommandListener, GpioPinListenerDigital {

    /* ExampleDevice implements a RGPIO device on Raspberry PI.
     All networking (REPORT,EVENT,GET,SET..) is handled by PiDevice().
     ExampleDevice implements a GpioListener to create an event when an input pin changes state.
     ExampleDevice implements onSetCommand()  to set a pin when a SET command is received.
     */
    DeviceInput button;
    DeviceOutput buzzer;

    // analog input and analog output for test only, no effect on GPIO
    DeviceInput sensor;
    DeviceOutput timer;
    
    static Integer sensorValue=0;

    // corresponding GPIO pins
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
                
        PiDevice.runDevice();

// convert listener thread back to in line so runDevice does not exit
        try {
            Thread.sleep(999999999);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
