package rgpioutils;

import utils.JSONString;
import utils.TimeStamp;

public class MessageEvent {

    public Long time = null;
    public MessageType type;
    public String description = null;

    // device information
    public String value = null;
    public String model = null;
    public String vdevice = null;
    public String HWid = null;
    public String ipAddress = null;

    // pin information
    public String pinType = null;
    public String pinLabel = null;

    public String vinput = null;
    public String voutput = null;

    public MessageEvent(MessageType type) {
        time = new TimeStamp().getTimeInMillis();
        this.type = type;
    }

    public String toString() {

        String msg = "";

        TimeStamp timeStamp = new TimeStamp(0);
        timeStamp.setTimeInMillis(time);

        msg = msg + timeStamp.asString();
        msg = msg + " " + type.toString();
        msg = msg + " \"" + description + " \"";
        if (HWid != null) {
            msg = msg + " HWid=" + HWid;
        }
        if (model != null) {
            msg = msg + " model=" + model;
        }
        if (vdevice != null) {
            msg = msg + " vdevice=" + vdevice;
        }
        if (ipAddress != null) {
            msg = msg + " ipAddress=" + ipAddress;
        }
        if (pinType != null) {
            msg = msg + " pinType=" + pinType;
        }
        if (pinLabel != null) {
            msg = msg + " pinLabel=" + pinLabel;
        }
        if (value != null) {
            msg = msg + " value=" + value;
        }
        if (vinput != null) {
            msg = msg + " vinput=" + vinput;
        }
        if (voutput != null) {
            msg = msg + " voutput=" + voutput;
        }
        return msg;
    }

    public String toJSON() {

        JSONString json = new JSONString();

        TimeStamp timeStamp = new TimeStamp(0);
        timeStamp.setTimeInMillis(time);

        json.addProperty("time", timeStamp.asString());
        json.addProperty("type", type.toString());
        json.addProperty("description", description);
        if (HWid != null) {
            json.addProperty("HWid", HWid);
        }
        if (model != null) {
            json.addProperty("model", model);
        }
        if (vdevice != null) {
            json.addProperty("vdevice", vdevice);
        }
        if (ipAddress != null) {
            json.addProperty("ipAddress", ipAddress);
        }
        if (pinType != null) {
            json.addProperty("pinType", pinType);
        }
        if (pinLabel != null) {
            json.addProperty("pinLabel", pinLabel);
        }
        if (value != null) {
            json.addProperty("value", value);
        }
        if (vinput != null) {
            json.addProperty("vinput", vinput);
        }
        if (voutput != null) {
            json.addProperty("voutput", voutput);
        }
        return json.asString();
    }
}
