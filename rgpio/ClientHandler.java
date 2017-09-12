package rgpio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import rgpioutils.MessageType;
import rgpioutils.MessageEvent;
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
        } else if (cmd.Command.equals("backgrounds")) {
            /*
            backgrounds  -- the names of all files in  /home/pi/git/RGPIO/html/backgrounds are returned to the client
            */

            String dir;
            dir= "C:\\Users\\ervanden\\Documents\\java\\RGPIO\\html\\backgrounds";
            dir="/home/pi/git/RGPIO/html/backgrounds";

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
                    reply.add(basename);
                }
            }

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
