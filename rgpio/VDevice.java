package rgpio;

import java.util.ArrayList;
import java.util.List;
import udputils.SendSleepCommandThread;
import utils.JSONString;

public class VDevice extends VIO {

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

    public void sleep(int sleepTime) {
        for (PDevice pdevice : RGPIO.PDeviceMap.values()) {
            if (pdevice.vdevice == this) {
                new SendSleepCommandThread(pdevice, sleepTime).start();
            }
        }
    }

    public void sendMessage(String message) {
        for (PDevice pdevice : RGPIO.PDeviceMap.values()) {
            if (pdevice.vdevice == this) {
              pdevice.sendMessage(message);
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

    private List<VDeviceListener> listeners = new ArrayList<>();

    public void addVDeviceListener(VDeviceListener toAdd) {
        listeners.add(toAdd);
    }

    public void deliverMessage(String message) {

        for (VDeviceListener l : listeners) {
            try {
                l.onDeviceMessage(this, message);
            } catch (Exception e) {
            }
        }
    }

}
