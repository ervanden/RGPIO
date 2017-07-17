package pidevice;

public class DeviceInput {

    String name;
    String value;
    String type;
    
    public GetCommandListener getCommandListener=null;

    public DeviceInput(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    //  call the application to perform the GPIO action
    
    public String getValue() {

        if (getCommandListener==null){
            System.out.println("Get command listener not assigned for Dip "+name);
        } else {
        return getCommandListener.onGetCommand(this);
        }
        return "X";

    }
}
