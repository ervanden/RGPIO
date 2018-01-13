package rgpio;

import java.awt.Color;
import utils.TimeStamp;

import udputils.UDPSender;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import static rgpio.RGPIO.RRDSAMPLE;
import rgpioutils.ConfigurationFileEntry;
import rgpioutils.DeviceFileEntry;
import rrd.RRDGenerator;
import tcputils.WSServer;
import utils.JSON2Object;
import utils.JSONString;

class DeviceMonitorThread extends Thread {

    public DeviceMonitorThread() {
        super("DeviceMonitorThread");
    }

    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(RGPIO.serverPort);
            serverSocket.setBroadcast(true);
            byte[] receiveData = new byte[1024];
            byte[] sendData;
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                InetAddress deviceIPAddress = receivePacket.getAddress();
                //  int devicePort = receivePacket.getPort();
                String message = new String(receivePacket.getData());
                message = message.substring(0, receivePacket.getLength());

                boolean validCommand = DeviceHandler.handleDeviceMessage(deviceIPAddress.toString().substring(1), message);

                if (validCommand) {

                    // After receiving a valid command from the device, RGPIO sends OK
                    // 50 msec delay is required before sending anything to ESP.
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                    };
                    sendData = ("OK").getBytes();
                    DatagramPacket sendPacket
                            = new DatagramPacket(sendData, sendData.length, deviceIPAddress, RGPIO.devicePort);
                    serverSocket.send(sendPacket);

                    MessageEvent e = new MessageEvent(MessageType.SendMessage);
                    e.description = "OK to |" + message + "|";
                    e.ipAddress = deviceIPAddress.toString().substring(1);
                    RGPIO.message(e);
                }

            }
        } catch (SocketException so) {
            so.printStackTrace();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

}

class DeviceProbeThread extends Thread {

    int reportInterval;
    String broadcastReport;

    public DeviceProbeThread(int reportInterval) {
        super();
        this.reportInterval = reportInterval;

        JSONString json = new JSONString();
        json.addProperty("destination", "ALL");
        json.addProperty("command", "REPORT");
        broadcastReport = json.asString();
    }

    public void run() {
        while (true) {
            try {
                UDPSender.send(broadcastReport, "255.255.255.255", null, RGPIO.devicePort, 0, 1);
                //timeout==0  no use to wait for a reply here, it is sent to the listener
                //retries==1  send only once now, the broadcast is repeated anyway
                Thread.sleep(reportInterval * 1000);
                // Check if a device has not responded within the last reportInterval.
                long now = new TimeStamp().getTimeInMillis();
                for (PDevice device : RGPIO.PDeviceMap.values()) {
                    long inactive = now - device.lastContact.getTimeInMillis();
                    if (inactive > reportInterval * 1000) {
                        device.setNotResponding("device did not respond in last " + Math.round(inactive / 1000) + " sec");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (VDevice deviceGroup : RGPIO.VDeviceMap.values()) {
                int n = 0;
                for (PDevice device : RGPIO.PDeviceMap.values()) {
                    if ((device.vdevice == deviceGroup) && (device.get_status() == PDeviceStatus.ACTIVE)) {
                        n++;
                    }
                }
                if (deviceGroup.minMembers != null) {
                    if (n < deviceGroup.minMembers) {
                        MessageEvent e = new MessageEvent(MessageType.DeviceGroupMinimum);
                        e.description = "device group has too few members : " + n;
                        e.vdevice = deviceGroup.name;
                        RGPIO.message(e);
                    }
                }
            }

        }
    }
}

/*
 RrdDb rrddb = RRDGenerator.openRRD(rrdPath);

 // can now update database
 ArrayList<GaugeSource> gaugeSources = new ArrayList<>();
 int s = 0;
 for (String sensor : SENSORS) {
 gaugeSources.add(new GaugeSource(1909752002L + (s++), 20));
 }


 long SEED = 1909752002L;
 Random RANDOM = new Random(SEED);
 long START = Util.getTimestamp();
 long END = Util.getTimestamp() + 2 * 30 * 24 * 60 * 60;
 int MAX_STEP = 599;

 Sample sample = rrddb.createSample();
 long t = START;
 int n = 0;
 while (t <= END + 172800L) {
 sample.setTime(t);
 int i = 0;
 for (String sensor : SENSORS) {
 sample.setValue(sensor, gaugeSources.get(i).getValue());
 i++;
 }
 sample.update();
 t += RANDOM.nextDouble() * MAX_STEP + 1;
 }

 println("");
 println("== Finished. RRD file updated " + n + " times");
 */
class UpdateRRDThread extends Thread {

    int step;

    public UpdateRRDThread(int step) {
        super();
        this.step = step;
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(step * 1000);

                RGPIO.RRDSAMPLE.setTime(Util.getTimestamp());
                int updates = 0;
                for (VInput vinput : RGPIO.RRDVINPUTS) {
                    if (vinput.type == IOType.analogInput) {
                        Integer value=vinput.avg();
                        if (value!=null){
                        System.out.println("updating RRD with " + vinput.name + " = " + vinput.avg());
                        RGPIO.RRDSAMPLE.setValue(vinput.name, vinput.avg());
                        updates++;
                        }
                    }
                }
                if (updates > 0) {
                    try {
                        RGPIO.RRDSAMPLE.update();
                    } catch (IOException ioe) {
                    };
                }
            } catch (InterruptedException ie) {
            }
        }
    }
}

public class RGPIO {

    public static PDeviceMap PDeviceMap;
    public static VDeviceMap VDeviceMap;
    public static VInputMap VDigitalInputMap;
    public static VInputMap VAnalogInputMap;
    public static VInputMap VStringInputMap;
    public static VOutputMap VDigitalOutputMap;
    public static VOutputMap VAnalogOutputMap;
    public static VOutputMap VStringOutputMap;

    static ConfigurationFileEntry RGPIOConfiguration;

    public static WSServer webSocketServer;

    public static int serverPort;  // server listens on this port for commands from devices
    public static int devicePort;  // devices listen on this port for server commands
    public static int webSocketPort;
    public static int reportInterval; // server sends report request every reportInterval sec.
    public static String htmlDirectory;

    static RrdDb RRDDB;
    static Sample RRDSAMPLE;

    public static void initialize(String configurationDir) {

        VDeviceMap = new VDeviceMap();
        PDeviceMap = new PDeviceMap();
        VDigitalInputMap = new VInputMap(IOType.digitalInput);
        VAnalogInputMap = new VInputMap(IOType.analogInput);
        VStringInputMap = new VInputMap(IOType.stringInput);
        VDigitalOutputMap = new VOutputMap(IOType.digitalOutput);
        VAnalogOutputMap = new VOutputMap(IOType.analogOutput);
        VStringOutputMap = new VOutputMap(IOType.stringOutput);

        readConfigurationFile(configurationDir + "RGPIO.txt");
        readDevicesFile(configurationDir + "devices.txt");

        // Start listening to messages from devices
        new DeviceMonitorThread().start();

        // Start broadcasting Report requests
        DeviceProbeThread deviceProbeThread = new DeviceProbeThread(reportInterval);
        deviceProbeThread.start();

        ClientHandler clientHandler = new ClientHandler();
        webSocketServer = new WSServer(webSocketPort);
        webSocketServer.addListener(clientHandler);
        webSocketServer.start();
    }

    // createRRD is to be called by the main application after creating Vinputs
    // It will initialize the RRD database and start the thread that updates the database
    // every 2 seconds
    public static ArrayList<VInput> RRDVINPUTS = new ArrayList<>();

    public static void createRRD(String RRDDIRECTORY, int RRDSTEP) {

        ArrayList<String> vinputNames = new ArrayList<>();
        ArrayList<Color> allColors = new ArrayList<>();
        HashMap<String, Color> colors = new HashMap<>();

        allColors.add(Color.red);
        allColors.add(Color.blue);
        allColors.add(Color.cyan);
        allColors.add(Color.black);

        int colorIndex = 0;
        for (VInput vinput : VAnalogInputMap.values()) {
            System.out.println("RDD entry: " + vinput.name);
            RRDVINPUTS.add(vinput);
            vinputNames.add(vinput.name);
            colors.put(vinput.name, allColors.get(colorIndex));
            colorIndex = (colorIndex + 1) % allColors.size();
        }
        
        // create a RRD database with an entry every STEP seconds
        String RRDPATH = RRDDIRECTORY + "datastore.rrd";
        RRDGenerator.createRRD(RRDPATH, vinputNames, colors,RRDSTEP);
        System.out.println("Created " + RRDPATH);
        RRDDB = RRDGenerator.openRRD(RRDPATH);
        try {
            RRDSAMPLE = RRDDB.createSample();
        } catch (IOException ioe) {
        };

        // Start updating the RDD database every RRDSTEP seconds with the values of all RRDVINPUTS
        
        new UpdateRRDThread(RRDSTEP).start();
    }

    public static VDevice VDevice(String name) {
        VDevice vdev = VDeviceMap.get(name);
        if (vdev == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VDevice is not defined in devices.txt ";
            e.vdevice = name;
            message(e);
        }
        return vdev;
    }

    public static VDigitalInput VDigitalInput(String name) {
        VInput vin = VDigitalInputMap.get(name);
        if (vin == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VDigitalInput is not defined in devices.txt ";
            e.vinput = name;
            message(e);
        }
        return (VDigitalInput) vin;
    }

    public static VDigitalOutput VDigitalOutput(String name) {
        VOutput vout = VDigitalOutputMap.get(name);
        if (vout == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VDigitalOutput is not defined in devices.txt ";
            e.voutput = name;
            message(e);
        }
        return (VDigitalOutput) vout;
    }

    public static VAnalogInput VAnalogInput(String name) {
        VInput vin = VAnalogInputMap.get(name);
        if (vin == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VAnalogInput is not defined in devices.txt ";
            e.vinput = name;
            message(e);
        }
        return (VAnalogInput) vin;
    }

    public static VAnalogOutput VAnalogOutput(String name) {
        VOutput vout = VAnalogOutputMap.get(name);
        if (vout == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VAnalogOutput is not defined in devices.txt ";
            e.voutput = name;
            message(e);
        }
        return (VAnalogOutput) vout;
    }

    public static VStringInput VStringInput(String name) {
        VInput vin = VStringInputMap.get(name);
        if (vin == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VStringInput is not defined in devices.txt ";
            e.vinput = name;
            message(e);
        }
        return (VStringInput) vin;
    }

    public static VStringOutput VStringOutput(String name) {
        VOutput vout = VStringOutputMap.get(name);
        if (vout == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VStringOutput is not defined in devices.txt ";
            e.voutput = name;
            message(e);
        }
        return (VStringOutput) vout;
    }

    public static void receiveFromDevice(String ipAddress, String message) {
        // used for simulating devices
        DeviceHandler.handleDeviceMessage(ipAddress, message);

    }

    private static List<MessageListener> listeners = new ArrayList<>();

    public static void addMessageListener(MessageListener toAdd) {
        listeners.add(toAdd);
    }

    public static void message(MessageEvent e) {

        // This method forwards the messages from RGPIO.
        // Send the message  to clients connected to the message feed
        // Call all the listeners
        for (MessageListener l : listeners) {
            try {
                l.onMessage(e);
            } catch (Exception ex) {
            };
        }
    }

    public static void readConfigurationFile(String fileName) {
        ArrayList<Object> l;
        l = JSON2Object.readJSONFile(fileName, ConfigurationFileEntry.class);
        if (l.size() != 1) {
            System.out.println("Expected 1 JSON object in RGPIO configuration file, found : " + l.size());
            System.out.println("Using defaults...");
            RGPIOConfiguration = new ConfigurationFileEntry();
            if (l.size() == 0) {  // create the RGPIO.txt file with the defaults
                ArrayList<Object> lw = new ArrayList<>();
                lw.add(RGPIOConfiguration);
                JSON2Object.writeJSONFile(fileName, lw);
            }
        } else {
            Object o = l.get(0);
            RGPIOConfiguration = (ConfigurationFileEntry) o;
        }

        serverPort = RGPIOConfiguration.serverPort;
        devicePort = RGPIOConfiguration.devicePort;
        webSocketPort = RGPIOConfiguration.webSocketPort;
        reportInterval = RGPIOConfiguration.reportInterval;
        htmlDirectory = RGPIOConfiguration.htmlDirectory;

    }

    public static void readDevicesFile(String fileName) {
        ArrayList<Object> l;
        l = JSON2Object.readJSONFile(fileName, DeviceFileEntry.class);
        for (Object o : l) {
            DeviceFileEntry dfe = (DeviceFileEntry) o;
//            System.out.println(dfe.toString());
            VDevice device = null;
            VInput vinput = null;
            VOutput voutput = null;
            String model = null;
            String HWid = null;
            String pin = null;

            if (dfe.Device != null) {
                device = VDeviceMap.add(dfe.Device);
            }
            if (dfe.DigitalInput != null) {
                vinput = VDigitalInputMap.add(dfe.DigitalInput);
            }
            if (dfe.DigitalOutput != null) {
                voutput = VDigitalOutputMap.add(dfe.DigitalOutput);
            }
            if (dfe.AnalogInput != null) {
                vinput = VAnalogInputMap.add(dfe.AnalogInput);
            }
            if (dfe.AnalogOutput != null) {
                voutput = VAnalogOutputMap.add(dfe.AnalogOutput);
            }
            if (dfe.StringInput != null) {
                vinput = VStringInputMap.add(dfe.StringInput);
            }
            if (dfe.StringOutput != null) {
                voutput = VStringOutputMap.add(dfe.StringOutput);
            }
            if (dfe.Model != null) {
                model = dfe.Model;
            }
            if (dfe.HWid != null) {
                HWid = dfe.HWid;
            }
            if (dfe.Pin != null) {
                pin = dfe.Pin;
            }

            int selectors = 0;
            if (device != null) {
                if (model != null) {
                    device.addSelectSpec(null, "Model", model);
                    selectors++;
                }
                if (HWid != null) {
                    device.addSelectSpec(null, "HWid", HWid);
                    selectors++;
                }
            }
            if ((voutput != null) && (pin != null)) {
                if (model != null) {
                    voutput.addSelectSpec(pin, "Model", model);
                    selectors++;
                }
                if (HWid != null) {
                    voutput.addSelectSpec(pin, "HWid", HWid);
                    selectors++;
                }
            }

            if ((vinput != null) && (pin != null)) {
                if (model != null) {
                    vinput.addSelectSpec(pin, "Model", model);
                    selectors++;
                }
                if (HWid != null) {
                    vinput.addSelectSpec(pin, "HWid", HWid);
                    selectors++;
                }
            }

            if (selectors != 1) {
                MessageEvent e = new MessageEvent(MessageType.Info);
                e.description = "Skipped invalid entry : " + dfe.toString() + " selectors=" + selectors;
                message(e);
            }

        }
    }

}
