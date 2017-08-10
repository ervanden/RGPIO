package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageListener;
import rgpioutils.MessageEvent;
import utils.TimeStamp;

import udputils.UDPSender;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import rgpioutils.ConfigurationFileEntry;
import rgpioutils.DeviceFileEntry;
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
                //               int devicePort = receivePacket.getPort();
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

    public DeviceProbeThread(int reportInterval) {
        super();
        this.reportInterval = reportInterval;
    }

    public void run() {
        while (true) {
            try {
                UDPSender.send("Report", "255.255.255.255", null, RGPIO.devicePort, 0, 1);
                //timeout==0  no use to wait for a reply here, it is sent to the listener
                //retries==1  send only once now, the broadcast is repeated anyway
                Thread.sleep(reportInterval * 1000);
                // Check if a device has not responded within the last reportInterval.
                long now = new TimeStamp().getTimeInMillis();
                for (PDevice device : RGPIO.PDeviceMap.values()) {
                    if ((now - device.lastContact.getTimeInMillis()) > reportInterval * 1000) {
                        device.setNotResponding("device did not respond in last " + reportInterval + " sec");
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

    public static int serverPort = 2600;  // server listens on this port for commands from devices
    public static int devicePort = 2500;  // devices listen on this port for server commands
    public static int webSocketPort = 2603;
    public static int reportInterval = 20; // server sends report request every reportInterval sec.

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

    public static void printMaps(String title) {
//        System.out.println("===== maps after " + title + " =======");
//        System.out.println("\nDevice vdevice map\n");
        VDeviceMap.print();
//        System.out.println("\nDigital input map\n");
        VDigitalInputMap.print();
//        System.out.println("\nDigital output map\n");
        VDigitalOutputMap.print();
//        System.out.println();
//        System.out.println("\nPhysical devices\n");
        PDeviceMap.print();
    }

    public static void readConfigurationFile(String fileName) {
        ArrayList<Object> l;
        l = JSON2Object.readJSONFile(fileName, ConfigurationFileEntry.class);
        if (l.size() != 1) {
            System.out.println("Expected 1 JSON object in RGPIO configuration file, found : " + l.size());
            System.out.println("Using defaults...");
            ConfigurationFileEntry RGPIOConfiguration=new ConfigurationFileEntry();
            RGPIOConfiguration.serverPort=2600;
            RGPIOConfiguration.devicePort=2500;
            RGPIOConfiguration.webSocketPort=2603;
            RGPIOConfiguration.reportInterval=20;
        } else {
            Object o = l.get(0);
            RGPIOConfiguration = (ConfigurationFileEntry) o;
        }
        System.out.println(RGPIOConfiguration.toString());

        serverPort = RGPIOConfiguration.serverPort;
        devicePort = RGPIOConfiguration.devicePort;
        webSocketPort = RGPIOConfiguration.webSocketPort;
        reportInterval = RGPIOConfiguration.reportInterval;

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
