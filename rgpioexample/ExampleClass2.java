package rgpioexample;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Calendar;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import rgpio.*;

public class ExampleClass2 {

    /*
implements RGPIODigitalInputEventListener, RGPIOMessageListener, RGPIOTimerListener, MouseListener, ActionListener {



     there are a number of buttons and a number of lights.
     Any button that is pressed switches on the lights for 10 seconds
     Pressing any button longer than 2 seconds switches the lights off if they were on, and off if they were on.

    RGPIODeviceGroup buttons;
    RGPIODeviceGroup lights;

    RGPIOTimer lightsOut;
    Calendar buttonPressTime = null;
    Calendar buttonReleaseTime = null;
    boolean lightsOnWhenPressed;

    JButton b0; // represents lights
    JButton b1;
    JButton b2;

    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        System.out.println("action " + action);
        if (action.equals("PRINT MAPS")) {
            RGPIO.printMaps("");
        }
        if (action.equals("REPORT ALL")) {
            RGPIO.receiveFromDevice("192.168.0.34", "Report/HWid:B123/Model:BUTTON/Uptime:600");
            RGPIO.receiveFromDevice("192.168.0.40", "Report/HWid:B456/Model:BUTTON/Uptime:700");
            RGPIO.receiveFromDevice("192.168.0.35", "Report/HWid:B234/Model:BUTTON/Uptime:800");
            RGPIO.receiveFromDevice("192.168.0.36", "Report/HWid:R456/Model:RELAY/Uptime:0");
            RGPIO.receiveFromDevice("192.168.0.37", "Report/HWid:R123/Model:RELAY/Uptime:900");
            RGPIO.receiveFromDevice("192.168.0.38", "Report/HWid:R124/Model:RELAY/Uptime:1000");
        }
    }

    public void mousePressed(MouseEvent e) {
        //       System.out.println("Mouse pressed; # of clicks: " + e.getClickCount());
    }

    public void mouseReleased(MouseEvent e) {
        //       System.out.println("Mouse released; # of clicks: " + e.getClickCount());
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    public void mouseClicked(MouseEvent e) {
//        System.out.println("Mouse clicked (# of clicks: " + e.getClickCount() + ")");

        if (e.getComponent() == b1) {
            if (b1.getBackground() == Color.gray) {
                b1.setBackground(Color.red);
                RGPIO.receiveFromDevice("192.168.0.34", "Event/HWid:B123/Model:BUTTON/Pin:button/Value:HIGH");
                return;
            }
            if (b1.getBackground() == Color.red) {
                b1.setBackground(Color.gray);
                RGPIO.receiveFromDevice("192.168.0.34", "Event/HWid:B123/Model:BUTTON/Pin:button/Value:LOW");
                return;
            }

        } else if (e.getComponent() == b2) {
            if (b2.getBackground() == Color.gray) {
                b2.setBackground(Color.red);
                RGPIO.receiveFromDevice("192.168.0.40", "Event/HWid:B456/Model:BUTTON/Pin:button/Value:HIGH");
                return;
            }
            if (b2.getBackground() == Color.red) {
                b2.setBackground(Color.gray);
                RGPIO.receiveFromDevice("192.168.0.40", "Event/HWid:B456/Model:BUTTON/Pin:button/Value:LOW");
                return;
            }

        } else {
            System.out.println("clicked on " + e.getComponent().getClass().getName());
        }
    }

    private void switchLightsOn() {
        lights.digitalOutput("on").set(true);
        b0.setBackground(Color.white);
        System.out.println("lights switched ON");
    }

    private void switchLightsOff() {
        lights.digitalOutput("on").set(false);
        b0.setBackground(Color.gray);
        System.out.println("lights switched OFF");
    }

    public void onDigitalPinEvent(RGPIODigitalInputEvent event) {// throws Exception {

        if ((event.device.deviceGroupName.equals("buttons"))
                && (event.pin.label.equals("button"))) {

            if (event.state) {
                buttonPressTime = Calendar.getInstance();
                lightsOnWhenPressed = lights.digitalOutput("on").get();

                if (lightsOnWhenPressed) {
                    switchLightsOff();
                } else {
                    switchLightsOn();
                    lightsOut.set(10);
                }

            } else { // button released
                long deltaInMillis;
                buttonReleaseTime = Calendar.getInstance();
                if (buttonPressTime != null) {
                    deltaInMillis = buttonReleaseTime.getTimeInMillis() - buttonPressTime.getTimeInMillis();
                } else {
                    // button released before ever pressed: should not happen; Do nothing
                    deltaInMillis = 0;
                }

                System.out.println("button pressed during " + deltaInMillis + " msec");

                if (deltaInMillis > 2000) {
                    if (lightsOnWhenPressed) {
                        lightsOut.cancel();
                        switchLightsOff();
                    } else {
                        lightsOut.cancel();
                        switchLightsOn();
                    }
                }
            }
        }
    }

    public void onTimeOut(String timerName) {
        lights.digitalOutput("on").set(false);
        b0.setBackground(Color.gray);
        System.out.println("timer expired: lights switched out");
    }

    public void onMessage(RGPIOMessageEvent e) throws Exception {
        System.out.println(e.toString());
    }

    public void start() throws Exception {

        JFrame frame = new JFrame("ExampleClass2 Tester");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Create and set up the content pane.
        JPanel pane = new JPanel();
        pane.setOpaque(true); //content panes must be opaque
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        frame.setContentPane(pane);

        JPanel pane0 = new JPanel();
        pane0.setLayout(new BoxLayout(pane0, BoxLayout.LINE_AXIS));
        pane0.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        JButton b_report = new JButton("report");
        b_report.addActionListener(this);
        b_report.setActionCommand("REPORT ALL");
        pane0.add(b_report);

        JPanel pane1 = new JPanel();
        pane1.setLayout(new BoxLayout(pane1, BoxLayout.LINE_AXIS));
        pane1.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        JButton b_printmaps = new JButton("print maps");
        b_printmaps.addActionListener(this);
        b_printmaps.setActionCommand("PRINT MAPS");
        pane1.add(b_printmaps);

        JPanel pane2 = new JPanel();
        pane2.setLayout(new BoxLayout(pane2, BoxLayout.LINE_AXIS));
        pane2.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        b1 = new JButton("BUTTON 1");
        b1.setBackground(Color.gray);
        b1.addMouseListener(this);
        pane2.add(b1);

        b2 = new JButton("BUTTON 2");
        b2.setBackground(Color.gray);
        b2.addMouseListener(this);
        pane2.add(b2);

        JPanel pane3 = new JPanel();
        pane3.setLayout(new BoxLayout(pane3, BoxLayout.LINE_AXIS));
        pane3.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        b0 = new JButton("LIGHT");
        b0.setBackground(Color.gray);
        pane3.add(b0);

        pane.add(pane0);
        pane.add(pane1);
        pane.add(pane2);
        pane.add(pane3);

        frame.pack();
        frame.setVisible(true);

        lightsOut = new RGPIOTimer("LightsOut", true);
        lightsOut.addListener(this);
        lightsOut.start();  // nothing happens until timer is set

        RGPIO.addListener(this);

        String configurationDirectory;
        if (System.getProperty("file.separator").equals("/")) {
            configurationDirectory = System.getProperty("user.home") + "/RGPIO/Example2";
        } else {
            configurationDirectory = System.getProperty("user.home") + "\\Documents\\RGPIO\\Example2\\";
        }

        RGPIO.initialize(configurationDirectory);

        buttons = new RGPIODeviceGroup("BUTTON", "buttons", 2, 999);
        lights = new RGPIODeviceGroup("RELAY", "lights", 0, 999);

        RGPIO.startDeviceMonitor();

        buttons.digitalInput("button").addDigitalPinListener(this);

        while (true) {
            Thread.sleep(999999999);
        }
    }
    
    */
}
