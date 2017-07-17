package rgpio;

public class RGPIODeviceGroup extends RGPIOSelector{

    public String name = null;
    public Integer minMembers = null;


    public RGPIODeviceGroup(String name) {
        this.name = name;
        RGPIO.deviceGroupMap.put(name, this);
    }

}
