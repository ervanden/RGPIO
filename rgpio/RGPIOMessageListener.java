package rgpio;

public interface RGPIOMessageListener {

    // Messages to the user are forwarded to listener methods.
    // The application implements the methods and decides what to do with the messages
    // The method throws Exception because the error may be detected by a java Exception
    // that is not relevant since the message string gives the information.
    
    void onMessage(RGPIOMessageEvent e) throws Exception;

}
