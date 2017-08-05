package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageListener;
import rgpioutils.MessageEvent;
import utils.TimeStamp;
import utils.ByteOrderMark;
import utils.Console;
import udputils.UDPSender;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import rgpioutils.DeviceFileEntry;
import tcputils.TCPfeed;
import tcputils.TCPserver;
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
                    e.description = "OK to <" + message + ">";
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

    public static TCPfeed messageFeed;
    public static TCPfeed updateFeed;
    public static TCPserver clientRequests;

    public static final int serverPort = 2600;  // server listens on this port for commands from devices
    public static final int devicePort = 2500;  // devices listen on this port for server commands
    public static final int messageFeedPort = 2601; // server listens on this port. Clients can connect to message feed.
    public static final int clientRequestPort = 2602; // server listens on this port. Clients can send requests.
    public static final int updateFeedPort = 2603;  // server listens on this port. Client can get status updates from this feed
    public static final int reportInterval = 20; // server sends report request every reportInterval sec.

    public static void initialize(String configurationDir) {

        VDeviceMap = new VDeviceMap();
        PDeviceMap = new PDeviceMap();
        VDigitalInputMap = new VInputMap(IOType.digitalInput);
        VAnalogInputMap = new VInputMap(IOType.analogInput);
        VStringInputMap = new VInputMap(IOType.stringInput);
        VDigitalOutputMap = new VOutputMap(IOType.digitalOutput);
        VAnalogOutputMap = new VOutputMap(IOType.analogOutput);
        VStringOutputMap = new VOutputMap(IOType.stringOutput);

        readDevicesFile(configurationDir);

        // Start listening to messages from devices
        new DeviceMonitorThread().start();

        // Start broadcasting Report requests
        DeviceProbeThread deviceProbeThread = new DeviceProbeThread(reportInterval);
        deviceProbeThread.start();

        Console.verbose(false); // Controls  debug output from tcputils classes
        messageFeed = new TCPfeed(messageFeedPort);
        messageFeed.start();

//       clientRequests = new TCPserver(clientRequestPort);
//       no client requests implemented at this time
        ClientHandler clientHandler = new ClientHandler();
//        clientRequests.addListener(clientHandler);
//        clientRequests.start();

        updateFeed = new TCPfeed(updateFeedPort);
        updateFeed.addListener(clientHandler);
        updateFeed.start();
    }

    public static VDevice VDevice(String name) {
        VDevice vdev = VDeviceMap.get(name);
        if (vdev == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VDevice is not defined in devices.txt ";
            e.vdevice = name;
            RGPIO.message(e);
        }
        return vdev;
    }

    public static VDigitalInput VDigitalInput(String name) {
        VInput vin = VDigitalInputMap.get(name);
        if (vin == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VDigitalInput is not defined in devices.txt ";
            e.vinput = name;
            RGPIO.message(e);
        }
        return (VDigitalInput) vin;
    }

    public static VDigitalOutput VDigitalOutput(String name) {
        VOutput vout = VDigitalOutputMap.get(name);
        if (vout == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VDigitalOutput is not defined in devices.txt ";
            e.voutput = name;
            RGPIO.message(e);
        }
        return (VDigitalOutput) vout;
    }

    public static VAnalogInput VAnalogInput(String name) {
        VInput vin = VAnalogInputMap.get(name);
        if (vin == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VAnalogInput is not defined in devices.txt ";
            e.vinput = name;
            RGPIO.message(e);
        }
        return (VAnalogInput) vin;
    }

    public static VAnalogOutput VAnalogOutput(String name) {
        VOutput vout = VAnalogOutputMap.get(name);
        if (vout == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VAnalogOutput is not defined in devices.txt ";
            e.voutput = name;
            RGPIO.message(e);
        }
        return (VAnalogOutput) vout;
    }

    public static VStringInput VStringInput(String name) {
        VInput vin = VStringInputMap.get(name);
        if (vin == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VStringInput is not defined in devices.txt ";
            e.vinput = name;
            RGPIO.message(e);
        }
        return (VStringInput) vin;
    }

    public static VStringOutput VStringOutput(String name) {
        VOutput vout = VStringOutputMap.get(name);
        if (vout == null) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "VStringOutput is not defined in devices.txt ";
            e.voutput = name;
            RGPIO.message(e);
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

        // a message is generated from within RGPIO.
        // Send it out to clients connected to the message feed
        // and call all the listeners
        if (messageFeed != null) {  // when running as device, there is no messageFeed
            messageFeed.writeToClients(e.toJSON());
        }

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
        RGPIO.VDeviceMap.print();
//        System.out.println("\nDigital input map\n");
        RGPIO.VDigitalInputMap.print();
//        System.out.println("\nDigital output map\n");
        RGPIO.VDigitalOutputMap.print();
//        System.out.println();
//        System.out.println("\nPhysical devices\n");
        RGPIO.PDeviceMap.print();
    }

    public static void readDevicesFile(String fileName) {
        ArrayList<Object> l;
        l = JSON2Object.readJSONFile(fileName, DeviceFileEntry.class);
        for (Object o : l) {
            DeviceFileEntry dfe = (DeviceFileEntry) o;
            System.out.println(dfe.toString());
            VDevice device = null;
            VInput vinput = null;
            VOutput voutput = null;
            String model = null;
            String HWid = null;
            String pin = null;

            if (dfe.Device != null) {
                device = RGPIO.VDeviceMap.add(dfe.Device);
            }
            if (dfe.DigitalInput != null) {
                vinput = RGPIO.VDigitalInputMap.add(dfe.DigitalInput);
            }
            if (dfe.DigitalOutput != null) {
                voutput = RGPIO.VDigitalOutputMap.add(dfe.DigitalOutput);
            }
            if (dfe.AnalogInput != null) {
                vinput = RGPIO.VAnalogInputMap.add(dfe.AnalogInput);
            }
            if (dfe.AnalogOutput != null) {
                voutput = RGPIO.VAnalogOutputMap.add(dfe.AnalogOutput);
            }
            if (dfe.StringInput != null) {
                vinput = RGPIO.VStringInputMap.add(dfe.StringInput);
            }
            if (dfe.StringOutput != null) {
                voutput = RGPIO.VStringOutputMap.add(dfe.StringOutput);
            }
            if (dfe.Model != null) {
                model = dfe.Model;
            }
            if (dfe.HWid != null) {
                model = dfe.HWid;
            }
            if (dfe.Pin != null) {
                model = dfe.Pin;
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
                    e.description = "Skipped invalid entry : " + dfe.toString();
                    RGPIO.message(e);
                }


        }
    }

    public static void readDevicesFile2(String fileName) {

        BufferedReader inputStream;

        try {
            System.out.println("Opening " + fileName);
            File file = new File(fileName);
            InputStream is = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            inputStream = new BufferedReader(isr);

            int lineCount = 0;
            String inputLine;
            while ((inputLine = inputStream.readLine()) != null) {
                if (lineCount == 0) {
                    inputLine = ByteOrderMark.remove(inputLine);
                }

                lineCount++;
                VDevice device = null;
                VInput vinput = null;
                VOutput voutput = null;
                String model = null;
                String HWid = null;
                String pin = null;

                String[] nameValuePairs = inputLine.split("/");
                String name;
                String value;
                for (String nameValue : nameValuePairs) {
                    //                   System.out.println(lineCount + " " + nameValue);
                    String[] nv = nameValue.split(":");
                    name = nv[0];
                    value = nv[1];
                    if (name.equals("Device")) {
                        device = RGPIO.VDeviceMap.add(value);
                    }
                    if (name.equals("DigitalInput")) {
                        vinput = RGPIO.VDigitalInputMap.add(value);
                    }
                    if (name.equals("DigitalOutput")) {
                        voutput = RGPIO.VDigitalOutputMap.add(value);
                    }
                    if (name.equals("AnalogInput")) {
                        vinput = RGPIO.VAnalogInputMap.add(value);
                    }
                    if (name.equals("AnalogOutput")) {
                        voutput = RGPIO.VAnalogOutputMap.add(value);
                    }
                    if (name.equals("StringInput")) {
                        vinput = RGPIO.VStringInputMap.add(value);
                    }
                    if (name.equals("StringOutput")) {
                        voutput = RGPIO.VStringOutputMap.add(value);
                    }
                    if (name.equals("Model")) {
                        model = value;
                    }
                    if (name.equals("HWid")) {
                        HWid = value;
                    }
                    if (name.equals("Pin")) {
                        pin = value;
                    }
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
                    e.description = "skipped line : " + inputLine + " selectors " + selectors;
                    RGPIO.message(e);
                }

            }
            System.out.println("read from " + fileName + " : " + lineCount + " lines");
            inputStream.close();
        } catch (FileNotFoundException fnf) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "file not found : " + fileName;
            RGPIO.message(e);
        } catch (IOException io) {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "io exception while reading file : " + fileName;
            RGPIO.message(e);
        }
    }
}
