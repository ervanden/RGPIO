
package tcputils;

import java.util.ArrayList;


public interface WSServerListener {

    // TCPserver listens on a dedicated port. 
    // When a client connects, its message is forwarded to the listener.
    // The listener object handles the request and returns the reply to be sent back to the client.
    
   ArrayList<String> onClientRequest(String  clientID, String request);

}
