package main;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import rgpioexample.*;
import tcputils.WSServer;
import tcputils.TCPClient;

/*
 main program that can be used
 - to run on Raspberry PI so that it acts as remote device for RGPIO
 - to run on any system to run the ExampleServer application
 - to send a UDP datagram and wait for the reply (for testing)
 - to listen for UDP datagrams (for testing)
 */
public class RGPIOmain {

    private static void usage() {
        System.out.println("Usage");
        System.out.println("  RGPIOmain server");
        System.out.println("  RGPIOmain device");
        System.out.println("  RGPIOmain udpsend command ipAddress port [timeout]");
        System.out.println("  RGPIOmain udpreceive port");
        System.out.println("  RGPIOmain tcpclient server port command");
        System.out.println("   send command, wait for reply, print reply, exit");
        System.out.println("  RGPIOmain tcpclient server port");
        System.out.println("   prints all server output (e.g. from TCPfeed)");
        System.exit(-1);
    }

    public static void main(String args[]) { //throws Exception {

        String mode = "";
        String command = null;
        String server = null;
        Integer timeout = 0;
        int port = 0;

        try {

            mode = args[0];
            
            // remaining args must all be name=value

            for (int arg = 2; arg <= args.length; arg++) {
 //               System.out.println("arg " + args[arg - 1]);
                String[] s = args[arg - 1].split("=");
                if (s[0].equals("server")) {
                    server = s[1];
                } else if (s[0].equals("port")) {
                    port = Integer.parseInt(s[1]);
                } else if (s[0].equals("timeout")) {
                    timeout = Integer.parseInt(s[1]);
                } else if (s[0].equals("command")) {
                    command = s[1];
                }
            }
        } catch (Exception e) {
            usage();
        }

        if (mode.equals("udpsend")) {

            if ((server == null) || (port == 0) || (command == null)) {
                usage();
            }

            try {
                byte[] sendData = new byte[1024];
                byte[] receiveData = new byte[1024];
                System.out.println("SENDING TO " + server + " PORT " + port + " : " + command);
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.setSoTimeout(timeout);
                InetAddress IPAddress = InetAddress.getByName(server);
                sendData = command.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                clientSocket.send(sendPacket);

                if (timeout > 0) {
                    try {
                        System.out.println("waiting for reply...");
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        clientSocket.receive(receivePacket);
                        String reply = new String(receivePacket.getData());
                        reply = reply.substring(0, receivePacket.getLength());
                        System.out.println("REPLY : " + reply);
                    } catch (SocketTimeoutException se) {
                        System.out.println("timed out");
                    }
                }

                clientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (mode.equals("udpreceive")) {

            if (port == 0) {
                usage();
            }

            try {
                DatagramSocket serverSocket = new DatagramSocket(port);
                byte[] receiveData = new byte[1024];
                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);
                    String message = new String(receivePacket.getData());
                    message = message.substring(0, receivePacket.getLength());
                    System.out.println("RECEIVED: " + message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (mode.equals("server")) {

            (new ExampleServer()).start();

        } else if (mode.equals("device")) {

            (new ExampleDevice()).start();

        } else if (mode.equals("tcpclient")) {

            if ((server == null) || (port == 0)) {
                usage();
            }

            TCPClient.connect(server, port, command, false);
            
        } else {

            new WSServer(8887).start();

        }
    }
}
