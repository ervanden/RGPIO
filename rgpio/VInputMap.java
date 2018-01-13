package rgpio;

import java.util.HashMap;

public class VInputMap extends HashMap<String, VInput> {

    IOType type;

    public VInputMap(IOType type) {
        super();
        this.type = type;
    }

    public VInput add(String name) {
        VInput vinput = this.get(name);
        if (vinput == null) {
            if (type == IOType.digitalInput) {
                vinput = new VDigitalInput();
            }
            if (type == IOType.analogInput) {
                vinput = new VAnalogInput();
            }
            if (type == IOType.stringInput) {
                vinput = new VStringInput();
            }
            vinput.name = name;
            this.put(name, vinput);
            vinput.type = type;
        }
        return vinput;
    }
    
}
