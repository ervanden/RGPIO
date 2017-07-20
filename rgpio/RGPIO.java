package rgpio;

import utils.Console;
import udputils.UDPSender;
import devices.*;
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
import tcputils.TCPfeed;
import tcputils.TCPserver;

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

                    RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.SendMessage);
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

    int send_interval;

    public DeviceProbeThread(int interval) {
        super();
        this.send_interval = interval;
    }

    public void run() {
        while (true) {
            try {
                UDPSender.send("Report", "255.255.255.255", null, RGPIO.devicePort, 0, 1);
                //timeout==0  no use to wait for a reply here, it is sent to the listener
                //retries==1  send only once now, the broadcast is repeated anyway
                Thread.sleep(send_interval);
                // Check if a device has not responded within the last send_interval.
                long now = new TimeStamp().getTimeInMillis();
                for (Device device : RGPIO.deviceMap.values()) {
                    if ((now - device.lastContact.getTimeInMillis()) > send_interval) {
                        device.setNotResponding("device did not respond in last " + send_interval + " msec");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (RGPIODeviceGroup deviceGroup : RGPIO.deviceGroupMap.values()) {
                int n = 0;
                for (Device device : RGPIO.deviceMap.values()) {
                    if ((device.deviceGroup == deviceGroup) && (device.get_status() == DeviceStatus.ACTIVE)) {
                        n++;
                    }
                }
                if (deviceGroup.minMembers != null) {
                    if (n < deviceGroup.minMembers) {
                        RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.DeviceGroupMinimum);
                        e.description = "device group has too few members : " + n;
                        e.group = deviceGroup.name;
                        RGPIO.message(e);
                    }
                }
            }

        }
    }
}

public class RGPIO {

    public static DeviceMap deviceMap;
    public static RGPIODeviceGroupMap deviceGroupMap;
    public static RGPIOInputMap digitalInputMap;
    public static RGPIOOutputMap digitalOutputMap;

    public static TCPfeed messageFeed;
    public static TCPfeed updateFeed;
    public static TCPserver clientRequests;

    public static final int serverPort = 2600;  // server listens on this port for commands from devices
    public static final int devicePort = 2500;  // devices listen on this port for server commands
    public static final int messageFeedPort = 2601; // server listens on this port. Clients can connect to message feed.
    public static final int clientRequestPort = 2602; // server listens on this port. Clients can send requests.
    public static final int updateFeedPort = 2603;  // server listens on this port. Client can get status updates from this feed

    public static void initialize(String configurationDir) {

        deviceGroupMap = new RGPIODeviceGroupMap();
        deviceMap = new DeviceMap();
        digitalInputMap = new RGPIOInputMap();
        digitalOutputMap = new RGPIOOutputMap();

        readDevicesFile(configurationDir);

        // Start listening to messages from devices
        new DeviceMonitorThread().start();

        // Start broadcasting Report requests
        DeviceProbeThread deviceProbeThread = new DeviceProbeThread(20000);
        deviceProbeThread.start();

        Console.verbose(false); // Controls  debug output from tcputils classes
        messageFeed = new TCPfeed(messageFeedPort);
        messageFeed.start();

        clientRequests = new TCPserver(clientRequestPort);

        // the clientHandler object interprets the client request and generates a reply
        ClientHandler clientHandler = new ClientHandler();
        clientRequests.addListener(clientHandler);
        clientRequests.start();
        
        updateFeed = new TCPfeed(updateFeedPort);
        updateFeed.addListener(clientHandler);
        updateFeed.start();
    }

    public static void receiveFromDevice(String ipAddress, String message) {
        // used for simulating devices
        DeviceHandler.handleDeviceMessage(ipAddress, message);

    }

    private static List<RGPIOMessageListener> listeners = new ArrayList<>();

    public static void addMessageListener(RGPIOMessageListener toAdd) {
        listeners.add(toAdd);
    }

    public static void message(RGPIOMessageEvent e) {

        // a message is generated from within RGPIO.
        // Send it out to clients connected to the message feed
        // and call all the listeners
        if (messageFeed != null) {  // when running as device, there is no messageFeed
            messageFeed.writeToClients(e.toJSON());
        }

        for (RGPIOMessageListener l : listeners) {
            try {
                l.onMessage(e);
            } catch (Exception ex) {
            };
        }
    }

    public static void printMaps(String title) {
//        System.out.println("===== maps after " + title + " =======");
//        System.out.println("\nDevice group map\n");
        RGPIO.deviceGroupMap.print();
//        System.out.println("\nDigital input map\n");
        RGPIO.digitalInputMap.print();
//        System.out.println("\nDigital output map\n");
        RGPIO.digitalOutputMap.print();
//        System.out.println();
//        System.out.println("\nPhysical devices\n");
        RGPIO.deviceMap.print();
    }

    public static void readDevicesFile(String fileName) {

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
                RGPIODeviceGroup device = null;
                RGPIOInput digitalInput = null;
                RGPIOOutput digitalOutput = null;
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
                        device = RGPIO.deviceGroupMap.add(value);
                    }
                    if (name.equals("DigitalInput")) {
                        digitalInput = RGPIO.digitalInputMap.add(value);
                    }
                    if (name.equals("DigitalOutput")) {
                        digitalOutput = RGPIO.digitalOutputMap.add(value);
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
                        device.addSelector(null, "Model", model);
                        selectors++;
                    }
                    if (HWid != null) {
                        device.addSelector(null, "HWid", HWid);
                        selectors++;
                    }
                }
                if ((digitalOutput != null) && (pin != null)) {
                    if (model != null) {
                        digitalOutput.addSelector(pin, "Model", model);
                        selectors++;
                    }
                    if (HWid != null) {
                        digitalOutput.addSelector(pin, "HWid", HWid);
                        selectors++;
                    }
                }

                if ((digitalInput != null) && (pin != null)) {
                    if (model != null) {
                        digitalInput.addSelector(pin, "Model", model);
                        selectors++;
                    }
                    if (HWid != null) {
                        digitalInput.addSelector(pin, "HWid", HWid);
                        selectors++;
                    }
                }

                if (selectors != 1) {
                    RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.Info);
                    e.description = "invalid line : " + inputLine + " selectors " + selectors;
                    RGPIO.message(e);
                }

            }
            System.out.println("read from " + fileName + " : " + lineCount + " lines");
            inputStream.close();
        } catch (FileNotFoundException fnf) {
            RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.Info);
            e.description = "file not found : " + fileName;
            RGPIO.message(e);
        } catch (IOException io) {
            RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.Info);
            e.description = "io exception while reading file : " + fileName;
            RGPIO.message(e);
        }
    }
}
