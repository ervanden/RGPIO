package rgpio;

import rgpioutils.MessageType;
import rgpioutils.MessageEvent;
import java.util.ArrayList;
import tcputils.*;

public class ClientHandler implements TCPserverListener {

    public ArrayList<String> onClientRequest(String clientID, String request) {
        ArrayList<String> reply = new ArrayList<>();

        if (request.equals("status")) {
            RGPIO.printMaps("status");
            for (PDevice d : RGPIO.PDeviceMap.values()) {
                reply.add(d.toJSON());
                for (PInput dip : d.digitalInputs.values()) {
                    reply.add(dip.toJSON());
                }
                for (POutput dop : d.digitalOutputs.values()) {
                    reply.add(dop.toJSON());
                }
            }
            for (VInput dip : RGPIO.VDigitalInputMap.values()) {
                reply.add(dip.toJSON());
            }
            for (VOutput dop : RGPIO.VDigitalOutputMap.values()) {
                reply.add(dop.toJSON());
            }
        } else {
            MessageEvent e = new MessageEvent(MessageType.Info);
            e.description = "unrecognized request from client : <" + request + ">";
            e.ipAddress = clientID;
            RGPIO.message(e);
        }

        return reply;
    }

}
