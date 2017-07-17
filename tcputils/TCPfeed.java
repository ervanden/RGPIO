
package tcputils;

import utils.Console;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;



/* 
 A TCPfeed object listens to the given port and starts a thread for every client that connects.
 Input from the client is ignored.
 The method 'writeToClients(s) sends the string s to all the connected clients. Clients can connect and
 disconnect at any time.

 Intended usage: clients can be started from PHP and can continuously update a web site with
 output from an application that runs as a daemon
*/




class FeedThread extends Thread {

    private Socket socket = null;
    String clientID;
    DataOutputStream outToClient;
    BufferedReader inFromServer;

    public FeedThread(Socket socket) {
        super("Server Thread");
        this.socket = socket;

    }

    public void writeToClient(String s) {
        try {

            Console.println("writeToClient() client=" + clientID + " string=" + s);

            try {
                outToClient.writeBytes(s + "\n");
            } catch (SocketException se) {
                Console.println("socket exception while writing to client");
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
                while ((s = inFromServer.readLine()) != null) {
                    Console.println("MSG FROM CLIENT " + clientID + " : " + s);
                }

                Console.println("EXIT CLIENT " + clientID);

            } catch (SocketException se) {
                Console.println("socket exception while reading from client");
                socket.close();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        Console.println("EXIT CLIENT " + clientID);
    }
}

public class TCPfeed extends Thread {

    private int portNumber;
    private List<FeedThread> serverThreads = new ArrayList<>();

    public TCPfeed(int portNumber) {
        super();
        this.portNumber = portNumber;
    }


    public void writeToClients(String s) {
        Console.println("TCPfeed received msg to be forwarded: " + s);
        Console.println("nr of client threads=" + serverThreads.size());

        boolean purge = false;
        for (FeedThread t : serverThreads) {
            Console.println("CLIENT " + t.clientID + " STATE " + t.getState());
            if (t.getState() != Thread.State.TERMINATED) {
                t.writeToClient(s);
            } else {
                purge = true;
            }
        }

        if (purge) {
            Console.println("PURGING ");
            List<FeedThread> terminatedThreads = new ArrayList<>();
            for (FeedThread t : serverThreads) {
                if (t.getState() == Thread.State.TERMINATED) {
                    terminatedThreads.add(t);
                }
            }
            for (FeedThread t : terminatedThreads) {
                Console.println("REMOVING CLIENT " + t.clientID);
                serverThreads.remove(t);
            }
        }
    }

    public void run() {
        Console.println("TCP Server starts listening on port " + portNumber);
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            while (true) {
                FeedThread t = new FeedThread(serverSocket.accept());
                t.start();
                serverThreads.add(t);
            }
        } catch (IOException e) {
            Console.println("Could not listen on port " + portNumber + ". Exiting...");
            System.exit(-1);
        }
    }
}
