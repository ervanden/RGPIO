package rgpioutils;

// message from server to device

public class ServerMessage {

    public  String to;
    public  String from;
    public  String id;
    public  String command;

    // command = GET, SET
    
    public String pin;
    public String value;

    // command = MESSAGE
    
    public String message;
}
