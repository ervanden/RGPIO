package tcputils;

import java.io.*;
import java.net.*;


public class TCPClient {

    public static void connect(String host, int port, String command, boolean verbose) {
        Socket clientSocket;
        DataOutputStream outToServer;
        BufferedReader inFromServer;
        try {
            clientSocket = new Socket(host, port);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
            if (command != null) {
            if (verbose) {
                System.out.println("FROM " + clientSocket.getLocalAddress().toString() + " TO " + host + ": " + command);
            }
            outToServer.writeBytes(command + "\n");
            }
            
            String s;
            while ((s = inFromServer.readLine()) != null) {
                    System.out.println(s);
            }
            clientSocket.close();
        } catch (Exception e) {
            System.out.println("Socket error");
        }

    }

}