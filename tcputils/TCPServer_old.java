package tcputils;

import utils.Console;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class TCPServer_old extends Thread {

    private int portNumber;
    private static WSServerListener listener = null;

    public TCPServer_old(int portNumber) {
        super();
        this.portNumber = portNumber;
    }

    public void addListener(WSServerListener l) {
        if (listener != null) {
            System.out.println("TCPserver can only have 1 listener");
        } else {
            listener = l;
        }
    }
    
    // ServerThread is an inner class to give it access to 'listener'.

    class ServerThread extends Thread {

        private Socket socket = null;
        String clientID;
        DataOutputStream outToClient;
        BufferedReader inFromServer;

        public ServerThread(Socket socket) {
            super("Server Thread");
            this.socket = socket;
        }

        public void writeToClient(String s) {
            try {

                Console.println("writeToClient() client=" + clientID + " string=" + s);

                try {
                    outToClient.writeBytes(s + "\n");
                } catch (SocketException se) {
                    Console.println("socket exception while writing to client " + clientID);
                    socket.close();
                }
            } catch (IOException e) {
                Console.println("io exception while writing to client");
            }
        }

        public void run() {

            Console.println("A client has connected");

            try {

                clientID = socket.getLocalSocketAddress().toString();

                Console.println("Connection by client=" + clientID);

                outToClient = new DataOutputStream(socket.getOutputStream());
                inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                try {
                    String s;
                    ArrayList<String> reply;
                    s = inFromServer.readLine();
                    Console.println("REQUEST FROM CLIENT " + clientID + " : " + s);
                    reply = listener.onClientRequest(clientID, s);
                    for (String r : reply) {
                        writeToClient(r);
                    }
                    Console.println("DONE WITH CLIENT " + clientID);
                } catch (SocketException se) {
                    Console.println("socket exception while reading from client " + clientID);
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Console.println("SOCKET CLOSED FOR CLIENT " + clientID);
        }
    }


    public void run() {
        Console.println("TCP Server starts listening on port " + portNumber);
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            while (true) {
                ServerThread t = new ServerThread(serverSocket.accept());
                t.start();
            }
        } catch (IOException e) {
            System.out.println("Could not listen on port " + portNumber + ". Exiting...");
            System.exit(-1);
        }
    }
}
