package rgpio;

public class VDevice extends VSelector{

    public String name = null;
    public Integer minMembers = null;


    public VDevice(String name) {
        this.name = name;
        RGPIO.VDeviceMap.put(name, this);
    }

}
