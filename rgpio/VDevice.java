package rgpio;

import utils.JSONObject;

public class VDevice extends VSelector{

    public String name = null;
    public Integer activeMembers=0;
    public Integer minMembers = null;


    public VDevice(String name) {
        this.name = name;
        RGPIO.VDeviceMap.put(name, this);
    }
    
        public String toJSON() {
        JSONObject json = new JSONObject();
        json.addProperty("object", "VDEV");
        json.addProperty("name", name);
        json.addProperty("activeMembers", activeMembers.toString());
        return json.asString();
    }
    
    public void stateChange(){
        activeMembers=0;
        for (PDevice pdevice : RGPIO.PDeviceMap.values()){
            if (pdevice.vdevice==this) activeMembers++;
        }
              RGPIO.updateFeed.writeToClients(toJSON());
    }

}
