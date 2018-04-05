package udputils;

import rgpio.MessageType;
import rgpio.MessageEvent;
import rgpio.PDevice;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import rgpio.*;

public class UDPSender {
    
    int msgId = 1;

    public static String send(String message, String ipAddress, PDevice device, int port) {

        try {

            MessageEvent e = new MessageEvent(MessageType.SendMessage);
            e.description = message;
            e.ipAddress = ipAddress;
            if (device != null) {
                e.HWid = device.HWid;
            }
            RGPIO.message(e);

            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress IPAddress = InetAddress.getByName(ipAddress);
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, message.length(), IPAddress, port);
            clientSocket.send(sendPacket);
            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
