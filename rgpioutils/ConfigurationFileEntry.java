package rgpioutils;

public class ConfigurationFileEntry {

    public Integer serverPort = 2600;  // server listens on this port for commands from devices
    public Integer devicePort = 2500;  // devices listen on this port for server commands
    public Integer webSocketPort = 2603;  // websocket port
    public Integer reportInterval = 20; // server sends report requests every reportInterval sec.
    public String htmlDirectory = "/home/pi/html";

    public String toString() {
        String s = "";
        s = s + "{\n";
        s = s + "serverPort : " + serverPort + "\n";
        s = s + "devicePort : " + devicePort + "\n";
        s = s + "webSocketPort : " + webSocketPort + "\n";
        s = s + "reportInterval : " + reportInterval + "\n";
        s = s + "htmlDirectory : " + htmlDirectory + "\n";
        s = s + "}";
        return s;
    }
}
