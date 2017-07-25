

package pidevice;

import rgpio.IOType;

public class DeviceOutput {
    String name;
    String value; // stores the value the output was last SET to
    IOType type;
    
    public SetCommandListener setCommandListener=null;

    public DeviceOutput(String name,IOType type) {
        this.name = name;
        this.type=type;
    }
    

    //  call the application to perform the GPIO action
    
    public void setValue(String newValue) {

        if (setCommandListener==null){
            System.out.println("Set command listener not assigned for Dop "+name);
        } else {
        setCommandListener.onSetCommand(this,newValue);
        }

    }

    
}
