package rgpio;

import udputils.SendSleepCommandThread;
import utils.JSONString;

public class VDevice extends VSelector {

    public String name = null;
    public Integer activeMembers = 0;
    public Integer minMembers = null;

    public VDevice(String name) {
        this.name = name;
        RGPIO.VDeviceMap.put(name, this);
    }

    public String toJSON() {
        JSONString json = new JSONString();
        json.addProperty("object", "VDEV");
        json.addProperty("name", name);
        json.addProperty("activeMembers", activeMembers.toString());
        return json.asString();
    }
    
    public void sleep(int sleepTime){
           for (PDevice pdevice : RGPIO.PDeviceMap.values()) {
            if (pdevice.vdevice == this) {
                                new SendSleepCommandThread(pdevice, sleepTime).start();
            }
        }     
    }

    public void stateChange() {
        activeMembers = 0;
        for (PDevice pdevice : RGPIO.PDeviceMap.values()) {
            if (pdevice.vdevice == this) {
                if (pdevice.get_status() == PDeviceStatus.ACTIVE) {
                    activeMembers++;
                }
            }
        }
        RGPIO.webSocketServer.sendToAll(toJSON());
    }

}
