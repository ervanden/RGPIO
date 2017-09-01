package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageEvent;
import java.util.ArrayList;
import rgpioutils.WebClientCommand;
import tcputils.*;
import utils.JSON2Object;

public class ClientHandler implements WSServerListener {

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
            System.out.println("request to store");
            System.out.println(" file " + cmd.Arg1);
            System.out.println(" data " + cmd.Arg2);
        } else {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "unrecognized request from client : |" + request + "|";
            e.ipAddress = clientID;
            RGPIO.message(e);
        }

        return reply;
    }

}
