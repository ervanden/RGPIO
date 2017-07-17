package udputils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import rgpio.*;
import devices.*;

public class UDPSender {

    public static String send(String message, String ipAddress, Device device, int port, int timeout, int retries) {

        // returns the reply if it arrives within the timeout, otherwise null
        // timeout of 0 means we skip waiting for the reply
        try {
            //           byte[] sendData = new byte[1024];
            byte[] receiveData = new byte[1024];

            RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.SendMessage);
            e.description = "<" + message + ">";
            e.ipAddress = ipAddress;
            if (device != null) {
                e.HWid = device.HWid;
            }
            RGPIO.message(e);

            for (int r = 1; r <= retries; r++) {
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.setSoTimeout(timeout);
                InetAddress IPAddress = InetAddress.getByName(ipAddress);

                byte[] sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, message.length(), IPAddress, port);
//                sendPacket.setLength(message.length());
                clientSocket.send(sendPacket);

                if (timeout > 0) {
                    try {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        clientSocket.receive(receivePacket);
                        String reply = new String(receivePacket.getData());
                        reply = reply.substring(0, receivePacket.getLength());

                        RGPIOMessageEvent e1 = new RGPIOMessageEvent(RGPIOMessageType.SendMessage);
                        e1.description = ">REPLY(" + r + ") : " + reply;
                        e1.ipAddress = ipAddress;
                        if (device != null) {
                            e1.HWid = device.HWid;
                        }
                        RGPIO.message(e1);

                        clientSocket.close();
                        return reply;
                    } catch (SocketTimeoutException se) {
                        // System.out.println("socket timeout");
                    }
                } else {  // sender not interested in reply
                    clientSocket.close();
                    return null;
                }
            }
            //  socket timed out 3x
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
