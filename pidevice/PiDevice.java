package pidevice;

import rgpioutils.MessageListener;
import rgpioutils.MessageEvent;
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
            DatagramSocket serverSocket = new DatagramSocket(RGPIO.devicePort);
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
                System.out.println("DEVICE RECEIVED: " + message);

                String reply = PiDevice.handleServerMessage(message);

                if (reply != null) {
                    System.out.println("DEVICE REPLIED : " + reply);
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
        String report = "Report/HWid:" + getHWid() + "/Model:" + deviceModel;
        report = report + "/Uptime:" + getUpTime();
        for (DeviceInput ip : inputs) {
            if (ip.type == IOType.digitalInput) {
                report = report + "/Dip:" + ip.name;
            }
            if (ip.type == IOType.analogInput) {
                report = report + "/Aip:" + ip.name;
            }
            if (ip.type == IOType.stringInput) {
                report = report + "/Sip:" + ip.name;
            }
        }
        for (DeviceOutput op : outputs) {
            if (op.type == IOType.digitalOutput) {
                report = report + "/Dop:" + op.name;
            }
            if (op.type == IOType.analogOutput) {
                report = report + "/Aop:" + op.name;
            }
            if (op.type == IOType.stringOutput) {
                report = report + "/Sop:" + op.name;
            }
        }

        //       System.out.println("SENDING "+report);
        if (serverIPAddress != null) {
            UDPSender.send(report, serverIPAddress, null, RGPIO.serverPort, 0, 1);
        } else {
            UDPSender.send(report, "255.255.255.255", null, RGPIO.serverPort, 0, 1);
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
                String inputLabel = null;
                if (inputType == IOType.digitalInput) {
                    inputLabel = "Dip";
                }
                if (inputType == IOType.analogInput) {
                    inputLabel = "Aip";
                }
                if (inputType == IOType.stringInput) {
                    inputLabel = "Sip";
                }
                String event = "Event"
                        + "/HWid:" + HWid
                        + "/Model:" + deviceModel
                        + "/" + inputLabel + ":" + inputName
                        + "/Value:" + value;

                if (serverIPAddress != null) {
                    UDPSender.send(event, serverIPAddress, null, RGPIO.serverPort, 0, 1);
                } else {
                    System.out.println("sendEvent() : server IP address is not known");
                }
            } else {
                System.out.println("sendEvent() : invalid arguments");
            }
        }
    }

    static public void runDevice() {

        MessagePrinter m = new MessagePrinter();
        RGPIO.addMessageListener(m);

        HWid = getHWid();

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

        /*      
         This method executes an incoming command and 
         returns the reply string to be returned to the server socket port
       
         Report    : Report is sent to the RGPIO.serverPort, no reply to the server socket port
         Set/Pin:%s/Value:%p    OK is sent to the server socket port
         Get/Pin:%s    %p
         System/Command:%s/arg1:%s/arg2:%s…    ACK
         */
        if (message.equals("Report")) {
            sendReport();
            return null;

        } else {

            try {
                String[] s = message.split("/");
                String command = s[0];
                String name;
                String value;
                if (command.equals("OK")) {
                    // OK is sent by the server when it receives a Report
                    // only to let the device know the server IP address 
                    return null;
                } else if (command.equals("Get")) {
                    String ipName = null;
                    IOType ipType = null;

                    for (int i = 1; i < s.length; i++) {
                        String nameValue = s[i];
                        String[] nv = nameValue.split(":");
                        name = nv[0];
                        value = nv[1];
                        if (name.equals("Dip")) {
                            ipName = value;
                            ipType = IOType.digitalInput;
                        }
                        if (name.equals("Aip")) {
                            ipName = value;
                            ipType = IOType.analogInput;
                        }
                        if (name.equals("Sip")) {
                            ipName = value;
                            ipType = IOType.stringInput;
                        }
                    }
                    if ((ipName != null)) {
                        System.out.println("device received GET command for input " + ipName);
                        DeviceInput ip = null;
                        for (DeviceInput d : inputs) {
                            if (d.name.equals(ipName)) {
                                ip = d;
                            }
                        }
                        if (ip == null) {
                            System.out.println("GET : unknown output " + ipName);
                        } else if (ip.type != ipType) {
                            System.out.println("GET : input of wrong iotype " + ipName + " " + ip.type);
                        } else {
                            return ip.getValue();
                        }

                    } else {
                        System.out.println("GET : invalid syntax");
                    }

                    return "-- get failed --";

                } else if (command.equals("Set")) {
                    String opName = null;
                    String opValue = null;
                    IOType opType = null;

                    for (int i = 1; i < s.length; i++) {
                        String nameValue = s[i];
                        String[] nv = nameValue.split(":");
                        name = nv[0];
                        value = nv[1];
                        if (name.equals("Dop")) {
                            opName = value;
                            opType = IOType.digitalOutput;
                        }
                        if (name.equals("Aop")) {
                            opName = value;
                            opType = IOType.analogOutput;
                        }
                        if (name.equals("Sop")) {
                            opName = value;
                            opType = IOType.stringOutput;
                        }
                        if (name.equals("Value")) {
                            opValue = value;
                        }
                    }
                    if ((opName != null) && (opValue != null)) {
                        System.out.println("device received command to set  output " + opName
                                + " to " + opValue);
                        DeviceOutput op = null;
                        for (DeviceOutput d : outputs) {
                            if (d.name.equals(opName)) {
                                op = d;
                            }
                        }
                        if (op == null) {
                            System.out.println("SET : unknown output " + opName);
                        } else if (op.type != opType) {
                            System.out.println("SET : input of wrong iotype " + opName + " " + op.type);
                        } else {
                            op.setValue(opValue);
                        }
                    } else {
                        System.out.println("device received SET command with invalid syntax");
                    }

                    return "OK";

                } else {
                    System.out.println("Device received unknown command : " + command);
                    return null;
                }

            } catch (Exception e) {
                return "Device received invalid command : " + message;
            }
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
