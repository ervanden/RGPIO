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

    BufferedWriter openFile = null;

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
            if (openFile == null) {
                try {
                    String fileName = "/home/pi/RGPIO/storage/" + cmd.Arg1 + ".txt";
                    File file = new File(fileName);
                    OutputStream is = new FileOutputStream(file);
                    OutputStreamWriter isr = new OutputStreamWriter(is, "UTF-8");
                    openFile = new BufferedWriter(isr);
                } catch (FileNotFoundException fnf) {
                    reply.add("File not found");
                } catch (UnsupportedEncodingException uee) {
                    reply.add("UTF-8 encoding not supported");
                }
            }
            if (!cmd.Arg2.equals(".")) {
                try {
                    openFile.write(cmd.Arg2);
                    openFile.newLine();
                } catch (IOException io) {
                    reply.add("IOException");
                }
            } else {
                try {
                    openFile.close();
                } catch (IOException ioe) {
                    reply.add("IOException");
                }
                openFile = null;
            }

        } else {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "unrecognized request from client : |" + request + "|";
            e.ipAddress = clientID;
            RGPIO.message(e);
        }

        return reply;
    }

}
