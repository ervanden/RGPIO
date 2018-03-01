package pidevice;

import rgpio.MessageListener;
import rgpio.MessageEvent;
import udputils.UDPSender;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import rgpio.*;
import utils.TimeStamp;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Scanner;
import rgpioutils.ServerMessage;
import utils.JSON2Object;

class MessagePrinter implements MessageListener {

    public void onMessage(MessageEvent e) throws Exception {
        System.out.println(e.toString());
    }
}

class InitialReportThread extends Thread {

    public InitialReportThread() {
        super("InitialReportThread");
    }

    public void run() {
        // device broadcasts  Report every second when it powers up, until it knows the server IP address
        // (when server replies OK to Report)
        while (PiDevice.serverIPAddress == null) {
            PiDevice.sendReport();
            try {
                sleep(1000);
            } catch (InterruptedException ie) {
            }
        }
    }
}

class CommandListener extends Thread {

    public CommandListener() {
        super("CommandListener");
    }

    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(PiDevice.devicePort);
            serverSocket.setBroadcast(true);
            byte[] receiveData = new byte[1024];
            byte[] sendData;
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                InetAddress serverInetAddress = receivePacket.getAddress();
                PiDevice.serverIPAddress = serverInetAddress.toString().substring(1);
                int devicePort = receivePacket.getPort();
                String message = new String(receivePacket.getData());
                message = message.substring(0, receivePacket.getLength());
                // System.out.println("DEVICE RECEIVED: " + message);

                String reply = PiDevice.handleServerMessage(message);

                if (reply != null) {
                    //System.out.println("DEVICE REPLIED : " + reply);
                    sendData = reply.getBytes();
                    DatagramPacket sendPacket
                            = new DatagramPacket(sendData, sendData.length, serverInetAddress, devicePort);
                    serverSocket.send(sendPacket);
                }

            }
        } catch (SocketException so) {
            so.printStackTrace();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}

public class PiDevice {

    static public String deviceModel;
    static public String HWid;
    static public ArrayList<DeviceInput> inputs = new ArrayList<>();
    static public ArrayList<DeviceOutput> outputs = new ArrayList<>();

    static HashMap<String, IOType> inputTypeMap = new HashMap<>();

    static String serverIPAddress = null;
    static int serverPort;
    static int devicePort;
    static long bootTimeInMillis = 0;
    static TimeStamp windowsBootTime = null;

    static public DeviceInput addDigitalInput(String name) {
        DeviceInput dip = new DeviceInput(name, IOType.digitalInput);
        inputs.add(dip);
        inputTypeMap.put(name, IOType.digitalInput);
        return dip;
    }

    static public DeviceOutput addDigitalOutput(String name) {
        DeviceOutput dop = new DeviceOutput(name, IOType.digitalOutput);
        outputs.add(dop);
        inputTypeMap.put(name, IOType.digitalOutput);
        return dop;
    }

    static public DeviceInput addAnalogInput(String name) {
        DeviceInput aip = new DeviceInput(name, IOType.analogInput);
        inputs.add(aip);
        inputTypeMap.put(name, IOType.analogInput);
        return aip;
    }

    static public DeviceOutput addAnalogOutput(String name) {
        DeviceOutput aop = new DeviceOutput(name, IOType.analogOutput);
        outputs.add(aop);
        inputTypeMap.put(name, IOType.analogOutput);
        return aop;
    }

    static public DeviceInput addStringInput(String name) {
        DeviceInput sip = new DeviceInput(name, IOType.stringInput);
        inputs.add(sip);
        inputTypeMap.put(name, IOType.stringInput);
        return sip;
    }

    static public DeviceOutput addStringOutput(String name) {
        DeviceOutput sop = new DeviceOutput(name, IOType.stringOutput);
        outputs.add(sop);
        inputTypeMap.put(name, IOType.stringOutput);
        return sop;
    }

    static public void printDevicePins() {
        System.out.println("---device pins ---");
        for (DeviceInput ip : inputs) {
            System.out.println("device pin " + ip.name + " " + ip.type.name());
        }
        for (DeviceOutput op : outputs) {
            System.out.println("device pin " + op.name + " " + op.type.name());
        }
    }

    static void sendReport() {

        /*
         {"command":"REPORT","hwid":"1625496","model":"BUTTON","uptime":"7","dips":["on0","on2"]}
         */
        String report = "{\"command\":\"REPORT\","
                + "\"hwid\":\"" + getHWid() + "\","
                + "\"model\":\"" + deviceModel + "\","
                + "\"uptime\":\"" + getUpTime() + "\"";

        String ioString;
        String separator;

        ioString = "\"dips\":[";
        separator = "";
        for (DeviceInput ip : inputs) {
            if (ip.type == IOType.digitalInput) {
                ioString = ioString + separator + "\"" + ip.name + "\"";
                separator = ",";
            }
        }
        ioString = ioString + "]";
        if (separator.equals(",")) {
            report = report + "," + ioString;
        }

        ioString = "\"aips\":[";
        separator = "";
        for (DeviceInput ip : inputs) {
            if (ip.type == IOType.analogInput) {
                ioString = ioString + separator + "\"" + ip.name + "\"";
                separator = ",";
            }
        }
        ioString = ioString + "]";
        if (separator.equals(",")) {
            report = report + "," + ioString;
        }

        ioString = "\"dops\":[";
        separator = "";
        for (DeviceOutput op : outputs) {
            if (op.type == IOType.digitalOutput) {
                ioString = ioString + separator + "\"" + op.name + "\"";
                separator = ",";
            }
        }
        ioString = ioString + "]";
        if (separator.equals(",")) {
            report = report + "," + ioString;
        }

        ioString = "\"aops\":[";
        separator = "";
        for (DeviceOutput op : outputs) {
            if (op.type == IOType.analogOutput) {
                ioString = ioString + separator + "\"" + op.name + "\"";
                separator = ",";
            }
        }
        ioString = ioString + "]";
        if (separator.equals(",")) {
            report = report + "," + ioString;
        }
        
        report=report+"}";
        
        if (serverIPAddress != null) {
            UDPSender.send(report, serverIPAddress, null, PiDevice.serverPort, 0, 1);
        } else {
            UDPSender.send(report, RGPIO.broadcastAddress, null, PiDevice.serverPort, 0, 1);
        }
    }

    static public void sendEvent(String inputName, String value) {
        boolean validArgs = false;
        IOType inputType;

        inputType = inputTypeMap.get(inputName);
        if (inputType == null) {
            System.out.println("sendEvent() : unknown input " + inputName);
        } else {
            if (inputType.equals(IOType.digitalInput)) {
                if ((value.equals("High")) || (value.equals("Low"))) {
                    validArgs = true;
                } else {
                    System.out.println("sendEvent() : Digital input " + inputName
                            + " can only be set to High or Low");
                }
            }

            if (validArgs) {
                String event = "Event"
                        + "/HWid:" + HWid
                        + "/Model:" + deviceModel
                        + "/" + IOType.longToShort(inputType) + ":" + inputName
                        + "/Value:" + value;

                if (serverIPAddress != null) {
                    UDPSender.send(event, serverIPAddress, null, PiDevice.serverPort, 0, 1);
                } else {
                    System.out.println("sendEvent() : server IP address is not known");
                }
            } else {
                System.out.println("sendEvent() : invalid arguments");
            }
        }
    }

    static public void runDevice(int serverPort, int devicePort) {

        PiDevice.serverPort = serverPort;
        PiDevice.devicePort = devicePort;

        MessagePrinter m = new MessagePrinter();
        RGPIO.addMessageListener(m);

        HWid = getHWid();
        bootTimeInMillis = (new TimeStamp()).getTimeInMillis();

        Integer upTime = getUpTime();
        if (upTime == null) {
            System.out.println("System uptime could not be determined");
        } else {
            int upTimeDays = upTime / (60 * 60 * 24);
            upTime = upTime % (60 * 60 * 24);
            int upTimeHours = upTime / (60 * 60);
            upTime = upTime % (60 * 60);
            int upTimeMinutes = upTime / 60;
            upTime = upTime % 60;
            System.out.println("System up since " + upTimeDays + " days, "
                    + upTimeHours + " hours, "
                    + +upTimeMinutes + " minutes, "
                    + +upTime + " seconds");
        }

        (new InitialReportThread()).start();

        // device opens a socket and starts listening to commands
        (new CommandListener()).start();
    }

    public static String handleServerMessage(String message) {

        if (message.equals("OK")) {
            // OK is sent by the server when it receives a Report
            // only to let the device know the server IP address 
            return null;
        } else {

            ServerMessage cmd;
            cmd = (ServerMessage) JSON2Object.jsonStringToObject(message, ServerMessage.class);

            String destination = cmd.destination;
            String command = cmd.command;
            String pin = cmd.pin;
            String value = cmd.value;

            if (command.equals("REPORT")) {
                sendReport();
                return null;
            } else if (command.equals("GET")) {

                DeviceInput ip = null;
                for (DeviceInput d : inputs) {
                    if (d.name.equals(pin)) {
                        ip = d;
                    }
                }
                if (ip == null) {
                    System.out.println("GET : unknown output " + pin);
                } else {
System.out.println("RETURNING VALUE   " + ip.getValue() + " FOR " + pin);
                    return ip.getValue();
                }
                return "GET : unknown output " + pin;

            } else if (command.equals("SET")) {

                DeviceOutput op = null;
                for (DeviceOutput d : outputs) {
                    if (d.name.equals(pin)) {
                        op = d;
                    }
                }
                if (op == null) {
                    System.out.println("SET : unknown output " + pin);
                } else {
                    System.out.println("SETTING  " + pin + " TO " + value);
                    op.setValue(value);
                }

                return "OK";

            }
            return null;

        }

    }

    static public String getHWid() {
        // returns the host name as HWid
        // The 'hostname' command happens to be the same on windows and linux
        Process p;
        try {
            p = Runtime.getRuntime().exec("hostname");
            p.waitFor();
            BufferedReader reader
                    = new BufferedReader(new InputStreamReader(p.getInputStream()));

            return reader.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    static public Integer getUpTime() {
        long nowInMillis = (new TimeStamp()).getTimeInMillis();
        Long delta = (nowInMillis - bootTimeInMillis);
        return delta.intValue() / 1000;

    }

    static public Integer getSystemUpTime() {

        // Linux  read uptime from /proc
        try {
            Scanner sc = new Scanner(new FileInputStream("/proc/uptime"));
            sc.useDelimiter("[.]");
            return sc.nextInt();
        } catch (Exception e) {
        }

        // windows : find boot time from 'systeminfo'
        if (windowsBootTime == null) {  // first time it is stored because exec systeminfo is slow
            Process p;
            try {
                p = Runtime.getRuntime().exec("systeminfo");
                p.waitFor();
                BufferedReader reader
                        = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("System Boot Time:")) {
                        System.out.println(line);

                        Scanner sc = new Scanner(line);
                        sc.useDelimiter("[^0-9]+");
                        int day = sc.nextInt();
                        int month = sc.nextInt();
                        int year = sc.nextInt();
                        int hour = sc.nextInt();
                        int minute = sc.nextInt();
                        int second = sc.nextInt();

                        windowsBootTime = new TimeStamp(0);
                        windowsBootTime.set(Calendar.YEAR, year);
                        windowsBootTime.set(Calendar.MONTH, month - 1); // Calendar uses months 0-11                   
                        windowsBootTime.set(Calendar.DAY_OF_MONTH, day);
                        windowsBootTime.set(Calendar.HOUR_OF_DAY, hour);
                        windowsBootTime.set(Calendar.MINUTE, minute);
                        windowsBootTime.set(Calendar.SECOND, second);
                        System.out.println("windows system boot " + windowsBootTime.asLongString());

                    }
                }
            } catch (Exception e) {
            }
        }

        if (windowsBootTime != null) {
            return (int) ((new TimeStamp()).getTimeInMillis()
                    - windowsBootTime.getTimeInMillis()) / 1000;
        }

        // both attempts failed
        return null;
    }

}
