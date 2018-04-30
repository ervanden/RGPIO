package rgpio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import mail.MailGenerator;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import rgpioutils.ConfigurationFileEntry;
import rgpioutils.DeviceFileEntry;
import rgpioutils.DeviceTree;
import rrd.RRDGenerator;
import tcputils.WSServer;
import utils.JSON2Object;

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
                /* send an OK: no longer needed 
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
                 */
            }
        } catch (SocketException so) {
            so.printStackTrace();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

}

/*
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
 UDPSender.send(broadcastReport, RGPIO.broadcastAddress, null, RGPIO.devicePort);
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
 */
class UpdateRRDThread extends Thread {

    int step;

    public UpdateRRDThread(int step) {
        super();
        this.step = step;
    }

    public void run() {

        // message event to be re-used for every update
        MessageEvent e = new MessageEvent(MessageType.UpdateRRDB);

        while (true) {
            try {
                Thread.sleep(step * 1000);
                long time = Util.getTimestamp();
                RGPIO.RRDSample.setTime(time);
                int updates = 0;

                // new code not using RRDVIO
                // purpose : to store last recorded values in the VAnalogInput,... objects
                
                for (VInput v : RGPIO.VAnalogInputMap.values()) {
                    e.vdevice = v.name;
                    Integer value = v.avg();
                    if (value != null) {
                        e.description = "value=" + value;
                        RGPIO.message(e);
                        //System.out.println("updating RRD with " + vinput.name + " = " + value + " (time=" + time + ")");
                        RGPIO.RRDSample.setValue(v.name, v.avg());
                        updates++;
                    }
                }

                for (VOutput v : RGPIO.VAnalogOutputMap.values()) {
                    e.vdevice = v.name;
                    try {
                        //System.out.println("updating RRD with " + voutput.name + " = " + voutput.value + " (time=" + time + ")");
                        if (v.value != null) {
                            Integer value = Integer.parseInt(v.value);
                            e.description = "value=" + value;
                            RGPIO.message(e);
                            RGPIO.RRDSample.setValue(v.name, value);
                            updates++;
                        }
                    } catch (NumberFormatException nfe) {
                        System.out.println("UpdateRDD: value is not an integer: " + v.value + " Ignored.");
                    }
                }

                for (VInput v : RGPIO.VDigitalInputMap.values()) {
                    e.vdevice = v.name;
                    Integer value = v.nrHigh();
                    //System.out.println("updating RRD with " + vinput.name + " = " + value + " (time=" + time + ")");
                    if (value != null) {
                        e.description = "value=" + value;
                        RGPIO.message(e);
                        RGPIO.RRDSample.setValue(v.name, v.nrHigh());
                        updates++;
                    }
                }
                
                for (VOutput v : RGPIO.VDigitalOutputMap.values()) {
                    e.vdevice = v.name;
                        //System.out.println("updating RRD with " + voutput.name + " = " + voutput.value + " (time=" + time + ")");
                        if (v.value != null) {
                            if (v.value.equals("High")) {
                                e.description = "value=" + "1";
                                RGPIO.message(e);
                                RGPIO.RRDSample.setValue(v.name, 1);
                                updates++;
                            } else if (v.value.equals("Low")) {
                                e.description = "value=" + "0";
                                RGPIO.message(e);
                                RGPIO.RRDSample.setValue(v.name, 0);
                                updates++;
                            } else {
                                System.out.println("UpdateRDD: Invalid value of digital output : " + v.value + " Ignored.");
                            }
                        }
                    }
                
 /* using RRDVIO               

                for (VIO vio : RGPIO.RRDVIO) {
                    e.vdevice = vio.name;
                    if (vio.type == IOType.analogInput) {
                        VInput vinput = (VInput) vio;
                        Integer value = vinput.avg();
                        if (value != null) {
                            e.description = "value=" + value;
                            RGPIO.message(e);
                            //System.out.println("updating RRD with " + vinput.name + " = " + value + " (time=" + time + ")");
                            RGPIO.RRDSample.setValue(vinput.name, vinput.avg());
                            updates++;
                        }
                    }
                    if (vio.type == IOType.analogOutput) {
                        VOutput voutput = (VOutput) vio;
                        try {
                            //System.out.println("updating RRD with " + voutput.name + " = " + voutput.value + " (time=" + time + ")");
                            if (voutput.value != null) {
                                Integer value = Integer.parseInt(voutput.value);
                                e.description = "value=" + value;
                                RGPIO.message(e);
                                RGPIO.RRDSample.setValue(voutput.name, value);
                                updates++;
                            }

                        } catch (NumberFormatException nfe) {
                            System.out.println("UpdateRDD: value is not an integer: " + voutput.value + " Ignored.");
                        }
                    }
                    if (vio.type == IOType.digitalInput) {
                        VInput vinput = (VInput) vio;
                        Integer value = vinput.nrHigh();
                        //System.out.println("updating RRD with " + vinput.name + " = " + value + " (time=" + time + ")");
                        if (value != null) {
                            e.description = "value=" + value;
                            RGPIO.message(e);
                            RGPIO.RRDSample.setValue(vinput.name, vinput.nrHigh());
                            updates++;
                        }
                    }
                    if (vio.type == IOType.digitalOutput) {
                        VOutput voutput = (VOutput) vio;
                        //System.out.println("updating RRD with " + voutput.name + " = " + voutput.value + " (time=" + time + ")");
                        if (voutput.value != null) {
                            if (voutput.value.equals("High")) {
                                e.description = "value=" + "1";
                                RGPIO.message(e);
                                RGPIO.RRDSample.setValue(voutput.name, 1);
                                updates++;
                            } else if (voutput.value.equals("Low")) {
                                e.description = "value=" + "0";
                                RGPIO.message(e);
                                RGPIO.RRDSample.setValue(voutput.name, 0);
                                updates++;
                            } else {
                                System.out.println("UpdateRDD: Invalid value of digital output : " + voutput.value + " Ignored.");
                            }
                        }
                    }
                }
*/
                
                if (updates > 0) {
                    try {
                        RGPIO.RRDSample.update();
                    } catch (IOException ioe) {
                    }
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

    public static WSServer webSocketServer;

    public static DeviceTree deviceTree;

    // parameters configurable from the RGPIO.txt configuration file
    public static int serverPort;  // server listens on this port for commands from devices
    public static int devicePort;  // devices listen on this port for server commands
    public static String broadcastAddress;
    public static int webSocketPort;
    public static int reportInterval; // server sends report request every reportInterval sec.
    public static String htmlDirectory;

    // global variables hard coded here
    public static String RGPIODirectory = null;
    public static String RRDDirectory = null;

    static RrdDb RRDDB;
    static Sample RRDSample;

    public static void initialize() {

        String application = "RGPIO";   // maybe later make multi instance possible

        VDeviceMap = new VDeviceMap();
        PDeviceMap = new PDeviceMap();
        VDigitalInputMap = new VInputMap(IOType.digitalInput);
        VAnalogInputMap = new VInputMap(IOType.analogInput);
        VStringInputMap = new VInputMap(IOType.stringInput);
        VDigitalOutputMap = new VOutputMap(IOType.digitalOutput);
        VAnalogOutputMap = new VOutputMap(IOType.analogOutput);
        VStringOutputMap = new VOutputMap(IOType.stringOutput);

//        if (System.getProperty("file.separator").equals("/")) {
        RGPIODirectory = "/home/pi/" + application + "/";
        RRDDirectory = RGPIODirectory + "dataStore/";
        readConfigurationFile(RGPIODirectory + "RGPIO.json");
        readDevicesFile(RGPIODirectory + "devices.json");
//        } 

        // Start listening to messages from devices
        new DeviceMonitorThread().start();
        /*
         // Start broadcasting Report requests
         DeviceProbeThread deviceProbeThread = new DeviceProbeThread(reportInterval);
         deviceProbeThread.start();
         */
        ClientHandler clientHandler = new ClientHandler();

        webSocketServer = new WSServer(webSocketPort);
        webSocketServer.addListener(clientHandler);
        webSocketServer.start();

        deviceTree = new DeviceTree("RGPIO");
    }

    static Integer msgId = 0;

    public static String msgId() {
        msgId++;
        return msgId.toString();
    }

    public static void sendMail(String to, String subject, String content) {
        MailGenerator.sendMail(to, subject, content);
    }

    // createRRD is to be called by the main application after creating Vinputs
    // It will initialize the RRD database and start the thread that updates the database
    // every RRDStep seconds
    public static ArrayList<String> RRDVIO = new ArrayList<>();

    public static void createRRD(int RRDStep) {

        // create the list of data source names for the graph.
        // For now it is only the analog inputs
        for (VInput v : VAnalogInputMap.values()) {
            System.out.println("RDD entry: " + v.name);
            RRDVIO.add(v.name);
        }
        for (VOutput v : VAnalogOutputMap.values()) {
            System.out.println("RDD entry: " + v.name);
            RRDVIO.add(v.name);
        }
        for (VInput v : VDigitalInputMap.values()) {
            System.out.println("RDD entry: " + v.name);
            RRDVIO.add(v.name);
        }
        for (VOutput v : VDigitalOutputMap.values()) {
            System.out.println("RDD entry: " + v.name);
            RRDVIO.add(v.name);
        }
        // create a RRD database with an entry every Step seconds
        String RRDPath = RGPIO.RRDDirectory + "datastore.rrd";
        RRDDB = RRDGenerator.createRRD(RRDPath, RRDVIO, RRDStep);
        if (RRDDB == null) {
            System.out.println("No RRDB. Exiting...");
            System.exit(0);
        }

        try {
            RRDSample = RRDDB.createSample();
        } catch (IOException ioe) {
        }

        System.out.println("Starting RRDB update thread");
        // Start updating the RDD database every RRDStep seconds with the values of all RRDVIO
        new UpdateRRDThread(RRDStep).start();
    }

    public static VDevice VDevice(String name) {
        VDevice vdev = VDeviceMap.get(name);
        if (vdev == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VDevice is not defined in devices.json ";
            e.vdevice = name;
            message(e);
        }
        return vdev;
    }

    public static VDigitalInput VDigitalInput(String name) {
        VInput vin = VDigitalInputMap.get(name);
        if (vin == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VDigitalInput is not defined in devices.json ";
            e.vinput = name;
            message(e);
        }
        return (VDigitalInput) vin;
    }

    public static VDigitalOutput VDigitalOutput(String name) {
        VOutput vout = VDigitalOutputMap.get(name);
        if (vout == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VDigitalOutput is not defined in devices.json ";
            e.voutput = name;
            message(e);
        }
        return (VDigitalOutput) vout;
    }

    public static VAnalogInput VAnalogInput(String name) {
        VInput vin = VAnalogInputMap.get(name);
        if (vin == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VAnalogInput is not defined in devices.json ";
            e.vinput = name;
            message(e);
        }
        return (VAnalogInput) vin;
    }

    public static VAnalogOutput VAnalogOutput(String name) {
        VOutput vout = VAnalogOutputMap.get(name);
        if (vout == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VAnalogOutput is not defined in devices.json ";
            e.voutput = name;
            message(e);
        }
        return (VAnalogOutput) vout;
    }

    public static VStringInput VStringInput(String name) {
        VInput vin = VStringInputMap.get(name);
        if (vin == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VStringInput is not defined in devices.json ";
            e.vinput = name;
            message(e);
        }
        return (VStringInput) vin;
    }

    public static VStringOutput VStringOutput(String name) {
        VOutput vout = VStringOutputMap.get(name);
        if (vout == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VStringOutput is not defined in devices.json ";
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

        ConfigurationFileEntry RGPIOConfiguration;

        ArrayList<Object> l;
        l = JSON2Object.readJSONFile(fileName, ConfigurationFileEntry.class);
        if (l.size() != 1) {
            System.out.println("Expected 1 JSON object in RGPIO configuration file, found : " + l.size());
            System.out.println("Using defaults...");
            RGPIOConfiguration = new ConfigurationFileEntry();
            if (l.size() == 0) {  // create the RGPIO.config file with the defaults
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
        broadcastAddress = RGPIOConfiguration.broadcastAddress;

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
