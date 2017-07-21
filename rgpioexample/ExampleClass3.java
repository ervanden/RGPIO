package rgpioexample;

import rgpioutils.MessageListener;
import rgpioutils.MessageEvent;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import rgpio.*;

public class ExampleClass3 implements VInputEventListener, MessageListener, MouseListener, ActionListener {

    /*
     there are a number of buttons and a number of lights.
     Pressing any button switches the lights off if they were on, and off if they were on.
     */
    VDevice PIRDevices;
    VInput buttons;
    VOutput lights;

    boolean lightsOnWhenPressed;

    JButton b0; // represents lights
    JButton b1;
    JButton b2;

    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
//      System.out.println("action " + action);
        if (action.equals("PRINT MAPS")) {
            RGPIO.printMaps("");
        }
        if (action.equals("REPORT ALL")) {
            RGPIO.receiveFromDevice("192.168.0.34", "Report/HWid:PIR1/Model:PIR/Uptime:600/Dop:00/Dip:02");
            RGPIO.receiveFromDevice("192.168.0.40", "Report/HWid:PIR2/Model:PIR/Uptime:700/Dop:00/Dip:02");
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        mouseDo(e);
    }

    public void mouseEntered(MouseEvent e) {
        mouseDo(e);
    }

    public void mouseDo(MouseEvent e) {
        if (e.getComponent() == b1) {
            if (b1.getBackground() == Color.gray) {
                b1.setBackground(Color.red);
                RGPIO.receiveFromDevice("192.168.0.34", "Event/HWid:PIR1/Model:PIR/Pin:02/State:High");
                return;
            }
            if (b1.getBackground() == Color.red) {
                b1.setBackground(Color.gray);
                RGPIO.receiveFromDevice("192.168.0.34", "Event/HWid:PIR1/Model:PIR/Pin:02/State:Low");
                return;
            }

        } else if (e.getComponent() == b2) {
            if (b2.getBackground() == Color.gray) {
                b2.setBackground(Color.red);
                RGPIO.receiveFromDevice("192.168.0.40", "Event/HWid:PIR2/Model:PIR/Pin:02/State:High");
                return;
            }
            if (b2.getBackground() == Color.red) {
                b2.setBackground(Color.gray);
                RGPIO.receiveFromDevice("192.168.0.40", "Event/HWid:PIR2/Model:PIR/Pin:02/State:Low");
                return;
            }

        } else {
            System.out.println("clicked on " + e.getComponent().getClass().getName());
        }
    }

    public void onInputEvent(VInputEvent event) {

        if (event.rgpioInput.name.equals("button")) {

            if (event.rgpioInput.nrHigh>0) {

                if (lights.get().equals("High")) {
                    lights.set("Low");
                    b0.setBackground(Color.gray);
                } else {
                    lights.set("High");
                    b0.setBackground(Color.white);
                }

            } else { // button released

            }
        }
    }

    public void onMessage(MessageEvent e) throws Exception {
        System.out.println(e.toString());
    }

    public void start() {

        JFrame frame = new JFrame("ExampleClass3 Tester");
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

        String configurationDirectory;
        if (System.getProperty("file.separator").equals("/")) {
            configurationDirectory = System.getProperty("user.home") + "/RGPIO/Example3/devices.txt";
        } else {
            configurationDirectory = System.getProperty("user.home") + "\\Documents\\RGPIO\\Example3\\devices.txt";
        }

        RGPIO.addMessageListener(this);
        RGPIO.initialize(configurationDirectory);

        PIRDevices = RGPIO.VDeviceMap.get("PIR");
        PIRDevices.minMembers = 2;
        buttons = RGPIO.VDigitalInputMap.get("button");
        lights = RGPIO.VDigitalOutputMap.get("light");
        buttons.minMembers = 2;
        lights.minMembers = 1;

        buttons.addDigitalPinListener(this);

        while (true) {
            try {
                Thread.sleep(999999999);
            } catch (InterruptedException ie) {
            }
        }
    }
}
