package rgpio;

public class VDevice extends Selector{

    public String name = null;
    public Integer minMembers = null;


    public VDevice(String name) {
        this.name = name;
        RGPIO.deviceGroupMap.put(name, this);
    }

}
