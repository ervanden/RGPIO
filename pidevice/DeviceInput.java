package pidevice;

import rgpio.IOType;

public class DeviceInput {

    String name;
    IOType type;
    
    public GetCommandListener getCommandListener=null;

    public DeviceInput(String name, IOType type) {
        this.name = name;
        this.type = type;
    }

    //  call the application to perform the GPIO action
    
    public String getValue() {

        if (getCommandListener==null){
            System.out.println("Get command listener not defined for device input "+name);
        } else {
        return getCommandListener.onGetCommand(this);
        }
        return "X";

    }
}
