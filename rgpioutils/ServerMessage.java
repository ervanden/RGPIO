
package rgpioutils;

// JSON description of messages from server to device (REPORT, GET, SET)

public class ServerMessage {

        public String destination;
        public String command;
        public String pin;
        public String value;

    }
