
package rgpioutils;


public class ConfigurationFileEntry {

        public int serverPort = 2600;  // server listens on this port for commands from devices
        public int devicePort = 2500;  // devices listen on this port for server commands
        public int webSocketPort = 2603;  // websocket port
        public int reportInterval = 20; // server sends report requests every reportInterval sec.

        public String toString() {
            String s = "";
            s = s + "{\n";
            s = s + "serverPort : " + serverPort + "\n";
            s = s + "devicePort : " + devicePort + "\n";
            s = s + "webSocketPort : " + webSocketPort + "\n";
            s = s + "reportInterval : " + reportInterval + "\n";
            s = s + "}";
            return s;
        }
    }
