package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageEvent;
import java.util.ArrayList;
import tcputils.*;

public class ClientHandler implements WSServerListener {

    public ArrayList<String> onClientRequest(String clientID, String request) {
        ArrayList<String> reply = new ArrayList<>();

        if (request.equals("status")) {
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
        } else {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "unrecognized request from client : |" + request + "|";
            e.ipAddress = clientID;
            RGPIO.message(e);
        }

        return reply;
    }

}
