package rgpio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import rgpioutils.WebClientCommand;
import tcputils.*;
import utils.JSON2Object;

public class ClientHandler implements WSServerListener {

    BufferedWriter storeFile = null;

    public ClientHandler() {
    }

    public ArrayList<String> onClientRequest(String clientID, String request) {
        ArrayList<String> reply = new ArrayList<>();

        WebClientCommand cmd;
        cmd = (WebClientCommand) JSON2Object.jsonStringToObject(request, WebClientCommand.class);

        System.out.println(cmd.toString());

        if (cmd.Command.equals("status")) {
            for (PDevice d : RGPIO.PDeviceMap.values()) {
                reply.add(d.toJSON());
                for (PInput ip : d.inputs.values()) {
                    reply.add(ip.toJSON());
                }
                for (POutput op : d.outputs.values()) {
                    reply.add(op.toJSON());
                }
            }
            for (VInput ip : RGPIO.VDigitalInputMap.values()) {
                reply.add(ip.toJSON());
            }
            for (VOutput op : RGPIO.VDigitalOutputMap.values()) {
                reply.add(op.toJSON());
            }
            for (VInput ip : RGPIO.VAnalogInputMap.values()) {
                reply.add(ip.toJSON());
            }
            for (VOutput op : RGPIO.VAnalogOutputMap.values()) {
                reply.add(op.toJSON());
            }
            for (VInput ip : RGPIO.VStringInputMap.values()) {
                reply.add(ip.toJSON());
            }
            for (VOutput op : RGPIO.VStringOutputMap.values()) {
                reply.add(op.toJSON());
            }

            String jsonReply = "{ \"object\":\"STATUSCOMPLETE\"}";
            reply.add(jsonReply);

        } else if (cmd.Command.equals("store")) {
            /*
             store basename data1   -- file /home/pi/RGPIO/storage/basename.txt   is opened for writing
             -- data1 is written
             store basename data2   -- data2 is written
             store basename data3   -- data3 is written
             store basename .       -- file is closed
             */
            if (storeFile == null) {
                try {
                    String fileName = "/home/pi/RGPIO/storage/" + cmd.Arg1 + ".txt";
                    reply.add("opening file : " + fileName);
                    File file = new File(fileName);
                    OutputStream is = new FileOutputStream(file);
                    OutputStreamWriter isr = new OutputStreamWriter(is, "UTF-8");
                    storeFile = new BufferedWriter(isr);
                } catch (FileNotFoundException fnf) {
                    reply.add("File not found");
                } catch (UnsupportedEncodingException uee) {
                    reply.add("UTF-8 encoding not supported");
                }
            }
            if (!cmd.Arg2.equals(".")) {
                try {
                    storeFile.write(cmd.Arg2);
                    reply.add("storing : " + cmd.Arg2);
                    storeFile.newLine();
                } catch (IOException io) {
                    reply.add("IOException");
                }
            } else {
                try {
                    storeFile.close();
                } catch (IOException ioe) {
                    reply.add("IOException");
                }
                storeFile = null;
            }
        } else if (cmd.Command.equals("load")) {
            /*
             load basename   -- file /home/pi/RGPIO/storage/basename.txt   is read and its content sent to the client
             */

            try {
                String fileName = "/home/pi/RGPIO/storage/" + cmd.Arg1 + ".txt";
                reply.add("opening file : " + fileName);
                File file = new File(fileName);
                InputStream is = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader loadFile = new BufferedReader(isr);

                String l;
                while ((l = loadFile.readLine()) != null) {
                    reply.add(l);
                }
                loadFile.close();
            } catch (FileNotFoundException fnf) {
                reply.add("File not found");
            } catch (IOException ioe) {
                reply.add("IOException");
            }

        } else if (cmd.Command.equals("backgrounds")) {
            /*
             backgrounds  -- the names of all files in  /home/pi/html/backgrounds are returned to the client
             */

            String dir;
            dir = "C:\\Users\\ervanden\\Documents\\java\\RGPIO\\html\\backgrounds";
            dir = RGPIO.htmlDirectory + "/backgrounds";

            String jsonReply = "{ \"object\":\"BACKGROUNDS\", \"list\": [";
            String separator = "";

            File[] files = new File(dir).listFiles();//If dir does not denote a directory, then listFiles() returns null. 
            String basename = "";
            String fullname = "";
            for (File file : files) {
                if (file.isFile()) {
                    fullname = file.getName();
                    int pos = fullname.lastIndexOf(".");
                    if (pos > 0) {
                        basename = fullname.substring(0, pos);
                    }
                    jsonReply = jsonReply + separator + "\"" + basename + "\"";
                    separator = ", ";
                }
            }
            jsonReply = jsonReply + "] }";
            reply.add(jsonReply);

        } else if (cmd.Command.equals("icons")) {
            /*
             the full file names of all available icons are returned to the client
             */

            String dir;
            dir = RGPIO.htmlDirectory + "/allicons";

            String jsonReply = "{ \"object\":\"ICONS\", \"list\": [";
            String separator = "";

            File[] files = new File(dir).listFiles();//If dir does not denote a directory, then listFiles() returns null. 
            for (File file : files) {
                if (file.isFile()) {
                    jsonReply = jsonReply + separator + "\"" + file.getName() + "\"";
                    separator = ", ";
                }
            }
            jsonReply = jsonReply + "] }";
            reply.add(jsonReply);

        } else if (cmd.Command.equals("rrdentries")) {
            /*
             the names of all the rrd entries are returned to the client
             */

            String jsonReply = "{ \"object\":\"RRDENTRIES\", \"list\": [";
            String separator = "";

            for (VIO vio : RGPIO.RRDVIO) {
                jsonReply = jsonReply + separator + "\"" + vio.name + "\"";
                separator = ", ";
            }

            jsonReply = jsonReply + "] }";
            reply.add(jsonReply);

        } else if (cmd.Command.equals("graph")) {
            /*
             create graph and send file name back to client
             */
            String command = "";
            String graphFileName = "";
            Process p;
            try {

                if (System.getProperty("file.separator").equals("/")) {
                    command = "/home/pi/git/run RGPIOGraph " + cmd.Arg1;
                } else {
                    command = "java -jar C:\\Users\\erikv\\Documents\\NetBeansProjects\\RGPIOGraph\\dist\\RGPIOGraph.jar " + cmd.Arg1;
                }

                System.out.println("excuting " + command);
                p = Runtime.getRuntime().exec(command);
                p.waitFor();

                // RGPIOGraph outputs the file name of the graph 
                // The directory is /home/pi/html/graphs, hard coded in RGPIOGraph and in the web client
                BufferedReader reader
                        = new BufferedReader(new InputStreamReader(p.getInputStream()));

                graphFileName = reader.readLine();
                System.out.println(" Graph file name returned by RGPIOGraph = " + graphFileName);

            } catch (InterruptedException e) {
                System.out.println("ERROR : could not execute command " + command);
            } catch (IOException e) {
                System.out.println("ERROR : could not read RGPIOGraph output");
            }

            String fileName = "/graphs/" + graphFileName;
            String jsonReply = "{ \"object\":\"GRAPH\", \"filename\":\"" + fileName + "\"}";
            reply.add(jsonReply);

        } else {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "unrecognized websocket request : |" + request + "|";
            e.ipAddress = clientID;
            RGPIO.message(e);

            reply.add("unrecognized websocket request : " + request);
        }

        return reply;
    }

}
