package rgpio;

import devices.*;
import java.util.ArrayList;
import tcputils.*;

public class ClientHandler implements TCPserverListener {

    public ArrayList<String> onClientRequest(String clientID, String request) {
        ArrayList<String> reply = new ArrayList<>();

        if (request.equals("status")) {
            for (Device d : RGPIO.deviceMap.values()) {
                reply.add(d.toJSON());
                for (PInput dip : d.digitalInputs.values()) {
                    reply.add(dip.toJSON());
                }
                for (POutput dop : d.digitalOutputs.values()) {
                    reply.add(dop.toJSON());
                }
            }
            for (RGPIOInput dip : RGPIO.digitalInputMap.values()) {
                reply.add(dip.toJSON());
            }
            for (RGPIOOutput dop : RGPIO.digitalOutputMap.values()) {
                reply.add(dop.toJSON());
            }
        } else {
            RGPIOMessageEvent e = new RGPIOMessageEvent(RGPIOMessageType.Info);
            e.description = "unrecognized request from client : <" + request + ">";
            e.ipAddress = clientID;
            RGPIO.message(e);
        }

        return reply;
    }

}
