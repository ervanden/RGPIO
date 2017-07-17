

package pidevice;

import java.util.ArrayList;
import java.util.List;


public class DeviceOutput {
    String name;
    String value; // stores the value the output was last SET to
    String type;
    
    public SetCommandListener setCommandListener=null;

    public DeviceOutput(String name,String type, String value) {
        this.name = name;
        this.type=type;
        this.value=value;
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
